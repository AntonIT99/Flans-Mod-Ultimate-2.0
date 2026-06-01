package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.wolffsmod.api.client.model.ModelBase;

public class ModelAAGun extends ModelBase
{
    public ModelRendererTurbo[] baseModel;
    public ModelRendererTurbo[] seatModel;
    public int barrelX;
    public int barrelY;
    public int barrelZ;

    public void flipAll()
    {
        if (baseModel != null)
        {
            for (ModelRendererTurbo part : baseModel)
            {
                if (part != null)
                {
                    part.doMirror(false, true, true);
                    part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
                }
            }
        }
        if (seatModel != null)
        {
            for (ModelRendererTurbo part : seatModel)
            {
                if (part != null)
                {
                    part.doMirror(false, true, true);
                    part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
                }
            }
        }
    }
}