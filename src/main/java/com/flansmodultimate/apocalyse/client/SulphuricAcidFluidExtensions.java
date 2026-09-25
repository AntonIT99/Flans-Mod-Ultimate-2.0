package com.flansmodultimate.apocalyse.client;

import com.flansmodultimate.FlansMod;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Client textures of the sulphuric acid fluid type. Client-only. */
public final class SulphuricAcidFluidExtensions implements IClientFluidTypeExtensions
{
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidstill");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidflowing");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/misc/sulphuric_acid_overlay.png");

    @Override
    @NotNull
    public ResourceLocation getStillTexture()
    {
        return STILL_TEXTURE;
    }

    @Override
    @NotNull
    public ResourceLocation getFlowingTexture()
    {
        return FLOWING_TEXTURE;
    }

    @Override
    public ResourceLocation getRenderOverlayTexture(@NotNull Minecraft minecraft)
    {
        return OVERLAY_TEXTURE;
    }
}
