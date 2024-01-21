/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import javax.annotation.Nonnull;

/**
 * String utilities.
 */
@SuppressWarnings("unused")
public final class Strings {

    private Strings() {
        // No need to instantiate utility class.
    }

    /**
     * Require that a string is not blank/empty.
     *
     * @param string the string to be tested
     * @return Returns the same string iff not blank.
     */
    @CanIgnoreReturnValue
    @Nonnull
    public static String requireNotBlank(final String string) {
        if (string.isBlank()) {
            throw new IllegalArgumentException("String is not allowed to be blank.");
        }
        return string;
    }

    /**
     * Join multiple String parts into a single concatenated String.
     *
     * @param parts separated string parts
     * @return Joint string.
     */
    @Nonnull
    public static String concatenate(final String... parts) {
        final StringBuilder builder = new StringBuilder();
        StringBuilders.appendAll(builder, parts);
        return builder.toString();
    }

    /**
     * Cut a text at the first occurrence of the provided separator. Return two substrings, the part before the
     * separator and the part after the separator (excluding the separator itself).
     * <p>
     * If the separator is not present, a size 2 array is returned with second part {@code null}.
     *
     * @param text the text
     * @param separator the separator
     * @return Returns array of size 2, with substring before and after separator (excluding separator), or second part
     * {@code null} in case separator was not present.
     */
    @Nonnull
    public static String[] cut(final String text, final int separator) {
        final int idx = text.indexOf(separator);
        if (idx == -1) {
            return new String[]{text, null};
        }
        return cutAt(idx, text);
    }

    /**
     * Cut a text at the first occurrence of the provided separator. Return two substrings, the part before the
     * separator and the part after the separator (excluding the separator itself).
     * <p>
     * If the separator is not present, a size 2 array is returned with second part {@code null}.
     *
     * @param text the text
     * @param separator the separator
     * @param fromIndex the starting index from which to start searching
     * @return Returns array of size 2, with substring before and after separator (excluding separator), or second part
     * {@code null} in case separator was not present.
     */
    @Nonnull
    public static String[] cut(final String text, final int separator, final int fromIndex) {
        final int idx = text.indexOf(separator, fromIndex);
        if (idx == -1) {
            return new String[]{text, null};
        }
        return cutAt(idx, text);
    }

    /**
     * Cut a text at the first occurrence of the provided separator. Return two substrings, the part before the
     * separator and the part after the separator (excluding the separator itself).
     * <p>
     * If the separator is not present, a size 2 array is returned with second part {@code null}.
     *
     * @param text the text
     * @param separator the separator
     * @return Returns array of size 2, with substring before and after separator (excluding separator), or second part
     * {@code null} in case separator was not present.
     */
    @Nonnull
    public static String[] cut(final String text, final String separator) {
        final int idx = text.indexOf(separator);
        if (idx == -1) {
            return new String[]{text, null};
        }
        return cutAt(idx, text);
    }

    /**
     * Cut a text at the first occurrence of the provided separator. Return two substrings, the part before the
     * separator and the part after the separator (excluding the separator itself).
     * <p>
     * If the separator is not present, a size 2 array is returned with second part {@code null}.
     *
     * @param text the text
     * @param separator the separator
     * @param fromIndex starting index for search
     * @return Returns array of size 2, with substring before and after separator (excluding separator), or second part
     * {@code null} in case separator was not present.
     */
    @Nonnull
    public static String[] cut(final String text, final String separator, final int fromIndex) {
        final int idx = text.indexOf(separator, fromIndex);
        if (idx == -1) {
            return new String[]{text, null};
        }
        return cutAt(idx, text);
    }

    /**
     * cutAt cuts a text at the specified index. Return two substrings, the part before the index and the part after the
     * index (excluding the index).
     *
     * @param text the text
     * @param idx the cut index
     * @return Returns array of size 2, with substring before and after separator (excluding separator), or second part
     * {@code null} in case separator was not present.
     */
    @Nonnull
    public static String[] cutAt(final int idx, final String text) {
        Integers.requireInRange(0, text.length() - 1, idx);
        return new String[]{text.substring(0, idx), text.substring(idx + 1)};
    }
}
