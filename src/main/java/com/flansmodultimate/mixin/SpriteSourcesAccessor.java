package com.flansmodultimate.mixin;

import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;

/** Registers the item icon sprite source type that the item icon and block atlas definitions name. */
@Mixin(SpriteSources.class)
public interface SpriteSourcesAccessor
{
    @Invoker("register")
    static SpriteSourceType flansmodultimateRegister(String name, Codec<? extends SpriteSource> codec)
    {
        throw new AssertionError();
    }
}
