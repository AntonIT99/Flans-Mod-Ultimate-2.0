package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class FriendlyFireTest
{
    private static final BooleanSupplier UNREACHED = () -> fail("A setting of its own must not ask the level above");

    @Test
    void aSettingOfItsOwnWinsOverTheLevelAbove()
    {
        assertTrue(FriendlyFire.Setting.ON.or(UNREACHED));
        assertFalse(FriendlyFire.Setting.OFF.or(UNREACHED));
    }

    @Test
    void theDefaultFollowsTheLevelAbove()
    {
        assertTrue(FriendlyFire.Setting.DEFAULT.or(() -> true));
        assertFalse(FriendlyFire.Setting.DEFAULT.or(() -> false));
    }

    @Test
    void aFactionFollowsTheWorldWhichFollowsTheConfiguration()
    {
        // Faction, then world, then configuration, as FriendlyFire.enabled chains them.
        assertTrue(FriendlyFire.Setting.DEFAULT.or(() -> FriendlyFire.Setting.DEFAULT.or(() -> true)));
        assertFalse(FriendlyFire.Setting.DEFAULT.or(() -> FriendlyFire.Setting.OFF.or(() -> true)));
        assertTrue(FriendlyFire.Setting.ON.or(() -> FriendlyFire.Setting.OFF.or(() -> false)));
    }

    @Test
    void settingsSurviveSavingAndLoading()
    {
        Map<FriendlyFire.Kind, FriendlyFire.Setting> saved = new EnumMap<>(FriendlyFire.Kind.class);
        saved.put(FriendlyFire.Kind.NPCS, FriendlyFire.Setting.ON);
        saved.put(FriendlyFire.Kind.PLAYERS, FriendlyFire.Setting.OFF);
        Map<FriendlyFire.Kind, FriendlyFire.Setting> loaded = new EnumMap<>(FriendlyFire.Kind.class);
        FriendlyFire.load(FriendlyFire.save(saved), loaded);
        assertEquals(saved, loaded);
    }

    @Test
    void defaultsAreNotSavedAndLoadAsDefaults()
    {
        Map<FriendlyFire.Kind, FriendlyFire.Setting> saved = new EnumMap<>(FriendlyFire.Kind.class);
        saved.put(FriendlyFire.Kind.NPCS, FriendlyFire.Setting.DEFAULT);
        CompoundTag tag = FriendlyFire.save(saved);
        assertTrue(tag.isEmpty());
        Map<FriendlyFire.Kind, FriendlyFire.Setting> loaded = new EnumMap<>(FriendlyFire.Kind.class);
        loaded.put(FriendlyFire.Kind.PLAYERS, FriendlyFire.Setting.ON);
        FriendlyFire.load(tag, loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void commandWordsAreDistinct()
    {
        assertNotEquals(FriendlyFire.Kind.NPCS.getWord(), FriendlyFire.Kind.PLAYERS.getWord());
        assertEquals(3, Arrays.stream(FriendlyFire.Setting.values()).map(FriendlyFire.Setting::getWord).distinct().count());
    }
}
