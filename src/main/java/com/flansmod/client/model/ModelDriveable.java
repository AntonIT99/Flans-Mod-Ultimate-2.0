package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.DriveableType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.util.Mth;

import java.util.HashMap;

/**
 * Base for legacy driveable models.
 *
 * <p>The public fields and model-editing methods intentionally match the old
 * content-pack API. Rendering itself is stateless and receives all transient
 * values through {@link RenderState}; this keeps cached model instances safe to
 * reuse for multiple entities in the same frame.</p>
 */
@SuppressWarnings({"unused", "java:S1104"})
public class ModelDriveable extends ModelBase implements IFlanTypeModel<DriveableType>
{
    public static final float pi = (float) Math.PI;
    public static final float MODEL_SCALE = 1F / 16F;

    @Getter @Setter
    protected DriveableType type;

    public HashMap<String, ModelRendererTurbo[][]> gunModels = new HashMap<>();
    public ModelRendererTurbo[] bodyModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] bodyDoorOpenModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] bodyDoorCloseModel = new ModelRendererTurbo[0];

    /** Legacy models may opt into the original Z-Y-X part rotation order. */
    public boolean oldRotateOrder;

    @Override
    public Class<DriveableType> typeClass()
    {
        return DriveableType.class;
    }

    /**
     * Interpolated values shared by the plane, vehicle and mecha render paths.
     * Progress values are normalized to {@code [0, 1]}.
     */
    public record RenderState(
        float partialTick,
        float yaw,
        float pitch,
        float roll,
        float throttle,
        float turretYaw,
        float turretPitch,
        float wheelAngle,
        float steeringAngle,
        float animationTime,
        float gearProgress,
        float doorProgress,
        float modeProgress,
        float leftTrackProgress,
        float rightTrackProgress,
        float legSwing,
        AnimatedTransform wingTransform,
        AnimatedTransform wingWheelTransform,
        AnimatedTransform bodyWheelTransform,
        AnimatedTransform tailWheelTransform,
        AnimatedTransform doorTransform,
        AnimatedTransform door2Transform,
        LegAnimation legAnimation,
        int inputMask,
        int mode,
        boolean flareActive)
    {
        public static final RenderState ITEM = new RenderState(
            0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
            1F, 0F, 0F, 0F, 0F, 0F,
            AnimatedTransform.ZERO, AnimatedTransform.ZERO, AnimatedTransform.ZERO,
            AnimatedTransform.ZERO, AnimatedTransform.ZERO, AnimatedTransform.ZERO,
            LegAnimation.ZERO, 0, 0, false
        );
    }

    /**
     * Per-entity six-axis transform history. Legacy content specifies positions
     * in model pixels and rotations in degrees, so both are retained without
     * conversion until rendering.
     */
    public static final class AnimatedTransform
    {
        private static final int AXES = 6;
        public static final AnimatedTransform ZERO = new AnimatedTransform();

        private final float[] previous = new float[AXES];
        private final float[] current = new float[AXES];

        public void snap(Vector3f position, Vector3f rotation)
        {
            setVector(current, 0, position);
            setVector(current, 3, rotation);
            System.arraycopy(current, 0, previous, 0, AXES);
        }

        public void advance(Vector3f targetPosition, Vector3f targetRotation,
                            Vector3f positionRate, Vector3f rotationRate, int elapsedTicks)
        {
            System.arraycopy(current, 0, previous, 0, AXES);
            int elapsed = Math.max(1, elapsedTicks);
            for (int axis = 0; axis < 3; axis++)
            {
                current[axis] = approachConfigured(current[axis], value(targetPosition, axis),
                    value(positionRate, axis), elapsed);
                current[axis + 3] = approachConfigured(current[axis + 3], value(targetRotation, axis),
                    value(rotationRate, axis), elapsed);
            }
        }

        public float position(int axis, float partialTick)
        {
            return sample(Mth.clamp(axis, 0, 2), partialTick);
        }

        public float rotation(int axis, float partialTick)
        {
            return sample(Mth.clamp(axis, 0, 2) + 3, partialTick);
        }

        private float sample(int axis, float partialTick)
        {
            return Mth.lerp(Mth.clamp(partialTick, 0F, 1F), previous[axis], current[axis]);
        }

        private static float approachConfigured(float current, float target, float configuredRate, int elapsed)
        {
            float distance = Math.abs(target - current);
            if (distance <= 1.0E-5F)
                return target;
            float rate = Math.abs(configuredRate);
            if (rate <= 1.0E-5F)
                rate = Math.max(0.01F, distance * 0.16F);
            float amount = rate * elapsed;
            return current < target ? Math.min(current + amount, target) : Math.max(current - amount, target);
        }

        private static void setVector(float[] destination, int offset, Vector3f vector)
        {
            destination[offset] = value(vector, 0);
            destination[offset + 1] = value(vector, 1);
            destination[offset + 2] = value(vector, 2);
        }
    }

    /** Interpolated angles for the six configurable mecha leg joints. */
    public static final class LegAnimation
    {
        public static final int LEFT_UPPER = 0;
        public static final int LEFT_LOWER = 1;
        public static final int LEFT_FOOT = 2;
        public static final int RIGHT_UPPER = 3;
        public static final int RIGHT_LOWER = 4;
        public static final int RIGHT_FOOT = 5;
        public static final LegAnimation ZERO = new LegAnimation();

        private final float[] previous = new float[6];
        private final float[] current = new float[6];
        private final float[] target = new float[6];
        private final float[] speed = {1F, 1F, 1F, 1F, 1F, 1F};

        public void beginTick()
        {
            System.arraycopy(current, 0, previous, 0, current.length);
        }

        public void setTarget(int joint, float angle, float rate)
        {
            if (joint < 0 || joint >= current.length)
                return;
            target[joint] = angle;
            speed[joint] = Math.max(0.01F, Math.abs(rate));
        }

        public void approachTargets(int elapsedTicks)
        {
            int elapsed = Math.max(1, elapsedTicks);
            for (int joint = 0; joint < current.length; joint++)
            {
                float amount = speed[joint] * elapsed;
                current[joint] = current[joint] < target[joint]
                    ? Math.min(current[joint] + amount, target[joint])
                    : Math.max(current[joint] - amount, target[joint]);
            }
        }

        public float angle(int joint, float partialTick)
        {
            if (joint < 0 || joint >= current.length)
                return 0F;
            return Mth.lerp(Mth.clamp(partialTick, 0F, 1F), previous[joint], current[joint]);
        }
    }

    protected enum GunMountFilter
    {
        ALL,
        BODY,
        TURRET
    }

    protected enum GunYawConvention
    {
        PLANE,
        VEHICLE
    }

    /** Draw the non-animated base shared by every driveable. */
    public void render(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        renderPart(bodyModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(state.doorProgress() >= 0.5F ? bodyDoorOpenModel : bodyDoorCloseModel,
            poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    /** Draw a neutral preview used by item, GUI and item-frame renderers. */
    public void render(DriveableType driveableType, PoseStack poseStack, VertexConsumer vertexConsumer,
                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                       float scale, EnumRenderPass renderPass)
    {
        renderPart(bodyModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(bodyDoorCloseModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        for (ModelRendererTurbo[][] gun : gunModels.values())
            renderPartMatrix(gun, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void renderPart(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vertexConsumer,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                           float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;

        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                    red, green, blue, alpha, scale, renderPass, oldRotateOrder);
        }
    }

    protected void renderPartMatrix(ModelRendererTurbo[][] parts, PoseStack poseStack, VertexConsumer vertexConsumer,
                                    int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                    float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;
        for (ModelRendererTurbo[] row : parts)
            renderPart(row, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    /** Render legacy seat-gun groups: yaw, yaw+pitch, recoil and minigun rows. */
    protected void renderRegisteredGuns(Driveable driveable, RenderState state, GunMountFilter mountFilter,
                                        GunYawConvention yawConvention, PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                        float scale, EnumRenderPass renderPass)
    {
        DriveableType driveableType = driveable.getConfigType();
        if (driveableType == null || driveableType.getSeats().isEmpty() || gunModels.isEmpty())
            return;

        Seat driverSeat = driveable.getSeat(0);
        float driverYaw = interpolatedYaw(driverSeat, state.partialTick(), state.turretYaw());
        float gunScale = Math.max(0.001F, driveableType.getVehicleGunModelScale());
        for (SeatInfo seatInfo : driveableType.getSeats())
        {
            if (seatInfo == null || seatInfo.getGunName().isEmpty()
                || !driveable.isPartIntact(seatInfo.getPart()))
                continue;

            boolean turretMounted = seatInfo.getPart() == EnumDriveablePart.TURRET;
            if (mountFilter == GunMountFilter.BODY && turretMounted
                || mountFilter == GunMountFilter.TURRET && !turretMounted)
                continue;

            ModelRendererTurbo[][] gun = gunModels.get(seatInfo.getGunName());
            Seat seat = driveable.getSeat(seatInfo.getId());
            if (gun == null || seat == null)
                continue;

            float aimYaw = interpolatedYaw(seat, state.partialTick(), state.turretYaw());
            if (mountFilter == GunMountFilter.TURRET)
                aimYaw = Mth.wrapDegrees(aimYaw - driverYaw);
            float aimPitch = Mth.lerp(state.partialTick(), seat.getPrevAimPitch(), seat.getAimPitch());
            float yaw = (yawConvention == GunYawConvention.PLANE ? 180F - aimYaw : -aimYaw) * Mth.DEG_TO_RAD;
            float pitch = -aimPitch * Mth.DEG_TO_RAD;

            poseStack.pushPose();
            poseStack.scale(gunScale, gunScale, gunScale);
            float recoil = recoilOffset(driveable);
            for (int row = 0; row < gun.length; row++)
            {
                ModelRendererTurbo[] parts = gun[row];
                if (parts == null)
                    continue;
                boolean recoilingRow = row == 2 && recoil != 0F;
                if (recoilingRow)
                {
                    poseStack.pushPose();
                    poseStack.translate(recoil, 0F, 0F);
                }
                for (ModelRendererTurbo part : parts)
                {
                    if (part == null)
                        continue;
                    float oldX = part.rotateAngleX;
                    float oldY = part.rotateAngleY;
                    float oldZ = part.rotateAngleZ;
                    part.rotateAngleY = yaw;
                    if (row > 0)
                        part.rotateAngleZ = pitch;
                    if (row > 2 && seat.isInputDown(DriveableInput.PRIMARY_FIRE | DriveableInput.SECONDARY_FIRE))
                        part.rotateAngleX = state.animationTime() * 0.75F;
                    part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                        red, green, blue, alpha, scale, renderPass, oldRotateOrder);
                    part.rotateAngleX = oldX;
                    part.rotateAngleY = oldY;
                    part.rotateAngleZ = oldZ;
                }
                if (recoilingRow)
                    poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    protected static float recoilOffset(Driveable driveable)
    {
        float progress = Mth.clamp(driveable.getRecoilProgress(), 0F, 1F);
        return Mth.sin(Mth.PI * progress) * -(5F / 16F);
    }

    private static float interpolatedYaw(Seat seat, float partialTick, float fallback)
    {
        return seat == null ? fallback : Mth.rotLerp(partialTick, seat.getPrevAimYaw(), seat.getAimYaw());
    }

    protected static float value(Vector3f vector, int axis)
    {
        if (vector == null)
            return 0F;
        return switch (axis)
        {
            case 0 -> vector.x;
            case 1 -> vector.y;
            default -> vector.z;
        };
    }

    protected void renderPartAt(ModelRendererTurbo[] parts, Vector3f origin, PoseStack poseStack, VertexConsumer vertexConsumer,
                                int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        translateToModelPoint(poseStack, origin);
        renderPart(parts, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    protected static void translateToModelPoint(PoseStack poseStack, Vector3f point)
    {
        if (point != null)
            poseStack.translate(point.x, point.y, -point.z);
    }

    public void registerGunModel(String name, ModelRendererTurbo[][] gunModel)
    {
        if (name != null && gunModel != null)
            gunModels.put(name, gunModel);
    }

    protected void flip(ModelRendererTurbo[] model)
    {
        if (model == null)
            return;
        for (ModelRendererTurbo part : model)
        {
            if (part == null)
                continue;
            part.doMirror(false, true, true);
            part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
        }
    }

    protected void flip(ModelRendererTurbo[][] model)
    {
        if (model == null)
            return;
        for (ModelRendererTurbo[] row : model)
            flip(row);
    }

    public void flipAll()
    {
        flip(bodyModel);
        flip(bodyDoorOpenModel);
        flip(bodyDoorCloseModel);
        for (ModelRendererTurbo[][] gun : gunModels.values())
            flip(gun);
    }

    protected void translate(ModelRendererTurbo[] model, float x, float y, float z)
    {
        if (model == null)
            return;
        for (ModelRendererTurbo part : model)
        {
            if (part == null)
                continue;
            part.rotationPointX += x;
            part.rotationPointY += y;
            part.rotationPointZ += z;
        }
    }

    protected void translate(ModelRendererTurbo[][] model, float x, float y, float z)
    {
        if (model == null)
            return;
        for (ModelRendererTurbo[] row : model)
            translate(row, x, y, z);
    }

    public void translateAll(float x, float y, float z)
    {
        translate(bodyModel, x, y, z);
        translate(bodyDoorOpenModel, x, y, z);
        translate(bodyDoorCloseModel, x, y, z);
        for (ModelRendererTurbo[][] gun : gunModels.values())
            translate(gun, x, y, z);
    }

    public void translateAll(int x, int y, int z)
    {
        translateAll((float) x, (float) y, (float) z);
    }
}
