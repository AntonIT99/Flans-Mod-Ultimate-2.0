package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.BulletType;
import lombok.Getter;

public class BulletItem extends ShootableItem
{
    @Getter
    protected final BulletType configType;

    public BulletItem(BulletType configType)
    {
        super();
        this.configType = configType;
    }
}
