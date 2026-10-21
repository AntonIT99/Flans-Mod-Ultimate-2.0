package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Commands the bots of every side: gives each squad a task by its side's doctrine, tells each bot where to stand for its
 * squad's task, and sums the bots up for the scoreboard. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotCommander
{
    /** Blocks above its target an aircraft patrols at. */
    private static final double PLANE_ALTITUDE = 28D;
    private static final double HELICOPTER_ALTITUDE = 14D;
    /** Blocks beyond the radius of a defended objective within which its garrison and sniper posts count. */
    private static final double POST_MARGIN = 24D;
    /** Bots a team may have before the automatic scoreboard sums them up in one row. */
    private static final int INDIVIDUAL_ROWS = 12;
    private static final String ROW_PREFIX = "[AI] ";

    /**
     * Where a bot goes for its squad's task.
     *
     * @param arrival
     *            blocks from its spot within which the bot counts as there
     */
    record Order(Vec3 spot, double arrival, BotRole role, SquadTask.Kind kind)
    {}

    /** Gives every squad of every side a task. */
    static void plan(MinecraftServer server, ServerLevel level, BotRound round)
    {
        ITeamsLiveRound live = round.getLive();
        List<TeamsCapturePoint> points = live.getCapturePoints();
        int attacking = live.getAttackingSide().orElse(-1);
        List<Vec3> naval = BotMarkers.get(server).positions(live.getRound().getMapId(), BotMarkers.Type.NAVAL);
        Map<Integer, List<Vec3>> fighters = fightersBySide(server, level, round);
        for (int side = 0; side < live.getRound().getSideCount(); side++)
        {
            int ownSide = side;
            List<Vec3> enemies = new ArrayList<>();
            fighters.forEach((fighterSide, positions) ->
            {
                if (fighterSide != ownSide)
                    enemies.addAll(positions);
            });
            List<Vec3> enemySpawns = new ArrayList<>();
            for (ITeam team : live.getRound().getTeams())
                if (live.getRound().getSideOf(team) != side)
                    enemySpawns.addAll(live.getSpawnPositions(team));
            List<Doctrine.SquadView> views = views(level, round, side);
            if (views.isEmpty())
                continue;
            Doctrine.plan(new Doctrine.Battlefield(side, attacking, points, enemies, enemySpawns, naval), views)
                .forEach((squadId, task) -> Optional.ofNullable(round.getSquads().get(squadId)).ifPresent(squad -> squad.setTask(task)));
        }
    }

    /** Where every living player and bot of the round is, by side. */
    private static Map<Integer, List<Vec3>> fightersBySide(MinecraftServer server, ServerLevel level, BotRound round)
    {
        Map<Integer, List<Vec3>> fighters = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isAlive() && !player.isSpectator() && player.level() == level)
                FlansTeams.getTeam(player).ifPresent(team -> fighters.computeIfAbsent(round.getLive().getRound().getSideOf(team), ignored -> new ArrayList<>()).add(player.position()));
        }
        for (BotSlot slot : round.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity != null && entity.isAlive())
                fighters.computeIfAbsent(round.sideOf(slot.getTeam()), ignored -> new ArrayList<>()).add(entity.position());
        }
        return fighters;
    }

    private static List<Doctrine.SquadView> views(ServerLevel level, BotRound round, int side)
    {
        List<Doctrine.SquadView> views = new ArrayList<>();
        for (BotSquad squad : round.getSquads().values())
        {
            List<Vec3> members = squad.isDisbanded() || round.sideOf(squad.getTeam()) != side ? List.of() : livingMembers(level, round, squad);
            if (!members.isEmpty())
                views.add(new Doctrine.SquadView(squad.getId(), squad.getRole(), posture(squad), members.get(0), members.size()));
        }
        return views;
    }

    /** Where the living members of a squad are, the leader first. */
    private static List<Vec3> livingMembers(ServerLevel level, BotRound round, BotSquad squad)
    {
        List<Vec3> members = new ArrayList<>();
        for (int slotId : squad.getSlots())
        {
            BotSlot slot = round.getSlots().get(slotId);
            Entity entity = slot == null || slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity != null && entity.isAlive())
                members.add(entity.position());
        }
        return members;
    }

    private static SquadTemplate.Posture posture(BotSquad squad)
    {
        if (squad.getTemplate().isEmpty())
            return SquadTemplate.Posture.BALANCED;
        return SquadTemplates.get().find(squad.getTemplate()).map(SquadTemplate::posture).orElse(SquadTemplate.Posture.BALANCED);
    }

    /** @return where a bot should be for its squad's task, or empty for an NPC that is no bot of the round or has none */
    static Optional<Order> order(MinecraftServer server, BotRound round, EntityNPCInterface npc)
    {
        Integer slotId = round.getSlotOfEntity().get(npc.getUUID());
        BotSlot slot = slotId == null ? null : round.getSlots().get(slotId);
        BotSquad squad = slot == null ? null : round.getSquads().get(slot.getSquad());
        SquadTask task = squad == null ? null : squad.getTask();
        if (task == null)
            return Optional.empty();
        int index = Math.max(0, squad.getSlots().indexOf(slot.getId()));
        List<Vec3> posts = List.of();
        if (squad.getRole() == BotRole.INFANTRY && (task.kind() == SquadTask.Kind.DEFEND || task.kind() == SquadTask.Kind.GUARD))
            posts = posts(server, round, task);
        Vec3 spot = BotFormation.spot(task.target(), index, task.radius(), posts);
        double arrival = 2.5D;
        if (squad.getRole() == BotRole.PLANE || squad.getRole() == BotRole.HELICOPTER)
        {
            spot = spot.add(0D, squad.getRole() == BotRole.PLANE ? PLANE_ALTITUDE : HELICOPTER_ALTITUDE, 0D);
            arrival = task.radius();
        }
        else if (squad.getRole() != BotRole.INFANTRY)
            arrival = 6D;
        return Optional.of(new Order(spot, arrival, squad.getRole(), task.kind()));
    }

    /** The sniper posts, then the garrison posts, around a defended objective, nearest first. */
    private static List<Vec3> posts(MinecraftServer server, BotRound round, SquadTask task)
    {
        String map = round.getLive().getRound().getMapId();
        double reach = task.radius() + POST_MARGIN;
        Comparator<Vec3> nearest = Comparator.comparingDouble(post -> post.distanceToSqr(task.target()));
        List<Vec3> posts = new ArrayList<>();
        BotMarkers markers = BotMarkers.get(server);
        markers.positions(map, BotMarkers.Type.SNIPER).stream().filter(post -> post.distanceTo(task.target()) <= reach).sorted(nearest).forEach(posts::add);
        markers.positions(map, BotMarkers.Type.GARRISON).stream().filter(post -> post.distanceTo(task.target()) <= reach).sorted(nearest).forEach(posts::add);
        return posts;
    }

    /** Shows each team's bots on the scoreboard, one by one or summed up, as the round's settings say. */
    static void scoreboard(BotRound round)
    {
        for (ITeam team : round.getLive().getRound().getTeams())
        {
            List<BotSlot> slots = round.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && (!slot.isRetired() || slot.getKills() > 0 || slot.getDeaths() > 0))
                .toList();
            BotSettings.Scoreboard mode = round.getSettings().getScoreboard();
            boolean individual = mode == BotSettings.Scoreboard.INDIVIDUAL || mode == BotSettings.Scoreboard.AUTO && slots.size() <= INDIVIDUAL_ROWS;
            List<TeamsScoreEntry> rows = new ArrayList<>();
            if (individual)
            {
                for (BotSlot slot : slots)
                    rows.add(new TeamsScoreEntry(ROW_PREFIX + (slot.getName().isEmpty() ? "#" + slot.getId() : slot.getName()), slot.getScore(), slot.getKills(), slot.getDeaths(), label(slot)));
            }
            else if (!slots.isEmpty())
            {
                int kills = slots.stream().mapToInt(BotSlot::getKills).sum();
                int deaths = slots.stream().mapToInt(BotSlot::getDeaths).sum();
                int score = slots.stream().mapToInt(BotSlot::getScore).sum();
                long active = slots.stream().filter(slot -> !slot.isRetired()).count();
                rows.add(new TeamsScoreEntry(ROW_PREFIX + "x" + active, score, kills, deaths, ""));
            }
            FlansTeams.setScoreboardEntries(team, rows);
        }
    }

    private static String label(BotSlot slot)
    {
        return FlansModApi.getPlayerClass(slot.getRole()).map(IContentType::getName).or(() -> FlansModApi.getType(slot.getRole()).map(IContentType::getName)).orElse(slot.getRole());
    }
}
