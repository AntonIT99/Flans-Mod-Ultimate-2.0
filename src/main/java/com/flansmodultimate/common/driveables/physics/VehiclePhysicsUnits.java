package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

/**
 * The single place where real-world units are converted into Minecraft units.
 *
 * <p>The mod's physical convention, already assumed by the speed readout in
 * {@code ClientHudOverlays} and by {@code EnumSpeedUnit}, is:
 * <pre>
 * 1 block  = 1 metre
 * 20 ticks = 1 second
 * </pre>
 *
 * <p>Therefore {@code blocksPerTick = kmh / 3.6 / 20 = kmh / 72}. Nothing else in
 * the codebase may divide by 72; every conversion goes through this class so the
 * global speed scale can never be applied twice or forgotten.
 *
 * <p>Authored real-world values are never mutated by scaling. The scale is a
 * presentation/gameplay factor applied at conversion time only.
 */
public final class VehiclePhysicsUnits
{
    /** Minecraft server ticks per real second. */
    public static final double TICKS_PER_SECOND = 20D;
    /** km/h per m/s. */
    public static final double KMH_PER_METRE_PER_SECOND = 3.6D;
    /** {@code 3.6 * 20}. Dividing km/h by this yields blocks per tick at scale 1.0. */
    public static final double KMH_TO_BLOCKS_PER_TICK_DIVISOR = KMH_PER_METRE_PER_SECOND * TICKS_PER_SECOND;
    /** Standard gravity in m/s². Used only by real-world derivations, never by legacy physics. */
    public static final double STANDARD_GRAVITY = 9.80665D;
    /** Sea level air density in kg/m³. Used by the derived lift model. */
    public static final double AIR_DENSITY = 1.225D;
    /** Watts per kilowatt. */
    public static final double WATTS_PER_KILOWATT = 1000D;
    /** Newtons per kilonewton. */
    public static final double NEWTONS_PER_KILONEWTON = 1000D;
    /** Kilowatts per mechanical horsepower (1 hp = 745.699872 W), the imperial unit engine parts are commonly rated in. */
    public static final double KW_PER_HP = 0.745699872D;
    /** Kilowatts per metric horsepower / Pferdestärke (1 PS = 75 kgf·m/s = 735.49875 W exactly). */
    public static final double KW_PER_PS = 0.73549875D;
    /** km/h per knot (1 knot = 1 international nautical mile per hour = 1.852 km/h exactly). */
    public static final double KMH_PER_KNOT = 1.852D;
    /** Kilograms per metric tonne. */
    public static final double KG_PER_TONNE = 1000D;
    /** Kilograms per imperial long ton (2240 lb), the displacement unit of most pre-1960 naval sources. */
    public static final double KG_PER_LONG_TON = 1016.0469088D;

    private VehiclePhysicsUnits() {}

    /**
     * Converts mechanical horsepower to kilowatts, so {@code RealEnginePowerHp}
     * can be stored internally in the same unit as {@code RealEnginePowerKw}. A
     * non-finite or non-positive input yields zero rather than a poisoned
     * physics value.
     */
    public static double hpToKw(double hp)
    {
        if (!Double.isFinite(hp) || hp <= 0D)
            return 0D;
        return hp * KW_PER_HP;
    }

    /** Inverse of {@link #hpToKw(double)}; used for debug and tooltip readouts. */
    public static double kwToHp(double kw)
    {
        if (!Double.isFinite(kw) || kw <= 0D)
            return 0D;
        return kw / KW_PER_HP;
    }

    /**
     * Converts metric horsepower / Pferdestärke to kilowatts, so
     * {@code RealEnginePowerPS} can be stored internally in the same unit as
     * {@code RealEnginePowerKw}. A non-finite or non-positive input yields zero
     * rather than a poisoned physics value.
     */
    public static double psToKw(double ps)
    {
        if (!Double.isFinite(ps) || ps <= 0D)
            return 0D;
        return ps * KW_PER_PS;
    }

    /** Inverse of {@link #psToKw(double)}; used for debug and tooltip readouts. */
    public static double kwToPs(double kw)
    {
        if (!Double.isFinite(kw) || kw <= 0D)
            return 0D;
        return kw / KW_PER_PS;
    }

    /**
     * Converts knots to km/h, so {@code RealMaxSpeedKn} can be stored internally
     * in the same unit as {@code RealMaxSpeedKmh}. Naval sources state speed in
     * knots essentially without exception, and converting by hand is the single
     * most common authoring error for marine craft.
     */
    public static double knotsToKmh(double knots)
    {
        if (!Double.isFinite(knots) || knots <= 0D)
            return 0D;
        return knots * KMH_PER_KNOT;
    }

    /** Inverse of {@link #knotsToKmh(double)}; used for debug and tooltip readouts. */
    public static double kmhToKnots(double kmh)
    {
        if (!Double.isFinite(kmh) || kmh <= 0D)
            return 0D;
        return kmh / KMH_PER_KNOT;
    }

    /**
     * Converts metric tonnes to kilograms, so {@code RealDisplacementT} can be
     * stored internally in the same unit as {@code RealMassKg}.
     */
    public static double tonnesToKg(double tonnes)
    {
        if (!Double.isFinite(tonnes) || tonnes <= 0D)
            return 0D;
        return tonnes * KG_PER_TONNE;
    }

