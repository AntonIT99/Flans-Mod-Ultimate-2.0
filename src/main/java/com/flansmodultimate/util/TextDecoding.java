package com.flansmodultimate.util;

import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes legacy content pack text whose encoding is unknown.
 * <p>
 * Packs mix UTF-8, UTF-16, GBK/GB18030 and Windows-1252/Latin-1 files. GB18030 accepts almost
 * any byte sequence, so trying it blindly after UTF-8 turns Latin-1 text such as
 * {@code Sturmgeschütz} into CJK characters. GB18030 is only chosen when the high bytes
 * are structured like Chinese text.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TextDecoding
{
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    public static List<String> readLines(Path file) throws IOException
    {
        return new ArrayList<>(decode(Files.readAllBytes(file)).lines().toList());
    }

    public static String readString(Path file) throws IOException
    {
        return decode(Files.readAllBytes(file));
    }

    public static String decode(byte[] bytes)
    {
        if (startsWith(bytes, 0xEF, 0xBB, 0xBF))
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        if (startsWith(bytes, 0xFF, 0xFE) || startsWith(bytes, 0xFE, 0xFF))
            return stripBom(new String(bytes, StandardCharsets.UTF_16));

        String text = tryDecode(bytes, StandardCharsets.UTF_8);
        if (text != null)
            return text;

        Charset utf16 = guessBomlessUtf16(bytes);
        if (utf16 != null && (text = tryDecode(bytes, utf16)) != null)
            return text;

        if (looksLikeGb18030(bytes) && (text = tryDecode(bytes, GB18030)) != null)
            return text;

        text = tryDecode(bytes, WINDOWS_1252);
        return text != null ? text : new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Chinese text is made of multi-byte sequences whose trail bytes are mostly non-ASCII, while
     * Latin-1 text has isolated high bytes next to ASCII letters, spaces or punctuation.
     */
    static boolean looksLikeGb18030(byte[] bytes)
    {
        int highTrailSequences = 0;
        int asciiTrailSequences = 0;
        int index = 0;
        while (index < bytes.length)
        {
            int lead = bytes[index] & 0xFF;
            if (lead < 0x80)
            {
                ++index;
                continue;
            }
            if (lead == 0x80 || lead == 0xFF || index + 1 >= bytes.length)
                return false;

            int trail = bytes[index + 1] & 0xFF;
            if (trail >= 0x30 && trail <= 0x39)
            {
                // Four-byte form: lead, digit, high byte, digit.
                if (index + 3 >= bytes.length || !isGbLead(bytes[index + 2] & 0xFF) || !isDigit(bytes[index + 3] & 0xFF))
                    return false;
                ++highTrailSequences;
                index += 4;
                continue;
            }
            if (trail >= 0x40 && trail <= 0x7E)
                ++asciiTrailSequences;
            else if (trail >= 0x80 && trail <= 0xFE)
                ++highTrailSequences;
            else
                return false;
            index += 2;
        }
        // A few GBK extension characters have ASCII trail bytes; Latin-1 words almost only produce those.
        return highTrailSequences > 0 && asciiTrailSequences * 4 <= highTrailSequences;
    }

    private static Charset guessBomlessUtf16(byte[] bytes)
    {
        if (bytes.length < 2)
            return null;
        int evenZeros = 0;
        int oddZeros = 0;
        for (int index = 0; index < bytes.length; index++)
        {
            if (bytes[index] != 0)
                continue;
            if (index % 2 == 0)
                ++evenZeros;
            else
                ++oddZeros;
        }
        int half = bytes.length / 2;
        if (oddZeros * 3 >= half && oddZeros > evenZeros)
            return StandardCharsets.UTF_16LE;
        if (evenZeros * 3 >= half && evenZeros > oddZeros)
            return StandardCharsets.UTF_16BE;
        return null;
    }

    private static String tryDecode(byte[] bytes, Charset charset)
    {
        try
        {
            return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        }
        catch (CharacterCodingException e)
        {
            return null;
        }
    }

    private static boolean isGbLead(int value)
    {
        return value >= 0x81 && value <= 0xFE;
    }

    private static boolean isDigit(int value)
    {
        return value >= 0x30 && value <= 0x39;
    }

    private static boolean startsWith(byte[] bytes, int... prefix)
    {
        if (bytes.length < prefix.length)
            return false;
        for (int index = 0; index < prefix.length; index++)
            if ((bytes[index] & 0xFF) != prefix[index])
                return false;
        return true;
    }

    private static String stripBom(String text)
    {
        return !text.isEmpty() && text.charAt(0) == (char) 0xFEFF ? text.substring(1) : text;
    }
}
