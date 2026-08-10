package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.RewardBox;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class RewardBoxItem extends Item implements IFlanItem<RewardBox>
{
    @Getter
    private final RewardBox configType;

    public RewardBoxItem(RewardBox configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        appendContentPackNameAndItemDescription(stack, tooltip);
    }
}
