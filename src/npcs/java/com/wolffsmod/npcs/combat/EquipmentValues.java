package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedArmorProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.Resistances;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.util.Mth;

import java.util.function.Predicate;

/**
 * The NPC's own value of each equipment statistic: the Custom NPCs field that serves when the NPC does not take the
 * statistic from its equipment, read and written as the weapon and armor properties pages edit it. Resistances are expressed as
 * the share of a hit absorbed, in percent, as the Custom NPCs resistance sliders show them. Either side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EquipmentValues
{
    /** How a statistic's own value is edited; NONE is several fields at once, edited in the standard editor. */
    public enum Kind
    {
        INTEGER, DECIMAL, FLAG, TEXT, NONE
    }

    public static Kind kind(EquipmentProperty property)
    {
        return switch (property.mainHand())
        {
            case MELEE_DAMAGE, MELEE_DELAY, MELEE_KNOCKBACK, RANGED_DAMAGE, RANGED_ACCURACY, RANGED_SPEED, RANGED_SHOT_COUNT -> Kind.INTEGER;
            case MELEE_REACH, ARMOR_PROJECTILE, ARMOR_MELEE, ARMOR_EXPLOSION, ARMOR_KNOCKBACK, NATIVE_THROW_SPEED -> Kind.DECIMAL;
            case ARMOR_FIRE, ARMOR_BREATHING, ARMOR_FALL -> Kind.FLAG;
            case FIRING_SOUND -> Kind.TEXT;
            default -> Kind.NONE;
        };
    }

    /** @return the smallest value the NPC's own field accepts, as the Custom NPCs editor limits it */
    public static double minimum(EquipmentProperty property)
    {
        return switch (property.mainHand())
        {
            case MELEE_DELAY, RANGED_SHOT_COUNT -> 1D;
            case MELEE_KNOCKBACK -> -10D;
            case NATIVE_THROW_SPEED -> NpcThrowables.MIN_SPEED;
            case ARMOR_PROJECTILE, ARMOR_MELEE, ARMOR_EXPLOSION, ARMOR_KNOCKBACK -> -100D;
            default -> 0D;
        };
    }

    public static double maximum(EquipmentProperty property)
    {
        return switch (property.mainHand())
        {
            case MELEE_DELAY -> 1000D;
            case MELEE_KNOCKBACK, RANGED_SHOT_COUNT -> 10D;
            case MELEE_REACH -> 64D;
            case NATIVE_THROW_SPEED -> NpcThrowables.MAX_SPEED;
            case RANGED_ACCURACY, RANGED_SPEED, ARMOR_PROJECTILE, ARMOR_MELEE, ARMOR_EXPLOSION, ARMOR_KNOCKBACK -> 100D;
            default -> 100_000D;
        };
    }

    /** @return the model property that shares the statistic's Custom NPCs field, or null when none does */
    @Nullable
    public static NpcTypeProperty modelProperty(EquipmentProperty property)
    {
        return switch (property.mainHand())
        {
            case MELEE_REACH -> NpcTypeProperty.MELEE_REACH;
            case RANGED_DAMAGE -> NpcTypeProperty.DAMAGE;
            case RANGED_ACCURACY -> NpcTypeProperty.ACCURACY;
            case RANGED_SPEED -> NpcTypeProperty.SPEED;
            case RANGED_SHOT_COUNT -> NpcTypeProperty.SHOT_COUNT;
            case FIRING_SOUND -> NpcTypeProperty.FIRING_SOUND;
            case ARMOR_BREATHING -> NpcTypeProperty.CAN_DROWN;
            case ARMOR_FALL -> NpcTypeProperty.NO_FALL_DAMAGE;
            default -> null;
        };
    }

    /** Whether the selected model currently supplies the field, which then is not the NPC's to edit. */
    public static boolean modelLocked(EntityNPCInterface npc, EquipmentProperty property)
    {
        NpcTypeProperty shared = modelProperty(property);
        return shared != null && NpcTypeProperties.of(npc).values().containsKey(shared);
    }

    /** Whether the NPC takes the statistic from its equipment right now, so that its own value is not used. */
    public static boolean inUse(EntityNPCInterface npc, EquipmentProperty property)
    {
        return switch (property.getGroup())
        {
            case MELEE -> NpcEquipment.itemMelee(npc, property);
            case NATIVE_THROWABLE -> NpcEquipment.inherits(npc, property) && NpcThrowables.defaultSpeed(npc).isPresent();
            // The main hand's ranged weapon only fires while an off-hand gun is not the NPC's only one.
            case RANGED, FIRING, PROJECTILE ->
                NpcEquipment.inherits(npc, property) && NpcEquipment.weaponAuthority(npc) && !NpcEquipment.offHandPrimary(npc) && NpcEquipment.ranged(NpcEquipment.mainRangedWeapon(npc));
            case OFF_HAND_GUN -> NpcEquipment.inherits(npc, property) && NpcEquipment.offHandFires(npc);
            case OFF_HAND_SHIELD ->
                NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ITEM_WEAPONS) && (NpcEquipment.shield(npc.getOffhandItem()) || FlansEquipment.getShieldProperties(npc.getOffhandItem()).isPresent());
            case ARMOR -> NpcEquipment.inherits(npc, property);
            case ARMOR_EFFECTS -> NpcEquipment.armorProtects(npc, property, effect(property));
        };
    }

    /** The armor property behind an armor effect statistic. */
    public static Predicate<EquippedArmorProperties> effect(EquipmentProperty property)
    {
        return switch (property)
        {
            case ARMOR_FIRE -> EquippedArmorProperties::fireResistance;
            case ARMOR_BREATHING -> EquippedArmorProperties::waterBreathing;
            default -> EquippedArmorProperties::negatesFallDamage;
        };
    }

    /** @return the NPC's own value: an int, float, byte or string tag after {@link #kind}; null for {@link Kind#NONE} */
    @Nullable
    public static Tag read(EntityNPCInterface npc, EquipmentProperty statistic)
    {
        EquipmentProperty property = statistic.mainHand();
        NpcTypeProperty shared = modelProperty(property);
        if (shared != null)
        {
            Tag own = NpcTypeProperties.of(npc).own(npc, shared);
            // The NPC can drown unless it breathes water.
            return property == EquipmentProperty.ARMOR_BREATHING ? ByteTag.valueOf(((NumericTag) own).getAsByte() == 0) : own;
        }
        Resistances resistances = npc.stats.resistances;
        return switch (property)
        {
            case MELEE_DAMAGE -> IntTag.valueOf(npc.stats.melee.getStrength());
            case MELEE_DELAY -> IntTag.valueOf(npc.stats.melee.getDelay());
            case MELEE_KNOCKBACK -> IntTag.valueOf(npc.stats.melee.getKnockback());
            case ARMOR_PROJECTILE -> percent(resistances.arrow);
            case ARMOR_MELEE -> percent(resistances.melee);
            case ARMOR_EXPLOSION -> percent(resistances.explosion);
            case ARMOR_KNOCKBACK -> percent(resistances.knockback);
            case ARMOR_FIRE -> ByteTag.valueOf(npc.stats.immuneToFire);
            case NATIVE_THROW_SPEED -> FloatTag.valueOf(NpcThrowables.ownSpeed(npc));
            default -> null;
        };
    }

    /** Sets the NPC's own value, clamped to what its field accepts. */
    public static void write(EntityNPCInterface npc, EquipmentProperty statistic, Tag value)
    {
        EquipmentProperty property = statistic.mainHand();
        Tag clamped = clamp(property, value);
        NpcTypeProperty shared = modelProperty(property);
        if (shared != null)
        {
            Tag own = property == EquipmentProperty.ARMOR_BREATHING ? ByteTag.valueOf(((NumericTag) clamped).getAsByte() == 0) : clamped;
            NpcTypeProperties.of(npc).setOwn(npc, shared, own);
            return;
        }
        Resistances resistances = npc.stats.resistances;
        switch (property)
        {
            case MELEE_DAMAGE -> npc.stats.melee.setStrength(integer(clamped));
            case MELEE_DELAY -> npc.stats.melee.setDelay(integer(clamped));
            case MELEE_KNOCKBACK -> npc.stats.melee.setKnockback(integer(clamped));
            case ARMOR_PROJECTILE -> resistances.arrow = resistance(clamped);
            case ARMOR_MELEE -> resistances.melee = resistance(clamped);
            case ARMOR_EXPLOSION -> resistances.explosion = resistance(clamped);
            case ARMOR_KNOCKBACK -> resistances.knockback = resistance(clamped);
            case ARMOR_FIRE -> npc.stats.immuneToFire = ((NumericTag) clamped).getAsByte() != 0;
            case NATIVE_THROW_SPEED -> ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().setNativeThrowSpeed(((NumericTag) clamped).getAsFloat());
            default -> {
                /* Several fields at once, edited in the standard editor. */ }
        }
    }

    private static Tag clamp(EquipmentProperty property, Tag value)
    {
        if (!(value instanceof NumericTag number) || kind(property) == Kind.FLAG)
            return value;
        double clamped = Mth.clamp(number.getAsDouble(), minimum(property), maximum(property));
        return kind(property) == Kind.INTEGER ? IntTag.valueOf((int) clamped) : FloatTag.valueOf((float) clamped);
    }

    /** Custom NPCs stores a resistance as 1 plus the share absorbed: 1 is neutral, 2 immune and 0 doubles the damage. */
    private static FloatTag percent(float resistance)
    {
        return FloatTag.valueOf(Math.round((resistance - 1F) * 1000F) / 10F);
    }

    private static float resistance(Tag percent)
    {
        return 1F + ((NumericTag) percent).getAsFloat() / 100F;
    }

    private static int integer(Tag value)
    {
        return ((NumericTag) value).getAsInt();
    }
}
