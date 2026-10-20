package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcSight;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds cover from a target: a place to stand where something solid is between the target's eyes and the NPC's, with a
 * place a step or two to the side where the NPC sees the target and can fire. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCover
{
    /** Hidden places tested for a place to step out to before the search gives up, so that open ground stays cheap. */
    static final int MAX_HIDDEN_TESTS = 48;
    /** Nearest to its target cover may be. */
    static final double MIN_TARGET_DISTANCE = 3D;
    /** Steps to the side, along each axis, at which the NPC may step out of cover. */
    private static final int[][] PEEKS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};

    /**
     * Where the NPC hides, and where it steps out to fire.
     *
     * @param stand
     *            the block it stands in to hide
     * @param peek
     *            the block it stands in to fire
     */
    public record Cover(BlockPos stand, BlockPos peek)
    {
        public Vec3 standPoint()
        {
            return Vec3.atBottomCenterOf(stand);
        }

        public Vec3 peekPoint()
        {
            return Vec3.atBottomCenterOf(peek);
        }
    }

    /**
     * @param radius
     *            blocks round the NPC searched, ring by ring from the nearest
     * @return the nearest cover within its firing range of the target, or null where there is none
     */
    @Nullable
    public static Cover find(EntityNPCInterface npc, LivingEntity target, int radius, Engagement engagement)
    {
        Search search = new Search(npc, target, engagement);
        BlockPos origin = npc.blockPosition();
        for (int ring = 0; ring <= radius && search.budget > 0; ring++)
        {
            for (BlockPos stand : ring(origin, ring))
            {
                Cover cover = search.test(stand);
                if (cover != null)
                    return cover;
            }
        }
        return null;
    }

    /** @return the blocks at the given horizontal distance from the origin, counted as the larger of the two axes, a block above and below included */
    static List<BlockPos> ring(BlockPos origin, int ring)
    {
        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = -ring; dx <= ring; dx++)
        {
            // Inner columns of the square belong to smaller rings, except on its edges.
            int step = Math.abs(dx) == ring ? 1 : Math.max(1, 2 * ring);
            for (int dz = -ring; dz <= ring; dz += step)
                for (int dy = -1; dy <= 1; dy++)
                    blocks.add(origin.offset(dx, dy, dz));
        }
        return blocks;
    }

    /** One search: what it looks for, and how many more hidden places it may test. */
    private static final class Search
    {
        private final EntityNPCInterface npc;
        private final LivingEntity target;
        private final Engagement engagement;
        private final Level level;
        private final Vec3 targetEyes;
        private final double eyeHeight;
        private int budget = MAX_HIDDEN_TESTS;

        private Search(EntityNPCInterface npc, LivingEntity target, Engagement engagement)
        {
            this.npc = npc;
            this.target = target;
            this.engagement = engagement;
            this.level = npc.level();
            this.targetEyes = target.getEyePosition();
            this.eyeHeight = npc.getEyeHeight();
        }

        @Nullable
        private Cover test(BlockPos stand)
        {
            double distance = Vec3.atBottomCenterOf(stand).distanceTo(target.position());
            if (budget <= 0 || distance < MIN_TARGET_DISTANCE || distance > engagement.firing() || !standable(stand))
                return null;
            budget--;
            if (NpcSight.clear(level, targetEyes, eyes(stand, eyeHeight), 0, npc))
                return null;
            BlockPos peek = peek(stand);
            return peek == null ? null : new Cover(stand, peek);
        }

        /** @return a block a step or two aside from the hidden one where the NPC can stand and see the target */
        @Nullable
        private BlockPos peek(BlockPos stand)
        {
            for (int[] step : PEEKS)
            {
                BlockPos peek = stand.offset(step[0], 0, step[1]);
                if (standable(peek) && NpcSight.clear(level, eyes(peek, eyeHeight), targetEyes, 0, npc))
                    return peek;
            }
            return null;
        }

        /** @return whether an NPC can stand in the block: solid ground under it and room for its body */
        private boolean standable(BlockPos stand)
        {
            if (!level.isLoaded(stand))
                return false;
            BlockPos ground = stand.below();
            return !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty() && level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
                && level.getBlockState(stand.above()).getCollisionShape(level, stand.above()).isEmpty();
        }
    }

    /** @return whether the target can see into the cover any more */
    public static boolean exposed(EntityNPCInterface npc, LivingEntity target, Cover cover)
    {
        return NpcSight.clear(npc.level(), target.getEyePosition(), eyes(cover.stand(), npc.getEyeHeight()), 0, npc);
    }

    private static Vec3 eyes(BlockPos stand, double eyeHeight)
    {
        return Vec3.atBottomCenterOf(stand).add(0D, eyeHeight, 0D);
    }
}
