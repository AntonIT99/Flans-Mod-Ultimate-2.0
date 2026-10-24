package com.flansmodultimate.common.types;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.common.driveables.physics.GroundPropulsionPhysics;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsConstants;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.config.ModCommonConfig;

import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Reads a vehicle definition's longitudinal response the way {@code Vehicle} applies it to a driven vehicle, without
 * creating one: the derived power model under a complete real-world profile, otherwise the legacy throttle ramp, grip and
 * drag. Engine parts are taken at their reference values.
 */
public final class GroundPropulsionProfiles
{
    /** Share of the gap to its wanted velocity a grounded legacy vehicle closes each tick. */
    private static final double LEGACY_GRIP = 0.22D;
    /** Speed factors the legacy model applies to the throttle of tracked and wheeled vehicles. */
    private static final double LEGACY_TRACKED_FACTOR = 0.26D;
    private static final double LEGACY_WHEELED_FACTOR = 0.32D;
    /** Throttle a legacy pedal gains per tick, and the engine power assumed without an engine part. */
    private static final double LEGACY_THROTTLE_STEP = 0.01D;
    private static final double LEGACY_REFERENCE_ENGINE_POWER = 10D;
    private static final double LEGACY_MIN_REALISTIC_STEP = 0.0005D;

    private GroundPropulsionProfiles()
    {}

    public static Optional<GroundPropulsion> of(IContentType type)
    {
        if (!(type instanceof VehicleType vehicle))
            return Optional.empty();
        boolean legacy = ModCommonConfig.forceLegacyVehiclePhysics();
        double cap = legacy ? Double.POSITIVE_INFINITY : VehiclePhysicsUnits.kmhToBlocksPerTick(ModCommonConfig.maxVehicleSpeedKmh());
        if (cap <= 0D)
            cap = Double.POSITIVE_INFINITY;
        ResolvedVehiclePhysics physics = vehicle.getResolvedPhysics();
        boolean pushed = vehicle.getPushSpeedKmh() > 0F;
        GroundPropulsion result = !pushed && !legacy && physics.hasGroundPropulsion() ? derived(vehicle, physics, cap) : legacy(vehicle, physics, legacy, pushed, cap);
        return result.topSpeed() > 0D && Double.isFinite(result.topSpeed()) ? Optional.of(result) : Optional.empty();
    }

    private static GroundPropulsion derived(VehicleType vehicle, ResolvedVehiclePhysics physics, double cap)
    {
        double scale = ModCommonConfig.realisticSpeedScale(physics.category());
        double terminal = physics.maxSpeedBlocksPerTick(scale);
        double reverse = 0D;
        if (vehicle.getMaxNegativeThrottle() > 0F)
        {
            reverse = physics.hasReverseSpeedOverride()
                ? physics.reverseSpeedBlocksPerTick(scale)
                : terminal * Math.min(1D, vehicle.getMaxNegativeThrottle() / Math.max(1.0E-4D, vehicle.getMaxThrottle()));
        }
        return new Derived(Math.min(terminal, cap), Math.min(reverse, cap), terminal, physics.effectivePowerWatts(1F), physics.massKg(), physics.driveType().tractionFactor(),
            physics.baselinePowerKw(), vehicle.isTracked());
    }

    private static GroundPropulsion legacy(VehicleType vehicle, ResolvedVehiclePhysics physics, boolean forceLegacy, boolean pushed, double cap)
    {
        double drag = GroundPropulsionPhysics.postIntegrationHorizontalDrag(pushed, vehicle.getDrag());
        // The velocity closes a share of the gap to its target each tick and then loses its drag, so it settles below the target.
        double response = 1D - drag * (1D - LEGACY_GRIP);
        double settle = LEGACY_GRIP * drag / response;
        double factor = vehicle.isTracked() ? LEGACY_TRACKED_FACTOR : LEGACY_WHEELED_FACTOR;
        double forward;
        double reverse;
        if (pushed)
        {
            forward = VehiclePhysicsUnits.kmhToBlocksPerTick(vehicle.getPushSpeedKmh());
            reverse = vehicle.getMaxNegativeThrottle() > 0F ? forward : 0D;
        }
        else
        {
            forward = Math.max(0F, vehicle.getMaxThrottle()) * factor;
            reverse = Math.max(0F, vehicle.getMaxNegativeThrottle()) * factor;
            if (!forceLegacy && physics.hasReverseSpeedOverride())
                reverse = Math.min(reverse, physics.reverseSpeedBlocksPerTick(ModCommonConfig.realisticSpeedScale(physics.category())));
        }
        double step = vehicle.isUseRealisticAcceleration() ? Math.max(LEGACY_MIN_REALISTIC_STEP, LEGACY_REFERENCE_ENGINE_POWER / Math.max(1F, vehicle.getMass())) : LEGACY_THROTTLE_STEP;
        double decay = vehicle.getThrottleDecay() * ModCommonConfig.vehicleThrottleDecayMultiplier();
        OptionalDouble mass = vehicle.isUseRealisticAcceleration() ? OptionalDouble.of(Math.max(1F, vehicle.getMass())) : OptionalDouble.empty();
        return new Legacy(Math.min(forward * settle, cap), Math.min(reverse * settle, cap), response, step, Math.max(0F, vehicle.getBrakingModifier()), decay, mass, vehicle.isTracked());
    }

