package com.flansmodultimate.platform.item;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block item counterpart of {@link FlanItem}: routes the version-specific tooltip hook to a shared one. */
public abstract class FlanBlockItem extends BlockItem
{
    protected FlanBlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public final void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        HolderLookup.Provider registries = context.registries();
        appendTooltip(stack, registries == null ? ItemStackData.builtInRegistries() : registries, tooltip, flag);
    }

    /** Adds the item's tooltip lines; {@code registries} reads the stack's nested items. */
    // Items without tooltip lines add none; the block's own lines are not shown.
    @SuppressWarnings("unused")
    protected void appendTooltip(ItemStack stack, HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag)
    {}
}
