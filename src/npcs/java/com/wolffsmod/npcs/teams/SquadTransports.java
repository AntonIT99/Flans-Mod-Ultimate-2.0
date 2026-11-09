package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.network.BotOrdersStatePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Server-owned pickup and safe unloading of existing soldiers; never spawns, retires or re-tags them. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SquadTransports
{
    private static final double PICK_RANGE = 64D;
    private static final double WALK_RANGE = 128D;
    private static final long MISSING_GRACE = 1200L;
    private static final String CAPTURE_WEIGHT = "wolffsmodnpcs_transport_capture";
    private static final String PREFIX = "wolffsmodnpcs.orders.transport.";

    /** Requested by the order screen; the UUID is resolved and permissions are checked anew on the server. */
    public static Component request(ServerPlayer player, int squadId, @Nullable UUID transport, boolean unload)
    {
        BotBattle battle = BotBattles.ofPlayer(player).orElse(null);
        BotSquad squad = battle == null ? null : battle.getSquads().get(squadId);
        if (squad == null || squad.getRole() != BotRole.INFANTRY || !BotOrders.mayOrder(player, battle, squad))
            return Component.translatable(PREFIX + "denied");
        SquadTransport previous = squad.getTransport();
        if (unload)
        {
            if (previous == null)
                return Component.translatable(PREFIX + "none");
            previous.unload();
            members((ServerLevel) player.level(), battle, squad).forEach(NpcDrivers::releaseTransportSeat);
            return Component.translatable(PREFIX + "unloading");
        }
        if (previous != null || transport == null)
            return Component.translatable(PREFIX + "busy");
        ServerLevel level = (ServerLevel) player.level();
        Entity vehicle = level.getEntity(transport);
        List<EntityNPCInterface> soldiers = members(level, battle, squad).stream().filter(npc -> !npc.isPassenger()).toList();
        if (vehicle == null || player.distanceToSqr(vehicle) > PICK_RANGE * PICK_RANGE || !SquadTransportSeats.groundTransport(vehicle) || !SquadTransportSeats.stopped(vehicle)
            || soldiers.isEmpty() || soldiers.stream().anyMatch(npc -> npc.distanceToSqr(vehicle) > WALK_RANGE * WALK_RANGE || !SquadTransportSeats.allowed(npc, vehicle)))
            return Component.translatable(PREFIX + "unavailable");
        long free = SquadTransportSeats.seats(vehicle).stream().filter(SquadTransportSeats.Seat::free).filter(seat -> !reserved(battle, vehicle.getUUID(), seat.index(), null)).count();
        if (free < soldiers.size())
            return Component.translatable(PREFIX + "capacity", free, soldiers.size());
        SquadTransport state = new SquadTransport(vehicle.getUUID(), player.getUUID(), soldiers.stream().map(Entity::getUUID).toList(), vehicle.position(), level.getGameTime());
        List<SquadTransportSeats.Seat> available = SquadTransportSeats.seats(vehicle).stream().filter(SquadTransportSeats.Seat::free)
            .filter(seat -> !reserved(battle, vehicle.getUUID(), seat.index(), null)).toList();
        for (int index = 0; index < soldiers.size(); index++)
            state.getReservations().put(soldiers.get(index).getUUID(), available.get(index).index());
        squad.setTransport(state);
        squad.getTactics().clear();
        squad.getApproach().clear();
        return Component.translatable(PREFIX + "boarding");
    }

    /** Nearby loaded ground transports, with authoritative capacity; selecting one is not authorization to board it. */
    static List<BotOrdersStatePacket.Transport> choices(ServerPlayer player, BotBattle battle)
    {
        return player.level().getEntities(player, player.getBoundingBox().inflate(PICK_RANGE), SquadTransportSeats::groundTransport).stream()
            .filter(vehicle -> player.distanceToSqr(vehicle) <= PICK_RANGE * PICK_RANGE && SquadTransportSeats.stopped(vehicle))
            .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(32)
            .map(vehicle -> new BotOrdersStatePacket.Transport(vehicle.getUUID(), vehicle.getDisplayName().getString(),
                (int) SquadTransportSeats.seats(vehicle).stream().filter(SquadTransportSeats.Seat::free).filter(seat -> !reserved(battle, vehicle.getUUID(), seat.index(), null)).count()))
            .filter(choice -> choice.seats() > 0).toList();
    }

    static List<EntityNPCInterface> members(ServerLevel level, BotBattle battle, BotSquad squad)
    {
        return BotCommander.livingMembers(level, battle, squad).stream().filter(EntityNPCInterface.class::isInstance).map(EntityNPCInterface.class::cast).toList();
    }

    /** Called by every battle's upkeep, including standalone skirmishes and wars. */
    static void tick(ServerLevel level, BotBattle battle, long now)
    {
        for (BotSquad squad : battle.getSquads().values())
        {
            SquadTransport state = squad.getTransport();
            if (state != null)
                tick(level, battle, squad, state, now);
        }
    }

    private static void tick(ServerLevel level, BotBattle battle, BotSquad squad, SquadTransport state, long now)
    {
        Entity vehicle = level.getEntity(state.getVehicle());
        if (vehicle == null)
        {
            missing(level, battle, squad, state, now);
            return;
        }
        state.setLastSeen(now);
        state.setLastPosition(vehicle.position());
        List<EntityNPCInterface> soldiers = members(level, battle, squad).stream().filter(npc -> state.getSoldiers().contains(npc.getUUID())).toList();
        soldiers.forEach(SquadTransports::capture);
        if (!SquadTransportSeats.groundTransport(vehicle))
        {
            finish(level, battle, squad, "lost");
            return;
        }
        if (!authorized(level, battle, squad, state) || squad.isDisbanded() || soldiers.stream().anyMatch(npc -> !SquadTransportSeats.allowed(npc, vehicle)))
            state.unload();
        if (state.getPhase() == SquadTransport.Phase.BOARDING && now >= state.getDeadline())
            embark(level, state, "timeout");
        if (state.getPhase() == SquadTransport.Phase.UNLOADING)
        {
            unload(level, battle, squad, state, vehicle, soldiers);
            return;
        }
        if (state.getPhase() == SquadTransport.Phase.BOARDING && SquadTransportSeats.stopped(vehicle))
            board(battle, state, vehicle, soldiers);
        // Missing entities can be unloaded, so only finish pickup when every original soldier is known aboard or dead.
        if (state.getPhase() == SquadTransport.Phase.BOARDING && accountedFor(level, battle, squad, state, vehicle))
            embark(level, state, "embarked");
        if (state.getPhase() == SquadTransport.Phase.EMBARKED && soldiers.stream().noneMatch(npc -> SquadTransportSeats.aboard(npc, vehicle))
            && state.getSoldiers().stream().allMatch(id -> level.getEntity(id) != null || dead(battle, squad, id)))
            finish(level, battle, squad, "complete");
    }

    private static void missing(ServerLevel level, BotBattle battle, BotSquad squad, SquadTransport state, long now)
    {
        // A saved mount and its passengers may still be in an unloaded chunk. Never spawn replacements here.
        if (level.isLoaded(BlockPos.containing(state.getLastPosition())) && now - state.getLastSeen() > MISSING_GRACE)
            finish(level, battle, squad, "lost");
    }

    private static boolean authorized(ServerLevel level, BotBattle battle, BotSquad squad, SquadTransport state)
    {
        ServerPlayer issuer = level.getServer().getPlayerList().getPlayer(state.getPlayer());
        return issuer != null && issuer.level() == level && battle.sideOf(squad.getTeam()) >= 0 && battle.sideOfPlayer(issuer).orElse(-1) == battle.sideOf(squad.getTeam());
    }

    private static void embark(ServerLevel level, SquadTransport state, String message)
    {
        state.setPhase(SquadTransport.Phase.EMBARKED);
        state.getReservations().clear();
        tell(level, state, message);
    }

    private static void board(BotBattle battle, SquadTransport state, Entity vehicle, List<EntityNPCInterface> soldiers)
    {
        List<SquadTransportSeats.Seat> seats = SquadTransportSeats.seats(vehicle);
        state.getReservations().entrySet().removeIf(entry -> soldiers.stream().noneMatch(npc -> npc.getUUID().equals(entry.getKey()) && !npc.isPassenger())
            || seats.stream().noneMatch(seat -> seat.index() == entry.getValue() && seat.free()));
        for (EntityNPCInterface soldier : soldiers)
        {
            if (!soldier.isPassenger() && !NpcSuppression.isPinned(soldier))
                boardSoldier(battle, state, vehicle, seats, soldier);
        }
    }

    private static void boardSoldier(BotBattle battle, SquadTransport state, Entity vehicle, List<SquadTransportSeats.Seat> seats, EntityNPCInterface soldier)
    {
        SquadTransportSeats.Seat seat = seats.stream().filter(SquadTransportSeats.Seat::free)
            .filter(candidate -> !reserved(battle, vehicle.getUUID(), candidate.index(), soldier.getUUID()))
            .filter(candidate -> !(candidate.mount() instanceof com.flansmodultimate.api.driveables.ISeat) || NpcDrivers.reserveTransportSeat(soldier, candidate.mount()))
            .findFirst().orElse(null);
        if (seat == null)
            return;
        state.getReservations().put(soldier.getUUID(), seat.index());
        if (soldier.position().distanceToSqr(seat.approach()) <= 2.5D * 2.5D && SquadTransportSeats.board(soldier, seat))
        {
            soldier.getNavigation().stop();
            capture(soldier);
        }
    }
    private static boolean reserved(BotBattle battle, UUID vehicle, int seat, @Nullable UUID soldier)
    {
        Set<BotBattle> battles = new LinkedHashSet<>(BotBattles.all());
        battles.add(battle);
        for (BotSquad squad : battles.stream().flatMap(current -> current.getSquads().values().stream()).toList())
        {
            SquadTransport state = squad.getTransport();
            if (state != null && state.getVehicle().equals(vehicle))
            {
                for (Map.Entry<UUID, Integer> entry : state.getReservations().entrySet())
                    if (entry.getValue() == seat && !entry.getKey().equals(soldier))
                        return true;
            }
        }
        return false;
    }

    private static boolean dead(BotBattle battle, BotSquad squad, UUID soldier)
    {
        return squad.getSlots().stream().map(battle.getSlots()::get).filter(Objects::nonNull).noneMatch(slot -> soldier.equals(slot.getEntity()) && slot.getDiedAt() == 0);
    }

    private static boolean accountedFor(ServerLevel level, BotBattle battle, BotSquad squad, SquadTransport state, Entity vehicle)
    {
        return state.getSoldiers().stream().allMatch(id -> dead(battle, squad, id) || level.getEntity(id) instanceof EntityNPCInterface soldier && SquadTransportSeats.aboard(soldier, vehicle));
    }

    private static void unload(ServerLevel level, BotBattle battle, BotSquad squad, SquadTransport state, Entity vehicle, List<EntityNPCInterface> soldiers)
    {
        List<EntityNPCInterface> aboard = soldiers.stream().filter(npc -> SquadTransportSeats.aboard(npc, vehicle)).toList();
        if (!aboard.isEmpty() && SquadTransportSeats.stopped(vehicle))
        {
            // One per upkeep lets collisions account for the preceding soldier's exit.
            EntityNPCInterface soldier = aboard.get(0);
            SquadTransportSeats.exit(level, vehicle, soldier).ifPresent(spot ->
            {
                SquadTransportSeats.disembark(soldier, spot);
                capture(soldier);
            });
        }
        if (soldiers.stream().noneMatch(npc -> SquadTransportSeats.aboard(npc, vehicle))
            && state.getSoldiers().stream().allMatch(id -> level.getEntity(id) != null || dead(battle, squad, id)))
            finish(level, battle, squad, "complete");
    }

    private static void finish(ServerLevel level, BotBattle battle, BotSquad squad, String message)
    {
        SquadTransport state = squad.getTransport();
        squad.setTransport(null);
        members(level, battle, squad).forEach(soldier ->
        {
            NpcDrivers.releaseTransportSeat(soldier);
            capture(soldier);
        });
        if (state != null)
            tell(level, state, message);
    }

    private static void tell(ServerLevel level, SquadTransport state, String message)
    {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(state.getPlayer());
        if (player != null)
            player.sendSystemMessage(Component.translatable(PREFIX + message));
    }

    static Optional<SquadTransport> assignment(EntityNPCInterface soldier)
    {
        return BotBattles.of(soldier).flatMap(battle -> BotBattles.slotOf(battle, soldier).map(slot -> battle.getSquads().get(slot.getSquad())))
            .map(BotSquad::getTransport).filter(state -> state.getSoldiers().contains(soldier.getUUID()));
    }

    /** Whether a live assignment temporarily owns this soldier's boarding decisions. Server thread only. */
    public static boolean manages(EntityNPCInterface soldier)
    {
        return assignment(soldier).isPresent();
    }

    /** Whether an assignment keeps this soldier in this transport, including while waiting for a safe exit. */
    public static boolean carries(EntityNPCInterface soldier, Entity vehicle)
    {
        return assignment(soldier).filter(state -> state.getVehicle().equals(vehicle.getUUID())).isPresent();
    }

    static Optional<BotCommander.Order> order(EntityNPCInterface soldier)
    {
        SquadTransport state = assignment(soldier).orElse(null);
        if (state == null || state.getPhase() != SquadTransport.Phase.BOARDING || soldier.isPassenger())
            return Optional.empty();
        Entity vehicle = ((ServerLevel) soldier.level()).getEntity(state.getVehicle());
        if (vehicle == null)
            return Optional.of(new BotCommander.Order(soldier.position(), 2D, BotRole.INFANTRY, SquadTask.Kind.HOLD));
        int reserved = state.getReservations().getOrDefault(soldier.getUUID(), -1);
        Vec3 spot = SquadTransportSeats.seats(vehicle).stream().filter(seat -> seat.index() == reserved).map(SquadTransportSeats.Seat::approach).findFirst().orElse(soldier.position());
        return Optional.of(new BotCommander.Order(spot, 1.5D, BotRole.INFANTRY, SquadTask.Kind.GUARD));
    }

    /** Suppresses capture only while transported and restores the saved weight even after a bailout or reload. */
    static void capture(EntityNPCInterface soldier)
    {
        var data = soldier.getPersistentData();
        var team = FlansTeams.getTeam(soldier);
        boolean carried = soldier.isPassenger() && assignment(soldier).isPresent();
        if (carried && team.isPresent() && !data.contains(CAPTURE_WEIGHT, Tag.TAG_FLOAT))
        {
            data.putFloat(CAPTURE_WEIGHT, FlansTeams.getCaptureWeight(soldier));
            FlansTeams.assignCombatant(soldier, team.get(), 0F);
        }
        else if (!carried && data.contains(CAPTURE_WEIGHT, Tag.TAG_FLOAT))
        {
            team.ifPresent(value -> FlansTeams.assignCombatant(soldier, value, data.getFloat(CAPTURE_WEIGHT)));
            data.remove(CAPTURE_WEIGHT);
        }
    }
}
