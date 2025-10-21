package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.BulletType;
import lombok.Getter;
import lombok.Setter;

public class BulletItem extends ShootableItem
{
    @Getter @Setter
    protected final BulletType configType;

    public BulletItem(BulletType configType)
    {
        super(configType);
        this.configType = configType;
    }
}
