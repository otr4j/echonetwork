package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

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
        final ServerSocket server = new ServerSocket(8080);
        LOGGER.info("Server started on port " + server.getLocalPort());
        while (!server.isClosed()) {
            try (Socket connection = server.accept()) {
                while (!connection.isClosed()) {
                    final InputStream in = connection.getInputStream();
                    final OutputStream out = connection.getOutputStream();
                    final byte[] message = readMessage(in);
                    if (message == null) {
                        break;
                    }
                    writeMessage(out, message);
                }
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failure in client connection.", e);
            }
        }
        LOGGER.info("Server shut down.");
    }
}
