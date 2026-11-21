package com.flansmodultimate.mixin;

import com.flansmodultimate.client.model.ItemIconAtlas;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;

/**
 * Hooks for {@link SpriteLoader}.
 * <ul>
 * <li>Item icon atlas: stitches it without mipmaps, which item render types never sample.</li>
 * </ul>
 */
@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderMixin
{
    @Shadow
    @Final
    private ResourceLocation location;

    @ModifyVariable(method = "loadAndStitch", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int flansmodultimateItemIconMipLevel(int mipLevel)
    {
        return ItemIconAtlas.mipLevel(location, mipLevel);
    }
}
