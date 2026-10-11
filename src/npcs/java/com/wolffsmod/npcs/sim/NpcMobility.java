package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeMod;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import java.util.Set;

/**
 * Where and how fast an NPC travels: its terrain profile, its land and water speeds, and how far its path searches
 * reach. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcMobility
{
    /** Blocks per tick a walking mob settles at, per unit of its speed squared, on ordinary ground. */
    private static final double GROUND_SPEED_FACTOR = 2.158D;
    /** Push a swimming mob gets per tick for a full movement input. */
    private static final double WATER_PUSH = 0.02D;
    /** A path search never looks at less than this many blocks, as in vanilla. */
    static final double MIN_SEARCH_RANGE = 32D;
    private static final int REFRESH_INTERVAL = 20;
    /** Custom NPCs' editor limit of the ranged attack range. */
    private static final int EDITOR_RANGE_LIMIT = 256;

    // Speed

    /**
     * @return the factor that brings the NPC's walking speed to its land speed setting, or 1 without one or in water.
     *         The walking speed of a mob follows the square of its speed value, which the estimate allows for.
     */
    public static float landFactor(EntityNPCInterface npc)
    {
        int tenths = NpcTuning.of(npc).get(Value.LAND_SPEED);
        if (tenths <= 0 || npc.isInWater())
            return 1F;
        return landFactor(tenths, npc.ais.getWalkingSpeed());
    }

    static float landFactor(int tenthsPerSecond, int walkingSpeed)
    {
        double speed = Math.max(1, walkingSpeed) / 20D;
        double natural = GROUND_SPEED_FACTOR * speed * speed;
        return (float) Math.max(0.05D, Math.min(20D, tenthsPerSecond / 200D / natural));
    }

    /**
     * @param original
     *            the share of its speed the NPC keeps each tick in water
     * @return the share that lets it settle at its water speed setting; the original without one or while it is not
     *         trying to move, so that it still stops
     */
    public static float waterSlowDown(EntityNPCInterface npc, float original)
    {
        int tenths = NpcTuning.of(npc).get(Value.WATER_SPEED);
        if (tenths <= 0)
            return original;
        return waterSlowDown(tenths, Math.min(1D, Math.sqrt(npc.xxa * npc.xxa + npc.zza * npc.zza)), original);
    }

    static float waterSlowDown(int tenthsPerSecond, double input, float original)
    {
        double push = WATER_PUSH * input;
        if (push < 1.0E-5D)
            return original;
        double speed = tenthsPerSecond / 200D;
        return (float) Math.max(0.5D, Math.min(0.995D, speed / (speed + push)));
    }

    // Terrain

    /** Whether the profile keeps the NPC at the surface rather than on the bottom. */
    public static boolean floats(Terrain terrain)
    {
        return terrain == Terrain.WATERCRAFT || terrain == Terrain.AMPHIBIOUS;
    }

    /** Applies what a terrain profile needs outside path searches, and keeps saved ranges within the server's limit. */
    public static void tick(EntityNPCInterface npc)
    {
        Terrain terrain = NpcTuning.of(npc).terrain();
        if (floats(terrain) && npc.isInWater() && npc.getFluidTypeHeight(ForgeMod.WATER_TYPE.get()) > npc.getFluidJumpThreshold() && npc.getRandom().nextFloat() < 0.8F)
            npc.getJumpControl().jump();
        if ((npc.tickCount + npc.getId()) % REFRESH_INTERVAL != 0)
            return;
        if (floats(terrain))
        {
            npc.getNavigation().setCanFloat(true);
            npc.setPathfindingMalus(BlockPathTypes.WATER, 0F);
        }
        int limit = rangeLimit();
        if (npc.stats.aggroRange > limit)
            npc.stats.setAggroRange(limit);
        if (npc.stats.ranged.getRange() > limit)
            npc.stats.ranged.setRange(limit);
    }

    /** @return the farthest, in blocks, an NPC may notice or fire at an enemy on this server */
    public static int rangeLimit()
    {
        return Math.max(EDITOR_RANGE_LIMIT, FlansModApi.getNpcTrackingRange());
    }

    /**
     * Narrows the kind of ground a path search found to what the NPC's terrain profile accepts.
     *
     * @param type
     *            what the search made of the position
     * @return the same kind, or {@link BlockPathTypes#BLOCKED} where the profile does not go
     */
    public static BlockPathTypes filter(Mob mob, BlockGetter level, int x, int y, int z, BlockPathTypes type)
    {
        if (!(mob instanceof EntityNPCInterface npc))
            return type;
        NpcTuning tuning = NpcTuning.of(npc);
        return switch (tuning.terrain())
        {
            case DEFAULT, AMPHIBIOUS -> type;
            case GROUND -> type == BlockPathTypes.WATER && depth(level, x, y, z, tuning.get(Value.MAX_WADE_DEPTH) + 1) > tuning.get(Value.MAX_WADE_DEPTH) ? BlockPathTypes.BLOCKED : type;
            case WATERCRAFT -> watercraft(level, x, y, z, type, tuning.get(Value.MIN_WATER_DEPTH));
        };
    }

    private static BlockPathTypes watercraft(BlockGetter level, int x, int y, int z, BlockPathTypes type, int minimumDepth)
    {
        // Open air has to stay open: the search drops through it to find the water below.
        if (type == BlockPathTypes.OPEN)
            return type;
        return type == BlockPathTypes.WATER && depth(level, x, y, z, minimumDepth) >= minimumDepth ? type : BlockPathTypes.BLOCKED;
    }

    /** @return how many water blocks are stacked through the position, counted no further than the given limit */
    static int depth(BlockGetter level, int x, int y, int z, int limit)
    {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x, y, z);
        if (!level.getFluidState(position).is(FluidTags.WATER))
            return 0;
        int depth = 1;
        for (int up = 1; depth < limit && level.getFluidState(position.set(x, y + up, z)).is(FluidTags.WATER); up++)
            depth++;
        for (int down = 1; depth < limit && level.getFluidState(position.set(x, y - down, z)).is(FluidTags.WATER); down++)
            depth++;
        return depth;
    }

    // Path searches

    /**
     * @param followRange
     *            how far the search would reach by default: the NPC's aggro range, which may be hundreds of blocks
     * @return how far a search for the given goals needs to reach: the distance to the nearest goal plus a detour
     *         margin, never below the vanilla minimum and never above the default
     */
    public static float searchRange(Mob mob, Set<BlockPos> goals, float followRange)
    {
        if (!(mob instanceof EntityNPCInterface npc) || goals.isEmpty() || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.ADAPTIVE_PATH_RANGE))
            return followRange;
        double nearest = Double.MAX_VALUE;
        for (BlockPos goal : goals)
            nearest = Math.min(nearest, goal.distToCenterSqr(mob.getX(), mob.getY(), mob.getZ()));
        return searchRange(Math.sqrt(nearest), NpcTuning.of(npc).get(Value.PATH_MARGIN), followRange);
    }

    static float searchRange(double distance, int margin, float followRange)
    {
        return (float) Math.min(followRange, Math.max(MIN_SEARCH_RANGE, distance + margin));
    }

    /** @return ticks a pursuit keeps its path before searching again, or 0 to search on every call as Custom NPCs does */
    public static int pursuitInterval(Mob mob)
    {
        if (!(mob instanceof EntityNPCInterface npc) || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.ADAPTIVE_PATH_RANGE))
            return 0;
        return NpcTuning.of(npc).get(Value.PATH_RETRY_INTERVAL);
    }
}
