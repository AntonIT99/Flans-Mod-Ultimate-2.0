package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FactionJoiningTest
{
    private static final int NEUTRAL = 500;
    private static final int FRIENDLY = 1500;
    private static final long NEVER_LEFT = Long.MAX_VALUE;

    private static Optional<FactionJoining.Refusal> refusal(FactionJoining.Rule rule, int points)
    {
        return FactionJoining.refusal(rule, false, points, NEUTRAL, FRIENDLY, 0, NEVER_LEFT);
    }

    @Test
    void anOpenFactionTakesAnyoneItIsNotHostileTo()
    {
        assertEquals(Optional.empty(), refusal(FactionJoining.Rule.OPEN, NEUTRAL));
        assertEquals(Optional.of(FactionJoining.Refusal.HATED), refusal(FactionJoining.Rule.OPEN, NEUTRAL - 1));
    }

    @Test
    void aFactionJoinedByPointsTakesOnlyPlayersItIsFriendlyTo()
    {
        assertEquals(Optional.of(FactionJoining.Refusal.NOT_TRUSTED), refusal(FactionJoining.Rule.POINTS, FRIENDLY - 1));
        assertEquals(Optional.empty(), refusal(FactionJoining.Rule.POINTS, FRIENDLY));
        assertEquals(Optional.of(FactionJoining.Refusal.HATED), refusal(FactionJoining.Rule.POINTS, 0));
    }

    @Test
    void invitationOnlyAndHiddenFactionsTakeNobodyByThemselves()
    {
        assertEquals(Optional.of(FactionJoining.Refusal.INVITE), refusal(FactionJoining.Rule.INVITE, FRIENDLY));
        assertEquals(Optional.of(FactionJoining.Refusal.INVITE), FactionJoining.refusal(FactionJoining.Rule.OPEN, true, FRIENDLY, NEUTRAL, FRIENDLY, 0, NEVER_LEFT));
    }

    @Test
    void aPlayerWhoLeftMayRejoinOnlyAfterTheCooldown()
    {
        long cooldown = 10 * FactionJoining.TICKS_PER_MINUTE;
        assertEquals(Optional.of(FactionJoining.Refusal.COOLDOWN), FactionJoining.refusal(FactionJoining.Rule.OPEN, false, FRIENDLY, NEUTRAL, FRIENDLY, 10, cooldown - 1));
        assertEquals(Optional.empty(), FactionJoining.refusal(FactionJoining.Rule.OPEN, false, FRIENDLY, NEUTRAL, FRIENDLY, 10, cooldown));
        assertEquals(Optional.empty(), FactionJoining.refusal(FactionJoining.Rule.OPEN, false, FRIENDLY, NEUTRAL, FRIENDLY, 0, 0));
    }

    @Test
    void savedRuleNamesFallBackToOpen()
    {
        assertEquals(FactionJoining.Rule.POINTS, FactionJoining.Rule.named("POINTS"));
        assertEquals(FactionJoining.Rule.OPEN, FactionJoining.Rule.named(""));
        assertEquals(FactionJoining.Rule.OPEN, FactionJoining.Rule.named("bogus"));
    }
}
