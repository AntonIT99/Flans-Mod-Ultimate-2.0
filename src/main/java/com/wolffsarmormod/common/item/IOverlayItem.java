package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.InfoType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface IOverlayItem<T extends InfoType> extends IFlanItem<T>
{
    @OnlyIn(Dist.CLIENT)
    Optional<ResourceLocation> getOverlay();

    @OnlyIn(Dist.CLIENT)
    void setOverlay(ResourceLocation overlay);

    @OnlyIn(Dist.CLIENT)
    default void loadOverlay()
    {
        InfoType configType = getConfigType();
        Optional<ResourceLocation> overlay = configType.getOverlay();
        overlay.ifPresent(this::setOverlay);
    }
}
