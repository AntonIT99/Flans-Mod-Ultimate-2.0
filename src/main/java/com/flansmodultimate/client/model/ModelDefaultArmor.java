package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

import net.minecraft.world.item.equipment.ArmorType;

public class ModelDefaultArmor extends ModelCustomArmour
{
    public ModelDefaultArmor(ArmorType armorType, float expansion)
    {
        int textureX = 64;
        int textureY = 32;

        if (armorType == ArmorType.HELMET)
        {
            headModel = new ModelRendererTurbo[2];
            headModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
            headModel[1] = new ModelRendererTurbo(this, 32, 0, textureX, textureY);
            headModel[0].addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, expansion + 0.5F);
            headModel[1].addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, expansion + 1.0F);
        }

        if (armorType == ArmorType.CHESTPLATE || armorType == ArmorType.LEGGINGS)
        {
            bodyModel = new ModelRendererTurbo[1];
            bodyModel[0] = new ModelRendererTurbo(this, 16, 16, textureX, textureY);
            bodyModel[0].addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, expansion);

            rightArmModel = new ModelRendererTurbo[1];
            rightArmModel[0] = new ModelRendererTurbo(this, 40, 16, textureX, textureY);
            rightArmModel[0].addBox(-3.0F, -2.2F, -2.0F, 4, 12, 4, expansion);

            leftArmModel = new ModelRendererTurbo[1];
            leftArmModel[0] = new ModelRendererTurbo(this, 40, 16, textureX, textureY);
            leftArmModel[0].addBox(-1.0F, -2.2F, -2.0F, 4, 12, 4, expansion);
            leftArmModel[0].mirror = true;
        }

        if (armorType == ArmorType.BOOTS || armorType == ArmorType.LEGGINGS)
        {
            rightLegModel = new ModelRendererTurbo[1];
            rightLegModel[0] = new ModelRendererTurbo(this, 0, 16, textureX, textureY);
            rightLegModel[0].addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, expansion);

            leftLegModel = new ModelRendererTurbo[1];
            leftLegModel[0] = new ModelRendererTurbo(this, 0, 16, textureX, textureY);
            leftLegModel[0].addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, expansion);
            leftLegModel[0].mirror = true;
        }
    }

    public ModelDefaultArmor(ArmorType armorType)
    {
        this(armorType, 0.5F);
    }
}
