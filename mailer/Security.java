/*
 * Released under the MIT License.
 * Copyright 2021 Oscar Vega-Gisbert.
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
