package com.flansmodultimate.util;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.TypeFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TypeReaderUtils
{
    private TypeReaderUtils() {}

    public static boolean hasValueForConfigField(String key, TypeFile file)
    {
        if (file.hasConfigLine(key))
        {
            return file.getConfigLines(key).stream().anyMatch(StringUtils::isNotBlank);
        }
        return false;
    }

    public static boolean hasConfigFieldWithoutValue(String key, TypeFile file)
    {
        if (file.hasConfigLine(key))
        {
            return file.getConfigLines(key).stream().noneMatch(StringUtils::isNotBlank);
        }
        return false;
    }

    public static boolean readFieldWithOptionalValue(String key, boolean defaultValue, TypeFile file)
    {
        if (hasConfigFieldWithoutValue(key, file))
            return true;
        else if (file.hasConfigLine(key))
            return readValue(key, defaultValue, file);
        else
            return defaultValue;
    }

    public static String readValue(String key, String defaultValue, TypeFile file)
    {
        if (file.hasConfigLine(key))
        {
            List<String> lines = file.getConfigLines(key);

            if (!lines.isEmpty())
            {
                // Priority to the last line
                String line = lastNonNull(lines);
                if (line != null)
                {
                    String[] split = splitValues(line);
                    if (split.length == 0)
                    {
                        logError(incorrectFormat(key, "<single value>"), file);
                        return defaultValue;
                    }
                    if (split.length > 1)
                        logError("Incorrect format for '" + key + "': expected one value but found " + split.length
                            + "; using '" + split[0] + "'", file);
                    return split[0];
                }
                else
                {
                    logError(incorrectFormat(key, "<single value>"), file);
                }
            }
        }
        return defaultValue;
    }

    public static int readValue(String key, int defaultValue, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Integer.parseInt(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "an integer"), file);
            }
        }
        return defaultValue;
    }

    @Nullable
    public static Integer readInteger(String key, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Integer.parseInt(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "an integer"), file);
            }
        }
        return null;
    }

    public static float readValue(String key, float defaultValue, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Float.parseFloat(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "a float"), file);
            }
        }
        return defaultValue;
    }

    @Nullable
    public static Float readFloat(String key, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Float.parseFloat(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "a float"), file);
            }
        }
        return null;
    }

    public static double readValue(String key, double defaultValue, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Double.parseDouble(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "a float"), file);
            }
        }
        return defaultValue;
    }

    @Nullable
    public static Double readDouble(String key, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                return Double.parseDouble(strValue);
            }
            catch (Exception e)
            {
                logError(incorrectFormatWrongType(key, strValue, "a float"), file);
            }
        }
        return null;
    }

    public static boolean readValue(String key, boolean defaultValue, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            strValue = strValue.equals("1") ? Boolean.TRUE.toString() : strValue;
            strValue = strValue.equals("0") ? Boolean.FALSE.toString() : strValue;
            if (!Boolean.TRUE.toString().equalsIgnoreCase(strValue) && !Boolean.FALSE.toString().equalsIgnoreCase(strValue))
            {
                logError(incorrectFormatWrongType(key, strValue, "a boolean"), file);
                return defaultValue;
            }
            return Boolean.parseBoolean(strValue);
        }
        return defaultValue;
    }

    @Nullable
    public static Boolean readBoolean(String key, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            strValue = strValue.equals("1") ? Boolean.TRUE.toString() : strValue;
            strValue = strValue.equals("0") ? Boolean.FALSE.toString() : strValue;
            if (!Boolean.TRUE.toString().equalsIgnoreCase(strValue) && !Boolean.FALSE.toString().equalsIgnoreCase(strValue))
            {
                logError(incorrectFormatWrongType(key, strValue, "a boolean"), file);
                return null;
            }
            return Boolean.parseBoolean(strValue);
        }
        return null;
    }

    public static <E extends Enum<E>> E readValue(String key, E defaultValue, Class<E> enumType, TypeFile file)
    {
        String strValue = readValue(key, null, file);
        if (strValue != null)
        {
            try
            {
                // Exact: case-sensitive (fast path)
                return Enum.valueOf(enumType, strValue.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored)
            {
                // Fallback: case-insensitive
                for (E enumValue : enumType.getEnumConstants())
                {
                    if (enumValue.toString().equalsIgnoreCase(strValue))
                        return enumValue;
                }
                String allowed = Arrays.stream(enumType.getEnumConstants()).map(Enum::toString).collect(Collectors.joining(", "));
                logError(incorrectFormatWrongType(key, strValue, "one of: " + allowed), file);
            }
        }
        return defaultValue;
    }

    public static Vector3f readVector(String key, Vector3f defaultValue, TypeFile file)
    {
        String strValue = readValues(key, null, file);
        if (strValue != null)
        {
            try
            {
                return parseVectorValue(strValue);
            }
            catch (Exception ex)
            {
                logError(incorrectFormat(key, "<vector value: [float float float]>"), file);
            }
        }
        return defaultValue;
    }

    @Nullable
    public static Vector3f readVector(String key, TypeFile file)
    {
        String strValue = readValues(key, null, file);
        if (strValue != null)
        {
            try
            {
                return parseVectorValue(strValue);
            }
            catch (Exception ex)
            {
                logError(incorrectFormat(key, "<vector value: [float float float]>"), file);
            }
        }
        return null;
    }

    private static Vector3f parseVectorValue(String raw)
    {
        String value = raw.trim();
        int comment = value.indexOf("//");
        if (comment >= 0)
            value = value.substring(0, comment).trim();
        if (value.startsWith("="))
            value = value.substring(1).trim();
        value = value.replace('[', ' ').replace(']', ' ').replace('(', ' ').replace(')', ' ').trim();

        String[] split = value.split("\\s+");
        if (split.length == 3)
            return new Vector3f(parseVectorComponent(split[0]), parseVectorComponent(split[1]), parseVectorComponent(split[2]));

        // Compact legacy vectors commonly use commas as separators, e.g.
        // "1,2,3". Decimal commas remain supported when whitespace already
        // made the three components unambiguous.
        split = value.replace(',', ' ').trim().split("\\s+");
        if (split.length != 3)
            throw new IllegalArgumentException("Expected exactly three vector components");
        return new Vector3f(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
    }

    private static float parseVectorComponent(String raw)
    {
        String value = raw.trim().replaceAll("^,+|,+$", "");
        if (value.indexOf(',') == value.lastIndexOf(',') && value.indexOf(',') > 0 && value.indexOf('.') < 0)
            value = value.replace(',', '.');
        return Float.parseFloat(value);
    }

    public static String readValues(String key, String defaultValue, TypeFile file)
    {
        if (file.hasConfigLine(key))
        {
            List<String> lines = file.getConfigLines(key);

            if (!lines.isEmpty())
            {
                // Priority to the last line
                String line = lastNonNull(lines);
                if (line != null)
                    return line;
                else
                    logError(incorrectFormat(key, "<values separated by whitespaces>"), file);
            }

        }
        return defaultValue;
    }

    public static String[] readValues(String key, TypeFile file)
    {
        return Optional.ofNullable(readValues(key, null, file)).map(TypeReaderUtils::splitValues).orElse(new String[0]);
    }

    public static Optional<String[]> readValues(String key, TypeFile file, int minNumExpectedValues)
    {
        String[] values = readValues(key, file);
        if (values.length < minNumExpectedValues)
        {
            if (values.length > 0)
                logError(incorrectFormatWrongNumberOfValues(key, values, minNumExpectedValues), file);
            return Optional.empty();
        }
        return Optional.of(values);
    }

    public static Optional<int[]> readIntValues(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValues(key, file, minNumExpectedValues).map(values -> {
            int[] result = new int[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Integer.parseInt(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<integer values>"), file);
                    return null;
                }
            }
            return result;
        });
    }

    public static Optional<float[]> readFloatValues(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValues(key, file, minNumExpectedValues).map(values -> {
            float[] result = new float[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Float.parseFloat(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<float values>"), file);
                    return null;
                }
            }
            return result;
        });
    }

    public static Optional<double[]> readDoubleValues(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValues(key, file, minNumExpectedValues).map(values -> {
            double[] result = new double[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Float.parseFloat(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<float values>"), file);
                    return null;
                }
            }
            return result;
        });
    }

    public static Optional<boolean[]> readBooleanValues(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValues(key, file, minNumExpectedValues).map(values -> {
            boolean[] result = new boolean[values.length];

            for (int i = 0; i < values.length; i++)
            {
                values[i] = values[i].equals("1") ? Boolean.TRUE.toString() : values[i];
                values[i] = values[i].equals("0") ? Boolean.FALSE.toString() : values[i];
                if (!values[i].equalsIgnoreCase(Boolean.TRUE.toString()) && !values[i].equalsIgnoreCase(Boolean.FALSE.toString()))
                {
                    logError(incorrectFormatWrongType(key, "... " + values[i] + " ...", "a boolean"), file);
                    return null;
                }
                result[i] = Boolean.parseBoolean(values[i]);
            }
            return result;
        });
    }

    @Unmodifiable
    public static List<String> readValuesToList(String key, TypeFile file)
    {
        return Arrays.stream(readValues(key, file)).toList();
    }

    public static Optional<List<String>> readLines(String key, TypeFile file)
    {
        if (file.hasConfigLine(key))
        {
            List<String> lines = file.getConfigLines(key);
            if (!lines.isEmpty())
                return Optional.of(lines);

        }
        return Optional.empty();
    }

    public static Optional<List<String[]>> readValuesInLines(String key, TypeFile file)
    {
        return readLines(key, file).map(lines -> lines.stream().map(TypeReaderUtils::splitValues).toList());
    }

    public static Optional<List<String[]>> readValuesInLines(String key, TypeFile file, int minNumExpectedValues)
    {
        return readLines(key, file).map(lines -> lines.stream().map(TypeReaderUtils::splitValues).filter(split -> {
            if (split.length < minNumExpectedValues)
            {
                logError(incorrectFormatWrongNumberOfValues(key, split, minNumExpectedValues), file);
                return false;
            }
            return true;
        }).toList());
    }

    /**
     * Tokenizes a legacy config value while preserving bracketed vectors and
     * discarding inline comments. Type files historically mix "[x y z]" and
     * "x y z" syntax, so a plain whitespace split is not sufficient.
     */
    static String[] splitValues(@Nullable String raw)
    {
        if (StringUtils.isBlank(raw))
            return new String[0];

        String value = raw.trim();
        if (value.startsWith("="))
            value = value.substring(1).trim();

        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int bracketDepth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (quote != 0)
            {
                if (c == quote)
                    quote = 0;
                else
                    token.append(c);
                continue;
            }
            // Apostrophes are common inside legacy projectile names such as
            // Pzgr.L'Spur. They are only quoting delimiters when they begin a
            // value, not when they occur inside an unquoted token.
            if ((c == '\'' || c == '"') && bracketDepth == 0 && token.isEmpty())
            {
                quote = c;
                continue;
            }
            if (c == '/' && i + 1 < value.length() && value.charAt(i + 1) == '/' && bracketDepth == 0)
                break;
            // Some long-lived content packs omit the closing ']' before the
            // next vector. Legacy tokenization still treated those as separate
            // comma vectors, so recover that unambiguous form here as well.
            if (c == '[' && bracketDepth > 0)
            {
                String completedToken = token.toString().trim();
                if (!completedToken.isEmpty())
                    tokens.add(completedToken);
                token.setLength(0);
                bracketDepth = 0;
            }
            if (c == '[' || c == '(' || c == '{')
                bracketDepth++;
            else if (c == ']' || c == ')' || c == '}')
                bracketDepth = Math.max(0, bracketDepth - 1);

            if (Character.isWhitespace(c) && bracketDepth == 0)
            {
                if (!token.isEmpty())
                {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            }
            else
                token.append(c);
        }
        if (!token.isEmpty())
            tokens.add(token.toString());
        return tokens.toArray(String[]::new);
    }

    public static Optional<List<int[]>> readIntValuesInLines(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValuesInLines(key, file, minNumExpectedValues).map(lines -> lines.stream().map(values -> {
            int[] result = new int[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Integer.parseInt(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<integer values>"), file);
                    return null;
                }
            }
            return result;
        }).toList());
    }

    public static Optional<List<float[]>> readFloatValuesInLines(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValuesInLines(key, file, minNumExpectedValues).map(lines -> lines.stream().map(values -> {
            float[] result = new float[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Float.parseFloat(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<float values>"), file);
                    return null;
                }
            }
            return result;
        }).toList());
    }

    public static Optional<List<double[]>> readDoubleValuesInLines(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValuesInLines(key, file, minNumExpectedValues).map(lines -> lines.stream().map(values -> {
            double[] result = new double[values.length];

            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    result[i] = Float.parseFloat(values[i]);
                }
                catch (NumberFormatException e)
                {
                    logError(incorrectFormat(key, "<float values>"), file);
                    return null;
                }
            }
            return result;
        }).toList());
    }

    public static Optional<List<boolean[]>> readBooleanValuesInLines(String key, TypeFile file, int minNumExpectedValues)
    {
        return readValuesInLines(key, file, minNumExpectedValues).map(lines -> lines.stream().map(values -> {
            boolean[] result = new boolean[values.length];

            for (int i = 0; i < values.length; i++)
            {
                values[i] = values[i].equals("1") ? Boolean.TRUE.toString() : values[i];
                values[i] = values[i].equals("0") ? Boolean.FALSE.toString() : values[i];
                if (!values[i].equalsIgnoreCase(Boolean.TRUE.toString()) && !values[i].equalsIgnoreCase(Boolean.FALSE.toString()))
                {
                    logError(incorrectFormatWrongType(key, "... " + values[i] + " ...", "a boolean"), file);
                    return null;
                }
                result[i] = Boolean.parseBoolean(values[i]);
            }
            return result;
        }).toList());
    }

    @Nullable
    private static String lastNonNull(List<String> list)
    {
        return IntStream.iterate(list.size() - 1, i -> i >= 0, i -> i - 1)
            .mapToObj(list::get)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    public static void logError(String s, TypeFile file)
    {
        FlansMod.log.error("Error in {}/{}/{}: {}", file.getContentPack().getName(), file.getType().getConfigFolderName(), file.getName(), s);
    }

    public static void logError(String s, TypeFile file, Exception exception)
    {
        FlansMod.log.error("Error in {}/{}/{}: {}", file.getContentPack().getName(), file.getType().getConfigFolderName(), file.getName(), s, exception);
    }

    private static String incorrectFormat(String key, String valuePattern)
    {
        return String.format("Incorrect format for '%s': Should be '%s %s'", key, key, valuePattern);
    }

    private static String incorrectFormatWrongType(String key, String value, String type)
    {
        return String.format("Incorrect format for '%s %s': Not %s", key, value, type);
    }

    private static String incorrectFormatWrongNumberOfValues(String key, String[] values, int minNumExpectedValues)
    {
        return String.format("Incorrect format for '%s %s': Expected at least %d values separated by whitespaces", key, String.join(StringUtils.SPACE, values), minNumExpectedValues);
    }
}
