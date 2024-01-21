/*
 * client-otr4j, the echonetwork client for otr4j.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.client.otr4j;

import net.java.otr4j.api.Event;
import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.session.OtrSessionManager;
import nl.dannyvanheumen.echonetwork.protocol.Client;
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.Message;
import nl.dannyvanheumen.echonetwork.utils.LogManagers;

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

/**
 * EchoClient.
 */
public final class EchoClient implements Client {

    static {
        LogManagers.readResourceConfig("/logging.properties");
    }

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
    @SuppressWarnings({"PMD.AssignmentInOperand", "InfiniteLoopStatement"})
    public static void main(@Nonnull final String[] args) throws IOException {
        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket connection = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
             InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            LOGGER.log(Level.INFO, "Client started on address {0}:{1}",
                new Object[]{connection.getLocalAddress().getHostAddress(), connection.getLocalPort()});
            final String localID = generateLocalID(connection);
            Thread.currentThread().setName("EchoClient:" + localID);
            final Host host = new Host(out, tag, new OtrPolicy(OtrPolicy.REACTIVE));
            final OtrSessionManager manager = new OtrSessionManager(host);
            LOGGER.log(Level.INFO, "Local ID: {0}", new Object[]{localID});
            while (true) {
                processActions(host, manager);
                LOGGER.log(Level.FINE, "Waiting to receive next message from connection…");
                final Message raw = receiveMessage(in);
                final Session session = manager.getSession(new SessionID(localID, raw.address, DEFAULT_PROTOCOL_NAME));
                processMessage(raw, out, session);
            }
        }
    }

    private static void processMessage(final Message raw, final OutputStream out, final Session session)
        throws IOException {
        try {
            final Session.Result message = session.transformReceiving(raw.content);
            if (message.content == null) {
                return;
            }
            LOGGER.log(Level.INFO, "Echoing: ({0}, {1}) {2}",
                new Object[]{message.tag, message.status, message.content});
            final String[] parts = session.transformSending(message.content);
            sendMessage(out, raw.address, parts);
        } catch (final OtrException e) {
            LOGGER.log(Level.INFO, "Failed to process content.", e);
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static void processActions(final Host host, final OtrSessionManager manager) {
        for (Host.Action<?> action = host.actions.poll(); action != null; action = host.actions.poll()) {
            LOGGER.log(Level.FINE, "Handling actions queue for event follow-up…");
            if (action.event == Event.SMP_REQUEST_SECRET) {
                final String question = Event.SMP_REQUEST_SECRET.convert(action.payload);
                try {
                    manager.getSession(action.sessionID).respondSmp(question, DEFAULT_SMP_SECRET);
                } catch (OtrException e) {
                    LOGGER.log(Level.WARNING, "Failed to handle SMP Request Secret event.");
                }
            } else {
                throw new UnsupportedOperationException("Unsupported event type for handling by echo client.");
            }
        }
    }
}
