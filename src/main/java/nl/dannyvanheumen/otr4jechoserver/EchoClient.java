package nl.dannyvanheumen.otr4jechoserver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.readMessage;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.writeMessage;

public final class EchoClient {

    public static void main(@Nonnull final String[] args) throws IOException {
        try (Socket client = new Socket(InetAddress.getLocalHost(), 8080)) {
            System.err.println("Sending ...");
            writeMessage(client.getOutputStream(), "Hello world!".getBytes(UTF_8));
            final byte[] messageBytes = readMessage(client.getInputStream());
            if (messageBytes != null) {
                System.err.println("Received: " + new String(messageBytes, UTF_8));
            }
        }
    }
}
