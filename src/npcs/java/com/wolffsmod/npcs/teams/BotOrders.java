package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.TeamsCapturePoint;
import com.wolffsmod.npcs.network.BotOrdersStatePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
    /** The objective index that stands for the player's own position. */
    public static final int HERE = -1;

    /**
     * Orders squads of the player's side.
     *
     * @param squadId
     *            a squad, or {@link #NEARBY_SQUADS}
     * @param objective
     *            the capture point to attack or defend, by play order, or {@link #HERE}
     * @return how many squads took the order; 0 when the player is not playing or no squad hears them
     */
    public static int give(ServerPlayer player, int squadId, PlayerOrder.Kind kind, int objective)
    {
        Optional<BotRound> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty() || !(player.level() instanceof ServerLevel level))
            return 0;
        BotRound round = current.get();
        int side = round.getLive().getRound().getSideOf(team.get());
        boolean commander = player.getUUID().equals(round.getCommanders().get(side));
        Vec3 target = target(round, player, objective);
        PlayerOrder order = new PlayerOrder(player.getUUID(), kind, target, level.getGameTime() + PlayerOrder.DURATION);
        int ordered = 0;
        for (BotSquad squad : round.getSquads().values())
        {
            boolean chosen = squadId == NEARBY_SQUADS ? hears(level, round, squad, player) : squad.getId() == squadId && (commander || hears(level, round, squad, player));
            if (!chosen || squad.isDisbanded() || round.sideOf(squad.getTeam()) != side || squad.getRole() == BotRole.AA)
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

    /** Where an order sends a squad: the capture point chosen, or the player's own position. */
    private static Vec3 target(BotRound round, ServerPlayer player, int objective)
    {
        List<TeamsCapturePoint> points = round.getLive().getCapturePoints();
        return objective >= 0 && objective < points.size() ? points.get(objective).position() : player.position();
    }

    /** Whether a squad's leader is near enough to hear the player, or the squad already follows the player's orders. */
    private static boolean hears(ServerLevel level, BotRound round, BotSquad squad, ServerPlayer player)
    {
        if (squad.getOrder() != null && squad.getOrder().player().equals(player.getUUID()))
            return true;
        List<Entity> members = BotCommander.livingMembers(level, round, squad);
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
        Optional<BotRound> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty())
            return false;
        BotRound round = current.get();
        int side = round.getLive().getRound().getSideOf(team.get());
        UUID holder = round.getCommanders().get(side);
        if (!take)
        {
            if (player.getUUID().equals(holder))
                round.getCommanders().remove(side);
            return false;
        }
        if (holder != null && !holder.equals(player.getUUID()) && holdsSeat(player, round, side, holder))
            return false;
        round.getCommanders().put(side, player.getUUID());
        return true;
    }

    /** Whether the holder of a side's seat still plays for that side. */
    private static boolean holdsSeat(ServerPlayer asking, BotRound round, int side, UUID holder)
    {
        ServerPlayer player = Objects.requireNonNull(asking.getServer()).getPlayerList().getPlayer(holder);
        return player != null && FlansTeams.getTeam(player).map(team -> round.getLive().getRound().getSideOf(team) == side).orElse(false);
    }

    /** What the player's order screen shows: the squads they may order and the objectives they may send them to. */
    public static BotOrdersStatePacket state(ServerPlayer player)
    {
        Optional<BotRound> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty() || !(player.level() instanceof ServerLevel level))
            return BotOrdersStatePacket.notPlaying();
        BotRound round = current.get();
        int side = round.getLive().getRound().getSideOf(team.get());
        UUID holder = round.getCommanders().get(side);
        boolean commander = player.getUUID().equals(holder);
        String commanderName = holder == null ? "" : Optional.ofNullable(Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(holder)).map(ServerPlayer::getScoreboardName).orElse("");
        List<BotOrdersStatePacket.Squad> squads = new ArrayList<>();
        for (BotSquad squad : round.getSquads().values())
        {
            boolean listed = !squad.isDisbanded() && round.sideOf(squad.getTeam()) == side && squad.getRole() != BotRole.AA && (commander || hears(level, round, squad, player));
            List<Entity> members = listed ? BotCommander.livingMembers(level, round, squad) : List.of();
            if (!members.isEmpty())
                squads.add(line(squad, members, player));
        }
        squads.sort(Comparator.comparingInt(BotOrdersStatePacket.Squad::distance));
        List<BotOrdersStatePacket.Objective> objectives = round.getLive().getCapturePoints().stream().map(point -> new BotOrdersStatePacket.Objective(point.symbol(), point.name(), point.ownerSide()))
            .toList();
        return new BotOrdersStatePacket(true, side, commander, commanderName, squads, objectives);
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
        return new BotOrdersStatePacket.Squad(squad.getId(), squadName(squad), members.size(), task, mine, (int) members.get(0).distanceTo(player));
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
