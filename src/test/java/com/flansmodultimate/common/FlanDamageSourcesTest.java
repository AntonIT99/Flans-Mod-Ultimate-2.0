package com.flansmodultimate.common;

import com.flansmodultimate.common.driveables.damage.VehicleMeleeDamage;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlanDamageSourcesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aBulletNamesItsFiringWeaponRatherThanTheKillersCurrentItem()
    {
        DamageSource source = mock(DamageSource.class);
        Bullet bullet = mock(Bullet.class);
        FiredShot shot = mock(FiredShot.class);
        FireableGun gun = mock(FireableGun.class);
        GunType type = mock(GunType.class);
        when(source.getDirectEntity()).thenReturn(bullet);
        when(bullet.getFiredShot()).thenReturn(shot);
        when(shot.getFireableGun()).thenReturn(gun);
        when(gun.getType()).thenReturn(type);
        assertSame(type, FlanDamageSources.getWeaponType(source).orElseThrow());
    }

    @Test
    void grenadesAndVehicleImpactsKeepTheirOwnNames()
    {
        DamageSource source = mock(DamageSource.class);
        Grenade grenade = mock(Grenade.class);
        GrenadeType grenadeType = mock(GrenadeType.class);
        when(source.getDirectEntity()).thenReturn(grenade);
        when(grenade.getConfigType()).thenReturn(grenadeType);
        assertSame(grenadeType, FlanDamageSources.getWeaponType(source).orElseThrow());
        Driveable vehicle = mock(Driveable.class);
        DriveableType vehicleType = mock(DriveableType.class);
        when(source.getDirectEntity()).thenReturn(vehicle);
        when(vehicle.getConfigType()).thenReturn(vehicleType);
        assertSame(vehicleType, FlanDamageSources.getWeaponType(source).orElseThrow());
    }

    @Test
    void meleeAttributionRetainsTheStrikingWeaponAndOriginalDamageRules()
    {
        GunItem gun = mock(GunItem.class);
        GunType type = mock(GunType.class);
        ItemStack weapon = mock(ItemStack.class);
        when(gun.getConfigType()).thenReturn(type);
        when(weapon.getItem()).thenReturn(gun);
        when(weapon.copy()).thenReturn(weapon);
        DamageSource original = new DamageSource(Holder.direct(new DamageType("mob", 0F)));
        DamageSource melee = VehicleMeleeDamage.source(original, weapon);
        assertSame(type, FlanDamageSources.getWeaponType(melee).orElseThrow());
        assertSame(original.typeHolder(), melee.typeHolder());
    }

    @Test
    void weaponNamesRemainVisibleWhenDistanceIsDisabled()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::enableKillMessages).thenReturn(true);
            LivingEntity victim = mock(LivingEntity.class);
            when(victim.getDisplayName()).thenReturn(Component.literal("Victim"));
            DamageSource source = new DamageSource(Holder.direct(new DamageType("shootable", 0F)));
            Component weapon = Component.literal("Test rifle");
            Component message = FlanDamageSources.deathMessage(source, victim, weapon);
            TranslatableContents contents = assertInstanceOf(TranslatableContents.class, message.getContents());
            assertEquals("death.flansmodultimate.weapon", contents.getKey());
            assertEquals(weapon, contents.getArgs()[1]);
            config.when(ModCommonConfig::enableKillMessages).thenReturn(false);
            TranslatableContents plain = assertInstanceOf(TranslatableContents.class, FlanDamageSources.deathMessage(source, victim, weapon).getContents());
            assertEquals("death.attack.shootable", plain.getKey());
        }
    }
}
