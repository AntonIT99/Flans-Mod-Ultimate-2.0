package com.flansmodultimate.platform.item;

import com.flansmodultimate.common.item.IFlanItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
    public @NotNull Component getName(@NotNull ItemStack stack)
    {
        return this instanceof IFlanItem<?> content ? Component.translatableWithFallback(getDescriptionId(stack), content.getConfigType().getName()) : super.getName(stack);
    }

    @Override
    public final void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        appendTooltip(stack, ItemStackData.builtInRegistries(), tooltip, flag);
    }

    /** Adds the item's tooltip lines; {@code registries} reads the stack's nested items. */
    // Items without tooltip lines add none; the block's own lines are not shown.
    @SuppressWarnings("unused")
    protected void appendTooltip(ItemStack stack, HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag)
    {}
}
