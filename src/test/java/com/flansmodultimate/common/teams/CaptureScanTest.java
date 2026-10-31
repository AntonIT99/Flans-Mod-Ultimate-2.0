package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaptureScanTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void captureRulesExcludeRidersAndCapSideWeightWithoutRemovingRewardRecipients()
    {
        try (var managers = mockStatic(TeamsManager.class); var combatants = mockStatic(TeamsCombatants.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            RoundRuleRuntime runtime = new RoundRuleRuntime(manager);
            when(manager.getRoundRuleRuntime()).thenReturn(runtime);
            TeamsRound round = new TeamsRound("map", "con", List.of("red", "blue"), 15, 60);
            Team red = mock(Team.class);
            when(red.getShortName()).thenReturn("red");
            ServerLevel level = mock(ServerLevel.class);
            ITeamCapturePoint point = mock(ITeamCapturePoint.class);
            when(point.getTeamObjectPosition()).thenReturn(Vec3.ZERO);
            when(point.getCaptureRadius()).thenReturn(5);
            ServerPlayer rider = mock(ServerPlayer.class);
            ServerPlayer infantry = mock(ServerPlayer.class);
            when(rider.isPassenger()).thenReturn(true);
            when(level.getEntitiesOfClass(eq(LivingEntity.class), any(), any())).thenReturn(List.of(rider, infantry));
            for (ServerPlayer player : List.of(rider, infantry))
            {
                combatants.when(() -> TeamsCombatants.getCaptureWeight(manager, player)).thenReturn(1F);
                combatants.when(() -> TeamsCombatants.getTeam(player)).thenReturn(red);
            }
            assertEquals(2F, CaptureScan.of(point, level, round).count(2));
            runtime.rules().set(RoundRule.CAPTURE_ON_FOOT_ONLY, "true");
            CaptureScan footOnly = CaptureScan.of(point, level, round);
            assertEquals(1F, footOnly.count(2));
            assertEquals(List.of(infantry), footOnly.players(2));
            runtime.rules().set(RoundRule.CAPTURE_ON_FOOT_ONLY, "false");
            runtime.rules().set(RoundRule.MAX_CAPTURE_WEIGHT, "1");
            CaptureScan capped = CaptureScan.of(point, level, round);
            assertEquals(1F, capped.count(2));
            assertEquals(List.of(rider, infantry), capped.players(2));
        }
    }
}
