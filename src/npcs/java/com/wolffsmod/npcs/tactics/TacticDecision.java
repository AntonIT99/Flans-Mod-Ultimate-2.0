package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcCover.Cover;
import com.wolffsmod.npcs.tactics.NpcTacticState.Phase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * Where a tactic sends the NPC this tick. It decides from what the NPC knows and from what its tactic remembers, so that
 * it can be tested without a world; looking for cover is left to the caller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TacticDecision
{
    /** Blocks from where it was placed within which a holding NPC stays put. */
    static final double HOLD_RADIUS = 2D;
    static final double WALK = 1D;
    /** Speed an NPC dashes or runs at: an advancing one while far off, one taking cover, one bounding. */
    static final double RUN = 1.3D;
    /** Blocks from a point within which the NPC has got there. */
    static final double ARRIVED = 1.2D;
    /** Blocks from its cover or peek spot within which the NPC is in it; less than the block between the two, or it never steps out. */
    static final double COVER_ARRIVED = 0.3D;
    /** Blocks beyond the near edge of its band a stand-off NPC backs away to, so it does not hover at the edge. */
    static final double RETREAT_MARGIN = 3D;
    /** Ticks a bounding NPC stands and fires before it dashes on, and the longest a dash lasts. */
    static final int FIRE_TICKS = 40;
    static final int DASH_TICKS = 60;
    /** Blocks a bounding NPC dashes at a time. */
    static final double DASH_LENGTH = 6D;
    /** Ticks an NPC stays in cover before it steps out again, and the longest it fires from outside it. */
    static final int HIDE_TICKS = 20;
    static final int PEEK_TICKS = 60;
    /** Radians off the line from the target to the NPC at which a flanking NPC attacks from. */
    static final double FLANK_ANGLE = Math.toRadians(70D);
    /** Blocks within which a flanking NPC has reached its side. */
    static final double FLANK_ARRIVED = 3D;
    /** Radians a circling NPC moves on round its target at a time, and blocks within which it has reached a point. */
    static final double CIRCLE_STEP = Math.toRadians(30D);
    static final double CIRCLE_ARRIVED = 4D;
    /** Blocks a strafing helicopter slips sideways at a time, and the longest it slips one way. */
    static final double STRAFE_STEP = 6D;
    static final int STRAFE_TICKS = 60;

    /** What the NPC moves for. */
    public enum Move
    {
        /** Stands where it is. */
        STAND,
        /** Goes toward its target. */
        CLOSE_IN,
        /** Goes to the point of the order. */
        GO_TO
    }

    /**
     * What the NPC knows this tick.
     *
     * @param inSight
     *            whether it sees its target
     * @param position
     *            where the NPC is
     * @param target
     *            where its target is
     * @param home
     *            where the NPC was placed
     * @param reloading
     *            whether it is reloading
     * @param hit
     *            whether it was hit just now
     */
    public record Situation(boolean inSight, Vec3 position, Vec3 target, Vec3 home, Engagement engagement, boolean reloading, boolean hit)
    {
        public double targetDistance()
        {
            return position.distanceTo(target);
        }
    }

    /** Where the NPC goes, and how fast. The point is only set for {@link Move#GO_TO}. */
    public record Order(Move move, double speed, @Nullable Vec3 point)
    {
        static final Order STAND = new Order(Move.STAND, 0D, null);

        static Order closeIn(double speed)
        {
            return new Order(Move.CLOSE_IN, speed, null);
        }

        /** @return an order to go to the point, or to stand once the NPC is within the given blocks of it */
        static Order goTo(Situation situation, Vec3 point, double speed, double arrived)
        {
            return horizontal(situation.position(), point) <= arrived ? STAND : new Order(Move.GO_TO, speed, point);
        }
    }

    /** @return the order of a tactic the NPC follows a path for; Custom NPCs' tactics and those a plane flies are not decided here */
    public static Order decide(Tactic tactic, Situation situation, NpcTacticState memory, long now)
    {
        return switch (tactic)
        {
            case ADVANCE -> advance(situation);
            case HOLD -> horizontal(situation.position(), situation.home()) <= HOLD_RADIUS ? Order.STAND : new Order(Move.GO_TO, WALK, situation.home());
            case COVER -> cover(situation, memory, now);
            case STAND_OFF -> standOff(situation);
            case BOUNDING -> bounding(situation, memory, now);
            case FLANK -> flank(situation, memory, now);
            case CIRCLE -> circle(situation, memory);
            case STRAFE -> strafe(situation, memory, now);
            case CUSTOM_NPCS, STRAFING_RUN, ORBIT -> throw new IllegalArgumentException(tactic + " is not decided here");
        };
    }

    private static Order advance(Situation situation)
    {
        double preferred = situation.engagement().preferred();
        double distance = situation.targetDistance();
        if (situation.inSight() && distance <= preferred)
            return Order.STAND;
        return Order.closeIn(distance > preferred * 2D ? RUN : WALK);
    }

    private static Order standOff(Situation situation)
    {
        Engagement engagement = situation.engagement();
        double distance = situation.targetDistance();
        if (!situation.inSight() || distance > engagement.far())
            return Order.closeIn(distance > engagement.far() * 2D ? RUN : WALK);
        if (distance < engagement.near())
            return Order.goTo(situation, away(situation.position(), situation.target(), engagement.near() - distance + RETREAT_MARGIN), RUN, ARRIVED);
        return Order.STAND;
    }

    /** Hides in cover, steps out to fire when ready, and back in to reload or when hit. Without cover it advances. */
    private static Order cover(Situation situation, NpcTacticState memory, long now)
    {
        Cover cover = memory.getCover();
        if (cover == null)
            return advance(situation);
        boolean wantsCover = situation.reloading() || situation.hit();
        if (memory.getPhase() == Phase.PEEK && (wantsCover || now - memory.getPhaseStarted() >= PEEK_TICKS))
            memory.enter(Phase.HIDE, now);
        else if (memory.getPhase() == Phase.HIDE && !wantsCover && now - memory.getPhaseStarted() >= HIDE_TICKS && horizontal(situation.position(), cover.standPoint()) <= ARRIVED)
            memory.enter(Phase.PEEK, now);
        else if (memory.getPhase() != Phase.HIDE && memory.getPhase() != Phase.PEEK)
            memory.enter(Phase.HIDE, now);
        Vec3 spot = memory.getPhase() == Phase.PEEK ? cover.peekPoint() : cover.standPoint();
        // A peek spot is often the very next block, so the NPC has to stand in it, not merely near it, to see past the cover.
        return Order.goTo(situation, spot, horizontal(situation.position(), spot) > DASH_LENGTH ? RUN : WALK, COVER_ARRIVED);
    }

    /** Fires standing, then dashes closer while it reloads or cools down, until it is as close as it wants. */
    private static Order bounding(Situation situation, NpcTacticState memory, long now)
    {
        double distance = situation.targetDistance();
        double preferred = situation.engagement().preferred();
        if (situation.inSight() && distance <= preferred)
        {
            if (memory.getPhase() != Phase.FIRE)
                memory.enter(Phase.FIRE, now);
            return Order.STAND;
        }
        if (memory.getPhase() == Phase.MOVE)
        {
            Vec3 stop = memory.getPoint();
            boolean done = stop == null || horizontal(situation.position(), stop) <= ARRIVED || now - memory.getPhaseStarted() >= DASH_TICKS;
            if (!done)
                return new Order(Move.GO_TO, RUN, stop);
            memory.enter(Phase.FIRE, now);
        }
        if (memory.getPhase() != Phase.FIRE)
            memory.enter(Phase.FIRE, now);
        if (situation.inSight() && !situation.reloading() && now - memory.getPhaseStarted() < FIRE_TICKS)
            return Order.STAND;
        memory.enter(Phase.MOVE, now);
        memory.setPoint(toward(situation.position(), situation.target(), Math.min(DASH_LENGTH, Math.max(1D, distance - preferred))));
        return new Order(Move.GO_TO, RUN, memory.getPoint());
    }

    /** Swings out to its side of the target at its tactic distance, then attacks from there as an advancing NPC does. */
    private static Order flank(Situation situation, NpcTacticState memory, long now)
    {
        if (Double.isNaN(memory.getBearing()))
            memory.setBearing(bearing(situation.target(), situation.position()) + memory.getSide() * FLANK_ANGLE);
        if (memory.getPhase() == Phase.ATTACK)
            return advance(situation);
        Vec3 side = around(situation.target(), memory.getBearing(), situation.engagement().preferred(), situation.position().y);
        if (horizontal(situation.position(), side) <= FLANK_ARRIVED)
        {
            memory.enter(Phase.ATTACK, now);
            return advance(situation);
        }
        return new Order(Move.GO_TO, RUN, side);
    }

    /** Goes round its target from point to point of a circle at its tactic distance. */
    private static Order circle(Situation situation, NpcTacticState memory)
    {
        double radius = situation.engagement().preferred();
        if (Double.isNaN(memory.getBearing()))
            memory.setBearing(bearing(situation.target(), situation.position()) + memory.getSide() * CIRCLE_STEP);
        Vec3 point = around(situation.target(), memory.getBearing(), radius, situation.position().y);
        if (horizontal(situation.position(), point) <= CIRCLE_ARRIVED)
        {
            memory.setBearing(memory.getBearing() + memory.getSide() * CIRCLE_STEP);
            point = around(situation.target(), memory.getBearing(), radius, situation.position().y);
        }
        return new Order(Move.GO_TO, WALK, point);
    }

    /** Keeps its distance as a stand-off NPC does, and slips sideways one way and then the other while it holds it. */
    private static Order strafe(Situation situation, NpcTacticState memory, long now)
    {
        Order distance = standOff(situation);
        if (distance.move() != Move.STAND)
            return distance;
        Vec3 slip = memory.getPoint();
        if (memory.getPhase() != Phase.MOVE || slip == null || horizontal(situation.position(), slip) <= ARRIVED || now - memory.getPhaseStarted() >= STRAFE_TICKS)
        {
            if (memory.getPhase() == Phase.MOVE)
                memory.setSide(-memory.getSide());
            memory.enter(Phase.MOVE, now);
            double across = bearing(situation.target(), situation.position()) + memory.getSide() * Math.PI / 2D;
            memory.setPoint(situation.position().add(Math.cos(across) * STRAFE_STEP, 0D, Math.sin(across) * STRAFE_STEP));
            slip = memory.getPoint();
        }
        return new Order(Move.GO_TO, WALK, slip);
    }

    // Geometry, on the ground plane

    /** @return radians of the direction from one point to another, measured as cosine on x and sine on z */
    static double bearing(Vec3 from, Vec3 to)
    {
        return Math.atan2(to.z - from.z, to.x - from.x);
    }

    /** @return the point at the given bearing and distance from the centre, at the given height */
    static Vec3 around(Vec3 centre, double bearing, double distance, double y)
    {
        return new Vec3(centre.x + Math.cos(bearing) * distance, y, centre.z + Math.sin(bearing) * distance);
    }

    /** @return the point the given blocks from the position, straight away from the other point */
    static Vec3 away(Vec3 position, Vec3 from, double blocks)
    {
        return around(position, bearing(from, position), blocks, position.y);
    }

    /** @return the point the given blocks from the position, straight toward the other point */
    static Vec3 toward(Vec3 position, Vec3 to, double blocks)
    {
        return around(position, bearing(position, to), blocks, position.y);
    }

    static double horizontal(Vec3 from, Vec3 to)
    {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
