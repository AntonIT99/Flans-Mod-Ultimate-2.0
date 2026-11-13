package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.client.render.EntityVertexBatch;
import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

/**
 * Rigid geometry sink for scopes that cannot draw cached meshes, such as those of an active shader pack: it writes
 * every visible part's vertices to the scope's ordinary buffer, in submission order. Parts still take the rigid
 * submission paths, so static parts skip their pose and polygon walks, and their vertices come from flat arrays.
 * With a bulk vertex writer, one batch stays open across parts instead of one write per part. Render thread only.
 */
// Render diagnostics counters are deliberately global.
@SuppressWarnings("java:S2696")
final class DirectRigidSink implements RigidGeometryConsumer
{
    private MultiBufferSource source;
    private RenderType type;
    private VertexConsumer target;
    /** Whether the target takes bulk writes, so a batch is worth opening. */
    private boolean bulk;
    /** Whether this sink holds the open vertex batch. */
    private boolean batching;
    /** A nested scope may have ended the target's buffer: fetch it again before the next vertex. */
    private boolean stale;

    void begin(MultiBufferSource source, RenderType type)
    {
        this.source = source;
        this.type = type;
        target = null;
        batching = false;
        stale = true;
    }

    /** A nested renderer can change any buffer. Complete our writes before it runs. */
    void suspend()
    {
        closeBatch();
        stale = true;
    }

    void end()
    {
        try
        {
            closeBatch();
        }
        finally
        {
            source = null;
            type = null;
            target = null;
        }
    }

    private void closeBatch()
    {
        if (!batching)
            return;
        batching = false;
        EntityVertexBatch.end();
    }

    /** The scope's buffer, with the batch open whenever the buffer can take one. */
    private VertexConsumer batched()
    {
        if (stale)
        {
            stale = false;
            target = source.getBuffer(type);
            bulk = VertexWriterPlatform.canWrite(target);
        }
        if (bulk && !batching)
            batching = EntityVertexBatch.begin(target);
        return target;
    }

    /** The scope's buffer for element-wise writes, which must not overtake batched vertices. */
    private VertexConsumer unbatched()
    {
        closeBatch();
        if (stale)
        {
            stale = false;
            target = source.getBuffer(type);
            bulk = VertexWriterPlatform.canWrite(target);
        }
        return target;
    }

    @Override
    public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha)
    {
        if (RenderDiagnostics.enabled)
        {
            RenderDiagnostics.directParts++;
            RenderDiagnostics.directVertices += geometry.vertexCount();
        }
        geometry.emit(pose, batched(), light, overlay, red, green, blue, alpha);
    }

    @Override
    public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
    {
        if (visible)
            submit(geometry, pose, light, overlay, red, green, blue, alpha);
        else if (RenderDiagnostics.enabled)
            RenderDiagnostics.directCulledParts++;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz)
    {
        RenderDiagnostics.countCpuVertex();
        EntityVertexBatch.vertex(batched(), x, y, z, FastColor.ARGB32.red(color) / 255F, FastColor.ARGB32.green(color) / 255F, FastColor.ARGB32.blue(color) / 255F,
            FastColor.ARGB32.alpha(color) / 255F, u, v, overlay, light, nx, ny, nz);
    }

    /** Minecraft 1.21 has no endVertex; a chained vertex starts here and ends at the next one. */
    @Override
    @NotNull
    public VertexConsumer addVertex(float x, float y, float z)
    {
        RenderDiagnostics.countCpuVertex();
        unbatched().addVertex(x, y, z);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer setColor(int r, int g, int b, int a)
    {
        unbatched().setColor(r, g, b, a);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer setUv(float u, float v)
    {
        unbatched().setUv(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer setUv1(int u, int v)
    {
        unbatched().setUv1(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer setUv2(int u, int v)
    {
        unbatched().setUv2(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer setNormal(float x, float y, float z)
    {
        unbatched().setNormal(x, y, z);
        return this;
    }
}
