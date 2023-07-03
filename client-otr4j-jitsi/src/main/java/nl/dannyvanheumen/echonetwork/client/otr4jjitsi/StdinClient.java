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
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol;
import utils.java.lang.Strings;
import utils.java.util.logging.LogManagers;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * EchoClient.
 */
public final class StdinClient {

    static {
        LogManagers.readResourceConfig("/logging.properties");
    }

    private static final Logger LOGGER = Logger.getLogger(StdinClient.class.getName());

    private StdinClient() {
        // No need to instantiate.
    }

    /**
     * Main function for starting the client.
     *
     * @param args no program parameters defined
     * @throws IOException  In case of failure to establish client connection.
     * @throws OtrException In case of OTR-based exceptions.
     */
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AssignmentInOperand"})
    public static void main(@Nonnull final String[] args) throws IOException, OtrException {
        try (Socket client = new Socket(InetAddress.getLocalHost(), EchoProtocol.DEFAULT_PORT);
                OutputStream out = client.getOutputStream();
                InputStream in = client.getInputStream()) {
            final String localID = EchoProtocol.generateLocalID(client);
            final Host host = new Host(out, new OtrPolicyImpl(OtrPolicy.OTRL_POLICY_MANUAL));
            final OtrSessionManager manager = new OtrSessionManagerImpl(host);
            new Thread(() -> {
                EchoProtocol.Message m;
                try {
                    while (true) {
                        m = EchoProtocol.receiveMessage(in);
                        final SessionID sessionID = new SessionID(localID, m.address, "echo");
                        final Session session = manager.getSession(sessionID);
                        try {
                            final String message = session.transformReceiving(m.content);
                            LOGGER.log(INFO, "Received: {0}", new Object[]{message});
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
                    EchoProtocol.sendMessage(out, message.address, parts);
                }
            }
        }
    }

    @Nonnull
    private static Message parseLine(@Nonnull final String line) {
        final String[] parts = Strings.cut(line, ' ');
        if (parts[1] == null) {
            throw new IllegalArgumentException("Invalid message line.");
        }
        final String[] addr = Strings.cut(parts[0], '#');
        if (addr[1] == null) {
            return new Message(parts[0], 0, parts[1]);
        } else {
            return new Message(addr[0], Integer.parseInt(addr[1]), parts[1]);
        }
    }

    private static class Message {
        private final String address;
        private final int tag;
        private final String content;
        
        private Message(@Nonnull final String address, final int tag, @Nonnull final String content) {
            this.address = requireNonNull(address);
            this.tag = tag;
            this.content = requireNonNull(content);
        }
    }
}
