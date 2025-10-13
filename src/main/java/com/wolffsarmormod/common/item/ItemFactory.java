package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.InfoType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.Item;

import java.lang.reflect.InvocationTargetException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFactory
{
    public static Item createItem(InfoType config) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Class<? extends InfoType> typeClass = config.getType().getTypeClass();
        Class<? extends IFlanItem<?>> itemClass = config.getType().getItemClass();
        return itemClass.getConstructor(typeClass).newInstance(typeClass.cast(config)).asItem();
    }
}
