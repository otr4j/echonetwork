package nl.dannyvanheumen.otr4jechoserver;

import nl.dannyvanheumen.otr4jechoserver.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.generateRemoteID;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

/**
 * EchoServer.
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
        final ServerSocket server = new ServerSocket(DEFAULT_PORT);
        LOGGER.info("Server started on " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
        while (!server.isClosed()) {
            final Socket connection = server.accept();
            final String connectionID = generateRemoteID(connection);
            new Handler(connectionID, connection).start();
        }
        LOGGER.info("Server shut down.");
    }

    private static final class Handler extends Thread {

        private static final ConcurrentHashMap<String, OutputStream> CONNECTIONS = new ConcurrentHashMap<>();

        private final String id;
        private final Socket connection;

        private Handler(@Nonnull final String id, @Nonnull final Socket connection) throws IOException {
            this.id = requireNonNull(id);
            this.connection = requireNonNull(connection);
            CONNECTIONS.put(this.id, this.connection.getOutputStream());
        }

        @Override
        public void run() {
            LOGGER.log(Level.INFO, "Session {0} started.", this.id);
            try (this.connection) {
                final InputStream in = this.connection.getInputStream();
                while (!this.connection.isClosed()) {
                    final Message message = readMessage(in);
                    if (message == null) {
                        // end of communication
                        break;
                    }
                    final OutputStream destination = CONNECTIONS.get(message.address);
                    if (destination == null) {
                        LOGGER.log(Level.INFO, "Dropping message because destination is not available.");
                        continue;
                    }
                    writeMessage(destination, generateRemoteID(this.connection), message.content);
                }
                LOGGER.log(Level.INFO, "Session {0} finished.", this.id);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failure in client connection.", e);
            } finally {
                CONNECTIONS.remove(this.id);
            }
        }
    }
}