package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
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

    /** A shortlist of hidden positions. Routes are tried one at a time by the safety goal, including asynchronous results. */
    public static List<BlockPos> safePositions(EntityNPCInterface npc, List<Vec3> sources, int radius)
    {
        return safePositions(npc, sources, radius, null);
    }

    /** Scores progress toward an order before taking the shortlist, rather than discarding forward cover afterward. */
    public static List<BlockPos> safePositions(EntityNPCInterface npc, List<Vec3> sources, int radius, @Nullable NpcCombatIntent intent)
    {
        record Candidate(BlockPos point, double score)
        {}
        List<Candidate> choices = new ArrayList<>();
        List<Vec3> allies = allies(npc);
        for (int sample = 0; sample < 128; sample++)
        {
            BlockPos point = sample(npc, sample, radius);
            if (point != null && hidden(npc, sources, Vec3.atBottomCenterOf(point)))
            {
                Vec3 stand = Vec3.atBottomCenterOf(point);
                double score = stand.distanceToSqr(npc.position()) + spacing(npc, allies, stand);
                if (intent != null)
                    score -= 12D * intent.progress(npc.position(), stand);
                // A short exposed dash may still be the only route to concealment.
                if (!hidden(npc, sources, npc.position().lerp(stand, 0.5D)))
                    score += 12D;
                choices.add(new Candidate(point, score));
            }
        }
        return choices.stream().sorted(Comparator.comparingDouble(Candidate::score)).map(Candidate::point).distinct().limit(6).toList();
    }

    @Nullable
    private static BlockPos sample(EntityNPCInterface npc, int sample, int radius)
    {
        double angle = (sample % 16) * Math.PI / 8D + npc.getId() * 0.1D;
        int ring = sample / 16;
        double distance = Math.max(1D, radius * (1 + ring) / 8D);
        BlockPos point = BlockPos.containing(npc.position().add(Math.cos(angle) * distance, 0D, Math.sin(angle) * distance));
        for (int height : new int[]{0, -1, 1})
        {
            BlockPos stand = point.offset(0, height, 0);
            if (standable(npc.level(), stand) && NpcCombatMovement.permits(npc, Vec3.atBottomCenterOf(stand)))
                return stand;
        }
        return null;
    }

    /** Cover must protect both chest and head from every known firing position, including another attacker. */
    public static boolean hidden(EntityNPCInterface npc, List<Vec3> sources, Vec3 stand)
    {
        for (Vec3 source : sources)
        {
            if (NpcSight.clear(npc.level(), source, stand.add(0D, npc.getEyeHeight(), 0D), 0, npc) || NpcSight.clear(npc.level(), source, stand.add(0D, npc.getBbHeight() * 0.5D, 0D), 0, npc))
                return false;
        }
        return !sources.isEmpty();
    }

    @Nullable
    public static BlockPos peek(EntityNPCInterface npc, BlockPos stand, LivingEntity target)
    {
        for (int[] step : PEEKS)
        {
            BlockPos point = stand.offset(step[0], 0, step[1]);
            if (standable(npc.level(), point) && NpcCombatMovement.permits(npc, Vec3.atBottomCenterOf(point))
                && NpcSight.clear(npc.level(), eyes(point, npc.getEyeHeight()), NpcVehicleThreats.fireOrigin(target), 0, npc))
                return point;
        }
        return null;
    }

    public static boolean standable(Level level, BlockPos stand)
    {
        return level.isLoaded(stand) && !level.getBlockState(stand.below()).getCollisionShape(level, stand.below()).isEmpty() && level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
            && level.getBlockState(stand.above()).getCollisionShape(level, stand.above()).isEmpty();
    }

    private static List<Vec3> allies(EntityNPCInterface npc)
    {
        List<Vec3> positions = new ArrayList<>();
        if (NpcEquipment.enabled(npc, Feature.ALLY_SPACING) && npc.level() instanceof ServerLevel level)
        {
            NpcCombatants combatants = NpcCombatants.of(level);
            for (int index = 0; index < combatants.npcSize(); index++)
            {
                EntityNPCInterface ally = combatants.npc(index).npc();
                if (ally != null && ally != npc && ally.distanceToSqr(npc) < 1024D && NpcDrivers.ally(npc, ally))
                {
                    Vec3 destination = NpcTacticState.of(ally).getSafety().getDestination();
                    positions.add(destination == null ? ally.position() : destination);
                }
            }
        }
        return positions;
    }

    private static double spacing(EntityNPCInterface npc, List<Vec3> allies, Vec3 point)
    {
        double gap = NpcCombatPolicy.value(npc, Value.ALLY_GAP);
        double penalty = 0D;
        for (Vec3 ally : allies)
            penalty += Math.max(0D, gap * gap - ally.distanceToSqr(point)) * 8D;
        return penalty;
    }

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
        Search search = new Search(npc, target);
        BlockPos origin = npc.blockPosition();
        for (int ring = 0; ring <= radius && search.budget > 0; ring++)
        {
            for (BlockPos stand : ring(origin, ring))
            {
                Cover cover = search.test(stand, engagement);
                if (cover != null)
                    return cover;
            }
        }
        return null;
    }

    /**
     * Finds a place hidden from the enemy, with no place to fire from beside it: where an NPC out of its reach waits or
     * moves up to.
     *
     * @param origin
     *            the block the search starts round
     * @param radius
     *            blocks round the origin searched, ring by ring from the nearest
     * @param farthest
     *            blocks from the enemy the place may be at most
     * @return the hidden place nearest the origin, or null where there is none
     */
    @Nullable
    public static BlockPos hide(EntityNPCInterface npc, LivingEntity enemy, BlockPos origin, int radius, double farthest)
    {
        Search search = new Search(npc, enemy);
        for (int ring = 0; ring <= radius && search.budget > 0; ring++)
        {
            for (BlockPos stand : ring(origin, ring))
            {
                if (search.hidden(stand, farthest))
                    return stand;
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
        private final Level level;
        private final Vec3 targetEyes;
        private final double eyeHeight;
        private int budget = MAX_HIDDEN_TESTS;

        private Search(EntityNPCInterface npc, LivingEntity target)
        {
            this.npc = npc;
            this.target = target;
            this.level = npc.level();
            this.targetEyes = target.getEyePosition();
            this.eyeHeight = npc.getEyeHeight();
        }

        @Nullable
        private Cover test(BlockPos stand, Engagement engagement)
        {
            if (!hidden(stand, engagement.firing()))
                return null;
            BlockPos peek = peek(stand);
            return peek == null ? null : new Cover(stand, peek);
        }

        /** @return whether the NPC can stand in the block, no farther from the target than given, where the target cannot see it */
        private boolean hidden(BlockPos stand, double farthest)
        {
            double distance = Vec3.atBottomCenterOf(stand).distanceTo(target.position());
            if (budget <= 0 || distance < MIN_TARGET_DISTANCE || distance > farthest || !standable(stand))
                return false;
            budget--;
            return !NpcSight.clear(level, targetEyes, eyes(stand, eyeHeight), 0, npc);
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
