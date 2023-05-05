package nl.dannyvanheumen.otr4jechoserver.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * LogManagers provides functions for {@link java.util.logging.LogManager}.
 */
public final class LogManagers {

    private LogManagers() {
        // No need to instantiate.
    }

    /**
     * Read logging configuration file from resource location.
     *
     * @param resourcePath the resource path for the logging configuration.
     */
    public static void readResourceConfig(@Nonnull final String resourcePath) {
        try (InputStream config = LogManagers.class.getResourceAsStream(resourcePath)) {
            LogManager.getLogManager().readConfiguration(config);
            System.err.println("Logging configuration loaded.");
        } catch (final IOException e) {
            System.err.println("Unable to load logging configuration from resource: " + resourcePath + " (" + e.getMessage() + ")");
        }
    }
}
