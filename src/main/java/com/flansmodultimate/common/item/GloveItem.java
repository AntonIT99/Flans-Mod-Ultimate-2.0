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

    public GloveItem(GloveType configType, Properties properties)
    {
        super(properties(configType, properties));
        this.configType = configType;
        shortname = configType.getShortName();
    }

    private static Properties properties(GloveType configType, Properties properties)
    {
        properties.stacksTo(1);
        if (configType.hasDurability())
            properties.durability(configType.getDurability());
        if (configType.getEnchantability() > 0)
            properties.enchantable(configType.getEnchantability());
        return properties;
    }

    public int getEnchantmentValue(ItemStack stack)
    {
        return configType.getEnchantability();
    }

    public boolean isEnchantable(@NotNull ItemStack stack)
    {
        return configType.getEnchantability() > 0 && stack.getCount() == 1;
    }

    public boolean isDamageable(@NotNull ItemStack stack)
    {
        return configType.hasDurability();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltipBuilder, @NotNull TooltipFlag isAdvanced)
    {
        List<Component> tooltipComponents = IFlanItem.tooltipList(tooltipBuilder);
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.literal("Improves gun, sword or axe handling when enchanted and held in off hand").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.literal("Works with two-handed guns").withStyle(ChatFormatting.DARK_AQUA));
    }
}
