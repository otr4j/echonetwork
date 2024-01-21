/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.math.BigInteger;

/**
 * Utility methods for integers.
 */
public final class Integers {

    private Integers() {
        // No need to instantiate utility class.
    }

    /**
     * Require an integer value to be at least specified value (inclusive). If not, throw an exception.
     *
     * @param minInclusive Minimum acceptable value.
     * @param value        Value to check.
     * @return Returns same value as provided iff it passes minimum bound check.
     * @throws IllegalArgumentException Throws IllegalArgumentException in case value does not pass check.
     */
    @CanIgnoreReturnValue
    public static int requireAtLeast(final int minInclusive, final int value) {
        if (value < minInclusive) {
            throw new IllegalArgumentException("value is expected to be at minimum " + minInclusive + ", but was " + value);
        }
        return value;
    }

    /**
     * Require integer equality.
     *
     * @param expected expected integer value
     * @param value    actual integer value
     * @return Returns actual integer value iff equal.
     * @throws IllegalArgumentException in case actual value is not equal.
     */
    @CanIgnoreReturnValue
    public static int requireEquals(final int expected, final int value) {
        return requireEquals(expected, value, "value must be " + expected + ", but is " + value);
    }

    /**
     * Require integer equality.
     *
     * @param expected expected integer value
     * @param value    actual integer value
     * @param message  the error message in case of inequality
     * @return Returns actual integer value iff equal.
     * @throws IllegalArgumentException in case actual value is not equal.
     */
    @CanIgnoreReturnValue
    public static int requireEquals(final int expected, final int value, final String message) {
        if (value != expected) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Require an integer value to be different than the forbidden value.
     *
     * @param forbidden the forbidden value
     * @param value     the value to be verified
     * @return Returns value iff not equal to the forbidden value.
     */
    @CanIgnoreReturnValue
    public static int requireNotEquals(final int forbidden, final int value) {
        if (value == forbidden) {
            throw new IllegalArgumentException("value must not be: " + forbidden);
        }
        return value;
    }

    /**
     * Verify that value is in specified range.
     *
     * @param minInclusive the minimum value (inclusive)
     * @param maxInclusive the maximum value (inclusive)
     * @param value        the value to verify
     * @return Returns {@code value} in case in range.
     * @throws IllegalArgumentException In case of illegal value.
     */
    @CanIgnoreReturnValue
    public static int requireInRange(final int minInclusive, final int maxInclusive, final int value) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException("Illegal value: " + value);
        }
        return value;
    }

    /**
     * Parse unsigned integer textual value-representation. All 32 bits are used, the resulting integer may have a
     * negative value.
     *
     * @param text Textual representation of integer value.
     * @param radix Radix for parsing.
     * @return Returns integer value between 0 and 0xffffffff. (That is, all 32 bits are used. So might be negative.)
     */
    public static int parseUnsignedInt(final String text, final int radix) {
        // FIXME isn't this signed now? (did I do some weird refactoring?)
        return new BigInteger(text, radix).intValue();
    }
}
