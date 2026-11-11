package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.client.render.EntityVertexBatch;
import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

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
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz)
    {
        RenderDiagnostics.countCpuVertex();
        EntityVertexBatch.vertex(batched(), x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
    }

    @Override
    @NotNull
    public VertexConsumer vertex(double x, double y, double z)
    {
        unbatched().vertex(x, y, z);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer color(int r, int g, int b, int a)
    {
        unbatched().color(r, g, b, a);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv(float u, float v)
    {
        unbatched().uv(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer overlayCoords(int u, int v)
    {
        unbatched().overlayCoords(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv2(int u, int v)
    {
        unbatched().uv2(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer normal(float x, float y, float z)
    {
        unbatched().normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex()
    {
        RenderDiagnostics.countCpuVertex();
        unbatched().endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a)
    {
        unbatched().defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor()
    {
        unbatched().unsetDefaultColor();
    }
}
