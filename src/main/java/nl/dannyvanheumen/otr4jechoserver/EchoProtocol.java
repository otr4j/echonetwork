package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nullable
    static Message readMessage(@Nonnull final InputStream in) throws IOException {
        final byte[] address = read(in);
        if (address == null) {
            return null;
        }
        final byte[] message = read(in);
        if (message == null) {
            throw new ProtocolException("Failed to acquire 'message' component.");
        }
        return new Message(new String(address, UTF_8), new String(message, UTF_8));
    }

    private static byte[] read(@Nonnull final InputStream in) throws IOException {
        final byte[] length = new byte[4];
        final int readAddress = in.read(length, 0, length.length);
        if (readAddress == -1) {
            return null;
        }
        if (readAddress != length.length) {
            throw new ProtocolException("Failed to acquire a complete message.");
        }
        final byte[] entry = new byte[parseLength(length)];
        if (in.read(entry, 0, entry.length) != entry.length) {
            throw new ProtocolException("Failed to acquire a complete message.");
        }
        return entry;
    }

    static void writeMessage(@Nonnull final OutputStream out, @Nonnull final Message message) throws IOException {
        writeMessage(out, message.address, message.content);
    }

    static void writeMessage(@Nonnull final OutputStream out, @Nonnull final String address,
            @Nonnull final String message) throws IOException {
        synchronized (requireNonNull(out)) {
            final byte[] addressBytes = address.getBytes(UTF_8);
            final byte[] addressLengthBytes = encodeLength(addressBytes.length);
            final byte[] messageBytes = message.getBytes(UTF_8);
            final byte[] messageLengthBytes = encodeLength(messageBytes.length);
            out.write(addressLengthBytes, 0, addressLengthBytes.length);
            out.write(addressBytes, 0, addressBytes.length);
            out.write(messageLengthBytes, 0, messageLengthBytes.length);
            out.write(messageBytes, 0, messageBytes.length);
            out.flush();
        }
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

        final String address;
        final String content;

        Message(@Nonnull final String address, @Nonnull final String content) {
            this.address = requireNonNull(address);
            this.content = requireNonNull(content);
        }
    }
}
