package com.flansmodultimate.client.render.preview;

import com.flansmod.client.model.*;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.client.render.entity.*;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import com.flansmodultimate.client.render.thermal.VehicleThermalRenderer;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.flansmodultimate.platform.render.WorldModelBoundsCollector;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Static world models use the live driveable caches and policies without creating a driveable entity. */
public final class WorldModelPreview
{
    private static final Map<Entity, History> histories = new WeakHashMap<>();
    private static float viewProjectionPixels;

    private WorldModelPreview()
    {}

    public static boolean render(InfoType type, Entity entity, float partialTick, PoseStack.Pose entityPose, Vec3 renderOffset, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers,
        int light, int overlay, float red, float green, float blue, float alpha)
    {
        var model = ModelCache.getOrLoadTypeModel(type);
        if (!(model instanceof ModelDriveable && type instanceof DriveableType) && !(model instanceof ModelAAGun && type instanceof AAGunType))
            return false;
        var config = ModClientConfig.get();
        boolean shadow = ShaderPlatform.isRenderingShadowPass();
        boolean world = !DriveableRenderer.isRenderingPreview() && !VehicleThermalRenderer.isRenderingMask() && (shadow || perspectiveProjection(RenderSystem.getProjectionMatrix()))
            && buffers.getClass() == MultiBufferSource.BufferSource.class;
        float projectionPixels = viewProjectionPixels;
        if (world && !shadow)
        {
            projectionPixels = Math.abs(RenderSystem.getProjectionMatrix().m11()) * Minecraft.getInstance().getWindow().getHeight() * 0.5F;
            viewProjectionPixels = projectionPixels;
        }
        History history = histories.computeIfAbsent(entity, ignored -> new History());
        if (history.model != model || history.type != type || !texture.equals(history.texture))
        {
            history.model = model;
            history.type = type;
            history.texture = texture;
            history.usingImpostor = false;
            history.trackGroup = 0;
            history.radius = model instanceof ModelDriveable driveableModel && type instanceof DriveableType driveableType
                ? DriveableImpostorCache.modelRadius(driveableModel, driveableType)
                : measureRadius(type);
        }
        boolean translucent = config.useTranslucentRendering(type);
        boolean cull = config.useCullingRendering(type);
        int colour = type.getColour();
        float typeRed = (colour >> 16 & 255) / 255F;
        float typeGreen = (colour >> 8 & 255) / 255F;
        float typeBlue = (colour & 255) / 255F;
        pose.pushPose();
        int previousTrackGroup = TrackLinkLod.activeGroup();
        boolean culling = false;
        try
        {
            pose.mulPose(Axis.YP.rotationDegrees(type instanceof PlaneType ? -90F : 90F));
            // This relative transform includes NPC size, model height, living rotation and death tilt.
            history.relative.set(entityPose.pose()).invert().mul(pose.last().pose());
            float parentScale = WorldModelPose.uniformScale(history.relative);
            Vec3 cameraOffset = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(entity.getPosition(partialTick)).subtract(renderOffset).subtract(history.relative.m30(),
                history.relative.m31(), history.relative.m32());
            double distance = cameraOffset.length();
            float radius = history.radius * WorldModelPose.scaleBound(pose);
            boolean lod = world && config.enableDriveableLod;
            if (lod && model instanceof ModelDriveable driveableModel && type instanceof DriveableType driveableType)
            {
                if (shadow && history.usingImpostor)
                {
                    RenderDiagnostics.countShadowDriveable(true);
                    return true;
                }
                boolean uniform = Float.isFinite(parentScale);
                if (uniform)
                {
                    WorldModelPose.rotation(history.relative, parentScale, history.rotationScratch, history.rotation);
                    history.billboard.last().pose().set(entityPose.pose());
                    history.billboard.last().normal().set(entityPose.normal());
                    history.billboard.translate(history.relative.m30(), history.relative.m31(), history.relative.m32());
                    history.billboard.scale(parentScale, parentScale, parentScale);
                    var result = DriveableImpostorCache.renderPosedOrPrepare(driveableModel, driveableType, texture, translucent, cull, typeRed, typeGreen, typeBlue, history.billboard, buffers, light,
                        overlay, red, green, blue, alpha, parentScale, projectionPixels, distance, cameraOffset.scale(1D / parentScale), history.rotation,
                        Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation(), !shadow && alpha == 1F && !(type instanceof MechaType), history.usingImpostor);
                    if (!shadow)
                        history.usingImpostor = result.usingImpostor();
                    if (result.rendered())
                        return true;
                }
                else if (!shadow)
                    history.usingImpostor = false;
            }
            else if (!shadow)
                history.usingImpostor = false;
            if (shadow && model instanceof ModelDriveable)
                RenderDiagnostics.countShadowDriveable(false);

            if (model instanceof ModelDriveable)
            {
                LegacyTransformApplier.applyModelTransform(model, type, pose);
                float scale = type.getModelScale();
                pose.scale(scale, scale, scale);
            }
            int trackGroup = 0;
            if (lod && model instanceof ModelVehicle vehicleModel && type instanceof DriveableType driveableType)
            {
                if (!shadow)
                    trackGroup = vehicleModel.selectTrackLinkGroup(driveableType, projectionPixels, WorldModelPose.originDistance(pose), WorldModelPose.scaleBound(pose),
                        (float) config.driveableTrackLinkLodPixelSize, (float) config.driveableTrackLinkGroupingPixelSize, history.trackGroup);
                else if (config.driveableTrackLinkLodPixelSize > 0D && vehicleModel.hasTrackLinkEnvelopes(driveableType))
                    trackGroup = Math.max(1, history.trackGroup);
                else
                    trackGroup = history.trackGroup;
            }
            if (!shadow)
                history.trackGroup = trackGroup;
            TrackLinkLod.setGroup(trackGroup);
            float minimumPixels = world ? (float) config.minimumDriveablePartPixelSize : 0F;
            if (lod)
            {
                float distanceScale = DriveableLodPolicy.distanceScale(radius, type instanceof VehicleType vehicle && !vehicle.isFloatOnWater(), (float) config.groundVehicleLodDistanceFactor);
                minimumPixels = DriveableLodPolicy.partThreshold(minimumPixels, (float) config.maximumDriveableLodPartPixelSize, (float) config.driveableLodDetailMultiplier, distance, distanceScale);
            }
            culling = minimumPixels > 0F && projectionPixels > 0F;
            if (culling && shadow)
            {
                double nearest = Math.max(0.5D, Float.isFinite(radius) ? distance - radius : distance);
                ModelRendererTurbo.beginFixedScaleCulling(minimumPixels, (float) (projectionPixels / nearest));
            }
            else if (culling)
                ModelRendererTurbo.beginScreenSpaceCulling(minimumPixels, projectionPixels);
            for (var pass : ModelCache.getRenderPasses(model))
            {
                var vertices = GpuModelCache.begin(buffers, pass, texture, translucent, cull, world && alpha == 1F);
                try
                {
                    if (model instanceof ModelDriveable driveableModel && type instanceof DriveableType driveableType)
                        driveableModel.render(driveableType, pose, vertices, light, overlay, red * typeRed, green * typeGreen, blue * typeBlue, alpha, 1F, pass);
                    else if (model instanceof ModelAAGun aaModel && type instanceof AAGunType aaType)
                        aaModel.render(aaType, pose, vertices, light, overlay, red * typeRed, green * typeGreen, blue * typeBlue, alpha, type.getModelScale(), pass);
                }
                finally
                {
                    GpuModelCache.end(vertices);
                }
            }
            return true;
        }
        finally
        {
            if (culling)
                ModelRendererTurbo.endScreenSpaceCulling();
            TrackLinkLod.setGroup(previousTrackGroup);
            pose.popPose();
        }
    }

    static boolean perspectiveProjection(Matrix4f projection)
    {
        return projection.isFinite() && Math.abs(projection.m33()) < 1E-6F && Math.abs(projection.m23()) > 1E-6F;
    }

    public static void clear()
    {
        if (RenderSystem.isOnRenderThreadOrInit())
            clearNow();
        else
            RenderSystem.recordRenderCall(WorldModelPreview::clearNow);
    }

    private static void clearNow()
    {
        histories.clear();
        viewProjectionPixels = 0F;
    }

    /** Bounds include ModelScale and exclude the caller's parent transform. */
    private static float measureRadius(InfoType type)
    {
        WorldModelBoundsCollector bounds = new WorldModelBoundsCollector();
        TypeModelPreview.render(type, new PoseStack(), bounds, 0, 0, 1F, 1F, 1F, 1F);
        return bounds.radius();
    }

    private static final class History
    {
        private Object model;
        private InfoType type;
        private ResourceLocation texture;
        private boolean usingImpostor;
        private int trackGroup;
        private float radius;
        private final Matrix4f relative = new Matrix4f();
        private final Matrix4f rotationScratch = new Matrix4f();
        private final Quaternionf rotation = new Quaternionf();
        private final PoseStack billboard = new PoseStack();
    }
}
