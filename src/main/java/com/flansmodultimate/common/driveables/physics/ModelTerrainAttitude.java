package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

/**
 * Wheel support sampling for entities wearing a driveable model, using the normal suspension response and the wheel
 * probes of a real driveable, {@link WheelTerrainSampler}.
 *
 * <p>
 * Such an entity stands wherever its own collision puts it, which is not where its wheels would rest. Besides the tilt,
 * the sample therefore yields a lift that draws the model down (or up) until its most loaded wheel touches the terrain,
 * the part a real driveable's suspension does by moving the whole vehicle.
 * </p>
 *
 * <p>
 * The same probes also report how much of the footprint is carried. The tilt looks ahead along the model's movement, so
 * that a vehicle meets a slope already pitched for it; support must not, or a vehicle driving towards a ledge would be
 * dropped before it reached one. Where the two differ, the wheel is probed at both places.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelTerrainAttitude
{
    /** Wheels whose support lies more than this far above them, in blocks at scale 1, beyond the step height, are inside a wall. */
    private static final double MAX_BURIED = 0.5D;

    public static TerrainSupport sample(Entity entity, IContentType definition, UnaryOperator<Vec3> modelToWorld, double scale, TerrainAttitude current)
    {
        if (!(definition instanceof DriveableType type) || type.getWheelPositions().isEmpty())
            return TerrainSupport.LEVEL;
        float spring = Mth.clamp(type.getWheelSpringStrength(), 0F, 1F);
        double modelScale = Math.max(1.0E-4D, type.getModelScale());
        double step = Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F) * scale;
        double droop = (0.35D + (1D - spring) * 0.2D) * scale;
        Vec3 prediction = horizontalPrediction(entity, scale);
        // The probe itself bounds how deep support may lie: whatever it reaches both tilts and carries the model.
        WheelTerrainSampler.Reach reach = new WheelTerrainSampler.Reach(step + 0.6D * scale, droop + step + 0.9D * scale, type.getWheelGroundClearance() * scale, step + MAX_BURIED * scale,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        WheelTerrainSampler sampler = new WheelTerrainSampler((from, to) -> clip(entity, from, to), reach, prediction, true);
        boolean unloaded = false;
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            if (wheel == null)
                continue;
            // Preview points already include ModelScale horizontally. Legacy wheel anchor heights, like those of real driveables, do not.
            Vec3 local = LegacyDriveableCoordinates.toLocal(wheel.getPosition());
            Vec3 point = new Vec3(local.x * modelScale, local.y, local.z * modelScale);
            Vec3 standing = modelToWorld.apply(point);
            if (!entity.level().isLoaded(BlockPos.containing(standing.add(prediction))))
                unloaded = true;
            // In the preview frame the nose is -Z and right is +X, for both vehicles and aircraft.
            sampler.add(standing, -point.z * scale, point.x * scale, point.y * scale);
        }
        double highestError = sampler.highestError();
        WheelSupport support = unloaded ? WheelSupport.UNKNOWN : sampler.support();
        // Preserve the pose in unloaded terrain or in free fall. A hull beached without wheel support eases level.
        if (!Double.isFinite(highestError))
            return new TerrainSupport(unloaded || !entity.onGround()
                ? current
                : new TerrainAttitude(SuspensionPhysics.smoothTerrainAngle(current.pitch(), 0F, spring), SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring),
                    SuspensionPhysics.smoothLift(current.lift(), -current.lift(), spring, scale)),
                support);
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        float limit = ModCommonConfig.forceLegacyMovement(physics.category()) ? SuspensionPhysics.DEFAULT_TERRAIN_ANGLE : physics.terrainAngleLimitDegrees();
        // The preview frame pitches and banks the other way round than the support angle counts.
        float pitch = SuspensionPhysics.smoothTerrainAngle(current.pitch(), -sampler.pitchTarget(false, limit), spring);
        float bank = SuspensionPhysics.smoothTerrainAngle(current.bank(), type.isCanRoll() ? -sampler.rollTarget(false, limit) : 0F, spring);
        // The highest-reaching support carries the model; wheels over lower ground hang, as on a real suspension.
        float lift = SuspensionPhysics.smoothLift(current.lift(), highestError, spring, scale);
        lift = (float) Mth.clamp(lift, -(step + droop + 0.9D * scale), step + 0.1D * scale);
        return new TerrainSupport(new TerrainAttitude(pitch, bank, lift), support);
    }

    private static Vec3 horizontalPrediction(Entity entity, double scale)
    {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 prediction = new Vec3(motion.x, 0D, motion.z);
        return prediction.lengthSqr() > 2.25D * scale * scale ? prediction.normalize().scale(1.5D * scale) : prediction;
    }

    @Nullable
    private static BlockHitResult clip(Entity entity, Vec3 from, Vec3 to)
    {
        if (!entity.level().isLoaded(BlockPos.containing(from)) || !entity.level().isLoaded(BlockPos.containing(to)))
            return null;
        BlockHitResult hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }
}
