package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.penetration.PenetrationCalculator;
import com.flansmodultimate.common.guns.penetration.PenetrationResult;
import org.jetbrains.annotations.Nullable;

/** Selects legacy fixed versus normalized kinetic/fallback damage after the armour gate. */
public final class VehicleProjectileDamageResolver
{
    private VehicleProjectileDamageResolver() {}

    public record Result(float damage, boolean kineticDamage, PenetrationResult penetration) {}

    public static Result resolve(boolean normalizedHealth, float projectileMassGrams, float fixedDamage, double muzzleVelocityBlocksPerTick, ResolvedArmorHit armorHit, @Nullable Float penetrationAt100m)
    {
        float safeFixedDamage = Float.isFinite(fixedDamage) ? Math.max(0F, fixedDamage) : 0F;

        ResolvedArmorHit safeArmor = armorHit == null ? new ResolvedArmorHit(null, EnumArmorFacing.FRONT, ArmorPlate.UNARMOURED, EnumArmorFacing.FRONT.outwardNormal(), 0F, 0F) : armorHit;

        PenetrationResult penetration = PenetrationCalculator.resolve(penetrationAt100m, safeArmor.effectiveArmorMm());

        if (!penetration.penetrated())
            return new Result(0F, false, penetration);

        if (!normalizedHealth)
            return new Result(safeFixedDamage, false, penetration);

        if (!Float.isFinite(projectileMassGrams) || projectileMassGrams <= 0F)
        {
            // Compatibility bridge: mass-less legacy ammunition keeps its fixed
            // DamageStats value and does not receive the kinetic overmatch bonus.
            return new Result(safeFixedDamage, false, penetration);
        }

        float damage = ShootingHelper.getKineticDamage(projectileMassGrams, muzzleVelocityBlocksPerTick);
        if (safeArmor.isArmoured())
            damage *= hybridOvermatchMultiplier(penetration.overmatch());
        return new Result(Float.isFinite(damage) ? Math.max(0F, damage) : 0F, true, penetration);
    }

    public static float hybridOvermatchMultiplier(float overmatch)
    {
        if (!Float.isFinite(overmatch) || overmatch < 1F)
            return 0F;
        return Math.min(overmatch, 2.5F);
    }
}
