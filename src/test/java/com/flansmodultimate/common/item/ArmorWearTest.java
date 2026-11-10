package com.flansmodultimate.common.item;

import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.config.EnumBreakableArmor;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArmorWearTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static ItemStack wornBy(LivingEntity entity)
    {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(mock(ArmorItem.class));
        when(entity.getItemBySlot(any(EquipmentSlot.class))).thenReturn(ItemStack.EMPTY);
        when(entity.getItemBySlot(EquipmentSlot.CHEST)).thenReturn(stack);
        return stack;
    }

    @Test
    void aPlayersArmorLosesOnePointToAHitItFullyAbsorbed()
    {
        Player player = mock(Player.class);
        ItemStack stack = wornBy(player);

        CustomArmorItem.wearFromAbsorbedHit(player, mock(DamageSource.class));

        verify(stack).hurtAndBreak(eq(1), eq(player), any());
    }

    @Test
    void armorIsNotWornByDamageThatBypassesIt()
    {
        Player player = mock(Player.class);
        ItemStack stack = wornBy(player);
        DamageSource source = mock(DamageSource.class);
        when(source.is(DamageTypeTags.BYPASSES_ARMOR)).thenReturn(true);

        CustomArmorItem.wearFromAbsorbedHit(player, source);

        verify(stack, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
    }

    @Test
    void anEntitysPolicyDecidesWhetherItsArmorWearsAndOrdinaryMobsNeverDo()
    {
        LivingEntity spared = mock(LivingEntity.class, withSettings().extraInterfaces(IEquipmentPolicy.class));
        ItemStack sparedStack = wornBy(spared);
        LivingEntity worn = mock(LivingEntity.class, withSettings().extraInterfaces(IEquipmentPolicy.class));
        ItemStack wornStack = wornBy(worn);
        when(((IEquipmentPolicy) worn).flansArmorWears()).thenReturn(true);
        LivingEntity mob = mock(LivingEntity.class);
        ItemStack mobStack = wornBy(mob);

        CustomArmorItem.wearFromAbsorbedHit(spared, mock(DamageSource.class));
        CustomArmorItem.wearFromAbsorbedHit(worn, mock(DamageSource.class));
        CustomArmorItem.wearFromAbsorbedHit(mob, mock(DamageSource.class));

        verify(sparedStack, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
        verify(wornStack).hurtAndBreak(eq(1), eq(worn), any());
        verify(mobStack, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
    }

    @Test
    void anvilsRepairUnenchantableArmorButStillRefuseAnythingElse()
    {
        CustomArmorItem armor = mock(CustomArmorItem.class);
        ItemStack worn = mock(ItemStack.class);
        ItemStack material = mock(ItemStack.class);
        ItemStack samePiece = mock(ItemStack.class);
        ItemStack book = mock(ItemStack.class);
        when(armor.refusesAnvilInput(any(), any())).thenCallRealMethod();
        when(armor.getEnchantmentValue()).thenReturn(0);
        when(armor.isValidRepairItem(worn, material)).thenReturn(true);
        when(samePiece.is(armor)).thenReturn(true);

        assertFalse(armor.refusesAnvilInput(worn, material));
        assertFalse(armor.refusesAnvilInput(worn, samePiece));
        assertFalse(armor.refusesAnvilInput(worn, ItemStack.EMPTY));
        assertTrue(armor.refusesAnvilInput(worn, book));

        when(armor.getEnchantmentValue()).thenReturn(10);
        assertFalse(armor.refusesAnvilInput(worn, book));
    }

    @Test
    void everyPolicyRoundTripsThroughItsWordAndUnknownWordsAreRejected()
    {
        for (EnumBreakableArmor mode : EnumBreakableArmor.values())
            assertEquals(mode, EnumBreakableArmor.fromWord(mode.word()).orElseThrow());
        assertTrue(EnumBreakableArmor.fromWord("sometimes").isEmpty());
    }
}
