package com.flansmodultimate.common.physics;

import com.flansmodultimate.config.ModCommonConfig;
import net.minecraft.world.level.Level;

/** Dimension-specific multipliers for gravity and velocity loss owned by this mod. */
public final class ModPhysics
{
    private ModPhysics() {}

    public static double gravity(double base, Level level)
    {
        return gravity(base, ModCommonConfig.gravityFactor(level));
    }

    public static double gravity(double base, double factor)
    {
        return base * factor;
    }

    /** Applies the drag multiplier to a per-tick velocity-retention factor. */
    public static double dragRetention(double base, Level level)
    {
        return dragRetention(base, ModCommonConfig.dragFactor(level));
    }

    public static double dragRetention(double base, double factor)
    {
        return Math.pow(Math.max(0D, Math.min(1D, base)), factor);
    }

    /** Applies the drag multiplier to a force or acceleration already expressed as drag. */
    public static double dragForce(double base, Level level)
    {
        return dragForce(base, ModCommonConfig.dragFactor(level));
    }

    public static double dragForce(double base, double factor)
    {
        return base * factor;
    }
}
