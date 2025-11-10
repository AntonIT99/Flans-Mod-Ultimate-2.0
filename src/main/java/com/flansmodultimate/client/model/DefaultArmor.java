package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

import net.minecraft.world.item.ArmorItem;

public class DefaultArmor extends ModelCustomArmour
{
    public DefaultArmor(ArmorItem.Type armorType, float expansion)
    {
        int textureX = 64;
        int textureY = 32;

        if (armorType == ArmorItem.Type.HELMET)
        {
            headModel = new ModelRendererTurbo[2];
            headModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
            headModel[1] = new ModelRendererTurbo(this, 32, 0, textureX, textureY);
            headModel[0].addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, expansion);
            headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
            headModel[1].addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, expansion + 0.5F);
            headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
        }

        if (armorType == ArmorItem.Type.CHESTPLATE || armorType == ArmorItem.Type.LEGGINGS)
        {
            bodyModel = new ModelRendererTurbo[1];
            bodyModel[0] = new ModelRendererTurbo(this, 16, 16, textureX, textureY);
            bodyModel[0].addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, expansion);
            bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);

            rightArmModel = new ModelRendererTurbo[1];
            rightArmModel[0] = new ModelRendererTurbo(this, 40, 16, textureX, textureY);
            rightArmModel[0].addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, expansion);
            rightArmModel[0].setRotationPoint(-5.0F, 2.0F, 0.0F);

            leftArmModel = new ModelRendererTurbo[1];
            leftArmModel[0] = new ModelRendererTurbo(this, 40, 16, textureX, textureY);
            leftArmModel[0].mirror = true;
            leftArmModel[0].addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, expansion);
            leftArmModel[0].setRotationPoint(5.0F, 2.0F, 0.0F);
        }

        if (armorType == ArmorItem.Type.BOOTS || armorType == ArmorItem.Type.LEGGINGS)
        {
            rightLegModel = new ModelRendererTurbo[1];
            rightLegModel[0] = new ModelRendererTurbo(this, 0, 16, textureX, textureY);
            rightLegModel[0].addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, expansion);
            rightLegModel[0].setRotationPoint(-1.9F, 12.0F, 0.0F);

            leftLegModel = new ModelRendererTurbo[1];
            leftLegModel[0] = new ModelRendererTurbo(this, 0, 16, textureX, textureY);
            leftLegModel[0].mirror = true;
            leftLegModel[0].addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, expansion);
            leftLegModel[0].setRotationPoint(1.9F, 12.0F, 0.0F);
        }
    }

    public DefaultArmor(ArmorItem.Type armorType)
    {
        this(armorType, 1.0F);
    }
}
