package com.flansmodultimate.common.types;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.FlightPerformance;
import com.flansmodultimate.api.driveables.MechaMobility;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.physics.HelicopterPhysics;
import com.flansmodultimate.common.driveables.physics.MechaPhysics;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.config.ModCommonConfig;

import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Reads how a mecha walks, how high a driveable steps and how an aircraft flies from their definitions, the way the
 * driveable entities apply them, without creating one. Engine parts and addons are taken at their reference values.
 */
public final class DriveableMobilityProfiles
{
    /** Sentinel {@code JumpHeight} of a mecha definition that authors none. */
    private static final float NO_JUMP_HEIGHT = -99F;
    /** Highest step a vehicle and a mecha climb, as their entities clamp it. */
    private static final double MAX_VEHICLE_STEP = 2.5D;
    private static final double MAX_MECHA_STEP = 8D;
    /** Slowest top speed the legacy flight model lets an aircraft settle at, and the cap it keeps without NewFlightControl. */
    private static final double LEGACY_MIN_TOP_SPEED = 0.2D;
    private static final double LEGACY_SPEED_CAP = 2D;
    private static final double SUPERSONIC_SHARE = 1.5D;

    private DriveableMobilityProfiles()
    {}

    /** @return how the mecha walks, or empty for any other definition */
    public static Optional<MechaMobility> mecha(IContentType type)
    {
        if (!(type instanceof MechaType mecha))
            return Optional.empty();
        double speed = MechaPhysics.movementSpeed(mecha.getMoveSpeed(), mecha.getRealWorldSpec().maxSpeedKmh(), 1F, 1F);
        float jump = mecha.getJumpHeight();
        OptionalDouble jumpHeight = jump != NO_JUMP_HEIGHT && Float.isFinite(jump) ? OptionalDouble.of(Math.max(0F, jump)) : OptionalDouble.empty();
        return Optional.of(new MechaMobility(Math.max(0D, speed), jumpHeight));
    }

    /** @return the highest step a vehicle's wheels or a mecha's legs climb without jumping, or empty for any other definition */
    public static OptionalDouble stepHeight(IContentType type)
    {
        if (type instanceof VehicleType vehicle)
            return OptionalDouble.of(Mth.clamp(vehicle.getWheelStepHeight(), 0D, MAX_VEHICLE_STEP));
        if (type instanceof MechaType mecha)
            return OptionalDouble.of(Mth.clamp(mecha.getStepHeight(), 0D, MAX_MECHA_STEP));
        return OptionalDouble.empty();
    }

    /** @return how the aircraft flies, or empty for anything but a plane or helicopter with a usable top speed */
    public static Optional<FlightPerformance> flight(IContentType type)
    {
        if (!(type instanceof PlaneType plane) || plane.getMode() == EnumPlaneMode.SIXDOF)
            return Optional.empty();
        boolean legacy = ModCommonConfig.forceLegacyPlanePhysics();
        boolean rotorcraft = plane.getMode() == EnumPlaneMode.HELI || plane.getMode() == EnumPlaneMode.VTOL;
        Speeds speeds = rotorcraft ? rotorcraft(plane, legacy) : fixedWing(plane, legacy);
        double top = speeds.top();
        if (!legacy)
            top *= VehiclePhysicsUnits.speedCapScale(top, ModCommonConfig.maxPlaneSpeedKmh());
        if (top <= 0D || !Double.isFinite(top))
            return Optional.empty();
        return Optional.of(new FlightPerformance(top, Math.min(speeds.minimum(), top), speeds.climb(), authority(plane.getLookUpModifier(), plane.getLookDownModifier()),
            authority(plane.getRollLeftModifier(), plane.getRollRightModifier()), rotorcraft, speeds.realistic()));
    }

    /** The speeds a flight model resolves to, before the plane speed cap. */
    private record Speeds(double top, double minimum, OptionalDouble climb, boolean realistic)
    {}

    /** A rotorcraft flies at its real-world top speed and climb rate as its rotor model resolves them, and hovers. */
    private static Speeds rotorcraft(PlaneType plane, boolean legacy)
    {
        ResolvedVehiclePhysics physics = plane.getResolvedPhysics();
        RealWorldVehicleSpec spec = legacy ? RealWorldVehicleSpec.EMPTY : physics.source();
        HelicopterPhysics.Performance performance = HelicopterPhysics.resolve(spec, 1F, 0F, plane.getDrag(), ModCommonConfig.realisticSpeedScale(physics.category()));
        boolean realistic = performance.terminalSpeed() > 0D;
        OptionalDouble climb = performance.climbSpeed() > 0D ? OptionalDouble.of(performance.climbSpeed()) : OptionalDouble.empty();
        return new Speeds(realistic ? performance.terminalSpeed() : legacyTopSpeed(plane), 0D, climb, realistic);
    }

    /** A fixed-wing aircraft flies at its real-world profile's speeds and climb rate, or else at its legacy MaxSpeed and TakeoffSpeed. */
    private static Speeds fixedWing(PlaneType plane, boolean legacy)
    {
        ResolvedVehiclePhysics physics = plane.getResolvedPhysics();
        if (legacy || !physics.hasAircraftProfile())
            return new Speeds(legacyTopSpeed(plane), Math.max(0F, plane.getTakeoffSpeed()), OptionalDouble.empty(), false);
        double scale = ModCommonConfig.realisticSpeedScale(physics.category());
        double minimum = VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(physics.referenceSpeedMs(scale, ModCommonConfig.realisticAircraftReferenceSpeedScale()), 1D);
        double rate = physics.climbRateBlocksPerTick(scale);
        return new Speeds(physics.maxSpeedBlocksPerTick(scale), minimum, rate > 0D ? OptionalDouble.of(rate) : OptionalDouble.empty(), true);
    }

    /** The legacy flight model gains speed until its drag caps it at MaxSpeed, never above two blocks a tick without NewFlightControl. */
    private static double legacyTopSpeed(PlaneType plane)
    {
        double top = Math.max(LEGACY_MIN_TOP_SPEED, plane.getMaxSpeed() * (plane.isSupersonic() ? SUPERSONIC_SHARE : 1D));
        return plane.isNewFlightControl() ? top : Math.min(top, LEGACY_SPEED_CAP);
    }

    private static double authority(float first, float second)
    {
        double mean = (finite(first) + finite(second)) * 0.5D;
        return mean > 0D ? mean : 1D;
    }

    private static double finite(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, value) : 0D;
    }
}
