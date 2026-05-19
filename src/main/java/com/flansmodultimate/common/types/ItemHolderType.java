package com.flansmodultimate.common.types;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ItemHolderType extends InfoType
{
    public static ItemHolderType getItemHolder(String shortName)
    {
        if (InfoType.getInfoType(shortName) instanceof ItemHolderType itemHolderType)
            return itemHolderType;
        return null;
    }
}
