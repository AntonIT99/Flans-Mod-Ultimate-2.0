package com.flansmodultimate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

import java.util.List;

/** Read-only use: set per-frame uniforms on the passes of a post chain the mod owns. */
@Mixin(PostChain.class)
public interface PostChainAccessor
{
    @Accessor("passes")
    List<PostPass> flansmodultimatePasses();
}
