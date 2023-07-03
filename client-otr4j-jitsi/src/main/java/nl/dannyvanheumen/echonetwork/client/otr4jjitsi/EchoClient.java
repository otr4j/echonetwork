/*
 * client-otr4j-jitsi, the echonetwork client for Jitsi's original version of otr4j.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.client.otr4jjitsi;

import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.OtrSessionManager;
import net.java.otr4j.OtrSessionManagerImpl;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.Message;
import utils.java.util.logging.LogManagers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;

/**
 * EchoClient.
 */
public final class EchoClient {

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
        Logger.getLogger("").setLevel(Level.FINEST);
        try (Socket connection = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
                InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            LOGGER.log(Level.INFO, "Client started on address {0}:{1}",
                    new Object[]{connection.getLocalAddress().getHostAddress(), connection.getLocalPort()});
            final Host host = new Host(out, new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3 | OtrPolicy.ERROR_START_AKE | OtrPolicy.WHITESPACE_START_AKE));
            final OtrSessionManager manager = new OtrSessionManagerImpl(host);
            final String localID = generateLocalID(connection);
            LOGGER.log(Level.INFO, "Local ID: {0}", new Object[]{localID});
            Message raw;
            while (true) {
                LOGGER.log(Level.FINE, "Waiting to receive next message from connection…");
                raw = receiveMessage(in);
                try {
                    final SessionID sessionID = new SessionID(localID, raw.address, "echo");
                    final Session session = manager.getSession(sessionID);
                    final String message = session.transformReceiving(raw.content);
                    if (message == null) {
                        continue;
                    }
                    LOGGER.log(Level.INFO, "Echoing: {0}", new Object[]{message});
                    final String[] parts = session.transformSending(message);
                    sendMessage(out, raw.address, parts);
                } catch (final OtrException e) {
                    LOGGER.log(Level.INFO, "Failed to process content.", e);
                }
            }
        }
    }
}
