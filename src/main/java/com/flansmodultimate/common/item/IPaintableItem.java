package com.flansmodultimate.common.item;

import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IPaintableItem<T extends InfoType> extends IFlanItem<T>
{
    String NBT_PAINTJOB_ID = "paintjobid";

    PaintableType getPaintableType();

    @Override
    default void appendContentPackNameAndItemDescription(@NotNull ItemStack stack, @NotNull List<Component> tooltipComponents)
    {
        IFlanItem.super.appendContentPackNameAndItemDescription(stack, tooltipComponents);

        String paintjobName = getPaintjob(stack).getDisplayName();
        if (StringUtils.isNotBlank(paintjobName))
            tooltipComponents.add(Component.literal(paintjobName).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }

    default ItemStack makePaintjobStack(Paintjob paintjob)
    {
        ItemStack stack = new ItemStack(this);
        applyPaintjobToStack(stack, paintjob);
        return stack;
    }

    default ItemStack makeDefaultPaintjobStack()
    {
        ItemStack stack = new ItemStack(this);
        applyPaintjobToStack(stack, getPaintableType().getDefaultPaintjob());
        return stack;
    }

    default void applyPaintjobToStack(ItemStack stack, Paintjob paintjob)
    {
        stack.getOrCreateTag().putInt(NBT_PAINTJOB_ID, paintjob.getId());
    }

    default int getPaintjobId(ItemStack stack)
    {
        return stack.getOrCreateTag().getInt(NBT_PAINTJOB_ID);
    }

    default Paintjob getPaintjob(ItemStack stack)
    {
        return getPaintableType().getPaintjob(getPaintjobId(stack));
    }
}

