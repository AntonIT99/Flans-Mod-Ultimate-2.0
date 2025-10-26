package com.wolffsarmormod.common.item;

import com.wolffsarmormod.client.model.ModelGrenade;
import com.wolffsarmormod.common.types.GrenadeType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrenadeItem extends ShootableItem implements IModelItem<GrenadeType, ModelGrenade>
{
    @Getter
    protected final GrenadeType configType;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;
    }

    @Override
    public void clientSideInit()
    {

    }

    @Override
    public ResourceLocation getTexture()
    {
        return null;
    }

    @Override
    public void setTexture(ResourceLocation texture)
    {

    }

    @Override
    public @Nullable ModelGrenade getModel()
    {
        return null;
    }

    @Override
    public void setModel(ModelGrenade model)
    {

    }

    @Override
    public boolean useCustomItemRendering()
    {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @javax.annotation.Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);
    }

    public void throwGrenade(Level level, LivingEntity thrower)
    {
        //TODO: implement EntityGrenade
        /*EntityGrenade grenade = getGrenade(world, thrower);
        world.spawnEntity(grenade);*/
    }
}
