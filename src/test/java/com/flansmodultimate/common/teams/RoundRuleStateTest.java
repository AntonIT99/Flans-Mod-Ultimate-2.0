package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoundRuleStateTest
{
    @Test
    void respawnsMeetOnSharedWaveBoundariesAndRespectMinimumDelays()
    {
        assertEquals(300, RoundRuleState.nextWaveTick(1, 15));
        assertEquals(300, RoundRuleState.nextWaveTick(299, 15));
        assertEquals(300, RoundRuleState.nextWaveTick(300, 15));
        assertEquals(600, RoundRuleState.nextWaveTick(301, 15));
        assertEquals(301, RoundRuleState.nextWaveTick(301, 0));
    }

    @Test
    void limitedLivesAndPendingWavesSurviveRestartAndReconnect()
    {
        RoundRules rules = new RoundRules();
        rules.set(RoundRule.PLAYER_LIVES, "2");
        RoundRuleState state = new RoundRuleState(rules);
        UUID id = UUID.randomUUID();
        RoundRuleState.Participant player = state.player(id);
        player.setDeaths(1);
        player.setSide(2);
        player.setSwitchTick(60);
        player.setDeathTick(120);
        player.setPendingTick(300);
        player.setPendingReason(ITeamsRespawnRule.Reason.DIED);
        RoundRuleState restored = RoundRuleState.load(state.save());
        assertFalse(restored.exhausted(id));
        assertEquals(300, restored.player(id).getPendingTick());
        assertEquals(60, restored.player(id).getSwitchTick());
        assertEquals(120, restored.player(id).getDeathTick());
        assertEquals(2, restored.player(id).getSide());
        restored.player(id).setDeaths(2);
        assertTrue(RoundRuleState.load(restored.save()).exhausted(id));
        assertFalse(new RoundRuleState(rules).exhausted(id));
    }

    @Test
    void warmupAndOvertimeCapsDoNotRestartAfterServerReload()
    {
        RoundRules rules = new RoundRules();
        rules.set(RoundRule.WARMUP_SECONDS, "10");
        rules.set(RoundRule.OVERTIME_SECONDS, "30");
        RoundRuleState state = new RoundRuleState(rules);
        assertTrue(state.isStaging());
        assertEquals(200, state.getWarmupTicks());
        state.setWarmupTicks(45);
        state.setOvertime(true);
        state.setOvertimeTicks(150);
        RoundRuleState restored = RoundRuleState.load(state.save());
        assertEquals(45, restored.getWarmupTicks());
        assertEquals(150, restored.getOvertimeTicks());
        assertTrue(restored.isOvertime());
    }
}
