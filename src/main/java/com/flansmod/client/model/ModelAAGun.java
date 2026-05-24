package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ModelAAGun extends ModelBase implements IFlanTypeModel<AAGunType>
{
    public record BarrelOriginData(Vec3[] pivots, Vec3[] muzzles) {}

    @Getter @Setter
    protected AAGunType type;

    public boolean oldModel = false;

    public ModelRendererTurbo[] baseModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] seatModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] gunModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[][] barrelModel = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[][] ammoModel = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[] gunsightModel = new ModelRendererTurbo[0];

    public int barrelX;
    public int barrelY;
    public int barrelZ;

    @Override
    public Class<AAGunType> typeClass()
    {
        return AAGunType.class;
    }

    public void renderBase(AAGun aa, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        renderParts(baseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void renderGun(AAGun aa, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        float pitch = -aa.getGunPitch() * Mth.DEG_TO_RAD;

        renderParts(seatModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        for (ModelRendererTurbo part : gunModel)
        {
            if (part == null)
                continue;
            part.setPosition(barrelX, barrelY, barrelZ);
            part.rotateAngleZ = pitch;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        for (ModelRendererTurbo part : gunsightModel)
        {
            if (part == null)
                continue;
            part.rotateAngleZ = pitch;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        float[] recoil = aa.getBarrelRecoil();
        for (int i = 0; i < barrelModel.length; i++)
        {
            float barrelRecoil = i < recoil.length ? recoil[i] : 0F;
            float x = -barrelRecoil * Mth.cos(-aa.getGunPitch() * Mth.DEG_TO_RAD) + barrelX;
            float y = -barrelRecoil * Mth.sin(-aa.getGunPitch() * Mth.DEG_TO_RAD) + barrelY;
            renderBarrelPartArray(barrelModel[i], x, y, barrelZ, pitch, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        for (int i = 0; i < ammoModel.length; i++)
        {
            if (!aa.hasAmmo(i))
                continue;
            renderBarrelPartArray(ammoModel[i], barrelX, barrelY, barrelZ, pitch, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    private static void renderParts(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    private static void renderBarrelPartArray(ModelRendererTurbo[] parts, float x, float y, float z, float pitch, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;

        for (ModelRendererTurbo part : parts)
        {
            if (part == null)
                continue;
            part.setPosition(x, y, z);
            part.rotateAngleZ = pitch;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    public BarrelOriginData getModelBarrelOriginData(AAGunType type)
    {
        int count = type.getNumBarrels();
        if (count <= 0 || barrelModel == null || barrelModel.length < count)
            return null;

        Vec3[] pivots = new Vec3[count];
        Vec3[] muzzles = new Vec3[count];
        Vec3 pivot = new Vec3(barrelX, barrelY, barrelZ);

        for (int barrel = 0; barrel < count; barrel++)
        {
            Vec3 muzzle = findMuzzlePoint(barrelModel[barrel]);
            if (muzzle == null)
                return null;

            pivots[barrel] = pivot;
            muzzles[barrel] = muzzle;
        }

        return new BarrelOriginData(pivots, muzzles);
    }

    private static Vec3 findMuzzlePoint(ModelRendererTurbo[] parts)
    {
        if (parts == null || parts.length == 0)
            return null;

        double[] bounds = new double[] {
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        };
        boolean found = false;
        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                found |= part.appendVertexBounds(bounds);
        }

        if (!found)
            return null;

        return new Vec3(
            bounds[3],
            (bounds[1] + bounds[4]) * 0.5D,
            (bounds[2] + bounds[5]) * 0.5D
        );
    }

    public void flipAll()
    {
        flipPartArray(baseModel);
        flipPartArray(seatModel);
        flipPartArray(gunModel);
        flipPartArray(gunsightModel);
        flipPartMatrix(barrelModel);
        flipPartMatrix(ammoModel);
    }

    private static void flipPartMatrix(ModelRendererTurbo[][] parts)
    {
        for (ModelRendererTurbo[] row : parts)
            flipPartArray(row);
    }

    private static void flipPartArray(ModelRendererTurbo[] parts)
    {
        if (parts == null)
            return;

        for (ModelRendererTurbo part : parts)
        {
            if (part == null)
                continue;
            part.doMirror(false, true, true);
            part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
        }
    }
}
