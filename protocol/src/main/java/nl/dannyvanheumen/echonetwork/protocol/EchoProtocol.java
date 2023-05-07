package nl.dannyvanheumen.echonetwork.protocol;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ProtocolException;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Utilities for reading/writing messages in the length-value format.
 */
public final class EchoProtocol {

    /**
     * DEFAULT_PORT is the constant for the default echonetwork port.
     */
    public static final int DEFAULT_PORT = 8080;

    private EchoProtocol() {
        // No need to instantiate utility class.
    }

    /**
     * Receive a message from the provided inputstream.
     *
     * @param in the inputstream
     * @return Returns the read Message
     * @throws IOException thrown if failing to read message.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Nonnull
    public static Message receiveMessage(@Nonnull final InputStream in) throws IOException {
        synchronized (in) {
            final byte[] address = readValue(in);
            final byte[] message = readValue(in);
            return new Message(new String(address, UTF_8), new String(message, UTF_8));
        }
    }

    /**
     * Send message to the outputstream.
     *
     * @param out     the outputstream
     * @param message the message to be sent.
     * @throws IOException thrown if failing to write message to outputstream.
     */
    public static void sendMessage(@Nonnull final OutputStream out, @Nonnull final Message message) throws IOException {
        sendMessage(out, message.address, message.content);
    }

    /**
     * Send message to the outputstream.
     *
     * @param out      the output stream
     * @param address  the address
     * @param messages the message to be sent
     * @throws IOException throws if failing to write to the output stream
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void sendMessage(@Nonnull final OutputStream out, @Nonnull final String address,
                                   @Nonnull final String... messages) throws IOException {
        synchronized (out) {
            for (final String message : messages) {
                writeValue(out, address.getBytes(UTF_8));
                writeValue(out, message.getBytes(UTF_8));
            }
            out.flush();
        }
    }

    private static byte[] readValue(@Nonnull final InputStream in) throws IOException {
        final byte[] length = new byte[4];
        if (in.read(length, 0, length.length) != length.length) {
            throw new ProtocolException("Failure reading message from input.");
        }
        final byte[] entry = new byte[parseLength(length)];
        if (in.read(entry, 0, entry.length) != entry.length) {
            throw new ProtocolException("Failed to acquire a complete message.");
        }
        return entry;
    }

    private static void writeValue(@Nonnull final OutputStream out, @Nonnull final byte[] value) throws IOException {
        out.write(encodeLength(value.length), 0, 4);
        out.write(value, 0, value.length);
    }

    private static byte[] encodeLength(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Illegal value for length. Length must be zero or positive.");
        }
        final byte[] bytes = BigInteger.valueOf(length).toByteArray();
        final byte[] sized = new byte[4];
        final int offset = Math.max(0, sized.length - bytes.length);
        System.arraycopy(bytes, 0, sized, offset, sized.length - offset);
        return sized;
    }

    private static int parseLength(@Nonnull final byte[] lengthBytes) {
        return new BigInteger(1, lengthBytes).intValue();
    }

    /**
     * Generate the local ID, in form of `ip-address:port`.
     *
     * @param connection the connection
     * @return Returns the ID.
     */
    @Nonnull
    public static String generateLocalID(@Nonnull final Socket connection) {
        return connection.getLocalAddress().getHostAddress() + ":" + connection.getLocalPort();
    }

    /**
     * Generate the remote ID, in the form `ip-adress:port`.
     *
     * @param connection the connection
     * @return Returns the ID.
     */
    @Nonnull
    public static String generateRemoteID(@Nonnull final Socket connection) {
        return connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

    /**
     * The Message.
     */
    public static final class Message {

        /**
         * The address.
         */
        @Nonnull
        public final String address;
        /**
         * The content.
         */
        @Nonnull
        public final String content;

        /**
         * The constructor.
         *
         * @param address the address
         * @param content the content
         */
        public Message(@Nonnull final String address, @Nonnull final String content) {
            this.address = requireNonNull(address);
            this.content = requireNonNull(content);
        }
    }
}
