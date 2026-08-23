package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.GunBoxType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunBoxItem extends BlockItem implements IFlanItem<GunBoxType>
{
    @Getter
    protected final GunBoxType configType;
    protected final String shortname;

    public GunBoxItem(GunBoxType configType, Properties properties)
    {
        super(FlansMod.getBlocks().get(configType.getType()).get(configType.getShortName()).get(), properties);
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltipBuilder, @NotNull TooltipFlag isAdvanced)
    {
        List<Component> tooltipComponents = IFlanItem.tooltipList(tooltipBuilder);
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
    }
}
