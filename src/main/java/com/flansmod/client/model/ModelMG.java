package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Setter;

import net.minecraft.util.Mth;

public class ModelMG extends ModelBase implements IFlanTypeModel<GunType>
{
    @Setter
    protected GunType type;

    protected ModelRendererTurbo[] bipodModel;
    protected ModelRendererTurbo[] gunModel;
    protected ModelRendererTurbo[] ammoModel;
    protected ModelRendererTurbo[] ammoBoxModel = new ModelRendererTurbo[0];

    @Override
    public Class<GunType> typeClass()
    {
        return GunType.class;
    }

    public void renderBipod(DeployedGun mg, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for(ModelRendererTurbo bipodPart : bipodModel)
        {
            bipodPart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }
        if (mg.getReloadTimer() > 0 || mg.getAmmo().isEmpty())
            return;

        for(ModelRendererTurbo ammoBoxPart : ammoBoxModel)
        {
            ammoBoxPart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }
    }

    public void renderGun(DeployedGun mg, float partialTick, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for (ModelRendererTurbo gunPart : gunModel)
        {
            gunPart.rotateAngleX = -(mg.xRotO + (mg.getXRot() - mg.xRotO) * partialTick) / 180F * Mth.PI;
            gunPart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }

        if (mg.getReloadTimer() > 0 || mg.getAmmo().isEmpty())
            return;

        for (ModelRendererTurbo ammoPart : ammoModel)
        {
            ammoPart.rotateAngleX = -(mg.xRotO + (mg.getXRot() - mg.xRotO) * partialTick) / 180F * Mth.PI;
            ammoPart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }
    }

    public void flipAll()
    {
        for (ModelRendererTurbo aBipodModel : bipodModel)
        {
            aBipodModel.doMirror(false, true, true);
            aBipodModel.setRotationPoint(aBipodModel.rotationPointX, -aBipodModel.rotationPointY, -aBipodModel.rotationPointZ);
        }
        for (ModelRendererTurbo aGunModel : gunModel)
        {
            aGunModel.doMirror(false, true, true);
            aGunModel.setRotationPoint(aGunModel.rotationPointX, -aGunModel.rotationPointY, -aGunModel.rotationPointZ);
        }
        for (ModelRendererTurbo anAmmoModel : ammoModel)
        {
            anAmmoModel.doMirror(false, true, true);
            anAmmoModel.setRotationPoint(anAmmoModel.rotationPointX, -anAmmoModel.rotationPointY, -anAmmoModel.rotationPointZ);
        }
        for (ModelRendererTurbo anAmmoBoxModel : ammoBoxModel)
        {
            anAmmoBoxModel.doMirror(false, true, true);
            anAmmoBoxModel.setRotationPoint(anAmmoBoxModel.rotationPointX, -anAmmoBoxModel.rotationPointY, -anAmmoBoxModel.rotationPointZ);
        }
    }
}
