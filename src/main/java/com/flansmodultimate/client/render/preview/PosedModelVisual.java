package com.flansmodultimate.client.render.preview;

import com.flansmodultimate.api.MountAim;
import com.flansmodultimate.api.client.ModelPose;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.common.driveables.visual.*;
import com.flansmodultimate.common.types.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;

import java.util.List;

/**
 * Stands in for a driveable or AA gun entity when a model is drawn for something else. It turns the owner's
 * {@link ModelPose}, one per game tick, into the state a live entity would have: previous and current aim, wheel and
 * track phases, propeller angle, leg swing and recoil. Render thread only.
 */
public final class PosedModelVisual implements VehicleVisual, PlaneVisual, MechaVisual, AAGunVisual
{
    /** Throttle a running engine idles at for its propeller, as an aircraft entity does. */
    private static final float IDLE_ENGINE_THROTTLE = 0.08F;
    private static final float MAX_STEERING = 25F;
    private static final float STEERING_PER_TICK = 5F;
    /** Ticks a mounted minigun keeps spinning after a shot. */
    private static final int FIRING_TICKS = 4;
    /** Travel, in blocks per tick, at which mecha legs swing at their authored pace. */
    private static final float MECHA_STRIDE = 0.1F;

    @Nullable
    private InfoType type;
    private ModelPose pose = ModelPose.REST;
    private int lastTick = Integer.MIN_VALUE;
    private int shots;
    private int firingTicks;
    private float turretYaw;
    private float prevTurretYaw;
    private float turretPitch;
    private float prevTurretPitch;
    private Seat[] seats = new Seat[0];
    private float wheelAngle;
    private float prevWheelAngle;
    private float wheelYaw;
    private float prevWheelYaw;
    private float leftTrackProgress;
    private float rightTrackProgress;
    private float leftTrackStep;
    private float rightTrackStep;
    private float propellerAngle;
    private float prevPropellerAngle;
    private double rotorPhase;
    private double previousRotorPhase;
    private float rotorSpeed;
    private float legSwing;
    private int recoilDuration;
    private int recoilTicksRemaining;
    private float recoilProgress;
    private float[] barrelRecoil = new float[0];

    /**
     * Takes the owner's pose for the given game tick. Calls for a tick already seen change nothing, so this is safe to
     * call every frame.
     */
    public void update(InfoType newType, int tick, ModelPose newPose)
    {
        boolean first = type != newType || lastTick == Integer.MIN_VALUE;
        if (!first && tick == lastTick)
            return;
        if (first)
            reset(newType, newPose);
        lastTick = tick;
        pose = newPose;
        stepAim(first);
        stepShots(first);
        if (newType instanceof VehicleType vehicle)
            stepVehicle(vehicle);
        else if (newType instanceof PlaneType)
            stepPlane();
        else if (newType instanceof MechaType mecha)
            stepMecha(mecha);
    }

    private void reset(InfoType newType, ModelPose newPose)
    {
        type = newType;
        shots = newPose.shots();
        firingTicks = 0;
        recoilTicksRemaining = 0;
        recoilProgress = 0F;
        wheelYaw = prevWheelYaw = 0F;
        legSwing = 0F;
        barrelRecoil = new float[newType instanceof AAGunType aa ? aa.getNumBarrels() : 0];
        int seatCount = 0;
        if (newType instanceof DriveableType driveable)
        {
            for (SeatInfo info : driveable.getSeats())
            {
                if (info != null)
                    seatCount = Math.max(seatCount, info.getId() + 1);
            }
        }
        seats = new Seat[seatCount];
        for (int index = 0; index < seatCount; index++)
        {
            SeatInfo info = ((DriveableType) newType).getSeat(index);
            if (info != null)
                seats[index] = new Seat(index, info);
        }
    }

    private void stepAim(boolean snap)
    {
        prevTurretYaw = turretYaw;
        prevTurretPitch = turretPitch;
        turretYaw = pose.turret().yaw();
        turretPitch = pose.turret().pitch();
        if (snap)
        {
            prevTurretYaw = turretYaw;
            prevTurretPitch = turretPitch;
        }
        List<MountAim> aims = pose.seats();
        for (Seat seat : seats)
        {
            if (seat != null)
                seat.step(seat.index < aims.size() ? aims.get(seat.index) : null, snap);
        }
    }

