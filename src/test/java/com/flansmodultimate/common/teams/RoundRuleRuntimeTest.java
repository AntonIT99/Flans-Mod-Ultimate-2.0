package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoundRuleRuntimeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void deathsCannotBypassWavesThroughJoiningOrARepeatedSpawnRequest()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            Fixture fixture = new Fixture();
            managers.when(TeamsManager::getInstance).thenReturn(fixture.manager);
            fixture.rules.set(RoundRule.RESPAWN_WAVE_SECONDS, "15");
            fixture.start();
            fixture.runtime.died(fixture.player);
            // A join request after briefly choosing spectators still has to obey the death's wave.
            assertTrue(fixture.runtime.holdRespawn(fixture.player, ITeamsRespawnRule.Reason.JOINED));
            verify(fixture.player).setGameMode(GameType.SPECTATOR);
            assertEquals(300, fixture.playerState().getInt("pending_tick"));
            when(fixture.manager.getRoundElapsedTicks()).thenReturn(250);
            assertTrue(fixture.runtime.holdRespawn(fixture.player, null));
            assertEquals(300, fixture.playerState().getInt("pending_tick"));
            RoundRuleRuntime restored = new RoundRuleRuntime(fixture.manager);
            restored.load(fixture.runtime.save());
            assertTrue(restored.holdRespawn(fixture.player, ITeamsRespawnRule.Reason.JOINED));
        }
    }

    @Test
    void reconnectingBeforeRequestingRespawnQueuesTheDeathInsteadOfLeavingThePlayerStranded()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            Fixture fixture = new Fixture();
            managers.when(TeamsManager::getInstance).thenReturn(fixture.manager);
            fixture.rules.set(RoundRule.RESPAWN_WAVE_SECONDS, "15");
            fixture.start();
            fixture.runtime.died(fixture.player);
            fixture.runtime.loggedIn(fixture.player);
            assertEquals(300, fixture.playerState().getInt("pending_tick"));
            verify(fixture.player).setGameMode(GameType.SPECTATOR);
        }
    }

    @Test
    void exhaustedPlayersCannotReturnThroughOperatorOrTeamSelectionSpawnPaths()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            Fixture fixture = new Fixture();
            managers.when(TeamsManager::getInstance).thenReturn(fixture.manager);
            fixture.rules.set(RoundRule.PLAYER_LIVES, "1");
            fixture.start();
            fixture.runtime.died(fixture.player);
            assertTrue(fixture.runtime.exhausted(fixture.player));
            assertTrue(fixture.runtime.holdRespawn(fixture.player, ITeamsRespawnRule.Reason.JOINED));
            assertTrue(fixture.runtime.holdRespawn(fixture.player, null));
            fixture.runtime.load(fixture.runtime.save());
            assertTrue(fixture.runtime.exhausted(fixture.player));
        }
    }

    @Test
    void vehicleSlotsSurviveUnloadingAndReplacementWaitsForTheDestroyedSlotsCooldown()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.AA_LIMIT, "1");
        fixture.rules.set(RoundRule.VEHICLE_REPLACEMENT_SECONDS, "10");
        fixture.start();
        AAGun first = fixture.vehicle();
        AAGun second = fixture.vehicle();
        RoundVehicleLimits limits = fixture.runtime.getVehicles();
        assertTrue(limits.prepare(first, fixture.team));
        assertFalse(limits.prepare(second, fixture.team));
        when(first.getRemovalReason()).thenReturn(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        limits.left(first);
        limits.load(limits.save());
        assertFalse(limits.prepare(second, fixture.team));
        when(first.getRemovalReason()).thenReturn(Entity.RemovalReason.KILLED);
        limits.left(first);
        when(fixture.manager.getRoundElapsedTicks()).thenReturn(199);
        assertFalse(limits.prepare(second, fixture.team));
        when(fixture.manager.getRoundElapsedTicks()).thenReturn(200);
        assertTrue(limits.prepare(second, fixture.team));
    }

    @Test
    void zeroVehicleLimitDisablesTheCategoryAndFailedPlacementReturnsItsReservation()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.AA_LIMIT, "0");
        fixture.start();
        assertFalse(fixture.runtime.getVehicles().prepare(fixture.vehicle(), fixture.team));
        fixture.rules.set(RoundRule.AA_LIMIT, "1");
        fixture.start();
        AAGun first = fixture.vehicle();
        assertTrue(fixture.runtime.getVehicles().prepare(first, fixture.team));
        fixture.runtime.getVehicles().placementFailed(first);
        assertTrue(fixture.runtime.getVehicles().prepare(fixture.vehicle(), fixture.team));
    }

    @Test
    void flagOvertimeEndsWhenTheFlagIsDroppedAndNeverExceedsItsSavedCap()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.OVERTIME_SECONDS, "1");
        fixture.start();
        when(fixture.manager.getCurrentGameType()).thenReturn(Optional.of(mock(GameTypeCTF.class)));
        Flag flag = mock(Flag.class);
        when(fixture.manager.getFlags()).thenReturn(List.of(flag));
        assertFalse(fixture.runtime.extendAtTimeLimit());
        when(flag.hasCarrier()).thenReturn(true);
        assertTrue(fixture.runtime.extendAtTimeLimit());
        fixture.runtime.load(fixture.runtime.save());
        for (int tick = 1; tick < 20; tick++)
            assertTrue(fixture.runtime.extendAtTimeLimit());
        assertFalse(fixture.runtime.extendAtTimeLimit());
        fixture.start();
        assertTrue(fixture.runtime.extendAtTimeLimit());
        when(flag.hasCarrier()).thenReturn(false);
        assertFalse(fixture.runtime.extendAtTimeLimit());
    }

    @Test
    void assaultOnlyExtendsForTheActiveObjective()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.OVERTIME_SECONDS, "30");
        fixture.start();
        when(fixture.manager.getCurrentGameType()).thenReturn(Optional.of(GameTypes.ASSAULT));
        ITeamCapturePoint behind = capturePoint(CaptureState.ownedBy(3));
        ITeamCapturePoint active = capturePoint(CaptureState.ownedBy(2));
        when(fixture.manager.getCapturePoints()).thenReturn(List.of(behind, active));
        CaptureScan behindScan = mock(CaptureScan.class);
        CaptureScan activeScan = mock(CaptureScan.class);
        when(behindScan.isContested()).thenReturn(true);
        try (var scans = mockStatic(CaptureScan.class))
        {
            scans.when(() -> CaptureScan.of(behind, fixture.level, fixture.round)).thenReturn(behindScan);
            scans.when(() -> CaptureScan.of(active, fixture.level, fixture.round)).thenReturn(activeScan);
            assertFalse(fixture.runtime.extendAtTimeLimit());
            when(activeScan.isContested()).thenReturn(true);
            assertTrue(fixture.runtime.extendAtTimeLimit());
        }
    }

    private static ITeamCapturePoint capturePoint(CaptureState state)
    {
        ITeamCapturePoint point = mock(ITeamCapturePoint.class);
        when(point.getObjectId()).thenReturn(UUID.randomUUID());
        when(point.getCaptureState()).thenReturn(state);
        when(point.getTeamObjectPosition()).thenReturn(Vec3.ZERO);
        return point;
    }

    @Test
    void warmupWaitsForPopulationAndForEverySideToReadyUp()
    {
        Fixture fixture = new Fixture();
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(fixture.manager.getServer()).thenReturn(server);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of(fixture.player));
        when(fixture.player.isSpectator()).thenReturn(true);
        fixture.rules.set(RoundRule.MINIMUM_PLAYERS, "2");
        fixture.rules.set(RoundRule.REQUIRE_READY, "true");
        fixture.start();
        assertTrue(fixture.runtime.tickWarmup());
        assertTrue(fixture.runtime.ready(fixture.player, true));
        assertTrue(fixture.runtime.tickWarmup());
        ServerPlayer second = mock(ServerPlayer.class);
        when(second.getUUID()).thenReturn(UUID.randomUUID());
        when(second.level()).thenReturn(fixture.level);
        when(second.isSpectator()).thenReturn(true);
        Team blue = mock(Team.class);
        when(blue.getShortName()).thenReturn("blue");
        PlayerData.getInstance(second).setNewTeam(blue);
        when(players.getPlayers()).thenReturn(List.of(fixture.player, second));
        assertTrue(fixture.runtime.tickWarmup());
        assertTrue(fixture.runtime.ready(second, true));
        assertFalse(fixture.runtime.tickWarmup());
        assertFalse(fixture.runtime.isStaging());
        assertFalse(fixture.runtime.ready(second, false));
    }

    @Test
    void spectatingDoesNotResetTheSideSwitchCooldown()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.TEAM_SWITCH_SECONDS, "60");
        fixture.start();
        fixture.runtime.spawned(fixture.player);
        Team blue = mock(Team.class);
        when(blue.getShortName()).thenReturn("blue");
        assertTrue(fixture.runtime.mayChooseTeam(fixture.player, fixture.team));
        assertFalse(fixture.runtime.mayChooseTeam(fixture.player, blue));
        fixture.runtime.teamChosen(fixture.player, Team.SPECTATORS);
        assertFalse(fixture.runtime.mayChooseTeam(fixture.player, blue));
        when(fixture.manager.getRoundElapsedTicks()).thenReturn(1200);
        assertTrue(fixture.runtime.mayChooseTeam(fixture.player, blue));
    }

    @Test
    void restrictedSpectatorsCanFollowLivingAlliesAndOperatorsRemainExempt()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.RESTRICT_SPECTATORS, "true");
        fixture.start();
        when(fixture.player.isSpectator()).thenReturn(true);
        when(fixture.manager.areAllied(fixture.team, fixture.team)).thenReturn(true);
        ServerPlayer ally = mock(ServerPlayer.class);
        when(ally.isAlive()).thenReturn(true);
        when(ally.level()).thenReturn(fixture.level);
        when(fixture.manager.getPlayerTeam(ally)).thenReturn(fixture.team);
        assertTrue(fixture.runtime.maySpectate(fixture.player, ally));
        Team enemy = mock(Team.class);
        when(enemy.getShortName()).thenReturn("blue");
        when(fixture.manager.getPlayerTeam(ally)).thenReturn(enemy);
        assertFalse(fixture.runtime.maySpectate(fixture.player, ally));
        when(fixture.player.hasPermissions(2)).thenReturn(true);
        assertTrue(fixture.runtime.maySpectate(fixture.player, ally));
    }

    @Test
    void flagRestrictionsOnlyAffectCarriersAndRequireDismountingAndLandingBeforePickup()
    {
        Fixture fixture = new Fixture();
        fixture.rules.set(RoundRule.FLAG_NO_VEHICLES, "true");
        fixture.rules.set(RoundRule.FLAG_NO_FLIGHT, "true");
        fixture.rules.set(RoundRule.FLAG_BANNED_ITEMS, "minecraft:bow");
        fixture.start();
        when(fixture.player.getAbilities()).thenReturn(new Abilities());
        assertFalse(fixture.runtime.blocksItemUse(fixture.player, new ItemStack(Items.BOW)));
        assertTrue(fixture.runtime.mayMount(fixture.player));
        assertTrue(fixture.runtime.mayCarryFlag(fixture.player));
        when(fixture.player.isPassenger()).thenReturn(true);
        assertFalse(fixture.runtime.mayCarryFlag(fixture.player));
        when(fixture.player.isPassenger()).thenReturn(false);
        when(fixture.player.isFallFlying()).thenReturn(true);
        assertFalse(fixture.runtime.mayCarryFlag(fixture.player));
        Flag flag = mock(Flag.class);
        when(fixture.manager.getFlagCarriedBy(fixture.player)).thenReturn(flag);
        assertFalse(fixture.runtime.mayMount(fixture.player));
        assertTrue(fixture.runtime.blocksItemUse(fixture.player, new ItemStack(Items.BOW)));
        assertFalse(fixture.runtime.blocksItemUse(fixture.player, new ItemStack(Items.STONE)));
    }

    private static final class Fixture
    {
        private final TeamsManager manager = mock(TeamsManager.class);
        private final ServerLevel level = mock(ServerLevel.class);
        private final Team team = mock(Team.class);
        private final ServerPlayer player = mock(ServerPlayer.class);
        private final TeamsRound round = new TeamsRound("map", "tdm", List.of("red", "blue"), 10, 30);
        private final RoundRules rules = round.getRules();
        private final RoundRuleRuntime runtime = new RoundRuleRuntime(manager);

        private Fixture()
        {
            when(manager.isRoundRunning()).thenReturn(true);
            when(manager.getCurrentRound()).thenReturn(Optional.of(round));
            when(manager.getRoundRuleRuntime()).thenReturn(runtime);
            when(manager.getCurrentLevel()).thenReturn(level);
            when(manager.getRoundInstanceId()).thenReturn(UUID.randomUUID());
            when(manager.getRoundTeamIndex(team)).thenReturn(0);
            when(manager.getPlayerTeam(player)).thenReturn(team);
            when(team.getShortName()).thenReturn("red");
            when(player.getUUID()).thenReturn(UUID.randomUUID());
            when(player.level()).thenReturn(level);
            when(level.getAllEntities()).thenReturn(List.of());
            PlayerData.getInstance(player).setTeam(team);
            PlayerData.getInstance(player).setNewTeam(team);
        }

        private void start()
        {
            runtime.start(round, new RoundRules());
        }

        private CompoundTag playerState()
        {
            return runtime.save().getCompound("players").getCompound(player.getUUID().toString());
        }

        private AAGun vehicle()
        {
            AAGun gun = mock(AAGun.class);
            when(gun.getUUID()).thenReturn(UUID.randomUUID());
            when(gun.level()).thenReturn(level);
            when(gun.getPersistentData()).thenReturn(new CompoundTag());
            return gun;
        }
    }
}
