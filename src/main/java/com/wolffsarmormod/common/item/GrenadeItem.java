package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.GrenadeType;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrenadeItem extends ShootableItem implements IModelItem<GrenadeType, ModelBase>
{
    @Getter
    protected final GrenadeType configType;
    @Getter @Setter
    protected ModelBase model;
    @Getter @Setter
    protected ResourceLocation texture;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;

        if (FMLEnvironment.dist == Dist.CLIENT)
            clientSideInit();
    }

    @Override
    public void clientSideInit()
    {
        loadModelAndTexture(null);
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
