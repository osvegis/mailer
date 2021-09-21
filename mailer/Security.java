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
