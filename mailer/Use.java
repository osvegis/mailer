/*
 * Released under the MIT License.
 * Copyright 2021 Oscar Vega-Gisbert.
 */
package mailer;

import java.io.*;

/**
 * Example of using the Mailer library.
 */
public class Use
{
public static void main() throws IOException
{
    String smtpHost = "?",
           smtpUser = "?",
           password = "?",
           mailFrom = "?",
           mailTo   = "?";

    try(Mailer mailer = new Mailer(smtpHost, 587, smtpUser, password,
                                   Security.TLS, false, mailFrom))
    {
        // A single message.
        mailer.send(mailTo, "Message subject", "Message text");

        // A message with attached files
        mailer.send(mailTo, "Message subject", "Message text",
                    new Attachment(new File("pathname1")),
                    new Attachment(new File("pathname2")));

        // Send an EML file written with Thunderbird.
        mailer.send(mailTo, new File("EML file path"));
    }
}

} // Use
