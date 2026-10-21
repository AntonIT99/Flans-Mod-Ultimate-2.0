package com.flansmodultimate.common.item;

import com.flansmodultimate.api.IEquipmentPolicy;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArmorPotionPolicyTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void potionPolicyDefaultsToTheExistingArmorPolicy()
    {
        IEquipmentPolicy enabled = new IEquipmentPolicy()
        {};
        IEquipmentPolicy disabled = new IEquipmentPolicy()
        {
            @Override
            public boolean flansArmorEffects()
            {
                return false;
            }
        };
        assertTrue(enabled.flansmodultimateArmorPotionEffects());
        assertFalse(disabled.flansmodultimateArmorPotionEffects());
    }

    @Test
    void disablingArmorPotionsRemovesTrackedEffectsButPreservesExternalEffects()
    {
        LivingEntity entity = mock(LivingEntity.class, withSettings().extraInterfaces(IEquipmentPolicy.class));
        IEquipmentPolicy policy = (IEquipmentPolicy) entity;
        when(policy.flansArmorEffects()).thenReturn(true);
        when(policy.flansmodultimateArmorPotionEffects()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(entity.getUUID()).thenReturn(uuid);
        when(entity.getEffect(MobEffects.DAMAGE_RESISTANCE)).thenReturn(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 3, true, false));
        when(entity.getEffect(MobEffects.REGENERATION)).thenReturn(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, true));
        CustomArmorItem.LAST_ARMOR_EFFECTS.put(uuid, new HashSet<>(Set.of(MobEffects.DAMAGE_RESISTANCE, MobEffects.REGENERATION)));
        try
        {
            CustomArmorItem.handleMobEffects(entity);

            verify(entity).removeEffect(MobEffects.DAMAGE_RESISTANCE);
            verify(entity, never()).removeEffect(MobEffects.REGENERATION);
            verify(entity, never()).removeEffect(MobEffects.MOVEMENT_SPEED);
            verify(entity, never()).getArmorSlots();
            assertFalse(CustomArmorItem.LAST_ARMOR_EFFECTS.containsKey(uuid));
        }
        finally
        {
            CustomArmorItem.LAST_ARMOR_EFFECTS.remove(uuid);
        }
    }
}
