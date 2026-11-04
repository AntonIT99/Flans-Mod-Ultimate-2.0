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
