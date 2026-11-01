package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.equipment.*;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcServerRules;
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
    static final int PRIMARY_ARMED = 1;
    static final int SECONDARY_ARMED = 2;
    /** Which weapon groups of each model have weapons, as {@link #PRIMARY_ARMED} and {@link #SECONDARY_ARMED} bits; a loaded definition does not change. */
    private static final Map<IContentType, Integer> ARMED_GROUPS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * @param model
     *            the AA gun or driveable whose model the NPC wears, or null
     * @return whether the model's secondary weapon group drives the NPC's main attack: as its switch says, unless the group
     *         it chooses has no weapon and the other has, as on a bomber whose only group is its bombs
     */
    public static boolean secondaryBank(NpcWeaponOptions options, @Nullable IContentType model)
    {
        boolean chosen = options.enabled(Feature.SECONDARY_BANK);
        return model == null ? chosen : secondaryBank(chosen, ARMED_GROUPS.computeIfAbsent(model, NpcEquipment::armedGroups));
    }

    /**
     * @param chosen
     *            whether the switch chooses the secondary group
     * @param armed
     *            which groups have weapons, as {@link #PRIMARY_ARMED} and {@link #SECONDARY_ARMED} bits
     */
    static boolean secondaryBank(boolean chosen, int armed)
    {
        boolean chosenArmed = (armed & (chosen ? SECONDARY_ARMED : PRIMARY_ARMED)) != 0;
        boolean otherArmed = (armed & (chosen ? PRIMARY_ARMED : SECONDARY_ARMED)) != 0;
        // Only an empty chosen group gives way, and only to one that has a weapon.
        if (!chosenArmed && otherArmed)
            return !chosen;
        return chosen;
    }

    private static int armedGroups(IContentType model)
    {
        int primary = FlansProjectiles.getMuzzles(model, false, 0, false).isEmpty() ? 0 : PRIMARY_ARMED;
        return primary | (FlansProjectiles.getMuzzles(model, true, 0, false).isEmpty() ? 0 : SECONDARY_ARMED);
    }

    /** @return whether the NPC's switch is on; on the server, whether it uses up ammunition also follows the server's rule */
    public static boolean enabled(EntityNPCInterface npc, Feature feature)
    {
        boolean own = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
        return feature == Feature.CONSUME_AMMO ? NpcServerRules.consumesAmmo(npc, own) : own;
    }

    public static boolean weaponAuthority(EntityNPCInterface npc)
    {
        return enabled(npc, Feature.ITEM_WEAPONS) && (!rangedWeapon(npc).isEmpty() || enabled(npc, Feature.MELEE_DUAL_WIELD) && meleeWeapon(npc.getOffhandItem()));
    }

    /** Whether the selected melee hand holds an item while Use equipped weapons is enabled. */
    public static boolean meleeAuthority(EntityNPCInterface npc)
    {
        if (NpcMechaTools.isMecha(npc))
            return false;
        if (!enabled(npc, Feature.ITEM_WEAPONS))
            return false;
        ItemStack weapon = meleeWeapon(npc);
        return !weapon.isEmpty() && (!enabled(npc, Feature.MELEE_DUAL_WIELD) || meleeWeapon(weapon));
    }

    /** Whether a melee hit goes through the item; dual melee retains this path even with every hit statistic set to Custom. */
    public static boolean itemMelee(EntityNPCInterface npc)
    {
        return meleeAuthority(npc) && (enabled(npc, Feature.MELEE_DUAL_WIELD) || inherits(npc, EquipmentProperty.MELEE_DAMAGE) || inherits(npc, EquipmentProperty.MELEE_KNOCKBACK)
            || inherits(npc, EquipmentProperty.MELEE_EFFECT));
    }

    /** Whether the selected weapon decides this melee statistic; Custom values are shared by both hands. */
    public static boolean itemMelee(EntityNPCInterface npc, EquipmentProperty property)
    {
        return meleeAuthority(npc) && inherits(npc, property);
    }

    /** The next striking hand. Dual melee alternates weapons, skipping empty hands, shields and non-weapons. */
    public static InteractionHand meleeHand(EntityNPCInterface npc)
    {
        if (!enabled(npc, Feature.ITEM_WEAPONS) || !enabled(npc, Feature.MELEE_DUAL_WIELD) || !meleeWeapon(npc.getOffhandItem()))
            return InteractionHand.MAIN_HAND;
        if (!meleeWeapon(npc.getMainHandItem()))
            return InteractionHand.OFF_HAND;
        return ((NpcMeleeState.Holder) npc).wolffsmodnpcsMeleeState().getNextHand();
    }

    /** The weapon the AI's next reach, cooldown and damage attempt refer to. */
    public static ItemStack meleeWeapon(EntityNPCInterface npc)
    {
        return meleeHand(npc) == InteractionHand.OFF_HAND ? npc.getOffhandItem() : npc.getMainHandItem();
    }

    /** A damaging melee item, including tools and Flan gun melee; shields and decorative/ordinary throwable items are excluded. */
    public static boolean meleeWeapon(ItemStack item)
    {
        if (item.isEmpty() || shield(item))
            return false;
        Optional<WeaponKind> kind = FlansEquipment.getWeaponKind(item);
        if (kind.filter(WeaponKind::shield).isPresent())
            return false;
        // Only a Flan gun is worth working out all of its statistics for its melee damage.
        return (kind.isPresent() && FlansEquipment.getWeaponProperties(item, ItemStack.EMPTY).filter(properties -> properties.meleeDamage() > 0D).isPresent()) || meleeDamage(item) > 1D;
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
        return weaponAuthority(npc) ? ranged(npc) : !NpcAmmunition.modelTemplate(npc).isEmpty();
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
        if (weaponAuthority(npc))
            return null;
        // A throwable only thrown now and then leaves the NPC to fight in melee meanwhile.
        ItemStack projectile = NpcAmmunition.modelTemplate(npc);
        if (projectile.isEmpty())
            return null;
        IItemStack slot = npc.inventory.getProjectile();
        return slot != null && slot.getMCItemStack() == projectile ? slot : NpcAPI.Instance().getIItemStack(projectile);
    }

    /**
     * A ranged weapon in the main hand takes precedence, then a Flan gun in the off hand, which the NPC then fires on its
     * own; otherwise a grenade or throwable Flan gun it throws as its main weapon.
     */
    public static ItemStack rangedWeapon(EntityNPCInterface npc)
    {
        ItemStack held = npc.getMainHandItem();
        if (!ranged(held) && offHandFlanGun(npc))
            return npc.getOffhandItem();
        return mainRangedWeapon(npc);
    }

    /**
     * The ranged weapon of the main hand: the held one, or else a grenade or throwable Flan gun the NPC throws as its main
     * weapon, from the projectile slot or its inventory as the strategy for its kind says, which is drawn into the main hand
     * to be thrown. An off-hand gun is left out.
     */
    public static ItemStack mainRangedWeapon(EntityNPCInterface npc)
    {
        ItemStack held = npc.getMainHandItem();
        if (ranged(held))
            return held;
        ItemStack thrown = NpcThrowing.mainThrownWeapon(npc);
        return thrown.isEmpty() ? held : thrown;
    }

    public static boolean ranged(ItemStack stack)
    {
        if (FlansProjectiles.isGrenade(stack))
            return true;
        if (FlansEquipment.getWeaponKind(stack).map(WeaponKind::ranged).orElse(false))
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
        // Whether it is a gun depends on its definition alone.
        return !held.isEmpty() && FlansEquipment.getWeaponKind(held).filter(kind -> kind.ranged() && !kind.throwable() && !kind.shield()).isPresent();
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
        Optional<WeaponKind> kind = FlansEquipment.getWeaponKind(stack);
        if (kind.isEmpty())
            return vanillaChargeTime(stack, holder);
        if (!kind.get().throwable())
            return 0;
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(properties -> Math.max(1, properties.chargeTime())).orElse(1);
    }

    private static int vanillaChargeTime(ItemStack stack, LivingEntity holder)
    {
        if (stack.getItem() instanceof CrossbowItem)
            return CrossbowItem.isCharged(stack) ? 0 : NpcPlatform.crossbowChargeDuration(stack, holder);
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
    public static boolean armorProtects(EntityNPCInterface npc, EquipmentProperty property, java.util.function.Predicate<com.flansmodultimate.api.equipment.EquippedArmorProperties> protection)
    {
        return inherits(npc, property) && worn(npc, protection);
    }

    /** Whether worn Flan armor has an effect the NPC has no statistic of its own for, while Use equipped armor is on. */
    public static boolean armorProtects(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.equipment.EquippedArmorProperties> protection)
    {
        return enabled(npc, Feature.ITEM_ARMOR) && worn(npc, protection);
    }

    /** Whether a worn Flan armor piece has this property. */
    public static boolean worn(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.equipment.EquippedArmorProperties> protection)
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
