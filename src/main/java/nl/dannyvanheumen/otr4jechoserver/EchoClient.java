package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import nl.dannyvanheumen.otr4jechoserver.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.java.otr4j.session.OtrSessionManager.createSession;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoSession.generateSessionID;

/**
 * EchoClient.
 */
public final class EchoClient {

    private static final Logger LOGGER = Logger.getLogger(EchoClient.class.getName());

    private static final HashMap<SessionID, Session> SESSIONS = new HashMap<>();

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
        Logger.getLogger("").setLevel(Level.FINEST);
        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT)) {
            LOGGER.log(Level.INFO, "Client started on address {0}:{1}",
                    new Object[] {client.getLocalAddress().getHostAddress(), client.getLocalPort()});
            final OutputStream out = client.getOutputStream();
            final Host host = new Host(out, tag);
            Message raw;
            while ((raw = readMessage(client.getInputStream())) != null) {
                try {
                    final SessionID sessionID = new SessionID(generateLocalID(client), raw.address, "echo");
                    Session session = SESSIONS.get(sessionID);
                    if (session == null) {
                        session = createSession(sessionID, host);
                        SESSIONS.put(sessionID, session);
                    }
                    final String message = session.transformReceiving(raw.content);
                    if (message == null) {
                        continue;
                    }
                    LOGGER.log(Level.INFO, "Echoing: {0}", message);
                    final String[] parts = session.transformSending(message);
                    for (final String part : parts) {
                        writeMessage(out, raw.address, part);
                    }
                } catch (final OtrException e) {
                    LOGGER.log(Level.INFO, "Failed to process content.", e);
                }
            }
        }
    }
}
