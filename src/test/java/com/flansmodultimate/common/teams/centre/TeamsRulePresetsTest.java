package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;

class TeamsRulePresetsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void builtInsMatchLegacyCommandsAndKeepUnspecifiedRules()
    {
        for (String name : new String[]{"arena", "survival"})
        {
            TeamsManager legacy = new TeamsManager();
            legacy.setBotHealth(93);
            legacy.setBloodEffects(false);
            if (name.equals("arena"))
                legacy.applyArenaPreset();
            else
                legacy.applySurvivalPreset();
            CompoundTag expected = TeamsSection.rules(legacy);
            TeamsManager manager = new TeamsManager();
            manager.setBotHealth(93);
            manager.setBloodEffects(false);
            TeamsRulePresets presets = new TeamsRulePresets(() -> manager);
            presets.apply(presets.read(name), "");
            assertEquals(expected, TeamsSection.rules(manager), name);
            CompoundTag builtIn = presets.read(name);
            assertThrows(IllegalArgumentException.class, () -> presets.save(name, builtIn, true));
            assertThrows(IllegalArgumentException.class, () -> presets.remove(name));
        }
    }

    @Test
    void fullSnapshotRestoresSeaLevelDefaultAndRejectsAllInvalidValuesBeforeMutation()
    {
        TeamsManager manager = new TeamsManager();
        TeamsRulePresets presets = new TeamsRulePresets(() -> manager);
        CompoundTag original = presets.capture("");
        manager.setSeaLevelOverride(100);
        manager.setBotHealth(84);
        manager.setPlayerRegenSeconds(2);
        manager.setBotRegenSeconds(0);
        presets.apply(PresetFiles.decode(SettingsPresets.TEAMS, PresetFiles.encode(SettingsPresets.TEAMS, "test", original)).data(), "");
        assertEquals(original, presets.capture(""));
        CompoundTag invalid = original.copy();
        invalid.putInt("spawnProtection", 61);
        invalid.putBoolean("bloodEffects", !original.getBoolean("bloodEffects"));
        CompoundTag badRange = invalid;
        assertThrows(IllegalArgumentException.class, () -> presets.apply(badRange, ""));
        assertEquals(original, presets.capture(""));
        invalid = original.copy();
        invalid.putString("playerHealth", "oops");
        CompoundTag wrongType = invalid;
        assertThrows(IllegalArgumentException.class, () -> presets.validate(wrongType));
        invalid = original.copy();
        invalid.putBoolean("keepInventory", true);
        CompoundTag unknown = invalid;
        assertThrows(IllegalArgumentException.class, () -> presets.validate(unknown));
    }
}
