package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.TerrainAttitude;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.types.DriveableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;
import java.util.function.UnaryOperator;

/**
 * Wheel support sampling for entities wearing a driveable model, using the normal suspension response.
 *
 * <p>
 * Such an entity stands wherever its own collision puts it, which is not where its wheels would rest. Besides the tilt,
 * the sample therefore yields a lift that draws the model down (or up) until its most loaded wheel touches the terrain,
 * the part a real driveable's suspension does by moving the whole vehicle.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelTerrainAttitude
{
    /** Wheels whose support lies more than this far above them, in blocks at scale 1, beyond the step height, are inside a wall. */
    private static final double MAX_BURIED = 0.5D;

    public static TerrainAttitude sample(Entity entity, IContentType definition, UnaryOperator<Vec3> modelToWorld, double scale, TerrainAttitude current)
    {
        if (!(definition instanceof DriveableType type) || type.getWheelPositions().isEmpty())
            return TerrainAttitude.LEVEL;
        float spring = Mth.clamp(type.getWheelSpringStrength(), 0F, 1F);
        double modelScale = Math.max(1.0E-4D, type.getModelScale());
        double step = Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F) * scale;
        double droop = (0.35D + (1D - spring) * 0.2D) * scale;
        Axis longitudinal = new Axis();
        Axis lateral = new Axis();
        double highestError = Double.NEGATIVE_INFINITY;
        boolean unloaded = false;
        Vec3 prediction = horizontalPrediction(entity, scale);
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            if (wheel == null)
                continue;
            // Preview points already include ModelScale horizontally. Legacy wheel anchor heights, like those of real driveables, do not.
            Vec3 local = LegacyDriveableCoordinates.toLocal(wheel.getPosition());
            Vec3 point = new Vec3(local.x * modelScale, local.y, local.z * modelScale);
            Vec3 world = modelToWorld.apply(point).add(prediction);
            if (!entity.level().isLoaded(BlockPos.containing(world)))
                unloaded = true;
            OptionalDouble surface = supportHeight(entity, world, step, droop, scale, type.getWheelGroundClearance() * scale);
            if (surface.isPresent())
            {
                double error = surface.getAsDouble() + type.getWheelGroundClearance() * scale - world.y;
                highestError = Math.max(highestError, error);
                // In the preview frame the nose is -Z and right is +X, for both vehicles and aircraft.
                longitudinal.add(-point.z * scale, point.y * scale, surface.getAsDouble());
                lateral.add(point.x * scale, point.y * scale, surface.getAsDouble());
            }
        }
        // Preserve the pose in unloaded terrain or in free fall. A hull beached without wheel support eases level.
        if (!Double.isFinite(highestError))
            return unloaded || !entity.onGround()
                ? current
                : new TerrainAttitude(SuspensionPhysics.smoothTerrainAngle(current.pitch(), 0F, spring), SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring),
                    SuspensionPhysics.smoothLift(current.lift(), -current.lift(), spring, scale));
        float pitch = longitudinal.smoothAngle(current.pitch(), spring);
        float bank = type.isCanRoll() ? lateral.smoothAngle(current.bank(), spring) : SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring);
        // The highest-reaching support carries the model; wheels over lower ground hang, as on a real suspension.
        float lift = SuspensionPhysics.smoothLift(current.lift(), highestError, spring, scale);
        lift = (float) Mth.clamp(lift, -(step + droop + 0.9D * scale), step + 0.1D * scale);
        return new TerrainAttitude(pitch, bank, lift);
    }

    private static Vec3 horizontalPrediction(Entity entity, double scale)
    {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 prediction = new Vec3(motion.x, 0D, motion.z);
        return prediction.lengthSqr() > 2.25D * scale * scale ? prediction.normalize().scale(1.5D * scale) : prediction;
    }

    private static OptionalDouble supportHeight(Entity entity, Vec3 wheel, double step, double droop, double scale, double clearance)
    {
        Vec3 start = wheel.add(0D, step + 0.6D * scale, 0D);
        Vec3 end = wheel.add(0D, -droop - step - 0.9D * scale, 0D);
        if (!entity.level().isLoaded(BlockPos.containing(start)) || !entity.level().isLoaded(BlockPos.containing(end)))
            return OptionalDouble.empty();
        var hit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getLocation().y + clearance - wheel.y > step + MAX_BURIED * scale)
            return OptionalDouble.empty();
        return OptionalDouble.of(hit.getLocation().y);
    }

    private static final class Axis
    {
        private final Group positive = new Group();
        private final Group negative = new Group();

        private void add(double position, double mount, double surface)
        {
            if (position > 1.0E-4D)
                positive.add(position, mount, surface);
            else if (position < -1.0E-4D)
                negative.add(position, mount, surface);
        }

        /** Without support at both ends, eases level rather than holding whatever angle it had. */
        private float smoothAngle(float current, float spring)
        {
            float target = positive.count > 0 && negative.count > 0 ? -positive.angleFrom(negative) : 0F;
            return SuspensionPhysics.smoothTerrainAngle(current, target, spring);
        }
    }

    private static final class Group
    {
        private double position;
        private double mount;
        private double surface;
        private int count;

        private void add(double axisPosition, double mountHeight, double supportHeight)
        {
            position += axisPosition;
            mount += mountHeight;
            surface += supportHeight;
            count++;
        }

        private float angleFrom(Group other)
        {
            return SuspensionPhysics.supportAngle(surface / count - other.surface / other.count, position / count - other.position / other.count, mount / count - other.mount / other.count, false);
        }
    }
}
