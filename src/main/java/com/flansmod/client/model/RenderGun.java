package com.flansmod.client.model;

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
import net.minecraft.client.model.HumanoidModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
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
public final class RenderGun
{
    public static void renderItem(ModelGun model, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        poseStack.pushPose();
        GunAnimations animations = ModClient.getGunAnimations(ctx);
        model.reloadRotate = 0F;

        if (shouldRenderGun(model, ctx, stack))
        {
            int color = model.type.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            float modelScale = model.type.getModelScale();
            ResourceLocation gunTexture = model.type.getPaintjob(stack).getTexture();

            switch (ctx)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> applyFirstPersonAdjustments(model, animations, stack, poseStack, ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> applyThirdPersonAdjustments(model, animations, stack, poseStack, ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                case FIXED -> applyFixedEntityAdjustments(model, poseStack);
                case GROUND -> poseStack.translate(model.itemFrameOffset.x, model.itemFrameOffset.y, model.itemFrameOffset.z);
                default ->
                {
                    // no-op
                }
            }

            final int numRounds = countRoundsInGun(stack);
            if (model.slideLockOnEmpty)
            {
                if (numRounds == 0)
                    animations.onGunEmpty(true);
                else if (!animations.reloading)
                    animations.onGunEmpty(false);
            }



            poseStack.pushPose();
            if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            {
                handleGunRecoil(model, animations, stack, poseStack);

                if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    renderFirstPersonArm(model, animations, poseStack, buffer, packedLight);

                //This allows you to offset your gun with a sight attached to properly align the aiming reticle
                AttachmentType scopeAttachment = model.type.getScope(stack);
                if (model.gunOffset != 0 && ModClient.getZoomProgress() >= 0.5F && scopeAttachment != null && ModelCache.getOrLoadTypeModel(scopeAttachment) instanceof ModelAttachment scopeModel)
                    poseStack.translate(0F, -scopeModel.renderOffset + model.gunOffset / 16F, 0F);
            }
            poseStack.scale(modelScale, modelScale, modelScale);
            renderFlash(model, stack, animations, poseStack, buffer, packedOverlay);
            for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                renderGunAndComponents(model, stack, animations, numRounds, poseStack, buffer.getBuffer(renderPass.getRenderType(gunTexture)), packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
            if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                renderAnimArm(model, animations, poseStack, buffer, packedLight);
            renderAttachmentAmmo(model, stack, animations, numRounds, poseStack, buffer, packedLight, packedOverlay);
            renderCasingEjection(model, animations, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();

            renderMuzzleFlash(model, stack, animations, poseStack, buffer, packedOverlay);
            renderCustomAttachments(model, stack, animations, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static boolean shouldRenderGun(ModelGun model, ItemDisplayContext itemDisplayContext, ItemStack item)
    {
        if (itemDisplayContext.firstPerson())
            return !(ModClient.getZoomProgress() > 0.9F && model.type.getCurrentScope(item).hasZoomOverlay() && !model.stillRenderGunWhenScopedOverlay);
        return true;
    }

    private static boolean shouldRenderAmmo(GunAnimations animations, EnumAnimationType anim, int numRounds)
    {
        return numRounds != 0 || animations.reloading || (anim != EnumAnimationType.END_LOADED && anim != EnumAnimationType.BACK_LOADED);
    }

    private static void applyFixedEntityAdjustments(ModelGun model, PoseStack poseStack)
    {
        poseStack.translate(0.2F + model.itemFrameOffset.x, -0.2F + model.itemFrameOffset.y, model.itemFrameOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    private static void applyThirdPersonAdjustments(ModelGun model, GunAnimations animations, ItemStack stack, PoseStack poseStack, boolean leftHand)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.translate(-0.08F, -0.12F, 0F);
        poseStack.translate(model.thirdPersonOffset.x, model.thirdPersonOffset.y, model.thirdPersonOffset.z);

        if (ModClientConfig.get().enableGunAnimationsInThirdPerson)
        {
            renderMeleeMovement(model.type, animations, poseStack);
            renderSpinningCocking(model, animations, poseStack);
            renderReloadMovement(model, animations, stack, leftHand, poseStack);
        }
    }

    private static void applyFirstPersonAdjustments(ModelGun model, GunAnimations animations, ItemStack stack, PoseStack poseStack, boolean leftHand)
    {
        float adsSwitch = ModClient.getLastZoomProgress() + (ModClient.getZoomProgress() - ModClient.getLastZoomProgress()) * Minecraft.getInstance().getFrameTime();
        boolean crouching = ModClient.getZoomProgress() + 0.1F > 0.9F && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCrouching() && !animations.reloading;
        boolean sprinting = ModClient.getZoomProgress() + 0.1F < 0.2F && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSprinting() && !animations.reloading && model.fancyStance;

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        if (leftHand)
        {
            poseStack.translate(0.25F, -0.05F, 0.155F);
        }
        else
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-5F * adsSwitch));
            poseStack.translate(-0.25F, -0.05F + 0.175F * adsSwitch, -0.155F - 0.405F * adsSwitch);
            if (model.type.hasZoomOverlay() && !model.stillRenderGunWhenScopedOverlay)
            {
                poseStack.translate(-0.3F * adsSwitch, 0F, 0F);
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(4.5F * adsSwitch));
            poseStack.translate(crouching ? model.crouchZoom : 0F, -0.03F * adsSwitch, 0F);
        }

        renderWeaponSwitchMovement(animations, poseStack);
        renderSprintingMovement(model, animations, sprinting, poseStack);
        renderMeleeMovement(model.type, animations, poseStack);
        renderLookAtGunMovement(animations, poseStack);
        renderSpinningCocking(model, animations, poseStack);
        renderReloadMovement(model, animations, stack, leftHand, poseStack);
    }

    private static void renderWeaponSwitchMovement(GunAnimations animations, PoseStack poseStack)
    {
        if (animations.switchAnimationProgress <= 0F || animations.switchAnimationLength <= 0F)
            return;

        Vector3f pos1 = new Vector3f(0, -0.4f, 0);
        Vector3f pos2 = new Vector3f(0, 0, 0);
        Vector3f startAngles = new Vector3f(90, 30, -40);
        Vector3f endAngles = new Vector3f(0, 0, 0);
        float interp = (animations.switchAnimationProgress + Minecraft.getInstance().getFrameTime()) / animations.switchAnimationLength;

        poseStack.translate(pos2.x + (pos2.x - pos1.x) * interp, pos1.y + (pos2.y - pos1.y) * interp, pos1.z + (pos2.z - pos1.z) * interp);
        poseStack.mulPose(Axis.YP.rotationDegrees(startAngles.y + (endAngles.y - startAngles.y) * interp));
        poseStack.mulPose(Axis.ZP.rotationDegrees(startAngles.z + (endAngles.z - startAngles.z) * interp));
    }

    private static void renderSprintingMovement(ModelGun model, GunAnimations animations, boolean sprinting, PoseStack poseStack)
    {
        if (sprinting && animations.stanceTimer == 0 && ModClientConfig.get().enableWeaponSprintStance)
        {
            if (animations.runningStanceAnimationProgress == 0F)
                animations.runningStanceAnimationProgress = 1F;

            Vector3f defaultTranslate = new Vector3f(0, 0F, -0.2);
            Vector3f defaultRotation = new Vector3f(-15F, 45F, -10F);

            Vector3f configuredTranslate = model.sprintStanceTranslate;
            Vector3f configuredRotation = model.sprintStanceRotate;

            float progress = (animations.runningStanceAnimationProgress + Minecraft.getInstance().getFrameTime()) / animations.runningStanceAnimationLength;
            if (animations.runningStanceAnimationProgress == animations.runningStanceAnimationLength)
                progress = 1;

            if (ModClientConfig.get().enableRandomSprintStance)
            {
                animations.updateSprintStance(model.type);
                defaultRotation = animations.sprintingStance;
            }

            if (!Objects.equals(model.sprintStanceTranslate, new Vector3f(0F, 0F, 0F)))
                poseStack.translate(configuredTranslate.x * progress, configuredTranslate.y * progress, configuredTranslate.z * progress);
            else
                poseStack.translate(defaultTranslate.x * progress, defaultTranslate.y * progress, defaultTranslate.z * progress);

            if (!Objects.equals(model.sprintStanceRotate, new Vector3f(0F, 0F, 0F)))
            {
                poseStack.mulPose(Axis.XP.rotationDegrees(configuredRotation.x * progress));
                poseStack.mulPose(Axis.YP.rotationDegrees(configuredRotation.y * progress));
                poseStack.mulPose(Axis.ZP.rotationDegrees(configuredRotation.z * progress));
            }
            else
            {
                poseStack.mulPose(Axis.XP.rotationDegrees(defaultRotation.x * progress));
                poseStack.mulPose(Axis.YP.rotationDegrees(defaultRotation.y * progress));
                poseStack.mulPose(Axis.ZP.rotationDegrees(defaultRotation.z * progress));
            }
        }
        else
        {
            animations.runningStanceAnimationProgress = 0F;
        }
    }

    private static void renderMeleeMovement(GunType gunType, GunAnimations animations, PoseStack poseStack)
    {
        int progress = animations.meleeAnimationProgress;
        if (progress <= 0 || progress >= gunType.getMeleePath().size())
            return;

        float t = Mth.clamp(Minecraft.getInstance().getFrameTime(), 0.0f, 1.0f);

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
        float interp = animations.lookAtTimer + Minecraft.getInstance().getFrameTime();
        interp /= animations.lookAt.getTime();

        final Vector3f idlePos = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f look1Pos = new Vector3f(0.25f, 0.25f, 0.0f);
        final Vector3f look2Pos = new Vector3f(0.25f, 0.25f, -0.5f);
        final Vector3f idleAngles = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f look1Angles = new Vector3f(0.0f, 70.0f, 0.0f);
        final Vector3f look2Angles = new Vector3f(0.0f, -60.0f, 60.0f);
        Vector3f startPos = new Vector3f();
        Vector3f endPos = new Vector3f();
        Vector3f startAngles = new Vector3f();
        Vector3f endAngles = new Vector3f();

        switch (animations.lookAt)
        {
            case NONE -> {
                startPos = endPos = idlePos;
                startAngles = endAngles = idleAngles;
            }
            case LOOK1 -> {
                startPos = endPos = look1Pos;
                startAngles = endAngles = look1Angles;
            }
            case LOOK2 -> {
                startPos = endPos = look2Pos;
                startAngles = endAngles = look2Angles;
            }
            case TILT1 -> {
                startPos = idlePos;
                startAngles = idleAngles;
                endPos = look1Pos;
                endAngles = look1Angles;
            }
            case TILT2 -> {
                startPos = look1Pos;
                startAngles = look1Angles;
                endPos = look2Pos;
                endAngles = look2Angles;
            }
            case UNTILT -> {
                startPos = look2Pos;
                startAngles = look2Angles;
                endPos = idlePos;
                endAngles = idleAngles;
            }
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(startAngles.y + (endAngles.y - startAngles.y) * interp));
        poseStack.mulPose(Axis.ZP.rotationDegrees(startAngles.z + (endAngles.z - startAngles.z) * interp));
        poseStack.translate(startPos.x + (endPos.x - startPos.x) * interp, startPos.y + (endPos.y - startPos.y) * interp, startPos.z + (endPos.z - startPos.z) * interp);
    }

    private static void renderSpinningCocking(ModelGun model, GunAnimations animations, PoseStack poseStack)
    {
        if(!model.spinningCocking)
            return;

        poseStack.translate(model.spinPoint.x, model.spinPoint.y, model.spinPoint.z);
        float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime());
        poseStack.mulPose(Axis.ZP.rotationDegrees(pumped * 180F + 180F));
        poseStack.translate(-model.spinPoint.x, -model.spinPoint.y, -model.spinPoint.z);
    }

    private static void renderReloadMovement(ModelGun model, GunAnimations animations, ItemStack stack, boolean leftHand, PoseStack poseStack)
    {
        if (!animations.reloading)
            return;

        int flip = leftHand ? -1 : 1;

        EnumAnimationType anim = model.animationType;
        AttachmentType gripAttachment = model.type.getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;

        if (gripModel != null && model.type.getSecondaryFire(stack))
            anim = gripModel.secondaryAnimType;

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
                poseStack.mulPose(Axis.ZP.rotationDegrees(model.rotateGunVertical * reloadRotate));
                poseStack.mulPose(Axis.YP.rotationDegrees(model.rotateGunHorizontal * reloadRotate));
                poseStack.mulPose(Axis.XP.rotationDegrees(model.tiltGun * reloadRotate));
                poseStack.translate(model.translateGun.x * reloadRotate, model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
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
        float smoothing = Minecraft.getInstance().getFrameTime();

        poseStack.translate(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * recoilDistance, 0F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * recoilAngle));
        poseStack.mulPose(Axis.YP.rotationDegrees(((-animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * result * smoothing * model.ShakeDistance)));
        poseStack.mulPose(Axis.XP.rotationDegrees(((-animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * result * smoothing * model.ShakeDistance)));

        // Do not move gun when there's a pump in the reload
        if (model.animationType == EnumAnimationType.SHOTGUN && !animations.reloading)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * -5F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * 5F));
        }

        if (model.isSingleAction)
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * -5F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * 2.5F));
        }
    }

    /** Get the recoil distance, based on ammo type to reload */
    private static float getRecoilDistance(ModelGun model, ItemStack gunStack)
    {
        AttachmentType grip = model.type.getGrip(gunStack);
        if (grip != null && model.type.getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.recoilDistance;
        else
            return model.RecoilSlideDistance;
    }

    /** Get the recoil angle, based on ammo type to reload */
    private static float getRecoilAngle(ModelGun model, ItemStack gunStack)
    {
        AttachmentType grip = model.type.getGrip(gunStack);
        if (grip != null && model.type.getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.recoilAngle;
        else
            return model.RotateSlideDistance;
    }

    /** Render the gun and default attachment models */
    private static void renderGunAndComponents(ModelGun model, ItemStack stack, GunAnimations animations, int numRounds, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();

        //Get all the attachments that we may need to render
        AttachmentType scopeAttachment = model.type.getScope(stack);
        AttachmentType barrelAttachment = model.type.getBarrel(stack);
        AttachmentType stockAttachment = model.type.getStock(stack);
        AttachmentType gripAttachment = model.type.getGrip(stack);
        AttachmentType gadgetAttachment = model.type.getGadget(stack);
        AttachmentType slideAttachment = model.type.getSlide(stack);
        AttachmentType pumpAttachment = model.type.getPump(stack);

        model.render(model.gunModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        model.render(model.backpackModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        model.renderCustom(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, animations, renderPass);

        // Render the guns default parts if no attachment is installed
        if (scopeAttachment == null && !model.scopeIsOnSlide && !model.scopeIsOnBreakAction)
            model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (barrelAttachment == null)
            model.render(model.defaultBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (stockAttachment == null)
            model.render(model.defaultStockModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (gripAttachment == null && !model.gripIsOnPump)
            model.render(model.defaultGripModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (gadgetAttachment == null && !model.gadgetIsOnPump)
            model.render(model.defaultGadgetModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

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
        if (renderPass != EnumRenderPass.GLOW_ALPHA || (!model.isBulletCounterActive && !model.isAdvBulletCounterActive))
            return;

        if (model.isBulletCounterActive && numRounds < model.bulletCounterModel.length)
        {
            poseStack.pushPose();
            ModelRendererTurbo part = model.bulletCounterModel[numRounds];
            part.glow = true;
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            part.glow = false;
            poseStack.popPose();
        }

        if (model.isAdvBulletCounterActive)
        {
            poseStack.pushPose();

            // Number of digit positions available in the model
            final int places = model.advBulletCounterModel.length;

            // Render each digit position
            for (int i = 0; i < places; i++)
            {
                // Pick which decimal place this slot shows
                // If countOnRightHandSide == false: i=0 is most-significant (left)
                // If true: i=0 is least-significant (right)
                final int placeIndex = model.countOnRightHandSide ? i : (places - 1 - i);

                // Extract digit at 10^placeIndex
                int digit = numRounds;
                for (int k = 0; k < placeIndex; k++)
                    digit /= 10;
                digit %= 10;

                ModelRendererTurbo part = model.advBulletCounterModel[i][digit];
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
            final ItemStack bullet = gunItem.getAmmoItemStack(gunStack, i);
            if (bullet == null || !(bullet.getItem() instanceof ShootableItem))
                continue;

            final int max = bullet.getMaxDamage();
            final int damage = bullet.getDamageValue();

            if (damage < max)
                rounds += (max - damage);
        }

        return rounds;
    }

    private static void renderSlideModels(ModelGun model, ItemStack stack, GunAnimations animations, AttachmentType slideAttachment, AttachmentType scopeAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (slideAttachment == null)
        {
            poseStack.pushPose();
            if (!model.type.getSecondaryFire(stack))
            {
                poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * Minecraft.getInstance().getFrameTime()) * model.gunSlideDistance, 0F, 0F);
                poseStack.translate(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * Minecraft.getInstance().getFrameTime())) * model.chargeHandleDistance, 0F, 0F);
            }
            model.render(model.slideModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (scopeAttachment == null && model.scopeIsOnSlide)
                model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();

            if (!model.type.getSecondaryFire(stack))
            {
                poseStack.pushPose();
                poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * Minecraft.getInstance().getFrameTime()) * model.altgunSlideDistance, 0F, 0F);
                model.render(model.altslideModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
                poseStack.popPose();
            }
        }
    }

    private static void renderBreakAction(ModelGun model, AttachmentType scopeAttachment, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.breakAngle));
        poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
        model.render(model.breakActionModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        if (scopeAttachment == null && model.scopeIsOnBreakAction)
            model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.altbarrelBreakPoint.x, model.altbarrelBreakPoint.y, model.altbarrelBreakPoint.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.altbreakAngle));
        poseStack.translate(-model.altbarrelBreakPoint.x, -model.altbarrelBreakPoint.y, -model.altbarrelBreakPoint.z);
        model.render(model.altbreakActionModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderHammer(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.hammerSpinPoint.x, model.hammerSpinPoint.y, model.hammerSpinPoint.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.hammerRotation));
        poseStack.translate(-model.hammerSpinPoint.x, -model.hammerSpinPoint.y, -model.hammerSpinPoint.z);
        model.render(model.hammerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.althammerSpinPoint.x, model.althammerSpinPoint.y, model.althammerSpinPoint.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.althammerRotation));
        poseStack.translate(-model.althammerSpinPoint.x, -model.althammerSpinPoint.y, -model.althammerSpinPoint.z);
        model.render(model.althammerModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderPumpAction(ModelGun model, GunAnimations animations, AttachmentType pumpAttachment, AttachmentType gripAttachment, AttachmentType gadgetAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (pumpAttachment == null)
        {
            poseStack.pushPose();
            poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.pumpHandleDistance, 0F, 0F);
            model.render(model.pumpModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (gripAttachment == null && model.gripIsOnPump)
                model.render(model.defaultGripModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            if (gadgetAttachment == null && model.gadgetIsOnPump)
                model.render(model.defaultGadgetModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderBoltAction(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.boltCycleDistance, 0F, 0F);
        poseStack.translate(model.boltRotationOffset.x, model.boltRotationOffset.y, model.boltRotationOffset.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.boltRotationAngle));
        poseStack.translate(-model.boltRotationOffset.x, -model.boltRotationOffset.y, -model.boltRotationOffset.z);
        model.render(model.boltActionModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderChargeHandle(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.chargeHandleDistance != 0F)
        {
            poseStack.pushPose();
            poseStack.translate(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * Minecraft.getInstance().getFrameTime())) * model.chargeHandleDistance, 0F, 0F);
            model.render(model.chargeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderMinigunBarrels(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.type.getMode() == EnumFireMode.MINIGUN)
        {
            poseStack.pushPose();
            poseStack.translate(model.minigunBarrelOrigin.x, model.minigunBarrelOrigin.y, model.minigunBarrelOrigin.z);
            Vector3f axis = new Vector3f(model.minigunBarrelSpinDirection.x, model.minigunBarrelSpinDirection.y, model.minigunBarrelSpinDirection.z).normalise(null);
            poseStack.mulPose(Axis.of(new org.joml.Vector3f(axis.x, axis.y, axis.z)).rotationDegrees(animations.minigunBarrelRotation * model.minigunBarrelSpinSpeed));
            poseStack.translate(-model.minigunBarrelOrigin.x, -model.minigunBarrelOrigin.y, -model.minigunBarrelOrigin.z);
            model.render(model.minigunBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderRevolverBarrel(ModelGun model, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(reloadRotate * model.revolverFlipAngle));
        poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
        model.render(model.revolverBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-reloadRotate * model.revolverFlipAngle));
        poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
        model.render(model.revolver2BarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    private static void renderStaticAmmo(ModelGun model, ItemStack stack, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        if (model.type.getSecondaryFire(stack))
        {
            poseStack.pushPose();
            model.render(model.ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderAmmo(ModelGun model, GunAnimations animations, ItemStack stack, int numRounds, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        poseStack.pushPose();

        AttachmentType gripAttachment = model.type.getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;
        EnumAnimationType anim = model.animationType;

        if (gripModel != null && model.type.getSecondaryFire(stack))
            anim = gripModel.secondaryAnimType;

        boolean shouldRender = shouldRenderAmmo(animations, anim, numRounds);

        // If it should be rendered, do the transformations required
        if (shouldRender && animations.reloading)
        {
            // Calculate the amount of tilt required for the reloading animation
            float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * Minecraft.getInstance().getFrameTime();
            float clipPosition = getClipPosition(model, stack, effectiveReloadAnimationProgress);
            float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - model.tiltGunTime) / (model.unloadClipTime + model.loadClipTime))));

            // Rotate the gun dependent on the animation type
            switch (model.animationType)
            {
                case BREAK_ACTION, CUSTOMBREAK_ACTION ->
                {
                    poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(model.reloadRotate * -model.breakAngle));
                    poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
                    poseStack.translate(-model.breakActionAmmoDistance * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case REVOLVER, CUSTOMREVOLVER ->
                {
                    poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolverFlipAngle));
                    poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
                    poseStack.translate(-1F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case REVOLVER2, CUSTOMREVOLVER2 ->
                {
                    poseStack.translate(model.revolver2FlipPoint.x, model.revolver2FlipPoint.y, model.revolver2FlipPoint.z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolver2FlipAngle));
                    poseStack.translate(-model.revolver2FlipPoint.x, -model.revolver2FlipPoint.y, -model.revolver2FlipPoint.z);
                    poseStack.translate(-1F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case BOTTOM_CLIP, CUSTOMBOTTOM_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case PISTOL_CLIP, CUSTOMPISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90F * clipPosition * clipPosition));
                    poseStack.translate(0F, -1F * clipPosition / model.type.getModelScale(), 0F);
                }
                case ALT_PISTOL_CLIP, CUSTOMALT_PISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(5F * clipPosition));
                    poseStack.translate(0F, -3F * clipPosition / model.type.getModelScale(), 0F);
                }
                case SIDE_CLIP, CUSTOMSIDE_CLIP ->
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F * clipPosition));
                    poseStack.mulPose(Axis.YP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case BULLPUP, CUSTOMBULLPUP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-150F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(clipPosition, -0.5F * clipPosition / model.type.getModelScale(), 0F);
                }
                case P90, CUSTOMP90 ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * model.reloadRotate * model.reloadRotate));
                    poseStack.translate(0F, 0.075F * model.reloadRotate, 0F);
                    poseStack.translate(-2F * clipPosition / model.type.getModelScale(), -0.3F * clipPosition / model.type.getModelScale(), 0.5F * clipPosition / model.type.getModelScale());
                }
                case RIFLE ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                    poseStack.translate(bulletProgress * -1F / model.type.getModelScale(), 0F, bulletProgress * 0.5F / model.type.getModelScale());
                }
                case CUSTOMRIFLE ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * model.rotateClipVertical));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * model.rotateClipHorizontal));
                    poseStack.mulPose(Axis.XP.rotationDegrees(bulletProgress * model.tiltClip));
                    poseStack.translate(bulletProgress * model.translateClip.x / model.type.getModelScale(), bulletProgress * model.translateClip.y / model.type.getModelScale(), bulletProgress * model.translateClip.z / model.type.getModelScale());
                }
                case RIFLE_TOP, CUSTOMRIFLE_TOP ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                    poseStack.translate(bulletProgress * -0.1F / model.type.getModelScale(), bulletProgress / model.type.getModelScale(), bulletProgress * 0.5F / model.type.getModelScale());
                }
                case SHOTGUN, STRIKER, CUSTOMSHOTGUN, CUSTOMSTRIKER ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                    poseStack.translate(bulletProgress * -0.5F * 1 / model.type.getModelScale(), bulletProgress * -1F * 1 / model.type.getModelScale(), 0F);
                }
                case CUSTOM ->
                {
                    // Staged reload allows you to change the animation route halfway through
                    if (effectiveReloadAnimationProgress > 0.5 && model.stagedReload)
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.stagedrotateClipVertical * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.stagedrotateClipHorizontal * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.stagedtiltClip * clipPosition));
                        poseStack.translate(model.stagedtranslateClip.x * clipPosition / model.type.getModelScale(), model.stagedtranslateClip.y * clipPosition / model.type.getModelScale(), model.stagedtranslateClip.z * clipPosition / model.type.getModelScale());
                    }
                    else
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.rotateClipVertical * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.rotateClipHorizontal * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.tiltClip * clipPosition));
                        poseStack.translate(model.translateClip.x * clipPosition / model.type.getModelScale(), model.translateClip.y * clipPosition / model.type.getModelScale(), model.translateClip.z * clipPosition / model.type.getModelScale());
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

        if (shouldRender && gripAttachment == null || !model.type.getSecondaryFire(stack))
            model.render(model.ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        // Renders fullammo model for 2nd half of reload animation
        float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * Minecraft.getInstance().getFrameTime();
        if (effectiveReloadAnimationProgress > 0.5)
            model.render(model.fullammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        poseStack.popPose();
    }

    public static float getClipPosition(ModelGun model, ItemStack stack, float effectiveReloadAnimationProgress)
    {
        AttachmentType gripAttachment = model.type.getGrip(stack);
        ModelAttachment gripModel = gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment attachment ? attachment : null;

        float tiltGunTime = model.tiltGunTime;
        float unloadClipTime = model.unloadClipTime;
        float loadClipTime = model.loadClipTime;

        if (gripModel != null && model.type.getSecondaryFire(stack))
        {
            tiltGunTime = gripModel.tiltGunTime;
            unloadClipTime = gripModel.unloadClipTime;
            loadClipTime = gripModel.loadClipTime;
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
        if (grip != null && model.type.getSecondaryFire(gunStack) && ModelCache.getOrLoadTypeModel(grip) instanceof ModelAttachment gripModel)
            return gripModel.endLoadedAmmoDistance;
        else
            return model.endLoadedAmmoDistance;
    }

    /** Get the number of bullets to reload in animation, based on ammo type to reload */
    public static float getNumBulletsInReload(ModelGun model, GunAnimations animations)
    {
        // If this is a singles reload, we want to know the number of bullets already in the gun
        if (animations.singlesReload)
            return animations.reloadAmmoCount;
        else
            return model.numBulletsInReloadAnimation;
    }

    public static float getReloadRotate(ModelGun model, GunAnimations animations)
    {
        float reloadRotate = 1F;

        // Snap to zero if reload is finished. Otherwise, weird behaviour.
        if (!animations.reloading)
            return 0F;

        float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * Minecraft.getInstance().getFrameTime();

        if (effectiveReloadAnimationProgress < model.tiltGunTime)
            reloadRotate = effectiveReloadAnimationProgress / model.tiltGunTime;
        if (effectiveReloadAnimationProgress > model.tiltGunTime + model.unloadClipTime + model.loadClipTime)
            reloadRotate = 1F - (effectiveReloadAnimationProgress - (model.tiltGunTime + model.unloadClipTime + model.loadClipTime)) / model.untiltGunTime;

        return reloadRotate;
    }

    private static void renderFlash(ModelGun model, ItemStack item, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay)
    {
        ModelFlash flash = ModelCache.getOrLoadFlashModel(model.type);
        AttachmentType barrelAttachment = model.type.getBarrel(item);
        boolean isFlashEnabled = flash != null && (barrelAttachment == null || !barrelAttachment.isDisableMuzzleFlash());

        if (isFlashEnabled && animations.muzzleFlashTime > 0 && !model.type.getSecondaryFire(item))
        {
            poseStack.pushPose();
            poseStack.scale(model.flashScale, model.flashScale, model.flashScale);

            Vector3f base = Objects.requireNonNullElse(model.muzzleFlashPoint, Vector3f.Zero);

            if (barrelAttachment != null && ModelCache.getOrLoadTypeModel(barrelAttachment) instanceof ModelAttachment barrelModel)
            {
                Vector3f muzzleFlashPoint = barrelModel.getMuzzleFlashPoint(base, model.barrelAttachPoint);
                poseStack.translate(muzzleFlashPoint.x, muzzleFlashPoint.y, muzzleFlashPoint.z);
            }
            else
            {
                Vector3f defaultOffset = Objects.requireNonNullElse(model.defaultBarrelFlashPoint, Vector3f.Zero);
                poseStack.translate(base.x + defaultOffset.x, base.y + defaultOffset.y, base.z + defaultOffset.z);
            }

            ResourceLocation flashTexture = model.type.getFlashTexture();
            flash.renderFlash(animations.flashInt, poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(flashTexture)), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F, 1F);
            poseStack.popPose();
        }
    }

    private static void renderAttachmentAmmo(ModelGun model, ItemStack stack, GunAnimations animations, int numRounds, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        AttachmentType gripAttachment = model.type.getGrip(stack);
        ItemStack gripItemStack = model.type.getGripItemStack(stack);

        if (gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment gripModel)
        {
            int color = gripAttachment.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            float modelScale = gripAttachment.getModelScale();
            ResourceLocation ammoTexture = gripAttachment.getPaintjob(gripItemStack).getTexture();

            if (shouldRenderAmmo(animations, model.animationType, numRounds) || !model.type.getSecondaryFire(stack))
            {
                for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                    gripModel.renderAttachmentAmmo(poseStack, buffer.getBuffer(renderPass.getRenderType(ammoTexture)), packedLight, packedOverlay, red, green, blue, 1F, modelScale, renderPass);
            }
        }
    }

    private static void renderCasingEjection(ModelGun model, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        ModelCasing casing = ModelCache.getOrLoadCasingModel(model.type);
        if (casing != null)
        {
            float casingProg = (animations.lastCasingStage + (animations.casingStage - animations.lastCasingStage) * Minecraft.getInstance().getFrameTime()) / model.casingAnimTime;
            if (casingProg >= 1)
                casingProg = 0;
            float moveX = model.casingAnimDistance.x + (animations.casingRandom.x * model.casingAnimSpread.x);
            float moveY = model.casingAnimDistance.y + (animations.casingRandom.y * model.casingAnimSpread.y);
            float moveZ = model.casingAnimDistance.z + (animations.casingRandom.z * model.casingAnimSpread.z);
            poseStack.pushPose();
            poseStack.scale(model.caseScale, model.caseScale, model.caseScale);
            poseStack.translate(model.casingAttachPoint.x + (casingProg * moveX), model.casingAttachPoint.y + (casingProg * moveY), model.casingAttachPoint.z + (casingProg * moveZ));
            poseStack.mulPose(Axis.of(new org.joml.Vector3f(model.casingRotateVector.x, model.casingRotateVector.y, model.casingRotateVector.z)).rotationDegrees(casingProg * 180));
            ResourceLocation casingTexture = model.type.getCasingTexture();
            for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                casing.renderCasing(poseStack, buffer.getBuffer(renderPass.getRenderType(casingTexture)), packedLight, packedOverlay, 1F, 1F, 1F, 1F, 1F, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderMuzzleFlash(ModelGun model, ItemStack stack, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay)
    {
        AttachmentType barrelAttachment = model.type.getBarrel(stack);
        boolean isMuzzleFlashEnabled = StringUtils.isBlank(model.type.getFlashModelClassName())
                && (barrelAttachment == null || !barrelAttachment.isDisableMuzzleFlash())
                && (StringUtils.isNotBlank(model.type.getMuzzleFlashModelClassName()) || model.getClass().getName().contains("com.flansmod.modernweapons.client.model"));

        if (isMuzzleFlashEnabled && animations.muzzleFlashTime > 0 && !model.type.getSecondaryFire(stack))
        {
            ModelMuzzleFlash muzzleFlash = ModelCache.getOrLoadMuzzleFlashModel(model.type);
            if (muzzleFlash != null)
            {

                Vector3f mfPoint = Objects.requireNonNullElse(model.muzzleFlashPoint, Objects.requireNonNullElse(model.barrelAttachPoint, Vector3f.Zero));
                if (mfPoint.equals(ModelGun.invalid))
                    mfPoint = model.barrelAttachPoint;

                if (barrelAttachment != null && ModelCache.getOrLoadTypeModel(barrelAttachment) instanceof ModelAttachment barrelModel)
                {
                    mfPoint = barrelModel.getMuzzleFlashPoint(mfPoint, model.barrelAttachPoint);
                }
                else if (model.defaultBarrelFlashPoint != null)
                {
                    mfPoint = Vector3f.add(model.muzzleFlashPoint, model.defaultBarrelFlashPoint, null);
                }

                poseStack.pushPose();
                poseStack.translate(mfPoint.x * model.type.getModelScale(), mfPoint.y * model.type.getModelScale(), mfPoint.z * model.type.getModelScale());
                muzzleFlash.renderToBuffer(poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(muzzleFlash.getTexture())), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F);
                poseStack.popPose();
            }
        }
    }

    private static void renderCustomAttachments(ModelGun model, ItemStack item, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        float smoothing = Minecraft.getInstance().getFrameTime();

        ItemStack scopeItemStack = model.type.getScopeItemStack(item);
        ItemStack barrelItemStack = model.type.getBarrelItemStack(item);
        ItemStack stockItemStack = model.type.getStockItemStack(item);
        ItemStack gripItemStack = model.type.getGripItemStack(item);
        ItemStack gadgetItemStack = model.type.getGadgetItemStack(item);
        ItemStack slideItemStack = model.type.getSlideItemStack(item);
        ItemStack pumpItemStack = model.type.getPumpItemStack(item);
        ItemStack accessoryItemStack = model.type.getAccessoryItemStack(item);

        List<AttachmentType> attachments = model.type.getCurrentAttachments(item);
        // Get all the attachments that we may need to render
        for (AttachmentType attachment : attachments)
        {
            poseStack.pushPose();

            switch(attachment.getEnumAttachmentType())
            {
                case SIGHTS:
                    preRenderAttachment(attachment, model.scopeAttachPoint, poseStack, model.type.getModelScale());
                    if (model.scopeIsOnBreakAction)
                    {
                        poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(getReloadRotate(model, animations) * -model.breakAngle));
                        poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
                    }
                    if (model.scopeIsOnSlide)
                        poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
                    renderAttachment(attachment, scopeItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case GRIP:
                    preRenderAttachment(attachment, model.gripAttachPoint, poseStack, model.type.getModelScale());
                    if (model.gripIsOnPump)
                        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                    renderAttachment(attachment, gripItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case BARREL:
                    preRenderAttachment(attachment, model.barrelAttachPoint, poseStack, model.type.getModelScale());
                    renderAttachment(attachment, barrelItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case STOCK:
                    preRenderAttachment(attachment, model.stockAttachPoint, poseStack, model.type.getModelScale());
                    renderAttachment(attachment, stockItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case SLIDE:
                    preRenderAttachment(attachment, model.slideAttachPoint, poseStack, model.type.getModelScale());
                    poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
                    renderAttachment(attachment, slideItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case GADGET:
                    preRenderAttachment(attachment, model.gadgetAttachPoint, poseStack, model.type.getModelScale());
                    if (model.gadgetIsOnPump)
                        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                    renderAttachment(attachment, gadgetItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case ACCESSORY:
                    preRenderAttachment(attachment, model.accessoryAttachPoint, poseStack, model.type.getModelScale());
                    renderAttachment(attachment, accessoryItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case PUMP:
                    preRenderAttachment(attachment, model.pumpAttachPoint, poseStack, model.type.getModelScale());
                    poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
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
            for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                modelAttachment.renderAttachment(poseStack, buffer.getBuffer(renderPass.getRenderType((attachmentTexture))), packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
        }
    }

    private static void renderFirstPersonArm(ModelGun model, GunAnimations anim, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!ModClientConfig.get().enableArms || !model.hasArms)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        float smoothing = mc.getFrameTime();
        PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        ResourceLocation skin = player.getSkinTextureLocation();
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
        if (!anim.reloading && model.righthandPump)
            renderArmPump(model, anim, smoothing, model.rightArmRot, model.rightArmPos, poseStack);
        else if (anim.charged < 0.9 && model.leftHandAmmo && model.rightHandCharge && anim.charged != -1.0F)
            renderArmCharge(model, anim, smoothing, model.rightArmChargeRot, model.rightArmChargePos, poseStack);
        else if (anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo)
            renderArmBolt(model, anim, smoothing, model.rightArmChargeRot, model.rightArmChargePos, poseStack);
        else if (!anim.reloading)
            renderArmDefault(model, model.rightArmRot, model.rightArmPos, poseStack);
        else
            renderArmDefault(model, model.rightArmReloadRot, model.rightArmReloadPos, poseStack);
        poseStack.scale(model.rightArmScale.x, model.rightArmScale.y, model.rightArmScale.z);
        if (!model.rightHandAmmo)
            playerModel.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        if (!anim.reloading && model.lefthandPump)
            renderArmPump(model, anim, smoothing, model.leftArmRot, model.leftArmPos, poseStack);
        else if (anim.charged < 0.9 && model.rightHandCharge && model.leftHandAmmo && anim.charged != -1.0F)
            renderArmCharge(model, anim, smoothing, model.leftArmChargeRot, model.leftArmChargePos, poseStack);
        else if (anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo)
            renderArmBolt(model, anim, smoothing, model.leftArmChargeRot, model.leftArmChargePos, poseStack);
        else if (!anim.reloading)
            renderArmDefault(model, model.leftArmRot, model.leftArmPos, poseStack);
        else
            renderArmDefault(model, model.leftArmReloadRot, model.leftArmReloadPos, poseStack);
        poseStack.scale(model.leftArmScale.x, model.leftArmScale.y, model.leftArmScale.z);
        if (!model.leftHandAmmo)
            playerModel.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderAnimArm(ModelGun model, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (!ModClientConfig.get().enableArms || !model.hasArms)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        float smoothing = mc.getFrameTime();
        PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        ResourceLocation skin = player.getSkinTextureLocation();
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
        poseStack.scale(1F / model.type.getModelScale(), 1F / model.type.getModelScale(), 1F / model.type.getModelScale());

        poseStack.pushPose();
        float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
        if (animations.charged < 0.9 && model.rightHandCharge && model.rightHandAmmo && animations.charged != -1.0F)
            renderArmPump(model, animations, smoothing, model.rightArmRot, model.rightArmPos, poseStack);
        else if (animations.pumped < 0.9 && model.rightHandBolt && model.rightHandAmmo)
            renderArmBolt(model, animations, smoothing, model.rightArmChargeRot, model.rightArmChargePos, poseStack);
        else if (!animations.reloading)
            renderArmDefault(model, model.rightArmRot, model.rightArmPos, poseStack);
        else
            renderArmDefault(model, model.rightArmReloadRot, model.rightArmReloadPos, poseStack);
        poseStack.scale(model.rightArmScale.x, model.rightArmScale.y, model.rightArmScale.z);
        if (model.rightHandAmmo)
            playerModel.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        if (animations.charged < 0.9 && model.leftHandCharge && model.leftHandAmmo && animations.charged != -1.0F)
            renderArmCharge(model, animations, smoothing, model.leftArmChargeRot, model.leftArmChargePos, poseStack);
        else if (!animations.reloading && model.lefthandPump)
            renderArmPump(model, animations, smoothing, model.leftArmRot, model.leftArmPos, poseStack);
        else if (!animations.reloading)
            renderArmDefault(model, model.leftArmRot, model.leftArmPos, poseStack);
        else if (effectiveReloadAnimationProgress < 0.5 && model.stagedleftArmReloadPos.x != 0)
            renderArmDefault(model, model.leftArmReloadRot, model.leftArmReloadPos, poseStack);
        else if (effectiveReloadAnimationProgress > 0.5 && model.stagedleftArmReloadPos.x != 0)
            renderArmDefault(model, model.stagedleftArmReloadRot, model.stagedleftArmReloadPos, poseStack);
        else
        {
            ItemStack stack = player.getMainHandItem();
            float clipPosition = getClipPosition(model, stack, effectiveReloadAnimationProgress);
            renderArmDefault(model, model.leftArmReloadRot, model.leftArmReloadPos, poseStack);

            AttachmentType gripAttachment = model.type.getGrip(stack);
            float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - model.tiltGunTime) / (model.unloadClipTime + model.loadClipTime))));

            // Rotate the gun dependent on the animation type
            switch (model.animationType)
            {
                case BREAK_ACTION, CUSTOMBREAK_ACTION ->
                {
                    poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(model.reloadRotate * -model.breakAngle));
                    poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
                    poseStack.translate(-model.breakActionAmmoDistance * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case REVOLVER, CUSTOMREVOLVER ->
                {
                    poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolverFlipAngle));
                    poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
                    poseStack.translate(-1F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case REVOLVER2, CUSTOMREVOLVER2 ->
                {
                    poseStack.translate(model.revolver2FlipPoint.x, model.revolver2FlipPoint.y, model.revolver2FlipPoint.z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolver2FlipAngle));
                    poseStack.translate(-model.revolver2FlipPoint.x, -model.revolver2FlipPoint.y, -model.revolver2FlipPoint.z);
                    poseStack.translate(-1F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case BOTTOM_CLIP, CUSTOMBOTTOM_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case PISTOL_CLIP, CUSTOMPISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90F * clipPosition * clipPosition));
                    poseStack.translate(0F, -1F * clipPosition / model.type.getModelScale(), 0F);
                }
                case ALT_PISTOL_CLIP, CUSTOMALT_PISTOL_CLIP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(5F * clipPosition));
                    poseStack.translate(0F, -3F * clipPosition / model.type.getModelScale(), 0F);
                }
                case SIDE_CLIP, CUSTOMSIDE_CLIP ->
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F * clipPosition));
                    poseStack.mulPose(Axis.YP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(0.5F * clipPosition / model.type.getModelScale(), 0F, 0F);
                }
                case BULLPUP, CUSTOMBULLPUP ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-150F * clipPosition));
                    poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                    poseStack.translate(clipPosition, -0.5F * clipPosition / model.type.getModelScale(), 0F);
                }
                case P90, CUSTOMP90 ->
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * model.reloadRotate * model.reloadRotate));
                    poseStack.translate(0F, 0.075F * model.reloadRotate, 0F);
                    poseStack.translate(-2F * clipPosition / model.type.getModelScale(), -0.3F * clipPosition / model.type.getModelScale(), 0.5F * clipPosition / model.type.getModelScale());
                }
                case RIFLE ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                    poseStack.translate(bulletProgress * -1F / model.type.getModelScale(), 0F, bulletProgress * 0.5F / model.type.getModelScale());
                }
                case CUSTOMRIFLE ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * model.rotateClipVertical));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * model.rotateClipHorizontal));
                    poseStack.mulPose(Axis.XP.rotationDegrees(bulletProgress * model.tiltClip));
                    poseStack.translate(bulletProgress * model.translateClip.x / model.type.getModelScale(), bulletProgress * model.translateClip.y / model.type.getModelScale(), bulletProgress * model.translateClip.z / model.type.getModelScale());
                }
                case RIFLE_TOP, CUSTOMRIFLE_TOP ->
                {
                    float ammoPosition = clipPosition * getNumBulletsInReload(model, animations);
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                    poseStack.translate(bulletProgress * -0.1F / model.type.getModelScale(), bulletProgress / model.type.getModelScale(), bulletProgress * 0.5F / model.type.getModelScale());
                }
                case SHOTGUN, STRIKER, CUSTOMSHOTGUN, CUSTOMSTRIKER ->
                {
                    float maxBullets = getNumBulletsInReload(model, animations);
                    float ammoPosition = clipPosition * maxBullets;
                    int bulletNum = Mth.floor(ammoPosition);
                    float bulletProgress = ammoPosition - bulletNum;

                    poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                    poseStack.translate(bulletProgress * -0.5F * 1 / model.type.getModelScale(), bulletProgress * -1F * 1 / model.type.getModelScale(), 0F);
                }
                case CUSTOM ->
                {
                    // Staged reload allows you to change the animation route halfway through
                    if (effectiveReloadAnimationProgress > 0.5 && model.stagedReload)
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.stagedrotateClipVertical * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.stagedrotateClipHorizontal * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.stagedtiltClip * clipPosition));
                        poseStack.translate(model.stagedtranslateClip.x * clipPosition / model.type.getModelScale(), model.stagedtranslateClip.y * clipPosition / model.type.getModelScale(), model.stagedtranslateClip.z * clipPosition / model.type.getModelScale());
                    }
                    else
                    {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-model.rotateClipVertical * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.rotateClipHorizontal * clipPosition));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.tiltClip * clipPosition));
                        poseStack.translate(-model.translateClip.z * clipPosition / model.type.getModelScale(), model.translateClip.y * clipPosition / model.type.getModelScale(), model.translateClip.x * clipPosition / model.type.getModelScale());
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
        poseStack.scale(model.leftArmScale.x, model.leftArmScale.y, model.leftArmScale.z);
        if (model.leftHandAmmo)
            playerModel.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
    }

    // right hand pump action animation
    private static void renderArmPump(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        poseStack.translate(-(armPosition.x - Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.pumpModifier), armPosition.y, armPosition.z);
        handleRotate(rotationPoint, model, poseStack);
    }

    // This moves the right hand if leftHandAmmo & handCharge are true (For left hand reload with right hand charge)
    private static void renderArmCharge(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(
            -(armPosition.x - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.x),
            -(armPosition.y - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.y),
            -(armPosition.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)
        );
    }

    // This moves the right hand if leftHandAmmo & handBolt are true (For left hand reload with right hand bolt action)
    private static void renderArmBolt(ModelGun model, GunAnimations anim, float smoothing, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(
            armPosition.x + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.x,
            armPosition.y + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.y,
            -(armPosition.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)
        );
    }

    private static void renderArmDefault(ModelGun model, Vector3f rotationPoint, Vector3f armPosition, PoseStack poseStack)
    {
        handleRotate(rotationPoint, model, poseStack);
        poseStack.translate(armPosition.x, armPosition.y, armPosition.z);
    }

    private static void handleRotate(Vector3f rotationPoint, ModelGun model, PoseStack poseStack)
    {
        if (model.easyArms)
            poseStack.translate(0.4F * model.armScale.getX(), 0.75F * model.armScale.getY(), -0F * model.armScale.getZ());
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationPoint.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationPoint.z));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationPoint.x));
        if (model.easyArms)
            poseStack.translate(-0.4F * model.armScale.getX(), -0.75F * model.armScale.getY(), 0F * model.armScale.getZ());
    }
}
