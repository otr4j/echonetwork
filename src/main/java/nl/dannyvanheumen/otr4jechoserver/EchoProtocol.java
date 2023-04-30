package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ProtocolException;
import java.net.Socket;

import static java.math.BigInteger.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

// TODO we should extract an EchoOutputStream such that we can atomically write full messages.
final class EchoProtocol {

    static final int DEFAULT_PORT = 8080;

    private EchoProtocol() {
        // No need to instantiate utility class.
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Nonnull
    static Message receiveMessage(@Nonnull final InputStream in) throws IOException {
        synchronized (in) {
            final byte[] address = readValue(in);
            final byte[] message = readValue(in);
            return new Message(new String(address, UTF_8), new String(message, UTF_8));
        }
    }

    static void sendMessage(@Nonnull final OutputStream out, @Nonnull final Message message) throws IOException {
        sendMessage(out, message.address, message.content);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static void sendMessage(@Nonnull final OutputStream out, @Nonnull final String address,
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
        return asUnsignedByteArray(4, valueOf(length));
    }

    private static int parseLength(@Nonnull final byte[] lengthBytes) {
        return new BigInteger(1, lengthBytes).intValue();
    }

    @Nonnull
    static String generateLocalID(@Nonnull final Socket connection) {
        return connection.getLocalAddress().getHostAddress() + ":" + connection.getLocalPort();
    }

    @Nonnull
    static String generateRemoteID(@Nonnull final Socket connection) {
        return connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

    static final class Message {

        @Nonnull
        final String address;
        @Nonnull
        final String content;

        Message(@Nonnull final String address, @Nonnull final String content) {
            this.address = requireNonNull(address);
            this.content = requireNonNull(content);
        }
    }
}
