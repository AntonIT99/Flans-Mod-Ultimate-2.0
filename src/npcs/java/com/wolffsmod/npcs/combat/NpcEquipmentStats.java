package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;

/**
 * What the NPC's equipment does, as plain numbers. The editor readouts and the one-shot equipment import both read these,
 * so an imported value is the value the readout showed. Safe on either side.
 * <p>
 * Minecraft 1.21 enchantments are data-driven effects that can depend on the real target and damage source, so damage,
 * knockback and protection here are the item's base values; {@link #conditionalEffects} tells the editor when the item's
 * enchantments add to them.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipmentStats
{
    /** Vanilla inaccuracy 1 in Flan spread units, using the conversion Flan thrown weapons apply. */
    public static final double VANILLA_INACCURACY_SPREAD = 0.0172275D / 0.0025D;
    /** Arrow, trident and thrown-item hitbox width in blocks. */
    public static final double VANILLA_HITBOX = 0.5D;

    /** Ranged statistics of one shot of the held item. */
    public record Shot(double damage, double speed, double spread, int projectiles, String firingSound, EquippedWeaponProperties.Impact impact)
    {}

    /** The resistances Custom NPCs stores, in the order of its resistance sliders. */
    public enum Resistance
    {
        KNOCKBACK, PROJECTILE, MELEE, EXPLOSION
    }

    /** Ranged statistics of a Flan gun or a vanilla ranged item; {@code null} when the item's behavior cannot be inspected. */
    @Nullable
    public static Shot shot(EntityNPCInterface npc, ItemStack item)
    {
        var flan = NpcEquipment.flan(npc);
        if (flan.isPresent())
        {
            EquippedWeaponProperties properties = flan.get();
            return new Shot(properties.damage(), properties.speed(), properties.spread(), properties.projectiles(), properties.firingSound(), properties.impact());
        }
        String arrowHit = SoundEvents.ARROW_HIT.getLocation().toString();
        if (item.getItem() instanceof BowItem)
        {
            int power = enchantmentLevel(npc, item, Enchantments.POWER);
            return vanilla(2D + (power > 0 ? power * 0.5D + 0.5D : 0D), 3D, 1, SoundEvents.ARROW_SHOOT, arrowHit, arrowHit);
        }
        if (item.getItem() instanceof CrossbowItem)
            return vanilla(2D, NpcItemAttacks.CROSSBOW_VELOCITY, enchantmentLevel(npc, item, Enchantments.MULTISHOT) > 0 ? 3 : 1, SoundEvents.CROSSBOW_SHOOT, arrowHit, arrowHit);
        if (item.getItem() instanceof TridentItem)
            return vanilla(8D, 2.5D, 1, SoundEvents.TRIDENT_THROW.value(), SoundEvents.TRIDENT_HIT.getLocation().toString(), SoundEvents.TRIDENT_HIT_GROUND.getLocation().toString());
        if (item.getItem() instanceof ThrowablePotionItem)
            return vanilla(0D, 0.5D, 1, SoundEvents.SNOWBALL_THROW, "", SoundEvents.SPLASH_POTION_BREAK.getLocation().toString());
        if (item.is(Items.SNOWBALL) || item.is(Items.EGG) || item.is(Items.ENDER_PEARL))
            return vanilla(0D, 1.5D, 1, SoundEvents.SNOWBALL_THROW, "", "");
        return null;
    }

    private static Shot vanilla(double damage, double speed, int projectiles, SoundEvent firing, String entityHit, String blockHit)
    {
        // Item attacks fire with vanilla inaccuracy 1; express it in Flan spread units like the gun readouts.
        return new Shot(damage, speed, VANILLA_INACCURACY_SPREAD, projectiles, firing.getLocation().toString(),
            new EquippedWeaponProperties.Impact(1D, VANILLA_HITBOX, 1D, 0D, entityHit, blockHit, false));
    }

    /** Custom NPCs accuracy (0 to 100) equivalent of a Flan spread. */
    public static double accuracy(double spread)
    {
        return Math.clamp(100D - spread * 5D, 0D, 100D);
    }

    /** Custom NPCs none/small/medium/large explosion choice nearest to a radius in blocks. */
    public static int explosionSize(double radius)
    {
        if (radius <= 0D)
            return 0;
        if (radius < 2D)
            return 1;
        return radius < 4D ? 2 : 3;
    }

    /** Attack damage of the item; damage enchantments are conditional effects on this version. */
    public static double meleeDamage(ItemStack item)
    {
        return NpcEquipment.meleeDamage(item);
    }

    public static double meleeReach(ItemStack item)
    {
        return EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, Attributes.ENTITY_INTERACTION_RANGE, 3D);
    }

    /** Attack knockback of the item; knockback enchantments are conditional effects on this version. */
    public static double meleeKnockback(ItemStack item)
    {
        return EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, Attributes.ATTACK_KNOCKBACK, 0D);
    }

    /**
     * Whether an enchantment on the item has the given effect. Such effects need the real server target and damage source,
     * so the editor shows that the item decides the value instead of a number.
     */
    public static boolean conditionalEffects(LivingEntity holder, ItemStack item, DataComponentType<?> effect)
    {
        return item.getAllEnchantments(holder.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)).keySet().stream().anyMatch(enchantment -> enchantment.value().effects().has(effect));
    }

    /** Whether any worn armor carries a protection enchantment, whose value depends on the damage source. */
    public static boolean conditionalProtection(EntityNPCInterface npc)
    {
        for (ItemStack armor : npc.getArmorSlots())
            if (conditionalEffects(npc, armor, EnchantmentEffectComponents.DAMAGE_PROTECTION))
                return true;
        return false;
    }

    /**
     * Share of a ten-HP hit the worn armor absorbs, from 0 to 1, or the knockback resistance of the worn armor. Armor is
     * nonlinear, so the share is a preview at one reference damage. Protection enchantments count only on the server,
     * where Minecraft 1.21 evaluates them against the damage source.
     */
    public static float protection(EntityNPCInterface npc, Resistance resistance)
    {
        if (resistance == Resistance.KNOCKBACK)
            return equippedValue(npc, Attributes.KNOCKBACK_RESISTANCE);
        float enchantmentProtection = 0F;
        if (npc.level() instanceof ServerLevel level)
        {
            DamageSource source = switch (resistance)
            {
                case PROJECTILE -> new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW), npc, npc);
                case EXPLOSION -> npc.damageSources().explosion(npc, npc);
                default -> npc.damageSources().mobAttack(npc);
            };
            enchantmentProtection = EnchantmentHelper.getDamageProtection(level, npc, source);
        }
        return NpcArmorPreview.protection(10F, flanDefense(npc), equippedValue(npc, Attributes.ARMOR), equippedValue(npc, Attributes.ARMOR_TOUGHNESS), enchantmentProtection);
    }

    private static int enchantmentLevel(LivingEntity holder, ItemStack item, ResourceKey<Enchantment> enchantment)
    {
        return item.getEnchantmentLevel(holder.registryAccess().holderOrThrow(enchantment));
    }

    private static float flanDefense(EntityNPCInterface npc)
    {
        float defense = 0F;
        for (ItemStack stack : npc.getArmorSlots())
        {
            var properties = FlansEquipment.getArmorProperties(stack);
            if (properties.isPresent())
                defense = (float) (defense + properties.get().defense());
        }
        return defense;
    }

    private static float equippedValue(EntityNPCInterface npc, Holder<Attribute> attribute)
    {
        var modifiers = new ArrayList<AttributeModifier>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                npc.getItemBySlot(slot).forEachModifier(slot, (current, modifier) ->
                {
                    if (current.equals(attribute))
                        modifiers.add(modifier);
                });
        }
        var instance = npc.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }
}
