package com.flansmodultimate.apocalyse.common.world;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The motorway network left over the wasteland.
 *
 * <p>Two roads run along the world axes and ring roads circle the origin every 600 blocks,
 * all decked at a constant height. The layout is a pure function of world coordinates, so
 * neighbouring chunks always join up and no seed or ordering can shift it.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseRoads
{
    /** Height of the road deck. Everything below is filled in, everything above cleared. */
    private static final int DECK_Y = 64;

    private static final double RING_INNER = 400.0D;
    private static final double RING_MID = 410.0D;
    private static final double RING_OUTER = 420.0D;
    private static final double RING_SPACING = 600.0D;
    private static final double EDGE_WIDTH = 1.5D;
    private static final double AXIS_HALF_WIDTH = 10.0D;
    private static final double DASH_LENGTH = 10.0D;
    private static final double ARCH_LENGTH = 16.0D;
    /** Lowest a support pillar may reach before the deck is left to span the gap. */
    private static final double MIN_ARCH_HEIGHT = 20.0D;
    private static final double ARCH_RELIEF = 38.0D;
    private static final double TUNNEL_BASE = 70.0D;
    private static final double TUNNEL_RELIEF = 7.0D;

    private static final BlockState DECK = Blocks.BLACK_CONCRETE.defaultBlockState();
    private static final BlockState MARKING = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState KERB = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState SUPPORT = Blocks.STONE.defaultBlockState();

    public static void generate(WorldGenLevel level, ChunkPos chunkPos)
    {
        if (DECK_Y <= level.getMinBuildHeight() || DECK_Y >= level.getMaxBuildHeight() - 1)
            return;

        for (int localX = 0; localX < 16; localX++)
        {
            for (int localZ = 0; localZ < 16; localZ++)
                surface(level, chunkPos.getMinBlockX() + localX, chunkPos.getMinBlockZ() + localZ);
        }
    }

    private static void surface(WorldGenLevel level, int x, int z)
    {
        Carriageway carriageway = carriagewayAt(x, z);
        if (carriageway == null)
            return;

        BlockPos deck = new BlockPos(x, DECK_Y, z);
        if (!level.getWorldBorder().isWithinBounds(deck))
            return;
        ApocalypseWorldgen.setBlock(level, deck, carriageway.surface());

        // Carry the deck down onto the terrain, and cut a clear span above it.
        BlockPos support = deck.below();
        while (support.getY() > carriageway.archHeight() && support.getY() > level.getMinBuildHeight()
            && level.getBlockState(support).isAir())
        {
            ApocalypseWorldgen.setBlock(level, support, SUPPORT);
            support = support.below();
        }

        BlockPos headroom = deck.above();
        while (headroom.getY() < carriageway.tunnelHeight() && headroom.getY() < level.getMaxBuildHeight() - 1)
        {
            if (!level.getBlockState(headroom).isAir())
                ApocalypseWorldgen.setBlock(level, headroom, Blocks.AIR.defaultBlockState());
            headroom = headroom.above();
        }
    }

    /** The road covering this column, or null where there is none. */
    private static Carriageway carriagewayAt(int x, int z)
    {
        double distance = Math.sqrt((double) x * x + (double) z * z);
        double theta = Math.atan2(z, x);
        double ringOffset = Mth.positiveModulo(distance, RING_SPACING);
        int ring = (int) Math.floor(distance / RING_SPACING);
        double circumference = (RING_MID + RING_SPACING * ring) * Math.PI * 2.0D;
        double archArc = Math.PI * 2.0D * ARCH_LENGTH / circumference;
        double dashArc = Math.PI * 2.0D * DASH_LENGTH / circumference;

        int carriageways = 0;
        boolean kerb = false;
        boolean dash = false;
        double archHeight = 128.0D;
        double tunnelHeight = 0.0D;

        if (RING_INNER <= ringOffset && ringOffset <= RING_OUTER)
        {
            carriageways++;
            kerb = ringOffset <= RING_INNER + EDGE_WIDTH || RING_OUTER - EDGE_WIDTH <= ringOffset;
            dash = RING_MID - 0.5D <= ringOffset && ringOffset <= RING_MID + 0.5D
                && Mth.positiveModulo(theta, dashArc * 2.0D) < dashArc;
            archHeight = MIN_ARCH_HEIGHT + ARCH_RELIEF * Math.abs(Math.sin(theta / archArc));
            double across = (ringOffset - RING_INNER) / (RING_OUTER - RING_INNER);
            tunnelHeight = TUNNEL_BASE + TUNNEL_RELIEF * Math.sin(across * Math.PI);
        }

        double absX = Math.abs(x);
        if (absX < AXIS_HALF_WIDTH)
        {
            carriageways++;
            kerb = absX > AXIS_HALF_WIDTH - EDGE_WIDTH;
            dash = absX < 0.5D && Mth.positiveModulo((double) z, 2.0D * DASH_LENGTH) < DASH_LENGTH;
            archHeight = Math.min(archHeight, MIN_ARCH_HEIGHT + ARCH_RELIEF * Math.abs(Math.sin(z * Math.PI / (ARCH_LENGTH * 3.0D))));
            tunnelHeight = Math.max(tunnelHeight, TUNNEL_BASE + TUNNEL_RELIEF * Math.cos(absX / AXIS_HALF_WIDTH * Math.PI * 0.5D));
        }

        double absZ = Math.abs(z);
        if (absZ < AXIS_HALF_WIDTH)
        {
            carriageways++;
            kerb = absZ > AXIS_HALF_WIDTH - EDGE_WIDTH;
            dash = absZ < 0.5D && Mth.positiveModulo((double) x, 2.0D * DASH_LENGTH) < DASH_LENGTH;
            archHeight = Math.min(archHeight, MIN_ARCH_HEIGHT + ARCH_RELIEF * Math.abs(Math.sin(x * Math.PI / (ARCH_LENGTH * 3.0D))));
            tunnelHeight = Math.max(tunnelHeight, TUNNEL_BASE + TUNNEL_RELIEF * Math.cos(absZ / AXIS_HALF_WIDTH * Math.PI * 0.5D));
        }

        if (carriageways == 0)
            return null;
        // A junction is all carriageway: no kerb runs through the middle of it.
        return new Carriageway(carriageways > 1 ? false : kerb, dash, archHeight, tunnelHeight);
    }

    private record Carriageway(boolean kerb, boolean dash, double archHeight, double tunnelHeight)
    {
        BlockState surface()
        {
            if (kerb)
                return KERB;
            return dash ? MARKING : DECK;
        }
    }
}
