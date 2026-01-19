package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.util.Mth;

public class ModelMG extends ModelBase implements IFlanTypeModel<GunType>
{
    @Getter @Setter
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

    public void renderBipod(DeployedGun mg, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo bipodPart : bipodModel)
        {
            bipodPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (mg.getReloadTimer() > 0 || !mg.hasAmmo())
            return;

        for (ModelRendererTurbo ammoBoxPart : ammoBoxModel)
        {
            ammoBoxPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    public void renderGun(DeployedGun mg, float partialTick, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        float pitch = Mth.lerp(partialTick, mg.xRotO, mg.getXRot()) * Mth.DEG_TO_RAD;

        for (ModelRendererTurbo gunPart : gunModel)
        {
            gunPart.rotateAngleX = -pitch;
            gunPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        if (mg.getReloadTimer() > 0 || !mg.hasAmmo())
            return;

        for (ModelRendererTurbo ammoPart : ammoModel)
        {
            ammoPart.rotateAngleX = -pitch;
            ammoPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
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
