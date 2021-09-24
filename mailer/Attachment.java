/*
 * Released under the MIT License.
 * Copyright 2021 Tetra Informática, S.L.
 */
package mailer;

import java.io.*;
import java.util.*;

/**
 * This class allows you to attach files to messages.
 */
public class Attachment
{
private final File   file;
private final String name;

/**
 * Creates an attachment.
 * @param file Attached file.
 */
public Attachment(File file)
{
    this(file, file.getName());
}

/**
 * Creates an attachment.
 * @param file Attached file.
 * @param name Attachment name.
 */
public Attachment(File file, String name)
{
    this.file = file;
    this.name = name;
}

/**
 * Returns the attachment name.
 * @return Attachment name.
 */
protected String getName()
{
    return name;
}

/**
 * Write the attached file in base64 while reading it.
 * This function saves memory since you do not need to have the
 * entire file in memory to generate its full base64 encoding.
 * @param w Object where to write the file in base64.
 * @throws IOException .
 */
protected void writeBase64(Writer w) throws IOException
{
    // MIME-type base64 encoding generates lines of 76 characters.
    // 3 bytes will generate 4 characters in base64, so in each line of
    // 76 characters we will have 19 groups of 4 characters, therefore,
    // we will have encoded 19 groups of 3 bytes (57 bytes per line).

    try(InputStream is = new BufferedInputStream(
            new FileInputStream(file)))
    {
        int r;
        byte[] buf = new byte[57];
        Base64.Encoder encoder = Base64.getEncoder();

        while((r = readFully(is, buf)) > 0)
        {
            // If we don't read the entire buffer,
            // it will be at the end of the file.
            byte[] src = r < buf.length ? Arrays.copyOf(buf, r) : buf;
            w.append("\r\n").append(encoder.encodeToString(src));
        }
    }
}

private int readFully(InputStream is, byte b[]) throws IOException
{
    int r, n = 0;

    while(n < b.length)
    {
        if((r = is.read(b, n, b.length - n)) < 0)
            return n; //............................................RETURN

        n += r;
    }

    assert n == b.length;
    return n;
}

} // Attachment
