package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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
        return (float) Mth.clamp(tenthsPerSecond / 200D / naturalSpeed(walkingSpeed), 0.05D, 20D);
    }

    /** @return blocks per tick the NPC settles at on land at its full pace: its land speed setting, or what its walking speed comes to */
    public static double topSpeed(EntityNPCInterface npc)
    {
        int tenths = NpcTuning.of(npc).get(Value.LAND_SPEED);
        return tenths > 0 ? tenths / 200D : naturalSpeed(npc.ais.getWalkingSpeed());
    }

    static double naturalSpeed(int walkingSpeed)
    {
        double speed = Math.max(1, walkingSpeed) / 20D;
        return GROUND_SPEED_FACTOR * speed * speed;
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
        return (float) Mth.clamp(speed / (speed + push), 0.5D, 0.995D);
    }

    // Terrain

    /** Whether the profile keeps the NPC at the surface rather than on the bottom. */
    public static boolean floats(Terrain terrain)
    {
        return terrain == Terrain.WATERCRAFT || terrain == Terrain.AMPHIBIOUS;
    }

    /**
     * Applies what a terrain profile needs outside path searches, gives the NPC the server's ranges once an operator has
     * applied them to every NPC, and keeps saved ranges within the server's limit.
     */
    public static void tick(EntityNPCInterface npc)
    {
        Terrain terrain = NpcTuning.of(npc).terrain();
        if (floats(terrain) && NpcSubmarine.runningDepth(npc) == 0 && npc.isInWater() && NpcPlatform.waterHeight(npc) > npc.getFluidJumpThreshold() && npc.getRandom().nextFloat() < 0.8F)
            npc.getJumpControl().jump();
        if ((npc.tickCount + npc.getId()) % REFRESH_INTERVAL != 0)
            return;
        if (floats(terrain))
        {
            npc.getNavigation().setCanFloat(true);
            npc.setPathfindingMalus(BlockPathTypes.WATER, 0F);
        }
        takeServerRanges(npc);
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
     * Gives the NPC the server's aggro and firing ranges when they were applied to every NPC since it last took them, as
     * its own values: a model that supplies a range keeps covering it.
     */
    private static void takeServerRanges(EntityNPCInterface npc)
    {
        NpcServerSettings server = npc.getServer() == null ? null : NpcServerSettings.get(npc.getServer());
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        if (server == null || settings.wolffsmodnpcsRangeGeneration() == server.getRangeGeneration())
            return;
        NpcTypeProperties properties = settings.wolffsmodnpcsTypeProperties();
        properties.setOwn(npc, NpcTypeProperty.AGGRO_RANGE, IntTag.valueOf(server.getAggroRange()));
        properties.setOwn(npc, NpcTypeProperty.RANGE, IntTag.valueOf(server.getFiringRange()));
        settings.wolffsmodnpcsRangeGeneration(server.getRangeGeneration());
        npc.updateAI = true;
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
            case GROUND ->
                type == BlockPathTypes.WATER && depth(level, x, y, z, footprint(mob), tuning.get(Value.MAX_WADE_DEPTH) + 1) > tuning.get(Value.MAX_WADE_DEPTH) ? BlockPathTypes.BLOCKED : type;
            case WATERCRAFT -> watercraft(level, x, y, z, footprint(mob), type, tuning.get(Value.MIN_WATER_DEPTH));
        };
    }

    private static BlockPathTypes watercraft(BlockGetter level, int x, int y, int z, int footprint, BlockPathTypes type, int minimumDepth)
    {
        // Open air has to stay open: the search drops through it to find the water below.
        if (type == BlockPathTypes.OPEN)
            return type;
        return type == BlockPathTypes.WATER && hasWaterDepth(level, x, y, z, footprint, minimumDepth) ? type : BlockPathTypes.BLOCKED;
    }

    /** Every hull column must meet the minimum; one shallow column rejects the node without scanning the rest. */
    static boolean hasWaterDepth(BlockGetter level, int x, int y, int z, int footprint, int minimumDepth)
    {
        for (int column = 0; column < footprint * footprint; column++)
            if (depth(level, x + column / footprint, y, z + column % footprint, minimumDepth) < minimumDepth)
                return false;
        return true;
    }

    /** @return how many blocks wide the path search takes the mob to be, as vanilla does */
    private static int footprint(Mob mob)
    {
        return Mth.floor(mob.getBbWidth() + 1F);
    }

    /**
     * The path search merges the ground under a wide mob into one kind, so the water has to be measured under all of it.
     *
     * @param footprint
     *            blocks the mob covers east and south of the position
     * @return the deepest water column under the footprint, counted no further than the given limit
     */
    static int depth(BlockGetter level, int x, int y, int z, int footprint, int limit)
    {
        // A column reaching the limit already proves that the ground profile cannot wade here.
        int result = 0;
        for (int column = 0; column < footprint * footprint && result < limit; column++)
        {
            int found = depth(level, x + column / footprint, y, z + column % footprint, limit);
            result = Math.max(result, found);
        }
        return result;
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
