package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void anAllyFriendlyFireSparesIsNoReasonToHoldFire() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        LivingEntity ally = mock(LivingEntity.class);
        LivingEntity npc = mock(LivingEntity.class);
        Level level = mock(Level.class);
        when(ally.level()).thenReturn(level);
        try (var teams = mockStatic(FlansTeams.class))
        {
            teams.when(() -> FlansTeams.mayHarm(ally, npc)).thenReturn(Optional.of(false));
            assertFalse(FriendlyFire.harms(ally, npc), "a hit that is held back cannot hurt the ally");
            teams.when(() -> FlansTeams.mayHarm(ally, npc)).thenReturn(Optional.of(true));
            assertTrue(FriendlyFire.harms(ally, npc), "a hit that lands still has to be kept off the ally");
        }
    }

    @Test
    void whatFriendlyFireDoesNotCoverIsAlwaysTakenAsHurt() throws ReflectiveOperationException
    {
        // A driveable is no living entity, so no rule spares it and a blast still counts against it.
        EntityTestBootstrap.initialize();
        assertTrue(FriendlyFire.harms(mock(Entity.class), mock(LivingEntity.class)));
    }

    @Test
    void commandWordsAreDistinct()
    {
        assertNotEquals(FriendlyFire.Kind.NPCS.getWord(), FriendlyFire.Kind.PLAYERS.getWord());
        assertEquals(3, Arrays.stream(FriendlyFire.Setting.values()).map(FriendlyFire.Setting::getWord).distinct().count());
    }
}
