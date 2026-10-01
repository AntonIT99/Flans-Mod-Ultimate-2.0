package com.flansmodultimate.client.render;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** A single white pixel, for drawing flat-coloured geometry with entity render types. Render thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WhiteTexture
{
    @Nullable
    private static ResourceLocation location;

    public static ResourceLocation get()
    {
        if (location == null)
        {
            DynamicTexture texture = new DynamicTexture(1, 1, false);
            texture.getPixels().setPixelRGBA(0, 0, -1);
            texture.upload();
            location = Minecraft.getInstance().getTextureManager().register("flansmodultimate_white", texture);
        }
        return location;
    }
}
