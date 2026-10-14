package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.*;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.visual.*;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.*;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Animation state of one drawn driveable between frames: eased throttle, steering, doors and gear, track and leg phases.
 * It reads a {@link DriveableVisual}, so a live driveable and a stand-in animate alike. Render thread only.
 */
public final class DriveableAnimationHistory
{
    private static final float TRANSITION_PER_TICK = 0.16F;

    private DriveableType configuredType;
    private int lastTick = Integer.MIN_VALUE;
    private float previousThrottle;
    private float throttle;
    private float previousSteering;
    private float steering;
    private float previousGear;
    private float gear;
    private float previousDoor;
    private float door;
    private float previousMode;
    private float mode;
    private float previousLeftTrack;
    private float leftTrack;
    private float previousRightTrack;
    private float rightTrack;
    private float previousLegSwing;
    private float legSwing;
    private final ModelDriveable.AnimatedTransform wingTransform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.AnimatedTransform wingWheelTransform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.AnimatedTransform bodyWheelTransform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.AnimatedTransform tailWheelTransform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.AnimatedTransform doorTransform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.AnimatedTransform door2Transform = new ModelDriveable.AnimatedTransform();
    private final ModelDriveable.LegAnimation legAnimation = new ModelDriveable.LegAnimation();
    private final TrackLinkAnimation trackLinks = new TrackLinkAnimation();
    final GunAnimations leftGunAnimations = new GunAnimations();
    final GunAnimations rightGunAnimations = new GunAnimations();
    private GunItem leftGunItem;
    private GunItem rightGunItem;
    private int leftGunRounds = -1;
    private int rightGunRounds = -1;
    private DriveableType passengerPivotType;
    private ModelDriveable passengerPivotModel;
    private int shootPointPivotTick = Integer.MIN_VALUE;
    boolean usingImpostor;
    int trackLinkGroup;

    void updateModelAimPivots(Driveable driveable, DriveableType type, ModelDriveable model)
    {
        // Shoot points can be moved live by /flandebug, which can put one on a
        // different barrel section, so these are looked up again every tick.
        if (model instanceof ModelVehicle vehicleModel && shootPointPivotTick != driveable.getVisualTick())
        {
            shootPointPivotTick = driveable.getVisualTick();
            driveable.setModelShootPointPitchPivots(false, vehicleModel.getShootPointPitchPivots(type, false));
            driveable.setModelShootPointPitchPivots(true, vehicleModel.getShootPointPitchPivots(type, true));
        }
        if (passengerPivotType == type && passengerPivotModel == model)
            return;
        // Otherwise only the driver's input supplies the barrel pivot, so a
        // reloaded tank with its barrel elevated pitched it round TurretOrigin,
        // misplacing its muzzle marker until someone got in.
        if (model instanceof ModelVehicle vehicleModel)
            driveable.setModelBarrelPitchPivot(vehicleModel.getPrimaryBarrelPitchPivot());
        for (int seat = 1; seat <= type.getNumPassengers(); seat++)
        {
            var info = type.getSeat(seat);
            if (info != null && info.getGunType() != null)
                driveable.setModelPassengerGunAimPivot(seat, model.getRegisteredGunAimPivot(info.getGunName()));
        }
        passengerPivotType = type;
        passengerPivotModel = model;
    }

