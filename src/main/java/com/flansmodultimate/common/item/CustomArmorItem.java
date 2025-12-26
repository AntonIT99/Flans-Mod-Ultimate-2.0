package com.flansmodultimate.common.item;

import com.flansmodultimate.client.model.DefaultArmor;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CustomArmorItem extends ArmorItem implements IFlanItem<ArmorType>
{
    protected static final UUID[] uuid = new UUID[] { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };

    @Getter
    protected final ArmorType configType;
    protected final String shortname;

    public CustomArmorItem(ArmorType configType)
    {
        super(new CustomArmorMaterial(configType), configType.getArmorItemType(), new Properties());
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    public int getEnchantmentValue()
    {
        if (!configType.isReadEnchantability())
            return ModCommonConfigs.defaultArmorEnchantability.get();
        return material.getEnchantmentValue();
    }

    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex)
    {
        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);

        if (!level.isClientSide && isArmorSlot(slotIndex, player.getInventory()))
        {
            if (configType.isNightVision() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 250, 0, true, false));
            if (configType.isInvisible() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 250, 0, true, false));
            if (configType.getJumpModifier() > 1.01F && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 250, (int) ((configType.getJumpModifier() - 1F) * 2F), true, false));
            if (configType.isFireResistance() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 250, 0, true, false));
            if (configType.isWaterBreathing() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 250, 0, true, false));
            if (configType.isHunger() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 250, 0, true, false));
            if (configType.isRegeneration() && CommonEventHandler.getTicker() % 25 == 0)
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 250, 0, true, false));
            if (!configType.getEffects().isEmpty() && CommonEventHandler.getTicker() % 25 == 0)
            {
                configType.getEffects().forEach(effect -> player.addEffect(new MobEffectInstance(effect)));
            }
            if (configType.isNegateFallDamage())
                player.fallDistance = 0F;
            if (configType.isOnWaterWalking())
            {
                if (player.isInWater())
                {
                    player.getAbilities().mayfly = true;   // Allow flying
                }
                else
                {
                    player.getAbilities().flying = false;  // Disable flying
                }
                player.onUpdateAbilities();
            }
        }
    }

    protected boolean isArmorSlot(int slotIndex, Inventory inv)
    {
        return (slotIndex >= inv.items.size()) && (slotIndex - inv.items.size() < inv.armor.size());
    }

    @Override
    public boolean isDamageable(ItemStack stack)
    {
        //0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config
        int breakType = ModCommonConfigs.breakableArmor.get();
        return (breakType == 2 && configType.hasDurability()) || breakType == 1;
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        if (ModCommonConfigs.breakableArmor.get() == 1)
        {
            return ModCommonConfigs.defaultArmorDurability.get();
        }
        return super.getMaxDamage(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (!Screen.hasShiftDown())
        {
            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            String defense = BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()) ?
                    IFlanItem.formatDouble(configType.getDefence() * 100.0) + "%" : String.valueOf(Math.round(configType.getDefence() * ArmorType.ARMOR_POINT_FACTOR));
            String bulletDefense = BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()) ?
                    IFlanItem.formatDouble(configType.getBulletDefence() * 100.0) + "%" : String.valueOf(Math.round(configType.getBulletDefence() * ArmorType.ARMOR_POINT_FACTOR));

            tooltipComponents.add(IFlanItem.statLine("Defense", defense));
            if (configType.getBulletDefence() != configType.getDefence())
                tooltipComponents.add(IFlanItem.statLine("Bullet Defense", String.valueOf(bulletDefense)));

            if (configType.getDurability() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Durability", IFlanItem.formatDouble(configType.getDurability())));
            if (configType.getEnchantability() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Enchantability", IFlanItem.formatDouble(getEnchantmentValue())));

            if (Math.abs(configType.getJumpModifier() - 1F) > 0.01F)
                tooltipComponents.add(Component.literal("+" + IFlanItem.formatFloat((configType.getJumpModifier() - 1F) * 100F) + "% Jump Height").withStyle(ChatFormatting.AQUA));

            if (configType.isSmokeProtection())
                tooltipComponents.add(Component.literal("+Smoke Protection").withStyle(ChatFormatting.DARK_GREEN));
            if (configType.isNightVision())
                tooltipComponents.add(Component.literal("+Night Vision").withStyle(ChatFormatting.DARK_GREEN));
            if (configType.isInvisible())
                tooltipComponents.add(Component.literal("+Invisibility").withStyle(ChatFormatting.DARK_GREEN));
            if (configType.isNegateFallDamage())
                tooltipComponents.add(Component.literal("+Negates Fall Damage").withStyle(ChatFormatting.DARK_GREEN));
            if (configType.isFireResistance())
                tooltipComponents.add(Component.literal("+Fire Resistance").withStyle(ChatFormatting.DARK_GREEN));
            if (configType.isWaterBreathing())
                tooltipComponents.add(Component.literal("+Water Breathing").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel)
            {
                if (ModelCache.getOrLoadTypeModel(configType) instanceof HumanoidModel<?> humanoidModel)
                    return humanoidModel;
                else
                    return new DefaultArmor(configType.getArmorItemType());
            }
        });
    }

    @Override
    @NotNull
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> vanilla = super.getDefaultAttributeModifiers(slot);

        if (slot == configType.getArmorItemType().getSlot())
        {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            if (BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()))
            {
                // Copy everything EXCEPT vanilla armor/toughness (disables vanilla mitigation for this piece)
                for (var entry : vanilla.entries())
                {
                    Attribute attr = entry.getKey();
                    if (attr == Attributes.ARMOR || attr == Attributes.ARMOR_TOUGHNESS || attr == null || entry.getValue() == null)
                        continue;
                    builder.put(attr, entry.getValue());
                }
            }
            else
            {
                builder.putAll(vanilla);
            }

            builder.put(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(uuid[configType.getArmorItemType().getSlot().getIndex()], "Movement Speed", configType.getMoveSpeedModifier() - 1F, AttributeModifier.Operation.MULTIPLY_TOTAL));
            builder.put(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(uuid[configType.getArmorItemType().getSlot().getIndex()], "Knockback Resistance", configType.getKnockbackModifier(), AttributeModifier.Operation.ADDITION));
            return builder.build();
        }

        return vanilla;
    }
}