package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Where each side lays its mines: a belt of spots across the approach to every objective its infantry holds, worked out
 * from what the side has seen and from where the enemy comes in, never from hidden enemy positions. A spot is handed to
 * one soldier at a time, so a squad builds a belt instead of a heap. Bounded, transient and owned by one battle, as its
 * reports are. Server thread.
 */
final class BotMinefields
{
    /** Ticks a side's belts stand before they are worked out again. */
    static final long PLAN_TICKS = 200L;
    /** Ticks one soldier keeps a spot to itself before another may take it. */
    static final long CLAIM_TICKS = 600L;
    /** Ticks a spot that was mined is passed over, long enough for the mine to be found by the scan that follows. */
    static final long LAID_TICKS = 2400L;
    /** Blocks beyond the radius of the objective at which the infantry belt lies, and the armour belt behind it. */
    static final double INFANTRY_STANDOFF = 6D;
    static final double ARMOUR_STANDOFF = 14D;
    /** Fewest blocks between two spots of one belt, and between a spot and a defensive post. */
    static final double SPACING = 7D;
    static final double POST_CLEARANCE = 5D;
    /** Blocks from the enemy's approach a route point counts as the way armour takes in. */
    static final double ROUTE_REACH = 24D;
    /** Soldiers holding an objective per mine of its belt, and the most mines of either kind one belt holds. */
    static final int DEFENDERS_PER_MINE = 2;
    static final int MAX_INFANTRY_MINES = 4;
    static final int MAX_ARMOUR_MINES = 2;
    /** Degrees to either side of the approach the infantry belt spreads over. */
    private static final double ARC = 50D;
    /** Blocks within which a soldier is given a spot of a belt. */
    private static final double REACH = 28D;

    /** An objective a side holds, as the belt across its approach is worked out from. */
    record Held(Vec3 centre, double radius, int defenders)
    {}

    /**
     * What the side knows, in its own reports and in what the map holds.
     *
     * @param enemies
     *            reported ground enemies, which weigh heaviest in where the attack comes from
     * @param armour
     *            the reported enemies among them that are armoured
     * @param enemySpawns
     *            where the enemy comes into the battle, which stands in for reports before first contact
     * @param routes
     *            the route points of the map, which stand in for the ways vehicles take
     * @param posts
     *            the side's own garrison and sniper posts, which no mine is laid on
     */
    record Field(List<Vec3> enemies, List<Vec3> armour, List<Vec3> enemySpawns, List<Vec3> routes, List<Vec3> posts)
    {}

    /** A belt of spots across one objective's approach. */
    private record Belt(List<Vec3> infantry, List<Vec3> armour)
    {}

    private record Claim(int side, Vec3 spot, long until)
    {}

    private final Map<Integer, List<Belt>> belts = new HashMap<>();
    private final Map<Integer, Long> plannedAt = new HashMap<>();
    /** The spot each soldier was given, and the spots already mined, which are passed over while they stand. */
    private final Map<UUID, Claim> claims = new HashMap<>();
    private final List<Claim> laid = new ArrayList<>();

    /** Works out a side's belts again once they are stale; the commander calls this while it plans. */
    void update(int side, long now, List<Held> held, Field field)
    {
        Long planned = plannedAt.get(side);
        if (planned != null && planned > now - PLAN_TICKS && planned <= now)
            return;
        plannedAt.put(side, now);
        claims.values().removeIf(claim -> claim.until() <= now);
        laid.removeIf(claim -> claim.until() <= now);
        List<Belt> sideBelts = new ArrayList<>();
        for (Held objective : merge(held))
        {
            Belt belt = belt(objective, field);
            if (belt != null)
                sideBelts.add(belt);
        }
        belts.put(side, List.copyOf(sideBelts));
    }

    /**
     * Gives a soldier the nearest free spot of the kind it carries, which it keeps for a while so that its squad spreads
     * out over the belt.
     *
     * @return the spot, or empty while the side has no belt, none is free, or none is near enough
     */
    Optional<Vec3> claim(int side, UUID soldier, boolean armour, Vec3 from, long now)
    {
        Claim held = claims.get(soldier);
        if (held != null && held.until() > now && held.side() == side)
            return Optional.of(held.spot());
        claims.values().removeIf(claim -> claim.until() <= now);
        laid.removeIf(claim -> claim.until() <= now);
        Vec3 chosen = belts.getOrDefault(side, List.of()).stream().flatMap(belt -> (armour ? belt.armour() : belt.infantry()).stream())
            .filter(spot -> spot.distanceToSqr(from) <= REACH * REACH && free(side, spot, soldier, now)).min(Comparator.comparingDouble(spot -> spot.distanceToSqr(from))).orElse(null);
        if (chosen == null)
            return Optional.empty();
        claims.put(soldier, new Claim(side, chosen, now + CLAIM_TICKS));
        return Optional.of(chosen);
    }

    /** Gives up a spot a soldier did not use, so that another may take it at once. */
    void release(UUID soldier)
    {
        claims.remove(soldier);
    }

    /** Records that a soldier mined its spot, which keeps the belt from laying a second mine there. */
    void laid(int side, UUID soldier, Vec3 spot, long now)
    {
        claims.remove(soldier);
        laid.add(new Claim(side, spot, now + LAID_TICKS));
    }

    void clear()
    {
        belts.clear();
        plannedAt.clear();
        claims.clear();
        laid.clear();
    }

