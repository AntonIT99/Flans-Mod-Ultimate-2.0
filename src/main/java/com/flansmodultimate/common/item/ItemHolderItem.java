package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.tooltip.TooltipKeys;
import com.flansmodultimate.common.types.ItemHolderType;
import com.flansmodultimate.platform.item.FlanBlockItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.util.List;

public class ItemHolderItem extends FlanBlockItem implements IFlanItem<ItemHolderType>
{
    @Getter
    protected final ItemHolderType configType;
    protected final String shortname;

    public ItemHolderItem(ItemHolderType configType)
    {
        super(FlansMod.getBlocks().get(configType.getType()).get(configType.getShortName()).get(), new Properties());
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @NotNull HolderLookup.Provider registries, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.translatable(TooltipKeys.ITEM_HOLDER).withStyle(ChatFormatting.DARK_AQUA));
    }
}
