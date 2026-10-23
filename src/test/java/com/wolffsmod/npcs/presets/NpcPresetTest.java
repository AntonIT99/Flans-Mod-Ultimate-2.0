package com.wolffsmod.npcs.presets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wolffsmod.npcs.presets.NpcPreset.Target;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NpcPresetTest
{
    private static final Path LANG = Path.of("src", "npcs", "resources", "assets", "wolffsmodnpcs", "lang");

    @Test
    void presetsHaveExplicitLifecycleAndMovementWithoutChangingAmmunitionExceptForPlayers()
    {
        for (NpcPreset preset : NpcPreset.values())
        {
            assertTrue(preset.getTargets().contains(new Target(NpcPresetSetting.WALKING_RANGE, 0)));
            assertTrue(preset.getTargets().contains(new Target(NpcPresetSetting.RESPAWN, preset == NpcPreset.PLAYER ? 0 : 3)));
            assertTrue(preset.getTargets().contains(new Target(NpcPresetSetting.RETURN_TO_START, preset == NpcPreset.TURRET ? 1 : 0)));
            assertTrue(preset.getTargets().contains(new Target(NpcPresetSetting.MOVING_TYPE, preset == NpcPreset.TURRET ? 0 : 1)));
            assertEquals(preset == NpcPreset.PLAYER, preset.sets(NpcPresetSetting.CONSUME_AMMO));
        }
    }

    @Test
    void theMobPresetRoamsFreelyAndStaysDead()
    {
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.MOVING_TYPE, 1)));
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.WALKING_RANGE, 0)));
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.RETURN_TO_START, 0)));
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.NPC_INTERACTING, 0)));
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.RESPAWN, 3)));
        assertTrue(NpcPreset.MOB.getTargets().contains(new Target(NpcPresetSetting.HIDE_BODY, 1)));
    }

    @Test
    void everyPresetSetsEachSettingOnceToAValueItCanHold()
    {
        for (NpcPreset preset : NpcPreset.values())
        {
            Set<NpcPresetSetting> seen = new HashSet<>();
            for (Target target : preset.getTargets())
            {
                assertTrue(seen.add(target.setting()), preset + " sets " + target.setting() + " twice");
                assertTrue(target.setting().accepts(target.value()), preset + " gives " + target.setting() + " " + target.value());
            }
        }
    }

    @Test
    void everyPresetAndSettingIsTranslated() throws IOException
    {
        List<String> keys = new ArrayList<>();
        for (NpcPreset preset : NpcPreset.values())
            keys.addAll(List.of(preset.key(), preset.key() + ".help"));
        for (NpcPresetSetting setting : NpcPresetSetting.values())
            keys.add(setting.key());
        for (String file : List.of("en_us.json", "de_de.json"))
        {
            JsonObject lang = JsonParser.parseString(Files.readString(LANG.resolve(file), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : keys)
                assertTrue(lang.has(key), file + " lacks " + key);
        }
    }
}
