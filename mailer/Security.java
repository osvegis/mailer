/*
 * Released under the MIT License.
 * Copyright 2021 Tetra Informática, S.L.
 */
package mailer;

/**
 * Security protocol for SMTP connection.
 */
public enum Security
{
    /**
     * STARTTLS
     */
    TLS,

    /**
     * SSL/TLS
     */
    SSL,

    /**
     * Without security. Not recommended.
     */
    NONE
}
