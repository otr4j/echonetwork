package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.ClientProfile;
import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrEngineHost;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session.Version;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.crypto.DSAKeyPair;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.crypto.ed448.EdDSAKeyPair;
import net.java.otr4j.io.OtrInputStream;
import net.java.otr4j.io.OtrOutputStream;
import net.java.otr4j.messages.ClientProfilePayload;
import net.java.otr4j.messages.ValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.requireNonNull;
import static net.java.otr4j.session.smp.DSAPublicKeys.fingerprint;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.sendMessage;

final class Host implements OtrEngineHost {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());

    private static final SecureRandom RANDOM = new SecureRandom();
    private final DSAKeyPair dsaKeyPair = DSAKeyPair.generateDSAKeyPair(RANDOM);
    private final EdDSAKeyPair edDSAKeyPair = EdDSAKeyPair.generate(RANDOM);

    private final OtrPolicy policy;

    private final OutputStream out;
    private ClientProfile profile;

    Host(@Nonnull final OutputStream out, @Nonnull final InstanceTag tag, @Nonnull final OtrPolicy policy) {
        this.out = requireNonNull(out);
        this.policy = requireNonNull(policy);
        final EdDSAKeyPair forging = EdDSAKeyPair.generate(RANDOM);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, 7);
        this.profile = new ClientProfile(tag,
                this.edDSAKeyPair.getPublicKey(),
                forging.getPublicKey(),
                List.of(Version.THREE, Version.FOUR),
                this.dsaKeyPair.getPublic());
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
    public void unreadableMessageReceived(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "unreadableMessageReceived: {0}", new Object[]{sessionID});
    }

    @Override
    public void unencryptedMessageReceived(@Nonnull final SessionID sessionID, @Nonnull final String msg) {
        LOGGER.log(Level.FINE, "unencryptedMessageReceived: {0}: {1}", new Object[]{sessionID, msg});
    }

    @Override
    public void showError(@Nonnull final SessionID sessionID, @Nonnull final String error) {
        LOGGER.log(Level.SEVERE, "Client/OTR {1}: {0}", new Object[] {error, sessionID});
    }

    @Override
    public void finishedSessionMessage(@Nonnull final SessionID sessionID, @Nonnull final String msgText) {
        LOGGER.log(Level.FINE, "finishedSessionMessage: {0}: {1}", new Object[]{sessionID, msgText});
    }

    @Override
    public void requireEncryptedMessage(@Nonnull final SessionID sessionID, @Nonnull final String msgText) {
        LOGGER.log(Level.FINE, "requireEncryptedMessage: {0}: {1}", new Object[]{sessionID, msgText});
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
    public ClientProfile getClientProfile(@Nonnull final SessionID sessionID) {
        return this.profile;
    }

    @Override
    public void askForSecret(@Nonnull final SessionID sessionID, @Nonnull final InstanceTag receiverTag, @Nullable final String question) {
        LOGGER.log(Level.FINE, "askForSecret: {0}:{1}: {2}", new Object[]{sessionID, receiverTag, question});
    }

    @Nonnull
    @Override
    public byte[] getLocalFingerprintRaw(@Nonnull final SessionID sessionID) {
        return fingerprint(this.dsaKeyPair.getPublic());
    }

    @Override
    public void smpError(@Nonnull final SessionID sessionID, final int tlvType, final boolean cheated) {
        LOGGER.log(Level.FINE, "smpError: {0}:{1} ({2})", new Object[]{sessionID, tlvType, cheated});
    }

    @Override
    public void smpAborted(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "smpAborted: {0}", new Object[]{sessionID});
    }

    @Override
    public void verify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint) {
        LOGGER.log(Level.INFO, "Fingerprint {0} verified for session {1}", new Object[] {fingerprint, sessionID});
    }

    @Override
    public void unverify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint) {
        LOGGER.log(Level.INFO, "Fingerprint {0} verification REMOVED for session {1}", new Object[] {
                fingerprint, sessionID});
    }

    @Override
    public String getReplyForUnreadableMessage(@Nonnull final SessionID sessionID, @Nonnull final String identifier) {
        return null;
    }

    @Override
    public String getFallbackMessage(@Nonnull final SessionID sessionID) {
        return null;
    }

    @Override
    public void messageFromAnotherInstanceReceived(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "messageFromAnotherInstanceReceived: {0}", new Object[]{sessionID});
    }

    @Override
    public void multipleInstancesDetected(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "multipleInstancesDetected: {0}", new Object[]{sessionID});
    }

    @Override
    public void extraSymmetricKeyDiscovered(@Nonnull final SessionID sessionID, @Nonnull final String message, @Nonnull final byte[] extraSymmetricKey, @Nonnull final byte[] tlvData) {
        LOGGER.log(Level.FINE, "extraSymmetricKeyDiscovered: {0}: {1}", new Object[]{sessionID, message});
    }

    @Override
    public void updateClientProfilePayload(@Nonnull final byte[] payload) {
        LOGGER.log(Level.INFO, "Host was requested to update ClientProfile-payload. ({0} bytes)", payload.length);
        final OtrInputStream in = new OtrInputStream(payload);
        try {
            this.profile = ClientProfilePayload.readFrom(in).validate();
        } catch (ValidationException | OtrCryptoException | ProtocolException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public byte[] restoreClientProfilePayload() {
        final Calendar expiration = Calendar.getInstance();
        expiration.add(Calendar.HOUR, 24);
        final ClientProfilePayload payload = ClientProfilePayload.signClientProfile(this.profile,
                expiration.getTimeInMillis() / 1000, this.dsaKeyPair, this.edDSAKeyPair);
        return new OtrOutputStream().write(payload).toByteArray();
    }
}
