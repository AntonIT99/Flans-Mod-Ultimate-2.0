package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.physics.TrackAnimationPhysics;
import com.flansmodultimate.common.driveables.weapons.ShootPoint;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.VehicleType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Extensible, pass-aware model base for legacy ground vehicles. */
@SuppressWarnings({"unused", "java:S1104"})
public class ModelVehicle extends ModelDriveable
{
    public ModelRendererTurbo[] turretModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] barrelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[][] ammoModel = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[] frontWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] backWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftFrontWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightFrontWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftBackWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightBackWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightTrackModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftTrackModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightTrackWheelModels = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftTrackWheelModels = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] leftFrontLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightFrontLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftBackLegModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightBackLegModel = new ModelRendererTurbo[0];

    public ModelRendererTurbo[][] leftAnimTrackModel = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[][] rightAnimTrackModel = new ModelRendererTurbo[0][0];
    public ModelRendererTurbo[] fancyTrackModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimTrackModel1 = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimTrackModel1 = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimTrackModel2 = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimTrackModel2 = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] rightAnimTrackModel3 = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] leftAnimTrackModel3 = new ModelRendererTurbo[0];

    public ModelRendererTurbo[] trailerModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] steeringWheelModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] drillHeadModel = new ModelRendererTurbo[0];
    public Vector3f drillHeadOrigin = new Vector3f();
    public ModelRendererTurbo[] barrelSpecModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] animBarrelModel = new ModelRendererTurbo[0];
    public Vector3f barrelAttach = new Vector3f();

    public ModelRendererTurbo[] doorAnimModel = new ModelRendererTurbo[0];
    public Vector3f doorAttach = new Vector3f();
    public ModelRendererTurbo[] door2AnimModel = new ModelRendererTurbo[0];
    public Vector3f door2Attach = new Vector3f();

    public ModelRendererTurbo[] drakonModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] drakonReloadModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] drakonArmModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] drakonRailModel = new ModelRendererTurbo[0];
    public ModelRendererTurbo[] drakonDoorModel = new ModelRendererTurbo[0];
    public Vector3f drakonArmAttach = new Vector3f();
    public Vector3f drakonRailAttach = new Vector3f();
    public Vector3f drakonDoorAttach = new Vector3f();

    public float animFrameLeft;
    public float animFrameRight;
    public Vector3f turretScale = new Vector3f(1F, 1F, 1F);
    public Vector3f turretTrans = new Vector3f();
    public boolean fancyTurret;
    public String turretName;

    public float legMoveSpeed = 1F;
    public float legMaxMove = 1F;
    public float legSteerAmount = 1F;
    public boolean legSpeedChange = true;

    private DriveableType trackPathType;
    @Nullable
    private DriveableType trackSideType;
    private boolean trackMeshSidesSwapped;
    private boolean trackPathSidesSwapped;
    private TrackPath leftTrackPath = TrackPath.EMPTY;
    private TrackPath rightTrackPath = TrackPath.EMPTY;
    private TrackLinkLod trackLinkLod;
    private float trackPathRadius;
    private boolean barrelPitchPivotResolved;
    @Nullable
    private Vec3 primaryBarrelPitchPivot;
    private boolean barrelMuzzleResolved;
    @Nullable
    private Vec3 primaryBarrelMuzzle;

    /** Called before world part culling begins, so the derived mesh contains the complete link. */
    public boolean selectTrackLinkLod(DriveableType type, float projectionPixels, double distance, float modelScale, float threshold, boolean previous)
    {
        return selectTrackLinkGroup(type, projectionPixels, distance, modelScale, threshold, 0F, previous ? 1 : 0) > 0;
    }

    public int selectTrackLinkGroup(DriveableType type, float projectionPixels, double distance, float modelScale, float threshold, float groupingThreshold, int previousGroup)
    {
        if (distance < 32D || threshold <= 0F || !ensureTrackLinkLod(type))
            return 0;
        ensureTrackPaths(type);
        return trackLinkLod.selectGroup(projectionPixels, distance - trackPathRadius * Math.abs(modelScale), modelScale, threshold, groupingThreshold, previousGroup);
    }

    /**
     * Whether this model's fancy track links have single-link envelopes, deriving them if needed.
     * Called before world part culling begins, so the derived mesh contains the complete link.
     */
    public boolean hasTrackLinkEnvelopes(DriveableType type)
    {
        return ensureTrackLinkLod(type) && trackLinkLod.parts() != null;
    }

    private boolean ensureTrackLinkLod(DriveableType type)
    {
        if (fancyTrackModel == null || fancyTrackModel.length < 2)
            return false;
        if (trackLinkLod == null || !trackLinkLod.matches(fancyTrackModel, oldRotateOrder, type.getTrackLinkLength()))
            trackLinkLod = TrackLinkLod.create(fancyTrackModel, oldRotateOrder, type.getTrackLinkLength());
        return true;
    }

    /**
     * Finds the pitch pivot of the barrel section that reaches furthest along
     * the vehicle model's forward axis. ModelRendererTurbo stores vertices
     * relative to that pivot, so this mirrors the actual render transform.
     */
    @Nullable
    public Vec3 getPrimaryBarrelPitchPivot()
    {
        if (barrelPitchPivotResolved)
            return primaryBarrelPitchPivot;
        barrelPitchPivotResolved = true;

        ModelRendererTurbo bestPart = null;
        double furthestForward = Double.NEGATIVE_INFINITY;
        if (barrelModel != null)
        {
            for (ModelRendererTurbo part : barrelModel)
            {
                if (part == null)
                    continue;
                double[] bounds = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY};
                if (!part.appendVertexBounds(bounds))
                    continue;
                double forwardEnd = part.rotationPointX + bounds[3];
                if (forwardEnd > furthestForward)
                {
                    furthestForward = forwardEnd;
                    bestPart = part;
                }
            }
        }
        if (bestPart != null)
            primaryBarrelPitchPivot = new Vec3(bestPart.rotationPointX / 16D, bestPart.rotationPointY / 16D, bestPart.rotationPointZ / 16D);
        else if ((barrelSpecModel != null && barrelSpecModel.length > 0) || (animBarrelModel != null && animBarrelModel.length > 0))
            // Same model-point convention as the rotation points above: the
            // renderer applies barrelAttach with its Z negated (translateToModelPoint).
            primaryBarrelPitchPivot = new Vec3(barrelAttach.x, barrelAttach.y, -barrelAttach.z);
        return primaryBarrelPitchPivot;
    }

    /**
     * How far, in model pixels, a shoot point may lie from a barrel section's
     * geometry and still be taken as built on it. A mounted gun's point is
     * normally placed on or just in front of its own boxes.
     */
    private static final double SHOOT_POINT_PART_TOLERANCE = 3D;

    /** Rendered bounds of each barrel section, [part][minX minY minZ maxX maxY maxZ]; filled on first use. */
    private double[][] barrelPartBounds;

    /**
     * Pitch pivot of the barrel section a point is built on, in the units of
     * {@link #getPrimaryBarrelPitchPivot}. The renderer pitches every barrel
     * section around its own rotation point, so a machine gun modelled on the
     * turret roof tilts in place rather than round the main gun's trunnion.
     *
     * @param modelPixels
     *            the point in model pixels, before ModelScale
     * @return the section's pivot, or {@code null} when no barrel section lies near the point
     */
    @Nullable
    public Vec3 getBarrelPitchPivotNear(Vec3 modelPixels)
    {
        if (barrelModel == null)
            return null;
        if (barrelPartBounds == null)
        {
            double[][] bounds = new double[barrelModel.length][];
            for (int index = 0; index < barrelModel.length; index++)
            {
                ModelRendererTurbo part = barrelModel[index];
                double[] box = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
                if (part != null && part.appendFaceBounds(box))
                    bounds[index] = new double[]{part.rotationPointX + box[0], part.rotationPointY + box[1], part.rotationPointZ + box[2], part.rotationPointX + box[3], part.rotationPointY + box[4],
                        part.rotationPointZ + box[5]};
            }
            barrelPartBounds = bounds;
        }

        ModelRendererTurbo nearest = getNearest(modelPixels);
        return nearest == null ? null : new Vec3(nearest.rotationPointX / 16D, nearest.rotationPointY / 16D, nearest.rotationPointZ / 16D);
    }

    @Nullable
    private ModelRendererTurbo getNearest(Vec3 modelPixels)
    {
        ModelRendererTurbo nearest = null;
        double nearestDistance = SHOOT_POINT_PART_TOLERANCE;
        for (int index = 0; index < barrelModel.length; index++)
        {
            double[] box = barrelPartBounds[index];
            if (box == null)
                continue;
            double dx = Math.max(0D, Math.max(box[0] - modelPixels.x, modelPixels.x - box[3]));
            double dy = Math.max(0D, Math.max(box[1] - modelPixels.y, modelPixels.y - box[4]));
            double dz = Math.max(0D, Math.max(box[2] - modelPixels.z, modelPixels.z - box[5]));
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= nearestDistance)
            {
                nearestDistance = distance;
                nearest = barrelModel[index];
            }
        }
        return nearest;
    }

    /**
     * {@link #getBarrelPitchPivotNear} for each point of one weapon bank, in bank
     * order, or {@code null} for a point not on the turret or not near a barrel section.
     */
    public Vec3[] getShootPointPitchPivots(DriveableType type, boolean secondary)
    {
        List<ShootPoint> points = type.shootPoints(secondary);
        Vec3[] pivots = new Vec3[points.size()];
        double modelScale = Math.max(1.0E-4D, type.getModelScale());
        for (int index = 0; index < pivots.length; index++)
        {
            ShootPoint point = points.get(index);
            if (!EnumDriveablePart.isTurretMounted(point.getRootPos().getPart()))
                continue;
            Vector3f position = point.getRootPos().getPosition();
            Vector3f offset = point.getOffPos();
            // A ground vehicle's type-file point and its geometry share one frame
            // (LegacyDriveableCoordinates.modelPixelsToTypeFile), but the points are
            // authored at rendered size and the geometry is before ModelScale.
            pivots[index] = getBarrelPitchPivotNear(new Vec3(position.x + offset.x, position.y + offset.y, position.z + offset.z).scale(16D / modelScale));
        }
        return pivots;
    }

    /**
     * Muzzle of this vehicle's main armament, in model pixels, measured from the
     * barrel geometry the renderer draws.
     *
     * <p>
     * Prefers {@code barrelModel}, which is where all but a few packs build the
     * gun. The animated and special barrel groups are drawn translated to
     * {@code barrelAttach}, so their measurement carries that offset, applied the
     * same way {@link #translateToModelPoint} applies it.
     * </p>
     *
     * @return the muzzle in model pixels, or {@code null} when this model has no barrel
     */
    @Nullable
    public Vec3 getPrimaryBarrelMuzzle()
    {
        if (barrelMuzzleResolved)
            return primaryBarrelMuzzle;
        barrelMuzzleResolved = true;

        // Many packs split the gun: a mantlet in barrelModel and the recoiling
        // tube in animBarrelModel. Whichever group reaches further is the muzzle.
        Vec3 fixed = measureMuzzle(1F, barrelModel);
        Vec3 attached = measureMuzzle(1F, barrelSpecModel, animBarrelModel);
        if (attached != null)
            attached = attached.add(barrelAttach.x * 16D, barrelAttach.y * 16D, -barrelAttach.z * 16D);
        primaryBarrelMuzzle = fixed == null || (attached != null && attached.x > fixed.x) ? attached : fixed;
        return primaryBarrelMuzzle;
    }

    /**
     * {@link #getPrimaryBarrelMuzzle()} measured on the one barrel nearest
     *
     * @return the muzzle in model pixels, or {@code null} when this model has no barrel
     */
    @Nullable
    public Vec3 getPrimaryBarrelMuzzleNear(Vec3 hint)
    {
        Vec3 fixed = measureMuzzle(1F, hint, barrelModel);
        // The attached groups are drawn shifted by barrelAttach, so the hint is moved into their frame.
        Vec3 attached = measureMuzzle(1F, hint.subtract(barrelAttachPixels()), barrelSpecModel, animBarrelModel);
        if (attached != null)
            attached = attached.add(barrelAttachPixels());
        return fixed == null || (attached != null && attached.x > fixed.x) ? attached : fixed;
    }

    /**
     * The muzzle of every tube of the main armament, in model pixels: one for a
     * single gun, four for a quad mount. Taken from whichever barrel group reaches
     * further, as {@link #getPrimaryBarrelMuzzle()} does.
     */
    public List<Vec3> getPrimaryBarrelMuzzles()
    {
        List<Vec3> fixed = measureMuzzles(1F, barrelModel);
        List<Vec3> attached = measureMuzzles(1F, barrelSpecModel, animBarrelModel).stream().map(muzzle -> muzzle.add(barrelAttachPixels())).toList();
        if (fixed.isEmpty() || (!attached.isEmpty() && attached.get(0).x > fixed.get(0).x))
            return attached;
        return fixed;
    }

    private Vec3 barrelAttachPixels()
    {
        return new Vec3(barrelAttach.x * 16D, barrelAttach.y * 16D, -barrelAttach.z * 16D);
    }

    /** Turret parts, drawn at rest scaled by {@code turretScale} and shifted by {@code turretTrans}. */
    private Set<ModelRendererTurbo> turretParts;

    @Override
    protected double[] toRestPose(ModelRendererTurbo part, double[] bounds)
    {
        if (turretParts == null)
        {
            Set<ModelRendererTurbo> parts = Collections.newSetFromMap(new IdentityHashMap<>());
            addAll(parts, turretModel);
            addAll(parts, barrelModel);
            for (ModelRendererTurbo[] row : ammoModel)
                addAll(parts, row);
            turretParts = parts;
        }
        boolean attached = contains(barrelSpecModel, part) || contains(animBarrelModel, part);
        if (!attached && !turretParts.contains(part))
            return bounds;

        // renderTurret scales, then translates by turretTrans; the attached groups are drawn at barrelAttach.
        Vec3 shift = new Vec3(turretTrans.x * 16D, turretTrans.y * 16D, turretTrans.z * 16D);
        if (attached)
            shift = shift.add(barrelAttachPixels());
        double[] scale = {turretScale.x, turretScale.y, turretScale.z};
        double[] offset = {shift.x, shift.y, shift.z};
        double[] posed = new double[6];
        for (int axis = 0; axis < 3; axis++)
        {
            double a = (bounds[axis] + offset[axis]) * scale[axis];
            double b = (bounds[axis + 3] + offset[axis]) * scale[axis];
            posed[axis] = Math.min(a, b);
            posed[axis + 3] = Math.max(a, b);
        }
        return posed;
    }

    private static void addAll(Set<ModelRendererTurbo> set, @Nullable ModelRendererTurbo[] parts)
    {
        if (parts != null)
            Collections.addAll(set, parts);
    }

    private static boolean contains(@Nullable ModelRendererTurbo[] parts, ModelRendererTurbo part)
    {
        if (parts == null)
            return false;
        for (ModelRendererTurbo candidate : parts)
        {
            if (candidate == part)
                return true;
        }
        return false;
    }

    @Override
    protected boolean isTurretMountedGun(SeatInfo seatInfo)
    {
        return seatInfo.getPart() == EnumDriveablePart.TURRET;
    }

    /** The point transform {@link #renderTurret} applies before drawing turret-mounted guns. */
    @Override
    protected Vec3 toTurretPose(Driveable driveable, Vec3 modelPixels, float turretYaw)
    {
        Vec3 point = new Vec3(modelPixels.x * turretScale.x, modelPixels.y * turretScale.y, modelPixels.z * turretScale.z).add(turretTrans.x * 16D, turretTrans.y * 16D, turretTrans.z * 16D);
        Vector3f origin = driveable.getConfigType() == null ? null : driveable.getConfigType().getTurretOrigin();
        Vec3 pivot = origin == null ? Vec3.ZERO : new Vec3(origin.x * 16D, origin.y * 16D, -origin.z * 16D);
        return rotateY(point.subtract(pivot), -turretYaw * Mth.DEG_TO_RAD).add(pivot);
    }

    @Override
    public void render(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
        float scale, EnumRenderPass renderPass)
    {
        if (driveable.isPartIntact(EnumDriveablePart.CORE))
            super.render(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        float wheelSpin = -state.wheelAngle();
        float steering = -state.steeringAngle() * 3F * Mth.DEG_TO_RAD;
        renderWheelIfIntact(driveable, EnumDriveablePart.BACK_LEFT_WHEEL, leftBackWheelModel, wheelSpin, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale,
            renderPass);
        renderWheelIfIntact(driveable, EnumDriveablePart.BACK_RIGHT_WHEEL, rightBackWheelModel, wheelSpin, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale,
            renderPass);
        renderWheelIfIntact(driveable, EnumDriveablePart.FRONT_LEFT_WHEEL, leftFrontWheelModel, wheelSpin, steering, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
            scale, renderPass);
        renderWheelIfIntact(driveable, EnumDriveablePart.FRONT_RIGHT_WHEEL, rightFrontWheelModel, wheelSpin, steering, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
            scale, renderPass);
        renderWheelIfIntact(driveable, EnumDriveablePart.FRONT_WHEEL, frontWheelModel, wheelSpin, steering, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale,
            renderPass);
        renderWheelIfIntact(driveable, EnumDriveablePart.BACK_WHEEL, backWheelModel, wheelSpin, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(trackPartForDrawnSide(driveable.getConfigType(), true, true)))
        {
            renderPart(leftTrackModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderWheel(leftTrackWheelModels, wheelSpin, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (driveable.isPartIntact(trackPartForDrawnSide(driveable.getConfigType(), false, true)))
        {
            renderPart(rightTrackModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderWheel(rightTrackWheelModels, wheelSpin, 0F, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        renderTrackFrame(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.getConfigType() instanceof VehicleType vehicleType)
            renderFancyTracks(driveable, vehicleType, state, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(EnumDriveablePart.CORE))
        {
            renderLegs(state, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            boolean hugeBoat = driveable.getConfigType() instanceof VehicleType vehicleType && vehicleType.isFloatOnWater() && vehicleType.getWheelStepHeight() == 0F;
            float steeringWheelAngle = state.steeringAngle() * 3F * Mth.DEG_TO_RAD * (hugeBoat ? -1F : 1F);
            renderSteeringWheel(steeringWheelAngle, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        if (driveable.isPartIntact(EnumDriveablePart.TRAILER))
            renderPart(trailerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        if (driveable.isPartIntact(EnumDriveablePart.TURRET))
            renderTurret(driveable, state, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.HARVESTER))
            renderAround(drillHeadModel, drillHeadOrigin, Axis.XP, state.animationTime() * (24F + 48F * Math.abs(state.throttle())), poseStack, vertexConsumer, packedLight, packedOverlay, red, green,
                blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(EnumDriveablePart.CORE) && driveable.getConfigType() instanceof VehicleType vehicleType)
        {
            renderDoor(doorAnimModel, doorAttach, state.doorTransform(), state.partialTick(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderDoor(door2AnimModel, door2Attach, state.door2Transform(), state.partialTick(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        renderRegisteredGuns(driveable, state, GunMountFilter.BODY, GunYawConvention.VEHICLE, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    @Override
    public void render(DriveableType driveableType, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
        float scale, EnumRenderPass renderPass)
    {
        super.render(driveableType, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftBackWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightBackWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFrontWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFrontWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(frontWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(backWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightTrackModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftTrackModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightTrackWheelModels, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftTrackWheelModels, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftFrontLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightFrontLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(leftBackLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(rightBackLegModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(trailerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(steeringWheelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(drillHeadModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.pushPose();
        poseStack.scale(turretScale.x, turretScale.y, turretScale.z);
        poseStack.translate(turretTrans.x, turretTrans.y, turretTrans.z);
        renderPart(turretModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(barrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartMatrix(ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartAt(barrelSpecModel, barrelAttach, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartAt(animBarrelModel, barrelAttach, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIT1Preview(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
        renderPartAt(doorAnimModel, doorAttach, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPartAt(door2AnimModel, door2Attach, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderTrackPreview(driveableType, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveableType instanceof VehicleType vehicleType)
        {
            ensureTrackPaths(vehicleType);
            renderFancyTrackPath(vehicleType, leftTrackPath, 0F, null, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            renderFancyTrackPath(vehicleType, rightTrackPath, 0F, null, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    /** Draw the same stationary track frame that a newly placed vehicle uses. */
    private void renderTrackPreview(DriveableType driveableType, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue,
        float alpha, float scale, EnumRenderPass renderPass)
    {
        int configuredFrames = driveableType == null ? Integer.MAX_VALUE : driveableType.getAnimFrames() + 1;
        int leftFrame = frameIndex(leftAnimTrackModel.length, configuredFrames, 0F);
        int rightFrame = frameIndex(rightAnimTrackModel.length, configuredFrames, 0F);
        if (leftFrame >= 0)
            renderPart(leftAnimTrackModel[leftFrame], poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (rightFrame >= 0)
            renderPart(rightAnimTrackModel[rightFrame], poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        // Older content models expose the three animation frames as separate
        // fields instead of the frame matrices above.
        renderPart(selectFrame(0, leftAnimTrackModel1, leftAnimTrackModel2, leftAnimTrackModel3), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(selectFrame(0, rightAnimTrackModel1, rightAnimTrackModel2, rightAnimTrackModel3), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    public void renderTurret(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue,
        float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        Vector3f turretOrigin = driveable.getConfigType() == null ? null : driveable.getConfigType().getTurretOrigin();
        translateToModelPoint(poseStack, turretOrigin);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.turretYaw()));
        if (turretOrigin != null)
            poseStack.translate(-turretOrigin.x, -turretOrigin.y, turretOrigin.z);
        poseStack.scale(turretScale.x, turretScale.y, turretScale.z);
        poseStack.translate(turretTrans.x, turretTrans.y, turretTrans.z);
        renderPart(turretModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderWithRotation(barrelModel, 0F, 0F, -state.turretPitch() * Mth.DEG_TO_RAD, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderAmmo(driveable, -state.turretPitch() * Mth.DEG_TO_RAD, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        renderAround(barrelSpecModel, barrelAttach, Axis.ZP, -state.turretPitch(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderRecoilingBarrel(driveable, state.turretPitch(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderIT1(driveable, state.partialTick(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderRegisteredGuns(driveable, state, GunMountFilter.TURRET, GunYawConvention.VEHICLE, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderAmmo(Driveable driveable, float pitchRadians, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue,
        float alpha, float scale, EnumRenderPass renderPass)
    {
        DriveableData data = driveable.getDriveableData();
        int missileSlots = data == null ? 0 : data.getNumMissileSlots();
        for (int row = 0; row < ammoModel.length; row++)
        {
            if (data == null || row >= missileSlots || !data.getMissile(row).isEmpty())
            {
                renderWithRotation(ammoModel[row], 0F, 0F, pitchRadians, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            }
        }
    }

    private void renderRecoilingBarrel(Driveable driveable, float pitchDegrees, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green,
        float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (animBarrelModel == null || animBarrelModel.length == 0)
            return;

        poseStack.pushPose();
        translateToModelPoint(poseStack, barrelAttach);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitchDegrees));
        poseStack.translate(recoilOffset(driveable), 0F, 0F);
        renderPart(animBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderIT1(Driveable driveable, float partialTick, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue,
        float alpha, float scale, EnumRenderPass renderPass)
    {
        float armAngle = Mth.rotLerp(partialTick, driveable.getPrevIT1ArmAngle(), driveable.getIT1ArmAngle());
        float railAngle = Mth.rotLerp(partialTick, driveable.getPrevIT1RailAngle(), driveable.getIT1RailAngle());
        float doorAngle = Mth.rotLerp(partialTick, driveable.getPrevIT1DoorAngle(), driveable.getIT1DoorAngle());

        poseStack.pushPose();
        translateIT1Point(poseStack, drakonArmAttach);
        poseStack.mulPose(Axis.ZP.rotationDegrees(armAngle));
        renderPart(drakonArmModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.translate(value(drakonRailAttach, 0) - value(drakonArmAttach, 0), value(drakonRailAttach, 1) - value(drakonArmAttach, 1), value(drakonRailAttach, 2) - value(drakonArmAttach, 2));
        poseStack.mulPose(Axis.ZP.rotationDegrees(railAngle));
        renderPart(drakonRailModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isCanFireIT1())
        {
            renderPart(drakonModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        else if (driveable.isReloadingDrakon())
        {
            renderPart(drakonReloadModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        poseStack.popPose();

        poseStack.pushPose();
        translateIT1Point(poseStack, drakonDoorAttach);
        poseStack.mulPose(Axis.XP.rotationDegrees(doorAngle));
        renderPart(drakonDoorModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderIT1Preview(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        translateIT1Point(poseStack, drakonArmAttach);
        renderPart(drakonArmModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        translateIT1Point(poseStack, drakonRailAttach);
        renderPart(drakonRailModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPart(drakonModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        translateIT1Point(poseStack, drakonDoorAttach);
        renderPart(drakonDoorModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void translateIT1Point(PoseStack poseStack, Vector3f point)
    {
        if (point != null)
            poseStack.translate(point.x, point.y, point.z);
    }

    /**
     * Resolves which track part gates the track drawn on a given side.
     *
     * <p>
     * Type files and models disagree about which sign of the legacy lateral
     * axis is the left side, and they disagree per vehicle rather than per pack:
     * some author {@code leftTrack} at the coordinate the model uses for
     * {@code leftTrackModel}, others at the mirrored one. Comparing the two
     * authored sides against each other settles it per model instead of trusting
     * either name, so destroying a track always hides the track that was hit.
     * </p>
     *
     * <p>
     * Static meshes have already been mirrored by {@code flipAll()}, so their
     * lateral signs match their part boxes. Procedural link points have not been
     * mirrored, so their signs are opposite those of the part boxes.
     * </p>
     */
    private EnumDriveablePart trackPartForDrawnSide(@Nullable DriveableType type, boolean leftSide, boolean meshes)
    {
        if (trackSideType != type)
        {
            trackSideType = type;
            Float boxes = boxLateralDelta(type);
            trackMeshSidesSwapped = sidesSwapped(meshLateralDelta(), boxes);
            trackPathSidesSwapped = pathSidesSwapped(pathLateralDelta(type), boxes);
        }
        return trackPart(leftSide, meshes ? trackMeshSidesSwapped : trackPathSidesSwapped);
    }

    /** Matching lateral signs mean the authored track and part names agree. */
    static boolean sidesSwapped(@Nullable Float drawn, @Nullable Float boxes)
    {
        return drawn != null && boxes != null && drawn * boxes < 0F;
    }

    /** Link points are translated directly; compare their lateral sign with the part boxes. */
    static boolean pathSidesSwapped(@Nullable Float drawn, @Nullable Float boxes)
    {
        return sidesSwapped(drawn, boxes);
    }

    static EnumDriveablePart trackPart(boolean leftSide, boolean swapped)
    {
        return leftSide != swapped ? EnumDriveablePart.LEFT_TRACK : EnumDriveablePart.RIGHT_TRACK;
    }

    /** Lateral offset of the left track meshes from the right ones, or null when either side is empty. */
    @Nullable
    private Float meshLateralDelta()
    {
        Float left = meshLateral(leftTrackModel, leftTrackWheelModels, leftAnimTrackModel1, leftAnimTrackModel2, leftAnimTrackModel3);
        Float right = meshLateral(rightTrackModel, rightTrackWheelModels, rightAnimTrackModel1, rightAnimTrackModel2, rightAnimTrackModel3);
        return left == null || right == null ? null : left - right;
    }

    @Nullable
    private static Float meshLateral(ModelRendererTurbo[]... groups)
    {
        float total = 0F;
        int count = 0;
        for (ModelRendererTurbo[] group : groups)
        {
            if (group == null)
                continue;
            for (ModelRendererTurbo part : group)
            {
                if (part == null)
                    continue;
                total += part.rotationPointZ;
                count++;
            }
        }
        return count == 0 ? null : total / count;
    }

    @Nullable
    private static Float boxLateralDelta(@Nullable DriveableType type)
    {
        CollisionBox left = type == null ? null : type.getHealth().get(EnumDriveablePart.LEFT_TRACK);
        CollisionBox right = type == null ? null : type.getHealth().get(EnumDriveablePart.RIGHT_TRACK);
        return left == null || right == null ? null : left.getX() + left.getWidth() * 0.5F - (right.getX() + right.getWidth() * 0.5F);
    }

    @Nullable
    private static Float pathLateralDelta(@Nullable DriveableType type)
    {
        Float left = pathLateral(type == null ? null : type.getLeftTrackPoints());
        Float right = pathLateral(type == null ? null : type.getRightTrackPoints());
        return left == null || right == null ? null : left - right;
    }

    @Nullable
    private static Float pathLateral(@Nullable List<Vector3f> points)
    {
        if (points == null || points.isEmpty())
            return null;
        float total = 0F;
        int count = 0;
        for (Vector3f point : points)
        {
            if (point == null)
                continue;
            total += point.z;
            count++;
        }
        return count == 0 ? null : total / count;
    }

    private void renderTrackFrame(Driveable driveable, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue,
        float alpha, float scale, EnumRenderPass renderPass)
    {
        int configuredFrames = driveable.getConfigType() == null ? Integer.MAX_VALUE : driveable.getConfigType().getAnimFrames() + 1;
        float leftPhase = TrackAnimationPhysics.framePhase(state.leftTrackProgress());
        float rightPhase = TrackAnimationPhysics.framePhase(state.rightTrackProgress());
        int leftFrame = frameIndex(leftAnimTrackModel.length, configuredFrames, leftPhase);
        int rightFrame = frameIndex(rightAnimTrackModel.length, configuredFrames, rightPhase);
        animFrameLeft = leftFrame;
        animFrameRight = rightFrame;
        DriveableType type = driveable.getConfigType();
        if (leftFrame >= 0 && driveable.isPartIntact(trackPartForDrawnSide(type, true, true)))
            renderPart(leftAnimTrackModel[leftFrame], poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (rightFrame >= 0 && driveable.isPartIntact(trackPartForDrawnSide(type, false, true)))
            renderPart(rightAnimTrackModel[rightFrame], poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        int legacyFrameLeft = Mth.clamp((int) Math.floor(leftPhase * 3F), 0, 2);
        int legacyFrameRight = Mth.clamp((int) Math.floor(rightPhase * 3F), 0, 2);
        if (driveable.isPartIntact(trackPartForDrawnSide(type, true, true)))
            renderPart(selectFrame(legacyFrameLeft, leftAnimTrackModel1, leftAnimTrackModel2, leftAnimTrackModel3), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
                scale, renderPass);
        if (driveable.isPartIntact(trackPartForDrawnSide(type, false, true)))
            renderPart(selectFrame(legacyFrameRight, rightAnimTrackModel1, rightAnimTrackModel2, rightAnimTrackModel3), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
                scale, renderPass);
    }

    private void renderFancyTracks(Driveable driveable, VehicleType type, RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red,
        float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        ensureTrackPaths(type);
        // A live vehicle carries eased per-link angles that FixTrackLink steers
        // without them the links fall back to the static pose.
        TrackLinkAnimation links = state.trackLinks() != null && state.trackLinks().isActive() ? state.trackLinks() : null;
        if (driveable.isPartIntact(trackPartForDrawnSide(type, true, false)))
            renderFancyTrackPath(type, leftTrackPath, state.leftTrackProgress() * leftTrackPath.length(), links == null ? null : links.angles(true), poseStack, vertexConsumer, packedLight,
                packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (driveable.isPartIntact(trackPartForDrawnSide(type, false, false)))
            renderFancyTrackPath(type, rightTrackPath, state.rightTrackProgress() * rightTrackPath.length(), links == null ? null : links.angles(false), poseStack, vertexConsumer, packedLight,
                packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void ensureTrackPaths(DriveableType type)
    {
        if (trackPathType != type)
        {
            trackPathType = type;
            leftTrackPath = TrackPath.create(type.getLeftTrackPoints());
            rightTrackPath = TrackPath.create(type.getRightTrackPoints());
            trackPathRadius = Math.max(pathRadius(leftTrackPath), pathRadius(rightTrackPath));
        }
    }

    private static float pathRadius(TrackPath path)
    {
        float squared = 0F;
        for (int i = 0; i < path.size(); i++)
            squared = Math.max(squared, path.pointX(i) * path.pointX(i) + path.pointY(i) * path.pointY(i) + path.pointZ(i) * path.pointZ(i));
        return Mth.sqrt(squared) * MODEL_SCALE;
    }

    private void renderFancyTrackPath(DriveableType type, TrackPath path, float movement, @Nullable float[] linkAngles, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
        int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        float spacing = type.getTrackLinkLength();
        if (fancyTrackModel == null || fancyTrackModel.length == 0 || path.isEmpty() || spacing <= 0F)
            return;

        int originalCount = Mth.clamp(Math.round(path.length() / spacing), 1, 512);
        int group = scale == 1F && trackLinkLod != null ? TrackLinkLod.activeGroup() : 0;
        // Very short loops do not have enough links for a stable long envelope.
        if (group >= 4 && originalCount < 16)
            group = 2;
        if (group >= 2 && originalCount < 8)
            group = 1;
        ModelRendererTurbo[] selected = group > 0 ? trackLinkLod.parts(group) : null;
        if (selected == null)
            group = 1;
        ModelRendererTurbo[] linkParts = selected == null ? fancyTrackModel : selected;
        int linkCount = (originalCount + group - 1) / group;
        float normalizedMovement = path.wrap(movement);
        for (int link = 0; link < linkCount; link++)
        {
            int originalLink = link * group;
            float distance = path.wrap(normalizedMovement + 0.01F + spacing * originalLink);
            int segment = path.segmentAt(distance);
            int previous = path.previousOf(segment);
            float progress = path.progressAlongSegment(distance, segment);
            float x = Mth.lerp(progress, path.pointX(previous), path.pointX(segment));
            float y = Mth.lerp(progress, path.pointY(previous), path.pointY(segment));
            float z = Mth.lerp(progress, path.pointZ(previous), path.pointZ(segment));
            float rotation = linkAngles != null && originalLink < linkAngles.length
                ? (float) Math.toDegrees(linkAngles[originalLink])
                : (float) Math.toDegrees(Math.atan2(path.pointY(previous) - y, path.pointX(previous) - x));

            poseStack.pushPose();
            poseStack.translate(x * MODEL_SCALE, y * MODEL_SCALE, z * MODEL_SCALE);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            renderPart(linkParts, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private void renderDoor(ModelRendererTurbo[] parts, Vector3f attachment, AnimatedTransform transform, float partialTick, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
        int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (parts == null || parts.length == 0)
            return;

        poseStack.pushPose();
        poseStack.translate(value(attachment, 0) + transform.position(0, partialTick) * MODEL_SCALE, value(attachment, 1) + transform.position(1, partialTick) * MODEL_SCALE,
            -value(attachment, 2) + transform.position(2, partialTick) * MODEL_SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotation(0, partialTick)));
        poseStack.mulPose(Axis.YP.rotationDegrees(-transform.rotation(1, partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rotation(2, partialTick)));
        renderPart(parts, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private void renderLegs(RenderState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        float speed = legSpeedChange ? Math.abs(state.throttle()) : 1F;
        float phase = state.leftTrackProgress() * Mth.TWO_PI * legMoveSpeed;
        float steer = Mth.clamp(state.steeringAngle() / 20F, -1F, 1F) * legSteerAmount;
        renderWithRotation(leftFrontLegModel, 0F, 0F, Mth.sin(phase + Mth.PI) * speed * legMaxMove * (1F + steer), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
            scale, renderPass);
        renderWithRotation(rightFrontLegModel, 0F, 0F, Mth.sin(phase) * speed * legMaxMove * (1F - steer), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale,
            renderPass);
        renderWithRotation(leftBackLegModel, 0F, 0F, Mth.sin(phase + Mth.HALF_PI) * speed * legMaxMove * (1F + steer), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha,
            scale, renderPass);
        renderWithRotation(rightBackLegModel, 0F, 0F, Mth.sin(phase + Mth.PI + Mth.HALF_PI) * speed * legMaxMove * (1F - steer), poseStack, vertexConsumer, packedLight, packedOverlay, red, green,
            blue, alpha, scale, renderPass);
    }

    private void renderWheel(ModelRendererTurbo[] parts, float spin, float steering, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green,
        float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        renderWithRotation(parts, 0F, steering, spin, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderWheelIfIntact(Driveable driveable, EnumDriveablePart part, ModelRendererTurbo[] models, float spin, float steering, PoseStack poseStack, VertexConsumer vertexConsumer,
        int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (driveable.isPartIntact(part))
            renderWheel(models, spin, steering, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderWithRotation(ModelRendererTurbo[][] matrix, float x, float y, float z, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red,
        float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (matrix == null)
            return;
        for (ModelRendererTurbo[] row : matrix)
            renderWithRotation(row, x, y, z, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    private void renderWithRotation(ModelRendererTurbo[] parts, float x, float y, float z, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red,
        float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        ModelRendererTurbo.renderRotated(parts, x, y, z, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass, oldRotateOrder);
    }

    private void renderSteeringWheel(float angle, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo part : steeringWheelModel)
        {
            if (part == null)
                continue;
            float oldX = part.rotateAngleX;
            part.rotateAngleX = angle;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass, oldRotateOrder);
            part.rotateAngleX = oldX;
        }
    }

    private void renderAround(ModelRendererTurbo[] parts, Vector3f origin, Axis axis, float angleDegrees, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
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

    private static int frameIndex(int modelFrameCount, int configuredFrameCount, float progress)
    {
        int frameCount = Math.min(modelFrameCount, configuredFrameCount);
        if (frameCount <= 0)
            return -1;
        return Mth.clamp((int) Math.floor(progress * frameCount), 0, frameCount - 1);
    }

    private static ModelRendererTurbo[] selectFrame(int frame, ModelRendererTurbo[] first, ModelRendererTurbo[] second, ModelRendererTurbo[] third)
    {
        return switch (frame)
        {
            case 1 -> second;
            case 2 -> third;
            default -> first;
        };
    }

    public float rotateTowards(Vector3f point, Vector3f original)
    {
        return (float) Math.atan2(point.y - original.y, point.x - original.x);
    }

    @Override
    public void flipAll()
    {
        super.flipAll();
        flip(turretModel);
        flip(barrelModel);
        flip(ammoModel);
        flip(frontWheelModel);
        flip(backWheelModel);
        flip(leftFrontWheelModel);
        flip(rightFrontWheelModel);
        flip(leftBackWheelModel);
        flip(rightBackWheelModel);
        flip(rightTrackModel);
        flip(leftTrackModel);
        flip(rightTrackWheelModels);
        flip(leftTrackWheelModels);
        flip(leftFrontLegModel);
        flip(rightFrontLegModel);
        flip(leftBackLegModel);
        flip(rightBackLegModel);
        flip(leftAnimTrackModel);
        flip(rightAnimTrackModel);
        flip(fancyTrackModel);
        flip(rightAnimTrackModel1);
        flip(leftAnimTrackModel1);
        flip(rightAnimTrackModel2);
        flip(leftAnimTrackModel2);
        flip(rightAnimTrackModel3);
        flip(leftAnimTrackModel3);
        flip(trailerModel);
        flip(steeringWheelModel);
        flip(drillHeadModel);
        flip(barrelSpecModel);
        flip(animBarrelModel);
        flip(doorAnimModel);
        flip(door2AnimModel);
        flip(drakonModel);
        flip(drakonReloadModel);
        flip(drakonArmModel);
        flip(drakonRailModel);
        flip(drakonDoorModel);
    }

    @Override
    public void translateAll(float x, float y, float z)
    {
        super.translateAll(x, y, z);
        translate(turretModel, x, y, z);
        translate(barrelModel, x, y, z);
        translate(ammoModel, x, y, z);
        translate(frontWheelModel, x, y, z);
        translate(backWheelModel, x, y, z);
        translate(leftFrontWheelModel, x, y, z);
        translate(rightFrontWheelModel, x, y, z);
        translate(leftBackWheelModel, x, y, z);
        translate(rightBackWheelModel, x, y, z);
        translate(rightTrackModel, x, y, z);
        translate(leftTrackModel, x, y, z);
        translate(rightTrackWheelModels, x, y, z);
        translate(leftTrackWheelModels, x, y, z);
        translate(leftFrontLegModel, x, y, z);
        translate(rightFrontLegModel, x, y, z);
        translate(leftBackLegModel, x, y, z);
        translate(rightBackLegModel, x, y, z);
        translate(leftAnimTrackModel, x, y, z);
        translate(rightAnimTrackModel, x, y, z);
        translate(fancyTrackModel, x, y, z);
        translate(rightAnimTrackModel1, x, y, z);
        translate(leftAnimTrackModel1, x, y, z);
        translate(rightAnimTrackModel2, x, y, z);
        translate(leftAnimTrackModel2, x, y, z);
        translate(rightAnimTrackModel3, x, y, z);
        translate(leftAnimTrackModel3, x, y, z);
        translate(trailerModel, x, y, z);
        translate(steeringWheelModel, x, y, z);
        translate(drillHeadModel, x, y, z);
        translate(barrelSpecModel, x, y, z);
        translate(animBarrelModel, x, y, z);
        translate(doorAnimModel, x, y, z);
        translate(door2AnimModel, x, y, z);
        translate(drakonModel, x, y, z);
        translate(drakonReloadModel, x, y, z);
        translate(drakonArmModel, x, y, z);
        translate(drakonRailModel, x, y, z);
        translate(drakonDoorModel, x, y, z);
    }
}
