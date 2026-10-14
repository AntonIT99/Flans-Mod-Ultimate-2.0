package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.*;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import com.flansmodultimate.client.render.item.GunItemRenderer;
import com.flansmodultimate.client.render.thermal.VehicleThermalRenderer;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.weapons.DerivedMuzzle;
import com.flansmodultimate.common.driveables.weapons.ShootPoint;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Shared renderer for plane, vehicle and mecha root entities. */
public class DriveableRenderer<T extends Driveable> extends FlanEntityRenderer<T>
{
    /** Marker colours for the authored shoot points of each weapon bank, as red, green, blue. */
    private static final float[] PRIMARY_MARKER = {0F, 1F, 1F};
    private static final float[] SECONDARY_MARKER = {1F, 0.5F, 0F};
    private static final float[] GUN_ORIGIN_MARKER = {0F, 1F, 0.25F};
    /** A point the shoot-point debug command has moved, so it reads apart from an authored one. */
    private static final float[] OVERRIDDEN_MARKER = {1F, 1F, 0F};
    /** A muzzle measured off the model rather than read from the type file. */
    private static final float[] MEASURED_MARKER = {1F, 1F, 1F};

    /** Weak keys avoid retaining entities after a world unload. Render-thread only. */
    private final Map<Driveable, DriveableAnimationHistory> animationStates = new WeakHashMap<>();
    /** Limits diagnostic markers to once per game tick, rather than once per frame. */
    private final Map<Driveable, Integer> diagnosticMarkerTicks = new WeakHashMap<>();

    /** Nesting depth of {@link #renderPreview(Runnable)}. Render thread only. */
    private static int previewDepth;
    /**
     * The camera projection's scale in the last ordinary world pass. A shader pack's shadow pass
     * replaces the projection with the shadow map's, which says nothing about on-screen size.
     */
    private static float viewProjectionPixels;

    /**
     * Draws a driveable through the entity dispatcher for a menu preview.
     *
     * <p>
     * Both level of detail stages measure the entity in world space against
     * the game camera. A GUI supplies neither, so they are switched off for the
     * duration of the call instead of silently culling the whole model.
     * </p>
     */
    public static void renderPreview(Runnable render)
    {
        ++previewDepth;
        try
        {
            render.run();
        }
        finally
        {
            --previewDepth;
        }
    }

    public static boolean isRenderingPreview()
    {
        return previewDepth > 0;
    }

