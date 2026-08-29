package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.GloveType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GloveItem extends Item implements IFlanItem<GloveType>
{
    @Getter
    protected final GloveType configType;
    protected final String shortname;

    public GloveItem(GloveType configType)
    {
        super(properties(configType));
        this.configType = configType;
        shortname = configType.getShortName();
    }

    private static Properties properties(GloveType configType)
    {
        Properties properties = new Properties().stacksTo(1);
        if (configType.hasDurability())
            properties.durability(configType.getDurability());
        return properties;
    }

    @Override
    public int getEnchantmentValue()
    {
        return configType.getEnchantability();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack)
    {
        return configType.getEnchantability() > 0 && stack.getCount() == 1;
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack)
    {
        return configType.hasDurability();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.glove.handling").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.glove.two_handed").withStyle(ChatFormatting.DARK_AQUA));
    }
}
