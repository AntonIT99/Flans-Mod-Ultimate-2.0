package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.WeaponAim;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.VehicleStatePacket;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The moving state of an NPC drawn as a content-pack model: where its hull faces, where its turret and seat guns point,
 * whether the gun is on target, and how fast it may drive. Not saved: it is rebuilt from the NPC's settings and
 * surroundings.
 *
 * <p>
 * The server decides hull and gun directions and sends them to the clients tracking the NPC. Each side measures how
 * the NPC actually moved, which the client turns into wheel, track and propeller motion. Angles are in degrees; turret
 * and seat aims are relative to the hull.
 * </p>
 */
public final class NpcVehicleState
{
    /** Ticks between firing solutions while a target is held. */
    private static final int SOLVE_INTERVAL = 3;
    private static final int BALLISTICS_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 2;
    private static final int LERP_STEPS = 3;
    /** Hull turn rate, in degrees per tick, of a vehicle whose NPC states none. */
    private static final float DEFAULT_HULL_STEP = 4.5F;
    /** Traverse and elevation, in degrees per tick, of a turret whose model has no driver seat to state them. */
    private static final float DEFAULT_AIM_STEP = 8F;
    /** Degrees a hovering helicopter tips its nose up or down at most to bring guns fixed to its hull onto its target, and per tick. */
    private static final float MAX_HOVER_TIP = 45F;
    private static final float HOVER_TIP_STEP = 3F;
    /** Squared blocks a muzzle has to move by as the turret turns to count as one of the turret's. */
    private static final double TURNING_MUZZLE = 1.0E-6D;
    /** Fewest blocks above its target a bomb is let go from, and blocks from the target within which it may come down. */
    private static final double MIN_BOMB_HEIGHT = 4D;
    private static final double BOMB_RADIUS = 5D;
    private static final float[] NO_SEATS = new float[0];
    /** Degrees an aircraft gunner turns its gun before its torso follows, as a player's does in a cockpit. */
    private static final float COCKPIT_TORSO_FREE = 75F;
    /** Blocks above the ground, at size 5, below which dynamic landing gear comes down and above which it goes up again. */
    private static final double GEAR_DOWN_HEIGHT = 6D;
    private static final double GEAR_UP_HEIGHT = 10D;
    /** Share of each tick's rise and run the measured grade takes in. */
    private static final double GRADE_SMOOTHING = 0.1D;
    /** Blocks per tick below which a vehicle is too slow for its grade to mean anything. */
    private static final double MIN_GRADE_RUN = 1.0E-3D;

    /** Whether the NPC currently is a content-pack model. */
    @Getter
    private boolean active;
    @Nullable
    private Layout layout;
    @Getter
    private float hullYaw;
    private boolean hullKnown;
    private float turretYaw;
    private float turretPitch;
    private float[] seatYaw = NO_SEATS;
    private float[] seatPitch = NO_SEATS;
    /** Shots fired since the state was created; clients start a recoil stroke when it grows. */
    @Getter
    private int shots;
    @Getter
    private int barrel;
    /**
     * Whether the main weapon may fire: the gun points where the shot has to go, or a bomb let go now would come down on the
     * target. Always true for a gun when nothing has to line up.
     */
    @Getter
    private boolean aligned = true;
    /** Whether the guns point where the shot has to go, which guns beside a bomb bay go by. */
    @Getter
    private boolean gunAligned = true;
    /** Whether the hull points where the shot has to go, which guns fixed to the hull of a turreted model go by. */
    @Getter
    private boolean hullAligned = true;
    /** Whether the main weapon drops bombs rather than firing at the target. */
    @Getter
    private boolean bombing;
    /**
     * Degrees the model is pitched nose down, negative nose up, and rolled to its right, the right side down: the attitude of
     * a plane in flight, level otherwise.
     */
    @Getter
    private float attitudePitch;
    @Getter
    private float attitudeBank;
    /** Where a bomb let go now would have to come down to meet the target, while the NPC bombs one. */
    @Nullable
    private Vec3 bombPoint;
    /** Share of its speed the NPC keeps this tick while its hull turns toward where it wants to go. */
    @Getter
    private float speedFactor = 1F;
    /** Blocks moved along the hull during the last tick, negative in reverse. */
    @Getter
    private float travel;
    /** Degrees the hull turned during the last tick. */
    @Getter
    private float turn;
    @Nullable
    private Vec3 lastPosition;
    private float lastHullYaw;
    /** Blocks moved across the ground during the last tick, whichever way. */
    private double groundSpeed;
    /** Smoothed blocks the NPC rose and travelled across the ground per tick, which make the grade it drives up. */
    private double climbRise;
    private double climbRun;

    // Server
    private final TargetMotion motion = new TargetMotion();
    @Getter
    private final NpcDriving driving = new NpcDriving();
    @Getter
    private final NpcFlight flight = new NpcFlight();
    @Getter
    private final NpcSubmarine submarine = new NpcSubmarine();
    /** The part hitboxes of the model, on either side. */
    @Getter
    private final NpcParts parts = new NpcParts();
    /** Where a ground vehicle drives to for a shot its main weapon cannot make from where it stands. */
    @Getter
    private final NpcFiringPosition firingPosition = new NpcFiringPosition();
    private int targetId = -1;
    @Nullable
    private Vec3 solution;
    /** Where the main weapon's rounds left from when the solution was worked out. */
    @Nullable
    private Vec3 solvedFrom;
    /** The pitch nearest to its shot that the main weapon of a vehicle reaches, while the shot lies beyond its elevation; NaN otherwise. */
    private float reachablePitch = Float.NaN;
    private int solvedAt = Integer.MIN_VALUE;
    private double shotSpeed = Double.NaN;
    private double shotGravity;
    private double shotDrag = 1D;
    private int ballisticsAt = Integer.MIN_VALUE;
    private int sentSignature;
    private boolean sent;
    private int shapeSignature = Integer.MIN_VALUE;
    private int fireCheckedAt = Integer.MIN_VALUE;
    private boolean fireClear = true;
    /** The world direction each manned seat gun's rounds have to leave in, as its gunner last worked it out; null for none. */
    private Vec3[] seatLead = new Vec3[0];

    // Client
    private float targetHullYaw;
    private float targetTurretYaw;
    private float targetTurretPitch;
    /** Degrees per tick the hull and the turret turned at between the last two states received. */
    private float hullYawRate;
    private float turretYawRate;
    private float[] targetSeatYaw = NO_SEATS;
    private float[] targetSeatPitch = NO_SEATS;
    private int lerpSteps;
    private boolean received;
    private boolean gearDown = true;
    private float targetAttitudePitch;
    private float targetAttitudeBank;
    private float previousAttitudePitch;
    private float previousAttitudeBank;

