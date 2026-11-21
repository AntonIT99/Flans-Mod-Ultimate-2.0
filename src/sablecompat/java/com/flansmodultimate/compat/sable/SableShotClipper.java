package com.flansmodultimate.compat.sable;

import com.flansmodultimate.common.raytracing.Raytracer;
import com.flansmodultimate.common.raytracing.Raytracer.BlockContact;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Predicate;

public final class SableShotClipper
{
    private SableShotClipper()
    {
    }

    public static BlockContact nearest(Level level, Vec3 from, Vec3 to, Set<BlockPos> visited, BlockContact nearest)
    {
        var bounds = new BoundingBox3d(from.x, from.y, from.z, to.x, to.y, to.z);
        for (SubLevel structure : Sable.HELPER.getAllIntersecting(level, bounds))
        {
            nearest = clipStructure(level, from, to, structure.logicalPose(), pos -> !visited.contains(pos) && Sable.HELPER.getContaining(level, pos) == structure, nearest);
        }
        return nearest;
    }

    static BlockContact clipStructure(BlockGetter level, Vec3 from, Vec3 to, Pose3dc pose, Predicate<BlockPos> include, BlockContact nearest)
    {
        var hit = Raytracer.clipBlocks(level, pose.transformPositionInverse(from), pose.transformPositionInverse(to), include);
        if (hit.getType() == HitResult.Type.MISS)
            return nearest;
        Vec3 position = pose.transformPosition(hit.getLocation());
        if (nearest.local().getType() != HitResult.Type.MISS && position.distanceToSqr(from) >= nearest.position().distanceToSqr(from))
            return nearest;
        return new BlockContact(hit, position, pose.transformNormal(Vec3.atLowerCornerOf(hit.getDirection().getNormal())).normalize());
    }
}
