package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * How outside pushes, knockback and collisions act on a heavy driveable.
 *
 * <p>Minecraft knockback is tuned for a player. Applied unchanged to a vehicle
 * it throws a sixty-tonne tank as far as a person, so every outside velocity
 * change is weighed against the vehicle's mass instead, as momentum
 * conservation would: a push that moves a reference body by {@code dv} moves a
 * body {@code n} times heavier by {@code dv / n}.
 */
public final class VehicleImpulsePhysics
{
    private VehicleImpulsePhysics() {}

    /** Where a resolved impulse mass came from, in order of precedence. */
    public enum MassSource
    {
        /** {@code RealMassKg} (or a mass unit it accepts), from the type file or its category. */
        REAL_MASS,
        /** The legacy {@code Mass} key: tonnes for vehicles, kilograms for aircraft. */
        LEGACY_MASS,
        /** Neither was usable; the configured fallback for the class. */
        FALLBACK
    }

    public record ImpulseMass(float massKg, MassSource source) {}

    /** Velocity changes along a contact normal, in blocks per tick: negative for the striking body, positive for the struck one. */
    public record CollisionImpulse(double selfDelta, double otherDelta)
    {
        public static final CollisionImpulse NONE = new CollisionImpulse(0D, 0D);

        public boolean isNone()
        {
            return selfDelta == 0D && otherDelta == 0D;
        }
    }

    /**
     * Resolves the mass outside pushes are weighed against.
     *
     * <p>A usable real-world mass wins. The legacy {@code Mass} key is only a
     * fallback, already converted to kilograms from the unit its driveable class
     * authored it in, and only when it is plausible: a value lighter than
     * {@link VehiclePhysicsConstants#MIN_PLAUSIBLE_LEGACY_MASS_KG} is a
     * placeholder, not a weight.
     */
    public static ImpulseMass resolveMass(@Nullable Float realMassKg, @Nullable Float legacyMassKg, double fallbackMassKg)
    {
        if (realMassKg != null && Float.isFinite(realMassKg) && realMassKg > 0F)
            return new ImpulseMass(Math.max(VehiclePhysicsConstants.MIN_IMPULSE_MASS_KG, realMassKg), MassSource.REAL_MASS);
        if (legacyMassKg != null && Float.isFinite(legacyMassKg)
            && legacyMassKg >= VehiclePhysicsConstants.MIN_PLAUSIBLE_LEGACY_MASS_KG)
            return new ImpulseMass(legacyMassKg, MassSource.LEGACY_MASS);
        float fallback = Double.isFinite(fallbackMassKg) ? (float) fallbackMassKg : 0F;
        return new ImpulseMass(Math.max(VehiclePhysicsConstants.MIN_IMPULSE_MASS_KG, fallback), MassSource.FALLBACK);
    }

    /** Share of an outside velocity change a body of {@code massKg} keeps. Never more than the change itself. */
    public static double knockbackScale(double massKg, double referenceMassKg)
    {
        if (!finitePositive(massKg) || !finitePositive(referenceMassKg))
            return 1D;
        return Math.min(1D, referenceMassKg / massKg);
    }

    /**
     * Weighs an outside velocity change against mass. What is left below the
     * dead zone is dropped entirely.
     */
    public static Vec3 scaleExternalImpulse(Vec3 impulse, double massKg, double referenceMassKg)
    {
        if (!isFinite(impulse))
            return Vec3.ZERO;
        Vec3 scaled = impulse.scale(knockbackScale(massKg, referenceMassKg));
        double deadZone = VehiclePhysicsConstants.EXTERNAL_IMPULSE_DEAD_ZONE_BLOCKS_PER_TICK;
        return scaled.lengthSqr() < deadZone * deadZone ? Vec3.ZERO : scaled;
    }

    /**
     * A one-dimensional inelastic collision along the contact normal.
     *
     * <p>Momentum is conserved and the bodies separate at {@code restitution}
     * times their closing speed. Nothing happens unless they are actually
     * closing, so bodies already moving apart, or merely resting against each
     * other, exchange nothing.
     *
     * @param closingSpeed how fast the striking body approaches the struck one along the normal
     */
    public static CollisionImpulse collision(double selfMassKg, double otherMassKg, double closingSpeed, double restitution)
    {
        if (!(closingSpeed > 0D) || !Double.isFinite(closingSpeed)
            || !finitePositive(selfMassKg) || !finitePositive(otherMassKg))
            return CollisionImpulse.NONE;
        double exchange = (1D + Math.max(0D, Math.min(1D, restitution))) * closingSpeed;
        double total = selfMassKg + otherMassKg;
        return new CollisionImpulse(-exchange * otherMassKg / total, exchange * selfMassKg / total);
    }

    /**
     * Guarantees that horizontal motion slows by at least a given amount this
     * tick, and brings it to rest once it is slower than the rest speed.
     *
     * <p>Only ever removes speed: whatever the movement model already decided is
     * kept when it decelerates harder, and vertical motion is untouched.
     */
    public static Vec3 enforceMinimumDeceleration(Vec3 before, Vec3 after, double decelerationBlocksPerTickSquared,
                                                  double restSpeedBlocksPerTick)
    {
        if (!isFinite(before) || !isFinite(after))
            return after;
        double afterSpeed = after.horizontalDistance();
        if (afterSpeed <= 0D)
            return after;
        double ceiling = Math.max(0D, before.horizontalDistance() - Math.max(0D, decelerationBlocksPerTickSquared));
        double limited = Math.min(afterSpeed, ceiling);
        if (limited < restSpeedBlocksPerTick)
            limited = 0D;
        if (limited >= afterSpeed)
            return after;
        double scale = limited / afterSpeed;
        return new Vec3(after.x * scale, after.y, after.z * scale);
    }

    private static boolean isFinite(Vec3 vector)
    {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean finitePositive(double value)
    {
        return Double.isFinite(value) && value > 0D;
    }
}
