package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.ClientProfile;
import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrEngineHost;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session.OTRv;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.crypto.EdDSAKeyPair;
import net.java.otr4j.crypto.OtrCryptoEngine;
import net.java.otr4j.io.messages.ClientProfilePayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.util.Calendar;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.requireNonNull;
import static net.java.otr4j.crypto.OtrCryptoEngine.getFingerprintRaw;
import static net.java.otr4j.io.messages.ClientProfilePayload.sign;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

final class Host implements OtrEngineHost {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());

    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeyPair dsaKeyPair = OtrCryptoEngine.generateDSAKeyPair();

    private final EdDSAKeyPair edDSAKeyPair = EdDSAKeyPair.generate(RANDOM);

    private final OutputStream out;
    private final InstanceTag tag;
    private final ClientProfilePayload profilePayload;

    Host(@Nonnull final OutputStream out, @Nonnull final InstanceTag tag) {
        this.out = requireNonNull(out);
        this.tag = requireNonNull(tag);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, 7);
        final ClientProfile profile = new ClientProfile(tag, this.edDSAKeyPair.getPublicKey(),
                Set.of(OTRv.THREE, OTRv.FOUR), calendar.getTimeInMillis() / 1000,
                (DSAPublicKey) this.dsaKeyPair.getPublic());
        this.profilePayload = sign(profile, (DSAPrivateKey) this.dsaKeyPair.getPrivate(), this.edDSAKeyPair);
    }

    @Override
    public void injectMessage(@Nonnull final SessionID sessionID, @Nonnull final String msg) {
        try {
            writeMessage(this.out, sessionID.getAccountID(), msg);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inject message in network.", e);
        }
    }

    @Override
    public void unreadableMessageReceived(@Nonnull final SessionID sessionID) {

    }

    @Override
    public void unencryptedMessageReceived(@Nonnull final SessionID sessionID, @Nonnull final String msg) {

    }

    @Override
    public void showError(@Nonnull final SessionID sessionID, @Nonnull final String error) {
        LOGGER.log(Level.SEVERE, "Client/OTR {1}: {0}", new Object[] {error, sessionID});
    }

    @Override
    public void finishedSessionMessage(@Nonnull final SessionID sessionID, @Nonnull final String msgText) {

    }

    @Override
    public void requireEncryptedMessage(@Nonnull final SessionID sessionID, @Nonnull final String msgText) {

    }

    @Override
    public OtrPolicy getSessionPolicy(@Nonnull final SessionID sessionID) {
        return new OtrPolicy(OtrPolicy.OPPORTUNISTIC);
    }

    @Override
    public int getMaxFragmentSize(@Nonnull final SessionID sessionID) {
        return MAX_VALUE;
    }

    @Nonnull
    @Override
    public KeyPair getLocalKeyPair(@Nonnull final SessionID sessionID) {
        return this.dsaKeyPair;
    }

    @Nonnull
    @Override
    public EdDSAKeyPair getLongTermKeyPair(@Nonnull final SessionID sessionID) {
        return this.edDSAKeyPair;
    }

    @Nonnull
    @Override
    public ClientProfilePayload getClientProfile(@Nonnull final SessionID sessionID) {
        return this.profilePayload;
    }

    @Nonnull
    @Override
    public InstanceTag getInstanceTag(@Nonnull final SessionID sessionID) {
        return this.tag;
    }

    @Override
    public void askForSecret(@Nonnull final SessionID sessionID, @Nonnull final InstanceTag receiverTag, @Nullable final String question) {

    }

    @Nonnull
    @Override
    public byte[] getLocalFingerprintRaw(@Nonnull final SessionID sessionID) {
        return getFingerprintRaw((DSAPublicKey) this.dsaKeyPair.getPublic());
    }

    @Override
    public void smpError(@Nonnull final SessionID sessionID, final int tlvType, final boolean cheated) {

    }

    @Override
    public void smpAborted(@Nonnull final SessionID sessionID) {

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
    public String getReplyForUnreadableMessage(@Nonnull final SessionID sessionID) {
        return null;
    }

    @Override
    public String getFallbackMessage(@Nonnull final SessionID sessionID) {
        return null;
    }

    @Override
    public void messageFromAnotherInstanceReceived(@Nonnull final SessionID sessionID) {

    }

    @Override
    public void multipleInstancesDetected(@Nonnull final SessionID sessionID) {

    }

    @Override
    public void extraSymmetricKeyDiscovered(@Nonnull final SessionID sessionID, @Nonnull final String message, @Nonnull final byte[] extraSymmetricKey, @Nonnull final byte[] tlvData) {

    }
}