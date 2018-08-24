package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ProtocolException;

import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

final class EchoProtocol {

    private EchoProtocol() {
        // No need to instantiate utility class.
    }

    static byte[] readMessage(@Nonnull final InputStream in) throws IOException {
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
        return message;
    }

    static void writeMessage(@Nonnull final OutputStream out, @Nonnull final byte[] message) throws IOException {
        final byte[] length = encodeLength(message.length);
        out.write(length, 0, length.length);
        out.write(message, 0, message.length);
        out.flush();
    }

    private static byte[] encodeLength(final int length) {
        return asUnsignedByteArray(4, BigInteger.valueOf(length));
    }

    private static int parseLength(@Nonnull final byte[] lengthBytes) {
        return new BigInteger(1, lengthBytes).intValue();
    }
}