    private void stepShots(boolean snap)
    {
        if (firingTicks > 0)
            firingTicks--;
        for (int barrel = 0; barrel < barrelRecoil.length; barrel++)
            barrelRecoil[barrel] *= 0.9F;
        if (recoilTicksRemaining > 0 && recoilDuration > 0)
        {
            recoilProgress = Mth.clamp((float) (recoilDuration - recoilTicksRemaining + 1) / recoilDuration, 0F, 1F);
            recoilTicksRemaining--;
        }
        else
            recoilProgress = 0F;
        if (pose.shots() == shots)
            return;
        shots = pose.shots();
        if (snap)
            return;
        firingTicks = FIRING_TICKS;
        if (type instanceof AAGunType aa && barrelRecoil.length > 0)
            barrelRecoil[pose.barrel() % barrelRecoil.length] = aa.getRecoil();
        else if (type instanceof DriveableType driveable && driveable.getRecoilTime() > 0F)
        {
            recoilDuration = Math.max(1, Mth.ceil(driveable.getRecoilTime()));
            recoilTicksRemaining = recoilDuration;
            recoilProgress = 0F;
        }
    }

    private void stepVehicle(VehicleType vehicle)
    {
        float travel = pose.travel();
        prevWheelAngle = wheelAngle;
        if (vehicle.isRotateWheels() || vehicle.isTank())
            wheelAngle = Mth.wrapDegrees(wheelAngle + WheelAnimationPhysics.angularStepDegrees(travel, 0D, 1D, 0D));
        prevWheelYaw = wheelYaw;
        float steering = Math.abs(travel) < 1.0E-3F ? 0F : Mth.clamp(pose.turn() * 4F, -MAX_STEERING, MAX_STEERING);
        wheelYaw = wheelYaw < steering ? Math.min(wheelYaw + STEERING_PER_TICK, steering) : Math.max(wheelYaw - STEERING_PER_TICK, steering);
        float trackTravel = TrackAnimationPhysics.travelStep(travel, 0D, 1D, 0D);
        if (vehicle.isTank())
        {
            VehicleGeometry geometry = vehicle.getResolvedPhysics().geometry();
            Float width = geometry.trackWidthM() != null ? geometry.trackWidthM() : geometry.widthM();
            TrackAnimationPhysics.Steps steps = TrackAnimationPhysics.steps(trackTravel, pose.turn(), width == null ? 1D : width, true, true);
            leftTrackStep = steps.left();
            rightTrackStep = steps.right();
        }
        else
        {
            // Walkers keep their authored throttle-based leg timing, as the vehicle entity does.
            leftTrackStep = pose.throttle() * 0.075F - wheelYaw * 0.0025F;
            rightTrackStep = pose.throttle() * 0.075F + wheelYaw * 0.0025F;
        }
        leftTrackProgress += leftTrackStep;
        rightTrackProgress += rightTrackStep;
        leftTrackProgress -= Mth.floor(leftTrackProgress);
        rightTrackProgress -= Mth.floor(rightTrackProgress);
    }

    private void stepPlane()
    {
        prevPropellerAngle = propellerAngle;
        float throttle = LegacyPlanePhysics.engineAnimationThrottle(pose.engineRunning(), pose.throttle(), IDLE_ENGINE_THROTTLE);
        propellerAngle = Mth.wrapDegrees(propellerAngle + LegacyPlanePhysics.propellerStep(throttle));
        rotorSpeed = HelicopterPhysics.spool(rotorSpeed, pose.engineRunning() ? Mth.lerp(Mth.clamp(pose.throttle(), 0F, 1F), 0.6F, 1F) : 0F);
        previousRotorPhase = rotorPhase;
        rotorPhase += LegacyPlanePhysics.rotorStep(rotorSpeed);
    }

    private void stepMecha(MechaType mecha)
    {
        float pace = Math.abs(pose.travel()) / MECHA_STRIDE;
        if (pace < 0.05F)
        {
            legSwing *= 0.75F;
            return;
        }
        float increment = mecha.getLegAnimSpeed() > 0F ? mecha.getLegAnimSpeed() : 0.12F;
        legSwing += increment * Mth.clamp(pace, 0.25F, 2F);
        if (legSwing > 1F)
            legSwing -= Mth.floor(legSwing);
    }

    // Driveable

    @Nullable
    @Override
    public DriveableType getConfigType()
    {
        return type instanceof DriveableType driveable ? driveable : null;
    }

    @Override
    public boolean isPartIntact(@Nullable EnumDriveablePart part)
    {
        return true;
    }

    @Nullable
    @Override
    public SeatVisual getSeat(int index)
    {
        return index >= 0 && index < seats.length ? seats[index] : null;
    }

    @Nullable
    @Override
    public DriveableData getDriveableData()
    {
        return null;
    }

    @Override
    public float getTurretYaw()
    {
        return turretYaw;
    }

    @Override
    public float getPrevTurretYaw()
    {
        return prevTurretYaw;
    }

    @Override
    public float getTurretPitch()
    {
        return turretPitch;
    }

    @Override
    public float getPrevTurretPitch()
    {
        return prevTurretPitch;
    }

    @Override
    public float getRecoilProgress()
    {
        return recoilProgress;
    }

    @Override
    public float getThrottle()
    {
        return pose.throttle();
    }

