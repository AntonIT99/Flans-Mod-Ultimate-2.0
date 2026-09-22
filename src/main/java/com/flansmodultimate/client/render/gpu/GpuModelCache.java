package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.VehicleThermalRenderer;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/** Bounded, render-thread-only GPU batches of local rigid geometry with per-draw pose palettes. */
public final class GpuModelCache
{
    public static final int PARTS_PER_BATCH = 16;
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final long UPLOAD_BYTES_PER_TICK = 2L * 1024 * 1024;
    private static final int MAX_BATCHES = 2048;
    private static final LinkedHashMap<List<RigidGeometry>, Mesh> meshes = new LinkedHashMap<>(64, 0.75F, true);
    private static final BufferBuilder builder = new BufferBuilder(256);
    private static final PoseStack.Pose IDENTITY = new PoseStack().last();
    private static ShaderInstance shader;
    private static boolean failed;
    private static Boolean incompatibleRenderer;
    private static long bytes;
    private static long uploadTick = Long.MIN_VALUE;
    private static long uploadedBytes;

    private GpuModelCache() {}

    public static void registerShader(RegisterShadersEvent event)
    {
        shader = null;
        clear();
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "rigid_model"), DefaultVertexFormat.NEW_ENTITY), value -> {
                if (value.getUniform("PartPose") == null || value.getUniform("PartNormal") == null
                    || value.getUniform("PartData") == null)
                {
                    FlansMod.log.warn("GPU model shader lacks pose uniforms; using standard model rendering");
                    return;
                }
                shader = value;
            });
        }
        catch (IOException | RuntimeException ex)
        {
            FlansMod.log.warn("GPU model shader unavailable; using standard model rendering", ex);
        }
    }

    public static ShaderInstance shader() { return shader; }

    public static void clear()
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(GpuModelCache::clear);
            return;
        }
        meshes.values().forEach(mesh -> mesh.buffer.close());
        meshes.clear();
        bytes = 0;
        uploadedBytes = 0;
        uploadTick = Long.MIN_VALUE;
        failed = false;
    }

    private static boolean incompatibleRenderer()
    {
        if (incompatibleRenderer == null)
        {
            incompatibleRenderer = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
            try
            {
                Class.forName("net.optifine.Config", false, GpuModelCache.class.getClassLoader());
                incompatibleRenderer = true;
            }
            catch (ClassNotFoundException ignored) {}
        }
        return incompatibleRenderer;
    }

    /** Only explicit normal model passes opt in. Outlines, glint and special buffer wrappers fall back. */
    public static void render(MultiBufferSource source, EnumRenderPass pass, ResourceLocation texture,
                              boolean translucent, boolean cull, boolean allowed, Consumer<VertexConsumer> render)
    {
        RenderType vanilla = pass.getRenderType(texture, translucent, cull);
        ModClientConfig config = ModClientConfig.get();
        if (!allowed || config == null || !config.enableGpuModelCache || shader == null || failed
            || pass != EnumRenderPass.DEFAULT || translucent && !config.enableFastTranslucentRendering
            || source.getClass() != MultiBufferSource.BufferSource.class || VehicleThermalRenderer.isRenderingMask()
            || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS || incompatibleRenderer())
        {
            render.accept(source.getBuffer(vanilla));
            return;
        }
        try (Batch batch = new Batch((MultiBufferSource.BufferSource) source, vanilla,
            CustomRenderType.gpuModel(texture, translucent, cull)))
        {
            render.accept(batch);
        }
    }

    private record Mesh(VertexBuffer buffer, long bytes) {}
    private record Draw(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                        float red, float green, float blue, float alpha) {}

    private static Mesh mesh(List<RigidGeometry> key)
    {
        Mesh cached = meshes.get(key);
        if (cached != null)
            return cached;
        long size = key.stream().mapToLong(RigidGeometry::vertexCount).sum() * DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        long tick = Minecraft.getInstance().level == null ? System.nanoTime() / 50_000_000L
            : Minecraft.getInstance().level.getGameTime();
        if (tick != uploadTick)
        {
            uploadTick = tick;
            uploadedBytes = 0;
        }
        if (size == 0 || size > UPLOAD_BYTES_PER_TICK || uploadedBytes + size > UPLOAD_BYTES_PER_TICK)
            return null;
        while (!meshes.isEmpty() && (bytes + size > MAX_BYTES || meshes.size() >= MAX_BATCHES))
        {
            var iterator = meshes.entrySet().iterator();
            Mesh oldest = iterator.next().getValue();
            oldest.buffer.close();
            bytes -= oldest.bytes;
            iterator.remove();
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try
        {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            for (int i = 0; i < key.size(); i++)
                // UV1 is the palette index in the cached mesh; real overlay coordinates are uniforms.
                key.get(i).draw(IDENTITY, builder, 0, i, 1F, 1F, 1F, 1F);
            buffer.bind();
            buffer.upload(builder.end());
            Mesh mesh = new Mesh(buffer, size);
            meshes.put(key, mesh);
            bytes += size;
            uploadedBytes += size;
            return mesh;
        }
        catch (RuntimeException ex)
        {
            buffer.close();
            builder.discard();
            throw ex;
        }
        finally { VertexBuffer.unbind(); }
    }

    private static final class Batch implements RigidGeometryConsumer, AutoCloseable
    {
        private final MultiBufferSource.BufferSource source;
        private final RenderType vanilla;
        private final RenderType gpu;
        private final List<Draw> draws = new ArrayList<>(PARTS_PER_BATCH);
        private final float[] poses = new float[PARTS_PER_BATCH * 16];
        private final float[] normals = new float[PARTS_PER_BATCH * 16];
        private final float[] data = new float[PARTS_PER_BATCH * 16];
        private VertexConsumer fallback;
        private boolean drewGpu;

        private Batch(MultiBufferSource.BufferSource source, RenderType vanilla, RenderType gpu)
        {
            this.source = source;
            this.vanilla = vanilla;
            this.gpu = gpu;
        }

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                           float red, float green, float blue, float alpha)
        {
            if (fallback != null)
            {
                source.endBatch(vanilla);
                fallback = null;
            }
            // ModelRendererTurbo reuses its composed pose, so never retain that mutable object.
            PoseStack.Pose snapshot = new PoseStack().last();
            snapshot.pose().set(pose.pose());
            snapshot.normal().set(pose.normal());
            draws.add(new Draw(geometry, snapshot,
                light, overlay, red, green, blue, alpha));
            if (draws.size() == PARTS_PER_BATCH)
                flush();
        }

        private void flush()
        {
            if (draws.isEmpty()) return;
            try
            {
                Mesh mesh = failed ? null : mesh(draws.stream().map(Draw::geometry).toList());
                if (mesh == null)
                {
                    emitFallback();
                    return;
                }
                // Honor previously queued vanilla/glow geometry before immediate GPU draws.
                source.endBatch();
                fallback = null;
                for (int i = 0; i < draws.size(); i++)
                {
                    Draw draw = draws.get(i);
                    int offset = i * 16;
                    draw.pose.pose().get(poses, offset);
                    new Matrix4f().set(draw.pose.normal()).get(normals, offset);
                    data[offset] = draw.red;
                    data[offset + 1] = draw.green;
                    data[offset + 2] = draw.blue;
                    data[offset + 3] = draw.alpha;
                    data[offset + 4] = draw.light & 0xFFFF;
                    data[offset + 5] = draw.light >>> 16;
                    data[offset + 6] = draw.overlay & 0xFFFF;
                    data[offset + 7] = draw.overlay >>> 16;
                }
                ShaderInstance previousShader = RenderSystem.getShader();
                try
                {
                    gpu.setupRenderState();
                    shader.getUniform("PartPose").set(poses);
                    shader.getUniform("PartNormal").set(normals);
                    shader.getUniform("PartData").set(data);
                    mesh.buffer.bind();
                    mesh.buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
                    drewGpu = true;
                }
                finally
                {
                    VertexBuffer.unbind();
                    gpu.clearRenderState();
                    RenderSystem.setShader(() -> previousShader);
                }
            }
            catch (RuntimeException ex)
            {
                clear();
                failed = true;
                FlansMod.log.warn("GPU model rendering failed; using standard rendering until reload", ex);
                emitFallback();
            }
            finally { draws.clear(); }
        }

        private void emitFallback()
        {
            fallback = source.getBuffer(vanilla);
            for (Draw draw : draws)
                draw.geometry.draw(draw.pose, fallback, draw.light, draw.overlay, draw.red, draw.green, draw.blue, draw.alpha);
        }

        private VertexConsumer fallback()
        {
            flush();
            if (fallback == null) fallback = source.getBuffer(vanilla);
            return fallback;
        }

        @Override public void close()
        {
            flush();
            if (drewGpu && fallback != null) source.endBatch(vanilla);
        }

        @Override public void vertex(float x, float y, float z, float r, float g, float b, float a,
                                     float u, float v, int overlay, int light, float nx, float ny, float nz)
        { fallback().vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz); }
        @Override public VertexConsumer vertex(double x, double y, double z) { fallback().vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { fallback().color(r, g, b, a); return this; }
        @Override public VertexConsumer uv(float u, float v) { fallback().uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { fallback().overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { fallback().uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { fallback().normal(x, y, z); return this; }
        @Override public void endVertex() { fallback().endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { fallback().defaultColor(r, g, b, a); }
        @Override public void unsetDefaultColor() { fallback().unsetDefaultColor(); }
    }
}
