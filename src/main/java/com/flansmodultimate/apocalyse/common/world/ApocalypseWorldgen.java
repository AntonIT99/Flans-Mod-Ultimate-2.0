package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.config.ModCommonConfig;
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

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseWorldgen
{
    private static final int SULPHUR_POOL_RARITY = 8;
    private static final int BOSS_PILLAR_RARITY = 5000;

    public static void generate(ServerLevel level, ChunkAccess chunk)
    {
        if (!ModCommonConfig.apocalypseWorldgenEnabled())
            return;

        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L));
        boolean apocalypse = level.dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL);

        if (apocalypse)
        {
            if (random.nextInt(SULPHUR_POOL_RARITY) == 0)
                generateSulphurPool(level, random, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(ModCommonConfig.apocalypseDeadTreeRarity()) == 0)
                generateDeadTree(level, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(ModCommonConfig.apocalypseSkeletonRarity()) == 0)
                generateSkeletonDisplay(level, random, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(ModCommonConfig.apocalypseAbandonedPortalRarity()) == 0)
                ApocalypsePortalManager.createPortal(level, randomSurfacePos(level, chunkPos, random), null);
            if (random.nextInt(ModCommonConfig.apocalypseLabRarity()) == 0)
                generateResearchLab(level, random, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(ModCommonConfig.apocalypseDyeFactoryRarity()) == 0)
                generateFactory(level, random, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(ModCommonConfig.apocalypseAirportRarity()) == 0)
                generateRunway(level, randomSurfacePos(level, chunkPos, random));
            if (random.nextInt(BOSS_PILLAR_RARITY) == 0)
                generateBossPillar(level, randomSurfacePos(level, chunkPos, random));
            if (ModCommonConfig.apocalypseMobsEnabled() && random.nextInt(ModCommonConfig.apocalypseSurvivorRarity()) == 0)
                spawnSurvivor(level, randomSurfacePos(level, chunkPos, random));
        }
        else if (ModCommonConfig.apocalypseOverworldPortalGenerationEnabled() && random.nextInt(ModCommonConfig.apocalypseAbandonedPortalOverworldRarity()) == 0)
        {
            ApocalypsePortalManager.createPortal(level, randomSurfacePos(level, chunkPos, random), null);
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
        if (!ModCommonConfig.apocalypseMobsEnabled() || !isClear(level, pos))
            return;
        SurvivorEntity survivor = ApocalypseContent.survivor.get().create(level);
        if (survivor == null)
            return;
        survivor.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        survivor.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
        level.addFreshEntity(survivor);
    }

    private static void generateSulphurPool(ServerLevel level, RandomSource random, BlockPos center)
    {
        if (center.getY() <= level.getMinBuildHeight() + 2)
            return;
        int radius = 3 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + random.nextDouble() * 0.75D)
                    continue;
                BlockPos pos = center.offset(dx, 0, dz);
                BlockPos floor = pos.below();
                if (!level.getWorldBorder().isWithinBounds(pos))
                    continue;
                level.setBlock(floor, ApocalypseContent.blockSulphur.get().defaultBlockState(), 2);
                if (dist < radius - 1)
                    level.setBlock(pos, ApocalypseContent.blockSulphuricAcid.get().defaultBlockState(), 2);
                else if (level.getBlockState(pos).isAir())
                    level.setBlock(pos, ApocalypseContent.blockSulphur.get().defaultBlockState(), 2);
            }
        }
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

    private static void generateResearchLab(ServerLevel level, RandomSource random, BlockPos origin)
    {
        buildRoom(level, origin, 7, 4, 7, ApocalypseContent.blockLabStone.get().defaultBlockState());
        placeChest(level, random, origin.offset(2, 1, 2));
        placeChest(level, random, origin.offset(4, 1, 4));
        flanBlock("flangunrack").ifPresent(block -> placeItemHolder(level, random, block, origin.offset(3, 1, 1), Direction.SOUTH, true));
    }

    private static void generateFactory(ServerLevel level, RandomSource random, BlockPos origin)
    {
        buildRoom(level, origin, 9, 3, 5, Blocks.GRAY_CONCRETE.defaultBlockState());
        for (int x = 1; x < 8; x += 2)
            level.setBlock(origin.offset(x, 1, 2), Blocks.CAULDRON.defaultBlockState(), 3);
        placeChest(level, random, origin.offset(7, 1, 3));
    }

    private static void generateRunway(ServerLevel level, BlockPos origin)
    {
        for (int x = -2; x <= 2; x++)
        {
            for (int z = -12; z <= 12; z++)
            {
                BlockPos pos = surfacePos(level, origin.getX() + x, origin.getZ() + z).below();
                level.setBlock(pos, Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                if (x == 0 && z % 4 == 0)
                    level.setBlock(pos.above(), Blocks.WHITE_CARPET.defaultBlockState(), 2);
            }
        }
    }

    private static void generateBossPillar(ServerLevel level, BlockPos origin)
    {
        for (int y = 0; y < 18; y++)
        {
            BlockPos center = origin.above(y);
            level.setBlock(center, Blocks.OBSIDIAN.defaultBlockState(), 3);
            if (y % 4 == 0)
            {
                level.setBlock(center.north(), Blocks.OBSIDIAN.defaultBlockState(), 3);
                level.setBlock(center.south(), Blocks.OBSIDIAN.defaultBlockState(), 3);
                level.setBlock(center.east(), Blocks.OBSIDIAN.defaultBlockState(), 3);
                level.setBlock(center.west(), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
        ApocalypseBossFightManager.generateAltar(level, origin.offset(-1, 18, -1));
    }

    private static void buildRoom(ServerLevel level, BlockPos origin, int width, int height, int depth, BlockState wall)
    {
        for (int x = 0; x < width; x++)
        {
            for (int z = 0; z < depth; z++)
            {
                level.setBlock(origin.offset(x, 0, z), wall, 3);
                level.setBlock(origin.offset(x, height, z), wall, 3);
                for (int y = 1; y < height; y++)
                {
                    boolean edge = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                    level.setBlock(origin.offset(x, y, z), edge ? wall : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.offset(width / 2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(width / 2, 2, 0), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void placeChest(ServerLevel level, RandomSource random, BlockPos pos)
    {
        if (!level.getBlockState(pos).isAir())
            return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 3);
        if (level.getBlockEntity(pos) instanceof Container container)
            ApocalypseLoot.fillContainer(random, container);
    }

    private static void placeItemHolder(ServerLevel level, RandomSource random, Block block, BlockPos pos, Direction facing, boolean gunsOnly)
    {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder)
            holder.setStack(ApocalypseLoot.randomLoot(random, gunsOnly));
    }

    private static BlockPos randomSurfacePos(ServerLevel level, ChunkPos chunkPos, RandomSource random)
    {
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        return surfacePos(level, x, z);
    }

    private static BlockPos surfacePos(ServerLevel level, int x, int z)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(level.getMinBuildHeight() + 1, y), z);
    }

    private static boolean isClear(ServerLevel level, BlockPos pos)
    {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static Optional<Block> flanBlock(String path)
    {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, path));
        if (block == null || block == Blocks.AIR)
            return Optional.empty();
        return Optional.of(block);
    }
}
