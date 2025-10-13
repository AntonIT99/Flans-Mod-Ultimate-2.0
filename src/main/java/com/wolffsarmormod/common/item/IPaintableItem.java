package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.PaintableType;

public interface IPaintableItem<T extends InfoType> extends IFlanItem<T>
{
    PaintableType GetPaintableType();
}

