package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;

/**
 * Version boundary for building static meshes for upload, one at a time, in reusable storage. Forge 1.20.1 reuses one
 * {@code BufferBuilder}; Minecraft 1.21 starts a {@link BufferBuilder} over shared {@link ByteBufferBuilder} storage and
 * uploads its {@link MeshData}. Client-only.
 */
public final class MeshUploadBuffer implements AutoCloseable
{
    private final ByteBufferBuilder storage;
    @Nullable
    private BufferBuilder builder;

    public MeshUploadBuffer(int initialCapacity)
    {
        storage = new ByteBufferBuilder(initialCapacity);
    }

    /** Starts a mesh and returns the consumer to write it to. */
    public BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format)
    {
        builder = new BufferBuilder(storage, mode, format);
        return builder;
    }

    /** Finishes the mesh begun last and uploads it into the bound vertex buffer. */
    public void upload(VertexBuffer buffer)
    {
        BufferBuilder finished = builder;
        if (finished == null)
            throw new IllegalStateException("No mesh was begun");
        builder = null;
        try (MeshData meshData = finished.buildOrThrow())
        {
            buffer.upload(meshData);
        }
        finally
        {
            storage.clear();
        }
    }

    /** Drops an unfinished mesh, so that the next one can begin; harmless when none is open. */
    public void discard()
    {
        builder = null;
        storage.clear();
    }

    /** Frees the native storage. */
    @Override
    public void close()
    {
        builder = null;
        storage.close();
    }
}
