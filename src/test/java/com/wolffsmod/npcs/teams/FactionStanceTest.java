package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FactionStanceTest
{
    private static final int GERMANY = 1;
    private static final int BRITAIN = 2;
    private static final int ITALY = 3;
    private static final int FRANCE = 4;
    /**
     * Germany and Britain are at war, as declared from Germany's side only; Germany and Italy are allied; France is at
     * peace with everyone.
     */
    private static final FactionStance.Relations RELATIONS = new FactionStance.Relations()
    {
        @Override
        public boolean attacks(int faction, int other)
        {
            return faction == GERMANY && other == BRITAIN;
        }

        @Override
        public boolean allied(int faction, int other)
        {
            return Set.of(faction, other).equals(Set.of(GERMANY, ITALY));
        }
    };

    private static Optional<BotFactions.Stance> stance(int faction, Integer playerFaction)
    {
        return stance(faction, new FactionStance.Standing(playerFaction, false, false));
    }

    private static Optional<BotFactions.Stance> stance(int faction, FactionStance.Standing player)
    {
        return FactionStance.resolve(false, faction, player, RELATIONS, BattleFactions.Outsiders.POINTS);
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
    void membersOfAnAlliedFactionAreFriends()
    {
        assertEquals(Optional.of(BotFactions.Stance.FRIENDLY), stance(GERMANY, ITALY));
        assertEquals(Optional.of(BotFactions.Stance.FRIENDLY), stance(ITALY, GERMANY));
    }

    @Test
    void membersOfAFactionMerelyAtPeaceAreLeftToTheirFactionPoints()
    {
        for (int faction : Set.of(GERMANY, BRITAIN, ITALY))
            assertEquals(Optional.empty(), stance(faction, FRANCE));
    }

    @Test
    void aFactionThatHatesAPlayerByPointsIsHostileWhateverTheirFaction()
    {
        for (int playerFaction : Set.of(GERMANY, ITALY, FRANCE))
            assertEquals(Optional.of(BotFactions.Stance.HOSTILE), stance(GERMANY, new FactionStance.Standing(playerFaction, true, false)));
    }

    @Test
    void outsidersAreNeutralOrLeftToTheirFactionPointsEvenWithAGrudge()
    {
        FactionStance.Standing outsider = new FactionStance.Standing(null, true, false);
        assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), FactionStance.resolve(false, GERMANY, outsider, RELATIONS, BattleFactions.Outsiders.NEUTRAL));
        assertEquals(Optional.empty(), FactionStance.resolve(false, GERMANY, outsider, RELATIONS, BattleFactions.Outsiders.POINTS));
    }

    @Test
    void factionsAtRestAreNeutralToEveryone()
    {
        for (Integer player : new Integer[]{GERMANY, BRITAIN, ITALY, null})
            assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), FactionStance.resolve(true, GERMANY, new FactionStance.Standing(player, true, false), RELATIONS, BattleFactions.Outsiders.POINTS));
    }

    @Test
    void noFactionAttacksAPlayerInCreativeMode()
    {
        assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), stance(GERMANY, new FactionStance.Standing(BRITAIN, false, true)));
        assertEquals(Optional.of(BotFactions.Stance.NEUTRAL), stance(GERMANY, new FactionStance.Standing(GERMANY, true, true)));
    }
}
