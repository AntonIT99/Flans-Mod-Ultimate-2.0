package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DriveableAmmoLoaderTest
{
    @Test
    void prefersTheWeaponTypeTheDefinitionDeclaresForEachBank()
    {
        assertEquals(List.of(EnumWeaponType.MINE, EnumWeaponType.BOMB),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.MINE, EnumWeaponType.GUN, true));
        assertEquals(List.of(EnumWeaponType.SHELL, EnumWeaponType.MISSILE),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.SHELL, EnumWeaponType.GUN, false));
        assertEquals(List.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.MISSILE, false));
    }

    @Test
    void keepsBothBankTypesWhenOneDefinitionDeclaresBoth()
    {
        assertEquals(List.of(EnumWeaponType.SHELL, EnumWeaponType.MISSILE),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.SHELL, EnumWeaponType.MISSILE, false));
        assertEquals(List.of(EnumWeaponType.MINE, EnumWeaponType.BOMB),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.MINE, EnumWeaponType.BOMB, true));
    }

    @Test
    void fallsBackToTheCanonicalOrderWhenNoBankIsDeclared()
    {
        assertEquals(List.of(EnumWeaponType.BOMB, EnumWeaponType.MINE),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.NONE, EnumWeaponType.GUN, true));
        assertEquals(List.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL),
            DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.NONE, EnumWeaponType.NONE, false));
    }

    @Test
    void neverOffersTheOtherBanksWeaponTypes()
    {
        assertFalse(DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.SHELL, true)
            .contains(EnumWeaponType.SHELL));
        assertFalse(DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.SHELL, false)
            .contains(EnumWeaponType.BOMB));
    }
}
