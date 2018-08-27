package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrEngineHost;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.crypto.EdDSAKeyPair;
import net.java.otr4j.io.messages.ClientProfilePayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Objects.requireNonNull;
import static net.java.otr4j.session.OtrSessionManager.createSession;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoSession.generateSessionID;

/**
 * EchoServer.
 * <p>
 * Format: 4-byte message length in bytes, followed by message bytes.
 */
public final class EchoServer {

    private static final Logger LOGGER = Logger.getLogger(EchoServer.class.getName());

    private EchoServer() {
        // No need to instantiate.
    }

    /**
     * Main function for starting the EchoServer.
     *
     * @param args no program parameters defined
     * @throws IOException In case of failure to start the server instance.
     */
    public static void main(@Nonnull final String[] args) throws IOException {
        final SecureRandom random = new SecureRandom();
        final InstanceTag tag = InstanceTag.random(random);
        final ServerSocket server = new ServerSocket(DEFAULT_PORT);
        LOGGER.info("Server started on port " + server.getLocalPort());
        while (!server.isClosed()) {
            try (Socket connection = server.accept()) {
                final InputStream in = connection.getInputStream();
                final OutputStream out = connection.getOutputStream();
                final SessionID sessionID = generateSessionID(connection);
                final Host host = new Host(out, tag);
                final Session session = createSession(sessionID, host);
                LOGGER.log(Level.INFO, "Server session ID: " + sessionID);
                while (!connection.isClosed()) {
                    final String message = readMessage(in);
                    if (message == null) {
                        // end of communication
                        break;
                    }
                    final String plaintext = session.transformReceiving(message);
                    if (plaintext == null) {
                        // handled internally by otr4j
                        continue;
                    }
                    final String[] parts = session.transformSending(plaintext);
                    for (final String part : parts) {
                        writeMessage(out, part);
                    }
                }
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failure in client connection.", e);
                break;
            } catch (final OtrException e) {
                LOGGER.log(Level.WARNING, "OTR exception.", e);
            }
        }
        LOGGER.info("Server shut down.");
    }
}

final class Host implements OtrEngineHost {

    private final OutputStream out;
    private final InstanceTag tag;

    Host(@Nonnull final OutputStream out, @Nonnull final InstanceTag tag) {
        this.out = requireNonNull(out);
        this.tag = requireNonNull(tag);
    }

    @Override
    public void injectMessage(@Nonnull final SessionID sessionID, @Nonnull final String msg) {
        try {
            writeMessage(this.out, msg);
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
        return null;
    }

    @Nonnull
    @Override
    public EdDSAKeyPair getLongTermKeyPair(@Nonnull final SessionID sessionID) {
        return null;
    }

    @Nonnull
    @Override
    public ClientProfilePayload getClientProfile(@Nonnull final SessionID sessionID) {
        return null;
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
        return new byte[0];
    }

    @Override
    public void smpError(@Nonnull final SessionID sessionID, final int tlvType, final boolean cheated) {

    }

    @Override
    public void smpAborted(@Nonnull final SessionID sessionID) {

    }

    @Override
    public void verify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint) {

    }

    @Override
    public void unverify(@Nonnull final SessionID sessionID, @Nonnull final String fingerprint) {

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