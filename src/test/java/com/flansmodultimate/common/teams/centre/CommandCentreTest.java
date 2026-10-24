package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.common.teams.GameType;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandCentreTest
{
    @Test
    void onlyTeamsAndGameRuleCommandsAreRun()
    {
        assertEquals("teams admin next", CommandLines.normalise("  /teams admin next "));
        assertTrue(CommandLines.isCentreCommand("teams admin next"));
        assertTrue(CommandLines.isCentreCommand("Teams"));
        assertTrue(CommandLines.isCentreCommand("gamerule keepInventory true"));
        assertTrue(CommandLines.isCentreCommand("gamerule randomTickSpeed 3"));
        assertFalse(CommandLines.isCentreCommand("gamerules keepInventory true"));
        assertFalse(CommandLines.isCentreCommand("execute run gamerule keepInventory true"));
        assertFalse(CommandLines.isCentreCommand("teamspeak"));
        assertFalse(CommandLines.isCentreCommand(CommandLines.normalise("/op somebody")));
    }

    @Test
    void namesWithSpacesAreQuoted()
    {
        assertEquals("us+uk", CommandLines.quote("us+uk"));
        assertEquals("\"Red Army\"", CommandLines.quote("Red Army"));
        assertEquals("teams skirmish start \"Battle of Kursk\"", CommandLines.line("teams skirmish start", CommandLines.quote("Battle of Kursk")));
    }

    /** The centre shows each game type's variables with their values and sets them again by those names. */
    @Test
    void everyVariableAGameTypeShowsItTakesBack()
    {
        for (GameType type : GameType.values())
        {
            Map<String, String> variables = type.getVariables();
            variables.forEach((name, value) -> assertTrue(type.setVariable(name, value), type.getId() + " refuses its own variable " + name));
            assertEquals(variables, type.getVariables(), type.getId() + " changed when given its own values");
        }
    }

    @Test
    void theServerSideLoadsWithoutClientClasses() throws Exception
    {
        URL classes = CommandCentre.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d.") || name.startsWith("com.flansmodultimate.client."))
                    throw new ClassNotFoundException("Client classes are unavailable on a dedicated server: " + name);
                if (!name.startsWith("com.flansmodultimate.common.teams.centre.") && !name.contains("PacketCommandCentre"))
                    return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name))
                {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null)
                        loaded = findClass(name);
                    if (resolve)
                        resolveClass(loaded);
                    return loaded;
                }
            }
        })
        {
            for (String type : new String[]{"common.teams.centre.CommandCentre", "common.teams.centre.TeamsSection", "common.teams.centre.CommandLines"})
                assertDoesNotThrow(() -> Class.forName("com.flansmodultimate." + type, true, loader));
            for (String type : new String[]{"network.client.teams.PacketCommandCentreState", "network.server.teams.PacketCommandCentreAction"})
                assertDoesNotThrow(() -> Class.forName("com.flansmodultimate." + type, true, loader).getDeclaredConstructor().newInstance());
        }
    }
}
