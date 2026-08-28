package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.MechaType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/** Extensible, pass-aware model base for legacy mecha content packs. */
@SuppressWarnings({"unused", "java:S1104"})
public class ModelMecha extends ModelDriveable
{
    public ModelRendererTurbo[] leftArmModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightArmModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftHandModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightHandModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] hipsModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] headModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] barrelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimLegUpperModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimLegUpperModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimLegLowerModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimLegLowerModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimFootModel = new ModelRendererTurbo[0];

    public Vector3f hipsAttachmentPoint = new Vector3f();
    public Vector3f legsOrigin = new Vector3f();
    public Vector3f leftLegUpperOrigin = new Vector3f();
    public Vector3f leftLegLowerOrigin = new Vector3f();
    public Vector3f rightLegUpperOrigin = new Vector3f();
    public Vector3f rightLegLowerOrigin = new Vector3f();
    public Vector3f rightFootOrigin = new Vector3f();
    public Vector3f leftFootOrigin = new Vector3f();

    @Override
    public void render(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        if (driveable.isPartIntact(EnumDriveablePart.CORE))
            super.render(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);

        boolean hipsIntact = driveable.isPartIntact(EnumDriveablePart.HIPS);

        MechaType type = driveable.getConfigType() instanceof MechaType mechaType ? mechaType : null;
        if (driveable.isPartIntact(EnumDriveablePart.HEAD))
        {
            float headYaw = state.turretYaw();
            if (type != null && type.isLimitHeadTurn())
                headYaw = Mth.clamp(headYaw, -type.getLimitHeadTurnValue(), type.getLimitHeadTurnValue());
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
            renderPart(headModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (driveable.isPartIntact(EnumDriveablePart.BARREL))
                renderWithRotation(barrelModel, 0F, 0F, -state.turretPitch() * Mth.DEG_TO_RAD,
                    poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }

        float speed = Math.min(1F, Math.abs(state.throttle()));
        float legSwingTime = type == null ? 5F : Math.max(0.01F, type.getLegSwingTime());
        float legSwingLimit = type == null ? 2F : Math.max(1.01F, type.getLegSwingLimit());
        float amplitude = Mth.clamp((float) Math.toDegrees(1F / (legSwingLimit - 1F)), 0F, 70F) * speed;
        float phase = state.animationTime() / legSwingTime;
        float leftSwing = Mth.sin(phase) * amplitude;
        float rightSwing = -leftSwing;

        if (hipsIntact)
        {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.wrapDegrees(state.legYaw() - state.yaw())));
            renderPart(hipsModel, poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);
            renderLegs(type, state, leftSwing, rightSwing, poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }

        float aimPitch = type == null ? -state.turretPitch()
            : -Mth.clamp(state.turretPitch(), -type.getUpperArmLimit(), type.getLowerArmLimit());
        if (driveable.isPartIntact(EnumDriveablePart.LEFT_ARM))
        {
            renderArm(leftArmModel, leftHandModel, type == null ? null : type.getLeftArmOrigin(),
                type == null ? null : type.getLeftHandModifier(), type == null ? 1F : type.getArmLength(),
                aimPitch, !hasAddon(driveable, EnumMechaSlotType.LEFT_TOOL), poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_ARM))
        {
            renderArm(rightArmModel, rightHandModel, type == null ? null : type.getRightArmOrigin(),
                type == null ? null : type.getRightHandModifier(), type == null ? 1F : type.getArmLength(),
                aimPitch, !hasAddon(driveable, EnumMechaSlotType.RIGHT_TOOL), poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }

        renderRegisteredGuns(driveable, state, GunMountFilter.ALL, GunYawConvention.VEHICLE,
            poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
    }

    private void renderLegs(MechaType type, RenderState state, float leftSwing, float rightSwing,
                            PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        float legLength = type == null ? 1F : Math.max(0F, type.getLegLength());
        renderSimpleLeg(leftLegModel, leftFootModel, legLength, leftSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSimpleLeg(rightLegModel, rightFootModel, legLength, rightSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        LegAnimation animation = state.legAnimation();
        renderLimb(leftAnimLegUpperModel, leftLegUpperOrigin, Axis.ZP,
            animation.angle(LegAnimation.LEFT_UPPER, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderLimb(rightAnimLegUpperModel, rightLegUpperOrigin, Axis.ZP,
            animation.angle(LegAnimation.RIGHT_UPPER, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderLimb(leftAnimLegLowerModel, leftLegLowerOrigin, Axis.ZP,
            animation.angle(LegAnimation.LEFT_LOWER, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderLimb(rightAnimLegLowerModel, rightLegLowerOrigin, Axis.ZP,
            animation.angle(LegAnimation.RIGHT_LOWER, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderLimb(leftAnimFootModel, leftFootOrigin, Axis.ZP,
            animation.angle(LegAnimation.LEFT_FOOT, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderLimb(rightAnimFootModel, rightFootOrigin, Axis.ZP,
            animation.angle(LegAnimation.RIGHT_FOOT, state.partialTick()), poseStack, vertexConsumer,
            packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderSimpleLeg(ModelRendererTurbo[] leg, ModelRendererTurbo[] foot, float legLength,
                                 float swingDegrees, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                 float scale, EnumRenderPass renderPass)
    {
        float swing = swingDegrees * Mth.DEG_TO_RAD;
        poseStack.pushPose();
        poseStack.translate(0F, legLength, 0F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(swingDegrees));
        poseStack.translate(0F, -legLength, 0F);
        renderPart(leg, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.translate(Mth.sin(swing) * legLength,
            -Mth.cos(swing) * legLength, 0F);
        renderPart(foot, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static boolean hasAddon(Driveable driveable, EnumMechaSlotType slot)
    {
        DriveableData data = driveable.getDriveableData();
        return data != null && !data.getMechaAddon(slot).isEmpty();
    }

    private void renderArm(ModelRendererTurbo[] arm, ModelRendererTurbo[] hand, Vector3f origin,
                           Vector3f handModifier, float armLength, float aimPitch, boolean renderHand,
                           PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        translateToModelPoint(poseStack, origin);
        // Legacy mecha arm models point down their local Y axis and are rotated forward from here.
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F + aimPitch));
        renderPart(arm, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        if (renderHand)
        {
            float modifierX = handModifier == null ? 0F : handModifier.x;
            float modifierY = handModifier == null ? 0F : handModifier.y;
            float modifierZ = handModifier == null ? 0F : handModifier.z;
            poseStack.translate(modifierY, -armLength - modifierX, -modifierZ);
            renderPart(hand, poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);
        }
        poseStack.popPose();
    }

    @Override
    public void render(DriveableType driveableType, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        super.render(driveableType, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderPart(hipsModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimLegUpperModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimLegUpperModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimLegLowerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimLegLowerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(headModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(barrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftArmModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightArmModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftHandModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightHandModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderLimb(ModelRendererTurbo[] parts, Vector3f origin, Axis axis, float angleDegrees,
                            PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null || parts.length == 0)
            return;
        poseStack.pushPose();
        translateToModelPoint(poseStack, origin);
        poseStack.mulPose(axis.rotationDegrees(angleDegrees));
        if (origin != null)
            poseStack.translate(-origin.x, -origin.y, origin.z);
        renderPart(parts, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderWithRotation(ModelRendererTurbo[] parts, float x, float y, float z,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;
        for (ModelRendererTurbo part : parts)
        {
            if (part == null)
                continue;
            float oldX = part.rotateAngleX;
            float oldY = part.rotateAngleY;
            float oldZ = part.rotateAngleZ;
            part.rotateAngleX = x;
            part.rotateAngleY = y;
            part.rotateAngleZ = z;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass, oldRotateOrder);
            part.rotateAngleX = oldX;
            part.rotateAngleY = oldY;
            part.rotateAngleZ = oldZ;
        }
    }

    @Override
    public void flipAll()
    {
        super.flipAll();
        flip(leftArmModel); flip(rightArmModel); flip(leftHandModel); flip(rightHandModel); flip(hipsModel);
        flip(leftLegModel); flip(rightLegModel); flip(leftFootModel); flip(rightFootModel); flip(headModel); flip(barrelModel);
        flip(leftAnimLegUpperModel); flip(rightAnimLegUpperModel); flip(leftAnimLegLowerModel); flip(rightAnimLegLowerModel);
        flip(leftAnimFootModel); flip(rightAnimFootModel);
    }

    @Override
    public void translateAll(float x, float y, float z)
    {
        super.translateAll(x, y, z);
        translate(leftArmModel, x, y, z); translate(rightArmModel, x, y, z);
        translate(leftHandModel, x, y, z); translate(rightHandModel, x, y, z); translate(hipsModel, x, y, z);
        translate(leftLegModel, x, y, z); translate(rightLegModel, x, y, z);
        translate(leftFootModel, x, y, z); translate(rightFootModel, x, y, z); translate(headModel, x, y, z);
        translate(barrelModel, x, y, z); translate(leftAnimLegUpperModel, x, y, z);
        translate(rightAnimLegUpperModel, x, y, z); translate(leftAnimLegLowerModel, x, y, z);
        translate(rightAnimLegLowerModel, x, y, z); translate(leftAnimFootModel, x, y, z);
        translate(rightAnimFootModel, x, y, z);
    }
}
