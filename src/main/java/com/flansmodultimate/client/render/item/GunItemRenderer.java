package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunItemRenderer
{
    private static final Vector3f VEC3_ZERO = new Vector3f(0F, 0F, 0F);
    private static final Vector3f VEC3_SWITCH_POS1 = new Vector3f(0, -0.4f, 0);
    private static final Vector3f VEC3_SWITCH_POS2 = new Vector3f(0, 0, 0);
    private static final Vector3f VEC3_SWITCH_START_ANGLES = new Vector3f(90, 30, -40);
    private static final Vector3f VEC3_SWITCH_END_ANGLES = new Vector3f(0, 0, 0);
    private static final Vector3f VEC3_SPRINT_DEFAULT_TRANSLATE = new Vector3f(0, 0F, -0.2F);
    private static final Vector3f VEC3_SPRINT_DEFAULT_ROTATION = new Vector3f(-15F, 45F, -10F);
    private static final Vector3f VEC3_LOOK_IDLE_POS = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f VEC3_LOOK1_POS = new Vector3f(0.25f, 0.25f, 0.0f);
    private static final Vector3f VEC3_LOOK2_POS = new Vector3f(0.25f, 0.25f, -0.5f);
    private static final Vector3f VEC3_LOOK_IDLE_ANGLES = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f VEC3_LOOK1_ANGLES = new Vector3f(0.0f, 70.0f, 0.0f);
    private static final Vector3f VEC3_LOOK2_ANGLES = new Vector3f(0.0f, -60.0f, 60.0f);

    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (stack.getItem() instanceof GunItem gunItem && gunItem.useCustomRenderer(context) && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
        {
            GunItemRenderer.renderItem(modelGun, stack, context, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
    }

    public static void renderItem(ModelGun model, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        poseStack.pushPose();
        GunAnimations animations = ModClient.getGunAnimations(ctx);
        model.setReloadRotate(0F);

        if (shouldRenderGun(model, ctx, stack))
        {
            switch (ctx)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> applyFirstPersonAdjustments(model, animations, stack, poseStack, ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> applyThirdPersonAdjustments(model, animations, stack, poseStack, ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                case FIXED -> applyFixedEntityAdjustments(model, poseStack);
                case GROUND -> poseStack.translate(model.getItemFrameOffset().x, model.getItemFrameOffset().y, model.getItemFrameOffset().z);
                default ->
                {
                    // no-op
                }
            }
            renderGunContents(model, stack, animations, ctx, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * Render a gun inside another model's transform hierarchy. Unlike normal
     * item rendering, this deliberately applies no player, GUI or dropped-item
     * transform, while retaining paintjobs, attachments and animated model parts.
     */
    public static void renderEmbedded(ModelGun model, ItemStack stack, GunAnimations animations,
                                      PoseStack poseStack, MultiBufferSource buffer,
                                      int packedLight, int packedOverlay)
    {
        if (model == null || stack.isEmpty() || !(stack.getItem() instanceof GunItem))
            return;

        poseStack.pushPose();
        model.setReloadRotate(0F);
        renderGunContents(model, stack, animations == null ? new GunAnimations() : animations,
            null, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderGunContents(ModelGun model, ItemStack stack, GunAnimations animations,
                                          @Nullable ItemDisplayContext ctx, PoseStack poseStack,
                                          MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        int color = model.getType().getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float modelScale = model.getType().getModelScale();
        ResourceLocation gunTexture = model.getType().getPaintjob(stack).getTexture();

        final int numRounds = countRoundsInGun(stack);
        if (model.isSlideLockOnEmpty())
        {
            if (numRounds == 0)
                animations.onGunEmpty(true);
            else if (!animations.isReloading())
                animations.onGunEmpty(false);
        }

        boolean firstPerson = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean firstPersonRight = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        poseStack.pushPose();
        if (firstPerson)
        {
            handleGunRecoil(model, animations, stack, poseStack);

            if (firstPersonRight)
                renderFirstPersonArm(model, animations, poseStack, buffer, packedLight);

            //This allows you to offset your gun with a sight attached to properly align the aiming reticle
            AttachmentType scopeAttachment = model.getType().getScope(stack);
            if (model.getGunOffset() != 0 && ModClient.getZoomProgress() >= 0.5F && scopeAttachment != null
                && ModelCache.getOrLoadTypeModel(scopeAttachment) instanceof ModelAttachment scopeModel)
            {
                poseStack.translate(0F, -scopeModel.getRenderOffset() + model.getGunOffset() / 16F, 0F);
            }
        }
        poseStack.scale(modelScale, modelScale, modelScale);
        renderFlash(model, stack, animations, poseStack, buffer, packedOverlay);
        boolean translucent = ModClientConfig.get().useTranslucentRendering(model.getType());
        boolean cull = ModClientConfig.get().useCullingRendering(model.getType());
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            renderGunAndComponents(model, stack, animations, numRounds, poseStack,
                buffer.getBuffer(renderPass.getRenderType(gunTexture, translucent, cull)),
                packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
        }
        if (firstPersonRight)
            renderAnimArm(model, animations, poseStack, buffer, packedLight);
        renderAttachmentAmmo(model, stack, animations, numRounds, poseStack, buffer, packedLight, packedOverlay);
        renderCasingEjection(model, animations, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        renderMuzzleFlash(model, stack, animations, poseStack, buffer, packedOverlay);
        renderCustomAttachments(model, stack, animations, poseStack, buffer, packedLight, packedOverlay);
    }

    private static boolean shouldRenderGun(ModelGun model, ItemDisplayContext itemDisplayContext, ItemStack item)
    {
        if (itemDisplayContext.firstPerson())
            return !(ModClient.getZoomProgress() > 0.9F && model.getType().getCurrentScope(item).hasZoomOverlay() && !model.isStillRenderGunWhenScopedOverlay());
        return true;
    }

    private static boolean shouldRenderAmmo(GunAnimations animations, EnumAnimationType anim, int numRounds)
    {
        return numRounds != 0 || animations.isReloading() || (anim != EnumAnimationType.END_LOADED && anim != EnumAnimationType.BACK_LOADED);
    }

    private static void applyFixedEntityAdjustments(ModelGun model, PoseStack poseStack)
    {
        poseStack.translate(0.2F + model.getItemFrameOffset().x, -0.2F + model.getItemFrameOffset().y, model.getItemFrameOffset().z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    private static void applyThirdPersonAdjustments(ModelGun model, GunAnimations animations, ItemStack stack, PoseStack poseStack, boolean leftHand)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.translate(-0.08F, -0.12F, 0F);
        poseStack.translate(model.getThirdPersonOffset().x, model.getThirdPersonOffset().y, model.getThirdPersonOffset().z);

        if (ModClientConfig.get().enableGunAnimationsInThirdPerson)
        {
            renderMeleeMovement(model.getType(), animations, poseStack);
            renderSpinningCocking(model, animations, poseStack);
            renderReloadMovement(model, animations, stack, leftHand, poseStack);
        }
    }

    private static void applyFirstPersonAdjustments(ModelGun model, GunAnimations animations, ItemStack stack, PoseStack poseStack, boolean leftHand)
    {
        float adsSwitch = ModClient.getLastZoomProgress() + (ModClient.getZoomProgress() - ModClient.getLastZoomProgress()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        boolean crouching = ModClient.getZoomProgress() + 0.1F > 0.9F && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCrouching() && !animations.isReloading();
        boolean sprinting = ModClient.getZoomProgress() + 0.1F < 0.2F && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSprinting() && !animations.isReloading() && model.isFancyStance();

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        if (leftHand)
        {
            poseStack.translate(0.25F, -0.05F, 0.155F);
        }
        else
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-5F * adsSwitch));
            poseStack.translate(-0.25F, -0.05F + 0.175F * adsSwitch, -0.155F - 0.405F * adsSwitch);
            if (model.getType().hasZoomOverlay() && !model.isStillRenderGunWhenScopedOverlay())
            {
                poseStack.translate(-0.3F * adsSwitch, 0F, 0F);
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(4.5F * adsSwitch));
            poseStack.translate(crouching ? model.getCrouchZoom() : 0F, -0.03F * adsSwitch, 0F);
        }

        renderWeaponSwitchMovement(animations, poseStack);
        renderSprintingMovement(model, animations, sprinting, poseStack);
        renderMeleeMovement(model.getType(), animations, poseStack);
        renderLookAtGunMovement(animations, poseStack);
        renderSpinningCocking(model, animations, poseStack);
        renderReloadMovement(model, animations, stack, leftHand, poseStack);
    }

    private static void renderWeaponSwitchMovement(GunAnimations animations, PoseStack poseStack)
    {
        if (animations.getSwitchAnimationProgress() <= 0F || animations.getSwitchAnimationLength() <= 0F)
            return;

        float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float interp = (animations.getSwitchAnimationProgress() + frameTime) / animations.getSwitchAnimationLength();

        poseStack.translate(VEC3_SWITCH_POS2.x + (VEC3_SWITCH_POS2.x - VEC3_SWITCH_POS1.x) * interp, VEC3_SWITCH_POS1.y + (VEC3_SWITCH_POS2.y - VEC3_SWITCH_POS1.y) * interp, VEC3_SWITCH_POS1.z + (VEC3_SWITCH_POS2.z - VEC3_SWITCH_POS1.z) * interp);
        poseStack.mulPose(Axis.YP.rotationDegrees(VEC3_SWITCH_START_ANGLES.y + (VEC3_SWITCH_END_ANGLES.y - VEC3_SWITCH_START_ANGLES.y) * interp));
        poseStack.mulPose(Axis.ZP.rotationDegrees(VEC3_SWITCH_START_ANGLES.z + (VEC3_SWITCH_END_ANGLES.z - VEC3_SWITCH_START_ANGLES.z) * interp));
    }

    private static void renderSprintingMovement(ModelGun model, GunAnimations animations, boolean sprinting, PoseStack poseStack)
    {
        if (sprinting && animations.getStanceTimer() == 0 && ModClientConfig.get().enableWeaponSprintStance)
        {
            if (animations.getRunningStanceAnimationProgress() == 0F)
                animations.setRunningStanceAnimationProgress(1F);

            Vector3f configuredTranslate = model.getSprintStanceTranslate();
            Vector3f configuredRotation = model.getSprintStanceRotate();

            float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            float progress = (animations.getRunningStanceAnimationProgress() + frameTime) / animations.getRunningStanceAnimationLength();
            if (animations.getRunningStanceAnimationProgress() == animations.getRunningStanceAnimationLength())
                progress = 1;

            Vector3f translateToUse = VEC3_SPRINT_DEFAULT_TRANSLATE;
            Vector3f rotationToUse = VEC3_SPRINT_DEFAULT_ROTATION;

            if (ModClientConfig.get().enableRandomSprintStance)
            {
                animations.updateSprintStance(model.getType());
                rotationToUse = animations.getSprintingStance();
            }

            boolean hasCustomTranslate = !Objects.equals(configuredTranslate, VEC3_ZERO);
            boolean hasCustomRotation = !Objects.equals(configuredRotation, VEC3_ZERO);

            if (hasCustomTranslate)
                poseStack.translate(configuredTranslate.x * progress, configuredTranslate.y * progress, configuredTranslate.z * progress);
            else
                poseStack.translate(translateToUse.x * progress, translateToUse.y * progress, translateToUse.z * progress);

            Vector3f rot = hasCustomRotation ? configuredRotation : rotationToUse;
            poseStack.mulPose(Axis.XP.rotationDegrees(rot.x * progress));
            poseStack.mulPose(Axis.YP.rotationDegrees(rot.y * progress));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot.z * progress));
        }
        else
        {
            animations.setRunningStanceAnimationProgress(0F);
        }
    }

    private static void renderMeleeMovement(GunType gunType, GunAnimations animations, PoseStack poseStack)
    {
        int progress = animations.getMeleeAnimationProgress();
        if (progress <= 0 || progress >= gunType.getMeleePath().size())
            return;

        float t = Mth.clamp(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), 0.0f, 1.0f);

        Vector3f p0 = gunType.getMeleePath().get(progress);
        Vector3f p1 = (progress + 1 < gunType.getMeleePath().size()) ? gunType.getMeleePath().get(progress + 1) : new Vector3f();

        float x = Mth.lerp(t, p0.x, p1.x);
        float y = Mth.lerp(t, p0.y, p1.y);
        float z = Mth.lerp(t, p0.z, p1.z);

        poseStack.translate(x, y, z);

        Vector3f a0 = gunType.getMeleePathAngles().get(progress);
        Vector3f a1 = (progress + 1 < gunType.getMeleePathAngles().size()) ? gunType.getMeleePathAngles().get(progress + 1) : new Vector3f();

        float yaw = Mth.lerp(t, a0.y, a1.y);
        float roll = Mth.lerp(t, a0.z, a1.z);
        float pitch = Mth.lerp(t, a0.x, a1.x);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private static void renderLookAtGunMovement(GunAnimations animations, PoseStack poseStack)
    {
        float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float interp = animations.getLookAtTimer() + frameTime;
        interp /= animations.getLookAt().getTime();

        Vector3f startPos;
        Vector3f endPos;
        Vector3f startAngles;
        Vector3f endAngles;

        switch (animations.getLookAt())
        {
            case NONE -> {
                startPos = endPos = VEC3_LOOK_IDLE_POS;
                startAngles = endAngles = VEC3_LOOK_IDLE_ANGLES;
            }
            case LOOK1 -> {
                startPos = endPos = VEC3_LOOK1_POS;
                startAngles = endAngles = VEC3_LOOK1_ANGLES;
            }
            case LOOK2 -> {
                startPos = endPos = VEC3_LOOK2_POS;
                startAngles = endAngles = VEC3_LOOK2_ANGLES;
            }
            case TILT1 -> {
                startPos = VEC3_LOOK_IDLE_POS;
                startAngles = VEC3_LOOK_IDLE_ANGLES;
                endPos = VEC3_LOOK1_POS;
                endAngles = VEC3_LOOK1_ANGLES;
            }
            case TILT2 -> {
                startPos = VEC3_LOOK1_POS;
                startAngles = VEC3_LOOK1_ANGLES;
                endPos = VEC3_LOOK2_POS;
                endAngles = VEC3_LOOK2_ANGLES;
            }
            case UNTILT -> {
                startPos = VEC3_LOOK2_POS;
                startAngles = VEC3_LOOK2_ANGLES;
                endPos = VEC3_LOOK_IDLE_POS;
                endAngles = VEC3_LOOK_IDLE_ANGLES;
            }
            default -> {
                return;
            }
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(startAngles.y + (endAngles.y - startAngles.y) * interp));
        poseStack.mulPose(Axis.ZP.rotationDegrees(startAngles.z + (endAngles.z - startAngles.z) * interp));
        poseStack.translate(startPos.x + (endPos.x - startPos.x) * interp, startPos.y + (endPos.y - startPos.y) * interp, startPos.z + (endPos.z - startPos.z) * interp);
    }

    private static void renderSpinningCocking(ModelGun model, GunAnimations animations, PoseStack poseStack)
    {
        if(!model.isSpinningCocking())
            return;

        poseStack.translate(model.getSpinPoint().x, model.getSpinPoint().y, model.getSpinPoint().z);
        float pumped = (animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pumped * 180F + 180F));
        poseStack.translate(-model.getSpinPoint().x, -model.getSpinPoint().y, -model.getSpinPoint().z);
    }

    private static void renderReloadMovement(ModelGun model, GunAnimations animations, ItemStack stack, boolean leftHand, PoseStack poseStack)
    {
        if (!animations.isReloading())
            return;

        int flip = leftHand ? -1 : 1;

        EnumAnimationType anim = model.getAnimationType();
        AttachmentType gripAttachment = model.getType().getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;

        if (gripModel != null && model.getType().getSecondaryFire(stack))
            anim = gripModel.getSecondaryAnimType();

        // Calculate the amount of tilt required for the reloading animation
        float reloadRotate = getReloadRotate(model, animations);

        // Rotate/translate the GUN dependent on the animation type
        switch (anim)
        {
            case BOTTOM_CLIP, PISTOL_CLIP, SHOTGUN, END_LOADED ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(60F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(30F * reloadRotate * flip));
                poseStack.translate(0.25F * reloadRotate, 0F, 0F);
            }
            case CUSTOMBOTTOM_CLIP, CUSTOMPISTOL_CLIP, CUSTOMSHOTGUN, CUSTOMEND_LOADED, CUSTOMBACK_LOADED, CUSTOMBULLPUP, CUSTOMRIFLE, CUSTOMRIFLE_TOP, CUSTOMREVOLVER, CUSTOMREVOLVER2, CUSTOMALT_PISTOL_CLIP, CUSTOMSTRIKER, CUSTOMGENERIC, CUSTOM ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(model.getRotateGunVertical() * reloadRotate));
                poseStack.mulPose(Axis.YP.rotationDegrees(model.getRotateGunHorizontal() * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(model.getTiltGun() * reloadRotate));
                poseStack.translate(model.getTranslateGun().x * reloadRotate, model.getTranslateGun().y * reloadRotate, model.getTranslateGun().z * reloadRotate);
            }
            case BACK_LOADED ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-75F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(-30F * reloadRotate * flip));
                poseStack.translate(0.5F * reloadRotate, 0F, 0F);
            }
            case BULLPUP ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(70F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(10F * reloadRotate * flip));
                poseStack.translate(0.5F * reloadRotate, -0.2F * reloadRotate, 0F);
            }
            case RIFLE ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(30F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(-30F * reloadRotate * flip));
                poseStack.translate(0.5F * reloadRotate, 0F, -0.5F * reloadRotate);
            }
            case RIFLE_TOP, REVOLVER ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(30F * reloadRotate));
                poseStack.mulPose(Axis.YP.rotationDegrees(10F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10F * reloadRotate * flip));
                poseStack.translate(0.1F * reloadRotate, -0.2F * reloadRotate, -0.1F * reloadRotate);
            }
            case REVOLVER2 ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(20F * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10F * reloadRotate * flip));
            }
            case ALT_PISTOL_CLIP ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(60F * reloadRotate * flip));
                poseStack.translate(0.15F * reloadRotate, 0.25F * reloadRotate, 0F);
            }
            case STRIKER ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-35F * reloadRotate * flip));
                poseStack.translate(0.2F * reloadRotate, 0F, -0.1F * reloadRotate);
            }
            case GENERIC ->
            {
                // Gun reloads partly or completely off-screen.
                poseStack.mulPose(Axis.ZP.rotationDegrees(45F * reloadRotate));
                poseStack.translate(-0.2F * reloadRotate, -0.5F * reloadRotate, 0F);
            }
            default ->
            {
                // no-op
            }
        }
    }

    private static void handleGunRecoil(ModelGun model, GunAnimations animations, ItemStack stack, PoseStack poseStack)
    {
        float recoilDistance = getRecoilDistance(model, stack);
        float recoilAngle = getRecoilAngle(model, stack);
        float min = -1.5f;
        float max = 1.5f;
        float randomNum = GunAnimations.random.nextFloat();
        float result = min + (randomNum * (max - min));
        float smoothing = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        poseStack.translate(-(animations.getLastGunRecoil() + (animations.getGunRecoil() - animations.getLastGunRecoil()) * smoothing) * recoilDistance, 0F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(animations.getLastGunRecoil() + (animations.getGunRecoil() - animations.getLastGunRecoil()) * smoothing) * recoilAngle));
        poseStack.mulPose(Axis.YP.rotationDegrees(((-animations.getLastGunRecoil() + (animations.getGunRecoil() - animations.getLastGunRecoil()) * smoothing) * result * smoothing * model.getShakeDistance())));
        poseStack.mulPose(Axis.XP.rotationDegrees(((-animations.getLastGunRecoil() + (animations.getGunRecoil() - animations.getLastGunRecoil()) * smoothing) * result * smoothing * model.getShakeDistance())));

        // Do not move gun when there's a pump in the reload
        if (model.getAnimationType() == EnumAnimationType.SHOTGUN && !animations.isReloading())
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing)) * -5F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing)) * 5F));
        }

        if (model.isSingleAction())
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(1 - Math.abs(animations.getLastGunPullback() + (animations.getGunPullback() - animations.getLastGunPullback()) * smoothing)) * -5F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.getLastGunPullback() + (animations.getGunPullback() - animations.getLastGunPullback()) * smoothing)) * 2.5F));
        }
    }

    /** Get the recoil distance, based on ammo type to reload */
    private static float getRecoilDistance(ModelGun model, ItemStack gunStack)
    {
        AttachmentType grip = model.getType().getGrip(gunStack);
        if (grip != null && model.getType().getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.getRecoilDistance();
        else
            return model.getRecoilSlideDistance();
    }

    /** Get the recoil angle, based on ammo type to reload */
    private static float getRecoilAngle(ModelGun model, ItemStack gunStack)
    {
        AttachmentType grip = model.getType().getGrip(gunStack);
        if (grip != null && model.getType().getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.getRecoilAngle();
        else
            return model.getRotateSlideDistance();
    }

    /** Render the gun and default attachment models */
    private static void renderGunAndComponents(ModelGun model, ItemStack stack, GunAnimations animations, int numRounds, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();

        //Get all the attachments that we may need to render
        AttachmentType scopeAttachment = model.getType().getScope(stack);
        AttachmentType barrelAttachment = model.getType().getBarrel(stack);
        AttachmentType stockAttachment = model.getType().getStock(stack);
        AttachmentType gripAttachment = model.getType().getGrip(stack);
        AttachmentType gadgetAttachment = model.getType().getGadget(stack);
        AttachmentType slideAttachment = model.getType().getSlide(stack);
        AttachmentType pumpAttachment = model.getType().getPump(stack);

        model.render(model.getGunModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        model.render(model.getBackpackModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        model.renderCustom(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, animations, renderPass);

        // Render the guns default parts if no attachment is installed
        if (scopeAttachment == null && !model.isScopeIsOnSlide() && !model.isScopeIsOnBreakAction())
            model.render(model.getDefaultScopeModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (barrelAttachment == null)
            model.render(model.getDefaultBarrelModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (stockAttachment == null)
            model.render(model.getDefaultStockModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (gripAttachment == null && !model.isGripIsOnPump())
            model.render(model.getDefaultGripModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (gadgetAttachment == null && !model.isGadgetIsOnPump())
            model.render(model.getDefaultGadgetModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        renderBulletCounterModels(model, numRounds, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSlideModels(model, stack, animations, slideAttachment, scopeAttachment, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderBreakAction(model, scopeAttachment, getReloadRotate(model, animations), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderHammer(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPumpAction(model, animations, pumpAttachment, gripAttachment, gadgetAttachment, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderBoltAction(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderChargeHandle(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderMinigunBarrels(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderRevolverBarrel(model, getReloadRotate(model, animations), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderAmmo(model, animations, stack, numRounds, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderStaticAmmo(model, stack, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        poseStack.popPose();
    }

    private static void renderBulletCounterModels(ModelGun model, int numRounds, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (renderPass != EnumRenderPass.GLOW_ALPHA || (!model.isBulletCounterActive() && !model.isAdvBulletCounterActive()))
            return;

        if (model.isBulletCounterActive() && numRounds < model.getBulletCounterModel().length)
        {
            poseStack.pushPose();
            ModelRendererTurbo part = model.getBulletCounterModel()[numRounds];
            part.glow = true;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            part.glow = false;
            poseStack.popPose();
        }

        if (model.isAdvBulletCounterActive())
        {
            poseStack.pushPose();

            // Number of digit positions available in the model
            final int places = model.getAdvBulletCounterModel().length;

            // Render each digit position
            for (int i = 0; i < places; i++)
            {
                // Pick which decimal place this slot shows
                // If countOnRightHandSide == false: i=0 is most-significant (left)
                // If true: i=0 is least-significant (right)
                final int placeIndex = model.isCountOnRightHandSide() ? i : (places - 1 - i);

                // Extract digit at 10^placeIndex
                int digit = numRounds;
                for (int k = 0; k < placeIndex; k++)
                    digit /= 10;
                digit %= 10;

                ModelRendererTurbo part = model.getAdvBulletCounterModel()[i][digit];
                part.glow = true;
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
                part.glow = false;
            }

            poseStack.popPose();
        }
    }

    private static int countRoundsInGun(ItemStack gunStack)
    {
        final GunItem gunItem = (GunItem) gunStack.getItem();
        final GunType type = gunItem.getConfigType();
        final int slots = type.getNumAmmoItemsInGun(gunStack);
        int rounds = 0;

        for (int i = 0; i < slots; i++)
        {
            final ItemStack bullet = gunItem.getAmmoItemStack(gunStack, i, Minecraft.getInstance().level.registryAccess());
            if (bullet == null || !(bullet.getItem() instanceof ShootableItem))
                continue;

            rounds += ShootableItem.getRoundsRemaining(bullet);
        }

        return rounds;
    }

    private static void renderSlideModels(ModelGun model, ItemStack stack, GunAnimations animations, AttachmentType slideAttachment, AttachmentType scopeAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (slideAttachment == null)
        {
            poseStack.pushPose();
            if (!model.getType().getSecondaryFire(stack))
            {
                poseStack.translate(-(animations.getLastGunSlide() + (animations.getGunSlide() - animations.getLastGunSlide()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) * model.getGunSlideDistance(), 0F, 0F);
                poseStack.translate(-(1 - Math.abs(animations.getLastCharged() + (animations.getCharged() - animations.getLastCharged()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))) * model.getChargeHandleDistance(), 0F, 0F);
            }
            model.render(model.getSlideModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (scopeAttachment == null && model.isScopeIsOnSlide())
                model.render(model.getDefaultScopeModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();

            if (!model.getType().getSecondaryFire(stack))
            {
                poseStack.pushPose();
                poseStack.translate(-(animations.getLastGunSlide() + (animations.getGunSlide() - animations.getLastGunSlide()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) * model.getAltgunSlideDistance(), 0F, 0F);
                model.render(model.getAltslideModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
                poseStack.popPose();
            }
        }
    }

    private static void renderBreakAction(ModelGun model, AttachmentType scopeAttachment, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.getBarrelBreakPoint().x, model.getBarrelBreakPoint().y, model.getBarrelBreakPoint().z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.getBreakAngle()));
        poseStack.translate(-model.getBarrelBreakPoint().x, -model.getBarrelBreakPoint().y, -model.getBarrelBreakPoint().z);
        model.render(model.getBreakActionModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (scopeAttachment == null && model.isScopeIsOnBreakAction())
            model.render(model.getDefaultScopeModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.getAltbarrelBreakPoint().x, model.getAltbarrelBreakPoint().y, model.getAltbarrelBreakPoint().z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.getAltbreakAngle()));
        poseStack.translate(-model.getAltbarrelBreakPoint().x, -model.getAltbarrelBreakPoint().y, -model.getAltbarrelBreakPoint().z);
        model.render(model.getAltbreakActionModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderHammer(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.getHammerSpinPoint().x, model.getHammerSpinPoint().y, model.getHammerSpinPoint().z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.getHammerRotation()));
        poseStack.translate(-model.getHammerSpinPoint().x, -model.getHammerSpinPoint().y, -model.getHammerSpinPoint().z);
        model.render(model.getHammerModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.getAlthammerSpinPoint().x, model.getAlthammerSpinPoint().y, model.getAlthammerSpinPoint().z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.getAlthammerRotation()));
        poseStack.translate(-model.getAlthammerSpinPoint().x, -model.getAlthammerSpinPoint().y, -model.getAlthammerSpinPoint().z);
        model.render(model.getAlthammerModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderPumpAction(ModelGun model, GunAnimations animations, AttachmentType pumpAttachment, AttachmentType gripAttachment, AttachmentType gadgetAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (pumpAttachment == null)
        {
            poseStack.pushPose();
            poseStack.translate(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))) * model.getPumpHandleDistance(), 0F, 0F);
            model.render(model.getPumpModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (gripAttachment == null && model.isGripIsOnPump())
                model.render(model.getDefaultGripModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (gadgetAttachment == null && model.isGadgetIsOnPump())
                model.render(model.getDefaultGadgetModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderBoltAction(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))) * model.getBoltCycleDistance(), 0F, 0F);
        poseStack.translate(model.getBoltRotationOffset().x, model.getBoltRotationOffset().y, model.getBoltRotationOffset().z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))) * model.getBoltRotationAngle()));
        poseStack.translate(-model.getBoltRotationOffset().x, -model.getBoltRotationOffset().y, -model.getBoltRotationOffset().z);
        model.render(model.getBoltActionModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderChargeHandle(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.getChargeHandleDistance() != 0F)
        {
            poseStack.pushPose();
            poseStack.translate(-(1 - Math.abs(animations.getLastCharged() + (animations.getCharged() - animations.getLastCharged()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))) * model.getChargeHandleDistance(), 0F, 0F);
            model.render(model.getChargeModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderMinigunBarrels(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.getType().getMode() == EnumFireMode.MINIGUN)
        {
            poseStack.pushPose();
            poseStack.translate(model.getMinigunBarrelOrigin().x, model.getMinigunBarrelOrigin().y, model.getMinigunBarrelOrigin().z);
            org.joml.Vector3f axis = new org.joml.Vector3f(model.getMinigunBarrelSpinDirection().x, model.getMinigunBarrelSpinDirection().y, model.getMinigunBarrelSpinDirection().z).normalize();
            poseStack.mulPose(Axis.of(axis).rotationDegrees(animations.getMinigunBarrelRotation() * model.getMinigunBarrelSpinSpeed()));
            poseStack.translate(-model.getMinigunBarrelOrigin().x, -model.getMinigunBarrelOrigin().y, -model.getMinigunBarrelOrigin().z);
            model.render(model.getMinigunBarrelModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderRevolverBarrel(ModelGun model, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.getRevolverFlipPoint().x, model.getRevolverFlipPoint().y, model.getRevolverFlipPoint().z);
        poseStack.mulPose(Axis.XP.rotationDegrees(reloadRotate * model.getRevolverFlipAngle()));
        poseStack.translate(-model.getRevolverFlipPoint().x, -model.getRevolverFlipPoint().y, -model.getRevolverFlipPoint().z);
        model.render(model.getRevolverBarrelModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.getRevolverFlipPoint().x, model.getRevolverFlipPoint().y, model.getRevolverFlipPoint().z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-reloadRotate * model.getRevolverFlipAngle()));
        poseStack.translate(-model.getRevolverFlipPoint().x, -model.getRevolverFlipPoint().y, -model.getRevolverFlipPoint().z);
        model.render(model.getRevolver2BarrelModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderStaticAmmo(ModelGun model, ItemStack stack, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.getType().getSecondaryFire(stack))
        {
            poseStack.pushPose();
            model.render(model.getAmmoModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderAmmo(ModelGun model, GunAnimations animations, ItemStack stack, int numRounds, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();

        AttachmentType gripAttachment = model.getType().getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;
        EnumAnimationType anim = model.getAnimationType();

        if (gripModel != null && model.getType().getSecondaryFire(stack))
            anim = gripModel.getSecondaryAnimType();

        boolean shouldRender = shouldRenderAmmo(animations, anim, numRounds);

        // If it should be rendered, do the transformations required
        if (shouldRender && animations.isReloading())
        {
            // Calculate the amount of tilt required for the reloading animation
            float effectiveReloadAnimationProgress = animations.getLastReloadAnimationProgress() + (animations.getReloadAnimationProgress() - animations.getLastReloadAnimationProgress()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            float clipPosition = getClipPosition(model, stack, effectiveReloadAnimationProgress);
            float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - model.getTiltGunTime()) / (model.getUnloadClipTime() + model.getLoadClipTime()))));

            // Rotate the gun dependent on the animation type
            switch (model.getAnimationType())
            {
                case BREAK_ACTION, CUSTOMBREAK_ACTION ->
                {
                    poseStack.translate(model.getBarrelBreakPoint().x, model.getBarrelBreakPoint().y, model.getBarrelBreakPoint().z);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(model.getReloadRotate() * -model.getBreakAngle()));
                    poseStack.translate(-model.getBarrelBreakPoint().x, -model.getBarrelBreakPoint().y, -model.getBarrelBreakPoint().z);
                    poseStack.translate(-model.getBreakActionAmmoDistance() * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case REVOLVER, CUSTOMREVOLVER ->
                {
                    poseStack.translate(model.getRevolverFlipPoint().x, model.getRevolverFlipPoint().y, model.getRevolverFlipPoint().z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.getReloadRotate() * model.getRevolverFlipAngle()));
                    poseStack.translate(-model.getRevolverFlipPoint().x, -model.getRevolverFlipPoint().y, -model.getRevolverFlipPoint().z);
                    poseStack.translate(-1F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case REVOLVER2, CUSTOMREVOLVER2 ->
                {
                    poseStack.translate(model.getRevolver2FlipPoint().x, model.getRevolver2FlipPoint().y, model.getRevolver2FlipPoint().z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.getReloadRotate() * model.getRevolver2FlipAngle()));
                    poseStack.translate(-model.getRevolver2FlipPoint().x, -model.getRevolver2FlipPoint().y, -model.getRevolver2FlipPoint().z);
                    poseStack.translate(-1F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case BOTTOM_CLIP, CUSTOMBOTTOM_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case PISTOL_CLIP, CUSTOMPISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90F * clipPosition * clipPosition));
                    poseStack.translate(0F, -1F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case ALT_PISTOL_CLIP, CUSTOMALT_PISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(5F * clipPosition));
                    poseStack.translate(0F, -3F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case SIDE_CLIP, CUSTOMSIDE_CLIP ->
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F * clipPosition));
                    poseStack.mulPose(Axis.YP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case BULLPUP, CUSTOMBULLPUP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-150F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(clipPosition, -0.5F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case P90, CUSTOMP90 ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * model.getReloadRotate() * model.getReloadRotate()));
                    poseStack.translate(0F, 0.075F * model.getReloadRotate(), 0F);
                    poseStack.translate(-2F * clipPosition / model.getType().getModelScale(), -0.3F * clipPosition / model.getType().getModelScale(), 0.5F * clipPosition / model.getType().getModelScale());
                }
                case RIFLE ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                    poseStack.translate(bulletProgress * -1F / model.getType().getModelScale(), 0F, bulletProgress * 0.5F / model.getType().getModelScale());
                }
                case CUSTOMRIFLE ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * model.getRotateClipVertical()));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * model.getRotateClipHorizontal()));
                    poseStack.mulPose(Axis.XP.rotationDegrees(bulletProgress * model.getTiltClip()));
                    poseStack.translate(bulletProgress * model.getTranslateClip().x / model.getType().getModelScale(), bulletProgress * model.getTranslateClip().y / model.getType().getModelScale(), bulletProgress * model.getTranslateClip().z / model.getType().getModelScale());
                }
                case RIFLE_TOP, CUSTOMRIFLE_TOP ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                    poseStack.translate(bulletProgress * -0.1F / model.getType().getModelScale(), bulletProgress / model.getType().getModelScale(), bulletProgress * 0.5F / model.getType().getModelScale());
                }
                case SHOTGUN, STRIKER, CUSTOMSHOTGUN, CUSTOMSTRIKER ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                    poseStack.translate(bulletProgress * -0.5F * 1 / model.getType().getModelScale(), bulletProgress * -1F * 1 / model.getType().getModelScale(), 0F);
                }
                case CUSTOM ->
                {
                    // Staged reload allows you to change the animation route halfway through
                    if (effectiveReloadAnimationProgress > 0.5 && model.isStagedReload())
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.getStagedrotateClipVertical() * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.getStagedrotateClipHorizontal() * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.getStagedtiltClip() * clipPosition));
                        poseStack.translate(model.getStagedtranslateClip().x * clipPosition / model.getType().getModelScale(), model.getStagedtranslateClip().y * clipPosition / model.getType().getModelScale(), model.getStagedtranslateClip().z * clipPosition / model.getType().getModelScale());
                    }
                    else
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.getRotateClipVertical() * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.getRotateClipHorizontal() * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.getTiltClip() * clipPosition));
                        poseStack.translate(model.getTranslateClip().x * clipPosition / model.getType().getModelScale(), model.getTranslateClip().y * clipPosition / model.getType().getModelScale(), model.getTranslateClip().z * clipPosition / model.getType().getModelScale());
                    }
                }
                case END_LOADED, CUSTOMEND_LOADED ->
                {
                    float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-45F * dYaw));
                    poseStack.translate(-getEndLoadedDistance(model, gripAttachment, stack) * dYaw, -0.5F * dYaw, 0F);

                    float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
                    poseStack.translate(getEndLoadedDistance(model, gripAttachment, stack) * xDisplacement, 0F, 0F);
                }
                case BACK_LOADED, CUSTOMBACK_LOADED ->
                {
                    float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
                    poseStack.translate(getEndLoadedDistance(model, gripAttachment, stack) * dYaw, -0.5F * dYaw, 0F);

                    float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
                    poseStack.translate(-getEndLoadedDistance(model, gripAttachment, stack) * xDisplacement, 0F, 0F);
                }
                default ->
                {
                    // no-op
                }
            }
        }

        if (shouldRender && gripAttachment == null || !model.getType().getSecondaryFire(stack))
            model.render(model.getAmmoModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        // Renders fullammo model for 2nd half of reload animation
        float effectiveReloadAnimationProgress = animations.getLastReloadAnimationProgress() + (animations.getReloadAnimationProgress() - animations.getLastReloadAnimationProgress()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        if (effectiveReloadAnimationProgress > 0.5)
            model.render(model.getFullammoModel(), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        poseStack.popPose();
    }

    public static float getClipPosition(ModelGun model, ItemStack stack, float effectiveReloadAnimationProgress)
    {
        AttachmentType gripAttachment = model.getType().getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;

        float tiltGunTime = model.getTiltGunTime();
        float unloadClipTime = model.getUnloadClipTime();
        float loadClipTime = model.getLoadClipTime();

        if (gripModel != null && model.getType().getSecondaryFire(stack))
        {
            tiltGunTime = gripModel.getTiltGunTime();
            unloadClipTime = gripModel.getUnloadClipTime();
            loadClipTime = gripModel.getLoadClipTime();
        }

        float clipPosition = 0F;
        if (effectiveReloadAnimationProgress > tiltGunTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime)
            clipPosition = (effectiveReloadAnimationProgress - tiltGunTime) / unloadClipTime;
        if (effectiveReloadAnimationProgress >= tiltGunTime + unloadClipTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime + loadClipTime)
            clipPosition = 1F - (effectiveReloadAnimationProgress - (tiltGunTime + unloadClipTime)) / loadClipTime;
        return clipPosition;
    }

    /** Get the end loaded distance, based on ammo type to reload */
    public static float getEndLoadedDistance(ModelGun model, @Nullable AttachmentType grip, ItemStack gunStack)
    {
        if (grip != null && model.getType().getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.getEndLoadedAmmoDistance();
        else
            return model.getEndLoadedAmmoDistance();
    }

    /** Get the number of bullets to reload in animation, based on ammo type to reload */
    public static float getNumBulletsInReload(ModelGun model, GunAnimations animations)
    {
        // If this is a singles reload, we want to know the number of bullets already in the gun
        if (animations.isSinglesReload())
            return animations.getReloadAmmoCount();
        else
            return model.getNumBulletsInReloadAnimation();
    }

    public static float getReloadRotate(ModelGun model, GunAnimations animations)
    {
        float reloadRotate = 1F;

        // Snap to zero if reload is finished. Otherwise, weird behaviour.
        if (!animations.isReloading())
            return 0F;

        float effectiveReloadAnimationProgress = animations.getLastReloadAnimationProgress() + (animations.getReloadAnimationProgress() - animations.getLastReloadAnimationProgress()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        if (effectiveReloadAnimationProgress < model.getTiltGunTime())
            reloadRotate = effectiveReloadAnimationProgress / model.getTiltGunTime();
        if (effectiveReloadAnimationProgress > model.getTiltGunTime() + model.getUnloadClipTime() + model.getLoadClipTime())
            reloadRotate = 1F - (effectiveReloadAnimationProgress - (model.getTiltGunTime() + model.getUnloadClipTime() + model.getLoadClipTime())) / model.getUntiltGunTime();

        return reloadRotate;
    }

    private static void renderFlash(ModelGun model, ItemStack item, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay)
    {
        ModelFlash flash = ModelCache.getOrLoadFlashModel(model.getType());
        AttachmentType barrelAttachment = model.getType().getBarrel(item);
        boolean isFlashEnabled = flash != null && (barrelAttachment == null || !barrelAttachment.isDisableMuzzleFlash());

        if (isFlashEnabled && animations.getMuzzleFlashTime() > 0 && !model.getType().getSecondaryFire(item))
        {
            poseStack.pushPose();
            poseStack.scale(model.getFlashScale(), model.getFlashScale(), model.getFlashScale());

            Vector3f base = Objects.requireNonNullElse(model.getMuzzleFlashPoint(), Vector3f.Zero);

            if (barrelAttachment != null && ModelCache.getOrLoadTypeModel(barrelAttachment) instanceof ModelAttachment barrelModel)
            {
                Vector3f muzzleFlashPoint = barrelModel.getMuzzleFlashPoint(base, model.getBarrelAttachPoint());
                poseStack.translate(muzzleFlashPoint.x, muzzleFlashPoint.y, muzzleFlashPoint.z);
            }
            else
            {
                Vector3f defaultOffset = Objects.requireNonNullElse(model.getDefaultBarrelFlashPoint(), Vector3f.Zero);
                poseStack.translate(base.x + defaultOffset.x, base.y + defaultOffset.y, base.z + defaultOffset.z);
            }

            ResourceLocation flashTexture = model.getType().getFlashTexture();
            flash.renderFlash(animations.getFlashInt(), poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(flashTexture)), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F, 1F);
            poseStack.popPose();
        }
    }

    private static void renderAttachmentAmmo(ModelGun model, ItemStack stack, GunAnimations animations, int numRounds, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        AttachmentType gripAttachment = model.getType().getGrip(stack);
        ItemStack gripItemStack = model.getType().getGripItemStack(stack);

        if (gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment gripModel)
        {
            int color = gripAttachment.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            float modelScale = gripAttachment.getModelScale();
            ResourceLocation ammoTexture = gripAttachment.getPaintjob(gripItemStack).getTexture();

            if (shouldRenderAmmo(animations, model.getAnimationType(), numRounds) || !model.getType().getSecondaryFire(stack))
            {
                boolean translucent = ModClientConfig.get().useTranslucentRendering(gripAttachment);
                boolean cull = ModClientConfig.get().useCullingRendering(gripAttachment);
                for (EnumRenderPass renderPass : ModelCache.getRenderPasses(gripModel))
                    gripModel.renderAttachmentAmmo(poseStack, buffer.getBuffer(renderPass.getRenderType(ammoTexture, translucent, cull)), packedLight, packedOverlay, red, green, blue, 1F, modelScale, renderPass);
            }
        }
    }

    private static void renderCasingEjection(ModelGun model, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        ModelCasing casing = ModelCache.getOrLoadCasingModel(model.getType());
        if (casing != null)
        {
            float casingProg = (animations.getLastCasingStage() + (animations.getCasingStage() - animations.getLastCasingStage()) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) / model.getCasingAnimTime();
            if (casingProg >= 1)
                casingProg = 0;
            float moveX = model.getCasingAnimDistance().x + (animations.getCasingRandom().x * model.getCasingAnimSpread().x);
            float moveY = model.getCasingAnimDistance().y + (animations.getCasingRandom().y * model.getCasingAnimSpread().y);
            float moveZ = model.getCasingAnimDistance().z + (animations.getCasingRandom().z * model.getCasingAnimSpread().z);
            poseStack.pushPose();
            poseStack.scale(model.getCaseScale(), model.getCaseScale(), model.getCaseScale());
            poseStack.translate(model.getCasingAttachPoint().x + (casingProg * moveX), model.getCasingAttachPoint().y + (casingProg * moveY), model.getCasingAttachPoint().z + (casingProg * moveZ));
            poseStack.mulPose(Axis.of(new org.joml.Vector3f(model.getCasingRotateVector().x, model.getCasingRotateVector().y, model.getCasingRotateVector().z)).rotationDegrees(casingProg * 180));
            ResourceLocation casingTexture = model.getType().getCasingTexture();
            boolean translucent = ModClientConfig.get().useTranslucentRendering(model.getType());
            boolean cull = ModClientConfig.get().useCullingRendering(model.getType());
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(casing))
                casing.renderCasing(poseStack, buffer.getBuffer(renderPass.getRenderType(casingTexture, translucent, cull)), packedLight, packedOverlay, 1F, 1F, 1F, 1F, 1F, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderMuzzleFlash(ModelGun model, ItemStack stack, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay)
    {
        AttachmentType barrelAttachment = model.getType().getBarrel(stack);
        boolean isMuzzleFlashEnabled = StringUtils.isBlank(model.getType().getFlashModelClassName())
                && (barrelAttachment == null || !barrelAttachment.isDisableMuzzleFlash())
                && (StringUtils.isNotBlank(model.getType().getMuzzleFlashModelClassName()) || model.getClass().getName().contains("com.flansmod.modernweapons.client.model"));

        if (isMuzzleFlashEnabled && animations.getMuzzleFlashTime() > 0 && !model.getType().getSecondaryFire(stack))
        {
            ModelMuzzleFlash muzzleFlash = ModelCache.getOrLoadMuzzleFlashModel(model.getType());
            if (muzzleFlash != null)
            {

                Vector3f mfPoint = Objects.requireNonNullElse(model.getMuzzleFlashPoint(), Objects.requireNonNullElse(model.getBarrelAttachPoint(), Vector3f.Zero));
                if (mfPoint.equals(ModelGun.getInvalid()))
                    mfPoint = model.getBarrelAttachPoint();

                if (barrelAttachment != null && ModelCache.getOrLoadTypeModel(barrelAttachment) instanceof ModelAttachment barrelModel)
                {
                    mfPoint = barrelModel.getMuzzleFlashPoint(mfPoint, model.getBarrelAttachPoint());
                }
                else if (model.getDefaultBarrelFlashPoint() != null)
                {
                    mfPoint = Vector3f.add(model.getMuzzleFlashPoint(), model.getDefaultBarrelFlashPoint(), null);
                }

                poseStack.pushPose();
                poseStack.translate(mfPoint.x * model.getType().getModelScale(), mfPoint.y * model.getType().getModelScale(), mfPoint.z * model.getType().getModelScale());
                muzzleFlash.renderToBuffer(poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(muzzleFlash.getTexture())), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F);
                poseStack.popPose();
            }
        }
    }

    private static void renderCustomAttachments(ModelGun model, ItemStack item, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        float smoothing = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        ItemStack scopeItemStack = model.getType().getScopeItemStack(item);
        ItemStack barrelItemStack = model.getType().getBarrelItemStack(item);
        ItemStack stockItemStack = model.getType().getStockItemStack(item);
        ItemStack gripItemStack = model.getType().getGripItemStack(item);
        ItemStack gadgetItemStack = model.getType().getGadgetItemStack(item);
        ItemStack slideItemStack = model.getType().getSlideItemStack(item);
        ItemStack pumpItemStack = model.getType().getPumpItemStack(item);
        ItemStack accessoryItemStack = model.getType().getAccessoryItemStack(item);

        List<AttachmentType> attachments = model.getType().getCurrentAttachments(item);
        // Get all the attachments that we may need to render
        for (AttachmentType attachment : attachments)
        {
            poseStack.pushPose();

            switch(attachment.getEnumAttachmentType())
            {
                case SIGHTS:
                    preRenderAttachment(attachment, model.getScopeAttachPoint(), poseStack, model.getType().getModelScale());
                    if (model.isScopeIsOnBreakAction())
                    {
                        poseStack.translate(model.getBarrelBreakPoint().x, model.getBarrelBreakPoint().y, model.getBarrelBreakPoint().z);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(getReloadRotate(model, animations) * -model.getBreakAngle()));
                        poseStack.translate(-model.getBarrelBreakPoint().x, -model.getBarrelBreakPoint().y, -model.getBarrelBreakPoint().z);
                    }
                    if (model.isScopeIsOnSlide())
                        poseStack.translate(-(animations.getLastGunSlide() + (animations.getGunSlide() - animations.getLastGunSlide()) * smoothing) * model.getGunSlideDistance(), 0F, 0F);
                    renderAttachment(attachment, scopeItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case GRIP:
                    preRenderAttachment(attachment, model.getGripAttachPoint(), poseStack, model.getType().getModelScale());
                    if (model.isGripIsOnPump())
                        poseStack.translate(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing)) * model.getPumpHandleDistance(), 0F, 0F);
                    renderAttachment(attachment, gripItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case BARREL:
                    preRenderAttachment(attachment, model.getBarrelAttachPoint(), poseStack, model.getType().getModelScale());
                    renderAttachment(attachment, barrelItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case STOCK:
                    preRenderAttachment(attachment, model.getStockAttachPoint(), poseStack, model.getType().getModelScale());
                    renderAttachment(attachment, stockItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case SLIDE:
                    preRenderAttachment(attachment, model.getSlideAttachPoint(), poseStack, model.getType().getModelScale());
                    poseStack.translate(-(animations.getLastGunSlide() + (animations.getGunSlide() - animations.getLastGunSlide()) * smoothing) * model.getGunSlideDistance(), 0F, 0F);
                    renderAttachment(attachment, slideItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case GADGET:
                    preRenderAttachment(attachment, model.getGadgetAttachPoint(), poseStack, model.getType().getModelScale());
                    if (model.isGadgetIsOnPump())
                        poseStack.translate(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing)) * model.getPumpHandleDistance(), 0F, 0F);
                    renderAttachment(attachment, gadgetItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case ACCESSORY:
                    preRenderAttachment(attachment, model.getAccessoryAttachPoint(), poseStack, model.getType().getModelScale());
                    renderAttachment(attachment, accessoryItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case PUMP:
                    preRenderAttachment(attachment, model.getPumpAttachPoint(), poseStack, model.getType().getModelScale());
                    poseStack.translate(-(1 - Math.abs(animations.getLastPumped() + (animations.getPumped() - animations.getLastPumped()) * smoothing)) * model.getPumpHandleDistance(), 0F, 0F);
                    renderAttachment(attachment, pumpItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                default:
                    break;
            }
            poseStack.popPose();
        }
    }

    private static void preRenderAttachment(AttachmentType attachment, Vector3f attachPoint, PoseStack poseStack, float gunModelScale)
    {
        float modelScale = attachment.getModelScale();
        poseStack.translate(attachPoint.x * gunModelScale, attachPoint.y * gunModelScale, attachPoint.z * gunModelScale);
        poseStack.scale(modelScale, modelScale, modelScale);
    }

    public static void renderAttachment(AttachmentType attachment, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (ModelCache.getOrLoadTypeModel(attachment) instanceof ModelAttachment modelAttachment)
        {
            int color = attachment.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            ResourceLocation attachmentTexture = attachment.getPaintjob(stack).getTexture();
            boolean translucent = ModClientConfig.get().useTranslucentRendering(modelAttachment.getType());
            boolean cull = ModClientConfig.get().useCullingRendering(modelAttachment.getType());
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(modelAttachment))
                modelAttachment.renderAttachment(poseStack, buffer.getBuffer(renderPass.getRenderType(attachmentTexture, translucent, cull)), packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
        }
    }

    private static void renderFirstPersonArm(ModelGun model, GunAnimations anim, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!ModClientConfig.get().enableArms || !model.isHasArms())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        float smoothing = mc.getTimer().getGameTimeDeltaPartialTick(true);
        PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        ResourceLocation skin = player.getSkin().texture();
        RenderType rt = RenderType.entitySolid(skin); // or entityTranslucent if you need alpha
        VertexConsumer vc = buffer.getBuffer(rt);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        playerModel.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        playerModel.leftArmPose  = HumanoidModel.ArmPose.EMPTY;
        playerModel.crouching = false;
        playerModel.swimAmount = 0.0F;
        playerModel.attackTime = 0.0F;
        playerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        poseStack.pushPose();
        if (!anim.isReloading() && model.isRighthandPump())
            renderArmPump(model, anim, smoothing, model.getRightArmRot(), model.getRightArmPos(), poseStack);
        else if (anim.getCharged() < 0.9 && model.isLeftHandAmmo() && model.isRightHandCharge() && anim.getCharged() != -1.0F)
            renderArmCharge(model, anim, smoothing, model.getRightArmChargeRot(), model.getRightArmChargePos(), poseStack);
        else if (anim.getPumped() < 0.9 && model.isRightHandBolt() && model.isLeftHandAmmo())
            renderArmBolt(model, anim, smoothing, model.getRightArmChargeRot(), model.getRightArmChargePos(), poseStack);
        else if (!anim.isReloading())
            renderArmDefault(model, model.getRightArmRot(), model.getRightArmPos(), poseStack);
        else
            renderArmDefault(model, model.getRightArmReloadRot(), model.getRightArmReloadPos(), poseStack);
        poseStack.scale(model.getRightArmScale().x, model.getRightArmScale().y, model.getRightArmScale().z);
        if (!model.isRightHandAmmo())
            playerModel.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        if (!anim.isReloading() && model.isLefthandPump())
            renderArmPump(model, anim, smoothing, model.getLeftArmRot(), model.getLeftArmPos(), poseStack);
        else if (anim.getCharged() < 0.9 && model.isRightHandCharge() && model.isLeftHandAmmo() && anim.getCharged() != -1.0F)
            renderArmCharge(model, anim, smoothing, model.getLeftArmChargeRot(), model.getLeftArmChargePos(), poseStack);
        else if (anim.getPumped() < 0.9 && model.isRightHandBolt() && model.isLeftHandAmmo())
            renderArmBolt(model, anim, smoothing, model.getLeftArmChargeRot(), model.getLeftArmChargePos(), poseStack);
        else if (!anim.isReloading())
            renderArmDefault(model, model.getLeftArmRot(), model.getLeftArmPos(), poseStack);
        else
            renderArmDefault(model, model.getLeftArmReloadRot(), model.getLeftArmReloadPos(), poseStack);
        poseStack.scale(model.getLeftArmScale().x, model.getLeftArmScale().y, model.getLeftArmScale().z);
        if (!model.isLeftHandAmmo())
            playerModel.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderAnimArm(ModelGun model, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!ModClientConfig.get().enableArms || !model.isHasArms())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        float smoothing = mc.getTimer().getGameTimeDeltaPartialTick(true);
        PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        ResourceLocation skin = player.getSkin().texture();
        RenderType rt = RenderType.entitySolid(skin); // or entityTranslucent if you need alpha
        VertexConsumer vc = buffer.getBuffer(rt);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        playerModel.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        playerModel.leftArmPose  = HumanoidModel.ArmPose.EMPTY;
        playerModel.crouching = false;
        playerModel.swimAmount = 0.0F;
        playerModel.attackTime = 0.0F;
        playerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.scale(1F / model.getType().getModelScale(), 1F / model.getType().getModelScale(), 1F / model.getType().getModelScale());

        poseStack.pushPose();
        float effectiveReloadAnimationProgress = animations.getLastReloadAnimationProgress() + (animations.getReloadAnimationProgress() - animations.getLastReloadAnimationProgress()) * smoothing;
        if (animations.getCharged() < 0.9 && model.isRightHandCharge() && model.isRightHandAmmo() && animations.getCharged() != -1.0F)
            renderArmPump(model, animations, smoothing, model.getRightArmRot(), model.getRightArmPos(), poseStack);
        else if (animations.getPumped() < 0.9 && model.isRightHandBolt() && model.isRightHandAmmo())
            renderArmBolt(model, animations, smoothing, model.getRightArmChargeRot(), model.getRightArmChargePos(), poseStack);
        else if (!animations.isReloading())
            renderArmDefault(model, model.getRightArmRot(), model.getRightArmPos(), poseStack);
        else
            renderArmDefault(model, model.getRightArmReloadRot(), model.getRightArmReloadPos(), poseStack);
        poseStack.scale(model.getRightArmScale().x, model.getRightArmScale().y, model.getRightArmScale().z);
        if (model.isRightHandAmmo())
            playerModel.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        if (animations.getCharged() < 0.9 && model.isLeftHandCharge() && model.isLeftHandAmmo() && animations.getCharged() != -1.0F)
            renderArmCharge(model, animations, smoothing, model.getLeftArmChargeRot(), model.getLeftArmChargePos(), poseStack);
        else if (!animations.isReloading() && model.isLefthandPump())
            renderArmPump(model, animations, smoothing, model.getLeftArmRot(), model.getLeftArmPos(), poseStack);
        else if (!animations.isReloading())
            renderArmDefault(model, model.getLeftArmRot(), model.getLeftArmPos(), poseStack);
        else if (effectiveReloadAnimationProgress < 0.5 && model.getStagedleftArmReloadPos().x != 0)
            renderArmDefault(model, model.getLeftArmReloadRot(), model.getLeftArmReloadPos(), poseStack);
        else if (effectiveReloadAnimationProgress > 0.5 && model.getStagedleftArmReloadPos().x != 0)
            renderArmDefault(model, model.getStagedleftArmReloadRot(), model.getStagedleftArmReloadPos(), poseStack);
        else
        {
            ItemStack stack = player.getMainHandItem();
            float clipPosition = getClipPosition(model, stack, effectiveReloadAnimationProgress);
            renderArmDefault(model, model.getLeftArmReloadRot(), model.getLeftArmReloadPos(), poseStack);

            AttachmentType gripAttachment = model.getType().getGrip(stack);
            float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - model.getTiltGunTime()) / (model.getUnloadClipTime() + model.getLoadClipTime()))));

            // Rotate the gun dependent on the animation type
            switch (model.getAnimationType())
            {
                case BREAK_ACTION, CUSTOMBREAK_ACTION ->
                {
                    poseStack.translate(model.getBarrelBreakPoint().x, model.getBarrelBreakPoint().y, model.getBarrelBreakPoint().z);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(model.getReloadRotate() * -model.getBreakAngle()));
                    poseStack.translate(-model.getBarrelBreakPoint().x, -model.getBarrelBreakPoint().y, -model.getBarrelBreakPoint().z);
                    poseStack.translate(-model.getBreakActionAmmoDistance() * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case REVOLVER, CUSTOMREVOLVER ->
                {
                    poseStack.translate(model.getRevolverFlipPoint().x, model.getRevolverFlipPoint().y, model.getRevolverFlipPoint().z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.getReloadRotate() * model.getRevolverFlipAngle()));
                    poseStack.translate(-model.getRevolverFlipPoint().x, -model.getRevolverFlipPoint().y, -model.getRevolverFlipPoint().z);
                    poseStack.translate(-1F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case REVOLVER2, CUSTOMREVOLVER2 ->
                {
                    poseStack.translate(model.getRevolver2FlipPoint().x, model.getRevolver2FlipPoint().y, model.getRevolver2FlipPoint().z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.getReloadRotate() * model.getRevolver2FlipAngle()));
                    poseStack.translate(-model.getRevolver2FlipPoint().x, -model.getRevolver2FlipPoint().y, -model.getRevolver2FlipPoint().z);
                    poseStack.translate(-1F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case BOTTOM_CLIP, CUSTOMBOTTOM_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case PISTOL_CLIP, CUSTOMPISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90F * clipPosition * clipPosition));
                    poseStack.translate(0F, -1F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case ALT_PISTOL_CLIP, CUSTOMALT_PISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(5F * clipPosition));
                    poseStack.translate(0F, -3F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case SIDE_CLIP, CUSTOMSIDE_CLIP ->
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F * clipPosition));
                    poseStack.mulPose(Axis.YP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.getType().getModelScale(), 0F, 0F);
                }
                case BULLPUP, CUSTOMBULLPUP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-150F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(clipPosition, -0.5F * clipPosition / model.getType().getModelScale(), 0F);
                }
                case P90, CUSTOMP90 ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * model.getReloadRotate() * model.getReloadRotate()));
                    poseStack.translate(0F, 0.075F * model.getReloadRotate(), 0F);
                    poseStack.translate(-2F * clipPosition / model.getType().getModelScale(), -0.3F * clipPosition / model.getType().getModelScale(), 0.5F * clipPosition / model.getType().getModelScale());
                }
                case RIFLE ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                    poseStack.translate(bulletProgress * -1F / model.getType().getModelScale(), 0F, bulletProgress * 0.5F / model.getType().getModelScale());
                }
                case CUSTOMRIFLE ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * model.getRotateClipVertical()));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * model.getRotateClipHorizontal()));
                    poseStack.mulPose(Axis.XP.rotationDegrees(bulletProgress * model.getTiltClip()));
                    poseStack.translate(bulletProgress * model.getTranslateClip().x / model.getType().getModelScale(), bulletProgress * model.getTranslateClip().y / model.getType().getModelScale(), bulletProgress * model.getTranslateClip().z / model.getType().getModelScale());
                }
                case RIFLE_TOP, CUSTOMRIFLE_TOP ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                    poseStack.translate(bulletProgress * -0.1F / model.getType().getModelScale(), bulletProgress / model.getType().getModelScale(), bulletProgress * 0.5F / model.getType().getModelScale());
                }
                case SHOTGUN, STRIKER, CUSTOMSHOTGUN, CUSTOMSTRIKER ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                    poseStack.translate(bulletProgress * -0.5F * 1 / model.getType().getModelScale(), bulletProgress * -1F * 1 / model.getType().getModelScale(), 0F);
                }
                case CUSTOM ->
                {
                    // Staged reload allows you to change the animation route halfway through
                    if (effectiveReloadAnimationProgress > 0.5 && model.isStagedReload())
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.getStagedrotateClipVertical() * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.getStagedrotateClipHorizontal() * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.getStagedtiltClip() * clipPosition));
                        poseStack.translate(model.getStagedtranslateClip().x * clipPosition / model.getType().getModelScale(), model.getStagedtranslateClip().y * clipPosition / model.getType().getModelScale(), model.getStagedtranslateClip().z * clipPosition / model.getType().getModelScale());
                    }
                    else
                    {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-model.getRotateClipVertical() * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.getRotateClipHorizontal() * clipPosition));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.getTiltClip() * clipPosition));
                        poseStack.translate(-model.getTranslateClip().z * clipPosition / model.getType().getModelScale(), model.getTranslateClip().y * clipPosition / model.getType().getModelScale(), model.getTranslateClip().x * clipPosition / model.getType().getModelScale());
                    }
                }
                case END_LOADED, CUSTOMEND_LOADED ->
                {
                    float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-45F * dYaw));
                    poseStack.translate(-getEndLoadedDistance(model, gripAttachment, stack) * dYaw, -0.5F * dYaw, 0F);

                    float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
                    poseStack.translate(getEndLoadedDistance(model, gripAttachment, stack) * xDisplacement, 0F, 0F);
                }
                case BACK_LOADED, CUSTOMBACK_LOADED ->
                {
                    float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
                    poseStack.translate(getEndLoadedDistance(model, gripAttachment, stack) * dYaw, -0.5F * dYaw, 0F);

                    float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
                    poseStack.translate(-getEndLoadedDistance(model, gripAttachment, stack) * xDisplacement, 0F, 0F);
                }
                default ->
                {
                    // no-op
                }
            }
        }
        poseStack.scale(model.getLeftArmScale().x, model.getLeftArmScale().y, model.getLeftArmScale().z);
        if (model.isLeftHandAmmo())
            playerModel.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
    }

    // right hand pump action animation
    private static void renderArmPump(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        poseStack.translate(-(armPosition.x - Math.abs(anim.getLastPumped() + (anim.getPumped() - anim.getLastPumped()) * smoothing) / model.getPumpModifier()), armPosition.y, armPosition.z);
        handleRotate(rotationPoint, model, poseStack);
    }

    // This moves the right hand if leftHandAmmo & handCharge are true (For left hand reload with right hand charge)
    private static void renderArmCharge(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(
            -(armPosition.x - Math.abs(anim.getLastCharged() + (anim.getCharged() - anim.getLastCharged()) * smoothing) / model.getChargeModifier().x),
            -(armPosition.y - Math.abs(anim.getLastCharged() + (anim.getCharged() - anim.getLastCharged()) * smoothing) / model.getChargeModifier().y),
            -(armPosition.z - Math.abs(anim.getLastCharged() + (anim.getCharged() - anim.getLastCharged()) * smoothing) / model.getChargeModifier().z)
        );
    }

    // This moves the right hand if leftHandAmmo & handBolt are true (For left hand reload with right hand bolt action)
    private static void renderArmBolt(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(
            armPosition.x + Math.abs(anim.getLastPumped() + (anim.getPumped() - anim.getLastPumped()) * smoothing) / model.getChargeModifier().x,
            armPosition.y + Math.abs(anim.getLastPumped() + (anim.getPumped() - anim.getLastPumped()) * smoothing) / model.getChargeModifier().y,
            -(armPosition.z - Math.abs(anim.getLastCharged() + (anim.getCharged() - anim.getLastCharged()) * smoothing) / model.getChargeModifier().z)
        );
    }

    private static void renderArmDefault(ModelGun model, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(armPosition.x, armPosition.y, armPosition.z);
    }

    private static void handleRotate(Vector3f rotationPoint, ModelGun model, PoseStack poseStack)
    {
        if (model.isEasyArms())
            poseStack.translate(0.4F * model.getArmScale().getX(), 0.75F * model.getArmScale().getY(), -0F * model.getArmScale().getZ());
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationPoint.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationPoint.z));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationPoint.x));
        if (model.isEasyArms())
            poseStack.translate(-0.4F * model.getArmScale().getX(), -0.75F * model.getArmScale().getY(), 0F * model.getArmScale().getZ());
    }
}
