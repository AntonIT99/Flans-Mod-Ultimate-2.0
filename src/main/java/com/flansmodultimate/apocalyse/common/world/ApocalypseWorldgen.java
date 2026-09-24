package com.flansmodultimate.apocalyse.common.world;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.entity.WorldgenSpawnMarker;
import com.flansmodultimate.apocalyse.common.util.ApocalypseDriveableHelper;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The Apocalypse's per-chunk generators.
 *
 * <p>Block placement runs from {@link ApocalypseChunkFeature} on a worldgen worker thread, so
 * everything here writes through a {@link WorldGenLevel} and stays within the chunk being
 * decorated and its direct neighbours, the only ones a feature may write to. Spawns that need
 * the server thread are left to a {@link WorldgenSpawnMarker}, which calls back into
 * {@link #runDeferredSpawn} once the chunk is live.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseWorldgen
{
    /** Decorates one wasteland chunk. */
    public static boolean generateWasteland(WorldGenLevel level, ChunkPos chunkPos)
    {
        if (!ModApocalypseConfig.apocalypseWorldgenEnabled() || !ModApocalypseConfig.apocalypseDimensionEnabled())
            return false;

        RandomSource random = chunkRandom(level, chunkPos);

        // Ores, sulphur-pit acid lakes, dungeons, ravines and mineshafts are regular biome
        // features and structures in the flansmodapocalypse worldgen data.
        ApocalypseRoads.generate(level, chunkPos);
        ApocalypseStructures.generateBossPillars(level, chunkPos, random);
        ApocalypseVillage.generate(level, chunkPos);

        if (random.nextInt(ModApocalypseConfig.apocalypseDeadTreeRarity()) == 0)
            generateDeadTree(level, random, randomSurfacePos(level, chunkPos, random));
        if (random.nextInt(ModApocalypseConfig.apocalypseSkeletonRarity()) == 0)
            generateSkeletonDisplay(level, random, randomSurfacePos(level, chunkPos, random));
        if (ModApocalypseConfig.apocalypsePortalsEnabled()
            && random.nextInt(ModApocalypseConfig.apocalypseAbandonedPortalRarity()) == 0)
            generateAbandonedPortal(level, random, randomSurfacePos(level, chunkPos, random));

        // Labs and airfields span several chunks: each chunk decides from its region's seed
        // whether it holds a piece, and builds only that piece. Both need the high plateau.
        ApocalypseStructures.generateResearchLab(level, chunkPos);
        ApocalypseStructures.generateRunway(level, chunkPos);
        if (random.nextInt(ModApocalypseConfig.apocalypseDyeFactoryRarity()) == 0)
            ApocalypseStructures.generateDyeFactory(level, chunkPos, random);
        if (random.nextInt(ModApocalypseConfig.apocalypseVehicleRarity()) == 0)
            markIfStandable(level, WorldgenSpawnMarker.Kind.ABANDONED_VEHICLE, randomSurfacePos(level, chunkPos, random), random);
        if (ModApocalypseConfig.apocalypseMobsEnabled() && random.nextInt(ModApocalypseConfig.apocalypseSurvivorRarity()) == 0)
        {
            BlockPos pos = randomSurfacePos(level, chunkPos, random);
            if (isClear(level, pos))
                mark(level, WorldgenSpawnMarker.Kind.SURVIVOR, pos, random);
        }
        return true;
    }

    /** Seeds the occasional abandoned portal outside the wasteland. */
    public static boolean generateOverworldPortal(WorldGenLevel level, ChunkPos chunkPos)
    {
        if (!ModApocalypseConfig.apocalypseWorldgenEnabled()
            || !ModApocalypseConfig.apocalypseDimensionEnabled()
            || !ModApocalypseConfig.apocalypsePortalsEnabled()
            || !ModApocalypseConfig.apocalypseOverworldPortalGenerationEnabled()
            || !hasApocalypseDimension(level.getServer()))
            return false;

        RandomSource random = chunkRandom(level, chunkPos);
        if (random.nextInt(ModApocalypseConfig.apocalypseAbandonedPortalOverworldRarity()) != 0)
            return false;
        mark(level, WorldgenSpawnMarker.Kind.OVERWORLD_PORTAL, randomSurfacePos(level, chunkPos, random), random);
        return true;
    }

    /**
     * Whether this world has the Apocalypse dimension. Worlds choose it when they are created or
     * first opened, and a world that declined it gets no portals that would lead nowhere.
     */
    public static boolean hasApocalypseDimension(@Nullable MinecraftServer server)
    {
        return server != null && server.getLevel(ApocalypseContent.APOCALYPSE_LEVEL) != null;
    }

    /** Performs a spawn left behind by worldgen, now on the server thread in a live chunk. */
    public static void runDeferredSpawn(ServerLevel level, WorldgenSpawnMarker.Kind kind, Vec3 position, RandomSource random)
    {
        BlockPos pos = BlockPos.containing(position);
        FlansMod.log.debug("Apocalypse worldgen {} spawn at {}", kind, pos);
        switch (kind)
        {
            case SURVIVOR -> spawnSurvivor(level, pos);
            case ABANDONED_VEHICLE -> generateAbandonedVehicle(level, random, pos);
            case LAB_MECHA ->
            {
                if (ModApocalypseConfig.apocalypseMobsEnabled())
                    ApocalypseDriveableHelper.spawnDungeonMecha(level, pos, random);
            }
            case PARKED_PLANE -> ApocalypseDriveableHelper.spawnParkedPlane(level, position.x, position.y, position.z, random);
            case APOCALYPSE_PORTAL ->
            {
                if (!ModApocalypseConfig.apocalypsePortalsEnabled() || !ApocalypsePortalManager.createPortal(level, pos, null))
                    return;
                ApocalypseStructures.placePortalCache(level, random, pos);
                if (random.nextBoolean())
                    postGuard(level, random, pos.getX() + (random.nextBoolean() ? 6 : -3), pos.getZ() + (random.nextBoolean() ? 6 : -3));
            }
            case OVERWORLD_PORTAL ->
            {
                if (ModApocalypseConfig.apocalypsePortalsEnabled() && ModApocalypseConfig.apocalypseOverworldPortalGenerationEnabled()
                    && hasApocalypseDimension(level.getServer()))
                    ApocalypsePortalManager.createPortal(level, pos, null);
            }
        }
    }

    /** Leaves a marker for a server-thread spawn at {@code pos}. */
    static void mark(WorldGenLevel level, WorldgenSpawnMarker.Kind kind, BlockPos pos, RandomSource random)
    {
        mark(level, kind, pos.getX(), pos.getY(), pos.getZ(), random);
    }

    static void mark(WorldGenLevel level, WorldgenSpawnMarker.Kind kind, double x, double y, double z, RandomSource random)
    {
        if (!canWrite(level, BlockPos.containing(x, y, z)))
            return;
        FlansMod.log.debug("Apocalypse worldgen {} marker at ({}, {}, {})", kind, x, y, z);
        WorldgenSpawnMarker.place(level, kind, x, y, z, random);
    }

    private static void markIfStandable(WorldGenLevel level, WorldgenSpawnMarker.Kind kind, BlockPos pos, RandomSource random)
    {
        if (isClear(level, pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            mark(level, kind, pos, random);
    }

    public static Optional<BlockPos> findSafeSurface(ServerLevel level, BlockPos center, int radius, RandomSource random)
    {
        for (int attempt = 0; attempt < 64; attempt++)
        {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = surfacePos(level, x, z);
            if (isClear(level, pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
                return Optional.of(pos);
        }
        return Optional.empty();
    }

    public static void spawnSurvivor(ServerLevel level, BlockPos pos)
    {
        if (!ModApocalypseConfig.apocalypseMobsEnabled() || !isClear(level, pos))
            return;
        SurvivorEntity survivor = ApocalypseContent.survivor.get().create(level);
        if (survivor == null)
            return;
        survivor.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        survivor.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null);
        level.addFreshEntity(survivor);
    }

    /** Generates a pack-provided, recoverable vehicle instead of a decorative placeholder. */
    private static void generateAbandonedVehicle(ServerLevel level, RandomSource random, BlockPos pos)
    {
        if (!isClear(level, pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            return;

        // Info types are backed by a HashMap, so sort before using the worldgen RNG.
        // This keeps the selected vehicle stable for a given seed and installed pack set.
        List<VehicleType> candidates = InfoType.getInfoTypes().values().stream()
            .filter(VehicleType.class::isInstance)
            .map(VehicleType.class::cast)
            .filter(VehicleType::isPlaceableOnLand)
            .distinct()
            .sorted(Comparator.comparing(VehicleType::getShortName, String.CASE_INSENSITIVE_ORDER))
            .toList();
        if (candidates.isEmpty())
            return;

        VehicleType type = candidates.get(random.nextInt(candidates.size()));
        float yaw = random.nextFloat() * 360F;
        Driveable.spawn(level, type, pos.getX() + 0.5D, pos.getY() + type.getYOffset(), pos.getZ() + 0.5D,
            yaw, null, null).ifPresent(vehicle -> {
            vehicle.getPersistentData().putBoolean("flansmodultimate:apocalypse_abandoned", true);
            vehicle.getDriveableData().setFuelInTank(0F);

            // Leave the vehicle repairable and usable while making the generated
            // state visibly abandoned. Never destroy a part here because doing so
            // would trigger normal combat drops and chained part destruction.
            for (DriveablePart part : vehicle.getDriveableData().getParts().values())
            {
                if (part.getMaxHealth() <= 0F || random.nextFloat() >= 0.65F)
                    continue;
                float damageFraction = 0.15F + random.nextFloat() * 0.40F;
                part.damage(part.getMaxHealth() * damageFraction, false);
            }
            vehicle.getDriveableData().setChanged();
        });
    }

    private static void generateDeadTree(WorldGenLevel level, RandomSource random, BlockPos base)
    {
        if (!isClear(level, base))
            return;
        int height = 4 + random.nextInt(5);
        for (int y = 0; y < height; y++)
            setBlock(level, base.above(y), Blocks.OAK_LOG.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (random.nextBoolean())
            {
                BlockPos branch = base.above(height - 1).relative(direction);
                setBlock(level, branch, Blocks.OAK_LOG.defaultBlockState());
                if (random.nextBoolean())
                    setBlock(level, branch.below(), Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private static void generateSkeletonDisplay(WorldGenLevel level, RandomSource random, BlockPos pos)
    {
        Optional<Block> skeleton = random.nextBoolean() ? flanBlock("flanskeleton") : flanBlock("flanskeleton2");
        skeleton.ifPresent(block -> {
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(HorizontalDirectionalBlock.FACING))
                state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (setBlock(level, pos, state) && level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder)
                holder.setStack(ApocalypseLoot.randomLoot(random, false));
        });
    }

    /** Abandoned portals were the way in, and are still watched over. */
    private static void generateAbandonedPortal(WorldGenLevel level, RandomSource random, BlockPos origin)
    {
        // The 1.12.2 ruin: a stepped lab-stone foundation. The portal itself, its cache and
        // its guard need the server thread, because the portal registers a teleporter.
        ApocalypseStructures.buildPortalRuin(level, origin);
        mark(level, WorldgenSpawnMarker.Kind.APOCALYPSE_PORTAL, origin, random);
    }

    /** Stands an autonomous mecha on the ground beside whatever was just built. */
    private static void postGuard(ServerLevel level, RandomSource random, int x, int z)
    {
        if (!ModApocalypseConfig.apocalypseMobsEnabled())
            return;
        BlockPos ground = surfacePos(level, x, z);
        if (!isClear(level, ground))
            return;
        ApocalypseDriveableHelper.spawnGuardMecha(level, ground, random);
    }

    static void placeChest(WorldGenLevel level, RandomSource random, BlockPos pos)
    {
        if (!level.getBlockState(pos).isAir())
            return;
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        if (setBlock(level, pos, chest) && level.getBlockEntity(pos) instanceof Container container)
            ApocalypseLoot.fillContainer(random, container);
    }

    static void placeItemHolder(WorldGenLevel level, RandomSource random, Block block, BlockPos pos, Direction facing, boolean gunsOnly)
    {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        if (setBlock(level, pos, state) && level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder)
            holder.setStack(ApocalypseLoot.randomLoot(random, gunsOnly));
    }

    private static RandomSource chunkRandom(WorldGenLevel level, ChunkPos chunkPos)
    {
        return RandomSource.create(level.getSeed() ^ (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L));
    }

    private static BlockPos randomSurfacePos(LevelAccessor level, ChunkPos chunkPos, RandomSource random)
    {
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        return surfacePos(level, x, z);
    }

    static BlockPos surfacePos(LevelAccessor level, int x, int z)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(level.getMinBuildHeight() + 1, y), z);
    }

    static boolean isClear(LevelAccessor level, BlockPos pos)
    {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    /**
     * Whether a generator may place a block here.
     *
     * <p>During worldgen a feature may only write to the chunk it decorates and its direct
     * neighbours; anything further is refused with an error. On the server thread, a block is
     * never placed into an unloaded chunk, which would make the server wait on its own chunk
     * pipeline.</p>
     */
    static boolean canWrite(LevelAccessor level, BlockPos pos)
    {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight())
            return false;
        if (level instanceof WorldGenRegion region)
        {
            ChunkPos centre = region.getCenter();
            return Math.abs(SectionPos.blockToSectionCoord(pos.getX()) - centre.x) <= 1
                && Math.abs(SectionPos.blockToSectionCoord(pos.getZ()) - centre.z) <= 1;
        }
        return level.hasChunkAt(pos);
    }

    /** Places a block from a generator, if {@link #canWrite} allows it. */
    static boolean setBlock(LevelAccessor level, BlockPos pos, BlockState state)
    {
        if (!canWrite(level, pos))
            return false;
        int flags = Block.UPDATE_CLIENTS;
        if (!(level instanceof WorldGenRegion))
        {
            // Live-world placements on a chunk's edge skip neighbour shape updates, which would
            // read the next chunk over and could make the server load it synchronously.
            int localX = pos.getX() & 15;
            int localZ = pos.getZ() & 15;
            if (localX == 0 || localX == 15 || localZ == 0 || localZ == 15)
                flags |= Block.UPDATE_KNOWN_SHAPE;
        }
        return level.setBlock(pos, state, flags);
    }

    static Optional<Block> flanBlock(String path)
    {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, path));
        if (block == null || block == Blocks.AIR)
            return Optional.empty();
        return Optional.of(block);
    }
}
