package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.penetration.PenetrationCalculator;
import com.flansmodultimate.common.guns.penetration.PenetrationResult;
import org.jetbrains.annotations.Nullable;

/** Selects legacy fixed versus normalized kinetic/fallback damage after the armour gate. */
public final class VehicleProjectileDamageResolver
{
    /** Share of the shaped-charge damage left when the jet only just defeats the plate. */
    static final float MIN_SHAPED_CHARGE_RESIDUAL = 0.6F;
    /** Overmatch at and above which the jet arrives behind the plate with its full effect. */
    static final float FULL_SHAPED_CHARGE_OVERMATCH = 2F;

    private VehicleProjectileDamageResolver() {}

    /**
     * @param shapedCharge whether the shaped-charge channel set the damage: a HEAT round defeated an armoured face
     *                     of a normalized-health vehicle
     */
    public record Result(float damage, boolean kineticDamage, PenetrationResult penetration, boolean shapedCharge) {}

    public static Result resolve(boolean normalizedHealth, float projectileMassGrams, float fixedDamage, double muzzleVelocityBlocksPerTick, ResolvedArmorHit armorHit, @Nullable Float penetrationAt100m)
    {
        return resolve(normalizedHealth, projectileMassGrams, fixedDamage, muzzleVelocityBlocksPerTick, armorHit,
            penetrationAt100m, 0F, 0D);
    }

    /**
     * @param shapedChargeKg      charge of a HEAT round in kg TNT, or 0 for any other round
     * @param heatDamageReference post-penetration damage of a 1 kg shaped charge that fully defeats the plate
     */
    public static Result resolve(boolean normalizedHealth, float projectileMassGrams, float fixedDamage, double muzzleVelocityBlocksPerTick, ResolvedArmorHit armorHit, @Nullable Float penetrationAt100m, float shapedChargeKg, double heatDamageReference)
    {
        float safeFixedDamage = Float.isFinite(fixedDamage) ? Math.max(0F, fixedDamage) : 0F;

        ResolvedArmorHit safeArmor = armorHit == null ? new ResolvedArmorHit(null, EnumArmorFacing.FRONT, ArmorPlate.UNARMOURED, EnumArmorFacing.FRONT.outwardNormal(), 0F, 0F) : armorHit;

        PenetrationResult penetration = PenetrationCalculator.resolve(penetrationAt100m, safeArmor.effectiveArmorMm());

        if (!penetration.penetrated())
            return new Result(0F, false, penetration, false);

        if (!normalizedHealth)
            return new Result(safeFixedDamage, false, penetration, false);

        float damage;
        boolean kinetic;
        if (!Float.isFinite(projectileMassGrams) || projectileMassGrams <= 0F)
        {
            // Compatibility bridge: mass-less legacy ammunition keeps its fixed
            // DamageStats value and does not receive the kinetic overmatch bonus.
            damage = safeFixedDamage;
            kinetic = false;
        }
        else
        {
            damage = ShootingHelper.getKineticDamage(projectileMassGrams, muzzleVelocityBlocksPerTick);
            if (safeArmor.isArmoured())
                damage *= hybridOvermatchMultiplier(penetration.overmatch());
            damage = Float.isFinite(damage) ? Math.max(0F, damage) : 0F;
            kinetic = true;
        }

        // A shaped charge does its work with the jet, not with the speed of the round that carries it, so a slow
        // rocket would otherwise do almost nothing to the tank it was built to kill. Against armour it is credited
        // with whichever channel hurts more; soft targets keep the kinetic damage and the ungated blast.
        if (shapedChargeKg > 0F && Float.isFinite(shapedChargeKg) && safeArmor.isArmoured())
        {
            float shaped = shapedChargeDamage(shapedChargeKg, penetration.overmatch(), heatDamageReference);
            if (shaped > damage)
                return new Result(shaped, false, penetration, true);
            return new Result(damage, kinetic, penetration, true);
        }
        return new Result(damage, kinetic, penetration, false);
    }

    public static float hybridOvermatchMultiplier(float overmatch)
    {
        if (!Float.isFinite(overmatch) || overmatch < 1F)
            return 0F;
        return Math.min(overmatch, 2.5F);
    }

    /**
     * Damage behind the plate from a shaped charge. It grows with the square root of the charge, so a heavy
     * missile is decisive without a light rocket becoming useless, and is independent of the round's speed.
     */
    public static float shapedChargeDamage(float chargeKg, float overmatch, double reference)
    {
        if (!Float.isFinite(chargeKg) || chargeKg <= 0F || !Double.isFinite(reference) || reference <= 0D)
            return 0F;
        double damage = reference * Math.sqrt(chargeKg) * shapedChargeResidual(overmatch);
        return Double.isFinite(damage) ? (float) Math.min(damage, Float.MAX_VALUE) : 0F;
    }

    /**
     * Share of a shaped charge's effect that reaches the inside. A jet that only just gets through brings little
     * spall with it; one with a wide margin arrives intact. Unlike the kinetic overmatch multiplier this never
     * exceeds one: surplus penetration leaves through the far side rather than adding damage.
     */
    public static float shapedChargeResidual(float overmatch)
    {
        if (!Float.isFinite(overmatch) || overmatch < 1F)
            return MIN_SHAPED_CHARGE_RESIDUAL;
        float progress = (overmatch - 1F) / (FULL_SHAPED_CHARGE_OVERMATCH - 1F);
        return Math.min(1F, MIN_SHAPED_CHARGE_RESIDUAL + (1F - MIN_SHAPED_CHARGE_RESIDUAL) * progress);
    }
}
