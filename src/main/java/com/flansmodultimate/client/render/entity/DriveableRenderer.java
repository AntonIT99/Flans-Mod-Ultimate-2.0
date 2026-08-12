package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.ModelMecha;
import com.flansmod.client.model.ModelMechaTool;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.client.render.item.GunItemRenderer;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared renderer for plane, vehicle and mecha root entities. */
public class DriveableRenderer<T extends Driveable> extends FlanEntityRenderer<T>
{
    private static final float TRANSITION_PER_TICK = 0.16F;

    /** Weak keys avoid retaining entities after a world unload. Render-thread only. */
    private final Map<Driveable, AnimationHistory> animationStates = new WeakHashMap<>();
    /** Limits diagnostic markers to once per game tick, rather than once per frame. */
    private final Map<Driveable, Integer> diagnosticMarkerTicks = new WeakHashMap<>();

    public DriveableRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 1F;
    }

    @Override
    public void render(@NotNull T driveable, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        DriveableType type = driveable.getConfigType();
        if (type == null || !(ModelCache.getOrLoadTypeModel(type) instanceof ModelDriveable model))
            return;

        float yaw = Mth.rotLerp(partialTick, driveable.getPrevYaw(), driveable.getYaw());
        float pitch = Mth.rotLerp(partialTick, driveable.getPrevPitch(), driveable.getPitch());
        float roll = Mth.rotLerp(partialTick, driveable.getPrevRoll(), driveable.getRoll());
        float turretYaw = Mth.rotLerp(partialTick, driveable.getPrevTurretYaw(), driveable.getTurretYaw());
        float turretPitch = Mth.rotLerp(partialTick, driveable.getPrevTurretPitch(), driveable.getTurretPitch());

        AnimationHistory history = animationStates.computeIfAbsent(driveable, ignored -> new AnimationHistory());
        history.advance(driveable, type);
        renderDiagnosticMarkers(driveable, type);
        float throttle = Mth.lerp(partialTick, history.previousThrottle, history.throttle);
        float steering = Mth.lerp(partialTick, history.previousSteering, history.steering);
        float gearProgress = Mth.lerp(partialTick, history.previousGear, history.gear);
        float doorProgress = Mth.lerp(partialTick, history.previousDoor, history.door);
        float modeProgress = Mth.lerp(partialTick, history.previousMode, history.mode);
        float animationTime = driveable.tickCount + partialTick;
        float wheelAngle = 0F;
        float leftTrackProgress = 0F;
        float rightTrackProgress = 0F;
        if (driveable instanceof Vehicle vehicle)
        {
            wheelAngle = Mth.rotLerp(partialTick, vehicle.getPrevWheelAngle(), vehicle.getWheelAngle()) * Mth.DEG_TO_RAD;
            steering = Mth.rotLerp(partialTick, vehicle.getPrevWheelYaw(), vehicle.getWheelYaw());
            leftTrackProgress = wrappedLerp(partialTick, history.previousLeftTrack, history.leftTrack);
            rightTrackProgress = wrappedLerp(partialTick, history.previousRightTrack, history.rightTrack);
        }
        float legSwing = driveable instanceof Mecha
            ? wrappedLerp(partialTick, history.previousLegSwing, history.legSwing) : 0F;

        ModelDriveable.RenderState state = new ModelDriveable.RenderState(
            partialTick, yaw, pitch, roll, throttle, turretYaw, turretPitch,
            wheelAngle, steering, animationTime, gearProgress, doorProgress, modeProgress,
            leftTrackProgress, rightTrackProgress, legSwing,
            history.wingTransform, history.wingWheelTransform, history.bodyWheelTransform,
            history.tailWheelTransform, history.doorTransform, history.door2Transform,
            history.legAnimation, driveable.getInputMask(), driveable.getDriveableMode(), driveable.isVarFlare()
        );

        ResourceLocation texture = getTextureLocation(driveable);
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);
        float red = getRed(type);
        float green = getGreen(type);
        float blue = getBlue(type);
        float scale = type.getModelScale();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(driveable instanceof Plane || driveable instanceof Vehicle ? 180F - yaw : -yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);

        // Keep this in model space so the correction follows terrain pitch and roll,
        // and is scaled along with legacy models that use ModelScale.
        if (driveable instanceof Vehicle)
            poseStack.translate(0F, Vehicle.VEHICLE_MODEL_VERTICAL_OFFSET, 0F);

        // Legacy driveable renderers applied ModelScale to the complete model
        // hierarchy. Keep pivots, attachment points and procedural track paths
        // under the same transform instead of scaling every mesh independently.
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            model.render(driveable, state, poseStack,
                buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)),
                packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, 1F, renderPass);
        }
        poseStack.popPose();
        if (driveable instanceof Mecha && model instanceof ModelMecha mechaModel && type instanceof MechaType mechaType)
            renderMechaAddons(driveable, mechaType, mechaModel, state, history, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T driveable)
    {
        DriveableType type = driveable.getConfigType();
        if (type == null)
            return ResourceLocation.parse("");

        DriveableData data = driveable.getDriveableData();
        Paintjob paintjob = type.getPaintjob(data == null ? 0 : data.getPaintjobID());
        return paintjob != null && paintjob.getTexture() != null ? paintjob.getTexture() : type.getTexture();
    }

    private void renderDiagnosticMarkers(Driveable driveable, DriveableType type)
    {
        Integer renderedTick = diagnosticMarkerTicks.get(driveable);
        if (renderedTick != null && renderedTick == driveable.tickCount)
            return;
        diagnosticMarkerTicks.put(driveable, driveable.tickCount);

        if (driveable instanceof Vehicle)
        {
            for (var point : type.shootPoints(false))
                DebugHelper.spawnDebugDot(driveable.getDebugShootOrigin(point), 2, 0F, 1F, 1F);
            for (var point : type.shootPoints(true))
                DebugHelper.spawnDebugDot(driveable.getDebugShootOrigin(point), 2, 1F, 0.5F, 0F);
        }

        for (int seat = 0; seat <= type.getNumPassengers(); seat++)
        {
            if (type.getSeat(seat) == null)
                continue;
            if (seat == 0)
                DebugHelper.spawnDebugDot(driveable.getSeatWorldPosition(seat), 2, 0F, 0.45F, 1F);
            else
                DebugHelper.spawnDebugDot(driveable.getSeatWorldPosition(seat), 2, 1F, 0F, 1F);
        }
    }

    private static float inputAxis(int mask, int positive, int negative)
    {
        return (DriveableInput.isDown(mask, positive) ? 1F : 0F) - (DriveableInput.isDown(mask, negative) ? 1F : 0F);
    }

    private static void renderMechaAddons(Driveable driveable, MechaType mechaType, ModelMecha mechaModel, ModelDriveable.RenderState state, AnimationHistory history, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
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
        renderHandAddon(data.getMechaAddon(EnumMechaSlotType.LEFT_TOOL), true,
            driveable.isPartIntact(EnumDriveablePart.LEFT_ARM), mechaType, armPitch,
            DriveableInput.isDown(state.inputMask(), DriveableInput.PRIMARY_FIRE), state.animationTime(),
            history.leftGunAnimations, poseStack, buffer, packedLight);
        renderHandAddon(data.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL), false,
            driveable.isPartIntact(EnumDriveablePart.RIGHT_ARM), mechaType, armPitch,
            DriveableInput.isDown(state.inputMask(), DriveableInput.SECONDARY_FIRE), state.animationTime(),
            history.rightGunAnimations, poseStack, buffer, packedLight);
    }

    private static void renderHandAddon(ItemStack stack, boolean leftHand, boolean armIntact, MechaType mechaType, float armPitch, boolean active, float animationTime, GunAnimations gunAnimations, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!armIntact || stack.isEmpty())
            return;

        com.flansmod.common.vector.Vector3f armOrigin = leftHand
            ? mechaType.getLeftArmOrigin() : mechaType.getRightArmOrigin();
        com.flansmod.common.vector.Vector3f handModifier = leftHand
            ? mechaType.getLeftHandModifier() : mechaType.getRightHandModifier();

        poseStack.pushPose();
        translateModelVector(poseStack, armOrigin);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F - armPitch));
        poseStack.translate(handModifier.y, -mechaType.getArmLength() - handModifier.x, -handModifier.z);
        poseStack.scale(mechaType.getModelScale() * mechaType.getHeldItemScale(),
            mechaType.getModelScale() * mechaType.getHeldItemScale(),
            mechaType.getModelScale() * mechaType.getHeldItemScale());
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90F));
        if (stack.getItem() instanceof GunItem gunItem
            && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun gunModel)
            GunItemRenderer.renderEmbedded(gunModel, stack, gunAnimations, poseStack, buffer,
                packedLight, OverlayTexture.NO_OVERLAY);
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
        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            model.renderAll(poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)),
                packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F,
                type.getModelScale(), spinDegrees, renderPass);
        }
        poseStack.popPose();
    }

    private static void translateModelVector(PoseStack poseStack, com.flansmod.common.vector.Vector3f vector)
    {
        if (vector != null)
            poseStack.translate(vector.x, vector.y, -vector.z);
    }

    private static float approach(float value, float target, float amount)
    {
        return value < target ? Math.min(value + amount, target) : Math.max(value - amount, target);
    }

    private static float wrappedLerp(float partialTick, float previous, float current)
    {
        float delta = current - previous;
        if (delta > 0.5F)
            delta -= 1F;
        else if (delta < -0.5F)
            delta += 1F;
        float value = previous + Mth.clamp(partialTick, 0F, 1F) * delta;
        return value - Mth.floor(value);
    }

    private static final class AnimationHistory
    {
        private DriveableType configuredType;
        private int lastTick = Integer.MIN_VALUE;
        private float previousThrottle;
        private float throttle;
        private float previousSteering;
        private float steering;
        private float previousGear;
        private float gear;
        private float previousDoor;
        private float door;
        private float previousMode;
        private float mode;
        private float previousLeftTrack;
        private float leftTrack;
        private float previousRightTrack;
        private float rightTrack;
        private float previousLegSwing;
        private float legSwing;
        private final ModelDriveable.AnimatedTransform wingTransform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.AnimatedTransform wingWheelTransform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.AnimatedTransform bodyWheelTransform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.AnimatedTransform tailWheelTransform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.AnimatedTransform doorTransform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.AnimatedTransform door2Transform = new ModelDriveable.AnimatedTransform();
        private final ModelDriveable.LegAnimation legAnimation = new ModelDriveable.LegAnimation();
        private final GunAnimations leftGunAnimations = new GunAnimations();
        private final GunAnimations rightGunAnimations = new GunAnimations();
        private GunItem leftGunItem;
        private GunItem rightGunItem;
        private int leftGunRounds = -1;
        private int rightGunRounds = -1;

        private void advance(Driveable driveable, DriveableType type)
        {
            if (lastTick == Integer.MIN_VALUE || configuredType != type)
            {
                configuredType = type;
                throttle = driveable.getThrottle();
                previousThrottle = throttle;
                steering = targetSteering(driveable);
                previousSteering = steering;
                gear = driveable.isGearDeployed() ? 1F : 0F;
                previousGear = gear;
                door = driveable.isDoorOpen() ? 1F : 0F;
                previousDoor = door;
                mode = driveable.isWingFolded() || driveable.getDriveableMode() != 0 ? 1F : 0F;
                previousMode = mode;
                if (driveable instanceof Vehicle vehicle)
                {
                    leftTrack = previousLeftTrack = vehicle.getLeftTrackProgress();
                    rightTrack = previousRightTrack = vehicle.getRightTrackProgress();
                }
                if (driveable instanceof Mecha mecha)
                {
                    legSwing = previousLegSwing = mecha.getLegSwing();
                    updateLegTargets(type, legSwing, 1);
                }
                snapTransforms(driveable, type);
                updateHandGunAnimations(driveable, 1);
                lastTick = driveable.tickCount;
                return;
            }
            if (lastTick == driveable.tickCount)
                return;

            int elapsed = Mth.clamp(driveable.tickCount - lastTick, 1, 5);
            lastTick = driveable.tickCount;
            previousThrottle = throttle;
            previousSteering = steering;
            previousGear = gear;
            previousDoor = door;
            previousMode = mode;
            previousLeftTrack = leftTrack;
            previousRightTrack = rightTrack;
            previousLegSwing = legSwing;

            float amount = TRANSITION_PER_TICK * elapsed;
            throttle = approach(throttle, driveable.getThrottle(), amount);
            steering = approach(steering, targetSteering(driveable), amount * 90F);
            gear = approach(gear, driveable.isGearDeployed() ? 1F : 0F, amount);
            door = approach(door, driveable.isDoorOpen() ? 1F : 0F, amount);
            mode = approach(mode, driveable.isWingFolded() || driveable.getDriveableMode() != 0 ? 1F : 0F, amount);
            if (driveable instanceof Vehicle vehicle)
            {
                leftTrack = vehicle.getLeftTrackProgress();
                rightTrack = vehicle.getRightTrackProgress();
            }
            if (driveable instanceof Mecha mecha)
            {
                legSwing = mecha.getLegSwing();
                updateLegTargets(type, legSwing, elapsed);
            }
            advanceTransforms(driveable, type, elapsed);
            updateHandGunAnimations(driveable, elapsed);
        }

        private void updateHandGunAnimations(Driveable driveable, int elapsed)
        {
            DriveableData data = driveable.getDriveableData();
            if (data == null)
                return;
            HandAnimationState left = updateHandGunAnimation(data.getMechaAddon(EnumMechaSlotType.LEFT_TOOL),
                leftGunItem, leftGunRounds, leftGunAnimations,
                DriveableInput.isDown(driveable.getInputMask(), DriveableInput.PRIMARY_FIRE), elapsed);
            leftGunItem = left.item();
            leftGunRounds = left.rounds();
            HandAnimationState right = updateHandGunAnimation(data.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL),
                rightGunItem, rightGunRounds, rightGunAnimations,
                DriveableInput.isDown(driveable.getInputMask(), DriveableInput.SECONDARY_FIRE), elapsed);
            rightGunItem = right.item();
            rightGunRounds = right.rounds();
        }

        private static HandAnimationState updateHandGunAnimation(ItemStack stack, GunItem previousItem,
                                                                  int previousRounds, GunAnimations animations,
                                                                  boolean active, int elapsed)
        {
            for (int tick = 0; tick < elapsed; tick++)
                animations.update();
            if (!(stack.getItem() instanceof GunItem gunItem))
                return new HandAnimationState(null, -1);

            int rounds = 0;
            for (int slot = 0; slot < gunItem.getConfigType().getNumAmmoItemsInGun(stack); slot++)
                rounds += ShootableItem.getRoundsRemaining(gunItem.getAmmoItemStack(stack, slot));
            if (previousItem == gunItem && previousRounds >= 0
                && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun model)
            {
                if (rounds < previousRounds)
                    animations.doShoot(model.getPumpDelay(), model.getPumpTime(), model.getHammerDelay(),
                        model.getHammerAngle(), model.getAlthammerAngle(), model.getCasingDelay());
                else if (rounds > previousRounds)
                    animations.doReload(Math.max(1F, gunItem.getActualReloadTime(stack, ItemStack.EMPTY)),
                        model.getPumpDelayAfterReload(), model.getPumpTime(), model.getChargeDelayAfterReload(),
                        model.getChargeTime(), 1, false);
            }
            if (active)
                animations.addMinigunBarrelRotationSpeed(0.2F * elapsed);
            return new HandAnimationState(gunItem, rounds);
        }

        private record HandAnimationState(GunItem item, int rounds) {}

        private void snapTransforms(Driveable driveable, DriveableType type)
        {
            if (type instanceof PlaneType planeType)
            {
                boolean folded = driveable.isWingFolded();
                boolean gear = driveable.isGearDeployed();
                boolean door = driveable.isDoorOpen();
                wingTransform.snap(folded ? planeType.getWingPos2() : planeType.getWingPos1(),
                    folded ? planeType.getWingRot2() : planeType.getWingRot1());
                wingWheelTransform.snap(gear ? planeType.getWingWheelPos1() : planeType.getWingWheelPos2(),
                    gear ? planeType.getWingWheelRot1() : planeType.getWingWheelRot2());
                bodyWheelTransform.snap(gear ? planeType.getBodyWheelPos1() : planeType.getBodyWheelPos2(),
                    gear ? planeType.getBodyWheelRot1() : planeType.getBodyWheelRot2());
                tailWheelTransform.snap(gear ? planeType.getTailWheelPos1() : planeType.getTailWheelPos2(),
                    gear ? planeType.getTailWheelRot1() : planeType.getTailWheelRot2());
                doorTransform.snap(door ? planeType.getDoorPos2() : planeType.getDoorPos1(),
                    door ? planeType.getDoorRot2() : planeType.getDoorRot1());
            }
            else if (type instanceof VehicleType vehicleType)
            {
                boolean door = driveable.isDoorOpen();
                doorTransform.snap(door ? vehicleType.getDoorPos2() : vehicleType.getDoorPos1(),
                    door ? vehicleType.getDoorRot2() : vehicleType.getDoorRot1());
                door2Transform.snap(door ? vehicleType.getDoor2Pos2() : vehicleType.getDoor2Pos1(),
                    door ? vehicleType.getDoor2Rot2() : vehicleType.getDoor2Rot1());
            }
        }

        private void advanceTransforms(Driveable driveable, DriveableType type, int elapsed)
        {
            if (type instanceof PlaneType planeType)
            {
                boolean folded = driveable.isWingFolded();
                boolean gear = driveable.isGearDeployed();
                boolean door = driveable.isDoorOpen();
                wingTransform.advance(folded ? planeType.getWingPos2() : planeType.getWingPos1(),
                    folded ? planeType.getWingRot2() : planeType.getWingRot1(),
                    planeType.getWingRate(), planeType.getWingRotRate(), elapsed);
                wingWheelTransform.advance(gear ? planeType.getWingWheelPos1() : planeType.getWingWheelPos2(),
                    gear ? planeType.getWingWheelRot1() : planeType.getWingWheelRot2(),
                    planeType.getWingWheelRate(), planeType.getWingWheelRotRate(), elapsed);
                bodyWheelTransform.advance(gear ? planeType.getBodyWheelPos1() : planeType.getBodyWheelPos2(),
                    gear ? planeType.getBodyWheelRot1() : planeType.getBodyWheelRot2(),
                    planeType.getBodyWheelRate(), planeType.getBodyWheelRotRate(), elapsed);
                tailWheelTransform.advance(gear ? planeType.getTailWheelPos1() : planeType.getTailWheelPos2(),
                    gear ? planeType.getTailWheelRot1() : planeType.getTailWheelRot2(),
                    planeType.getTailWheelRate(), planeType.getTailWheelRotRate(), elapsed);
                doorTransform.advance(door ? planeType.getDoorPos2() : planeType.getDoorPos1(),
                    door ? planeType.getDoorRot2() : planeType.getDoorRot1(),
                    planeType.getDoorRate(), planeType.getDoorRotRate(), elapsed);
            }
            else if (type instanceof VehicleType vehicleType)
            {
                boolean door = driveable.isDoorOpen();
                doorTransform.advance(door ? vehicleType.getDoorPos2() : vehicleType.getDoorPos1(),
                    door ? vehicleType.getDoorRot2() : vehicleType.getDoorRot1(),
                    vehicleType.getDoorRate(), vehicleType.getDoorRotRate(), elapsed);
                door2Transform.advance(door ? vehicleType.getDoor2Pos2() : vehicleType.getDoor2Pos1(),
                    door ? vehicleType.getDoor2Rot2() : vehicleType.getDoor2Rot1(),
                    vehicleType.getDoor2Rate(), vehicleType.getDoor2RotRate(), elapsed);
            }
        }

        private void updateLegTargets(DriveableType type, float phase, int elapsed)
        {
            if (!(type instanceof MechaType mechaType))
                return;
            legAnimation.beginTick();
            for (MechaType.LegNode node : mechaType.getLegNodes())
            {
                if (node != null && phase >= node.lowerBound() && phase <= node.upperBound())
                    legAnimation.setTarget(node.legPart() - 1, node.rotation(), node.speed());
            }
            legAnimation.approachTargets(elapsed);
        }

        private static float targetSteering(Driveable driveable)
        {
            return inputAxis(driveable.getInputMask(), DriveableInput.RIGHT, DriveableInput.LEFT) * 20F;
        }
    }
}
