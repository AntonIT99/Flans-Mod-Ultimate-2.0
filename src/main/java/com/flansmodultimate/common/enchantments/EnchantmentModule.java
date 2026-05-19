package com.flansmodultimate.common.enchantments;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.item.GloveItem;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;
import java.util.function.Supplier;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class EnchantmentModule
{
    static RegistryObject<Enchantment> steadyEnchant;
    static RegistryObject<Enchantment> nimbleEnchant;
    static RegistryObject<Enchantment> lumberjackEnchant;
    static RegistryObject<Enchantment> duelistEnchant;
    static RegistryObject<Enchantment> sharpshooterEnchant;
    static RegistryObject<Enchantment> juggernautEnchant;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    public static void register(DeferredRegister<Enchantment> registry)
    {
        steadyEnchant = register(registry, "enchantment_steady", EnchantmentSteady::new);
        nimbleEnchant = register(registry, "enchantment_nimble", EnchantmentNimble::new);
        lumberjackEnchant = register(registry, "enchantment_lumberjack", EnchantmentLumberjack::new);
        duelistEnchant = register(registry, "enchantment_duelist", EnchantmentDuelist::new);
        sharpshooterEnchant = register(registry, "enchantment_sharpshooter", EnchantmentSharpshooter::new);
        juggernautEnchant = register(registry, "enchantment_juggernaut", EnchantmentJuggernaut::new);
    }

    private static RegistryObject<Enchantment> register(DeferredRegister<Enchantment> registry, String name, Supplier<Enchantment> supplier)
    {
        return registry.register(name, supplier);
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

    public static void applyOffHandWeaponDamage(LivingHurtEvent event)
    {
        if (!isEnabled() || event.getEntity().level().isClientSide)
            return;

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker))
            return;

        Entity directEntity = event.getSource().getDirectEntity();
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

        event.setAmount(event.getAmount() * (float) Math.pow(1.10F, level));
        damageEquipment(offHandStack, attacker, EquipmentSlot.OFFHAND, 1);
    }

    public static void applyJuggernaut(LivingHurtEvent event)
    {
        if (!isEnabled() || event.getEntity().level().isClientSide)
            return;

        LivingEntity entity = event.getEntity();
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

        if (event.getAmount() <= threshold)
            return;

        float absorbedDamage = Math.min(event.getAmount() - threshold, 256.0F);
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

        FlansMod.log.debug("Juggernaut capped incoming damage {} to {}", event.getAmount(), threshold);
        event.setAmount(threshold);
    }

    private static int getLevel(@Nullable RegistryObject<Enchantment> enchantment, ItemStack stack)
    {
        if (enchantment == null || stack.isEmpty())
            return 0;
        return stack.getEnchantmentLevel(enchantment.get());
    }

    private static void damageEquipment(ItemStack stack, @Nullable LivingEntity entity, EquipmentSlot slot, int amount)
    {
        if (amount <= 0 || entity == null || entity.level().isClientSide || stack.isEmpty() || !stack.isDamageableItem())
            return;

        stack.hurtAndBreak(amount, entity, owner -> Optional.ofNullable(owner).ifPresent(o -> o.broadcastBreakEvent(slot)));
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
