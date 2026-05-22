package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.hooks.ClientHooks;
import lombok.Getter;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

public class PartItem extends Item implements ICustomRendereredItem<PartType>
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
    @SuppressWarnings("removal")
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
    {
        ClientHooks.RENDER.initCustomBewlr(consumer);
    }

    @Override
    public boolean useCustomRendererInHand()
    {
        return false;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return false;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return false;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);

        if (configType.getCategory() == PartType.Category.FUEL)
            tooltipComponents.add(IFlanItem.statLine("Fuel Stored", (configType.getFuel() - stack.getDamageValue()) + " / " + configType.getFuel()));
    }
}
