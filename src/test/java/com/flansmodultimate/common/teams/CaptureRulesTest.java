package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaptureRulesTest
{
    private static final int FIRST = 2;
    private static final int SECOND = 3;
    private static final int TICKS = 10;

    private static CaptureRules.Result repeat(CaptureState state, int ticks, java.util.function.Function<CaptureState, CaptureRules.Result> rule)
    {
        CaptureRules.Result result = new CaptureRules.Result(state, CaptureRules.Event.NONE);
        for (int i = 0; i < ticks; i++)
            result = rule.apply(result.state());
        return result;
    }

    @Test
    void onePlayerTakesANeutralPointInCaptureTicks()
    {
        CaptureRules.Result almost = repeat(CaptureState.NEUTRAL, TICKS - 2, state -> CaptureRules.domination(state, FIRST, 1, false, TICKS));
        assertEquals(0, almost.state().ownerTeamId());
        assertEquals(FIRST, almost.state().capturingTeamId());

        CaptureRules.Result done = repeat(almost.state(), 3, state -> CaptureRules.domination(state, FIRST, 1, false, TICKS));
        assertTrue(done.state().isHeldBy(FIRST));
        assertEquals(0, done.state().capturingTeamId());
    }

    @Test
    void theTickThatCompletesACaptureReportsIt()
    {
        CaptureState nearly = new CaptureState(0, FIRST, 0.95F, false);
        CaptureRules.Result result = CaptureRules.domination(nearly, FIRST, 1, false, TICKS);
        assertEquals(CaptureRules.Event.CAPTURED, result.event());
        assertEquals(FIRST, result.state().ownerTeamId());
    }

    @Test
    void morePlayersCaptureFasterInDomination()
    {
        CaptureRules.Result result = repeat(CaptureState.NEUTRAL, TICKS / 2, state -> CaptureRules.domination(state, FIRST, 2, false, TICKS));
        assertTrue(result.state().isHeldBy(FIRST));
    }

    @Test
    void anEnemyOnThePointFreezesDomination()
    {
        CaptureState state = new CaptureState(0, FIRST, 0.5F, false);
        CaptureRules.Result result = CaptureRules.domination(state, 0, 0, true, TICKS);
        assertEquals(0.5F, result.state().progress());
        assertTrue(result.state().contested());
        assertEquals(CaptureRules.Event.NONE, result.event());
    }

    @Test
    void anOwnedPointIsNeutralizedBeforeItCanBeTaken()
    {
        CaptureRules.Result result = new CaptureRules.Result(CaptureState.ownedBy(FIRST), CaptureRules.Event.NONE);
        int ticks = 0;
        while (result.event() != CaptureRules.Event.NEUTRALIZED && ticks++ <= TICKS)
            result = CaptureRules.domination(result.state(), SECOND, 1, false, TICKS);
        assertEquals(CaptureRules.Event.NEUTRALIZED, result.event());
        assertTrue(ticks >= TICKS - 1);
        assertEquals(0, result.state().ownerTeamId());
        assertEquals(0F, result.state().progress());
        assertEquals(SECOND, result.state().capturingTeamId());

        CaptureRules.Result first = CaptureRules.domination(CaptureState.ownedBy(FIRST), SECOND, TICKS, false, TICKS);
        assertEquals(CaptureRules.Event.NEUTRALIZED, first.event());
    }

    @Test
    void anAbandonedCaptureDrainsAwayAndAnAbandonedAttackIsRepaired()
    {
        CaptureState partial = new CaptureState(0, FIRST, 0.15F, false);
        CaptureState drained = CaptureRules.decay(CaptureRules.decay(partial, TICKS), TICKS);
        assertEquals(CaptureState.NEUTRAL, drained);

        CaptureState damaged = new CaptureState(FIRST, SECOND, 0.85F, false);
        CaptureState repaired = CaptureRules.decay(CaptureRules.decay(damaged, TICKS), TICKS);
        assertEquals(CaptureState.ownedBy(FIRST), repaired);
    }

    @Test
    void aSettledPointDoesNotDecay()
    {
        assertEquals(CaptureState.ownedBy(FIRST), CaptureRules.decay(CaptureState.ownedBy(FIRST), TICKS));
        assertEquals(CaptureState.NEUTRAL, CaptureRules.decay(CaptureState.NEUTRAL, TICKS));
    }

    @Test
    void conquestMovesAtThePaceOfTheHeadCountAdvantage()
    {
        CaptureRules.Result result = CaptureRules.pressure(CaptureState.NEUTRAL, FIRST, 3, SECOND, 1, TICKS);
        assertEquals(FIRST, result.state().capturingTeamId());
        assertEquals(0.2F, result.state().progress(), 1.0E-6F);
        assertTrue(result.state().contested());
    }

    @Test
    void equalNumbersHoldAConquestPointStill()
    {
        CaptureState state = new CaptureState(0, SECOND, 0.4F, false);
        CaptureRules.Result result = CaptureRules.pressure(state, FIRST, 2, SECOND, 2, TICKS);
        assertEquals(0.4F, result.state().progress());
        assertEquals(SECOND, result.state().capturingTeamId());
        assertTrue(result.state().contested());
    }

    @Test
    void aRivalCaptureIsUndoneBeforeTheNewOneStarts()
    {
        CaptureState rival = new CaptureState(0, SECOND, 0.1F, false);
        CaptureRules.Result undone = CaptureRules.pressure(rival, FIRST, 1, SECOND, 0, TICKS);
        assertEquals(0F, undone.state().progress());
        assertEquals(FIRST, undone.state().capturingTeamId());
        assertEquals(0, undone.state().ownerTeamId());

        CaptureRules.Result started = CaptureRules.pressure(undone.state(), FIRST, 1, SECOND, 0, TICKS);
        assertEquals(0.1F, started.state().progress(), 1.0E-6F);
    }

    @Test
    void anEmptyConquestPointDecays()
    {
        CaptureState partial = new CaptureState(0, FIRST, 0.5F, true);
        CaptureRules.Result result = CaptureRules.pressure(partial, FIRST, 0, SECOND, 0, TICKS);
        assertEquals(0.4F, result.state().progress(), 1.0E-6F);
        assertFalse(result.state().contested());
    }

    @Test
    void assaultDefendersRestoreTheirOwnPointAtOnce()
    {
        CaptureState damaged = new CaptureState(FIRST, SECOND, 0.2F, false);
        CaptureRules.Result result = CaptureRules.assault(damaged, SECOND, 1, FIRST, 2, TICKS);
        assertEquals(CaptureState.ownedBy(FIRST).progress(), result.state().progress());
        assertEquals(FIRST, result.state().ownerTeamId());
        assertEquals(0, result.state().capturingTeamId());
    }

    @Test
    void assaultAttackersNeutralizeThenCapture()
    {
        CaptureRules.Result neutralized = repeat(CaptureState.ownedBy(FIRST), TICKS + 1, state -> CaptureRules.assault(state, SECOND, 1, FIRST, 0, TICKS));
        assertEquals(0, neutralized.state().ownerTeamId());

        CaptureRules.Result captured = repeat(neutralized.state(), TICKS + 1, state -> CaptureRules.assault(state, SECOND, 1, FIRST, 0, TICKS));
        assertTrue(captured.state().isHeldBy(SECOND));
    }

    @Test
    void assaultDefendersHaveToRetakeANeutralizedPoint()
    {
        CaptureRules.Result result = CaptureRules.assault(CaptureState.NEUTRAL, SECOND, 0, FIRST, TICKS, TICKS);
        assertEquals(CaptureRules.Event.CAPTURED, result.event());
        assertTrue(result.state().isHeldBy(FIRST));
    }

    @Test
    void withSeveralSidesOnAPointTheStrongestAdvancesByItsLeadOverTheNextOne()
    {
        int third = 4;
        CaptureRules.Result result = CaptureRules.pressure(CaptureState.NEUTRAL, java.util.Map.of(FIRST, 2, SECOND, 1, third, 4), TICKS);
        assertEquals(third, result.state().capturingTeamId());
        assertEquals(2F / TICKS, result.state().progress(), 1.0E-6F);
        assertTrue(result.state().contested());
    }

    @Test
    void aSharedLeadBetweenSeveralSidesHoldsThePointStill()
    {
        CaptureState state = new CaptureState(0, FIRST, 0.4F, false);
        CaptureRules.Result result = CaptureRules.pressure(state, java.util.Map.of(FIRST, 1, SECOND, 3, 4, 3), TICKS);
        assertEquals(0.4F, result.state().progress());
        assertEquals(FIRST, result.state().capturingTeamId());
        assertTrue(result.state().contested());
    }

    @Test
    void theSeveralSidesRuleMatchesTheTwoSidedOne()
    {
        CaptureRules.Result pair = CaptureRules.pressure(CaptureState.NEUTRAL, FIRST, 3, SECOND, 1, TICKS);
        CaptureRules.Result many = CaptureRules.pressure(CaptureState.NEUTRAL, java.util.Map.of(FIRST, 3, SECOND, 1), TICKS);
        assertEquals(pair, many);
        assertEquals(CaptureRules.pressure(CaptureState.NEUTRAL, FIRST, 0, SECOND, 0, TICKS), CaptureRules.pressure(CaptureState.NEUTRAL, java.util.Map.of(), TICKS));
    }

    @Test
    void theActiveAssaultObjectiveIsTheFirstOneNotHeldByTheAttackers()
    {
        CaptureState taken = CaptureState.ownedBy(SECOND);
        CaptureState defended = CaptureState.ownedBy(FIRST);
        CaptureState falling = new CaptureState(SECOND, FIRST, 0.5F, false);
        assertEquals(0, GameTypeAssault.activeObjectiveIndex(List.of(defended, defended), SECOND));
        assertEquals(1, GameTypeAssault.activeObjectiveIndex(List.of(taken, defended), SECOND));
        assertEquals(1, GameTypeAssault.activeObjectiveIndex(List.of(taken, falling, defended), SECOND));
        assertEquals(2, GameTypeAssault.activeObjectiveIndex(List.of(taken, taken), SECOND));
        assertEquals(0, GameTypeAssault.activeObjectiveIndex(List.of(), SECOND));
    }

    @Test
    void captureStateClampsWhatItIsGiven()
    {
        CaptureState state = new CaptureState(-1, -4, 3F, false);
        assertEquals(0, state.ownerTeamId());
        assertEquals(0, state.capturingTeamId());
        assertEquals(1F, state.progress());
        assertEquals(0F, new CaptureState(0, 0, Float.NaN, false).progress());
        assertFalse(new CaptureState(FIRST, 0, 0.99F, false).isHeldBy(FIRST));
        assertEquals(SECOND, new CaptureState(0, SECOND, 0.3F, false).displayTeamId());
    }
}
