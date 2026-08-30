package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.DriveableControlPhysics;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.ThrottleLeverRamp;
import com.flansmodultimate.common.driveables.physics.GroundPropulsionPhysics;
import com.flansmodultimate.common.driveables.physics.GroundSlopePhysics;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsConstants;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketPlaySound;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Wheel-, track- and water-capable server vehicle simulation. */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Vehicle extends Driveable
{
    /** Model-space correction for legacy vehicle models, expressed in blocks. */
    public static final float VEHICLE_MODEL_VERTICAL_OFFSET = -5.5F / 16F;

    @Getter protected float wheelYaw;
    @Getter protected float prevWheelYaw;
    @Getter protected float wheelAngle;
    @Getter protected float prevWheelAngle;
    @Getter protected float leftTrackProgress;
    @Getter protected float rightTrackProgress;
    private int throttleDecayDelay;
    private boolean fixedThrottle;
    /** Progressive throttle lever state. Transient, and tracked per side. */
    private final ThrottleLeverRamp throttleRamp = new ThrottleLeverRamp();
    private final List<PendingSmoke> pendingSmoke = new ArrayList<>();

    public Vehicle(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Vehicle(Level level, VehicleType type, double x, double y, double z, float yaw,
                   @Nullable Player placer, ItemStack sourceStack)
    {
        super(FlansMod.vehicleEntity.get(), level, type, x, y, z, yaw, placer, sourceStack);
    }

    @Nullable
    public VehicleType getVehicleType()
    {
        return getConfigType() instanceof VehicleType type ? type : null;
    }

    @Override
    protected void tickDriveable()
    {
        VehicleType type = getVehicleType();
        if (type == null)
            return;
        // Let the root collision body clear the same ledges that the wheel
        // probes can reach. Legacy vehicles always used a one-block entity
        // step; using the configured value preserves that behaviour without
        // injecting an upward suspension impulse.
        setMaxUpStep(Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F));
        float previousLeftPhase = leftTrackProgress;
        float previousRightPhase = rightTrackProgress;
        advanceAnimations(type);
        tickWalkerStompSounds(type, previousLeftPhase, previousRightPhase);
        tickPendingSmoke();
        updateThrottleAndSteering(type);

        float traction = traction();
        boolean tracked = type.isTank();
        if (tracked && (!isPartIntact(EnumDriveablePart.LEFT_TRACK) || !isPartIntact(EnumDriveablePart.RIGHT_TRACK)))
        {
            traction = 0F;
            setThrottle(approach(getThrottle(), 0F, 0.04F));
            prevWheelYaw = wheelYaw;
            wheelYaw = 0F;
        }

        float throttleLimit = DriveableControlPhysics.damagedThrottleLimit(getThrottleDamageNerf());
        float effectiveThrottle = Mth.clamp(getThrottle(), -throttleLimit, throttleLimit);
        if (!isEngineActive())
            effectiveThrottle = 0F;
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        double speedScale = ModCommonConfig.realisticVehicleSpeedScale();
        float normalizedThrottle = DriveableControlPhysics.normalizedThrottle(effectiveThrottle,
            type.getMaxNegativeThrottle());
        double targetSpeed;
        if (physics.hasGroundPropulsion())
        {
            // The real-world profile owns terminal speed outright. Throttle is
            // reduced to the driver demand fraction, so MaxThrottle and the
            // legacy 0.26 / 0.32 speed factors no longer scale it and cannot
            // double-apply on top of the derived value.
            double terminal = normalizedThrottle >= 0F
                ? physics.maxSpeedBlocksPerTick(speedScale)
                : reverseTerminalSpeed(type, physics, speedScale);
            targetSpeed = normalizedThrottle * terminal * traction;
        }
        else
        {
            float propulsion = DriveableControlPhysics.directionalPropulsion(effectiveThrottle, type.getMaxThrottle(),
                type.getMaxNegativeThrottle(), type.getMaxThrottleInWater(), isInWater());
            targetSpeed = propulsion * getEngineSpeed() * (tracked ? 0.26D : 0.32D) * traction;
            // An authored reverse speed caps legacy propulsion rather than
            // scaling it, so MaxNegativeThrottle is never applied twice.
            if (physics.hasReverseSpeedOverride() && targetSpeed < 0D)
                targetSpeed = Math.max(targetSpeed, -physics.reverseSpeedBlocksPerTick(speedScale));
        }
        // Slope limiting is independently usable: it applies whether propulsion
        // is legacy or derived, and is inert when no limit was authored.
        if (physics.hasSlopeLimit())
            targetSpeed *= GroundSlopePhysics.propulsionFactor(getPitch(),
                Math.signum(normalizedThrottle), physics.maxSlopeDeg());
        float steeringModifier = wheelYaw > 0F ? type.getTurnLeftModifier() : type.getTurnRightModifier();
        float directionalThrottle = effectiveThrottle > 0F
            ? (isInWater() ? type.getMaxThrottleInWater() : type.getMaxThrottle())
            : type.getMaxNegativeThrottle();
        double throttleModifier = tracked ? 1D : legacyThrottleCurve(effectiveThrottle);
        double velocityScale = (tracked ? 0.04D : 0.1D) * throttleModifier
            * Math.max(0F, directionalThrottle) * getEngineSpeed();
        double steeringScale = 0.1D * Math.max(0F, steeringModifier);
        float yawDelta = isEngineActive()
            ? (float) Math.toDegrees(wheelYaw * steeringScale * velocityScale) : 0F;
        if (!isPartIntact(EnumDriveablePart.STEERING))
            yawDelta = 0F;
        boolean supported = onGround() || hasWheelContact();
        float pitch = supported ? getPitch() : approach(getPitch(), 0F, 0.8F);
        float roll = type.isCanRoll()
            ? (supported ? getRoll() : approach(getRoll(), 0F, 1F)) : 0F;
        setOrientation(getYaw() + yawDelta, pitch, roll);

        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 transformedForward = localDirectionToWorld(legacyForward);
        Vec3 forward = new Vec3(transformedForward.x, 0D, transformedForward.z);
        if (forward.lengthSqr() > 1.0E-8D)
            forward = forward.normalize();
        Vec3 current = getDeltaMovement();
        double grip = isInWater() ? 0.08D : (onGround() || hasWheelContact() ? 0.22D : 0.035D);
        boolean braking = DriveableInput.isDown(getInputMask(), DriveableInput.BRAKE | DriveableInput.ASCEND);
        Vec3 velocity;
        if (physics.hasGroundPropulsion())
        {
            velocity = derivedGroundVelocity(physics, current, forward, targetSpeed,
                traction, grip, braking, speedScale);
        }
        else
        {
            Vec3 desired = forward.scale(targetSpeed);
            velocity = new Vec3(Mth.lerp(grip, current.x, desired.x), current.y, Mth.lerp(grip, current.z, desired.z));
            if (braking)
                velocity = velocity.multiply(0.55D, 1D, 0.55D);
        }
        velocity = applyVehicleVerticalPhysics(velocity, type);
        double descent = velocity.y;
        velocity = applyWheelContactPhysics(velocity, !type.isFloatOnWater() || !isInWater());
        double horizontalDrag = GroundPropulsionPhysics.postIntegrationHorizontalDrag(
            physics.hasGroundPropulsion(), type.getDrag());
        velocity = velocity.multiply(horizontalDrag, 1D, horizontalDrag);
        moveWithCollisions(velocity);
        if (tickCount > 20 && verticalCollision && descent < -0.65D && !isInWater())
        {
            float damage = (float) ((-descent - 0.45D) * 30D * Math.max(0F, type.getFallDamageFactor()));
            if (damage > 0F)
            {
                damagePart(EnumDriveablePart.CORE, damage, level().damageSources().fall());
                DriveablePosition firstWheel = type.getWheelPosition(0);
                if (firstWheel != null && firstWheel.getPart() != EnumDriveablePart.CORE)
                    damagePart(firstWheel.getPart(), damage * 0.2F, level().damageSources().fall());
            }
        }

        harvestConfiguredBlocks();
        if (isEngineActive())
            consumeFuel(DriveableControlPhysics.vehicleFuelLoad(effectiveThrottle, type.getWheelPositions().size()));
    }

    @Override
    protected void tickClientDriveable()
    {
        VehicleType type = getVehicleType();
        if (type != null)
        {
            float steeringInput = isPartIntact(EnumDriveablePart.STEERING)
                ? axis(getInputMask(), DriveableInput.RIGHT, DriveableInput.LEFT) : 0F;
            prevWheelYaw = wheelYaw;
            wheelYaw = DriveableControlPhysics.dampedControl(wheelYaw, steeringInput, 1F);
            advanceAnimations(type);
        }
    }

    @Override
    protected void handleRisingInputs(Seat seat, Player player, int rising)
    {
        boolean deploySmoke = DriveableInput.isDown(rising, DriveableInput.FLARE);
        boolean smokeWasReady = !isVarFlare() && !isCountermeasureReloading();
        super.handleRisingInputs(seat, player, rising);

        VehicleType type = getVehicleType();
        if (deploySmoke && smokeWasReady && isVarFlare() && type != null && !type.getSmokers().isEmpty())
            dischargeSmoke();
    }

    /**
     * Vehicles use the legacy smoke dispensers as their countermeasures. The
     * shared timer and flags still provide cooldown and lock-on behaviour, but
     * the generic flare particle must not be emitted as well.
     */
    @Override
    protected void updateFlares()
    {
        if (ticksFlareUsing <= 0)
        {
            setFlag(FLAG_FLARE, false);
            setFlag(FLAG_COUNTERMEASURE_RELOADING, flareDelay > 0);
            return;
        }
        --ticksFlareUsing;
        setFlag(FLAG_FLARE, true);
        setFlag(FLAG_COUNTERMEASURE_RELOADING, flareDelay > 0);
    }

    private void updateThrottleAndSteering(VehicleType type)
    {
        int input = getInputMask();
        float throttle = getThrottle();
        boolean canControl = getControllingEntity() != null && hasFuelForEngine();
        boolean braking = DriveableInput.isDown(input, DriveableInput.BRAKE | DriveableInput.ASCEND);
        boolean pedalInput = DriveableInput.isDown(input, DriveableInput.FORWARD | DriveableInput.BACKWARD);
        fixedThrottle = DriveableControlPhysics.fixedVehicleThrottle(fixedThrottle, canControl, braking, input);
        // Advanced every tick, including the ones where nothing is held, so the
        // ramp is counting an uninterrupted hold and nothing else.
        int leverDirection = canControl && !pedalInput ? ThrottleLeverRamp.direction(input,
            DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE) : 0;
        float leverMultiplier = throttleRamp.advance(leverDirection, ThrottleLeverRamp.VEHICLE_MAX_STEP_MULTIPLIER);
        if (canControl)
        {
            float damageMultiplier = DriveableControlPhysics.damagedAccelerationMultiplier(getThrottleDamageNerf());
            // Precedence: a complete real-world profile beats the legacy
            // UseRealisticAcceleration experiment, which beats the fixed legacy
            // rate. Under the profile this is only how fast the pedal travels;
            // how fast the vehicle actually accelerates comes from the power
            // model, not from a per-pack number.
            float acceleration;
            if (type.getResolvedPhysics().hasGroundPropulsion())
                acceleration = VehiclePhysicsConstants.REAL_THROTTLE_RAMP_PER_TICK;
            else if (type.isUseRealisticAcceleration())
                acceleration = Math.max(0.0005F, getEnginePower() / Math.max(1F, type.getMass()));
            else
                acceleration = 0.01F;
            acceleration *= damageMultiplier;
            if (DriveableInput.isDown(input, DriveableInput.FORWARD))
            {
                throttle += acceleration * (throttle < 0F ? type.getBrakingModifier() : 1F);
                throttleDecayDelay = 10;
            }
            if (DriveableInput.isDown(input, DriveableInput.BACKWARD))
            {
                throttle -= acceleration * (throttle > 0F ? type.getBrakingModifier() : 1F);
                throttleDecayDelay = 10;
            }
            // Unlike the pedals the lever retains the selected demand after
            // release, so it travels on its own fine step rather than the
            // pedal rate, and holding it moves it progressively faster. The
            // ramp restarts as the lever passes through zero, so drive to
            // reverse is never swept.
            float leverStep = ThrottleLeverRamp.VEHICLE_LEVER_BASE_STEP * damageMultiplier * leverMultiplier;
            if (leverDirection > 0)
                throttle += leverStep * (throttle < 0F ? type.getBrakingModifier() : 1F);
            else if (leverDirection < 0)
                throttle -= leverStep * (throttle > 0F ? type.getBrakingModifier() : 1F);
        }
        if (braking)
            throttle = 0F;
        else if (type.isTank() && Math.abs(throttle) < 0.3F && Math.abs(axis(input, DriveableInput.RIGHT, DriveableInput.LEFT)) > 0F)
            throttle += Math.max(0F, type.getClutchBrake());
        else if (throttleDecayDelay > 0)
            --throttleDecayDelay;
        else if (!fixedThrottle)
            throttle = approach(throttle, 0F, type.getThrottleDecay());
        float damageLimit = DriveableControlPhysics.damagedThrottleLimit(getThrottleDamageNerf());
        if (Math.abs(throttle) > damageLimit)
            throttle = (Math.copySign(damageLimit, throttle) + throttle * 2F) / 3F;
        // Whatever moved the lever through zero — the driver, the brake, or the
        // idle decay — the progressive step starts again from the base rate.
        throttleRamp.resetOnZeroCrossing(getThrottle(), throttle);
        setThrottle(throttle);

        float steeringInput = isPartIntact(EnumDriveablePart.STEERING)
            ? axis(input, DriveableInput.RIGHT, DriveableInput.LEFT) : 0F;
        prevWheelYaw = wheelYaw;
        wheelYaw = DriveableControlPhysics.dampedControl(wheelYaw, steeringInput, 1F);
    }

    /**
     * Longitudinal motion under the real-world profile.
     *
     * <p>The power model owns speed along the forward axis: available
     * acceleration falls as the vehicle speeds up and reaches zero exactly at the
     * authored top speed, so the vehicle approaches it without snapping and
     * without overshooting at 20 Hz. Lateral slip keeps decaying at the existing
     * grip constant, so terrain feel and drift are unchanged.
     */
    private Vec3 derivedGroundVelocity(ResolvedVehiclePhysics physics, Vec3 current, Vec3 forward,
                                       double targetSpeed, float traction, double grip,
                                       boolean braking, double speedScale)
    {
        Vec3 horizontal = new Vec3(current.x, 0D, current.z);
        double forwardSpeed = horizontal.dot(forward);
        Vec3 lateral = horizontal.subtract(forward.scale(forwardSpeed));

        double terminal = physics.maxSpeedBlocksPerTick(speedScale);
        double power = physics.effectivePowerWatts(getEngineSpeed());
        double tractionFactor = physics.driveType().tractionFactor()
            * (isInWater() ? 0.35D : 1D) * Math.max(0F, traction);
        double acceleration = GroundPropulsionPhysics.accelerationBlocksPerTickSquared(
            forwardSpeed, power, physics.massKg(), terminal, tractionFactor);
        double deceleration = GroundPropulsionPhysics.decelerationBlocksPerTickSquared(
            forwardSpeed, power, physics.massKg(), terminal, braking);
        double newForwardSpeed = GroundPropulsionPhysics.approach(forwardSpeed, targetSpeed,
            acceleration, deceleration);

        Vec3 driven = forward.scale(newForwardSpeed).add(lateral.scale(1D - grip));
        return new Vec3(driven.x, current.y, driven.z);
    }

    /**
     * Reverse terminal speed under the real-world profile.
     *
     * <p>An authored RealMaxReverseSpeedKmh wins. Otherwise the definition's own
     * forward-to-reverse throttle ratio is carried over, so a pack that always
     * reversed at half speed still does. The MaxNegativeThrottle zero gate lives
     * upstream in normalizedThrottle, so a vehicle that could never reverse still
     * cannot.
     */
    private static double reverseTerminalSpeed(VehicleType type, ResolvedVehiclePhysics physics, double speedScale)
    {
        if (physics.hasReverseSpeedOverride())
            return physics.reverseSpeedBlocksPerTick(speedScale);
        float forwardPower = Math.max(1.0E-4F, type.getMaxThrottle());
        float reversePower = Math.max(0F, type.getMaxNegativeThrottle());
        return physics.maxSpeedBlocksPerTick(speedScale) * Math.min(1D, reversePower / forwardPower);
    }

    private Vec3 applyVehicleVerticalPhysics(Vec3 velocity, VehicleType type)
    {
        if (type.isFloatOnWater() && isInWater())
        {
            setOrientation(getYaw(), approach(getPitch(), 0F, 1.5F), approach(getRoll(), 0F, 1.5F));
            return applyGravityAndBuoyancy(velocity, 0D);
        }
        double gravity = Math.max(0.005D, Math.min(0.08D, type.getGravity() * 0.08D));
        velocity = applyGravityAndBuoyancy(velocity, gravity);
        if (velocity.y < -type.getMaxFallSpeed())
            velocity = new Vec3(velocity.x, -type.getMaxFallSpeed(), velocity.z);
        return velocity;
    }

    private void advanceAnimations(VehicleType type)
    {
        prevWheelAngle = wheelAngle;
        if (type.isRotateWheels() || type.isTank())
            wheelAngle = Mth.wrapDegrees(wheelAngle + getThrottle() * 18F);
        leftTrackProgress += getThrottle() * 0.075F - wheelYaw * 0.0025F;
        rightTrackProgress += getThrottle() * 0.075F + wheelYaw * 0.0025F;
        leftTrackProgress -= Mth.floor(leftTrackProgress);
        rightTrackProgress -= Mth.floor(rightTrackProgress);
    }

    private void tickWalkerStompSounds(VehicleType type, float previousLeftPhase, float previousRightPhase)
    {
        if (level().isClientSide || !isEngineActive() || Math.abs(getThrottle()) <= 0.01F)
            return;
        playWalkerStomp(type.getStompSoundFrontRight(), crossedLegZero(previousRightPhase, rightTrackProgress, 0F));
        playWalkerStomp(type.getStompSoundBackRight(), crossedLegZero(previousRightPhase, rightTrackProgress, 0.75F));
        playWalkerStomp(type.getStompSoundFrontLeft(), crossedLegZero(previousLeftPhase, leftTrackProgress, 0.5F));
        playWalkerStomp(type.getStompSoundBackLeft(), crossedLegZero(previousLeftPhase, leftTrackProgress, 0.25F));
    }

    private void playWalkerStomp(String sound, boolean crossed)
    {
        if (crossed && StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(this, 50D, sound, false);
    }

    private static boolean crossedLegZero(float previousPhase, float currentPhase, float phaseOffset)
    {
        float previous = Mth.sin((previousPhase + phaseOffset) * Mth.TWO_PI);
        float current = Mth.sin((currentPhase + phaseOffset) * Mth.TWO_PI);
        return previous != 0F && current != 0F && Math.signum(previous) != Math.signum(current);
    }

    private float traction()
    {
        VehicleType type = getVehicleType();
        if (type == null || type.getWheelPositions().isEmpty())
            return 1F;
        // An explicit DriveType chooses which wheels drive; without one the
        // legacy FourWheelDrive flag still decides, so inference alone can never
        // change an existing pack's traction.
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        boolean allWheelsDriven = physics.driveTypeExplicit()
            ? physics.driveType().drivesAllWheels()
            : type.isFourWheelDrive();
        int configured = 0;
        int intact = 0;
        int wheelIndex = 0;
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            int currentIndex = wheelIndex++;
            if (wheel == null)
                continue;
            if (!allWheelsDriven && type.getWheelPositions().size() >= 4 && currentIndex >= 2)
                continue;
            ++configured;
            if (isPartIntact(wheel.getPart()))
                ++intact;
        }
        return configured == 0 ? 1F : Mth.clamp((float) intact / configured, 0F, 1F);
    }

    public void dischargeSmoke()
    {
        VehicleType type = getVehicleType();
        if (type == null)
            return;
        for (VehicleType.SmokePoint smoker : type.getSmokers())
        {
            if (smoker == null || !isPartIntact(smoker.part()))
                continue;
            Vec3 localOrigin = LegacyDriveableCoordinates.toLocal(smoker.position());
            Vec3 localDirection = LegacyDriveableCoordinates.toLocal(smoker.direction());
            Vec3 origin = localToWorld(localOrigin.x, localOrigin.y, localOrigin.z);
            Vec3 direction = localDirectionToWorld(localDirection);
            int detonation = Mth.clamp(smoker.detonationTime(), 1, 20 * 60);
            if (detonation == 20)
                PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_SMOKER, origin.x, origin.y, origin.z,
                    direction.x, direction.y, direction.z), origin, 150D, level().dimension());
            else if (pendingSmoke.size() < 64)
                pendingSmoke.add(new PendingSmoke(origin, direction, detonation));
        }
    }

    private void tickPendingSmoke()
    {
        Iterator<PendingSmoke> iterator = pendingSmoke.iterator();
        while (iterator.hasNext())
        {
            PendingSmoke smoke = iterator.next();
            smoke.position = smoke.position.add(smoke.velocity);
            smoke.velocity = smoke.velocity.add(0D, -0.04D, 0D).scale(0.99D);
            if (--smoke.ticks > 0)
            {
                if (smoke.ticks % 3 == 0)
                    PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_SMOKE, smoke.position.x, smoke.position.y,
                        smoke.position.z, 0D, 0D, 0D), smoke.position, 96D, level().dimension());
                continue;
            }
            PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_SMOKE_BURST, smoke.position.x, smoke.position.y,
                smoke.position.z, 0D, 0D, 0D), smoke.position, 150D, level().dimension());
            PacketHandler.sendToAllAround(new PacketParticle(FlanParticles.FM_BIG_SMOKE, smoke.position.x, smoke.position.y,
                smoke.position.z, 0D, 0D, 0D), smoke.position, 150D, level().dimension());
            iterator.remove();
        }
    }

    private static final class PendingSmoke
    {
        private Vec3 position;
        private Vec3 velocity;
        private int ticks;

        private PendingSmoke(Vec3 position, Vec3 velocity, int ticks)
        {
            this.position = position;
            this.velocity = velocity;
            this.ticks = ticks;
        }
    }

    @Override
    protected boolean canFireWeaponBank(boolean secondary)
    {
        VehicleType type = getVehicleType();
        return super.canFireWeaponBank(secondary) && (type == null || !isDoorOpen() || type.isShootWithOpenDoor());
    }

    @Override
    protected boolean shouldSquashEntities()
    {
        VehicleType type = getVehicleType();
        return type != null && type.isSquashMobs();
    }

    private static float axis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }

    private static double legacyThrottleCurve(float throttle)
    {
        double absolute = Math.abs(throttle);
        return (2.4D * absolute - 2.5D * throttle * throttle + 0.5D * absolute * absolute * absolute)
            * Math.signum(throttle);
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }

}
