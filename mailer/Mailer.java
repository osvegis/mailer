/*
 * Released under the MIT License.
 * Copyright 2021 Tetra Informática, S.L.
 */
package mailer;

import java.io.*;
import java.util.*;
import org.apache.commons.net.*;
import org.apache.commons.net.smtp.*;

/**
 * Basic SMTP client for Java.
 */
public class Mailer implements Closeable
{
private final AuthenticatingSMTPClient smtpClient;
private final CharArrayWriter debug = new CharArrayWriter();
private final String mailFrom, mailCco;

/**
 * Creates an object to send messages using SMTP.
 * @param smtp SMTP server.
 * @param port Port. If it is {@code null}, the default port will be used.
 * @param user User name.
 * @param password User password.
 * @param security Security protocol.
 * @param certrust Check the server certificate.
 * @param mailFrom Sender.
 * @throws IOException .
 */
public Mailer(String smtp, Integer port, String user, String password,
              Security security, boolean certrust, String mailFrom)
    throws IOException
{
    this(smtp, port, user, password, security, certrust, mailFrom, null);
}

/**
 * Creates an object to send messages using SMTP.
 * @param smtp SMTP server.
 * @param port Port. If it is {@code null}, the default port will be used.
 * @param user User name.
 * @param password User password.
 * @param security Security protocol.
 * @param certrust Check the server certificate.
 * @param mailFrom Sender.
 * @param mailCco Hidden recipients.
 * @throws IOException .
 */
public Mailer(String smtp, Integer port,
              String user, String password,
              Security security, boolean certrust,
              String mailFrom, String mailCco)
    throws IOException
{
    Objects.requireNonNull(smtp);
    Objects.requireNonNull(user);
    Objects.requireNonNull(password);
    Objects.requireNonNull(security);
    this.mailFrom = Objects.requireNonNull(mailFrom).trim();
    this.mailCco  = mailCco;

    if(port == null)
        port = getDefaultPort(security);

    smtpClient = connect(smtp, port, security, certrust);
    clientAuth(user, password);
}

private int getDefaultPort(Security security)
{
    switch(security)
    {
        case TLS:  return 587;
        case SSL:  return 465;
        case NONE: return 25;
        default: throw new AssertionError();
    }
}

private AuthenticatingSMTPClient connect(
        String smtp, int port, Security security, boolean certrust)
    throws IOException
{
    // Today the vast majority of SMTP servers require authentication.
    // Java Commons Net allows you to connect to an SMTP server without
    // security with the SMTPClient class, but this class does not allow
    // to authenticate with username and password. So to connect to a
    // server without security, but with authentication, we have no choice
    // but to use AuthenticatingSMTPClient, but without calling 'execTLS'.

    AuthenticatingSMTPClient client;
    
    switch(security)
    {
        case TLS:
            client = new AuthenticatingSMTPClient("TLS", false);
            break;
        case SSL:
            client = new AuthenticatingSMTPClient("SSL", true);
            break;
        case NONE:
            // We will not call 'execTLS'.
            client = new AuthenticatingSMTPClient("TLS", false);
            break;
        default:
            throw new AssertionError();
    }

    client.addProtocolCommandListener(new PrintCommandListener(
            new PrintWriter(debug, true), true));

    client.setHostnameVerifier((hostname, session) ->
                               hostname.equals(session.getPeerHost()));

    client.setEndpointCheckingEnabled(certrust);
    client.connect(smtp, port);

    if(!SMTPReply.isPositiveCompletion(client.getReplyCode()))
    {
        client.disconnect();
        throw newIOException("SMTP server refused connection.");
    }

    if(!client.elogin())
        throw newIOException("Access denied.");

    if(security == Security.TLS)
    {
        if(!client.execTLS())
            throw newIOException("TLS negotiation failure.");

        // In Office365 we must log in again!

        if(!client.elogin())
            throw newIOException("Access denied.");
    }

    return client;
}

private void clientAuth(String user, String password) throws IOException
{
    try
    {
        // We try with the methods indicated by the server.
        String header = "250-AUTH ";

        for(String reply : smtpClient.getReplyStrings())
        {
            if(reply.startsWith(header))
            {
                StringTokenizer st = new StringTokenizer(
                                     reply.substring(header.length()));

                while(st.hasMoreTokens())
                {
                    AuthenticatingSMTPClient.AUTH_METHOD method;
                    method = getAuthMethod(st.nextToken());

                    if(method != null)
                    {
                        if(smtpClient.auth(method, user, password))
                            return; //..............................RETURN
                    }
                }
            }
        }
    }
    catch(Exception ex)
    {
        throw newIOException(ex);
    }
}

private AuthenticatingSMTPClient.AUTH_METHOD getAuthMethod(String name)
{
    switch(name)
    {
        case "PLAIN":
            return AuthenticatingSMTPClient.AUTH_METHOD.PLAIN;
        case "CRAM-MD5":
            return AuthenticatingSMTPClient.AUTH_METHOD.CRAM_MD5;
        case "LOGIN":
            return AuthenticatingSMTPClient.AUTH_METHOD.LOGIN;
        case "XOAUTH":
            return AuthenticatingSMTPClient.AUTH_METHOD.XOAUTH;
        case "XOAUTH2":
            return AuthenticatingSMTPClient.AUTH_METHOD.XOAUTH2;
        default:
            return null; // Unsupported method.
    }
}

/**
 * Sends a message.
 * @param mailTo Recipients separated by semicolons.
 * @param subject Subject of the message.
 * @param text Text of the message.
 * @param attachments Attachments.
 * @throws IOException .
 */
public void send(String mailTo, String subject, String text,
                 Attachment...attachments)
    throws IOException
{
    String header   = getHeader(mailTo, Util.encodeSubject(subject)),
           boundary = attachments.length > 0 ? getBoundary() : null;

    try(Writer w = smtpClient.sendMessageData())
    {
        if(w == null)
            throw newIOException("Unable to send message data.");

        w.append(header);

        if(boundary != null)
        {
            w.append("MIME-Version: 1.0\r\n");
            w.append("Content-Type: multipart/mixed; ");
            w.append("boundary=").append(boundary).append("\r\n");
            w.append("\r\n--").append(boundary).append("\r\n");
        }

        if(Util.isHtml(text))
        {
            w.append("Content-Type: text/html; charset=ISO-8859-1\r\n");
            w.append("Content-Transfer-Encoding: 8bit\r\n\r\n");
            w.append(text).append("\r\n");
        }
        else
        {
            w.append("Content-Type: text/plain; charset=ISO-8859-1\r\n");
            w.append("Content-Transfer-Encoding: quoted-printable");
            w.append("\r\n\r\n");
            w.append(Util.encodeQuotedPrintable(text)).append("\r\n");
        }

        for(Attachment a : attachments)
        {
            String fileName = "\""+ Util.encodeString(a.getName()) +"\"";
            w.append("\r\n--").append(boundary).append("\r\n");
            w.append("Content-Type: application/octet-stream; ");
            w.append("name=").append(fileName).append("\r\n");
            w.append("Content-Transfer-Encoding: base64\r\n");
            w.append("Content-Disposition: attachment; filename=");
            w.append(fileName).append("\r\n");
            a.writeBase64(w);
        }

        if(boundary != null)
            w.append("\r\n--").append(boundary).append("--\r\n");
    }

    if(!smtpClient.completePendingCommand())
        throw newIOException("The message could not be sent.");
}

/**
 * Sends an EML file.
 * @param mailTo Recipients separated by semicolons.
 * @param eml EML file to send.
 * @throws IOException .
 */
public void send(String mailTo, File eml) throws IOException
{
    try(BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(eml), "ISO-8859-1")))
    {
        String  subject  = "Subject: ",
                content  = null,
                boundary = null,
                header   = null,
                line;

        while((line = reader.readLine()) != null && !line.isEmpty())
        {
            if(line.startsWith(subject))
            {
                // The subject can be several lines long.
                StringBuilder sb = new StringBuilder(
                        line.substring(subject.length()));

                while((line = reader.readLine()) != null
                      && !line.isEmpty()
                      && Character.isWhitespace(line.charAt(0)))
                {
                    sb.append('\n').append(line);
                }

                header = getHeader(mailTo, sb.toString());

                if(line == null || line.isEmpty())
                    break; //........................................BREAK
            }

            if(line.startsWith("Content-Type:"))
                content = line;
            else if(line.contains("boundary="))
                boundary = line; // It is on the next line.
        }

        if(header == null)
            throw new IOException("EML file has no subject.");

        if(line == null || content == null)
            throw new IOException("Invalid EML file.");

        try(Writer w = smtpClient.sendMessageData())
        {
            w.append(header);
            w.append(content).append("\r\n");

            if(boundary != null)
                w.append(boundary).append("\r\n");

            while(line != null)
            {
                w.append(line).append("\r\n");
                line = reader.readLine();
            }
        }
    }

    if(!smtpClient.completePendingCommand())
        throw newIOException("The message could not be sent.");
}

