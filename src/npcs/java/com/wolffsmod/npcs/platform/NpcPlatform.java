package com.wolffsmod.npcs.platform;

import com.flansmodultimate.platform.entity.EntityPlatform;
import com.wolffsmod.npcs.combat.EquipmentAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Version boundary for the Forge and Minecraft item, attribute, enchantment and fluid APIs the module reads. Forge 1.20.1
 * has scalar enchantments; Minecraft 1.21 enchantments are data-driven effects that may need the real target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPlatform
{
    private static final UUID KNOCKBACK_IMMUNITY = UUID.fromString("5b0f7a52-6f0b-4c0e-9d1f-0c2c6f7a3e41");
    private static final String KNOCKBACK_IMMUNITY_NAME = "Wolff's Mod knockback immunity";

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

    /** The effects a potion item brings: its potion's, then its own custom ones. */
    public static List<MobEffectInstance> potionEffects(ItemStack stack)
    {
        return PotionUtils.getMobEffects(stack);
    }

    /** Whether a firework rocket carries stars, and so harms what is near where it bursts. */
    public static boolean fireworkExplodes(ItemStack stack)
    {
        CompoundTag fireworks = stack.getTagElement(FireworkRocketItem.TAG_FIREWORKS);
        return fireworks != null && !fireworks.getList(FireworkRocketItem.TAG_EXPLOSIONS, Tag.TAG_COMPOUND).isEmpty();
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
            if (EntityPlatform.isHumanoidArmor(slot))
                modifiers.addAll(wearer.getItemBySlot(slot).getAttributeModifiers(slot).get(attribute));
        }
        var instance = wearer.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }

    /** The highest maximum health the game accepts. */
    public static double maxHealthLimit()
    {
        return Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
    }

    /** A blaze fireball fired by the shooter along the given direction. */
    public static SmallFireball smallFireball(Level level, LivingEntity shooter, Vec3 direction)
    {
        return new SmallFireball(level, shooter, direction.x, direction.y, direction.z);
    }

    /** Adds or removes the full knockback-resistance modifier that keeps an immune NPC in place. */
    public static void setKnockbackImmunity(AttributeInstance resistance, boolean immune)
    {
        boolean applied = resistance.getModifier(KNOCKBACK_IMMUNITY) != null;
        if (immune && !applied)
            resistance.addTransientModifier(new AttributeModifier(KNOCKBACK_IMMUNITY, KNOCKBACK_IMMUNITY_NAME, 1D, AttributeModifier.Operation.ADDITION));
        else if (!immune && applied)
            resistance.removeModifier(KNOCKBACK_IMMUNITY);
    }
}
