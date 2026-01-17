package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.ERenderPass;
import com.flansmodultimate.common.types.AttachmentType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;

public class ModelAttachment extends ModelBase implements IFlanTypeModel<AttachmentType>
{
    @Getter @Setter
    protected AttachmentType type;

    protected ModelRendererTurbo[] attachmentModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] ammoModel = new ModelRendererTurbo[0];

    protected EnumAnimationType secondaryAnimType = EnumAnimationType.NONE;
    protected float tiltGunTime = 0.15F;
    protected float unloadClipTime = 0.35F;
    protected float loadClipTime = 0.35F;
    protected float untiltGunTime = 0.15F;
    /** For rifles and shotguns. Currently, a generic reload animation regardless of how full the internal magazine already is */
    protected float numBulletsInReloadAnimation = 1F;
    /** For end loaded projectiles */
    protected float endLoadedAmmoDistance = 1F;
    /** For big scopes, so that the player actually looks through them properly */
    protected float renderOffset;
    /** Visual recoil of the gun when firing */
    protected float recoilDistance = 0.125F;
    protected float recoilAngle = -8F;
    /** Offset the flash model if enabled */
    protected Vector3f attachmentFlashOffset = new Vector3f(0F, 0F, 0F);
    protected Vector3f muzzleFlashPoint = new Vector3f(0F, 0F, 0F);

    @Override
    public Class<AttachmentType> typeClass()
    {
        return AttachmentType.class;
    }

    public Vector3f getMuzzleFlashPoint(Vector3f gunMuzzleFlashPoint, Vector3f barrelAttachPoint)
    {
        if (attachmentFlashOffset != null && !attachmentFlashOffset.equals(Vector3f.Zero))
            return Vector3f.add(gunMuzzleFlashPoint, attachmentFlashOffset, null);
        if (muzzleFlashPoint != null && !muzzleFlashPoint.equals(Vector3f.Zero))
            return Vector3f.add(barrelAttachPoint, muzzleFlashPoint, null);

        return gunMuzzleFlashPoint;
    }

    public void renderAttachment(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        for (ModelRendererTurbo model : attachmentModel)
            if (model != null)
                model.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void renderAttachmentAmmo(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        for (ModelRendererTurbo model : ammoModel)
            if (model != null)
                model.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void flipAll()
    {
        for (ModelRendererTurbo anAttachmentModel : attachmentModel)
        {
            anAttachmentModel.doMirror(false, true, true);
            anAttachmentModel.setRotationPoint(anAttachmentModel.rotationPointX, -anAttachmentModel.rotationPointY, -anAttachmentModel.rotationPointZ);
        }

        for (ModelRendererTurbo anAmmoModel : ammoModel)
        {
            anAmmoModel.doMirror(false, true, true);
            anAmmoModel.setRotationPoint(anAmmoModel.rotationPointX, -anAmmoModel.rotationPointY, -anAmmoModel.rotationPointZ);
        }
    }
}
