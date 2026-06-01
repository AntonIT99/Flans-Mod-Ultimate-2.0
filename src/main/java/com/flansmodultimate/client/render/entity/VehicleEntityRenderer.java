package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.entity.VehicleEntity;
import com.flansmodultimate.common.types.VehicleType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class VehicleEntityRenderer extends EntityRenderer<VehicleEntity>
{
    public VehicleEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity entity)
    {
        VehicleType type = entity.getConfigType();
        if (type == null) return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/vehicles/missing.png");
        return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/vehicles/" + type.getShortName() + ".png");
    }

    @Override
    public void render(@NotNull VehicleEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight)
    {
        VehicleType type = entity.getConfigType();
        if (type == null) return;

        IModelBase model = ModelCache.getOrLoadTypeModel(type);
        if (!(model instanceof ModelVehicle modelVehicle)) return;

        ResourceLocation texture = getTextureLocation(entity);

        int color = type.getColour();
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;

        poseStack.pushPose();

        LegacyTransformApplier.applyTransforms(poseStack, model.getClass().getName());

        poseStack.mulPose(Axis.YP.rotationDegrees(180F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-entity.getVehicleRoll()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVehiclePitch()));

        float modelScale = type.getModelScale();
        poseStack.scale(modelScale, modelScale, modelScale);

        float wheelsAngle = entity.getWheelsAngle();
        float wheelsYaw = entity.getWheelsYaw();

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            VertexConsumer vc = buffer.getBuffer(renderPass.getRenderType(texture));
            float scale = modelVehicle.getScale();

            renderParts(modelVehicle.bodyModel, poseStack, vc, packedLight, r, g, b, scale, renderPass);

            renderWheelParts(modelVehicle.leftBackWheelModel, poseStack, vc, packedLight, r, g, b, scale, renderPass,
                -wheelsAngle, 0);
            renderWheelParts(modelVehicle.rightBackWheelModel, poseStack, vc, packedLight, r, g, b, scale, renderPass,
                -wheelsAngle, 0);
            renderWheelParts(modelVehicle.leftFrontWheelModel, poseStack, vc, packedLight, r, g, b, scale, renderPass,
                -wheelsAngle, -wheelsYaw * 3.14159265F / 180F * 3F);
            renderWheelParts(modelVehicle.rightFrontWheelModel, poseStack, vc, packedLight, r, g, b, scale, renderPass,
                -wheelsAngle, -wheelsYaw * 3.14159265F / 180F * 3F);

            renderParts(modelVehicle.steeringWheelModel, poseStack, vc, packedLight, r, g, b, scale, renderPass);
        }

        renderTurretAndBarrel(entity, modelVehicle, texture, poseStack, buffer, packedLight, partialTicks);

        poseStack.popPose();
    }

    private void renderTurretAndBarrel(VehicleEntity entity, ModelVehicle modelVehicle, ResourceLocation texture,
                                        PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks)
    {
        VehicleType type = entity.getConfigType();
        if (type == null) return;

        Entity driver = entity.getFirstPassenger();
        if (driver == null) return;

        float driverYaw = driver.getYHeadRot();
        float driverPitch = driver.getXRot();

        float relativeYaw = driverYaw - entity.getYRot();

        int color = type.getColour();
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;

        poseStack.pushPose();

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            VertexConsumer vc = buffer.getBuffer(renderPass.getRenderType(texture));
            float scale = modelVehicle.getScale();

            if (modelVehicle.turretModel != null)
            {
                for (ModelRendererTurbo part : modelVehicle.turretModel)
                {
                    part.rotateAngleY = -relativeYaw * 3.14159265F / 180F;
                    part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1F, scale, renderPass);
                    part.rotateAngleY = 0;
                }
            }

            if (modelVehicle.barrelModel != null)
            {
                for (ModelRendererTurbo part : modelVehicle.barrelModel)
                {
                    part.rotateAngleZ = -driverPitch * 3.14159265F / 180F;
                    part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1F, scale, renderPass);
                    part.rotateAngleZ = 0;
                }
            }
        }

        poseStack.popPose();
    }

    private void renderParts(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vc,
                              int packedLight, float r, float g, float b, float scale, EnumRenderPass renderPass)
    {
        if (parts == null) return;
        for (ModelRendererTurbo part : parts)
            part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1F, scale, renderPass);
    }

    private void renderWheelParts(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vc,
                                   int packedLight, float r, float g, float b, float scale, EnumRenderPass renderPass,
                                   float rotZ, float rotY)
    {
        if (parts == null) return;
        for (ModelRendererTurbo part : parts)
        {
            if (rotZ != 0) part.rotateAngleZ = rotZ;
            if (rotY != 0) part.rotateAngleY = rotY;
            part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1F, scale, renderPass);
            if (rotZ != 0) part.rotateAngleZ = 0;
            if (rotY != 0) part.rotateAngleY = 0;
        }
    }
}