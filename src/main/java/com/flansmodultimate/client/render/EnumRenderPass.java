package com.flansmodultimate.client.render;

import com.flansmodultimate.config.ModClientConfig;
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

    public RenderType getRenderType(ResourceLocation texture, boolean translucent, boolean cull)
    {
        return switch(this)
        {
            case GLOW_ALPHA_NO_DEPTH_WRITE -> CustomRenderType.entityEmissiveAlphaNoDepthWrite(texture, cull);
            case GLOW_ALPHA -> CustomRenderType.entityEmissiveAlpha(texture, cull);
            case GLOW_ADDITIVE -> CustomRenderType.entityEmissiveAdditive(texture, cull);
            default ->
            {
                if (translucent)
                {
                    if (ModClientConfig.get().enableFastTranslucentRendering)
                        yield CustomRenderType.entityTranslucentUnsorted(texture, cull);
                    yield cull ? RenderType.entityTranslucentCull(texture) : RenderType.entityTranslucent(texture);
                }
                if (cull)
                    yield RenderType.entityCutout(texture);

                yield RenderType.entityCutoutNoCull(texture);
            }
        };
    }

    public RenderType getArmorRenderType(ResourceLocation texture, boolean translucent, boolean cull)
    {
        return switch(this)
        {
            case GLOW_ALPHA_NO_DEPTH_WRITE -> CustomRenderType.entityEmissiveAlphaNoDepthWrite(texture, cull);
            case GLOW_ALPHA -> CustomRenderType.entityEmissiveAlpha(texture, cull);
            case GLOW_ADDITIVE -> CustomRenderType.entityEmissiveAdditive(texture, cull);
            default -> translucent
                ? (ModClientConfig.get().enableFastTranslucentRendering
                    ? CustomRenderType.armorTranslucentUnsorted(texture, cull)
                    : CustomRenderType.armorTranslucent(texture, cull))
                : CustomRenderType.armorCutout(texture, cull);
        };
    }
}
