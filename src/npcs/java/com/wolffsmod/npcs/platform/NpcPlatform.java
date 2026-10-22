package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.combat.EquipmentAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Version boundary for the NeoForge and Minecraft item, attribute, enchantment and fluid APIs the module reads. Minecraft
 * 1.21 enchantments are data-driven effects that may need the real target and damage source, so their bonuses are left to
 * real combat and {@link #conditionalEffect} tells the editor when the item decides a value.
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
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** The folder of the game's configuration files. */
    public static Path configDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    /** The attribute that extends melee and interaction reach. */
    // A seam: the 1.20.1 platform returns Forge's reach attribute.
    @SuppressWarnings("SameReturnValue")
    public static Holder<Attribute> reachAttribute()
    {
        return Attributes.ENTITY_INTERACTION_RANGE;
    }

    /** How deep the entity stands in water, in blocks. */
    public static double waterHeight(Entity entity)
    {
        return entity.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value());
    }

    /** The sound of a thrown trident. */
    public static SoundEvent tridentThrowSound()
    {
        return SoundEvents.TRIDENT_THROW.value();
    }

    public static int powerLevel(LivingEntity holder, ItemStack item)
    {
        return level(holder, item, Enchantments.POWER);
    }

    public static int multishotLevel(LivingEntity holder, ItemStack item)
    {
        return level(holder, item, Enchantments.MULTISHOT);
    }

    public static int fireAspectLevel(LivingEntity holder, ItemStack item)
    {
        return level(holder, item, Enchantments.FIRE_ASPECT);
    }

    /** Melee damage the item's enchantments add; on 1.21 only the real target decides it. */
    // Part of the cross-version platform signature; 1.20.1 adds the scalar enchantment bonus.
    @SuppressWarnings({"java:S1172", "unused", "SameReturnValue"})
    public static double meleeDamageBonus(LivingEntity holder, ItemStack item)
    {
        return 0D;
    }

    /** Knockback the item's enchantments add; on 1.21 only the real target decides it. */
    // Part of the cross-version platform signature; 1.20.1 adds the scalar enchantment bonus.
    @SuppressWarnings({"java:S1172", "unused", "SameReturnValue"})
    public static double meleeKnockbackBonus(LivingEntity holder, ItemStack item)
    {
        return 0D;
    }

    /** Whether the item's enchantments have the effect but only the real target decides its value. */
    public static boolean conditionalEffect(LivingEntity holder, ItemStack item, Effect effect)
    {
        DataComponentType<?> component = switch (effect)
        {
            case DAMAGE -> EnchantmentEffectComponents.DAMAGE;
            case KNOCKBACK -> EnchantmentEffectComponents.KNOCKBACK;
            case PROTECTION -> EnchantmentEffectComponents.DAMAGE_PROTECTION;
        };
        return item.getAllEnchantments(holder.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)).keySet().stream().anyMatch(enchantment -> enchantment.value().effects().has(component));
    }

    /** Protection the worn armor's enchantments give against the source; 1.21 evaluates it on the server only. */
    public static float armorEnchantmentProtection(LivingEntity wearer, DamageSource source)
    {
        return wearer.level() instanceof ServerLevel level ? EnchantmentHelper.getDamageProtection(level, wearer, source) : 0F;
    }

    /** The wearer's value of the attribute from its base and the modifiers of its worn armor. */
    public static float equippedArmorValue(LivingEntity wearer, Holder<Attribute> attribute)
    {
        var modifiers = new ArrayList<AttributeModifier>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                wearer.getItemBySlot(slot).forEachModifier(slot, (current, modifier) ->
                {
                    if (current.equals(attribute))
                        modifiers.add(modifier);
                });
        }
        var instance = wearer.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }

    private static int level(LivingEntity holder, ItemStack item, ResourceKey<Enchantment> enchantment)
    {
        return item.getEnchantmentLevel(holder.registryAccess().holderOrThrow(enchantment));
    }
}
