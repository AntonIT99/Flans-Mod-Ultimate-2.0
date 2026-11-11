package com.flansmodultimate.common.item;

import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.config.EnumBreakableArmor;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

        CustomArmorItem.wearArmor(player, mock(DamageSource.class), 10F, true);

        verify(stack).hurtAndBreak(eq(1), eq(player), any());
    }

    @Test
    void flanArmorLosesWhatItAbsorbsAndVanillaWearThenPassesOverIt()
    {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);
        when(level.getGameTime()).thenReturn(7L);
        ItemStack light = flanPiece(0.03F);
        ItemStack heavy = flanPiece(0.5F);
        ItemStack plain = flanPiece(0F);
        when(player.getItemBySlot(any(EquipmentSlot.class))).thenReturn(ItemStack.EMPTY);
        when(player.getItemBySlot(EquipmentSlot.HEAD)).thenReturn(light);
        when(player.getItemBySlot(EquipmentSlot.CHEST)).thenReturn(heavy);
        when(player.getItemBySlot(EquipmentSlot.LEGS)).thenReturn(plain);

        CustomArmorItem.wearArmor(player, mock(DamageSource.class), 30F, false);

        verify(light).hurtAndBreak(eq(1), eq(player), any());
        verify(heavy).hurtAndBreak(eq(15), eq(player), any());
        verify(plain, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
        assertTrue(CustomArmorItem.wornThisHit(heavy, player));
        assertFalse(CustomArmorItem.wornThisHit(plain, player));
        when(level.getGameTime()).thenReturn(8L);
        assertFalse(CustomArmorItem.wornThisHit(heavy, player));
    }

    private static ItemStack flanPiece(float ratio)
    {
        CustomArmorItem armor = mock(CustomArmorItem.class);
        when(armor.wearRatio(anyBoolean())).thenReturn(ratio);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(armor);
        return stack;
    }

    @Test
    void armorIsNotWornByDamageThatBypassesIt()
    {
        Player player = mock(Player.class);
        ItemStack stack = wornBy(player);
        DamageSource source = mock(DamageSource.class);
        when(source.is(DamageTypeTags.BYPASSES_ARMOR)).thenReturn(true);

        CustomArmorItem.wearArmor(player, source, 10F, true);

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

        CustomArmorItem.wearArmor(spared, mock(DamageSource.class), 10F, true);
        CustomArmorItem.wearArmor(worn, mock(DamageSource.class), 10F, true);
        CustomArmorItem.wearArmor(mob, mock(DamageSource.class), 10F, true);

        verify(sparedStack, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
        verify(wornStack).hurtAndBreak(eq(1), eq(worn), any());
        verify(mobStack, never()).hurtAndBreak(anyInt(), any(LivingEntity.class), any());
    }

    private static CustomArmorItem durableArmor(ItemStack stack, int maxDamage)
    {
        CustomArmorItem armor = mock(CustomArmorItem.class);
        when(armor.wear(any(), anyInt(), anyBoolean())).thenCallRealMethod();
        when(armor.isDepleted(any())).thenCallRealMethod();
        when(armor.isDamageable(stack)).thenReturn(true);
        when(armor.getMaxDamage(stack)).thenReturn(maxDamage);
        return armor;
    }

    private static ItemStack stackWith(CompoundTag tag, int damage)
    {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getTag()).thenReturn(tag);
        when(stack.getOrCreateTag()).thenReturn(tag);
        when(stack.getDamageValue()).thenReturn(damage);
        return stack;
    }

    @Test
    void keptArmorStopsAtItsLastPointAndBecomesDepleted()
    {
        ItemStack stack = stackWith(new CompoundTag(), 95);
        CustomArmorItem armor = durableArmor(stack, 100);

        assertEquals(4, armor.wear(stack, 20, true));
        when(stack.getDamageValue()).thenReturn(99);
        assertTrue(armor.isDepleted(stack));
        assertEquals(0, armor.wear(stack, 20, true));
    }

    @Test
    void armorStillBreaksWhenTheRuleDoesNotKeepItAndOrdinaryWearDoesNotDepleteIt()
    {
        ItemStack stack = stackWith(new CompoundTag(), 95);
        CustomArmorItem armor = durableArmor(stack, 100);

        assertEquals(20, armor.wear(stack, 20, false));
        assertEquals(2, armor.wear(stack, 2, true));
        when(stack.getDamageValue()).thenReturn(99);
        assertFalse(armor.isDepleted(stack));
    }

    @Test
    void repairedOrUnbreakableArmorIsNoLongerDepleted()
    {
        CompoundTag tag = new CompoundTag();
        ItemStack stack = stackWith(tag, 95);
        CustomArmorItem armor = durableArmor(stack, 100);
        armor.wear(stack, 20, true);

        assertFalse(armor.isDepleted(stack));
        when(stack.getDamageValue()).thenReturn(99);
        assertTrue(armor.isDepleted(stack));
        when(armor.isDamageable(stack)).thenReturn(false);
        assertFalse(armor.isDepleted(stack));
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
