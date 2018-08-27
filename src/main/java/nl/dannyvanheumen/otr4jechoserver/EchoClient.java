package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.session.OtrSessionManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.logging.Logger;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoSession.generateSessionID;

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
    public static void main(@Nonnull final String[] args) throws IOException, InterruptedException {
        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT)) {
            final OutputStream out = client.getOutputStream();
            final InputStream in = client.getInputStream();
            final SessionID sessionID = generateSessionID(client);
            final Host host = new Host(out, tag);
            final Session session = OtrSessionManager.createSession(sessionID, host);
            LOGGER.info("Sending ...");
            writeMessage(out, "Hello world!");
            processReplies(in);
            writeMessage(client.getOutputStream(), "?OTRv3?");
            Thread.sleep(10000L);
            processReplies(in);
        }
    }

    private static void processReplies(@Nonnull final InputStream in) throws IOException {
        while (true) {
            if (in.available() == 0) {
                break;
            }
            final String message = readMessage(in);
            if (message == null) {
                break;
            }
            LOGGER.info("Received: " + message);
        }
    }
}