    @Override
    public int getInputMask()
    {
        return firingTicks > 0 ? DriveableInput.PRIMARY_FIRE : 0;
    }

    @Override
    public int getDriveableMode()
    {
        return 0;
    }

    @Override
    public boolean isGearDeployed()
    {
        return pose.gearDeployed();
    }

    @Override
    public boolean isDoorOpen()
    {
        return pose.doorsOpen();
    }

    @Override
    public boolean isWingFolded()
    {
        return pose.wingsFolded();
    }

    @Override
    public boolean isVarFlare()
    {
        return false;
    }

    @Override
    public int getVisualTick()
    {
        return lastTick;
    }

    // Vehicle

    @Override
    public float getWheelAngle()
    {
        return wheelAngle;
    }

    @Override
    public float getPrevWheelAngle()
    {
        return prevWheelAngle;
    }

    @Override
    public float getWheelYaw()
    {
        return wheelYaw;
    }

    @Override
    public float getPrevWheelYaw()
    {
        return prevWheelYaw;
    }

    @Override
    public float getLeftTrackProgress()
    {
        return leftTrackProgress;
    }

    @Override
    public float getRightTrackProgress()
    {
        return rightTrackProgress;
    }

    @Override
    public float getLeftTrackStep()
    {
        return leftTrackStep;
    }

    @Override
    public float getRightTrackStep()
    {
        return rightTrackStep;
    }

    // Plane

    @Override
    public EnumPlaneMode getPlaneMode()
    {
        if (!(type instanceof PlaneType plane))
            return EnumPlaneMode.PLANE;
        // A tilt-rotor stands in its hover mode while its gear is down.
        if (plane.getMode() == EnumPlaneMode.VTOL)
            return pose.gearDeployed() ? EnumPlaneMode.HELI : EnumPlaneMode.PLANE;
        return plane.getMode();
    }

    @Override
    public float getPropellerAngle()
    {
        return propellerAngle;
    }

    @Override
    public float getPrevPropellerAngle()
    {
        return prevPropellerAngle;
    }

    @Override
    public float getRotorRenderAngle(float partialTick, float speedRatio)
    {
        return HelicopterPhysics.rotorAngle(previousRotorPhase, rotorPhase, partialTick, speedRatio);
    }

    @Override
    public float getFlapYaw()
    {
        return 0F;
    }

    @Override
    public float getPrevFlapYaw()
    {
        return 0F;
    }

    @Override
    public float getFlapPitchLeft()
    {
        return 0F;
    }

    @Override
    public float getPrevFlapPitchLeft()
    {
        return 0F;
    }

    @Override
    public float getFlapPitchRight()
    {
        return 0F;
    }

    @Override
    public float getPrevFlapPitchRight()
    {
        return 0F;
    }

    @Nullable
    @Override
    public ValkyrieAnimation getValkyrieAnimation()
    {
        return null;
    }

    // Mecha

    @Override
    public float getLegSwing()
    {
        return legSwing;
    }

    @Override
    public float getLegYaw()
    {
        return 0F;
    }

    @Override
    public float getPrevLegYaw()
    {
        return 0F;
    }

    // AA gun

    // The model only reads the array while drawing, on this thread.
    @SuppressWarnings("java:S2384")
    @Override
    public float[] getBarrelRecoil()
    {
        return barrelRecoil;
    }

    @Override
    public boolean hasAmmo(int barrelIndex)
    {
        return true;
    }

    private final class Seat implements SeatVisual
    {
        private final int index;
        private final SeatInfo info;
        private float yaw;
        private float prevYaw;
        private float pitch;
        private float prevPitch;

        private Seat(int index, SeatInfo info)
        {
            this.index = index;
            this.info = info;
        }

        private void step(@Nullable MountAim aim, boolean snap)
        {
            prevYaw = yaw;
            prevPitch = pitch;
            if (aim != null)
            {
                yaw = aim.yaw();
                pitch = aim.pitch();
            }
            else if (isDriverSeat())
            {
                yaw = turretYaw;
                pitch = turretPitch;
            }
            else
            {
                yaw = info.getYawCentre();
                pitch = 0F;
            }
            if (snap)
            {
                prevYaw = yaw;
                prevPitch = pitch;
            }
        }

        @Override
        public float getAimYaw()
        {
            return yaw;
        }

        @Override
        public float getPrevAimYaw()
        {
            return prevYaw;
        }

        @Override
        public float getAimPitch()
        {
            return pitch;
        }

        @Override
        public float getPrevAimPitch()
        {
            return prevPitch;
        }

        @Override
        public boolean isDriverSeat()
        {
            return info.isDriver();
        }

        @Override
        public boolean isInputDown(int input)
        {
            return DriveableInput.isDown(getInputMask(), input);
        }
    }
}
