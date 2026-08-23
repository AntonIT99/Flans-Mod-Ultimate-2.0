package com.flansmod.client.tmt;

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
        // Textures are bound by the immutable RenderType/RenderSetup chosen by
        // the caller. Immediate shader texture mutation is not supported by
        // the 26.1 extraction renderer.
    }
}
