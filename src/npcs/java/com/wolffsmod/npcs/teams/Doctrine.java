package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.TeamsCapturePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;

/**
 * How a side's commander deploys its squads, given the state of the round. Objective rounds are fought over their capture
 * points: Assault attackers push the objective in play while its defenders hold it and the next one, and Domination and
 * Conquest squads weigh every point by how much it needs them. Defenders of a round played attacker against defender,
 * who cannot take points, hold the ones they have. Rounds without points send squads hunting the enemy.
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
    /** Extra value of every held point for defenders who cannot take a lost one back. */
    private static final double DEFENDER_BONUS = 4D;
    private static final double POSTURE_BONUS = 3D;
    private static final double ARMOR_ATTACK_BONUS = 2D;
    /** Value lost for each squad already sent to a point, which spreads squads over the points. */
    private static final double CROWDING = 4D;
    /** Value a point keeps for a squad already sent there, so squads do not swap between points worth about the same. */
    private static final double STAY_BONUS = 2D;
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
    /** Blocks from a squad within which an enemy draws it off its way to the front. */
    private static final double ENGAGE_RANGE = 48D;
    /** Blocks around the front a squad spreads over. */
    private static final double FRONT_RADIUS = 16D;
    /** Blocks beyond an objective's radius at which armour covering it stands off, out of the reach of hand-held anti-tank weapons. */
    private static final double OVERWATCH_STANDOFF = 24D;
    /** Blocks around its standoff an armour squad spreads over, enough for its vehicles to stand apart with their own lines of fire. */
    private static final double OVERWATCH_RADIUS = 12D;
    /** Blocks from the objective in play within which a naval squad already supports the battle, rather than making for a patrol point. */
    private static final double NAVAL_SUPPORT_RANGE = 96D;
    /** Blocks beyond the overwatch standoff a ship holds off an objective it shells, so that it stays out over its own water. */
    private static final double NAVAL_STANDOFF = 48D;
    /** Fewest blocks of water a naval squad spreads over, and the room each of its hulls is given within it. */
    private static final double NAVAL_RADIUS = 16D;
    private static final double NAVAL_SPACING = 24D;

    /**
     * A squad as the commander sees it.
     *
     * @param strength
     *            its living members
     * @param carrying
     *            whether one of its members carries an enemy flag
     * @param currentTarget
     *            where its current task sends it, or null; a squad keeps to it unless another point is clearly worth more
     * @param readiness
     *            fraction of readiness retained after casualties, injury, ammunition and suppression, from zero to one
     * @param antiArmour
     *            readiness-weighted members equipped to engage reported armour
     */
    public record SquadView(int id, BotRole role, SquadTemplate.Posture posture, Vec3 position, int strength, boolean carrying, @Nullable Vec3 currentTarget, double readiness, double antiArmour,
        @Nullable Vec3 failedTarget, @Nullable Vec3 supportTarget)
    {
        public SquadView(int id, BotRole role, SquadTemplate.Posture posture, Vec3 position, int strength, boolean carrying, @Nullable Vec3 currentTarget, double readiness, double antiArmour)
        {
            this(id, role, posture, position, strength, carrying, currentTarget, readiness, antiArmour, null, null);
        }

        public SquadView(int id, BotRole role, SquadTemplate.Posture posture, Vec3 position, int strength, boolean carrying, @Nullable Vec3 currentTarget)
        {
            this(id, role, posture, position, strength, carrying, currentTarget, 1D, role == BotRole.ARMOR ? strength : 0D);
        }

        double power()
        {
            return Math.max(0.25D, strength * Math.max(0D, Math.min(1D, readiness)));
        }
    }

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
     *            the enemy's reported positions, including unexpired last-known contacts
     * @param navalPoints
     *            the patrol points ships may use
     * @param flags
     *            the flags in Capture the Flag; empty in other game types
     * @param mayCapture
     *            whether the side may take points it does not hold; false for the defenders of a round played attacker
     *            against defender
     * @param front
     *            where the side's ground squads meet the enemy in a battle without objectives, or null to hunt the enemy
     *            wherever it is
     * @param armour
     *            positions of the reports classified as armour
     */
    public record Battlefield(int side, int attackingSide, List<TeamsCapturePoint> points, List<Vec3> enemies, List<Vec3> enemySpawns, List<Vec3> navalPoints, List<FlagView> flags, boolean mayCapture,
        @Nullable Vec3 front, List<Vec3> armour)
    {
        public Battlefield(int side, int attackingSide, List<TeamsCapturePoint> points, List<Vec3> enemies, List<Vec3> enemySpawns, List<Vec3> navalPoints, List<FlagView> flags, boolean mayCapture,
            @Nullable Vec3 front)
        {
            this(side, attackingSide, points, enemies, enemySpawns, navalPoints, flags, mayCapture, front, List.of());
        }

        public Battlefield(int side, int attackingSide, List<TeamsCapturePoint> points, List<Vec3> enemies, List<Vec3> enemySpawns, List<Vec3> navalPoints, List<FlagView> flags, boolean mayCapture)
        {
            this(side, attackingSide, points, enemies, enemySpawns, navalPoints, flags, mayCapture, null);
        }

        public Battlefield(int side, int attackingSide, List<TeamsCapturePoint> points, List<Vec3> enemies, List<Vec3> enemySpawns, List<Vec3> navalPoints, List<FlagView> flags)
        {
            this(side, attackingSide, points, enemies, enemySpawns, navalPoints, flags, true);
        }
    }

    /** Calculated once per objective and planning pass, rather than rescanning reports for each squad. */
    private record Pressure(double power, long armour, boolean threatened)
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

    /**
     * Without objectives, squads divide reported enemies, or enemy spawns when none are seen, by travel cost and allied
     * commitments. With a front, they head for it instead and only turn to nearby enemies, so the two sides meet there.
     */
    private static void planHunt(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        Map<Vec3, Double> assigned = new LinkedHashMap<>();
        List<Vec3> candidates = new ArrayList<>(field.enemies().isEmpty() ? field.enemySpawns() : field.enemies());
        for (SquadView squad : squads)
            if (squad.supportTarget() != null && !candidates.contains(squad.supportTarget()))
                candidates.add(squad.supportTarget());
        Map<Vec3, Double> support = new HashMap<>();
        for (Vec3 candidate : candidates)
            support.put(candidate, squads.stream().filter(squad -> squad.supportTarget() != null && squad.supportTarget().distanceToSqr(candidate) <= 576D).mapToDouble(SquadView::power).sum());
        List<SquadView> ordered = new ArrayList<>(squads);
        ordered.sort(Comparator.comparingDouble(SquadView::power).reversed().thenComparingInt(SquadView::id));
        for (SquadView squad : ordered)
        {
            Optional<Vec3> enemy = nearest(squad.position(), field.enemies());
            if (field.front() != null && enemy.map(found -> found.distanceTo(squad.position()) > ENGAGE_RANGE).orElse(true))
            {
                tasks.put(squad.id(), new SquadTask(SquadTask.Kind.HUNT, field.front(), FRONT_RADIUS));
                continue;
            }
            Vec3 target = candidates.stream().filter(candidate -> field.front() == null || candidate.distanceTo(squad.position()) <= ENGAGE_RANGE)
                .min(Comparator.comparingDouble(candidate -> huntCost(squad, candidate, assigned, support.get(candidate)))).orElse(squad.position());
            assigned.merge(target, squad.power(), Double::sum);
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.HUNT, target, HUNT_RADIUS));
        }
    }

    /** Nearby reports share a commitment count, so a cluster of enemies does not attract every squad independently. */
    private static double huntCost(SquadView squad, Vec3 candidate, Map<Vec3, Double> assigned, double requested)
    {
        double committed = assigned.entrySet().stream().filter(entry -> entry.getKey().distanceToSqr(candidate) <= HUNT_RADIUS * HUNT_RADIUS).mapToDouble(Map.Entry::getValue).sum();
        double staying = candidate.equals(squad.currentTarget()) ? STAY_BONUS : 0D;
        double reinforcement = reinforcement(squad, requested);
        return squad.position().distanceTo(candidate) / DISTANCE_SCALE + CROWDING * committed / 4D - staying + failureCost(squad, candidate) - reinforcement;
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
        free.sort(Comparator.comparingDouble(SquadView::power).reversed().thenComparingInt(SquadView::id));
        for (FlagView flag : own)
        {
            if (!flag.atHome())
                recover(flag, free, tasks);
        }
        free.sort(Comparator.comparingDouble(SquadView::power).reversed().thenComparingInt(SquadView::id));
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
                tasks.put(squad.id(), assaultTask(field, squad, front));
            return;
        }
        TeamsCapturePoint second = active + 1 < field.points().size() ? field.points().get(active + 1) : front;
        List<SquadView> ordered = new ArrayList<>(squads);
        // Ready squads go to the front first; posture and distance settle equal-strength choices.
        ordered.sort(Comparator.comparingDouble(SquadView::power).reversed().thenComparing(squad -> squad.posture() != SquadTemplate.Posture.DEFENSE)
            .thenComparingDouble(squad -> squad.position().distanceTo(front.position())));
        double neededAtFront = ordered.stream().mapToDouble(SquadView::power).sum() * FRONT_SHARE;
        double atFront = 0D;
        for (SquadView squad : ordered)
        {
            TeamsCapturePoint point = atFront < neededAtFront ? front : second;
            if (point == front)
                atFront += squad.power();
            tasks.put(squad.id(), new SquadTask(SquadTask.Kind.DEFEND, point.position(), point.radius()));
        }
    }

    private static SquadTask assaultTask(Battlefield field, SquadView squad, TeamsCapturePoint front)
    {
        if (squad.readiness() < 0.5D)
        {
            var guarded = field.points().stream().filter(point -> point.ownerSide() == field.side()).min(Comparator.comparingDouble(point -> point.position().distanceToSqr(squad.position())));
            if (guarded.isPresent())
                return taskAt(field, squad, guarded.get());
        }
        if (squad.role() == BotRole.ARMOR)
            return new SquadTask(SquadTask.Kind.OVERWATCH, standoff(squad.position(), front.position(), front.radius()), OVERWATCH_RADIUS);
        return new SquadTask(SquadTask.Kind.ATTACK, front.position(), front.radius());
    }

    /**
     * Domination and Conquest: each squad takes the point worth most to it, worth less the more squads went there.
     * Defenders who cannot take points only weigh the ones they hold, and hunt the enemy once they hold none.
     */
    private static void planObjectives(Battlefield field, List<SquadView> squads, Map<Integer, SquadTask> tasks)
    {
        if (!field.mayCapture() && field.points().stream().noneMatch(point -> point.ownerSide() == field.side()))
        {
            planHunt(field, squads, tasks);
            return;
        }
        Map<Integer, Double> assigned = new HashMap<>();
        Map<Integer, Double> infantry = new HashMap<>();
        List<Pressure> pressure = field.points().stream().map(point -> pressure(field, point)).toList();
        // Summed once per point, rather than scanning requesting squads inside the assignment loop.
        List<Double> support = field.points().stream()
            .map(point -> squads.stream().filter(squad -> squad.supportTarget() != null && near(point, squad.supportTarget())).mapToDouble(SquadView::power).sum()).toList();
        List<SquadView> ordered = new ArrayList<>(squads);
        ordered.sort(Comparator.comparingDouble(SquadView::power).reversed().thenComparingInt(SquadView::id));
        for (SquadView squad : ordered)
        {
            int best = bestObjective(field, squad, pressure, assigned, support, infantry);
            TeamsCapturePoint point = field.points().get(best);
            assigned.merge(best, squad.power(), Double::sum);
            if (squad.role() == BotRole.INFANTRY && squad.readiness() >= 0.5D)
                infantry.merge(best, squad.power(), Double::sum);
            tasks.put(squad.id(), taskAt(field, squad, point));
        }
    }

    private static int bestObjective(Battlefield field, SquadView squad, List<Pressure> pressure, Map<Integer, Double> assigned, List<Double> support, Map<Integer, Double> infantry)
    {
        int best = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < field.points().size(); i++)
        {
            TeamsCapturePoint candidate = field.points().get(i);
            if (!field.mayCapture() && candidate.ownerSide() != field.side())
                continue;
            double value = objectiveValue(field, candidate, squad, pressure.get(i), assigned.getOrDefault(i, 0D), support.get(i));
            // Reinforce a fight with one ready squad before committing every remaining squad to it.
            // Keep the ordinary travel, danger and failure costs, so distant or impossible objectives still lose.
            if (needsInfantryReinforcement(field, squad) && infantry.getOrDefault(i, 0D) >= 4D)
                value -= 12D;
            if (value > bestValue)
            {
                bestValue = value;
                best = i;
            }
        }
        return best;
    }

    private static boolean needsInfantryReinforcement(Battlefield field, SquadView squad)
    {
        return field.mayCapture() && squad.role() == BotRole.INFANTRY && squad.readiness() >= 0.5D;
    }

    private static double objectiveValue(Battlefield field, TeamsCapturePoint point, SquadView squad, Pressure pressure, double assigned, double requested)
    {
        double demand = Math.max(4D, pressure.power() + requested);
        // Pinning is itself a report of danger, even when the shooter has not been located.
        double reportedThreat = requested > 0D && point.ownerSide() == field.side() && !pressure.threatened() ? (THREATENED_VALUE - SAFE_VALUE) * Math.min(1D, squad.readiness()) : 0D;
        return value(field, point, squad, pressure) + reportedThreat + reinforcement(squad, requested) - CROWDING * assigned / demand;
    }

    private static double reinforcement(SquadView squad, double requested)
    {
        if (squad.supportTarget() != null || squad.readiness() < 0.5D || requested <= 0D)
            return 0D;
        return Math.min(8D, 4D + requested);
    }

    static double value(Battlefield field, TeamsCapturePoint point, SquadView squad)
    {
        return value(field, point, squad, pressure(field, point));
    }

    private static double value(Battlefield field, TeamsCapturePoint point, SquadView squad, Pressure pressure)
    {
        double value = baseValue(field, point, squad, pressure.threatened());
        if (point.position().equals(squad.currentTarget()))
            value += STAY_BONUS;
        if (point.ownerSide() != field.side() || pressure.threatened())
        {
            value *= Math.max(0.15D, Math.min(1D, squad.readiness()));
            value -= Math.max(0D, pressure.power() - squad.power());
            value -= 4D * Math.max(0D, pressure.armour() - squad.antiArmour());
        }
        return value - squad.position().distanceTo(point.position()) / DISTANCE_SCALE - failureCost(squad, point.position());
    }

    private static double failureCost(SquadView squad, Vec3 target)
    {
        return squad.failedTarget() != null && squad.failedTarget().distanceToSqr(target) <= 256D ? 12D : 0D;
    }

    private static double baseValue(Battlefield field, TeamsCapturePoint point, SquadView squad, boolean threatened)
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
            value = threatened ? THREATENED_VALUE : SAFE_VALUE;
            // A point a defender loses is lost for good, so a quiet one is worth guarding too.
            if (!field.mayCapture())
                value += DEFENDER_BONUS;
            if (squad.posture() == SquadTemplate.Posture.DEFENSE)
                value += POSTURE_BONUS;
        }
        return value;
    }

    private static Pressure pressure(Battlefield field, TeamsCapturePoint point)
    {
        long enemies = field.enemies().stream().filter(position -> near(point, position)).count();
        long armour = field.armour().stream().filter(position -> near(point, position)).count();
        boolean threatened = enemies > 0 || point.contested() || point.capturingSide() >= 0 && point.capturingSide() != field.side();
        return new Pressure(enemies + 2D * armour, armour, threatened);
    }

    private static boolean near(TeamsCapturePoint point, Vec3 position)
    {
        double reach = point.radius() * 2D + THREAT_MARGIN;
        return position.distanceToSqr(point.position()) <= reach * reach;
    }

    private static SquadTask.Kind kindAt(Battlefield field, TeamsCapturePoint point)
    {
        if (point.ownerSide() != field.side())
            return SquadTask.Kind.ATTACK;
        return threatened(field, point) ? SquadTask.Kind.DEFEND : SquadTask.Kind.GUARD;
    }

    /**
     * @return what the squad does at the point. Armour sent to take one covers it from a standoff rather than driving onto
     *         it: from there its guns reach the objective while the anti-tank weapons waiting on it do not reach the
     *         armour, and the infantry who alone can take the point go in under its fire.
     */
    private static SquadTask taskAt(Battlefield field, SquadView squad, TeamsCapturePoint point)
    {
        SquadTask.Kind kind = kindAt(field, point);
        if (squad.role() != BotRole.ARMOR || kind != SquadTask.Kind.ATTACK)
            return new SquadTask(kind, point.position(), point.radius());
        return new SquadTask(SquadTask.Kind.OVERWATCH, standoff(squad.position(), point.position(), point.radius()), OVERWATCH_RADIUS);
    }

    /**
     * @return a point back from the objective along the way the squad is coming, far enough out that it covers the
     *         objective from beyond it; the objective itself when the squad already stands on it
     */
    static Vec3 standoff(Vec3 from, Vec3 point, double radius)
    {
        Vec3 back = from.subtract(point).multiply(1D, 0D, 1D);
        return back.lengthSqr() < 1.0E-4D ? point : point.add(back.normalize().scale(radius + OVERWATCH_STANDOFF));
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

    /**
     * Where a naval squad goes. A squad with fight left in it supports the battle ashore: it makes for the patrol point
     * nearest the objective in play, and once it is within reach of that objective it holds station off it and shells it
     * instead of steaming back to the point, since a ship that leaves gun range to patrol takes its guns out of the
     * battle. A battered squad leaves the line for the patrol point farthest from the enemy. The water it spreads over
     * grows with the number of hulls in it: ships cannot share a station the way soldiers share a rally point.
     */
    private static SquadTask naval(Battlefield field, SquadView squad)
    {
        double radius = navalRadius(squad);
        if (squad.readiness() < 0.5D)
            return new SquadTask(SquadTask.Kind.PATROL, navalRefuge(field, squad), radius);
        Vec3 focus = field.points().isEmpty() ? centroid(field.enemies()) : field.points().get(Math.min(activeObjective(field.points(), field.attackingSide()), field.points().size() - 1)).position();
        if (focus == null)
            return nearest(squad.position(), field.navalPoints()).map(point -> new SquadTask(SquadTask.Kind.PATROL, point, radius))
                .orElseGet(() -> new SquadTask(SquadTask.Kind.HOLD, squad.position(), radius));
        if (squad.position().distanceTo(focus) <= NAVAL_SUPPORT_RANGE)
            return new SquadTask(SquadTask.Kind.OVERWATCH, standoff(squad.position(), focus, NAVAL_STANDOFF), radius);
        Vec3 station = standoff(squad.position(), focus, NAVAL_STANDOFF);
        return nearest(focus, field.navalPoints()).map(point -> new SquadTask(SquadTask.Kind.PATROL, point, radius)).orElseGet(() -> new SquadTask(SquadTask.Kind.OVERWATCH, station, radius));
    }

    /** @return blocks a naval squad's line spreads over: room for every hull in it, and never less than one station */
    static double navalRadius(SquadView squad)
    {
        return Math.max(NAVAL_RADIUS, NAVAL_SPACING * Math.max(1, squad.strength()) * 0.5D);
    }

    /** @return the patrol point farthest from the enemy, where a battered squad goes; its own water without any point */
    private static Vec3 navalRefuge(Battlefield field, SquadView squad)
    {
        Vec3 enemy = centroid(field.enemies());
        if (enemy == null)
            return nearest(squad.position(), field.navalPoints()).orElse(squad.position());
        return field.navalPoints().stream().max(Comparator.comparingDouble(point -> point.distanceToSqr(enemy))).orElse(squad.position());
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
