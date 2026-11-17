package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotMinefieldsTest
{
    private static final Vec3 OBJECTIVE = new Vec3(0D, 64D, 0D);
    private static final UUID FIRST = UUID.nameUUIDFromBytes("first".getBytes());
    private static final UUID SECOND = UUID.nameUUIDFromBytes("second".getBytes());
    private static final double RADIUS = 8D;

    private static BotMinefields.Field field(List<Vec3> enemies, List<Vec3> armour, List<Vec3> spawns, List<Vec3> routes, List<Vec3> posts)
    {
        return new BotMinefields.Field(enemies, armour, spawns, routes, posts);
    }

    private static BotMinefields prepared(BotMinefields.Field field, int defenders)
    {
        BotMinefields minefields = new BotMinefields();
        minefields.update(0, 100L, List.of(new BotMinefields.Held(OBJECTIVE, RADIUS, defenders)), field);
        return minefields;
    }

    @Test
    void theBeltLiesAcrossTheReportedApproachAndArmourMinesGoOnTheRoutesLeadingIn()
    {
        BotMinefields minefields = prepared(field(List.of(new Vec3(60D, 64D, 0D)), List.of(), List.of(), List.of(new Vec3(20D, 64D, 0D)), List.of()), 4);
        Vec3 infantry = minefields.claim(0, FIRST, false, OBJECTIVE, 100L).orElseThrow();
        assertTrue(infantry.x > 0D, "the belt lies towards the reported enemy");
        assertEquals(RADIUS + BotMinefields.INFANTRY_STANDOFF, infantry.subtract(OBJECTIVE).horizontalDistance(), 0.001D);
        Vec3 armour = minefields.claim(0, SECOND, true, OBJECTIVE, 100L).orElseThrow();
        assertEquals(18D, armour.x, 0.001D, "the armour mine lies on the route point leading in, a little short of it");
        assertEquals(0D, armour.z, 0.001D);
    }

    @Test
    void theEnemySpawnsStandInForReportsBeforeFirstContactAndNothingAtAllLeavesNoBelt()
    {
        BotMinefields spawns = prepared(field(List.of(), List.of(), List.of(new Vec3(0D, 64D, -120D)), List.of(), List.of()), 4);
        Vec3 spot = spawns.claim(0, FIRST, false, OBJECTIVE, 100L).orElseThrow();
        assertTrue(spot.z < 0D, "the belt faces where the enemy comes into the battle");
        BotMinefields blind = prepared(field(List.of(), List.of(), List.of(), List.of(), List.of()), 4);
        assertTrue(blind.claim(0, FIRST, false, OBJECTIVE, 100L).isEmpty());
    }

    @Test
    void twoSoldiersOfOneSquadSpreadOverTheBeltAndAMinedSpotIsNotOfferedAgain()
    {
        BotMinefields minefields = prepared(field(List.of(new Vec3(60D, 64D, 0D)), List.of(), List.of(), List.of(), List.of()), 6);
        Vec3 first = minefields.claim(0, FIRST, false, OBJECTIVE, 100L).orElseThrow();
        Vec3 second = minefields.claim(0, SECOND, false, OBJECTIVE, 100L).orElseThrow();
        assertTrue(first.distanceTo(second) >= BotMinefields.SPACING, "two soldiers never prepare the same spot");
        // The spot a soldier kept is its own until it gives it up, and stays taken once it is mined.
        assertEquals(first, minefields.claim(0, FIRST, false, OBJECTIVE, 120L).orElseThrow());
        minefields.laid(0, FIRST, first, 120L);
        minefields.release(SECOND);
        Vec3 next = minefields.claim(0, SECOND, false, OBJECTIVE, 140L).orElseThrow();
        assertTrue(next.distanceTo(first) >= BotMinefields.SPACING, "the mined spot is left alone");
    }

    @Test
    void aPostOfTheSideIsNeverMinedAndTheBeltGrowsWithTheSquadHoldingTheObjective()
    {
        Vec3 approach = OBJECTIVE.add(RADIUS + BotMinefields.INFANTRY_STANDOFF, 0D, 0D);
        BotMinefields guarded = prepared(field(List.of(new Vec3(60D, 64D, 0D)), List.of(), List.of(), List.of(), List.of(approach)), 2);
        assertTrue(guarded.claim(0, FIRST, false, OBJECTIVE, 100L).isEmpty(), "a lone defender mines only the approach its own post stands on");
        BotMinefields squad = prepared(field(List.of(new Vec3(60D, 64D, 0D)), List.of(), List.of(), List.of(), List.of(approach)), 8);
        assertTrue(squad.claim(0, FIRST, false, OBJECTIVE, 100L).isPresent(), "a full squad still mines to either side of it");
    }
}
