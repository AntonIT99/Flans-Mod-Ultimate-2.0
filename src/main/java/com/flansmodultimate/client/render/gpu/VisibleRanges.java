package com.flansmodultimate.client.render.gpu;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.nio.IntBuffer;

/** Ordered quad-index ranges; visibility never participates in mesh identity. */
final class VisibleRanges
{
    final boolean[] visible;
    final int[] starts;
    final int[] counts;
    int count;
    int geometries;
    int indices;
    int visibleVertices;
    private IntBuffer rangeCounts;
    private PointerBuffer rangeOffsets;

    VisibleRanges(int capacity)
    {
        visible = new boolean[capacity];
        starts = new int[capacity];
        counts = new int[capacity];
    }

    void add(int vertices, boolean draw)
    {
        int size = vertices / 4 * 6;
        visible[geometries++] = draw;
        if (draw)
        {
            visibleVertices += vertices;
            if (count > 0 && starts[count - 1] + counts[count - 1] == indices)
                counts[count - 1] += size;
            else
            {
                starts[count] = indices;
                counts[count++] = size;
            }
        }
        indices += size;
    }

    boolean allVisible() { return count == 1 && starts[0] == 0 && counts[0] == indices; }

    /** The caller binds the mesh and shader first. Native storage is reused by this batch. */
    void draw(VertexBuffer mesh)
    {
        if (count == 0) return;
        if (allVisible())
        {
            mesh.draw();
            return;
        }
        if (rangeCounts == null)
        {
            rangeCounts = BufferUtils.createIntBuffer(visible.length);
            rangeOffsets = BufferUtils.createPointerBuffer(visible.length);
        }
        // The shared sequential buffer may grow from shorts to ints after this mesh
        // was uploaded. Query its current type just as VertexBuffer.draw() does.
        VertexFormat.IndexType indexType = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type();
        rangeCounts.clear();
        rangeOffsets.clear();
        int indexBytes = indexType == VertexFormat.IndexType.INT ? 4 : 2;
        for (int i = 0; i < count; i++)
        {
            rangeCounts.put(counts[i]);
            rangeOffsets.put((long)starts[i] * indexBytes);
        }
        rangeCounts.flip();
        rangeOffsets.flip();
        GL32C.glMultiDrawElements(GL32C.GL_TRIANGLES, rangeCounts, indexType.asGLType, rangeOffsets);
    }

    void clear()
    {
        count = geometries = indices = visibleVertices = 0;
    }
}
