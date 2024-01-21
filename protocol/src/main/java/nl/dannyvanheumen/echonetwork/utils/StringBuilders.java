/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.utils;

import javax.annotation.Nonnull;

/**
 * StringBuilders provides utilities for StringBuilder.
 */
public final class StringBuilders {

    private StringBuilders() {
        // No need to instantiate.
    }

    /**
     * Extract current content, then clear the builder.
     *
     * @param builder the builder
     * @return Returns the content currently in the builder and leaves the builder itself empty.
     */
    @Nonnull
    public static String drain(final StringBuilder builder) {
        final String content = builder.toString();
        clear(builder);
        return content;
    }

    /**
     * appendAll appends all elements to the provided StringBuilder.
     *
     * @param builder the (destination) string builder
     * @param parts all of the parts to be appended
     */
    public static void appendAll(final StringBuilder builder, final String... parts) {
        for (final String part : parts) {
            builder.append(part);
        }
    }

    /**
     * Clear a stringbuilder. (Sets the length to 0.)
     *
     * @param builder the stringbuilder
     */
    public static void clear(final StringBuilder builder) {
        builder.setLength(0);
    }
}
