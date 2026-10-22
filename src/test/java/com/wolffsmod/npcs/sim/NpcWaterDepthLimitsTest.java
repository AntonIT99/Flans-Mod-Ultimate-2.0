package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcWaterDepthLimitsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"16,17,false", "17,17,true", "32,33,false", "63,64,false", "64,64,true", "64,65,false"})
    void groundProfilesRejectOnlyColumnsDeeperThanTheirConfiguredAllowance(int allowance, int depth, boolean allowed)
    {
        NpcTuning tuning = tuning(Terrain.GROUND, Value.MAX_WADE_DEPTH, allowance);
        BlockGetter level = water(position -> depth, new AtomicInteger());
        assertEquals(allowed ? PathType.WATER : PathType.BLOCKED, filter(level, tuning, 0.6F));
    }

    @ParameterizedTest
    @CsvSource({"17,16,false", "17,17,true", "64,63,false", "64,64,true", "64,65,true"})
    void watercraftProfilesRequireTheConfiguredMinimum(int minimum, int depth, boolean allowed)
    {
        NpcTuning tuning = tuning(Terrain.WATERCRAFT, Value.MIN_WATER_DEPTH, minimum);
        assertEquals(allowed ? PathType.WATER : PathType.BLOCKED, filter(water(position -> depth, new AtomicInteger()), tuning, 0.6F));
    }

    @Test
    void theEntireHullMustFitAndOneShallowColumnStopsFurtherSearches()
    {
        AtomicInteger reads = new AtomicInteger();
        NpcTuning tuning = tuning(Terrain.WATERCRAFT, Value.MIN_WATER_DEPTH, 64);
        assertEquals(PathType.WATER, filter(water(position -> 1000, reads), tuning, 1F));
        assertEquals(4 * 64, reads.get(), "four hull columns, each stops at the requested depth");
        reads.set(0);
        assertEquals(PathType.BLOCKED, filter(water(position -> 63, reads), tuning, 1F));
        assertEquals(65, reads.get(), "one 63-water column plus its two dry boundaries; other columns are skipped");
        reads.set(0);
        assertEquals(PathType.BLOCKED, filter(water(position -> position.getX() == 1 && position.getZ() == 1 ? 63 : 64, reads), tuning, 1F));
        assertEquals(3 * 64 + 65, reads.get(), "a shallow edge column rejects the hull too");
    }

    @Test
    void groundDepthSixtyFourStopsOnItsFirstTooDeepColumn()
    {
        AtomicInteger reads = new AtomicInteger();
        NpcTuning tuning = tuning(Terrain.GROUND, Value.MAX_WADE_DEPTH, 64);
        assertEquals(PathType.WATER, filter(water(position -> 64, reads), tuning, 1F));
        assertEquals(4 * 66, reads.get(), "valid depth plus both dry boundaries per column");
        reads.set(0);
        assertEquals(PathType.BLOCKED, filter(water(position -> 1000, reads), tuning, 1F));
        assertEquals(65, reads.get(), "the first 65-water column proves the allowance is exceeded");
    }

    @Test
    void defaultsKeepTheirSmallScanBudgetAndOpenAirStaysOpen()
    {
        AtomicInteger reads = new AtomicInteger();
        NpcTuning tuning = new NpcTuning();
        assertEquals(1, tuning.get(Value.MAX_WADE_DEPTH));
        assertEquals(1, tuning.get(Value.MIN_WATER_DEPTH));
        tuning.set(Value.TERRAIN, Terrain.GROUND.ordinal());
        assertEquals(PathType.BLOCKED, filter(water(position -> 1000, reads), tuning, 0.6F));
        assertEquals(2, reads.get());
        reads.set(0);
        tuning.set(Value.TERRAIN, Terrain.WATERCRAFT.ordinal());
        assertEquals(PathType.WATER, filter(water(position -> 1000, reads), tuning, 1F));
        assertEquals(4, reads.get());
        EntityNPCInterface npc = npc(tuning, 1F);
        BlockGetter unused = mock(BlockGetter.class);
        assertEquals(PathType.OPEN, NpcMobility.filter(npc, unused, 0, 0, 0, PathType.OPEN));
        verifyNoInteractions(unused);
    }

    @Test
    void largerDepthSettingsSurviveSaveAndReloadAndAreBoundedAtSixtyFour()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.MAX_WADE_DEPTH, 32);
        tuning.set(Value.MIN_WATER_DEPTH, 64);
        CompoundTag saved = new CompoundTag();
        tuning.save(saved);
        NpcTuning restored = new NpcTuning();
        restored.load(saved);
        assertEquals(32, restored.get(Value.MAX_WADE_DEPTH));
        assertEquals(64, restored.get(Value.MIN_WATER_DEPTH));
        restored.set(Value.MAX_WADE_DEPTH, 100);
        restored.set(Value.MIN_WATER_DEPTH, 100);
        assertEquals(64, restored.get(Value.MAX_WADE_DEPTH));
        assertEquals(64, restored.get(Value.MIN_WATER_DEPTH));
        restored.set(Value.MAX_WADE_DEPTH, -1);
        restored.set(Value.MIN_WATER_DEPTH, -1);
        assertEquals(0, restored.get(Value.MAX_WADE_DEPTH));
        assertEquals(1, restored.get(Value.MIN_WATER_DEPTH));
        restored.load(new CompoundTag());
        assertEquals(1, restored.get(Value.MAX_WADE_DEPTH));
        assertEquals(1, restored.get(Value.MIN_WATER_DEPTH));
    }

    private static NpcTuning tuning(Terrain terrain, Value setting, int depth)
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.TERRAIN, terrain.ordinal());
        tuning.set(setting, depth);
        return tuning;
    }

    private static PathType filter(BlockGetter level, NpcTuning tuning, float width)
    {
        return NpcMobility.filter(npc(tuning, width), level, 0, 0, 0, PathType.WATER);
    }

    private static EntityNPCInterface npc(NpcTuning tuning, float width)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        when(npc.getBbWidth()).thenReturn(width);
        return npc;
    }

    private static BlockGetter water(ToIntFunction<BlockPos> height, AtomicInteger reads)
    {
        BlockGetter level = mock(BlockGetter.class);
        FluidState wet = mock(FluidState.class);
        FluidState dry = mock(FluidState.class);
        when(wet.is(FluidTags.WATER)).thenReturn(true);
        when(level.getFluidState(any())).thenAnswer(call ->
        {
            reads.incrementAndGet();
            BlockPos position = call.getArgument(0);
            return position.getY() >= 0 && position.getY() < height.applyAsInt(position) ? wet : dry;
        });
        return level;
    }
}
