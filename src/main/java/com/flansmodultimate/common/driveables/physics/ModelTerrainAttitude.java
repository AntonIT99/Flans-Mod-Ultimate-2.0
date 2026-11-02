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

/** Wheel support sampling for entities wearing a driveable model, using the normal suspension response. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelTerrainAttitude
{
    public static TerrainAttitude sample(Entity entity, IContentType definition, UnaryOperator<Vec3> modelToWorld, double scale, TerrainAttitude current)
    {
        if (!(definition instanceof DriveableType type) || type.getWheelPositions().isEmpty())
            return TerrainAttitude.LEVEL;
        float spring = Mth.clamp(type.getWheelSpringStrength(), 0F, 1F);
        double modelScale = Math.max(1.0E-4D, type.getModelScale());
        Axis longitudinal = new Axis();
        Axis lateral = new Axis();
        for (DriveablePosition wheel : type.getWheelPositions())
        {
            if (wheel == null)
                continue;
            // Preview points already include ModelScale horizontally. Legacy wheel anchor heights, like those of real driveables, do not.
            Vec3 local = LegacyDriveableCoordinates.toLocal(wheel.getPosition());
            Vec3 point = new Vec3(local.x * modelScale, local.y, local.z * modelScale);
            OptionalDouble surface = supportHeight(entity, type, modelToWorld.apply(point), scale);
            if (surface.isPresent())
            {
                // In the preview frame the nose is -Z and right is +X, for both vehicles and aircraft.
                longitudinal.add(-point.z * scale, point.y * scale, surface.getAsDouble());
                lateral.add(point.x * scale, point.y * scale, surface.getAsDouble());
            }
        }
        float pitch = longitudinal.smoothAngle(current.pitch(), spring);
        float bank = type.isCanRoll() ? lateral.smoothAngle(current.bank(), spring) : SuspensionPhysics.smoothTerrainAngle(current.bank(), 0F, spring);
        return new TerrainAttitude(pitch, bank);
    }

    private static OptionalDouble supportHeight(Entity entity, DriveableType type, Vec3 wheel, double scale)
    {
        float spring = Mth.clamp(type.getWheelSpringStrength(), 0F, 1F);
        double step = Mth.clamp(type.getWheelStepHeight(), 0F, 2.5F) * scale;
        double droop = (0.35D + (1D - spring) * 0.2D) * scale;
        Vec3 start = wheel.add(0D, step + 0.6D * scale, 0D);
        Vec3 end = wheel.add(0D, -droop - step - 0.9D * scale, 0D);
        if (!entity.level().isLoaded(BlockPos.containing(start)) || !entity.level().isLoaded(BlockPos.containing(end)))
            return OptionalDouble.empty();
        var hit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK)
            return OptionalDouble.empty();
        double surface = hit.getLocation().y;
        double error = surface + type.getWheelGroundClearance() * scale - wheel.y;
        return error > Math.max(0.15D * scale, step + 0.1D * scale) ? OptionalDouble.empty() : OptionalDouble.of(surface);
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

        private float smoothAngle(float current, float spring)
        {
            return positive.count > 0 && negative.count > 0 ? SuspensionPhysics.smoothTerrainAngle(current, -positive.angleFrom(negative), spring) : current;
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
