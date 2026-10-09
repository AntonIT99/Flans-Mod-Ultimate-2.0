package com.flansmodultimate.common.driveables.physics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/** Labjac naval damage rules, evaluated without a world or mutable entity state. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EpicShipPhysics
{
    private static final double RESPONSE = 0.35D;
    private static final float ROLL_STEP = (float) Math.toDegrees(0.0005D);
    private static final float PITCH_STEP = (float) Math.toDegrees(0.00025D);
    private static final float MAX_ROLL = (float) Math.toDegrees(1.5D);
    private static final float MAX_PITCH = (float) Math.toDegrees(0.6D);

    /** Undefined parts are healthy rather than zero-health naval compartments. */
    public record PartHealth(float health, float maximum)
    {
        public float ratio()
        {
            if (!Float.isFinite(maximum) || maximum <= 0F)
                return 1F;
            return Float.isFinite(health) ? Math.max(0F, Math.min(1F, health / maximum)) : 1F;
        }

        public boolean destroyed()
        {
            return ratio() <= 0F;
        }
    }

    public record Damage(float buoyancyRatio, boolean bowLost, boolean sternLost, boolean portLost, boolean starboardLost, boolean midsectionLost)
    {
        public boolean hullLost()
        {
            return bowLost || sternLost || portLost || starboardLost || midsectionLost;
        }

        public boolean sinking()
        {
            return buoyancyRatio < 0.1F || hullLost();
        }
    }

    public static Damage damage(PartHealth buoyancy, PartHealth bow, PartHealth stern, PartHealth port, PartHealth starboard, PartHealth midsection)
    {
        return new Damage(buoyancy.ratio(), bow.destroyed(), stern.destroyed(), port.destroyed(), starboard.destroyed(), midsection.destroyed());
    }

    /** The fork's three wide, deep water samples; a single bucket cannot pass. */
    public static boolean atSea(Vec3 origin, Predicate<Vec3> water)
    {
        return water.test(origin.add(6D, -4D, 6D)) && water.test(origin.add(-6D, -4D, -6D)) && water.test(origin.add(0D, -6D, 0D));
    }

    public static float throttleLimit(Damage damage, boolean atSea, double depth)
    {
        if (damage.buoyancyRatio() < 0.1F && depth > 1D)
            return 0F;
        return Math.min(damage.buoyancyRatio(), atSea ? 1F : 0.3F);
    }

    public static double verticalVelocity(double current, double healthy, boolean deeplySubmerged, Damage damage)
    {
        if (damage.sinking())
        {
            double target = -0.5D;
            if (deeplySubmerged)
                target = damage.hullLost() ? -0.009D : 0.3D * (1.1D * damage.buoyancyRatio() - 0.5D);
            return current + (target - current) * RESPONSE;
        }
        if (damage.buoyancyRatio() >= 1F)
            return healthy;
        if (deeplySubmerged)
        {
            double target = Math.min(healthy, 0.3D * (1.1D * damage.buoyancyRatio() - 0.5D));
            return current + (target - current) * RESPONSE;
        }
        return healthy > 0D ? healthy * damage.buoyancyRatio() : healthy;
    }

    public static float roll(float current, Damage damage)
    {
        if (damage.portLost() != damage.starboardLost())
            return Math.max(-MAX_ROLL, Math.min(MAX_ROLL, current + (damage.portLost() ? ROLL_STEP : -ROLL_STEP)));
        return damage.hullLost() ? current : approachLevel(current);
    }

    public static float pitch(float current, Damage damage)
    {
        if (damage.bowLost() != damage.sternLost())
            return Math.max(-MAX_PITCH, Math.min(MAX_PITCH, current + (damage.bowLost() ? PITCH_STEP : -PITCH_STEP)));
        return damage.hullLost() ? current : approachLevel(current);
    }

    public static boolean shouldDestroy(Damage damage, double depth)
    {
        return damage.sinking() && Double.isFinite(depth) && depth > 15D;
    }

    private static float approachLevel(float current)
    {
        return Math.copySign(Math.max(0F, Math.abs(current) - 1.5F), current);
    }
}
