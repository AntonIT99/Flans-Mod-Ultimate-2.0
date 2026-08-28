package com.flansmod.client.model.wolffstarwars;

import org.lwjgl.opengl.GL11;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.wolffsmod.api.client.model.ModelBase;

import net.minecraft.world.entity.Entity;


public class ModelLaserBeam extends ModelBase
{
    public ModelRendererTurbo laserModel;

    int textureX = 32;
    int textureY = 32;

    public ModelLaserBeam()
    {
        laserModel = new ModelRendererTurbo(this, 0, 0);
        laserModel.addBox(-1.5F, -16.0F, -1.5F, 3, 32, 3);
        laserModel.glow = true;
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
    {
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        GL11.glTranslatef(0F, 3F, 0F);
        laserModel.render(f5);
    }
}
