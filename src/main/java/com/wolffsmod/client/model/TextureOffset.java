package com.wolffsmod.client.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @param textureOffsetX The x coordinate offset of the texture
 * @param textureOffsetY The y coordinate offset of the texture
 */
@OnlyIn(Dist.CLIENT)
public record TextureOffset(int textureOffsetX, int textureOffsetY) {}
