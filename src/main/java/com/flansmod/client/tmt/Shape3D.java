package com.flansmod.client.tmt;

@SuppressWarnings("java:S1104")
public class Shape3D
{
    public PositionTransformVertex[] vertices;
    public TexturedPolygon[] faces;

    public Shape3D(PositionTransformVertex[] verts, TexturedPolygon[] poly)
    {
        vertices = verts;
        faces = poly;
    }
}
