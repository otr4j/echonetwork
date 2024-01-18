/*
 * client-otr4j, the echonetwork client for otr4j.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.client.otr4j;

import net.java.otr4j.api.ClientProfile;
import net.java.otr4j.api.Event;
import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrEngineHost;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session.Version;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.crypto.DSAKeyPair;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.crypto.ed448.EdDSAKeyPair;
import net.java.otr4j.io.OtrEncodables;
import net.java.otr4j.io.OtrInputStream;
import net.java.otr4j.messages.ClientProfilePayload;
import net.java.otr4j.messages.ValidationException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.requireNonNull;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;

final class Host implements OtrEngineHost {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());

    private static final SecureRandom RANDOM = new SecureRandom();

    public final ArrayBlockingQueue<Action<?>> actions = new ArrayBlockingQueue<>(9);

    private final DSAKeyPair dsaKeyPair = DSAKeyPair.generateDSAKeyPair(RANDOM);
    private final EdDSAKeyPair edDSAKeyPair = EdDSAKeyPair.generate(RANDOM);

    private final EdDSAKeyPair forgingKeyPair = EdDSAKeyPair.generate(RANDOM);

    private final OtrPolicy policy;

    private final OutputStream out;

    private ClientProfilePayload payload;

    Host(@Nonnull final OutputStream out, @Nonnull final InstanceTag tag, @Nonnull final OtrPolicy policy) {
        this.out = requireNonNull(out);
        this.policy = requireNonNull(policy);
        final EdDSAKeyPair forging = EdDSAKeyPair.generate(RANDOM);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        this.payload = ClientProfilePayload.signClientProfile(new ClientProfile(tag, this.edDSAKeyPair.getPublicKey(),
                forging.getPublicKey(), List.of(Version.THREE, Version.FOUR), this.dsaKeyPair.getPublic()),
            calendar.getTimeInMillis() / 1000, this.dsaKeyPair, this.edDSAKeyPair);
    }

    @Override
    public void injectMessage(@Nonnull final SessionID sessionID, @Nonnull final String msg) {
        try {
            sendMessage(this.out, sessionID.getUserID(), msg);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to inject message in network.", e);
        }
    }

    @Override
    public OtrPolicy getSessionPolicy(@Nonnull final SessionID sessionID) {
        return this.policy;
    }

    @Override
    public int getMaxFragmentSize(@Nonnull final SessionID sessionID) {
        return MAX_VALUE;
    }

    @Nonnull
    @Override
    public DSAKeyPair getLocalKeyPair(@Nonnull final SessionID sessionID) {
        return this.dsaKeyPair;
    }

    @Nonnull
    @Override
    public EdDSAKeyPair getLongTermKeyPair(@Nonnull final SessionID sessionID) {
        return this.edDSAKeyPair;
    }

    @Nonnull
    @Override
    public EdDSAKeyPair getForgingKeyPair(@Nonnull final SessionID sessionID) {
        return this.forgingKeyPair;
    }

    @Nonnull
    @Override
    public String getReplyForUnreadableMessage(@Nonnull final SessionID sessionID, @Nonnull final String identifier) {
        return "Unreadable message";
    }

    @Override
    public String getFallbackMessage(@Nonnull final SessionID sessionID) {
        return null;
    }

    @Override
    public void updateClientProfilePayload(@Nonnull final byte[] payload) {
        LOGGER.log(Level.INFO, "Host was requested to update ClientProfile-payload. ({0} bytes)", payload.length);
        try {
            this.payload = ClientProfilePayload.readFrom(new OtrInputStream(payload));
        } catch (ValidationException | OtrCryptoException | ProtocolException e) {
            throw new IllegalArgumentException("Invalid client profile payload provided for update and publishing.", e);
        }
    }

    @Nonnull
    @Override
    public byte[] restoreClientProfilePayload() {
        return OtrEncodables.encode(this.payload);
    }

    @SuppressWarnings({"PMD.CompareObjectsWithEquals", "PMD.CognitiveComplexity"})
    @Override
    public <T> void handleEvent(@Nonnull final SessionID sessionID, @Nonnull final InstanceTag receiver,
                                @Nonnull final Event<T> event, @Nonnull final T payload) {
        if (event == Event.UNREADABLE_MESSAGE_RECEIVED) {
            LOGGER.log(Level.FINE, "unreadableMessageReceived: {0}", new Object[]{sessionID});
        } else if (event == Event.UNENCRYPTED_MESSAGE_RECEIVED) {
            final String msg = Event.UNENCRYPTED_MESSAGE_RECEIVED.convert(payload);
            LOGGER.log(Level.FINE, "unencryptedMessageReceived: {0}: {1}", new Object[]{sessionID, msg});
        } else if (event == Event.ERROR) {
            final String error = Event.ERROR.convert(payload);
            LOGGER.log(Level.SEVERE, "Client/OTR {1}: {0}", new Object[]{error, sessionID});
        } else if (event == Event.SESSION_FINISHED) {
            LOGGER.log(Level.FINE, "Session is finished: {0}", new Object[]{sessionID});
        } else if (event == Event.ENCRYPTED_MESSAGES_REQUIRED) {
            final String msgText = Event.ENCRYPTED_MESSAGES_REQUIRED.convert(payload);
            LOGGER.log(Level.FINE, "requireEncryptedMessage: {0}: {1}", new Object[]{sessionID, msgText});
        } else if (event == Event.SMP_REQUEST_SECRET) {
            final String question = Event.SMP_REQUEST_SECRET.convert(payload);
            LOGGER.log(Level.FINE, "askForSecret: {0}:{1}: {2}", new Object[]{sessionID, receiver, question});
            this.actions.add(new Action<>(sessionID, receiver, Event.SMP_REQUEST_SECRET, question));
        } else if (event == Event.SMP_ABORTED) {
            final Event.AbortReason reason = Event.SMP_ABORTED.convert(payload);
            if (reason == Event.AbortReason.USER) {
                LOGGER.log(Level.FINE, "SMP aborted: {0}", new Object[]{sessionID});
            } else if (reason == Event.AbortReason.INTERRUPTION) {
                LOGGER.log(Level.FINE, "SMP interrupted: {0}", new Object[]{sessionID});
            } else if (reason == Event.AbortReason.VIOLATION) {
                LOGGER.log(Level.FINE, "SMP cheated: {0}", new Object[]{sessionID});
            } else {
                throw new IllegalArgumentException("Invalid abort reason.");
            }
        } else if (event == Event.MESSAGE_FOR_ANOTHER_INSTANCE_RECEIVED) {
            LOGGER.log(Level.FINE, "messageFromAnotherInstanceReceived: {0}", new Object[]{sessionID});
        } else if (event == Event.MULTIPLE_INSTANCES_DETECTED) {
            LOGGER.log(Level.FINE, "multipleInstancesDetected: {0}", new Object[]{sessionID});
        } else if (event == Event.EXTRA_SYMMETRIC_KEY_DISCOVERED) {
            final Event.ExtraSymmetricKey extraSymmetricKey = Event.EXTRA_SYMMETRIC_KEY_DISCOVERED.convert(payload);
            LOGGER.log(Level.FINE, "extraSymmetricKeyDiscovered: {0}: {1}", new Object[]{sessionID, extraSymmetricKey});
        } else if (event == Event.SMP_COMPLETED) {
            final Event.SMPResult result = Event.SMP_COMPLETED.convert(payload);
            if (result.success) {
                LOGGER.log(Level.INFO, "Fingerprint {0} verified for session {1}", new Object[]{result.fingerprint, sessionID});
            } else {
                LOGGER.log(Level.INFO, "Fingerprint {0} verification REMOVED for session {1}", new Object[]{result.fingerprint, sessionID});
            }
        }
    }

    final static class Action<T> {
        public final SessionID sessionID;
        public final InstanceTag tag;
        public final Event<T> event;
        public final T payload;

        public Action(final SessionID sessionID, final InstanceTag tag, final Event<T> event, final T payload) {
            this.sessionID = requireNonNull(sessionID);
            this.tag = requireNonNull(tag);
            this.event = requireNonNull(event);
            this.payload = requireNonNull(payload);
        }
    }
}
