package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.driveables.IFlanSeatOccupant;
import com.flansmodultimate.common.FlanEntityPermissions;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import net.minecraftforge.server.permission.PermissionAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleDeploymentTest
{
    private TeamsManager manager;
    private TeamsVehicleDeployment deployment;
    private TeamSpawnerBlockEntity point;
    private ServerPlayer player;
    private ServerLevel level;
    private TeamsMap map;
    private Driveable vehicle;
    private Seat driver;
    private Seat passenger;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void setUp()
    {
        manager = mock(TeamsManager.class);
        deployment = new TeamsVehicleDeployment(manager);
        point = mock(TeamSpawnerBlockEntity.class);
        player = mock(ServerPlayer.class);
        level = mock(ServerLevel.class);
        map = mock(TeamsMap.class);
        vehicle = mock(Driveable.class);
        driver = seat(0);
        passenger = seat(1);
        UUID spawner = UUID.randomUUID();
        when(point.getObjectId()).thenReturn(spawner);
        when(point.getBlockPos()).thenReturn(BlockPos.ZERO);
        when(point.getDimension()).thenReturn(Level.OVERWORLD);
        when(point.getVehicleTemplateName()).thenReturn(Component.literal("Tank"));
        when(point.getRemainingSpawnDelayTicks()).thenReturn(400);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(vehicle.getUUID()).thenReturn(UUID.randomUUID());
        when(vehicle.isAlive()).thenReturn(true);
        when(vehicle.position()).thenReturn(Vec3.ZERO);
        when(vehicle.getSeats()).thenReturn(new Seat[]{driver, passenger});
        when(vehicle.isUsableSeat(any())).thenReturn(true);
        when(vehicle.canPlayerAccessWithoutBinding(any())).thenReturn(true);
        CompoundTag tag = new CompoundTag();
        tag.putUUID(SpawnerVehicles.SPAWNER, spawner);
        when(vehicle.getPersistentData()).thenReturn(tag);
        when(level.getAllEntities()).thenReturn(List.of(vehicle));
        when(manager.isRoundRunning()).thenReturn(true);
        when(manager.getRoundInstanceId()).thenReturn(UUID.randomUUID());
        when(manager.getVehicleDeploymentSpawners(any())).thenReturn(List.of(point));
        MinecraftServer server = mock(MinecraftServer.class);
        when(manager.getServer()).thenReturn(server);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        TeamsRound round = mock(TeamsRound.class);
        when(round.getMapId()).thenReturn("arena");
        when(manager.getCurrentRound()).thenReturn(Optional.of(round));
        when(manager.getMap("arena")).thenReturn(Optional.of(map));
        when(map.getDimension()).thenReturn(Level.OVERWORLD);
        GameType type = mock(GameType.class);
        when(type.getObjectiveMode()).thenReturn(ObjectiveMode.CONQUEST);
        when(manager.getCurrentGameType()).thenReturn(Optional.of(type));
        Team team = mock(Team.class);
        when(manager.getPlayerTeam(any())).thenReturn(team);
        when(manager.getRoundTeamIndex(team)).thenReturn(2);
        when(round.areAllied(team, team)).thenReturn(true);
    }

    private Seat seat(int index)
    {
        Seat seat = mock(Seat.class);
        when(seat.level()).thenReturn(level);
        when(seat.getSeatIndex()).thenReturn(index);
        when(seat.position()).thenReturn(new Vec3(40D, 70D, 30D));
        when(seat.getPassengers()).thenReturn(List.of());
        return seat;
    }

    @Test
    void alliedNpcSeatsCanBeOfferedWithoutDisplacingThemAndDisablingTakeoverRechecksAvailability()
    {
        Entity npc = mock(Entity.class, withSettings().extraInterfaces(IFlanSeatOccupant.class));
        when(npc.getPersistentData()).thenReturn(new CompoundTag());
        IFlanSeatOccupant policy = (IFlanSeatOccupant) npc;
        ServerPlayer other = mock(ServerPlayer.class);
        when(driver.getPassengers()).thenReturn(List.of(npc));
        when(passenger.getPassengers()).thenReturn(List.of(other));
        when(policy.flansmodultimateMayYieldSeat(player, true)).thenReturn(true);
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(true);
            assertEquals(VehicleDeploySnapshot.Availability.READY, deployment.describe(player, point, List.of(vehicle)).availability());
            assertEquals(Optional.of(driver), deployment.target(player, List.of(vehicle)));
            assertTrue(deployment.select(player, point.getObjectId()));
            verify(policy, never()).flansmodultimateYieldSeat(any(), anyBoolean());
            when(policy.flansmodultimateMayYieldSeat(player, true)).thenReturn(false);
            assertFalse(deployment.deploy(player));
            assertEquals(VehicleDeploySnapshot.Availability.FULL, deployment.describe(player, point, List.of(vehicle)).availability());
            verify(policy, never()).flansmodultimateYieldSeat(any(), anyBoolean());
        }
    }

    @Test
    void freeSeatsAreAvailableDuringReplacementCooldownAndFullAndWaitingReasonsDiffer()
    {
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(true);
            assertEquals(VehicleDeploySnapshot.Availability.READY, deployment.describe(player, point, List.of(vehicle)).availability());
            when(driver.getPassengers()).thenReturn(List.of(mock(ServerPlayer.class)));
            assertEquals(Optional.of(passenger), deployment.target(player, List.of(vehicle)));
            when(passenger.getPassengers()).thenReturn(List.of(mock(ServerPlayer.class)));
            assertEquals(VehicleDeploySnapshot.Availability.FULL, deployment.describe(player, point, List.of(vehicle)).availability());
            assertEquals(VehicleDeploySnapshot.Availability.COOLDOWN, deployment.describe(player, point, List.of()).availability());
            when(point.getRemainingSpawnDelayTicks()).thenReturn(0);
            assertEquals(VehicleDeploySnapshot.Availability.UNAVAILABLE, deployment.describe(player, point, List.of()).availability());
        }
    }

    @Test
    void respawnBoardsAFreePassengerSeatOnTheExistingVehicleWithoutRestartingItsDelay()
    {
        when(driver.getPassengers()).thenReturn(List.of(mock(ServerPlayer.class)));
        ServerPlayer respawned = mock(ServerPlayer.class);
        UUID playerId = player.getUUID();
        when(respawned.getUUID()).thenReturn(playerId);
        when(passenger.tryEnter(respawned, true)).thenReturn(true);
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(any(), eq(FlanEntityPermissions.DRIVEABLE_ENTER))).thenReturn(true);
            assertTrue(deployment.select(player, point.getObjectId()));
            verify(player, never()).teleportTo(any(ServerLevel.class), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyFloat());
            assertTrue(deployment.deploy(respawned));
            verify(respawned).teleportTo(level, 40D, 70D, 30D, 0F, 0F);
            verify(passenger).tryEnter(respawned, true);
            verify(driver, never()).tryEnter(any());
            verify(point, never()).recordSpawn();
            assertFalse(deployment.deploy(respawned), "Selections are consumed once");
        }
    }

    @Test
    void rechecksSeatOccupancyAndSpawnerEligibilityAfterSelection()
    {
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(true);
            assertTrue(deployment.select(player, point.getObjectId()));
            when(driver.getPassengers()).thenReturn(List.of(mock(ServerPlayer.class)));
            when(passenger.getPassengers()).thenReturn(List.of(mock(ServerPlayer.class)));
            assertFalse(deployment.deploy(player));
            verify(player, never()).teleportTo(any(ServerLevel.class), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyFloat());
            verify(driver, never()).tryEnter(any());
            verify(passenger, never()).tryEnter(any());

            when(driver.getPassengers()).thenReturn(List.of());
            assertTrue(deployment.select(player, point.getObjectId()));
            when(manager.getVehicleDeploymentSpawners(player)).thenReturn(List.of());
            assertFalse(deployment.deploy(player), "Removed templates and captured or disconnected bases invalidate the selection");
            assertFalse(deployment.select(player, UUID.randomUUID()));
        }
    }

    @Test
    void lockedAndDamagedSeatsAndVehiclesFromAnotherRoundAreRejected()
    {
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(true);
            when(vehicle.canPlayerAccessWithoutBinding(player)).thenReturn(false);
            assertEquals(VehicleDeploySnapshot.Availability.LOCKED, deployment.describe(player, point, List.of(vehicle)).availability());
            when(vehicle.canPlayerAccessWithoutBinding(player)).thenReturn(true);
            when(vehicle.isUsableSeat(any())).thenReturn(false);
            assertTrue(TeamsVehicleDeployment.freeSeats(vehicle).findAny().isEmpty());
            when(vehicle.isUsableSeat(driver)).thenReturn(true);
            assertTrue(deployment.select(player, point.getObjectId()));
            when(manager.getRoundInstanceId()).thenReturn(UUID.randomUUID());
            assertFalse(deployment.deploy(player));
            verify(driver, never()).tryEnter(any());
        }
    }

    @Test
    void onlyLiveTemplateVehiclesInsideTheMapAreTrackedForBoarding()
    {
        assertEquals(List.of(vehicle), TeamsVehicleDeployment.vehicles(level, map).get(point.getObjectId()));
        when(vehicle.isWreck()).thenReturn(true);
        assertTrue(TeamsVehicleDeployment.vehicles(level, map).isEmpty());
        when(vehicle.isWreck()).thenReturn(false);
        vehicle.getPersistentData().remove(SpawnerVehicles.SPAWNER);
        vehicle.getPersistentData().putUUID("npc_source_spawner", point.getObjectId());
        assertTrue(TeamsVehicleDeployment.vehicles(level, map).isEmpty());
        vehicle.getPersistentData().putUUID(SpawnerVehicles.SPAWNER, point.getObjectId());
        when(map.getBounds()).thenReturn(new TeamsMap.Bounds(-1, -1, -1, 1, 1, 1));
        when(vehicle.position()).thenReturn(new Vec3(10D, 0D, 0D));
        assertTrue(TeamsVehicleDeployment.vehicles(level, map).isEmpty());
    }
}
