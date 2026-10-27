package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.IMechaType;
import com.flansmodultimate.api.driveables.MechaMobility;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import com.wolffsmod.npcs.sim.NpcTuning.Terrain;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * Where and how fast an NPC travels: its terrain profile, its land and water speeds, the steps it climbs and how it jumps,
 * and how far its path searches reach. Server side, but for the step height and jump, which both sides move by.
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
    /** Blocks below its feet a floating NPC looks for water, and above its waterline it falls rather than sinks. */
    private static final double WATERLINE_REACH = 2D;
    /** Share of the distance to its waterline a floating NPC closes per tick. */
    private static final double WATERLINE_RESPONSE = 0.25D;
    private static final double WATERLINE_MAX_SPEED = 0.3D;
    /** Blocks below its feet water still keeps a vehicle model afloat, as one floating at its waterline may stand above it. */
    private static final double AFLOAT_REACH = 0.5D;
    /** Blocks a floating NPC looks upward for the water surface. */
    private static final int SURFACE_SEARCH = 12;
    /** Gravity of a mob, in blocks per tick squared, and the share of the ideal rise a jump keeps against air drag. */
    private static final double MOB_GRAVITY = 0.08D;
    private static final double MOB_JUMP_EFFICIENCY = 0.42D / Math.sqrt(2D * 0.08D * 1.25D);
    /** Blocks a mecha clears beyond its {@code JumpHeight}, as its jump velocity is worked out. */
    private static final double MECHA_JUMP_CLEARANCE = 0.2D;

    // Speed

    /**
     * @return the factor that brings the NPC's walking speed to its land speed: its setting, or the top speed of the
     *         vehicle model it inherits; 1 without either or in water. The walking speed of a mob follows the square of its
     *         speed value, which the estimate allows for.
     */
    public static float landFactor(EntityNPCInterface npc)
    {
        if (npc.isInWater())
            return 1F;
        int tenths = NpcTuning.of(npc).get(Value.LAND_SPEED);
        if (tenths > 0)
            return landFactor(tenths, npc.ais.getWalkingSpeed());
        OptionalDouble inherited = modelSpeed(npc);
        return inherited.isPresent() ? landFactor(inherited.getAsDouble(), npc.ais.getWalkingSpeed()) : 1F;
    }

    static float landFactor(int tenthsPerSecond, int walkingSpeed)
    {
        return landFactor(tenthsPerSecond / 200D, walkingSpeed);
    }

    static float landFactor(double blocksPerTick, int walkingSpeed)
    {
        return (float) Mth.clamp(blocksPerTick / naturalSpeed(walkingSpeed), 0.05D, 20D);
    }

    /**
     * @return blocks per tick the NPC settles at at its full pace: its land speed setting, or its water speed setting while a
     *         vehicle model keeps it afloat, the top speed of the vehicle model it inherits there, or what its walking speed
     *         comes to
     */
    public static double topSpeed(EntityNPCInterface npc)
    {
        int tenths = NpcTuning.of(npc).get(afloat(npc) ? Value.WATER_SPEED : Value.LAND_SPEED);
        if (tenths > 0)
            return tenths / 200D;
        return modelSpeed(npc).orElseGet(() -> naturalSpeed(npc.ais.getWalkingSpeed()));
    }

    /**
     * @return whether the NPC drives as a vehicle model in water, which moves it as the vehicle moves there: at its water
     *         speed and handling, along its hull
     */
    public static boolean afloat(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || !(model.getInfoType() instanceof IDriveableType))
            return false;
        if (npc.isInWater())
            return true;
        // An NPC not yet in a level has nothing to float on.
        return npc.level() != null && npc.level().getFluidState(BlockPos.containing(npc.getX(), npc.getY() - AFLOAT_REACH, npc.getZ())).is(FluidTags.WATER);
    }

    /**
     * @return the top speed of the vehicle model the NPC drives as, when it drives like one and takes the model's
     *         properties, or the walking speed of the mecha model it walks as, when it takes the model's properties
     */
    public static OptionalDouble modelSpeed(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        FlanModelEntity model = NpcModels.flan(npc);
        if (model != null && options.enabled(Feature.TYPE_PROPERTIES) && model.getInfoType() instanceof IMechaType)
            return FlansEntityTypes.getMechaMobility(model.getInfoType()).map(mecha -> OptionalDouble.of(mecha.walkSpeed())).orElse(OptionalDouble.empty());
        if (!options.enabled(Feature.HULL_FACES_TRAVEL) || !options.enabled(Feature.DRIVING))
            return OptionalDouble.empty();
        return NpcDriving.propulsion(npc, options).map(found -> OptionalDouble.of(found.topSpeed())).orElse(OptionalDouble.empty());
    }

    static double naturalSpeed(int walkingSpeed)
    {
        double speed = Math.max(1, walkingSpeed) / 20D;
        return GROUND_SPEED_FACTOR * speed * speed;
    }

    /**
     * @param blocksPerTick
     *            speed the NPC should settle at on ordinary ground
     * @return the Custom NPCs walking speed that comes closest to it: the walking speed of a mob follows the square of its
     *         speed value, as {@link #naturalSpeed} says
     */
    public static double walkingSpeedFor(double blocksPerTick)
    {
        return Double.isFinite(blocksPerTick) && blocksPerTick > 0D ? 20D * Math.sqrt(blocksPerTick / GROUND_SPEED_FACTOR) : 0D;
    }

    // Steps and jumps

    /**
     * @return the highest step the NPC climbs without jumping: its step height setting, or the step height of the vehicle or
     *         mecha model it shows, at its scale; empty to keep what Custom NPCs gives it
     */
    public static OptionalDouble stepHeight(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        FlanModelEntity model = NpcModels.flan(npc);
        if (!options.enabled(Feature.STEP_CLIMB) || model == null || keepsMobSteps(model.getInfoType()))
            return OptionalDouble.empty();
        int tenths = NpcTuning.of(npc).get(Value.STEP_HEIGHT);
        if (tenths > 0)
            return OptionalDouble.of(tenths / 10D);
        if (!options.enabled(Feature.TYPE_PROPERTIES))
            return OptionalDouble.empty();
        OptionalDouble step = FlansEntityTypes.getStepHeight(model.getInfoType());
        // A mecha without a step height climbs only by jumping, which a mob does by itself.
        return step.isPresent() && step.getAsDouble() > 0D ? OptionalDouble.of(step.getAsDouble() * NpcModels.scale(npc)) : OptionalDouble.empty();
    }

    /** @return the step height of the model the NPC shows, at its scale, for the editor; empty when it has none to inherit */
    public static OptionalDouble modelStepHeight(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || keepsMobSteps(model.getInfoType()) || !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.TYPE_PROPERTIES))
            return OptionalDouble.empty();
        OptionalDouble step = FlansEntityTypes.getStepHeight(model.getInfoType());
        return step.isPresent() ? OptionalDouble.of(step.getAsDouble() * NpcModels.scale(npc)) : OptionalDouble.empty();
    }

    /** @return whether the model is anything but a vehicle or mecha, which climb steps of their own */
    private static boolean keepsMobSteps(@Nullable IContentType type)
    {
        FlanModelKind kind = type == null ? null : FlanModelKind.of(type);
        return kind != FlanModelKind.VEHICLE && kind != FlanModelKind.MECHA;
    }

    /**
     * @return whether the NPC, a ground vehicle model climbing by its step height, stays on its wheels or tracks rather than
     *         jumping like a mob: only once its step height carries it up a block, so that it never gets stuck where a mob
     *         would have jumped
     */
    public static boolean keepsWheelsDown(EntityNPCInterface npc)
    {
        if (npc.isInWater() || npc.isInLava())
            return false;
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || FlanModelKind.of(model.getInfoType()) != FlanModelKind.VEHICLE)
            return false;
        OptionalDouble step = stepHeight(npc);
        return step.isPresent() && step.getAsDouble() >= 1D;
    }

    /**
     * @param original
     *            the power the NPC jumps with otherwise
     * @return the power that lifts a mecha model as high as its definition's {@code JumpHeight} does the mecha, under a
     *         mob's gravity; the original for any other NPC or a mecha without its own jump
     */
    public static float jumpPower(EntityNPCInterface npc, float original)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || !options.enabled(Feature.STEP_CLIMB) || !options.enabled(Feature.TYPE_PROPERTIES) || !(model.getInfoType() instanceof IMechaType))
            return original;
        OptionalDouble height = FlansEntityTypes.getMechaMobility(model.getInfoType()).map(MechaMobility::jumpHeight).orElse(OptionalDouble.empty());
        return height.isPresent() ? jumpPower(height.getAsDouble() * NpcModels.scale(npc)) : original;
    }

    /**
     * The mecha clears {@code JumpHeight} plus a fifth of a block; a mob's jump of 0.42 clears 1.25 blocks, which gives the
     * share air drag takes from the ideal rise.
     *
     * @return the jump power that clears the given height under a mob's gravity
     */
    static float jumpPower(double height)
    {
        return (float) (MOB_JUMP_EFFICIENCY * Math.sqrt(2D * MOB_GRAVITY * Math.max(0D, height + MECHA_JUMP_CLEARANCE)));
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
     * Applies what a terrain profile needs outside path searches, gives the NPC the server's ranges and search intervals
     * once an operator has applied them to every NPC and the server's share of health, lets a dormant NPC rest, and keeps
     * saved ranges within the server's limit.
     */
    public static void tick(EntityNPCInterface npc)
    {
        Terrain terrain = NpcTuning.of(npc).terrain();
        if (floats(terrain) && NpcSubmarine.runningDepth(npc) == 0)
            floatAtSurface(npc);
        if ((npc.tickCount + npc.getId()) % REFRESH_INTERVAL != 0)
            return;
        if (floats(terrain))
        {
            npc.getNavigation().setCanFloat(true);
            npc.setPathfindingMalus(BlockPathTypes.WATER, 0F);
        }
        takeServerRanges(npc);
        if (npc.getServer() != null)
            NpcServerRules.refresh(npc, NpcServerSettings.get(npc.getServer()));
        NpcDormancy.rest(npc);
        int limit = rangeLimit();
        if (npc.stats.aggroRange > limit)
            npc.stats.setAggroRange(limit);
        if (npc.stats.ranged.getRange() > limit)
            npc.stats.ranged.setRange(limit);
    }

    /**
     * Holds a driveable model at the waterline its driveable floats at, with a damped correction so it neither rides
     * high nor bobs; any other NPC swims up by jumping, as vanilla mobs do.
     */
    private static void floatAtSurface(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || model.getModelType() == null || !(model.getModelType().getInfoType() instanceof IDriveableType driveable))
        {
            if (npc.isInWater() && NpcPlatform.waterHeight(npc) > npc.getFluidJumpThreshold() && npc.getRandom().nextFloat() < 0.8F)
                npc.getJumpControl().jump();
            return;
        }
        double surface = waterSurface(npc);
        if (Double.isNaN(surface))
        {
            if (npc.isInWater())
                npc.getJumpControl().jump();
            return;
        }
        double target = surface + (driveable.getFloatingHeight() - model.getModelType().getShape().modelHeight()) * NpcModels.scale(npc) - NpcModels.bodyOffset(npc);
        double error = target - npc.getY();
        // Out of the water and above its waterline, it falls back in under gravity.
        if (error < -WATERLINE_REACH)
            return;
        double vertical = Mth.clamp(error * WATERLINE_RESPONSE, -WATERLINE_MAX_SPEED, WATERLINE_MAX_SPEED);
        npc.setDeltaMovement(npc.getDeltaMovement().x, vertical, npc.getDeltaMovement().z);
        npc.fallDistance = 0F;
    }

    /** @return world Y of the surface of the water column around the NPC's feet, or NaN when there is none within reach */
    private static double waterSurface(EntityNPCInterface npc)
    {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(npc.getX(), npc.getY() - WATERLINE_REACH, npc.getZ()).mutable();
        boolean water = false;
        for (int step = 0; step <= SURFACE_SEARCH; step++, cursor.move(0, 1, 0))
        {
            boolean here = npc.level().getFluidState(cursor).is(FluidTags.WATER);
            if (water && !here)
            {
                cursor.move(0, -1, 0);
                return cursor.getY() + npc.level().getFluidState(cursor).getHeight(npc.level(), cursor);
            }
            water |= here;
        }
        return Double.NaN;
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
