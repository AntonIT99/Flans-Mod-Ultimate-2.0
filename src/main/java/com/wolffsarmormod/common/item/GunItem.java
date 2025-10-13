package com.wolffsarmormod.common.item;

import com.flansmod.client.model.ModelGun;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.common.types.PaintableType;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class GunItem extends Item implements IModelItem<GunType, ModelGun>, IOverlayItem<GunType>, IPaintableItem<GunType>
{
    @Getter
    protected final GunType configType;
    @Getter @Setter
    protected ModelGun model;
    @Getter @Setter
    protected ResourceLocation texture;
    @Setter
    protected ResourceLocation overlay;

    public GunItem(GunType configType)
    {
        super(new Item.Properties());
        this.configType = configType;

        if (FMLEnvironment.dist == Dist.CLIENT)
            clientSideInit();
    }

    @Override
    public void clientSideInit()
    {
        loadModelAndTexture(null);
        loadOverlay();
    }

    @Override
    public boolean useCustomItemRendering()
    {
        return true;
    }

    @Override
    public Optional<ResourceLocation> getOverlay()
    {
        return Optional.ofNullable(overlay);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);
    }

    @Override
    public PaintableType GetPaintableType()
    {
        return null;
    }
}
