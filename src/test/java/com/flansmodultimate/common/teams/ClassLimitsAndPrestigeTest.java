package com.flansmodultimate.common.teams;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClassLimitsAndPrestigeTest
{
    @Test
    void aClassWithoutLimitsIsUnlimited()
    {
        assertEquals(ClassLimits.UNLIMITED, ClassLimits.capacity(-1, -1F, 12));
        assertTrue(ClassLimits.hasSlot(ClassLimits.UNLIMITED, 1000));
    }

    @Test
    void aFixedLimitIsTheNumberOfSlots()
    {
        assertEquals(2, ClassLimits.capacity(2, -1F, 12));
        assertTrue(ClassLimits.hasSlot(2, 1));
        assertFalse(ClassLimits.hasSlot(2, 2));
        assertFalse(ClassLimits.hasSlot(0, 0));
    }

    @Test
    void aPercentageIsRoundedUpSoASmallTeamStillGetsASlot()
    {
        assertEquals(1, ClassLimits.capacity(-1, 10F, 1));
        assertEquals(1, ClassLimits.capacity(-1, 10F, 10));
        assertEquals(2, ClassLimits.capacity(-1, 10F, 11));
        assertEquals(0, ClassLimits.capacity(-1, 0F, 10));
        assertEquals(0, ClassLimits.capacity(-1, 50F, 0));
    }

    @Test
    void theSmallerOfBothLimitsWins()
    {
        assertEquals(1, ClassLimits.capacity(1, 50F, 10));
        assertEquals(2, ClassLimits.capacity(5, 20F, 10));
    }

    @Test
    void prestigeNeedsTheRequiredRankAndStartsTheRankOver()
    {
        PlayerStats stats = new PlayerStats(UUID.randomUUID(), "player");
        assertFalse(stats.gainPrestige(PlayerStats.DEFAULT_PRESTIGE_RANK));

        stats.setRank(PlayerStats.DEFAULT_PRESTIGE_RANK);
        stats.addExperience(500);
        int totalBefore = stats.getTotalExperience();
        assertTrue(stats.gainPrestige(PlayerStats.DEFAULT_PRESTIGE_RANK));
        assertEquals(1, stats.getPrestige());
        assertEquals(1, stats.getRank());
        assertEquals(0, stats.getExperience());
        assertEquals(totalBefore, stats.getTotalExperience());
    }

    @Test
    void prestigeStopsAtItsMaximumAndIsSaved()
    {
        PlayerStats stats = new PlayerStats(UUID.randomUUID(), "player");
        for (int i = 0; i < PlayerStats.MAX_PRESTIGE; i++)
        {
            stats.setRank(PlayerStats.DEFAULT_PRESTIGE_RANK);
            assertTrue(stats.gainPrestige(PlayerStats.DEFAULT_PRESTIGE_RANK));
        }
        stats.setRank(PlayerStats.DEFAULT_PRESTIGE_RANK);
        assertFalse(stats.gainPrestige(PlayerStats.DEFAULT_PRESTIGE_RANK));
        assertEquals(PlayerStats.DEFAULT_PRESTIGE_RANK, stats.getRank());

        PlayerStats loaded = PlayerStats.load(stats.save(null), null);
        assertEquals(PlayerStats.MAX_PRESTIGE, loaded.getPrestige());

        stats.resetRankProgress();
        assertEquals(0, stats.getPrestige());
    }

    @Test
    void anObjectiveSnapshotSurvivesTheNetwork()
    {
        ObjectiveSnapshot snapshot = new ObjectiveSnapshot(UUID.randomUUID(), 'C', "Church", 3, 2, 0.42F, true, true, 24, 10.5D, 64D, -30.5D);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        snapshot.write(buffer);
        assertEquals(snapshot, ObjectiveSnapshot.read(buffer));
        assertEquals(0, buffer.readableBytes());
        assertEquals(new CaptureState(3, 2, 0.42F, true), snapshot.state());
    }

    @Test
    void onlyConquestAndAssaultUseTheDeploymentMap()
    {
        assertFalse(ObjectiveMode.NONE.usesDeployment());
        assertFalse(ObjectiveMode.DOMINATION.usesDeployment());
        assertTrue(ObjectiveMode.CONQUEST.usesDeployment());
        assertTrue(ObjectiveMode.ASSAULT.usesDeployment());
        assertEquals(ObjectiveMode.NONE, ObjectiveMode.byOrdinal(99));
        assertEquals(ObjectiveMode.ASSAULT, ObjectiveMode.byOrdinal(ObjectiveMode.ASSAULT.ordinal()));
    }
}
