package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
    void heatHitsUseTheFaceProtectionAgainstHeatThroughTheSameSlope()
    {
        ResolvedVehicleArmor glacis = resolved(new ArmorPlate(80F, 55F, 160F));
        Vec3 headOn = new Vec3(0D, 0D, 1D);
        assertEquals(139.47F, glacis.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false)
            .effectiveArmorMm(), 0.02F);
        assertEquals(278.95F, glacis.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, true)
            .effectiveArmorMm(), 0.05F);
    }

    @Test
    void aFaceWithNoProtectionAgainstHeatIsUnarmouredForAHeatHit()
    {
        ResolvedVehicleArmor armor = resolved(new ArmorPlate(20F, 0F, 0F));
        ResolvedArmorHit heat = armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT,
            new Vec3(0D, 0D, 1D), 80D, true);
        assertFalse(heat.isArmoured());
        assertEquals(0F, heat.effectiveArmorMm());
        assertTrue(hit(armor, new Vec3(0D, 0D, 1D)).isArmoured());
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
        assertTrue(armor.isPlateConfigured(EnumDriveablePart.CORE, EnumArmorFacing.TOP));
        assertFalse(armor.isPlateConfigured(EnumDriveablePart.CORE, EnumArmorFacing.FRONT));
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
        assertTrue(armor.isPlateConfigured(EnumDriveablePart.LEFT_TRACK, EnumArmorFacing.REAR));
        assertTrue(armor.isPlateConfigured(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT));
        assertFalse(armor.isPlateConfigured(EnumDriveablePart.TURRET, EnumArmorFacing.REAR));
    }

    @Test
    void krishnaTurretPartsUseTurretArmorWhileOtherExtraArmorUsesHullArmor()
    {
        VehicleArmorSpec spec = new VehicleArmorSpec(
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(40F, 0F)),
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 0F)), Map.of());
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(spec, List.of(
            EnumDriveablePart.TURRET_ARMOR, EnumDriveablePart.MORE_TURRET_ARMOR,
            EnumDriveablePart.TURRET_SIDE, EnumDriveablePart.TURRET_SKIRT,
            EnumDriveablePart.TURRET_WEAK, EnumDriveablePart.TURRET_WEAK_2,
            EnumDriveablePart.COMPOSITE));

        assertEquals(100F, armor.plate(EnumDriveablePart.TURRET_ARMOR, EnumArmorFacing.FRONT)
            .authored().thicknessMm());
        assertEquals(100F, armor.plate(EnumDriveablePart.MORE_TURRET_ARMOR, EnumArmorFacing.FRONT)
            .authored().thicknessMm());
        assertEquals(100F, armor.plate(EnumDriveablePart.TURRET_WEAK_2, EnumArmorFacing.FRONT)
            .authored().thicknessMm());
        assertEquals(40F, armor.plate(EnumDriveablePart.COMPOSITE, EnumArmorFacing.FRONT)
            .authored().thicknessMm());
    }

    @Test
    void shotOffFrontalArmorOpensOtherHullPartsButCoreKeepsItsPlate()
    {
        VehicleArmorSpec spec = new VehicleArmorSpec(
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 9F), EnumArmorFacing.LEFT, new ArmorPlate(80F, 0F)),
            Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 0F)),
            Map.of(EnumDriveablePart.LEFT_TRACK, new ArmorPlate(15F, 0F)));
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(spec, List.of(EnumDriveablePart.CORE, EnumDriveablePart.TURRET,
            EnumDriveablePart.LEFT_TRACK, EnumDriveablePart.FRONTAL_ARMOR, EnumDriveablePart.LEFT_SKIRT, EnumDriveablePart.ENGINE_ROOM_1));
        Vec3 headOn = new Vec3(0D, 0D, 1D);
        java.util.function.Predicate<EnumDriveablePart> frontGone = part -> part == EnumDriveablePart.FRONTAL_ARMOR;
        java.util.function.Predicate<EnumDriveablePart> nothingGone = part -> false;

        assertTrue(armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, frontGone).isArmoured());
        assertTrue(armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, true, frontGone).isArmoured());
        assertEquals(100F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, frontGone).authored().thicknessMm());
        assertFalse(armor.resolveHit(EnumDriveablePart.ENGINE_ROOM_1, EnumArmorFacing.FRONT, headOn, 80D, false, frontGone).isArmoured());
        assertFalse(armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, nothingGone).isArmoured());
        assertTrue(armor.resolveHit(EnumDriveablePart.FRONTAL_ARMOR, EnumArmorFacing.FRONT, headOn, 80D, false, nothingGone).isArmoured());
        // Other faces, the turret, parts with their own plate and other armour parts keep their plates.
        assertEquals(80F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.LEFT, frontGone).authored().thicknessMm());
        assertEquals(100F, armor.plate(EnumDriveablePart.TURRET, EnumArmorFacing.FRONT, frontGone).authored().thicknessMm());
        assertEquals(15F, armor.plate(EnumDriveablePart.LEFT_TRACK, EnumArmorFacing.FRONT, frontGone).authored().thicknessMm());
        assertEquals(100F, armor.plate(EnumDriveablePart.LEFT_SKIRT, EnumArmorFacing.FRONT, frontGone).authored().thicknessMm());
    }

    @Test
    void carriedDamageSkipsOnlyTheMatchingInheritedCorePlate()
    {
        VehicleArmorSpec spec = new VehicleArmorSpec(Map.of(EnumArmorFacing.FRONT, new ArmorPlate(100F, 0F)), Map.of(), Map.of());
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(spec, List.of(EnumDriveablePart.CORE, EnumDriveablePart.FRONTAL_ARMOR));
        Vec3 headOn = new Vec3(0D, 0D, 1D);
        assertFalse(armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, part -> true, EnumDriveablePart.FRONTAL_ARMOR).isArmoured());
        assertTrue(armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, part -> true, EnumDriveablePart.LEFTSIDE_ARMOR).isArmoured());

        VehicleArmorSpec explicitCore = new VehicleArmorSpec(spec.hull(), spec.turret(), Map.of(EnumDriveablePart.CORE, new ArmorPlate(40F, 0F)));
        ResolvedVehicleArmor independent = VehicleArmorResolver.resolve(explicitCore, List.of(EnumDriveablePart.CORE, EnumDriveablePart.FRONTAL_ARMOR));
        assertEquals(40F, independent.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, part -> false).effectiveArmorMm());
        assertEquals(40F, independent.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, headOn, 80D, false, part -> true, EnumDriveablePart.FRONTAL_ARMOR).effectiveArmorMm());
    }

    @Test
    void coreKeepsBothSidePlatesAfterOuterSidePartsAreDestroyed()
    {
        VehicleArmorSpec spec = new VehicleArmorSpec(Map.of(EnumArmorFacing.LEFT, new ArmorPlate(80F, 0F), EnumArmorFacing.RIGHT, new ArmorPlate(70F, 0F)), Map.of(), Map.of());
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(spec, List.of(EnumDriveablePart.CORE, EnumDriveablePart.LEFTSIDE_ARMOR, EnumDriveablePart.RIGHTSIDE_ARMOR));
        assertEquals(80F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.LEFT, part -> true).authored().thicknessMm());
        assertEquals(70F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.RIGHT, part -> true).authored().thicknessMm());
        assertEquals(0F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.LEFT, part -> false).authored().thicknessMm());
        assertEquals(0F, armor.plate(EnumDriveablePart.CORE, EnumArmorFacing.RIGHT, part -> false).authored().thicknessMm());
    }

    @Test
    void aDriveableWithoutTheArmourPartIsNeverBreached()
    {
        ResolvedVehicleArmor armor = resolved(new ArmorPlate(100F, 0F));
        assertFalse(armor.isBreached(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, part -> true));
        assertEquals(100F, armor.resolveHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT, new Vec3(0D, 0D, 1D), 80D, false, part -> true).effectiveArmorMm(), 1.0E-3F);
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
