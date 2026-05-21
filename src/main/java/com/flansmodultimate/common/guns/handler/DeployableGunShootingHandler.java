package com.flansmodultimate.common.guns.handler;

import com.flansmodultimate.common.item.ShootableItem;
import net.minecraft.world.item.ItemStack;

public class DeployableGunShootingHandler implements ShootingHandler
{
    private final ItemStack bulletStack;

    public DeployableGunShootingHandler(ItemStack bulletStack)
    {
        this.bulletStack = bulletStack;
    }

    @Override
    public void onShoot()
    {
        ShootableItem.consumeRound(bulletStack);
    }
}
