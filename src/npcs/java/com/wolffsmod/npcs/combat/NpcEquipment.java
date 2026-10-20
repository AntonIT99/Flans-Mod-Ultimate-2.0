package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

import java.util.*;

/** Item classification and combat defaults. Readouts use the same inspector as runtime actions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcEquipment
{
    public static boolean enabled(EntityNPCInterface npc, Feature feature)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
    }

    public static boolean weaponAuthority(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.ITEM_WEAPONS) && !rangedWeapon(npc).isEmpty();
    }

    /** Whether the main-hand item, rather than the NPC's melee stats, swings in melee: an empty hand leaves melee to the NPC. */
    public static boolean meleeAuthority(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.ITEM_WEAPONS) && !npc.getMainHandItem().isEmpty();
    }

    /** Whether a melee hit goes through the item; with every hit statistic the NPC's own, Custom NPCs strikes as usual. */
    public static boolean itemMelee(EntityNPCInterface npc)
    {
        return meleeAuthority(npc) && (inherits(npc, EquipmentProperty.MELEE_DAMAGE) || inherits(npc, EquipmentProperty.MELEE_KNOCKBACK) || inherits(npc, EquipmentProperty.MELEE_EFFECT));
    }

    /** Whether the main-hand item decides this melee statistic. */
    public static boolean itemMelee(EntityNPCInterface npc, EquipmentProperty property)
    {
        return meleeAuthority(npc) && inherits(npc, property);
    }

    /**
     * Whether the NPC takes this statistic from its equipment: the matching Use equipped switch is on and the NPC does not
     * keep its own saved value for it.
     */
    public static boolean inherits(EntityNPCInterface npc, EquipmentProperty property)
    {
        return enabled(npc, property.getGroup().feature()) && !((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().isCustom(property);
    }

    public static boolean ranged(EntityNPCInterface npc)
    {
        return weaponAuthority(npc) && ranged(rangedWeapon(npc));
    }

    /** @return whether the NPC attacks at range: with an equipped ranged weapon, or else with what its projectile slot holds */
    public static boolean fightsAtRange(EntityNPCInterface npc)
    {
        return weaponAuthority(npc) ? ranged(npc) : npc.inventory.getProjectile() != null;
    }

    /**
     * The projectile Custom NPCs' combat AI decides between ranged and melee goals by. An equipped ranged weapon stands in
     * for one, so the NPC gets the ranged attack and the ranged tactics; any other equipped weapon fights in melee whatever
     * the projectile slot holds.
     */
    @Nullable
    public static IItemStack combatProjectile(EntityNPCInterface npc)
    {
        if (ranged(npc))
            return NpcAPI.Instance().getIItemStack(rangedWeapon(npc));
        return weaponAuthority(npc) ? null : npc.inventory.getProjectile();
    }

    /**
     * A ranged weapon in the main hand takes precedence, then a Flan gun in the off hand, which the NPC then fires on its
     * own; otherwise a Flan throwable in the projectile slot can be thrown.
     */
    public static ItemStack rangedWeapon(EntityNPCInterface npc)
    {
        ItemStack held = npc.getMainHandItem();
        if (!ranged(held) && offHandFlanGun(npc))
            return npc.getOffhandItem();
        return mainRangedWeapon(npc);
    }

    /**
     * The ranged weapon of the main hand: the held one, or else a Flan throwable in the projectile slot, which is drawn into
     * the main hand to be thrown. An off-hand gun is left out.
     */
    public static ItemStack mainRangedWeapon(EntityNPCInterface npc)
    {
        ItemStack held = npc.getMainHandItem();
        if (ranged(held))
            return held;
        IItemStack slot = npc.inventory.getProjectile();
        ItemStack projectile = slot == null ? ItemStack.EMPTY : slot.getMCItemStack();
        if (projectile != null && (FlansEquipment.getWeaponProperties(projectile, ItemStack.EMPTY).filter(EquippedWeaponProperties::throwable).isPresent()
            || enabled(npc, Feature.GRENADES) && FlansProjectiles.isGrenade(projectile)))
            return projectile;
        return held;
    }

    public static boolean ranged(ItemStack stack)
    {
        if (FlansProjectiles.isGrenade(stack))
            return true;
        if (FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(EquippedWeaponProperties::ranged).orElse(false))
            return true;
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem || stack.getItem() instanceof ThrowablePotionItem
            || stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL) || stack.getItem() instanceof NpcWeaponAdapter;
    }

    public static Optional<EquippedWeaponProperties> flan(EntityNPCInterface npc)
    {
        return FlansEquipment.getWeaponProperties(npc.getMainHandItem(), noppes.npcs.api.wrapper.ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
    }

    public static Optional<EquippedWeaponProperties> flan(EntityNPCInterface npc, InteractionHand hand)
    {
        return FlansEquipment.getWeaponProperties(npc.getItemInHand(hand), noppes.npcs.api.wrapper.ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
    }

    /** Whether the off hand holds a second Flan gun that the NPC fires alongside its main-hand weapon. */
    public static boolean offHandGun(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.DUAL_WIELD) && weaponAuthority(npc) && !offHandPrimary(npc) && offHandFlanGun(npc);
    }

    /** Whether a Flan gun in the off hand fires, either beside the main-hand weapon or on its own. */
    public static boolean offHandFires(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.ITEM_WEAPONS) && (offHandPrimary(npc) || offHandGun(npc));
    }

    /** The hand whose gun the NPC fires as its weapon: the off hand only while it is the only one with a ranged weapon. */
    public static InteractionHand firingHand(EntityNPCInterface npc)
    {
        return offHandPrimary(npc) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Whether the NPC's only ranged weapon in hand is a Flan gun in its off hand, which it then fires as its weapon. */
    public static boolean offHandPrimary(EntityNPCInterface npc)
    {
        return !ranged(npc.getMainHandItem()) && offHandFlanGun(npc);
    }

    private static boolean offHandFlanGun(EntityNPCInterface npc)
    {
        ItemStack held = npc.getOffhandItem();
        // Whether it is a gun does not depend on its ammunition, so none is looked up.
        return !held.isEmpty() && FlansEquipment.getWeaponProperties(held, ItemStack.EMPTY).filter(properties -> properties.ranged() && !properties.throwable() && !properties.shield()).isPresent();
    }

    /**
     * The NPC's own ranged values in place of the gun's, for the statistics the gun in this hand does not inherit. Each hand
     * chooses for itself; both fire with the same Custom NPCs ranged values when they keep their own.
     */
    public static WeaponOverrides rangedOverrides(EntityNPCInterface npc, InteractionHand hand)
    {
        DataRanged ranged = npc.stats.ranged;
        return new WeaponOverrides(own(npc, EquipmentProperty.RANGED_DAMAGE.inHand(hand), ranged.getStrength()), own(npc, EquipmentProperty.RANGED_ACCURACY.inHand(hand), spread(ranged.getAccuracy())),
            own(npc, EquipmentProperty.RANGED_SPEED.inHand(hand), speed(ranged.getSpeed())),
            inherits(npc, EquipmentProperty.RANGED_SHOT_COUNT.inHand(hand)) ? OptionalInt.empty() : OptionalInt.of(Math.max(1, Math.min(10, ranged.getShotCount()))));
    }

    private static OptionalDouble own(EntityNPCInterface npc, EquipmentProperty property, double value)
    {
        return inherits(npc, property) ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /** Flan spread units of a Custom NPCs accuracy from 0 to 100. */
    public static double spread(int accuracy)
    {
        return (100D - Math.max(0, Math.min(100, accuracy))) * 0.2D;
    }

    /** Blocks per tick of a Custom NPCs projectile speed, which counts tenths. */
    public static double speed(int speed)
    {
        return Math.max(0.01D, speed / 10D);
    }

    public static double meleeDamage(ItemStack stack)
    {
        return EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1D);
    }

    public static int meleeDelay(ItemStack stack)
    {
        int recovery = EquipmentAttributes.attackDelay(EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, 4D));
        // Custom melee animations must finish, but cannot bypass the equipped weapon's attack recovery.
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(properties -> Math.max(recovery, properties.meleeTime())).orElse(recovery);
    }

    public static int chargeTime(ItemStack stack, LivingEntity holder)
    {
        // Grenades get a visible half-second wind-up; native throwable guns retain their authored charge time.
        if (FlansProjectiles.isGrenade(stack))
            return 10;
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(properties -> properties.throwable() ? Math.max(1, properties.chargeTime()) : 0)
            .orElseGet(() -> vanillaChargeTime(stack, holder));
    }

    private static int vanillaChargeTime(ItemStack stack, LivingEntity holder)
    {
        if (stack.getItem() instanceof CrossbowItem)
            return CrossbowItem.isCharged(stack) ? 0 : EntityPlatform.crossbowChargeDuration(stack, holder);
        if (stack.getItem() instanceof BowItem)
            return 20;
        return stack.getItem() instanceof TridentItem ? 10 : 0;
    }

    public static double shotDelay(ItemStack stack)
    {
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(EquippedWeaponProperties::shotDelay).orElse(20D);
    }

    public static boolean shield(ItemStack stack)
    {
        return !stack.isEmpty() && NpcPlatform.canShieldBlock(stack);
    }

    public static int fireSeconds(ItemStack stack, LivingEntity holder)
    {
        return NpcPlatform.fireAspectLevel(holder, stack) * 4;
    }

    /** Whether worn Flan armor grants the protection behind this statistic and the NPC inherits it rather than keeping its own. */
    public static boolean armorProtects(EntityNPCInterface npc, EquipmentProperty property, java.util.function.Predicate<com.flansmodultimate.api.EquippedArmorProperties> protection)
    {
        return inherits(npc, property) && worn(npc, protection);
    }

    /** Whether worn Flan armor has an effect the NPC has no statistic of its own for, while Use equipped armor is on. */
    public static boolean armorProtects(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.EquippedArmorProperties> protection)
    {
        return enabled(npc, Feature.ITEM_ARMOR) && worn(npc, protection);
    }

    /** Whether a worn Flan armor piece has this property. */
    public static boolean worn(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.EquippedArmorProperties> protection)
    {
        for (ItemStack stack : npc.getArmorSlots())
            if (FlansEquipment.getArmorProperties(stack).filter(protection).isPresent())
                return true;
        return false;
    }

    /** The armor statistic that decides how a hit of this kind is reduced, or null for damage armor always handles. */
    @Nullable
    public static EquipmentProperty armorProperty(NpcDamageCategory category)
    {
        return switch (category)
        {
            case PROJECTILE -> EquipmentProperty.ARMOR_PROJECTILE;
            case MELEE -> EquipmentProperty.ARMOR_MELEE;
            case EXPLOSION -> EquipmentProperty.ARMOR_EXPLOSION;
            case OTHER -> null;
        };
    }

    /**
     * Whether worn armor, rather than the NPC's resistance, reduces a hit of this kind. Damage of no resisted kind is
     * reduced by armor whenever Use equipped armor is on, as the NPC has no resistance against it.
     */
    public static boolean armorResists(EntityNPCInterface npc, NpcDamageCategory category)
    {
        EquipmentProperty property = armorProperty(category);
        return property == null ? enabled(npc, Feature.ITEM_ARMOR) : inherits(npc, property);
    }
}
