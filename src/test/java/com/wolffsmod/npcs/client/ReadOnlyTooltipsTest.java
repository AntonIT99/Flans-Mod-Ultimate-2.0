package com.wolffsmod.npcs.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOnlyTooltipsTest
{
    private static final Path LANG = Path.of("src/npcs/resources/assets/wolffsmodnpcs/lang/en_us.json");
    private static final String PREFIX = "wolffsmodnpcs.weapons.read_only.";

    @Test
    void everyParameterTooltipNamesAScreenParameterAndHasARestoreLine() throws IOException
    {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        Set<String> names = ReadOnlyTooltips.names();
        for (Reason reason : Reason.values())
        {
            String generic = PREFIX + reason.name().toLowerCase(Locale.ROOT);
            assertTrue(lang.has(generic), generic);
            boolean specific = false;
            for (String key : lang.keySet())
            {
                if (!key.startsWith(generic + ".") || key.equals(generic + ".restore"))
                    continue;
                specific = true;
                assertTrue(names.contains(key.substring(generic.length() + 1)), key);
            }
            assertTrue(!specific || lang.has(generic + ".restore"), generic + ".restore");
        }
    }
}
