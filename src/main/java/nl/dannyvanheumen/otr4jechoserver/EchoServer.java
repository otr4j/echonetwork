package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

public final class EchoServer {

    public static void main(@Nonnull final String[] args) throws IOException {
        final ServerSocket server = new ServerSocket(8080);
        System.err.println("Server started on port " + server.getLocalPort());
        while (!server.isClosed()) {
            try (final Socket connection = server.accept()) {
                while (!connection.isClosed()) {
                    final InputStream in = connection.getInputStream();
                    final OutputStream out = connection.getOutputStream();
                    final byte[] message = readMessage(in);
                    if (message == null) {
                        break;
                    }
                    writeMessage(out, message);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("Server shut down.");
    }
}
