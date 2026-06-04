package com.flansmod.client.tmt;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "java:S1104"})
public class PositionTransformVertex extends PositionTextureVertex
{
    public Vec3 neutralVector;
    public List<TransformGroup> transformGroups = new ArrayList<>();

    public PositionTransformVertex(float x, float y, float z, float u, float v)
    {
        this(new Vec3(x, y, z), u, v);
    }

    public PositionTransformVertex(PositionTextureVertex vertex, float u, float v)
    {
        super(vertex, u, v);
        if (vertex instanceof PositionTransformVertex positionTransformVertex)
            neutralVector = positionTransformVertex.neutralVector;
        else
            neutralVector = new Vec3(vertex.vector3D.x, vertex.vector3D.y, vertex.vector3D.z);
    }

    public PositionTransformVertex(PositionTextureVertex vertex)
    {
        this(vertex, vertex.texturePositionX, vertex.texturePositionY);
    }

    public PositionTransformVertex(Vec3 vector, float u, float v)
    {
        super(vector, u, v);
        neutralVector = new Vec3(vector.x, vector.y, vector.z);
    }

    public void setTransformation()
    {
        if(transformGroups.isEmpty())
        {
            vector3D = neutralVector;
            return;
        }
        double weight = 0D;
        for(TransformGroup transformGroup : transformGroups)
        {
            weight += transformGroup.getWeight();
        }
        if(weight == 0D)
        {
            vector3D = neutralVector;
            return;
        }

        double inverseWeight = 1D / weight;
        double x = 0D;
        double y = 0D;
        double z = 0D;
        for(TransformGroup group : transformGroups)
        {
            double cWeight = group.getWeight() * inverseWeight;
            Vec3 vector = group.doTransformation(this);

            x += cWeight * vector.x;
            y += cWeight * vector.y;
            z += cWeight * vector.z;
        }
        vector3D = new Vec3(x, y, z);
    }

    public void addGroup(TransformGroup group)
    {
        transformGroups.add(group);
    }

    public void removeGroup(TransformGroup group)
    {
        transformGroups.remove(group);
    }
}
