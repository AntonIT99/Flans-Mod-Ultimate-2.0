package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;

/** Pure severity curve for forgiving aircraft landing and crash damage. */
public final class PlaneCrashDamage
{
    static final double SAFE_DESCENT_SPEED = 0.35D;
    private static final double FULL_DAMAGE_DESCENT_SPEED = 1.2D;
    private static final double SAFE_UPRIGHT_COSINE = Math.cos(Math.toRadians(25D));
    private static final double WORST_UPRIGHT_COSINE = Math.cos(Math.toRadians(75D));
    private static final double MIN_DAMAGE_SEVERITY = 0.16D;

    private PlaneCrashDamage() {}

    public static Impact evaluate(double descentSpeed, double uprightCosine, float fallDamageFactor)
    {
        if (!Double.isFinite(descentSpeed) || !Double.isFinite(uprightCosine)
            || descentSpeed <= SAFE_DESCENT_SPEED || fallDamageFactor <= 0F)
            return Impact.NONE;

        double speed = Mth.clamp((descentSpeed - SAFE_DESCENT_SPEED)
            / (FULL_DAMAGE_DESCENT_SPEED - SAFE_DESCENT_SPEED), 0D, 1D);
        double angle = Mth.clamp((SAFE_UPRIGHT_COSINE - uprightCosine)
            / (SAFE_UPRIGHT_COSINE - WORST_UPRIGHT_COSINE), 0D, 1D);
        // Angle dominates so a fast but level touchdown is survivable, while a
        // wingtip/nose strike at the same speed is meaningfully destructive.
        double severity = angle * 0.7D + speed * 0.3D;
        if (severity < MIN_DAMAGE_SEVERITY)
            return Impact.NONE;
        float damage = (float) ((8D + severity * 92D) * fallDamageFactor);
        return new Impact(damage, (float) severity);
    }

    public record Impact(float damage, float severity)
    {
        private static final Impact NONE = new Impact(0F, 0F);

        public boolean damaging()
        {
            return damage > 0F;
        }
    }
}
