package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class FactionStanceTest
{
    private static final int GERMANY = 1;
    private static final int BRITAIN = 2;
    private static final int ITALY = 3;
    /** Germany and Britain are at war, as declared from Germany's side only; Italy is at peace with both. */
    private static final BiPredicate<Integer, Integer> WAR = (faction, other) -> faction == GERMANY && other == BRITAIN;

    private static Optional<BotFactions.Stance> stance(int faction, Integer playerFaction)
    {
        return FactionStance.resolve(false, faction, playerFaction, WAR, BattleFactions.Outsiders.NEUTRAL, false);
    }

    @Test
    void membersAreFriendsOfTheirOwnFaction()
    {
        assertEquals(Optional.of(BotFactions.Stance.FRIENDLY), stance(GERMANY, GERMANY));
    }

    @Test
    void membersOfAFactionAtWarAreEnemiesWhicheverSideDeclaredIt()
    {
        assertEquals(Optional.of(BotFactions.Stance.HOSTILE), stance(GERMANY, BRITAIN));
        assertEquals(Optional.of(BotFactions.Stance.HOSTILE), stance(BRITAIN, GERMANY));
    }

    @Test
    void membersOfAFactionAtPeaceAreFriends()
    {
        for (int faction : Set.of(GERMANY, BRITAIN))
            assertEquals(Optional.of(BotFactions.Stance.FRIENDLY), stance(faction, ITALY));
    }

    @Test
    void outsidersAreNeutralOrLeftToTheirFactionPoints()
    {
        assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), stance(GERMANY, null));
        assertEquals(Optional.empty(), FactionStance.resolve(false, GERMANY, null, WAR, BattleFactions.Outsiders.POINTS, false));
    }

    @Test
    void factionsAtRestAreNeutralToEveryone()
    {
        for (Integer player : new Integer[]{GERMANY, BRITAIN, ITALY, null})
            assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), FactionStance.resolve(true, GERMANY, player, WAR, BattleFactions.Outsiders.POINTS, false));
    }

    @Test
    void noFactionAttacksAPlayerInCreativeMode()
    {
        assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), FactionStance.resolve(false, GERMANY, BRITAIN, WAR, BattleFactions.Outsiders.NEUTRAL, true));
    }
}
