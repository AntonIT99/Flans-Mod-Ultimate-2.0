package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableTypeWeaponSoundTest
{
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
