package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.TeamsCapturePoint;
import com.wolffsmod.npcs.network.BotOrdersStatePacket;
import com.wolffsmod.npcs.network.BotWaypointsPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Players giving orders to bots. Any player may order the allied squads around them; the player who takes a side's
 * command seat may order every squad of the side, wherever it is. Orders come before the side's AI until they run out
 * or are released. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotOrders
{
    /** Blocks from a player within which a squad's leader hears the player's orders. */
    public static final double NEARBY = 24D;
    /** The squad id that stands for every allied squad near the player. */
    public static final int NEARBY_SQUADS = -1;
    /** Blocks the player's view reaches for an order to hold or take the place looked at. */
    private static final double LOOK_RANGE = 128D;
    /** The objective index that stands for the place the player looks at, or the player's own position. */
    public static final int HERE = -1;

    /**
     * Orders squads of the player's side.
     *
     * @param squadId
     *            a squad, or {@link #NEARBY_SQUADS}
     * @param objective
     *            the capture point to attack or defend, by play order, or {@link #HERE}
     * @param at
     *            a place the player picked on the map, or null for the objective, or the block the player looks at
     * @return how many squads took the order; 0 when the player is not playing or no squad hears them
     */
    public static int give(ServerPlayer player, int squadId, PlayerOrder.Kind kind, int objective, @Nullable Vec3 at)
    {
        Optional<BotBattle> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty() || !(player.level() instanceof ServerLevel level))
            return 0;
        BotBattle battle = current.get();
        int side = battle.getGround().sideOf(team.get());
        boolean commander = player.getUUID().equals(battle.getCommanders().get(side));
        Vec3 target = at != null ? ground(level, at) : target(battle, player, objective);
        PlayerOrder order = new PlayerOrder(player.getUUID(), kind, target, level.getGameTime() + PlayerOrder.DURATION);
        int ordered = 0;
        for (BotSquad squad : battle.getSquads().values())
        {
            boolean chosen = squadId == NEARBY_SQUADS ? hears(level, battle, squad, player) : squad.getId() == squadId && (commander || hears(level, battle, squad, player));
            if (!chosen || squad.isDisbanded() || battle.sideOf(squad.getTeam()) != side || squad.getRole() == BotRole.AA)
                continue;
            if (kind == PlayerOrder.Kind.RELEASE)
                squad.setOrder(null);
            else
            {
                squad.setOrder(order);
                squad.setTask(order.task(player.position()));
            }
            ordered++;
        }
        return ordered;
    }

    /** Where an order sends a squad: the capture point chosen, or the block the player looks at, or the player's position. */
    private static Vec3 target(BotBattle battle, ServerPlayer player, int objective)
    {
        List<TeamsCapturePoint> points = battle.getGround().capturePoints();
        if (objective >= 0 && objective < points.size())
            return points.get(objective).position();
        HitResult look = player.pick(LOOK_RANGE, 1F, false);
        return look.getType() == HitResult.Type.BLOCK ? look.getLocation() : player.position();
    }

    /** The ground at a place picked on the map, which knows its position across but not its height. */
    private static Vec3 ground(ServerLevel level, Vec3 at)
    {
        return new Vec3(at.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z)), at.z);
    }

    /** Players who were last sent waypoints, who are sent an empty list once they have none left. */
    private static final Set<UUID> MARKED = new HashSet<>();

    /**
     * Sends every player of the round the places their orders send squads to, and the rally point of the squad they lead,
     * for their client to mark in the world.
     */
    static void sendWaypoints(MinecraftServer server, BotBattle battle)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            List<BotWaypointsPacket.Waypoint> waypoints = waypoints(battle, player);
            if (!waypoints.isEmpty() || MARKED.contains(player.getUUID()))
                NpcNetwork.sendTo(player, new BotWaypointsPacket(waypoints));
            if (waypoints.isEmpty())
                MARKED.remove(player.getUUID());
            else
                MARKED.add(player.getUUID());
        }
    }

    /** The places a player's orders send squads to, and the rally point of the squad they lead. */
    private static List<BotWaypointsPacket.Waypoint> waypoints(BotBattle battle, ServerPlayer player)
    {
        List<BotWaypointsPacket.Waypoint> waypoints = new ArrayList<>();
        for (BotSquad squad : battle.getSquads().values())
        {
            PlayerOrder order = squad.getOrder();
            boolean marked = !squad.isDisbanded() && order != null && order.player().equals(player.getUUID()) && order.kind() != PlayerOrder.Kind.FOLLOW && order.kind() != PlayerOrder.Kind.RELEASE;
            if (marked)
                waypoints.add(new BotWaypointsPacket.Waypoint(order.target().x, order.target().y, order.target().z, squad.getId(), lower(order.kind())));
            Vec3 rally = squad.getRally();
            if (!squad.isDisbanded() && player.getUUID().equals(squad.getLeader()) && rally != null)
                waypoints.add(new BotWaypointsPacket.Waypoint(rally.x, rally.y, rally.z, squad.getId(), "rally"));
        }
        return waypoints;
    }

    /** Takes every waypoint down, as the round ends. */
    static void clearWaypoints(MinecraftServer server)
    {
        for (UUID marked : MARKED)
        {
            ServerPlayer player = server.getPlayerList().getPlayer(marked);
            if (player != null)
                NpcNetwork.sendTo(player, new BotWaypointsPacket(List.of()));
        }
        MARKED.clear();
    }

    /** Whether a squad's leader is near enough to hear the player, or the squad already follows the player's orders. */
    private static boolean hears(ServerLevel level, BotBattle battle, BotSquad squad, ServerPlayer player)
    {
        if (squad.getOrder() != null && squad.getOrder().player().equals(player.getUUID()))
            return true;
        List<Entity> members = BotCommander.livingMembers(level, battle, squad);
        return !members.isEmpty() && members.get(0).distanceTo(player) <= NEARBY;
    }

    /**
     * Takes or leaves the command seat of the player's side. A seat is free while nobody holds it, or while its holder is
     * no longer playing for the side.
     *
     * @return whether the player holds the seat afterwards
     */
    public static boolean command(ServerPlayer player, boolean take)
    {
        Optional<BotBattle> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty())
            return false;
        BotBattle battle = current.get();
        int side = battle.getGround().sideOf(team.get());
        UUID holder = battle.getCommanders().get(side);
        if (!take)
        {
            if (player.getUUID().equals(holder))
            {
                battle.getCommanders().remove(side);
                tellSide(player, battle, side, Component.translatable("wolffsmodnpcs.orders.commander_left", player.getDisplayName()));
            }
            return false;
        }
        if (holder != null && !holder.equals(player.getUUID()) && holdsSeat(player, battle, side, holder))
            return false;
        if (!player.getUUID().equals(holder))
            tellSide(player, battle, side, Component.translatable("wolffsmodnpcs.orders.new_commander", player.getDisplayName()));
        battle.getCommanders().put(side, player.getUUID());
        return true;
    }

    /** Tells every player of a side, the one who changed the seat included, who commands the side now. */
    private static void tellSide(ServerPlayer source, BotBattle battle, int side, Component message)
    {
        for (ServerPlayer player : Objects.requireNonNull(source.getServer()).getPlayerList().getPlayers())
            if (battle.getGround().sideOf(player) == side)
                player.sendSystemMessage(message);
    }

    /** Whether the holder of a side's seat still plays for that side. */
    private static boolean holdsSeat(ServerPlayer asking, BotBattle battle, int side, UUID holder)
    {
        ServerPlayer player = Objects.requireNonNull(asking.getServer()).getPlayerList().getPlayer(holder);
        return player != null && battle.getGround().sideOf(player) == side;
    }

    /** What the player's order screen shows: the squads they may order and the objectives they may send them to. */
    public static BotOrdersStatePacket state(ServerPlayer player)
    {
        Optional<BotBattle> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty() || !(player.level() instanceof ServerLevel level))
            return BotOrdersStatePacket.notPlaying();
        BotBattle battle = current.get();
        int side = battle.getGround().sideOf(team.get());
        UUID holder = battle.getCommanders().get(side);
        boolean commander = player.getUUID().equals(holder);
        String commanderName = holder == null ? "" : Optional.ofNullable(Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(holder)).map(ServerPlayer::getScoreboardName).orElse("");
        List<BotOrdersStatePacket.Squad> squads = new ArrayList<>();
        for (BotSquad squad : battle.getSquads().values())
        {
            boolean listed = !squad.isDisbanded() && battle.sideOf(squad.getTeam()) == side && squad.getRole() != BotRole.AA && (commander || hears(level, battle, squad, player));
            List<Entity> members = listed ? BotCommander.livingMembers(level, battle, squad) : List.of();
            if (!members.isEmpty())
                squads.add(line(squad, members, player));
        }
        squads.sort(Comparator.comparingInt(BotOrdersStatePacket.Squad::distance));
        List<BotOrdersStatePacket.Objective> objectives = battle.getGround().capturePoints().stream()
            .map(point -> new BotOrdersStatePacket.Objective(point.symbol(), point.name(), point.ownerSide(), point.position().x, point.position().z)).toList();
        Optional<BotSquad> led = BotLeaders.ledBy(battle, player.getUUID());
        BotOrdersStatePacket.Leading leading = new BotOrdersStatePacket.Leading(led.map(BotSquad::getId).orElse(-1), led.map(squad -> squad.getRally() != null).orElse(false),
            battle.getSpawnChoices().getOrDefault(player.getUUID(), BotLeaders.SpawnChoice.BASE).ordinal(), battle.getSettings().isRallyPoints());
        return new BotOrdersStatePacket(true, side, commander, commanderName, squads, objectives, leading, false);
    }

    /** A squad as the order screen lists it, with what it is doing and how far its leader is. */
    private static BotOrdersStatePacket.Squad line(BotSquad squad, List<Entity> members, ServerPlayer player)
    {
        String task;
        if (squad.getOrder() != null)
            task = "order: " + lower(squad.getOrder().kind());
        else
            task = squad.getTask() == null ? "idle" : lower(squad.getTask().kind());
        boolean mine = squad.getOrder() != null && squad.getOrder().player().equals(player.getUUID());
        Entity leader = members.get(0);
        return new BotOrdersStatePacket.Squad(squad.getId(), squadName(squad), members.size(), task, mine, (int) leader.distanceTo(player), leader.getX(), leader.getZ());
    }

    private static String squadName(BotSquad squad)
    {
        String kind = squad.getTemplate().isEmpty() ? lower(squad.getRole()) : squad.getTemplate();
        return "#" + squad.getId() + " " + squad.getTeam() + " " + kind;
    }

    private static String lower(Enum<?> constant)
    {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
