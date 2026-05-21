package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.wolffsmod.api.client.model.ModelBase;

public class ModelItemHolder extends ModelBase
{
    public ModelRendererTurbo[] baseModel = new ModelRendererTurbo[0];

    public Vector3f itemOffset = new Vector3f();
    public Vector3f itemRotation = new Vector3f();

    public void flipAll()
    {
        flip(baseModel);
    }

    protected void flip(ModelRendererTurbo[] model)
    {
        for (ModelRendererTurbo part : model)
        {
            part.doMirror(false, true, true);
            part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
        }
    }

    public void translateAll(float x, float y, float z)
    {
        translate(baseModel, x, y, z);
    }

    protected void translate(ModelRendererTurbo[] model, float x, float y, float z)
    {
        for (ModelRendererTurbo part : model)
        {
            part.rotationPointX += x;
            part.rotationPointY += y;
            part.rotationPointZ += z;
        }
    }
}
