package nl.dannyvanheumen.echonetwork.client.otr4j;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.session.OtrSessionManager;
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;
import static nl.dannyvanheumen.echonetwork.util.LogManagers.readResourceConfig;

/**
 * EchoClient.
 */
public final class StdinClient {

    static {
        readResourceConfig("/logging.properties");
    }

    private static final Logger LOGGER = Logger.getLogger(StdinClient.class.getName());

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
            final OtrSessionManager manager = new OtrSessionManager(host);
            new Thread(() -> {
                Message m;
                try {
                    while (true) {
                        m = receiveMessage(in);
                        final SessionID sessionID = new SessionID(localID, m.address, "echo");
                        final Session session = manager.getSession(sessionID);
                        try {
                            final Session.Result message = session.transformReceiving(m.content);
                            LOGGER.log(INFO, "Received ({0}, {1}): {2}", new Object[]{message.tag, message.status, message.content});
                        } catch (final OtrException e) {
                            LOGGER.log(WARNING, "Failed to process message.", e);
                        }
                    }
                } catch (final IOException e) {
                    LOGGER.log(WARNING, "Error reading from input: {0}", e.getMessage());
                }
            }, "StdinClient:" + localID).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    final Message message = parseLine(reader.readLine());
                    final SessionID sessionID = new SessionID(localID, message.address, "echo");
                    final Session session = manager.getSession(sessionID);
                    final String[] parts = session.transformSending(message.content);
                    sendMessage(out, message.address, parts);
                }
            }
        }
    }

    @Nonnull
    private static Message parseLine(@Nonnull final String line) {
        final int sepindex = line.indexOf(' ');
        if (sepindex < 0) {
            throw new IllegalArgumentException("Invalid message line.");
        }
        return new Message(line.substring(0, sepindex), line.substring(sepindex + 1));
    }
}