    /**
     * Converts imperial long tons to kilograms. Displacement in British and
     * American naval records before metrication is in long tons, not metric
     * tonnes, and the 1.6% difference is large enough to matter once it is cubed
     * into a health curve.
     */
    public static double longTonsToKg(double longTons)
    {
        if (!Double.isFinite(longTons) || longTons <= 0D)
            return 0D;
        return longTons * KG_PER_LONG_TON;
    }

    /** Converts km/h to blocks per tick at scale 1.0. */
    public static double kmhToBlocksPerTick(double kmh)
    {
        return kmhToBlocksPerTick(kmh, 1D);
    }

    /**
     * Converts km/h to blocks per tick, applying the global realistic speed
     * scale. A non-finite or non-positive input yields zero rather than a
     * poisoned physics value.
     */
    public static double kmhToBlocksPerTick(double kmh, double speedScale)
    {
        if (!Double.isFinite(kmh) || kmh <= 0D)
            return 0D;
        return kmh / KMH_TO_BLOCKS_PER_TICK_DIVISOR * sanitizeScale(speedScale);
    }

    /**
     * Factor that brings a speed in blocks per tick under an absolute ceiling
     * expressed in km/h, or exactly one when it is already under.
     *
     * <p>Scaling a velocity by this preserves its direction, which is what makes
     * it a speed limit rather than a per-axis clamp.
     *
     * @param capKmh the ceiling; a non-positive or non-finite value means no cap
     */
    public static double speedCapScale(double speedBlocksPerTick, double capKmh)
    {
        if (!Double.isFinite(capKmh) || capKmh <= 0D || !Double.isFinite(speedBlocksPerTick)
            || speedBlocksPerTick <= 0D)
            return 1D;
        double cap = kmhToBlocksPerTick(capKmh);
        return speedBlocksPerTick <= cap ? 1D : cap / speedBlocksPerTick;
    }

    /** Inverse of {@link #kmhToBlocksPerTick(double)}; used for debug and tooltip readouts. */
    public static double blocksPerTickToKmh(double blocksPerTick)
    {
        if (!Double.isFinite(blocksPerTick))
            return 0D;
        return blocksPerTick * KMH_TO_BLOCKS_PER_TICK_DIVISOR;
    }

    /** Converts m/s to blocks per tick, applying the global realistic speed scale. */
    public static double metresPerSecondToBlocksPerTick(double metresPerSecond, double speedScale)
    {
        if (!Double.isFinite(metresPerSecond))
            return 0D;
        return metresPerSecond / TICKS_PER_SECOND * sanitizeScale(speedScale);
    }

    /** Converts blocks per tick back to m/s at scale 1.0. */
    public static double blocksPerTickToMetresPerSecond(double blocksPerTick)
    {
        if (!Double.isFinite(blocksPerTick))
            return 0D;
        return blocksPerTick * TICKS_PER_SECOND;
    }

    /**
     * Converts an acceleration in m/s² to blocks per tick squared. There are
     * {@code 20 * 20} tick-squared units in one second-squared.
     */
    public static double metresPerSecondSquaredToBlocksPerTickSquared(double metresPerSecondSquared)
    {
        if (!Double.isFinite(metresPerSecondSquared))
            return 0D;
        return metresPerSecondSquared / (TICKS_PER_SECOND * TICKS_PER_SECOND);
    }

    /** Power-to-weight ratio in kW/kg. Returns zero for unusable inputs. */
    public static float powerToWeight(float powerKw, float massKg)
    {
        if (!isUsablePositive(powerKw) || !isUsablePositive(massKg))
            return 0F;
        return powerKw / massKg;
    }

    /** Thrust-to-weight ratio, dimensionless. Returns zero for unusable inputs. */
    public static float thrustToWeight(float thrustKn, float massKg)
    {
        if (!isUsablePositive(thrustKn) || !isUsablePositive(massKg))
            return 0F;
        return (float) (thrustKn * NEWTONS_PER_KILONEWTON / (massKg * STANDARD_GRAVITY));
    }

    /** Wing loading in kg/m². Returns zero for unusable inputs. */
    public static float wingLoading(float massKg, float wingAreaM2)
    {
        if (!isUsablePositive(massKg) || !isUsablePositive(wingAreaM2))
            return 0F;
        return massKg / wingAreaM2;
    }

    /**
     * A value is usable as real-world physics input only when it is finite and
     * strictly positive. Zero mass, negative power and NaN are all rejected here
     * rather than being allowed to reach a division.
     */
    public static boolean isUsablePositive(float value)
    {
        return Float.isFinite(value) && value > 0F;
    }

    /** Nullable-aware form of {@link #isUsablePositive(float)}. */
    public static boolean isUsablePositive(@Nullable Float value)
    {
        return value != null && isUsablePositive(value.floatValue());
    }

    /** Clamps a configured speed scale into the range the config also enforces. */
    public static double sanitizeScale(double speedScale)
    {
        if (!Double.isFinite(speedScale) || speedScale <= 0D)
            return 1D;
        return Math.min(speedScale, 10D);
    }
}
