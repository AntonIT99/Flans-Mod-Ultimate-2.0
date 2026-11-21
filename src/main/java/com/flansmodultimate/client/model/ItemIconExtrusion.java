package com.flansmodultimate.client.model;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the edge walls of flat Flan item models from a coarse outline of large icons. Vanilla adds a wall per
 * transparent-to-opaque pixel edge, so a 512 pixel icon bakes and draws thousands of quads its few screen pixels never
 * show. Like vanilla's, each wall shows one row or column of icon pixels: the outermost opaque pixels of its cells.
 * Icons up to {@link #OUTLINE_RESOLUTION} pixels, animated icons and other mods' items keep vanilla's walls.
 */
public final class ItemIconExtrusion
{
    /** Side length of the outline grid, and the largest icon that keeps per-pixel walls. */
    static final int OUTLINE_RESOLUTION = 64;
    private static final float MIN_Z = 7.5F;
    private static final float MAX_Z = 8.5F;
    private static final String ICON_PREFIX = "item/";

    private ItemIconExtrusion()
    {}

    /** Whether a source pixel is opaque. */
    @FunctionalInterface
    interface Opacity
    {
        boolean opaque(int x, int y);
    }

    /** The walls for a large Flan icon, or null to let vanilla build them. Coordinates follow vanilla's item generator. */
    @Nullable
    public static List<BlockElement> sideElements(SpriteContents sprite, String texture, int tintIndex)
    {
        int width = sprite.width();
        int height = sprite.height();
        if (Math.max(width, height) <= OUTLINE_RESOLUTION || !isFlanIcon(sprite.name()) || sprite.getUniqueFrames().count() != 1)
            return null;
        int frame = sprite.getUniqueFrames().findFirst().orElse(0);
        return walls(Outline.of(width, height, OUTLINE_RESOLUTION, (x, y) -> !sprite.isTransparent(frame, x, y)), texture, tintIndex);
    }

    /** The walls around the solid cells of an outline spanning the whole 16 unit item face. */
    static List<BlockElement> walls(Outline outline, String texture, int tintIndex)
    {
        Walls walls = new Walls(outline, texture, tintIndex);
        for (int y = 0; y < outline.height; y++)
        {
            walls.horizontalRuns(Direction.UP, y, y - 1);
            walls.horizontalRuns(Direction.DOWN, y, y + 1);
        }
        for (int x = 0; x < outline.width; x++)
        {
            walls.verticalRuns(Direction.EAST, x, x - 1);
            walls.verticalRuns(Direction.WEST, x, x + 1);
        }
        return walls.elements;
    }

    private static boolean isFlanIcon(ResourceLocation name)
    {
        return FlansMod.FLANSMOD_ID.equals(name.getNamespace()) && name.getPath().startsWith(ICON_PREFIX);
    }

    /**
     * A grid of cells over the icon. A cell is solid when any of its pixels is opaque, so thin details keep their walls,
     * and remembers its outermost opaque source row or column on each side for the wall there to show.
     */
    static final class Outline
    {
        final int width;
        final int height;
        final int sourceWidth;
        final int sourceHeight;
        private final int[] top;
        private final int[] bottom;
        private final int[] left;
        private final int[] right;

        private Outline(int width, int height, int sourceWidth, int sourceHeight)
        {
            this.width = width;
            this.height = height;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            top = new int[width * height];
            bottom = new int[width * height];
            left = new int[width * height];
            right = new int[width * height];
            Arrays.fill(top, Integer.MAX_VALUE);
            Arrays.fill(bottom, -1);
            Arrays.fill(left, Integer.MAX_VALUE);
            Arrays.fill(right, -1);
        }

        /** An outline at most {@code resolution} cells on its longer side; smaller icons get one cell per pixel. */
        static Outline of(int sourceWidth, int sourceHeight, int resolution, Opacity opacity)
        {
            float scale = Math.min(1F, (float) resolution / Math.max(sourceWidth, sourceHeight));
            int width = Math.max(1, Math.round(sourceWidth * scale));
            int height = Math.max(1, Math.round(sourceHeight * scale));
            Outline outline = new Outline(width, height, sourceWidth, sourceHeight);
            for (int y = 0; y < sourceHeight; y++)
            {
                int row = y * height / sourceHeight * width;
                for (int x = 0; x < sourceWidth; x++)
                {
                    if (!opacity.opaque(x, y))
                        continue;
                    int cell = row + x * width / sourceWidth;
                    outline.top[cell] = Math.min(outline.top[cell], y);
                    outline.bottom[cell] = Math.max(outline.bottom[cell], y);
                    outline.left[cell] = Math.min(outline.left[cell], x);
                    outline.right[cell] = Math.max(outline.right[cell], x);
                }
            }
            return outline;
        }

        boolean solid(int x, int y)
        {
            return x >= 0 && y >= 0 && x < width && y < height && bottom[y * width + x] >= 0;
        }

        /** The source row or column the wall on this side of the cell shows. */
        int sample(Direction facing, int x, int y)
        {
            int cell = y * width + x;
            return switch (facing)
            {
                case UP -> top[cell];
                case DOWN -> bottom[cell];
                case EAST -> left[cell];
                default -> right[cell];
            };
        }
    }

    /** Emits one wall per run of edge cells that show the same source row or column. */
    private static final class Walls
    {
        private final Outline outline;
        private final String texture;
        private final int tintIndex;
        private final float cellWidth;
        private final float cellHeight;
        private final float pixelWidth;
        private final float pixelHeight;
        private final List<BlockElement> elements = new ArrayList<>();

        Walls(Outline outline, String texture, int tintIndex)
        {
            this.outline = outline;
            this.texture = texture;
            this.tintIndex = tintIndex;
            cellWidth = 16F / outline.width;
            cellHeight = 16F / outline.height;
            pixelWidth = 16F / outline.sourceWidth;
            pixelHeight = 16F / outline.sourceHeight;
        }

        private boolean edge(int x, int y, int neighbourX, int neighbourY)
        {
            return outline.solid(x, y) && !outline.solid(neighbourX, neighbourY);
        }

        /** Walls facing up (neighbour row above) or down (below) along row {@code y}. */
        void horizontalRuns(Direction facing, int y, int neighbourY)
        {
            int x = 0;
            while (x < outline.width)
            {
                if (!edge(x, y, x, neighbourY))
                {
                    x++;
                    continue;
                }
                int start = x;
                int row = outline.sample(facing, x, y);
                while (x < outline.width && edge(x, y, x, neighbourY) && outline.sample(facing, x, y) == row)
                    x++;
                float x0 = start * cellWidth;
                float x1 = x * cellWidth;
                float wallY = 16F - (facing == Direction.UP ? y : y + 1) * cellHeight;
                add(facing, new Vector3f(x0, wallY, MIN_Z), new Vector3f(x1, wallY, MAX_Z), new float[] {x0, row * pixelHeight, x1, (row + 1) * pixelHeight});
            }
        }

        /** Walls facing east (neighbour column to the left) or west (to the right) along column {@code x}. */
        void verticalRuns(Direction facing, int x, int neighbourX)
        {
            int y = 0;
            while (y < outline.height)
            {
                if (!edge(x, y, neighbourX, y))
                {
                    y++;
                    continue;
                }
                int start = y;
                int column = outline.sample(facing, x, y);
                while (y < outline.height && edge(x, y, neighbourX, y) && outline.sample(facing, x, y) == column)
                    y++;
                float wallX = (facing == Direction.EAST ? x : x + 1) * cellWidth;
                float top = 16F - start * cellHeight;
                float bottom = 16F - y * cellHeight;
                add(facing, new Vector3f(wallX, top, MIN_Z), new Vector3f(wallX, bottom, MAX_Z),
                    new float[] {column * pixelWidth, y * cellHeight, (column + 1) * pixelWidth, start * cellHeight});
            }
        }

        private void add(Direction facing, Vector3f from, Vector3f to, float[] uv)
        {
            Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
            faces.put(facing, new BlockElementFace(null, tintIndex, texture, new BlockFaceUV(uv, 0)));
            elements.add(new BlockElement(from, to, faces, null, true));
        }
    }
}
