package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            } catch (final OtrException e) {
                LOGGER.log(Level.WARNING, "OTR exception.", e);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failure in client connection.", e);
                break;
            }
        }
        LOGGER.info("Server shut down.");
    }
}