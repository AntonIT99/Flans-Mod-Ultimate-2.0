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
    /** Hidden indices between two visible ranges that bridging range sets draw anyway, keeping one range. */
    static final int DEFAULT_BRIDGED_GAP = 600;
    /** The bridge applied now; adjustable at runtime to compare frame times from one spot. */
    static volatile int bridgedGap = DEFAULT_BRIDGED_GAP;
    private final boolean bridging;
    private IntBuffer rangeCounts;
    private PointerBuffer rangeOffsets;

    VisibleRanges(int capacity)
    {
        this(capacity, false);
    }

    /**
     * @param bridging draw up to {@link #bridgedGap} size-culled indices between visible ranges, merging them.
     *                 Some drivers process each range of a multi-draw much like a draw call, while culled parts
     *                 are below a pixel: drawing a short stretch of them can cost less than another range.
     */
    VisibleRanges(int capacity, boolean bridging)
    {
        this.bridging = bridging;
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
            int gap = count == 0 ? -1 : indices - starts[count - 1] - counts[count - 1];
            if (gap == 0)
                counts[count - 1] += size;
            else if (gap > 0 && bridging && gap <= bridgedGap)
            {
                // The hidden stretch is drawn too, so its vertices count as drawn.
                visibleVertices += gap / 6 * 4;
                counts[count - 1] += gap + size;
            }
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
