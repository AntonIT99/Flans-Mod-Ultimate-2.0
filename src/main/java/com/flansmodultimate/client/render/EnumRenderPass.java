package com.flansmodultimate.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public enum EnumRenderPass
{
    DEFAULT,
    GLOW_ALPHA_NO_DEPTH_WRITE,
    GLOW_ALPHA,
    GLOW_ADDITIVE;

    public static final List<EnumRenderPass> ORDER = List.of(GLOW_ALPHA_NO_DEPTH_WRITE, GLOW_ALPHA, GLOW_ADDITIVE, DEFAULT);

    public RenderType getRenderType(ResourceLocation texture)
    {
        return switch(this)
        {
            case GLOW_ALPHA_NO_DEPTH_WRITE -> CustomRenderType.entityEmissiveAlphaNoDepthWrite(texture);
            case GLOW_ALPHA -> CustomRenderType.entityEmissiveAlpha(texture);
            case GLOW_ADDITIVE -> CustomRenderType.entityEmissiveAdditive(texture);
            default -> RenderType.entityTranslucent(texture);
        };
    }
}
