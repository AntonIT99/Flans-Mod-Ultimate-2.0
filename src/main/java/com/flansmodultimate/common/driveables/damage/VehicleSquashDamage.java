package com.flansmodultimate.common.driveables.damage;

/**
 * Pure curve for what a driveable whose definition declares {@code SquashMobs}
 * does to a body it runs into.
 *
 * <p>
 * Legacy Flan's dealt a flat {@code 2 + speed * 12}: a bicycle and a sixty-tonne
 * tank delivered it alike, and it grew only linearly with speed, so the
 * difference between a car park crawl and a road charge was slight. What a
 * struck body actually absorbs in a plastic collision is {@code 0.5 * mu * v * v}
 * with the reduced mass {@code mu = M * m / (M + m)}, and that is what this
 * reports instead. Speed therefore enters squared, which is where the real
 * danger is, and mass enters through the reduced mass, which saturates once the
 * hull is far heavier than the body.
 *
 * <p>
 * The saturation is the honest part rather than a shortcut: a hull well under a
 * tonne hurts markedly less because it is slowed by the impact, while a lorry
 * and a tank at the same speed hurt much the same because neither is slowed at
 * all and the body absorbs what its own mass dictates. A heavy hull is still far
 * more dangerous than a light one over the range packs actually author.
 *
 * <p>
 * The calibration point is a car: a person struck by about 1.5 t at 60 km/h is
 * unlikely to survive, so that is the case that reaches a player's twenty
 * hitpoints.
 */
public final class VehicleSquashDamage
{
    /** Mass of the body being run into, in kilograms: an adult. */
    private static final double BODY_MASS_KG = 80D;
    /** Half hearts per kilojoule the struck body absorbs, fixed by the 1.5 t at 60 km/h case. */
    private static final double HALF_HEARTS_PER_KILOJOULE = 1.9D;
    /** Ticks a second, which reads a speed in blocks per tick as metres per second. */
    private static final double METRES_PER_SECOND_PER_BLOCK_PER_TICK = 20D;
    private static final double JOULES_PER_KILOJOULE = 1000D;
    /**
     * Nothing is deadlier than dead. This bounds what an absurd speed reports
     * rather than limiting how lethal a plausible impact is, which the energy
     * curve passes several times over well before here.
     */
    private static final double MAX_DAMAGE = 100D;
    /** Legacy flat curve, kept as the bridge for a definition with no usable mass. */
    private static final double LEGACY_BASE_DAMAGE = 2D;
    private static final double LEGACY_DAMAGE_PER_BLOCK_PER_TICK = 12D;
    private static final double LEGACY_MAX_DAMAGE = 40D;

    private VehicleSquashDamage()
    {}

    /**
     * @param massKg
     *            mass of the driveable doing the running over, in kilograms; a
     *            value that is not usable falls back to the legacy flat curve
     * @param speedBlocksPerTick
     *            how fast it moves across the ground
     * @return the damage in half hearts
     */
    public static double damage(double massKg, double speedBlocksPerTick)
    {
        if (!Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0D)
            return 0D;
        if (!Double.isFinite(massKg) || massKg <= 0D)
            return legacyDamage(speedBlocksPerTick);
        double damage = absorbedKilojoules(massKg, speedBlocksPerTick) * HALF_HEARTS_PER_KILOJOULE;
        return Double.isFinite(damage) ? Math.min(MAX_DAMAGE, damage) : MAX_DAMAGE;
    }

    /**
     * Energy the struck body absorbs, which is the impact energy carried by the
     * reduced mass of the pair rather than the driveable's whole kinetic energy.
     * A hull that is not slowed by the impact cannot deliver more than the body's
     * own mass will take.
     *
     * @return the absorbed energy in kilojoules, or zero for an unusable input.
     *         A speed large enough to overflow reports an unbounded energy
     *         rather than none, which {@link #damage} then bounds.
     */
    public static double absorbedKilojoules(double massKg, double speedBlocksPerTick)
    {
        if (!Double.isFinite(massKg) || massKg <= 0D || !Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0D)
            return 0D;
        double reducedMassKg = massKg * BODY_MASS_KG / (massKg + BODY_MASS_KG);
        double metresPerSecond = speedBlocksPerTick * METRES_PER_SECOND_PER_BLOCK_PER_TICK;
        double joules = 0.5D * reducedMassKg * metresPerSecond * metresPerSecond;
        return joules / JOULES_PER_KILOJOULE;
    }

    /**
     * The curve legacy Flan's used, which knows nothing of mass.
     *
     * <p>
     * Every driveable class resolves a usable impulse mass, falling back to a
     * configured figure for its class, so this is reached only by an addon
     * asking about a mass it could not establish at all.
     */
    public static double legacyDamage(double speedBlocksPerTick)
    {
        if (!Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0D)
            return 0D;
        return Math.min(LEGACY_MAX_DAMAGE, LEGACY_BASE_DAMAGE + speedBlocksPerTick * LEGACY_DAMAGE_PER_BLOCK_PER_TICK);
    }
}
