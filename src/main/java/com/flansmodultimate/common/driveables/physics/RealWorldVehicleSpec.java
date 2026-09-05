package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

/**
 * Authored real-world source data, exactly as written in a content pack and in
 * the units the key names declare. Nothing here is scaled, clamped to gameplay
 * ranges, or converted to Minecraft units; that is {@link ResolvedVehiclePhysics}'
 * job.
 *
 * <p>Every field is nullable because every key is optional. A pack that declares
 * none of them produces {@link #EMPTY}, and {@link #isEmpty()} is what the
 * resolver uses to keep such a definition on the legacy path.
 *
 * <p>The category sub-records are never null, only empty, so that consuming code
 * does not have to null-check two levels deep.
 */
public record RealWorldVehicleSpec(
    @Nullable Float massKg,
    @Nullable Float maxSpeedKmh,
    @Nullable Float enginePowerKw,
    @Nullable Float engineThrustKn,
    Aircraft aircraft,
    Ground ground,
    Marine marine)
{
    /** Aircraft-only authored values. */
    public record Aircraft(
        @Nullable Float wingSpanM,
        @Nullable Float wingAreaM2,
        @Nullable Float climbRateMs,
        @Nullable Float rotorDiameterM,
        @Nullable Integer rotorCount)
    {
        public static final Aircraft EMPTY = new Aircraft(null, null, null, null, null);

        public Aircraft(@Nullable Float wingSpanM, @Nullable Float wingAreaM2, @Nullable Float climbRateMs)
        {
            this(wingSpanM, wingAreaM2, climbRateMs, null, null);
        }

        public boolean isEmpty()
        {
            return wingSpanM == null && wingAreaM2 == null && climbRateMs == null
                && rotorDiameterM == null && rotorCount == null;
        }

        /** Number of main rotors, defaulting to the single rotor of a conventional helicopter. */
        public int effectiveRotorCount()
        {
            return rotorCount == null || rotorCount < 1 ? 1 : rotorCount;
        }

        /**
         * Span of the lifting surface. A rotorcraft has no wing, so its rotor disc is
         * the lifting surface and the rotor diameter is its span. An authored wing span
         * always wins, which is what a compound helicopter with real wings needs.
         */
        @Nullable
        public Float effectiveWingSpanM()
        {
            if (VehiclePhysicsUnits.isUsablePositive(wingSpanM))
                return wingSpanM;
            return VehiclePhysicsUnits.isUsablePositive(rotorDiameterM) ? rotorDiameterM : null;
        }

        /**
         * Reference area of the lifting surface. For a rotorcraft this is the swept
         * rotor disc, {@code count * pi * (diameter / 2)^2}, which is the quantity a
         * helicopter's published disc loading is calculated from.
         */
        @Nullable
        public Float effectiveWingAreaM2()
        {
            if (VehiclePhysicsUnits.isUsablePositive(wingAreaM2))
                return wingAreaM2;
            if (!VehiclePhysicsUnits.isUsablePositive(rotorDiameterM))
                return null;
            double radius = rotorDiameterM / 2D;
            return (float) (effectiveRotorCount() * Math.PI * radius * radius);
        }

        /** Whether the lifting surface is derived from rotor geometry rather than authored. */
        public boolean usesRotorGeometry()
        {
            return !VehiclePhysicsUnits.isUsablePositive(wingSpanM)
                && !VehiclePhysicsUnits.isUsablePositive(wingAreaM2)
                && VehiclePhysicsUnits.isUsablePositive(rotorDiameterM);
        }
    }

    /** Ground-vehicle-only authored values. Both are independently usable. */
    public record Ground(
        @Nullable EnumDriveType driveType,
        @Nullable Float maxReverseSpeedKmh)
    {
        public static final Ground EMPTY = new Ground(null, null);

        public boolean isEmpty()
        {
            return driveType == null && maxReverseSpeedKmh == null;
        }
    }

    /** Marine-only authored values. Independently usable. */
    public record Marine(@Nullable Float draftM)
    {
        public static final Marine EMPTY = new Marine(null);

        public boolean isEmpty()
        {
            return draftM == null;
        }
    }

    public static final RealWorldVehicleSpec EMPTY =
        new RealWorldVehicleSpec(null, null, null, null, Aircraft.EMPTY, Ground.EMPTY, Marine.EMPTY);

    public RealWorldVehicleSpec
    {
        aircraft = aircraft == null ? Aircraft.EMPTY : aircraft;
        ground = ground == null ? Ground.EMPTY : ground;
        marine = marine == null ? Marine.EMPTY : marine;
    }

    /** True when a content pack declared no usable real-world key at all. */
    public boolean isEmpty()
    {
        return massKg == null && maxSpeedKmh == null && enginePowerKw == null && engineThrustKn == null
            && aircraft.isEmpty() && ground.isEmpty() && marine.isEmpty();
    }

    /**
     * Whether the coupled ground propulsion set is present and usable:
     * mass, engine power and top speed.
     */
    public boolean hasCompleteGroundProfile()
    {
        return VehiclePhysicsUnits.isUsablePositive(massKg)
            && VehiclePhysicsUnits.isUsablePositive(enginePowerKw)
            && VehiclePhysicsUnits.isUsablePositive(maxSpeedKmh);
    }

    /**
     * Whether the coupled aircraft set is present and usable: mass, top speed,
     * wing span, wing area and one of power or thrust. Climb rate is a
     * calibration input and is deliberately not required.
     */
    /**
     * Whether the coupled marine set is present and usable: mass, top speed, engine
     * power and a draft.
     *
     * <p>Deliberately not gated on the definition's category. A warship may be authored
     * as a {@code VehicleType} or, where its model extends {@code ModelPlane}, as a
     * {@code PlaneType}; both are the same ship and both deserve the same propulsion.
     * The draft is what distinguishes a hull from an amphibious vehicle that merely
     * floats, and the caller additionally requires that the definition actually floats.
     */
    public boolean hasCompleteMarineProfile()
    {
        return VehiclePhysicsUnits.isUsablePositive(massKg)
            && VehiclePhysicsUnits.isUsablePositive(maxSpeedKmh)
            && VehiclePhysicsUnits.isUsablePositive(enginePowerKw)
            && VehiclePhysicsUnits.isUsablePositive(marine.draftM());
    }

    public boolean hasCompleteAircraftProfile()
    {
        return VehiclePhysicsUnits.isUsablePositive(massKg)
            && VehiclePhysicsUnits.isUsablePositive(maxSpeedKmh)
            && VehiclePhysicsUnits.isUsablePositive(aircraft.effectiveWingSpanM())
            && VehiclePhysicsUnits.isUsablePositive(aircraft.effectiveWingAreaM2())
            && (VehiclePhysicsUnits.isUsablePositive(enginePowerKw)
                || VehiclePhysicsUnits.isUsablePositive(engineThrustKn));
    }
}
