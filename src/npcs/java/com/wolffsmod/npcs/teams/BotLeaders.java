package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Players leading squads of bots, as squad leaders do in Battlefield and Forgotten Hope: a player takes the lead of an
 * allied infantry squad, which follows them unless they order it elsewhere. The leader may spawn on the squad or at a
 * rally point they place, where the squad's own fallen come back too; enemies close to a rally point overrun it. Server
 * thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotLeaders
{
    private static final String PREFIX = "wolffsmodnpcs.squad.";
    private static final String NOT_PLAYING = "not_playing";
    /** Ticks a rally point stands: five minutes. */
    static final long RALLY_TICKS = 5L * 60L * 20L;
    /** Blocks around a rally point within which an enemy overruns it, and within which none may be to place it. */
    private static final double OVERRUN = 8D;
    private static final double PLACE_CLEAR = 16D;
    /** Blocks around a squad member within which no enemy may be for its leader to spawn on it. */
    private static final double SPAWN_CLEAR = 12D;
    private static final double SPAWN_SPREAD = 2D;

    /** Where a player who leads a squad comes back after dying. */
    public enum SpawnChoice
    {
        /** Where the game type puts players. */
        BASE,
        /** On a member of the squad they lead, when no enemy is near it. */
        SQUAD,
        /** At the rally point of the squad they lead. */
        RALLY
    }

    /** @return the squad a player leads, if any */
    static Optional<BotSquad> ledBy(BotBattle battle, UUID player)
    {
        return battle.getSquads().values().stream().filter(squad -> player.equals(squad.getLeader()) && !squad.isDisbanded()).findFirst();
    }

    /**
     * Gives a player the lead of an allied infantry squad: the one they chose, or the nearest one that hears them. A
     * player leads one squad at most, and a squad has one leader.
     *
     * @return what the player is told
     */
    public static Component lead(ServerPlayer player, int squadId)
    {
        Optional<BotBattle> current = TeamsBots.current();
        Optional<ITeam> team = FlansTeams.getTeam(player);
        if (current.isEmpty() || team.isEmpty() || !(player.level() instanceof ServerLevel level))
            return failure(NOT_PLAYING);
        BotBattle battle = current.get();
        int side = battle.getGround().sideOf(team.get());
        Optional<BotSquad> chosen = battle.getSquads().values().stream()
            .filter(squad -> !squad.isDisbanded() && squad.getRole() == BotRole.INFANTRY && squad.getCargo().isEmpty() && battle.sideOf(squad.getTeam()) == side)
            .filter(squad -> squadId == BotOrders.NEARBY_SQUADS || squad.getId() == squadId)
            .filter(squad -> !BotCommander.livingMembers(level, battle, squad).isEmpty() && BotCommander.livingMembers(level, battle, squad).get(0).distanceTo(player) <= BotOrders.NEARBY)
            .min(Comparator.comparingDouble(squad -> BotCommander.livingMembers(level, battle, squad).get(0).distanceToSqr(player)));
        if (chosen.isEmpty())
            return failure("no_squad");
        BotSquad squad = chosen.get();
        if (squad.getLeader() != null && !squad.getLeader().equals(player.getUUID()) && stillLeads(player.server, battle, squad))
            return failure("taken");
        ledBy(battle, player.getUUID()).filter(other -> other != squad).ifPresent(BotLeaders::release);
        squad.setLeader(player.getUUID());
        follow(squad, player);
        return Component.translatable(PREFIX + "leading", "#" + squad.getId());
    }

    /** Gives up the lead of the squad a player leads; the squad returns to its commander. */
    public static Component leave(ServerPlayer player)
    {
        Optional<BotSquad> squad = TeamsBots.current().flatMap(battle -> ledBy(battle, player.getUUID()));
        if (squad.isEmpty())
            return failure("not_leading");
        release(squad.get());
        return Component.translatable(PREFIX + "left");
    }

    /** Places the rally point of the squad a player leads where the player stands, replacing the one it had. */
    public static Component rally(ServerPlayer player)
    {
        Optional<BotBattle> current = TeamsBots.current();
        Optional<BotSquad> squad = current.flatMap(battle -> ledBy(battle, player.getUUID()));
        if (current.isEmpty() || squad.isEmpty())
            return failure("not_leading");
        if (!current.get().getSettings().isRallyPoints())
            return failure("no_rally_points");
        if (!player.isAlive() || player.isSpectator() || !(player.level() instanceof ServerLevel level))
            return failure(NOT_PLAYING);
        int side = sideOf(current.get(), player);
        if (enemyWithin(player.server, level, current.get(), side, player.position(), PLACE_CLEAR))
            return failure("enemies_near");
        squad.get().setRally(player.position());
        squad.get().setRallyUntil(level.getGameTime() + RALLY_TICKS);
        return Component.translatable(PREFIX + "rally_placed", RALLY_TICKS / 20L / 60L);
    }

    /** Chooses where a player comes back after dying. */
    public static Component chooseSpawn(ServerPlayer player, SpawnChoice choice)
    {
        Optional<BotBattle> current = TeamsBots.current();
        if (current.isEmpty() || FlansTeams.getTeam(player).isEmpty())
            return failure(NOT_PLAYING);
        current.get().getSpawnChoices().put(player.getUUID(), choice);
        return Component.translatable(PREFIX + "spawn." + choice.name().toLowerCase(Locale.ROOT));
    }

    /** Where the player chose to come back, if that place is there and safe; empty leaves it to the game type. */
    static Optional<Vec3> spawnFor(MinecraftServer server, BotBattle battle, ServerPlayer player, RandomGenerator random)
    {
        SpawnChoice choice = battle.getSpawnChoices().getOrDefault(player.getUUID(), SpawnChoice.BASE);
        Optional<BotSquad> squad = ledBy(battle, player.getUUID());
        ServerLevel level = server.getLevel(battle.getGround().dimension());
        if (choice == SpawnChoice.BASE || squad.isEmpty() || level == null)
            return Optional.empty();
        if (choice == SpawnChoice.RALLY)
            return Optional.ofNullable(squad.get().getRally()).map(rally -> BotFactory.freeSpot(level, rally, random));
        int side = battle.sideOf(squad.get().getTeam());
        return BotCommander.livingMembers(level, battle, squad.get()).stream().filter(member -> !enemyWithin(server, level, battle, side, member.position(), SPAWN_CLEAR)).findFirst()
            .map(member -> BotFactory.freeSpot(level, member.position().add(random.nextDouble() * SPAWN_SPREAD - 1D, 0D, random.nextDouble() * SPAWN_SPREAD - 1D), random));
    }

    /**
     * Keeps the leaders and rally points up to date: a leader who left the side loses the squad, a squad follows its leader
     * again once they are back in the field, and a rally point runs out or is overrun.
     */
    static void tick(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        long now = level.getGameTime();
        for (BotSquad squad : battle.getSquads().values())
        {
            if (squad.getLeader() != null)
                tickLeader(server, level, battle, squad, now);
        }
    }

    private static void tickLeader(MinecraftServer server, ServerLevel level, BotBattle battle, BotSquad squad, long now)
    {
        ServerPlayer leader = server.getPlayerList().getPlayer(Objects.requireNonNull(squad.getLeader()));
        if (!stillLeads(server, battle, squad) || leader == null)
        {
            release(squad);
            return;
        }
        boolean inField = leader.isAlive() && !leader.isSpectator();
        PlayerOrder order = squad.getOrder();
        if (inField && order == null)
            follow(squad, leader);
        else if (!inField && order != null && order.player().equals(leader.getUUID()) && order.kind() == PlayerOrder.Kind.FOLLOW)
            squad.setOrder(null);
        tickRally(server, level, battle, squad, leader, now);
    }

    private static void tickRally(MinecraftServer server, ServerLevel level, BotBattle battle, BotSquad squad, ServerPlayer leader, long now)
    {
        Vec3 rally = squad.getRally();
        if (rally == null)
            return;
        if (now >= squad.getRallyUntil())
        {
            squad.setRally(null);
            leader.sendSystemMessage(Component.translatable(PREFIX + "rally_expired").withStyle(ChatFormatting.YELLOW));
        }
        else if (enemyWithin(server, level, battle, battle.sideOf(squad.getTeam()), rally, OVERRUN))
        {
            squad.setRally(null);
            leader.sendSystemMessage(Component.translatable(PREFIX + "rally_overrun").withStyle(ChatFormatting.RED));
        }
    }

    /** Whether the squad's leader is online and still plays for the squad's side. */
    private static boolean stillLeads(MinecraftServer server, BotBattle battle, BotSquad squad)
    {
        ServerPlayer leader = squad.getLeader() == null ? null : server.getPlayerList().getPlayer(squad.getLeader());
        return leader != null && FlansTeams.getTeam(leader).map(team -> battle.getGround().sideOf(team) == battle.sideOf(squad.getTeam())).orElse(false);
    }

    private static void follow(BotSquad squad, ServerPlayer leader)
    {
        PlayerOrder order = new PlayerOrder(leader.getUUID(), PlayerOrder.Kind.FOLLOW, leader.position(), Long.MAX_VALUE);
        squad.setOrder(order);
        squad.setTask(order.task(leader.position()));
    }

    private static void release(BotSquad squad)
    {
        PlayerOrder order = squad.getOrder();
        if (order != null && order.player().equals(squad.getLeader()))
            squad.setOrder(null);
        squad.setLeader(null);
        squad.setRally(null);
    }

    private static int sideOf(BotBattle battle, ServerPlayer player)
    {
        return battle.getGround().sideOf(player);
    }

    /** Whether a living enemy of a side, player or bot, is within some blocks of a place. */
    static boolean enemyWithin(MinecraftServer server, ServerLevel level, BotBattle battle, int side, Vec3 at, double within)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isAlive() && !player.isSpectator() && player.level() == level && player.position().distanceTo(at) <= within
                && FlansTeams.getTeam(player).map(team -> battle.getGround().sideOf(team) != side).orElse(false))
                return true;
        }
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof EntityNPCInterface npc && npc.isAlive() && battle.sideOf(slot.getTeam()) != side && npc.position().distanceTo(at) <= within)
                return true;
        }
        return false;
    }

    private static Component failure(String key)
    {
        return Component.translatable(PREFIX + key).withStyle(ChatFormatting.RED);
    }
}
