package com.wolffsmod.npcs.platform;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.EquipmentAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Version boundary for the NeoForge and Minecraft entity, item, sound, attribute, enchantment and fluid APIs the module
 * reads. Minecraft 1.21 enchantments are data-driven effects that may need the real target and damage source, so their
 * bonuses are left to real combat and {@link #conditionalEffect} tells the editor when the item decides a value.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPlatform
{
    private static final ResourceLocation KNOCKBACK_IMMUNITY = ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "knockback_immunity");
    private static final ResourceLocation SERVER_HEALTH = ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "server_health");

    /** Enchantment effects the editor readouts may not be able to show as a number. */
    public enum Effect
    {
        DAMAGE, KNOCKBACK, PROTECTION
    }

    /** An enchanted bow's arrow and the ammunition it costs; zero means the shot is free and the arrow cannot be picked up. */
    public record BowShot(AbstractArrow arrow, int usedAmmo)
    {}

    /** Whether the slot is one of the four worn armor slots, not a hand or a body slot. */
    public static boolean isHumanoidArmor(EquipmentSlot slot)
    {
        return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public static float width(EntityDimensions dimensions)
    {
        return dimensions.width();
    }

    public static float height(EntityDimensions dimensions)
    {
        return dimensions.height();
    }

    /** Whether the entity has been added to a level, as opposed to only constructed. */
    public static boolean isAddedToLevel(Entity entity)
    {
        return entity.isAddedToLevel();
    }

    /** Offset from a vehicle seat position to where the passenger stands. */
    public static Vec3 riderOffset(Entity passenger, Entity vehicle)
    {
        // Minecraft 1.21 replaced the rider's own riding offset with its attachment point on the vehicle.
        return passenger.getVehicleAttachmentPoint(vehicle).reverse();
    }

    /** Ticks a crossbow takes to charge for the entity. */
    public static int crossbowChargeDuration(ItemStack crossbow, LivingEntity entity)
    {
        return CrossbowItem.getChargeDuration(crossbow, entity);
    }

    /** Whether both stacks hold the same item with the same custom data, ignoring count. */
    public static boolean isSameItemSameData(ItemStack first, ItemStack second)
    {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    /** Damages the stack, breaking it with the usual effects when it runs out of durability. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot)
    {
        stack.hurtAndBreak(amount, entity, slot);
    }

    /** The trident throw sound, which 1.21 exposes only as a registry holder. */
    public static SoundEvent tridentThrow()
    {
        return SoundEvents.TRIDENT_THROW.value();
    }

    /** Whether the item blocks like a shield. */
    public static boolean canShieldBlock(ItemStack stack)
    {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** The effects a potion item brings: its potion's, then its own custom ones. */
    public static List<MobEffectInstance> potionEffects(ItemStack stack)
    {
        List<MobEffectInstance> effects = new ArrayList<>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(effects::add);
        return effects;
    }

    /** Whether the effect harms whoever it is applied to. */
    public static boolean isHarmful(MobEffectInstance effect)
    {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    /** Whether applying the effect would change the entity: an instant effect, or one not already working on it. */
    public static boolean wouldAffect(LivingEntity entity, MobEffectInstance effect)
    {
        return effect.getEffect().value().isInstantenous() || !entity.hasEffect(effect.getEffect());
    }

    /** Whether a firework rocket carries stars, and so harms what is near where it bursts. */
    public static boolean fireworkExplodes(ItemStack stack)
    {
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks != null && !fireworks.explosions().isEmpty();
    }

    /** A text component as JSON, to keep in NBT. Text needs no registry data, so the built-in registries serve. */
    public static String componentToJson(Component component)
    {
        return Component.Serializer.toJson(component, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    /** @return the text component of JSON written by {@link #componentToJson}, or null when there is none */
    public static Component componentFromJson(String json)
    {
        return Component.Serializer.fromJson(json, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
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

    /** Entity interaction reach in blocks, including the creative-mode bonus. */
    // A seam: 1.21.1 vanilla applies the creative bonus as an attribute modifier; the 1.20.1 platform reads Forge's reach.
    public static double entityInteractionRange(Player player)
    {
        return player.entityInteractionRange();
    }

    /** How deep the entity stands in water, in blocks. */
    public static double waterHeight(Entity entity)
    {
        return entity.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value());
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

    /** Melee damage after the weapon's enchantments act on the real target. */
    public static float meleeDamage(LivingEntity holder, ItemStack weapon, LivingEntity target, float damage)
    {
        return EnchantmentHelper.modifyDamage((ServerLevel) holder.level(), weapon, target, holder.damageSources().mobAttack(holder), damage);
    }

    /** Melee knockback after the weapon's enchantments act on the real target; 1.21 enchantments scale it rather than add a level. */
    public static double meleeKnockback(LivingEntity holder, ItemStack weapon, LivingEntity target, double attributeKnockback)
    {
        return EnchantmentHelper.modifyKnockback((ServerLevel) holder.level(), weapon, target, holder.damageSources().mobAttack(holder), (float) attributeKnockback);
    }

    /**
     * Post-attack enchantment effects of a melee hit. 1.21 applies the target's and the striking weapon's effects together, so
     * the idle hand of a dual wielder never adds its own.
     */
    // 1.20.1 needs the dual-wield flag to skip the idle hand's scalar enchantments.
    @SuppressWarnings({"java:S1172", "unused"})
    public static void meleePostAttackEffects(LivingEntity holder, LivingEntity target, ItemStack weapon, boolean dualWield)
    {
        EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel) holder.level(), target, holder.damageSources().mobAttack(holder), weapon);
    }

    /** The weapon's own on-hit behaviour; 1.21 splits durability loss into postHurtEnemy. */
    public static void hurtEnemy(ItemStack weapon, LivingEntity target, LivingEntity attacker)
    {
        if (weapon.getItem().hurtEnemy(weapon, target, attacker) && !weapon.isEmpty())
            weapon.getItem().postHurtEnemy(weapon, target, attacker);
    }

    public static void ignite(Entity entity, int seconds)
    {
        entity.igniteForSeconds(seconds);
    }

    public static MobEffectInstance effectInstance(MobEffect effect, int durationTicks, int amplifier)
    {
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), durationTicks, amplifier);
    }

    /** Builds an enchanted bow's arrow from one round of the ammunition; null when the ammunition cannot pay for it. */
    public static BowShot bowArrow(LivingEntity shooter, ItemStack weapon, BowItem bow, ArrowItem arrowItem, ItemStack ammo)
    {
        int usedAmmo = arrowItem.isInfinite(ammo, weapon, shooter) ? 0 : EnchantmentHelper.processAmmoUse((ServerLevel) shooter.level(), weapon, ammo, 1);
        if (usedAmmo > ammo.getCount())
            return null;
        ItemStack projectileAmmo = ammo.copyWithCount(1);
        if (usedAmmo == 0)
            projectileAmmo.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
        // Modern arrows retain the firing weapon and apply its enchantment effects natively.
        AbstractArrow arrow = bow.customArrow(arrowItem.createArrow(shooter.level(), projectileAmmo, shooter, weapon), projectileAmmo, weapon);
        return new BowShot(arrow, usedAmmo);
    }

    /** Loads the ammunition into an uncharged crossbow, as many rounds as its enchantments fire; false when it cannot pay. */
    public static boolean chargeCrossbow(LivingEntity shooter, ItemStack weapon, ItemStack ammo)
    {
        ServerLevel level = (ServerLevel) shooter.level();
        int usedAmmo = EnchantmentHelper.processAmmoUse(level, weapon, ammo, 1);
        if (usedAmmo > ammo.getCount())
            return false;
        List<ItemStack> projectiles = new ArrayList<>();
        int count = EnchantmentHelper.processProjectileCount(level, weapon, shooter, 1);
        for (int i = 0; i < count; i++)
        {
            ItemStack projectileAmmo = ammo.copyWithCount(1);
            if (usedAmmo == 0 || i > 0)
                projectileAmmo.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            projectiles.add(projectileAmmo);
        }
        weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
        ammo.shrink(usedAmmo);
        return true;
    }

    public static boolean crossbowHasRocket(ItemStack weapon)
    {
        return weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).contains(Items.FIREWORK_ROCKET);
    }

    /** Fires and empties a charged crossbow from the main hand. */
    public static void shootCrossbow(LivingEntity shooter, ItemStack weapon, CrossbowItem crossbow, float speed, float inaccuracy)
    {
        crossbow.performShooting(shooter.level(), shooter, InteractionHand.MAIN_HAND, weapon, speed, inaccuracy, null);
    }

    /** Whether the trident launches its holder instead of being thrown. */
    public static boolean hasRiptide(ItemStack weapon, LivingEntity holder)
    {
        return EnchantmentHelper.getTridentSpinAttackStrength(weapon, holder) > 0;
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
            if (isHumanoidArmor(slot))
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

    /** The highest maximum health the game accepts. */
    public static double maxHealthLimit()
    {
        // Attributes are holders on 1.21; the holder itself is never a RangedAttribute.
        return Attributes.MAX_HEALTH.value() instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
    }

    /** A blaze fireball fired by the shooter along the given direction. */
    public static SmallFireball smallFireball(Level level, LivingEntity shooter, Vec3 direction)
    {
        return new SmallFireball(level, shooter, direction);
    }

    /** Adds or removes the full knockback-resistance modifier that keeps an immune NPC in place. */
    public static void setKnockbackImmunity(AttributeInstance resistance, boolean immune)
    {
        boolean applied = resistance.getModifier(KNOCKBACK_IMMUNITY) != null;
        if (immune && !applied)
            resistance.addTransientModifier(new AttributeModifier(KNOCKBACK_IMMUNITY, 1D, AttributeModifier.Operation.ADD_VALUE));
        else if (!immune && applied)
            resistance.removeModifier(KNOCKBACK_IMMUNITY);
    }

    /** @return the factor the server's health modifier puts on the most health, 1 without one */
    public static double serverHealthFactor(AttributeInstance health)
    {
        AttributeModifier modifier = health.getModifier(SERVER_HEALTH);
        return modifier == null ? 1D : 1D + modifier.amount();
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
            health.addPermanentModifier(new AttributeModifier(SERVER_HEALTH, factor - 1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
