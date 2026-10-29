package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleMeleeDamageTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void weakHitsAreRejectedButTheExactThresholdAndDisabledThresholdAcceptHits()
    {
        assertTrue(VehicleMeleeDamage.belowThreshold(10F, 9.99F));
        assertFalse(VehicleMeleeDamage.belowThreshold(10F, 10F));
        assertFalse(VehicleMeleeDamage.belowThreshold(10F, 24F));
        assertFalse(VehicleMeleeDamage.belowThreshold(0F, 1F));
    }

    @Test
    void vehicleWeaponDamageIsAppliedBeforeTheThresholdAndPreservesHitStrength()
    {
        GunItem gun = mock(GunItem.class);
        GunType type = mock(GunType.class);
        ItemStack weapon = mock(ItemStack.class);
        when(weapon.getItem()).thenReturn(gun);
        when(gun.getConfigType()).thenReturn(type);
        when(type.getMeleeDamage(weapon, false)).thenReturn(12F);
        when(type.getMeleeDamage(weapon, true)).thenReturn(24F);
        LivingEntity attacker = mock(LivingEntity.class);
        when(attacker.getMainHandItem()).thenReturn(weapon);
        DamageSource hit = mock(DamageSource.class);
        when(hit.getMsgId()).thenReturn("mob");
        when(hit.getDirectEntity()).thenReturn(attacker);
        assertEquals(24F, VehicleMeleeDamage.againstVehicle(hit, 12F));
        assertEquals(6F, VehicleMeleeDamage.againstVehicle(hit, 3F));
        assertTrue(VehicleMeleeDamage.belowThreshold(10F, VehicleMeleeDamage.againstVehicle(hit, 3F)));
        when(hit.is(DamageTypeTags.IS_PROJECTILE)).thenReturn(true);
        assertEquals(12F, VehicleMeleeDamage.againstVehicle(hit, 12F));

        // Retain the striking offhand weapon even while the main hand is empty, without changing mob attribution.
        when(weapon.copy()).thenReturn(weapon);
        DamageSource original = new DamageSource(Holder.direct(new DamageType("mob", 0F)), attacker);
        DamageSource offhand = VehicleMeleeDamage.source(original, weapon);
        when(attacker.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        assertEquals("mob", offhand.getMsgId());
        assertSame(attacker, offhand.getEntity());
        assertEquals(24F, VehicleMeleeDamage.againstVehicle(offhand, 12F));
    }

    @Test
    void mountedLivingEntitiesForwardMeleeOnceButKeepProjectilesAndExplosionsCancelled()
    {
        LivingEntity rider = mock(LivingEntity.class);
        Seat seat = mock(Seat.class);
        when(rider.getVehicle()).thenReturn(seat);
        Level level = mock(Level.class);
        when(rider.level()).thenReturn(level);
        DamageSource melee = mock(DamageSource.class);
        when(melee.getMsgId()).thenReturn("mob");
        assertTrue(VehicleMeleeDamage.protectRider(rider, melee, 12F));
        verify(seat).hurt(melee, 12F);
        for (var tag : java.util.List.of(DamageTypeTags.IS_PROJECTILE, DamageTypeTags.IS_EXPLOSION))
        {
            DamageSource other = mock(DamageSource.class);
            when(other.is(tag)).thenReturn(true);
            assertTrue(VehicleMeleeDamage.protectRider(rider, other, 12F));
            verify(seat, never()).hurt(other, 12F);
        }
        when(rider.getVehicle()).thenReturn(null);
        assertFalse(VehicleMeleeDamage.protectRider(rider, melee, 12F));
    }
}
