package com.flansmodultimate.compat.sable;

import com.flansmodultimate.common.entity.geometry.EmplacementFrame;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SableEmplacementFrame(Pose3dc pose) implements EmplacementFrame
{
    public static EmplacementFrame at(Level level, BlockPos pos)
    {
        var structure = Sable.HELPER.getContaining(level, pos);
        return structure == null ? TERRAIN : new SableEmplacementFrame(structure.logicalPose());
    }

    @Override
    public Vec3 worldPosition(Vec3 local) { return pose.transformPosition(local); }

    @Override
    public Vec3 localPosition(Vec3 world) { return pose.transformPositionInverse(world); }

    @Override
    public Vec3 worldDirection(Vec3 local) { return pose.transformNormal(local).normalize(); }

    @Override
    public Vec3 localDirection(Vec3 world) { return pose.transformNormalInverse(world).normalize(); }
}
