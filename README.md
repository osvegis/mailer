# Mailer
Basic SMTP client for Java

Mailer is built on top of [Apache Commons Net API](https://commons.apache.org/proper/commons-net/).

This library is sponsored by [Tetra Informática, S.L.](https://www.tetrainfo.com)

## Example of using the Mailer library

```java
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
```
