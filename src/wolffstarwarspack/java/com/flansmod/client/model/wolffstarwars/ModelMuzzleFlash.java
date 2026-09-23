package com.flansmod.client.model.wolffstarwars;

import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.tmt.ModelRendererTurbo;


public class ModelMuzzleFlash extends ModelFlash
{
  int textureX = 256;
  int textureY = 128;
  
  public ModelMuzzleFlash() {
    flashModel = new ModelRendererTurbo[3][1];
    flashModel[0][0] = new ModelRendererTurbo(this, 165, 2, textureX, textureY);
    flashModel[1][0] = new ModelRendererTurbo(this, 0, 2, textureX, textureY);
    flashModel[2][0] = new ModelRendererTurbo(this, 80, 0, textureX, textureY);
    
    flashModel[0][0].addShapeBox(0.0F, -8.0F, -0.5F, 1, 35, 35, 0.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F, -0.45F, -13.0F, -13.0F);
    flashModel[0][0].setRotationPoint(-0.5F, -9.5F, -17.0F);
    
    flashModel[1][0].addShapeBox(0.0F, -8.0F, -0.5F, 1, 35, 35, 0.0F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F, -0.45F, -13.25F, -13.25F);
    flashModel[1][0].setRotationPoint(-0.5F, -9.5F, -17.0F);
    
    flashModel[2][0].addShapeBox(0.0F, -8.0F, -0.5F, 1, 35, 35, 0.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F, -0.45F, -12.0F, -12.0F);
    flashModel[2][0].setRotationPoint(-0.5F, -9.5F, -17.0F);
    
    flashModel[0][0].glow = true;
    flashModel[1][0].glow = true;
    flashModel[2][0].glow = true;
    
    flipAll();
  }
}
