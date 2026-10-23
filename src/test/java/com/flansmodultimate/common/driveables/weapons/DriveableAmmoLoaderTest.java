package com.flansmodultimate.common.driveables.weapons;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class DriveableAmmoLoaderTest
{
    @Test
    void defaultOrdnanceStaysInTheSelectedWeaponBank()
    {
        DriveableType type = mock(DriveableType.class);
        BulletType shell = mock(BulletType.class);
        BulletType bomb = mock(BulletType.class);
        when(type.weaponType(false)).thenReturn(EnumWeaponType.SHELL);
        when(type.weaponType(true)).thenReturn(EnumWeaponType.BOMB);
        when(shell.getWeaponType()).thenReturn(EnumWeaponType.SHELL);
        when(bomb.getWeaponType()).thenReturn(EnumWeaponType.BOMB);
        when(type.getAmmoTypes()).thenReturn(List.of(bomb, shell));
        assertSame(shell, DriveableAmmoLoader.defaultBankAmmo(type, false).orElseThrow());
        assertSame(bomb, DriveableAmmoLoader.defaultBankAmmo(type, true).orElseThrow());
        when(type.weaponType(true)).thenReturn(EnumWeaponType.NONE);
        assertFalse(DriveableAmmoLoader.defaultBankAmmo(type, true).isPresent());
    }

    @Test
    void prefersTheWeaponTypeTheDefinitionDeclaresForEachBank()
    {
        assertEquals(List.of(EnumWeaponType.MINE, EnumWeaponType.BOMB), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.MINE, EnumWeaponType.GUN, true));
        assertEquals(List.of(EnumWeaponType.SHELL, EnumWeaponType.MISSILE), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.SHELL, EnumWeaponType.GUN, false));
        assertEquals(List.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.MISSILE, false));
    }

    @Test
    void keepsBothBankTypesWhenOneDefinitionDeclaresBoth()
    {
        assertEquals(List.of(EnumWeaponType.SHELL, EnumWeaponType.MISSILE), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.SHELL, EnumWeaponType.MISSILE, false));
        assertEquals(List.of(EnumWeaponType.MINE, EnumWeaponType.BOMB), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.MINE, EnumWeaponType.BOMB, true));
    }

    @Test
    void fallsBackToTheCanonicalOrderWhenNoBankIsDeclared()
    {
        assertEquals(List.of(EnumWeaponType.BOMB, EnumWeaponType.MINE), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.NONE, EnumWeaponType.GUN, true));
        assertEquals(List.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL), DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.NONE, EnumWeaponType.NONE, false));
    }

    @Test
    void neverOffersTheOtherBanksWeaponTypes()
    {
        assertFalse(DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.SHELL, true).contains(EnumWeaponType.SHELL));
        assertFalse(DriveableAmmoLoader.bankWeaponTypes(EnumWeaponType.BOMB, EnumWeaponType.SHELL, false).contains(EnumWeaponType.BOMB));
    }
}
