package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableTypeWeaponSoundTest
{
    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        com.flansmodultimate.platform.entity.EntityTestBootstrap.initialize();
    }

    @Test
    void explicitPrimarySoundOutranksTypedAndGenericSounds()
    {
        assertEquals("primary", sounds(EnumWeaponType.SHELL, EnumWeaponType.NONE).shootSound(false));
        assertEquals("primary", sounds(EnumWeaponType.BOMB, EnumWeaponType.NONE).shootSound(false));
        assertEquals("primary", sounds(EnumWeaponType.GUN, EnumWeaponType.NONE).shootSound(false));
    }

    @Test
    void explicitSecondarySoundOutranksTypedAndGenericSounds()
    {
        assertEquals("secondary", sounds(EnumWeaponType.NONE, EnumWeaponType.SHELL).shootSound(true));
        assertEquals("secondary", sounds(EnumWeaponType.NONE, EnumWeaponType.BOMB).shootSound(true));
        assertEquals("secondary", sounds(EnumWeaponType.NONE, EnumWeaponType.GUN).shootSound(true));
    }

    @Test
    void shellBankFallsBackFromBankSoundToShellSoundThenGenericSound()
    {
        DriveableType primary = sounds(EnumWeaponType.SHELL, EnumWeaponType.NONE);
        primary.shootSoundPrimary = "";
        assertEquals("shell", primary.shootSound(false));
        primary.shellSound = "";
        assertEquals("generic", primary.shootSound(false));

        DriveableType secondary = sounds(EnumWeaponType.NONE, EnumWeaponType.SHELL);
        secondary.shootSoundSecondary = "";
        assertEquals("shell", secondary.shootSound(true));
        secondary.shellSound = "";
        assertEquals("generic", secondary.shootSound(true));
    }

    @Test
    void bombBankFallsBackFromBankSoundToBombSoundThenGenericSound()
    {
        DriveableType primary = sounds(EnumWeaponType.BOMB, EnumWeaponType.NONE);
        primary.shootSoundPrimary = "";
        assertEquals("bomb", primary.shootSound(false));
        primary.bombSound = "";
        assertEquals("generic", primary.shootSound(false));

        DriveableType secondary = sounds(EnumWeaponType.NONE, EnumWeaponType.BOMB);
        secondary.shootSoundSecondary = "";
        assertEquals("bomb", secondary.shootSound(true));
        secondary.bombSound = "";
        assertEquals("generic", secondary.shootSound(true));
    }

    @Test
    void gunBankIgnoresShellAndBombSounds()
    {
        DriveableType primary = sounds(EnumWeaponType.GUN, EnumWeaponType.NONE);
        primary.shootSoundPrimary = "";
        assertEquals("generic", primary.shootSound(false));

        DriveableType secondary = sounds(EnumWeaponType.NONE, EnumWeaponType.GUN);
        secondary.shootSoundSecondary = "";
        assertEquals("generic", secondary.shootSound(true));
    }

    @Test
    void distantSoundsAreOptionalAndBanksRemainIndependent()
    {
        VehicleType type = new VehicleType();
        org.junit.jupiter.api.Assertions.assertTrue(type.getDistantShootSoundPrimary().isBlank());
        org.junit.jupiter.api.Assertions.assertTrue(type.getDistantShootSoundSecondary().isBlank());
        var pack = new com.flansmodultimate.content.ContentPack("test", java.nio.file.Path.of("build", "test-packs", "weapon-sounds"));
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, java.util.List.of("Driver 0 0 0", "farSound distant_main", "DistantSoundSecondary distant_coax", "farSoundRange 350")));
        org.junit.jupiter.api.Assertions.assertTrue(type.getDistantShootSoundPrimary().endsWith("distant_main"));
        org.junit.jupiter.api.Assertions.assertTrue(type.getDistantShootSoundSecondary().endsWith("distant_coax"));
        assertEquals(350, type.getDistantSoundRange());
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, java.util.List.of("Driver 0 0 0", "DistantSoundPrimary cannon", "DistantSoundRange 400")));
        org.junit.jupiter.api.Assertions.assertTrue(type.getDistantShootSoundPrimary().endsWith("cannon"));
        assertEquals(400, type.getDistantSoundRange());
    }

    private static DriveableType sounds(EnumWeaponType primary, EnumWeaponType secondary)
    {
        DriveableType type = new DriveableType();
        type.primary = primary;
        type.secondary = secondary;
        type.shootSoundPrimary = "primary";
        type.shootSoundSecondary = "secondary";
        type.shootSound = "generic";
        type.shellSound = "shell";
        type.bombSound = "bomb";
        return type;
    }
}