private String getHeader(String mailTo, String subject) throws IOException
{
    if(!smtpClient.reset())
        throw newIOException("Unable to reset SMTP connection.");

    debug.reset();
    String[] recipient = Util.getAddressArray(mailTo);

    if(!smtpClient.setSender(Util.getMailAddress(mailFrom)))
        throw newIOException("Invalid email: "+ mailFrom);

    addRecipient(recipient[0]);

    for(String cco : Util.getAddressArray(mailCco))
        addRecipient(cco);

    SimpleSMTPHeader header = new SimpleSMTPHeader(
            Util.encodeMailAddress(mailFrom), recipient[0], subject);

    for(int i = 1; i < recipient.length; i++)
    {
        addRecipient(recipient[i]);
        header.addCC(recipient[i]);
    }

    StringBuilder sb = new StringBuilder();
    sb.append(header);

    // We remove the last '\n' because if not, the following directives
    // (MIME-VERSION, Content-Type, boundary) are not taken into account.
    // There is a 'SimpleSMTPHeader.addHeaderField' function, but it
    // appends the fields at the beginning, which in our case is wrong.
    sb.setLength(sb.length() - 1);
    return sb.toString();
}

private void addRecipient(String address) throws IOException
{
    if(!smtpClient.addRecipient(address))
        throw newIOException("Invalid address: "+ address);
}

private String getBoundary()
{
    long millis = System.currentTimeMillis();
    return "_boundary_"+ millis +"_"+ Long.toHexString(millis) +"_";
}

private IOException newIOException(String message)
{
    StringBuilder sb = new StringBuilder();

    if(debug.size() > 0)
    {
        sb.append(message).append("\n\n");
        sb.append(debug.toString());
        debug.reset();
    }

    return new IOException(sb.toString());
}

private IOException newIOException(Exception ex)
{
    if(ex instanceof IOException)
        return (IOException)ex;

    IOException ioex = new IOException(ex);
    ioex.setStackTrace(ex.getStackTrace());
    return ioex;
}

/**
 * Closes this object.
 * @throws IOException .
 */
@Override public void close() throws IOException
{
    try
    {
        smtpClient.logout();
    }
    finally
    {
        smtpClient.disconnect();
    }
}

} // Mailer