    /** Lowest deceleration of a grounded vehicle: rolling resistance off the pedals, locked wheels or tracks under the brake. */
    private static double floor(boolean tracked, double brake)
    {
        double idle = tracked ? VehiclePhysicsConstants.TRACKED_IDLE_DECELERATION_MS2 : VehiclePhysicsConstants.WHEELED_IDLE_DECELERATION_MS2;
        return VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(Mth.lerp(brake, idle, VehiclePhysicsConstants.PARKED_GROUND_FRICTION_DECELERATION_MS2));
    }

    private static double brakeShare(double brake)
    {
        return Double.isFinite(brake) ? Mth.clamp(brake, 0D, 1D) : 0D;
    }

    private static double magnitude(double speed)
    {
        return Double.isFinite(speed) ? Math.abs(speed) : 0D;
    }

    /**
     * @param terminal
     *            the profile's own top speed, which calibrates its resistance even when the speed cap lowers the top speed
     */
    private record Derived(double topSpeed, double reverseSpeed, double terminal, double powerWatts, double mass, double traction, double powerKw, boolean tracked) implements GroundPropulsion
    {
        @Override
        public boolean realistic()
        {
            return true;
        }

        @Override
        public OptionalDouble massKg()
        {
            return OptionalDouble.of(mass);
        }

        @Override
        public OptionalDouble enginePowerKw()
        {
            return OptionalDouble.of(powerKw);
        }

        @Override
        public double acceleration(double speed)
        {
            double current = magnitude(speed);
            return current >= topSpeed ? 0D : GroundPropulsionPhysics.accelerationBlocksPerTickSquared(current, powerWatts, mass, terminal, traction);
        }

        @Override
        public double deceleration(double speed, double brake)
        {
            double share = brakeShare(brake);
            return Math.max(GroundPropulsionPhysics.decelerationBlocksPerTickSquared(magnitude(speed), powerWatts, mass, terminal, share), floor(tracked, share));
        }
    }

    /**
     * @param response
     *            share of the gap to its settled speed the vehicle closes each tick
     * @param step
     *            throttle the pedal gains per tick
     * @param brakingModifier
     *            the pedal's rate against the direction of travel, relative to its forward rate
     * @param decay
     *            throttle shed per tick off the pedals
     */
    private record Legacy(double topSpeed, double reverseSpeed, double response, double step, double brakingModifier, double decay, OptionalDouble massKg, boolean tracked) implements GroundPropulsion
    {
        @Override
        public boolean realistic()
        {
            return false;
        }

        @Override
        public OptionalDouble enginePowerKw()
        {
            return OptionalDouble.empty();
        }

        @Override
        public double acceleration(double speed)
        {
            // The pedal raises the demand at a fixed rate, and the vehicle lags behind the demand by its grip.
            return Math.max(0D, Math.min(step * topSpeed, response * (topSpeed - magnitude(speed))));
        }

        @Override
        public double deceleration(double speed, double brake)
        {
            double share = brakeShare(brake);
            // Off the pedals the demand decays; the brake drops it at once, and the grip then pulls the vehicle to a halt.
            double coasting = Math.max(decay * topSpeed, floor(tracked, 0D));
            double braking = Math.max(Math.max(response * magnitude(speed), step * brakingModifier * topSpeed), floor(tracked, 1D));
            return Mth.lerp(share, coasting, braking);
        }
    }
}