    public DriveableRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 1F;
    }

    @Override
    public void render(@NotNull T driveable, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        long started = RenderDiagnostics.startTimer();
        try
        {
            renderDriveable(driveable, partialTick, poseStack, buffer, packedLight);
        }
        finally
        {
            RenderDiagnostics.countDriveableTime(started);
        }
    }

    // The projection scale of the world pass is shared by every driveable renderer.
    @SuppressWarnings("java:S2696")
    private void renderDriveable(T driveable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        DriveableType type = driveable.getConfigType();
        if (type == null || !(ModelCache.getOrLoadTypeModel(type) instanceof ModelDriveable model))
            return;

        float yaw = Mth.rotLerp(partialTick, driveable.getPrevYaw(), driveable.getYaw());
        float pitch = Mth.rotLerp(partialTick, driveable.getPrevPitch(), driveable.getPitch());
        float roll = Mth.rotLerp(partialTick, driveable.getPrevRoll(), driveable.getRoll());

        DriveableAnimationHistory history = animationStates.computeIfAbsent(driveable, ignored -> new DriveableAnimationHistory());
        history.advance(driveable, type);
        history.updateModelAimPivots(driveable, type, model);
        boolean thermalMask = VehicleThermalRenderer.isRenderingMask();
        boolean shadowPass = ShaderPlatform.isRenderingShadowPass();
        if (!thermalMask && !shadowPass)
            renderDiagnosticMarkers(driveable, type);
        ResourceLocation texture = getTextureLocation(driveable);
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);
        float red = getRed(type);
        float green = getGreen(type);
        float blue = getBlue(type);
        float scale = type.getModelScale();
        // A shader pack's shadow pass replaces the projection with the shadow map's.
        float projectionPixels = viewProjectionPixels;
        if (!shadowPass)
        {
            projectionPixels = Math.abs(RenderSystem.getProjectionMatrix().m11()) * Minecraft.getInstance().getWindow().getHeight() * 0.5F;
            if (!isRenderingPreview())
                viewProjectionPixels = projectionPixels;
        }
        Vec3 cameraOffset = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(driveable.getPosition(partialTick));
        double cameraDistance = cameraOffset.length();

        float entityYawRotation = driveable instanceof Plane || driveable instanceof Vehicle || driveable instanceof Mecha ? 180F - yaw : -yaw;
        boolean locallyControlled = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == driveable;
        boolean intact = true;
        for (EnumDriveablePart part : type.getHealth().keySet())
        {
            if (!driveable.isPartIntact(part))
            {
                intact = false;
                break;
            }
        }
        // Distance based level of detail measures the entity against the world
        // camera through a perspective projection. A menu draws the same entity
        // under the GUI's orthographic matrix at a pose only tens of pixels from
        // the origin, which those measurements read as vanishingly small.
        // A heat silhouette must use geometry: an impostor's transparent texels
        // cannot be recovered after the mask substitutes its white texture.
        boolean preview = isRenderingPreview() || thermalMask;
        DriveableImpostorCache.Result lodResult = DriveableImpostorCache.Result.notRendered();
        if (!preview && !locallyControlled)
        {
            // A shader pack's shadow pass must not replace the main pass's decision. A camera-facing
            // billboard would also cast an edge-on shadow, so a driveable drawn as an impostor casts none.
            if (shadowPass && history.usingImpostor)
            {
                RenderDiagnostics.countShadowDriveable(true);
                return;
            }
            lodResult = DriveableImpostorCache.renderOrPrepare(model, type, texture, translucent, cull, red, green, blue, poseStack, buffer, packedLight, projectionPixels, cameraDistance,
                cameraOffset, entityYawRotation, pitch, roll, !shadowPass && !(driveable instanceof Mecha) && intact, history.usingImpostor);
            if (!shadowPass)
                history.usingImpostor = lodResult.usingImpostor();
            if (lodResult.rendered())
            {
                DriveableMuzzleFlashes.render(driveable, poseStack, buffer);
                return;
            }
        }
        if (shadowPass)
            RenderDiagnostics.countShadowDriveable(false);

        // Only exact geometry needs the interpolated wheel/track/turret state.
        // Tick history above still advances while an impostor is displayed.
        ModelDriveable.RenderState state = history.state(driveable, partialTick, yaw, pitch, roll);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYawRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);

        // Legacy driveable renderers applied ModelScale to the complete model
        // hierarchy. Keep pivots, attachment points and procedural track paths
        // under the same transform instead of scaling every mesh independently.
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        float minimumPartPixels = preview ? 0F : (float) ModClientConfig.get().minimumDriveablePartPixelSize;
        if (!preview && !locallyControlled && ModClientConfig.get().enableDriveableLod)
        {
            float distanceScale = DriveableLodPolicy.distanceScale(lodResult.modelRadius(), type instanceof VehicleType && !type.isFloatOnWater(),
                (float) ModClientConfig.get().groundVehicleLodDistanceFactor);
            minimumPartPixels = DriveableLodPolicy.partThreshold(minimumPartPixels, (float) ModClientConfig.get().maximumDriveableLodPartPixelSize,
                (float) ModClientConfig.get().driveableLodDetailMultiplier, cameraDistance, distanceScale);
        }
        int trackLinkGroup = 0;
        if (!preview && !locallyControlled && ModClientConfig.get().enableDriveableLod && model instanceof ModelVehicle vehicleModel)
        {
            if (!shadowPass)
                trackLinkGroup = vehicleModel.selectTrackLinkGroup(type, projectionPixels, modelOriginDistance(poseStack), modelScaleBound(poseStack),
                    (float) ModClientConfig.get().driveableTrackLinkLodPixelSize, (float) ModClientConfig.get().driveableTrackLinkGroupingPixelSize, history.trackLinkGroup);
            // The shadow view is measured from the sun, so reuse the main pass's choice without touching
            // its hysteresis. A shadow shows only each link's outline, which its envelope keeps.
            else if (ModClientConfig.get().driveableTrackLinkLodPixelSize > 0D && vehicleModel.hasTrackLinkEnvelopes(type))
                trackLinkGroup = Math.max(1, history.trackLinkGroup);
            else
                trackLinkGroup = history.trackLinkGroup;
        }
        if (!shadowPass)
            history.trackLinkGroup = trackLinkGroup;
        int previousTrackGroup = TrackLinkLod.activeGroup();
        TrackLinkLod.setGroup(trackLinkGroup);
        boolean useScreenSpaceCulling = minimumPartPixels > 0F && projectionPixels > 0F;
        if (useScreenSpaceCulling && shadowPass)
        {
            // Depths in the shadow view are measured from the sun. Size every part as the camera sees
            // the model's nearest point instead, so the shadow drops no part the view keeps.
            float radius = lodResult.modelRadius();
            double nearest = Math.max(0.5D, Float.isFinite(radius) ? cameraDistance - radius : cameraDistance);
            ModelRendererTurbo.beginFixedScaleCulling(minimumPartPixels, (float) (projectionPixels / nearest));
        }
        else if (useScreenSpaceCulling)
            ModelRendererTurbo.beginScreenSpaceCulling(minimumPartPixels, projectionPixels);
        try
        {
            var renderPasses = ModelCache.getRenderPasses(model);
            for (EnumRenderPass renderPass : renderPasses)
            {
                var consumer = GpuModelCache.begin(buffer, renderPass, texture, translucent, cull, !preview);
                try
                {
                    model.render(driveable, state, poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, 1F, renderPass);
                }
                finally
                {
                    GpuModelCache.end(consumer);
                }
            }
            poseStack.popPose();
            if (driveable instanceof Mecha && model instanceof ModelMecha mechaModel && type instanceof MechaType mechaType)
                renderMechaAddons(driveable, mechaType, mechaModel, state, history, poseStack, buffer, packedLight);
        }
        finally
        {
            TrackLinkLod.setGroup(previousTrackGroup);
            if (useScreenSpaceCulling)
                ModelRendererTurbo.endScreenSpaceCulling();
        }
        poseStack.popPose();
        if (!thermalMask && !preview)
            DriveableMuzzleFlashes.render(driveable, poseStack, buffer);
    }

    private static double modelOriginDistance(PoseStack pose)
    {
        return com.flansmodultimate.client.render.preview.WorldModelPose.originDistance(pose);
    }

    /** Includes constructor-time legacy scaling. Gershgorin bounds the largest singular value even with shear. */
    static float modelScaleBound(PoseStack pose)
    {
        return com.flansmodultimate.client.render.preview.WorldModelPose.scaleBound(pose);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T driveable)
    {
        DriveableType type = driveable.getConfigType();
        if (type == null)
            return FlansModTextures.FALLBACK_TEXTURE;

        Paintjob paintjob = type.getPaintjob(driveable.getPaintjobId());
        return paintjob != null && paintjob.getTexture() != null ? paintjob.getTexture() : Optional.ofNullable(type.getTexture()).orElse(FlansModTextures.FALLBACK_TEXTURE);
    }

    private static void renderShootPointMarkers(Driveable driveable, DriveableType type, boolean secondary, float[] authoredColour)
    {
        for (var point : type.shootPoints(secondary))
        {
            float[] colour = point.isDebugOverride() ? OVERRIDDEN_MARKER : authoredColour;
            Vec3 direction = driveable.getDebugShootDirection(point, secondary).scale(2D);
            // A twin or quad mount fires from each of its barrels in turn, so each gets a marker.
            for (int barrel = 0; barrel < point.getBarrelCount(); barrel++)
            {
                Vec3 muzzle = driveable.getDebugShootOrigin(point, barrel);
                DebugHelper.spawnDebugDot(muzzle, 2, colour[0], colour[1], colour[2]);
                DebugHelper.spawnDebugVector(muzzle, direction, 2, colour[0], colour[1], colour[2]);
            }
        }
    }

    /**
     * Draws where the loaded model puts each muzzle, next to where the type file
     * does. The gap between the two markers is the correction a definition needs,
     * and seeing it on the vehicle is quicker to judge than reading the delta out
     * of {@code /flandebug shootpoint list}.
     */
    private static void renderMeasuredMuzzles(Driveable driveable, DriveableType type)
    {
        // Every tube of the main armament gets its own marker, so a quad mount shows four.
        var authored = type.shootPoints(false);
        EnumDriveablePart part = authored.isEmpty() ? EnumDriveablePart.TURRET : authored.get(0).getRootPos().getPart();
        for (DerivedMuzzle barrel : ClientHooks.RENDER.derivePrimaryBarrels(type))
        {
            // Follow whichever part the authored point is mounted on, so the
            // measured marker tracks the turret exactly as the real one does.
            Vec3 position = driveable.getDebugShootOrigin(new ShootPoint(new DriveablePosition(toBlocks(barrel), part), new Vector3f()));
            DebugHelper.spawnDebugDot(position, 2, MEASURED_MARKER[0], MEASURED_MARKER[1], MEASURED_MARKER[2]);
        }

        for (DerivedMuzzle muzzle : ClientHooks.RENDER.deriveMuzzles(type))
        {
            if (muzzle.isBarrel())
                continue;
            for (Vector3f barrel : aimedSeatMuzzles(driveable, type, muzzle, toBlocks(muzzle)))
            {
                Vec3 position = driveable.getGunOriginWorldPosition(barrel);
                DebugHelper.spawnDebugDot(position, 2, MEASURED_MARKER[0], MEASURED_MARKER[1], MEASURED_MARKER[2]);
            }
        }
    }

    private static Vector3f toBlocks(DerivedMuzzle muzzle)
    {
        return new Vector3f(muzzle.position().x / 16F, muzzle.position().y / 16F, muzzle.position().z / 16F);
    }

    /**
     * The derived seat muzzle is measured at rest, which is what a GunOrigin
     * suggestion needs. The markers instead follow the gun as it is drawn, so
     * this re-measures each of its barrels at the seat's live aim, in the same
     * GunOrigin terms.
     */
    private static List<Vector3f> aimedSeatMuzzles(Driveable driveable, DriveableType type, DerivedMuzzle muzzle, Vector3f restBlocks)
    {
        SeatInfo seat = type.getSeat(muzzle.seatIndex());
        if (seat == null || !(ModelCache.getLoadedTypeModel(type) instanceof ModelDriveable model))
            return List.of(restBlocks);
        List<Vec3> aimed = model.getAimedRegisteredGunMuzzles(driveable, seat, 1F);
        if (aimed.isEmpty())
            return List.of(restBlocks);
        return aimed.stream().map(barrel ->
        {
            Vector3f position = LegacyDriveableCoordinates.modelPixelsToTypeFile(barrel.scale(Math.max(1.0E-4D, type.getModelScale())), type instanceof PlaneType);
            return new Vector3f(position.x / 16F, position.y / 16F - (float) Driveable.PASSENGER_GUN_MOUNTED_OFFSET, position.z / 16F);
        }).toList();
    }

    private void renderDiagnosticMarkers(Driveable driveable, DriveableType type)
    {
        if (!ModClient.isDebug())
            return;

        Integer renderedTick = diagnosticMarkerTicks.get(driveable);
        if (renderedTick != null && renderedTick == driveable.tickCount)
            return;
        diagnosticMarkerTicks.put(driveable, driveable.tickCount);

        renderShootPointMarkers(driveable, type, false, PRIMARY_MARKER);
        renderShootPointMarkers(driveable, type, true, SECONDARY_MARKER);
        renderMeasuredMuzzles(driveable, type);

        for (int seat = 0; seat <= type.getNumPassengers(); seat++)
        {
            var seatInfo = type.getSeat(seat);
            if (seatInfo == null)
                continue;
            Vec3 seatPosition = driveable.getSeatWorldPosition(seat);
            if (seat == 0)
            {
                DebugHelper.spawnDebugDot(seatPosition, 2, 0F, 0.45F, 1F);
                DebugHelper.spawnDebugVector(seatPosition, new Vec3(0D, 2D, 0D), 2, 0F, 0.45F, 1F);
            }
            else
            {
                DebugHelper.spawnDebugDot(seatPosition, 2, 1F, 0F, 1F);
                DebugHelper.spawnDebugVector(seatPosition, new Vec3(0D, 2D, 0D), 2, 1F, 0F, 1F);
            }
            if (seat > 0 && seatInfo.getGunType() != null)
            {
                Vec3 direction = driveable.getPassengerShootDirection(seat);
                float[] colour = type.isGunOriginOverridden(seat) ? OVERRIDDEN_MARKER : GUN_ORIGIN_MARKER;
                for (int barrel = 0; barrel < seatInfo.getGunBarrelCount() && direction != null; barrel++)
                {
                    Vec3 muzzle = driveable.getPassengerShootOrigin(seat, barrel);
                    if (muzzle == null)
                        continue;
                    DebugHelper.spawnDebugDot(muzzle, 2, colour[0], colour[1], colour[2]);
                    DebugHelper.spawnDebugVector(muzzle, direction.scale(2D), 2, colour[0], colour[1], colour[2]);
                }
            }
        }
    }

    private static void renderMechaAddons(Driveable driveable, MechaType mechaType, ModelMecha mechaModel, ModelDriveable.RenderState state, DriveableAnimationHistory history, PoseStack poseStack,
        MultiBufferSource buffer, int packedLight)
    {
        DriveableData data = driveable.getDriveableData();
        if (data == null)
            return;

        if (driveable.isPartIntact(EnumDriveablePart.HIPS))
        {
            poseStack.pushPose();
            poseStack.scale(mechaType.getModelScale(), mechaType.getModelScale(), mechaType.getModelScale());
            translateModelVector(poseStack, mechaModel.hipsAttachmentPoint);
            poseStack.scale(mechaType.getHeldItemScale(), mechaType.getHeldItemScale(), mechaType.getHeldItemScale());
            renderMechaAddon(data.getMechaAddon(EnumMechaSlotType.HIPS), 0F, poseStack, buffer, packedLight);
            poseStack.popPose();
        }

        float armPitch = Mth.clamp(state.turretPitch(), -mechaType.getUpperArmLimit(), mechaType.getLowerArmLimit());
        renderHandAddon(data.getMechaAddon(EnumMechaSlotType.LEFT_TOOL), true, driveable.isPartIntact(EnumDriveablePart.LEFT_ARM), mechaType, armPitch,
            DriveableInput.isDown(state.inputMask(), DriveableInput.PRIMARY_FIRE), state.animationTime(), history.leftGunAnimations, poseStack, buffer, packedLight);
        renderHandAddon(data.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL), false, driveable.isPartIntact(EnumDriveablePart.RIGHT_ARM), mechaType, armPitch,
            DriveableInput.isDown(state.inputMask(), DriveableInput.SECONDARY_FIRE), state.animationTime(), history.rightGunAnimations, poseStack, buffer, packedLight);
    }

    private static void renderHandAddon(ItemStack stack, boolean leftHand, boolean armIntact, MechaType mechaType, float armPitch, boolean active, float animationTime, GunAnimations gunAnimations,
        PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!armIntact || stack.isEmpty())
            return;

        com.flansmod.common.vector.Vector3f armOrigin = leftHand ? mechaType.getLeftArmOrigin() : mechaType.getRightArmOrigin();
        com.flansmod.common.vector.Vector3f handModifier = leftHand ? mechaType.getLeftHandModifier() : mechaType.getRightHandModifier();

        poseStack.pushPose();
        translateModelVector(poseStack, armOrigin);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F - armPitch));
        // The arm has already been rotated around Z, so model-space X/Y map to pose-space Y/X here.
        double translatedX = handModifier.y;
        double translatedY = -mechaType.getArmLength() - handModifier.x;
        poseStack.translate(translatedX, translatedY, -handModifier.z);
        poseStack.scale(mechaType.getModelScale() * mechaType.getHeldItemScale(), mechaType.getModelScale() * mechaType.getHeldItemScale(), mechaType.getModelScale() * mechaType.getHeldItemScale());
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90F));
        if (stack.getItem() instanceof GunItem gunItem && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun gunModel)
            GunItemRenderer.renderEmbedded(gunModel, stack, gunAnimations, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        else
            renderMechaAddon(stack, active ? animationTime * 25F : 0F, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void renderMechaAddon(ItemStack stack, float spinDegrees, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!(stack.getItem() instanceof MechaAddonItem addon))
            return;
        MechaItemType type = addon.getConfigType();
        if (!(ModelCache.getOrLoadTypeModel(type) instanceof ModelMechaTool model))
            return;

        ResourceLocation texture = type.getTexture();
        int color = type.getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);

        poseStack.pushPose();
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            model.renderAll(poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, type.getModelScale(),
                spinDegrees, renderPass);
        }
        poseStack.popPose();
    }

    private static void translateModelVector(PoseStack poseStack, com.flansmod.common.vector.Vector3f vector)
    {
        if (vector != null)
            poseStack.translate(vector.x, vector.y, -vector.z);
    }
}
