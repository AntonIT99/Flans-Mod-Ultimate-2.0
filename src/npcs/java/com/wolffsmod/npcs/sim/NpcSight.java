package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * Line of sight for NPCs. Unlike the vanilla check it is not cut off at 128 blocks, so long-range NPCs can see as far
 * as they may fight, it never loads a chunk to look through it, and it can look through a limited number of
 * transparent blocks: those in the {@code wolffsmodnpcs:see_through} block tag.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSight
{
    /** The vanilla limit of line of sight, which stays the least an NPC can see. */
    private static final double VANILLA_RANGE = 128D;
    /** Blocks beyond the far face of the last block a shot starts at, so it does not start inside the face. */
    private static final double EXIT_MARGIN = 0.1D;

    /** @return how far, in blocks, the NPC can see at all */
    public static double range()
    {
        return Math.max(VANILLA_RANGE, FlansModApi.getNpcTrackingRange());
    }

    /** @return whether the NPC sees the entity, looking through as many transparent blocks as its settings allow */
    public static boolean sees(EntityNPCInterface npc, Entity target)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        return clear(npc, target, tuning.sight() == NpcTuning.Sight.NORMAL ? 0 : tuning.get(Value.VISION_LIMIT));
    }

    /**
     * @param transparent
     *            how many see-through blocks may stand in the way
     * @return whether nothing else stands between the NPC's eyes and the target's
     */
    public static boolean clear(EntityNPCInterface npc, Entity target, int transparent)
    {
        if (target.level() != npc.level())
            return false;
        Vec3 from = npc.getEyePosition();
        Vec3 to = target.getEyePosition();
        if (from.distanceToSqr(to) > range() * range())
            return false;
        return clear(npc.level(), from, to, transparent, npc);
    }

    /**
     * Finds where a shot through transparent blocks should start so that those blocks cannot stop it: the point at
     * which the line of fire leaves the last of them.
     *
     * @param transparent
     *            how many see-through blocks the NPC may fire through
     * @return that point, or the muzzle itself when the line is blocked or crosses no such block
     */
    public static Vec3 launchPoint(EntityNPCInterface npc, Vec3 muzzle, Vec3 target, int transparent)
    {
        if (transparent <= 0 || muzzle.distanceToSqr(target) > range() * range())
            return muzzle;
        Set<BlockPos> crossed = new LinkedHashSet<>();
        Chunks chunks = new Chunks(npc.level());
        ClipContext context = new ClipContext(muzzle, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc);
        Boolean blocked = BlockGetter.traverseBlocks(muzzle, target, context, (clip, position) -> blocks(chunks, clip, position, transparent, crossed) ? Boolean.TRUE : null, clip -> Boolean.FALSE);
        if (Boolean.TRUE.equals(blocked) || crossed.isEmpty())
            return muzzle;
        BlockPos last = null;
        for (BlockPos position : crossed)
            last = position;
        return exit(last, muzzle, target);
    }

    /** @return where a line leaves a block it crosses, a little beyond its far face; the start of the line if it misses the block */
    static Vec3 exit(BlockPos block, Vec3 from, Vec3 to)
    {
        // Entering the block from the far end of the line finds the face the line leaves it by.
        return new AABB(block).clip(to, from).map(face -> face.add(to.subtract(from).normalize().scale(EXIT_MARGIN))).orElse(from);
    }

    /** @return whether nothing but at most the given number of see-through blocks lies between the two points */
    public static boolean clear(Level level, Vec3 from, Vec3 to, int transparent, Entity viewer)
    {
        Set<BlockPos> crossed = transparent > 0 ? new HashSet<>() : Set.of();
        Chunks chunks = new Chunks(level);
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer);
        Boolean blocked = BlockGetter.traverseBlocks(from, to, context, (clip, position) -> blocks(chunks, clip, position, transparent, crossed) ? Boolean.TRUE : null, clip -> Boolean.FALSE);
        return !Boolean.TRUE.equals(blocked);
    }

    private static boolean blocks(Chunks chunks, ClipContext context, BlockPos position, int transparent, Set<BlockPos> crossed)
    {
        Level level = chunks.level;
        // What cannot be looked at without loading it counts as a wall.
        BlockState state = chunks.state(position);
        if (state == null)
            return true;
        if (state.isAir())
            return false;
        VoxelShape shape = context.getBlockShape(state, level, position);
        if (shape.isEmpty() || level.clipWithInteractionOverride(context.getFrom(), context.getTo(), position, shape, state) == null)
            return false;
        if (transparent > 0 && state.is(Tags.SEE_THROUGH))
        {
            crossed.add(position.immutable());
            return crossed.size() > transparent;
        }
        return true;
    }

    /**
     * The blocks along one line. A line stays in a chunk for many blocks, so the chunk is looked up when the line enters it
     * rather than twice for every block.
     */
    private static final class Chunks
    {
        private final Level level;
        private boolean entered;
        private int chunkX;
        private int chunkZ;
        @Nullable
        private LevelChunk chunk;

        private Chunks(Level level)
        {
            this.level = level;
        }

        /** @return the block at the position, or null where it is outside the world or its chunk is not loaded */
        @Nullable
        private BlockState state(BlockPos position)
        {
            if (level.isOutsideBuildHeight(position))
                return null;
            int x = SectionPos.blockToSectionCoord(position.getX());
            int z = SectionPos.blockToSectionCoord(position.getZ());
            if (!entered || x != chunkX || z != chunkZ)
            {
                entered = true;
                chunkX = x;
                chunkZ = z;
                chunk = level.getChunkSource().hasChunk(x, z) ? level.getChunk(x, z) : null;
            }
            return chunk == null ? null : chunk.getBlockState(position);
        }
    }

    /** Held apart so that the geometry above can be used, and tested, before the game's registries exist. */
    private static final class Tags
    {
        /** Blocks an NPC may see or fire through; a data pack can change the list. */
        private static final TagKey<Block> SEE_THROUGH = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "see_through"));
    }
}
