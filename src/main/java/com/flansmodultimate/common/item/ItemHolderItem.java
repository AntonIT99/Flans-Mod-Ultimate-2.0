package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.ItemHolderType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ItemHolderItem extends BlockItem implements IFlanItem<ItemHolderType>
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.literal("Place items on display").withStyle(ChatFormatting.DARK_AQUA));
    }
}
