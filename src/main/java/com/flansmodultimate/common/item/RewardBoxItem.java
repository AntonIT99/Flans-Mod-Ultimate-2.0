package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.RewardBox;
import com.flansmodultimate.platform.item.FlanItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class RewardBoxItem extends FlanItem implements IFlanItem<RewardBox>
{
    @Getter
    private final RewardBox configType;

    public RewardBoxItem(RewardBox configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @NotNull HolderLookup.Provider registries, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        appendContentPackNameAndItemDescription(stack, tooltip);
    }
}
