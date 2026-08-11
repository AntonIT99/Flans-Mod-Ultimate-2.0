package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.types.VehicleType;
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
        }

        float waterLimit = isInWater() ? Math.min(Math.max(0F, type.getMaxThrottle()), Math.max(0F, type.getMaxThrottleInWater()))
            : Math.max(0F, type.getMaxThrottle());
        float effectiveThrottle = Mth.clamp(getThrottle(), -Math.max(0F, type.getMaxNegativeThrottle()),
            waterLimit);
        if (!isEngineActive())
            effectiveThrottle = 0F;
        double targetSpeed = effectiveThrottle * getEngineSpeed() * (tracked ? 0.26D : 0.32D) * traction;
        float steeringModifier = wheelYaw > 0F ? type.getTurnLeftModifier() : type.getTurnRightModifier();
        float directionalThrottle = effectiveThrottle > 0F ? type.getMaxThrottle() : type.getMaxNegativeThrottle();
        double throttleModifier = tracked ? 1D : legacyThrottleCurve(effectiveThrottle);
        double velocityScale = (tracked ? 0.04D : 0.1D) * throttleModifier
            * Math.max(0F, directionalThrottle) * getEngineSpeed();
        double steeringScale = 0.1D * Math.max(0F, steeringModifier);
        float yawDelta = isEngineActive()
            ? (float) Math.toDegrees(wheelYaw * steeringScale * velocityScale) : 0F;
        if (!isPartIntact(EnumDriveablePart.STEERING))
            yawDelta = 0F;
        setOrientation(getYaw() + yawDelta, approach(getPitch(), 0F, 0.8F),
            type.isCanRoll() ? approach(getRoll(), 0F, 1F) : 0F);

        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 transformedForward = localDirectionToWorld(legacyForward);
        Vec3 forward = new Vec3(transformedForward.x, 0D, transformedForward.z);
        if (forward.lengthSqr() > 1.0E-8D)
            forward = forward.normalize();
        Vec3 current = getDeltaMovement();
        Vec3 desired = forward.scale(targetSpeed);
        double grip = isInWater() ? 0.08D : (onGround() || hasWheelContact() ? 0.22D : 0.035D);
        Vec3 velocity = new Vec3(Mth.lerp(grip, current.x, desired.x), current.y, Mth.lerp(grip, current.z, desired.z));
        if (DriveableInput.isDown(getInputMask(), DriveableInput.BRAKE | DriveableInput.ASCEND))
            velocity = velocity.multiply(0.55D, 1D, 0.55D);
        velocity = applyVehicleVerticalPhysics(velocity, type);
        double descent = velocity.y;
        velocity = applyWheelContactPhysics(velocity, !type.isFloatOnWater() || !isInWater());
        velocity = velocity.multiply(dragFactor(type), 1D, dragFactor(type));
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
            consumeFuel(Math.abs(effectiveThrottle));
    }

    @Override
    protected void tickClientDriveable()
    {
        VehicleType type = getVehicleType();
        if (type != null)
            advanceAnimations(type);
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
        if (getControllingEntity() != null && hasFuelForEngine())
        {
            float acceleration = type.isUseRealisticAcceleration()
                ? Math.max(0.0005F, getEnginePower() / Math.max(1F, type.getMass()))
                : 0.01F;
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
        }
        if (DriveableInput.isDown(input, DriveableInput.BRAKE | DriveableInput.ASCEND))
            throttle = approach(throttle, 0F, Math.max(0.04F, type.getBrakingModifier() * 0.04F));
        else if (type.isTank() && Math.abs(throttle) < 0.3F && Math.abs(axis(input, DriveableInput.RIGHT, DriveableInput.LEFT)) > 0F)
            throttle += Math.max(0F, type.getClutchBrake());
        else if (throttleDecayDelay > 0)
            --throttleDecayDelay;
        else
            throttle = approach(throttle, 0F, type.getThrottleDecay());
        setThrottle(throttle);

        float steeringTarget = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT) * 20F;
        if (!isPartIntact(EnumDriveablePart.STEERING))
            steeringTarget = 0F;
        prevWheelYaw = wheelYaw;
        wheelYaw = approach(wheelYaw, steeringTarget, steeringTarget == 0F ? 2F : 1.5F);
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
        int configured = 0;
        int intact = 0;
        int wheelIndex = 0;
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            int currentIndex = wheelIndex++;
            if (wheel == null)
                continue;
            if (!type.isFourWheelDrive() && type.getWheelPositions().size() >= 4 && currentIndex >= 2)
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

    private static double dragFactor(VehicleType type)
    {
        return Mth.clamp(0.98D - Math.max(0F, type.getDrag() - 1F) * 0.01D, 0.75D, 0.995D);
    }
}
