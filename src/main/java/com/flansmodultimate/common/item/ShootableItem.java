package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.ShootableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class ShootableItem extends Item
{
    protected final String shortname;

    protected ShootableItem(ShootableType configType)
    {
        super(createProperties(configType));
        shortname = configType.getShortName();
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

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getDamage(), "Damage");

        if (getConfigType().getRoundsPerItem() != 0)
            tooltipComponents.add(IFlanItem.statLine("Rounds", String.valueOf(getConfigType().getRoundsPerItem())));
        if (getConfigType().getFallSpeed() > 1F || getConfigType().getFallSpeed() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Fall Speed", String.valueOf(getConfigType().getFallSpeed())));
        if (getConfigType().getBulletSpread() > 1F || getConfigType().getBulletSpread() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Spread", String.valueOf(getConfigType().getBulletSpread())));

        IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getExplosionDamage(), "Explosion Damage");

        if (getConfigType().getExplosionRadius() > 0F)
            tooltipComponents.add(IFlanItem.statLine("Explosion Radius", String.valueOf(getConfigType().getExplosionRadius())));
        if (getConfigType().getExplosionPower() > 1F || getConfigType().getExplosionPower() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Explosion Power", String.valueOf(getConfigType().getExplosionPower())));
    }
}
