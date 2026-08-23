package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.MechaItemType;
import lombok.Getter;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public final class MechaAddonItem extends Item implements ICustomRendereredItem<MechaItemType>
{
    @Getter
    private final MechaItemType configType;

    public MechaAddonItem(MechaItemType configType, Properties properties)
    {
        super(properties.stacksTo(1));
        this.configType = configType;
    }

    @Override
    public boolean useCustomRendererInHand()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltipBuilder, @NotNull TooltipFlag advanced)
    {
        List<Component> tooltip = IFlanItem.tooltipList(tooltipBuilder);
        appendContentPackNameAndItemDescription(stack, tooltip);
    }
}
