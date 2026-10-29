package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.combat.EquipmentAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;

/**
 * Version boundary for the Forge and Minecraft entity, item, sound, attribute, enchantment and fluid APIs the module reads.
 * Forge 1.20.1 has scalar enchantments; Minecraft 1.21 enchantments are data-driven effects that may need the real target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPlatform
{
    private static final UUID KNOCKBACK_IMMUNITY = UUID.fromString("5b0f7a52-6f0b-4c0e-9d1f-0c2c6f7a3e41");
    private static final String KNOCKBACK_IMMUNITY_NAME = "Wolff's Mod knockback immunity";
    private static final UUID SERVER_HEALTH = UUID.fromString("c3f1d7a4-2b6e-4f0a-8a51-7e9d2c4b6a13");
    private static final String SERVER_HEALTH_NAME = "Wolff's Mod server health";

    /** Enchantment effects the editor readouts may not be able to show as a number. */
    public enum Effect
    {
        DAMAGE, KNOCKBACK, PROTECTION
    }

    /** Whether the slot is one of the four worn armor slots, not a hand or a body slot. */
    public static boolean isHumanoidArmor(EquipmentSlot slot)
    {
        return slot.getType() == EquipmentSlot.Type.ARMOR;
    }

    public static float width(EntityDimensions dimensions)
    {
        return dimensions.width;
    }

    public static float height(EntityDimensions dimensions)
    {
        return dimensions.height;
    }

    /** Whether the entity has been added to a level, as opposed to only constructed. */
    public static boolean isAddedToLevel(Entity entity)
    {
        return entity.isAddedToWorld();
    }

    /** Offset from a vehicle seat position to where the passenger stands. */
    // Part of the cross-version platform signature; 1.21 asks the passenger for its attachment point on the vehicle.
    @SuppressWarnings({"java:S1172", "unused"})
    public static Vec3 riderOffset(Entity passenger, Entity vehicle)
    {
        return new Vec3(0D, passenger.getMyRidingOffset(), 0D);
    }

    /** Ticks a crossbow takes to charge for the entity. */
    // Part of the cross-version platform signature; 1.21 asks the crossbow's enchantments for the entity.
    @SuppressWarnings({"java:S1172", "unused"})
    public static int crossbowChargeDuration(ItemStack crossbow, LivingEntity entity)
    {
        return CrossbowItem.getChargeDuration(crossbow);
    }

    /** Whether both stacks hold the same item with the same custom data, ignoring count. */
    public static boolean isSameItemSameData(ItemStack first, ItemStack second)
    {
        return ItemStack.isSameItemSameTags(first, second);
    }

    /** Damages the stack, breaking it with the usual effects when it runs out of durability. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot)
    {
        stack.hurtAndBreak(amount, entity, owner -> owner.broadcastBreakEvent(slot));
    }

    /** The trident throw sound, which 1.21 exposes only as a registry holder. */
    // A fixed sound here; 1.21 unwraps the holder of the same sound.
    @SuppressWarnings("SameReturnValue")
    public static SoundEvent tridentThrow()
    {
        return SoundEvents.TRIDENT_THROW;
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

    /** Whether the effect harms whoever it is applied to. */
    public static boolean isHarmful(MobEffectInstance effect)
    {
        return effect.getEffect().getCategory() == MobEffectCategory.HARMFUL;
    }

    /** Whether applying the effect would change the entity: an instant effect, or one not already working on it. */
    public static boolean wouldAffect(LivingEntity entity, MobEffectInstance effect)
    {
        return effect.getEffect().isInstantenous() || !entity.hasEffect(effect.getEffect());
    }

    /** Whether a firework rocket carries stars, and so harms what is near where it bursts. */
    public static boolean fireworkExplodes(ItemStack stack)
    {
        CompoundTag fireworks = stack.getTagElement(FireworkRocketItem.TAG_FIREWORKS);
        return fireworks != null && !fireworks.getList(FireworkRocketItem.TAG_EXPLOSIONS, Tag.TAG_COMPOUND).isEmpty();
    }

    /** A text component as JSON, to keep in NBT. */
    public static String componentToJson(Component component)
    {
        return Component.Serializer.toJson(component);
    }

    /** @return the text component of JSON written by {@link #componentToJson}, or null when there is none */
    public static Component componentFromJson(String json)
    {
        return Component.Serializer.fromJson(json);
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
            if (isHumanoidArmor(slot))
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

    /** @return the factor the server's health modifier puts on the most health, 1 without one */
    public static double serverHealthFactor(AttributeInstance health)
    {
        AttributeModifier modifier = health.getModifier(SERVER_HEALTH);
        return modifier == null ? 1D : 1D + modifier.getAmount();
    }

    /**
     * Puts a factor on the most health, saved with the entity so a loaded NPC keeps the health it had; a factor of 1
     * removes the modifier.
     */
    public static void setServerHealthFactor(AttributeInstance health, double factor)
    {
        if (health.getModifier(SERVER_HEALTH) != null)
            health.removeModifier(SERVER_HEALTH);
        if (factor != 1D)
            health.addPermanentModifier(new AttributeModifier(SERVER_HEALTH, SERVER_HEALTH_NAME, factor - 1D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
