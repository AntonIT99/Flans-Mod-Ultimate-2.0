package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.DriveableType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        float legYaw,
        AnimatedTransform wingTransform,
        AnimatedTransform wingWheelTransform,
        AnimatedTransform bodyWheelTransform,
        AnimatedTransform tailWheelTransform,
        AnimatedTransform doorTransform,
        AnimatedTransform door2Transform,
        LegAnimation legAnimation,
        int inputMask,
        int mode,
        boolean flareActive,
        /** Live per-link track angles, or null for static poses such as inventory renders. */
        @Nullable TrackLinkAnimation trackLinks)
    {
        public static final RenderState ITEM = new RenderState(
            0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
            1F, 0F, 0F, 0F, 0F, 0F, 0F,
            AnimatedTransform.ZERO, AnimatedTransform.ZERO, AnimatedTransform.ZERO,
            AnimatedTransform.ZERO, AnimatedTransform.ZERO, AnimatedTransform.ZERO,
            LegAnimation.ZERO, 0, 0, false, null
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

            float[] angles = registeredGunAngles(seat, state.partialTick(), state.turretYaw(),
                mountFilter == GunMountFilter.TURRET ? driverYaw : 0F, yawConvention);
            float yaw = angles[0];
            float pitch = angles[1];

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

    /**
     * Part rotation angles, in radians as {yaw, pitch}, that the renderer gives a
     * registered seat gun. {@code relativeYaw} is subtracted from the seat's aim,
     * which turret-mounted guns need because the turret already carries it.
     */
    protected static float[] registeredGunAngles(Seat seat, float partialTick, float fallbackYaw,
                                                 float relativeYaw, GunYawConvention yawConvention)
    {
        float aimYaw = Mth.wrapDegrees(interpolatedYaw(seat, partialTick, fallbackYaw) - relativeYaw);
        float aimPitch = Mth.lerp(partialTick, seat.getPrevAimPitch(), seat.getAimPitch());
        float yaw = (yawConvention == GunYawConvention.PLANE ? 180F - aimYaw : -aimYaw) * Mth.DEG_TO_RAD;
        return new float[] { yaw, -aimPitch * Mth.DEG_TO_RAD };
    }

    /**
     * Muzzle of a registered seat gun as currently drawn, in model pixels: the
     * rest-pose measurement turned by the seat's live yaw and pitch around the
     * gun's pivot, the way each part turns around its rotation point. Turret
     * guns are drawn inside the turret transform, which subclasses add.
     *
     * @return the aimed muzzle, or {@code null} when the seat has no measurable gun
     */
    @Nullable
    public Vec3 getAimedRegisteredGunMuzzle(Driveable driveable, SeatInfo seatInfo, float partialTick)
    {
        return aimRegisteredGunPoint(driveable, seatInfo, getRegisteredGunMuzzle(seatInfo.getGunName()), partialTick);
    }

    /**
     * {@link #getAimedRegisteredGunMuzzle} for every barrel of a twin or quad
     * seat gun, in model pixels; one entry for a single gun.
     */
    public List<Vec3> getAimedRegisteredGunMuzzles(Driveable driveable, SeatInfo seatInfo, float partialTick)
    {
        return getRegisteredGunMuzzles(seatInfo.getGunName(), gunModelScale()).stream()
            .map(muzzle -> aimRegisteredGunPoint(driveable, seatInfo, muzzle, partialTick))
            .toList();
    }

    @Nullable
    private Vec3 aimRegisteredGunPoint(Driveable driveable, SeatInfo seatInfo, @Nullable Vec3 muzzle, float partialTick)
    {
        Vec3 pivot = getRegisteredGunAimPivot(seatInfo.getGunName());
        Seat seat = driveable.getSeat(seatInfo.getId());
        if (muzzle == null || pivot == null || seat == null)
            return muzzle;

        boolean turretMounted = isTurretMountedGun(seatInfo);
        Seat driverSeat = driveable.getSeat(0);
        float turretYaw = driveable.getTurretYaw();
        float driverYaw = turretMounted ? interpolatedYaw(driverSeat, partialTick, turretYaw) : 0F;
        float[] angles = registeredGunAngles(seat, partialTick, turretYaw, driverYaw,
            this instanceof ModelPlane ? GunYawConvention.PLANE : GunYawConvention.VEHICLE);

        Vec3 aimed = rotatePartOffset(muzzle.subtract(pivot.scale(16D)), angles[0], angles[1]).add(pivot.scale(16D));
        return turretMounted ? toTurretPose(driveable, aimed, turretYaw) : aimed;
    }

    /** Whether the renderer draws this seat's gun inside the turret transform. */
    protected boolean isTurretMountedGun(SeatInfo seatInfo)
    {
        return false;
    }

    /** Applies the turret transform around a point drawn inside it. */
    protected Vec3 toTurretPose(Driveable driveable, Vec3 modelPixels, float turretYaw)
    {
        return modelPixels;
    }

    /** Mirrors {@code ModelRendererTurbo#translateAndRotate} for a vector relative to a rotation point. */
    private Vec3 rotatePartOffset(Vec3 offset, float yaw, float pitch)
    {
        return oldRotateOrder
            ? rotateZ(rotateY(offset, -yaw), -pitch)
            : rotateY(rotateZ(offset, pitch), yaw);
    }

    protected static Vec3 rotateY(Vec3 v, float radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }

    protected static Vec3 rotateZ(Vec3 v, float radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.y * sin, v.x * sin + v.y * cos, v.z);
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

    /**
     * Returns the model-authored pivot used by a registered passenger gun's
     * yaw and pitch rows. The constructor-time flip/translation has already
     * been applied to these rotation points, so this matches rendering.
     */
    @Nullable
    public Vec3 getRegisteredGunAimPivot(String gunName)
    {
        return getRegisteredGunAimPivot(gunName, gunModelScale());
    }

    /**
     * {@link #getRegisteredGunAimPivot(String)} at an explicit
     * {@code VehicleGunModelScale}, for callers that measure a model without
     * its type, such as offline tooling.
     */
    @Nullable
    public Vec3 getRegisteredGunAimPivot(String gunName, float gunScale)
    {
        ModelRendererTurbo[][] gun = gunModels.get(gunName);
        if (gun == null || gun.length == 0)
            return null;

        ModelRendererTurbo pivotPart = firstPart(gun.length > 1 ? gun[1] : null);
        if (pivotPart == null)
            pivotPart = firstPart(gun[0]);
        if (pivotPart == null)
            return null;

        return new Vec3(pivotPart.rotationPointX * MODEL_SCALE * gunScale,
            pivotPart.rotationPointY * MODEL_SCALE * gunScale,
            pivotPart.rotationPointZ * MODEL_SCALE * gunScale);
    }

    private static ModelRendererTurbo firstPart(ModelRendererTurbo[] parts)
    {
        if (parts == null)
            return null;
        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                return part;
        }
        return null;
    }

    /**
     * Distance, in model pixels, within which several boxes ending at about the
     * same depth are treated as one muzzle face. A muzzle brake is usually built
     * from a handful of boxes, and the single furthest one is often an asymmetric
     * corner piece; averaging the group is what keeps the result on the bore axis.
     *
     * <p>Kept tight because a gun's front sight usually stands a couple of pixels
     * behind the muzzle: the Warfare 44 Tiger's MG34 sight ends 2.3 pixels short
     * of the tip, and at 3 pixels it lifted the measured bore by half a pixel.</p>
     */
    private static final float MUZZLE_FACE_TOLERANCE = 1.5F;

    /**
     * Measures the muzzle of a group of model parts: the furthest point along the
     * model's forward axis, centred laterally and vertically on every box that
     * reaches within {@link #MUZZLE_FACE_TOLERANCE} of it.
     *
     * <p>Reads the parts as the renderer sees them, after any constructor-time
     * {@code flipAll} and {@code translateAll}, so the result is directly
     * comparable with the pivots {@link #getRegisteredGunAimPivot} returns.</p>
     *
     * @param scale factor the renderer applies to this group before drawing it
     * @return the muzzle in model pixels, or {@code null} when the group carries no geometry
     */
    @Nullable
    protected static Vec3 measureMuzzle(float scale, ModelRendererTurbo[]... groups)
    {
        return measureMuzzle(scale, (Vec3) null, groups);
    }

    /** Gap, in model pixels, below which two pieces of a muzzle face always belong to one barrel. */
    private static final double BARREL_GAP = 1D;

    /**
     * Gaps up to this many times the width of the pieces either side still join
     * them into one barrel. A muzzle brake's side plates stand a plate width or two
     * off the bore; the tubes of a twin or quad mount stand several widths apart.
     */
    private static final double BARREL_GAP_PER_WIDTH = 2.5D;

    /**
     * {@link #measureMuzzle(float, ModelRendererTurbo[]...)} restricted, when the
     * front face is split into several barrels, to the barrel nearest {@code hint}.
     * A twin mount measured whole puts its muzzle on the empty centreline between
     * its tubes. A hint that falls between barrels keeps the whole face, since that
     * is where a single point firing for all of them sits.
     *
     * @param hint point, in unscaled model pixels, whose Y and Z pick the barrel,
     *             or {@code null} to take the whole face
     */
    @Nullable
    protected static Vec3 measureMuzzle(float scale, @Nullable Vec3 hint, ModelRendererTurbo[]... groups)
    {
        MuzzleFace face = muzzleFace(groups);
        if (face == null)
            return null;
        List<double[]> parts = face.parts();
        if (hint != null)
            parts = nearestBarrel(barrels(parts, partBounds(groups)), parts, hint);
        return centre(face.forward(), union(parts), scale);
    }

    /**
     * The muzzle of every barrel ending at the front face of a group of parts, in
     * model pixels: one for a single gun, four for a quad mount.
     */
    protected static List<Vec3> measureMuzzles(float scale, ModelRendererTurbo[]... groups)
    {
        MuzzleFace face = muzzleFace(groups);
        if (face == null)
            return List.of();
        List<double[]> parts = partBounds(groups);
        List<Vec3> muzzles = new ArrayList<>();
        List<double[]> frontBarrels = new ArrayList<>();
        for (List<double[]> barrel : barrels(face.parts(), parts))
        {
            double[] rect = union(barrel);
            frontBarrels.add(rect);
            muzzles.add(centre(face.forward(), rect, scale));
        }

        // Barrels ending a little behind the front face: tube ends nothing carries on past.
        // Only a mount already showing several tubes has them; on a single gun such a
        // tube is a recoil cylinder or a sight.
        if (frontBarrels.size() < 2)
            return muzzles;
        List<double[]> ends = new ArrayList<>();
        for (double[] part : parts)
        {
            if (part[3] >= face.forward() - MUZZLE_FACE_TOLERANCE || part[3] < face.forward() - SHORTER_BARREL_REACH
                || !isTube(part) || continuesPast(part, parts))
                continue;
            ends.add(new double[] { part[1], part[2], part[4], part[5], part[3] });
        }
        for (List<double[]> barrel : barrels(ends, parts))
        {
            double[] rect = union(barrel);
            // A tube tucked right under a barrel, such as a Vickers K's gas cylinder, is part of that gun.
            if (frontBarrels.stream().anyMatch(front -> overlaps(front, rect)
                || faceDistance(front, rectCentre(rect)) < SHORTER_BARREL_CLEARANCE))
                continue;
            double end = barrel.stream().mapToDouble(piece -> piece[4]).max().orElse(face.forward());
            muzzles.add(centre((float) end, rect, scale));
        }
        return muzzles;
    }

    /**
     * How far, in model pixels, a barrel of a multi-barrel mount may end behind the
     * furthest one and still be one of its barrels: the M45 quad mount's lower pair
     * is modelled four pixels shorter than its upper pair.
     */
    private static final float SHORTER_BARREL_REACH = 6F;

    /**
     * How close, in model pixels, a shorter tube may lie to a front barrel and
     * still be a barrel of its own: the M45's lower pair stands five pixels off
     * the upper, while a Vickers K's gas cylinder lies one pixel under its barrel.
     */
    private static final double SHORTER_BARREL_CLEARANCE = 3D;

    /** Every part's bounds as {@code {minX, minY, minZ, maxX, maxY, maxZ}}, read as the muzzle face reads them. */
    private static List<double[]> partBounds(ModelRendererTurbo[]... groups)
    {
        List<double[]> bounds = new ArrayList<>();
        for (ModelRendererTurbo[] group : groups)
        {
            if (group == null)
                continue;
            for (ModelRendererTurbo part : group)
            {
                double[] b = emptyBounds();
                if (part == null || !part.appendFaceBounds(b))
                    continue;
                bounds.add(new double[] {
                    part.rotationPointX + b[0], part.rotationPointY + b[1], part.rotationPointZ + b[2],
                    part.rotationPointX + b[3], part.rotationPointY + b[4], part.rotationPointZ + b[5]
                });
            }
        }
        return bounds;
    }

    private static boolean isTube(double[] b)
    {
        double height = b[4] - b[1];
        double width = b[5] - b[2];
        return height <= TUBE_WIDTH && width <= TUBE_WIDTH && b[3] - b[0] >= Math.max(height, width);
    }

    /** Whether another part runs on through the front end of {@code tube}, making it a section rather than a muzzle. */
    private static boolean continuesPast(double[] tube, List<double[]> parts)
    {
        for (double[] other : parts)
        {
            if (other != tube && other[3] > tube[3] + 0.25D && other[0] <= tube[3] + 0.25D
                && other[1] < tube[4] && other[4] > tube[1] && other[2] < tube[5] && other[5] > tube[2])
                return true;
        }
        return false;
    }

    private static boolean overlaps(double[] a, double[] b)
    {
        return a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3];
    }

    /**
     * @param forward how far forward the furthest part reaches, in model pixels
     * @param parts   the Y and Z extents, as {@code {minY, minZ, maxY, maxZ}}, of every
     *                part reaching within {@link #MUZZLE_FACE_TOLERANCE} of it
     */
    private record MuzzleFace(float forward, List<double[]> parts) {}

    @Nullable
    private static MuzzleFace muzzleFace(ModelRendererTurbo[]... groups)
    {
        float furthestForward = Float.NEGATIVE_INFINITY;
        for (ModelRendererTurbo[] group : groups)
            furthestForward = Math.max(furthestForward, forwardEnd(group));
        if (furthestForward == Float.NEGATIVE_INFINITY)
            return null;

        float threshold = furthestForward - MUZZLE_FACE_TOLERANCE;
        List<double[]> parts = new ArrayList<>();
        for (ModelRendererTurbo[] group : groups)
            collectMuzzleFaces(group, threshold, parts);
        return parts.isEmpty() ? null : new MuzzleFace(furthestForward, parts);
    }

    private static Vec3 centre(float forward, double[] face, float scale)
    {
        return new Vec3(forward * scale, (face[0] + face[2]) * 0.5D * scale, (face[1] + face[3]) * 0.5D * scale);
    }

    private static double[] union(List<double[]> parts)
    {
        double[] face = {
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double[] part : parts)
        {
            face[0] = Math.min(face[0], part[0]);
            face[1] = Math.min(face[1], part[1]);
            face[2] = Math.max(face[2], part[2]);
            face[3] = Math.max(face[3], part[3]);
        }
        return face;
    }

    /**
     * Joins the pieces of a muzzle face into barrels, keeping the order they were found in.
     *
     * @param bodies every part of the groups the face was read from, whose gun tubes tell
     *               the pieces that end separate bores apart
     */
    private static List<List<double[]>> barrels(List<double[]> parts, List<double[]> bodies)
    {
        double[][] bores = new double[parts.size()][];
        for (int i = 0; i < bores.length; i++)
            bores[i] = bore(parts.get(i), bodies);
        int[] root = new int[parts.size()];
        for (int i = 0; i < root.length; i++)
            root[i] = i;
        for (int i = 0; i < root.length; i++)
        {
            for (int j = i + 1; j < root.length; j++)
            {
                if (sameBarrel(parts.get(i), parts.get(j)) && !separateBores(parts.get(i), bores[i], parts.get(j), bores[j]))
                    root[find(root, j)] = find(root, i);
            }
        }

        Map<Integer, List<double[]>> barrels = new LinkedHashMap<>();
        for (int i = 0; i < root.length; i++)
            barrels.computeIfAbsent(find(root, i), ignored -> new ArrayList<>()).add(parts.get(i));
        return new ArrayList<>(barrels.values());
    }

    private static int find(int[] root, int index)
    {
        int current = index;
        while (root[current] != current)
        {
            root[current] = root[root[current]];
            current = root[current];
        }
        return current;
    }

    private static boolean sameBarrel(double[] a, double[] b)
    {
        return closeEnough(a[0], a[2], b[0], b[2]) && closeEnough(a[1], a[3], b[1], b[3]);
    }

    /** A gun tube at least this long, in model pixels, behind a face piece is the bore it ends. */
    private static final double MIN_BORE_LENGTH = 8D;
    /** Bores closer than this, in model pixels, are the walls of one tube rather than two guns. */
    private static final double MIN_BORE_SPACING = 3D;

    /**
     * The Y and Z centre of the long gun tube a face piece ends, or {@code null}
     * when none runs behind it. A muzzle brake's side plates stand either side of
     * the bore and end none; each flash hider of a stacked twin ends its own.
     */
    @Nullable
    private static double[] bore(double[] piece, List<double[]> bodies)
    {
        for (double[] body : bodies)
        {
            if (!isTube(body) || body[3] - body[0] < MIN_BORE_LENGTH)
                continue;
            double y = (body[1] + body[4]) * 0.5D;
            double z = (body[2] + body[5]) * 0.5D;
            if (y >= piece[0] && y <= piece[2] && z >= piece[1] && z <= piece[3])
                return new double[] { y, z };
        }
        return null;
    }

    /**
     * Whether two face pieces end tubes of their own, standing apart, as the upper
     * and lower guns of a Flakvierling do. Their flash hiders stand as close as a
     * muzzle brake's plates, so the gap alone cannot tell them apart.
     */
    private static boolean separateBores(double[] a, @Nullable double[] boreA, double[] b, @Nullable double[] boreB)
    {
        if (boreA == null || boreB == null)
            return false;
        boolean apart = Math.max(a[0], b[0]) > Math.min(a[2], b[2]) || Math.max(a[1], b[1]) > Math.min(a[3], b[3]);
        return apart && Math.hypot(boreA[0] - boreB[0], boreA[1] - boreB[1]) >= MIN_BORE_SPACING;
    }

    private static Vec3 rectCentre(double[] rect)
    {
        return new Vec3(0D, (rect[0] + rect[2]) * 0.5D, (rect[1] + rect[3]) * 0.5D);
    }

    /** Whether two extents along one axis overlap, or stand closer than their widths allow. */
    private static boolean closeEnough(double minA, double maxA, double minB, double maxB)
    {
        double gap = Math.max(minA, minB) - Math.min(maxA, maxB);
        double width = Math.max(maxA - minA, maxB - minB);
        return gap <= Math.max(BARREL_GAP, BARREL_GAP_PER_WIDTH * width);
    }

    private static List<double[]> nearestBarrel(List<List<double[]>> barrels, List<double[]> parts, Vec3 hint)
    {
        if (barrels.size() < 2)
            return parts;

        List<double[]> nearest = parts;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (List<double[]> barrel : barrels)
        {
            double distance = faceDistance(union(barrel), hint);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = barrel;
            }
        }
        return nearestDistance > 0D && faceDistance(union(parts), hint) == 0D ? parts : nearest;
    }

    /** Distance, in the Y-Z plane, from a hint to a face rectangle; zero inside it. */
    private static double faceDistance(double[] face, Vec3 hint)
    {
        double dy = Math.max(0D, Math.max(face[0] - hint.y, hint.y - face[2]));
        double dz = Math.max(0D, Math.max(face[1] - hint.z, hint.z - face[3]));
        return Math.sqrt(dy * dy + dz * dz);
    }

    /** Rest-pose bounds of every drawn part, in model pixels, built on first use. */
    private transient List<double[]> restPartBounds;

    /**
     * Distance, in model pixels, from a point to the nearest part as the model
     * draws it at rest, or zero when the point lies inside one. Tells a point on
     * the model, such as a muzzle, from one floating beside it.
     */
    public double distanceToGeometry(Vec3 modelPixels)
    {
        double nearest = Double.POSITIVE_INFINITY;
        for (double[] b : restPartBounds())
        {
            double dx = Math.max(0D, Math.max(b[0] - modelPixels.x, modelPixels.x - b[3]));
            double dy = Math.max(0D, Math.max(b[1] - modelPixels.y, modelPixels.y - b[4]));
            double dz = Math.max(0D, Math.max(b[2] - modelPixels.z, modelPixels.z - b[5]));
            nearest = Math.min(nearest, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return nearest;
    }

    /** Widest cross-section, in model pixels, of a part still taken for a gun tube. */
    private static final double TUBE_WIDTH = 3D;

    /**
     * Distance, in model pixels, from a point to the nearest muzzle end of a gun
     * tube: a part no more than {@link #TUBE_WIDTH} across that runs along the
     * forward axis. A machine gun is modelled as such a tube, so its muzzle is
     * where one ends, while the turret or hull around it is not.
     *
     * @param forwardNegativeX whether the model faces -X, as aircraft models do
     */
    public double distanceToTubeEnd(Vec3 modelPixels, boolean forwardNegativeX)
    {
        double nearest = Double.POSITIVE_INFINITY;
        for (double[] b : restPartBounds())
        {
            double length = b[3] - b[0];
            double height = b[4] - b[1];
            double width = b[5] - b[2];
            if (height > TUBE_WIDTH || width > TUBE_WIDTH || length < Math.max(height, width))
                continue;
            Vec3 end = new Vec3(forwardNegativeX ? b[0] : b[3], (b[1] + b[4]) * 0.5D, (b[2] + b[5]) * 0.5D);
            nearest = Math.min(nearest, end.distanceTo(modelPixels));
        }
        return nearest;
    }

    private List<double[]> restPartBounds()
    {
        if (restPartBounds == null)
        {
            List<double[]> bounds = new ArrayList<>();
            forEachModelBox(box -> {
                if (box instanceof ModelRendererTurbo part)
                {
                    double[] rest = part.restBounds();
                    if (rest != null)
                        bounds.add(toRestPose(part, rest));
                }
            });
            restPartBounds = bounds;
        }
        return restPartBounds;
    }

    /**
     * Where the renderer draws a part's rest bounds, for a part it draws inside
     * a frame of its own such as a turret. Parts are drawn where they are built
     * unless a model says otherwise.
     */
    protected double[] toRestPose(ModelRendererTurbo part, double[] bounds)
    {
        return bounds;
    }

    private static float forwardEnd(ModelRendererTurbo[] parts)
    {
        float furthest = Float.NEGATIVE_INFINITY;
        if (parts == null)
            return furthest;
        for (ModelRendererTurbo part : parts)
        {
            double[] bounds = emptyBounds();
            if (part == null || !part.appendFaceBounds(bounds))
                continue;
            furthest = Math.max(furthest, (float) (part.rotationPointX + bounds[3]));
        }
        return furthest;
    }

    /** Adds the Y and Z extents, as {@code {minY, minZ, maxY, maxZ}}, of every part reaching {@code threshold}. */
    private static void collectMuzzleFaces(ModelRendererTurbo[] parts, float threshold, List<double[]> faces)
    {
        if (parts == null)
            return;
        for (ModelRendererTurbo part : parts)
        {
            double[] bounds = emptyBounds();
            if (part == null || !part.appendFaceBounds(bounds)
                || part.rotationPointX + bounds[3] < threshold)
                continue;
            faces.add(new double[] {
                part.rotationPointY + bounds[1], part.rotationPointZ + bounds[2],
                part.rotationPointY + bounds[4], part.rotationPointZ + bounds[5]
            });
        }
    }

    private static double[] emptyBounds()
    {
        return new double[] {
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
    }

    private record GunMuzzleKey(String gunName, float gunScale) {}

    /** Measurements are a full vertex walk, and diagnostics ask for them every tick. */
    private final transient HashMap<GunMuzzleKey, Optional<Vec3>> gunMuzzles = new HashMap<>();

    /**
     * Muzzle of a registered passenger gun, in model pixels, or {@code null} when
     * the gun name is not registered or its rows carry no geometry.
     */
    @Nullable
    public Vec3 getRegisteredGunMuzzle(String gunName)
    {
        return getRegisteredGunMuzzle(gunName, gunModelScale());
    }

    /**
     * {@link #getRegisteredGunMuzzle(String)} at an explicit
     * {@code VehicleGunModelScale}, for callers that measure a model without
     * its type, such as offline tooling.
     */
    @Nullable
    public Vec3 getRegisteredGunMuzzle(String gunName, float gunScale)
    {
        return gunMuzzles.computeIfAbsent(new GunMuzzleKey(gunName, gunScale), key -> {
            ModelRendererTurbo[][] gun = gunModels.get(key.gunName());
            if (gun == null || gun.length == 0)
                return Optional.empty();
            return Optional.ofNullable(measureMuzzle(key.gunScale(), gun));
        }).orElse(null);
    }

    private final transient HashMap<GunMuzzleKey, List<Vec3>> gunBarrelMuzzles = new HashMap<>();

    /**
     * The muzzle of every barrel of a registered passenger gun, in model pixels:
     * one for a single gun, two for a twin mount, none when the gun name is not
     * registered or its rows carry no geometry.
     */
    public List<Vec3> getRegisteredGunMuzzles(String gunName, float gunScale)
    {
        return gunBarrelMuzzles.computeIfAbsent(new GunMuzzleKey(gunName, gunScale), key -> {
            ModelRendererTurbo[][] gun = gunModels.get(key.gunName());
            return gun == null || gun.length == 0 ? List.of() : List.copyOf(measureMuzzles(key.gunScale(), gun));
        });
    }

    /** The scale the renderer draws registered passenger guns at. */
    private float gunModelScale()
    {
        return type == null ? 1F : Math.max(0.001F, type.getVehicleGunModelScale());
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
