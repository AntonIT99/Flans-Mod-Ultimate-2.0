package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.IScope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderGun
{
    public static void renderItem(ModelGun model, ItemStack item, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        poseStack.pushPose();
        float modelScale = model.type != null ? model.type.getModelScale() : 1F;
        GunAnimations animations = ModClient.getGunAnimations(ctx);
        model.reloadRotate = 0F;

        if (shouldRenderGun(model, ctx, item))
        {
            switch (ctx)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
                {
                    boolean left = (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                    applyFirstPersonAdjustments(model, item, poseStack, left, animations);
                }
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                {
                    boolean left = (ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                    applyThirdPersonAdjustments(model, poseStack, left);
                }
                case GROUND, FIXED -> poseStack.translate(model.itemFrameOffset.x, model.itemFrameOffset.y, model.itemFrameOffset.z);
                default ->
                {
                    // No custom item rendering
                }
            }
            renderGun(model, item, animations, poseStack, buffer, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale);
        }
        poseStack.popPose();
    }

    private static boolean shouldRenderGun(ModelGun model, ItemDisplayContext itemDisplayContext, ItemStack item) {
        if (itemDisplayContext.firstPerson())
            return !(ModClient.getZoomProgress() > 0.9F && model.type.getCurrentScope(item).hasZoomOverlay() && !model.stillRenderGunWhenScopedOverlay);
        return true;
    }

    private static void applyFirstPersonAdjustments(ModelGun model, ItemStack item, PoseStack poseStack, boolean leftHand, GunAnimations animations)
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

        IScope scope = model.type.getCurrentScope(item);

        /*if(animations.meleeAnimationProgress > 0 && animations.meleeAnimationProgress < gunType.meleePath.size())
        {
            Vector3f meleePos = gunType.meleePath.get(animations.meleeAnimationProgress);
            Vector3f nextMeleePos = animations.meleeAnimationProgress + 1 < gunType.meleePath.size() ? gunType.meleePath.get(animations.meleeAnimationProgress + 1) : new Vector3f();
            poseStack.translate(meleePos.x + (nextMeleePos.x - meleePos.x) * smoothing, meleePos.y + (nextMeleePos.y - meleePos.y) * smoothing, meleePos.z + (nextMeleePos.z - meleePos.z) * smoothing);
            Vector3f meleeAngles = gunType.meleePathAngles.get(animations.meleeAnimationProgress);
            Vector3f nextMeleeAngles = animations.meleeAnimationProgress + 1 < gunType.meleePathAngles.size() ? gunType.meleePathAngles.get(animations.meleeAnimationProgress + 1) : new Vector3f();
            GlStateManager.rotate(meleeAngles.y + (nextMeleeAngles.y - meleeAngles.y) * smoothing, 0F, 1F, 0F);
            GlStateManager.rotate(meleeAngles.z + (nextMeleeAngles.z - meleeAngles.z) * smoothing, 0F, 0F, 1F);
            GlStateManager.rotate(meleeAngles.x + (nextMeleeAngles.x - meleeAngles.x) * smoothing, 1F, 0F, 0F);
        }*/

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

        switch(animations.lookAt)
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

    private static void applyThirdPersonAdjustments(ModelGun model, PoseStack poseStack, boolean leftHand)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.translate(-0.08F, -0.12F, 0F);
        poseStack.translate(model.thirdPersonOffset.x, model.thirdPersonOffset.y, model.thirdPersonOffset.z);

    }

    /**
     * Gun render method, seperated from transforms so that mecha renderer may also call this
     */
    public static void renderGun(ModelGun model, ItemStack item, GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale)
    {
        float smoothing = Minecraft.getInstance().getFrameTime();

        //Render the gun and default attachment models
        poseStack.pushPose();
        {
            //Get all the attachments that we may need to render
            AttachmentType scopeAttachment = model.type.getScope(item);
            AttachmentType barrelAttachment = model.type.getBarrel(item);
            AttachmentType stockAttachment = model.type.getStock(item);
            AttachmentType gripAttachment = model.type.getGrip(item);

            ItemStack scopeItemStack = model.type.getScopeItemStack(item);
            ItemStack barrelItemStack = model.type.getBarrelItemStack(item);
            ItemStack stockItemStack = model.type.getStockItemStack(item);
            ItemStack gripItemStack = model.type.getGripItemStack(item);

            renderMuzzleFlash(model, animations, barrelAttachment, item, poseStack, buffer, packedOverlay, red, green, blue, alpha, scale);
            model.render(model.gunModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            model.renderCustom(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, animations);
            if (scopeAttachment == null && !model.scopeIsOnSlide && !model.scopeIsOnBreakAction)
                model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            if (barrelAttachment == null)
                model.render(model.defaultBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            if (stockAttachment == null)
                model.render(model.defaultStockModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            if (gripAttachment == null && !model.gripIsOnPump)
                model.render(model.defaultGripModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);

            //Render various shoot / reload animated parts
            //Render the slide
            poseStack.pushPose();
            {
                poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
                model.render(model.slideModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                if (scopeAttachment == null && model.scopeIsOnSlide)
                    model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the break action
            poseStack.pushPose();
            {
                poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
                poseStack.mulPose(Axis.ZP.rotationDegrees(model.reloadRotate * -model.breakAngle));
                poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
                model.render(model.breakActionModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                if (scopeAttachment == null && model.scopeIsOnBreakAction)
                    model.render(model.defaultScopeModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the pump-action handle
            poseStack.pushPose();
            {
                poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
                model.render(model.pumpModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                if (gripAttachment == null && model.gripIsOnPump)
                    model.render(model.defaultGripModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the minigun barrels
            if (model.type.getMode() == EnumFireMode.MINIGUN)
            {
                poseStack.pushPose();
                poseStack.translate(model.minigunBarrelOrigin.x, model.minigunBarrelOrigin.y, model.minigunBarrelOrigin.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(animations.minigunBarrelRotation));
                poseStack.translate(-model.minigunBarrelOrigin.x, -model.minigunBarrelOrigin.y, -model.minigunBarrelOrigin.z);
                model.render(model.minigunBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                poseStack.popPose();
            }

            //Render the cocking handle

            //Render the revolver barrel
            poseStack.pushPose();
            {
                poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(model.reloadRotate * model.revolverFlipAngle));
                poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
                model.render(model.revolverBarrelModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

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
                    model.render(model.ammoModel, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderMuzzleFlash(ModelGun model, GunAnimations animations, AttachmentType barrelAttachment, ItemStack item, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay, float red, float green, float blue, float alpha, float scale)
    {
        boolean isFlashEnabled = barrelAttachment == null || !barrelAttachment.isDisableMuzzleFlash();
        ModelFlash flash = ModelCache.getOrLoadFlashModel(model.type);

        if (isFlashEnabled && animations.muzzleFlashTime > 0 && flash != null && !model.type.getSecondaryFire(item))
        {
            poseStack.pushPose();
            {
                poseStack.scale(model.flashScale, model.flashScale, model.flashScale);
                Vector3f base = model.muzzleFlashPoint == null ? Vector3f.Zero : model.muzzleFlashPoint;
                if (barrelAttachment != null)
                {
                    //TODO: implement attachment model
                    //Vector3f barrelOffset = (barrelAttachment.model != null && barrelAttachment.model.attachmentFlashOffset != null) ? barrelAttachment.model.attachmentFlashOffset : Vector3f.Zero;
                    //poseStack.translate(base.x + barrelOffset.x, base.y + barrelOffset.y, base.z + barrelOffset.z);
                }
                else
                {
                    Vector3f defaultOffset = model.defaultBarrelFlashPoint == null ? Vector3f.Zero : model.defaultBarrelFlashPoint;

                    poseStack.translate(base.x + defaultOffset.x, base.y + defaultOffset.y, base.z + defaultOffset.z);
                }

                //TODO: fix issue with texture of gun
                VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(model.type.getFlashTexture()));
                flash.renderFlash(animations.flashInt, poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();
        }
    }
}
