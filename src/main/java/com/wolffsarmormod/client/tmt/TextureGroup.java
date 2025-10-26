package com.wolffsarmormod.client.tmt;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

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
        if(!texture.isEmpty())
        {
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("", texture));
        }
        else if(defaultTexture > -1)
        {
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("", ""));
        }
    }
}
