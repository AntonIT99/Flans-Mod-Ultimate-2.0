package com.flansmodultimate.common.item;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
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
        appendContentPackNameAndItemDescription(tooltipComponents);

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

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel)
            {
                return (HumanoidModel<?>) ModelCache.getOrLoadTypeModel(configType);
            }
        });
    }

    @Override
    @NotNull
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot pEquipmentSlot) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getDefaultAttributeModifiers(pEquipmentSlot);

        if (pEquipmentSlot == configType.getArmorItemType().getSlot())
        {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(modifiers);
            builder.put(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(uuid[configType.getArmorItemType().getSlot().getIndex()], "Movement Speed", configType.getMoveSpeedModifier() - 1F, AttributeModifier.Operation.MULTIPLY_TOTAL));
            builder.put(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(uuid[configType.getArmorItemType().getSlot().getIndex()], "Knockback Resistance", configType.getKnockbackModifier(), AttributeModifier.Operation.ADDITION));
            return builder.build();
        }

        return modifiers;
    }
}