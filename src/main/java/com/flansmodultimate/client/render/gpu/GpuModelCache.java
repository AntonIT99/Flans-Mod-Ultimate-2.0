package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.*;
import com.flansmodultimate.client.render.*;
import com.flansmodultimate.client.render.thermal.*;
import com.flansmodultimate.config.*;
import com.flansmodultimate.mixin.*;
import com.flansmodultimate.platform.render.*;
import com.flansmodultimate.util.*;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraftforge.client.event.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.function.*;

/** Bounded, render-thread-only GPU batches of local rigid geometry with per-draw pose palettes. */
public final class GpuModelCache
{
    /**
     * Palette entries per draw. The palette is a std140 uniform block of 144-byte entries (mat4 pose, mat3
     * normal, tint, light and overlay), so 96 fit the 16 KiB block size OpenGL 3.1 guarantees. Fancy track
     * links each take an entry, so this decides how many links share one draw.
     */
    public static final int PARTS_PER_BATCH = 96;
    /** Geometries per draw; most parts share their parent's entry, so batches hold several per entry. */
    public static final int GEOMETRIES_PER_BATCH = PARTS_PER_BATCH * 8;
    static final int PALETTE_ENTRY_BYTES = 144;
    /** Uniform buffer binding point of the palette, clear of those other renderers commonly use. */
    private static final int PALETTE_BINDING = 7;
    /** Each draw writes its palette at the next offset; the buffer is replaced only when the ring wraps. */
    private static final int PALETTE_RING_BYTES = 4 << 20;
    private static int paletteRing;
    private static int paletteRingOffset;
    private static int paletteBlockBytes;
    private static int uniformOffsetAlignment = 256;
    private static ByteBuffer paletteBytes;
    private static final long MIB = 1024L * 1024;
    /** Automatic budget bounds, and the budget when the driver does not report its video memory. */
    static final long MIN_AUTOMATIC_BYTES = 128 * MIB;
    static final long MAX_AUTOMATIC_BYTES = 512 * MIB;
    static final long UNKNOWN_MEMORY_BYTES = 128 * MIB;
    public static final int MIN_CONFIGURED_MEGABYTES = 16;
    public static final int MAX_CONFIGURED_MEGABYTES = 2048;
    /** Share of dedicated video memory the automatic budget may take. */
    private static final int VIDEO_MEMORY_SHARE = 16;
    private static final long UPLOAD_BYTES_PER_TICK = 2L * 1024 * 1024;
    /** Bounds the number of buffer objects; even small meshes average well above this per entry. */
    private static final long BYTES_PER_ENTRY = 32L * 1024L;
    private static final int MIN_ENTRIES = 2048;
    private static final MeshCache<Mesh> meshes = new MeshCache<>(MIN_AUTOMATIC_BYTES, MIN_ENTRIES);
    /** Dedicated video memory in KiB: unqueried, or 0 when the driver offers no memory extension. */
    private static long videoMemoryKiB = -1;
    private static boolean budgetStale = true;
    /** Automatic growth: an eighth of video memory, at most 1 GiB, keeping 512 MiB of video memory free. */
    private static final int GROWN_VIDEO_MEMORY_SHARE = 8;
    static final long MAX_GROWN_BYTES = 1024 * MIB;
    private static final long FREE_VIDEO_MEMORY_RESERVE_KIB = 512 * 1024;
    private static final long GROWTH_WINDOW_NANOS = 2_000_000_000L;
    private static final int GROWTH_EVICTIONS = 8;
    private static final int GROWTH_WINDOWS = 2;
    /** The automatic budget after growth, or 0 before any. */
    private static long grownBytes;
    private static long growthWindowStart;
    private static int thrashingWindows;
    private static final List<Context> contexts = new ArrayList<>();
    private static int depth;
    private static final BufferBuilder builder = new BufferBuilder(256);
    private static final PoseStack.Pose IDENTITY = new PoseStack().last();
    private static ShaderInstance shader;
    private static boolean failed;
    private static final String[] SAMPLERS = {"Sampler0", "Sampler1", "Sampler2"};
    private static final int[] samplerTextures = {-1, -1, -1};
    private static long uploadTick = Long.MIN_VALUE;
    private static long uploadedBytes;

