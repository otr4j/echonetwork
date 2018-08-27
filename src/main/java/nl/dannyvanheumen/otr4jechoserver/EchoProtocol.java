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
import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

final class EchoProtocol {

    static final int DEFAULT_PORT = 8080;

    private EchoProtocol() {
        // No need to instantiate utility class.
    }

    @Nullable
    static String readMessage(@Nonnull final InputStream in) throws IOException {
        final byte[] length = new byte[4];
        final int read = in.read(length, 0, length.length);
        if (read == -1) {
            return null;
        }
        if (read != length.length) {
            throw new ProtocolException("Failed to acquire a complete message.");
        }
        final byte[] message = new byte[parseLength(length)];
        if (in.read(message, 0, message.length) != message.length) {
            throw new ProtocolException("Failed to acquire a complete message.");
        }
        return new String(message, UTF_8);
    }

    static void writeMessage(@Nonnull final OutputStream out, @Nonnull final String message) throws IOException {
        final byte[] messageBytes = message.getBytes(UTF_8);
        final byte[] lengthBytes = encodeLength(messageBytes.length);
        out.write(lengthBytes, 0, lengthBytes.length);
        out.write(messageBytes, 0, messageBytes.length);
        out.flush();
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
}
