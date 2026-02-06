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
        if (getConfigType().getRoundsPerItem() != 0)
            tooltipComponents.add(IFlanItem.statLine("Rounds", String.valueOf(getConfigType().getRoundsPerItem())));

        if (getConfigType().useKineticDamageSystem())
            tooltipComponents.add(IFlanItem.statLine("Mass", IFlanItem.formatFloat(getConfigType().getMass()) + "g"));
        else
            IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getDamage(), "Damage");

        if (getConfigType().getFallSpeed() > 1F || getConfigType().getFallSpeed() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Gravity Factor", IFlanItem.formatFloat(getConfigType().getFallSpeed())));

        if (getConfigType().getBulletSpread() > 0F)
            tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(getConfigType().getDispersionForDisplay()) + "°"));

        if (getConfigType().getExplosionRadius() > 0F)
        {
            IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getExplosionDamage(), "Explosion Damage");
            tooltipComponents.add(IFlanItem.statLine("Explosion Radius", IFlanItem.formatFloat(getConfigType().getExplosionRadius(), 1)));
        }

        if (getConfigType().getExplosionPower() > 1F || getConfigType().getExplosionPower() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Explosion Power", IFlanItem.formatFloat(getConfigType().getExplosionPower(), 1)));
    }
}
