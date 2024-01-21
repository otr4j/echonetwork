/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.utils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * LogManagers provides functions for {@link LogManager}.
 */
public final class LogManagers {

    private LogManagers() {
        // No need to instantiate.
    }

    /**
     * Read logging configuration file from resource location.
     * <p>
     * Note that {@link System#err} is used to report any issues w.r.t. loading the logging configuration.
     *
     * @param resourcePath the resource path for the logging configuration.
     */
    // FIXME needs tests
    @SuppressWarnings({"PMD.SystemPrintln", "SystemOut"})
    public static void readResourceConfig(@Nonnull final String resourcePath) {
        try (InputStream config = LogManagers.class.getResourceAsStream(resourcePath)) {
            LogManager.getLogManager().readConfiguration(config);
        } catch (final IOException e) {
            System.err.println("Unable to load logging configuration from resource: " + resourcePath + " (" + e.getMessage() + ")");
        }
    }
}
