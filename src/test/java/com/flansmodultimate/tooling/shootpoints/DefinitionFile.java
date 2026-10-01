package com.flansmodultimate.tooling.shootpoints;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A legacy type file, read and written without disturbing anything the tooling
 * does not mean to change.
 *
 * <p>The bytes are decoded as ISO-8859-1, which maps every byte to one char and
 * back, so encodings, byte-order marks, line endings, whitespace and comments
 * survive a round trip exactly. Only ASCII numerals are ever rewritten.</p>
 *
 * <p>Lines are read the way {@code TypeFile} and {@code TypeReaderUtils.splitValues}
 * read them: blank lines and lines starting with {@code //} are skipped, the key is
 * matched case-insensitively and an inline {@code //} ends the values. Lines using
 * the bracketed, quoted or {@code =} syntax are reported as not plain, and the
 * tooling leaves them to be edited by hand.</p>
 */
final class DefinitionFile
{
    private static final String UTF8_BOM = "ï»¿";

    /** A value on a config line and where it sits in the line's text. */
    record Token(String text, int start, int end) {}

    /**
     * A config line.
     *
     * @param index  the line's position in the file, from 0
     * @param key    the key as written
     * @param values the values after the key, without any inline comment
     * @param plain  false when the values use a syntax the tooling does not rewrite
     */
    record Line(int index, String key, List<Token> values, boolean plain)
    {
        String value(int position)
        {
            return values.get(position).text();
        }

        int size()
        {
            return values.size();
        }
    }

    private final List<String> contents = new ArrayList<>();
    private final List<String> separators = new ArrayList<>();
    private final Map<Integer, List<String>> insertedAfter = new TreeMap<>();
    private final String newline;
    private boolean modified;

    private DefinitionFile(String text)
    {
        int start = 0;
        int crlf = 0;
        int lf = 0;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c != '\n' && c != '\r')
                continue;
            contents.add(text.substring(start, i));
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n')
            {
                separators.add("\r\n");
                crlf++;
                i++;
            }
            else
            {
                separators.add(String.valueOf(c));
                lf++;
            }
            start = i + 1;
        }
        contents.add(text.substring(start));
        separators.add("");
        newline = crlf >= lf && crlf > 0 ? "\r\n" : "\n";
    }

    static DefinitionFile read(Path path) throws IOException
    {
        return parse(Files.readAllBytes(path));
    }

    static DefinitionFile parse(byte[] bytes)
    {
        return new DefinitionFile(new String(bytes, StandardCharsets.ISO_8859_1));
    }

    byte[] toBytes()
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < contents.size(); i++)
        {
            text.append(contents.get(i));
            List<String> inserted = insertedAfter.get(i);
            if (inserted == null)
            {
                text.append(separators.get(i));
                continue;
            }
            // A line added after the last one needs a break first, and the file
            // keeps whether it ended with one.
            String ending = separators.get(i);
            for (String line : inserted)
                text.append(newline).append(line);
            text.append(ending);
        }
        return text.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    void write(Path path) throws IOException
    {
        Files.write(path, toBytes());
    }

    boolean isModified()
    {
        return modified;
    }

    /** Every config line with this key, in file order. */
    List<Line> lines(String key)
    {
        List<Line> found = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++)
        {
            Line line = parseLine(i);
            if (line != null && line.key().equalsIgnoreCase(key))
                found.add(line);
        }
        return found;
    }

    /** The first value of the last line with this key, which is what a single-value key reads. */
    String lastValue(String key)
    {
        List<Line> found = lines(key);
        for (int i = found.size() - 1; i >= 0; i--)
        {
            if (found.get(i).size() > 0)
                return found.get(i).value(0);
        }
        return null;
    }

    /** Replaces consecutive values of a line, from {@code firstValue} on, keeping everything around them. */
    void replaceValues(int lineIndex, int firstValue, List<String> replacements)
    {
        Line line = parseLine(lineIndex);
        if (line == null || firstValue + replacements.size() > line.size())
            throw new IllegalArgumentException("Line " + (lineIndex + 1) + " has no values "
                + firstValue + ".." + (firstValue + replacements.size() - 1));
        String content = contents.get(lineIndex);
        StringBuilder rebuilt = new StringBuilder(content);
        for (int i = replacements.size() - 1; i >= 0; i--)
        {
            Token token = line.values().get(firstValue + i);
            rebuilt.replace(token.start(), token.end(), replacements.get(i));
        }
        if (!rebuilt.toString().equals(content))
        {
            contents.set(lineIndex, rebuilt.toString());
            modified = true;
        }
    }

    /** Adds a line after an existing one, indented like it, with the file's own line ending. */
    void insertAfter(int lineIndex, String line)
    {
        String anchor = contents.get(lineIndex);
        int indent = 0;
        while (indent < anchor.length() && (anchor.charAt(indent) == ' ' || anchor.charAt(indent) == '\t'))
            indent++;
        insertedAfter.computeIfAbsent(lineIndex, ignored -> new ArrayList<>()).add(anchor.substring(0, indent) + line);
        modified = true;
    }

    /** Adds a line at the end of the file. */
    void append(String line)
    {
        int last = contents.size() - 1;
        // A trailing line break leaves an empty last "line"; add before it.
        if (last > 0 && contents.get(last).isEmpty())
            last--;
        insertedAfter.computeIfAbsent(last, ignored -> new ArrayList<>()).add(line);
        modified = true;
    }

    int lineCount()
    {
        return contents.size();
    }

    private Line parseLine(int index)
    {
        String content = contents.get(index);
        int offset = 0;
        if (index == 0 && content.startsWith(UTF8_BOM))
            offset = UTF8_BOM.length();
        String rest = content.substring(offset);
        if (rest.isBlank() || rest.startsWith("//"))
            return null;

        List<Token> tokens = new ArrayList<>();
        boolean plain = true;
        int i = offset;
        while (i < content.length())
        {
            char c = content.charAt(i);
            if (Character.isWhitespace(c))
            {
                i++;
                continue;
            }
            // The key is split off on whitespace alone; among the values, // starts a comment.
            boolean isValue = !tokens.isEmpty();
            if (isValue && startsComment(content, i))
                break;
            int start = i;
            while (i < content.length() && !Character.isWhitespace(content.charAt(i))
                && !(isValue && startsComment(content, i)))
                i++;
            String text = content.substring(start, i);
            if (!tokens.isEmpty() && (text.indexOf('[') >= 0 || text.indexOf('(') >= 0 || text.indexOf('{') >= 0
                || text.indexOf('"') >= 0 || text.startsWith("'") || (tokens.size() == 1 && text.startsWith("="))))
                plain = false;
            tokens.add(new Token(text, start, i));
        }
        if (tokens.isEmpty())
            return null;
        return new Line(index, tokens.get(0).text(), List.copyOf(tokens.subList(1, tokens.size())), plain);
    }

    private static boolean startsComment(String content, int index)
    {
        return content.charAt(index) == '/' && index + 1 < content.length() && content.charAt(index + 1) == '/';
    }

    /** Reads a number the way {@code DriveableType#parseLegacyFloat} does, decimal comma included. */
    static float parseFloat(String raw)
    {
        String value = raw.trim();
        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0 && commaIndex == value.lastIndexOf(',') && value.indexOf('.') < 0)
            value = value.replace(',', '.');
        return Float.parseFloat(value);
    }

    static boolean isFloat(String raw)
    {
        try
        {
            parseFloat(raw);
            return true;
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    /** A number rounded to {@code decimals} places, without trailing zeros or a negative zero. */
    static String formatNumber(double value, int decimals)
    {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.signum() == 0)
            return "0";
        return rounded.toPlainString();
    }
}
