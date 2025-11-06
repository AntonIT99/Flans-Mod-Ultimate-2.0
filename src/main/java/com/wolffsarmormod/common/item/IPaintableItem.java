package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.paintjob.Paintjob;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.PaintableType;

import net.minecraft.world.item.ItemStack;

public interface IPaintableItem<T extends InfoType> extends IFlanItem<T>
{
    String NBT_PAINTJOB_ID = "paintjobid";

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

