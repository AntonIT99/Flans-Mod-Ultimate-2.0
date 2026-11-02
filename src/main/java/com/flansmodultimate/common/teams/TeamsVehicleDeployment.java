package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.FlanEntityPermissions;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.SeatOccupants;
import com.flansmodultimate.common.types.Team;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Stream;

/** Resolves template vehicle deployments on the server thread, allowing cooperative allied NPC seat handoff. */
@RequiredArgsConstructor
public final class TeamsVehicleDeployment
{
    private final TeamsManager manager;
    private final Map<UUID, Selection> selections = new HashMap<>();
    @Nullable
    private ServerLevel snapshotLevel;
    private long snapshotTick = Long.MIN_VALUE;
    private Map<UUID, List<Entity>> snapshotVehicles = Map.of();

    private record Selection(UUID round, UUID spawner)
    {}

    public void clear()
    {
        selections.clear();
        snapshotLevel = null;
        snapshotVehicles = Map.of();
    }

    public void forget(ServerPlayer player)
    {
        selections.remove(player.getUUID());
    }

    /** Only already spawned template vehicles count; NPC vehicles use a separate source tag. */
    static Map<UUID, List<Entity>> vehicles(ServerLevel level, TeamsMap map)
    {
        Map<UUID, List<Entity>> result = new HashMap<>();
        for (Entity entity : level.getAllEntities())
        {
            if (!(entity instanceof Driveable || entity instanceof AAGun) || !entity.isAlive() || entity instanceof Driveable driveable && driveable.isWreck()
                || !entity.getPersistentData().hasUUID(SpawnerVehicles.SPAWNER) || map.getBounds() != null && !map.getBounds().contains(entity.position()))
                continue;
            result.computeIfAbsent(entity.getPersistentData().getUUID(SpawnerVehicles.SPAWNER), ignored -> new ArrayList<>()).add(entity);
        }
        result.values().forEach(list -> list.sort(Comparator.comparing(Entity::getUUID)));
        return result;
    }

    public List<VehicleDeploySnapshot> snapshots(ServerPlayer player)
    {
        List<TeamSpawnerBlockEntity> points = manager.getVehicleDeploymentSpawners(player);
        if (points.isEmpty())
            return List.of();
        TeamsMap map = manager.getCurrentRound().flatMap(round -> manager.getMap(round.getMapId())).orElseThrow();
        ServerLevel level = manager.getServer().getLevel(map.getDimension());
        if (level == null)
            return List.of();
        Map<UUID, List<Entity>> vehicles = snapshotVehicles(level, map);
        return points.stream().map(point -> describe(player, point, vehicles.getOrDefault(point.getObjectId(), List.of()))).toList();
    }

    /** A broadcast scans the loaded entities once, then checks each viewer's access against the same index. */
    private Map<UUID, List<Entity>> snapshotVehicles(ServerLevel level, TeamsMap map)
    {
        if (snapshotLevel != level || snapshotTick != level.getGameTime())
        {
            snapshotLevel = level;
            snapshotTick = level.getGameTime();
            snapshotVehicles = vehicles(level, map);
        }
        return snapshotVehicles;
    }

    VehicleDeploySnapshot describe(ServerPlayer player, TeamSpawnerBlockEntity point, List<Entity> vehicles)
    {
        VehicleDeploySnapshot.Availability availability;
        if (vehicles.isEmpty())
            availability = point.getRemainingSpawnDelayTicks() > 0 ? VehicleDeploySnapshot.Availability.COOLDOWN : VehicleDeploySnapshot.Availability.UNAVAILABLE;
        else if (vehicles.stream().noneMatch(vehicle -> availableSeats(player, vehicle).findAny().isPresent()))
            availability = VehicleDeploySnapshot.Availability.FULL;
        else
            availability = target(player, vehicles).isPresent() ? VehicleDeploySnapshot.Availability.READY : VehicleDeploySnapshot.Availability.LOCKED;
        return new VehicleDeploySnapshot(point.getObjectId(), point.getVehicleTemplateName(), point.getBlockPos().getX() + 0.5D, point.getBlockPos().getZ() + 0.5D, availability,
            point.getRemainingSpawnDelayTicks());
    }