    /**
     * What the selected model offers, resolved once per model and weapon bank.
     *
     * @param barrel
     *            the middle of the main weapon's muzzles at rest, or null without any
     * @param pivot
     *            the point the main weapon turns about: where a turret turns and elevates it, or the middle of the hull at the
     *            height of a gun fixed to it; null without muzzles
     */
    private record Layout(IContentType model, FlanModelKind kind, boolean secondary, List<WeaponMount> mounts, @Nullable WeaponMount driver, boolean turreted, @Nullable Vec3 muzzle, double reach,
        double top, @Nullable Vec3 barrel, @Nullable Vec3 pivot)
    {
        static Layout of(IContentType model, FlanModelKind kind, boolean secondary)
        {
            List<WeaponMount> mounts = FlansEntityTypes.getMounts(model);
            WeaponMount driver = mounts.stream().filter(WeaponMount::driver).findFirst().orElse(null);
            List<WeaponMuzzle> rest = FlansProjectiles.getMuzzles(model, secondary, 0, false);
            List<WeaponMuzzle> turned = FlansProjectiles.getMuzzles(model, secondary, 0, false, new MountAim(90F, 0F));
            boolean turreted = kind == FlanModelKind.AA_GUN;
            for (int index = 0; index < rest.size() && index < turned.size() && !turreted; index++)
                turreted = rest.get(index).offset().distanceToSqr(turned.get(index).offset()) > 1.0E-6D;
            Optional<AABB> bounds = model instanceof IDriveableType driveable ? driveable.getBounds() : Optional.empty();
            double reach = bounds.map(box -> Math.max(Math.max(Math.abs(box.minX), Math.abs(box.maxX)), Math.max(Math.abs(box.minZ), Math.abs(box.maxZ)))).orElse(0D);
            Vec3 restMuzzles = middle(rest);
            return new Layout(model, kind, secondary, mounts, driver, turreted, rest.isEmpty() ? null : rest.get(0).offset(), reach, bounds.map(box -> box.maxY).orElse(0D), restMuzzles,
                restMuzzles == null ? null : pivot(model, secondary, turreted, restMuzzles));
        }

        /**
         * @return where a turret turns and elevates its gun: halfway between its muzzles at rest and turned half round, at the
         *         height halfway between them pitched straight up and straight down; the middle of the hull, at the
         *         muzzles' height, for a gun fixed to it
         */
        private static Vec3 pivot(IContentType model, boolean secondary, boolean turreted, Vec3 rest)
        {
            if (!turreted)
                return new Vec3(0D, rest.y, 0D);
            Vec3 behind = middle(FlansProjectiles.getMuzzles(model, secondary, 0, false, new MountAim(180F, 0F)));
            Vec3 up = middle(FlansProjectiles.getMuzzles(model, secondary, 0, false, new MountAim(0F, -90F)));
            Vec3 down = middle(FlansProjectiles.getMuzzles(model, secondary, 0, false, new MountAim(0F, 90F)));
            if (behind == null || up == null || down == null)
                return rest;
            return new Vec3((rest.x + behind.x) / 2D, (up.y + down.y) / 2D, (rest.z + behind.z) / 2D);
        }

        /** @return whether the model drives along its hull: a vehicle, or a ship a content pack filed as a plane */
        boolean drives()
        {
            return kind == FlanModelKind.VEHICLE || kind == FlanModelKind.PLANE && model instanceof IDriveableType driveable && driveable.isShip();
        }
    }

    public static NpcVehicleState of(EntityNPCInterface npc)
    {
        return ((NpcVehicleStateHolder) npc).wolffsmodnpcsVehicleState();
    }

    /** @return where the turret points, relative to the hull */
    public MountAim turretAim()
    {
        return new MountAim(turretYaw, turretPitch);
    }

    /** @return where a seat's own gun points; the rest direction for a seat that has none */
    public MountAim seatAim(int seat)
    {
        return seat >= 0 && seat < seatYaw.length ? new MountAim(seatYaw[seat], seatPitch[seat]) : MountAim.REST;
    }

    public int seatCount()
    {
        return seatYaw.length;
    }

    /** @return the tracked motion of the given entity, when it is the target the NPC currently follows */
    public Optional<TargetMotion> motionOf(Entity target)
    {
        return target.getId() == targetId ? Optional.of(motion) : Optional.empty();
    }

    /** @return the kind of model the NPC currently is, or null */
    @Nullable
    public FlanModelKind kind()
    {
        return active && layout != null ? layout.kind() : null;
    }

    /** @return whether the NPC is a model that drives along its hull: a vehicle, or a ship a content pack filed as a plane */
    public boolean drivesHull()
    {
        return active && layout != null && layout.drives();
    }

    /** Records a shot, so clients show the recoil. Server only. */
    public void fired(int firedBarrel)
    {
        shots = shots == Integer.MAX_VALUE ? 0 : shots + 1;
        barrel = Math.max(0, firedBarrel);
    }

    // Tick

