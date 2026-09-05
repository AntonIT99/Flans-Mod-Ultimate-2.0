package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class VehicleProjectileDamageResolverTest
{
    @Test
    void normalizedMassUsesKineticWhileMasslessUsesFixedFallback()
    {
        VehicleProjectileDamageResolver.Result kinetic = resolve(true, 9F, 25F, 333D, unarmoured(), null);
        assertTrue(kinetic.kineticDamage());
        assertEquals(5F, kinetic.damage(), 0.02F);

        VehicleProjectileDamageResolver.Result fixed = resolve(true, 0F, 25F, 333D, unarmoured(), null);
        assertFalse(fixed.kineticDamage());
        assertEquals(25F, fixed.damage());
    }

    @Test
    void legacyHealthKeepsItsExistingFixedMagnitude()
    {
        VehicleProjectileDamageResolver.Result result = resolve(false, 10_200F, 70F, 773D,
            unarmoured(), 151F);
        assertEquals(70F, result.damage());
        assertFalse(result.kineticDamage());
    }

    @Test
    void armourGateRunsBeforeDamageSelection()
    {
        VehicleProjectileDamageResolver.Result blocked = resolve(true, 45F, 100F, 890D,
            armoured(80F), 20F);
        assertEquals(0F, blocked.damage());
        assertFalse(blocked.penetration().penetrated());
    }

    @Test
    void hybridMultiplierHasTheRequestedFloorAndCap()
    {
        assertEquals(0F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(0.99F));
        assertEquals(1F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(1F));
        assertEquals(2F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(2F));
        assertEquals(2.5F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(3F));
        assertEquals(2.5F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(10F));
    }

    @Test
    void syntheticWeaponClassesProduceTheRequiredQualitativeBalance()
    {
        // Rifle vs soft target works; no P100 is needed for 0 mm armour.
        assertTrue(resolve(true, 12.8F, 1F, 760D, unarmoured(), null).damage() > 10F);
        // Rifle with no penetration cannot chip armour.
        assertEquals(0F, resolve(true, 12.8F, 1F, 760D, armoured(40F), null).damage());
        // .50-like against thin versus heavy armour.
        assertTrue(resolve(true, 45F, 1F, 890D, armoured(13F), 20F).damage() > 0F);
        assertEquals(0F, resolve(true, 45F, 1F, 890D, armoured(80F), 20F).damage());
        // 20 mm-like against light armour.
        assertTrue(resolve(true, 130F, 1F, 830D, armoured(20F), 35F).damage() > 40F);
        // 75 mm-like: sloped heavy front blocked, side penetrated.
        assertEquals(0F, resolve(true, 6_790F, 100F, 618D, armoured(139.47F), 98F).damage());
        assertTrue(resolve(true, 6_790F, 100F, 618D, armoured(80F), 98F).damage() > 200F);

        // 88 mm-like massively overmatches and overkills a light core.
        float lightDamage = resolve(true, 10_200F, 100F, 773D, armoured(13F), 151F).damage();
        assertTrue(lightDamage > 1_010F);
        // Heavy versus heavy remains within the intended one-to-three penetrating hits.
        float heavyDamage = resolve(true, 10_200F, 100F, 773D, armoured(101F), 151F).damage();
        assertTrue(2_336F / heavyDamage >= 1F && 2_336F / heavyDamage <= 3F);
    }

    @Test
    void categorized88mmPzgr39MeetsVehicleLethalityTargets()
    {
        // Values below combine bullet_categories.json's 88 mm Pzgr.39 with category-derived vehicle mass/armour
        // and the positive SetupPart weights from the named content-pack definitions.
        assertHits(1, 3, armoured(50F), partHp(30_300F, 10_720F, 37_240F)); // official Sherman
        assertHits(1, 3, armoured(50F),                                  // Warfare44 Sherman
            partHp(30_300F, 1_250F, 2_850F), partHp(30_300F, 1_000F, 2_850F));
        assertHits(1, 3, armoured(90F), partHp(32_000F, 10_670F, 37_140F)); // official T-34-85
        assertHits(1, 3, armoured(90F),                                  // Warfare44 T-34-76
            partHp(30_900F, 1_750F, 4_100F), partHp(30_900F, 1_750F, 4_100F));
        assertHits(1, 3, armoured(90F),                                  // Warfare44 T-34-85
            partHp(32_000F, 2_000F, 5_200F), partHp(32_000F, 2_000F, 5_200F));
        assertHits(1, 1, armoured(30F), partHp(11_800F, 8_720F, 29_240F)); // official Luchs
        assertHits(1, 1, armoured(30F), partHp(11_740F, 950F, 950F));      // Warfare44 Puma
        assertHits(1, 1, armoured(28F), partHp(15_200F, 1_500F, 2_100F)); // Warfare44 M5A1
    }

    private static void assertHits(int minimum, int maximum, ResolvedArmorHit armor, float... layerHp)
    {
        float damage = resolve(true, 10_200F, 100F, 773D, armor, 162F).damage();
        int hits = 0;
        for (float hp : layerHp)
            hits += (int) Math.ceil(hp / damage);
        assertTrue(hits >= minimum && hits <= maximum, "expected " + minimum + "-" + maximum
            + " hits, got " + hits + " (damage=" + damage + ")");
    }

    private static float partHp(float massKg, float partWeight, float totalWeight)
    {
        return (float) (5D * Math.pow(massKg, 2D / 3D) * partWeight / totalWeight);
    }

    private static VehicleProjectileDamageResolver.Result resolve(boolean normalized, float mass, float fixed,
                                                                    double velocityMs, ResolvedArmorHit armor,
                                                                    Float p100)
    {
        return VehicleProjectileDamageResolver.resolve(normalized, mass, fixed, velocityMs / 20D,
            armor, p100);
    }

    private static ResolvedArmorHit unarmoured()
    {
        return armoured(0F);
    }

    private static ResolvedArmorHit armoured(float effectiveMm)
    {
        ArmorPlate authored = new ArmorPlate(effectiveMm, 0F);
        return new ResolvedArmorHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, authored,
            new Vec3(0D, 0D, -1D), 0F, effectiveMm);
    }
}
