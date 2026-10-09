package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.api.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.common.ItemAbilities;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

import java.util.Optional;

/** Item classification and combat defaults. Readouts use the same inspector as runtime actions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipment
{
    public static boolean enabled(EntityNPCInterface npc, Feature feature)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
    }

    public static boolean weaponAuthority(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.ITEM_WEAPONS) && !npc.getMainHandItem().isEmpty();
    }

    public static boolean ranged(EntityNPCInterface npc)
    {
        return weaponAuthority(npc) && ranged(npc.getMainHandItem());
    }

    public static boolean ranged(ItemStack stack)
    {
        if (FlansProjectiles.isGrenade(stack))
            return true;
        if (FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(EquippedWeaponProperties::ranged).orElse(false))
            return true;
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem || stack.getItem() instanceof ThrowablePotionItem
            || stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL) || stack.getItem() instanceof NpcWeaponAdapter;
    }

    public static Optional<EquippedWeaponProperties> flan(EntityNPCInterface npc)
    {
        return FlansEquipment.getWeaponProperties(npc.getMainHandItem(), noppes.npcs.api.wrapper.ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
    }

    public static double meleeDamage(ItemStack stack)
    {
        return EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1D);
    }

    public static int meleeDelay(ItemStack stack)
    {
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).filter(properties -> properties.meleeTime() > 1).map(EquippedWeaponProperties::meleeTime)
            .orElseGet(() -> EquipmentAttributes.attackDelay(EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, 4D)));
    }

    public static int chargeTime(ItemStack stack, LivingEntity holder)
    {
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(properties -> properties.throwable() ? Math.max(1, properties.chargeTime()) : 0)
            .orElseGet(() -> vanillaChargeTime(stack, holder));
    }

    private static int vanillaChargeTime(ItemStack stack, LivingEntity holder)
    {
        if (stack.getItem() instanceof CrossbowItem)
            return CrossbowItem.isCharged(stack) ? 0 : CrossbowItem.getChargeDuration(stack, holder);
        if (stack.getItem() instanceof BowItem)
            return 20;
        return stack.getItem() instanceof TridentItem ? 10 : 0;
    }

    public static double shotDelay(ItemStack stack)
    {
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(EquippedWeaponProperties::shotDelay).orElse(20D);
    }

    public static boolean shield(ItemStack stack)
    {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    public static int fireSeconds(ItemStack stack, LivingEntity holder)
    {
        return stack.getEnchantmentLevel(holder.registryAccess().holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT)) * 4;
    }

    public static boolean armorProtects(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.EquippedArmorProperties> protection)
    {
        if (!enabled(npc, Feature.ITEM_ARMOR))
            return false;
        for (ItemStack stack : npc.getArmorSlots())
            if (FlansEquipment.getArmorProperties(stack).filter(protection).isPresent())
                return true;
        return false;
    }
}
