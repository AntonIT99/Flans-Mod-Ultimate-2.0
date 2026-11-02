package com.flansmodultimate.common.teams.centre;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class PresetFilesTest
{
    @TempDir
    Path directory;

    @Test
    void readableJsonRoundTripsBooleansNumbersAndNestedSettings() throws Exception
    {
        CompoundTag data = new CompoundTag();
        data.putBoolean("enabled", true);
        data.putInt("count", 12);
        data.putFloat("weight", 0.45F);
        data.putString("label", "日本語");
        data.putUUID("allocation", java.util.UUID.randomUUID());
        data.putLongArray("longs", new long[]{Long.MIN_VALUE, Long.MAX_VALUE});
        data.putByteArray("bytes", new byte[]{-128, 0, 127});
        ListTag list = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("type", "tank");
        list.add(entry);
        data.put("vehicles", list);
        String json = PresetFiles.encode(SettingsPresets.TEAMS, "example", data);
        assertTrue(json.contains("\"enabled\": true"));
        CompoundTag loaded = PresetFiles.decode(SettingsPresets.TEAMS, json).data();
        assertTrue(loaded.getBoolean("enabled"));
        assertEquals(0.45F, loaded.getFloat("weight"));
        assertEquals("日本語", loaded.getString("label"));
        assertEquals(list, loaded.get("vehicles"));
        assertEquals(data.getUUID("allocation"), loaded.getUUID("allocation"));
        assertArrayEquals(data.getLongArray("longs"), loaded.getLongArray("longs"));
        assertArrayEquals(data.getByteArray("bytes"), loaded.getByteArray("bytes"));
        Path file = directory.resolve("example.json");
        PresetFiles.write(file, json, false);
        assertEquals(json, PresetFiles.read(file));
        assertThrows(FileAlreadyExistsException.class, () -> PresetFiles.write(file, "replacement", false));
        assertEquals(json, PresetFiles.read(file));
        PresetFiles.write(file, json + "\n", true);
        assertEquals(json + "\n", PresetFiles.read(file));
    }

    @Test
    void malformedWrongTypeFutureVersionOversizedAndDeepFilesAreRejected() throws Exception
    {
        String json = PresetFiles.encode(SettingsPresets.TEAMS, "example", new CompoundTag());
        ResourceLocation bots = ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", "bots");
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(bots, json));
        String future = json.replace("\"format\": 1", "\"format\": 2");
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(SettingsPresets.TEAMS, future));
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(SettingsPresets.TEAMS, "{"));
        String nested = "[".repeat(40) + "]".repeat(40);
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(SettingsPresets.TEAMS, nested));
        String nullValue = json.replace("\"data\": {}", "\"data\": {\"x\": null}");
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(SettingsPresets.TEAMS, nullValue));
        String oversized = "x".repeat(PresetFiles.MAX_BYTES + 1);
        assertThrows(RuntimeException.class, () -> PresetFiles.decode(SettingsPresets.TEAMS, oversized));
        Path huge = directory.resolve("huge.json");
        Files.writeString(huge, "x".repeat(PresetFiles.MAX_BYTES + 1));
        assertThrows(java.io.IOException.class, () -> PresetFiles.read(huge));
        for (String name : new String[]{"../outside", "a/b", "A", "", "a".repeat(33)})
            assertThrows(IllegalArgumentException.class, () -> PresetFiles.checkName(name));
    }
}
