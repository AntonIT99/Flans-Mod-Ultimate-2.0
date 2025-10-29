package com.wolffsarmormod.util;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.types.TypeFile;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TypeReaderUtils
{
    private TypeReaderUtils() {}

    public static String readValue(String[] split, String key, String currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length == 2)
            {
                currentValue = split[1];
            }
            else
            {
                logError(incorrectFormat(key, "<singleWord>"), file);
            }
        }
        return currentValue;
    }

    public static String readValues(String[] split, String key, String currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length > 1)
            {
                currentValue = String.join(StringUtils.SPACE, Arrays.copyOfRange(split, 1, split.length));
            }
            else
            {
                logError(incorrectFormat(key, "<values separated by whitespaces>"), file);
            }
        }
        return currentValue;
    }

    public static List<String> readValues(String[] split, String key, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length > 1)
            {
                return Arrays.stream(Arrays.copyOfRange(split, 1, split.length)).toList();
            }
            else
            {
                logError(incorrectFormat(key, "<values separated by whitespaces>"), file);
            }
        }
        return Collections.emptyList();
    }

    public static int readValue(String[] split, String key, int currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length == 2)
            {
                try
                {
                    currentValue = Integer.parseInt(split[1]);
                }
                catch (Exception e)
                {
                    logError(incorrectFormatWrongType(key, split[1], "an integer"), file);
                }
            }
            else
            {
                logError(incorrectFormat(key, "<integer value>"), file);
            }
        }
        return currentValue;
    }

    public static float readValue(String[] split, String key, float currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length == 2)
            {
                try
                {
                    currentValue = Float.parseFloat(split[1]);
                }
                catch (Exception e)
                {
                    logError(incorrectFormatWrongType(key, split[1], "a float"), file);
                }
            }
            else
            {
                logError(incorrectFormat(key, "<float value>"), file);
            }
        }
        return currentValue;
    }

    public static double readValue(String[] split, String key, double currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length == 2)
            {
                try
                {
                    currentValue = Double.parseDouble(split[1]);
                }
                catch (Exception e)
                {
                    logError(incorrectFormatWrongType(key, split[1], "a float"), file);
                }
            }
            else
            {
                logError(incorrectFormat(key, "<float value>"), file);
            }
        }
        return currentValue;
    }

    public static boolean readValue(String[] split, String key, boolean currentValue, TypeFile file)
    {
        if (keyMatches(split, key))
        {
            if (split.length == 2)
            {
                try
                {
                    currentValue = Boolean.parseBoolean(split[1]);
                }
                catch (Exception e)
                {
                    logError(incorrectFormatWrongType(key, split[1], "a boolean"), file);
                }
            }
            else
            {
                logError(incorrectFormat(key, "<true/false>"), file);
            }
        }
        return currentValue;
    }

    public static boolean keyMatches(String[] split, String key)
    {
        return split != null && split.length > 0 && split[0].equalsIgnoreCase(key);
    }

    public static void logError(String s, TypeFile file)
    {
        ArmorMod.log.error("Error in {}/{}/{}: {}", file.getContentPack().getName(), file.getType().getConfigFolderName(), file.getName(), s);
    }

    private static String incorrectFormat(String key, String valuePattern)
    {
        return String.format("Incorrect format for '%s'. Should be '%s %s'", key, key, valuePattern);
    }

    private static String incorrectFormatWrongType(String key, String value, String type)
    {
        return String.format("Incorrect format for '%s'. Passed in value '%s' is not %s", key, value, type);
    }
}
