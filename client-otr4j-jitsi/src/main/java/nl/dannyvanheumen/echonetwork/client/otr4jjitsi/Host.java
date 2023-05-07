package nl.dannyvanheumen.echonetwork.client.otr4jjitsi;


import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.SessionID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;

final class Host implements OtrEngineHost {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());
    
    private final KeyPair keypair;

    private final OtrPolicy policy;

    private final OutputStream out;

    Host(@Nonnull final OutputStream out, @Nonnull final OtrPolicy policy) {
        this.keypair = new OtrCryptoEngineImpl().generateDSAKeyPair();
        this.out = requireNonNull(out);
        this.policy = requireNonNull(policy);
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
    public FragmenterInstructions getFragmenterInstructions(final SessionID sessionID) {
        return new FragmenterInstructions(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public KeyPair getLocalKeyPair(final SessionID sessionID) throws OtrException {
        return this.keypair;
    }

    @Override
    public void askForSecret(@Nonnull final SessionID sessionID, @Nonnull final InstanceTag receiverTag, @Nullable final String question) {
        LOGGER.log(Level.FINE, "askForSecret: {0}:{1}: {2}", new Object[]{sessionID, receiverTag, question});
    }

    @Nonnull
    @Override
    public byte[] getLocalFingerprintRaw(@Nonnull final SessionID sessionID) {
        try {
            return new OtrCryptoEngineImpl().getFingerprintRaw(this.getLocalKeyPair(sessionID).getPublic());
        } catch (final OtrException e) {
            throw new IllegalStateException("Failed to acquire local keypair", e);
        }
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
    public void verify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint, final boolean approve) {
        LOGGER.log(Level.INFO, "Fingerprint {0} verified for session {1}", new Object[] {fingerprint, sessionID});
    }

    @Override
    public void unverify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint) {
        LOGGER.log(Level.INFO, "Fingerprint {0} verification REMOVED for session {1}", new Object[] {
                fingerprint, sessionID});
    }

    @Override
    public String getReplyForUnreadableMessage(final SessionID sessionID) {
        return "This message is unreadable.";
    }

    @Override
    public String getFallbackMessage(@Nonnull final SessionID sessionID) {
        return "OTR is not supported. Please download the plugin.";
    }

    @Override
    public void messageFromAnotherInstanceReceived(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "messageFromAnotherInstanceReceived: {0}", new Object[]{sessionID});
    }

    @Override
    public void multipleInstancesDetected(@Nonnull final SessionID sessionID) {
        LOGGER.log(Level.FINE, "multipleInstancesDetected: {0}", new Object[]{sessionID});
    }
}
