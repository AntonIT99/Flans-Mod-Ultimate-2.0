package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Balance of Second World War HEAT weapons against the tanks they met, using the shipped
 * {@code bullet_categories.json} rounds, the {@code vehicle_categories.json} hull armour and mass, and the
 * official pack's {@code SetupPart} weights for the hull's share of the derived health.
 *
 * <p>The targets are a gameplay reading of the historical record: a Panzerfaust or Panzerschreck that gets through
 * nearly always knocked out a medium tank, heavy tanks could take a hit, the thickest frontal armour of the war
 * defeated the warheads of its day, and the lighter Allied launchers had to find a flank.</p>
 */
class WorldWarTwoAntiTankBalanceTest
{
    private record Weapon(String name, float massG, double velocityMs, float penetrationMm, float chargeKg) {}

    private record Tank(String name, float massKg, float hullWeight, float totalWeight, ArmorPlate front,
                        ArmorPlate side)
    {
        float hullHp()
        {
            return (float) (ModCommonConfig.DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE * Math.pow(massKg, 2D / 3D)
                * hullWeight / totalWeight);
        }
    }

    // Handheld launchers.
    private static final Weapon PANZERFAUST_60 = new Weapon("Panzerfaust 60", 2_900F, 45D, 200F, 0.95F);
    private static final Weapon PANZERSCHRECK = new Weapon("Panzerschreck", 3_300F, 110D, 210F, 0.86F);
    private static final Weapon BAZOOKA_M6A3 = new Weapon("Bazooka M6A3", 1_590F, 82D, 100F, 0.42F);
    private static final Weapon PIAT = new Weapon("PIAT", 1_130F, 76D, 100F, 0.40F);
    // HEAT shells.
    private static final Weapon HL_GR_38C = new Weapon("75mm Hl.Gr. 38C", 4_800F, 450D, 100F, 0.53F);
    private static final Weapon BP_460A = new Weapon("122mm BP-460A", 13_340F, 329D, 160F, 2.18F);

    // Hull front, hull side.
    private static final Tank PANZER_IV = new Tank("Panzer IV Ausf. H", 25_000F, 10_720F, 37_830F,
        new ArmorPlate(80F, 10F), new ArmorPlate(30F, 0F));
    private static final Tank SHERMAN = new Tank("M4 Sherman", 30_300F, 10_720F, 37_240F,
        new ArmorPlate(50F, 56F), new ArmorPlate(38F, 0F));
    private static final Tank T_34_85 = new Tank("T-34-85", 32_000F, 10_670F, 37_140F,
        new ArmorPlate(45F, 60F), new ArmorPlate(45F, 40F));
    private static final Tank TIGER = new Tank("Tiger I", 57_250F, 13_500F, 42_800F,
        new ArmorPlate(100F, 9F), new ArmorPlate(80F, 0F));
    private static final Tank KING_TIGER = new Tank("Tiger II", 69_800F, 13_500F, 42_800F,
        new ArmorPlate(150F, 50F), new ArmorPlate(80F, 25F));
    private static final Tank IS_2 = new Tank("IS-2", 46_000F, 13_500F, 42_800F,
        new ArmorPlate(100F, 60F), new ArmorPlate(90F, 0F));
    private static final Tank CHURCHILL = new Tank("Churchill Mk VII", 40_600F, 12_600F, 41_400F,
        new ArmorPlate(152F, 0F), new ArmorPlate(95F, 0F));
    private static final Tank LUCHS = new Tank("Luchs", 11_800F, 8_720F, 29_240F,
        new ArmorPlate(30F, 0F), new ArmorPlate(20F, 0F));
    private static final Tank SDKFZ_251 = new Tank("Sd.Kfz. 251", 7_000F, 540F, 1_520F,
        new ArmorPlate(14.5F, 0F), new ArmorPlate(8F, 0F));

    private static final List<Weapon> GERMAN_LAUNCHERS = List.of(PANZERFAUST_60, PANZERSCHRECK);
    private static final List<Weapon> ALLIED_LAUNCHERS = List.of(BAZOOKA_M6A3, PIAT);

