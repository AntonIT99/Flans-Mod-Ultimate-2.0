package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.BulletType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BulletItem extends ShootableItem implements IFlanItem<BulletType>
{
    @Getter
    protected final BulletType configType;

    public BulletItem(BulletType configType)
    {
        super(configType);
        this.configType = configType;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);
    }
}
