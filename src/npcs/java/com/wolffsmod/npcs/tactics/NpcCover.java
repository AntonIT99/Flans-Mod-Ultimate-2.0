package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.teams.NpcSuppression;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

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
    /** Squared blocks of extra walk an NPC that enters buildings accepts for cover inside one. */
    private static final double INDOOR_PREFERENCE = 48D;
    /** Rings a firing search keeps looking for a window or doorway after finding cover outdoors. */
    private static final int INDOOR_EXTRA_RINGS = 2;

    /** A pending search is distinct from a completed search that found no cover. */
    public record Result<T>(boolean complete, @Nullable T value)
    {
        private static <T> Result<T> pending()
        {
            return new Result<>(false, null);
        }
    }

    /** Small per-NPC continuations, discarded when a tactic or target changes. */
    public static final class Searches
    {
        @Nullable
        private SafeSearch safe;
        @Nullable
        private RingSearch firing;
        private final List<RingSearch> hiding = new ArrayList<>();

        public void clear()
        {
            safe = null;
            firing = null;
            hiding.clear();
        }
    }

    /** One decision's conservative ray allowance; reservations include every possible ray for a candidate. */
    private static final class Slice
    {
        private final EntityNPCInterface npc;
        private final long waited;
        private final boolean urgent;
        private int rays = 128;

        private Slice(EntityNPCInterface npc, long started)
        {
            this.npc = npc;
            waited = npc.level().getGameTime() - started;
            urgent = npc.hurtTime > 0 || NpcSuppression.isPinned(npc);
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Reservations report success; callers return early when capacity is unavailable.
        private boolean reserve(int count)
        {
            if (count > rays || npc.getServer() != null && !NpcBudgets.coverSearch(npc.getServer(), count, waited, urgent))
                return false;
            rays -= count;
            return true;
        }
    }

    private record Candidate(BlockPos point, double score)
    {}

    private static final class SafeSearch
    {
        private final Vec3 origin;
        private final List<Vec3> sources;
        private final int radius;
        @Nullable
        private final NpcCombatIntent intent;
        private final long started;
        private final List<Candidate> choices = new ArrayList<>();
        private final List<BlockPos> accepted = new ArrayList<>();
        private int sample;
        private int validated;
        private boolean sorted;

        private SafeSearch(EntityNPCInterface npc, List<Vec3> sources, int radius, @Nullable NpcCombatIntent intent)
        {
            origin = npc.position();
            this.sources = List.copyOf(sources);
            this.radius = radius;
            this.intent = intent;
            started = npc.level().getGameTime();
        }

        private boolean matches(EntityNPCInterface npc, List<Vec3> current, int reach, @Nullable NpcCombatIntent order)
        {
            boolean sameOrder = Objects.equals(intent, order) || intent != null && order != null && intent.ordered() == order.ordered() && intent.arrival() == order.arrival()
                && intent.radius() == order.radius() && intent.destination().distanceToSqr(order.destination()) <= 4D && intent.centre().distanceToSqr(order.centre()) <= 4D;
            return radius == reach && sameOrder && origin.distanceToSqr(npc.position()) <= 4D && sameSources(sources, current);
        }

        private Result<List<BlockPos>> step(EntityNPCInterface npc, List<Vec3> current)
        {
            Slice slice = new Slice(npc, started);
            if (!collect(npc, slice))
                return Result.pending();
            if (!sorted)
            {
                choices.sort(Comparator.comparingDouble(Candidate::score));
                sorted = true;
            }
            // Recheck the shortlist against current firing positions before handing it to a movement goal.
            while (validated < choices.size() && accepted.size() < 6)
            {
                if (!slice.reserve(current.size() * 2))
                    return Result.pending();
                BlockPos point = choices.get(validated++).point();
                if (!accepted.contains(point) && TeamsBots.coverAvailable(npc, point, npc.level().getGameTime()) && standable(npc.level(), point)
                    && NpcCombatMovement.permits(npc, Vec3.atBottomCenterOf(point)) && hidden(npc, current, Vec3.atBottomCenterOf(point)))
                    accepted.add(point);
            }
            return new Result<>(true, List.copyOf(accepted));
        }

        private boolean collect(EntityNPCInterface npc, Slice slice)
        {
            if (sample >= 128)
                return true;
            List<Vec3> allies = allies(npc);
            int checked = 0;
            while (sample < 128 && checked++ < 16)
            {
                BlockPos point = sample(npc, origin, sample, radius);
                if (point != null)
                {
                    if (!slice.reserve(sources.size() * 4))
                        return false;
                    consider(npc, allies, point);
                }
                sample++;
            }
            return sample >= 128;
        }

        private void consider(EntityNPCInterface npc, List<Vec3> allies, BlockPos point)
        {
            Vec3 stand = Vec3.atBottomCenterOf(point);
            if (!hidden(npc, sources, stand))
                return;
            double score = stand.distanceToSqr(origin) + spacing(npc, allies, stand);
            if (intent != null)
                score -= 12D * intent.progress(origin, stand);
            if (!hidden(npc, sources, origin.lerp(stand, 0.5D)))
                score += 12D;
            // A roof and walls also shelter from shells, splinters and aircraft that the sight lines above do not cover.
            if (NpcBuildings.entersBuildings(npc) && NpcBuildings.indoors(npc.level(), point))
                score -= INDOOR_PREFERENCE;
            choices.add(new Candidate(point, score));
        }

        private static boolean sameSources(List<Vec3> previous, List<Vec3> current)
        {
            if (previous.size() != current.size())
                return false;
            for (int index = 0; index < previous.size(); index++)
                if (previous.get(index).distanceToSqr(current.get(index)) > 16D)
                    return false;
            return true;
        }

        @Nullable
        private static BlockPos sample(EntityNPCInterface npc, Vec3 origin, int sample, int radius)
        {
            double angle = (sample % 16) * Math.PI / 8D + npc.getId() * 0.1D;
            int ring = sample / 16;
            double distance = Math.max(1D, radius * (1 + ring) / 8D);
            BlockPos point = BlockPos.containing(origin.add(Math.cos(angle) * distance, 0D, Math.sin(angle) * distance));
            for (int height : new int[]{0, -1, 1})
            {
                BlockPos stand = point.offset(0, height, 0);
                if (TeamsBots.coverAvailable(npc, stand, npc.level().getGameTime()) && standable(npc.level(), stand) && NpcCombatMovement.permits(npc, Vec3.atBottomCenterOf(stand)))
                    return stand;
            }
            return null;
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
            if (allies.isEmpty())
                return 0D;
            double gap = NpcCombatPolicy.value(npc, Value.ALLY_GAP);
            double penalty = 0D;
            for (Vec3 ally : allies)
                penalty += Math.max(0D, gap * gap - ally.distanceToSqr(point)) * 8D;
            return penalty;
        }
    }

    /** A shortlist of hidden positions. Routes are tried one at a time by the safety goal, including asynchronous results. */
    public static Result<List<BlockPos>> safePositions(EntityNPCInterface npc, List<Vec3> sources, int radius)
    {
        return safePositions(npc, sources, radius, null);
    }

    /** Scores progress toward an order before taking the shortlist, rather than discarding forward cover afterward. */
    public static Result<List<BlockPos>> safePositions(EntityNPCInterface npc, List<Vec3> sources, int radius, @Nullable NpcCombatIntent intent)
    {
        if (sources.isEmpty())
            return new Result<>(true, List.of());
        Searches searches = NpcTacticState.of(npc).getCoverSearches();
        if (searches.safe == null || !searches.safe.matches(npc, sources, radius, intent))
            searches.safe = new SafeSearch(npc, sources, radius, intent);
        Result<List<BlockPos>> result = searches.safe.step(npc, sources);
        if (result.complete())
            searches.safe = null;
        return result;
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
     * @return a pending result, or the nearest cover within firing range of the target; a completed null means no cover
     */
    public static Result<Cover> find(EntityNPCInterface npc, LivingEntity target, int radius, Engagement engagement)
    {
        Searches searches = NpcTacticState.of(npc).getCoverSearches();
        if (searches.firing == null || !searches.firing.matches(npc, target, npc.blockPosition(), radius, engagement.firing()))
            searches.firing = new RingSearch(npc, target, npc.blockPosition(), radius, engagement.firing(), true);
        Result<Cover> result = searches.firing.step();
        if (result.complete())
            searches.firing = null;
        return result;
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
     * @return a pending result, or the hidden place nearest the origin; a completed null means no cover
     */
    public static Result<BlockPos> hide(EntityNPCInterface npc, LivingEntity enemy, BlockPos origin, int radius, double farthest)
    {
        Searches searches = NpcTacticState.of(npc).getCoverSearches();
        RingSearch search = searches.hiding.stream().filter(known -> known.matches(npc, enemy, origin, radius, farthest)).findFirst().orElse(null);
        if (search == null)
        {
            search = new RingSearch(npc, enemy, origin, radius, farthest, false);
            if (searches.hiding.size() >= 3)
                searches.hiding.remove(0);
            searches.hiding.add(search);
        }
        Result<Cover> result = search.step();
        return new Result<>(result.complete(), result.value() == null ? null : result.value().stand());
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
    private static final class RingSearch
    {
        private final EntityNPCInterface npc;
        private final LivingEntity target;
        private final Level level;
        private final double eyeHeight;
        private final BlockPos origin;
        private final int radius;
        private final double farthest;
        private final boolean firing;
        private final Vec3 enemyPosition;
        private final long started;
        private int budget = MAX_HIDDEN_TESTS;
        private int ring;
        private int index;
        private List<BlockPos> blocks = List.of();
        private boolean complete;
        private long completedAt;
        private int blockTests;
        @Nullable
        private Cover found;
        /** Cover outdoors, kept while the search looks a little farther for a window or doorway. */
        @Nullable
        private Cover outdoors;
        private int lastRing;
        private final boolean prefersIndoors;

        private RingSearch(EntityNPCInterface npc, LivingEntity target, BlockPos origin, int radius, double farthest, boolean firing)
        {
            this.npc = npc;
            this.target = target;
            this.level = npc.level();
            this.eyeHeight = npc.getEyeHeight();
            this.origin = origin;
            this.radius = radius;
            this.farthest = farthest;
            this.firing = firing;
            enemyPosition = target.position();
            started = npc.level().getGameTime();
            // Hiding places carry no window to fire from; only firing cover looks for one.
            prefersIndoors = firing && NpcBuildings.entersBuildings(npc);
        }

        private boolean matches(EntityNPCInterface viewer, LivingEntity enemy, BlockPos centre, int reach, double distance)
        {
            return viewer == npc && enemy == target && origin.distSqr(centre) <= 4D && radius == reach && Math.abs(farthest - distance) <= 4D && enemyPosition.distanceToSqr(enemy.position()) <= 16D
                && (!complete || level.getGameTime() - completedAt < 20L);
        }

        private Result<Cover> step()
        {
            Slice slice = new Slice(npc, started);
            if (complete)
                return recheck(slice);
            int checked = 0;
            while (!complete && checked++ < 64 && budget > 0 && blockTests < 2048)
            {
                if (!nextRing())
                {
                    complete = true;
                    break;
                }
                if (!test(blocks.get(index), slice))
                    return Result.pending();
            }
            complete |= budget <= 0 || blockTests >= 2048;
            if (complete && found == null)
                found = outdoors;
            if (complete)
                completedAt = level.getGameTime();
            return new Result<>(complete, found);
        }

        private Result<Cover> recheck(Slice slice)
        {
            if (found != null)
            {
                if (!slice.reserve(1))
                    return Result.pending();
                if (NpcSight.clear(level, target.getEyePosition(), eyes(found.stand(), eyeHeight), 0, npc))
                    found = null;
            }
            return new Result<>(true, found);
        }

        private boolean nextRing()
        {
            if (index < blocks.size())
                return true;
            if (ring > radius || outdoors != null && ring > lastRing)
                return false;
            blocks = ring(origin, ring++);
            index = 0;
            return true;
        }

        /** @return false only when this candidate needs to wait for ray capacity */
        private boolean test(BlockPos stand, Slice slice)
        {
            double distance = Vec3.atBottomCenterOf(stand).distanceTo(target.position());
            if (distance < MIN_TARGET_DISTANCE || distance > farthest || !TeamsBots.coverAvailable(npc, stand, level.getGameTime()) || !standable(stand))
            {
                index++;
                blockTests++;
                return true;
            }
            if (!slice.reserve(firing ? 9 : 1))
                return false;
            index++;
            blockTests++;
            budget--;
            if (NpcSight.clear(level, target.getEyePosition(), eyes(stand, eyeHeight), 0, npc))
                return true;
            BlockPos peek = firing ? peek(stand) : stand;
            if (peek != null)
            {
                if (!prefersIndoors || NpcBuildings.indoors(level, stand))
                {
                    found = new Cover(stand, peek);
                    complete = true;
                }
                else if (outdoors == null)
                {
                    // A window or doorway a little farther protects better than a wall outside.
                    outdoors = new Cover(stand, peek);
                    lastRing = ring - 1 + INDOOR_EXTRA_RINGS;
                }
            }
            return true;
        }

        /** @return a block a step or two aside from the hidden one where the NPC can stand and see the target */
        @Nullable
        private BlockPos peek(BlockPos stand)
        {
            for (int[] step : PEEKS)
            {
                BlockPos peek = stand.offset(step[0], 0, step[1]);
                if (standable(peek) && NpcSight.clear(level, eyes(peek, eyeHeight), target.getEyePosition(), 0, npc))
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