    /** Runs once per tick on either side, after the NPC has moved. */
    public void tick(EntityNPCInterface npc)
    {
        Layout current = resolve(npc);
        if (current == null)
        {
            parts.tick(npc, null, 0F, MountAim.REST);
            deactivate(npc);
            return;
        }
        layout = current;
        active = true;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!hullKnown)
        {
            hullYaw = npc.yBodyRot;
            lastHullYaw = hullYaw;
            lastPosition = npc.position();
            hullKnown = true;
        }
        boolean held = holdsHull(current, options);
        if (!held)
            hullYaw = npc.yBodyRot;
        if (npc.level().isClientSide)
            clientTick(held);
        else
            serverTick(npc, current, options, held);
        measure(npc);
        if (!npc.level().isClientSide && (current.kind() == FlanModelKind.VEHICLE || current.kind() == FlanModelKind.MECHA))
            NpcRunOver.tick(npc, current.model(), groundSpeed, options);
        if (held)
        {
            // The body would otherwise swing toward the head while the vehicle stands still.
            npc.setYBodyRot(hullYaw);
        }
        int shape = (options.enabled(Feature.EYE_AT_MUZZLE) ? 1 : 0) + 31 * (System.identityHashCode(current.model()) + (current.secondary() ? 1 : 0));
        if (shape != shapeSignature)
        {
            shapeSignature = shape;
            npc.refreshDimensions();
        }
        // Last: the part boxes stand where the hull and turret have just been put.
        parts.tick(npc, NpcModels.flan(npc), hullYaw, turretAim());
    }

    private void deactivate(EntityNPCInterface npc)
    {
        if (!active)
            return;
        active = false;
        layout = null;
        hullKnown = false;
        aligned = true;
        gunAligned = true;
        hullAligned = true;
        bombing = false;
        bombPoint = null;
        attitudePitch = 0F;
        attitudeBank = 0F;
        targetAttitudePitch = 0F;
        targetAttitudeBank = 0F;
        speedFactor = 1F;
        climbRise = 0D;
        climbRun = 0D;
        turretYaw = 0F;
        turretPitch = 0F;
        seatYaw = NO_SEATS;
        seatPitch = NO_SEATS;
        seatLead = new Vec3[0];
        targetId = -1;
        motion.reset();
        solution = null;
        solvedFrom = null;
        reachablePitch = Float.NaN;
        sent = false;
        received = false;
        shapeSignature = Integer.MIN_VALUE;
        npc.refreshDimensions();
    }

    @Nullable
    private Layout resolve(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        FlanModelKind kind = FlanModelKind.of(definition);
        if (definition == null || kind == null)
            return null;
        boolean secondary = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.SECONDARY_BANK);
        if (layout != null && layout.model() == definition && layout.secondary() == secondary)
            return layout;
        Layout created = Layout.of(definition, kind, secondary);
        int seats = created.mounts().stream().mapToInt(WeaponMount::seat).max().orElse(-1) + 1;
        seatYaw = new float[seats];
        seatPitch = new float[seats];
        seatLead = new Vec3[seats];
        for (WeaponMount mount : created.mounts())
            seatYaw[mount.seat()] = mount.driver() ? 0F : mount.restYaw();
        targetSeatYaw = seatYaw.clone();
        targetSeatPitch = seatPitch.clone();
        turretYaw = created.driver() == null ? 0F : created.driver().restYaw();
        turretPitch = 0F;
        sent = false;
        return created;
    }

    /** A vehicle's or ship's hull follows its travel and an AA gun's base stays put; other models turn like any NPC. */
    private static boolean holdsHull(Layout layout, NpcWeaponOptions options)
    {
        return options.enabled(Feature.HULL_FACES_TRAVEL) && (layout.drives() || layout.kind() == FlanModelKind.AA_GUN);
    }

    private void measure(EntityNPCInterface npc)
    {
        Vec3 position = npc.position();
        Vec3 moved = lastPosition == null ? Vec3.ZERO : position.subtract(lastPosition);
        lastPosition = position;
        Vec3 forward = Vec3.directionFromRotation(0F, hullYaw);
        // A teleport is not travel.
        travel = moved.lengthSqr() > 64D ? 0F : (float) (moved.x * forward.x + moved.z * forward.z);
        groundSpeed = moved.lengthSqr() > 64D ? 0D : moved.horizontalDistance();
        // Smoothed, so that a single step up a block edge barely registers while a hillside does.
        climbRise = Mth.lerp(GRADE_SMOOTHING, climbRise, moved.lengthSqr() > 64D ? 0D : moved.y);
        climbRun = Mth.lerp(GRADE_SMOOTHING, climbRun, groundSpeed);
        turn = Mth.wrapDegrees(hullYaw - lastHullYaw);
        lastHullYaw = hullYaw;
    }

    // Server

    private void serverTick(EntityNPCInterface npc, Layout current, NpcWeaponOptions options, boolean held)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        LivingEntity target = npc.getTarget();
        boolean engaged = target != null && target.isAlive() && !npc.isKilled() && target.level() == npc.level();
        if (engaged)
        {
            if (target.getId() != targetId)
            {
                targetId = target.getId();
                motion.reset();
                solution = null;
            }
            motion.observe(target.position(), npc.level().getGameTime());
        }
        else if (targetId != -1)
        {
            targetId = -1;
            motion.reset();
            solution = null;
            solvedFrom = null;
        }
        Vec3 aim = engaged ? solve(npc, current, target, options) : null;
        attitude(npc, current, aim);
        // Relative to the hull as it is pitched and banked, which the guns are carried with.
        MountAim wanted = aim == null ? null : NpcModels.relative(npc, aim, hullYaw);
        float tolerance = tuning.get(Value.ALIGN_TOLERANCE);
        if (held && wanted != null && current.kind() == FlanModelKind.VEHICLE && options.enabled(Feature.HULL_ASSISTS_AIM) && !driving(npc))
            wanted = assistWithHull(npc, current, tuning, wanted, tolerance);
        aimTurret(current, options, tuning, wanted, tolerance);
        reachablePitch = wanted != null && !bombing && current.kind() == FlanModelKind.VEHICLE ? reachablePitch(current.driver(), current.turreted(), wanted, tolerance) : Float.NaN;
        gunAligned = aligned;
        bombPoint = null;
        // A bomb is let go when it would come down on the target, whatever the switches say: from anywhere else it misses.
        if (engaged && bombing)
            aligned = bombsAway(npc, current, target, options);
        aimSeats(npc, current, options, engaged ? target : null);
        sync(npc, held);
    }

    private static boolean driving(EntityNPCInterface npc)
    {
        return !npc.getNavigation().isDone() || npc.zza != 0F || npc.xxa != 0F || NpcVehicleState.of(npc).driving.getThrottle() != 0F;
    }

    /**
     * Turns a standing vehicle's hull toward a target its gun cannot reach: a gun fixed to the hull, or a turret with a
     * limited traverse.
     *
     * @return the wanted aim relative to the turned hull
     */
    private MountAim assistWithHull(EntityNPCInterface npc, Layout current, NpcTuning tuning, MountAim wanted, float tolerance)
    {
        WeaponMount driver = current.driver();
        float rest = current.turreted() && driver != null ? driver.restYaw() : 0F;
        boolean reachable = current.turreted() && (driver == null || MountMotion.error(driver.clamp(wanted), wanted) <= tolerance);
        if (reachable)
            return wanted;
        float before = hullYaw;
        hullYaw = MountMotion.turn(hullYaw, hullYaw + wanted.yaw() - rest, hullStep(npc, tuning));
        return new MountAim(Mth.wrapDegrees(wanted.yaw() - Mth.wrapDegrees(hullYaw - before)), wanted.pitch());
    }

    /**
     * Turns the turret toward the wanted aim, and tells whether the guns point there: the turret's, or those fixed to the
     * hull, which point where the hull and a plane's nose do, and a mecha's, which rise and fall with its arms. Rounds leave
     * along the barrels, so a gun waits until it points where they have to go, within the tolerance.
     */
    private void aimTurret(Layout current, NpcWeaponOptions options, NpcTuning tuning, @Nullable MountAim wanted, float tolerance)
    {
        if (current.turreted())
        {
            // A turret that has been shot off no longer turns.
            if (!options.enabled(Feature.TURRET_LOCKED) && !parts.effects().gunDisabled())
                turnTurret(current.driver(), tuning, wanted, options.enabled(Feature.GRADUAL_AIM));
        }
        else
        {
            turretYaw = 0F;
            turretPitch = current.kind() == FlanModelKind.MECHA ? armPitch(options, tuning, wanted) : 0F;
        }
        boolean check = options.enabled(Feature.FIRE_WHEN_ALIGNED) && wanted != null;
        aligned = !check || MountMotion.error(turretAim(), wanted) <= tolerance;
        hullAligned = !check || MountMotion.error(MountAim.REST, wanted) <= tolerance;
    }

    /**
     * Pitches and banks a plane in flight as it flies, and tips a hovering helicopter whose guns are fixed to its hull onto
     * its shot; any other model stays level.
     *
     * @param aim
     *            the world direction the shot has to leave in, or null without one
     */
    private void attitude(EntityNPCInterface npc, Layout current, @Nullable Vec3 aim)
    {
        NpcTuning.Flight type = current.kind() == FlanModelKind.PLANE ? NpcFlight.type(npc) : NpcTuning.Flight.NORMAL;
        if (type == NpcTuning.Flight.PLANE)
        {
            attitudePitch = flight.getPitch();
            attitudeBank = flight.getBank();
            return;
        }
        attitudePitch = type == NpcTuning.Flight.HELICOPTER && !current.turreted() && !npc.onGround() ? tip(aim) : 0F;
        attitudeBank = 0F;
    }

    /**
     * Pitches, as Minecraft counts them, between which the main weapon of a hovering helicopter can be laid.
     *
     * @param highest
     *            the steepest above the horizon, negative
     * @param lowest
     *            the steepest below it
     */
    public record ShotPitch(float highest, float lowest)
    {
        /** A weapon that points anywhere, as one fired from the NPC's eyes does. */
        static final ShotPitch FREE = new ShotPitch(-90F, 90F);
    }

    /**
     * @return how steeply the main weapon of a hovering helicopter reaches: as far as it tips its nose for guns fixed to its
     *         hull, its turret's elevation limits for a turret, and anywhere without a model
     */
    public ShotPitch hoverShotPitch()
    {
        Layout current = active ? layout : null;
        if (current == null)
            return ShotPitch.FREE;
        if (!current.turreted())
            return new ShotPitch(-MAX_HOVER_TIP, MAX_HOVER_TIP);
        WeaponMount driver = current.driver();
        return driver == null ? ShotPitch.FREE : new ShotPitch(driver.minimum().pitch(), driver.maximum().pitch());
    }

    /** @return the nose-down pitch a hovering helicopter tips toward this tick: onto the shot it has to make, or level without one */
    private float tip(@Nullable Vec3 aim)
    {
        float goal = aim == null ? 0F : Mth.clamp(MountMotion.relative(aim, 0F).pitch(), -MAX_HOVER_TIP, MAX_HOVER_TIP);
        return attitudePitch + Mth.clamp(goal - attitudePitch, -HOVER_TIP_STEP, HOVER_TIP_STEP);
    }

    /** A mecha has no turret, but its arms raise and lower with its aim, as far as its model lets them. */
    private float armPitch(NpcWeaponOptions options, NpcTuning tuning, @Nullable MountAim wanted)
    {
        if (options.enabled(Feature.TURRET_LOCKED))
            return turretPitch;
        float goal = wanted == null ? 0F : wanted.pitch();
        float step = options.enabled(Feature.GRADUAL_AIM) ? aimStep(tuning, Value.AIM_SPEED_PITCH, DEFAULT_AIM_STEP) : 360F;
        return turretPitch + Mth.clamp(goal - turretPitch, -step, step);
    }

    /** Turns the turret toward the wanted aim, or back to its rest direction without one. */
    private void turnTurret(@Nullable WeaponMount driver, NpcTuning tuning, @Nullable MountAim wanted, boolean gradual)
    {
        float modelYawStep = driver == null ? DEFAULT_AIM_STEP : driver.yawSpeed();
        float modelPitchStep = driver == null ? DEFAULT_AIM_STEP : driver.pitchSpeed();
        float yawStep = gradual ? aimStep(tuning, Value.AIM_SPEED_YAW, modelYawStep) : 360F;
        float pitchStep = gradual ? aimStep(tuning, Value.AIM_SPEED_PITCH, modelPitchStep) : 360F;
        float yaw = driver == null ? 0F : driver.restYaw();
        MountAim goal = wanted != null ? wanted : new MountAim(yaw, 0F);
        MountAim next = MountMotion.approach(turretAim(), goal, driver, yawStep, pitchStep);
        turretYaw = next.yaw();
        turretPitch = next.pitch();
    }

    /**
     * @param driver
     *            the seat whose limits a turret keeps to, or null for a turret that elevates freely
     * @param turreted
     *            whether the main weapon turns with a turret; otherwise it points straight along the hull
     * @param wanted
     *            where the main weapon has to point, relative to the hull
     * @return the pitch nearest to the wanted one that the main weapon reaches within the tolerance, while the wanted one
     *         lies beyond its elevation; NaN while it reaches it
     */
    static float reachablePitch(@Nullable WeaponMount driver, boolean turreted, MountAim wanted, float tolerance)
    {
        if (turreted && driver == null)
            return Float.NaN;
        float highest = (driver != null && turreted ? driver.minimum().pitch() : 0F) - tolerance;
        float lowest = (driver != null && turreted ? driver.maximum().pitch() : 0F) + tolerance;
        if (wanted.pitch() > lowest)
            return lowest;
        return wanted.pitch() < highest ? highest : Float.NaN;
    }

    private void aimSeats(EntityNPCInterface npc, Layout current, NpcWeaponOptions options, @Nullable LivingEntity target)
    {
        boolean track = target != null && options.enabled(Feature.PASSENGER_GUNS) && !options.enabled(Feature.TURRET_LOCKED);
        Vec3 direction = track ? aimPoint(target).subtract(npc.getEyePosition()) : Vec3.ZERO;
        for (WeaponMount mount : current.mounts())
        {
            int seat = mount.seat();
            // A manned gun is laid where its rounds have to go, led as its gunner works it out, or where a player gunner looks.
            Optional<Vec3> lead = seatLead(seat);
            MountAim next = seatAim(seat);
            if (mount.driver())
                // The driver's gun is the turret's.
                next = turretAim();
            else if (mount.gun().isPresent())
            {
                MountAim goal;
                if (lead.isPresent())
                    goal = seatRelative(npc, mount, lead.get());
                else
                    goal = track ? seatRelative(npc, mount, direction) : new MountAim(mount.restYaw(), 0F);
                next = MountMotion.approach(next, goal, mount, mount.yawSpeed(), mount.pitchSpeed());
            }
            seatYaw[seat] = next.yaw();
            seatPitch[seat] = next.pitch();
        }
    }

    /** @return the aim of a seat gun, relative to the hull or to the turret it stands on, that points along a world direction */
    private MountAim seatRelative(EntityNPCInterface npc, WeaponMount mount, Vec3 direction)
    {
        MountAim aim = NpcModels.relative(npc, direction, hullYaw);
        return mount.onTurret() ? new MountAim(Mth.wrapDegrees(aim.yaw() - turretYaw), aim.pitch()) : aim;
    }

    /**
     * @return where a manned seat gun's rounds have to go, as its gunner last worked it out, and forgets it: a gun nobody
     *         mans any more goes back to tracking the target plainly
     */
    private Optional<Vec3> seatLead(int seat)
    {
        if (seat < 0 || seat >= seatLead.length)
            return Optional.empty();
        Vec3 lead = seatLead[seat];
        seatLead[seat] = null;
        return Optional.ofNullable(lead);
    }

    /** Lays a manned seat gun, from the next tick on, for rounds that have to leave along a world direction. Server only. */
    public void leadSeat(int seat, Vec3 direction)
    {
        if (seat >= 0 && seat < seatLead.length)
            seatLead[seat] = direction;
    }

    /** @return whether a seat gun points along a world direction, within the NPC's tolerance */
    public boolean seatPoints(EntityNPCInterface npc, WeaponMount mount, Vec3 direction)
    {
        return MountMotion.error(seatAim(mount.seat()), seatRelative(npc, mount, direction)) <= NpcTuning.of(npc).get(Value.ALIGN_TOLERANCE);
    }

    /** @return the world direction a seat gun points along as the model is drawn now, which its rounds leave along */
    public Vec3 seatBarrelDirection(EntityNPCInterface npc, WeaponMount mount)
    {
        MountAim aim = seatAim(mount.seat());
        return NpcModels.direction(npc, mount.onTurret() ? new MountAim(turretYaw + aim.yaw(), aim.pitch()) : aim, hullYaw);
    }

    /**
     * @param fixed
     *            whether the weapon is fixed to the hull of a turreted model rather than turning with its turret
     * @return the world direction a weapon of the model's banks points along as it is drawn now, which its rounds leave
     *         along as a player's do: the turret's or AA gun's, a mecha's arms', or straight ahead along the hull and a
     *         plane's nose
     */
    public Vec3 barrelDirection(EntityNPCInterface npc, boolean fixed)
    {
        return NpcModels.direction(npc, fixed ? MountAim.REST : turretAim(), hullYaw);
    }

    /**
     * @param muzzles
     *            the muzzles of a trigger, selected with the turret at rest
     * @param turned
     *            the same muzzles selected with the turret as it is aimed now
     * @return for each muzzle, in order, whether it stays fixed to the hull while the turret turns; never for a model
     *         without a turret, or an AA gun, whose guns all point one way
     */
    public boolean[] fixedMuzzles(List<WeaponMuzzle> muzzles, List<WeaponMuzzle> turned)
    {
        boolean[] fixed = new boolean[muzzles.size()];
        Layout current = active ? layout : null;
        if (current == null || !current.turreted() || current.kind() == FlanModelKind.AA_GUN || turretAim().equals(MountAim.REST))
            return fixed;
        for (int index = 0; index < fixed.length && index < turned.size(); index++)
            fixed[index] = muzzles.get(index).offset().distanceToSqr(turned.get(index).offset()) <= TURNING_MUZZLE;
        return fixed;
    }

    /**
     * @param pitch
     *            whether to read the elevation speed rather than the traverse speed
     * @return degrees per tick the NPC's model turns its gun at when its own aim speed is left to the model: the driver
     *         seat's speed, or the default for a model whose seat states none and for a mecha's arms
     */
    public static float modelAimStep(EntityNPCInterface npc, boolean pitch)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        if (definition == null || FlanModelKind.of(definition) == FlanModelKind.MECHA)
            return DEFAULT_AIM_STEP;
        WeaponMount driver = FlansEntityTypes.getMounts(definition).stream().filter(WeaponMount::driver).findFirst().orElse(null);
        if (driver == null)
            return DEFAULT_AIM_STEP;
        return pitch ? driver.pitchSpeed() : driver.yawSpeed();
    }

    private static float aimStep(NpcTuning tuning, Value value, float modelStep)
    {
        int perSecond = tuning.get(value);
        return perSecond > 0 ? perSecond / 20F : modelStep;
    }

    private static float hullStep(EntityNPCInterface npc, NpcTuning tuning)
    {
        int perSecond = tuning.get(Value.HULL_TURN_RATE);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        return perSecond > 0 ? perSecond / 20F : (float) NpcDriving.handling(npc, options).turnRate().orElse(DEFAULT_HULL_STEP * 20D) / 20F;
    }

    /** @return the world direction a shot at the target has to leave in, from where the rounds leave, refreshed every few ticks */
    private Vec3 solve(EntityNPCInterface npc, Layout current, LivingEntity target, NpcWeaponOptions options)
    {
        if (solution != null && npc.tickCount - solvedAt < SOLVE_INTERVAL && npc.tickCount >= solvedAt)
            return solution;
        solvedAt = npc.tickCount;
        Vec3 origin = fireOrigin(npc, current, options);
        solvedFrom = origin;
        Vec3 displacement = aimPoint(target).subtract(origin);
        refreshBallistics(npc);
        if (Double.isNaN(shotSpeed))
            solution = displacement.normalize();
        else
        {
            boolean arc = options.enabled(Feature.BALLISTIC_AIM);
            solution = lead(displacement, target, options, shotSpeed, arc ? shotGravity : 0D, arc ? shotDrag : 1D, indirect(npc, target));
        }
        return solution;
    }

    /**
     * @return where the main weapon's rounds leave from: the middle of its muzzles, carried with the turret as the NPC fires
     *         them, or the NPC's eyes when it fires from there
     */
    private Vec3 fireOrigin(EntityNPCInterface npc, Layout current, NpcWeaponOptions options)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || !options.enabled(Feature.MODEL_MUZZLES))
            return npc.getEyePosition();
        MountAim aim = options.enabled(Feature.TURRET_MUZZLES) ? turretAim() : MountAim.REST;
        Vec3 middle = middle(FlansProjectiles.getMuzzles(current.model(), current.secondary(), 0, false, aim));
        return middle == null ? npc.getEyePosition() : NpcModels.world(npc, model, middle, hullYaw);
    }

    /** @return the middle of the muzzles, in their own frame; null without any */
    @Nullable
    private static Vec3 middle(List<WeaponMuzzle> muzzles)
    {
        if (muzzles.isEmpty())
            return null;
        Vec3 sum = Vec3.ZERO;
        for (WeaponMuzzle muzzle : muzzles)
            sum = sum.add(muzzle.offset());
        return sum.scale(1D / muzzles.size());
    }

    /** Whether Custom NPCs' ranged AI fires at the target along the high arc, as it decides when it pulls the trigger. */
    static boolean indirect(EntityNPCInterface npc, LivingEntity target)
    {
        double range = npc.stats.ranged.getRange();
        return switch (npc.stats.ranged.getFireType())
        {
            case 1 -> npc.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) > range * range / 2D;
            case 2 -> !npc.getSensing().hasLineOfSight(target);
            default -> false;
        };
    }

    /** The launch direction against a target, led the way the NPC's switches ask for. */
    public Vec3 lead(Vec3 displacement, LivingEntity target, NpcWeaponOptions options, double speed, double gravity, double drag, boolean highArc)
    {
        if (!options.enabled(Feature.LEAD_TARGET))
            return ProjectileAim.direction(displacement, Vec3.ZERO, speed, gravity, drag, highArc);
        Optional<TargetMotion> tracked = options.enabled(Feature.ADVANCED_LEAD) ? motionOf(target) : Optional.empty();
        if (tracked.isPresent())
            return ProjectileAim.solve(displacement, tracked.get(), speed, gravity, drag, highArc).direction();
        return ProjectileAim.direction(displacement, target.getDeltaMovement(), speed, gravity, drag, highArc);
    }

    private void refreshBallistics(EntityNPCInterface npc)
    {
        if (npc.tickCount - ballisticsAt < BALLISTICS_INTERVAL && npc.tickCount >= ballisticsAt)
            return;
        ballisticsAt = npc.tickCount;
        Optional<ProjectileShot> shot = NpcRangedAttack.preview(npc);
        shotSpeed = shot.map(value -> (double) value.speed()).orElse(Double.NaN);
        shotGravity = shot.map(ProjectileShot::gravity).orElse(0D);
        shotDrag = shot.map(ProjectileShot::drag).orElse(1D);
        bombing = shot.map(ProjectileShot::dropped).orElse(false);
    }

    /** @return the world direction, from where its rounds leave, a shot at its target leaves along; empty while it has none */
    public Optional<Vec3> aimDirection()
    {
        return Optional.ofNullable(solution);
    }

    /** @return where the main weapon's rounds leave from, as of the last shot at its target worked out; empty while it has none */
    public Optional<Vec3> fireOrigin()
    {
        return Optional.ofNullable(solvedFrom);
    }

    /**
     * @return the pitch, relative to the hull and positive downwards, nearest to its shot that the main weapon of a vehicle
     *         reaches, while that shot lies beyond its elevation; empty while it reaches it or has no shot to make
     */
    public Optional<Float> elevationLimit()
    {
        return Float.isNaN(reachablePitch) ? Optional.empty() : Optional.of(reachablePitch);
    }

    /**
     * The point the main weapon turns about, in the world, and how far across the ground its rounds leave from it. Its rounds
     * leave from its muzzles, so nothing nearer the pivot than they are lies in front of the barrel, wherever it points.
     *
     * @param pivot
     *            where a turret turns and elevates its gun, or the middle of the hull at the height of a gun fixed to it
     * @param reach
     *            blocks across the ground from the pivot to the muzzles
     * @param elevates
     *            whether the gun elevates about the pivot; one fixed to the hull lies level from its muzzles
     */
    public record GunReach(Vec3 pivot, double reach, boolean elevates)
    {}

    /** @return the point the main weapon turns about and its reach; empty while its rounds do not leave from muzzles that turn with it */
    public Optional<GunReach> gunReach(EntityNPCInterface npc)
    {
        Layout current = active ? layout : null;
        FlanModelEntity model = NpcModels.flan(npc);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (current == null || model == null || current.barrel() == null || current.pivot() == null || !options.enabled(Feature.MODEL_MUZZLES)
            || current.turreted() && !options.enabled(Feature.TURRET_MUZZLES))
            return Optional.empty();
        Vec3 pivot = NpcModels.world(npc, model, current.pivot(), hullYaw);
        Vec3 muzzles = NpcModels.world(npc, model, current.barrel(), hullYaw);
        return Optional.of(new GunReach(pivot, Math.sqrt(Mth.square(muzzles.x - pivot.x) + Mth.square(muzzles.z - pivot.z)), current.turreted()));
    }

    /** @return where a bomb let go now would have to come down to meet the target; empty while the NPC bombs nothing */
    public Optional<Vec3> bombPoint()
    {
        return Optional.ofNullable(bombPoint);
    }

    /**
     * Whether a bomb let go now, from the model's first muzzle at the NPC's velocity, would come down within reach of the
     * target. A moving target is met where it will be when the bomb lands, as far as the NPC leads its targets.
     */
    private boolean bombsAway(EntityNPCInterface npc, Layout current, LivingEntity target, NpcWeaponOptions options)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        Vec3 origin = model != null && current.muzzle() != null ? NpcModels.world(npc, model, current.muzzle(), hullYaw) : npc.position();
        WeaponAim.Fall fall = WeaponAim.fall(origin, npc.getDeltaMovement(), shotGravity, shotDrag, target.getY());
        Vec3 meeting = target.position();
        if (options.enabled(Feature.LEAD_TARGET))
        {
            Vec3 velocity = options.enabled(Feature.ADVANCED_LEAD) ? motionOf(target).map(TargetMotion::velocity).orElse(target.getDeltaMovement()) : target.getDeltaMovement();
            meeting = meeting.add(velocity.x * fall.ticks(), 0D, velocity.z * fall.ticks());
        }
        bombPoint = meeting;
        if (origin.y < target.getY() + MIN_BOMB_HEIGHT)
            return false;
        double x = fall.impact().x - meeting.x;
        double z = fall.impact().z - meeting.z;
        return x * x + z * z <= BOMB_RADIUS * BOMB_RADIUS;
    }

    /** The point of a target a shot is aimed at: the middle of a large body, the chest of a smaller one. */
    public static Vec3 aimPoint(LivingEntity target)
    {
        float height = target.getBbHeight();
        return target.position().add(0D, height * (height > 2.5F ? 0.5D : 0.6D), 0D);
    }

    // Movement

    /**
     * @return the share of its propulsion a ground vehicle keeps on the grade it drives up, as its definition's real-world
     *         profile has the vehicle bog down; 1 for anything else, downhill, or with the switch off. Server only.
     */
    public float climbFactor(EntityNPCInterface npc)
    {
        if (!active || layout == null || layout.kind() != FlanModelKind.VEHICLE || !npc.onGround() || npc.isInWater())
            return 1F;
        double grade = grade(climbRise, climbRun);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (grade <= 0D || !options.enabled(Feature.SLOPE_PHYSICS))
            return 1F;
        return NpcDriving.propulsion(npc, options).map(propulsion -> (float) Mth.clamp(propulsion.climbFactor(grade), 0D, 1D)).orElse(1F);
    }

    /**
     * @param rise
     *            blocks risen per tick
     * @param run
     *            blocks travelled across the ground per tick
     * @return degrees of the grade they make; 0 while barely moving
     */
    static double grade(double rise, double run)
    {
        return run < MIN_GRADE_RUN ? 0D : Math.toDegrees(Math.atan2(rise, run));
    }

    /**
     * Turns a driving vehicle's hull toward where it wants to go, makes it drive where the hull points, and slows it
     * down while the two differ: with the driving controller as a vehicle would, without it by plain steering.
     * Server only, before the NPC moves.
     *
     * @param travel
     *            the movement input the NPC was about to use
     * @return the movement input it uses instead
     */
    public Vec3 beforeTravel(EntityNPCInterface npc, Vec3 travel)
    {
        speedFactor = 1F;
        if (!active || layout == null || !layout.drives() || !hullKnown || npc.isKilled() || npc.isVehicle() && npc.ais.mountControl)
        {
            driving.reset();
            return travel;
        }
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!options.enabled(Feature.HULL_FACES_TRAVEL))
        {
            driving.reset();
            return travel;
        }
        NpcTuning tuning = NpcTuning.of(npc);
        if (options.enabled(Feature.DRIVING))
        {
            NpcDriving.Command command = driving.travel(npc, hullYaw, Math.abs(this.travel), tuning, options);
            hullYaw = command.hullYaw();
            npc.setYRot(hullYaw);
            if (NpcMobility.afloat(npc))
                return sail(npc, command.throttle(), travel);
            return new Vec3(0D, travel.y, NpcDriving.input(npc, command.throttle()));
        }
        driving.reset();
        if (npc.zza == 0F && npc.xxa == 0F)
            return travel;
        float desired = npc.getYRot();
        hullYaw = MountMotion.turn(hullYaw, desired, hullStep(npc, tuning));
        if (options.enabled(Feature.MISALIGNED_THROTTLE))
            speedFactor = MountMotion.throttle(desired - hullYaw, tuning.get(Value.THROTTLE_FLOOR) / 100F);
        npc.setYRot(hullYaw);
        // Nothing pushes a hull sideways through the water.
        if (NpcMobility.afloat(npc))
            return new Vec3(0D, travel.y, travel.z * speedFactor);
        return speedFactor == 1F ? travel : new Vec3(travel.x * speedFactor, travel.y, travel.z * speedFactor);
    }

    /**
     * Moves a hull in water straight along its heading at the commanded share of its water speed, as the vehicle itself
     * moves there, instead of letting the swimming of a mob carry it on and sideways through every turn.
     *
     * @return the movement input left for the swimming, which then only carries the hull up or down
     */
    private Vec3 sail(EntityNPCInterface npc, float throttle, Vec3 travel)
    {
        double speed = throttle * NpcMobility.topSpeed(npc) * parts.effects().mobility();
        Vec3 forward = Vec3.directionFromRotation(0F, hullYaw);
        npc.setDeltaMovement(forward.x * speed, npc.getDeltaMovement().y, forward.z * speed);
        return new Vec3(0D, travel.y, 0D);
    }

    // Shape

    /**
     * @return the eye height that puts the NPC's eyes at its first muzzle, so that it looks and checks its line of
     *         sight from the gun; empty when the model or the switch does not ask for it
     */
    public Optional<Float> eyeHeight(EntityNPCInterface npc, float bodyHeight)
    {
        // Asked for while the entity is still being constructed, before it has a model or settings to look at.
        Layout current = layout;
        if (!active || current == null || current.muzzle() == null || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.EYE_AT_MUZZLE))
            return Optional.empty();
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null)
            return Optional.empty();
        float modelHeight = model.getModelType() == null ? 0F : model.getModelType().getShape().modelHeight();
        float height = (float) ((current.muzzle().y + modelHeight) * NpcModels.scale(npc));
        return Optional.of(Mth.clamp(height, 0.2F, Math.max(0.2F, bodyHeight * 1.5F)));
    }

    // Seats

    /**
     * @return the seats riders and crew sit in, in seat order, the driver's first; none for an AA gun or an NPC that shows
     *         no driveable model. Resolves the model when the NPC has not ticked yet, so a vehicle can be crewed as soon as
     *         it is made.
     */
    public List<WeaponMount> crewSeats(EntityNPCInterface npc)
    {
        Layout current = active ? layout : resolve(npc);
        if (current == null)
            return List.of();
        if (!active)
        {
            layout = current;
            active = true;
        }
        return current.kind() == FlanModelKind.AA_GUN ? List.of() : current.mounts();
    }

    /**
     * @return where the occupant of a seat sits, relative to the NPC's feet, as the hull, turret and the seat's own gun
     *         stand now
     */
    public Optional<Vec3> seatOffset(EntityNPCInterface npc, WeaponMount seat)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return !active || model == null ? Optional.empty() : Optional.of(NpcModels.local(npc, model, seat.positionAt(turretYaw, seatAim(seat.seat()).yaw()), hullYaw));
    }

    /**
     * @return the yaw the occupant of a seat faces: along its gun, whose aim is relative to the turret for a seat on it, or
     *         ahead of the hull, or of the turret it sits on
     */
    public float seatFacing(WeaponMount seat)
    {
        float frame = seatFrameYaw(seat);
        if (!seat.driver() && seat.gun().isPresent())
            return frame + seatAim(seat.seat()).yaw();
        return frame;
    }

    /** @return the yaw ahead of the hull, or of the turret for a seat on it */
    public float seatFrameYaw(WeaponMount seat)
    {
        return seat.onTurret() ? hullYaw + turretYaw : hullYaw;
    }

    /**
     * @return the yaw the body of a seat's occupant keeps, as a player's does in that seat: a gunner turns with its gun, in
     *         an aircraft only once the gun turns further than a torso planted in the cockpit lets it look; anyone else
     *         stays ahead of the hull, or of the turret it sits on
     */
    public float seatBodyYaw(WeaponMount seat)
    {
        float frame = seatFrameYaw(seat);
        if (seat.driver() || seat.gun().isEmpty())
            return frame;
        float aim = Mth.wrapDegrees(seatAim(seat.seat()).yaw());
        Layout current = layout;
        if (current != null && current.kind() == FlanModelKind.PLANE)
            return frame + aim - Mth.clamp(aim, -COCKPIT_TORSO_FREE, COCKPIT_TORSO_FREE);
        return frame + aim;
    }

    /** @return blocks from the NPC's middle to the farthest side of its model, across the ground; 0 without a model */
    public double reach(EntityNPCInterface npc)
    {
        Layout current = layout;
        return active && current != null ? current.reach() * NpcModels.scale(npc) : 0D;
    }

    /**
     * @return the box the client culls the NPC by: its hitbox widened to the whole model, which may reach far beyond a
     *         hitbox that is never wider than a few blocks
     */
    public Optional<AABB> cullingBox(EntityNPCInterface npc)
    {
        Layout current = layout;
        if (!active || current == null || current.reach() <= 0D || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.MODEL_CULLING))
            return Optional.empty();
        double scale = NpcModels.scale(npc);
        double reach = current.reach() * scale;
        AABB box = npc.getBoundingBox();
        double top = Math.max(box.maxY, npc.getY() + (current.top() + 1D) * scale);
        return Optional.of(
            new AABB(Math.min(box.minX, npc.getX() - reach), box.minY, Math.min(box.minZ, npc.getZ() - reach), Math.max(box.maxX, npc.getX() + reach), top, Math.max(box.maxZ, npc.getZ() + reach)));
    }

    // Line of fire

    /**
     * @return whether the NPC sees its target only through blocks it must not fire through, so that it holds its fire
     *         and keeps closing in. Checked every few ticks.
     */
    public boolean holdsFire(EntityNPCInterface npc, LivingEntity target)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        NpcTuning.Sight sight = tuning.sight();
        if (sight == NpcTuning.Sight.NORMAL)
            return false;
        int fireLimit = sight == NpcTuning.Sight.SEE_AND_FIRE_THROUGH ? tuning.get(Value.FIRE_LIMIT) : 0;
        if (fireLimit >= tuning.get(Value.VISION_LIMIT))
            return false;
        if (npc.tickCount - fireCheckedAt >= 4 || npc.tickCount < fireCheckedAt)
        {
            fireCheckedAt = npc.tickCount;
            fireClear = NpcSight.clear(npc, target, fireLimit);
        }
        return !fireClear;
    }

    // Synchronisation

    private void sync(EntityNPCInterface npc, boolean held)
    {
        int signature = Arrays.hashCode(
            // A hull the model does not hold is the body yaw of the NPC, which clients already know.
            new int[]{held ? Math.round(hullYaw * 2F) : 0, Math.round(turretYaw * 4F), Math.round(turretPitch * 4F), Arrays.hashCode(quantise(seatYaw)), Arrays.hashCode(quantise(seatPitch)), shots,
                barrel, Math.round(attitudePitch * 2F), Math.round(attitudeBank * 2F)});
        if (sent && signature == sentSignature || npc.tickCount % SYNC_INTERVAL != 0)
            return;
        sent = true;
        sentSignature = signature;
        NpcNetwork.sendToTracking(npc, packet(npc));
    }

    private static int[] quantise(float[] angles)
    {
        int[] steps = new int[angles.length];
        for (int index = 0; index < angles.length; index++)
            steps[index] = Math.round(angles[index] * 2F);
        return steps;
    }

    /** @return the state as sent to a client that starts tracking the NPC */
    public VehicleStatePacket packet(EntityNPCInterface npc)
    {
        return new VehicleStatePacket(npc.getId(), hullYaw, turretYaw, turretPitch, seatYaw.clone(), seatPitch.clone(), shots, barrel, attitudePitch, attitudeBank);
    }

    /**
     * @return whether dynamic landing gear is down: on the ground, in water or close above the ground, and up once the
     *         aircraft has climbed clear of it. Either side.
     */
    public boolean dynamicGearDown(EntityNPCInterface npc)
    {
        if (npc.onGround() || npc.isInWater())
            gearDown = true;
        else
        {
            double height = npc.getY() - npc.level().getHeight(Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ());
            // Gear that is down stays down until the aircraft is well clear, so that it does not flicker at one height.
            gearDown = height < (gearDown ? GEAR_UP_HEIGHT : GEAR_DOWN_HEIGHT) * Math.max(1F, NpcModels.scale(npc));
        }
        return gearDown;
    }

    /** Takes the server's state. Client only. */
    public void receive(VehicleStatePacket packet)
    {
        if (received)
        {
            // While they turn, the server sends a state every sync interval.
            hullYawRate = Math.abs(Mth.wrapDegrees(packet.hullYaw() - targetHullYaw)) / SYNC_INTERVAL;
            turretYawRate = Math.abs(Mth.wrapDegrees(packet.turretYaw() - targetTurretYaw)) / SYNC_INTERVAL;
        }
        else
            turretYaw = packet.turretYaw();
        targetHullYaw = packet.hullYaw();
        targetTurretYaw = packet.turretYaw();
        targetTurretPitch = packet.turretPitch();
        targetSeatYaw = packet.seatYaw();
        targetSeatPitch = packet.seatPitch();
        shots = packet.shots();
        barrel = packet.barrel();
        targetAttitudePitch = packet.attitudePitch();
        targetAttitudeBank = packet.attitudeBank();
        lerpSteps = received ? LERP_STEPS : 1;
        received = true;
    }

    /** @return the attitude's nose-down pitch between the last tick and this one, for drawing. Client only. */
    public float attitudePitch(float partialTick)
    {
        return Mth.lerp(partialTick, previousAttitudePitch, attitudePitch);
    }

    /** @return the attitude's bank to the right between the last tick and this one, for drawing. Client only. */
    public float attitudeBank(float partialTick)
    {
        return Mth.lerp(partialTick, previousAttitudeBank, attitudeBank);
    }

    private void clientTick(boolean held)
    {
        previousAttitudePitch = attitudePitch;
        previousAttitudeBank = attitudeBank;
        if (!received)
            return;
        // States arrive every few ticks and unevenly, so easing onto each one in turn would turn the hull and turret in fits
        // and starts, which whoever stands on them feels as a juddering view. They follow at the rate the server turns them.
        if (held)
            hullYaw = Mth.wrapDegrees(follow(hullYaw, targetHullYaw, hullYawRate));
        turretYaw = follow(turretYaw, targetTurretYaw, turretYawRate);
        if (lerpSteps <= 0)
            return;
        attitudePitch += (targetAttitudePitch - attitudePitch) / lerpSteps;
        attitudeBank += (targetAttitudeBank - attitudeBank) / lerpSteps;
        turretPitch += (targetTurretPitch - turretPitch) / lerpSteps;
        for (int seat = 0; seat < seatYaw.length && seat < targetSeatYaw.length && seat < targetSeatPitch.length; seat++)
        {
            seatYaw[seat] += Mth.wrapDegrees(targetSeatYaw[seat] - seatYaw[seat]) / lerpSteps;
            seatPitch[seat] += (targetSeatPitch[seat] - seatPitch[seat]) / lerpSteps;
        }
        lerpSteps--;
    }

    /**
     * @return the yaw one tick on toward the target: at the rate it last turned at, and faster only to catch up once it lags
     *         further behind than the next states would take it
     */
    static float follow(float yaw, float target, float rate)
    {
        float remaining = Mth.wrapDegrees(target - yaw);
        float step = Math.max(rate, Math.abs(remaining) / LERP_STEPS);
        return yaw + Mth.clamp(remaining, -step, step);
    }
}
