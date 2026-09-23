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
    public ModelRendererTurbo[] leftRearLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightRearLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftRearFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightRearFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftFrontLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightFrontLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftFrontFootModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightFrontFootModel = new ModelRendererTurbo[0];
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
        float armSpaceScale = configSpaceScale(type);
        if (driveable.isPartIntact(EnumDriveablePart.LEFT_ARM))
        {
            renderArm(leftArmModel, leftHandModel, type == null ? null : type.getLeftArmOrigin(),
                type == null ? null : type.getLeftHandModifier(), type == null ? 1F : type.getArmLength(),
                armSpaceScale, aimPitch, !hasAddon(driveable, EnumMechaSlotType.LEFT_TOOL), poseStack, vertexConsumer,
                packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (driveable.isPartIntact(EnumDriveablePart.RIGHT_ARM))
        {
            renderArm(rightArmModel, rightHandModel, type == null ? null : type.getRightArmOrigin(),
                type == null ? null : type.getRightHandModifier(), type == null ? 1F : type.getArmLength(),
                armSpaceScale, aimPitch, !hasAddon(driveable, EnumMechaSlotType.RIGHT_TOOL), poseStack, vertexConsumer,
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
        float legTrans = type == null ? 0F : type.getLegTrans();
        renderSimpleLeg(leftLegModel, leftFootModel, legTrans, legLength, leftSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSimpleLeg(rightLegModel, rightFootModel, legTrans, legLength, rightSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        // 1.12.2 multi-legged mechas: rear and front pairs swing opposite to the main pair.
        float rearLegLength = type == null ? 1F : Math.max(0F, type.getRearLegLength());
        float rearLegTrans = type == null ? 0F : type.getRearLegTrans();
        renderSimpleLeg(leftRearLegModel, leftRearFootModel, rearLegTrans, rearLegLength, rightSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSimpleLeg(rightRearLegModel, rightRearFootModel, rearLegTrans, rearLegLength, leftSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        float frontLegLength = type == null ? 1F : Math.max(0F, type.getFrontLegLength());
        float frontLegTrans = type == null ? 0F : type.getFrontLegTrans();
        renderSimpleLeg(leftFrontLegModel, leftFrontFootModel, frontLegTrans, frontLegLength, rightSwing,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSimpleLeg(rightFrontLegModel, rightFrontFootModel, frontLegTrans, frontLegLength, leftSwing,
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

    private void renderSimpleLeg(ModelRendererTurbo[] leg, ModelRendererTurbo[] foot, float legTrans, float legLength,
                                 float swingDegrees, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                 float scale, EnumRenderPass renderPass)
    {
        if ((leg == null || leg.length == 0) && (foot == null || foot.length == 0))
            return;
        float swing = swingDegrees * Mth.DEG_TO_RAD;
        poseStack.pushPose();
        poseStack.translate(legTrans, legLength, 0F);
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

    /**
     * Undoes the ModelScale the whole hierarchy is rendered under.
     *
     * <p>The legacy renderer scaled only the arm meshes, leaving ArmOrigin,
     * ArmLength and the hand modifiers in unscaled entity space, and its own
     * inventory preview divided them by ModelScale for exactly that reason.
     * Without this a mecha such as the Alpha Titan (ModelScale 2) has its arms
     * hanging in the air twice as far from the torso as authored.</p>
     */
    private static float configSpaceScale(MechaType type)
    {
        float modelScale = type == null ? 1F : type.getModelScale();
        return modelScale <= 0F ? 1F : 1F / modelScale;
    }

    private void renderArm(ModelRendererTurbo[] arm, ModelRendererTurbo[] hand, Vector3f origin,
                           Vector3f handModifier, float armLength, float configScale, float aimPitch, boolean renderHand,
                           PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        if (origin != null)
            poseStack.translate(origin.x * configScale, origin.y * configScale, -origin.z * configScale);
        // Legacy mecha arm models point down their local Y axis and are rotated forward from here.
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F + aimPitch));
        renderPart(arm, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        if (renderHand)
        {
            float modifierX = (handModifier == null ? 0F : handModifier.x) * configScale;
            float modifierY = (handModifier == null ? 0F : handModifier.y) * configScale;
            float modifierZ = (handModifier == null ? 0F : handModifier.z) * configScale;
            poseStack.translate(modifierY, -armLength * configScale - modifierX, -modifierZ);
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
        MechaType type = driveableType instanceof MechaType mechaType ? mechaType : null;
        renderPart(hipsModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftRearLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightRearLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftRearFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightRearFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFrontLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFrontLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFrontFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFrontFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimLegUpperModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimLegUpperModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimLegLowerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimLegLowerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftAnimFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightAnimFootModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(headModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(barrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        float armSpaceScale = configSpaceScale(type);
        renderArm(leftArmModel, leftHandModel, type == null ? null : type.getLeftArmOrigin(),
            type == null ? null : type.getLeftHandModifier(), type == null ? 1F : type.getArmLength(),
            armSpaceScale, 0F, true, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderArm(rightArmModel, rightHandModel, type == null ? null : type.getRightArmOrigin(),
            type == null ? null : type.getRightHandModifier(), type == null ? 1F : type.getArmLength(),
            armSpaceScale, 0F, true, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
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
        flip(leftRearLegModel); flip(rightRearLegModel); flip(leftRearFootModel); flip(rightRearFootModel);
        flip(leftFrontLegModel); flip(rightFrontLegModel); flip(leftFrontFootModel); flip(rightFrontFootModel);
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
        translate(leftRearLegModel, x, y, z); translate(rightRearLegModel, x, y, z);
        translate(leftRearFootModel, x, y, z); translate(rightRearFootModel, x, y, z);
        translate(leftFrontLegModel, x, y, z); translate(rightFrontLegModel, x, y, z);
        translate(leftFrontFootModel, x, y, z); translate(rightFrontFootModel, x, y, z);
    }
}
