package com.flansmodultimate.api;

import com.flansmodultimate.IContentProvider;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;

/** A content-pack type definition: a gun, bullet, driveable, armour and so on. */
public interface IInfoType
{
    /**
     * @return the content pack this type originated from
     */
    IContentProvider getContentPack();

    /**
     * @return the item this type is bound to, or {@code null} for itemless types
     */
    @Nullable
    Item getItem();

    /**
     * @return the name of this type, without localisation
     */
    String getName();

    /**
     * @return the short name, which is also the item's registry path
     */
    String getShortName();

    /**
     * @return the description of this type
     */
    String getDescription();
}
