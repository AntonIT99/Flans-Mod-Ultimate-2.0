package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.PartType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PartItem extends Item implements IFlanItem<PartType>
{
    @Getter
    protected final PartType configType;
    protected final String shortname;

    public PartItem(PartType configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);

        if (configType.getCategory() == PartType.Category.FUEL)
            tooltipComponents.add(IFlanItem.statLine("Fuel Stored", (configType.getFuel() - stack.getDamageValue()) + " / " + configType.getFuel()));
    }
}
