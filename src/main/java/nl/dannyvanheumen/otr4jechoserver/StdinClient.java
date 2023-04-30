package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import nl.dannyvanheumen.otr4jechoserver.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.java.otr4j.session.OtrSessionManager.createSession;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.sendMessage;

/**
 * EchoClient.
 */
public final class StdinClient {

    private static final Logger LOGGER = Logger.getLogger(EchoClient.class.getName());

    private static final Map<SessionID, Session> SESSIONS = Collections.synchronizedMap(new HashMap<>());

    private StdinClient() {
        // No need to instantiate.
    }

    /**
     * Main function for starting the client.
     *
     * @param args no program parameters defined
     * @throws IOException In case of failure to establish client connection.
     * @throws OtrException In case of OTR-based exceptions.
     */
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AssignmentInOperand"})
    public static void main(@Nonnull final String[] args) throws IOException, OtrException {
        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
             OutputStream out = client.getOutputStream();
             InputStream in = client.getInputStream()) {
            final String localID = generateLocalID(client);
            final Host host = new Host(out, tag, new OtrPolicy(OtrPolicy.OTRL_POLICY_MANUAL));
            new Thread(() -> {
                Message m;
                try {
                    while (true) {
                        m = receiveMessage(in);
                        final SessionID sessionID = new SessionID(localID, m.address, "echo");
                        final Session session = SESSIONS.computeIfAbsent(sessionID, id -> createSession(id, host));
                        try {
                            final Session.Msg message = session.transformReceiving(m.content);
                            LOGGER.log(Level.INFO, "Received: {0}", message.content);
                        } catch (final OtrException e) {
                            LOGGER.log(Level.WARNING, "Failed to process message.", e);
                        }
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException("Failed to read from connection.", e);
                }
            }, "StdinClient:" + localID).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    final String address = reader.readLine();
                    if ("".equals(address)) {
                        break;
                    }
                    final String content = reader.readLine();
                    final SessionID sessionID = new SessionID(localID, address, "echo");
                    final Session session = SESSIONS.computeIfAbsent(sessionID, id -> createSession(id, host));
                    final String[] parts = session.transformSending(content);
                    sendMessage(out, address, parts);
                }
            }
        }
    }
}
