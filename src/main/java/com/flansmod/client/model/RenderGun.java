package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.ERenderPass;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IScope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> applyFirstPersonAdjustments(model, stack, poseStack, ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND, animations);
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> applyThirdPersonAdjustments(model, poseStack, ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND, animations);
                case FIXED -> applyFixedEntityAdjustments(model, poseStack);
                case GROUND -> poseStack.translate(model.itemFrameOffset.x, model.itemFrameOffset.y, model.itemFrameOffset.z);
                default ->
                {
                    // No Adjustments
                }
            }

            poseStack.pushPose();
            poseStack.scale(modelScale, modelScale, modelScale);
            renderFlash(model, stack, animations, poseStack, buffer, packedOverlay);
            for (ERenderPass renderPass : ERenderPass.ORDER)
                renderGunAndComponents(model, stack, animations, poseStack, buffer.getBuffer(renderPass.getRenderType(gunTexture)), packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
            renderAttachmentAmmo(model, stack, poseStack, buffer, packedLight, packedOverlay);
            renderCasingEjection(model, animations, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
            renderMuzzleFlash(model, stack, animations, poseStack, buffer, packedOverlay);
            renderCustomAttachments(model, stack, animations, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static boolean shouldRenderGun(ModelGun model, ItemDisplayContext itemDisplayContext, ItemStack item) {
        if (itemDisplayContext.firstPerson())
            return !(ModClient.getZoomProgress() > 0.9F && model.type.getCurrentScope(item).hasZoomOverlay() && !model.stillRenderGunWhenScopedOverlay);
        return true;
    }

    private static void applyFixedEntityAdjustments(ModelGun model, PoseStack poseStack)
    {
        poseStack.translate(0.2F + model.itemFrameOffset.x, -0.2F + model.itemFrameOffset.y, model.itemFrameOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    private static void applyFirstPersonAdjustments(ModelGun model, ItemStack stack, PoseStack poseStack, boolean leftHand, GunAnimations animations)
    {
        float smoothing = Minecraft.getInstance().getFrameTime();
        float adsSwitch = ModClient.getLastZoomProgress() + (ModClient.getZoomProgress() - ModClient.getLastZoomProgress()) * smoothing;
        boolean crouching = ModClient.getZoomProgress() + 0.1F > 0.9F && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCrouching() && !animations.reloading;

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

        IScope scope = model.type.getCurrentScope(stack);

        applyMeleeMovement(poseStack, model.type, animations, smoothing);

        // Look at gun stuff
        float interp = animations.lookAtTimer + smoothing;
        interp /= GunAnimations.lookAtTimes[animations.lookAt.ordinal()];

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
        poseStack.translate(startPos.x + (endPos.x - startPos.x) * interp,
                startPos.y + (endPos.y - startPos.y) * interp,
                startPos.z + (endPos.z - startPos.z) * interp);

        //TODO: fix recoil
        //poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.recoilAngle * (float)Math.sqrt(type.getRecoil()) * 1.5f));
        poseStack.translate(animations.recoilOffset.x, animations.recoilOffset.y, animations.recoilOffset.z);

        /*if(spinningCocking)
        {
            poseStack.translate(spinPoint.x, spinPoint.y, spinPoint.z);
            float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing);
            GlStateManager.rotate(pumped * 180F + 180F, 0F, 0F, 1F);
            poseStack.translate(-spinPoint.x, -spinPoint.y, -spinPoint.z);
        }*/

        /*if(animations.reloading)
        {
            //Calculate the amount of tilt required for the reloading animation
            float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
            reloadRotate = 1F;
            if(effectiveReloadAnimationProgress < tiltGunTime)
                reloadRotate = effectiveReloadAnimationProgress / tiltGunTime;
            if(effectiveReloadAnimationProgress > tiltGunTime + unloadClipTime + loadClipTime)
                reloadRotate = 1F - (effectiveReloadAnimationProgress - (tiltGunTime + unloadClipTime + loadClipTime)) / untiltGunTime;

            //Rotate the gun dependent on the animation type
            switch(animationType)
            {
                case BOTTOM_CLIP: case PISTOL_CLIP: case SHOTGUN: case END_LOADED:
                {
                    GlStateManager.rotate(60F * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(30F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.25F * reloadRotate, 0F, 0F);
                    break;
                }
                case BACK_LOADED:
                {
                    GlStateManager.rotate(-75F * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(-30F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.5F * reloadRotate, 0F, 0F);
                    break;
                }
                case BULLPUP:
                {
                    GlStateManager.rotate(70F * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(10F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.5F * reloadRotate, -0.2F * reloadRotate, 0F);
                    break;
                }
                case RIFLE:
                {
                    GlStateManager.rotate(30F * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(-30F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.5F * reloadRotate, 0F, -0.5F * reloadRotate);
                    break;
                }
                case RIFLE_TOP: case REVOLVER:
                {
                    GlStateManager.rotate(30F * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(10F * reloadRotate, 0F, 1F, 0F);
                    GlStateManager.rotate(-10F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.1F * reloadRotate, -0.2F * reloadRotate, -0.1F * reloadRotate);
                    break;
                }
                case ALT_PISTOL_CLIP:
                {
                    GlStateManager.rotate(60F * reloadRotate * flip, 0F, 1F, 0F);
                    poseStack.translate(0.15F * reloadRotate, 0.25F * reloadRotate, 0F);
                    break;
                }
                case STRIKER:
                {
                    GlStateManager.rotate(-35F * reloadRotate * flip, 1F, 0F, 0F);
                    poseStack.translate(0.2F * reloadRotate, 0F, -0.1F * reloadRotate);
                    break;
                }
                case GENERIC:
                {
                    //Gun reloads partly or completely off-screen.
                    GlStateManager.rotate(45F * reloadRotate, 0F, 0F, 1F);
                    poseStack.translate(-0.2F * reloadRotate, -0.5F * reloadRotate, 0F);
                    break;
                }
                case CUSTOM:
                {
                    GlStateManager.rotate(rotateGunVertical * reloadRotate, 0F, 0F, 1F);
                    GlStateManager.rotate(rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
                    GlStateManager.rotate(tiltGun * reloadRotate, 1F, 0F, 0F);
                    poseStack.translate(translateGun.x * reloadRotate, translateGun.y * reloadRotate, translateGun.z * reloadRotate);
                    break;
                }
                default: break;
            }
        }*/
    }

    private static void applyThirdPersonAdjustments(ModelGun model, PoseStack poseStack, boolean leftHand, GunAnimations animations)
    {
        float smoothing = Minecraft.getInstance().getFrameTime();

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.translate(-0.08F, -0.12F, 0F);
        poseStack.translate(model.thirdPersonOffset.x, model.thirdPersonOffset.y, model.thirdPersonOffset.z);

        //TODO: config option to disable animations in 3rd Person
        applyMeleeMovement(poseStack, model.type, animations, Minecraft.getInstance().getFrameTime());
    }

    public static void applyMeleeMovement(PoseStack poseStack, GunType gunType, GunAnimations animations, float smoothing)
    {
        int i = animations.meleeAnimationProgress;
        if (i <= 0 || i >= gunType.getMeleePath().size())
            return;

        float t = Mth.clamp(smoothing, 0.0f, 1.0f);

        Vector3f p0 = gunType.getMeleePath().get(i);
        Vector3f p1 = (i + 1 < gunType.getMeleePath().size()) ? gunType.getMeleePath().get(i + 1) : new Vector3f();

        float x = Mth.lerp(t, p0.x, p1.x);
        float y = Mth.lerp(t, p0.y, p1.y);
        float z = Mth.lerp(t, p0.z, p1.z);

        poseStack.translate(x, y, z);

        Vector3f a0 = gunType.getMeleePathAngles().get(i);
        Vector3f a1 = (i + 1 < gunType.getMeleePathAngles().size()) ? gunType.getMeleePathAngles().get(i + 1) : new Vector3f();

        float yaw = Mth.lerp(t, a0.y, a1.y);
        float roll = Mth.lerp(t, a0.z, a1.z);
        float pitch = Mth.lerp(t, a0.x, a1.x);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private static void renderGunAndComponents(ModelGun model, ItemStack stack, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        float smoothing = Minecraft.getInstance().getFrameTime();

        //Render the gun and default attachment models
        poseStack.pushPose();

        //Get all the attachments that we may need to render
        AttachmentType scopeAttachment = model.type.getScope(stack);
        AttachmentType barrelAttachment = model.type.getBarrel(stack);
        AttachmentType stockAttachment = model.type.getStock(stack);
        AttachmentType gripAttachment = model.type.getGrip(stack);
        AttachmentType gadgetAttachment = model.type.getGadget(stack);
        AttachmentType slideAttachment = model.type.getSlide(stack);
        AttachmentType pumpAttachment = model.type.getPump(stack);
        AttachmentType accessoryAttachment = model.type.getAccessory(stack);

        ItemStack scopeItemStack = model.type.getScopeItemStack(stack);
        ItemStack barrelItemStack = model.type.getBarrelItemStack(stack);
        ItemStack stockItemStack = model.type.getStockItemStack(stack);
        ItemStack gripItemStack = model.type.getGripItemStack(stack);

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

        renderBulletCounterModels(model, stack, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderSlideModels(model, stack, animations, slideAttachment, scopeAttachment, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderBreakAction(model, scopeAttachment, getReloadRotate(model, animations), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderHammer(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderPumpAction(model, animations, pumpAttachment, gripAttachment, gadgetAttachment, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderBoltAction(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderChargeHandle(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderMinigunBarrels(model, animations, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        renderRevolverBarrel(model, getReloadRotate(model, animations), poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        //TODO: renderAmmo(model, type, gripAttachment, item, empty, animations, getReloadRotate(model, animations), rtype, f, gripItemStack);
        //Render the clip
        poseStack.pushPose();
        {
            boolean shouldRender = true;
            //Check to see if the ammo should be rendered first
            /*switch(animationType)
            {
                case END_LOADED: case BACK_LOADED:
                {
                    if(empty)
                        shouldRender = false;
                    break;
                }
                default: break;
            }*/
            //If it should be rendered, do the transformations required
            if (shouldRender && animations.reloading && Minecraft.getInstance().options.getCameraType().isFirstPerson())
            {
                //Calculate the amount of tilt required for the reloading animation
                float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
                float clipPosition = 0F;
                if(effectiveReloadAnimationProgress > model.tiltGunTime && effectiveReloadAnimationProgress < model.tiltGunTime + model.unloadClipTime)
                    clipPosition = (effectiveReloadAnimationProgress - model.tiltGunTime) / model.unloadClipTime;
                if(effectiveReloadAnimationProgress >= model.tiltGunTime + model.unloadClipTime && effectiveReloadAnimationProgress < model.tiltGunTime + model.unloadClipTime + model.loadClipTime)
                    clipPosition = 1F - (effectiveReloadAnimationProgress - (model.tiltGunTime + model.unloadClipTime)) / model.loadClipTime;

                float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - model.tiltGunTime) / (model.unloadClipTime + model.loadClipTime))));

                //Rotate the gun dependent on the animation type
                switch(model.animationType)
                {
                    case BREAK_ACTION ->
                    {
                        poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.reloadRotate * -model.breakAngle));
                        poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
                        poseStack.translate(-1F * clipPosition, 0F, 0F);
                    }
                    case REVOLVER ->
                    {
                        poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolverFlipAngle));
                        poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
                        poseStack.translate(-1F * clipPosition, 0F, 0F);
                    }
                    case BOTTOM_CLIP ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-180F * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                        poseStack.translate(0.5F * clipPosition, 0F, 0F);
                    }
                    case PISTOL_CLIP ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-90F * clipPosition * clipPosition));
                        poseStack.translate(0F, -1F * clipPosition, 0F);
                    }
                    case ALT_PISTOL_CLIP ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(5F * clipPosition));
                        poseStack.translate(0F, -3F * clipPosition, 0F);
                    }
                    case SIDE_CLIP ->
                    {
                        poseStack.mulPose(Axis.YP.rotationDegrees(180F * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(60F * clipPosition));
                        poseStack.translate(0.5F * clipPosition, 0F, 0F);
                    }
                    case BULLPUP ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-150F * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(60F * clipPosition));
                        poseStack.translate(clipPosition, -0.5F * clipPosition, 0F);
                    }
                    case P90 ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * model.reloadRotate * model.reloadRotate));
                        poseStack.translate(0F, 0.075F * model.reloadRotate, 0F);
                        poseStack.translate(-2F * clipPosition, -0.3F * clipPosition, 0.5F * clipPosition);
                    }
                    case RIFLE ->
                    {
                        float thing = clipPosition * model.numBulletsInReloadAnimation;
                        int bulletNum = Mth.floor(thing);
                        float bulletProgress = thing - bulletNum;

                        poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                        poseStack.translate(bulletProgress * -1F, 0F, bulletProgress * 0.5F);
                    }
                    case RIFLE_TOP ->
                    {
                        float thing = clipPosition * model.numBulletsInReloadAnimation;
                        int bulletNum = Mth.floor(thing);
                        float bulletProgress = thing - bulletNum;

                        poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                        poseStack.translate(bulletProgress * -0.1F, bulletProgress, bulletProgress * 0.5F);
                    }
                    case SHOTGUN, STRIKER ->
                    {
                        float thing = clipPosition * model.numBulletsInReloadAnimation;
                        int bulletNum = Mth.floor(thing);
                        float bulletProgress = thing - bulletNum;

                        poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                        poseStack.translate(bulletProgress * -0.5F, bulletProgress * -1F, 0F);
                    }
                    case CUSTOM ->
                    {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(model.rotateClipVertical * clipPosition));
                        poseStack.mulPose(Axis.YP.rotationDegrees(model.rotateClipHorizontal * clipPosition));
                        poseStack.mulPose(Axis.XP.rotationDegrees(model.tiltClip * clipPosition));
                        poseStack.translate(model.translateClip.x * clipPosition, model.translateClip.y * clipPosition, model.translateClip.z * clipPosition);
                    }
                    case END_LOADED ->
                    {
                        float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

                        poseStack.mulPose(Axis.ZP.rotationDegrees(-45F * dYaw));
                        poseStack.translate(-model.endLoadedAmmoDistance * dYaw, -0.5F * dYaw, 0F);

                        float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);

                        poseStack.translate(model.endLoadedAmmoDistance * xDisplacement, 0F, 0F);
                    }
                    case BACK_LOADED ->
                    {
                        float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

                        poseStack.translate(model.endLoadedAmmoDistance * dYaw, -0.5F * dYaw, 0F);

                        float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);

                        poseStack.translate(-model.endLoadedAmmoDistance * xDisplacement, 0F, 0F);
                    }
                }
            }

            if (shouldRender)
                model.render(model.ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
        poseStack.popPose();

        renderStaticAmmo(model, stack, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);

        poseStack.popPose();
    }

    private static void renderBulletCounterModels(ModelGun model, ItemStack stack, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        if (renderPass != ERenderPass.GLOW_ALPHA || (!model.isBulletCounterActive && !model.isAdvBulletCounterActive))
            return;

        final int numRounds = countRoundsInGun(stack);

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

    private static void renderSlideModels(ModelGun model, ItemStack stack, GunAnimations animations, AttachmentType slideAttachment, AttachmentType scopeAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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

    private static void renderBreakAction(ModelGun model, AttachmentType scopeAttachment, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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

    private static void renderHammer(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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

    private static void renderPumpAction(ModelGun model, GunAnimations animations, AttachmentType pumpAttachment, AttachmentType gripAttachment, AttachmentType gadgetAttachment, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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
            if (ModClient.getShotState() != -1 && -(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.pumpHandleDistance != -0.0F)
            {
                ModClient.setShotState(-1);
                //TODO: should this stay client only?
                if (StringUtils.isNotBlank(model.type.getActionSound()))
                    FlansMod.getSoundEvent(model.type.getActionSound()).ifPresent(soundEvent -> Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent.get(), 1.0F)));
            }
            poseStack.popPose();
        }
    }

    private static void renderBoltAction(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.boltCycleDistance, 0F, 0F);
        poseStack.translate(model.boltRotationOffset.x, model.boltRotationOffset.y, model.boltRotationOffset.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.boltRotationAngle));
        poseStack.translate(-model.boltRotationOffset.x, -model.boltRotationOffset.y, -model.boltRotationOffset.z);
        model.render(model.boltActionModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();

        if (ModClient.getShotState() != -1 && -(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * Minecraft.getInstance().getFrameTime())) * model.boltCycleDistance != -0.0)
        {
            ModClient.setShotState(-1);
            //TODO: should this stay client only?
            if (StringUtils.isNotBlank(model.type.getActionSound()))
                FlansMod.getSoundEvent(model.type.getActionSound()).ifPresent(soundEvent -> Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent.get(), 1.0F)));
        }
    }

    private static void renderChargeHandle(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        if (model.chargeHandleDistance != 0F)
        {
            poseStack.pushPose();
            poseStack.translate(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * Minecraft.getInstance().getFrameTime())) * model.chargeHandleDistance, 0F, 0F);
            model.render(model.chargeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    private static void renderMinigunBarrels(ModelGun model, GunAnimations animations, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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

    private static void renderRevolverBarrel(ModelGun model, float reloadRotate, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
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

    private static void renderStaticAmmo(ModelGun model, ItemStack stack, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, ERenderPass renderPass)
    {
        if (model.type.getSecondaryFire(stack))
        {
            poseStack.pushPose();
            model.render(model.ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    //TODO: check reload rotate logic
    private static float getReloadRotate(ModelGun model, GunAnimations animations)
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

    private static void renderAttachmentAmmo(ModelGun model, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        AttachmentType gripAttachment = model.type.getGrip(stack);
        ItemStack gripItemStack = model.type.getGripItemStack(stack);

        if (!model.type.getSecondaryFire(stack) && gripAttachment != null && ModelCache.getOrLoadTypeModel(gripAttachment) instanceof ModelAttachment gripModel)
        {
            int color = gripAttachment.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            float modelScale = gripAttachment.getModelScale();
            ResourceLocation ammoTexture = gripAttachment.getPaintjob(gripItemStack).getTexture();
            for (ERenderPass renderPass : ERenderPass.ORDER)
                gripModel.renderAttachmentAmmo(poseStack, buffer.getBuffer(renderPass.getRenderType(ammoTexture)), packedLight, packedOverlay, red, green, blue, 1F, modelScale, renderPass);
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
            for (ERenderPass renderPass : ERenderPass.ORDER)
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
                    preRenderAttachment(attachment, model.scopeAttachPoint, poseStack);
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
                    preRenderAttachment(attachment, model.gripAttachPoint, poseStack);
                    if (model.gripIsOnPump)
                        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                    renderAttachment(attachment, gripItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case BARREL:
                    preRenderAttachment(attachment, model.barrelAttachPoint, poseStack);
                    renderAttachment(attachment, barrelItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case STOCK:
                    preRenderAttachment(attachment, model.stockAttachPoint, poseStack);
                    renderAttachment(attachment, stockItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case SLIDE:
                    preRenderAttachment(attachment, model.slideAttachPoint, poseStack);
                    poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
                    renderAttachment(attachment, slideItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case GADGET:
                    preRenderAttachment(attachment, model.gadgetAttachPoint, poseStack);
                    if (model.gadgetIsOnPump)
                        poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                    renderAttachment(attachment, gadgetItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case ACCESSORY:
                    preRenderAttachment(attachment, model.accessoryAttachPoint, poseStack);
                    renderAttachment(attachment, accessoryItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                case PUMP:
                    preRenderAttachment(attachment, model.pumpAttachPoint, poseStack);
                    poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                    renderAttachment(attachment, pumpItemStack, poseStack, buffer, packedLight, packedOverlay);
                    break;
                default:
                    break;
            }
            poseStack.popPose();
        }
    }

    private static void preRenderAttachment(AttachmentType attachment, Vector3f attachPoint, PoseStack poseStack)
    {
        float modelScale = attachment.getModelScale();
        poseStack.translate(attachPoint.x * modelScale, attachPoint.y * modelScale, attachPoint.z * modelScale);
        poseStack.scale(modelScale, modelScale, modelScale);
    }

    private static void renderAttachment(AttachmentType attachment, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (ModelCache.getOrLoadTypeModel(attachment) instanceof ModelAttachment modelAttachment)
        {
            int color = attachment.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            ResourceLocation attachmentTexture = attachment.getPaintjob(stack).getTexture();
            for (ERenderPass renderPass : ERenderPass.ORDER)
                modelAttachment.renderAttachment(poseStack, buffer.getBuffer(renderPass.getRenderType((attachmentTexture))), packedLight, packedOverlay, red, green, blue, 1F, 1F, renderPass);
        }
    }
}
