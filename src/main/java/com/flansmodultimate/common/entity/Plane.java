package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.Propeller;
import com.flansmodultimate.common.types.PlaneType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

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
    @Getter protected float propellerAngle;
    @Getter protected float prevPropellerAngle;
    @Getter protected float flapYaw;
    @Getter protected float flapPitchLeft;
    @Getter protected float flapPitchRight;

    public Plane(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Plane(Level level, PlaneType type, double x, double y, double z, float yaw,
                 @Nullable Player placer, ItemStack sourceStack)
    {
        super(FlansMod.planeEntity.get(), level, type, x, y, z, yaw, placer, sourceStack);
        setDriveableMode(type.getMode() == EnumPlaneMode.VTOL ? 0 : type.getMode().ordinal());
        setGearDeployed(type.isHasGear());
    }

    @Nullable
    public PlaneType getPlaneType()
    {
        return getConfigType() instanceof PlaneType type ? type : null;
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

    @Override
    protected void toggleDriveableMode()
    {
        PlaneType type = getPlaneType();
        if (type == null)
            return;
        if (type.isHasWing() || type.isValkyrie())
            setWingFolded(!isWingFolded());
        if (type.getMode() == EnumPlaneMode.VTOL)
            setDriveableMode(Math.floorMod(getDriveableMode() + 1, 2));
    }

    @Override
    protected void tickDriveable()
    {
        PlaneType type = getPlaneType();
        if (type == null)
            return;
        advanceAnimations();
        updateThrottle(type);

        if (type.isHasGear() && type.isAutoDeployLandingGearNearGround() && getThrottle() <= 0.4F && isNearGround(10))
            setGearDeployed(true);
        if (type.isHasWing() && type.isFoldWingForLand() && isNearGround(10) && getThrottle() <= 0.4F)
            setWingFolded(false);
        if (type.isHasDoor())
        {
            if (type.isAutoOpenDoorsNearGround() && isNearGround(3) && Math.abs(getThrottle()) <= 0.05F)
                setDoorOpen(true);
            else if (!type.isFlyWithOpenDoor() && Math.abs(getThrottle()) > 0.05F)
                setDoorOpen(false);
        }

        Vec3 velocity = switch (getPlaneMode())
        {
            case HELI, VTOL -> helicopterPhysics(type);
            case SIXDOF -> sixDofPhysics(type);
            case PLANE -> fixedWingPhysics(type);
        };
        double descent = velocity.y;
        if (isGearDeployed())
            velocity = applyWheelContactPhysics(velocity, true);
        moveWithCollisions(velocity);
        if (verticalCollision && descent < -0.55D)
        {
            float landingDamage = (float) ((-descent - 0.45D) * 18D * Math.max(0F, type.getFallDamageFactor()));
            if (!isGearDeployed())
                landingDamage *= 2F;
            damagePart(isGearDeployed() ? EnumDriveablePart.CORE_WHEEL : EnumDriveablePart.CORE,
                landingDamage, level().damageSources().fall());
        }
        if (isEngineActive())
            consumeFuel(Math.abs(getThrottle()) * Math.max(1, type.numEngines()));
    }

    @Override
    protected void tickClientDriveable()
    {
        advanceAnimations();
    }

    private void advanceAnimations()
    {
        prevPropellerAngle = propellerAngle;
        propellerAngle = Mth.wrapDegrees(propellerAngle + 4F + Math.abs(getThrottle()) * 46F);
        int input = getInputMask();
        float yawTarget = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT) * 20F;
        float pitch = pitchInput(input) * 20F;
        float roll = rollInput(input);
        flapYaw = approach(flapYaw, yawTarget, 2.5F);
        flapPitchLeft = approach(flapPitchLeft, pitch + roll * 15F, 2.5F);
        flapPitchRight = approach(flapPitchRight, pitch - roll * 15F, 2.5F);
    }

    private void updateThrottle(PlaneType type)
    {
        boolean occupied = getControllingEntity() != null;
        boolean powered = occupied && hasFuelForEngine() && hasWorkingPropeller(type);
        float throttle = getThrottle();
        if (powered && DriveableInput.isDown(getInputMask(), DriveableInput.FORWARD))
            throttle += 0.012F * getEngineSpeed();
        if (powered && DriveableInput.isDown(getInputMask(), DriveableInput.BACKWARD))
            throttle -= 0.018F * getEngineSpeed();

        if (!powered)
            throttle = approach(throttle, 0F, 0.008F);
        else if (getPlaneMode() == EnumPlaneMode.HELI && type.isHeliThrottlePull()
            && !DriveableInput.isDown(getInputMask(), DriveableInput.FORWARD | DriveableInput.BACKWARD))
            throttle = Mth.lerp(0.01F, throttle, 0.5F);
        if (isUnderWater() && !type.isWorksUnderWater())
            throttle = 0F;
        else if (isInWater())
            throttle = Math.min(throttle, Math.max(0F, type.getMaxThrottleInWater()));
        setThrottle(throttle);
    }

    private Vec3 fixedWingPhysics(PlaneType type)
    {
        int input = getInputMask();
        double speed = getDeltaMovement().horizontalDistance();
        float control = Mth.clamp((float) (speed / Math.max(0.05F, type.getTakeoffSpeed())), 0.15F, 1.25F);
        float tail = isPartIntact(EnumDriveablePart.TAIL) ? 1F : type.isSpinWithoutTail() ? 0.2F : 0.45F;
        float yawInput = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        float pitchInput = pitchInput(input);
        float rollInput = rollInput(input);
        float yawRate = yawInput * (yawInput > 0F ? type.getTurnRightModifier() : type.getTurnLeftModifier()) * control * tail;
        float pitchRate = pitchInput * (pitchInput > 0F ? type.getLookDownModifier() : type.getLookUpModifier()) * control;
        float desiredRoll = Mth.clamp(rollInput * 45F + yawInput * 22F, -70F, 70F);
        float rollRate = (desiredRoll - getRoll()) * 0.08F
            * (rollInput > 0F ? type.getRollRightModifier() : type.getRollLeftModifier());
        setOrientation(getYaw() + yawRate, getPitch() + pitchRate, getRoll() + rollRate);

        float wingEfficiency = wingEfficiency();
        double thrustToMass = Math.sqrt(Mth.clamp(type.getMaxThrust() / 50F, 0.05F, 20F)
            * Mth.clamp(1000F / Math.max(1F, type.getMass()), 0.05F, 20F));
        double poweredThrottle = isEngineActive() ? getThrottle() : 0D;
        double targetSpeed = type.getMaxSpeed() * getEngineSpeed() * poweredThrottle * 0.42D * wingEfficiency * thrustToMass;
        if (isWingFolded())
            targetSpeed *= 0.25D;
        Vec3 desired = flightForwardVector().scale(targetSpeed);
        Vec3 current = getDeltaMovement();
        double steeringBlend = type.isNewFlightControl() ? 0.12D : 0.075D;
        Vec3 velocity = current.add(desired.subtract(current).scale(steeringBlend));
        double liftRatio = Mth.clamp(velocity.horizontalDistance() / Math.max(0.05D, type.getTakeoffSpeed()), 0D, 1.5D);
        double wingLoading = Mth.clamp(type.getWingArea() * 1000F / Math.max(1F, type.getMass()), 0.1F, 4F);
        double lift = 0.055D * type.getLift() * wingEfficiency * liftRatio * wingLoading;
        velocity = velocity.add(0D, lift - 0.045D, 0D);
        if (onGround() && Math.abs(getThrottle()) < 0.1F)
            setPitch(Mth.lerp(0.12F, getPitch(), type.getRestingPitch()));
        if (getControllingEntity() == null)
            velocity = velocity.multiply(emptyDrag(type), 0.98D, emptyDrag(type));
        return applyAerodynamicDrag(velocity, type);
    }

    private Vec3 helicopterPhysics(PlaneType type)
    {
        int input = getInputMask();
        float yawInput = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        yawInput *= intactPropellerFraction(type.getHeliTailPropellers());
        if (!isPartIntact(EnumDriveablePart.TAIL))
            yawInput *= 0.2F;
        float pitchInput = pitchInput(input);
        float rollInput = rollInput(input);
        float desiredPitch = pitchInput * 22F;
        float desiredRoll = rollInput * 28F;
        setOrientation(getYaw() + yawInput * type.getTurnRightModifier(),
            getPitch() + (desiredPitch - getPitch()) * 0.08F,
            getRoll() + (desiredRoll - getRoll()) * 0.08F);

        float rotorEfficiency = rotorEfficiency(type);
        Vec3 current = getDeltaMovement();
        double thrustToMass = Math.sqrt(Mth.clamp(type.getMaxThrust() / 50F, 0.05F, 20F)
            * Mth.clamp(1000F / Math.max(1F, type.getMass()), 0.05F, 20F));
        boolean powered = isEngineActive();
        double verticalTarget = powered ? (getThrottle() - 0.48F) * 0.32D * getEngineSpeed() * rotorEfficiency * thrustToMass : -0.12D;
        Vec3 forward = flightForwardVector();
        Vec3 right = flightRightVector();
        Vec3 horizontalForward = new Vec3(forward.x, 0D, forward.z).normalize();
        Vec3 horizontalRight = new Vec3(right.x, 0D, right.z).normalize();
        double tiltForward = -getPitch() / 45D;
        double tiltRight = getRoll() / 45D;
        Vec3 desiredHorizontal = powered
            ? horizontalForward.scale(tiltForward * type.getMaxSpeed() * 0.16D)
                .add(horizontalRight.scale(tiltRight * type.getMaxSpeed() * 0.12D))
            : Vec3.ZERO;
        Vec3 velocity = new Vec3(Mth.lerp(0.08D, current.x, desiredHorizontal.x), Mth.lerp(0.12D, current.y, verticalTarget),
            Mth.lerp(0.08D, current.z, desiredHorizontal.z));
        if (rotorEfficiency <= 0.05F)
            velocity = velocity.add(0D, -0.06D, 0D);
        return applyAerodynamicDrag(velocity, type);
    }

    private Vec3 sixDofPhysics(PlaneType type)
    {
        int input = getInputMask();
        float yawInput = axis(input, DriveableInput.RIGHT, DriveableInput.LEFT);
        float rollInput = rollInput(input);
        float pitchInput = pitchInput(input);
        setOrientation(getYaw() + yawInput * type.getTurnRightModifier(), getPitch() + pitchInput * type.getLookDownModifier(),
            getRoll() + rollInput * type.getRollRightModifier());
        double thrust = type.getMaxSpeed() * (isEngineActive() ? getThrottle() : 0F) * getEngineSpeed() * 0.35D;
        Vec3 desired = flightForwardVector().scale(thrust);
        Vec3 velocity = getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(0.1D));
        return applyAerodynamicDrag(velocity, type);
    }

    private Vec3 applyAerodynamicDrag(Vec3 velocity, PlaneType type)
    {
        if (type.isFloatOnWater() && isInWater())
            return applyGravityAndBuoyancy(velocity, 0D);
        double factor = Mth.clamp(0.995D - Math.max(0F, type.getDrag() - 1F) * 0.01D, 0.82D, 0.998D);
        double maximum = Math.max(0.2D, type.getMaxSpeed() * getEngineSpeed() * (type.isSupersonic() ? 1.5D : 1D));
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

    /**
     * Legacy plane content is authored with model X as forward and model Z as
     * lateral. Convert that basis before using the modern driveable transform,
     * matching the corrected vehicle propulsion path.
     */
    private Vec3 flightForwardVector()
    {
        return localDirectionToWorld(LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D))).normalize();
    }

    private Vec3 flightRightVector()
    {
        return localDirectionToWorld(LegacyDriveableCoordinates.toLocal(new Vec3(0D, 0D, 1D))).normalize();
    }

    private static float axis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }

    private float pitchInput(int mask)
    {
        float keyboard = axis(mask, DriveableInput.DESCEND, DriveableInput.ASCEND);
        return Mth.clamp(keyboard + (isMouseControlEnabled() ? getFlightPitchControl() : 0F), -1F, 1F);
    }

    private float rollInput(int mask)
    {
        float keyboard = axis(mask, DriveableInput.ROLL_RIGHT, DriveableInput.ROLL_LEFT);
        return Mth.clamp(keyboard + (isMouseControlEnabled() ? getFlightRollControl() : 0F), -1F, 1F);
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }
}
