package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.DriveControl;
import com.flansmodultimate.api.ISeat;
import com.wolffsmod.npcs.sim.MountMotion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Drives the Flan's driveable an NPC sits at the wheel of, and remembers what that takes: how far along its path the
 * driveable has come, whether it is stuck, and the driveables the NPC gave up on. One per NPC, kept on it and never
 * saved. Server thread.
 *
 * <p>
 * The NPC goes where it would walk: its goals still choose where and the path search still chooses the way, and the
 * driveable is steered along that path. A rider's own path follower hardly advances, so the driveable's progress along
 * the path is counted here. The driveable's weapons are aimed at the NPC's target and fired once they point at it.
 * </p>
 */
@SuppressWarnings("unused") // Made only by the Custom NPCs entity mixin, which IntelliJ does not follow.
public final class NpcDriver
{
    /** Blocks of path ahead the driveable steers for, beyond half its width. */
    private static final double LOOKAHEAD = 6D;
    /** Nodes beyond its progress searched for the node nearest the driveable. */
    private static final int PROGRESS_WINDOW = 12;
    /** Fewest blocks from its goal at which a driveable has arrived. */
    private static final double MIN_ARRIVAL = 2.5D;
    /** Ticks after the NPC takes the wheel before standing still counts as being stuck: the engine may take that long to start. */
    private static final int START_GRACE = 100;
    /** Ticks within which a driveable that is driven has to move to not count as stuck. */
    private static final int STUCK_INTERVAL = 30;
    private static final double STUCK_DISTANCE = 0.75D;
    /** Blocks a stuck driveable has to get from where it got stuck to count as free again. */
    private static final double FREE_DISTANCE = 6D;
    private static final int RECOVERY_TICKS = 30;
    /** Manoeuvres out of being stuck after which the NPC gives the driveable up. */
    private static final int GIVE_UP = 5;
    /** Cosine of the 3 degrees a weapon may point off its target and still fire. */
    private static final double FIRE_CONE = Math.cos(Math.toRadians(3D));
    /** Fewest blocks at which the driveable's weapons engage, whatever the NPC's firing range. */
    private static final double MIN_FIRE_RANGE = 16D;
    /** Every so many ticks the triggers are let go for a tick, so that a semi-automatic weapon fires again. */
    private static final int TRIGGER_RELEASE = 30;

