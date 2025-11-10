package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFactory
{
    @Nullable
    public static Item createItem(InfoType config)
    {
        try
        {
            Class<? extends InfoType> typeClass = config.getType().getTypeClass();
            Class<? extends IFlanItem<?>> itemClass = config.getType().getItemClass();
            return itemClass.getConstructor(typeClass).newInstance(typeClass.cast(config)).asItem();
        }
        catch (Exception e)
        {
            FlansMod.log.error("Failed to instantiate {}", config);
            LogUtils.logWithoutStacktrace(e);
            return null;
        }
    }
}
