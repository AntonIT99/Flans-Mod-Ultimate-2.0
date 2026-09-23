package com.flansmodultimate.client.render.entity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Lazily generated far-distance driveable impostors.
 *
 * <p>Every atlas cell is captured independently and at most one cell is generated
 * per client tick. Until a requested view is ready, callers retain the exact model
 * renderer. The cache is render-thread-only and deliberately excludes entity state:
 * captures use the existing neutral preview renderer so damaged or animated entities
 * cannot contaminate other instances of the same model and paintjob.</p>
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DriveableImpostorCache
{
    private static final int PITCH_ANGLES = 3;
    private static final float[] CAPTURE_PITCH = {-30F, 0F, 30F};
    private static final float CAPTURE_MARGIN = 1.12F;
    private static final float HYSTERESIS = 1.2F;
    private static final float PREWARM_MULTIPLIER = 2F;
    private static final long CACHE_RETENTION_MILLIS = 2_000L;

    /** Small MRU list; reverse lookup avoids allocating a cache key for every rendered entity. */
    private static final List<Entry> entries = new ArrayList<>(16);
    // Geometry LOD must also work for damaged vehicles and when the atlas cache is full.
    private static final IdentityHashMap<ModelDriveable, IdentityHashMap<DriveableType, ModelBounds>> boundsCache = new IdentityHashMap<>();
    private static TextureTarget captureTarget;
    private static MultiBufferSource.BufferSource captureBuffer;
    private static final ImpostorAtlasBlitter atlasBlitter = new ImpostorAtlasBlitter();
    private static Settings settings;
    private static long lastCaptureTick = Long.MIN_VALUE;

    public record Result(boolean rendered, boolean usingImpostor, float projectedPixelDiameter, float modelRadius)
    {
        private static final Result EXACT_UNKNOWN = new Result(false, false, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        /** The exact model must be drawn, with no distance measurement to base a threshold on. */
        public static Result notRendered()
        {
            return EXACT_UNKNOWN;
        }

        private static Result exact(float projectedPixelDiameter, float radius)
        {
            if (projectedPixelDiameter == Float.POSITIVE_INFINITY)
                return EXACT_UNKNOWN;
            return new Result(false, false, projectedPixelDiameter, radius);
        }
    }

    /**
     * Prepare the requested view and render it if it is ready and below the LOD
     * threshold. Returning {@code rendered == false} always means the exact model
     * must be rendered by the caller.
     */
    public static Result renderOrPrepare(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float projectionPixels, double cameraDistance, Vec3 cameraOffset, float entityYaw, float entityPitch, float entityRoll, Quaternionf cameraOrientation, boolean allowImpostor, boolean wasUsingImpostor)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null || !config.enableDriveableLod || projectionPixels <= 0F || cameraDistance <= 0D)
            return Result.notRendered();

        ensureSettings(config);
        ModelBounds bounds = cachedBounds(model, type);
        if (!bounds.valid())
            return Result.notRendered();
        float projectedPixels = projectedDiameter(bounds.radius(), projectionPixels, cameraDistance);
        Result exact = Result.exact(projectedPixels, bounds.radius());
        if (!allowImpostor)
            return exact;

        boolean groundVehicle = type instanceof VehicleType && !type.isFloatOnWater();
        float sizeDistanceScale = DriveableLodPolicy.distanceScale(bounds.radius(), groundVehicle,
            (float)config.groundVehicleLodDistanceFactor);
        float minimumDistance = config.driveableImpostorMinimumDistance * sizeDistanceScale;
        float maximumDistance = config.driveableImpostorMaximumDistance > 0F
            ? config.driveableImpostorMaximumDistance * sizeDistanceScale : 0F;
        float impostorThreshold = DriveableLodPolicy.impostorThreshold((float)config.driveableImpostorPixelSize,
            cameraDistance, minimumDistance, maximumDistance, groundVehicle, settings.resolution());
        if ((impostorThreshold <= 0F && maximumDistance <= 0F)
            || cameraDistance < minimumDistance / PREWARM_MULTIPLIER)
            return exact;

        Entry entry = getOrCreate(model, type, sourceTexture, translucent, cull, red, green, blue, bounds);
        if (entry == null || entry.failed)
            return exact;
        Quaternionf entityRotation = new Quaternionf()
            .rotateY(entityYaw * Mth.DEG_TO_RAD)
            .rotateZ(entityPitch * Mth.DEG_TO_RAD)
            .rotateX(entityRoll * Mth.DEG_TO_RAD);
        ViewSelection view = selectView(cameraOffset, entityRotation, bounds.centerX(), bounds.centerY(),
            bounds.centerZ(), settings.yawAngles());
        int yawIndex = view.yawIndex();
        int pitchIndex = view.pitchIndex();
        int cellIndex = pitchIndex * settings.yawAngles() + yawIndex;
        boolean withinPrewarmRange = impostorThreshold > 0F
            && projectedPixels <= impostorThreshold * PREWARM_MULTIPLIER;
        if (maximumDistance > 0F)
            withinPrewarmRange |= cameraDistance >= maximumDistance / PREWARM_MULTIPLIER;
        if (!entry.captured[cellIndex] && withinPrewarmRange)
            captureOneCell(entry, yawIndex, pitchIndex, cellIndex);

        if (!entry.captured[cellIndex] || cameraDistance < minimumDistance
            || !DriveableLodPolicy.withinImageQuality(projectedPixels, settings.resolution(), wasUsingImpostor)
            || !shouldUseImpostor(projectedPixels, cameraDistance,
                impostorThreshold, maximumDistance, wasUsingImpostor))
            return exact;

        renderBillboard(entry, yawIndex, pitchIndex, entityRotation, cameraOrientation,
            poseStack, buffer, packedLight);
        return new Result(true, true, projectedPixels, bounds.radius());
    }

    static boolean shouldUseImpostor(float projectedPixels, double cameraDistance,
                                     float pixelThreshold, float maximumDistance,
                                     boolean wasUsingImpostor)
    {
        float activePixelThreshold = wasUsingImpostor ? pixelThreshold * HYSTERESIS : pixelThreshold;
        double activeDistanceThreshold = wasUsingImpostor
            ? maximumDistance / HYSTERESIS : maximumDistance;
        return (pixelThreshold > 0F && projectedPixels <= activePixelThreshold)
            || (maximumDistance > 0F && cameraDistance >= activeDistanceThreshold);
    }

    public static void clear()
    {
        if (RenderSystem.isOnRenderThreadOrInit())
            clearNow();
        else
            RenderSystem.recordRenderCall(DriveableImpostorCache::clearNow);
    }

    private static void ensureSettings(ModClientConfig config)
    {
        Settings requested = new Settings(DriveableLodPolicy.resolution(config.driveableImpostorResolution, config.driveableImpostorQualityMultiplier),
            DriveableLodPolicy.yawAngles(config.driveableImpostorYawAngles, config.driveableImpostorQualityMultiplier), config.driveableImpostorCacheEntries);
        if (requested.equals(settings))
            return;
        clearNow();
        settings = requested;
    }

    @Nullable
    private static Entry getOrCreate(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue, ModelBounds bounds)
    {
        long now = net.minecraft.Util.getMillis();
        for (int index = entries.size() - 1; index >= 0; index--)
        {
            Entry existing = entries.get(index);
            if (!existing.matches(model, type, sourceTexture, translucent, cull, red, green, blue))
                continue;
            existing.lastUsedMillis = now;
            if (index != entries.size() - 1)
            {
                entries.remove(index);
                entries.add(existing);
            }
            return existing;
        }

        if (entries.size() >= settings.maxEntries())
        {
            Entry eldest = entries.get(0);
            if (now - eldest.lastUsedMillis < CACHE_RETENTION_MILLIS)
                return null;
            entries.remove(0);
            release(eldest);
        }

        Entry created = new Entry(model, type, sourceTexture, translucent, cull,
            red, green, blue, bounds,
            settings.cellCount(), now);
        entries.add(created);
        return created;
    }

    private static ModelBounds measureBounds(ModelDriveable model, DriveableType type)
    {
        BoundsConsumer bounds = new BoundsConsumer();
        PoseStack poseStack = new PoseStack();
        renderNeutralModel(model, type, poseStack, bounds, LightTexture.FULL_BRIGHT, 1F, 1F, 1F, ModelCache.getRenderPasses(model));
        return bounds.toBounds();
    }

    private static ModelBounds cachedBounds(ModelDriveable model, DriveableType type)
    {
        IdentityHashMap<DriveableType, ModelBounds> byType = boundsCache.get(model);
        if (byType == null)
        {
            byType = new IdentityHashMap<>();
            boundsCache.put(model, byType);
        }
        ModelBounds bounds = byType.get(type);
        if (bounds == null)
        {
            bounds = measureBounds(model, type);
            byType.put(type, bounds);
        }
        return bounds;
    }

    private static void captureOneCell(Entry entry, int yawIndex, int pitchIndex, int cellIndex)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;
        long gameTick = minecraft.level.getGameTime();
        if (lastCaptureTick == gameTick)
            return;
        lastCaptureTick = gameTick;

        try
        {
            ensureAtlas(entry);
            ensureCaptureTarget();
            capture(entry, yawIndex, pitchIndex);
            entry.captured[cellIndex] = true;
        }
        catch (Exception | LinkageError e)
        {
            // A custom model can fail midway through a batch. Do not reuse that
            // incomplete buffer for the next atlas capture.
            captureBuffer = null;
            entry.failed = true;
            release(entry);
            FlansMod.log.warn("Disabling generated LOD impostor for model {} and texture {}: {}",
                entry.model.getClass().getName(), entry.sourceTexture, e.toString());
        }
    }

    private static void capture(Entry entry, int yawIndex, int pitchIndex)
    {
        int restoreDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int restoreReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] restoreViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, restoreViewport);
        Matrix4f restoreProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting restoreSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        NativeImage pixels = new NativeImage(settings.resolution(), settings.resolution(), true);
        boolean modelViewPushed = false;

        try
        {
            float halfExtent = entry.bounds.halfExtent();
            Matrix4f projection = new Matrix4f().setOrtho(-halfExtent, halfExtent,
                -halfExtent, halfExtent, -halfExtent * 4F, halfExtent * 4F);

            captureTarget.setClearColor(0F, 0F, 0F, 0F);
            captureTarget.clear(Minecraft.ON_OSX);
            captureTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            modelView.pushMatrix();
            modelViewPushed = true;
            modelView.identity();
            RenderSystem.applyModelViewMatrix();

            PoseStack capturePose = new PoseStack();
            capturePose.mulPose(Axis.XP.rotationDegrees(CAPTURE_PITCH[pitchIndex]));
            capturePose.mulPose(Axis.YP.rotationDegrees(360F * yawIndex / settings.yawAngles()));
            capturePose.translate(-entry.bounds.centerX(), -entry.bounds.centerY(), -entry.bounds.centerZ());

            try (ByteBufferBuilder captureBuilder = new ByteBufferBuilder(256))
            {
                MultiBufferSource.BufferSource captureBuffer = MultiBufferSource.immediate(captureBuilder);
                for (EnumRenderPass renderPass : ModelCache.getRenderPasses(entry.model))
                {
                    PoseStack layerPose = new PoseStack();
                    layerPose.mulPose(capturePose.last().pose());
                    renderNeutralModel(entry.model, entry.type, layerPose,
                        captureBuffer.getBuffer(renderPass.getRenderType(entry.sourceTexture,
                            entry.translucent, entry.cull)), LightTexture.FULL_BRIGHT,
                        entry.red, entry.green, entry.blue, List.of(renderPass));
                    captureBuffer.endBatch();
                }
            }

            atlasBlitter.copyFlipped(captureTarget.frameBufferId, entry.dynamicTexture.getId(),
                settings.resolution(), yawIndex, pitchIndex);
        }
        finally
        {
            if (modelViewPushed)
            {
                modelView.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
            RenderSystem.setProjectionMatrix(restoreProjection, restoreSorting);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, restoreReadFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, restoreDrawFramebuffer);
            RenderSystem.viewport(restoreViewport[0], restoreViewport[1], restoreViewport[2], restoreViewport[3]);
        }
    }

    private static void renderNeutralModel(ModelDriveable model, DriveableType type, PoseStack poseStack, VertexConsumer vertices, int packedLight, float red, float green, float blue, List<EnumRenderPass> passes)
    {
        poseStack.pushPose();
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);
        float scale = type.getModelScale();
        poseStack.scale(scale, scale, scale);
        for (EnumRenderPass renderPass : passes)
            model.render(type, poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY,
                red, green, blue, 1F, 1F, renderPass);
        poseStack.popPose();
    }

    private static void ensureAtlas(Entry entry)
    {
        if (entry.dynamicTexture != null)
            return;
        int width = settings.resolution() * settings.yawAngles();
        int height = settings.resolution() * PITCH_ANGLES;
        entry.dynamicTexture = new DynamicTexture(new NativeImage(width, height, true));
        entry.dynamicTexture.setFilter(true, false);
        entry.impostorTexture = Minecraft.getInstance().getTextureManager()
            .register("flans_driveable_impostor", entry.dynamicTexture);
    }

    private static void ensureCaptureTarget()
    {
        if (captureTarget != null && captureTarget.width == settings.resolution()
            && captureTarget.height == settings.resolution())
            return;
        if (captureTarget != null)
            captureTarget.destroyBuffers();
        captureTarget = new TextureTarget(settings.resolution(), settings.resolution(), true, Minecraft.ON_OSX);
    }

    private static void renderBillboard(Entry entry, int yawIndex, int pitchIndex, Quaternionf entityRotation, Quaternionf cameraOrientation, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int resolution = settings.resolution();
        float atlasWidth = (float)resolution * settings.yawAngles();
        float atlasHeight = (float)resolution * PITCH_ANGLES;
        float inset = 0.5F;
        float u0 = (yawIndex * resolution + inset) / atlasWidth;
        float u1 = ((yawIndex + 1) * resolution - inset) / atlasWidth;
        float v0 = (pitchIndex * resolution + inset) / atlasHeight;
        float v1 = ((pitchIndex + 1) * resolution - inset) / atlasHeight;

        Vector3f offset = new Vector3f(entry.bounds.centerX(), entry.bounds.centerY(), entry.bounds.centerZ())
            .rotate(entityRotation);
        float halfExtent = entry.bounds.halfExtent();
        poseStack.pushPose();
        poseStack.translate(offset.x(), offset.y(), offset.z());
        poseStack.mulPose(cameraOrientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(EnumRenderPass.DEFAULT
            .getRenderType(entry.impostorTexture, true, false));
        vertex(pose, vertices, -halfExtent, -halfExtent, u0, v1, packedLight);
        vertex(pose, vertices, halfExtent, -halfExtent, u1, v1, packedLight);
        vertex(pose, vertices, halfExtent, halfExtent, u1, v0, packedLight);
        vertex(pose, vertices, -halfExtent, halfExtent, u0, v0, packedLight);
        poseStack.popPose();
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float u, float v, int packedLight)
    {
        vertices.addVertex(pose.pose(), x, y, 0F).setColor(1F, 1F, 1F, 1F).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
            .setNormal(pose, 0F, 1F, 0F);
    }

    private static int yawIndex(float viewYaw, int yawAngles)
    {
        float normalized = Mth.positiveModulo(viewYaw, 360F);
        return Mth.floor(normalized / 360F * yawAngles + 0.5F) % yawAngles;
    }

    private static int pitchIndex(float viewPitch)
    {
        if (viewPitch < -15F)
            return 0;
        return viewPitch > 15F ? 2 : 1;
    }

    /**
     * Selects an atlas view from the camera's position in model space. Looking in
     * another direction without moving the camera must not make a different side
     * of the vehicle appear. The inverse entity rotation also handles pitched and
     * rolled aircraft without relying on the world render pose's Euler angles.
     */
    static ViewSelection selectView(Vec3 cameraOffset, Quaternionf entityRotation,
                                    float centerX, float centerY, float centerZ, int yawAngles)
    {
        Vector3f localCamera = new Vector3f((float)cameraOffset.x(), (float)cameraOffset.y(),
            (float)cameraOffset.z());
        new Quaternionf(entityRotation).conjugate().transform(localCamera);
        localCamera.sub(centerX, centerY, centerZ);
        if (!Float.isFinite(localCamera.lengthSquared()) || localCamera.lengthSquared() <= 1.0E-6F)
            return new ViewSelection(0, 1);

        localCamera.normalize();
        // Capturing rotates the model, so its yaw is opposite the model-space
        // azimuth from the model towards the camera.
        float captureYaw = -(float)Math.toDegrees(Math.atan2(localCamera.x(), localCamera.z()));
        float capturePitch = (float)Math.toDegrees(Math.asin(Mth.clamp(localCamera.y(), -1F, 1F)));
        return new ViewSelection(yawIndex(captureYaw, yawAngles), pitchIndex(capturePitch));
    }

    record ViewSelection(int yawIndex, int pitchIndex)
    {
    }

    private static float projectedDiameter(float radius, float projectionPixels, double cameraDistance)
    {
        double nearestDistance = Math.max(0.01D, cameraDistance - radius);
        return (float)(2D * radius * projectionPixels / nearestDistance);
    }

    private static void clearNow()
    {
        for (Entry entry : entries)
            release(entry);
        entries.clear();
        boundsCache.clear();
        captureBuffer = null;
        atlasBlitter.close();
        if (captureTarget != null)
        {
            captureTarget.destroyBuffers();
            captureTarget = null;
        }
        settings = null;
        lastCaptureTick = Long.MIN_VALUE;
    }

    private static void release(Entry entry)
    {
        if (entry.impostorTexture != null)
            Minecraft.getInstance().getTextureManager().release(entry.impostorTexture);
        entry.impostorTexture = null;
        entry.dynamicTexture = null;
    }

    private record Settings(int resolution, int yawAngles, int maxEntries)
    {
        private int cellCount()
        {
            return yawAngles * PITCH_ANGLES;
        }
    }

    private static final class Entry
    {
        private final ModelDriveable model;
        private final DriveableType type;
        private final ResourceLocation sourceTexture;
        private final boolean translucent;
        private final boolean cull;
        private final float red;
        private final float green;
        private final float blue;
        private final ModelBounds bounds;
        private final boolean[] captured;
        private DynamicTexture dynamicTexture;
        private ResourceLocation impostorTexture;
        private boolean failed;
        private long lastUsedMillis;

        private Entry(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture,
                      boolean translucent, boolean cull, float red, float green, float blue,
                      ModelBounds bounds, int cellCount, long lastUsedMillis)
        {
            this.model = model;
            this.type = type;
            this.sourceTexture = sourceTexture;
            this.translucent = translucent;
            this.cull = cull;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.bounds = bounds;
            this.captured = new boolean[cellCount];
            this.lastUsedMillis = lastUsedMillis;
        }

        private boolean matches(ModelDriveable otherModel, DriveableType otherType, ResourceLocation otherTexture,
                                boolean otherTranslucent, boolean otherCull,
                                float otherRed, float otherGreen, float otherBlue)
        {
            return model == otherModel && type == otherType && sourceTexture.equals(otherTexture)
                && translucent == otherTranslucent && cull == otherCull
                && Float.floatToIntBits(red) == Float.floatToIntBits(otherRed)
                && Float.floatToIntBits(green) == Float.floatToIntBits(otherGreen)
                && Float.floatToIntBits(blue) == Float.floatToIntBits(otherBlue);
        }
    }

    private record ModelBounds(float centerX, float centerY, float centerZ, float radius, float halfExtent,
                               boolean valid)
    {
        private static final ModelBounds INVALID = new ModelBounds(0F, 0F, 0F, 0F, 0F, false);
    }

    private static final class BoundsConsumer implements VertexConsumer
    {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        @Override
        @NotNull
        public VertexConsumer addVertex(float x, float y, float z)
        {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            return this;
        }

        @Override
        @NotNull
        public VertexConsumer setColor(int red, int green, int blue, int alpha)
        {
            return this;
        }

        @Override
        @NotNull
        public VertexConsumer setUv(float u, float v)
        {
            return this;
        }

        @Override
        @NotNull
        public VertexConsumer setUv1(int u, int v)
        {
            return this;
        }

        @Override
        @NotNull
        public VertexConsumer setUv2(int u, int v)
        {
            return this;
        }

        @Override
        @NotNull
        public VertexConsumer setNormal(float x, float y, float z)
        {
            return this;
        }

        private ModelBounds toBounds()
        {
            if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ))
                return ModelBounds.INVALID;
            float centerX = (minX + maxX) * 0.5F;
            float centerY = (minY + maxY) * 0.5F;
            float centerZ = (minZ + maxZ) * 0.5F;
            float extentX = (maxX - minX) * 0.5F;
            float extentY = (maxY - minY) * 0.5F;
            float extentZ = (maxZ - minZ) * 0.5F;
            float radius = Mth.sqrt(extentX * extentX + extentY * extentY + extentZ * extentZ);
            if (!Float.isFinite(radius) || radius <= 0.001F)
                return ModelBounds.INVALID;
            return new ModelBounds(centerX, centerY, centerZ, radius, radius * CAPTURE_MARGIN, true);
        }
    }
}
