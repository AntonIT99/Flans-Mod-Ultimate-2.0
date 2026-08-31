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
        assertEquals(1.5F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(2F));
        assertEquals(2F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(3F));
        assertEquals(2F, VehicleProjectileDamageResolver.hybridOvermatchMultiplier(10F));
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

        // 88 mm-like vs a light core is about two penetrating hits.
        float lightDamage = resolve(true, 10_200F, 100F, 773D, armoured(13F), 151F).damage();
        assertTrue(1_010F / lightDamage >= 1F && 1_010F / lightDamage <= 3F);
        // Heavy versus heavy remains several hits rather than one or dozens.
        float heavyDamage = resolve(true, 10_200F, 100F, 773D, armoured(101F), 151F).damage();
        assertTrue(2_336F / heavyDamage >= 4F && 2_336F / heavyDamage <= 8F);
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
