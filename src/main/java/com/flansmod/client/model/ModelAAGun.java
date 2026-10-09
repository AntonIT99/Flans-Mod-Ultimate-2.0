package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ModelAAGun extends ModelBase implements IFlanTypeModel<AAGunType>
{
    public record BarrelOriginData(Vec3[] pivots, Vec3[] muzzles)
    {}

    /** Parts ending this close behind the furthest one form the muzzle face, in model pixels. */
    private static final double MUZZLE_FACE_TOLERANCE = 1.5D;

    @Getter @Setter
    protected AAGunType type;

    public boolean oldModel = false;

    public ModelRendererTurbo[] baseModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] seatModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] gunModel = new ModelRendererTurbo[0];
    // Some legacy pack models fill inherited barrel rows without allocating the outer array.
    // Empty rows stay null until a model supplies geometry for them.
    public ModelRendererTurbo[][] barrelModel = new ModelRendererTurbo[AAGunType.MAX_BARRELS][];
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

    public void renderBase(AAGun aa, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        renderParts(baseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void renderGun(AAGun aa, float gunPitch, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
        float scale, EnumRenderPass renderPass)
    {
        float pitch = -gunPitch * Mth.DEG_TO_RAD;

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
            float x = -barrelRecoil * Mth.cos(pitch) + barrelX;
            float y = -barrelRecoil * Mth.sin(pitch) + barrelY;
            renderBarrelPartArray(barrelModel[i], x, y, barrelZ, pitch, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        for (int i = 0; i < ammoModel.length; i++)
        {
            if (!aa.hasAmmo(i))
                continue;
            renderBarrelPartArray(ammoModel[i], barrelX, barrelY, barrelZ, pitch, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    /** Draw a neutral preview, with the gun level and every barrel loaded, for renderers without an AA gun entity. */
    public void render(AAGunType aaGunType, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        renderParts(baseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderParts(seatModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderBarrelPartArray(gunModel, barrelX, barrelY, barrelZ, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        for (ModelRendererTurbo part : gunsightModel)
        {
            if (part == null)
                continue;
            part.rotateAngleZ = 0F;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        for (ModelRendererTurbo[] barrel : barrelModel)
            renderBarrelPartArray(barrel, barrelX, barrelY, barrelZ, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        for (ModelRendererTurbo[] ammo : ammoModel)
            renderBarrelPartArray(ammo, barrelX, barrelY, barrelZ, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private static void renderParts(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
        float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    private static void renderBarrelPartArray(ModelRendererTurbo[] parts, float x, float y, float z, float pitch, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
        int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
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
        return getModelBarrelOriginData(type.getNumBarrels());
    }

    /** {@link #getModelBarrelOriginData(AAGunType)} for a barrel count read without the type. */
    public BarrelOriginData getModelBarrelOriginData(int count)
    {
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

    /**
     * The centre of a barrel's front face, relative to the barrel pivot every
     * barrel part is drawn at.
     *
     * <p>
     * Reads face bounds, not vertex bounds: {@link #flipAll} mirrors the faces
     * and leaves each part's vertex array where it was built, so vertex bounds
     * put the muzzle of a flipped model on the wrong side and height. Only the
     * parts reaching the front face count, as in
     * {@link ModelDriveable#measureMuzzle}, so a breech, cradle or cooling jacket
     * wider than the bore does not pull the muzzle off the barrel's axis.
     * </p>
     */
    @Nullable
    static Vec3 findMuzzlePoint(ModelRendererTurbo[] parts)
    {
        if (parts == null || parts.length == 0)
            return null;

        double furthestForward = Double.NEGATIVE_INFINITY;
        for (ModelRendererTurbo part : parts)
        {
            double[] bounds = emptyBounds();
            if (part != null && part.appendFaceBounds(bounds))
                furthestForward = Math.max(furthestForward, bounds[3]);
        }
        if (furthestForward == Double.NEGATIVE_INFINITY)
            return null;

        double threshold = furthestForward - MUZZLE_FACE_TOLERANCE;
        double[] face = emptyBounds();
        for (ModelRendererTurbo part : parts)
        {
            double[] bounds = emptyBounds();
            if (part == null || !part.appendFaceBounds(bounds) || bounds[3] < threshold)
                continue;
            face[1] = Math.min(face[1], bounds[1]);
            face[2] = Math.min(face[2], bounds[2]);
            face[4] = Math.max(face[4], bounds[4]);
            face[5] = Math.max(face[5], bounds[5]);
        }

        return new Vec3(furthestForward, (face[1] + face[4]) * 0.5D, (face[2] + face[5]) * 0.5D);
    }

    private static double[] emptyBounds()
    {
        return new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
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
