package com.wolffsmod.api.client.model;

import net.minecraft.world.phys.Vec3;

public class PositionTextureVertex
{
    public Vec3 vector3D;
    public float texturePositionX;
    public float texturePositionY;

    public PositionTextureVertex(float x, float y, float z, float texturePositionXIn, float texturePositionYIn)
    {
        this(new Vec3(x, y, z), texturePositionXIn, texturePositionYIn);
    }

    public PositionTextureVertex setTexturePosition(float texturePositionXIn, float texturePositionYIn)
    {
        return new PositionTextureVertex(this, texturePositionXIn, texturePositionYIn);
    }

    public PositionTextureVertex(PositionTextureVertex textureVertex, float texturePositionXIn, float texturePositionYIn)
    {
        this.vector3D = textureVertex.vector3D;
        this.texturePositionX = texturePositionXIn;
        this.texturePositionY = texturePositionYIn;
    }

    public PositionTextureVertex(Vec3 vector3D, float texturePositionX, float texturePositionY)
    {
        this.vector3D = vector3D;
        this.texturePositionX = texturePositionX;
        this.texturePositionY = texturePositionY;
    }
}