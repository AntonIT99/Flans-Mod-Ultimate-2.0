package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.paintjob.Paintjob;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.PaintableType;

import net.minecraft.world.item.ItemStack;

public interface IPaintableItem<T extends InfoType> extends IFlanItem<T>
{
    String NBT_PAINTJOB_ID = "PaintjobId";

    PaintableType getPaintableType();

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

    /** Write the given paintjob onto the stack. */
    default void applyPaintjobToStack(ItemStack stack, Paintjob paintjob)
    {
        var tag = stack.getOrCreateTag();
        tag.putInt(NBT_PAINTJOB_ID, paintjob.getId());
    }
}

