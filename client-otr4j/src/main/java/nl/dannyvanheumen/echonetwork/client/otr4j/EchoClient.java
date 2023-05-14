package nl.dannyvanheumen.echonetwork.client.otr4j;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.session.OtrSessionManager;
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.Message;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;
import static nl.dannyvanheumen.echonetwork.util.LogManagers.readResourceConfig;

/**
 * EchoClient.
 */
public final class EchoClient {

    static {
        readResourceConfig("/logging.properties");
    }

    private static final Logger LOGGER = Logger.getLogger(EchoClient.class.getName());

    private static final SecureRandom RANDOM = new SecureRandom();

    private EchoClient() {
        // No need to instantiate.
    }

    /**
     * Main function for starting the client.
     *
     * @param args no program parameters defined
     * @throws IOException In case of failure to establish client connection.
     */
    @SuppressWarnings({"PMD.AssignmentInOperand", "InfiniteLoopStatement"})
    public static void main(@Nonnull final String[] args) throws IOException {
        final InstanceTag tag = InstanceTag.random(RANDOM);
        try (Socket connection = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
                InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            LOGGER.log(Level.INFO, "Client started on address {0}:{1}",
                    new Object[]{connection.getLocalAddress().getHostAddress(), connection.getLocalPort()});
            final Host host = new Host(out, tag, new OtrPolicy(OtrPolicy.REACTIVE));
            final OtrSessionManager manager = new OtrSessionManager(host);
            final String localID = generateLocalID(connection);
            LOGGER.log(Level.INFO, "Local ID: {0}", new Object[]{localID});
            Message raw;
            while (true) {
                LOGGER.log(Level.FINE, "Waiting to receive next message from connection…");
                raw = receiveMessage(in);
                try {
                    final SessionID sessionID = new SessionID(localID, raw.address, "echo");
                    final Session session = manager.getSession(sessionID);
                    final Session.Result message = session.transformReceiving(raw.content);
                    if (message.content == null) {
                        continue;
                    }
                    LOGGER.log(Level.INFO, "Echoing: ({0}, {1}) {2}",
                            new Object[]{message.tag, message.status, message.content});
                    final String[] parts = session.transformSending(message.content);
                    sendMessage(out, raw.address, parts);
                } catch (final OtrException e) {
                    LOGGER.log(Level.INFO, "Failed to process content.", e);
                }
            }
        }
    }
}
