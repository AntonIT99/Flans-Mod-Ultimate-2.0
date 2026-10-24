package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.TeamsCapturePoint;
import com.flansmodultimate.api.teams.TeamsFlag;
import com.flansmodultimate.api.teams.TeamsScoreEntry;
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
    static void plan(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        Battleground ground = battle.getGround();
        List<TeamsCapturePoint> points = ground.capturePoints();
        int attacking = ground.attackingSide().orElse(-1);
        List<Vec3> naval = markers(server, battle, BotMarkers.Type.NAVAL);
        Map<Integer, List<Vec3>> fighters = fightersBySide(server, level, battle);
        List<TeamsFlag> flags = ground.flags();
        List<Doctrine.FlagView> flagViews = flags.stream().map(flag -> flagView(server, level, battle, flag)).toList();
        Set<UUID> carriers = new HashSet<>();
        flags.stream().map(TeamsFlag::carrier).filter(Objects::nonNull).forEach(carriers::add);
        BotLeaders.tick(server, level, battle);
        applyOrders(server, level, battle);
        for (int side = 0; side < ground.sideCount(); side++)
        {
            int ownSide = side;
            List<Vec3> enemies = new ArrayList<>();
            fighters.forEach((fighterSide, positions) ->
            {
                if (ground.hostile(ownSide, fighterSide))
                    enemies.addAll(positions);
            });
            List<Vec3> enemySpawns = new ArrayList<>();
            for (ITeam team : ground.teams())
                if (ground.hostile(side, ground.sideOf(team)))
                    enemySpawns.addAll(ground.spawnPositions(team));
            List<Doctrine.SquadView> views = views(level, battle, side, carriers);
            if (!views.isEmpty())
                Doctrine.plan(new Doctrine.Battlefield(side, attacking, points, enemies, enemySpawns, naval, flagViews, ground.mayCapture(side), ground.front().orElse(null)), views)
                    .forEach((squadId, task) -> Optional.ofNullable(battle.getSquads().get(squadId)).ifPresent(squad -> squad.setTask(task)));
        }
    }

    /**
     * Gives squads under a player's order the task of that order, and returns the others to their commander once their order
     * ran out, was released, or its player left the round.
     */
    private static void applyOrders(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        long now = level.getGameTime();
        for (BotSquad squad : battle.getSquads().values())
        {
            PlayerOrder order = squad.getOrder();
            if (order == null)
                continue;
            ServerPlayer player = server.getPlayerList().getPlayer(order.player());
            boolean playing = player != null && player.isAlive() && battle.sideOfPlayer(player).isPresent();
            if (now >= order.expiresAt() || order.kind() == PlayerOrder.Kind.RELEASE || order.kind() == PlayerOrder.Kind.FOLLOW && !playing)
                squad.setOrder(null);
            else
                squad.setTask(order.task(player == null ? order.target() : player.position()));
        }
    }

    /** A flag as a commander sees it, with the side of whoever carries it. */
    private static Doctrine.FlagView flagView(MinecraftServer server, ServerLevel level, BotBattle battle, TeamsFlag flag)
    {
        int owner = flag.owner() == null ? -1 : battle.getGround().sideOf(flag.owner());
        int carrierSide = -1;
        if (flag.carrier() != null)
        {
            Entity carrier = server.getPlayerList().getPlayer(flag.carrier());
            if (carrier == null)
                carrier = level.getEntity(flag.carrier());
            carrierSide = carrier == null ? -1 : battle.getGround().sideOf(carrier);
        }
        return new Doctrine.FlagView(owner, flag.position(), flag.home(), flag.atHome(), carrierSide);
    }

    /** Where every living player and bot of the round is, by side. */
    private static Map<Integer, List<Vec3>> fightersBySide(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        Map<Integer, List<Vec3>> fighters = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isAlive() && !player.isSpectator() && player.level() == level)
                battle.sideOfPlayer(player).ifPresent(side -> fighters.computeIfAbsent(side, ignored -> new ArrayList<>()).add(player.position()));
        }
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity != null && entity.isAlive())
                fighters.computeIfAbsent(battle.sideOf(slot.getTeam()), ignored -> new ArrayList<>()).add(entity.position());
        }
        return fighters;
    }

    /**
     * The squads of a side its commander plans for: not those under a player's order. A transport still carrying its
     * squad is planned as infantry, so that it heads for an objective to unload at.
     */
    private static List<Doctrine.SquadView> views(ServerLevel level, BotBattle battle, int side, Set<UUID> carriers)
    {
        List<Doctrine.SquadView> views = new ArrayList<>();
        for (BotSquad squad : battle.getSquads().values())
        {
            boolean planned = !squad.isDisbanded() && squad.getOrder() == null && battle.sideOf(squad.getTeam()) == side;
            List<Entity> members = planned ? livingMembers(level, battle, squad) : List.of();
            if (members.isEmpty())
                continue;
            BotRole role = squad.getCargo().isEmpty() ? squad.getRole() : BotRole.INFANTRY;
            boolean carrying = members.stream().anyMatch(member -> carriers.contains(member.getUUID()));
            views.add(new Doctrine.SquadView(squad.getId(), role, posture(squad), members.get(0).position(), members.size(), carrying, squad.getTask() == null ? null : squad.getTask().target()));
        }
        return views;
    }

    /** The living members of a squad, the leader first. */
    static List<Entity> livingMembers(ServerLevel level, BotBattle battle, BotSquad squad)
    {
        List<Entity> members = new ArrayList<>();
        for (int slotId : squad.getSlots())
        {
            BotSlot slot = battle.getSlots().get(slotId);
            Entity entity = slot == null || slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity != null && entity.isAlive())
                members.add(entity);
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
    static Optional<Order> order(MinecraftServer server, BotBattle battle, EntityNPCInterface npc)
    {
        Integer slotId = battle.getSlotOfEntity().get(npc.getUUID());
        BotSlot slot = slotId == null ? null : battle.getSlots().get(slotId);
        BotSquad squad = slot == null ? null : battle.getSquads().get(slot.getSquad());
        SquadTask task = squad == null ? null : squad.getTask();
        if (task == null)
            return Optional.empty();
        // A squad told to follow keeps up with its player between two commander passes.
        PlayerOrder playerOrder = squad.getOrder();
        ServerPlayer followed = playerOrder != null && playerOrder.kind() == PlayerOrder.Kind.FOLLOW ? server.getPlayerList().getPlayer(playerOrder.player()) : null;
        if (followed != null)
            task = playerOrder.task(followed.position());
        int index = Math.max(0, squad.getSlots().indexOf(slot.getId()));
        List<Vec3> posts = List.of();
        if (squad.getRole() == BotRole.INFANTRY && (task.kind() == SquadTask.Kind.DEFEND || task.kind() == SquadTask.Kind.GUARD))
            posts = posts(server, battle, task);
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
    private static List<Vec3> posts(MinecraftServer server, BotBattle battle, SquadTask task)
    {
        double reach = task.radius() + POST_MARGIN;
        Comparator<Vec3> nearest = Comparator.comparingDouble(post -> post.distanceToSqr(task.target()));
        List<Vec3> posts = new ArrayList<>();
        markers(server, battle, BotMarkers.Type.SNIPER).stream().filter(post -> post.distanceTo(task.target()) <= reach).sorted(nearest).forEach(posts::add);
        markers(server, battle, BotMarkers.Type.GARRISON).stream().filter(post -> post.distanceTo(task.target()) <= reach).sorted(nearest).forEach(posts::add);
        return posts;
    }

    /** The markers of a type on the map of the battle; none for a battle on no map. */
    static List<Vec3> markers(MinecraftServer server, BotBattle battle, BotMarkers.Type type)
    {
        return battle.getGround().mapId().map(map -> BotMarkers.get(server).positions(map, type)).orElse(List.of());
    }

    /** Shows each team's bots on the scoreboard, one by one or summed up, as the round's settings say. */
    static void scoreboard(BotBattle battle)
    {
        for (ITeam team : battle.getGround().teams())
        {
            List<BotSlot> slots = battle.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && (!slot.isRetired() || slot.getKills() > 0 || slot.getDeaths() > 0))
                .toList();
            BotSettings.Scoreboard mode = battle.getSettings().getScoreboard();
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
