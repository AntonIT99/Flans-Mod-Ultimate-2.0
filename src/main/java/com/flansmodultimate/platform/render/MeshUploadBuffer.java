package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Version boundary for building static meshes for upload, one at a time, in reusable storage. Forge 1.20.1 reuses one
 * {@link BufferBuilder} and uploads its rendered buffer; Minecraft 1.21 builds into a {@code ByteBufferBuilder}. Client-only.
 */
public final class MeshUploadBuffer implements AutoCloseable
{
    private final BufferBuilder builder;

    public MeshUploadBuffer(int initialCapacity)
    {
        builder = new BufferBuilder(initialCapacity);
    }

    /** Starts a mesh and returns the consumer to write it to. */
    public BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format)
    {
        builder.begin(mode, format);
        return builder;
    }

    /** Finishes the mesh begun last and uploads it into the bound vertex buffer. */
    public void upload(VertexBuffer buffer)
    {
        buffer.upload(builder.end());
    }

    /** Drops an unfinished mesh, so that the next one can begin; harmless when none is open. */
    public void discard()
    {
        // discard() alone leaves BufferBuilder.building set, poisoning every upload after a failure.
        if (builder.building())
            builder.end().release();
        builder.discard();
    }

    @Override
    public void close()
    {
        discard();
    }
}
