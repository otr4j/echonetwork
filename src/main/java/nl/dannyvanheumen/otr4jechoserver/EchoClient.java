package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

/**
 * EchoClient.
 * <p>
 * Format: 4-byte message length in bytes, followed by message bytes.
 */
public final class EchoClient {

    private static final Logger LOGGER = Logger.getLogger(EchoClient.class.getName());

    private EchoClient() {
        // No need to instantiate.
    }

    /**
     * Main function for starting the client.
     *
     * @param args no program parameters defined
     * @throws IOException In case of failure to establish client connection.
     */
    public static void main(@Nonnull final String[] args) throws IOException {
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT)) {
            LOGGER.info("Sending ...");
            writeMessage(client.getOutputStream(), "Hello world!");
            final String message = readMessage(client.getInputStream());
            if (message != null) {
                LOGGER.info("Received: " + message);
            }
            writeMessage(client.getOutputStream(), "?OTRv3?");
            final String message2 = readMessage(client.getInputStream());
            if (message2 != null) {
                LOGGER.info("Received: " + message2);
            }
        }
    }
}
