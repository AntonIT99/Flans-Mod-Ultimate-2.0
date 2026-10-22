package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Resistance;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Shot;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * What each equipment statistic is worth with the NPC's current equipment, as the weapon and armor properties pages show it.
 * Presentation only: it reads the same inspection as the attacks and the imports, and changes nothing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EquipmentReadouts
{
    private static final String PREFIX = "wolffsmodnpcs.combat.equipment.";
    private static final String TICKS = PREFIX + "ticks";

    /** @return the statistic as the slot gives it, or null when the slot is empty or offers nothing for it */
    @Nullable
    static String value(EntityNPCInterface npc, EquipmentProperty property)
    {
        return switch (property.getGroup())
        {
            case NATIVE_THROWABLE -> {
                var speed = NpcThrowables.defaultSpeed(npc);
                yield speed.isPresent() ? number(speed.getAsDouble()) : null;
            }
            case MELEE -> melee(npc, property);
            case RANGED, FIRING, PROJECTILE -> ranged(npc, property, NpcEquipment.mainRangedWeapon(npc));
            case OFF_HAND_GUN -> offHandGun(npc, property);
            case OFF_HAND_SHIELD -> shield(npc.getOffhandItem(), property);
            case ARMOR -> armor(npc, property);
            case ARMOR_EFFECTS -> armorEffect(npc, property);
        };
    }

    /** @return the NPC's own melee effect as Custom NPCs applies it: a potion effect, fire, or none */
    static String ownEffect(EntityNPCInterface npc)
    {
        int type = npc.stats.melee.getEffectType();
        int seconds = npc.stats.melee.getEffectTime();
        if (type == 0)
            return I18n.get("gui.none");
        if (type == NpcEquipmentImport.FIRE_EFFECT)
            return I18n.get(PREFIX + "fire", seconds);
        MobEffect effect = PotionEffectType.getMCType(type);
        return effect == null ? I18n.get("gui.none") : I18n.get(PREFIX + "potion", effect.getDisplayName().getString(), npc.stats.melee.getEffectStrength() + 1, seconds);
    }

    /** @return how many armor pieces the NPC wears */
    static int armorPieces(EntityNPCInterface npc)
    {
        int pieces = 0;
        for (ItemStack stack : npc.getArmorSlots())
            if (!stack.isEmpty())
                pieces++;
        return pieces;
    }

    @Nullable
    private static String melee(EntityNPCInterface npc, EquipmentProperty property)
    {
        ItemStack main = npc.getMainHandItem();
        ItemStack off = npc.getOffhandItem();
        if (NpcEquipment.enabled(npc, Feature.MELEE_DUAL_WIELD) && NpcEquipment.meleeWeapon(off))
        {
            String offValue = melee(npc, property, off);
            return NpcEquipment.meleeWeapon(main) ? I18n.get(PREFIX + "both_hands", melee(npc, property, main), offValue) : I18n.get(PREFIX + "off_hand", offValue);
        }
        return melee(npc, property, main);
    }

    @Nullable
    private static String melee(EntityNPCInterface npc, EquipmentProperty property, ItemStack item)
    {
        if (item.isEmpty())
            return null;
        return switch (property)
        {
            case MELEE_DAMAGE -> number(NpcEquipmentStats.meleeDamage(npc, item));
            case MELEE_REACH -> number(NpcEquipmentStats.meleeReach(item));
            case MELEE_DELAY -> I18n.get(TICKS, NpcEquipment.meleeDelay(item));
            case MELEE_KNOCKBACK -> number(NpcEquipmentStats.meleeKnockback(npc, item));
            default -> {
                int fire = NpcEquipment.fireSeconds(item, npc);
                yield fire > 0 ? I18n.get(PREFIX + "fire", fire) : I18n.get(PREFIX + "item_effects");
            }
        };
    }

    @Nullable
    private static String ranged(EntityNPCInterface npc, EquipmentProperty property, ItemStack item)
    {
        Shot shot = item.isEmpty() || !NpcEquipment.ranged(item) ? null : NpcEquipmentStats.shot(npc, item);
        if (shot == null)
            return null;
        return switch (property)
        {
            case RANGED_DAMAGE -> number(shot.damage());
            case RANGED_ACCURACY -> number(Math.round(NpcEquipmentStats.accuracy(shot.spread())));
            case RANGED_SPEED -> number(shot.speed() * 10D);
            case RANGED_SHOT_COUNT -> Integer.toString(shot.projectiles());
            case FIRING_SOUND -> sound(shot.firingSound());
            case FIRING_DELAY -> I18n.get(TICKS, NpcEquipmentImport.rhythm(npc, item).delay());
            case FIRING_BURST -> Integer.toString(NpcEquipmentImport.rhythm(npc, item).burst());
            case FIRING_BURST_DELAY -> I18n.get(TICKS, NpcEquipmentImport.rhythm(npc, item).burstDelay());
            case FIRING_ENTITY_HIT_SOUND -> sound(shot.impact().entityHitSound());
            case FIRING_BLOCK_HIT_SOUND -> shot.impact().materialImpactSounds() ? I18n.get("wolffsmodnpcs.weapons.material_sound") : sound(shot.impact().blockHitSound());
            case PROJECTILE_GRAVITY -> I18n.get(shot.impact().gravity() > 0D ? "gui.yes" : "gui.no");
            case PROJECTILE_EXPLOSION -> I18n.get(PREFIX + "meters", number(shot.impact().explosionRadius()));
            case PROJECTILE_KNOCKBACK -> number(shot.impact().knockback()) + "x";
            default -> I18n.get(PREFIX + "meters", number(shot.impact().hitboxSize()));
        };
    }

    private static String sound(String sound)
    {
        return sound.isBlank() ? I18n.get("wolffsmodnpcs.weapons.no_sound") : sound;
    }

    /** The off-hand gun's statistics; it only counts as a gun when it is a Flan gun that can fire from the hand. */
    @Nullable
    private static String offHandGun(EntityNPCInterface npc, EquipmentProperty property)
    {
        ItemStack item = npc.getOffhandItem();
        var gun = NpcEquipment.flan(npc, InteractionHand.OFF_HAND).filter(properties -> properties.ranged() && !properties.throwable() && !properties.shield());
        if (gun.isEmpty())
            return null;
        return switch (property)
        {
            case OFF_HAND_SHOT_DELAY -> I18n.get(TICKS, number(gun.get().shotDelay()));
            case OFF_HAND_RELOAD -> I18n.get(TICKS, number(gun.get().reloadTime()));
            default -> ranged(npc, property.mainHand(), item);
        };
    }

    /** A shield raised to block, as a vanilla shield is, or a Flan shield that protects while held. */
    @Nullable
    private static String shield(ItemStack item, EquipmentProperty property)
    {
        var flan = FlansEquipment.getShieldProperties(item);
        if (flan.isPresent())
        {
            EquippedShieldProperties shield = flan.get();
            return switch (property)
            {
                case SHIELD_KIND -> I18n.get(PREFIX + "shield.flan");
                case SHIELD_ABSORPTION -> percent(shield.damageAbsorption());
                case SHIELD_BLOCK_CHANCE -> percent(shield.blockChance());
                default -> number(shield.maxBlockableMeleeDamage());
            };
        }
        if (!NpcEquipment.shield(item))
            return null;
        // A raised vanilla shield stops every hit from the front.
        return switch (property)
        {
            case SHIELD_KIND -> I18n.get(PREFIX + "shield.raised");
            case SHIELD_MAX_BLOCK -> I18n.get(PREFIX + "shield.any");
            default -> percent(1D);
        };
    }

    private static String percent(double share)
    {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0D, Math.min(1D, share)) * 100D);
    }

    @Nullable
    private static String armor(EntityNPCInterface npc, EquipmentProperty property)
    {
        if (armorPieces(npc) == 0)
            return null;
        Resistance kind = switch (property)
        {
            case ARMOR_PROJECTILE -> Resistance.PROJECTILE;
            case ARMOR_MELEE -> Resistance.MELEE;
            case ARMOR_EXPLOSION -> Resistance.EXPLOSION;
            default -> Resistance.KNOCKBACK;
        };
        float protection = NpcEquipmentStats.protection(npc, kind);
        if (kind != Resistance.KNOCKBACK && NpcEquipmentStats.conditionalProtection(npc))
            return I18n.get("wolffsmodnpcs.weapons.item_value");
        return String.format(Locale.ROOT, "%.1f%%", Math.max(0F, Math.min(1F, protection)) * 100F);
    }

    @Nullable
    private static String armorEffect(EntityNPCInterface npc, EquipmentProperty property)
    {
        if (armorPieces(npc) == 0)
            return null;
        Predicate<EquippedArmorProperties> effect = switch (property)
        {
            case ARMOR_FIRE -> EquippedArmorProperties::fireResistance;
            case ARMOR_BREATHING -> EquippedArmorProperties::waterBreathing;
            default -> EquippedArmorProperties::negatesFallDamage;
        };
        return I18n.get(NpcEquipment.worn(npc, effect) ? "gui.yes" : "gui.no");
    }

    private static String number(double value)
    {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
