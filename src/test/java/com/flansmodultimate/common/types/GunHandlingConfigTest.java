package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GunHandlingConfigTest
{
    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        com.flansmodultimate.platform.entity.EntityTestBootstrap.initialize();
    }

    @Test
    void defaultsEnableMagazineTacticalReloadAndTimedSwitches()
    {
        GunType gun = gun();
        assertEquals(-1F, gun.getTacticalReloadTime());
        assertEquals(0.8F, gun.getTacticalReloadMultiplier());
        assertEquals(8F, gun.getSecondarySwitchTime());
        assertEquals(8F, gun.getSecondaryUnSwitchTime());
        assertTrue(gun.isSecondarySwitchMovement());
    }

    @Test
    void packControlsAcceptLegacySpellingAndProvideIndependentOptOuts()
    {
        GunType gun = gun("TacticalReloadTime 42", "TacticalReloadMultiplier 1", "SecondarySwitchTime 8.2", "SecondaryUnSwitchTime 0", "SecondarySwitchMovement false");
        assertEquals(42F, gun.getTacticalReloadTime());
        assertEquals(1F, gun.getTacticalReloadMultiplier());
        assertEquals(9F, gun.getSecondarySwitchTime());
        assertEquals(0F, gun.getSecondaryUnSwitchTime());
        assertFalse(gun.isSecondarySwitchMovement());
        assertEquals(-1F, gun("TacticalReloadTime NaN").getTacticalReloadTime());
        assertEquals(0.8F, gun("TacticalReloadMultiplier Infinity").getTacticalReloadMultiplier());
    }

    @Test
    void secondaryVisibilityIsAnAttachmentTrait()
    {
        AttachmentType attachment = new AttachmentType();
        attachment.load(file(EnumType.ATTACHMENT, "RenderOnlyOnMode true"));
        assertTrue(attachment.isRenderOnlyOnMode());
        assertFalse(new AttachmentType().isRenderOnlyOnMode());
    }

    @Test
    void equipSoundIsOptionalAndRegistersPackSound()
    {
        assertTrue(gun().getEquipSound() == null || gun().getEquipSound().isBlank());
        assertTrue(gun("EquipSound draw_weapon").getEquipSound().endsWith("draw_weapon"));
    }

    @Test
    void legacySightCorrectionsUseRawRenderUnitsAndRejectNonfiniteValues()
    {
        GunType type = gun("IronSightOffset 0.01 -0.0075 0.2", "IronSightPitch 0.65");
        assertEquals(0.01F, type.getIronSightOffset().x);
        assertEquals(-0.0075F, type.getIronSightOffset().y);
        assertEquals(0.2F, type.getIronSightOffset().z);
        assertEquals(0.65F, type.getIronSightPitch());
        assertEquals(0F, gun("IronSightPitch NaN").getIronSightPitch());
        assertEquals(0F, gun("IronSightOffset Infinity 0 0").getIronSightOffset().x);
        assertEquals(0F, gun().getIronSightPitch());
    }

    @Test
    void legacyLockAliasesAndTimingControlsHaveCompatibleDefaults()
    {
        GunType defaults = gun();
        assertEquals(0.66F, defaults.getAimTimeConstant());
        assertEquals(-1F, defaults.getRunPosTime());
        GunType type = gun("CanLockOnAngle 60", "LockOnToVehicle true", "AimTimeConstant 0.5", "RunPosTime 6");
        assertEquals(60, type.getCanLockOnAngle());
        assertTrue(type.isLockOnToVehicles());
        assertEquals(0.5F, type.getAimTimeConstant());
        assertEquals(6F, type.getRunPosTime());
        GunType canonical = gun("CanLockOnAngle 60", "CanLockAngle 30", "LockOnToVehicle true", "LockOnToVehicles false");
        assertEquals(30, canonical.getCanLockOnAngle());
        assertFalse(canonical.isLockOnToVehicles());
        assertEquals(0.95F, gun("AimTimeConstant 2").getAimTimeConstant());
        assertEquals(0.66F, gun("AimTimeConstant NaN").getAimTimeConstant());
        assertEquals(-1F, gun("RunPosTime Infinity").getRunPosTime());
        assertEquals(0F, gun("RunPosTime 0").getRunPosTime());
        BulletType bullet = new BulletType();
        bullet.load(file(EnumType.BULLET, "LockOnToVehicle true"));
        assertTrue(bullet.isLockOnToVehicles());
    }

    @Test
    void rpmAliasNeverOverridesCategoryRoundsPerMin()
    {
        TypeFile definition = file(EnumType.GUN, "RPM 700", "ShootDelay 10");
        var category = new com.flansmodultimate.config.Category(EnumType.GUN, "Cadence");
        category.setProperties(java.util.Map.of("RoundsPerMin", List.of("300")));
        definition.addCategoryConfigMap(category, "handling");
        GunType type = new GunType();
        type.load(definition);
        assertEquals(4F, type.getShootDelay(null));
        assertEquals(1200F / 700F, gun("RPM 700").getShootDelay(null), 0.0001F);
        assertEquals(2F, gun("RPM 700", "RoundsPerMin 600").getShootDelay(null));
        assertEquals(2F, gun("RoundsPerMin 600", "RPM 700").getShootDelay(null));
    }

    @Test
    void pluralDescriptionIsAnInheritedAliasAndCanonicalTextWins()
    {
        for (InfoType type : List.of(new BulletType(), new AttachmentType(), new GunType()))
        {
            type.load(file(EnumType.BULLET, "Descriptions First line_Second line"));
            assertEquals("First line_Second line", type.getDescription());
            type.load(file(EnumType.BULLET, "Descriptions Alias", "Description Canonical"));
            assertEquals("Canonical", type.getDescription());
        }
        assertTrue(gun("ActionEndSound bolt_close").getActionEndSound().endsWith("bolt_close"));
    }

    private static GunType gun(String... lines)
    {
        GunType type = new GunType();
        type.load(file(EnumType.GUN, lines));
        return type;
    }

    private static TypeFile file(EnumType type, String... lines)
    {
        return new TypeFile("handling", type, new ContentPack("test", Path.of("build", "test-packs", "handling")), List.of(lines));
    }
}
