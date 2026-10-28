package com.wolffsmod.npcs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the rule in {@code src/npcs/AGENTS.md}: the module's code uses nothing of the main mod but its public API, and
 * reaches version differences through its own platform package.
 */
class MainModBoundaryTest
{
    private static final Path SOURCES = Path.of("src/npcs/java");
    private static final Pattern MAIN_MOD_REFERENCE = Pattern.compile("\\bcom\\.flansmodultimate\\.(\\w+)");

    @Test
    void moduleCodeUsesOnlyTheMainModsApi() throws IOException
    {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES))
        {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList())
            {
                List<String> lines = Files.readAllLines(file);
                for (int index = 0; index < lines.size(); index++)
                {
                    String line = lines.get(index).trim();
                    // Documentation may point at internals it explains; only code depends on them.
                    if (line.startsWith("*") || line.startsWith("/*") || line.startsWith("//"))
                        continue;
                    Matcher matcher = MAIN_MOD_REFERENCE.matcher(line);
                    while (matcher.find())
                    {
                        if (!matcher.group(1).equals("api"))
                            violations.add(SOURCES.relativize(file) + ":" + (index + 1) + ": " + line);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "NPC module code references main-mod internals:\n" + String.join("\n", violations));
    }
}
