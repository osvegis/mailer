/*
 * Released under the MIT License.
 * Copyright 2021 Tetra Informática, S.L.
 */
package mailer;

import java.io.*;
import java.util.*;

/**
 * Utility methods for Mailer.
 */
public class Util
{
/**
 * Divide a list of email addresses into an array of strings.
 * @param mailTo Email addresses separated by semicolons.
 * @return Array of email addresses.
 */
public static String[] getAddressArray(String mailTo)
{
    if(mailTo == null)
        return new String[0]; //....................................RETURN

    StringTokenizer st = new StringTokenizer(mailTo, ";");
    String[] recipients = new String[st.countTokens()];

    for(int i = 0; i < recipients.length; i++)
        recipients[i] = st.nextToken().trim();

    return recipients;
}

/**
 * Returns an email address contained in the string "Name <email@host>".
 * @param s String containing an email address.
 * @return Email address.
 */
public static String getMailAddress(String s)
{
    if(!s.isEmpty() && s.charAt(s.length() - 1) == '>')
    {
        int i = s.lastIndexOf(' ');

        if(i != -1 && s.charAt(i + 1) == '<')
            return s.substring(i + 2, s.length() - 1); //...........RETURN
    }

    return s;
}

/**
 * Encodes the name of an email address in the format "Name <email@host>".
 * @param s String containing an email address.
 * @return Encoded email address.
 */
public static String encodeMailAddress(String s)
{
    if(!s.isEmpty() && s.charAt(s.length() - 1) == '>')
    {
        int i = s.lastIndexOf(' ');

        if(i != -1 && s.charAt(i + 1) == '<')
        {
            StringBuilder sb = new StringBuilder();
            sb.append(encodeString(s.substring(0, i)));
            sb.append(s.substring(i, s.length()));
            return sb.toString(); //................................RETURN
        }
    }

    return s;
}

/**
 * Encodes a string if necessary.
 * @param s String to encode.
 * @return Encoded string.
 */
public static String encodeString(String s)
{
    if(s == null || s.isEmpty())
        return ""; //...............................................RETURN

    if(!needEncoding(s))
        return s; //................................................RETURN

    // RFC 2047
    byte[] content = getBytes(s);
    Base64.Encoder e = Base64.getEncoder();
    return "=?UTF-8?B?"+ e.encodeToString(content) +"?=";
}

/**
 * Encodes the subject text of an email.
 * @param s Subject text.
 * @return Encoded text.
 */
public static String encodeSubject(String s)
{
    if(s == null || s.isEmpty())
        return ""; //...............................................RETURN

    if(!needEncoding(s))
        return splitSubject(s);

    // The base64 encoding of the subject generates a first line of 56
    // characters and the following lines of 64. 3 bytes will generate 4
    // characters in base64, so in the first line we will have 14 groups
    // of 4 characters and in the following lines 16 groups. Therefore,
    // in the first line we will encode 42 bytes and in the following 48.

    byte[] content = getBytes(s);
    Base64.Encoder e = Base64.getEncoder();
    StringBuilder sb = new StringBuilder();
    int chunk = 42;

    for(int i = 0; i < content.length;)
    {
        if(sb.length() > 0)
            sb.append("\n ");

        int end = i + Math.min(chunk, content.length - i);
        byte[] line = Arrays.copyOfRange(content, i, end);
        sb.append("=?UTF-8?B?");
        sb.append(e.encodeToString(line));
        sb.append("?=");
        chunk = 48;
        i = end;
    }

    return sb.toString();
}

//------------------------------------------------------------------------
private static boolean needEncoding(String s)
{
    int length = s.length();

    for(int i = 0; i < length; i++)
    {
        char c = s.charAt(i);

        if(c < 32 || c > 126)
            return true;
    }

    return false;
}

//------------------------------------------------------------------------
private static byte[] getBytes(String s)
{
    try
    {
        return s.getBytes("UTF-8");
    }
    catch(UnsupportedEncodingException ex)
    {
        return s.getBytes();
    }
}

//------------------------------------------------------------------------
private static String splitSubject(String s)
{
    StringTokenizer st = new StringTokenizer(s);
    StringBuilder sb = new StringBuilder();
    int i = -"Subject: ".length();

    while(st.hasMoreTokens())
    {
        String t = st.nextToken();
        int length = sb.length() - i;

        if(length > 0)
        {
            if(length + t.length() > 75)
            {
                sb.append("\n ");
                i = sb.length();
            }
            else
            {
                sb.append(' ');
            }
        }

        sb.append(t);
    }

    return sb.toString();
}

/**
 * Checks if a string is HTML.
 * @param s String to check if it is HTML.
 * @return {@code true} if it is HTML.
 */
public static boolean isHtml(String s)
{
    if(s == null || s.isEmpty())
        return false; //............................................RETURN

    int i = 0,
        n = s.length();

    while(i < n)
    {
        if(!Character.isWhitespace(s.charAt(i)))
            break;
    }

    String html = "<html>";
    return s.regionMatches(true, i, html, 0, html.length());
}

/**
 * Encodes a string in quoted-printable.
 * @param str String to encode.
 * @return String encoded in quoted-printable.
 */
public static String encodeQuotedPrintable(String str)
{
    if(str == null || str.isEmpty())
        return ""; //...............................................RETURN

    byte[] content;

    try
    {
        content = str.getBytes("ISO-8859-1");
    }
    catch(UnsupportedEncodingException e)
    {
        content = str.getBytes();
    }

    StringBuilder sb = new StringBuilder();

    int start  = 0,
        length = content.length,
        last   = length - 1;

    for(int i = 0; i < length; i++)
    {
        byte c = content[i];

        if(c == '\n')
        {
            sb.append("\r\n");
            start = sb.length();
        }
        else if(c >= 33 && c <= 126 && c != '=' ||
                c == ' ' && i < last && content[i+1] != '\n')
        {
            start = checkLineLength(sb, start, 1);
            sb.append((char)c);
        }
        else
        {
            start = checkLineLength(sb, start, 3);
            sb.append('=').append(String.format("%02X", c));
        }
    }

    return sb.toString();
}

private static int checkLineLength(
        StringBuilder sb, int lineStart, int required)
{
    final int maxLength  = 76,
              lineLength = sb.length() - lineStart;

    if(lineLength + required >= maxLength)
        sb.append("=\r\n");

    if(sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n')
        lineStart = sb.length();

    return lineStart;
}

} // Util
