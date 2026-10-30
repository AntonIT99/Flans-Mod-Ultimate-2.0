package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * A top-down picture of the terrain around the points of a deployment map, drawn from the chunks the client has loaded
 * and coloured and shaded the way vanilla maps are. Columns of chunks the client does not have stay blank. North is up.
 */
final class DeployTerrainMap implements AutoCloseable
{
    /** Texels along the longer side of the picture; a bigger area is sampled more sparsely. */
    private static final int MAX_TEXELS = 320;
    /** Blocks a water column is looked into for its depth. */
    private static final int MAX_WATER_DEPTH = 10;
    /** Blank columns, in the ABGR order of {@link NativeImage}. */
    private static final int UNKNOWN_COLOUR = 0xFF2A3023;

    @Getter
    private final double minX;
    @Getter
    private final double minZ;
    @Getter
    private final double maxX;
    @Getter
    private final double maxZ;
    private final DynamicTexture texture;
    @Getter
    private final ResourceLocation location;

    private DeployTerrainMap(double minX, double minZ, double maxX, double maxZ, DynamicTexture texture, ResourceLocation location)
    {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.texture = texture;
        this.location = location;
    }

    /**
     * Samples the terrain of an area into a texture whose sides keep the area's proportions.
     *
     * @return the picture, or null without a level to draw it from
     */
    @Nullable
    static DeployTerrainMap build(Minecraft minecraft, double minX, double minZ, double maxX, double maxZ)
    {
        ClientLevel level = minecraft.level;
        if (level == null)
            return null;
        double spanX = Math.max(1D, maxX - minX);
        double spanZ = Math.max(1D, maxZ - minZ);
        double blocksPerTexel = Math.max(1D, Math.max(spanX, spanZ) / MAX_TEXELS);
        int width = Math.max(1, (int) Math.ceil(spanX / blocksPerTexel));
        int height = Math.max(1, (int) Math.ceil(spanZ / blocksPerTexel));
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int column = 0; column < width; column++)
        {
            int x = (int) Math.floor(minX + (column + 0.5D) * blocksPerTexel);
            // The surface north of the first texel shades it, as the row above does for every other one.
            int northHeight = surfaceY(level, x, (int) Math.floor(minZ - blocksPerTexel * 0.5D));
            for (int row = 0; row < height; row++)
            {
                int z = (int) Math.floor(minZ + (row + 0.5D) * blocksPerTexel);
                int surface = surfaceY(level, x, z);
                image.setPixelRGBA(column, row, surface == Integer.MIN_VALUE ? UNKNOWN_COLOUR : colour(level, pos, x, z, surface, northHeight));
                if (surface != Integer.MIN_VALUE)
                    northHeight = surface;
            }
        }
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = minecraft.getTextureManager().register(FlansMod.FLANSMOD_ID + "_deploy_terrain", texture);
        return new DeployTerrainMap(minX, minZ, minX + width * blocksPerTexel, minZ + height * blocksPerTexel, texture, location);
    }

    /** The y of the highest block of a column, or {@link Integer#MIN_VALUE} when its chunk is not loaded. */
    private static int surfaceY(ClientLevel level, int x, int z)
    {
        if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)))
            return Integer.MIN_VALUE;
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static int colour(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z, int surface, int northHeight)
    {
        // Columns cleared down to the void read as nothing at all, like the inside of an unloaded chunk.
        if (surface < level.getMinBuildHeight())
            return UNKNOWN_COLOUR;
        pos.set(x, surface, z);
        BlockState state = level.getBlockState(pos);
        MapColor colour = state.getMapColor(level, pos);
        while (colour == MapColor.NONE && pos.getY() > level.getMinBuildHeight())
        {
            pos.move(0, -1, 0);
            state = level.getBlockState(pos);
            colour = state.getMapColor(level, pos);
        }
        if (state.getFluidState().is(FluidTags.WATER))
            return colour.calculateRGBColor(waterShade(level, pos, x, z));
        MapColor.Brightness brightness = MapColor.Brightness.NORMAL;
        if (northHeight != Integer.MIN_VALUE && surface > northHeight)
            brightness = MapColor.Brightness.HIGH;
        else if (surface < northHeight)
            brightness = MapColor.Brightness.LOW;
        return colour.calculateRGBColor(brightness);
    }

    /** Deep water is darker, with a dither between depths as on a vanilla map. */
    private static MapColor.Brightness waterShade(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z)
    {
        int depth = 0;
        while (depth < MAX_WATER_DEPTH && pos.getY() > level.getMinBuildHeight() && level.getFluidState(pos).is(FluidTags.WATER))
        {
            pos.move(0, -1, 0);
            depth++;
        }
        double shade = depth * 0.1D + ((x + z) & 1) * 0.2D;
        if (shade < 0.5D)
            return MapColor.Brightness.HIGH;
        return shade > 0.9D ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    /** Where a block position falls across the picture, from 0 at its west edge to 1 at its east edge. */
    float u(double x)
    {
        return (float) ((x - minX) / (maxX - minX));
    }

    /** Where a block position falls down the picture, from 0 at its north edge to 1 at its south edge. */
    float v(double z)
    {
        return (float) ((z - minZ) / (maxZ - minZ));
    }

    @Override
    public void close()
    {
        Minecraft.getInstance().getTextureManager().release(location);
        texture.close();
    }
}
