package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.ShootableType;

import net.minecraft.world.item.Item;

public abstract class ShootableItem extends Item
{
    protected ShootableItem(ShootableType configType)
    {
        super(createProperties(configType));
    }

    public abstract ShootableType getConfigType();

    private static Properties createProperties(ShootableType configType)
    {
        Properties p = new Properties();
        int rounds = configType.getRoundsPerItem();
        int maxStack = Math.max(1, configType.getMaxStackSize());

        if (rounds > 0)
        {
            // durability implies unstackable
            p.durability(rounds);
        }
        else
        {
            // stackable, no durability
            p.stacksTo(maxStack);
        }
        return p;
    }
}
