/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

/**
 * Utilities for java.lang.Thread.
 */
public final class Threads {

    private Threads() {
        // No need to instantiate.
    }

    /**
     * Start a daemon-thread. The daemon-thread cannot live on its own and will die if the rest of the application
     * exits.
     *
     * @param name the thread name
     * @param runnable the runnable that contains the logic to be executed in the thread
     * @param handler the uncaught-exception handler, in case of failures happening inside the thread
     * @return Returns the handle of the thread that has started running in the background.
     */
    @CanIgnoreReturnValue
    @Nonnull
    public static Thread startDaemon(final String name, final Runnable runnable,
            final Thread.UncaughtExceptionHandler handler) {
        final Thread daemon = new Thread(runnable, name);
        daemon.setDaemon(true);
        daemon.setUncaughtExceptionHandler(handler);
        daemon.start();
        return daemon;
    }

    /**
     * Create an uncaught-exception handler that logs the received exception.
     *
     * @param loggerId the class that will become the Logger's identity.
     * @return Returns the constructed exception handler.
     */
    @CheckReturnValue
    @Nonnull
    public static Thread.UncaughtExceptionHandler createLoggingHandler(final Class<?> loggerId) {
        return createLoggingHandler(Logger.getLogger(loggerId.getName()));
    }

    /**
     * Create an uncaught-exception handler that logs the received exception.
     *
     * @param logger the logger.
     * @return Returns the constructed exception handler.
     */
    @CheckReturnValue
    @Nonnull
    public static Thread.UncaughtExceptionHandler createLoggingHandler(final Logger logger) {
        return (t, e) -> logger.log(SEVERE, "Thread [" + t.getName() + "] failed with exception: " + e.getMessage(), e);
    }
}
