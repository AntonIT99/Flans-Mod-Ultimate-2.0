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
        tooltipComponents.add(Component.literal("Improves gun, sword or axe handling when enchanted and held in off hand").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.literal("Works with two-handed guns").withStyle(ChatFormatting.DARK_AQUA));
    }
}
