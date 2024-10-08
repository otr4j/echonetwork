/*
 * client-otr4j, the echonetwork client for otr4j.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.client.otr4j;

import net.java.otr4j.api.Instance;
import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.Session;
import net.java.otr4j.api.SessionID;
import net.java.otr4j.session.OtrSessionManager;
import nl.dannyvanheumen.echonetwork.protocol.Client;
import nl.dannyvanheumen.echonetwork.protocol.EchoProtocol;
import nl.dannyvanheumen.echonetwork.utils.Strings;
import nl.dannyvanheumen.echonetwork.utils.Threads;
import nl.dannyvanheumen.echonetwork.utils.LogManagers;

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

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.DEFAULT_PORT;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.receiveMessage;
import static nl.dannyvanheumen.echonetwork.protocol.EchoProtocol.sendMessage;

/**
 * EchoClient.
 */
public final class StdinClient implements Client {

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
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AssignmentInOperand", "InfiniteLoopStatement"})
    public static void main(@Nonnull final String[] args) throws IOException, OtrException {
        final InstanceTag tag = InstanceTag.random(new SecureRandom());
        try (Socket client = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
             OutputStream out = client.getOutputStream(); InputStream in = client.getInputStream()) {
            final String localID = generateLocalID(client);
            final Host host = new Host(out, tag, new OtrPolicy(OtrPolicy.OTRL_POLICY_MANUAL));
            final OtrSessionManager manager = new OtrSessionManager(host);
            // Network communications thread.
            Threads.startDaemon("StdinClient:" + localID, () -> {
                try {
                    EchoProtocol.Message m;
                    while (true) {
                        m = receiveMessage(in);
                        final SessionID sessionID = new SessionID(localID, m.address, DEFAULT_PROTOCOL_NAME);
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
            }, Threads.createLoggingHandler(LOGGER));
            // Event loop for processing user input.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    final Message message = parseLine(reader.readLine());
                    final SessionID sessionID = new SessionID(localID, message.address, DEFAULT_PROTOCOL_NAME);
                    final Session session = manager.getSession(sessionID);
                    final Instance instance = session.getInstance(new InstanceTag(message.tag));
                    if (instance == null) {
                        LOGGER.log(INFO, "Non-existant instance tag specified. Ignoring. ({0})", message.tag);
                        continue;
                    }
                    sendMessage(out, message.address, instance.transformSending(message.content));
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
        final String[] addr = Strings.cut(parts[0], TAG_SEPARATOR);
        if (addr[1] == null) {
            return new Message(parts[0], 0, parts[1]);
        } else {
            final int tag = Integer.parseInt(addr[1]);
            return new Message(addr[0], tag, parts[1]);
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
