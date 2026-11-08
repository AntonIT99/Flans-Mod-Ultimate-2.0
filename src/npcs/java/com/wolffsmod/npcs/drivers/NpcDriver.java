package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.*;
import com.wolffsmod.npcs.teams.BotDriveables;
import lombok.*;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;

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
 *
 * <p>
 * Fighting takes the driveable off that path twice over. One whose weapons have something explosive loaded backs away
 * from a target close enough for its own blast to catch it, and holds that bank's fire meanwhile. A ship then steams a
 * course of its own, as {@link ShipBearing} lays it: bow on while it still has torpedoes or another bank fixed to its
 * hull to fire, round its target once it has none, so that the turrets along its side bear.
 * </p>
 */
@SuppressWarnings("unused") // Made only by the Custom NPCs entity mixin, which IntelliJ does not follow.
public final class NpcDriver
{
    /** Blocks of path ahead the driveable steers for, beyond half its width. */
    private static final double LOOKAHEAD = 6D;
    /**
     * Blocks of path ahead the driveable steers for at the least, beyond half its width, where its hull would run into
     * terrain on the straight way to any point further along.
     */
    private static final double NEAR_LOOKAHEAD = 2D;
    /** Ticks after getting stuck for which the driveable keeps clear of terrain by an extra margin as it cuts corners. */
    private static final int CAREFUL_TICKS = 200;
    /** Blocks kept clear on each side of the hull meanwhile. */
    private static final double CAREFUL_MARGIN = 1D;
    /** Manoeuvres out of being stuck after which the NPC searches its path afresh from where it backed up to. */
    private static final int REPATH_AFTER = 2;
    /** Degrees a tracked vehicle has to come round within the stuck interval while it turns on the spot, not to count as stuck. */
    private static final float STUCK_TURN = 3F;
    /** Hull lengths of path ahead a ship steers for, which it needs to begin a turn early enough to answer it. */
    private static final double SHIP_LOOKAHEAD_LENGTHS = 1.5D;
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
    /** Blocks from the target within which a bomb may come down as a fallback. */
    private static final double BOMB_RADIUS = 4D;
    /** Ticks between looks for a driveable ahead to keep a distance to, and degrees off the heading within which one is ahead. */
    private static final int FOLLOW_INTERVAL = 10;
    private static final float FOLLOW_CONE = 35F;
    /** Ticks a vehicle waits behind a driveable that stands in its way, and ticks it then drives on to find its way round. */
    private static final int WAIT_TICKS = 100;
    private static final int PASS_TICKS = 200;
    /** Blocks past the reach of its own rounds a driveable pulls back to, so that it does not stop on the edge of its own blast. */
    private static final double STANDOFF_MARGIN = 3D;
    /** Blocks above the reach of their blast an aircraft climbs to before it lets its bombs go. */
    private static final double BOMB_ALTITUDE_MARGIN = 4D;

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
    private float anchorYaw;
    private long anchorTime;
    /** Game time until which the driveable keeps an extra margin clear of terrain, after it got stuck. */
    private long carefulUntil;
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
    private final boolean[] triggers = new boolean[2];
    @Nullable
    private BlockHitResult mining;
    @Nullable
    private BlockPos clearing;
    private double clearingProgress;
    private final NpcPilot pilot = new NpcPilot();
    /** Shows the front armour of a halted tracked vehicle to the enemy vehicle that threatens it. */
    private final NpcHullAngling angling = new NpcHullAngling();
    /** How the target fired at moves, to lead it, and which target that is. */
    private final TargetMotion motion = new TargetMotion();
    @Nullable
    private UUID tracked;
    /** What the driveable's weapons have loaded, as last read, and the game time of that reading. */
    private long weaponsAt = Long.MIN_VALUE;
    private double primaryBlast;
    private double secondaryBlast;
    /** Whether each bank has bombs loaded, which fall away rather than being fired, as last read. */
    private boolean primaryDrops;
    private boolean secondaryDrops;
    /** Whether a bank that points along the hull, such as a ship's torpedo tubes, has a round left to fire. */
    private boolean bowWeapon;
    /** Blocks per tick the round of that bank makes, to lay the bow where it meets a moving target; NaN while that is not known. */
    private double bowSpeed = Double.NaN;
    /** Whether a weapon was on the target when the NPC last laid them, which is what a vehicle stands still to shoot for. */
    private boolean bearing;
    /** What a ship is doing with its target; null while it fights nothing. */
    @Nullable
    private ShipBearing.Course course;
    @Nullable
    private Vec3 armorOrdered;
    private long armorOrderedAt = Long.MIN_VALUE / 2;
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
        refreshWeapons(seat, now);
        Vec3 position = driveable.position();
        double speed = lastPosition == null ? 0D : position.subtract(lastPosition).dot(heading);
        lastPosition = position;
        double arrival = Math.max(MIN_ARRIVAL, driveable.getBbWidth() * 0.5D + 1.5D);
        resetClearedObstruction(npc);
        double meleeReach = mechaReach(seat, mecha);
        arrival = mechaArrival(npc, meleeReach, arrival);
        Route route = fightingCourse(npc, seat, position, heading);
        if (route == null)
            // A ship keeps to its own waters; only a vehicle's hull is kept off the terrain beside its path.
            route = route(npc, ship(seat) || mecha ? null : driveable, position, lookahead(driveable, ship(seat)), NpcDrivers.pathSize(npc).map(size -> size.width() * 0.5D).orElse(0.5D));
        if (route == null)
        {
            anchor = null;
            reversing = false;
            Set<DriveControl> controls = DriveSteering.brake(speed);
            Float angle = tracked && !mecha && !ship(seat) && NpcEquipment.enabled(npc, Feature.FRONTAL_ARMOR_ANGLING) ? angling.turn(npc, driveable, heading) : null;
            if (angle != null && angle != 0F && Math.abs(speed) <= DriveSteering.STOPPED)
                controls = EnumSet.of(angle > 0F ? DriveControl.RIGHT : DriveControl.LEFT);
            report(mecha, null, controls, angle == null ? 0F : angle, 0D, speed);
            act(npc, seat, driveable, mecha, controls, null);
            return true;
        }
        float off = Mth.wrapDegrees(MountMotion.yaw(route.steer().subtract(position)) - MountMotion.yaw(heading));
        Set<DriveControl> controls = recovery > 0 ? recover(off, route, arrival, speed, mecha, tracked) : steer(off, route, arrival, speed, mecha, tracked, ship(seat), driveable.getBbWidth());
        if (recovery == 0 && !mecha)
            controls = keepDistance(npc, driveable, heading, speed, controls, now);
        controls = mechaTraversal(npc, seat, driveable, route, controls, mecha);
        updateMiningProgress(seat);
        // A ship steams on; anything else without a stabiliser stands for a moment to shoot, steering on as it does.
        controls = haltForFire(npc, seat, mecha, meleeReach, controls, speed);
        if (recovery > 0)
        {
            recovery--;
            // Backing out of the same spot again and again means the path itself leads into it: search it afresh from here.
            if (recovery == 0 && recoveries >= REPATH_AFTER)
                npc.getNavigation().recomputePath();
        }
        else if (stuck(position, MountMotion.yaw(heading), pushes(controls, mecha), turns(controls, mecha, tracked), now) && !startRecovery(controls, now))
            return false;
        report(mecha, route.steer(), controls, off, route.distance(), speed);
        act(npc, seat, driveable, mecha, controls, off);
        return true;
    }

    private void resetClearedObstruction(EntityNPCInterface npc)
    {
        mining = null;
        if (clearing != null && (!npc.level().hasChunkAt(clearing) || npc.level().getBlockState(clearing).isAir()))
        {
            anchor = null;
            recovery = 0;
            recoveries = 0;
            clearing = null;
            clearingProgress = 0D;
        }
    }

    private static double mechaReach(ISeat seat, boolean mecha)
    {
        return mecha ? MechaCombat.meleeReach(seat) : 0D;
    }

    private static double mechaArrival(EntityNPCInterface npc, double meleeReach, double arrival)
    {
        LivingEntity enemy = npc.getTarget();
        return meleeReach > 0D && enemy != null && enemy.isAlive() ? MechaCombat.approach(meleeReach, enemy.getBoundingBox().getCenter().y - npc.getEyeY()) : arrival;
    }

    private Set<DriveControl> mechaTraversal(EntityNPCInterface npc, ISeat seat, Entity driveable, Route route, Set<DriveControl> controls, boolean mecha)
    {
        if (!mecha)
            return controls;
        double rise = route.steer().y - driveable.getY();
        if (path != null && progress + 1 < path.getNodeCount())
            rise = path.getNodePos(progress + 1).getY() - driveable.getY();
        double step = seat.getDriveableType().map(type -> FlansEntityTypes.getStepHeight(type).orElse(0.5D)).orElse(0.5D);
        double jump = seat.getDriveableType().flatMap(FlansEntityTypes::getMechaMobility).map(mobility -> mobility.jumpHeight().orElse(1.25D)).orElse(1.25D);
        if (MechaCombat.ascend(rise, step, jump, driveable.onGround(), driveable.horizontalCollision && pushes(controls, true), seat.hasDriverRocketPack()))
        {
            controls = new HashSet<>(controls);
            controls.add(DriveControl.ASCEND);
        }
        if (pushes(controls, true) && (driveable.horizontalCollision || recovery > 0))
            mining = miningObstruction(npc, seat, route.steer());
        return controls;
    }

    private Set<DriveControl> haltForFire(EntityNPCInterface npc, ISeat seat, boolean mecha, double meleeReach, Set<DriveControl> controls, double speed)
    {
        boolean halt = recovery == 0 && (!mecha || meleeReach <= 0D) && mining == null && !ship(seat) && NpcVehicleState.of(npc).getGunnery().haltsToFire(options(npc), bearing);
        return halt ? DriveSteering.halt(controls, speed) : controls;
    }

    private void updateMiningProgress(ISeat seat)
    {
        if (mining == null)
            return;
        if (!mining.getBlockPos().equals(clearing))
        {
            clearing = mining.getBlockPos();
            clearingProgress = 0D;
        }
        double completed = seat.getDriverMiningProgress(clearing);
        if (completed > clearingProgress)
        {
            clearingProgress = completed;
            anchor = null;
            recovery = 0;
            recoveries = 0;
        }
    }

    /**
     * Where the driveable goes to fight, rather than along the NPC's path: out of the reach of its own rounds first, and
     * then, for a ship, onto the bearing its weapons need. Null while the NPC's own path leads it.
     *
     * @param heading
     *            where the driveable's forward control takes it, which it backs away along from a target right on top of it
     */
    @Nullable
    private Route fightingCourse(EntityNPCInterface npc, ISeat seat, Vec3 position, Vec3 heading)
    {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || target.level() != npc.level())
        {
            course = null;
            return null;
        }
        double distance = horizontal(position, target.position());
        if (meleeMecha(seat))
            return meleeMechaCourse(npc, target, position, distance);
        double standoff = combatDistance(npc, seat, target);
        boolean adaptive = NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) && NpcCombatMovement.allowed(npc);
        if (standoff > distance)
        {
            course = null;
            double back = standoff - distance + STANDOFF_MARGIN;
            Vec3 point = position.add(away(position, target.position(), heading).scale(back));
            return !adaptive || NpcCombatMovement.permits(npc, point) ? new Route(point, back) : null;
        }
        if (adaptiveMecha(seat, adaptive))
            return rangedMechaCourse(npc, seat, target, position, distance, standoff);
        if (!NpcEquipment.enabled(npc, Feature.SHIP_WEAPON_BEARING) || !ship(seat) || distance > Math.max(MIN_FIRE_RANGE, npc.stats.ranged.getRange()))
        {
            course = null;
            return adaptive ? groundCourse(npc, seat, target, position) : null;
        }
        // A ship steams on rather than stopping, so its goal stays a fixed lead ahead of it and it never brakes for it.
        double hold = Math.max(standoff, NpcTactics.engagement(npc).preferred());
        course = ShipBearing.course(bowWeapon, distance, hold, course);
        Vec3 aim = bowAim(npc, target, position);
        Entity hull = NpcVehicleThreats.platform(npc);
        Vec3 point = ShipBearing.clearPoint(course, position, aim, hold, (npc.getId() & 1) == 0 ? 1 : -1, ahead -> ShipWaters.clear(npc, hull, ahead));
        return new Route(point, ShipBearing.LEAD);
    }

    private static boolean meleeMecha(ISeat seat)
    {
        return seat.getDriveableType().filter(IMechaType.class::isInstance).isPresent() && MechaCombat.meleeReach(seat) > 0D;
    }

    private static boolean adaptiveMecha(ISeat seat, boolean adaptive)
    {
        return seat.getDriveableType().filter(IMechaType.class::isInstance).isPresent() && adaptive;
    }

    @Nullable
    private Route meleeMechaCourse(EntityNPCInterface npc, LivingEntity target, Vec3 position, double distance)
    {
        course = null;
        if (!NpcCombatMovement.allowed(npc))
            return null;
        Route pursuit = route(npc, null, position, 2D, NpcDrivers.pathSize(npc).map(size -> size.width() * 0.5D).orElse(0.5D));
        Vec3 destination = pursuit == null ? target.position() : pursuit.steer();
        return NpcCombatMovement.permits(npc, destination) ? new Route(destination, distance) : null;
    }

    @Nullable
    private Route rangedMechaCourse(EntityNPCInterface npc, ISeat seat, LivingEntity target, Vec3 position, double distance, double standoff)
    {
        course = null;
        double hold = Math.max(standoff, Math.max(1D, npc.stats.ranged.getRange()) * 0.5D);
        if (distance > hold && NpcCombatMovement.permits(npc, target.position()))
            return new Route(target.position(), distance - hold + Math.max(MIN_ARRIVAL, NpcVehicleThreats.platform(npc).getBbWidth() * 0.5D + 1.5D));
        Route lateral = lateralCourse(npc, seat, target, position);
        if (lateral != null)
            return lateral;
        return distance <= hold ? new Route(position, 0D) : null;
    }

    /**
     * @return the point the ship lays its bow on: where the round its hull-fixed bank fires meets the target while it is
     *         running in to fire one, and the target itself on any other course
     */
    private Vec3 bowAim(EntityNPCInterface npc, LivingEntity target, Vec3 position)
    {
        if (course != ShipBearing.Course.RUN_IN)
            return target.position();
        Optional<TargetMotion> motion = NpcVehicleState.of(npc).motionOf(target);
        if (motion.isEmpty())
            return target.position();
        return ShipBearing.intercept(position, target.position(), bowSpeed, motion.get()::lead);
    }

    @Nullable
    private Route groundCourse(EntityNPCInterface npc, ISeat seat, LivingEntity target, Vec3 position)
    {
        Route flank = armorCourse(npc, seat, target, position);
        return flank != null ? flank : lateralCourse(npc, seat, target, position);
    }

    @Nullable
    private Route armorCourse(EntityNPCInterface npc, ISeat seat, LivingEntity target, Vec3 position)
    {
        Vec3 flank = NpcVehicleCombat.armorFlank(npc, target);
        if (flank == null)
        {
            armorOrdered = null;
            return null;
        }
        long now = npc.level().getGameTime();
        if (armorOrdered == null || armorOrdered.distanceToSqr(flank) > 4D || npc.getNavigation().isDone() && now - armorOrderedAt >= 20L)
        {
            npc.getNavigation().moveTo(flank.x, flank.y, flank.z, 1D);
            armorOrdered = flank;
            armorOrderedAt = now;
        }
        Entity hull = NpcVehicleThreats.platform(npc);
        return route(npc, ship(seat) ? null : hull, position, LOOKAHEAD + hull.getBbWidth() * 0.5D, NpcDrivers.pathSize(npc).map(size -> size.width() * 0.5D).orElse(0.5D));
    }

    private double combatDistance(EntityNPCInterface npc, ISeat seat, LivingEntity target)
    {
        double standoff = NpcBlastSafety.standoff(npc, blastReach());
        if (seat.getDriveableType().filter(IMechaType.class::isInstance).isPresent())
            return MechaCombat.meleeReach(seat) > 0D ? 0D : standoff;
        if (!NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) || !NpcCombatMovement.allowed(npc) || ship(seat) || NpcVehicleThreats.groundVehicle(target))
            return standoff;
        boolean safeBank = seat.getDriverWeaponOrigin(false).isPresent() && primaryBlast <= 0D || seat.getDriverWeaponOrigin(true).isPresent() && secondaryBlast <= 0D;
        return NpcVehicleCombat.infantryDistance(npc.stats.ranged.getRange(), safeBank ? 0D : standoff);
    }

    @Nullable
    private static Route lateralCourse(EntityNPCInterface npc, ISeat seat, LivingEntity target, Vec3 position)
    {
        if (ship(seat) || npc.distanceTo(target) > npc.stats.ranged.getRange())
            return null;
        Optional<Vec3> muzzle = seat.getDriverWeaponOrigin(turretBank(seat));
        if (muzzle.isEmpty()
            || npc.level().clip(new ClipContext(muzzle.get(), target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS)
            return null;
        Vec3 point = NpcVehicleCombat.lateralPosition(npc, muzzle.get(), target);
        return point == null ? null : new Route(point, horizontal(position, point));
    }

    /** @return the horizontal unit direction from the target to the driveable, or straight back along its heading when it is right on top of it */
    private static Vec3 away(Vec3 position, Vec3 target, Vec3 heading)
    {
        Vec3 offset = new Vec3(position.x - target.x, 0D, position.z - target.z);
        return offset.lengthSqr() < 1.0E-4D ? heading.scale(-1D) : offset.normalize();
    }

    private static boolean ship(ISeat seat)
    {
        return seat.getDriveableType().map(IDriveableType::isShip).orElse(false);
    }

    /** Whether the driveable fights from the air, whose speed does not throw off guns laid by flying it; a ship built on a plane does not. */
    private static boolean aircraft(ISeat seat)
    {
        return seat.getDriveableType().filter(type -> !type.isShip()).map(type -> type.getDriveableKind() == DriveableKind.PLANE).orElse(false);
    }

    /**
     * Reads what the driveable's weapons have loaded now and then, rather than every tick: finding the round scans the
     * driveable for it.
     */
    private void refreshWeapons(ISeat seat, long now)
    {
        if (weaponsAt != Long.MIN_VALUE && now >= weaponsAt && now - weaponsAt < NpcBlastSafety.REACH_INTERVAL)
            return;
        weaponsAt = now;
        primaryBlast = seat.getDriverWeaponDetonation(false).map(NpcBlastSafety::reach).orElse(0D);
        secondaryBlast = seat.getDriverWeaponDetonation(true).map(NpcBlastSafety::reach).orElse(0D);
        primaryDrops = seat.getDriverWeaponBallistics(false).map(WeaponBallistics::dropped).orElse(false);
        secondaryDrops = seat.getDriverWeaponBallistics(true).map(WeaponBallistics::dropped).orElse(false);
        bowWeapon = bowWeapon(seat, false) || bowWeapon(seat, true);
        bowSpeed = bowSpeed(seat);
    }

    /** @return whether the bank points along the hull and has a round to fire, as a ship's torpedo tubes do until they are spent */
    private static boolean bowWeapon(ISeat seat, boolean secondary)
    {
        return seat.isDriverWeaponHullFixed(secondary) && seat.getDriverWeaponBallistics(secondary).isPresent();
    }

    /** @return blocks per tick the round of the bank fixed to the hull makes, or NaN while no such bank has one loaded */
    private static double bowSpeed(ISeat seat)
    {
        for (boolean secondary : new boolean[]{false, true})
        {
            Optional<WeaponBallistics> ballistics = seat.isDriverWeaponHullFixed(secondary) ? seat.getDriverWeaponBallistics(secondary) : Optional.empty();
            if (ballistics.isPresent())
                return ballistics.get().speed();
        }
        return Double.NaN;
    }

    /**
     * @return how far round where it goes off the furthest-reaching round the driveable's weapons have loaded reaches; 0
     *         while the NPC drives nothing, or nothing it has loaded goes off
     */
    public double blastReach()
    {
        return Math.max(primaryBlast, secondaryBlast);
    }

    /** @return that reach for one bank alone, which decides whether that bank may fire now */
    private double blastReach(boolean secondary)
    {
        return secondary ? secondaryBlast : primaryBlast;
    }

    /** @return whether the bank has bombs loaded */
    private boolean drops(boolean secondary)
    {
        return secondary ? secondaryDrops : primaryDrops;
    }

    /**
     * @return how far the furthest-reaching round the aircraft fires, rather than drops, reaches: what it has to keep its
     *         distance from across the ground, since it keeps clear of its bombs by height instead
     */
    private double fireReach()
    {
        return Math.max(primaryDrops ? 0D : primaryBlast, secondaryDrops ? 0D : secondaryBlast);
    }

    /**
     * The bomb run an aircraft flies on its target: on a target on the ground, either worth its bombs or the only kind
     * of attack its weapons make, with the main bank's bombs where both banks carry them.
     *
     * @return the run, or null while it has no bombs for the target
     */
    @Nullable
    private NpcPilot.BombRun bombRun(EntityNPCInterface npc, ISeat seat, Entity aircraft)
    {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || !primaryDrops && !secondaryDrops || NpcBombTargets.airborne(target.getRootVehicle()))
            return null;
        boolean secondary = !primaryDrops;
        boolean guns = rangedBank(seat, false) && !primaryDrops || rangedBank(seat, true) && !secondaryDrops;
        if (guns && !NpcBombTargets.worthBombing(npc, target))
            return null;
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        Optional<WeaponBallistics> ballistics = seat.getDriverWeaponBallistics(secondary);
        if (origin.isEmpty() || ballistics.isEmpty())
            return null;
        WeaponAim.Fall fall = WeaponAim.fall(origin.get(), aircraft.getDeltaMovement(), ballistics.get().gravity(), ballistics.get().drag(), target.getY());
        if (!fall.landed())
            return null;
        Vec3 lead = bombLead(npc, target);
        Vec3 meeting = target.position().add(lead.x * fall.ticks(), 0D, lead.z * fall.ticks());
        double standoff = NpcBlastSafety.standoff(npc, blastReach(secondary));
        return new NpcPilot.BombRun(releasePoint(aircraft.position(), fall.impact(), meeting), bombAltitude(target.getY(), standoff));
    }

    /**
     * @param origin
     *            where a bomb let go now leaves the aircraft
     * @param impact
     *            where it comes down
     * @param meeting
     *            where it has to come down to meet the target
     * @return where across the ground the bomb has to leave the aircraft to come down there, at the height of the meeting
     */
    static Vec3 releasePoint(Vec3 origin, Vec3 impact, Vec3 meeting)
    {
        return WeaponAim.releasePoint(origin, impact, meeting);
    }

    /**
     * @param standoff
     *            blocks from where they go off within which the aircraft's bombs would catch it; 0 while it does not keep
     *            clear of their blast
     * @return the lowest height above the target a bomb is let go from, clear of its blast; negative infinity without a standoff
     */
    static double bombAltitude(double targetY, double standoff)
    {
        return standoff > 0D ? targetY + standoff + BOMB_ALTITUDE_MARGIN : Double.NEGATIVE_INFINITY;
    }

    /**
     * @param standoff
     *            blocks from where they go off within which the aircraft's bombs would catch it
     * @return whether a bomb let go from that height above its target would catch the aircraft: it flies on above where the
     *         bomb comes down, so the height is how close it is to the blast
     */
    static boolean belowOwnBlast(double originY, double targetY, double standoff)
    {
        return standoff > 0D && originY - targetY < standoff;
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

    /** Starts a manoeuvre out of being stuck, after which the driveable keeps clear of terrain for a while. @return false once the NPC has tried too often */
    private boolean startRecovery(Set<DriveControl> controls, long now)
    {
        if (++recoveries > GIVE_UP)
            return false;
        recovery = RECOVERY_TICKS;
        carefulUntil = now + CAREFUL_TICKS;
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
        refreshWeapons(seat, now);
        Set<DriveControl> controls = pilot.fly(npc, aircraft, mode, goal(npc), NpcBlastSafety.standoff(npc, fireReach()), bombRun(npc, seat, aircraft));
        report = new Report(pilot.getPhase().name().toLowerCase(Locale.ROOT), pilot.getPoint(), controls, pilot::describe);
        // An aircraft that cannot get off the ground, for want of fuel or of room, is given up.
        if (stuck(aircraft.position(), 0F, pilot.isRolling(), false, now) && ++recoveries > GIVE_UP)
            return false;
        if (!seat.setAutomatedDriving(controls))
            return true;
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive())
            setTriggers(seat, false, false);
        else
            fire(npc, seat, aircraft, target, MIN_AIR_FIRE_RANGE);
        return true;
    }

    /** @return where the NPC's path ends, or null without one */
    @Nullable
    private static Vec3 goal(EntityNPCInterface npc)
    {
        Optional<Vec3> mission = BotDriveables.destination(npc);
        if (mission.isPresent())
            return mission.get();
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

    private Set<DriveControl> steer(float off, Route route, double arrival, double speed, boolean mecha, boolean tracked, boolean ship, double length)
    {
        if (mecha)
            return DriveSteering.mecha(off, route.distance(), arrival);
        DriveSteering.Situation situation = new DriveSteering.Situation(off, route.distance(), arrival, speed, tracked, reversing);
        DriveSteering.Decision decision = ship ? DriveSteering.ship(situation, length) : DriveSteering.vehicle(situation);
        reversing = decision.reversing();
        return decision.controls();
    }

    /**
     * @param ship
     *            whether the driveable answers its rudder over a length of its own, so that it has to be steered for a
     *            point much farther along its path than a vehicle, or it turns too late and sways down a channel
     * @return blocks ahead along its path the driveable steers for
     */
    private static double lookahead(Entity driveable, boolean ship)
    {
        double ahead = LOOKAHEAD + driveable.getBbWidth() * 0.5D;
        return ship ? Math.max(ahead, driveable.getBbWidth() * SHIP_LOOKAHEAD_LENGTHS) : ahead;
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

    /** Whether a tracked vehicle turns on the spot, which a wall along its side can hold it from as surely as one ahead. */
    private static boolean turns(Set<DriveControl> controls, boolean mecha, boolean tracked)
    {
        return tracked && !mecha && (controls.contains(DriveControl.LEFT) || controls.contains(DriveControl.RIGHT));
    }

    /**
     * Whether the driveable has been driven without moving for a while, or a tracked one turned on the spot without coming
     * round.
     *
     * @param yaw
     *            where its heading points
     * @param turning
     *            whether it turns on the spot, which counts as moving as long as it comes round
     */
    private boolean stuck(Vec3 position, float yaw, boolean pushing, boolean turning, long now)
    {
        if (stuckAt != null && position.distanceToSqr(stuckAt) > FREE_DISTANCE * FREE_DISTANCE)
        {
            stuckAt = null;
            recoveries = 0;
        }
        if (!pushing && !turning || now - drivingSince < START_GRACE)
        {
            anchor = null;
            return false;
        }
        if (anchor == null)
        {
            anchor = position;
            anchorYaw = yaw;
            anchorTime = now;
            return false;
        }
        if (now - anchorTime < STUCK_INTERVAL)
            return false;
        // A vehicle that only comes round while it pushes against something still gets nowhere.
        boolean moved = anchor.distanceToSqr(position) >= STUCK_DISTANCE * STUCK_DISTANCE || !pushing && Math.abs(Mth.wrapDegrees(yaw - anchorYaw)) >= STUCK_TURN;
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
        if (mining != null)
        {
            seat.aimDriverWeapon(!seat.canDriverMineBlock(false, mining.getBlockPos()), mining.getLocation());
            setTriggers(seat, seat.canDriverMineBlock(false, mining.getBlockPos()), seat.canDriverMineBlock(true, mining.getBlockPos()));
            bearing = false;
            return;
        }
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive())
        {
            bearing = false;
            setTriggers(seat, false, false);
            if (!mecha)
                seat.setAutomatedAim(0F, 0F);
            else if (off != null)
                seat.setAutomatedAim(seat.getAimYaw() + off, 0F);
            return;
        }
        fire(npc, seat, driveable, target, mecha ? 1D : MIN_FIRE_RANGE);
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
        boolean secondary = turretBank(seat);
        if (NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) && !NpcVehicleThreats.groundVehicle(target))
        {
            boolean other = !secondary;
            if (rangedBank(seat, other) && !seat.isDriverWeaponHullFixed(other) && blastReach(other) < blastReach(secondary))
                secondary = other;
        }
        boolean aimedBank = secondary;
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        // The driveable launches its own rounds along its barrels, so what the NPC's aim is worth has to go into how it lays
        // them: its own limits, and how steady the hull it rides is. Only the driveable's speed is known here, not its turn.
        NpcSkill.AimError error = NpcSkill.layingError(npc, target).plus(NpcGunSteadiness.spread(npc, driveable.getDeltaMovement().horizontalDistance(), 0F, options(npc), aircraft(seat)));
        Optional<Vec3> launch = origin.map(from -> error.apply(launch(npc, seat, aimedBank, aimPoint.subtract(from))));
        if (origin.isPresent() && launch.isPresent())
            seat.aimDriverWeapon(secondary, origin.get().add(launch.get().scale(aimPoint.distanceTo(origin.get()))));
        else
            seat.aimAt(aimPoint);
        // Guns wait for the NPC to react; bombs must take their brief release window.
        double range = Math.max(reach, npc.stats.ranged.getRange());
        boolean primaryBears = readyToFire(npc, seat, driveable, false, target, range);
        boolean secondaryBears = readyToFire(npc, seat, driveable, true, target, range);
        // Whether a weapon is on the target at all, rather than whether the trigger is down this tick, is what tells the
        // driving that it is worth standing still for: the triggers are let go of regularly to cycle them.
        bearing = primaryBears || secondaryBears;
        setTriggers(seat, !NpcSkill.holdsFire(npc, target, primaryDrops) && primaryBears, !NpcSkill.holdsFire(npc, target, secondaryDrops) && secondaryBears);
    }

    private void setTriggers(ISeat seat, boolean primary, boolean secondary)
    {
        triggers[0] = MechaCombat.trigger(primary, triggers[0], seat.getDriverTriggerState(false));
        triggers[1] = MechaCombat.trigger(secondary, triggers[1], seat.getDriverTriggerState(true));
        seat.setAutomatedTriggers(triggers[0], triggers[1]);
    }

    @Nullable
    private static BlockHitResult miningObstruction(EntityNPCInterface npc, ISeat seat, Vec3 ahead)
    {
        for (boolean secondary : new boolean[]{false, true})
        {
            OptionalDouble reach = seat.getDriverMiningReach(secondary);
            Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
            if (reach.isEmpty() || origin.isEmpty())
                continue;
            Entity hull = seat.getDriveable();
            Vec3 feet = hull == null ? npc.position() : hull.position();
            Vec3 toward = ahead.subtract(feet).multiply(1D, 0D, 1D).normalize();
            double probe = hull == null ? 1.5D : hull.getBbWidth() * 0.5D + 1D;
            Vec3 direction = feet.add(toward.scale(probe)).add(0D, 0.5D, 0D).subtract(origin.get()).normalize();
            var hit = npc.level().clip(new ClipContext(origin.get(), origin.get().add(direction.scale(reach.getAsDouble())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().getY() >= Mth.floor(feet.y) && seat.canDriverMineBlock(secondary, hit.getBlockPos()))
                return hit;
        }
        return null;
    }

    /**
     * @return which of the driver's two banks the turret, or a mecha's arms, follows: the one that turns with it, since
     *         a bank fixed to the hull, such as a ship's torpedo tubes, is laid by turning the whole driveable instead.
     *         The main weapon otherwise, or the other one where the driveable has no main weapon
     */
    static boolean turretBank(ISeat seat)
    {
        boolean primaryRanged = rangedBank(seat, false);
        boolean secondaryRanged = rangedBank(seat, true);
        if (primaryRanged != secondaryRanged)
            return secondaryRanged;
        boolean primary = seat.getDriverWeaponOrigin(false).isPresent();
        boolean secondary = seat.getDriverWeaponOrigin(true).isPresent();
        if (!primary || !secondary)
            return !primary && secondary;
        return seat.isDriverWeaponHullFixed(false) && !seat.isDriverWeaponHullFixed(true);
    }

    private static boolean rangedBank(ISeat seat, boolean secondary)
    {
        return seat.getDriverWeaponOrigin(secondary).isPresent() && seat.getDriverMeleeProperties(secondary).isEmpty() && seat.getDriverMiningReach(secondary).isEmpty();
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
        return seat.getDriverWeaponBallistics(secondary).filter(ballistics -> !ballistics.dropped()).map(ballistics -> WeaponAim.launch(displacement, motion, ballistics,
            options.enabled(Feature.LEAD_TARGET), options.enabled(Feature.ADVANCED_LEAD), options.enabled(Feature.BALLISTIC_AIM), NpcSkill.leadShare(npc))).orElse(displacement.normalize());
    }

    private boolean readyToFire(EntityNPCInterface npc, ISeat seat, Entity driveable, boolean secondary, LivingEntity target, double range)
    {
        if (seat.getDriverMiningReach(secondary).isPresent())
            return false;
        var melee = seat.getDriverMeleeProperties(secondary);
        if (melee.isPresent())
        {
            Optional<Vec3> hand = seat.getDriverWeaponOrigin(secondary);
            if (hand.isEmpty() || !NpcVehicleCombat.mayFire(npc, target, hand.get(), Optional.empty(), false))
                return false;
            Vec3 closest = new Vec3(Mth.clamp(hand.get().x, target.getBoundingBox().minX, target.getBoundingBox().maxX),
                Mth.clamp(hand.get().y, target.getBoundingBox().minY, target.getBoundingBox().maxY), Mth.clamp(hand.get().z, target.getBoundingBox().minZ, target.getBoundingBox().maxZ));
            return hand.get().distanceToSqr(closest) <= melee.get().reach() * melee.get().reach()
                && seat.getDriverWeaponDirection(secondary).map(direction -> direction.dot(target.getBoundingBox().getCenter().subtract(hand.get()).normalize()) > 0.8D).orElse(false)
                && npc.level().clip(new ClipContext(hand.get(), target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS;
        }
        Optional<Vec3> origin = seat.getDriverWeaponOrigin(secondary);
        Optional<Vec3> direction = seat.getDriverWeaponDirection(secondary);
        if (origin.isEmpty() || direction.isEmpty())
            return false;
        if (!NpcVehicleCombat.mayFire(npc, target, origin.get(), seat.getDriverWeaponArmor(secondary), blastReach(secondary) > 0D))
            return false;
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        Vec3 line = aimPoint.subtract(origin.get());
        if (line.lengthSqr() > range * range)
            return false;
        Optional<WeaponBallistics> ballistics = seat.getDriverWeaponBallistics(secondary);
        boolean dropped = ballistics.isPresent() && ballistics.get().dropped();
        // What this bank has loaded would catch the driveable itself from here; it waits until the range has opened, or
        // for a bomb, which the aircraft flies on above, until it is high enough above the target.
        if (dropped ? belowOwnBlast(origin.get().y, target.getY(), NpcBlastSafety.standoff(npc, blastReach(secondary))) : NpcBlastSafety.tooClose(npc, target, blastReach(secondary)))
            return false;
        if (dropped)
            return NpcBombing.solve(driveable, origin.get(), ballistics.get().gravity(), ballistics.get().drag(), Math.max(BOMB_RADIUS, NpcBombTargets.releaseRadius(blastReach(secondary))), target, bombLead(npc, target)).drop();
        if (direction.get().dot(launch(npc, seat, secondary, line)) < FIRE_CONE)
            return false;
        ClipContext sight = new ClipContext(origin.get(), aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc);
        return npc.level().clip(sight).getType() == HitResult.Type.MISS;
    }

    /** @return the NPC's own weapon switches */
    private static NpcWeaponOptions options(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
    }

    /** @return the target's velocity a bomb is led by, as far as the NPC's aiming switches lead targets; none without leading */
    private Vec3 bombLead(EntityNPCInterface npc, LivingEntity target)
    {
        NpcWeaponOptions options = options(npc);
        if (!options.enabled(Feature.LEAD_TARGET))
            return Vec3.ZERO;
        // A long fall amplifies even a small skill penalty into a large miss. Bombs use full measured lead.
        return options.enabled(Feature.ADVANCED_LEAD) ? motion.velocity() : target.getDeltaMovement();
    }

    /**
     * Reads the point to steer for and the distance to the goal off the NPC's path; none without a path. The point lies as
     * far along the path as the lookahead reaches, but no further than the hull can drive to in a straight line without
     * running into terrain: cutting the corner of a building the path goes round is what drives a vehicle into its wall.
     *
     * @param hull
     *            the driveable whose width has to fit along the straight way to that point; null to not check it
     * @param middle
     *            blocks from a node's corner to the middle of the path: half the width it was searched for
     */
    @Nullable
    private Route route(EntityNPCInterface npc, @Nullable Entity hull, Vec3 position, double lookahead, double middle)
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
        int furthest = progress;
        int near = progress;
        double nearReach = hull == null ? lookahead : Math.min(lookahead, NEAR_LOOKAHEAD + hull.getBbWidth() * 0.5D);
        double travelled = horizontal(position, steer);
        for (int index = progress + 1; index < current.getNodeCount(); index++)
        {
            Vec3 next = node(current, index, middle);
            travelled += horizontal(steer, next);
            if (travelled > lookahead)
                break;
            steer = next;
            furthest = index;
            if (travelled <= nearReach)
                near = index;
        }
        if (hull != null && furthest > near)
            steer = node(current, clearNode(current, hull, position, near, furthest, middle), middle);
        return new Route(steer, horizontal(position, node(current, current.getNodeCount() - 1, middle)));
    }

    /**
     * @param near
     *            the node steered for, however close, when the hull cannot drive straight to any further one
     * @return the furthest node up to the given one that the hull drives to in a straight line without running into
     *         terrain, with a margin to spare while it is being careful after getting stuck
     */
    private int clearNode(Path current, Entity hull, Vec3 position, int near, int furthest, double middle)
    {
        Level level = hull.level();
        double width = NpcDrivers.hullWidth(hull) + (level.getGameTime() < carefulUntil ? CAREFUL_MARGIN * 2D : 0D);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        DriveCorridor.Solid solid = (x, y, z) -> level.isLoaded(cursor.set(x, y, z)) && !level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty();
        for (int index = furthest; index > near; index--)
        {
            if (DriveCorridor.clear(position, node(current, index, middle), width, solid))
                return index;
        }
        return near;
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
        Arrays.fill(triggers, false);
        mining = null;
        clearing = null;
        clearingProgress = 0D;
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
        carefulUntil = 0L;
        stuckAt = null;
        recovery = 0;
        recoveries = 0;
        ahead = DriveSteering.Traffic.NONE;
        waited = 0;
        passUntil = 0L;
        weaponsAt = Long.MIN_VALUE;
        primaryBlast = 0D;
        secondaryBlast = 0D;
        primaryDrops = false;
        secondaryDrops = false;
        angling.reset();
        bowWeapon = false;
        course = null;
    }
}
