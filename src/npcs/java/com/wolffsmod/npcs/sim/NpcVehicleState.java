package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
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
    /** A gun fixed to the hull only has to point roughly at the target: the shot itself is aimed exactly. */
    private static final float FIXED_GUN_TOLERANCE = 12F;
    private static final double MOVING_DISTANCE = 0.003D;
    private static final float[] NO_SEATS = new float[0];

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
    /** Whether the gun points where the shot has to go. Always true when nothing has to line up. */
    @Getter
    private boolean aligned = true;
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

    // Server
    private final TargetMotion motion = new TargetMotion();
    private int targetId = -1;
    @Nullable
    private Vec3 solution;
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

    // Client
    private float targetHullYaw;
    private float targetTurretYaw;
    private float targetTurretPitch;
    private float[] targetSeatYaw = NO_SEATS;
    private float[] targetSeatPitch = NO_SEATS;
    private int lerpSteps;
    private boolean received;

    /** What the selected model offers, resolved once per model and weapon bank. */
    private record Layout(IContentType model, FlanModelKind kind, boolean secondary, List<WeaponMount> mounts, @Nullable WeaponMount driver, boolean turreted, @Nullable Vec3 muzzle, double reach,
        double top)
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
            return new Layout(model, kind, secondary, mounts, driver, turreted, rest.isEmpty() ? null : rest.get(0).offset(), reach, bounds.map(box -> box.maxY).orElse(0D));
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
    }

    private void deactivate(EntityNPCInterface npc)
    {
        if (!active)
            return;
        active = false;
        layout = null;
        hullKnown = false;
        aligned = true;
        speedFactor = 1F;
        turretYaw = 0F;
        turretPitch = 0F;
        seatYaw = NO_SEATS;
        seatPitch = NO_SEATS;
        targetId = -1;
        motion.reset();
        solution = null;
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
        for (WeaponMount mount : created.mounts())
            seatYaw[mount.seat()] = mount.driver() ? 0F : mount.restYaw();
        targetSeatYaw = seatYaw.clone();
        targetSeatPitch = seatPitch.clone();
        turretYaw = created.driver() == null ? 0F : created.driver().restYaw();
        turretPitch = 0F;
        sent = false;
        return created;
    }

    /** A vehicle's hull follows its travel and an AA gun's base stays put; other models turn like any NPC. */
    private static boolean holdsHull(Layout layout, NpcWeaponOptions options)
    {
        return options.enabled(Feature.HULL_FACES_TRAVEL) && (layout.kind() == FlanModelKind.VEHICLE || layout.kind() == FlanModelKind.AA_GUN);
    }

    private void measure(EntityNPCInterface npc)
    {
        Vec3 position = npc.position();
        Vec3 moved = lastPosition == null ? Vec3.ZERO : position.subtract(lastPosition);
        lastPosition = position;
        Vec3 forward = Vec3.directionFromRotation(0F, hullYaw);
        // A teleport is not travel.
        travel = moved.lengthSqr() > 64D ? 0F : (float) (moved.x * forward.x + moved.z * forward.z);
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
        }
        MountAim wanted = engaged ? MountMotion.relative(solve(npc, target, options), hullYaw) : null;
        float tolerance = tuning.get(Value.ALIGN_TOLERANCE);
        if (held && wanted != null && current.kind() == FlanModelKind.VEHICLE && options.enabled(Feature.HULL_ASSISTS_AIM) && !driving(npc))
            wanted = assistWithHull(current, tuning, wanted, tolerance);
        aimTurret(current, options, tuning, wanted, tolerance, held);
        aimSeats(npc, current, options, engaged ? target : null);
        sync(npc, held);
    }

    private static boolean driving(EntityNPCInterface npc)
    {
        return !npc.getNavigation().isDone() || npc.zza != 0F || npc.xxa != 0F;
    }

    /**
     * Turns a standing vehicle's hull toward a target its gun cannot reach: a gun fixed to the hull, or a turret with a
     * limited traverse.
     *
     * @return the wanted aim relative to the turned hull
     */
    private MountAim assistWithHull(Layout current, NpcTuning tuning, MountAim wanted, float tolerance)
    {
        WeaponMount driver = current.driver();
        float rest = current.turreted() && driver != null ? driver.restYaw() : 0F;
        boolean reachable = current.turreted() && (driver == null || MountMotion.error(driver.clamp(wanted), wanted) <= tolerance);
        if (reachable)
            return wanted;
        float before = hullYaw;
        hullYaw = MountMotion.turn(hullYaw, hullYaw + wanted.yaw() - rest, hullStep(tuning));
        return new MountAim(Mth.wrapDegrees(wanted.yaw() - Mth.wrapDegrees(hullYaw - before)), wanted.pitch());
    }

    private void aimTurret(Layout current, NpcWeaponOptions options, NpcTuning tuning, @Nullable MountAim wanted, float tolerance, boolean held)
    {
        if (current.turreted())
        {
            if (!options.enabled(Feature.TURRET_LOCKED))
                turnTurret(current.driver(), tuning, wanted, options.enabled(Feature.GRADUAL_AIM));
            aligned = wanted == null || MountMotion.error(turretAim(), wanted) <= tolerance;
        }
        else
        {
            turretYaw = 0F;
            turretPitch = 0F;
            // A fixed gun is on target when the hull is; a model that turns like any NPC faces its target anyway.
            aligned = wanted == null || !held || Math.abs(wanted.yaw()) <= Math.max(tolerance, FIXED_GUN_TOLERANCE);
        }
        if (!options.enabled(Feature.FIRE_WHEN_ALIGNED))
            aligned = true;
    }

    /** Turns the turret toward the wanted aim, or back to its rest direction without one. */
    private void turnTurret(@Nullable WeaponMount driver, NpcTuning tuning, @Nullable MountAim wanted, boolean gradual)
    {
        float modelYawStep = driver == null ? DEFAULT_AIM_STEP : driver.yawSpeed();
        float modelPitchStep = driver == null ? DEFAULT_AIM_STEP : driver.pitchSpeed();
        float yawStep = gradual ? aimStep(tuning, Value.AIM_SPEED_YAW, modelYawStep) : 360F;
        float pitchStep = gradual ? aimStep(tuning, Value.AIM_SPEED_PITCH, modelPitchStep) : 360F;
        MountAim goal = wanted != null ? wanted : new MountAim(driver == null ? 0F : driver.restYaw(), 0F);
        MountAim next = MountMotion.approach(turretAim(), goal, driver, yawStep, pitchStep);
        turretYaw = next.yaw();
        turretPitch = next.pitch();
    }

    private void aimSeats(EntityNPCInterface npc, Layout current, NpcWeaponOptions options, @Nullable LivingEntity target)
    {
        boolean track = target != null && options.enabled(Feature.PASSENGER_GUNS) && !options.enabled(Feature.TURRET_LOCKED);
        Vec3 direction = track ? aimPoint(target).subtract(npc.getEyePosition()) : Vec3.ZERO;
        for (WeaponMount mount : current.mounts())
        {
            int seat = mount.seat();
            MountAim next = seatAim(seat);
            if (mount.driver())
                // The driver's gun is the turret's.
                next = turretAim();
            else if (mount.gun().isPresent())
            {
                MountAim goal = track ? MountMotion.relative(direction, hullYaw + (mount.onTurret() ? turretYaw : 0F)) : new MountAim(mount.restYaw(), 0F);
                next = MountMotion.approach(next, goal, mount, mount.yawSpeed(), mount.pitchSpeed());
            }
            seatYaw[seat] = next.yaw();
            seatPitch[seat] = next.pitch();
        }
    }

    private static float aimStep(NpcTuning tuning, Value value, float modelStep)
    {
        int perSecond = tuning.get(value);
        return perSecond > 0 ? perSecond / 20F : modelStep;
    }

    private static float hullStep(NpcTuning tuning)
    {
        int perSecond = tuning.get(Value.HULL_TURN_RATE);
        return perSecond > 0 ? perSecond / 20F : DEFAULT_HULL_STEP;
    }

    /** @return the world direction a shot at the target has to leave in, refreshed every few ticks */
    private Vec3 solve(EntityNPCInterface npc, LivingEntity target, NpcWeaponOptions options)
    {
        if (solution != null && npc.tickCount - solvedAt < SOLVE_INTERVAL && npc.tickCount >= solvedAt)
            return solution;
        solvedAt = npc.tickCount;
        Vec3 displacement = aimPoint(target).subtract(npc.getEyePosition());
        refreshBallistics(npc);
        if (Double.isNaN(shotSpeed))
            solution = displacement.normalize();
        else
        {
            boolean arc = options.enabled(Feature.BALLISTIC_AIM);
            solution = lead(displacement, target, options, shotSpeed, arc ? shotGravity : 0D, arc ? shotDrag : 1D, false);
        }
        return solution;
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
    }

    /** The point of a target a shot is aimed at: the middle of a large body, the chest of a smaller one. */
    public static Vec3 aimPoint(LivingEntity target)
    {
        float height = target.getBbHeight();
        return target.position().add(0D, height * (height > 2.5F ? 0.5D : 0.6D), 0D);
    }

    // Movement

    /**
     * Turns a driving vehicle's hull toward where it wants to go, makes it drive where the hull points, and slows it
     * down while the two differ. Server only, before the NPC moves.
     */
    public void beforeTravel(EntityNPCInterface npc)
    {
        speedFactor = 1F;
        if (!active || layout == null || layout.kind() != FlanModelKind.VEHICLE || !hullKnown || npc.isKilled() || npc.isVehicle() && npc.ais.mountControl)
            return;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!options.enabled(Feature.HULL_FACES_TRAVEL) || npc.zza == 0F && npc.xxa == 0F)
            return;
        NpcTuning tuning = NpcTuning.of(npc);
        float desired = npc.getYRot();
        hullYaw = MountMotion.turn(hullYaw, desired, hullStep(tuning));
        if (options.enabled(Feature.MISALIGNED_THROTTLE))
            speedFactor = MountMotion.throttle(desired - hullYaw, tuning.get(Value.THROTTLE_FLOOR) / 100F);
        npc.setYRot(hullYaw);
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

    /** @return where the rider of the driver's seat sits, relative to the NPC's feet, when riders sit in the seat */
    public Optional<Vec3> riderOffset(EntityNPCInterface npc)
    {
        Layout current = layout;
        if (!active || current == null || current.driver() == null || current.kind() == FlanModelKind.AA_GUN
            || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.RIDER_AT_SEAT))
            return Optional.empty();
        FlanModelEntity model = NpcModels.flan(npc);
        return model == null ? Optional.empty() : Optional.of(NpcModels.local(npc, model, current.driver().positionAt(turretYaw), hullYaw));
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
                barrel});
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
        return new VehicleStatePacket(npc.getId(), hullYaw, turretYaw, turretPitch, seatYaw.clone(), seatPitch.clone(), shots, barrel);
    }

    /** Takes the server's state. Client only. */
    public void receive(VehicleStatePacket packet)
    {
        targetHullYaw = packet.hullYaw();
        targetTurretYaw = packet.turretYaw();
        targetTurretPitch = packet.turretPitch();
        targetSeatYaw = packet.seatYaw();
        targetSeatPitch = packet.seatPitch();
        shots = packet.shots();
        barrel = packet.barrel();
        lerpSteps = received ? LERP_STEPS : 1;
        received = true;
    }

    private void clientTick(boolean held)
    {
        if (!received || lerpSteps <= 0)
            return;
        if (held)
            hullYaw = Mth.wrapDegrees(hullYaw + Mth.wrapDegrees(targetHullYaw - hullYaw) / lerpSteps);
        turretYaw += Mth.wrapDegrees(targetTurretYaw - turretYaw) / lerpSteps;
        turretPitch += (targetTurretPitch - turretPitch) / lerpSteps;
        for (int seat = 0; seat < seatYaw.length && seat < targetSeatYaw.length && seat < targetSeatPitch.length; seat++)
        {
            seatYaw[seat] += Mth.wrapDegrees(targetSeatYaw[seat] - seatYaw[seat]) / lerpSteps;
            seatPitch[seat] += (targetSeatPitch[seat] - seatPitch[seat]) / lerpSteps;
        }
        lerpSteps--;
    }
}
