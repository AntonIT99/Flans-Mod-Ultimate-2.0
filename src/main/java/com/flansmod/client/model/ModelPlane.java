package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PlaneType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/** Extensible, pass-aware model base for legacy plane content packs. */
@SuppressWarnings({"unused", "java:S1104"})
public class ModelPlane extends ModelDriveable
{
    public ModelRendererTurbo[] noseModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftWingModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightWingModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] topWingModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] bayModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] tailModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[][] propellerModels = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[] yawFlapModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] pitchFlapLeftModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] pitchFlapRightModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] pitchFlapLeftWingModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] pitchFlapRightWingModel = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] leftAnimWingModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimWingModel = new ModelRendererTurbo[0];
    public Vector3f leftWingAttach = new Vector3f();
    public Vector3f rightWingAttach = new Vector3f();

    public ModelRendererTurbo[] bodyAnimWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] tailAnimWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimWingWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimWingWheelModel = new ModelRendererTurbo[0];
    public Vector3f bodyWheelAttach = new Vector3f();
    public Vector3f tailWheelAttach = new Vector3f();
    public Vector3f leftWingWheelAttach = new Vector3f();
    public Vector3f rightWingWheelAttach = new Vector3f();

    public ModelRendererTurbo[] doorAnimModel = new ModelRendererTurbo[0];
    public Vector3f doorAttach = new Vector3f();

    public ModelRendererTurbo[][] heliMainRotorModels = new ModelRendererTurbo[0][0];
    public Vector3f[] heliMainRotorOrigins = new Vector3f[0];
    public float[] heliRotorSpeeds = new float[0];
    public ModelRendererTurbo[][] heliTailRotorModels = new ModelRendererTurbo[0][0];
    public Vector3f[] heliTailRotorOrigins = new Vector3f[0];
    public ModelRendererTurbo[] skidsModel = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] helicopterModeParts = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] planeModeParts = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] bodyWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] tailWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftWingWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightWingWheelModel = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] tailDoorOpenModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] tailDoorCloseModel = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] rightWingPos1Model = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightWingPos2Model = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftWingPos1Model = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftWingPos2Model = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] hudModel = new ModelRendererTurbo[0];

    /** Experimental multi-part animation frames retained for pack compatibility. */
    public ModelRendererTurbo[][] valkyrie = new ModelRendererTurbo[0][0];

    @Override
    public void render(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        if (driveable.isPartIntact(EnumDriveablePart.CORE))
            super.render(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);

        renderIfIntact(driveable, EnumDriveablePart.NOSE, noseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIfIntact(driveable, EnumDriveablePart.BAY, bayModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIfIntact(driveable, EnumDriveablePart.TAIL, tailModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIfIntact(driveable, EnumDriveablePart.LEFT_WING, leftWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIfIntact(driveable, EnumDriveablePart.RIGHT_WING, rightWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIfIntact(driveable, EnumDriveablePart.TOP_WING, topWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(EnumDriveablePart.TAIL))
            renderPart(state.doorProgress() >= 0.5F ? tailDoorOpenModel : tailDoorCloseModel,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        boolean gearVisible = state.gearProgress() > 0.02F;
        if (gearVisible)
        {
            renderIfIntact(driveable, EnumDriveablePart.SKIDS, skidsModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderIfIntact(driveable, EnumDriveablePart.CORE_WHEEL, bodyWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderIfIntact(driveable, EnumDriveablePart.TAIL_WHEEL, tailWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderIfIntact(driveable, EnumDriveablePart.LEFT_WING_WHEEL, leftWingWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderIfIntact(driveable, EnumDriveablePart.RIGHT_WING_WHEEL, rightWingWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        boolean alternateWingPosition = state.modeProgress() >= 0.5F;
        if (driveable.isPartIntact(EnumDriveablePart.LEFT_WING))
            renderPart(alternateWingPosition ? leftWingPos1Model : leftWingPos2Model,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_WING))
            renderPart(alternateWingPosition ? rightWingPos1Model : rightWingPos2Model,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        renderControlSurfaces(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPropellers(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderRotors(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderAnimatedParts(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(EnumDriveablePart.CORE))
            renderPart(driveable instanceof Plane plane && plane.getPlaneMode() == EnumPlaneMode.HELI
                    ? helicopterModeParts : planeModeParts,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.roll()));
        renderPart(hudModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        if (driveable.getConfigType() instanceof PlaneType planeType && planeType.isValkyrie() && valkyrie.length > 0)
        {
            int frameCount = Math.min(valkyrie.length, Math.max(1, planeType.getAnimFrames() + 1));
            int frame = Mth.clamp(Math.round(state.modeProgress() * (frameCount - 1)), 0, frameCount - 1);
            renderPart(valkyrie[frame], poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);
        }
        renderRegisteredGuns(driveable, state, GunMountFilter.ALL, GunYawConvention.PLANE,
            poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
    }

    private void renderIfIntact(Driveable driveable, EnumDriveablePart part, ModelRendererTurbo[] models,
                                PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (driveable.isPartIntact(part))
            renderPart(models, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    @Override
    public void render(DriveableType driveableType, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        super.render(driveableType, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPart(noseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartAt(leftAnimWingModel, leftWingAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartAt(rightAnimWingModel, rightWingAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPart(topWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(bayModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(tailModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(skidsModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(bodyWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(tailWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftWingWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightWingWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartAt(bodyAnimWheelModel, bodyWheelAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartAt(tailAnimWheelModel, tailWheelAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartAt(leftAnimWingWheelModel, leftWingWheelAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartAt(rightAnimWingWheelModel, rightWingWheelAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartAt(doorAnimModel, doorAttach, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPart(tailDoorCloseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(driveableType instanceof PlaneType planeType
                && (planeType.getMode() == EnumPlaneMode.HELI || planeType.getMode() == EnumPlaneMode.VTOL)
                ? helicopterModeParts : planeModeParts,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftWingPos1Model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightWingPos1Model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(hudModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(yawFlapModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(pitchFlapLeftModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(pitchFlapRightModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(pitchFlapLeftWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(pitchFlapRightWingModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPreviewPropellers(poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPartMatrix(heliMainRotorModels, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartMatrix(heliTailRotorModels, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartMatrix(valkyrie, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderPreviewPropellers(PoseStack poseStack, VertexConsumer vertexConsumer,
                                         int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                         float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo[] propeller : propellerModels)
        {
            if (propeller == null)
                continue;
            int count = Math.max(1, propeller.length);
            for (int blade = 0; blade < propeller.length; blade++)
                renderWithRotation(propeller[blade], RotationAxis.X, blade * Mth.TWO_PI / count,
                    poseStack, vertexConsumer, packedLight, packedOverlay,
                    red, green, blue, alpha, scale, renderPass);
        }
    }

    private void renderControlSurfaces(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                       float scale, EnumRenderPass renderPass)
    {
        float yawControl = driveable instanceof Plane plane
            ? Mth.lerp(state.partialTick(), plane.getPrevFlapYaw(), plane.getFlapYaw()) * Mth.DEG_TO_RAD
            : Mth.clamp(state.steeringAngle(), -30F, 30F) * Mth.DEG_TO_RAD;
        float leftPitch = driveable instanceof Plane plane
            ? Mth.lerp(state.partialTick(), plane.getPrevFlapPitchLeft(), plane.getFlapPitchLeft()) * Mth.DEG_TO_RAD : 0F;
        float rightPitch = driveable instanceof Plane plane
            ? Mth.lerp(state.partialTick(), plane.getPrevFlapPitchRight(), plane.getFlapPitchRight()) * Mth.DEG_TO_RAD : 0F;
        if (driveable.isPartIntact(EnumDriveablePart.TAIL))
        {
            renderWithRotationOffset(yawFlapModel, RotationAxis.Y, yawControl, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderWithRotationOffset(pitchFlapLeftModel, RotationAxis.Z, leftPitch, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderWithRotationOffset(pitchFlapRightModel, RotationAxis.Z, rightPitch, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (driveable.isPartIntact(EnumDriveablePart.LEFT_WING))
            renderWithRotationOffset(pitchFlapLeftWingModel, RotationAxis.Z, leftPitch, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_WING))
            renderWithRotationOffset(pitchFlapRightWingModel, RotationAxis.Z, rightPitch, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderPropellers(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                  float scale, EnumRenderPass renderPass)
    {
        float rotation = driveable instanceof Plane plane
            ? Mth.rotLerp(state.partialTick(), plane.getPrevPropellerAngle(), plane.getPropellerAngle()) * Mth.DEG_TO_RAD
            : state.animationTime() * (0.35F + Math.abs(state.throttle()) * 1.8F);
        PlaneType type = driveable.getConfigType() instanceof PlaneType planeType ? planeType : null;
        for (int propellerIndex = 0; propellerIndex < propellerModels.length; propellerIndex++)
        {
            ModelRendererTurbo[] propeller = propellerModels[propellerIndex];
            if (propeller == null)
                continue;
            if (type != null && propellerIndex < type.getPropellers().size()
                && !driveable.isPartIntact(type.getPropellers().get(propellerIndex).getPlanePart()))
                continue;
            int count = Math.max(1, propeller.length);
            for (int blade = 0; blade < propeller.length; blade++)
            {
                renderWithRotation(propeller[blade], RotationAxis.X,
                    rotation + blade * Mth.TWO_PI / count, poseStack, vertexConsumer,
                    packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            }
        }
    }

    private void renderRotors(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                              float scale, EnumRenderPass renderPass)
    {
        PlaneType type = driveable.getConfigType() instanceof PlaneType planeType ? planeType : null;
        float rotorAngle = driveable instanceof Plane plane
            ? Mth.rotLerp(state.partialTick(), plane.getPrevPropellerAngle(), plane.getPropellerAngle())
            : state.animationTime() * (18F + 34F * Math.abs(state.throttle()));
        for (int i = 0; i < heliMainRotorModels.length; i++)
        {
            if (type != null && i < type.getHeliPropellers().size()
                && !driveable.isPartIntact(type.getHeliPropellers().get(i).getPlanePart()))
                continue;
            float speed = i < heliRotorSpeeds.length ? heliRotorSpeeds[i] : 1F;
            renderAround(heliMainRotorModels[i], vectorAt(heliMainRotorOrigins, i), Axis.YP,
                rotorAngle * speed, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        for (int i = 0; i < heliTailRotorModels.length; i++)
        {
            if (type != null && i < type.getHeliTailPropellers().size()
                && !driveable.isPartIntact(type.getHeliTailPropellers().get(i).getPlanePart()))
                continue;
            renderAround(heliTailRotorModels[i], vectorAt(heliTailRotorOrigins, i), Axis.ZP,
                rotorAngle, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    private void renderAnimatedParts(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                                     int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                     float scale, EnumRenderPass renderPass)
    {
        if (!(driveable.getConfigType() instanceof PlaneType type))
            return;

        if (driveable.isPartIntact(EnumDriveablePart.LEFT_WING))
            renderConfigured(leftAnimWingModel, leftWingAttach, state.wingTransform(), state.partialTick(), false, true,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_WING))
            renderConfigured(rightAnimWingModel, rightWingAttach, state.wingTransform(), state.partialTick(), true, true,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(EnumDriveablePart.CORE_WHEEL))
            renderConfigured(bodyAnimWheelModel, bodyWheelAttach, state.bodyWheelTransform(), state.partialTick(), false, false,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.TAIL_WHEEL))
            renderConfigured(tailAnimWheelModel, tailWheelAttach, state.tailWheelTransform(), state.partialTick(), false, false,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.LEFT_WING_WHEEL))
            renderConfigured(leftAnimWingWheelModel, leftWingWheelAttach, state.wingWheelTransform(), state.partialTick(), false, true,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_WING_WHEEL))
            renderConfigured(rightAnimWingWheelModel, rightWingWheelAttach, state.wingWheelTransform(), state.partialTick(), true, true,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.CORE))
            renderConfigured(doorAnimModel, doorAttach, state.doorTransform(), state.partialTick(), false, false,
                poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderConfigured(ModelRendererTurbo[] parts, Vector3f attachment, AnimatedTransform transform,
                                  float partialTick, boolean mirrorXYRotation, boolean invertAttachmentZ,
                                  PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null || parts.length == 0)
            return;

        float x = value(attachment, 0) + transform.position(0, partialTick) * MODEL_SCALE;
        float y = value(attachment, 1) + transform.position(1, partialTick) * MODEL_SCALE;
        float z = (invertAttachmentZ ? -value(attachment, 2) : value(attachment, 2))
            + transform.position(2, partialTick) * MODEL_SCALE;
        float rotationSign = mirrorXYRotation ? -1F : 1F;
        float rotationX = transform.rotation(0, partialTick) * rotationSign;
        float rotationY = transform.rotation(1, partialTick) * rotationSign;
        float rotationZ = transform.rotation(2, partialTick);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));
        renderPart(parts, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderAround(ModelRendererTurbo[] parts, Vector3f origin, Axis axis, float angleDegrees,
                              PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null || parts.length == 0)
            return;
        poseStack.pushPose();
        if (origin != null)
            poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(axis.rotationDegrees(angleDegrees));
        if (origin != null)
            poseStack.translate(-origin.x, -origin.y, -origin.z);
        renderPart(parts, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderWithRotation(ModelRendererTurbo[] parts, RotationAxis axis, float angle,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        renderWithRotation(parts, axis, angle, false, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
    }

    private void renderWithRotationOffset(ModelRendererTurbo[] parts, RotationAxis axis, float angle,
                                          PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                          float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        renderWithRotation(parts, axis, angle, true, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
    }

    private void renderWithRotation(ModelRendererTurbo[] parts, RotationAxis axis, float angle, boolean additive,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;
        for (ModelRendererTurbo part : parts)
        {
            if (part == null)
                continue;
            float oldAngle = axis.get(part);
            axis.set(part, additive ? oldAngle + angle : angle);
            try
            {
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                    red, green, blue, alpha, scale, renderPass, oldRotateOrder);
            }
            finally
            {
                axis.set(part, oldAngle);
            }
        }
    }

    private void renderWithRotation(ModelRendererTurbo part, RotationAxis axis, float angle,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (part == null)
            return;
        float oldAngle = axis.get(part);
        axis.set(part, angle);
        try
        {
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass, oldRotateOrder);
        }
        finally
        {
            axis.set(part, oldAngle);
        }
    }

    private enum RotationAxis
    {
        X
        {
            @Override float get(ModelRendererTurbo part) { return part.rotateAngleX; }
            @Override void set(ModelRendererTurbo part, float angle) { part.rotateAngleX = angle; }
        },
        Y
        {
            @Override float get(ModelRendererTurbo part) { return part.rotateAngleY; }
            @Override void set(ModelRendererTurbo part, float angle) { part.rotateAngleY = angle; }
        },
        Z
        {
            @Override float get(ModelRendererTurbo part) { return part.rotateAngleZ; }
            @Override void set(ModelRendererTurbo part, float angle) { part.rotateAngleZ = angle; }
        };

        abstract float get(ModelRendererTurbo part);
        abstract void set(ModelRendererTurbo part, float angle);
    }

    private static Vector3f vectorAt(Vector3f[] vectors, int index)
    {
        return vectors != null && index >= 0 && index < vectors.length && vectors[index] != null
            ? vectors[index] : null;
    }

    public void renderValk(Driveable plane, RenderState state, int id, PoseStack poseStack, VertexConsumer vertexConsumer,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                           float scale, EnumRenderPass renderPass)
    {
        if (id >= 0 && id < valkyrie.length)
            renderPart(valkyrie[id], poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    @Override
    public void flipAll()
    {
        super.flipAll();
        flip(noseModel); flip(leftWingModel); flip(rightWingModel); flip(topWingModel); flip(bayModel); flip(tailModel);
        flip(yawFlapModel); flip(skidsModel); flip(helicopterModeParts); flip(planeModeParts);
        flip(pitchFlapLeftModel); flip(pitchFlapRightModel); flip(pitchFlapLeftWingModel); flip(pitchFlapRightWingModel);
        flip(bodyWheelModel); flip(tailWheelModel); flip(leftWingWheelModel); flip(rightWingWheelModel);
        flip(tailDoorOpenModel); flip(tailDoorCloseModel); flip(rightWingPos1Model); flip(rightWingPos2Model);
        flip(leftWingPos1Model); flip(leftWingPos2Model); flip(hudModel); flip(leftAnimWingModel); flip(rightAnimWingModel);
        flip(bodyAnimWheelModel); flip(tailAnimWheelModel); flip(leftAnimWingWheelModel); flip(rightAnimWingWheelModel);
        flip(doorAnimModel); flip(valkyrie); flip(propellerModels); flip(heliMainRotorModels); flip(heliTailRotorModels);
    }

    @Override
    public void translateAll(float x, float y, float z)
    {
        super.translateAll(x, y, z);
        translate(noseModel, x, y, z); translate(leftWingModel, x, y, z); translate(rightWingModel, x, y, z);
        translate(topWingModel, x, y, z); translate(bayModel, x, y, z); translate(tailModel, x, y, z);
        translate(yawFlapModel, x, y, z); translate(skidsModel, x, y, z); translate(helicopterModeParts, x, y, z);
        translate(planeModeParts, x, y, z); translate(pitchFlapLeftModel, x, y, z); translate(pitchFlapRightModel, x, y, z);
        translate(pitchFlapLeftWingModel, x, y, z); translate(pitchFlapRightWingModel, x, y, z);
        translate(bodyWheelModel, x, y, z); translate(tailWheelModel, x, y, z);
        translate(leftWingWheelModel, x, y, z); translate(rightWingWheelModel, x, y, z);
        translate(tailDoorOpenModel, x, y, z); translate(tailDoorCloseModel, x, y, z);
        translate(rightWingPos1Model, x, y, z); translate(rightWingPos2Model, x, y, z);
        translate(leftWingPos1Model, x, y, z); translate(leftWingPos2Model, x, y, z); translate(hudModel, x, y, z);
        translate(leftAnimWingModel, x, y, z); translate(rightAnimWingModel, x, y, z);
        translate(bodyAnimWheelModel, x, y, z); translate(tailAnimWheelModel, x, y, z);
        translate(leftAnimWingWheelModel, x, y, z); translate(rightAnimWingWheelModel, x, y, z);
        translate(doorAnimModel, x, y, z); translate(valkyrie, x, y, z); translate(propellerModels, x, y, z);
        translate(heliMainRotorModels, x, y, z); translate(heliTailRotorModels, x, y, z);

        translateOrigins(heliMainRotorOrigins, x, y, z);
        translateOrigins(heliTailRotorOrigins, x, y, z);
    }

    private static void translateOrigins(Vector3f[] origins, float x, float y, float z)
    {
        if (origins == null)
            return;
        for (Vector3f origin : origins)
        {
            if (origin != null)
            {
                origin.x += x * MODEL_SCALE;
                origin.y += y * MODEL_SCALE;
                origin.z += z * MODEL_SCALE;
            }
        }
    }
}
