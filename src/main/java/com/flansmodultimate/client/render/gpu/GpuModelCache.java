package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.VehicleThermalRenderer;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.mixin.BufferSourceAccessor;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Bounded, render-thread-only GPU batches of local rigid geometry with per-draw pose palettes. */
public final class GpuModelCache
{
    private static final Logger LOG = LogUtils.getLogger();
    // mat4 pose + mat3 normal + two metadata columns: at most 216 vec4 slots for 24
    // parts (including mat3 padding), plus <16 slots for vanilla uniforms. GL 3.2
    // guarantees 256 vertex-uniform vec4 slots. Leave room for driver padding.
    public static final int PARTS_PER_BATCH = 24;
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final long UPLOAD_BYTES_PER_TICK = 2L * 1024 * 1024;
    private static final int MAX_BATCHES = 2048;
    private static final MeshCache<Mesh> meshes = new MeshCache<>(MAX_BYTES, MAX_BATCHES);
    private static final List<Context> contexts = new ArrayList<>();
    private static int depth;
    private static final BufferBuilder builder = new BufferBuilder(256);
    private static final PoseStack.Pose IDENTITY = new PoseStack().last();
    private static ShaderInstance shader;
    private static boolean failed;
    private static Boolean incompatibleRenderer;
    private static Uniform poseUniform;
    private static Uniform normalUniform;
    private static Uniform dataUniform;
    private static final String[] SAMPLERS = {"Sampler0", "Sampler1", "Sampler2"};
    private static final int[] samplerTextures = {-1, -1, -1};
    private static long uploadTick = Long.MIN_VALUE;
    private static long uploadedBytes;

    private GpuModelCache() {}

