package com.flansmod.client.tmt;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "java:S1104"})
public class TextureGroup
{
    public List<TexturedPolygon> poly;
    public String texture;

    public TextureGroup()
    {
        poly = new ArrayList<>();
        texture = "";
    }

    public void addPoly(TexturedPolygon polygon)
    {
        poly.add(polygon);
    }

    public void loadTexture()
    {
        loadTexture(-1);
    }

    public void loadTexture(int defaultTexture)
    {
        ResourceLocation textureLocation = ResourceLocation.tryParse(texture.trim().replace('\\', '/'));
        if (textureLocation != null && !textureLocation.getPath().isEmpty())
        {
            RenderSystem.setShaderTexture(0, textureLocation);
        }
        else if (defaultTexture > -1)
        {
            // The legacy fallback is an already allocated OpenGL texture id,
            // not a resource path. 1.21 rejects the former "minecraft:" shim.
            RenderSystem.setShaderTexture(0, defaultTexture);
        }
    }
}
