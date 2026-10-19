package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

import java.util.Optional;

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

    public static boolean ranged(EntityNPCInterface npc)
    {
        return weaponAuthority(npc) && ranged(rangedWeapon(npc));
    }

    /** A held ranged weapon takes precedence; otherwise a Flan throwable in the projectile slot can be thrown. */
    public static ItemStack rangedWeapon(EntityNPCInterface npc)
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
        return enabled(npc, Feature.DUAL_WIELD) && weaponAuthority(npc)
            && flan(npc, InteractionHand.OFF_HAND).filter(properties -> properties.ranged() && !properties.throwable() && !properties.shield()).isPresent();
    }

    public static double meleeDamage(ItemStack stack)
    {
        return EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1D);
    }

    public static int meleeDelay(ItemStack stack)
    {
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).filter(properties -> properties.meleeTime() > 1).map(EquippedWeaponProperties::meleeTime)
            .orElseGet(() -> EquipmentAttributes.attackDelay(EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, 4D)));
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

    public static boolean armorProtects(EntityNPCInterface npc, java.util.function.Predicate<com.flansmodultimate.api.EquippedArmorProperties> protection)
    {
        if (!enabled(npc, Feature.ITEM_ARMOR))
            return false;
        for (ItemStack stack : npc.getArmorSlots())
            if (FlansEquipment.getArmorProperties(stack).filter(protection).isPresent())
                return true;
        return false;
    }
}