    private GpuModelCache()
    {}

    public static void registerShader(RegisterShadersEvent event)
    {
        shader = null;
        java.util.Arrays.fill(samplerTextures, -1);
        clear();
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "rigid_model"), DefaultVertexFormat.NEW_ENTITY), value ->
            {
                int program = value.getId();
                int block = GL31C.glGetUniformBlockIndex(program, "PartPalette");
                int size = block == GL31C.GL_INVALID_INDEX ? 0 : GL31C.glGetActiveUniformBlocki(program, block, GL31C.GL_UNIFORM_BLOCK_DATA_SIZE);
                if (block == GL31C.GL_INVALID_INDEX || size <= 0 || size > GL11C.glGetInteger(GL31C.GL_MAX_UNIFORM_BLOCK_SIZE))
                {
                    FlansLog.log.warn("GPU model shader lacks a usable pose palette block; using standard model rendering");
                    return;
                }
                GL31C.glUniformBlockBinding(program, block, PALETTE_BINDING);
                paletteBlockBytes = size;
                shader = value;
            });
        }
        catch (IOException | RuntimeException ex)
        {
            FlansLog.log.warn("GPU model shader unavailable; using standard model rendering", ex);
        }
    }

    public static ShaderInstance shader()
    {
        return shader;
    }

    /** Frames rendered so far, for caches that act on time rather than on calls. */
    public static long frame()
    {
        return meshes.frame();
    }

    public static String status()
    {
        ModClientConfig config = ModClientConfig.get();
        String state = config == null || !config.enableGpuModelCache
            ? "disabled"
            : shader == null
                ? "shader unavailable"
                : failed
                    ? "failed until reload"
                    : ShaderPlatform.isOptiFineLoaded()
                        ? "OptiFine compatibility fallback"
                        : ShaderPlatform.isShaderPackInUse()
                            ? ShaderPlatform.shaderModName() + " shader pack fallback"
                            : Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS ? "Fabulous fallback" : "available for eligible passes";
        int configured = config == null ? 0 : config.gpuModelCacheMegabytes;
        String budget = configured > 0
            ? "configured"
            : videoMemoryKiB < 0
                ? "automatic"
                : videoMemoryKiB == 0
                    ? "automatic, video memory not reported"
                    : "automatic" + (grownBytes > 0 ? ", grown from " + budgetBytes(0, videoMemoryKiB) / MIB + " MiB" : "") + ", " + videoMemoryKiB / 1024 + " MiB video memory";
        return String.format(java.util.Locale.ROOT, "GPU cache %s; resident %d of %d meshes, %d of %d KiB (%.0f%%, %s)%s.", state, meshes.size(), meshes.maximumEntries(), meshes.bytes() / 1024,
            meshes.maximumBytes() / 1024, 100D * meshes.bytes() / meshes.maximumBytes(), budget, RenderDiagnostics.peakUsage(meshes.maximumBytes()));
    }

    /**
     * Bytes the cache may keep resident. It holds the meshes of every distinct model drawn in a frame, so it
     * must exceed that working set or meshes are evicted and re-uploaded every frame. Buffers are allocated
     * only as models are drawn, so the budget caps memory rather than reserving it. The automatic budget is
     * a sixteenth of dedicated video memory within 128-512 MiB.
     */
    static long budgetBytes(int configuredMegabytes, long videoMemoryKiB)
    {
        // Far above the largest single upload, which must always fit.
        if (configuredMegabytes > 0)
            return Math.max(MIN_CONFIGURED_MEGABYTES, configuredMegabytes) * MIB;
        if (videoMemoryKiB <= 0)
            return UNKNOWN_MEMORY_BYTES;
        return Math.max(MIN_AUTOMATIC_BYTES, Math.min(MAX_AUTOMATIC_BYTES, videoMemoryKiB * 1024 / VIDEO_MEMORY_SHARE));
    }

    static int entryLimit(long budgetBytes)
    {
        return (int) Math.max(MIN_ENTRIES, budgetBytes / BYTES_PER_ENTRY);
    }

    /** Re-read the configured budget on the render thread before the next upload, evicting down to it. */
    public static void budgetChanged()
    {
        budgetStale = true;
        RenderSystem.recordRenderCall(GpuModelCache::applyBudget);
    }

    private static void applyBudget()
    {
        if (!budgetStale)
            return;
        budgetStale = false;
        if (videoMemoryKiB < 0)
            videoMemoryKiB = queryVideoMemoryKiB();
        ModClientConfig config = ModClientConfig.get();
        int configured = config == null ? 0 : config.gpuModelCacheMegabytes;
        long budget = budgetBytes(configured, videoMemoryKiB);
        if (configured == 0)
            budget = Math.max(budget, grownBytes);
        meshes.limits(budget, entryLimit(budget));
    }

    /** The budget in MiB that Automatic currently uses on this machine, including growth. Render thread only. */
    public static long automaticBudgetMegabytes()
    {
        if (videoMemoryKiB < 0)
            videoMemoryKiB = queryVideoMemoryKiB();
        return Math.max(budgetBytes(0, videoMemoryKiB), grownBytes) / MIB;
    }

    /**
     * The highest budget automatic growth may reach: an eighth of video memory up to 1 GiB, never below the
     * starting budget. Video memory is shared with chunks, textures and shader packs; overcommitting it makes
     * the driver page buffers to system memory, which stutters worse than re-uploading meshes.
     */
    static long growthCeiling(long videoMemoryKiB)
    {
        return Math.max(budgetBytes(0, videoMemoryKiB), Math.min(MAX_GROWN_BYTES, videoMemoryKiB * 1024 / GROWN_VIDEO_MEMORY_SHARE));
    }

    /** One growth step of a quarter, within the ceiling and leaving the reserve of currently free video memory. */
    static long grownBudget(long current, long ceiling, long freeVideoMemoryKiB)
    {
        if (freeVideoMemoryKiB < 0)
            return current;
        long next = Math.min(ceiling, current + current / 4);
        next = Math.min(next, current + Math.max(0, freeVideoMemoryKiB - FREE_VIDEO_MEMORY_RESERVE_KIB) * 1024);
        return Math.max(current, next);
    }

    /**
     * Grows the automatic budget when meshes drawn in the last two frames keep being evicted: a full cache of
     * stale meshes is normal, but evicting the visible working set re-uploads it every frame. Growth needs
     * {@value #GROWTH_WINDOWS} consecutive windows of evictions, so a single burst does not trigger it. It never
     * shrinks; resource reload and disconnect return to the starting budget.
     */
    private static void growIfThrashing()
    {
        long now = System.nanoTime();
        if (growthWindowStart == 0 || now - growthWindowStart < GROWTH_WINDOW_NANOS)
        {
            if (growthWindowStart == 0)
                growthWindowStart = now;
            return;
        }
        growthWindowStart = now;
        thrashingWindows = meshes.takeWorkingSetEvictions() >= GROWTH_EVICTIONS ? thrashingWindows + 1 : 0;
        if (thrashingWindows < GROWTH_WINDOWS)
            return;
        thrashingWindows = 0;
        ModClientConfig config = ModClientConfig.get();
        if (config == null || config.gpuModelCacheMegabytes > 0 || videoMemoryKiB <= 0)
            return;
        long current = meshes.maximumBytes();
        long next = grownBudget(current, growthCeiling(videoMemoryKiB), queryFreeVideoMemoryKiB());
        if (next <= current)
            return;
        grownBytes = next;
        meshes.limits(next, entryLimit(next));
        FlansLog.log.info("GPU model cache budget grown from {} to {} MiB after repeated evictions of visible meshes", current / MIB, next / MIB);
    }

    /** Currently free video memory in KiB, or -1 when the driver does not report it. */
    private static long queryFreeVideoMemoryKiB()
    {
        try
        {
            GLCapabilities capabilities = GL.getCapabilities();
            int[] values = new int[4];
            if (capabilities.GL_NVX_gpu_memory_info)
                GL11C.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, values);
            else if (capabilities.GL_ATI_meminfo)
                GL11C.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, values);
            else
                return -1;
            return Math.max(0, values[0]);
        }
        catch (RuntimeException | LinkageError ex)
        {
            FlansLog.log.debug("Free video memory unavailable for GPU model cache growth", ex);
            return -1;
        }
    }

    /** Video memory in MiB the automatic budget is based on; 0 when the driver does not report it. Render thread only. */
    public static long reportedVideoMemoryMegabytes()
    {
        if (videoMemoryKiB < 0)
            videoMemoryKiB = queryVideoMemoryKiB();
        return videoMemoryKiB / 1024;
    }

    /** Dedicated (NVIDIA) or currently free buffer (AMD, Mesa) video memory in KiB; 0 when unreported. */
    private static long queryVideoMemoryKiB()
    {
        try
        {
            GLCapabilities capabilities = GL.getCapabilities();
            int[] values = new int[4];
            if (capabilities.GL_NVX_gpu_memory_info)
                GL11C.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX, values);
            else if (capabilities.GL_ATI_meminfo)
                GL11C.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, values);
            return Math.max(0, values[0]);
        }
        catch (RuntimeException | LinkageError ex)
        {
            FlansLog.log.debug("Video memory size unavailable for the GPU model cache budget", ex);
            return 0;
        }
    }

    /** Once per rendered frame, before the world: marks meshes in use and counts recorded frames. */
    public static void beginFrame()
    {
        meshes.nextFrame();
        RenderDiagnostics.countFrame();
        growIfThrashing();
    }

    public static void clear()
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(GpuModelCache::clear);
            return;
        }
        com.flansmod.client.tmt.ModelRendererTurbo.clearRenderScratch();
        meshes.clear();
        meshes.takeWorkingSetEvictions();
        grownBytes = 0;
        thrashingWindows = 0;
        budgetStale = true;
        discardUpload(builder);
        uploadedBytes = 0;
        uploadTick = Long.MIN_VALUE;
        failed = false;
    }

    /**
     * Writes the batch's palette entries in the shader's std140 layout: per entry the pose's four columns,
     * the normal matrix's three columns each padded to four floats, the tint, then light and overlay.
     */
    static void writePartPalette(RigidBatch batch, ByteBuffer out)
    {
        // The batch keeps its entries in this layout already: one bulk copy instead of a write per float.
        int floats = batch.paletteCount() * RigidBatch.PALETTE_FLOATS;
        out.clear();
        out.asFloatBuffer().put(batch.palette, 0, floats);
        out.limit(floats * Float.BYTES);
    }

    /** Writes the batch's palette at the ring's next offset and binds the block's range there. */
    private static void bindPartPalette(RigidBatch batch)
    {
        if (paletteRing == 0)
        {
            paletteRing = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, paletteRing);
            GL15C.glBufferData(GL31C.GL_UNIFORM_BUFFER, PALETTE_RING_BYTES, GL15C.GL_STREAM_DRAW);
            uniformOffsetAlignment = Math.max(1, GL11C.glGetInteger(GL31C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));
            paletteBytes = MemoryUtil.memAlloc(PARTS_PER_BATCH * PALETTE_ENTRY_BYTES).order(java.nio.ByteOrder.nativeOrder());
            paletteRingOffset = 0;
        }
        else
            GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, paletteRing);
        if (paletteRingOffset + paletteBlockBytes > PALETTE_RING_BYTES)
        {
            // Wrapped: a fresh data store, so no write waits for draws still reading the old one.
            GL15C.glBufferData(GL31C.GL_UNIFORM_BUFFER, PALETTE_RING_BYTES, GL15C.GL_STREAM_DRAW);
            paletteRingOffset = 0;
        }
        writePartPalette(batch, paletteBytes);
        GL15C.glBufferSubData(GL31C.GL_UNIFORM_BUFFER, paletteRingOffset, paletteBytes);
        // The bound range covers the whole block; entries past the batch's are left over and never read.
        GL30C.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, PALETTE_BINDING, paletteRing, paletteRingOffset, paletteBlockBytes);
        GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, 0);
        int used = Math.max(paletteBytes.limit(), 1);
        paletteRingOffset += (used + uniformOffsetAlignment - 1) / uniformOffsetAlignment * uniformOffsetAlignment;
    }

    /**
     * A shader pack replaces vanilla programs and vertex formats and masks writes from shaders it does not know
     * while it renders the world, including its shadow pass. Installed without an active pack, Oculus and Iris keep
     * the vanilla pipeline, so the cache stays available.
     */
    private static boolean incompatibleRenderer()
    {
        return ShaderPlatform.isOptiFineLoaded() || ShaderPlatform.isShaderPackInUse();
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
    public static VertexConsumer begin(MultiBufferSource source, EnumRenderPass pass, ResourceLocation texture, boolean translucent, boolean cull, boolean allowed)
    {
        if (depth != 0)
            contexts.get(depth - 1).batch.suspend();
        RenderType vanilla = pass.getRenderType(texture, translucent, cull);
        ModClientConfig config = ModClientConfig.get();
        if (!allowed || config == null || !config.enableGpuModelCache || shader == null || failed || pass != EnumRenderPass.DEFAULT || translucent && !config.enableFastTranslucentRendering
            || source.getClass() != MultiBufferSource.BufferSource.class || VehicleThermalRenderer.isRenderingMask() || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS
            || incompatibleRenderer())
        {
            if (RenderDiagnostics.enabled)
                RenderDiagnostics.excludedScopes++;
            return source.getBuffer(vanilla);
        }
        if (depth == contexts.size())
            contexts.add(new Context());
        RenderType gpu = CustomRenderType.gpuModel(texture, translucent, cull);
        Context context = contexts.get(depth);
        context.source = (MultiBufferSource.BufferSource) source;
        context.vanilla = vanilla;
        context.gpu = gpu;
        context.batch.begin(context);
        depth++;
        return context.batch;
    }

    public static void end(VertexConsumer consumer)
    {
        if (!(consumer instanceof RigidBatch batch))
            return;
        if (depth == 0 || contexts.get(depth - 1).batch != batch)
            throw new IllegalStateException("GPU model scopes must close in reverse order");
        Context context = contexts.get(depth - 1);
        try
        {
            batch.end();
        }
        finally
        {
            context.source = null;
            context.vanilla = context.gpu = null;
            context.previousShader = null;
            depth--;
            // Pathological recursion must not permanently grow the reusable pool.
            if (depth >= 16)
                contexts.remove(depth);
        }
    }

    private record Mesh(VertexBuffer buffer, long bytes) implements MeshCache.Resource
    {
        @Override
        public void close()
        {
            buffer.close();
        }
    }

    private static Mesh mesh(GeometryKey key)
    {
        Mesh cached = meshes.get(key);
        if (cached != null)
            return cached;

        long vertices = 0;
        for (int i = 0; i < key.count; i++)
            vertices += key.geometries[i].vertexCount();
        long size = vertices * DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        long tick = Minecraft.getInstance().level == null ? System.nanoTime() / 50_000_000L : Minecraft.getInstance().level.getGameTime();

        if (tick != uploadTick)
        {
            uploadTick = tick;
            uploadedBytes = 0;
        }

        if (size == 0 || size > UPLOAD_BYTES_PER_TICK || uploadedBytes + size > UPLOAD_BYTES_PER_TICK)
        {
            if (RenderDiagnostics.enabled)
                RenderDiagnostics.throttled++;
            return null;
        }

        applyBudget();
        meshes.reserve(size);
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try
        {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            boolean batched = EntityVertexBatch.begin(builder);
            try
            {
                for (int i = 0; i < key.count; i++)
                    // UV1 is the palette index in the cached mesh; real overlay coordinates are uniforms.
                    key.geometries[i].draw(IDENTITY, builder, 0, key.paletteIndices[i], 1F, 1F, 1F, 1F);
            }
            finally
            {
                if (batched)
                    EntityVertexBatch.end();
            }
            buffer.bind();
            buffer.upload(builder.end());
            Mesh mesh = new Mesh(buffer, size);
            meshes.put(key, mesh);
            uploadedBytes += size;
            if (RenderDiagnostics.enabled)
            {
                RenderDiagnostics.uploads++;
                RenderDiagnostics.uploadBytes += size;
            }
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
        if (upload.building())
            upload.end().release();
        upload.discard();
    }

    /** Equivalent vanilla quad draw uniforms, without twelve concatenated sampler names/boxed IDs per draw. */
    private static void prepareShader(ShaderInstance shader, int[] samplerTextures)
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
        if (shader.MODEL_VIEW_MATRIX != null)
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (shader.PROJECTION_MATRIX != null)
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null)
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        if (shader.COLOR_MODULATOR != null)
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.GLINT_ALPHA != null)
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        if (shader.FOG_START != null)
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null)
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null)
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null)
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null)
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null)
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        if (shader.SCREEN_SIZE != null)
        {
            var window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
    }

    private static final class Context implements RigidBatch.Backend, Supplier<ShaderInstance>
    {
        private final RigidBatch batch = new RigidBatch(PARTS_PER_BATCH, GEOMETRIES_PER_BATCH, (int) (UPLOAD_BYTES_PER_TICK / DefaultVertexFormat.NEW_ENTITY.getVertexSize()));
        private MultiBufferSource.BufferSource source;
        private RenderType vanilla;
        private RenderType gpu;
        private ShaderInstance previousShader;
        /** Render state stays set up between consecutive GPU draws of this scope. */
        private boolean open;

        @Override
        public boolean draw(RigidBatch batch, boolean flushPending)
        {
            long started = RenderDiagnostics.startTimer();
            try
            {
                return submit(batch, flushPending);
            }
            finally
            {
                RenderDiagnostics.countDrawTime(started);
            }
        }

        /** Mesh lookup, any upload, buffered-vertex flush, state setup and the GL draw itself. */
        private boolean submit(RigidBatch batch, boolean flushPending)
        {
            long lookup = RenderDiagnostics.startTimer();
            Mesh mesh = failed || shader == null ? null : mesh(batch.key);
            RenderDiagnostics.countLookupTime(lookup);
            if (mesh == null)
                return false;
            // Consecutive GPU batches have no intervening buffered vertices. Only
            // initial entry, fallback vertices and reentrant boundaries need this.
            if (flushPending || !(source instanceof BufferSourceAccessor buffers) || !buffers.flansmodultimateStartedBuffers().isEmpty())
            {
                endDraws();
                source.endBatch();
            }
            // Consecutive draws of one model share their render state. Any other draw in between sets
            // its own shader, so a changed current shader means the state must be set up again.
            if (!open || RenderSystem.getShader() != shader)
                openDraws();
            try
            {
                // The shader was applied with the state; a draw only binds its palette range and mesh.
                long palette = RenderDiagnostics.startTimer();
                bindPartPalette(batch);
                long draw = RenderDiagnostics.countPaletteTime(palette);
                mesh.buffer.bind();
                batch.ranges.draw(mesh.buffer);
                RenderDiagnostics.countGlDrawTime(draw);
                if (RenderDiagnostics.enabled)
                {
                    RenderDiagnostics.draws++;
                    RenderDiagnostics.immediateDraws++;
                    RenderDiagnostics.paletteEntries += batch.paletteCount();
                    RenderDiagnostics.ranges += batch.ranges.count;
                    RenderDiagnostics.vertices += batch.ranges.visibleVertices;
                }
                return true;
            }
            catch (RuntimeException exception)
            {
                endDraws();
                throw exception;
            }
        }

        /** Render type, samplers, matrices, fog and lights: constant for the draws of one scope. */
        private void openDraws()
        {
            if (!open)
                previousShader = RenderSystem.getShader();
            open = true;
            gpu.setupRenderState();
            prepareShader(shader, samplerTextures);
            shader.apply();
            if (RenderDiagnostics.enabled)
                RenderDiagnostics.stateSetups++;
        }

        @Override
        public void endDraws()
        {
            if (!open)
                return;
            open = false;
            try
            {
                GL30C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, PALETTE_BINDING, 0);
                shader.clear();
                VertexBuffer.unbind();
                gpu.clearRenderState();
            }
            finally
            {
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
            // Switching buffers may draw the previous one at once.
            endDraws();
            return source.getBuffer(vanilla);
        }

        @Override
        public void flushFallback()
        {
            endDraws();
            source.endBatch(vanilla);
        }

        @Override
        public void failed(RuntimeException exception)
        {
            clear();
            failed = true;
            FlansLog.log.warn("GPU model rendering failed; using standard rendering until reload", exception);
        }
    }
}
