package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleArmorResolverTest
{
    @Test
    void flatAndPantherStyleHeadOnThicknessUseTheVirtualNormal()
    {
        ResolvedVehicleArmor flat = resolved(new ArmorPlate(100F, 0F));
        assertEquals(100F, hit(flat, new Vec3(0D, 0D, 1D)).effectiveArmorMm(), 1.0E-3F);

        ResolvedVehicleArmor glacis = resolved(new ArmorPlate(80F, 55F));
        ResolvedArmorHit hit = hit(glacis, new Vec3(0D, 0D, 1D));
        assertEquals(139.47F, hit.effectiveArmorMm(), 0.02F);
        assertEquals(55F, hit.impactAngleDeg(), 1.0E-3F);
    }

    @Test
    void actualObliquityCombinesWithTheAuthoredNormalInOneDotProduct()
    {
        ResolvedVehicleArmor armor = resolved(new ArmorPlate(100F, 0F));
        ResolvedArmorHit hit = hit(armor, new Vec3(1D, 0D, 1D));
        assertEquals(141.42F, hit.effectiveArmorMm(), 0.02F);
    }

    @Test
    void grazingAngleIsCappedAndFinite()
    {
        ResolvedArmorHit hit = hit(resolved(new ArmorPlate(100F, 0F)), new Vec3(1D, 0D, 0D));
        assertTrue(Float.isFinite(hit.effectiveArmorMm()));
        assertEquals(575.88F, hit.effectiveArmorMm(), 0.1F);
    }

    @Test
    void explicitZeroIsUnarmouredAndMissingEventuallyFallsBackToZero()
    {
        EnumMap<EnumArmorFacing, ArmorPlate> hull = new EnumMap<>(EnumArmorFacing.class);
        hull.put(EnumArmorFacing.TOP, ArmorPlate.UNARMOURED);
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(
            new VehicleArmorSpec(hull, Map.of(), Map.of()), List.of(EnumDriveablePart.CORE));
        assertEquals(0F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.TOP).authored().thicknessMm());
        assertEquals(0F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.FRONT).authored().thicknessMm());
    }

    @Test
    void partOverrideBeatsSemanticAndTurretSemanticBeatsHull()
    {
        VehicleArmorSpec spec = new VehicleArmorSpec(
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(40F, 0F)),
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 0F)),
            Map.of(EnumDriveablePart.LEFT_TRACK, new ArmorPlate(15F, 0F)));
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(spec,
            List.of(EnumDriveablePart.CORE, EnumDriveablePart.TURRET, EnumDriveablePart.LEFT_TRACK));
        assertEquals(40F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.FRONT).authored().thicknessMm());
        assertEquals(100F, armor.plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT).authored().thicknessMm());
        assertEquals(15F, armor.plate(EnumDriveablePart.LEFT_TRACK, EnumArmorFacing.FRONT).authored().thicknessMm());
    }

    private static ResolvedVehicleArmor resolved(ArmorPlate front)
    {
        return VehicleArmorResolver.resolve(new VehicleArmorSpec(
            Map.of(EnumArmorFacing.FRONT, front), Map.of(), Map.of()), List.of(EnumDriveablePart.CORE));
    }

    private static ResolvedArmorHit hit(ResolvedVehicleArmor armor, Vec3 direction)
    {
        return armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, direction, 80D);
    }
}
