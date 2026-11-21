package com.flansmodultimate.client.model;

import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.core.Direction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Coarse outline walls must sit and sample where vanilla's per-pixel walls would, without bridging gaps. */
class ItemIconExtrusionTest
{
    private static final String TEXTURE = "#layer0";

    @Test
    void singlePixelGetsVanillaWallsAroundTheWholeFace()
    {
        List<BlockElement> walls = walls(1, new String[] {"#"});

        assertEquals(4, walls.size());
        assertWall(wall(walls, Direction.UP), 0F, 16F, 16F, 16F, new float[] {0F, 0F, 16F, 16F});
        assertWall(wall(walls, Direction.DOWN), 0F, 0F, 16F, 0F, new float[] {0F, 0F, 16F, 16F});
        assertWall(wall(walls, Direction.EAST), 0F, 16F, 0F, 0F, new float[] {0F, 16F, 16F, 0F});
        assertWall(wall(walls, Direction.WEST), 16F, 16F, 16F, 0F, new float[] {0F, 16F, 16F, 0F});
    }

    @Test
    void solidRunsShareOneWallPerSideAndInteriorEdgesGetNone()
    {
        List<BlockElement> walls = walls(2, new String[] {"##", "##"});

        assertEquals(4, walls.size());
        assertWall(wall(walls, Direction.UP), 0F, 16F, 16F, 16F, new float[] {0F, 0F, 16F, 8F});
    }

    @Test
    void gapsSplitWallsInsteadOfBridgingThem()
    {
        List<BlockElement> walls = walls(3, new String[] {"#.#"});

        assertEquals(8, walls.size());
        assertEquals(2, walls.stream().filter(element -> element.faces.containsKey(Direction.UP)).count());
        assertEquals(2, walls.stream().filter(element -> element.faces.containsKey(Direction.EAST)).count());
    }

    @Test
    void coarseWallsShowTheOutermostOpaquePixelsOfTheirCell()
    {
        // One 4x4 cell whose only opaque pixel sits in its second row and third column
        List<BlockElement> walls = walls(1, new String[] {"....", "..#.", "....", "...."});

        assertEquals(4, walls.size());
        // The cell spans the whole face; the top wall samples source row 1, a quarter of the texture per row
        assertWall(wall(walls, Direction.UP), 0F, 16F, 16F, 16F, new float[] {0F, 4F, 16F, 8F});
        assertWall(wall(walls, Direction.EAST), 0F, 16F, 0F, 0F, new float[] {8F, 16F, 12F, 0F});
    }

    @Test
    void cellsShowingDifferentPixelsGetSeparateWalls()
    {
        // Two cells side by side whose topmost opaque pixels sit in different source rows
        List<BlockElement> walls = walls(2, new String[] {"#...", "..#.", "....", "...."});

        assertEquals(2, walls.stream().filter(element -> element.faces.containsKey(Direction.UP)).count());
    }

    private static List<BlockElement> walls(int resolution, String[] rows)
    {
        ItemIconExtrusion.Outline outline = ItemIconExtrusion.Outline.of(rows[0].length(), rows.length, resolution, (x, y) -> rows[y].charAt(x) == '#');
        return ItemIconExtrusion.walls(outline, TEXTURE, 0);
    }

    private static BlockElement wall(List<BlockElement> walls, Direction facing)
    {
        return walls.stream().filter(element -> element.faces.containsKey(facing)).findFirst().orElseThrow();
    }

    private static void assertWall(BlockElement wall, float fromX, float fromY, float toX, float toY, float[] uv)
    {
        assertEquals(fromX, wall.from.x(), 1e-5F);
        assertEquals(fromY, wall.from.y(), 1e-5F);
        assertEquals(7.5F, wall.from.z(), 1e-5F);
        assertEquals(toX, wall.to.x(), 1e-5F);
        assertEquals(toY, wall.to.y(), 1e-5F);
        assertEquals(8.5F, wall.to.z(), 1e-5F);
        assertArrayEquals(uv, wall.faces.values().iterator().next().uv.uvs, 1e-5F);
    }
}