    /** Steps the animation to the source's current tick. Safe to call every frame. */
    public void advance(DriveableVisual driveable, DriveableType type)
    {
        if (lastTick == Integer.MIN_VALUE || configuredType != type)
        {
            configuredType = type;
            throttle = driveable.getThrottle();
            previousThrottle = throttle;
            steering = targetSteering(driveable.getInputMask());
            previousSteering = steering;
            gear = driveable.isGearDeployed() ? 1F : 0F;
            previousGear = gear;
            door = driveable.isDoorOpen() ? 1F : 0F;
            previousDoor = door;
            mode = driveable.isWingFolded() || driveable.getDriveableMode() != 0 ? 1F : 0F;
            previousMode = mode;
            if (driveable instanceof VehicleVisual vehicle)
            {
                leftTrack = previousLeftTrack = vehicle.getLeftTrackProgress();
                rightTrack = previousRightTrack = vehicle.getRightTrackProgress();
                if (type instanceof VehicleType vehicleType)
                    trackLinks.advance(vehicle, vehicleType, 1);
            }
            if (driveable instanceof MechaVisual mecha)
            {
                legSwing = previousLegSwing = mecha.getLegSwing();
                updateLegTargets(type, legSwing, 1);
            }
            snapTransforms(driveable, type);
            if (driveable instanceof Driveable live)
                updateHandGunAnimations(live, 1);
            lastTick = driveable.getVisualTick();
            return;
        }
        if (lastTick == driveable.getVisualTick())
            return;

        int elapsed = Mth.clamp(driveable.getVisualTick() - lastTick, 1, 5);
        lastTick = driveable.getVisualTick();
        previousThrottle = throttle;
        previousSteering = steering;
        previousGear = gear;
        previousDoor = door;
        previousMode = mode;
        previousLeftTrack = leftTrack;
        previousRightTrack = rightTrack;
        previousLegSwing = legSwing;

        float amount = TRANSITION_PER_TICK * elapsed;
        throttle = approach(throttle, driveable.getThrottle(), amount);
        steering = approach(steering, targetSteering(driveable.getInputMask()), amount * 90F);
        gear = approach(gear, driveable.isGearDeployed() ? 1F : 0F, amount);
        door = approach(door, driveable.isDoorOpen() ? 1F : 0F, amount);
        mode = approach(mode, driveable.isWingFolded() || driveable.getDriveableMode() != 0 ? 1F : 0F, amount);
        if (driveable instanceof VehicleVisual vehicle)
        {
            leftTrack = vehicle.getLeftTrackProgress();
            rightTrack = vehicle.getRightTrackProgress();
            if (type instanceof VehicleType vehicleType)
                trackLinks.advance(vehicle, vehicleType, elapsed);
        }
        if (driveable instanceof MechaVisual mecha)
        {
            legSwing = mecha.getLegSwing();
            updateLegTargets(type, legSwing, elapsed);
        }
        advanceTransforms(driveable, type, elapsed);
        if (driveable instanceof Driveable live)
            updateHandGunAnimations(live, elapsed);
    }

