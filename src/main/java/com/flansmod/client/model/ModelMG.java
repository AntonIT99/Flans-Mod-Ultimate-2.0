package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ModelMG extends ModelBase implements IFlanTypeModel<GunType>
{
    /** Parts ending this close to the frontmost face contribute to the muzzle centre. */
    private static final double MUZZLE_FACE_TOLERANCE = 1.5D;

    @Getter @Setter
    protected GunType type;

    protected ModelRendererTurbo[] bipodModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] gunModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] ammoModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] ammoBoxModel = new ModelRendererTurbo[0];

    private boolean muzzleMeasured;
    @Nullable
    private MuzzleOriginData modelMuzzle;

    public record MuzzleOriginData(Vec3 pivot, Vec3 muzzle) {}
    private record MeasuredPart(ModelRendererTurbo part, double[] bounds) {}

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

    public void renderGun(DeployedGun mg, float pitchDeg, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        float pitch = pitchDeg * Mth.DEG_TO_RAD;

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

    /**
     * Front face of the drawn gun, pitched around the barrel part's model pivot.
     * The point is in model pixels; the entity renderer applies ModelScale and yaw.
     */
    @Nullable
    public Vec3 getModelMuzzle(float pitchDeg)
    {
        MuzzleOriginData data = getModelMuzzleOriginData();
        if (data == null)
            return null;

        Vec3 pivot = data.pivot();
        Vec3 offset = data.muzzle().subtract(pivot);
        double pitch = -pitchDeg * Mth.DEG_TO_RAD;
        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        return pivot.add(offset.x, offset.y * cos - offset.z * sin,
            offset.y * sin + offset.z * cos);
    }

    /** Level model-space pivot and muzzle, in model pixels. */
    @Nullable
    public MuzzleOriginData getModelMuzzleOriginData()
    {
        if (!muzzleMeasured)
        {
            modelMuzzle = measureModelMuzzle();
            muzzleMeasured = true;
        }
        return modelMuzzle;
    }

    /** Read rendered face bounds, including constructor-time mirrors and part pivots. */
    @Nullable
    private MuzzleOriginData measureModelMuzzle()
    {
        if (gunModel == null)
            return null;

        List<MeasuredPart> parts = new ArrayList<>();

        for (ModelRendererTurbo part : gunModel)
        {
            double[] bounds = levelBounds(part);
            if (bounds == null)
                continue;
            parts.add(new MeasuredPart(part, bounds));
        }
        if (parts.isEmpty())
            return null;

        // Completed legacy ModelMG models consistently point down local -Z.
        // flipAll() is used by old model sources to normalize their authored
        // geometry into that convention; it does not change the convention.
        double front = Double.POSITIVE_INFINITY;
        Vec3 pivot = Vec3.ZERO;
        for (MeasuredPart measured : parts)
        {
            double[] bounds = measured.bounds();
            double face = bounds[2];
            if (face < front)
            {
                front = face;
                ModelRendererTurbo part = measured.part();
                pivot = new Vec3(part.rotationPointX, part.rotationPointY, part.rotationPointZ);
            }
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (MeasuredPart measured : parts)
        {
            double[] bounds = measured.bounds();
            double face = bounds[2];
            if (Math.abs(face - front) > MUZZLE_FACE_TOLERANCE)
                continue;
            minX = Math.min(minX, bounds[0]);
            minY = Math.min(minY, bounds[1]);
            maxX = Math.max(maxX, bounds[3]);
            maxY = Math.max(maxY, bounds[4]);
        }
        return new MuzzleOriginData(pivot,
            new Vec3((minX + maxX) * 0.5D, (minY + maxY) * 0.5D, front));
    }

    private static double @Nullable [] levelBounds(@Nullable ModelRendererTurbo part)
    {
        if (part == null)
            return null;
        // renderGun replaces each part's X rotation with the current aim. Measure
        // the level pose regardless of whether this model rendered last frame.
        float previousPitch = part.rotateAngleX;
        part.rotateAngleX = 0F;
        try
        {
            return part.restBounds();
        }
        finally
        {
            part.rotateAngleX = previousPitch;
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
