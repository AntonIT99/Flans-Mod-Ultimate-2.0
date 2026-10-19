package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

/**
 * What the NPC's equipment does, as plain numbers. The editor readouts and the one-shot equipment import both read these,
 * so an imported value is the value the readout showed. Safe on either side.
 * <p>
 * Enchantment bonuses come from {@link NpcPlatform}. Where only the real target and damage source decide one, as for
 * Minecraft 1.21's data-driven enchantments, the values here are the item's own, and
 * {@link NpcPlatform#conditionalEffect} and {@link #conditionalProtection} tell the editor that the item decides them.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipmentStats
{
    /** Vanilla inaccuracy 1 in Flan spread units, using the conversion Flan thrown weapons apply. */
    public static final double VANILLA_INACCURACY_SPREAD = 0.0172275D / 0.0025D;
    /** Arrow, trident and thrown-item hitbox width in blocks. */
    public static final double VANILLA_HITBOX = 0.5D;

    /** Ranged statistics of one shot of the held item; an absent firing sound is an empty string. */
    public record Shot(double damage, double speed, double spread, int projectiles, String firingSound, EquippedWeaponProperties.Impact impact)
    {
        public Shot
        {
            // Legacy weapons may omit ShootSound. Both the import and editor readouts expect a string.
            firingSound = firingSound == null ? "" : firingSound;
        }
    }

    /** The resistances Custom NPCs stores, in the order of its resistance sliders. */
    public enum Resistance
    {
        KNOCKBACK, PROJECTILE, MELEE, EXPLOSION
    }

    /** Ranged statistics of a Flan gun or a vanilla ranged item; {@code null} when the item's behavior cannot be inspected. */
    @Nullable
    public static Shot shot(EntityNPCInterface npc, ItemStack item)
    {
        var flan = item == npc.getMainHandItem() ? NpcEquipment.flan(npc) : FlansEquipment.getWeaponProperties(item, ItemStack.EMPTY);
        if (flan.isPresent())
        {
            EquippedWeaponProperties properties = flan.get();
            return new Shot(properties.damage(), properties.speed(), properties.spread(), properties.projectiles(), properties.firingSound(), properties.impact());
        }
        String arrowHit = SoundEvents.ARROW_HIT.getLocation().toString();
        if (item.getItem() instanceof BowItem)
        {
            int power = NpcPlatform.powerLevel(npc, item);
            return vanilla(2D + (power > 0 ? power * 0.5D + 0.5D : 0D), 3D, 1, SoundEvents.ARROW_SHOOT, arrowHit, arrowHit);
        }
        if (item.getItem() instanceof CrossbowItem)
            return vanilla(2D, NpcItemAttacks.CROSSBOW_VELOCITY, NpcPlatform.multishotLevel(npc, item) > 0 ? 3 : 1, SoundEvents.CROSSBOW_SHOOT, arrowHit, arrowHit);
        if (item.getItem() instanceof TridentItem)
            return vanilla(8D, 2.5D, 1, NpcPlatform.tridentThrowSound(), SoundEvents.TRIDENT_HIT.getLocation().toString(), SoundEvents.TRIDENT_HIT_GROUND.getLocation().toString());
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
        return Mth.clamp(100D - spread * 5D, 0D, 100D);
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

    /** Attack damage plus the enchantment bonus against a generic target. */
    public static double meleeDamage(LivingEntity holder, ItemStack item)
    {
        return NpcEquipment.meleeDamage(item) + NpcPlatform.meleeDamageBonus(holder, item);
    }

    public static double meleeReach(ItemStack item)
    {
        return EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, NpcPlatform.reachAttribute(), 3D);
    }

    /** Attack knockback plus the enchantment bonus. */
    public static double meleeKnockback(LivingEntity holder, ItemStack item)
    {
        return EquipmentAttributes.value(item, EquipmentSlot.MAINHAND, Attributes.ATTACK_KNOCKBACK, 0D) + NpcPlatform.meleeKnockbackBonus(holder, item);
    }

    /** Whether any worn armor carries a protection enchantment whose value only the damage source decides. */
    public static boolean conditionalProtection(EntityNPCInterface npc)
    {
        for (ItemStack armor : npc.getArmorSlots())
            if (NpcPlatform.conditionalEffect(npc, armor, NpcPlatform.Effect.PROTECTION))
                return true;
        return false;
    }

    /**
     * Share of a ten-HP hit the worn armor absorbs, from 0 to 1, or the knockback resistance of the worn armor. Armor is
     * nonlinear, so the share is a preview at one reference damage.
     */
    public static float protection(EntityNPCInterface npc, Resistance resistance)
    {
        if (resistance == Resistance.KNOCKBACK)
            return NpcPlatform.equippedArmorValue(npc, Attributes.KNOCKBACK_RESISTANCE);
        DamageSource source = switch (resistance)
        {
            case PROJECTILE -> new DamageSource(npc.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW), npc, npc);
            case EXPLOSION -> npc.damageSources().explosion(npc, npc);
            default -> npc.damageSources().mobAttack(npc);
        };
        return NpcArmorPreview.protection(10F, flanDefense(npc), NpcPlatform.equippedArmorValue(npc, Attributes.ARMOR), NpcPlatform.equippedArmorValue(npc, Attributes.ARMOR_TOUGHNESS),
            NpcPlatform.armorEnchantmentProtection(npc, source));
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
}
