package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.minecraft.world.InteractionHand;

import java.util.Arrays;
import java.util.List;

/**
 * A statistic the NPC's equipment supplies. While its matching feature switch is on, the NPC either inherits the statistic
 * from the slot, live, or keeps its own saved value for it; either way the slot's value can be imported into the saved one.
 * A statistic that cannot be switched is always the item's while the switch is on, and can only be imported.
 */
@RequiredArgsConstructor
public enum EquipmentProperty
{
    // spotless:off
    MELEE_DAMAGE(Group.MELEE, true), MELEE_REACH(Group.MELEE, true), MELEE_DELAY(Group.MELEE, true), MELEE_KNOCKBACK(Group.MELEE, true), MELEE_EFFECT(Group.MELEE, true),
    RANGED_DAMAGE(Group.RANGED, true), RANGED_ACCURACY(Group.RANGED, true), RANGED_SPEED(Group.RANGED, true), RANGED_SHOT_COUNT(Group.RANGED, true),
    FIRING_SOUND(Group.FIRING, true), FIRING_DELAY(Group.FIRING, false), FIRING_BURST(Group.FIRING, false), FIRING_BURST_DELAY(Group.FIRING, false),
    FIRING_ENTITY_HIT_SOUND(Group.FIRING, false), FIRING_BLOCK_HIT_SOUND(Group.FIRING, false),
    PROJECTILE_GRAVITY(Group.PROJECTILE, false), PROJECTILE_EXPLOSION(Group.PROJECTILE, false), PROJECTILE_KNOCKBACK(Group.PROJECTILE, false), PROJECTILE_SIZE(Group.PROJECTILE, false),
    OFF_HAND_DAMAGE(Group.OFF_HAND_GUN, true), OFF_HAND_ACCURACY(Group.OFF_HAND_GUN, true), OFF_HAND_SPEED(Group.OFF_HAND_GUN, true), OFF_HAND_SHOT_COUNT(Group.OFF_HAND_GUN, true),
    OFF_HAND_SOUND(Group.OFF_HAND_GUN, true), OFF_HAND_SHOT_DELAY(Group.OFF_HAND_GUN, false), OFF_HAND_RELOAD(Group.OFF_HAND_GUN, false),
    SHIELD_KIND(Group.OFF_HAND_SHIELD, false), SHIELD_ABSORPTION(Group.OFF_HAND_SHIELD, false), SHIELD_BLOCK_CHANCE(Group.OFF_HAND_SHIELD, false),
    SHIELD_MAX_BLOCK(Group.OFF_HAND_SHIELD, false),
    ARMOR_PROJECTILE(Group.ARMOR, true), ARMOR_MELEE(Group.ARMOR, true), ARMOR_EXPLOSION(Group.ARMOR, true), ARMOR_KNOCKBACK(Group.ARMOR, true),
    ARMOR_FIRE(Group.ARMOR_EFFECTS, true), ARMOR_BREATHING(Group.ARMOR_EFFECTS, true), ARMOR_FALL(Group.ARMOR_EFFECTS, true), NATIVE_THROW_SPEED(Group.NATIVE_THROWABLE, true);
    // spotless:on

    /** The slots with a page of their own, each listing the statistics read from them. */
    public enum Slot
    {
        MAIN_HAND, OFF_HAND, ARMOR, PROJECTILE_SLOT
    }

    /** The tabs of the slot pages, each the statistics of one kind of use of the slot. */
    @RequiredArgsConstructor
    public enum Group
    {
        MELEE(Slot.MAIN_HAND), RANGED(Slot.MAIN_HAND), FIRING(Slot.MAIN_HAND), PROJECTILE(Slot.MAIN_HAND), OFF_HAND_GUN(Slot.OFF_HAND), OFF_HAND_SHIELD(Slot.OFF_HAND), ARMOR(
            Slot.ARMOR), ARMOR_EFFECTS(Slot.ARMOR), NATIVE_THROWABLE(Slot.PROJECTILE_SLOT);

        @Getter
        private final Slot slot;

        /** @return the switch that gives this equipment or native-projectile group authority */
        public Feature feature()
        {
            if (this == NATIVE_THROWABLE)
                return Feature.NATIVE_THROWABLES;
            return slot == Slot.ARMOR ? Feature.ITEM_ARMOR : Feature.ITEM_WEAPONS;
        }

        public List<EquipmentProperty> properties()
        {
            return Arrays.stream(EquipmentProperty.values()).filter(property -> property.group == this).toList();
        }
    }

    @Getter
    private final Group group;
    /** Whether the NPC can keep its own value while the switch is on, rather than only import the item's. */
    @Getter
    private final boolean switchable;

    /**
     * Whether the item's value can be copied into the NPC's own. Projectile knockback and size cannot: Custom NPCs counts
     * them as punch levels and render size, not as the share of knockback a round keeps and the width of its hitbox. An
     * off-hand gun's cadence and a shield have no Custom NPCs field at all.
     */
    public boolean importable()
    {
        return this != PROJECTILE_KNOCKBACK && this != PROJECTILE_SIZE && this != OFF_HAND_SHOT_DELAY && this != OFF_HAND_RELOAD && group != Group.OFF_HAND_SHIELD;
    }

    /**
     * @return the main-hand statistic whose Custom NPCs field this one shares: the NPC has one set of ranged values, which
     *         either hand fires with when it keeps its own; the statistic itself for any other
     */
    public EquipmentProperty mainHand()
    {
        return switch (this)
        {
            case OFF_HAND_DAMAGE -> RANGED_DAMAGE;
            case OFF_HAND_ACCURACY -> RANGED_ACCURACY;
            case OFF_HAND_SPEED -> RANGED_SPEED;
            case OFF_HAND_SHOT_COUNT -> RANGED_SHOT_COUNT;
            case OFF_HAND_SOUND -> FIRING_SOUND;
            default -> this;
        };
    }

    /** @return the statistic a gun in this hand fires with: this main-hand statistic, or its off-hand counterpart */
    public EquipmentProperty inHand(InteractionHand hand)
    {
        if (hand == InteractionHand.MAIN_HAND)
            return this;
        return switch (this)
        {
            case RANGED_DAMAGE -> OFF_HAND_DAMAGE;
            case RANGED_ACCURACY -> OFF_HAND_ACCURACY;
            case RANGED_SPEED -> OFF_HAND_SPEED;
            case RANGED_SHOT_COUNT -> OFF_HAND_SHOT_COUNT;
            case FIRING_SOUND -> OFF_HAND_SOUND;
            default -> this;
        };
    }
}
