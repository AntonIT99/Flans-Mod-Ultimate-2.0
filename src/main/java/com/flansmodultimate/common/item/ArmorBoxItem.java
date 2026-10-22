package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.platform.item.FlanBlockItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorBoxItem extends FlanBlockItem implements IFlanItem<ArmorBoxType>
{
    @Getter
    protected final ArmorBoxType configType;
    protected final String shortname;

    public ArmorBoxItem(ArmorBoxType configType)
    {
        super(FlansMod.getBlocks().get(configType.getType()).get(configType.getShortName()).get(), new Properties());
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @NotNull HolderLookup.Provider registries, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
    }
}