    public static void registerShader(RegisterShadersEvent event)
    {
        shader = null;
        poseUniform = normalUniform = dataUniform = null;
        java.util.Arrays.fill(samplerTextures, -1);
        clear();
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "rigid_model"), DefaultVertexFormat.NEW_ENTITY), value -> {
                Uniform poses = value.getUniform("PartPose");
                Uniform normals = value.getUniform("PartNormal");
                Uniform data = value.getUniform("PartData");
                if (poses == null || normals == null || data == null)
                {
                    LOG.warn("GPU model shader lacks pose uniforms; using standard model rendering");
                    return;
                }
                poseUniform = poses;
                normalUniform = normals;
                dataUniform = data;
                shader = value;
            });
        }
        catch (IOException | RuntimeException ex)
        {
            LOG.warn("GPU model shader unavailable; using standard model rendering", ex);
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
        meshes.clear();
        discardUpload(builder);
        uploadedBytes = 0;
        uploadTick = Long.MIN_VALUE;
        failed = false;
    }

    private static boolean incompatibleRenderer()
    {
        if (incompatibleRenderer == null)
        {
            incompatibleRenderer = PlatformEnvironment.isModLoaded("oculus") || PlatformEnvironment.isModLoaded("iris");
            try
            {
                Class.forName("net.optifine.Config", false, GpuModelCache.class.getClassLoader());
                incompatibleRenderer = true;
            }
            catch (ClassNotFoundException ignored)
            {
                // Ignored
            }
        }
        return incompatibleRenderer;
    }

    /** Compatibility wrapper; hot call sites use begin/end to avoid capturing callbacks. */
    public static void render(MultiBufferSource source, EnumRenderPass pass, ResourceLocation texture, boolean translucent, boolean cull, boolean allowed, Consumer<VertexConsumer> render)
    {
        VertexConsumer consumer = begin(source, pass, texture, translucent, cull, allowed);
        try
        {
            render.accept(consumer);
        }
        finally
        {
            end(consumer);
        }
    }

    /** Only explicit normal model passes opt in. A context belongs to one nesting depth. */
    public static VertexConsumer begin(MultiBufferSource source, EnumRenderPass pass, ResourceLocation texture,
                                       boolean translucent, boolean cull, boolean allowed)
    {
        if (depth != 0) contexts.get(depth - 1).batch.suspend();
        RenderType vanilla = pass.getRenderType(texture, translucent, cull);
        ModClientConfig config = ModClientConfig.get();
        if (!allowed || config == null || !config.enableGpuModelCache || shader == null || failed
            || pass != EnumRenderPass.DEFAULT || translucent && !config.enableFastTranslucentRendering
            || source.getClass() != MultiBufferSource.BufferSource.class || VehicleThermalRenderer.isRenderingMask()
            || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS || incompatibleRenderer())
            return source.getBuffer(vanilla);
        if (depth == contexts.size()) contexts.add(new Context());
        RenderType gpu = CustomRenderType.gpuModel(texture, translucent, cull);
        Context context = contexts.get(depth);
        context.source = (MultiBufferSource.BufferSource)source;
        context.vanilla = vanilla;
        context.gpu = gpu;
        context.batch.begin(context);
        depth++;
        return context.batch;
    }

    public static void end(VertexConsumer consumer)
    {
        if (!(consumer instanceof RigidBatch batch)) return;
        if (depth == 0 || contexts.get(depth - 1).batch != batch)
            throw new IllegalStateException("GPU model scopes must close in reverse order");
        Context context = contexts.get(depth - 1);
        try { batch.end(); }
        finally
        {
            context.source = null;
            context.vanilla = context.gpu = null;
            context.previousShader = null;
            depth--;
            // Pathological recursion must not permanently grow the reusable pool.
            if (depth >= 16) contexts.remove(depth);
        }
    }

    private record Mesh(VertexBuffer buffer, long bytes) implements MeshCache.Resource
    {
        @Override
        public void close() { buffer.close(); }
    }

    private static Mesh mesh(GeometryKey key)
    {
        Mesh cached = meshes.get(key);
        if (cached != null)
            return cached;

        long vertices = 0;
        for (int i = 0; i < key.count; i++) vertices += key.geometries[i].vertexCount();
        long size = vertices * DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        long tick = Minecraft.getInstance().level == null ? System.nanoTime() / 50_000_000L : Minecraft.getInstance().level.getGameTime();

        if (tick != uploadTick)
        {
            uploadTick = tick;
            uploadedBytes = 0;
        }

        if (size == 0 || size > UPLOAD_BYTES_PER_TICK || uploadedBytes + size > UPLOAD_BYTES_PER_TICK)
            return null;

        meshes.reserve(size);
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try
        {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            for (int i = 0; i < key.count; i++)
                // UV1 is the palette index in the cached mesh; real overlay coordinates are uniforms.
                key.geometries[i].draw(IDENTITY, builder, 0, key.paletteIndices[i], 1F, 1F, 1F, 1F);
            buffer.bind();
            buffer.upload(builder.end());
            Mesh mesh = new Mesh(buffer, size);
            meshes.put(key, mesh);
            uploadedBytes += size;
            return mesh;
        }
        catch (RuntimeException ex)
        {
            buffer.close();
            discardUpload(builder);
            throw ex;
        }
        finally
        {
            VertexBuffer.unbind();
        }
    }

    static void discardUpload(BufferBuilder upload)
    {
        // discard() alone leaves BufferBuilder.building set, poisoning every upload after a failure.
        if (upload.building()) upload.end().release();
        upload.discard();
    }

    /** Equivalent vanilla quad draw uniforms, without twelve concatenated sampler names/boxed IDs per draw. */
    private static void prepareShader()
    {
        for (int i = 0; i < SAMPLERS.length; i++)
        {
            int texture = RenderSystem.getShaderTexture(i);
            if (samplerTextures[i] != texture)
            {
                shader.setSampler(SAMPLERS[i], texture);
                samplerTextures[i] = texture;
            }
        }
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.GLINT_ALPHA != null) shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null) shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        if (shader.SCREEN_SIZE != null)
        {
            var window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
    }

    private static final class Context implements RigidBatch.Backend, Supplier<ShaderInstance>
    {
        private final RigidBatch batch = new RigidBatch(PARTS_PER_BATCH, PARTS_PER_BATCH * 8,
            (int)(UPLOAD_BYTES_PER_TICK / DefaultVertexFormat.NEW_ENTITY.getVertexSize()));
        private MultiBufferSource.BufferSource source;
        private RenderType vanilla;
        private RenderType gpu;
        private ShaderInstance previousShader;

        @Override
        public boolean draw(RigidBatch batch, boolean flushPending)
        {
            Mesh mesh = failed || shader == null ? null : mesh(batch.key);
            if (mesh == null) return false;
            // Consecutive GPU batches have no intervening buffered vertices. Only
            // initial entry, fallback vertices and reentrant boundaries need this.
            if (flushPending || !(source instanceof BufferSourceAccessor buffers)
                || !buffers.flansmodultimate$startedBuffers().isEmpty()) source.endBatch();
            previousShader = RenderSystem.getShader();
            try
            {
                gpu.setupRenderState();
                poseUniform.set(batch.poses);
                normalUniform.set(batch.normals);
                dataUniform.set(batch.data);
                mesh.buffer.bind();
                prepareShader();
                try
                {
                    shader.apply();
                    mesh.buffer.draw();
                }
                finally { shader.clear(); }
                return true;
            }
            finally
            {
                VertexBuffer.unbind();
                gpu.clearRenderState();
                RenderSystem.setShader(this);
                previousShader = null;
            }
        }

        @Override
        public ShaderInstance get()
        {
            return previousShader;
        }

        @Override
        public VertexConsumer fallback()
        {
            return source.getBuffer(vanilla);
        }

        @Override
        public void flushFallback()
        {
            source.endBatch(vanilla);
        }

        @Override
        public void failed(RuntimeException exception)
        {
            clear();
            failed = true;
            LOG.warn("GPU model rendering failed; using standard rendering until reload", exception);
        }
    }
}
