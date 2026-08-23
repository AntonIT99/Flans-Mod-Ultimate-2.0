package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemFactory
{
    @Nullable
    public static Item createItem(InfoType config, Item.Properties properties)
    {
        try
        {
            Class<? extends InfoType> typeClass = config.getType().getTypeClass();
            Class<? extends IFlanItem<?>> itemClass = config.getType().getItemClass();
            return itemClass.getConstructor(typeClass, Item.Properties.class)
                .newInstance(typeClass.cast(config), properties).asItem();
        }
        catch (Exception e)
        {
            FlansMod.log.error("Failed to instantiate Item for {}", config);
            LogUtils.logErrorWithoutStacktrace(e);
            return null;
        }
    }
}