    /** Two squads holding one objective defend one belt, laid out for both their strengths together. */
    private static List<Held> merge(List<Held> held)
    {
        List<Held> merged = new ArrayList<>();
        for (Held objective : held)
        {
            int index = -1;
            for (int candidate = 0; candidate < merged.size(); candidate++)
                if (merged.get(candidate).centre().subtract(objective.centre()).horizontalDistanceSqr() <= SPACING * SPACING)
                    index = candidate;
            if (index < 0)
                merged.add(objective);
            else
            {
                Held existing = merged.get(index);
                merged.set(index, new Held(existing.centre(), Math.max(existing.radius(), objective.radius()), existing.defenders() + objective.defenders()));
            }
        }
        return merged;
    }

    private boolean free(int side, Vec3 spot, UUID soldier, long now)
    {
        for (Map.Entry<UUID, Claim> entry : claims.entrySet())
            if (!entry.getKey().equals(soldier) && entry.getValue().side() == side && entry.getValue().until() > now && near(entry.getValue().spot(), spot))
                return false;
        return laid.stream().noneMatch(claim -> claim.side() == side && claim.until() > now && near(claim.spot(), spot));
    }

    private static boolean near(Vec3 first, Vec3 second)
    {
        return first.subtract(second).horizontalDistanceSqr() < SPACING * SPACING;
    }

    /** @return the belt across one objective's approach, or null while the side cannot tell where the attack comes from */
    @Nullable
    private static Belt belt(Held objective, Field field)
    {
        Vec3 axis = approach(objective.centre(), field);
        if (axis == null)
            return null;
        int mines = Math.max(1, objective.defenders() / DEFENDERS_PER_MINE);
        List<Vec3> infantry = arc(objective.centre(), axis, objective.radius() + INFANTRY_STANDOFF, Math.min(MAX_INFANTRY_MINES, mines), field.posts());
        List<Vec3> armour = armourSpots(objective, axis, field, Math.min(MAX_ARMOUR_MINES, mines));
        return infantry.isEmpty() && armour.isEmpty() ? null : new Belt(infantry, armour);
    }

    /**
     * Where the attack on an objective comes from: towards the reported ground enemies, which the armour among them
     * weighs double, or else towards the enemy's own spawns.
     *
     * @return the horizontal unit vector from the objective towards the enemy, or null while neither is known
     */
    @Nullable
    private static Vec3 approach(Vec3 centre, Field field)
    {
        Vec3 sum = Vec3.ZERO;
        double weight = 0D;
        for (Vec3 enemy : field.enemies())
        {
            double value = field.armour().contains(enemy) ? 2D : 1D;
            sum = sum.add(direction(centre, enemy).scale(value));
            weight += value;
        }
        if (weight <= 0D)
            for (Vec3 spawn : field.enemySpawns())
            {
                sum = sum.add(direction(centre, spawn));
                weight += 1D;
            }
        if (weight <= 0D || sum.horizontalDistanceSqr() < 1.0E-4D)
            return null;
        return sum.multiply(1D, 0D, 1D).normalize();
    }

    private static Vec3 direction(Vec3 from, Vec3 to)
    {
        Vec3 offset = to.subtract(from).multiply(1D, 0D, 1D);
        return offset.horizontalDistanceSqr() < 1.0E-4D ? Vec3.ZERO : offset.normalize();
    }

    /** Spreads the infantry mines over an arc of the approach, a belt's width apart and clear of the side's own posts. */
    private static List<Vec3> arc(Vec3 centre, Vec3 axis, double distance, int count, List<Vec3> posts)
    {
        List<Vec3> spots = new ArrayList<>();
        for (int index = 0; index < count; index++)
        {
            // One mine sits on the approach itself; further ones alternate to either side of it, widening each pair.
            double step = count <= 1 ? 0D : ARC * 2D / (count - 1) * (index - (count - 1) / 2D);
            Vec3 spot = centre.add(rotate(axis, step).scale(distance));
            if (spots.stream().noneMatch(other -> near(other, spot)) && posts.stream().noneMatch(post -> post.subtract(spot).horizontalDistanceSqr() < POST_CLEARANCE * POST_CLEARANCE))
                spots.add(spot);
        }
        return List.copyOf(spots);
    }

    /**
     * Where armour is mined: on the route points the enemy's way in passes, which stand in for the roads and crossings a
     * vehicle takes, or else on the approach itself farther out than the infantry belt.
     */
    private static List<Vec3> armourSpots(Held objective, Vec3 axis, Field field, int count)
    {
        if (count <= 0)
            return List.of();
        double standoff = objective.radius() + ARMOUR_STANDOFF;
        Vec3 ahead = objective.centre().add(axis.scale(standoff));
        List<Vec3> spots = new ArrayList<>();
        field.routes().stream().filter(point ->
        {
            Vec3 offset = point.subtract(objective.centre()).multiply(1D, 0D, 1D);
            double along = offset.dot(axis);
            return along > objective.radius() && offset.horizontalDistanceSqr() <= ROUTE_REACH * ROUTE_REACH;
        }).sorted(Comparator.comparingDouble(point -> point.distanceToSqr(ahead))).limit(count).forEach(point ->
        {
            // The mine lies on the way in, a little short of the route point itself, so it is not under the marker.
            Vec3 toward = direction(point, objective.centre());
            Vec3 spot = toward.lengthSqr() < 0.01D ? point : point.add(toward.scale(2D));
            if (spots.stream().noneMatch(other -> near(other, spot)))
                spots.add(spot);
        });
        if (spots.isEmpty())
            spots.add(ahead);
        return List.copyOf(spots);
    }

    /** Turns a horizontal unit vector by an angle in degrees. */
    private static Vec3 rotate(Vec3 axis, double degrees)
    {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(axis.x * cos - axis.z * sin, 0D, axis.x * sin + axis.z * cos);
    }
}
