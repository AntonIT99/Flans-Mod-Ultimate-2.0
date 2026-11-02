package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Surface spray for impacts and water entry. Never loads a chunk or projects a deep explosion to the surface. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WaterEffects
{
    /** Returns the top of nearby water, or NaN when the source is dry, too deep, or outside loaded chunks. */
    public static double surface(Level level, Vec3 position, int maxDepth)
    {
        BlockPos block = BlockPos.containing(position);
        if (!level.hasChunkAt(block) || !level.getFluidState(block).is(FluidTags.WATER))
            return Double.NaN;
        for (int i = 0; i <= Math.min(maxDepth, 8); i++)
        {
            BlockPos at = block.above(i);
            if (!level.hasChunkAt(at))
                return Double.NaN;
            if (!level.getFluidState(at).is(FluidTags.WATER))
                return at.getY();
        }
        return Double.NaN;
    }

    public static void splash(Level level, Vec3 position, float strength)
    {
        if (!level.isClientSide || !Float.isFinite(strength) || strength <= 0F)
            return;
        double surface = surface(level, position, 6);
        if (!Double.isFinite(surface))
            return;
        int count = Math.min(12, Math.max(2, (int) Math.ceil(strength * 2F)));
        for (int i = 0; i < count; i++)
        {
            double angle = i * Math.PI * 2D / count;
            double speed = Math.min(0.18D, strength * 0.03D);
            ParticleHelper.spawnFromString(strength < 1F ? FlanParticles.FM_WATER_MINI : FlanParticles.FM_WATER, position.x, surface, position.z, Math.cos(angle) * speed,
                0.12D + Math.min(strength, 6F) * 0.06D, Math.sin(angle) * speed, Math.min(strength, 3F));
        }
    }
}
