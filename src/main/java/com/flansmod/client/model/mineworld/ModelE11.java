//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: E11
// Model Creator:
// Created on: 12.01.2026 - 01:39:03
// Last changed on: 12.01.2026 - 01:39:03

package com.flansmod.client.model.mineworld; //Path where the model is located

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelE11 extends ModelGun //Same as Filename
{
    int textureX = 64;
    int textureY = 64;

    public ModelE11() //Same as Filename
    {
        gunModel = new ModelRendererTurbo[19];
        gunModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import ImportImportAngledBody
        gunModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Import ImportImportBarrel
        gunModel[2] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Import ImportImportBody
        gunModel[3] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Import ImportImportUnderBarrelFront
        gunModel[4] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Import ImportImportGrip
        gunModel[5] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Import ImportImportSightFront
        gunModel[6] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Import ImportImportSightRear
        gunModel[7] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Import ImportImportBodyBack
        gunModel[8] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Import ImportImportUnderBarrel
        gunModel[9] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Import ImportImportBodySide
        gunModel[10] = new ModelRendererTurbo(this, 57, 9, textureX, textureY); // Import ImportImportBodyMag
        gunModel[11] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Import ImportBox13
        gunModel[12] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Import ImportBox14
        gunModel[13] = new ModelRendererTurbo(this, 33, 33, textureX, textureY); // Import ImportBox18
        gunModel[14] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 0
        gunModel[15] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 2
        gunModel[16] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 3
        gunModel[17] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 4
        gunModel[18] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 6

        gunModel[0].addBox(0F, 0F, 0F, 2, 2, 2, 0F); // Import ImportImportAngledBody
        gunModel[0].setRotationPoint(1.5F, -3.5F, -1F);
        gunModel[0].rotateAngleZ = -1.57079633F;

        gunModel[1].addShapeBox(0F, 0F, 0F, 8, 1, 1, 0F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F, 0F, 0.25F, 0.25F); // Import ImportImportBarrel
        gunModel[1].setRotationPoint(4F, -4.7F, -0.5F);

        gunModel[2].addBox(0F, 0F, 0F, 10, 2, 2, 0F); // Import ImportImportBody
        gunModel[2].setRotationPoint(-6F, -5F, -1F);

        gunModel[3].addBox(0F, 0F, 0F, 4, 1, 2, 0F); // Import ImportImportUnderBarrelFront
        gunModel[3].setRotationPoint(13F, -2.25F, -1F);
        gunModel[3].rotateAngleZ = 3.28121899F;

        gunModel[4].addBox(0F, 0F, 0F, 2, 5, 2, 0F); // Import ImportImportGrip
        gunModel[4].setRotationPoint(-1F, -5F, -1F);
        gunModel[4].rotateAngleZ = -0.34906585F;

        gunModel[5].addBox(0F, 0F, 0F, 1, 1, 1, 0F); // Import ImportImportSightFront
        gunModel[5].setRotationPoint(10F, -5F, -0.5F);
        gunModel[5].rotateAngleZ = 0.6981317F;

        gunModel[6].addBox(0F, 0F, 0F, 2, 1, 1, 0F); // Import ImportImportSightRear
        gunModel[6].setRotationPoint(3F, -5.4F, -0.5F);

        gunModel[7].addBox(0F, 0F, 0F, 2, 1, 2, 0F); // Import ImportImportBodyBack
        gunModel[7].setRotationPoint(-2.5F, -3.5F, -1F);

        gunModel[8].addBox(0F, 0F, 0F, 12, 1, 2, 0F); // Import ImportImportUnderBarrel
        gunModel[8].setRotationPoint(1F, -3.25F, -1F);

        gunModel[9].addBox(0F, 0F, 0F, 2, 1, 3, 0F); // Import ImportImportBodySide
        gunModel[9].setRotationPoint(2F, -4.8F, -1.5F);

        gunModel[10].addBox(0F, 0F, 0F, 1, 1, 1, 0F); // Import ImportImportBodyMag
        gunModel[10].setRotationPoint(4F, -4.8F, -1.5F);

        gunModel[11].addBox(0F, 0F, 0F, 8, 1, 2, 0F); // Import ImportBox13
        gunModel[11].setRotationPoint(-5F, -5.5F, -1F);

        gunModel[12].addShapeBox(0F, 0F, 0F, 4, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F); // Import ImportBox14
        gunModel[12].setRotationPoint(-2F, -6.1F, -0.5F);

        gunModel[13].addBox(0F, 0F, 0F, 3, 1, 1, 0F); // Import ImportBox18
        gunModel[13].setRotationPoint(-1.5F, -5.85F, -0.5F);

        gunModel[14].addShapeBox(0F, 0F, 0F, 4, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F); // Box 0
        gunModel[14].setRotationPoint(-2F, -7F, 0.4F);

        gunModel[15].addShapeBox(0F, 0F, 0F, 4, 1, 1, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 2
        gunModel[15].setRotationPoint(-2F, -7.9F, -0.5F);

        gunModel[16].addShapeBox(0F, 0F, 0F, 4, 1, 1, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 3
        gunModel[16].setRotationPoint(-2F, -7F, -1.4F);

        gunModel[17].addShapeBox(-1F, 0F, -1F, 1, 1, 1, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F); // Box 4
        gunModel[17].setRotationPoint(0F, -7F, 0.5F);
        gunModel[17].glow = true;

        gunModel[18].addShapeBox(-1F, 0F, -1F, 1, 1, 1, 0F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F, -0.47F, 0F, -0.47F); // Box 6
        gunModel[18].setRotationPoint(0F, -7F, 0.5F);
        gunModel[18].glow = true;

        ammoModel = new ModelRendererTurbo[1];
        ammoModel[0] = new ModelRendererTurbo(this, 49, 33, textureX, textureY); // Import ImportImportMagazine

        ammoModel[0].addBox(0F, 0F, 0F, 2, 1, 2, 0F); // Import ImportImportMagazine
        ammoModel[0].setRotationPoint(3F, -4.8F, -2.5F);

        hasFlash = true;
        muzzleFlashPoint = new Vector3f(12F /16F, 4F / 16F, 0F / 16F);

        barrelAttachPoint = new Vector3f(-12F /16F, 3F /16F, 0F / 16F);
        stockAttachPoint = new Vector3f(0F /16F, 0F /16F, 0F /16F);
        scopeAttachPoint = new Vector3f(0F /16F, 5F /16F, 0F /16F);
        gripAttachPoint = new Vector3f(10 /16F, 3F /16F, 0F /16F);

        gunSlideDistance = 0F;
        animationType = EnumAnimationType.RIFLE;

        translateAll(0F, 0F, 0F);

        flipAll();
    }
}