package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.PositionCollector;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Lazily generated far-distance driveable impostors.
 *
 * <p>
 * Every atlas cell is captured independently and at most one cell is generated
 * per client tick. Rendering only requests a cell; it is captured at the start of the
 * next frame, outside level rendering, so neither fog, the current view's lighting nor
 * a shader mod's world and shadow passes can reach the atlas. Until a requested view is
 * ready, callers retain the exact model renderer. The cache is render-thread-only and
 * deliberately excludes entity state:
 * captures use the existing neutral preview renderer so damaged or animated entities
 * cannot contaminate other instances of the same model and paintjob.
 * </p>
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DriveableImpostorCache
{
    private static final int PITCH_ANGLES = 9;
    private static final float PITCH_STEP = 180F / (PITCH_ANGLES - 1);
    // Keep expanded angular coverage cheap until those views are actually visited.
    private static final int PAGE_COLUMNS = 4;
    private static final int PAGE_ROWS = 3;
    private static final float CAPTURE_MARGIN = 1.12F;
    private static final float HYSTERESIS = 1.2F;
    private static final float PREWARM_MULTIPLIER = 2F;
    private static final long CACHE_RETENTION_MILLIS = 2_000L;
    /** Vanilla's level light directions, before the view rotation is applied. */
    private static final Vector3f LEVEL_LIGHT_0 = new Vector3f(0.2F, 1F, -0.7F).normalize();
    private static final Vector3f LEVEL_LIGHT_1 = new Vector3f(-0.2F, 1F, 0.7F).normalize();

    /** Small MRU list; reverse lookup avoids allocating a cache key for every rendered entity. */
    private static final List<Entry> entries = new ArrayList<>(16);
    // Geometry LOD must also work for damaged vehicles and when the atlas cache is full.
    private static final IdentityHashMap<ModelDriveable, IdentityHashMap<DriveableType, ModelBounds>> boundsCache = new IdentityHashMap<>();
    private static TextureTarget captureTarget;
    private static MultiBufferSource.BufferSource captureBuffer;
    private static final ImpostorAtlasBlitter atlasBlitter = new ImpostorAtlasBlitter();
    private static Settings settings;
    private static long lastCaptureTick = Long.MIN_VALUE;
    /** The first cell requested since the last capture, captured before the next frame. */
    @Nullable
    private static Entry pendingEntry;
    private static int pendingYawIndex;
    private static int pendingPitchIndex;

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
    public static Result renderOrPrepare(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue,
        PoseStack poseStack, MultiBufferSource buffer, int packedLight, float projectionPixels, double cameraDistance, Vec3 cameraOffset, float entityYaw, float entityPitch, float entityRoll,
        boolean allowImpostor, boolean wasUsingImpostor, float drawTint)
    {
        return renderOrPrepare(model, type, sourceTexture, translucent, cull, red, green, blue, poseStack, buffer, packedLight, projectionPixels, cameraDistance, cameraOffset, entityYaw, entityPitch,
            entityRoll, null, allowImpostor, wasUsingImpostor, 1F, OverlayTexture.NO_OVERLAY, drawTint, drawTint, drawTint, 1F);
    }

    /** Neutral-model atlas shared with externally posed instances; tint and damage overlays stay per draw. */
    public static Result renderPosedOrPrepare(ModelDriveable model, DriveableType type, ResourceLocation texture, boolean translucent, boolean cull, float typeRed, float typeGreen, float typeBlue,
        PoseStack pose, MultiBufferSource buffers, int light, int overlay, float red, float green, float blue, float alpha, float parentScale, float projectionPixels, double distance,
        Vec3 localCameraOffset, Quaternionf rotation, boolean allowImpostor, boolean wasUsingImpostor)
    {
        return renderOrPrepare(model, type, texture, translucent, cull, typeRed, typeGreen, typeBlue, pose, buffers, light, projectionPixels, distance, localCameraOffset, 0F, 0F, 0F, rotation,
            allowImpostor, wasUsingImpostor, parentScale, overlay, red, green, blue, alpha);
    }

    private static Result renderOrPrepare(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue,
        PoseStack poseStack, MultiBufferSource buffer, int packedLight, float projectionPixels, double cameraDistance, Vec3 cameraOffset, float entityYaw, float entityPitch, float entityRoll,
        @Nullable Quaternionf suppliedRotation, boolean allowImpostor, boolean wasUsingImpostor, float parentScale, int overlay, float drawRed, float drawGreen, float drawBlue, float alpha)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null || !config.enableDriveableLod || projectionPixels <= 0F || cameraDistance <= 0D)
            return Result.notRendered();

        ensureSettings(config);
        ModelBounds bounds = cachedBounds(model, type);
        if (!bounds.valid())
            return Result.notRendered();
        float radius = bounds.radius() * parentScale;
        float projectedPixels = projectedDiameter(radius, projectionPixels, cameraDistance);
        Result exact = Result.exact(projectedPixels, radius);
        if (!allowImpostor)
            return exact;

        boolean groundVehicle = type instanceof VehicleType && !type.isFloatOnWater();
        float sizeDistanceScale = DriveableLodPolicy.distanceScale(radius, groundVehicle, (float) config.groundVehicleLodDistanceFactor);
        float minimumDistance = config.driveableImpostorMinimumDistance * sizeDistanceScale;
        float maximumDistance = config.driveableImpostorMaximumDistance > 0F ? config.driveableImpostorMaximumDistance * sizeDistanceScale : 0F;
        float impostorThreshold = DriveableLodPolicy.impostorThreshold((float) config.driveableImpostorPixelSize, cameraDistance, minimumDistance, maximumDistance, groundVehicle,
            settings.resolution());
        if ((impostorThreshold <= 0F && maximumDistance <= 0F) || cameraDistance < minimumDistance / PREWARM_MULTIPLIER)
            return exact;

        Entry entry = getOrCreate(model, type, sourceTexture, translucent, cull, red, green, blue, bounds);
        if (entry == null || entry.failed)
            return exact;
        Quaternionf entityRotation = suppliedRotation != null
            ? suppliedRotation
            : new Quaternionf().rotateY(entityYaw * Mth.DEG_TO_RAD).rotateZ(entityPitch * Mth.DEG_TO_RAD).rotateX(entityRoll * Mth.DEG_TO_RAD);
        ViewSelection view = selectView(cameraOffset, entityRotation, bounds.centerX(), bounds.centerY(), bounds.centerZ(), settings.yawAngles());
        int yawIndex = view.yawIndex();
        int pitchIndex = view.pitchIndex();
        int cellIndex = pitchIndex * settings.yawAngles() + yawIndex;
        boolean withinPrewarmRange = impostorThreshold > 0F && projectedPixels <= impostorThreshold * PREWARM_MULTIPLIER;
        if (maximumDistance > 0F)
            withinPrewarmRange |= cameraDistance >= maximumDistance / PREWARM_MULTIPLIER;
        if (!entry.captured[cellIndex] && withinPrewarmRange && pendingEntry == null)
        {
            pendingEntry = entry;
            pendingYawIndex = yawIndex;
            pendingPitchIndex = pitchIndex;
        }

        if (!entry.captured[cellIndex] || cameraDistance < minimumDistance || !DriveableLodPolicy.withinImageQuality(projectedPixels, settings.resolution(), wasUsingImpostor)
            || !shouldUseImpostor(projectedPixels, cameraDistance, impostorThreshold, maximumDistance, wasUsingImpostor))
            return exact;

        renderBillboard(entry, view, entityRotation, poseStack, buffer, packedLight, overlay, drawRed, drawGreen, drawBlue, alpha);
        return new Result(true, true, projectedPixels, radius);
    }

    static boolean shouldUseImpostor(float projectedPixels, double cameraDistance, float pixelThreshold, float maximumDistance, boolean wasUsingImpostor)
    {
        float activePixelThreshold = wasUsingImpostor ? pixelThreshold * HYSTERESIS : pixelThreshold;
        double activeDistanceThreshold = wasUsingImpostor ? maximumDistance / HYSTERESIS : maximumDistance;
        return (pixelThreshold > 0F && projectedPixels <= activePixelThreshold) || (maximumDistance > 0F && cameraDistance >= activeDistanceThreshold);
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
    private static Entry getOrCreate(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue,
        ModelBounds bounds)
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

        Entry created = new Entry(model, type, sourceTexture, translucent, cull, red, green, blue, bounds, settings.cellCount(), now);
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
        return boundsCache.computeIfAbsent(model, ignored -> new IdentityHashMap<>()).computeIfAbsent(type, ignored -> measureBounds(model, type));
    }

    /** Conservative radius about the model origin, including an off-center hull; needs no atlas entry. */
    public static float modelRadius(ModelDriveable model, DriveableType type)
    {
        ModelBounds bounds = cachedBounds(model, type);
        if (!bounds.valid())
            return Float.POSITIVE_INFINITY;
        return bounds.radius() + (float) Math.sqrt(bounds.centerX() * bounds.centerX() + bounds.centerY() * bounds.centerY() + bounds.centerZ() * bounds.centerZ());
    }

    /**
     * The atlas a distant shape of this model and skin is drawn from, after asking for the view the camera has
     * of it. Null while no view of it is captured yet, while the cache holds no room for it, or when the model
     * cannot be pictured; the shape is then drawn another way. Unlike {@link #renderOrPrepare}, this does not
     * depend on the level of detail settings: a distant shape is always a picture once one is ready. The atlas is
     * shared with driveables of the same type and skin drawn as entities.
     *
     * @param cameraOffset
     *            the camera position minus the shape's origin
     * @param rotation
     *            the model's rotation about its origin, built as {@link #renderOrPrepare} builds it
     */
    @Nullable
    public static DistantAtlas requestDistant(ModelDriveable model, DriveableType type, ResourceLocation texture, Vec3 cameraOffset, Quaternionf rotation)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config == null)
            return null;
        ensureSettings(config);
        ModelBounds bounds = cachedBounds(model, type);
        if (!bounds.valid())
            return null;
        // As the driveable renderer draws the type, so that both find the same atlas
        int colour = type.getColour();
        Entry entry = getOrCreate(model, type, texture, config.useTranslucentRendering(type), config.useCullingRendering(type), (colour >> 16 & 255) / 255F, (colour >> 8 & 255) / 255F,
            (colour & 255) / 255F, bounds);
        if (entry == null || entry.failed)
            return null;
        DistantAtlas atlas = new DistantAtlas(entry);
        return distantView(atlas, cameraOffset, rotation) != null ? atlas : null;
    }

    /**
     * Draws a distant shape as a billboard of its model, in a pose whose origin is the camera. The billboard is
     * pulled {@code pull} blocks towards the camera and shrunk so that it keeps its size on screen, which keeps
     * the ground a vehicle stands on from cutting into its picture. False once nothing captured can be drawn.
     *
     * @param offset
     *            the shape's origin minus the camera position
     */
    public static boolean renderDistantBillboard(DistantAtlas atlas, Vec3 offset, Quaternionf rotation, float pull, PoseStack poseStack, MultiBufferSource buffers, int light)
    {
        ViewSelection view = distantView(atlas, offset.reverse(), rotation);
        if (view == null)
            return false;
        Entry entry = atlas.entry;
        AtlasPage page = entry.pages[atlasCell(view.yawIndex(), view.pitchIndex(), settings.yawAngles()).pageIndex()];
        if (page == null || page.location == null)
            return false;

        Vector3f center = new Vector3f(entry.bounds.centerX(), entry.bounds.centerY(), entry.bounds.centerZ()).rotate(rotation)
            .add((float) offset.x, (float) offset.y, (float) offset.z);
        float scale = pullScale(center.length(), pull);
        poseStack.pushPose();
        // Scaled about the camera, which leaves the picture where it was on screen
        poseStack.scale(scale, scale, scale);
        poseStack.translate(offset.x, offset.y, offset.z);
        renderBillboard(entry, view, rotation, poseStack, buffers, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        poseStack.popPose();
        return true;
    }

    /**
     * Puts the billboard of a distant shape into {@code out}, relative to the camera and pulled towards it as
     * {@link #renderDistantBillboard} pulls it. False once nothing captured can be drawn.
     *
     * @param offset
     *            the shape's origin minus the camera position
     */
    public static boolean fillDistantBillboard(DistantAtlas atlas, Vec3 offset, Quaternionf rotation, float pull, DistantBillboard out)
    {
        ViewSelection view = distantView(atlas, offset.reverse(), rotation);
        if (view == null)
            return false;
        Entry entry = atlas.entry;
        AtlasCell cell = atlasCell(view.yawIndex(), view.pitchIndex(), settings.yawAngles());
        AtlasPage page = entry.pages[cell.pageIndex()];
        if (page == null || page.texture == null)
            return false;

        float[] uvs = cellUvs(cell);
        out.texture = page.texture.getId();
        out.atlasWidth = (float) settings.resolution() * PAGE_COLUMNS;
        out.atlasHeight = (float) settings.resolution() * PAGE_ROWS;
        out.u0 = uvs[0];
        out.u1 = uvs[1];
        out.v0 = uvs[2];
        out.v1 = uvs[3];
        Quaternionf facing = view.billboardRotation(rotation);
        float halfExtent = entry.bounds.halfExtent();
        out.center.set(entry.bounds.centerX(), entry.bounds.centerY(), entry.bounds.centerZ()).rotate(rotation).add((float) offset.x, (float) offset.y, (float) offset.z);
        out.right.set(halfExtent, 0F, 0F).rotate(facing);
        out.up.set(0F, halfExtent, 0F).rotate(facing);
        float scale = pullScale(out.center.length(), pull);
        out.center.mul(scale);
        out.right.mul(scale);
        out.up.mul(scale);
        return true;
    }

    /** Radius in blocks of the model the atlas pictures, about the model's centre. */
    public static float distantRadius(DistantAtlas atlas)
    {
        return atlas.entry.bounds.radius();
    }

    /**
     * The view of a distant shape to draw: the camera's own once captured, else the nearest captured one, still
     * facing the camera. Asks for the camera's own when it is missing. Null once the atlas is gone or holds none.
     */
    @Nullable
    private static ViewSelection distantView(DistantAtlas atlas, Vec3 cameraOffset, Quaternionf rotation)
    {
        Entry entry = atlas.entry;
        // A clear or an eviction releases the entry's pages; a settings change also replaces the cell layout
        if (settings == null || entry.failed || !entries.contains(entry))
            return null;
        int yawAngles = settings.yawAngles();
        ViewSelection view = selectView(cameraOffset, rotation, entry.bounds.centerX(), entry.bounds.centerY(), entry.bounds.centerZ(), yawAngles);
        if (!entry.captured[view.pitchIndex() * yawAngles + view.yawIndex()] && pendingEntry == null)
        {
            pendingEntry = entry;
            pendingYawIndex = view.yawIndex();
            pendingPitchIndex = view.pitchIndex();
        }
        int cell = nearestCaptured(entry.captured, yawAngles, view.yawIndex(), view.pitchIndex());
        if (cell < 0)
            return null;
        return new ViewSelection(cell % yawAngles, cell / yawAngles, view.viewYaw(), view.viewPitch());
    }

    /**
     * The captured cell, numbered {@code pitchIndex * yawAngles + yawIndex}, whose view is nearest the given one,
     * or -1 when none is captured. A yaw step turns the view by less towards the poles, so it counts for less there.
     */
    static int nearestCaptured(boolean[] captured, int yawAngles, int yawIndex, int pitchIndex)
    {
        int wanted = pitchIndex * yawAngles + yawIndex;
        if (captured[wanted])
            return wanted;
        int best = -1;
        float bestCost = Float.POSITIVE_INFINITY;
        float yawStep = 360F / yawAngles;
        for (int pitch = 0; pitch < PITCH_ANGLES; pitch++)
        {
            float yawWeight = Math.abs(Mth.cos(capturePitch(pitch) * Mth.DEG_TO_RAD));
            float pitchCost = Math.abs(pitch - pitchIndex) * PITCH_STEP;
            for (int yaw = 0; yaw < yawAngles; yaw++)
            {
                int cell = pitch * yawAngles + yaw;
                if (!captured[cell])
                    continue;
                int steps = Math.abs(yaw - yawIndex);
                float cost = pitchCost + Math.min(steps, yawAngles - steps) * yawStep * yawWeight;
                if (cost < bestCost)
                {
                    bestCost = cost;
                    best = cell;
                }
            }
        }
        return best;
    }

    /** Scale about the camera that brings a billboard {@code distance} blocks away {@code pull} blocks nearer; 1 when it is too near to move. */
    static float pullScale(double distance, float pull)
    {
        // Written so that a NaN distance or pull leaves the billboard where it is
        if (pull > 0F && distance > 2D * pull)
            return (float) ((distance - pull) / distance);
        return 1F;
    }

    /**
     * Captures the cell requested while rendering. Call at the start of a frame, before
     * any level rendering, with the main render target bound.
     */
    public static void capturePending()
    {
        Entry entry = pendingEntry;
        if (entry == null)
            return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            pendingEntry = null;
            return;
        }
        long gameTick = minecraft.level.getGameTime();
        if (lastCaptureTick == gameTick)
            return;
        pendingEntry = null;
        // Eviction or a settings change may have released the entry since the request.
        if (entry.failed || !entries.contains(entry))
            return;
        int cellIndex = pendingPitchIndex * settings.yawAngles() + pendingYawIndex;
        if (entry.captured[cellIndex])
            return;
        lastCaptureTick = gameTick;
        captureOneCell(entry, pendingYawIndex, pendingPitchIndex, cellIndex);
    }

    private static void captureOneCell(Entry entry, int yawIndex, int pitchIndex, int cellIndex)
    {
        try
        {
            ensureAtlas(entry, yawIndex, pitchIndex);
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
            FlansLog.log.warn("Disabling generated LOD impostor for model {} and texture {}: {}", entry.model.getClass().getName(), entry.sourceTexture, e.toString());
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
        boolean modelViewPushed = false;

        try
        {
            float halfExtent = entry.bounds.halfExtent();
            Matrix4f projection = new Matrix4f().setOrtho(-halfExtent, halfExtent, -halfExtent, halfExtent, -halfExtent * 4F, halfExtent * 4F);

            captureTarget.setClearColor(0F, 0F, 0F, 0F);
            captureTarget.clear(Minecraft.ON_OSX);
            captureTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            VertexPlatform.pushModelView();
            modelViewPushed = true;
            VertexPlatform.resetModelView();
            RenderSystem.applyModelViewMatrix();

            PoseStack capturePose = new PoseStack();
            capturePose.mulPose(Axis.XP.rotationDegrees(capturePitch(pitchIndex)));
            capturePose.mulPose(Axis.YP.rotationDegrees(360F * yawIndex / settings.yawAngles()));
            capturePose.translate(-entry.bounds.centerX(), -entry.bounds.centerY(), -entry.bounds.centerZ());
            // Light every cell from above as in the level, rotated into this view, and without fog.
            Matrix4f view = capturePose.last().pose();
            RenderSystem.setShaderLights(view.transformDirection(new Vector3f(LEVEL_LIGHT_0)), view.transformDirection(new Vector3f(LEVEL_LIGHT_1)));
            FogRenderer.setupNoFog();

            if (captureBuffer == null)
                captureBuffer = VertexPlatform.immediateBuffers(32_768);
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(entry.model))
            {
                PoseStack layerPose = new PoseStack();
                VertexPlatform.mulPose(layerPose, capturePose.last().pose());
                renderNeutralModel(entry.model, entry.type, layerPose, captureBuffer.getBuffer(renderPass.getRenderType(entry.sourceTexture, entry.translucent, entry.cull)), LightTexture.FULL_BRIGHT,
                    entry.red, entry.green, entry.blue, List.of(renderPass));
                captureBuffer.endBatch();
            }

            AtlasCell cell = atlasCell(yawIndex, pitchIndex, settings.yawAngles());
            AtlasPage page = entry.pages[cell.pageIndex()];
            atlasBlitter.copyFlipped(captureTarget.frameBufferId, page.texture.getId(), settings.resolution(), cell.column(), cell.row());
        }
        finally
        {
            if (modelViewPushed)
            {
                VertexPlatform.popModelView();
                RenderSystem.applyModelViewMatrix();
            }
            RenderSystem.setProjectionMatrix(restoreProjection, restoreSorting);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, restoreReadFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, restoreDrawFramebuffer);
            RenderSystem.viewport(restoreViewport[0], restoreViewport[1], restoreViewport[2], restoreViewport[3]);
        }
    }

    private static void renderNeutralModel(ModelDriveable model, DriveableType type, PoseStack poseStack, VertexConsumer vertices, int packedLight, float red, float green, float blue,
        List<EnumRenderPass> passes)
    {
        poseStack.pushPose();
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);
        float scale = type.getModelScale();
        poseStack.scale(scale, scale, scale);
        for (EnumRenderPass renderPass : passes)
            model.render(type, poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, 1F, renderPass);
        poseStack.popPose();
    }

    private static void ensureAtlas(Entry entry, int yawIndex, int pitchIndex)
    {
        int pageIndex = atlasCell(yawIndex, pitchIndex, settings.yawAngles()).pageIndex();
        if (entry.pages[pageIndex] != null)
            return;
        AtlasPage page = new AtlasPage();
        // Install first so the failure path also releases a partially created page.
        entry.pages[pageIndex] = page;
        int width = settings.resolution() * PAGE_COLUMNS;
        int height = settings.resolution() * PAGE_ROWS;
        page.texture = new DynamicTexture(new NativeImage(width, height, true));
        page.texture.setFilter(true, false);
        page.location = Minecraft.getInstance().getTextureManager().register("flans_driveable_impostor", page.texture);
    }

    private static void ensureCaptureTarget()
    {
        if (captureTarget != null && captureTarget.width == settings.resolution() && captureTarget.height == settings.resolution())
            return;
        if (captureTarget != null)
            captureTarget.destroyBuffers();
        captureTarget = new TextureTarget(settings.resolution(), settings.resolution(), true, Minecraft.ON_OSX);
    }

    private static void renderBillboard(Entry entry, ViewSelection view, Quaternionf entityRotation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay, float red,
        float green, float blue, float alpha)
    {
        AtlasCell cell = atlasCell(view.yawIndex(), view.pitchIndex(), settings.yawAngles());
        AtlasPage page = entry.pages[cell.pageIndex()];
        float[] uvs = cellUvs(cell);
        float u0 = uvs[0];
        float u1 = uvs[1];
        float v0 = uvs[2];
        float v1 = uvs[3];

        Vector3f offset = new Vector3f(entry.bounds.centerX(), entry.bounds.centerY(), entry.bounds.centerZ()).rotate(entityRotation);
        float halfExtent = entry.bounds.halfExtent();
        poseStack.pushPose();
        poseStack.translate(offset.x(), offset.y(), offset.z());
        poseStack.mulPose(view.billboardRotation(entityRotation));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(EnumRenderPass.DEFAULT.getRenderType(page.location, true, false));
        vertex(pose, vertices, -halfExtent, -halfExtent, u0, v1, packedLight, overlay, red, green, blue, alpha);
        vertex(pose, vertices, halfExtent, -halfExtent, u1, v1, packedLight, overlay, red, green, blue, alpha);
        vertex(pose, vertices, halfExtent, halfExtent, u1, v0, packedLight, overlay, red, green, blue, alpha);
        vertex(pose, vertices, -halfExtent, halfExtent, u0, v0, packedLight, overlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    /**
     * Texture coordinates of a cell, {@code u0, u1, v0, v1}, half a texel inside its edges so that filtering never
     * reads a neighbouring cell.
     */
    private static float[] cellUvs(AtlasCell cell)
    {
        int resolution = settings.resolution();
        float atlasWidth = (float) resolution * PAGE_COLUMNS;
        float atlasHeight = (float) resolution * PAGE_ROWS;
        float inset = 0.5F;
        return new float[]{(cell.column() * resolution + inset) / atlasWidth, ((cell.column() + 1) * resolution - inset) / atlasWidth, (cell.row() * resolution + inset) / atlasHeight,
            ((cell.row() + 1) * resolution - inset) / atlasHeight};
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float u, float v, int packedLight, int overlay, float red, float green, float blue, float alpha)
    {
        VertexPlatform.vertex(vertices, pose, x, y, 0F, red, green, blue, alpha, u, v, overlay, packedLight, 0F, 1F, 0F);
    }

    private static int yawIndex(float viewYaw, int yawAngles)
    {
        float normalized = Mth.positiveModulo(viewYaw, 360F);
        return Mth.floor(normalized / 360F * yawAngles + 0.5F) % yawAngles;
    }

    private static int pitchIndex(float viewPitch)
    {
        return Mth.clamp(Math.round((viewPitch + 90F) / PITCH_STEP), 0, PITCH_ANGLES - 1);
    }

    static float capturePitch(int pitchIndex)
    {
        return -90F + pitchIndex * PITCH_STEP;
    }

    static AtlasCell atlasCell(int yawIndex, int pitchIndex, int yawAngles)
    {
        int pagesPerPitch = (yawAngles + PAGE_COLUMNS - 1) / PAGE_COLUMNS;
        return new AtlasCell(pitchIndex / PAGE_ROWS * pagesPerPitch + yawIndex / PAGE_COLUMNS, yawIndex % PAGE_COLUMNS, pitchIndex % PAGE_ROWS);
    }

    record AtlasCell(int pageIndex, int column, int row)
    {}

    /**
     * Selects an atlas view from the camera's position in model space. Looking in
     * another direction without moving the camera must not make a different side
     * of the vehicle appear. The inverse entity rotation also handles pitched and
     * rolled aircraft without relying on the world render pose's Euler angles.
     */
    static ViewSelection selectView(Vec3 cameraOffset, Quaternionf entityRotation, float centerX, float centerY, float centerZ, int yawAngles)
    {
        Vector3f localCamera = new Vector3f((float) cameraOffset.x(), (float) cameraOffset.y(), (float) cameraOffset.z());
        new Quaternionf(entityRotation).conjugate().transform(localCamera);
        localCamera.sub(centerX, centerY, centerZ);
        if (!Float.isFinite(localCamera.lengthSquared()) || localCamera.lengthSquared() <= 1.0E-6F)
            return new ViewSelection(0, PITCH_ANGLES / 2, 0F, 0F);

        localCamera.normalize();
        // Azimuth is undefined at the poles. A stable view avoids noise from the
        // inverse rotation selecting arbitrary top/bottom captures.
        double horizontal = Math.hypot(localCamera.x(), localCamera.z());
        if (horizontal <= 1.0E-5D)
        {
            float polePitch = localCamera.y() < 0F ? -90F : 90F;
            return new ViewSelection(0, pitchIndex(polePitch), 0F, polePitch);
        }
        // Capturing rotates the model, so its yaw is opposite the model-space
        // azimuth from the model towards the camera.
        float captureYaw = -(float) Math.toDegrees(Math.atan2(localCamera.x(), localCamera.z()));
        float capturePitch = (float) Math.toDegrees(Math.atan2(localCamera.y(), horizontal));
        return new ViewSelection(yawIndex(captureYaw, yawAngles), pitchIndex(capturePitch), captureYaw, capturePitch);
    }

    record ViewSelection(int yawIndex, int pitchIndex, float viewYaw, float viewPitch)
    {
        /**
         * Undo the continuous capture rotation, then restore the entity rotation.
         * This faces the model center towards the camera while preserving bank and
         * screen orientation, including when the camera looks away from the model.
         */
        Quaternionf billboardRotation(Quaternionf entityRotation)
        {
            return new Quaternionf(entityRotation).rotateY(-viewYaw * Mth.DEG_TO_RAD).rotateX(-viewPitch * Mth.DEG_TO_RAD);
        }
    }

    private static float projectedDiameter(float radius, float projectionPixels, double cameraDistance)
    {
        double nearestDistance = Math.max(0.01D, cameraDistance - radius);
        return (float) (2D * radius * projectionPixels / nearestDistance);
    }

    private static void clearNow()
    {
        for (Entry entry : entries)
            release(entry);
        entries.clear();
        boundsCache.clear();
        pendingEntry = null;
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
        for (int index = 0; index < entry.pages.length; index++)
        {
            AtlasPage page = entry.pages[index];
            if (page == null)
                continue;
            if (page.location != null)
                Minecraft.getInstance().getTextureManager().release(page.location);
            else if (page.texture != null)
                page.texture.close();
            entry.pages[index] = null;
        }
    }

    private record Settings(int resolution, int yawAngles, int maxEntries)
    {
        private int cellCount()
        {
            return yawAngles * PITCH_ANGLES;
        }

        private int pageCount()
        {
            return (yawAngles + PAGE_COLUMNS - 1) / PAGE_COLUMNS * ((PITCH_ANGLES + PAGE_ROWS - 1) / PAGE_ROWS);
        }
    }

    private static final class AtlasPage
    {
        private DynamicTexture texture;
        private ResourceLocation location;
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
        private final AtlasPage[] pages;
        private boolean failed;
        private long lastUsedMillis;

        private Entry(ModelDriveable model, DriveableType type, ResourceLocation sourceTexture, boolean translucent, boolean cull, float red, float green, float blue, ModelBounds bounds,
            int cellCount, long lastUsedMillis)
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
            this.pages = new AtlasPage[settings.pageCount()];
            this.lastUsedMillis = lastUsedMillis;
        }

        private boolean matches(ModelDriveable otherModel, DriveableType otherType, ResourceLocation otherTexture, boolean otherTranslucent, boolean otherCull, float otherRed, float otherGreen,
            float otherBlue)
        {
            return model == otherModel && type == otherType && sourceTexture.equals(otherTexture) && translucent == otherTranslucent && cull == otherCull
                && Float.floatToIntBits(red) == Float.floatToIntBits(otherRed) && Float.floatToIntBits(green) == Float.floatToIntBits(otherGreen)
                && Float.floatToIntBits(blue) == Float.floatToIntBits(otherBlue);
        }
    }

    /**
     * The atlas of one model and skin that a distant shape is drawn from. A clear or an eviction may release it at
     * any time, after which it draws nothing until requested again. Render thread.
     */
    public static final class DistantAtlas
    {
        private final Entry entry;

        private DistantAtlas(Entry entry)
        {
            this.entry = entry;
        }
    }

    /**
     * A camera-facing billboard of a distant shape relative to the camera, filled by {@link #fillDistantBillboard}.
     * Its corners are {@code center} plus or minus {@code right} and {@code up}; the lower left one shows
     * {@code u0, v1} and the upper right one {@code u1, v0}. The vectors belong to the billboard and change with it.
     */
    @Getter
    public static final class DistantBillboard
    {
        private final Vector3f center = new Vector3f();
        private final Vector3f right = new Vector3f();
        private final Vector3f up = new Vector3f();
        /** OpenGL name of the atlas page. */
        private int texture;
        private float atlasWidth;
        private float atlasHeight;
        private float u0;
        private float u1;
        private float v0;
        private float v1;
    }

    private record ModelBounds(float centerX, float centerY, float centerZ, float radius, float halfExtent, boolean valid)
    {
        private static final ModelBounds INVALID = new ModelBounds(0F, 0F, 0F, 0F, 0F, false);
    }

    private static final class BoundsConsumer extends PositionCollector
    {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        @Override
        public void vertexPosition(float x, float y, float z)
        {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private ModelBounds toBounds()
        {
            if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ) || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ))
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
