package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.damage.WreckLoad;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DestructionAmmoTest
{
    @Test
    void npcMayAssumeAReserveWithoutInventingAnExplosiveMagazine()
    {
        DriveableType type = unarmed();
        DestructionAmmo ammo = DestructionAmmo.of(type);
        WreckLoad load = WreckLoad.UNKNOWN;
        assertTrue(load.cooksOff());
        assertEquals(WreckLoad.UNKNOWN.fuel(), load.fuel());
        assertEquals(0F, ammo.magazineBurstScale(WreckLoad.UNKNOWN));
        DamageEffectSource npc = mock(DamageEffectSource.class, CALLS_REAL_METHODS);
        when(npc.type()).thenReturn(type);
        assertEquals(load, npc.load(), "NPCs without a known inventory may retain an assumed reserve");
    }

    @Test
    void lcvpWithTwoRifleCalibreMachineGunsOnlyGetsCookOffPops()
    {
        DriveableType type = unarmed();
        ShootableType belt = mock(ShootableType.class);
        GunType browning = mock(GunType.class);
        when(browning.getDefaultAmmo()).thenReturn(Optional.of(belt));
        when(type.getNumAmmoSlots()).thenReturn(2);
        when(type.getGunTypeForAmmoSlot(0)).thenReturn(browning);
        when(type.getGunTypeForAmmoSlot(1)).thenReturn(browning);

        DestructionAmmo ammo = DestructionAmmo.of(type);
        WreckLoad load = WreckLoad.UNKNOWN;
        assertTrue(load.cooksOff(), "Belts may crackle in the fire");
        assertEquals(0F, ammo.magazineBurstScale(load), "A boat armed with two Brownings has no explosive magazine blast");
    }

    @Test
    void explosivePassengerGunIsRecognisedIndependentlyOfTheDriverBanks()
    {
        DriveableType type = unarmed();
        GunType cannon = mock(GunType.class);
        ShootableType shell = mock(ShootableType.class);
        when(shell.getExplosionRadius()).thenReturn(5F);
        when(cannon.getDefaultAmmo()).thenReturn(Optional.of(shell));
        when(type.getNumAmmoSlots()).thenReturn(1);
        when(type.getGunTypeForAmmoSlot(0)).thenReturn(cannon);

        DestructionAmmo ammo = DestructionAmmo.of(type);
        assertEquals(5F, ammo.explosionRadius());
        assertTrue(ammo.magazineBurstScale(WreckLoad.UNKNOWN) > 0F);
    }

    @Test
    void usesTheBanksDefaultRoundInsteadOfTheLargestCompatibleWarhead()
    {
        DriveableType type = unarmed();
        BulletType shell = mock(BulletType.class);
        BulletType heavyShell = mock(BulletType.class);
        when(shell.getWeaponType()).thenReturn(EnumWeaponType.SHELL);
        when(shell.getExplosionRadius()).thenReturn(4F);
        when(heavyShell.getWeaponType()).thenReturn(EnumWeaponType.SHELL);
        when(heavyShell.getExplosionRadius()).thenReturn(100F);
        when(type.weaponType(false)).thenReturn(EnumWeaponType.SHELL);
        when(type.getAmmoTypes()).thenReturn(List.of(shell, heavyShell));

        DestructionAmmo ammo = DestructionAmmo.of(type);
        assertEquals(4F, ammo.explosionRadius());
        assertEquals(1F, ammo.magazineBurstScale(new WreckLoad(1F, 1F)));
    }

    @Test
    void knownEmptyMagazineDoesNotExplodeAndPartialLoadsMakeSmallerBursts()
    {
        DestructionAmmo ammo = new DestructionAmmo(4F);
        assertEquals(0F, ammo.magazineBurstScale(new WreckLoad(0F, 1F)));
        assertEquals(0.5F, ammo.magazineBurstScale(new WreckLoad(0.25F, 1F)));
        assertEquals(1F, ammo.magazineBurstScale(new WreckLoad(1F, 1F)));
        assertTrue(new DestructionAmmo(1000F).magazineBurstScale(WreckLoad.UNKNOWN) <= 2.25F);
    }

    @Test
    void unresolvedWeaponsAndInvalidRadiiDoNotInventExplosiveMagazines()
    {
        DriveableType type = unarmed();
        when(type.getNumAmmoSlots()).thenReturn(1);
        assertEquals(0F, DestructionAmmo.of(type).magazineBurstScale(WreckLoad.UNKNOWN));
        for (float radius : new float[]{-1F, Float.NaN, Float.POSITIVE_INFINITY})
            assertEquals(0F, new DestructionAmmo(radius).magazineBurstScale(WreckLoad.UNKNOWN));
    }

    private static DriveableType unarmed()
    {
        DriveableType type = mock(DriveableType.class);
        when(type.weaponType(false)).thenReturn(EnumWeaponType.NONE);
        when(type.weaponType(true)).thenReturn(EnumWeaponType.NONE);
        return type;
    }
}
