package nl.dannyvanheumen.otr4jechoserver;

import net.java.otr4j.api.SessionID;

import javax.annotation.Nonnull;
import java.net.Socket;

import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.generateLocalID;
import static nl.dannyvanheumen.otr4jechoserver.EchoProtocol.generateRemoteID;

final class EchoSession {

    private EchoSession() {
        // No need to instantiate.
    }

    static SessionID generateSessionID(@Nonnull final Socket socket) {
        return new SessionID(generateLocalID(socket), generateRemoteID(socket), "echo");
    }
}
