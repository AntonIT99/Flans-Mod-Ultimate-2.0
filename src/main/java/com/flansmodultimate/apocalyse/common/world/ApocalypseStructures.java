package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.WorldgenSpawnMarker;
import com.flansmodultimate.apocalyse.common.util.ApocalypseLoot;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Optional;

/**
 * Ports of the large 1.12.2 Apocalypse structures.
 *
 * <p>Every structure is built one chunk at a time, exactly as the 1.12.2 populators did: a
 * multi-chunk structure is chosen by seeding a random from its region's coordinates, so all of
 * its chunks agree without loading one another, and each chunk then builds only its own piece.
 * Coordinates are chunk-aligned instead of carrying the old populator's +8 offset, and the fixed
 * 1.12.2 heights (Y=99 labs, Y=108 runways) are replaced by the noise surface at the structure's
 * centre because the modern terrain does not share the old height profile.</p>
 *
 * <p>Everything here runs from {@link ApocalypseChunkFeature} during worldgen, apart from the
 * portal cache, which is placed once the portal itself exists on the server thread.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseStructures
{
    private static final int LAB_REGION_CHUNKS = 3;
    private static final int LAB_FLOORS = 8;
    private static final int LAB_FLOOR_HEIGHT = 8;
    private static final int RUNWAY_CHUNKS = 4;

    // 1.12.2 WorldGenBossPillar constants.
    public static final double PILLAR_INNER_EDGE = 12D;
    private static final double PILLAR_INNER_RADIUS = PILLAR_INNER_EDGE * Math.sqrt(2);
    private static final double PILLAR_OUTER_EDGE = 300D;
    private static final double PILLAR_MAX_HEIGHT = 240D;
    public static final double BOSS_SPAWN_HEIGHT = 220D;
    private static final double PILLAR_KB = -Math.log(PILLAR_MAX_HEIGHT) / (PILLAR_OUTER_EDGE - PILLAR_INNER_RADIUS);
    private static final double PILLAR_KA = Math.exp(-PILLAR_KB * PILLAR_OUTER_EDGE);

    private static final List<Block> LEGACY_FLOWER_POTS = List.of(Blocks.FLOWER_POT, Blocks.POTTED_POPPY,
        Blocks.POTTED_DANDELION, Blocks.POTTED_OAK_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, Blocks.POTTED_BIRCH_SAPLING,
        Blocks.POTTED_JUNGLE_SAPLING, Blocks.POTTED_RED_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_CACTUS,
        Blocks.POTTED_DEAD_BUSH, Blocks.POTTED_FERN, Blocks.POTTED_ACACIA_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING);

    private static final List<Block> WOOL = List.of(Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL,
        Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL, Blocks.GRAY_WOOL,
        Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL, Blocks.BROWN_WOOL,
        Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL);

    // ---------------------------------------------------------------------------------------------
    // Research lab: a 3x3-chunk, eight-floor complex (1.12.2 WorldGenResearchLab)
    // ---------------------------------------------------------------------------------------------

    public static void generateResearchLab(WorldGenLevel level, ChunkPos chunk)
    {
        int regionX = Math.floorDiv(chunk.x, LAB_REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunk.z, LAB_REGION_CHUNKS);
        if (regionRandom(level.getSeed(), regionX, regionZ).nextInt(ModApocalypseConfig.apocalypseLabRarity()) != 0)
            return;

        // The whole lab and a one-chunk margin around it must sit on the high plateau.
        for (int n = 0; n < 5; n++)
        {
            for (int m = 0; m < 5; m++)
            {
                if (!isBiomeAtChunkCentre(level, regionX * LAB_REGION_CHUNKS - 1 + n, regionZ * LAB_REGION_CHUNKS - 1 + m, ApocalypseContent.BIOME_HIGH_PLATEAU))
                    return;
            }
        }

        // The entrance bunker on the centre chunk stands on the surface; every floor is below it.
        int centreX = regionX * LAB_REGION_CHUNKS * 16 + 24;
        int centreZ = regionZ * LAB_REGION_CHUNKS * 16 + 24;
        int surface = noiseSurface(level, centreX, centreZ);
        int minTop = level.getMinBuildHeight() + LAB_FLOOR_HEIGHT * (LAB_FLOORS - 1) + 3;
        int maxTop = level.getMaxBuildHeight() - 2 * LAB_FLOOR_HEIGHT - 1;
        if (minTop > maxTop)
            return;
        int top = Mth.clamp(surface - LAB_FLOOR_HEIGHT - 1, minTop, maxTop);

        FlansMod.log.debug("Apocalypse research lab piece at chunk {} (top floor Y={})", chunk, top);
        buildResearchLabPiece(level, chunk, chunkRandom(level.getSeed(), chunk.x, chunk.z), top);
    }

    private static void buildResearchLabPiece(WorldGenLevel level, ChunkPos chunk, RandomSource rand, int top)
    {
        int pieceX = Math.floorMod(chunk.x, LAB_REGION_CHUNKS);
        int pieceZ = Math.floorMod(chunk.z, LAB_REGION_CHUNKS);
        boolean centre = pieceX == 1 && pieceZ == 1;
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        BlockState lab = ApocalypseContent.blockLabStone.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Empty rooms, one per floor, plus the entrance bunker above the centre piece.
        for (int i = centre ? -1 : 0; i < LAB_FLOORS; i++)
        {
            int y = top - LAB_FLOOR_HEIGHT * i;
            fill(level, x, y, z, x + 16, y + 8, z + 16, lab, air);
            fill(level, x, y + 6, z, x + 16, y + 7, z + 16, lab);
            for (int j = 0; j < 2; j++)
            {
                for (int k = 0; k < 2; k++)
                    fill(level, x + 3 + 8 * j, y + 6, z + 3 + 8 * k, x + 5 + 8 * j, y + 7, z + 5 + 8 * k, Blocks.GLOWSTONE.defaultBlockState());
            }

            // Doors into neighbouring pieces of the same lab.
            if (pieceX != 0)
                fill(level, x, y + 1, z + 7, x + 1, y + 4, z + 9, air);
            if (pieceX != 2)
                fill(level, x + 15, y + 1, z + 7, x + 16, y + 4, z + 9, air);
            if (pieceZ != 0)
                fill(level, x + 7, y + 1, z, x + 9, y + 4, z + 1, air);
            if (pieceZ != 2)
                fill(level, x + 7, y + 1, z + 15, x + 9, y + 4, z + 16, air);
        }

        for (int i = 0; i < LAB_FLOORS; i++)
        {
            int y = top - LAB_FLOOR_HEIGHT * i;
            if (i == LAB_FLOORS - 1 && centre)
            {
                buildLabTeleporterRoom(level, rand, x, y, z);
            }
            else if (i == 0 && centre)
            {
                // The entrance stairs lead from the surface bunker down to the first floor.
                buildLabStairs(level, x, y + LAB_FLOOR_HEIGHT, z);
            }
            else
            {
                boolean spawnMecha = true;
                switch (i == LAB_FLOORS - 2 && centre ? 4 : rand.nextInt(7))
                {
                    case 0 ->
                    {
                        buildLabStairs(level, x, y, z);
                        // The floor below is the landing, and is left empty.
                        i++;
                        spawnMecha = false;
                    }
                    case 1 ->
                    {
                        for (int j = 0; j < 2; j++)
                        {
                            for (int k = 0; k < 2; k++)
                            {
                                if (rand.nextInt(3) == 0)
                                    buildLiquidsLab(level, rand, x + 1 + 8 * j, y + 1, z + 1 + 8 * k);
                                else
                                    buildLiquidContainer(level, rand, x + 2 + 8 * j, y + 1, z + 2 + 8 * k, randomLiquid(rand));
                            }
                        }
                    }
                    case 2 -> buildGunRange(level, rand, x, y, z);
                    case 3 ->
                    {
                        for (int j = 0; j < 2; j++)
                        {
                            for (int k = 0; k < 2; k++)
                            {
                                if (rand.nextInt(2) == 0)
                                    buildPlantPots(level, rand, x + 1 + 8 * j, y + 1, z + 1 + 8 * k);
                                else
                                    buildFarm(level, rand, x + 9 * j, y + 1, z + 9 * k);
                            }
                        }
                    }
                    case 4 -> buildForge(level, rand, x, y, z);
                    case 5 ->
                    {
                        buildPowerRoom(level, x, y, z);
                        spawnMecha = false;
                    }
                    default ->
                    {
                        // An empty room.
                    }
                }
                if (spawnMecha && rand.nextBoolean())
                    spawnLabMecha(level, rand, x + 8, y + 1, z + 8);
            }
        }
    }

    private static void buildLabTeleporterRoom(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        BlockState lab = ApocalypseContent.blockLabStone.get().defaultBlockState();
        BlockState quartzSlab = Blocks.QUARTZ_SLAB.defaultBlockState();
        fill(level, x + 3, y, z + 3, x + 13, y + 1, z + 13, Blocks.GLOWSTONE.defaultBlockState());
        fill(level, x + 4, y, z + 4, x + 12, y + 1, z + 12, lab);
        // An unpowered portal frame: obsidian corners and centre, one of its four power cubes.
        fill(level, x + 6, y + 1, z + 6, x + 10, y + 2, z + 10, Blocks.OBSIDIAN.defaultBlockState());
        fill(level, x + 6, y + 1, z + 7, x + 7, y + 2, z + 9, quartzSlab);
        fill(level, x + 9, y + 1, z + 7, x + 10, y + 2, z + 9, quartzSlab);
        fill(level, x + 7, y + 1, z + 6, x + 9, y + 2, z + 7, quartzSlab);
        fill(level, x + 7, y + 1, z + 9, x + 9, y + 2, z + 10, quartzSlab);
        set(level, x + 6, y + 2, z + 6, ApocalypseContent.blockPowerCube.get().defaultBlockState());

        for (int k = 0; k < 8; k++)
            spawnLabMecha(level, rand, x + 4 + rand.nextInt(8), y + 2, z + 4 + rand.nextInt(8));
    }

    /** A fenced stairwell from floor {@code y} down to the floor below it. */
    private static void buildLabStairs(WorldGenLevel level, int x, int y, int z)
    {
        BlockState lab = ApocalypseContent.blockLabStone.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(level, x + 4, y + 1, z + 4, x + 12, y + 2, z + 12, Blocks.NETHER_BRICK_FENCE.defaultBlockState());
        fill(level, x + 5, y - 2, z + 5, x + 11, y + 2, z + 11, air);
        fill(level, x + 7, y + 1, z + 11, x + 9, y + 2, z + 12, air);

        fill(level, x + 7, y - 1, z + 9, x + 9, y, z + 11, lab);
        fill(level, x + 5, y - 2, z + 9, x + 7, y - 1, z + 11, lab);
        fill(level, x + 9, y - 2, z + 9, x + 11, y - 1, z + 11, lab);
        fill(level, x + 9, y - 3, z + 7, x + 11, y - 2, z + 9, lab);
        fill(level, x + 5, y - 3, z + 7, x + 7, y - 2, z + 9, lab);
        fill(level, x + 5, y - 4, z + 5, x + 11, y - 3, z + 7, lab);
        fill(level, x + 7, y - 5, z + 7, x + 9, y - 4, z + 9, lab);
        fill(level, x + 7, y - 6, z + 9, x + 9, y - 5, z + 11, lab);
        fill(level, x + 7, y - 7, z + 11, x + 9, y - 6, z + 13, lab);
        fill(level, x + 5, y - 7, z + 9, x + 7, y - 6, z + 11, lab);
        fill(level, x + 9, y - 7, z + 9, x + 11, y - 6, z + 11, lab);
    }

    private static void buildGunRange(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        for (int j = 0; j < 2; j++)
            buildTarget(level, x + 2 + 7 * j, y + 1, z + 1);
        fill(level, x + 3, y + 1, z + 6, x + 4, y + 2, z + 12, planks);
        fill(level, x + 12, y + 1, z + 6, x + 13, y + 2, z + 12, planks);
        fill(level, x + 4, y + 1, z + 11, x + 12, y + 2, z + 12, Blocks.PETRIFIED_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        set(level, x + 6, y + 1, z + 11, planks);
        set(level, x + 9, y + 1, z + 11, planks);

        buildGunRack(level, rand, x + 1, y + 1, z + 14);
        buildGunRack(level, rand, x + 4, y + 1, z + 14);
        buildGunRack(level, rand, x + 10, y + 1, z + 14);
        buildGunRack(level, rand, x + 13, y + 1, z + 14);
    }

    private static void buildForge(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        for (int j = 0; j < 2; j++)
        {
            if (rand.nextBoolean())
            {
                buildFurnace(level, x + 2 + 8 * j, y + 1, z + 1);
            }
            else
            {
                set(level, x + 2 + 8 * j, y + 1, z + 1, rand.nextBoolean()
                    ? Blocks.CRAFTING_TABLE.defaultBlockState()
                    : FlansMod.vehicleCraftingTable.get().defaultBlockState());
                set(level, x + 4 + 8 * j, y + 1, z + 1, Blocks.IRON_BLOCK.defaultBlockState());
                set(level, x + 5 + 8 * j, y + 2, z + 1, Blocks.IRON_BLOCK.defaultBlockState());
                set(level, x + 5 + 8 * j, y + 1, z + 1, Blocks.IRON_BLOCK.defaultBlockState());
                set(level, x + 4 + 8 * j, y + 1, z + 4, Blocks.ANVIL.defaultBlockState());
            }
            buildWeapons(level, rand, x + 2 + 8 * j, y + 1, z + 10);
        }
    }

    private static void buildPowerRoom(WorldGenLevel level, int x, int y, int z)
    {
        buildServerRack(level, x + 1, y + 1, z + 2, true);
        buildServerRack(level, x + 1, y + 1, z + 5, false);
        buildServerRack(level, x + 1, y + 1, z + 10, false);
        buildServerRack(level, x + 1, y + 1, z + 13, true);
        buildServerRack(level, x + 10, y + 1, z + 2, true);
        buildServerRack(level, x + 12, y + 1, z + 5, false);
        buildServerRack(level, x + 12, y + 1, z + 10, false);
        buildServerRack(level, x + 10, y + 1, z + 13, true);
        buildServerPower(level, x + 6, y + 1, z + 6);
    }

    private static void spawnLabMecha(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        if (ModApocalypseConfig.apocalypseMobsEnabled())
            ApocalypseWorldgen.mark(level, WorldgenSpawnMarker.Kind.LAB_MECHA, new BlockPos(x, y, z), rand);
    }

    private static void buildServerPower(WorldGenLevel level, int x, int y, int z)
    {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState trapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
        fill(level, x, y, z, x + 2, y + 3, z + 2, obsidian);
        fill(level, x + 1, y, z + 1, x + 3, y + 3, z + 3, obsidian);
        fill(level, x + 2, y, z + 2, x + 4, y + 3, z + 4, obsidian);
        set(level, x + 1, y + 1, z + 1, ApocalypseContent.blockPowerCube.get().defaultBlockState());
        set(level, x + 2, y + 1, z + 2, Blocks.AIR.defaultBlockState());
        set(level, x + 1, y + 2, z + 1, trapdoor);
        set(level, x + 2, y + 2, z + 2, trapdoor);
    }

    private static void buildServerRack(WorldGenLevel level, int x, int y, int z, boolean big)
    {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState quartzPillar = Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        fill(level, x, y, z, x + 3, y + 3, z + 1, obsidian);
        fill(level, x + 1, y, z, x + 2, y + 3, z + 1, quartzPillar);
        if (big)
        {
            fill(level, x + 3, y, z, x + 4, y + 3, z + 1, quartzPillar);
            fill(level, x + 4, y, z, x + 5, y + 3, z + 1, obsidian);
        }
    }

    private static void buildWeapons(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        fill(level, x + 1, y, z, x + 3, y + 1, z + 2, Blocks.OAK_PLANKS.defaultBlockState());
        for (int i = 0; i < 2; i++)
        {
            placeChest(level, x, y, z + i, Direction.NORTH);
            placeChest(level, x + 3, y, z + i, Direction.NORTH);
        }

        Optional<Block> gunRack = ApocalypseWorldgen.flanBlock("flangunrack");
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                int rackX = x + 1 + i;
                int rackZ = z + j;
                Direction facing = j == 0 ? Direction.SOUTH : Direction.NORTH;
                gunRack.ifPresent(block -> placeItemHolder(level, block, rackX, y + 1, rackZ, facing));
                if (rand.nextInt(3) != 0)
                    fillItemHolder(level, rand, rackX, y + 1, rackZ);
            }
        }

        for (int i = 0; i < 2; i++)
        {
            fillContainer(level, x, y, z + i, container -> ApocalypseLoot.fillWeaponChest(rand, container));
            fillContainer(level, x + 3, y, z + i, container -> ApocalypseLoot.fillWeaponChest(rand, container));
        }
    }

    private static void buildFurnace(WorldGenLevel level, int x, int y, int z)
    {
        BlockState lab = ApocalypseContent.blockLabStone.get().defaultBlockState();
        fill(level, x, y, z, x + 1, y + 2, z + 2, lab);
        fill(level, x + 3, y, z, x + 4, y + 2, z + 2, lab);
        fill(level, x + 1, y, z + 2, x + 3, y + 1, z + 3, lab);
        fill(level, x + 1, y + 2, z, x + 3, y + 3, z + 2, lab);
        fill(level, x + 1, y + 3, z, x + 3, y + 5, z + 1, lab);
        fill(level, x + 1, y, z, x + 3, y + 1, z + 2, Blocks.LAVA.defaultBlockState());
    }

    private static void buildPlantPots(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState slab = Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int row = 0; row <= 5; row += 5)
        {
            fill(level, x, y, z + row, x + 6, y + 1, z + row + 1, quartz);
            fill(level, x + 1, y, z + row, x + 5, y + 1, z + row + 1, slab);
            for (int i = 0; i < 6; i++)
                set(level, x + i, y + 1, z + row, LEGACY_FLOWER_POTS.get(rand.nextInt(LEGACY_FLOWER_POTS.size())).defaultBlockState());
        }
    }

    private static void buildFarm(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        fill(level, x, y, z, x + 7, y + 1, z + 7, ApocalypseContent.blockLabStone.get().defaultBlockState());
        fill(level, x + 1, y, z + 1, x + 6, y + 1, z + 6, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        Block crop = switch (rand.nextInt(3))
        {
            case 0 -> Blocks.WHEAT;
            case 1 -> Blocks.CARROTS;
            default -> Blocks.POTATOES;
        };
        fill(level, x + 1, y + 1, z + 1, x + 6, y + 2, z + 6, crop.defaultBlockState().setValue(CropBlock.AGE, rand.nextInt(5) + 2));
        set(level, x + 3, y + 1, z + 3, Blocks.AIR.defaultBlockState());
        set(level, x + 3, y, z + 3, Blocks.WATER.defaultBlockState());
    }

    private static void buildGunRack(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        fill(level, x, y, z, x + 2, y + 1, z + 1, Blocks.OAK_PLANKS.defaultBlockState());
        Optional<Block> gunRack = ApocalypseWorldgen.flanBlock("flangunrack");
        for (int i = 0; i < 2; i++)
        {
            int rackX = x + i;
            gunRack.ifPresent(block -> placeItemHolder(level, block, rackX, y + 1, z, Direction.SOUTH));
            if (rand.nextInt(3) != 0)
                fillItemHolder(level, rand, rackX, y + 1, z);
        }
    }

    private static void buildTarget(WorldGenLevel level, int x, int y, int z)
    {
        BlockState red = Blocks.RED_WOOL.defaultBlockState();
        BlockState white = Blocks.WHITE_WOOL.defaultBlockState();
        fill(level, x + 1, y + 1, z, x + 4, y + 4, z + 1, red);
        set(level, x + 2, y, z, red);
        set(level, x + 2, y + 4, z, red);
        set(level, x, y + 2, z, red);
        set(level, x + 4, y + 2, z, red);
        set(level, x + 2, y + 1, z, white);
        set(level, x + 2, y + 3, z, white);
        set(level, x + 1, y + 2, z, white);
        set(level, x + 3, y + 2, z, white);
    }

    private static void buildLiquidsLab(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState slab = Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        fill(level, x, y, z, x + 4, y + 1, z + 1, quartz);
        fill(level, x + 1, y, z, x + 3, y + 1, z + 1, slab);
        fill(level, x, y, z + 5, x + 5, y + 1, z + 6, quartz);
        fill(level, x + 1, y, z + 5, x + 4, y + 1, z + 6, slab);

        int waterLevel = rand.nextInt(4);
        set(level, x + 5, y, z + 5, waterLevel == 0
            ? Blocks.CAULDRON.defaultBlockState()
            : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, waterLevel));

        for (int i = 4; i <= 5; i++)
        {
            placeChest(level, x + i, y, z, Direction.SOUTH);
            fillContainer(level, x + i, y, z, container -> ApocalypseLoot.fillLiquidLabChest(rand, container));
        }

        int standX = x + rand.nextInt(4);
        set(level, standX, y + 1, z, Blocks.BREWING_STAND.defaultBlockState());
        fillContainer(level, standX, y + 1, z, container -> ApocalypseLoot.fillBrewingStand(rand, container));
        standX = x + rand.nextInt(5);
        set(level, standX, y + 1, z + 5, Blocks.BREWING_STAND.defaultBlockState());
        fillContainer(level, standX, y + 1, z + 5, container -> ApocalypseLoot.fillBrewingStand(rand, container));
    }

    private static void buildLiquidContainer(WorldGenLevel level, RandomSource rand, int x, int y, int z, BlockState liquid)
    {
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(level, x, y, z, x + 4, y + 5, z + 4, ApocalypseContent.blockLabStone.get().defaultBlockState());
        fill(level, x, y + 1, z, x + 4, y + 4, z + 4, Blocks.GLASS.defaultBlockState());
        fill(level, x + 1, y, z + 1, x + 3, y + 5, z + 3, air);
        fill(level, x + 1, y, z + 1, x + 3, y + rand.nextInt(4), z + 3, liquid);
        fill(level, x, y, z, x + 1, y + 5, z + 1, air);
        fill(level, x + 3, y, z, x + 4, y + 5, z + 1, air);
        fill(level, x + 3, y, z + 3, x + 4, y + 5, z + 4, air);
        fill(level, x, y, z + 3, x + 1, y + 5, z + 4, air);
    }

    private static BlockState randomLiquid(RandomSource rand)
    {
        return switch (rand.nextInt(3))
        {
            case 1 -> Blocks.LAVA.defaultBlockState();
            case 2 -> ApocalypseContent.blockSulphuricAcid.get().defaultBlockState();
            default -> Blocks.WATER.defaultBlockState();
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Airfield: a four-chunk runway with a hangar and a parked plane (1.12.2 WorldGenRunway)
    // ---------------------------------------------------------------------------------------------

    public static void generateRunway(WorldGenLevel level, ChunkPos chunk)
    {
        int stripX = Math.floorDiv(chunk.x, RUNWAY_CHUNKS);
        if (regionRandom(level.getSeed(), stripX, chunk.z).nextInt(ModApocalypseConfig.apocalypseAirportRarity()) != 0)
            return;
        for (int n = 0; n < RUNWAY_CHUNKS; n++)
        {
            if (!isBiomeAtChunkCentre(level, stripX * RUNWAY_CHUNKS + n, chunk.z, ApocalypseContent.BIOME_HIGH_PLATEAU))
                return;
        }

        int y = noiseSurface(level, stripX * RUNWAY_CHUNKS * 16 + RUNWAY_CHUNKS * 8, chunk.getMinBlockZ() + 8);
        y = Mth.clamp(y, level.getMinBuildHeight() + 8, level.getMaxBuildHeight() - 12);
        FlansMod.log.debug("Apocalypse runway section at chunk {} (Y={})", chunk, y);
        buildRunwaySection(level, chunk, chunkRandom(level.getSeed(), chunk.x, chunk.z), y);
    }

    private static void buildRunwaySection(WorldGenLevel level, ChunkPos chunk, RandomSource rand, int y)
    {
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int section = Math.floorMod(chunk.x, RUNWAY_CHUNKS);

        for (int j = 1; j < 8; j++)
            fill(level, x, y - j, z + j, x + 16, y - j + 1, z + 16 - j, Blocks.STONE.defaultBlockState());
        fill(level, x, y, z, x + 16, y + 1, z + 16, Blocks.BLACK_TERRACOTTA.defaultBlockState());
        fill(level, x + 2, y, z + 7, x + 6, y + 1, z + 9, Blocks.QUARTZ_BLOCK.defaultBlockState());
        fill(level, x + 10, y, z + 7, x + 14, y + 1, z + 9, Blocks.QUARTZ_BLOCK.defaultBlockState());
        fill(level, x, y + 1, z, x + 16, y + 11, z + 16, Blocks.AIR.defaultBlockState());

        if (section == 1)
            buildHangar(level, rand, x, y, z);
        else if (section == 0)
            ApocalypseWorldgen.mark(level, WorldgenSpawnMarker.Kind.PARKED_PLANE, x + 8D, y + 3D, z + 8D, rand);
    }

    private static void buildHangar(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        BlockState wool = Blocks.GREEN_WOOL.defaultBlockState();
        // The arched roof, sealed at the western end.
        fill(level, x, y + 1, z, x + 16, y + 5, z + 1, wool);
        fill(level, x, y + 5, z + 1, x + 16, y + 7, z + 2, wool);
        fill(level, x, y + 7, z + 2, x + 16, y + 8, z + 3, wool);
        fill(level, x, y + 8, z + 3, x + 16, y + 9, z + 5, wool);
        fill(level, x, y + 9, z + 5, x + 16, y + 10, z + 11, wool);
        fill(level, x, y + 8, z + 11, x + 16, y + 9, z + 13, wool);
        fill(level, x, y + 7, z + 13, x + 16, y + 8, z + 14, wool);
        fill(level, x, y + 5, z + 14, x + 16, y + 7, z + 15, wool);
        fill(level, x, y + 1, z + 15, x + 16, y + 5, z + 16, wool);
        fill(level, x, y + 1, z + 1, x + 1, y + 5, z + 15, wool);
        fill(level, x, y + 5, z + 2, x + 1, y + 7, z + 14, wool);
        fill(level, x, y + 7, z + 3, x + 1, y + 8, z + 13, wool);
        fill(level, x, y + 8, z + 5, x + 1, y + 9, z + 11, wool);

        set(level, x + 1, y + 1, z + 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, x + 2, y + 1, z + 1, FlansMod.gunWorkbench.get().defaultBlockState());
        set(level, x + 1, y + 1, z + 14, FlansMod.gunWorkbench.get().defaultBlockState());

        set(level, x + 1, y + 4, z + 14, Blocks.GLOWSTONE.defaultBlockState());
        set(level, x + 1, y + 4, z + 1, Blocks.GLOWSTONE.defaultBlockState());
        set(level, x + 1, y + 7, z + 12, Blocks.GLOWSTONE.defaultBlockState());
        set(level, x + 1, y + 7, z + 3, Blocks.GLOWSTONE.defaultBlockState());

        placeWeaponBox(level, rand, x + 3, y + 1, z + 14);
        placeWeaponBox(level, rand, x + 4, y + 1, z + 14);

        for (int i = 4; i <= 5; i++)
        {
            placeChest(level, x + i, y + 1, z + 1, Direction.NORTH);
            fillContainer(level, x + i, y + 1, z + 1, container -> ApocalypseLoot.fillVillageChest(rand, container));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Dye factory: a single-chunk workshop on flat Deep Canyon floor (1.12.2 WorldGenDyeFactory)
    // ---------------------------------------------------------------------------------------------

    public static void generateDyeFactory(WorldGenLevel level, ChunkPos chunk, RandomSource rand)
    {
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        if (!level.getBiome(new BlockPos(x + 8, 64, z + 8)).is(ApocalypseContent.BIOME_DEEP_CANYON))
            return;

        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        for (int i = 0; i < 16; i++)
        {
            for (int k = 0; k < 16; k++)
            {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x + i, z + k) - 1;
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        // The chunk is too bumpy to build on.
        if (maxHeight - minHeight > 3 || maxHeight + 7 >= level.getMaxBuildHeight())
            return;

        FlansMod.log.debug("Apocalypse dye factory at chunk {}", chunk);
        int y = maxHeight + 1;
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState logX = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);

        fill(level, x, minHeight, z, x + 16, maxHeight + 1, z + 16, Blocks.RED_SAND.defaultBlockState());

        // Walls, beams and posts, then hollow it out and open the door.
        fill(level, x, y, z, x + 6, y + 1, z + 16, cobble);
        fill(level, x, y + 1, z, x + 6, y + 5, z + 16, planks);
        fill(level, x, y + 4, z, x + 6, y + 5, z + 1, logX);
        fill(level, x, y + 4, z + 15, x + 6, y + 5, z + 16, logX);
        fill(level, x, y + 4, z, x + 1, y + 5, z + 16, logZ);
        fill(level, x + 5, y + 4, z, x + 6, y + 5, z + 16, logZ);
        for (int i = 0; i < 6; i += 5)
        {
            for (int k = 0; k < 16; k += 5)
                fill(level, x + i, y + 1, z + k, x + i + 1, y + 6, z + k + 1, Blocks.OAK_LOG.defaultBlockState());
        }
        fill(level, x + 1, y, z + 1, x + 5, y + 4, z + 15, air);
        fill(level, x + 5, y, z + 6, x + 6, y + 4, z + 10, air);

        // Two random weapon boxes either side of a gun workbench and a vehicle crafting table.
        placeWeaponBox(level, rand, x + 1, y, z + 6);
        set(level, x + 1, y, z + 7, FlansMod.gunWorkbench.get().defaultBlockState());
        set(level, x + 1, y, z + 8, FlansMod.vehicleCraftingTable.get().defaultBlockState());
        placeWeaponBox(level, rand, x + 1, y, z + 9);

        // Chest racks.
        for (int i = 1; i < 6; i += 3)
        {
            for (int k = 1; k < 6; k += 3)
                fill(level, x + i, y, z + k, x + i + 1, y + 3, z + k + 1, Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int j = 0; j < 3; j += 2)
        {
            int[][] chests = {{1, 2}, {1, 3}, {4, 2}, {4, 3}, {2, 1}, {3, 1}};
            for (int[] chest : chests)
            {
                placeChest(level, x + chest[0], y + j, z + chest[1], Direction.NORTH);
                fillContainer(level, x + chest[0], y + j, z + chest[1], container -> ApocalypseLoot.fillDyeFactoryChest(rand, container));
            }
        }

        // Sewing table and dressed armour stands.
        fill(level, x + 1, y, z + 11, x + 2, y + 1, z + 15, Blocks.CRAFTING_TABLE.defaultBlockState());
        fill(level, x + 1, y, z + 12, x + 2, y + 1, z + 14, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        for (int k = 0; k < 2; k++)
        {
            // A plain armour stand saves with the chunk, so worldgen can place it directly.
            ArmorStand stand = EntityType.ARMOR_STAND.create(level.getLevel());
            if (stand == null)
                continue;
            stand.moveTo(x + 4.5D, y, z + 11.5D + k * 2D, 90F, 0F);
            ApocalypseLoot.dressMob(stand, rand);
            level.addFreshEntity(stand);
        }

        // Dye vats.
        for (int k = 1; k < 16; k += 5)
            buildVat(level, rand, x + 11, y, z + k);
    }

    private static void buildVat(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        boolean tall = rand.nextBoolean();
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(level, x, y, z, x + 4, y + 1, z + 4, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, x, y + 1, z, x + 4, y + (tall ? 3 : 2), z + 4, Blocks.OAK_PLANKS.defaultBlockState());
        for (int i = 0; i < 4; i += 3)
        {
            for (int k = 0; k < 4; k += 3)
                fill(level, x + i, y, z + k, x + i + 1, y + 3, z + k + 1, air);
        }
        fill(level, x + 1, y, z + 1, x + 3, y + 3, z + 3, air);
        fill(level, x + 1, y, z + 1, x + 3, y + (tall ? 2 : 1), z + 3, WOOL.get(rand.nextInt(WOOL.size())).defaultBlockState());
    }

    // ---------------------------------------------------------------------------------------------
    // Boss pillars: the bedrock formation around the world origin (1.12.2 WorldGenBossPillar)
    // ---------------------------------------------------------------------------------------------

    /**
     * Four bedrock towers, one per quadrant, rising exponentially from the ground 300 blocks out
     * to about Y=236 at the inner corners. The axes between them are left open for the roads.
     * Their inner corners at (&plusmn;13, &plusmn;13) are where the power cubes that summon the
     * boss are placed.
     */
    public static void generateBossPillars(WorldGenLevel level, ChunkPos chunk, RandomSource rand)
    {
        double nearestX = Math.max(0, Math.max(chunk.getMinBlockX(), -chunk.getMaxBlockX()));
        double nearestZ = Math.max(0, Math.max(chunk.getMinBlockZ(), -chunk.getMaxBlockZ()));
        if (nearestX * nearestX + nearestZ * nearestZ > PILLAR_OUTER_EDGE * PILLAR_OUTER_EDGE)
            return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++)
        {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++)
            {
                if (Math.abs(x) <= PILLAR_INNER_EDGE || Math.abs(z) <= PILLAR_INNER_EDGE)
                    continue;
                double height = pillarHeight(x, z);
                if (height <= 1D)
                    continue;

                boolean innerEdge = Math.abs(x) == PILLAR_INNER_EDGE + 1 || Math.abs(z) == PILLAR_INNER_EDGE + 1;
                pos.set(x, Math.min((int) height, level.getMaxBuildHeight() - 1), z);
                while (level.getBlockState(pos).isAir() && pos.getY() > level.getMinBuildHeight() + 1)
                {
                    set(level, pos.getX(), pos.getY(), pos.getZ(), innerEdge && rand.nextInt(500) == 0
                        ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : Blocks.BEDROCK.defaultBlockState());
                    pos.move(Direction.DOWN);
                }
            }
        }
    }

    public static double pillarHeight(int x, int z)
    {
        return PILLAR_KA * Math.exp(PILLAR_KB * Math.sqrt((double) x * x + (double) z * z));
    }

    // ---------------------------------------------------------------------------------------------
    // Abandoned portal ruin (1.12.2 WorldGenAbandonedPortal)
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds the stepped lab-stone foundation and the weapon cache of a 1.12.2 abandoned portal
     * around a portal whose lower-left power cube sits at {@code corner}.
     */
    public static void buildPortalRuin(WorldGenLevel level, BlockPos corner)
    {
        int x = corner.getX() - 4;
        int y = corner.getY() - 2;
        int z = corner.getZ() - 4;
        BlockState lab = ApocalypseContent.blockLabStone.get().defaultBlockState();
        replaceEmpty(level, x, y - 3, z, x + 12, y - 2, z + 12, lab);
        replaceEmpty(level, x + 1, y - 2, z + 1, x + 11, y - 1, z + 11, lab);
        replaceEmpty(level, x + 2, y - 1, z + 2, x + 10, y, z + 10, lab);
        replaceEmpty(level, x + 3, y, z + 3, x + 9, y + 1, z + 9, lab);
    }

    /** The 1.12.2 portal cache: weapon loot plus the obsidian and a power cube for a new portal. */
    public static void placePortalCache(WorldGenLevel level, RandomSource rand, BlockPos corner)
    {
        int x = corner.getX() - 4 + 3 + rand.nextInt(2) * 5;
        int y = corner.getY() - 1;
        int z = corner.getZ() - 4 + 3 + rand.nextInt(2) * 5;
        placeChest(level, x, y, z, Direction.SOUTH);
        fillContainer(level, x, y, z, container -> {
            ApocalypseLoot.fillWeaponChest(rand, container);
            container.setItem(rand.nextInt(container.getContainerSize()), new ItemStack(Blocks.OBSIDIAN, rand.nextInt(8) + 1));
            container.setItem(rand.nextInt(container.getContainerSize()), new ItemStack(ApocalypseContent.BLOCK_POWER_CUBE_ITEM.get()));
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------------------------------

    /** The 1.12.2 populator seed for a region or chunk, so that every chunk of it agrees. */
    static RandomSource regionRandom(long seed, int x, int z)
    {
        RandomSource seeder = RandomSource.create(seed);
        long k = seeder.nextLong() / 2L * 2L + 1L;
        long l = seeder.nextLong() / 2L * 2L + 1L;
        return RandomSource.create((long) x * k + (long) z * l ^ seed);
    }

    static RandomSource chunkRandom(long seed, int chunkX, int chunkZ)
    {
        // Salted so a chunk's contents do not correlate with the region roll that selected it.
        return regionRandom(seed ^ 0x5DEECE66DL, chunkX, chunkZ);
    }

    /** Surface height straight from the noise, readable for chunks that are not loaded. */
    private static int noiseSurface(WorldGenLevel level, int x, int z)
    {
        ServerChunkCache chunkSource = level.getLevel().getChunkSource();
        return chunkSource.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, chunkSource.randomState());
    }

    private static boolean isBiomeAtChunkCentre(WorldGenLevel level, int chunkX, int chunkZ, ResourceKey<Biome> biome)
    {
        // Every Apocalypse biome has the same depth parameter, so the height sampled is irrelevant.
        return level.getBiome(new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8)).is(biome);
    }

    private static void placeWeaponBox(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        ApocalypseLoot.randomWeaponBox(rand).ifPresent(block -> set(level, x, y, z, block.defaultBlockState()));
    }

    private static void placeChest(WorldGenLevel level, int x, int y, int z, Direction facing)
    {
        set(level, x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
    }

    private static void placeItemHolder(WorldGenLevel level, Block block, int x, int y, int z, Direction facing)
    {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        set(level, x, y, z, state);
    }

    private static void fillItemHolder(WorldGenLevel level, RandomSource rand, int x, int y, int z)
    {
        if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof ItemHolderBlockEntity holder)
        {
            ItemStack stack = ApocalypseLoot.itemHolderLoot(rand, true);
            if (!stack.isEmpty())
                holder.setStack(stack);
        }
    }

    private static void fillContainer(WorldGenLevel level, int x, int y, int z, java.util.function.Consumer<Container> filler)
    {
        if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof Container container)
            filler.accept(container);
    }

    private static void set(WorldGenLevel level, int x, int y, int z, BlockState state)
    {
        ApocalypseWorldgen.setBlock(level, new BlockPos(x, y, z), state);
    }

    /** 1.12.2 {@code WorldGenFlan.fillArea}: exclusive upper bounds, {@code shell} on every face. */
    private static void fill(WorldGenLevel level, int x1, int y1, int z1, int x2, int y2, int z2, BlockState shell, BlockState inner)
    {
        for (int i = x1; i < x2; i++)
        {
            for (int j = y1; j < y2; j++)
            {
                for (int k = z1; k < z2; k++)
                {
                    boolean edge = i == x1 || i == x2 - 1 || j == y1 || j == y2 - 1 || k == z1 || k == z2 - 1;
                    set(level, i, j, k, edge ? shell : inner);
                }
            }
        }
    }

    private static void fill(WorldGenLevel level, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)
    {
        fill(level, x1, y1, z1, x2, y2, z2, state, state);
    }

    private static void replaceEmpty(WorldGenLevel level, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)
    {
        for (int i = x1; i < x2; i++)
        {
            for (int j = y1; j < y2; j++)
            {
                for (int k = z1; k < z2; k++)
                {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (ApocalypseWorldgen.canWrite(level, pos) && level.getBlockState(pos).isAir())
                        set(level, i, j, k, state);
                }
            }
        }
    }
}
