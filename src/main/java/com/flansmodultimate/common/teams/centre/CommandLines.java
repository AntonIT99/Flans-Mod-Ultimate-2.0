package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.api.FlansCommandCentre;
import com.mojang.brigadier.arguments.StringArgumentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Writes and checks the command lines the Teams command centre runs. Both sides. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandLines
{
    /** A command line without its slash and the spaces around it. */
    public static String normalise(String command)
    {
        String line = command.strip();
        while (line.startsWith("/"))
            line = line.substring(1).stripLeading();
        return line;
    }

    /** Whether a normalised line is a {@code /teams} command, the only kind the centre runs. */
    public static boolean isCentreCommand(String line)
    {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.equals(FlansCommandCentre.COMMAND) || lower.startsWith(FlansCommandCentre.COMMAND + " ");
    }

    /** Joins the words of a command with spaces, each written as it is. */
    public static String line(Object... words)
    {
        return Arrays.stream(words).map(String::valueOf).collect(Collectors.joining(" "));
    }

    /** A string argument, quoted when it holds anything but the characters an unquoted argument may. */
    public static String quote(String value)
    {
        return StringArgumentType.escapeIfRequired(value);
    }
}