    @Test
    void germanLaunchersKnockOutMediumTanksInTwoHitsFromAnyAngle()
    {
        for (Weapon weapon : GERMAN_LAUNCHERS)
        {
            for (Tank tank : List.of(PANZER_IV, SHERMAN, T_34_85))
            {
                for (EnumArmorFacing facing : List.of(EnumArmorFacing.FRONT, EnumArmorFacing.LEFT))
                {
                    assertHits(2, 2, weapon, tank, facing);
                    float share = damage(weapon, tank, facing) / tank.hullHp();
                    assertTrue(share >= 2F / 3F, weapon.name() + " on " + tank.name() + " " + facing
                        + " took only " + Math.round(share * 100F) + "% of the hull; the first hit should disable");
                }
            }
        }
    }

    @Test
    void germanLaunchersDestroyLightArmourWithOneHit()
    {
        for (Weapon weapon : GERMAN_LAUNCHERS)
        {
            for (Tank tank : List.of(LUCHS, SDKFZ_251))
            {
                assertHits(1, 1, weapon, tank, EnumArmorFacing.FRONT);
                assertHits(1, 1, weapon, tank, EnumArmorFacing.LEFT);
            }
        }
    }

    @Test
    void heavyTanksSurviveOnePenetratingHitButNotThree()
    {
        for (Weapon weapon : GERMAN_LAUNCHERS)
        {
            assertHits(2, 3, weapon, TIGER, EnumArmorFacing.FRONT);
            assertHits(2, 3, weapon, TIGER, EnumArmorFacing.LEFT);
            assertHits(2, 3, weapon, IS_2, EnumArmorFacing.LEFT);
            assertHits(2, 3, weapon, CHURCHILL, EnumArmorFacing.FRONT);
            assertHits(2, 3, weapon, CHURCHILL, EnumArmorFacing.LEFT);
            assertHits(2, 3, weapon, KING_TIGER, EnumArmorFacing.LEFT);
        }
    }

    @Test
    void theThickestFrontalArmourDefeatsTheWarheadsOfItsDay()
    {
        // The Tiger II glacis, 150 mm at 50 degrees, resists about 233 mm head on.
        for (Weapon weapon : GERMAN_LAUNCHERS)
            assertBlocked(weapon, KING_TIGER, EnumArmorFacing.FRONT);
        for (Weapon weapon : ALLIED_LAUNCHERS)
        {
            assertBlocked(weapon, TIGER, EnumArmorFacing.FRONT);
            assertBlocked(weapon, IS_2, EnumArmorFacing.FRONT);
            assertBlocked(weapon, CHURCHILL, EnumArmorFacing.FRONT);
            assertBlocked(weapon, KING_TIGER, EnumArmorFacing.FRONT);
        }
    }

    @Test
    void alliedLaunchersNeedAFlankShotOnMediumTanks()
    {
        for (Weapon weapon : ALLIED_LAUNCHERS)
        {
            for (Tank tank : List.of(PANZER_IV, SHERMAN, T_34_85))
            {
                // Frontal hits barely get through, so little of the jet reaches the inside.
                assertHits(3, 4, weapon, tank, EnumArmorFacing.FRONT);
                assertHits(2, 3, weapon, tank, EnumArmorFacing.LEFT);
                assertTrue(damage(weapon, tank, EnumArmorFacing.LEFT) > damage(weapon, tank, EnumArmorFacing.FRONT),
                    weapon.name() + " should hurt " + tank.name() + " more from the side");
            }
            assertHits(1, 2, weapon, LUCHS, EnumArmorFacing.LEFT);
            assertHits(1, 1, weapon, SDKFZ_251, EnumArmorFacing.LEFT);
            // A heavy tank's side is still penetrable, but takes many hits.
            assertHits(4, 6, weapon, TIGER, EnumArmorFacing.LEFT);
        }
    }

    @Test
    void largerChargesHitHarder()
    {
        float panzerfaust = damage(PANZERFAUST_60, PANZER_IV, EnumArmorFacing.LEFT);
        float panzerschreck = damage(PANZERSCHRECK, PANZER_IV, EnumArmorFacing.LEFT);
        float bazooka = damage(BAZOOKA_M6A3, PANZER_IV, EnumArmorFacing.LEFT);
        float piat = damage(PIAT, PANZER_IV, EnumArmorFacing.LEFT);
        assertTrue(panzerfaust > panzerschreck && panzerschreck > bazooka && bazooka > piat,
            "expected Panzerfaust > Panzerschreck > Bazooka > PIAT but got " + panzerfaust + ", " + panzerschreck
                + ", " + bazooka + ", " + piat);
    }

