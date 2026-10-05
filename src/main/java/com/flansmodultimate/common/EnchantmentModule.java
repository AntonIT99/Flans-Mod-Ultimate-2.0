package com.flansmodultimate.common;

import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.item.GloveItem;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.damage.MutableDamageContext;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.FlansLog;
import lombok.NoArgsConstructor;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

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
        steadyEnchant = register(registry, "steady", EnchantmentSteady::new);
        nimbleEnchant = register(registry, "nimble", EnchantmentNimble::new);
        lumberjackEnchant = register(registry, "lumberjack", EnchantmentLumberjack::new);
        duelistEnchant = register(registry, "duelist", EnchantmentDuelist::new);
        sharpshooterEnchant = register(registry, "sharpshooter", EnchantmentSharpshooter::new);
        juggernautEnchant = register(registry, "juggernaut", EnchantmentJuggernaut::new);
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

        FlansLog.log.debug("Juggernaut capped incoming damage {} to {}", event.amount(), threshold);
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

        ItemStackData.hurtAndBreak(stack, amount, entity, slot);
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
    private static class EnchantmentDuelist extends OffHandDamageEnchantment
    {
    }

    private static class EnchantmentJuggernaut extends Enchantment
    {
        public EnchantmentJuggernaut()
        {
            super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
            });
        }

        @Override
        public int getMinCost(int level)
        {
            return level * 25;
        }

        @Override
        public int getMaxCost(int level)
        {
            return getMinCost(level) + 50;
        }

        @Override
        public boolean isTreasureOnly()
        {
            return true;
        }
    }

    private static class EnchantmentLumberjack extends OffHandDamageEnchantment
    {
    }

    private static class EnchantmentNimble extends OffHandEnchantment
    {
        public EnchantmentNimble()
        {
            super(Rarity.UNCOMMON, true);
        }

        @Override
        public int getMaxLevel()
        {
            return 3;
        }

        @Override
        public int getMinCost(int level)
        {
            return 5 + (level - 1) * 8;
        }

        @Override
        public int getMaxCost(int level)
        {
            return getMinCost(level) + 8;
        }
    }

    private static class EnchantmentSharpshooter extends OffHandEnchantment
    {
        public EnchantmentSharpshooter()
        {
            super(Rarity.RARE, false);
        }

        @Override
        public int getMaxLevel()
        {
            return 3;
        }

        @Override
        public int getMinCost(int level)
        {
            return 10 + (level - 1) * 10;
        }

        @Override
        public int getMaxCost(int level)
        {
            return getMinCost(level) + 10;
        }

        @Override
        protected boolean checkCompatibility(@NotNull Enchantment other)
        {
            return !(other instanceof EnchantmentSharpshooter) && super.checkCompatibility(other);
        }
    }

    private static class EnchantmentSteady extends OffHandEnchantment
    {
        public EnchantmentSteady()
        {
            super(Rarity.UNCOMMON, false);
        }

        @Override
        public int getMaxLevel()
        {
            return 3;
        }

        @Override
        public int getMinCost(int level)
        {
            return 5 + (level - 1) * 8;
        }

        @Override
        public int getMaxCost(int level)
        {
            return getMinCost(level) + 8;
        }
    }

    private static abstract class OffHandDamageEnchantment extends OffHandEnchantment
    {
        protected OffHandDamageEnchantment()
        {
            super(Rarity.COMMON, false);
        }

        @Override
        public int getMaxLevel()
        {
            return 3;
        }

        @Override
        protected boolean checkCompatibility(@NotNull Enchantment other)
        {
            return !(other instanceof OffHandDamageEnchantment) && super.checkCompatibility(other);
        }
    }

    private static abstract class OffHandEnchantment extends Enchantment
    {
        private final boolean glovesOnly;

        protected OffHandEnchantment(Rarity rarity, boolean glovesOnly)
        {
            super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[] { EquipmentSlot.OFFHAND });
            this.glovesOnly = glovesOnly;
        }

        @Override
        public boolean canEnchant(@NotNull ItemStack stack)
        {
            return isValidOffHandStack(stack);
        }

        @Override
        public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack)
        {
            return isValidOffHandStack(stack) || stack.is(Items.BOOK);
        }

        protected boolean isValidOffHandStack(ItemStack stack)
        {
            if (stack.isEmpty())
                return false;
            if (stack.getItem() instanceof GloveItem)
                return true;
            return !glovesOnly && stack.getItem() instanceof ShieldItem;
        }
    }
}