    public boolean select(ServerPlayer player, UUID spawner)
    {
        forget(player);
        // Selecting records a wish only; it cannot move or refill a living player.
        if (!manager.isRoundRunning() || manager.getRoundInstanceId() == null || !manager.getCurrentGameType().map(type -> type.getObjectiveMode().usesDeployment()).orElse(false))
            return false;
        VehicleDeploySnapshot point = snapshots(player).stream().filter(candidate -> candidate.id().equals(spawner)).findFirst().orElse(null);
        if (point == null || point.availability() != VehicleDeploySnapshot.Availability.READY)
            return false;
        selections.put(player.getUUID(), new Selection(manager.getRoundInstanceId(), spawner));
        return true;
    }

    /** Rechecks the plate, round, map bounds, access and seats after vanilla creates the respawned player. */
    public boolean deploy(ServerPlayer player)
    {
        Selection selection = selections.remove(player.getUUID());
        if (selection == null)
            return false;
        if (selection.round().equals(manager.getRoundInstanceId()) && board(player, selection.spawner()))
            return true;
        player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.vehicle_deploy_unavailable"));
        return false;
    }

    private boolean board(ServerPlayer player, UUID spawner)
    {
        TeamSpawnerBlockEntity point = manager.getVehicleDeploymentSpawners(player).stream().filter(candidate -> candidate.getObjectId().equals(spawner)).findFirst().orElse(null);
        TeamsMap map = manager.getCurrentRound().flatMap(round -> manager.getMap(round.getMapId())).orElse(null);
        if (point == null || map == null)
            return false;
        ServerLevel level = manager.getServer().getLevel(point.getDimension());
        if (level == null)
            return false;
        Entity seat = target(player, vehicles(level, map).getOrDefault(spawner, List.of())).orElse(null);
        if (seat == null)
            return false;
        Vec3 position = seat.position();
        player.teleportTo(level, position.x, position.y, position.z, player.getYRot(), player.getXRot());
        if (seat instanceof Seat driveableSeat)
            return driveableSeat.tryEnter(player, true);
        if (!SeatOccupants.makeRoom(seat, player, true))
            return false;
        seat.interact(player, InteractionHand.MAIN_HAND);
        return player.getVehicle() == seat;
    }

    Optional<Entity> target(ServerPlayer player, List<Entity> vehicles)
    {
        return vehicles.stream().filter(vehicle -> accessible(player, vehicle)).flatMap(vehicle -> availableSeats(player, vehicle)).findFirst();
    }

    private boolean accessible(ServerPlayer player, Entity vehicle)
    {
        Team team = manager.getPlayerTeam(player);
        if (occupants(vehicle).anyMatch(occupant ->
        {
            Team other = TeamsCombatants.getPlayingTeam(manager, occupant);
            return other != null && !manager.getCurrentRound().map(round -> round.areAllied(team, other)).orElse(false);
        }))
            return false;
        if (vehicle instanceof Driveable driveable)
            return driveable.canPlayerAccessWithoutBinding(player) && FlanEntityPermissions.allows(player, FlanEntityPermissions.DRIVEABLE_ENTER);
        return FlanEntityPermissions.allows(player, FlanEntityPermissions.AA_GUN_ENTER);
    }

    private static Stream<Entity> occupants(Entity vehicle)
    {
        return vehicle instanceof Driveable driveable ? Arrays.stream(driveable.getSeats()).filter(Objects::nonNull).flatMap(seat -> seat.getPassengers().stream()) : vehicle.getPassengers().stream();
    }

    static Stream<Entity> freeSeats(Entity vehicle)
    {
        return seats(vehicle).filter(seat -> seat.getPassengers().isEmpty());
    }

    private static Stream<Entity> availableSeats(ServerPlayer player, Entity vehicle)
    {
        return seats(vehicle).filter(seat -> SeatOccupants.canEnter(seat, player, true));
    }

    private static Stream<Entity> seats(Entity vehicle)
    {
        if (vehicle instanceof Driveable driveable)
            return Arrays.stream(driveable.getSeats()).filter(driveable::isUsableSeat).sorted(Comparator.comparingInt(Seat::getSeatIndex)).map(Entity.class::cast);
        return vehicle instanceof AAGun gun && gun.getConfigType() != null && !gun.getConfigType().isSentry() ? Stream.of(gun) : Stream.empty();
    }
}
