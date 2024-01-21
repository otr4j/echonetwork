/*
 * server, the echonetwork (relay) server.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.server;

import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.Message;
import nl.dannyvanheumen.echonetwork.utils.LogManagers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.generateRemoteID;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;

/**
 * EchoServer.
 */
public final class EchoServer {

    static {
        LogManagers.readResourceConfig("/logging.properties");
    }

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
        LOGGER.log(Level.FINE, "Loglevel 'FINE' is being processed.");
        final Map<String, OutputStream> clients = Collections.synchronizedMap(new HashMap<>());
        try (ServerSocket server = new ServerSocket(DEFAULT_PORT)) {
            LOGGER.log(Level.INFO, "Server started on {0}:{1}",
                    new Object[]{server.getInetAddress().getHostAddress(), server.getLocalPort()});
            while (!server.isClosed()) {
                final Socket connection = server.accept();
                final String connectionID = generateRemoteID(connection);
                clients.put(connectionID, connection.getOutputStream());
                new Handler(clients, connectionID, connection).start();
            }
        }
        LOGGER.info("Server shut down.");
    }

    @SuppressWarnings({"PMD.DoNotUseThreads", "resource"})
    private static final class Handler extends Thread {

        private final Map<String, OutputStream> clients;
        private final String id;
        private final Socket connection;

        private Handler(@Nonnull final Map<String, OutputStream> clients, @Nonnull final String id, @Nonnull final Socket connection) {
            super();
            this.clients = requireNonNull(clients);
            this.id = requireNonNull(id);
            this.connection = requireNonNull(connection);
        }

        @Override
        public void run() {
            LOGGER.log(Level.INFO, "Session {0} registered.", this.id);
            try (this.connection; InputStream in = this.connection.getInputStream()) {
                while (!this.connection.isClosed()) {
                    final Message message = receiveMessage(in);
                    final OutputStream destination = this.clients.get(message.address);
                    if (destination == null) {
                        LOGGER.log(Level.INFO, "Dropping message because destination is not available.");
                        continue;
                    }
                    LOGGER.log(Level.FINE, "Relaying {0} => {1}: {2}",
                            new Object[]{this.id, message.address, message.content});
                    sendMessage(destination, generateRemoteID(this.connection), message.content);
                }
                LOGGER.log(Level.INFO, "Session {0} finished.", this.id);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failure in client connection: {0}", new Object[]{e.getMessage()});
            } finally {
                this.clients.remove(this.id);
            }
        }
    }
}
