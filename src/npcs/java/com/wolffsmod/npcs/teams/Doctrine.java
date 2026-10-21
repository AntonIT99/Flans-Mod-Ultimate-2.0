package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.TeamsCapturePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;

/**
 * How a side's commander deploys its squads, given the state of the round. Objective rounds are fought over their capture
 * points: Assault attackers push the objective in play while its defenders hold it and the next one, and Domination and
 * Conquest squads weigh every point by how much it needs them. Rounds without points send squads hunting the enemy.
 * Pure arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Doctrine
{
    private static final double HUNT_RADIUS = 16D;
    private static final double PLANE_PATROL_RADIUS = 48D;
    private static final double HELICOPTER_PATROL_RADIUS = 24D;
    /** Value of a point the side does not hold, and the extra for one nobody holds, which is quicker to take. */
    private static final double ATTACK_VALUE = 10D;
    private static final double NEUTRAL_BONUS = 3D;
    /** Value of a held point the enemy is taking or closing in on. */
    private static final double THREATENED_VALUE = 12D;
    /** Value of a held point nobody threatens. */
    private static final double SAFE_VALUE = 3D;
    private static final double POSTURE_BONUS = 3D;
    private static final double ARMOR_ATTACK_BONUS = 2D;
    /** Value lost for each squad already sent to a point, which spreads squads over the points. */
    private static final double CROWDING = 4D;
    /** Blocks of travel that cost one point of value. */
    private static final double DISTANCE_SCALE = 64D;
    /** Blocks beyond a point's radius within which an enemy counts as closing in on it. */
    private static final double THREAT_MARGIN = 8D;
    /** Share of the Assault defenders that holds the objective in play; the rest hold the next one. */
    private static final double FRONT_SHARE = 2D / 3D;
    /** Blocks around a flag a squad spreads over, close enough for every member to touch it. */
    private static final double FLAG_RADIUS = 2D;
    /** Blocks around its own flag a guarding squad spreads over. */
    private static final double GUARD_RADIUS = 8D;

    /**
     * A squad as the commander sees it.
     *
     * @param strength
     *            its living members
     * @param carrying
     *            whether one of its members carries an enemy flag
     */
    public record SquadView(int id, BotRole role, SquadTemplate.Posture posture, Vec3 position, int strength, boolean carrying)
    {}

    /**
     * A Capture the Flag flag as a commander sees it.
     *
     * @param ownerSide
     *            the side whose flag it is
     * @param home
     *            where it stands at its base
     * @param carrierSide
     *            the side of whoever carries it, or -1
     */
    public record FlagView(int ownerSide, Vec3 position, Vec3 home, boolean atHome, int carrierSide)
    {}

    /**
     * The round as one side sees it. Sides are numbered as the Teams API numbers them.
     *
     * @param attackingSide
     *            the side that attacks in Assault, -1 in other game types
     * @param points
     *            the capture points in play order; empty in game types without them
     * @param enemies
     *            where the enemy's players and bots are
     * @param navalPoints
     *            the patrol points ships may use
     * @param flags
     *            the flags in Capture the Flag; empty in other game types
     */
    public record Battlefield(int side, int attackingSide, List<TeamsCapturePoint> points, List<Vec3> enemies, List<Vec3> enemySpawns, List<Vec3> navalPoints, List<FlagView> flags)
    {}

    /** @return a task for every squad that has one; squads left out keep theirs */
    public static Map<Integer, SquadTask> plan(Battlefield field, List<SquadView> squads)
    {
        Map<Integer, SquadTask> tasks = new LinkedHashMap<>();
        List<SquadView> ground = new ArrayList<>();
        List<SquadView> air = new ArrayList<>();
        for (SquadView squad : squads)
        {
            switch (squad.role())
            {
                case AA -> tasks.put(squad.id(), new SquadTask(SquadTask.Kind.HOLD, squad.position(), 4D));
                case NAVAL -> tasks.put(squad.id(), naval(field, squad));
                case PLANE, HELICOPTER -> air.add(squad);
                default -> ground.add(squad);
            }
        }
        if (!field.flags().isEmpty())
            planFlags(field, ground, tasks);
        else if (field.points().isEmpty())
            planHunt(field, ground, tasks);
        else if (field.attackingSide() >= 0)
            planAssault(field, ground, tasks);
        else
            planObjectives(field, ground, tasks);
        Vec3 focus = airFocus(field, tasks);
        for (SquadView squad : air)
        {
            double radius = squad.role() == BotRole.HELICOPTER ? HELICOPTER_PATROL_RADIUS : PLANE_PATROL_RADIUS;
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.PATROL, focus == null ? squad.position() : focus, radius));
        }
        return tasks;
    }

    /** Without objectives, squads seek out the nearest enemy, or the enemy's spawn when none is seen. */
    private static void planHunt(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        for (SquadView squad : squads)
        {
            Vec3 target = nearest(squad.position(), field.enemies()).or(() -> nearest(squad.position(), field.enemySpawns())).orElse(squad.position());
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.HUNT, target, HUNT_RADIUS));
        }
    }

    /**
     * Capture the Flag: a squad carrying an enemy flag takes it home; some squads go after a carrier of the side's own flag,
     * or bring a dropped one back; half the others go for the nearest enemy flag, and the rest guard their own.
     */
    private static void planFlags(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        List<FlagView> own = field.flags().stream().filter(flag -> flag.ownerSide() == field.side()).toList();
        List<FlagView> enemy = field.flags().stream().filter(flag -> flag.ownerSide() != field.side() && flag.carrierSide() != field.side()).toList();
        List<SquadView> free = new ArrayList<>();
        for (SquadView squad : squads)
        {
            Optional<FlagView> home = nearestFlag(squad.position(), own, FlagView::home);
            if (squad.carrying() && home.isPresent())
                tasks.put(squad.id(), new SquadTask(SquadTask.Kind.ATTACK, home.get().home(), FLAG_RADIUS));
            else
                free.add(squad);
        }
        free.sort(Comparator.comparingInt(SquadView::id));
        for (FlagView flag : own)
        {
            if (!flag.atHome())
                recover(flag, free, tasks);
        }
        int raiders = enemy.isEmpty() ? 0 : (free.size() + 1) / 2;
        for (int i = 0; i < free.size(); i++)
            raidOrGuard(field, free.get(i), i < raiders, own, enemy, tasks);
    }

    /**
     * Sends the free squads nearest a flag of the side that left its base after it: a third of them hunt its carrier, or
     * one brings it back once dropped. The squads sent are taken from the free ones.
     */
    private static void recover(FlagView flag, List<SquadView> free, Map<Integer, SquadTask> tasks)
    {
        boolean carried = flag.carrierSide() >= 0;
        int hunters = Math.min(free.size(), carried ? Math.max(1, free.size() / 3) : 1);
        free.sort(Comparator.comparingDouble(squad -> squad.position().distanceToSqr(flag.position())));
        List<SquadView> sent = List.copyOf(free.subList(0, hunters));
        free.removeAll(sent);
        for (SquadView squad : sent)
            tasks.put(squad.id(), new SquadTask(carried ? SquadTask.Kind.HUNT : SquadTask.Kind.ATTACK, flag.position(), FLAG_RADIUS));
    }

    /** A free squad goes for the nearest enemy flag when it is a raider, and guards its own side's flag otherwise. */
    private static void raidOrGuard(Battlefield field, SquadView squad, boolean raider, List<FlagView> own, List<FlagView> enemy, Map<Integer, SquadTask> tasks)
    {
        Optional<FlagView> target = raider ? nearestFlag(squad.position(), enemy, FlagView::position) : Optional.empty();
        Optional<FlagView> guarded = nearestFlag(squad.position(), own, FlagView::home);
        if (target.isPresent())
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.ATTACK, target.get().position(), FLAG_RADIUS));
        else if (guarded.isPresent())
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.DEFEND, guarded.get().home(), GUARD_RADIUS));
        else
            planHunt(field, List.of(squad), tasks);
    }

    private static Optional<FlagView> nearestFlag(Vec3 from, List<FlagView> flags, Function<FlagView, Vec3> where)
    {
        return flags.stream().min(Comparator.comparingDouble(flag -> where.apply(flag).distanceToSqr(from)));
    }

    /** Assault: attackers push the objective in play; defenders hold it, and some of them the next one. */
    private static void planAssault(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        int active = activeObjective(field.points(), field.attackingSide());
        if (active >= field.points().size())
        {
            planHunt(field, squads, tasks);
            return;
        }
        TeamsCapturePoint front = field.points().get(active);
        if (field.side() == field.attackingSide())
        {
            for (SquadView squad : squads)
                tasks.put(squad.id(), new SquadTask(SquadTask.Kind.ATTACK, front.position(), front.radius()));
            return;
        }
        TeamsCapturePoint second = active + 1 < field.points().size() ? field.points().get(active + 1) : front;
        List<SquadView> ordered = new ArrayList<>(squads);
        // Defensive squads go to the front first; nearest first within each posture.
        ordered.sort(Comparator.comparing((SquadView squad) -> squad.posture() != SquadTemplate.Posture.DEFENSE).thenComparingDouble(squad -> squad.position().distanceTo(front.position())));
        int atFront = (int) Math.ceil(ordered.size() * FRONT_SHARE);
        for (int i = 0; i < ordered.size(); i++)
        {
            TeamsCapturePoint point = i < atFront ? front : second;
            tasks.put(ordered.get(i).id(), new SquadTask(SquadTask.Kind.DEFEND, point.position(), point.radius()));
        }
    }

    /** Domination and Conquest: each squad takes the point worth most to it, worth less the more squads went there. */
    private static void planObjectives(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        Map<Integer, Integer> assigned = new HashMap<>();
        List<SquadView> ordered = new ArrayList<>(squads);
        ordered.sort(Comparator.comparingInt(SquadView::id));
        for (SquadView squad : ordered)
        {
            int best = -1;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < field.points().size(); i++)
            {
                double value = value(field, field.points().get(i), squad) - CROWDING * assigned.getOrDefault(i, 0);
                if (value > bestValue)
                {
                    bestValue = value;
                    best = i;
                }
            }
            TeamsCapturePoint point = field.points().get(best);
            assigned.merge(best, 1, Integer::sum);
            tasks.put(squad.id(), new SquadTask(kindAt(field, point), point.position(), point.radius()));
        }
    }

    static double value(Battlefield field, TeamsCapturePoint point, SquadView squad)
    {
        double value;
        boolean ours = point.ownerSide() == field.side();
        if (!ours)
        {
            value = ATTACK_VALUE + (point.ownerSide() < 0 ? NEUTRAL_BONUS : 0D);
            if (squad.posture() == SquadTemplate.Posture.ASSAULT)
                value += POSTURE_BONUS;
            if (squad.role() == BotRole.ARMOR)
                value += ARMOR_ATTACK_BONUS;
        }
        else
        {
            value = threatened(field, point) ? THREATENED_VALUE : SAFE_VALUE;
            if (squad.posture() == SquadTemplate.Posture.DEFENSE)
                value += POSTURE_BONUS;
        }
        return value - squad.position().distanceTo(point.position()) / DISTANCE_SCALE;
    }

    private static SquadTask.Kind kindAt(Battlefield field, TeamsCapturePoint point)
    {
        if (point.ownerSide() != field.side())
            return SquadTask.Kind.ATTACK;
        return threatened(field, point) ? SquadTask.Kind.DEFEND : SquadTask.Kind.GUARD;
    }

    /** Whether the enemy is taking a point the side holds, or closing in on it. */
    static boolean threatened(Battlefield field, TeamsCapturePoint point)
    {
        if (point.contested() || point.capturingSide() >= 0 && point.capturingSide() != field.side())
            return true;
        double reach = point.radius() * 2D + THREAT_MARGIN;
        return field.enemies().stream().anyMatch(enemy -> enemy.distanceTo(point.position()) <= reach);
    }

    /** The objective in play in Assault: the first one the attackers do not hold. */
    static int activeObjective(List<TeamsCapturePoint> points, int attackingSide)
    {
        for (int i = 0; i < points.size(); i++)
            if (!points.get(i).isHeldBy(attackingSide))
                return i;
        return points.size();
    }

    /** Where aircraft patrol: over the point most squads were sent to, or over the enemy when there is none. */
    private static Vec3 airFocus(Battlefield field, Map<Integer, SquadTask> tasks)
    {
        Map<Vec3, Integer> votes = new HashMap<>();
        for (SquadTask task : tasks.values())
            if (task.kind() == SquadTask.Kind.ATTACK || task.kind() == SquadTask.Kind.DEFEND)
                votes.merge(task.target(), 1, Integer::sum);
        return votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElseGet(() -> centroid(field.enemies()));
    }

    private static SquadTask naval(Battlefield field, SquadView squad)
    {
        Vec3 focus = field.points().isEmpty() ? centroid(field.enemies()) : field.points().get(Math.min(activeObjective(field.points(), field.attackingSide()), field.points().size() - 1)).position();
        Vec3 anchor = focus == null ? squad.position() : focus;
        return nearest(anchor, field.navalPoints()).map(point -> new SquadTask(SquadTask.Kind.PATROL, point, 16D)).orElse(new SquadTask(SquadTask.Kind.HOLD, squad.position(), 8D));
    }

    private static Optional<Vec3> nearest(Vec3 from, List<Vec3> candidates)
    {
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(from)));
    }

    @Nullable
    private static Vec3 centroid(List<Vec3> positions)
    {
        if (positions.isEmpty())
            return null;
        Vec3 sum = Vec3.ZERO;
        for (Vec3 position : positions)
            sum = sum.add(position);
        return sum.scale(1D / positions.size());
    }
}