    /** Driveables the NPC gave up on, with the game time from which it may try them again. */
    private final Map<UUID, Long> refused = new HashMap<>();
    /** Game time of the next search for a driveable to board. */
    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
    private long nextSearch;
    /** Game time before which the NPC does not board again, after it got out. */
    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
    private long boardAfter;
    /** The driveable the NPC got into by itself, which it leaves again when its switch is turned off. */
    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE) @Nullable
    private UUID boarded;
    /** Whether the NPC drove last tick. */
    @Getter(AccessLevel.PACKAGE)
    private boolean driving;
    private long drivingSince;
    /** Where the driveable was last tick. */
    @Nullable
    private Vec3 lastPosition;
    private boolean reversing;
    /** The path {@link #progress} counts along. */
    @Nullable
    private Path path;
    /** The node of the path nearest the driveable. */
    private int progress;
    /** Where the driveable was when the current check for being stuck began, and when. */
    @Nullable
    private Vec3 anchor;
    private long anchorTime;
    /** Where the driveable first got stuck; it has to get well away from there to count as free again. */
    @Nullable
    private Vec3 stuckAt;
    /** Ticks left of the manoeuvre out of being stuck. */
    private int recovery;
    /** Whether that manoeuvre backs the driveable away. */
    private boolean recoveryBackward;
    /** Manoeuvres since the driveable last got free. */
    private int recoveries;

    /** Where the path leads from here. */
    private record Route(Vec3 steer, double distance)
    {}

    /** Implemented on the Custom NPCs entity, which carries its driver. */
    public interface Holder
    {
        NpcDriver wolffsmodnpcsDriver();
    }

    public static NpcDriver of(EntityNPCInterface npc)
    {
        return ((Holder) npc).wolffsmodnpcsDriver();
    }

    boolean refuses(UUID driveable, long now)
    {
        Long until = refused.get(driveable);
        if (until == null)
            return false;
        if (now < until)
            return true;
        refused.remove(driveable);
        return false;
    }

    void refuse(UUID driveable, long until)
    {
        refused.put(driveable, until);
    }

    /**
     * Drives for one tick.
     *
     * @param heading
     *            where the driveable's forward control takes it
     * @param mecha
     *            whether the driveable walks and steps sideways rather than steering
     * @param tracked
     *            whether it turns on the spot
     * @return whether the NPC keeps the driveable; false once it gives it up as stuck for good
     */
    boolean drive(EntityNPCInterface npc, ISeat seat, Entity driveable, Vec3 heading, boolean mecha, boolean tracked)
    {
        long now = npc.level().getGameTime();
        if (!driving)
        {
            stopDriving();
            driving = true;
            drivingSince = now;
        }
        Vec3 position = driveable.position();
        double speed = lastPosition == null ? 0D : position.subtract(lastPosition).dot(heading);
        lastPosition = position;
        double arrival = Math.max(MIN_ARRIVAL, driveable.getBbWidth() * 0.5D + 1.5D);
        Route route = route(npc, position, LOOKAHEAD + driveable.getBbWidth() * 0.5D);
        Float off = null;
        Set<DriveControl> controls;
        if (route == null)
        {
            anchor = null;
            reversing = false;
            controls = DriveSteering.brake(speed);
        }
        else
        {
            float angle = Mth.wrapDegrees(MountMotion.yaw(route.steer().subtract(position)) - MountMotion.yaw(heading));
            off = angle;
            controls = recovery > 0 ? recover(angle, route, arrival, speed, mecha, tracked) : steer(angle, route, arrival, speed, mecha, tracked);
            if (recovery > 0)
            {
                recovery--;
            }
            else if (stuck(position, pushes(controls, mecha), now))
            {
                if (++recoveries > GIVE_UP)
                    return false;
                recovery = RECOVERY_TICKS;
                recoveryBackward = !controls.contains(DriveControl.BACKWARD);
            }
        }
        // A driveable that takes no controls, such as one just wrecked, has no weapons to fire either.
        if (seat.setAutomatedDriving(controls))
            fight(npc, seat, mecha, off);
        return true;
    }

    private Set<DriveControl> steer(float off, Route route, double arrival, double speed, boolean mecha, boolean tracked)
    {
        if (mecha)
            return DriveSteering.mecha(off, route.distance(), arrival);
        DriveSteering.Decision decision = DriveSteering.vehicle(new DriveSteering.Situation(off, route.distance(), arrival, speed, tracked, reversing));
        reversing = decision.reversing();
        return decision.controls();
    }

    /** A vehicle backs away from what holds it in a three-point turn; a mecha jumps, or fires its rocket pack, on its way. */
    private Set<DriveControl> recover(float off, Route route, double arrival, double speed, boolean mecha, boolean tracked)
    {
        if (!mecha)
            return DriveSteering.recovery(off, recoveryBackward, speed, tracked);
        Set<DriveControl> controls = EnumSet.of(DriveControl.ASCEND);
        controls.addAll(DriveSteering.mecha(off, route.distance(), arrival));
        return controls;
    }

    private static boolean pushes(Set<DriveControl> controls, boolean mecha)
    {
        boolean walks = controls.contains(DriveControl.FORWARD) || controls.contains(DriveControl.BACKWARD);
        return walks || mecha && (controls.contains(DriveControl.LEFT) || controls.contains(DriveControl.RIGHT));
    }

    /** Whether the driveable has been driven without moving for a while. */
    private boolean stuck(Vec3 position, boolean pushing, long now)
    {
        if (stuckAt != null && position.distanceToSqr(stuckAt) > FREE_DISTANCE * FREE_DISTANCE)
        {
            stuckAt = null;
            recoveries = 0;
        }
        if (!pushing || now - drivingSince < START_GRACE)
        {
            anchor = null;
            return false;
        }
        if (anchor == null)
        {
            anchor = position;
            anchorTime = now;
            return false;
        }
        if (now - anchorTime < STUCK_INTERVAL)
            return false;
        boolean moved = anchor.distanceToSqr(position) >= STUCK_DISTANCE * STUCK_DISTANCE;
        anchor = null;
        if (moved)
            return false;
        if (stuckAt == null)
            stuckAt = position;
        return true;
    }

    /**
     * Aims the driveable's weapons at the NPC's target and fires each once it points there, within firing range and with
     * nothing solid in the way. Without a target, a vehicle's turret comes back to the front and a mecha faces where it
     * walks, as its walk follows its torso.
     *
     * @param off
     *            degrees the driveable's way lies off its heading, or null while it stays where it is
     */
    private static void fight(EntityNPCInterface npc, ISeat seat, boolean mecha, @Nullable Float off)
    {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive())
        {
            seat.setAutomatedTriggers(false, false);
            if (!mecha)
                seat.setAutomatedAim(0F, 0F);
            else if (off != null)
                seat.setAutomatedAim(seat.getAimYaw() + off, 0F);
            return;
        }
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        seat.aimAt(aimPoint);
        boolean release = npc.level().getGameTime() % TRIGGER_RELEASE == 0;
        seat.setAutomatedTriggers(!release && readyToFire(npc, seat, false, aimPoint), !release && readyToFire(npc, seat, true, aimPoint));
    }

    private static boolean readyToFire(EntityNPCInterface npc, ISeat seat, boolean secondary, Vec3 aimPoint)
    {
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        Optional<Vec3> direction = seat.getDriverWeaponDirection(secondary);
        if (origin.isEmpty() || direction.isEmpty())
            return false;
        Vec3 line = aimPoint.subtract(origin.get());
        double range = Math.max(MIN_FIRE_RANGE, npc.stats.ranged.getRange());
        if (line.lengthSqr() > range * range || direction.get().dot(line.normalize()) < FIRE_CONE)
            return false;
        ClipContext sight = new ClipContext(origin.get(), aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc);
        return npc.level().clip(sight).getType() == HitResult.Type.MISS;
    }

    /** Reads the point to steer for and the distance to the goal off the NPC's path; none without a path. */
    @Nullable
    private Route route(EntityNPCInterface npc, Vec3 position, double lookahead)
    {
        Path current = npc.getNavigation().getPath();
        if (current == null || current.isDone() || current.getNodeCount() == 0)
        {
            path = null;
            return null;
        }
        if (current != path)
        {
            path = current;
            progress = 0;
        }
        progress = nearest(current, position, Math.min(progress, current.getNodeCount() - 1));
        // Steering for a node further along keeps the driveable from turning back to one it has just passed.
        Vec3 steer = node(current, progress);
        double travelled = horizontal(position, steer);
        for (int index = progress + 1; index < current.getNodeCount(); index++)
        {
            Vec3 next = node(current, index);
            travelled += horizontal(steer, next);
            if (travelled > lookahead)
                break;
            steer = next;
        }
        return new Route(steer, horizontal(position, node(current, current.getNodeCount() - 1)));
    }

    private static int nearest(Path path, Vec3 position, int from)
    {
        int best = from;
        double bestDistance = Double.MAX_VALUE;
        int last = Math.min(path.getNodeCount() - 1, from + PROGRESS_WINDOW);
        for (int index = from; index <= last; index++)
        {
            double distance = horizontal(position, node(path, index));
            if (distance < bestDistance)
            {
                best = index;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Vec3 node(Path path, int index)
    {
        return Vec3.atBottomCenterOf(path.getNodePos(index));
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }

    /** Forgets how the NPC drove, once it no longer drives. */
    void stopDriving()
    {
        driving = false;
        lastPosition = null;
        reversing = false;
        path = null;
        progress = 0;
        anchor = null;
        stuckAt = null;
        recovery = 0;
        recoveries = 0;
    }
}
