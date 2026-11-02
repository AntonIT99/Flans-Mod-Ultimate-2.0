package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.sim.NpcSight;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCoverSchedulingTest
{
    private static final List<Vec3> SOURCES = List.of(new Vec3(20D, 65.6D, 0D));

    private static final class Fixture implements AutoCloseable
    {
        private final EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        private final Level level = mock(Level.class);
        private final MockedStatic<NpcEquipment> equipment = mockStatic(NpcEquipment.class);
        private final MockedStatic<NpcCombatMovement> movement = mockStatic(NpcCombatMovement.class);
        private final MockedStatic<NpcSight> sight = mockStatic(NpcSight.class);
        private long now;
        private int rays;

        private Fixture(boolean visible)
        {
            when(npc.level()).thenReturn(level);
            when(npc.position()).thenReturn(new Vec3(0D, 64D, 0D));
            when(npc.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
            when(npc.getEyeHeight()).thenReturn(1.6F);
            when(npc.getBbHeight()).thenReturn(1.8F);
            when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
            when(level.getGameTime()).thenAnswer(call -> now);
            when(level.isLoaded(any())).thenReturn(true);
            when(level.getBlockState(any())).thenAnswer(call -> ((BlockPos) call.getArgument(0)).getY() < 64 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            movement.when(() -> NpcCombatMovement.permits(eq(npc), any())).thenReturn(true);
            sight.when(() -> NpcSight.clear(eq(level), any(), any(), eq(0), eq(npc))).thenAnswer(call ->
            {
                rays++;
                return visible;
            });
        }

        @Override
        public void close()
        {
            sight.close();
            movement.close();
            equipment.close();
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S8714") // fail bounds a resumable search; the try-with-resources only closes static mocks.
    void aCoverShortlistResumesAcrossTicksAndBoundsEachSlicesRays()
    {
        try (Fixture fixture = new Fixture(false))
        {
            int pending = 0;
            for (int tick = 0; tick < 30; tick++)
            {
                fixture.now = tick;
                int before = fixture.rays;
                NpcCover.Result<List<BlockPos>> result = NpcCover.safePositions(fixture.npc, SOURCES, 8);
                assertTrue(fixture.rays - before <= 128, "one NPC cannot consume an unbounded ray budget");
                if (result.complete())
                {
                    assertNotNull(result.value());
                    assertEquals(6, result.value().size());
                    assertEquals(6, new HashSet<>(result.value()).size());
                    assertTrue(pending > 0);
                    assertTrue(fixture.rays > 128, "the full search must have continued across slices");
                    return;
                }
                assertNull(result.value(), "an incomplete search must not look like an empty shortlist");
                pending++;
            }
            fail("the search lost its progress instead of finishing");
        }
    }

    @Test
    @SuppressWarnings("java:S8714") // fail bounds a resumable search; the try-with-resources only closes static mocks.
    void budgetDeferralIsNotReportedAsNoCoverAndResumesAfterCapacityReturns()
    {
        try (Fixture fixture = new Fixture(false); MockedStatic<NpcBudgets> budgets = mockStatic(NpcBudgets.class))
        {
            MinecraftServer server = mock(MinecraftServer.class);
            when(fixture.npc.getServer()).thenReturn(server);
            assertFalse(NpcCover.safePositions(fixture.npc, SOURCES, 8).complete());
            assertEquals(0, fixture.rays);
            budgets.when(() -> NpcBudgets.coverSearch(eq(server), anyInt(), anyLong(), anyBoolean())).thenReturn(true);
            for (int tick = 1; tick < 30; tick++)
            {
                fixture.now = tick;
                NpcCover.Result<List<BlockPos>> result = NpcCover.safePositions(fixture.npc, SOURCES, 8);
                if (result.complete())
                {
                    assertNotNull(result.value());
                    assertFalse(result.value().isEmpty());
                    return;
                }
            }
            fail("a deferred cover search did not recover");
        }
    }

    @Test
    @SuppressWarnings("java:S8714") // fail bounds a resumable search; the try-with-resources only closes static mocks.
    void firingCoverSearchRetainsItsCandidateLimitAcrossSlicesInOpenGround()
    {
        try (Fixture fixture = new Fixture(true))
        {
            LivingEntity target = mock(LivingEntity.class);
            when(target.position()).thenReturn(new Vec3(20D, 64D, 0D));
            when(target.getEyePosition()).thenReturn(SOURCES.get(0));
            for (int tick = 0; tick < 30; tick++)
            {
                fixture.now = tick;
                int before = fixture.rays;
                NpcCover.Result<NpcCover.Cover> result = NpcCover.find(fixture.npc, target, 8, new Engagement(20D, 64D, 0D));
                assertTrue(fixture.rays - before <= 128);
                if (result.complete())
                {
                    assertNull(result.value());
                    assertEquals(NpcCover.MAX_HIDDEN_TESTS, fixture.rays);
                    assertTrue(tick > 0);
                    return;
                }
            }
            fail("an open-ground search restarted its candidate limit every tick");
        }
    }
}
