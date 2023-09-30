/*
 * protocol, the protocol (convenience) logic for the echonetwork.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package nl.dannyvanheumen.echonetwork.protocol;

/**
 * Interface for Echonetwork client.
 */
@SuppressWarnings("PMD.ConstantsInInterface")
public interface Client {
    /**
     * Default protocol name.
     */
    String DEFAULT_PROTOCOL_NAME = "echo";
    /**
     * Default test password for SMP.
     */
    String DEFAULT_SMP_SECRET = "Password!";
    /**
     * Default separator for tags, used in the format that adds the instance tag to the connection string.
     */
    char TAG_SEPARATOR = '#';
}
