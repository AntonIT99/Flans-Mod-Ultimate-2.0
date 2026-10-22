package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.combat.EquipmentAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Version boundary for the Forge and Minecraft item, attribute, enchantment and fluid APIs the module reads. Forge 1.20.1
 * has scalar enchantments; Minecraft 1.21 enchantments are data-driven effects that may need the real target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPlatform
{
    /** Enchantment effects the editor readouts may not be able to show as a number. */
    public enum Effect
    {
        DAMAGE, KNOCKBACK, PROTECTION
    }

    /** Whether the item blocks like a shield. */
    public static boolean canShieldBlock(ItemStack stack)
    {
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    /** The folder of the game's configuration files. */
    public static Path configDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    /** The attribute that extends melee and interaction reach. */
    public static Attribute reachAttribute()
    {
        return ForgeMod.ENTITY_REACH.get();
    }

    /** How deep the entity stands in water, in blocks. */
    public static double waterHeight(Entity entity)
    {
        return entity.getFluidTypeHeight(ForgeMod.WATER_TYPE.get());
    }

    /** The sound of a thrown trident. */
    public static SoundEvent tridentThrowSound()
    {
        return SoundEvents.TRIDENT_THROW;
    }

    // The holder supplies 1.21's enchantment registry; 1.20.1 enchantments are constants.
    @SuppressWarnings({"java:S1172", "unused"})
    public static int powerLevel(LivingEntity holder, ItemStack item)
    {
        return item.getEnchantmentLevel(Enchantments.POWER_ARROWS);
    }

    // The holder supplies 1.21's enchantment registry; 1.20.1 enchantments are constants.
    @SuppressWarnings({"java:S1172", "unused"})
    public static int multishotLevel(LivingEntity holder, ItemStack item)
    {
        return item.getEnchantmentLevel(Enchantments.MULTISHOT);
    }

    // The holder supplies 1.21's enchantment registry; 1.20.1 enchantments are constants.
    @SuppressWarnings({"java:S1172", "unused"})
    public static int fireAspectLevel(LivingEntity holder, ItemStack item)
    {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, item);
    }

    /** Melee damage the item's enchantments add against a generic target. */
    // The holder supplies 1.21's enchantment registry; 1.20.1 enchantments are constants.
    @SuppressWarnings({"java:S1172", "unused"})
    public static double meleeDamageBonus(LivingEntity holder, ItemStack item)
    {
        return EnchantmentHelper.getDamageBonus(item, MobType.UNDEFINED);
    }

    /** Knockback the item's enchantments add. */
    // The holder supplies 1.21's enchantment registry; 1.20.1 enchantments are constants.
    @SuppressWarnings({"java:S1172", "unused"})
    public static double meleeKnockbackBonus(LivingEntity holder, ItemStack item)
    {
        return item.getEnchantmentLevel(Enchantments.KNOCKBACK);
    }

    /** Attacking enchantments come from armor and the striking weapon, never from the idle hand as well. */
    public static void meleePostDamageEffects(LivingEntity holder, Entity target, ItemStack weapon)
    {
        for (ItemStack armor : holder.getArmorSlots())
            armor.getAllEnchantments().forEach((enchantment, level) -> enchantment.doPostAttack(holder, target, level));
        weapon.getAllEnchantments().forEach((enchantment, level) -> enchantment.doPostAttack(holder, target, level));
    }

    /** Whether the item's enchantments have the effect but only the real target decides its value; never on 1.20.1. */
    // Part of the cross-version platform signature; 1.21 inspects the enchantments' effects.
    @SuppressWarnings({"java:S1172", "unused", "SameReturnValue"})
    public static boolean conditionalEffect(LivingEntity holder, ItemStack item, Effect effect)
    {
        return false;
    }

    /** Protection the worn armor's enchantments give against the source. */
    public static float armorEnchantmentProtection(LivingEntity wearer, DamageSource source)
    {
        return EnchantmentHelper.getDamageProtection(wearer.getArmorSlots(), source);
    }

    /** The wearer's value of the attribute from its base and the modifiers of its worn armor. */
    public static float equippedArmorValue(LivingEntity wearer, Attribute attribute)
    {
        var modifiers = new ArrayList<AttributeModifier>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.ARMOR)
                modifiers.addAll(wearer.getItemBySlot(slot).getAttributeModifiers(slot).get(attribute));
        }
        var instance = wearer.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }
}
