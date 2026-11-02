package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import io.netty.buffer.Unpooled;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcAdvancedTargetSearchTest
{
    private static final Value[] BANDS = {Value.SEARCH_BAND_0_INTERVAL, Value.SEARCH_BAND_1_INTERVAL, Value.SEARCH_BAND_2_INTERVAL, Value.SEARCH_BAND_3_INTERVAL, Value.SEARCH_BAND_4_INTERVAL,
        Value.SEARCH_BAND_5_INTERVAL};

    private record Scan(long tick, int radius)
    {}

    private record Fixture(EntityNPCInterface npc, Level level, NpcTuning tuning, NpcWeaponOptions options, AtomicLong clock, List<Scan> scans, NpcTargetSearch search)
    {
        private void tick(long now, double range)
        {
            clock.set(now);
            search.find(npc, TargetingConditions.forCombat(), range);
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("unchecked") // Minecraft's generic entity-index callback is erased at the Mockito boundary.
    void largeLevelsUseTheLiveSpatialIndexForNearbySearches()
    {
        Fixture fixture = fixture(1);
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getGameTime()).thenReturn(1L);
        when(fixture.npc.level()).thenReturn(level);
        List<Entity> distant = new ArrayList<>();
        for (int entity = 0; entity < 300; entity++)
            distant.add(mock(LivingEntity.class));
        when(level.getAllEntities()).thenReturn(distant);
        LevelEntityGetter<Entity> index = mock(LevelEntityGetter.class);
        when(level.getEntities()).thenReturn(index);
        LivingEntity nearby = mock(LivingEntity.class);
        when(nearby.isAlive()).thenReturn(true);
        when(fixture.npc.distanceToSqr(nearby)).thenReturn(100D);
        doAnswer(call ->
        {
            AbortableIterationConsumer<LivingEntity> visitor = call.getArgument(2);
            visitor.accept(nearby);
            return null;
        }).when(index).get(any(EntityTypeTest.class), any(AABB.class), any());
        TargetingConditions conditions = mock(TargetingConditions.class);
        when(conditions.test(fixture.npc, nearby)).thenReturn(true);
        try
        {
            assertSame(nearby, fixture.search.find(fixture.npc, conditions, 32D));
            verify(index).get(any(EntityTypeTest.class), any(AABB.class), any());
            for (Entity entity : distant)
                verify(fixture.npc, never()).distanceToSqr(entity);
        }
        finally
        {
            NpcCombatants.clear();
        }
    }

    @Test
    void rejectedCandidatesAreStillTestedInPriorityAndDistanceOrder()
    {
        Fixture fixture = fixture(1);
        List<LivingEntity> candidates = new ArrayList<>();
        for (int index = 0; index < 100; index++)
        {
            LivingEntity entity = mock(LivingEntity.class);
            when(entity.isAlive()).thenReturn(true);
            when(entity.getId()).thenReturn(index);
            when(fixture.npc.distanceToSqr(entity)).thenReturn(1000D - index);
            candidates.add(entity);
        }
        when(fixture.level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenReturn(candidates);
        List<LivingEntity> expected = candidates.stream().sorted(Comparator.comparingInt((LivingEntity entity) -> entity.getId() % 7).thenComparingDouble(fixture.npc::distanceToSqr)).toList();
        List<LivingEntity> tested = new ArrayList<>();
        TargetingConditions conditions = mock(TargetingConditions.class);
        when(conditions.test(eq(fixture.npc), any())).thenAnswer(call ->
        {
            LivingEntity entity = call.getArgument(1);
            tested.add(entity);
            return entity == expected.get(expected.size() - 1);
        });
        NpcTargetSearch.setPriority((npc, entity) -> entity.getId() % 7);
        try
        {
            assertSame(expected.get(expected.size() - 1), fixture.search.find(fixture.npc, conditions, 32D));
            assertEquals(expected, tested);
        }
        finally
        {
            NpcTargetSearch.setPriority((npc, entity) -> 0);
        }
    }

    @Test
    void oldSavesKeepSharedNearbyTimingAndTheExistingInterpolatedSchedule()
    {
        Fixture fixture = fixture(1);
        assertFalse(fixture.options.enabled(Feature.ADVANCED_SEARCH_INTERVALS));
        assertFalse(fixture.options.enabled(Feature.STAGGER_NEAR_SEARCH));
        for (Value band : BANDS)
            assertEquals(0, fixture.tuning.get(band));
        for (long now = 0; now <= 120; now++)
            fixture.tick(now, 512);
        assertEquals(List.of(0L, 10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 110L, 120L), ticks(fixture, 32));
        assertCadence(fixture, 64, 20);
        assertCadence(fixture, 128, 30);
        assertCadence(fixture, 256, 40);
        assertCadence(fixture, 384, 50);
        assertCadence(fixture, 512, 60);
    }

    @Test
    void explicitIntervalsAreInactiveUntilEnabledAndCanBeNonMonotonic()
    {
        Fixture fixture = fixture(1);
        int[] intervals = {5, 10, 20, 40, 80, 160};
        for (int band = 0; band < BANDS.length; band++)
        {
            fixture.tuning.set(BANDS[band], intervals[band]);
            assertEquals(10 + band * 10, NpcTargetSearch.interval(band, 10, 60, fixture.tuning, false));
            assertEquals(intervals[band], NpcTargetSearch.interval(band, 10, 60, fixture.tuning, true));
        }
        fixture.tuning.set(Value.SEARCH_BAND_2_INTERVAL, 3);
        fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
        for (long now = 0; now <= 320; now++)
            fixture.tick(now, 512);
        assertCadence(fixture, 32, 5);
        assertCadence(fixture, 64, 10);
        assertCadence(fixture, 128, 3);
        assertCadence(fixture, 256, 40);
        assertCadence(fixture, 384, 80);
        assertCadence(fixture, 512, 160);
    }

    @Test
    void unsetBandsFollowCalculatedEndpointsAndDisablingOverridesRetainsStoredChoices()
    {
        Fixture fixture = fixture(1);
        fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
        fixture.tuning.set(Value.SEARCH_NEAR_INTERVAL, 7);
        fixture.tuning.set(Value.SEARCH_FAR_INTERVAL, 107);
        fixture.tuning.set(Value.SEARCH_BAND_2_INTERVAL, 1200);
        for (long now = 0; now <= 2400; now++)
            fixture.tick(now, 128);
        assertCadence(fixture, 32, 7);
        assertCadence(fixture, 64, 27);
        assertCadence(fixture, 128, 1200);
        fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, false);
        assertEquals(47, NpcTargetSearch.interval(2, 7, 107, fixture.tuning, false));
        assertEquals(1200, fixture.tuning.get(Value.SEARCH_BAND_2_INTERVAL));
    }

    @Test
    void staggeringSpreadsFirstAndLaterNearSearchesWithoutEnablingBandOverrides()
    {
        Fixture first = fixture(1);
        Fixture second = fixture(2);
        first.options.set(Feature.STAGGER_NEAR_SEARCH, true);
        second.options.set(Feature.STAGGER_NEAR_SEARCH, true);
        for (long now = 0; now <= 40; now++)
        {
            first.tick(now, 32);
            second.tick(now, 32);
        }
        assertEquals(List.of(1L, 11L, 21L, 31L), ticks(first, 32));
        assertEquals(List.of(2L, 12L, 22L, 32L), ticks(second, 32));
        assertFalse(first.options.enabled(Feature.ADVANCED_SEARCH_INTERVALS));
        first.options.set(Feature.STAGGER_NEAR_SEARCH, false);
        first.tick(41, 32);
        assertEquals(41L, ticks(first, 32).get(4));
        first.tick(50, 32);
        assertEquals(50L, ticks(first, 32).get(5));
    }

    @Test
    void sharedCadenceStillAlignsNpcsThatStartAtDifferentTimesAndWorksWithOverrides()
    {
        Fixture first = fixture(1);
        Fixture second = fixture(2);
        for (Fixture fixture : List.of(first, second))
        {
            fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
            fixture.tuning.set(Value.SEARCH_BAND_0_INTERVAL, 5);
        }
        first.tick(1, 32);
        second.tick(3, 32);
        first.tick(4, 32);
        second.tick(4, 32);
        first.tick(5, 32);
        second.tick(5, 32);
        assertEquals(List.of(1L, 5L), ticks(first, 32));
        assertEquals(List.of(3L, 5L), ticks(second, 32));
    }

    @Test
    void intervalOneAndModeChangesDoNotOverflowOrStarveTheFirstBand()
    {
        Fixture fixture = fixture(1);
        fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
        fixture.tuning.set(Value.SEARCH_BAND_0_INTERVAL, 1200);
        fixture.tick(0, 32);
        fixture.options.set(Feature.STAGGER_NEAR_SEARCH, true);
        fixture.tuning.set(Value.SEARCH_BAND_0_INTERVAL, 1);
        fixture.tick(1, 32);
        fixture.tick(2, 32);
        assertEquals(List.of(0L, 1L, 2L), ticks(fixture, 32));
    }

    @Test
    void aggroRangeStillCapsTheScannedBandsAndFindingATargetStopsFurtherSearches()
    {
        Fixture fixture = fixture(1);
        fixture.options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
        for (Value band : BANDS)
            fixture.tuning.set(band, 1);
        fixture.tick(0, 50);
        fixture.tick(1, 50);
        assertTrue(fixture.scans.stream().allMatch(scan -> scan.radius <= 50));
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        when(fixture.npc.distanceToSqr(target)).thenReturn(100D);
        when(fixture.level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenAnswer(call ->
        {
            Predicate<LivingEntity> filter = call.getArgument(2);
            assertTrue(filter.test(target));
            return new ArrayList<>(List.of(target));
        });
        TargetingConditions conditions = mock(TargetingConditions.class);
        when(conditions.test(fixture.npc, target)).thenReturn(true);
        fixture.clock.set(2);
        clearInvocations(fixture.level);
        assertSame(target, fixture.search.find(fixture.npc, conditions, 512));
        verify(fixture.level, times(1)).getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any());
        fixture.options.set(Feature.PROGRESSIVE_SEARCH, false);
        assertFalse(NpcTargetSearch.enabled(fixture.npc));
    }

    @Test
    void settingsSurviveNbtTransportAndInvalidValuesAreClampedWithoutChangingOldDefaults()
    {
        NpcTuning tuning = new NpcTuning();
        NpcWeaponOptions options = new NpcWeaponOptions();
        tuning.set(Value.SEARCH_BAND_0_INTERVAL, 5);
        tuning.set(Value.SEARCH_BAND_5_INTERVAL, 1200);
        options.set(Feature.ADVANCED_SEARCH_INTERVALS, true);
        options.set(Feature.STAGGER_NEAR_SEARCH, true);
        CompoundTag root = new CompoundTag();
        tuning.save(root);
        options.save(root);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            buffer.writeNbt(root);
            CompoundTag received = buffer.readNbt();
            assertNotNull(received);
            assertEquals(0, buffer.readableBytes());
            tuning.load(received);
            options.load(received);
        }
        finally
        {
            buffer.release();
        }
        assertEquals(5, tuning.get(Value.SEARCH_BAND_0_INTERVAL));
        assertEquals(1200, tuning.get(Value.SEARCH_BAND_5_INTERVAL));
        assertTrue(options.enabled(Feature.ADVANCED_SEARCH_INTERVALS));
        assertTrue(options.enabled(Feature.STAGGER_NEAR_SEARCH));
        CompoundTag values = root.getCompound(NpcTuning.NBT_KEY);
        values.putInt(Value.SEARCH_BAND_0_INTERVAL.name(), -4);
        values.putInt(Value.SEARCH_BAND_5_INTERVAL.name(), 1201);
        values.putString(Value.SEARCH_BAND_1_INTERVAL.name(), "bad");
        tuning.load(root);
        assertEquals(0, tuning.get(Value.SEARCH_BAND_0_INTERVAL));
        assertEquals(0, tuning.get(Value.SEARCH_BAND_1_INTERVAL));
        assertEquals(1200, tuning.get(Value.SEARCH_BAND_5_INTERVAL));
        tuning.load(new CompoundTag());
        options.load(new CompoundTag());
        assertEquals(0, tuning.get(Value.SEARCH_BAND_5_INTERVAL));
        assertFalse(options.enabled(Feature.ADVANCED_SEARCH_INTERVALS));
        assertFalse(options.enabled(Feature.STAGGER_NEAR_SEARCH));
    }

    private static List<Long> ticks(Fixture fixture, int radius)
    {
        return fixture.scans.stream().filter(scan -> scan.radius == radius).map(Scan::tick).toList();
    }

    private static void assertCadence(Fixture fixture, int radius, int interval)
    {
        List<Long> times = ticks(fixture, radius);
        assertTrue(times.size() >= 2, "at least two searches in radius " + radius);
        for (int index = 1; index < times.size(); index++)
            assertEquals(interval, times.get(index) - times.get(index - 1), "radius " + radius);
    }

    private static Fixture fixture(int id)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcTuning tuning = new NpcTuning();
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(npc.getId()).thenReturn(id);
        when(npc.getBoundingBox()).thenReturn(new AABB(0, 0, 0, 0, 0, 0));
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        AtomicLong clock = new AtomicLong();
        when(level.getGameTime()).thenAnswer(call -> clock.get());
        List<Scan> scans = new ArrayList<>();
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenAnswer(call ->
        {
            AABB box = call.getArgument(1);
            scans.add(new Scan(clock.get(), (int) box.maxX));
            return new ArrayList<LivingEntity>();
        });
        return new Fixture(npc, level, tuning, options, clock, scans, new NpcTargetSearch());
    }
}
