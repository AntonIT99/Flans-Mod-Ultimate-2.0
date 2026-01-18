package com.flansmodultimate.common.guns.handler;

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
        // Damage the bullet item
        bulletStack.setDamageValue(bulletStack.getDamageValue() + 1);
    }
}
