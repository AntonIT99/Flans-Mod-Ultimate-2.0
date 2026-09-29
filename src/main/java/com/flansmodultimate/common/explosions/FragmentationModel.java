package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.types.ShootableType.EnumFragType;

/** Expected injury from casing fragments. Distances and projectile areas are in Minecraft metres. */
public final class FragmentationModel
{
    private static final double PLAYER_AREA_M2 = 1.2D;
    private static final double FULL_SPHERE = 4D * Math.PI;
    private static final double MIN_DAMAGE_FOR_QUERY = 1D;

    private FragmentationModel() {}

    public enum Pattern
    {
        RADIAL(FULL_SPHERE),
        /** Raised mine: a narrow, horizontal ring of fragments. */
        HORIZONTAL_BAND(4D * Math.PI * Math.sin(Math.toRadians(8D))),
        /** About 60 degrees wide and 16 degrees high, along the explosive's facing. */
        FORWARD_FAN(4D * Math.asin(Math.sin(Math.toRadians(30D)) * Math.sin(Math.toRadians(8D)))),
        /** Compact forward cloud, for preformed airburst projectiles. */
        FORWARD_CONE(2D * Math.PI * (1D - Math.cos(Math.toRadians(5D))));

        public final double solidAngle;

        Pattern(double solidAngle) { this.solidAngle = solidAngle; }

        public double distribution(double dx, double dy, double dz, double fx, double fy, double fz)
        {
            if (this == RADIAL)
                return 1D;

            double horizontal = Math.hypot(dx, dz);
            boolean b = Math.abs(Math.atan2(dy, horizontal)) <= Math.toRadians(8D);
            if (this == HORIZONTAL_BAND)
                return b ? 1D : 0D;

            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double facing = Math.sqrt(fx * fx + fy * fy + fz * fz);
            if (length < 1.0E-9D || facing < 1.0E-9D)
                return 0D;

            double dot = (dx * fx + dy * fy + dz * fz) / (length * facing);
            if (this == FORWARD_CONE)
                return dot >= Math.cos(Math.toRadians(5D)) ? 1D : 0D;

            double horizontalFacing = Math.hypot(fx, fz);
            double horizontalDot = horizontalFacing > 1.0E-9D && horizontal > 1.0E-9D ? (dx * fx + dz * fz) / (horizontal * horizontalFacing) : 0D;
            return horizontalDot >= Math.cos(Math.toRadians(30D)) && b ? 1D : 0D;
        }
    }

    public record Burst(double fragmentCount, double energyLength, double peakDamage, Pattern pattern,
                        double queryRadius)
    {
        public static final Burst NONE = new Burst(0D, 0D, 0D, Pattern.RADIAL, 0D);

        public Burst withPeak(double peak)
        {
            return new Burst(fragmentCount, energyLength, peak, pattern,
                effectiveRadius(fragmentCount, energyLength, peak, pattern));
        }

        public double hitChance(double seen, double distance, double distribution)
        {
            if (fragmentCount <= 0D || energyLength <= 0D || seen <= 0D || distribution <= 0D
                || !Double.isFinite(distance) || distance < 0D || distance > queryRadius)
                return 0D;
            // Poisson arrival of effective fragments through an exposed human silhouette.
            double density = fragmentCount * PLAYER_AREA_M2 * distribution
                / (pattern.solidAngle * (distance * distance + 1D));
            return -Math.expm1(-density * Math.min(1D, seen));
        }

        public double damage(double peak, double seen, double distance, double distribution)
        {
            return Math.max(0D, peak) * hitChance(seen, distance, distribution)
                * Math.exp(-distance / Math.max(1D, energyLength));
        }
    }

    public static Burst create(EnumFragType type, double chargeKg, double projectileMassGrams,
                               double metalMassGrams, double authoredCount, Pattern pattern)
    {
        return create(type, chargeKg, projectileMassGrams, metalMassGrams, authoredCount, pattern, 0D);
    }

    public static Burst create(EnumFragType type, double chargeKg, double projectileMassGrams,
                               double metalMassGrams, double authoredCount, Pattern pattern,
                               double projectileSpeedMps)
    {
        if (type == EnumFragType.DEFAULT || !Double.isFinite(chargeKg) || chargeKg <= 0D)
            return Burst.NONE;

        double metal;
        if (metalMassGrams > 0D)
        {
            metal = metalMassGrams;
        }
        else
        {
            if (projectileMassGrams > chargeKg * 1000D)
                metal = projectileMassGrams - chargeKg * 1000D;
            else
                metal = type.defaultMetalGrams * Math.pow(Math.max(chargeKg, 0.001D) / 0.1D, 0.2D);
        }

        double count = authoredCount > 0D ? authoredCount : metal / type.fragmentMassGrams * type.effectiveFraction;
        double chargeToMetal = chargeKg * 1000D / (metal + chargeKg * 500D);
        double velocityFactor = Math.max(0.65D, Math.min(1.5D, Math.max(Math.sqrt(chargeToMetal / 0.18D), projectileSpeedMps / 800D)));
        double energyLength = type.dragLength * Math.cbrt(type.fragmentMassGrams) * velocityFactor;
        double radius = effectiveRadius(count, energyLength, type.kFragDamage, pattern);
        return new Burst(count, energyLength, type.kFragDamage, pattern, radius);
    }

    private static double effectiveRadius(double count, double energyLength, double peak, Pattern pattern)
    {
        double low = 0D;
        double high = 4096D;
        for (int i = 0; i < 24; i++)
        {
            double middle = (low + high) * 0.5D;
            double density = count * PLAYER_AREA_M2 / (pattern.solidAngle * (middle * middle + 1D));
            double damage = peak * -Math.expm1(-density) * Math.exp(-middle / Math.max(1D, energyLength));
            if (damage >= MIN_DAMAGE_FOR_QUERY)
                low = middle;
            else
                high = middle;
        }
        return Math.ceil(low);
    }
}