    @Test
    void heatTankShellsAreDecisiveAgainstTheArmourTheyWereIssuedFor()
    {
        // The Panzer IV's own HEAT round against a Sherman's flank, and the SU-122's against a Panzer IV.
        assertHits(1, 2, HL_GR_38C, SHERMAN, EnumArmorFacing.LEFT);
        assertHits(1, 1, BP_460A, PANZER_IV, EnumArmorFacing.FRONT);
        assertHits(1, 1, BP_460A, PANZER_IV, EnumArmorFacing.LEFT);
        assertHits(2, 3, BP_460A, TIGER, EnumArmorFacing.FRONT);
    }

    @Test
    void withoutTheShapedChargeChannelTheLaunchersWereUseless()
    {
        // The regression this channel exists to fix: the kinetic formula alone, at the round's real speed.
        VehicleProjectileDamageResolver.Result kineticOnly = VehicleProjectileDamageResolver.resolve(true,
            PANZERFAUST_60.massG(), 0F, PANZERFAUST_60.velocityMs() / 20D,
            armourHit(PANZER_IV, EnumArmorFacing.LEFT), PANZERFAUST_60.penetrationMm());
        assertTrue(PANZER_IV.hullHp() / kineticOnly.damage() > 10F,
            "kinetic damage alone was " + kineticOnly.damage());
        assertTrue(damage(PANZERFAUST_60, PANZER_IV, EnumArmorFacing.LEFT) > 10F * kineticOnly.damage());
    }

    private static void assertHits(int minimum, int maximum, Weapon weapon, Tank tank, EnumArmorFacing facing)
    {
        float damage = damage(weapon, tank, facing);
        assertTrue(damage > 0F, weapon.name() + " failed to penetrate " + tank.name() + " " + facing);
        int hits = (int) Math.ceil(tank.hullHp() / damage);
        assertTrue(hits >= minimum && hits <= maximum, weapon.name() + " on " + tank.name() + " " + facing
            + ": expected " + minimum + "-" + maximum + " hits, got " + hits + " (damage " + damage
            + ", hull " + tank.hullHp() + " HP)");
    }

    private static void assertBlocked(Weapon weapon, Tank tank, EnumArmorFacing facing)
    {
        VehicleProjectileDamageResolver.Result result = resolve(weapon, tank, facing);
        assertFalse(result.penetration().penetrated(), weapon.name() + " should not penetrate " + tank.name()
            + " " + facing + " (" + result.penetration().effectiveArmorMm() + " mm)");
        assertEquals(0F, result.damage());
    }

    private static float damage(Weapon weapon, Tank tank, EnumArmorFacing facing)
    {
        return resolve(weapon, tank, facing).damage();
    }

    private static VehicleProjectileDamageResolver.Result resolve(Weapon weapon, Tank tank, EnumArmorFacing facing)
    {
        return VehicleProjectileDamageResolver.resolve(true, weapon.massG(), 0F, weapon.velocityMs() / 20D,
            armourHit(tank, facing), weapon.penetrationMm(), weapon.chargeKg(),
            ModCommonConfig.DEFAULT_HEAT_DAMAGE_REFERENCE);
    }

    /** A level shot straight at the face, resolved against its protection against HEAT. */
    private static ResolvedArmorHit armourHit(Tank tank, EnumArmorFacing facing)
    {
        ResolvedVehicleArmor armour = VehicleArmorResolver.resolve(new VehicleArmorSpec(
            Map.of(EnumArmorFacing.FRONT, tank.front(), EnumArmorFacing.LEFT, tank.side(),
                EnumArmorFacing.RIGHT, tank.side()), Map.of(), Map.of()), List.of(EnumDriveablePart.CORE));
        Vec3 headOn = facing.outwardNormal().scale(-1D);
        return armour.resolveHit(EnumDriveablePart.CORE, facing, headOn, ModCommonConfig.DEFAULT_MAX_ARMOR_IMPACT_ANGLE_DEG,
            true);
    }
}
