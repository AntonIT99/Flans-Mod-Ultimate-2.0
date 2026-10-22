package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.AircraftMode;
import com.flansmodultimate.api.DriveControl;
import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.ISeat;
import com.flansmodultimate.api.WeaponBallistics;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.MountMotion;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.sim.TargetMotion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

/**
 * Drives the Flan's driveable an NPC sits at the wheel of, and remembers what that takes: how far along its path the
 * driveable has come, whether it is stuck, and the driveables the NPC gave up on. One per NPC, kept on it and never
 * saved. Server thread.
 *
 * <p>
 * The NPC goes where it would walk: its goals still choose where and the path search still chooses the way, and the
 * driveable is steered along that path. A rider's own path follower hardly advances, so the driveable's progress along
 * the path is counted here. An aircraft flies straight to where the path ends, its {@link NpcPilot} at the controls. The
 * driveable's weapons are aimed at the NPC's target and fired once they point at it.
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
    /** Fewest blocks at which the driveable's weapons engage, whatever the NPC's firing range; more for an aircraft's. */
    private static final double MIN_FIRE_RANGE = 16D;
    private static final double MIN_AIR_FIRE_RANGE = 96D;
    /** Blocks above its target from which a bomb is dropped, and from its target within which it has to come down. */
    private static final double MIN_BOMB_HEIGHT = 4D;
    private static final double BOMB_RADIUS = 4D;
    /** Every so many ticks the triggers are let go for a tick, so that a semi-automatic weapon fires again. */
    private static final int TRIGGER_RELEASE = 30;
    /** Ticks between looks for a driveable ahead to keep a distance to, and degrees off the heading within which one is ahead. */
    private static final int FOLLOW_INTERVAL = 10;
    private static final float FOLLOW_CONE = 35F;
    /** Ticks a vehicle waits behind a driveable that stands in its way, and ticks it then drives on to find its way round. */
    private static final int WAIT_TICKS = 100;
    private static final int PASS_TICKS = 200;

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
    /** Ticks the driveable the NPC crews has gone without a driver. */
    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
    private int driverless;
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
    /** The driveable ahead the vehicle keeps its distance to, as last looked for. */
    private DriveSteering.Traffic ahead = DriveSteering.Traffic.NONE;
    /** Ticks the vehicle has stood waiting behind it, and the game time until which it drives on regardless. */
    private int waited;
    private long passUntil;
    private final NpcPilot pilot = new NpcPilot();
    /** How the target fired at moves, to lead it, and which target that is. */
    private final TargetMotion motion = new TargetMotion();
    @Nullable
    private UUID tracked;
    /** What the NPC did at the controls last tick, for the movement debug; null while it does not drive. */
    @Nullable
    private Report report;

    /**
     * What the NPC did at the controls, for the movement debug.
     *
     * @param point
     *            where it steered or flew for
     * @param details
     *            the rest, worded only when the log asks for it
     */
    private record Report(String activity, @Nullable Vec3 point, Set<DriveControl> controls, Supplier<String> details)
    {}

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
        begin(now);
        Vec3 position = driveable.position();
        double speed = lastPosition == null ? 0D : position.subtract(lastPosition).dot(heading);
        lastPosition = position;
        double arrival = Math.max(MIN_ARRIVAL, driveable.getBbWidth() * 0.5D + 1.5D);
        Route route = route(npc, position, LOOKAHEAD + driveable.getBbWidth() * 0.5D, NpcDrivers.pathSize(npc).map(size -> size.width() * 0.5D).orElse(0.5D));
        if (route == null)
        {
            anchor = null;
            reversing = false;
            Set<DriveControl> controls = DriveSteering.brake(speed);
            report(mecha, null, controls, 0F, 0D, speed);
            act(npc, seat, driveable, mecha, controls, null);
            return true;
        }
        float off = Mth.wrapDegrees(MountMotion.yaw(route.steer().subtract(position)) - MountMotion.yaw(heading));
        Set<DriveControl> controls = recovery > 0 ? recover(off, route, arrival, speed, mecha, tracked) : steer(off, route, arrival, speed, mecha, tracked);
        if (recovery == 0 && !mecha)
            controls = keepDistance(npc, driveable, heading, speed, controls, now);
        if (recovery > 0)
            recovery--;
        else if (stuck(position, pushes(controls, mecha), now) && !startRecovery(controls))
            return false;
        report(mecha, route.steer(), controls, off, route.distance(), speed);
        act(npc, seat, driveable, mecha, controls, off);
        return true;
    }

    /** Records that the NPC crews a driveable this tick, for the movement debug and to notice when it is thrown out. */
    void crew(String activity, @Nullable Vec3 point, long now)
    {
        begin(now);
        report = new Report(activity, point, Set.of(), () -> "driverless=" + driverless);
    }

    /** Starts the NPC driving, unless it already does. */
    private void begin(long now)
    {
        if (driving)
            return;
        stopDriving();
        driving = true;
        drivingSince = now;
    }

    /** Starts a manoeuvre out of being stuck. @return false once the NPC has tried too often */
    private boolean startRecovery(Set<DriveControl> controls)
    {
        if (++recoveries > GIVE_UP)
            return false;
        recovery = RECOVERY_TICKS;
        recoveryBackward = !controls.contains(DriveControl.BACKWARD);
        return true;
    }

    /** Holds the controls, and fights unless the driveable takes no controls, as one just wrecked does not. */
    private void act(EntityNPCInterface npc, ISeat seat, Entity driveable, boolean mecha, Set<DriveControl> controls, @Nullable Float off)
    {
        if (seat.setAutomatedDriving(controls))
            fight(npc, seat, driveable, mecha, off);
    }

    private void report(boolean mecha, @Nullable Vec3 point, Set<DriveControl> controls, float off, double distance, double speed)
    {
        boolean backward = reversing;
        int manoeuvre = recovery;
        int tries = recoveries;
        report = new Report(mecha ? "walk" : "drive", point, controls,
            () -> String.format(Locale.ROOT, "off=%.0f goal=%.1f speed=%.2f%s recovery=%d tries=%d", off, distance, speed, backward ? " reverse" : "", manoeuvre, tries));
    }

    /**
     * Flies for one tick.
     *
     * @return whether the NPC keeps the aircraft; false once it gives up getting it off the ground
     */
    boolean fly(EntityNPCInterface npc, ISeat seat, Entity aircraft, AircraftMode mode)
    {
        long now = npc.level().getGameTime();
        begin(now);
        Set<DriveControl> controls = pilot.fly(npc, aircraft, mode, goal(npc));
        report = new Report(pilot.getPhase().name().toLowerCase(Locale.ROOT), pilot.getPoint(), controls, pilot::describe);
        // An aircraft that cannot get off the ground, for want of fuel or of room, is given up.
        if (stuck(aircraft.position(), pilot.isRolling(), now) && ++recoveries > GIVE_UP)
            return false;
        if (!seat.setAutomatedDriving(controls))
            return true;
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive())
            seat.setAutomatedTriggers(false, false);
        else
            fire(npc, seat, aircraft, target, MIN_AIR_FIRE_RANGE);
        return true;
    }

    /** @return where the NPC's path ends, or null without one */
    @Nullable
    private static Vec3 goal(EntityNPCInterface npc)
    {
        Path current = npc.getNavigation().getPath();
        return current == null || current.isDone() || current.getNodeCount() == 0 ? null : node(current, current.getNodeCount() - 1, 0.5D);
    }

    /**
     * Keeps a vehicle its distance to a driveable ahead. One that has stood waiting too long drives on for a while, to find
     * its way round what blocks it as it would round any obstacle.
     */
    private Set<DriveControl> keepDistance(EntityNPCInterface npc, Entity driveable, Vec3 heading, double speed, Set<DriveControl> controls, long now)
    {
        int distance = NpcTuning.of(npc).get(Value.KEEP_DISTANCE);
        if (distance <= 0 || now < passUntil)
        {
            ahead = DriveSteering.Traffic.NONE;
            return controls;
        }
        if ((now + npc.getId()) % FOLLOW_INTERVAL == 0)
            ahead = traffic(driveable, heading, speed, distance);
        Set<DriveControl> kept = DriveSteering.keepDistance(controls, ahead, distance, speed);
        waited = !kept.equals(controls) && Math.abs(speed) < DriveSteering.STOPPED ? waited + 1 : 0;
        if (waited <= WAIT_TICKS)
            return kept;
        waited = 0;
        passUntil = now + PASS_TICKS;
        return controls;
    }

    /** @return the nearest driveable with a driver, or vehicle NPC, ahead of the vehicle within twice the distance */
    private static DriveSteering.Traffic traffic(Entity driveable, Vec3 heading, double speed, int distance)
    {
        float yaw = MountMotion.yaw(heading);
        DriveSteering.Traffic nearest = DriveSteering.Traffic.NONE;
        for (Entity other : driveable.level().getEntities(driveable, driveable.getBoundingBox().inflate(distance * 2D, 2D, distance * 2D), NpcDriver::traffic))
        {
            Vec3 offset = other.position().subtract(driveable.position());
            double gap = offset.horizontalDistance() - (driveable.getBbWidth() + other.getBbWidth()) * 0.5D;
            if (gap < nearest.gap() && Math.abs(Mth.wrapDegrees(MountMotion.yaw(offset) - yaw)) <= FOLLOW_CONE)
                nearest = new DriveSteering.Traffic(gap, speed - other.position().subtract(other.xo, other.yo, other.zo).dot(heading));
        }
        return nearest;
    }

    /** Whether an entity is traffic to keep a distance to: a driveable someone drives that is no wreck, or an NPC drawn as a vehicle. */
    private static boolean traffic(Entity entity)
    {
        if (entity instanceof EntityNPCInterface npc)
            return NpcVehicleState.of(npc).kind() == FlanModelKind.VEHICLE;
        return FlansEntityTypes.getDriveableType(entity).isPresent() && !FlansEntityTypes.isWreck(entity) && NpcDrivers.driverOf(entity) != null;
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
    private void fight(EntityNPCInterface npc, ISeat seat, Entity driveable, boolean mecha, @Nullable Float off)
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
        fire(npc, seat, driveable, target, MIN_FIRE_RANGE);
    }

    /**
     * Aims the driveable's weapons at the target, leading it and allowing for the drop of their rounds as the NPC's aiming
     * switches say, and fires each one that points where it has to, within firing range and with nothing solid in the
     * way. A bomb is dropped once it would come down by the target.
     *
     * @param reach
     *            fewest blocks at which the weapons engage, whatever the NPC's firing range
     */
    private void fire(EntityNPCInterface npc, ISeat seat, Entity driveable, LivingEntity target, double reach)
    {
        track(target, npc.level().getGameTime());
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        // The turret, or a mecha's arms, follows the main weapon, or the other one without it.
        boolean secondary = seat.getDriverWeaponOrigin(false).isEmpty();
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        Optional<Vec3> launch = origin.map(from -> launch(npc, seat, secondary, aimPoint.subtract(from)));
        if (origin.isPresent() && launch.isPresent())
            seat.aimAt(origin.get().add(launch.get().scale(aimPoint.distanceTo(origin.get()))));
        else
            seat.aimAt(aimPoint);
        boolean release = npc.level().getGameTime() % TRIGGER_RELEASE == 0;
        double range = Math.max(reach, npc.stats.ranged.getRange());
        seat.setAutomatedTriggers(!release && readyToFire(npc, seat, driveable, false, target, range), !release && readyToFire(npc, seat, driveable, true, target, range));
    }

    /** Follows how the target moves, to lead it; a new target starts afresh. */
    private void track(LivingEntity target, long now)
    {
        if (!target.getUUID().equals(tracked))
        {
            motion.reset();
            tracked = target.getUUID();
        }
        motion.observe(target.position(), now);
    }

    /** @return the direction a weapon launches along to meet the target, or straight at it when what it fires is not known */
    private Vec3 launch(EntityNPCInterface npc, ISeat seat, boolean secondary, Vec3 displacement)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        return seat.getDriverWeaponBallistics(secondary).filter(ballistics -> !ballistics.dropped())
            .map(ballistics -> WeaponAim.launch(displacement, motion, ballistics, options.enabled(Feature.LEAD_TARGET), options.enabled(Feature.ADVANCED_LEAD), options.enabled(Feature.BALLISTIC_AIM)))
            .orElse(displacement.normalize());
    }

    private boolean readyToFire(EntityNPCInterface npc, ISeat seat, Entity driveable, boolean secondary, LivingEntity target, double range)
    {
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        Optional<Vec3> direction = seat.getDriverWeaponDirection(secondary);
        if (origin.isEmpty() || direction.isEmpty())
            return false;
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        Vec3 line = aimPoint.subtract(origin.get());
        if (line.lengthSqr() > range * range)
            return false;
        Optional<WeaponBallistics> ballistics = seat.getDriverWeaponBallistics(secondary);
        if (ballistics.isPresent() && ballistics.get().dropped())
            return bombsAway(origin.get(), driveable.getDeltaMovement(), ballistics.get(), target);
        if (direction.get().dot(launch(npc, seat, secondary, line)) < FIRE_CONE)
            return false;
        ClipContext sight = new ClipContext(origin.get(), aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc);
        return npc.level().clip(sight).getType() == HitResult.Type.MISS;
    }

    /** Whether a bomb dropped now, from above the target, would come down within reach of it. */
    private static boolean bombsAway(Vec3 origin, Vec3 velocity, WeaponBallistics ballistics, LivingEntity target)
    {
        if (origin.y < target.getY() + MIN_BOMB_HEIGHT)
            return false;
        Vec3 impact = WeaponAim.impact(origin, velocity, ballistics.gravity(), ballistics.drag(), target.getY());
        return horizontal(impact, target.position()) <= BOMB_RADIUS;
    }

    /**
     * Reads the point to steer for and the distance to the goal off the NPC's path; none without a path.
     *
     * @param middle
     *            blocks from a node's corner to the middle of the path: half the width it was searched for
     */
    @Nullable
    private Route route(EntityNPCInterface npc, Vec3 position, double lookahead, double middle)
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
        progress = nearest(current, position, Math.min(progress, current.getNodeCount() - 1), middle);
        // Steering for a node further along keeps the driveable from turning back to one it has just passed.
        Vec3 steer = node(current, progress, middle);
        double travelled = horizontal(position, steer);
        for (int index = progress + 1; index < current.getNodeCount(); index++)
        {
            Vec3 next = node(current, index, middle);
            travelled += horizontal(steer, next);
            if (travelled > lookahead)
                break;
            steer = next;
        }
        return new Route(steer, horizontal(position, node(current, current.getNodeCount() - 1, middle)));
    }

    private static int nearest(Path path, Vec3 position, int from, double middle)
    {
        int best = from;
        double bestDistance = Double.MAX_VALUE;
        int last = Math.min(path.getNodeCount() - 1, from + PROGRESS_WINDOW);
        for (int index = from; index <= last; index++)
        {
            double distance = horizontal(position, node(path, index, middle));
            if (distance < bestDistance)
            {
                best = index;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** @return the middle of the path at the node: a wide path is searched for room from each node's corner on */
    private static Vec3 node(Path path, int index, double middle)
    {
        BlockPos node = path.getNodePos(index);
        return new Vec3(node.getX() + middle, node.getY(), node.getZ() + middle);
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }

    /** @return what the NPC does at the controls, for the movement debug log; empty while it does not drive */
    public Optional<String> describe()
    {
        Report last = report;
        return last == null ? Optional.empty() : Optional.of(last.activity() + " controls=" + last.controls() + " " + last.details().get());
    }

    /** @return where the NPC steers or flies for, for the movement debug; empty while it does not drive or has nowhere to go */
    public Optional<Vec3> debugPoint()
    {
        Report last = report;
        return last == null ? Optional.empty() : Optional.ofNullable(last.point());
    }

    /** Forgets how the NPC drove, once it no longer drives. */
    void stopDriving()
    {
        pilot.reset();
        report = null;
        driverless = 0;
        motion.reset();
        tracked = null;
        driving = false;
        lastPosition = null;
        reversing = false;
        path = null;
        progress = 0;
        anchor = null;
        stuckAt = null;
        recovery = 0;
        recoveries = 0;
        ahead = DriveSteering.Traffic.NONE;
        waited = 0;
        passUntil = 0L;
    }
}
