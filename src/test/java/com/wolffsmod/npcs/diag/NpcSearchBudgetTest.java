package com.wolffsmod.npcs.diag;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.NpcsCommonConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.server.MinecraftServer;

import static com.wolffsmod.npcs.diag.NpcBudgets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcSearchBudgetTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void coverPlanningHasSeparateBoundedUrgentAndOverdueCapacity()
    {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getAverageTickTime()).thenReturn(10F);
        clear();
        try (var config = mockStatic(NpcsCommonConfig.class))
        {
            config.when(NpcsCommonConfig::budgetTargetMillisPerTick).thenReturn(45D);
            assertTrue(coverSearch(server, 2048, 0L, false));
            assertFalse(coverSearch(server, 1, 0L, false));
            assertTrue(coverSearch(server, 512, 0L, true));
            assertFalse(coverSearch(server, 1, MAX_WAIT - 1L, true));
            assertTrue(coverSearch(server, 256, MAX_WAIT, false));
            assertFalse(coverSearch(server, 1, MAX_WAIT, true));
            when(server.getTickCount()).thenReturn(1);
            assertTrue(coverSearch(server, 2048, 0L, false));
        }
        finally
        {
            clear();
        }
    }

    @Test
    void overdueSearchesShareABoundedAllowanceInsteadOfAllBypassingTheLimit()
    {
        assertFalse(overdue(MAX_WAIT - 1L, 0, 100));
        assertTrue(overdue(MAX_WAIT, 0, 100));
        assertTrue(overdue(MAX_WAIT * 10L, 24, 100));
        assertFalse(overdue(MAX_WAIT * 10L, 25, 100));
        assertTrue(overdue(MAX_WAIT, 0, 1));
        assertFalse(overdue(MAX_WAIT, 1, 1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sustainedOverloadServesEveryNpcEvenWithAFixedIterationOrder(boolean paths)
    {
        MinecraftServer server = mock(MinecraftServer.class);
        NpcServerSettings settings = mock(NpcServerSettings.class);
        when(settings.get(any(NpcServerSettings.Setting.class))).thenReturn(1);
        long[] waitingSince = new long[64];
        int[] completed = new int[64];
        clear();
        try (var config = mockStatic(NpcsCommonConfig.class); var stored = mockStatic(NpcServerSettings.class))
        {
            config.when(NpcsCommonConfig::budgetTargetMillisPerTick).thenReturn(0D);
            stored.when(() -> NpcServerSettings.get(server)).thenReturn(settings);
            for (int tick = 0; tick < 200; tick++)
            {
                when(server.getTickCount()).thenReturn(tick);
                int started = 0;
                for (int npc = 0; npc < waitingSince.length; npc++)
                {
                    long waited = tick - waitingSince[npc];
                    if (paths ? pathSearch(server, waited) : targetSearch(server, waited))
                    {
                        completed[npc]++;
                        waitingSince[npc] = tick;
                        started++;
                    }
                }
                assertTrue(started <= 2, "overdue requests must respect the bounded extra allowance");
            }
            for (int npc = 0; npc < completed.length; npc++)
                assertTrue(completed[npc] > 0, "NPC " + npc + " starved behind earlier requests");
        }
        finally
        {
            clear();
        }
    }

    @Test
    void anAllowanceAtItsCeilingMeansNoLimit()
    {
        assertEquals(0, limitOf(MAX_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES));
        assertEquals(0, limitOf(MAX_AUTO_TARGET_SEARCHES + 1D, MAX_AUTO_TARGET_SEARCHES));
        assertEquals(100, limitOf(100.9D, MAX_AUTO_TARGET_SEARCHES));
    }

    @Test
    void slowTicksCutTheAllowanceBackAndFastOnesLetItGrow()
    {
        double over = steer(100D, 60D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertTrue(over < 100D, "a tick over the target must cut the allowance back");
        double under = steer(100D, 30D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertTrue(under > 100D, "a tick under the target must let the allowance grow");
        // Growth is gentler than the cutback, so a server at its limit settles rather than oscillating widely.
        assertTrue(under - 100D < 100D - over);
    }

    @Test
    void theAllowanceNeverStarvesEveryNpcHoweverLongTheTicksAre()
    {
        double allowance = MAX_AUTO_TARGET_SEARCHES;
        for (int tick = 0; tick < 1000; tick++)
            allowance = steer(allowance, 200D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertEquals(MIN_AUTO_TARGET_SEARCHES, allowance);
        assertEquals(MIN_AUTO_TARGET_SEARCHES, limitOf(allowance, MAX_AUTO_TARGET_SEARCHES));
    }

    @Test
    void theAllowanceClimbsBackToNoLimitOnceTheServerRecovers()
    {
        double allowance = MIN_AUTO_PATH_SEARCHES;
        for (int tick = 0; tick < 1000; tick++)
            allowance = steer(allowance, 10D, 45D, MIN_AUTO_PATH_SEARCHES, MAX_AUTO_PATH_SEARCHES);
        assertEquals(MAX_AUTO_PATH_SEARCHES, allowance);
        assertEquals(0, limitOf(allowance, MAX_AUTO_PATH_SEARCHES));
    }
}
