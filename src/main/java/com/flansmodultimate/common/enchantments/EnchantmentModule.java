package com.flansmodultimate.common.enchantments;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.item.GloveItem;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.damage.MutableDamageContext;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class EnchantmentModule
{
    static final ResourceKey<Enchantment> steadyEnchant = key("steady");
    static final ResourceKey<Enchantment> nimbleEnchant = key("nimble");
    static final ResourceKey<Enchantment> lumberjackEnchant = key("lumberjack");
    static final ResourceKey<Enchantment> duelistEnchant = key("duelist");
    static final ResourceKey<Enchantment> sharpshooterEnchant = key("sharpshooter");
    static final ResourceKey<Enchantment> juggernautEnchant = key("juggernaut");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private static ResourceKey<Enchantment> key(String name)
    {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, name));
    }

    public static void modifyGun(@NotNull FireableGun fireableGun, @Nullable LivingEntity entity, @Nullable ItemStack otherHand)
    {
        if (!isEnabled() || otherHand == null || !isOffHandModifierStack(otherHand))
            return;

        int steadyLevel = getLevel(steadyEnchant, otherHand);
        if (steadyLevel > 0)
            fireableGun.multiplySpread((float) Math.pow(0.75F, steadyLevel));

        int sharpshooterLevel = getLevel(sharpshooterEnchant, otherHand);
        if (sharpshooterLevel > 0)
            fireableGun.multiplyDamage((float) Math.pow(1.10F, sharpshooterLevel));

        if (steadyLevel > 0 || sharpshooterLevel > 0)
            damageEquipment(otherHand, entity, EquipmentSlot.OFFHAND, 1);
    }

    public static float modifyReloadTime(float reloadTime, @Nullable LivingEntity entity, @Nullable ItemStack otherHand)
    {
        float modifiedReloadTime = getModifiedReloadTime(reloadTime, otherHand);
        if (modifiedReloadTime < reloadTime)
            damageReloadModifier(entity, otherHand);
        return modifiedReloadTime;
    }

    public static float getModifiedReloadTime(float reloadTime, @Nullable ItemStack otherHand)
    {
        if (!isEnabled() || otherHand == null || !isGloveStack(otherHand))
            return reloadTime;

        int nimbleLevel = getLevel(nimbleEnchant, otherHand);
        if (nimbleLevel <= 0)
            return reloadTime;

        return reloadTime * (float) Math.pow(0.85F, nimbleLevel);
    }

    public static void damageReloadModifier(@Nullable LivingEntity entity, @Nullable ItemStack otherHand)
    {
        if (!isEnabled() || otherHand == null || !isGloveStack(otherHand))
            return;
        if (getLevel(nimbleEnchant, otherHand) > 0)
            damageEquipment(otherHand, entity, EquipmentSlot.OFFHAND, 1);
    }

    public static void applyOffHandWeaponDamage(MutableDamageContext event)
    {
        if (!isEnabled() || event.entity().level().isClientSide)
            return;

        Entity sourceEntity = event.source().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker))
            return;

        Entity directEntity = event.source().getDirectEntity();
        if (directEntity != null && directEntity != attacker)
            return;

        ItemStack weaponStack = attacker.getMainHandItem();
        ItemStack offHandStack = attacker.getOffhandItem();
        if (!isOffHandModifierStack(offHandStack))
            return;

        int level = 0;

        if (weaponStack.getItem() instanceof AxeItem)
            level = getLevel(lumberjackEnchant, offHandStack);
        else if (weaponStack.getItem() instanceof SwordItem)
            level = getLevel(duelistEnchant, offHandStack);

        if (level <= 0)
            return;

        event.setAmount(event.amount() * (float) Math.pow(1.10F, level));
        damageEquipment(offHandStack, attacker, EquipmentSlot.OFFHAND, 1);
    }

    public static void applyJuggernaut(MutableDamageContext event)
    {
        if (!isEnabled() || event.entity().level().isClientSide)
            return;

        LivingEntity entity = event.entity();
        int juggernautLevel = 0;

        for (EquipmentSlot slot : ARMOR_SLOTS)
            juggernautLevel += getLevel(juggernautEnchant, entity.getItemBySlot(slot));

        if (juggernautLevel <= 0)
            return;

        final float minPercent = 0.25F;
        final float exponent = (float) Math.log(minPercent) / 4F;
        float maxDamagePercent = (float) Math.exp(exponent * juggernautLevel);
        float maxHealthWithArmor = entity.getMaxHealth() + entity.getArmorValue();
        float threshold = maxHealthWithArmor * maxDamagePercent;

        if (event.amount() <= threshold)
            return;

        float absorbedDamage = Math.min(event.amount() - threshold, 256.0F);
        int armorDamage = Mth.floor(absorbedDamage);

        if (armorDamage > 0)
        {
            for (EquipmentSlot slot : ARMOR_SLOTS)
            {
                ItemStack armor = entity.getItemBySlot(slot);
                if (getLevel(juggernautEnchant, armor) > 0)
                    damageEquipment(armor, entity, slot, armorDamage);
            }
        }

        FlansMod.log.debug("Juggernaut capped incoming damage {} to {}", event.amount(), threshold);
        event.setAmount(threshold);
    }

    private static int getLevel(ResourceKey<Enchantment> enchantment, ItemStack stack)
    {
        if (stack.isEmpty())
            return 0;
        return stack.getTagEnchantments().entrySet().stream()
            .filter(entry -> entry.getKey().is(enchantment))
            .mapToInt(entry -> entry.getIntValue())
            .findFirst()
            .orElse(0);
    }

    private static void damageEquipment(ItemStack stack, @Nullable LivingEntity entity, EquipmentSlot slot, int amount)
    {
        if (amount <= 0 || entity == null || entity.level().isClientSide || stack.isEmpty() || !stack.isDamageableItem())
            return;

        stack.hurtAndBreak(amount, entity, slot);
    }

    private static boolean isEnabled()
    {
        CommonConfigSnapshot config = ModCommonConfig.get();
        return config == null || config.enchantmentModuleEnabled();
    }

    private static boolean isOffHandModifierStack(ItemStack stack)
    {
        return !stack.isEmpty() && (stack.getItem() instanceof GloveItem || stack.getItem() instanceof ShieldItem);
    }

    private static boolean isGloveStack(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof GloveItem;
    }
}
