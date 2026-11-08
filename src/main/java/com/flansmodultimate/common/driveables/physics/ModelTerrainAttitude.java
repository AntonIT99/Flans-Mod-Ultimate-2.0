package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.*;
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
    /** Movement below which the look-ahead is rounding and the predicted probe is the standing one. */
    private static final double PREDICTION_EPSILON = 1.0E-8D;

    public static TerrainSupport sample(Entity entity, IContentType definition, UnaryOperator<Vec3> modelToWorld, double scale, TerrainAttitude current)
    {
        if (!(definition instanceof DriveableType type) || type.getWheelPositions().isEmpty())
            return TerrainSupport.LEVEL;
        float spring = Mth.clamp(type.getWheelSpringStrength(), 0F, 1F);
        Sampling sampling = new Sampling(entity, modelToWorld, type, scale, spring);
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            if (wheel != null)
                sampling.add(wheel);
        }
        double highestError = sampling.highestError;
        WheelSupport support = sampling.unloaded ? WheelSupport.UNKNOWN : sampling.footprint.support();
        // Preserve the pose in unloaded terrain or in free fall. A hull beached without wheel support eases level.
        if (!Double.isFinite(highestError))
            return new TerrainSupport(sampling.unloaded || !entity.onGround()
                ? current
                : new TerrainAttitude(SuspensionPhysics.smoothTerrainAngle(current.pitch(), 0F, spring), SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring),
                    SuspensionPhysics.smoothLift(current.lift(), -current.lift(), spring, scale)),
                support);
        float pitch = sampling.longitudinal.smoothAngle(current.pitch(), spring);
        float bank = type.isCanRoll() ? sampling.lateral.smoothAngle(current.bank(), spring) : SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring);
        // The highest-reaching support carries the model; wheels over lower ground hang, as on a real suspension.
        float lift = SuspensionPhysics.smoothLift(current.lift(), highestError, spring, scale);
        lift = (float) Mth.clamp(lift, -(sampling.step + sampling.droop + 0.9D * scale), sampling.step + 0.1D * scale);
        return new TerrainSupport(new TerrainAttitude(pitch, bank, lift), support);
    }

    /** Probes each wheel and accumulates the tilt, lift and footprint support of the whole model. */
    private static final class Sampling
    {
        private final Entity entity;
        private final UnaryOperator<Vec3> modelToWorld;
        private final double scale;
        private final double modelScale;
        private final double step;
        private final double droop;
        private final double clearance;
        private final Vec3 prediction;
        private final boolean predicting;
        private final Axis longitudinal = new Axis();
        private final Axis lateral = new Axis();
        private final Footprint footprint = new Footprint();
        private double highestError = Double.NEGATIVE_INFINITY;
        private boolean unloaded;

        private Sampling(Entity entity, UnaryOperator<Vec3> modelToWorld, DriveableType type, double scale, float spring)
        {
            this.entity = entity;
            this.modelToWorld = modelToWorld;
            this.scale = scale;
            modelScale = Math.max(1.0E-4D, type.getModelScale());
            step = Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F) * scale;
            droop = (0.35D + (1D - spring) * 0.2D) * scale;
            clearance = type.getWheelGroundClearance() * scale;
            prediction = horizontalPrediction(entity, scale);
            predicting = prediction.lengthSqr() > PREDICTION_EPSILON;
        }

        private void add(DriveablePosition wheel)
        {
            // Preview points already include ModelScale horizontally. Legacy wheel anchor heights, like those of real driveables, do not.
            Vec3 local = LegacyDriveableCoordinates.toLocal(wheel.getPosition());
            Vec3 point = new Vec3(local.x * modelScale, local.y, local.z * modelScale);
            Vec3 standing = modelToWorld.apply(point);
            Vec3 world = standing.add(prediction);
            if (!entity.level().isLoaded(BlockPos.containing(world)))
                unloaded = true;
            OptionalDouble surface = supportHeight(world);
            if (surface.isPresent())
            {
                double error = surface.getAsDouble() + clearance - world.y;
                highestError = Math.max(highestError, error);
                // In the preview frame the nose is -Z and right is +X, for both vehicles and aircraft.
                longitudinal.add(-point.z * scale, point.y * scale, surface.getAsDouble());
                lateral.add(point.x * scale, point.y * scale, surface.getAsDouble());
            }
            // Where the model stands decides what carries it; where it is heading only decides how it is tilted.
            footprint.add(standing, predicting ? supportHeight(standing).isPresent() : surface.isPresent());
        }

        private static Vec3 horizontalPrediction(Entity entity, double scale)
        {
            Vec3 motion = entity.getDeltaMovement();
            Vec3 prediction = new Vec3(motion.x, 0D, motion.z);
            return prediction.lengthSqr() > 2.25D * scale * scale ? prediction.normalize().scale(1.5D * scale) : prediction;
        }

        /** @return the height of the terrain carrying a wheel at this world position, or empty where nothing within its suspension travel does */
        private OptionalDouble supportHeight(Vec3 wheel)
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
    }

    /** Collects where the wheels stand and which of them the terrain carries, to say how the model hangs over. */
    private static final class Footprint
    {
        private double carriedX;
        private double carriedZ;
        private double hangingX;
        private double hangingZ;
        private int carried;
        private int hanging;

        private void add(Vec3 wheel, boolean supported)
        {
            if (supported)
            {
                carriedX += wheel.x;
                carriedZ += wheel.z;
                carried++;
            }
            else
            {
                hangingX += wheel.x;
                hangingZ += wheel.z;
                hanging++;
            }
        }

        private WheelSupport support()
        {
            int total = carried + hanging;
            if (carried == 0 || hanging == 0)
                return new WheelSupport(carried, total, Vec3.ZERO);
            return new WheelSupport(carried, total, new Vec3(hangingX / hanging - carriedX / carried, 0D, hangingZ / hanging - carriedZ / carried));
        }
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
