package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.config.ModCommonConfig;
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

    @Test
    void slowShapedChargeIsCreditedWithItsChargeNotItsSpeed()
    {
        // Panzerfaust 60: 2.9 kg warhead at 45 m/s, 0.95 kg charge, 200 mm penetration.
        VehicleProjectileDamageResolver.Result kinetic = resolve(true, 2_900F, 4_500F, 45D, armoured(80F), 200F);
        VehicleProjectileDamageResolver.Result heat = panzerfaust(armoured(80F));
        assertTrue(kinetic.damage() < 100F, "kinetic damage alone was " + kinetic.damage());
        assertTrue(heat.shapedCharge());
        assertFalse(heat.kineticDamage());
        assertEquals(HEAT_REFERENCE * Math.sqrt(0.95D), heat.damage(), 0.5D);
    }

    @Test
    void panzerfaustNeedsTwoPenetratingHitsOnAPanzerIvHull()
    {
        float hullHp = partHp(25_000F, 10_720F, 37_830F); // official Panzer IV core
        assertEquals(2, (int) Math.ceil(hullHp / panzerfaust(armoured(80F)).damage()));
        assertEquals(2, (int) Math.ceil(hullHp / panzerfaust(armoured(30F)).damage()));
    }

    @Test
    void aJetThatBarelyPenetratesDeliversLessThanOneWithAMargin()
    {
        // 60 mm Bazooka: 420 g charge, 100 mm penetration, against a frontal and a side plate.
        float frontal = resolveHeat(1_590F, 82D, armoured(80F), 100F, 0.42F).damage();
        float side = resolveHeat(1_590F, 82D, armoured(30F), 100F, 0.42F).damage();
        assertTrue(frontal < side);
        assertEquals(VehicleProjectileDamageResolver.MIN_SHAPED_CHARGE_RESIDUAL,
            VehicleProjectileDamageResolver.shapedChargeResidual(1F));
        assertEquals(1F, VehicleProjectileDamageResolver.shapedChargeResidual(2F));
        assertEquals(1F, VehicleProjectileDamageResolver.shapedChargeResidual(9F));
    }

    @Test
    void shapedChargeStoppedByArmourDoesNothingThroughTheJet()
    {
        VehicleProjectileDamageResolver.Result blocked = resolveHeat(1_590F, 82D, armoured(120F), 100F, 0.42F);
        assertEquals(0F, blocked.damage());
        assertFalse(blocked.shapedCharge());
    }

    @Test
    void shapedChargeChannelIsLimitedToArmouredTargetsOnRealisticHealth()
    {
        // A soft target is left to the kinetic hit and the ungated blast.
        VehicleProjectileDamageResolver.Result soft = panzerfaust(unarmoured());
        assertFalse(soft.shapedCharge());
        assertTrue(soft.damage() < 100F);

        // Legacy health keeps the authored damage, which already stands for the whole warhead.
        VehicleProjectileDamageResolver.Result legacy = VehicleProjectileDamageResolver.resolve(false, 2_900F,
            4_500F, 45D / 20D, armoured(80F), 200F, 0.95F, HEAT_REFERENCE);
        assertEquals(4_500F, legacy.damage());
        assertFalse(legacy.shapedCharge());
    }

    @Test
    void fastHeatShellKeepsItsKineticDamageWhenThatIsHigher()
    {
        // 114 mm HEAT shell: 20 kg at 750 m/s with a 2 kg charge.
        VehicleProjectileDamageResolver.Result plain = resolve(true, 20_000F, 100F, 750D, armoured(100F), 300F);
        VehicleProjectileDamageResolver.Result heat = resolveHeat(20_000F, 750D, armoured(100F), 300F, 2F);
        assertEquals(plain.damage(), heat.damage());
        assertTrue(heat.kineticDamage());
        assertTrue(heat.shapedCharge());
    }

    private static final double HEAT_REFERENCE = ModCommonConfig.DEFAULT_HEAT_DAMAGE_REFERENCE;

    private static VehicleProjectileDamageResolver.Result panzerfaust(ResolvedArmorHit armor)
    {
        return resolveHeat(2_900F, 45D, armor, 200F, 0.95F);
    }

    private static VehicleProjectileDamageResolver.Result resolveHeat(float mass, double velocityMs,
                                                                        ResolvedArmorHit armor, float p100,
                                                                        float chargeKg)
    {
        return VehicleProjectileDamageResolver.resolve(true, mass, 100F, velocityMs / 20D, armor, p100,
            chargeKg, HEAT_REFERENCE);
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
