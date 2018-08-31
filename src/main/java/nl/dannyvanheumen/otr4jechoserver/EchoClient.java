package nl.dannyvanheumen.otr4jechoserver;

import nl.dannyvanheumen.otr4jechoserver.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

/**
 * EchoClient.
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
//        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT)) {
            LOGGER.log(Level.INFO, "Client started on address {0}:{1}",
                    new Object[]{client.getLocalAddress().getHostAddress(), client.getLocalPort()});
            final OutputStream out = client.getOutputStream();
            final InputStream in = client.getInputStream();
//            final SessionID sessionID = generateSessionID(client);
//            final Host host = new Host(out, tag);
//            final Session session = createSession(sessionID, host);
            Message message;
            while ((message = readMessage(in)) != null) {
                LOGGER.log(Level.INFO, "Echoing: {0}", message.content);
                writeMessage(out, message);
            }
        }
    }
}
