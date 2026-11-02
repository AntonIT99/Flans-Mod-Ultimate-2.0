package com.wolffsmod.npcs.teams;

import com.flansmodultimate.common.teams.centre.PresetFiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.CrewPost;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.*;

import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotSettingsPresetsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }
    @TempDir
    Path directory;

    private static CompoundTag document(BotSettings settings)
    {
        CompoundTag data = new CompoundTag();
        ListTag teams = new ListTag();
        teams.add(StringTag.valueOf("red"));
        teams.add(StringTag.valueOf("blue"));
        data.put("teams", teams);
        data.put("settings", settings.save());
        return data;
    }

    @Test
    void oldPresetsInheritEmplacementSupportWithoutMutatingTheImport()
    {
        CompoundTag data = document(new BotSettings());
        data.getCompound("settings").remove("emplacements");
        data.getCompound("settings").remove("player_seat_deployment");
        assertDoesNotThrow(() -> new BotSettingsPresets().validate(data));
        assertFalse(data.getCompound("settings").contains("emplacements"));
        assertTrue(BotSettings.load(data.getCompound("settings")).isEmplacements());
    }

    @Test
    void oldPlayerTicketSwitchDoesNotPreventPresetImports()
    {
        for (boolean value : new boolean[]{false, true})
        {
            CompoundTag data = document(new BotSettings());
            data.getCompound("settings").putBoolean("player_tickets", value);
            assertDoesNotThrow(() -> new BotSettingsPresets().validate(data));
            assertEquals(value, data.getCompound("settings").getBoolean("player_tickets"), "validation must not mutate the import");
            assertFalse(BotSettings.load(data.getCompound("settings")).save().contains("player_tickets"));
        }
    }

    @Test
    void completeBotSettingsTravelThroughJsonAndCarryOverToOtherTeams()
    {
        BotSettings settings = new BotSettings();
        settings.setDifficulty(BotSettings.Difficulty.CUSTOM);
        settings.setVehicleRespawnSeconds(45);
        settings.setCommanderBot(true);
        settings.team("red").setCount(12);
        settings.team("red").setCrewClass(CrewPost.DRIVER, "driver");
        settings.team("red").getVehicles().add(new BotSettings.VehicleEntry("tank", 2, 0, 90, 1, null, ""));
        settings.team("red").getClassWeights().put("rifleman", 3);
        CompoundTag loaded = PresetFiles.decode(BotSettingsPresets.KEY, PresetFiles.encode(BotSettingsPresets.KEY, "test", document(settings))).data();
        assertDoesNotThrow(() -> new BotSettingsPresets().validate(loaded));
        BotSettings carried = BotPresets.carryOver(loaded.getCompound("settings"), List.of("red", "blue"), List.of("green", "yellow"));
        assertEquals(settings.save().getCompound("teams").getCompound("red"), carried.save().getCompound("teams").getCompound("green"));
        assertEquals(45, carried.getVehicleRespawnSeconds());
        assertTrue(carried.isCommanderBot());
        assertEquals(BotSettings.Difficulty.CUSTOM, carried.getDifficulty());
    }

    @Test
    void invalidValuesEnumsAndUnknownKeysAreRejected()
    {
        BotSettingsPresets library = new BotSettingsPresets();
        CompoundTag data = document(new BotSettings());
        data.getCompound("settings").putInt("total", -1);
        assertThrows(IllegalArgumentException.class, () -> library.validate(data));
        data.getCompound("settings").putInt("total", 0);
        data.getCompound("settings").putString("difficulty", "impossible");
        assertThrows(IllegalArgumentException.class, () -> library.validate(data));
        data.getCompound("settings").putString("difficulty", "REGULAR");
        data.getCompound("settings").putBoolean("unknown", true);
        assertThrows(IllegalArgumentException.class, () -> library.validate(data));
    }

    @Test
    void failedLibraryWritesRestoreThePreviousPreset() throws Exception
    {
        Path blocker = directory.resolve("not-a-directory");
        Files.writeString(blocker, "block");
        BotPresets library = new BotPresets(blocker.resolve("bot-presets.json"));
        assertThrows(java.io.IOException.class, () -> library.putChecked(new BotPresets.Preset("test", List.of("red"), new BotSettings().save())));
        assertTrue(library.names().isEmpty());
        BotPresets working = new BotPresets(directory.resolve("bot-presets.json"));
        working.putChecked(new BotPresets.Preset("test", List.of("red"), new BotSettings().save()));
        BotPresets reloaded = new BotPresets(directory.resolve("bot-presets.json"));
        reloaded.load();
        assertTrue(reloaded.find("test").isPresent());
        reloaded.removeChecked("test");
        working.load();
        assertTrue(working.names().isEmpty());
    }
}
