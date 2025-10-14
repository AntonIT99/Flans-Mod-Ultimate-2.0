package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.GrenadeType;
import lombok.Getter;

public class GrenadeItem extends ShootableItem
{
    @Getter
    protected final GrenadeType configType;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;
    }
}
