package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableControlPhysics;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.LegacyPlanePhysics;
import com.flansmodultimate.common.driveables.PlaneCrashDamage;
import com.flansmodultimate.common.driveables.Propeller;
import com.flansmodultimate.common.driveables.ThrottleLeverRamp;
import com.flansmodultimate.common.driveables.physics.AircraftPerformancePhysics;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsConstants;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-authoritative flight runtime supporting fixed wing, helicopter, VTOL and six-DOF craft. */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Plane extends Driveable
{
    private static final Vec3 MODEL_FLIGHT_FORWARD = LegacyDriveableCoordinates.applyPlaneModelFacing(
        LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)));
    private static final Vec3 MODEL_FLIGHT_UP = LegacyDriveableCoordinates.toLocal(new Vec3(0D, 1D, 0D));
    /** Legacy gear toggle required clear air three blocks below the plane. */
    private static final int GEAR_TOGGLE_CLEARANCE = 3;
    /** Legacy landing automation scanned ten blocks below the plane. */
    private static final int LANDING_APPROACH_CLEARANCE = 10;
    /** Legacy automatic doors scanned three blocks below the plane. */
    private static final int DOOR_CLEARANCE = 3;
    /** Throttle at or below which the legacy plane counted as parked. */
    private static final float PARKED_THROTTLE = 0.05F;

    @Getter protected float propellerAngle;
    @Getter protected float prevPropellerAngle;
    @Getter protected float rotorAngle;
    @Getter protected float prevRotorAngle;
    @Getter protected float flapYaw;
    @Getter protected float flapPitchLeft;
    @Getter protected float flapPitchRight;
    @Getter protected float prevFlapYaw;
    @Getter protected float prevFlapPitchLeft;
    @Getter protected float prevFlapPitchRight;
    private float angularYaw;
    private float angularPitch;
    private float angularRoll;
    /** Latches the one automatic door opening per landing, as in 1.7.10. */
    private boolean doorsAutoOpened;
    /** Prevents automatic closing from overriding a later manual reopen. */
    private boolean doorsAutoCloseApplied;
    private int crashImpactCooldown;
    /** Progressive throttle lever state. Transient, and tracked per side. */
    private final ThrottleLeverRamp throttleRamp = new ThrottleLeverRamp();

    public Plane(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Plane(Level level, PlaneType type, double x, double y, double z, float yaw,
                 @Nullable Player placer, ItemStack sourceStack)
    {
        super(FlansMod.planeEntity.get(), level, type, x, y, z, yaw, placer, sourceStack);
        setOrientation(yaw, getInitialPlacementPitch(), 0F);
        setDriveableMode(type.getMode() == EnumPlaneMode.VTOL ? 0 : type.getMode().ordinal());
        // HasGear means retractable gear. Fixed landing gear is still always down.
        setGearDeployed(true);
    }

    @Nullable
    public PlaneType getPlaneType()
    {
        return getConfigType() instanceof PlaneType type ? type : null;
    }

    @Override
    public float getInitialPlacementPitch()
    {
        PlaneType type = getPlaneType();
        // Plane model facing reverses the simulation pitch sign. This is the
        // modern equivalent of 1.7.10's one-time rotatePitch(restingPitch).
        return type == null ? 0F : -type.getRestingPitch();
    }

    public EnumPlaneMode getPlaneMode()
    {
        PlaneType type = getPlaneType();
        if (type == null)
            return EnumPlaneMode.PLANE;
        if (type.getMode() == EnumPlaneMode.VTOL)
            return Math.floorMod(getDriveableMode(), 2) == 0 ? EnumPlaneMode.HELI : EnumPlaneMode.PLANE;
        return type.getMode();
    }

    /**
     * The legacy controller refused to cycle the gear unless the block three
     * below the plane was air, so it can be neither retracted while parked nor
     * raised during the flare.
     */
    @Override
    protected void toggleGear(@NotNull Player player)
    {
        PlaneType type = getPlaneType();
        if (type == null || !type.isHasGear())
            return;
        if (isNearGround(GEAR_TOGGLE_CLEARANCE))
        {
            player.displayClientMessage(
                Component.translatable("message.flansmodultimate.driveable.gear.blocked"), true);
            return;
        }
        setGearDeployed(!isGearDeployed());
        player.displayClientMessage(Component.translatable(isGearDeployed()
            ? "message.flansmodultimate.driveable.gear.down"
            : "message.flansmodultimate.driveable.gear.up"), true);
    }

    /** Automatic deployment on a low, slow approach, announced as in 1.7.10. */
    private void deployGearForLanding()
    {
        if (isGearDeployed())
            return;
        setGearDeployed(true);
        if (getControllingEntity() instanceof Player pilot)
            pilot.displayClientMessage(
                Component.translatable("message.flansmodultimate.driveable.gear.auto_deploy"), true);
    }

    /** Retracted gear is stowed inside the airframe, so it cannot be shot. */
    @Override
    public boolean canHitPart(@Nullable EnumDriveablePart part)
    {
        return isGearDeployed() || !EnumDriveablePart.isWheel(part);
    }

    @Override
    protected void toggleDoor(@NotNull Player player)
    {
        super.toggleDoor(player);
        PlaneType type = getPlaneType();
        if (type != null && type.isHasDoor())
            player.displayClientMessage(Component.translatable(isDoorOpen()
                ? "message.flansmodultimate.driveable.door.open"
                : "message.flansmodultimate.driveable.door.closed"), true);
    }

    @Override
    protected void toggleDriveableMode(@NotNull Player player)
    {
        PlaneType type = getPlaneType();
        if (type == null)
            return;
        if (type.isHasWing() || type.isValkyrie())
        {
            setWingFolded(!isWingFolded());
            player.displayClientMessage(
                Component.translatable("message.flansmodultimate.driveable.wings.switched"), true);
        }
        if (type.getMode() == EnumPlaneMode.VTOL)
        {
            setDriveableMode(Math.floorMod(getDriveableMode() + 1, 2));
            player.displayClientMessage(Component.translatable(getPlaneMode() == EnumPlaneMode.HELI
                ? "message.flansmodultimate.driveable.mode.hover"
                : "message.flansmodultimate.driveable.mode.plane"), true);
        }
    }

    /** Wings are unfolded automatically for a low, slow approach, as in 1.7.10. */
    private void extendWingsForLanding()
    {
        if (!isWingFolded())
            return;
        setWingFolded(false);
        if (getControllingEntity() instanceof Player pilot)
            pilot.displayClientMessage(
                Component.translatable("message.flansmodultimate.driveable.wings.extending"), true);
    }

    /**
     * Doors open themselves once the plane is parked, and are pulled shut again
     * as soon as it is under power. The latch is what makes the manual toggle
     * usable while parked, which is the only time the legacy toggle had any
     * lasting effect on a plane that cannot fly with its doors open.
     */
    private void updateAutomaticDoors(PlaneType type)
    {
        if (!type.isHasDoor())
            return;
        boolean parkedNearGround = Math.abs(getThrottle()) <= PARKED_THROTTLE
            && isNearGround(DOOR_CLEARANCE);
        if (type.isAutoOpenDoorsNearGround() && parkedNearGround)
        {
            if (!doorsAutoOpened)
                setDoorOpen(true);
            doorsAutoOpened = true;
        }
        else
            doorsAutoOpened = false;

        if (!parkedNearGround && !type.isFlyWithOpenDoor())
        {
            // Close only once when leaving the parked condition. Reapplying
            // this every tick used to undo a pilot's manual reopen immediately.
            if (!doorsAutoCloseApplied)
                setDoorOpen(false);
            doorsAutoCloseApplied = true;
        }
        else
            doorsAutoCloseApplied = false;
    }

    @Override
    protected void tickDriveable()
    {
        PlaneType type = getPlaneType();
        if (type == null)
            return;
        if (crashImpactCooldown > 0)
            --crashImpactCooldown;
        advanceAnimations();
        updateThrottle(type);

        if (!type.isHasGear())
            setGearDeployed(true);
        if (type.isHasGear() && type.isAutoDeployLandingGearNearGround() && getThrottle() <= 0.4F
            && isNearGround(LANDING_APPROACH_CLEARANCE))
            deployGearForLanding();
        if (type.isHasWing() && type.isFoldWingForLand() && getThrottle() <= 0.4F
            && isNearGround(LANDING_APPROACH_CLEARANCE))
            extendWingsForLanding();
        updateAutomaticDoors(type);

        Vec3 velocity = switch (getPlaneMode())
        {
            case HELI, VTOL -> helicopterPhysics(type);
            case SIXDOF -> sixDofPhysics(type);
            case PLANE -> fixedWingPhysics(type);
        };
        double descent = velocity.y;
        ResolvedVehiclePhysics resolvedPhysics = type.getResolvedPhysics();
        boolean derivedAircraft = !ModCommonConfig.forceLegacyPlanePhysics() && resolvedPhysics.hasAircraftProfile();
        double requiredTakeoffSpeed = derivedAircraft
            ? VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(
                resolvedPhysics.referenceSpeedMs(ModCommonConfig.realisticSpeedScale(resolvedPhysics.category()),
                    ModCommonConfig.realisticAircraftReferenceSpeedScale()), 1D)
            : type.getTakeoffSpeed();
        double measuredTakeoffSpeed = derivedAircraft
            ? velocity.length() : velocity.horizontalDistance();
        boolean liftingOff = LegacyPlanePhysics.isLiftingOff(getPlaneMode(), measuredTakeoffSpeed,
            requiredTakeoffSpeed, flightForwardVector().y, velocity.y);
        if (!ModCommonConfig.forceLegacyPlanePhysics())
            velocity = enforceSpeedCap(velocity, ModCommonConfig.maxPlaneSpeedKmh());
        if (isGearDeployed() && !liftingOff)
            velocity = applyWheelContactPhysics(velocity, true);
        else
            // Wheel contact is only sampled while the gear is down and the
            // aircraft is not lifting off. Clearing it on the other branch keeps
            // the next tick from reading a stale contact from the takeoff roll
            // and treating an airborne aircraft as still rolling.
            clearWheelContact();
        moveWithCollisions(velocity);
        handleGroundImpact(type, descent);
        if (isEngineActive() && hasWorkingPropeller(type))
            consumeFuel(DriveableControlPhysics.aircraftFuelLoad(getThrottle(), configuredThrottlePower(type),
                getEngineSpeed()));
    }

    @Override
    protected void tickClientDriveable()
    {
        advanceAnimations();
    }

    private void advanceAnimations()
    {
        prevPropellerAngle = propellerAngle;
        prevRotorAngle = rotorAngle;
        // Blades stand still on standby: the legacy plane advanced them only
        // while the throttle was actually open.
        float throttle = getThrottle();
        propellerAngle = Mth.wrapDegrees(propellerAngle + LegacyPlanePhysics.propellerStep(throttle));
        rotorAngle = Mth.wrapDegrees(rotorAngle + LegacyPlanePhysics.rotorStep(throttle));
        prevFlapYaw = flapYaw;
        prevFlapPitchLeft = flapPitchLeft;
        prevFlapPitchRight = flapPitchRight;
        int input = getInputMask();
        float yaw = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        float pitch = axis(input, DriveableInput.ASCEND, DriveableInput.DESCEND);
        float roll = axis(input, DriveableInput.ROLL_RIGHT, DriveableInput.ROLL_LEFT);
        flapYaw = LegacyPlanePhysics.flap(flapYaw, yaw);
        if (isMouseControlEnabled())
        {
            // Mouse packets use flap-angle units while keys use a normalised
            // axis. Combine them as equal stick inputs, then run both through
            // the same legacy flap response so mouse roll cannot be more than
            // twice as fast and WASD remains fully effective in mouse mode.
            float combinedPitch = LegacyPlanePhysics.combinedControlInput(getFlightPitchControl(), pitch);
            float combinedRoll = LegacyPlanePhysics.combinedControlInput(getFlightRollControl(), roll);
            flapPitchLeft = LegacyPlanePhysics.flap(flapPitchLeft, combinedPitch - combinedRoll);
            flapPitchRight = LegacyPlanePhysics.flap(flapPitchRight, combinedPitch + combinedRoll);
        }
        else
        {
            flapPitchLeft = LegacyPlanePhysics.flap(flapPitchLeft, pitch - roll);
            flapPitchRight = LegacyPlanePhysics.flap(flapPitchRight, pitch + roll);
        }
    }

    private void updateThrottle(PlaneType type)
    {
        boolean occupied = getControllingEntity() != null;
        boolean powered = occupied && hasFuelForEngine() && hasWorkingPropeller(type);
        float throttle = getThrottle();
        // Holding the lever moves it progressively faster; a tap is still the
        // authored fine step. Released or reversed, the ramp starts over.
        int direction = powered
            ? ThrottleLeverRamp.direction(getInputMask(), DriveableInput.FORWARD, DriveableInput.BACKWARD) : 0;
        float step = throttleRamp.advance(direction, ThrottleLeverRamp.PLANE_MAX_STEP_MULTIPLIER);
        if (direction > 0)
            throttle += 0.002F * step;
        else if (direction < 0)
            throttle -= 0.005F * step;

        if (!powered)
            throttle = approach(throttle, 0F, 0.008F);
        else if (getPlaneMode() == EnumPlaneMode.HELI && type.isHeliThrottlePull()
            && !DriveableInput.isDown(getInputMask(), DriveableInput.FORWARD | DriveableInput.BACKWARD))
            throttle = Mth.lerp(0.01F, throttle, 0.5F);
        if (isUnderWater() && !type.isWorksUnderWater())
            throttle = 0F;
        setThrottle(throttle);
    }

    private Vec3 fixedWingPhysics(PlaneType type)
    {
        Vec3 current = getDeltaMovement();
        // Precedence: a complete real-world profile beats the legacy
        // NewFlightControl experiment, which beats the legacy flight model.
        // Helicopter, VTOL and six-DOF craft are untouched by the new path.
        if (!ModCommonConfig.forceLegacyPlanePhysics() && type.getResolvedPhysics().hasAircraftProfile())
            return derivedFixedWingPhysics(type, type.getResolvedPhysics(), current);
        applyLegacyControls(type, current);
        if (type.getPropellers().isEmpty())
            return current.add(0D, -LegacyPlanePhysics.GRAVITY, 0D);

        float throttle = isEngineActive() && hasWorkingPropeller(type) ? getThrottle() : 0F;
        float thrust = LegacyPlanePhysics.thrust(throttle, type.getMaxThrottle(), type.getMaxNegativeThrottle(),
            type.getMaxThrottleInWater(), getEngineSpeed(), isUnderWater());
        float drag = Math.max(0F, LegacyPlanePhysics.drag(type.getDrag())
            - (float)Math.sqrt(angularYaw * angularYaw + angularPitch * angularPitch + angularRoll * angularRoll) / 100F);
        double speed = Math.min(current.length(), type.isNewFlightControl() ? type.getMaxSpeed() : 2D);
        double newSpeed = type.isNewFlightControl()
            ? speed + throttle * type.getMaxThrust() / Math.max(1F, type.getMass())
                * type.getPropellers().stream().filter(propeller -> isPartIntact(propeller.getPlanePart())).count()
            : speed + thrust * 2F;
        double correction = Mth.clamp(2D * Math.abs(throttle), 0D, 1.5D);
        Vec3 forward = flightForwardVector();
        Vec3 velocity = current.scale(1D - correction).add(forward.scale(correction * newSpeed));

        int intactWings = (isPartIntact(EnumDriveablePart.LEFT_WING) ? 1 : 0)
            + (isPartIntact(EnumDriveablePart.RIGHT_WING) ? 1 : 0);
        double lift = type.isNewFlightControl()
            ? type.getLift() * speed * speed * 0.5D * type.getWingArea() * intactWings * 0.5D
            : current.lengthSqr() * intactWings * 0.5D;
        lift *= Math.abs(flightUpVector().y);
        lift = Math.min(lift, LegacyPlanePhysics.GRAVITY);
        velocity = velocity.add(0D, lift - LegacyPlanePhysics.GRAVITY, 0D);
        if (onGround() && velocity.y <= 0D)
            velocity = new Vec3(velocity.x, -0.01D, velocity.z);
        velocity = new Vec3(velocity.x * drag,
            velocity.y * (velocity.y < 0D && drag < 1F ? 0.999D : drag), velocity.z * drag);
        if (isWingFolded())
            velocity = velocity.multiply(0.98D, 1D, 0.98D);
        if (getControllingEntity() == null)
            velocity = velocity.multiply(emptyDrag(type), 0.98D, emptyDrag(type));
        return velocity;
    }

    /**
     * Fixed-wing flight derived from real-world data.
     *
     * <p>Thrust comes from the authored kilonewtons, or from shaft power through
     * a propeller efficiency. Drag is calibrated so that full thrust exactly
     * balances at the authored top speed, which makes {@code RealMaxSpeedKmh}
     * authoritative without needing a separate clamp. Lift is derived from wing
     * loading: the wing carries the aircraft at and above its reference airspeed
     * and falls off with the square of speed below it, so a heavy wing sinks
     * without a hard stall threshold. {@code RealClimbRateMs} enters only as a
     * cap on how much excess lift may be converted into a sustained climb.
     */
    private Vec3 derivedFixedWingPhysics(PlaneType type, ResolvedVehiclePhysics physics, Vec3 current)
    {
        double speedScale = ModCommonConfig.realisticSpeedScale(physics.category());
        double terminalBlocksPerTick = physics.maxSpeedBlocksPerTick(speedScale);
        applyDerivedControls(type, physics, current, terminalBlocksPerTick);

        if (type.getPropellers().isEmpty())
            return current.add(0D, -LegacyPlanePhysics.GRAVITY, 0D);

        float throttle = isEngineActive() && hasWorkingPropeller(type) ? Math.max(0F, getThrottle()) : 0F;
        // On the wheels the aircraft rolls along its nose and neither gravity
        // nor the wing may steer it. Measuring airspeed in three dimensions is
        // right in flight, but on the ground the constant downward term would be
        // fed back into forward motion by the alignment below, which is what
        // made a parked aircraft creep away on its own.
        boolean rolling = isSupportedByGround();
        double airspeedBlocksPerTick = rolling ? current.horizontalDistance() : current.length();
        double airspeedMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(airspeedBlocksPerTick);
        double terminalMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(terminalBlocksPerTick);

        float engineModifier = getEngineSpeed();
        double powerKw = physics.effectivePowerWatts(engineModifier) / VehiclePhysicsUnits.WATTS_PER_KILOWATT;
        double thrustKn = physics.effectiveThrustKn(engineModifier);
        double propellerFraction = intactPropellerFraction(type.getPropellers());
        double referenceThrust = AircraftPerformancePhysics.thrustNewtons(thrustKn, powerKw, terminalMs, terminalMs);
        // The lever meters power at the default exponent of one; the config can
        // bend it toward a speed-linear response instead.
        double throttleDemand = AircraftPerformancePhysics.throttleThrustFactor(throttle,
            ModCommonConfig.realisticAircraftThrottleResponse());
        double thrustNewtons = AircraftPerformancePhysics.thrustNewtons(thrustKn, powerKw, airspeedMs, terminalMs)
            * throttleDemand * propellerFraction;

        Float wingSpan = physics.source().aircraft().wingSpanM();
        double accelerationMs2 = AircraftPerformancePhysics.accelerationMs2(thrustNewtons, physics.massKg(),
            airspeedMs, terminalMs, referenceThrust, wingSpan == null ? 0D : wingSpan,
            throttleDemand * propellerFraction);
        accelerationMs2 = Math.max(-VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2,
            accelerationMs2 - AircraftPerformancePhysics.maneuverDecelerationMs2(airspeedMs,
                angularYaw, angularPitch, angularRoll));
        if (rolling)
            accelerationMs2 -= AircraftPerformancePhysics.groundDecelerationMs2(throttleDemand);
        double newSpeed = Math.max(0D, airspeedBlocksPerTick
            + VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(accelerationMs2));
        newSpeed = Math.min(newSpeed, terminalBlocksPerTick * (type.isSupersonic() ? 1.2D : 1D));
        // A closed throttle below walking pace on the ground is parked. Without
        // this floor the deceleration tail leaves a permanent crawl.
        if (rolling && throttleDemand <= 0D && VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(newSpeed)
            < VehiclePhysicsConstants.GROUND_PARKING_SPEED_MS)
            newSpeed = 0D;

        int intactWings = (isPartIntact(EnumDriveablePart.LEFT_WING) ? 1 : 0)
            + (isPartIntact(EnumDriveablePart.RIGHT_WING) ? 1 : 0);
        double liftFraction = AircraftPerformancePhysics.liftFraction(airspeedMs,
            physics.referenceSpeedMs(speedScale, ModCommonConfig.realisticAircraftReferenceSpeedScale()))
            * intactWings * 0.5D;
        Float climbRate = physics.source().aircraft().climbRateMs();
        double excessAllowance = AircraftPerformancePhysics.maxExcessLiftFraction(
            climbRate == null ? 0D : climbRate, terminalMs);
        liftFraction = Math.min(liftFraction, 1D + excessAllowance);

        Vec3 forward = flightForwardVector();
        Vec3 velocity;
        if (rolling)
        {
            // Rolling on the wheels: the aircraft tracks its nose in the
            // horizontal plane only, and the vertical axis is left entirely to
            // gravity, lift and the suspension.
            Vec3 heading = new Vec3(forward.x, 0D, forward.z);
            heading = heading.lengthSqr() > 1.0E-8D ? heading.normalize()
                : new Vec3(current.x, 0D, current.z);
            heading = heading.lengthSqr() > 1.0E-8D ? heading.normalize() : Vec3.ZERO;
            velocity = heading.scale(newSpeed).add(0D, current.y, 0D);
        }
        else
        {
            // Velocity swings toward the nose in proportion to how much the wing
            // is actually biting, so a stalled aircraft keeps its old momentum
            // and mushes rather than pointing wherever the pilot aims.
            double alignment = Mth.clamp(0.12D + 0.6D * liftFraction, 0.05D, 0.85D);
            velocity = current.scale(1D - alignment).add(forward.scale(alignment * newSpeed));
        }

        double lift = liftFraction * LegacyPlanePhysics.GRAVITY * Math.abs(flightUpVector().y);
        velocity = velocity.add(0D, lift - LegacyPlanePhysics.GRAVITY, 0D);
        if (onGround() && velocity.y <= 0D)
            velocity = new Vec3(velocity.x, -0.01D, velocity.z);
        // Retained legacy trims: folded wings and an unoccupied airframe still
        // add drag, because neither is expressible in the real-world data.
        if (isWingFolded())
            velocity = velocity.multiply(0.98D, 1D, 0.98D);
        if (getControllingEntity() == null)
            velocity = velocity.multiply(emptyDrag(type), 0.98D, emptyDrag(type));
        return velocity;
    }

    /**
     * Attitude integration for the derived model. Control authority is
     * normalised against the aircraft's own terminal speed rather than the
     * legacy fixed breakpoints, and the slew rate is scaled by a roll inertia
     * factor derived from wing span and mass. The authored pitch, yaw and roll
     * modifiers are retained unchanged as handling trims.
     */
    private void applyDerivedControls(PlaneType type, ResolvedVehiclePhysics physics, Vec3 velocity,
                                      double terminalBlocksPerTick)
    {
        float pitchControl = (flapPitchLeft + flapPitchRight) * 0.5F;
        float rollControl = (flapPitchRight - flapPitchLeft) * 0.5F;
        float authority = AircraftPerformancePhysics.normalizedControlAuthority(velocity.length(),
            terminalBlocksPerTick);
        LegacyPlanePhysics.ControlRates rates = LegacyPlanePhysics.derivedControlRates(authority, flapYaw,
            pitchControl, rollControl, type.getTurnLeftModifier(), type.getTurnRightModifier(),
            type.getLookUpModifier(), type.getLookDownModifier(), type.getRollLeftModifier(),
            type.getRollRightModifier());
        float yawRate = rates.yaw();
        float pitchRate = rates.pitch();
        float rollRate = rates.roll();
        if (!isPartIntact(EnumDriveablePart.TAIL))
        {
            yawRate = 0F;
            pitchRate = 0F;
        }
        if (!isPartIntact(EnumDriveablePart.LEFT_WING))
            rollRate -= 2F * velocity.horizontalDistance();
        if (!isPartIntact(EnumDriveablePart.RIGHT_WING))
            rollRate += 2F * velocity.horizontalDistance();

        float response = physics.rollInertiaFactor();
        angularYaw = LegacyPlanePhysics.approachMomentum(angularYaw, yawRate, response);
        angularPitch = LegacyPlanePhysics.approachMomentum(angularPitch, pitchRate, response);
        angularRoll = LegacyPlanePhysics.approachMomentum(angularRoll, rollRate, response);
        axes.rotateLocalYaw(angularYaw);
        axes.rotateLocalPitch(angularPitch);
        axes.rotateLocalRoll(-angularRoll);
        // A wing that has run out of speed stops holding the nose up. Simulation
        // pitch is negative nose-up, so the correction is added, and it is only
        // ever a bias back toward level: the pilot keeps full authority.
        float pitch = axes.getPitch() + AircraftPerformancePhysics.stallRecoveryPitchDegrees(
            VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(velocity.length()),
            physics.referenceSpeedMs(ModCommonConfig.realisticSpeedScale(physics.category()),
                ModCommonConfig.realisticAircraftReferenceSpeedScale()),
            axes.getPitch());
        setOrientation(axes.getYaw(), pitch, axes.getRoll());
        angularYaw *= 0.99F;
        angularPitch *= 0.99F;
        angularRoll *= 0.99F;
    }

    private Vec3 helicopterPhysics(PlaneType type)
    {
        Vec3 current = getDeltaMovement();
        applyLegacyControls(type, current);
        if (type.getHeliPropellers().isEmpty())
            return current.add(0D, -0.05D, 0D);
        float rotorFraction = rotorEfficiency(type);
        float throttle = isEngineActive() ? getThrottle() : 0F;
        float thrust = LegacyPlanePhysics.thrust(throttle, type.getMaxThrottle(), type.getMaxNegativeThrottle(),
            type.getMaxThrottleInWater(), getEngineSpeed(), isUnderWater()) * rotorFraction * 2F;
        double upwardsForce = throttle * thrust + (0.05D - thrust * 0.5D);
        if (throttle < 0.5F)
            upwardsForce = 0.05D * throttle * 2D;
        if (!isPartIntact(EnumDriveablePart.BLADES))
            upwardsForce = 0D;
        Vec3 up = flightUpVector();
        if (throttle > 0.48F && throttle < 0.52F && up.y >= 0.7D)
            upwardsForce = 0.05D / up.y;
        Vec3 velocity = current.add(up.x * upwardsForce * 0.5D,
            up.y * upwardsForce - 0.05D, up.z * upwardsForce * 0.5D);
        float drag = LegacyPlanePhysics.drag(type.getDrag());
        double horizontalDrag = 1D - (1D - drag) / 5D;
        return new Vec3(velocity.x * horizontalDrag, velocity.y * drag, velocity.z * horizontalDrag);
    }

    private Vec3 sixDofPhysics(PlaneType type)
    {
        int input = getInputMask();
        float yawInput = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        float rollInput = rollInput(input);
        float pitchInput = pitchInput(input);
        setOrientation(getYaw() + yawInput * type.getTurnRightModifier(), getPitch() + pitchInput * type.getLookDownModifier(),
            getRoll() + rollInput * type.getRollRightModifier());
        float poweredThrottle = isEngineActive() && hasWorkingPropeller(type) ? getThrottle() : 0F;
        double propulsion = DriveableControlPhysics.directionalPropulsion(poweredThrottle, type.getMaxThrottle(),
            type.getMaxNegativeThrottle(), type.getMaxThrottleInWater(), isInWater())
            + poweredThrottle * getEngineSpeed();
        double thrust = type.getMaxSpeed() * propulsion * intactPropellerFraction(type.getPropellers()) * 0.35D;
        Vec3 desired = flightForwardVector().scale(thrust);
        Vec3 velocity = getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(0.1D));
        return applyAerodynamicDrag(velocity, type);
    }

    private void applyLegacyControls(PlaneType type, Vec3 velocity)
    {
        float pitchControl = (flapPitchLeft + flapPitchRight) * 0.5F;
        float rollControl = (flapPitchRight - flapPitchLeft) * 0.5F;
        LegacyPlanePhysics.ControlRates rates = LegacyPlanePhysics.controlRates(getPlaneMode(),
            (float)velocity.length(), (float)velocity.horizontalDistance(), getThrottle(), flapYaw,
            pitchControl, rollControl, type.getTurnLeftModifier(), type.getTurnRightModifier(),
            type.getLookUpModifier(), type.getLookDownModifier(), type.getRollLeftModifier(),
            type.getRollRightModifier());
        float yawRate = rates.yaw();
        float pitchRate = rates.pitch();
        float rollRate = rates.roll();
        if (getPlaneMode() == EnumPlaneMode.PLANE)
        {
            if (!isPartIntact(EnumDriveablePart.TAIL))
            {
                yawRate = 0F;
                pitchRate = 0F;
            }
            if (!isPartIntact(EnumDriveablePart.LEFT_WING))
                rollRate -= 2F * velocity.horizontalDistance();
            if (!isPartIntact(EnumDriveablePart.RIGHT_WING))
                rollRate += 2F * velocity.horizontalDistance();
        }
        else if (getPlaneMode() == EnumPlaneMode.HELI && !isPartIntact(EnumDriveablePart.TAIL))
        {
            yawRate = 10F * getThrottle();
        }

        angularYaw = LegacyPlanePhysics.approachMomentum(angularYaw, yawRate);
        angularPitch = LegacyPlanePhysics.approachMomentum(angularPitch, pitchRate);
        angularRoll = LegacyPlanePhysics.approachMomentum(angularRoll, rollRate);
        axes.rotateLocalYaw(angularYaw);
        axes.rotateLocalPitch(angularPitch);
        axes.rotateLocalRoll(-angularRoll);
        setOrientation(axes.getYaw(), axes.getPitch(), axes.getRoll());
        angularYaw *= 0.99F;
        angularPitch *= 0.99F;
        angularRoll *= 0.99F;
    }

    private Vec3 applyAerodynamicDrag(Vec3 velocity, PlaneType type)
    {
        if (type.isFloatOnWater() && isInWater())
            return applyGravityAndBuoyancy(velocity, 0D);
        double factor = Mth.clamp(0.995D - Math.max(0F, type.getDrag() - 1F) * 0.01D, 0.82D, 0.998D);
        double maximum = Math.max(0.2D, type.getMaxSpeed() * (type.isSupersonic() ? 1.5D : 1D));
        if (velocity.length() > maximum)
            velocity = velocity.normalize().scale(maximum);
        return velocity.scale(factor);
    }

    private float wingEfficiency()
    {
        float left = isPartIntact(EnumDriveablePart.LEFT_WING) ? 1F : 0.15F;
        float right = isPartIntact(EnumDriveablePart.RIGHT_WING) ? 1F : 0.15F;
        return (left + right) * 0.5F;
    }

    private float rotorEfficiency(PlaneType type)
    {
        float efficiency = intactPropellerFraction(type.getHeliPropellers());
        if (!isPartIntact(EnumDriveablePart.BLADES))
            efficiency *= 0.1F;
        return efficiency;
    }

    private boolean hasWorkingPropeller(PlaneType type)
    {
        List<Propeller> relevant = getPlaneMode() == EnumPlaneMode.HELI ? type.getHeliPropellers() : type.getPropellers();
        return relevant.isEmpty() || relevant.stream().anyMatch(propeller -> isPartIntact(propeller.getPlanePart()));
    }

    private float configuredThrottlePower(PlaneType type)
    {
        if (getThrottle() < 0F)
            return Math.max(0F, type.getMaxNegativeThrottle());
        return isInWater() ? Math.max(0F, type.getMaxThrottleInWater()) : Math.max(0F, type.getMaxThrottle());
    }

    private float intactPropellerFraction(List<Propeller> propellers)
    {
        if (propellers.isEmpty())
            return 1F;
        long intact = propellers.stream().filter(propeller -> isPartIntact(propeller.getPlanePart())).count();
        return (float) intact / propellers.size();
    }

    private static float emptyDrag(PlaneType type)
    {
        return Mth.clamp(1F - 0.05F * Math.max(0F, type.getEmptyDrag() - 1F), 0.7F, 1F);
    }

    /** Uses the rendered legacy-plane basis so pitch and roll tilt propulsion with the visible aircraft. */
    private Vec3 flightForwardVector()
    {
        return modelLocalDirectionToWorld(MODEL_FLIGHT_FORWARD).normalize();
    }

    private Vec3 flightRightVector()
    {
        return localDirectionToWorld(LegacyDriveableCoordinates.toLocal(new Vec3(0D, 0D, 1D))).normalize();
    }

    private Vec3 flightUpVector()
    {
        return modelLocalDirectionToWorld(MODEL_FLIGHT_UP).normalize();
    }

    private static float axis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }

    private float pitchInput(int mask)
    {
        float keyboard = axis(mask, DriveableInput.DESCEND, DriveableInput.ASCEND);
        return isMouseControlEnabled()
            ? LegacyPlanePhysics.combinedControlInput(getFlightPitchControl(), keyboard) : keyboard;
    }

    private void handleGroundImpact(@NotNull PlaneType type, double descentVelocity)
    {
        if (crashImpactCooldown > 0 || descentVelocity >= -0.01D
            || !verticalCollision && !isSupportedByGround())
            return;

        PlaneCrashDamage.Impact impact = PlaneCrashDamage.evaluate(-descentVelocity,
            Mth.clamp(getUpVector().y, -1D, 1D), type.getFallDamageFactor());
        if (!impact.damaging())
            return;

        crashImpactCooldown = 12;
        float damage = impact.damage() * (isGearDeployed() ? 1F : 1.35F);
        if (isGearDeployed())
        {
            damageCrashPart(EnumDriveablePart.CORE_WHEEL, damage * 0.35F, impact.severity(), 0.2F, false);
            damageCrashPart(EnumDriveablePart.LEFT_WING_WHEEL, damage * 0.25F, impact.severity(), 0.15F, false);
            damageCrashPart(EnumDriveablePart.RIGHT_WING_WHEEL, damage * 0.25F, impact.severity(), 0.15F, false);
            damageCrashPart(EnumDriveablePart.TAIL_WHEEL, damage * 0.2F, impact.severity(), 0.12F, false);
        }

        Vec3 forward = flightForwardVector();
        double roll = getRoll();
        EnumDriveablePart struckPart;
        if (Math.abs(roll) >= Math.abs(getPitch()))
            struckPart = roll >= 0D ? EnumDriveablePart.RIGHT_WING : EnumDriveablePart.LEFT_WING;
        else
            struckPart = forward.y < 0D ? EnumDriveablePart.NOSE : EnumDriveablePart.TAIL;
        damageCrashPart(struckPart, damage * (0.45F + impact.severity() * 0.35F),
            impact.severity(), 0.3F, true);
        damageCrashPart(EnumDriveablePart.CORE, damage * 0.25F, impact.severity(), 0.06F, false);
        handleCollisionPointImpacts(new Vec3(0D, descentVelocity, 0D));
    }

    private void damageCrashPart(@NotNull EnumDriveablePart partType, float flatDamage, float severity,
                                 float healthFraction, boolean breakOnBrutalImpact)
    {
        var part = driveableData == null ? null : driveableData.getPart(partType);
        if (part == null || part.isDestroyed())
            return;
        float amount = Math.max(flatDamage, part.getMaxHealth() * severity * healthFraction);
        if (breakOnBrutalImpact && severity >= 0.9F)
            amount = Math.max(amount, part.getHealth() + 1F);
        damagePart(partType, amount, level().damageSources().flyIntoWall());
    }

    private float rollInput(int mask)
    {
        float keyboard = axis(mask, DriveableInput.ROLL_RIGHT, DriveableInput.ROLL_LEFT);
        return isMouseControlEnabled()
            ? LegacyPlanePhysics.combinedControlInput(getFlightRollControl(), keyboard) : keyboard;
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }
}