    private void updateHandGunAnimations(Driveable driveable, int elapsed)
    {
        DriveableData data = driveable.getDriveableData();
        if (data == null)
            return;
        HandAnimationState left = updateHandGunAnimation(data.getMechaAddon(EnumMechaSlotType.LEFT_TOOL), leftGunItem, leftGunRounds, leftGunAnimations,
            DriveableInput.isDown(driveable.getInputMask(), DriveableInput.PRIMARY_FIRE), elapsed, driveable);
        leftGunItem = left.item();
        leftGunRounds = left.rounds();
        HandAnimationState right = updateHandGunAnimation(data.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL), rightGunItem, rightGunRounds, rightGunAnimations,
            DriveableInput.isDown(driveable.getInputMask(), DriveableInput.SECONDARY_FIRE), elapsed, driveable);
        rightGunItem = right.item();
        rightGunRounds = right.rounds();
    }

    private static HandAnimationState updateHandGunAnimation(ItemStack stack, GunItem previousItem, int previousRounds, GunAnimations animations, boolean active, int elapsed, Driveable driveable)
    {
        for (int tick = 0; tick < elapsed; tick++)
            animations.update();
        if (!(stack.getItem() instanceof GunItem gunItem))
            return new HandAnimationState(null, -1);

        int rounds = 0;
        for (int slot = 0; slot < gunItem.getConfigType().getNumAmmoItemsInGun(stack); slot++)
            rounds += ShootableItem.getRoundsRemaining(gunItem.getAmmoItemStack(stack, slot, driveable.level().registryAccess()));
        if (previousItem == gunItem && previousRounds >= 0 && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun model)
        {
            if (rounds < previousRounds)
                animations.doShoot(model.getPumpDelay(), model.getPumpTime(), model.getHammerDelay(), model.getHammerAngle(), model.getAlthammerAngle(), model.getCasingDelay());
            else if (rounds > previousRounds)
                animations.doReload(Math.max(1F, gunItem.getActualReloadTime(stack, driveable.level().registryAccess(), ItemStack.EMPTY)), model.getPumpDelayAfterReload(), model.getPumpTime(),
                    model.getChargeDelayAfterReload(), model.getChargeTime(), 1, false);
        }
        if (active)
            animations.addMinigunBarrelRotationSpeed(0.2F * elapsed);
        return new HandAnimationState(gunItem, rounds);
    }

    private record HandAnimationState(GunItem item, int rounds)
    {}

    private void snapTransforms(DriveableVisual driveable, DriveableType type)
    {
        if (type instanceof PlaneType planeType)
        {
            boolean folded = driveable.isWingFolded();
            boolean gearDeployed = driveable.isGearDeployed();
            boolean doorOpen = driveable.isDoorOpen();
            wingTransform.snap(folded ? planeType.getWingPos2() : planeType.getWingPos1(), folded ? planeType.getWingRot2() : planeType.getWingRot1());
            wingWheelTransform.snap(gearDeployed ? planeType.getWingWheelPos1() : planeType.getWingWheelPos2(), gearDeployed ? planeType.getWingWheelRot1() : planeType.getWingWheelRot2());
            bodyWheelTransform.snap(gearDeployed ? planeType.getBodyWheelPos1() : planeType.getBodyWheelPos2(), gearDeployed ? planeType.getBodyWheelRot1() : planeType.getBodyWheelRot2());
            tailWheelTransform.snap(gearDeployed ? planeType.getTailWheelPos1() : planeType.getTailWheelPos2(), gearDeployed ? planeType.getTailWheelRot1() : planeType.getTailWheelRot2());
            doorTransform.snap(doorOpen ? planeType.getDoorPos2() : planeType.getDoorPos1(), doorOpen ? planeType.getDoorRot2() : planeType.getDoorRot1());
        }
        else if (type instanceof VehicleType vehicleType)
        {
            boolean doorOpen = driveable.isDoorOpen();
            doorTransform.snap(doorOpen ? vehicleType.getDoorPos2() : vehicleType.getDoorPos1(), doorOpen ? vehicleType.getDoorRot2() : vehicleType.getDoorRot1());
            door2Transform.snap(doorOpen ? vehicleType.getDoor2Pos2() : vehicleType.getDoor2Pos1(), doorOpen ? vehicleType.getDoor2Rot2() : vehicleType.getDoor2Rot1());
        }
    }

    private void advanceTransforms(DriveableVisual driveable, DriveableType type, int elapsed)
    {
        if (type instanceof PlaneType planeType)
        {
            boolean folded = driveable.isWingFolded();
            boolean gearDeployed = driveable.isGearDeployed();
            boolean doorOpen = driveable.isDoorOpen();
            wingTransform.advance(folded ? planeType.getWingPos2() : planeType.getWingPos1(), folded ? planeType.getWingRot2() : planeType.getWingRot1(), planeType.getWingRate(),
                planeType.getWingRotRate(), elapsed);
            wingWheelTransform.advance(gearDeployed ? planeType.getWingWheelPos1() : planeType.getWingWheelPos2(), gearDeployed ? planeType.getWingWheelRot1() : planeType.getWingWheelRot2(),
                planeType.getWingWheelRate(), planeType.getWingWheelRotRate(), elapsed);
            bodyWheelTransform.advance(gearDeployed ? planeType.getBodyWheelPos1() : planeType.getBodyWheelPos2(), gearDeployed ? planeType.getBodyWheelRot1() : planeType.getBodyWheelRot2(),
                planeType.getBodyWheelRate(), planeType.getBodyWheelRotRate(), elapsed);
            tailWheelTransform.advance(gearDeployed ? planeType.getTailWheelPos1() : planeType.getTailWheelPos2(), gearDeployed ? planeType.getTailWheelRot1() : planeType.getTailWheelRot2(),
                planeType.getTailWheelRate(), planeType.getTailWheelRotRate(), elapsed);
            doorTransform.advance(doorOpen ? planeType.getDoorPos2() : planeType.getDoorPos1(), doorOpen ? planeType.getDoorRot2() : planeType.getDoorRot1(), planeType.getDoorRate(),
                planeType.getDoorRotRate(), elapsed);
        }
        else if (type instanceof VehicleType vehicleType)
        {
            boolean doorOpen = driveable.isDoorOpen();
            doorTransform.advance(doorOpen ? vehicleType.getDoorPos2() : vehicleType.getDoorPos1(), doorOpen ? vehicleType.getDoorRot2() : vehicleType.getDoorRot1(), vehicleType.getDoorRate(),
                vehicleType.getDoorRotRate(), elapsed);
            door2Transform.advance(doorOpen ? vehicleType.getDoor2Pos2() : vehicleType.getDoor2Pos1(), doorOpen ? vehicleType.getDoor2Rot2() : vehicleType.getDoor2Rot1(), vehicleType.getDoor2Rate(),
                vehicleType.getDoor2RotRate(), elapsed);
        }
    }

    private void updateLegTargets(DriveableType type, float phase, int elapsed)
    {
        if (!(type instanceof MechaType mechaType))
            return;
        legAnimation.beginTick();
        for (MechaType.LegNode node : mechaType.getLegNodes())
        {
            if (node != null && phase >= node.lowerBound() && phase <= node.upperBound())
                legAnimation.setTarget(node.legPart() - 1, node.rotation(), node.speed());
        }
        legAnimation.approachTargets(elapsed);
    }

    private static float targetSteering(int inputMask)
    {
        return ((DriveableInput.isDown(inputMask, DriveableInput.RIGHT) ? 1F : 0F) - (DriveableInput.isDown(inputMask, DriveableInput.LEFT) ? 1F : 0F)) * 20F;
    }

    /**
     * @param yaw
     *            interpolated hull yaw, which a mecha's legs fall back to
     * @return the interpolated pose the model draws this frame
     */
    public ModelDriveable.RenderState state(DriveableVisual source, float partialTick, float yaw, float pitch, float roll)
    {
        float turretYaw = Mth.rotLerp(partialTick, source.getPrevTurretYaw(), source.getTurretYaw());
        float turretPitch = Mth.rotLerp(partialTick, source.getPrevTurretPitch(), source.getTurretPitch());
        float steeringAngle = Mth.lerp(partialTick, previousSteering, steering);
        float wheelAngle = 0F;
        float leftTrackProgress = 0F;
        float rightTrackProgress = 0F;
        if (source instanceof VehicleVisual vehicle)
        {
            wheelAngle = Mth.rotLerp(partialTick, vehicle.getPrevWheelAngle(), vehicle.getWheelAngle()) * Mth.DEG_TO_RAD;
            steeringAngle = Mth.rotLerp(partialTick, vehicle.getPrevWheelYaw(), vehicle.getWheelYaw());
            leftTrackProgress = wrappedLerp(partialTick, previousLeftTrack, leftTrack);
            rightTrackProgress = wrappedLerp(partialTick, previousRightTrack, rightTrack);
        }
        float swing = source instanceof MechaVisual ? wrappedLerp(partialTick, previousLegSwing, legSwing) : 0F;
        float legYaw = source instanceof MechaVisual mecha ? Mth.rotLerp(partialTick, mecha.getPrevLegYaw(), mecha.getLegYaw()) : yaw;
        return new ModelDriveable.RenderState(partialTick, yaw, pitch, roll, Mth.lerp(partialTick, previousThrottle, throttle), turretYaw, turretPitch, wheelAngle, steeringAngle,
            source.getVisualTick() + partialTick, Mth.lerp(partialTick, previousGear, gear), Mth.lerp(partialTick, previousDoor, door), Mth.lerp(partialTick, previousMode, mode), leftTrackProgress,
            rightTrackProgress, swing, legYaw, wingTransform, wingWheelTransform, bodyWheelTransform, tailWheelTransform, doorTransform, door2Transform, legAnimation, source.getInputMask(),
            source.getDriveableMode(), source.isVarFlare(), trackLinks);
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(value + amount, target) : Math.max(value - amount, target);
    }

    private static float wrappedLerp(float partialTick, float previous, float current)
    {
        float delta = current - previous;
        if (delta > 0.5F)
            delta -= 1F;
        else if (delta < -0.5F)
            delta += 1F;
        float value = previous + Mth.clamp(partialTick, 0F, 1F) * delta;
        return value - Mth.floor(value);
    }
}
