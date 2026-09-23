package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TextDecodingTest
{
    private static final String LATIN_NAME = "Name Sturmgeschütz IV\nShortName 44_StuGIV\n";
    private static final String CHINESE_NAME = "Name 三号突击炮\nShortName stug\n";

    @Test
    void utf8IsDecodedAsUtf8()
    {
        assertEquals(LATIN_NAME, TextDecoding.decode(LATIN_NAME.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void latin1UmlautIsNotMistakenForGb18030()
    {
        byte[] bytes = LATIN_NAME.getBytes(StandardCharsets.ISO_8859_1);
        assertFalse(TextDecoding.looksLikeGb18030(bytes));
        assertEquals(LATIN_NAME, TextDecoding.decode(bytes));
    }

    @Test
    void windows1252PunctuationIsDecoded()
    {
        String text = "Description “StuG” – Français\n";
        assertEquals(text, TextDecoding.decode(text.getBytes(Charset.forName("windows-1252"))));
    }

    @Test
    void chineseGbkTextIsStillDecodedAsGb18030()
    {
        byte[] bytes = CHINESE_NAME.getBytes(Charset.forName("GB18030"));
        assertTrue(TextDecoding.looksLikeGb18030(bytes));
        assertEquals(CHINESE_NAME, TextDecoding.decode(bytes));
    }

    @Test
    void utf16WithAndWithoutBomIsDecoded()
    {
        String text = "item.44_StuGIV.name=Sturmgeschütz IV\n";
        assertEquals(text, TextDecoding.decode(text.getBytes(StandardCharsets.UTF_16)));
        assertEquals(text, TextDecoding.decode(text.getBytes(StandardCharsets.UTF_16LE)));
    }

    @Test
    void utf8BomIsStripped()
    {
        byte[] body = LATIN_NAME.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[body.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(body, 0, bytes, 3, body.length);
        assertEquals(LATIN_NAME, TextDecoding.decode(bytes));
    }
}
