package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.projectiles.BallisticDrag;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.ModCommonConfig;

/**
 * Chooses the speed-dependent air drag of a round (see {@link BallisticDrag}).
 *
 * <p>
 * Rounds with a projectile mass get a coefficient from it: geometrically similar rounds have a cross-section growing with
 * {@code mass^(2/3)}, so the coefficient falls with {@code mass^(-1/3)}. The scale is set so a 4 g rifle bullet keeps 70% of
 * its speed over 300 m; a 9.5 g 7.62 mm round then keeps about 76%, a 42 g 12.7 mm round 85%, and a 4.5 kg penetrator 89%
 * over a kilometre. {@code QuadraticDragMultiplier} corrects shapes far from that. A round whose definition sets
 * {@code DragInAir} keeps the constant retention it describes, unless {@code QuadraticDrag true} asks to convert it; a
 * converted round keeps the authored retention at its muzzle velocity and loses less as it slows down. Rounds without mass
 * keep constant retention unless converted the same way.
 *
 * <p>
 * Powered, guided, dropped and underwater ordnance keeps its own motion: missiles, bombs, mines, torpedoes, depth charges and
 * manually, laser or SACLOS guided rounds.
 */
public final class ProjectileDragModel
{
    static final double REFERENCE_MASS_GRAMS = 4D;
    static final double REFERENCE_DISTANCE = 300D;
    static final double REFERENCE_RETENTION = 0.7D;
    static final double REFERENCE_COEFFICIENT = -Math.log(REFERENCE_RETENTION) / REFERENCE_DISTANCE;

    private ProjectileDragModel()
    {}

    /** Coefficient for a round in play, under the server's drag settings and the dimension's drag factor. */
    public static double coefficient(BulletType type, float projectileMassGrams, float muzzleVelocity, double dimensionDragFactor)
    {
        if (!ModCommonConfig.realisticProjectileDrag())
            return 0D;
        return coefficient(type, projectileMassGrams, muzzleVelocity) * ModCommonConfig.projectileDragScale() * Math.max(0D, dimensionDragFactor);
    }

    /** Coefficient in inverse blocks before server and dimension scaling, or 0 when the round keeps constant retention. */
    static double coefficient(BulletType type, float projectileMassGrams, float muzzleVelocity)
    {
        Boolean setting = type.getQuadraticDragSetting();
        if (Boolean.FALSE.equals(setting) || !isBallistic(type))
            return 0D;
        double coefficient;
        if (projectileMassGrams > 0F && !type.isExplicitDragInAir())
            coefficient = REFERENCE_COEFFICIENT * Math.pow(projectileMassGrams / REFERENCE_MASS_GRAMS, -1D / 3D);
        // Off was ruled out above, so a setting here asks for the conversion.
        else if (setting != null && muzzleVelocity > 0F && type.getDragInAir() > 0F)
            coefficient = (1D / type.getDragInAir() - 1D) / muzzleVelocity;
        else
            return 0D;
        coefficient *= type.getQuadraticDragMultiplier();
        return Double.isFinite(coefficient) && coefficient > 0D ? coefficient : 0D;
    }

    /** Whether the round flies freely after launch, so air drag alone shapes its speed. */
    static boolean isBallistic(BulletType type)
    {
        EnumWeaponType weapon = type.getWeaponType();
        if (weapon != EnumWeaponType.GUN && weapon != EnumWeaponType.SHELL)
            return false;
        return !type.isTorpedo() && !type.isDepthCharge() && !type.isManualGuidance() && !type.isLaserGuidance() && !type.isEnableSACLOS();
    }

    /** Share of its muzzle velocity a round keeps on reaching something {@code range} blocks from where it was launched. */
    public static double speedShare(double coefficient, double range)
    {
        return BallisticDrag.speedAt(1D, coefficient, range);
    }

    /**
     * Factor on penetration rated at 100 m for a hit at {@code range} blocks: penetration grows with the impact velocity to
     * the power {@code exponent}, 1.43 after de Marre for solid shot into steel plate.
     */
    public static double penetrationShare(double coefficient, double range, double exponent)
    {
        if (coefficient <= 0D || exponent <= 0D || !Double.isFinite(range))
            return 1D;
        return Math.exp(-exponent * coefficient * (range - 100D));
    }
}
