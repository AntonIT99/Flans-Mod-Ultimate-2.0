package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
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
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseWorldgen
{
    public static void generate(ServerLevel level, ChunkAccess chunk)
    {
        if (!ModApocalypseConfig.apocalypseWorldgenEnabled())
            return;

        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L));
        boolean apocalypse = level.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL);
        if (apocalypse && !ModApocalypseConfig.apocalypseDimensionEnabled())
            return;

        if (apocalypse)
        {
            // Ores, sulphur-pit acid lakes, dungeons, ravines and mineshafts are regular biome
            // features and structures in the flansmodapocalypse worldgen data.
            ApocalypseRoads.generate(level, chunk);
            ApocalypseStructures.generateBossPillars(level, chunkPos, random);
            ApocalypseVillage.generate(level, chunk);

            if (random.nextInt(ModApocalypseConfig.apocalypseDeadTreeRarity()) == 0)
                generateDeadTree(level, randomSurfacePos(chunk, random));
            if (random.nextInt(ModApocalypseConfig.apocalypseSkeletonRarity()) == 0)
                generateSkeletonDisplay(level, random, randomSurfacePos(chunk, random));
            if (ModApocalypseConfig.apocalypseDimensionEnabled()
                && ModApocalypseConfig.apocalypsePortalsEnabled()
                && random.nextInt(ModApocalypseConfig.apocalypseAbandonedPortalRarity()) == 0)
                generateAbandonedPortal(level, random, randomSurfacePos(chunk, random));

            // Labs and airfields span several chunks: each chunk decides from its region's seed
            // whether it holds a piece, and builds only that piece. Both need the high plateau.
            ApocalypseStructures.generateResearchLab(level, chunkPos);
            ApocalypseStructures.generateRunway(level, chunkPos);
            if (random.nextInt(ModApocalypseConfig.apocalypseDyeFactoryRarity()) == 0)
                ApocalypseStructures.generateDyeFactory(level, chunkPos, random);
            if (random.nextInt(ModApocalypseConfig.apocalypseVehicleRarity()) == 0)
                generateAbandonedVehicle(level, random, randomSurfacePos(chunk, random));
            if (ModApocalypseConfig.apocalypseMobsEnabled() && random.nextInt(ModApocalypseConfig.apocalypseSurvivorRarity()) == 0)
                spawnSurvivor(level, randomSurfacePos(chunk, random));
        }
        else if (ModApocalypseConfig.apocalypseDimensionEnabled()
            && ModApocalypseConfig.apocalypsePortalsEnabled()
            && ModApocalypseConfig.apocalypseOverworldPortalGenerationEnabled()
            && random.nextInt(ModApocalypseConfig.apocalypseAbandonedPortalOverworldRarity()) == 0)
        {
            ApocalypsePortalManager.createPortal(level, randomSurfacePos(chunk, random), null);
        }
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
        survivor.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
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

    private static void generateDeadTree(ServerLevel level, BlockPos base)
    {
        if (!isClear(level, base))
            return;
        int height = 4 + level.random.nextInt(5);
        for (int y = 0; y < height; y++)
            level.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (level.random.nextBoolean())
            {
                BlockPos branch = base.above(height - 1).relative(direction);
                level.setBlock(branch, Blocks.OAK_LOG.defaultBlockState(), 2);
                if (level.random.nextBoolean())
                    level.setBlock(branch.below(), Blocks.COBWEB.defaultBlockState(), 2);
            }
        }
    }

    private static void generateSkeletonDisplay(ServerLevel level, RandomSource random, BlockPos pos)
    {
        Optional<Block> skeleton = random.nextBoolean() ? flanBlock("flanskeleton") : flanBlock("flanskeleton2");
        skeleton.ifPresent(block -> {
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(HorizontalDirectionalBlock.FACING))
                state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
            level.setBlock(pos, state, 3);
            if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder)
                holder.setStack(ApocalypseLoot.randomLoot(random, false));
        });
    }

    /** Abandoned portals were the way in, and are still watched over. */
    private static void generateAbandonedPortal(ServerLevel level, RandomSource random, BlockPos origin)
    {
        // The 1.12.2 ruin: a stepped lab-stone foundation and a cache with spare portal parts.
        ApocalypseStructures.buildPortalRuin(level, origin);
        if (!ApocalypsePortalManager.createPortal(level, origin, null))
            return;
        ApocalypseStructures.placePortalCache(level, random, origin);
        if (random.nextBoolean())
            postGuard(level, random, origin.getX() + (random.nextBoolean() ? 6 : -3), origin.getZ() + (random.nextBoolean() ? 6 : -3));
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

    static void placeChest(ServerLevel level, RandomSource random, BlockPos pos)
    {
        if (!level.getBlockState(pos).isAir())
            return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 3);
        if (level.getBlockEntity(pos) instanceof Container container)
            ApocalypseLoot.fillContainer(random, container);
    }

    static void placeItemHolder(ServerLevel level, RandomSource random, Block block, BlockPos pos, Direction facing, boolean gunsOnly)
    {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder)
            holder.setStack(ApocalypseLoot.randomLoot(random, gunsOnly));
    }

    private static BlockPos randomSurfacePos(ChunkAccess chunk, RandomSource random)
    {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(chunk.getMinBuildHeight() + 1, y), z);
    }

    static BlockPos surfacePos(ServerLevel level, int x, int z)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(level.getMinBuildHeight() + 1, y), z);
    }

    static boolean isClear(ServerLevel level, BlockPos pos)
    {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    static Optional<Block> flanBlock(String path)
    {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, path));
        if (block == null || block == Blocks.AIR)
            return Optional.empty();
        return Optional.of(block);
    }
}
