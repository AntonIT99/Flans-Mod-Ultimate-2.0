package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.client.model.IFlanModel;
import com.wolffsarmormod.common.guns.EnumFireMode;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsmod.client.model.ModelRenderer;
import com.wolffsmod.client.model.TextureOffset;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelGun extends Model implements IFlanModel<GunType>
{
    protected static final Vector3f invalid = new Vector3f(0f, Float.MAX_VALUE, 0f);

    @Setter
    protected GunType type;

    /** Static models with no animation */
    protected ModelRendererTurbo[] gunModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] backpackModel = new ModelRendererTurbo[0]; //For flamethrowers and such like. Rendered on the player's back

    /** These models appear when no attachment exists */
    protected ModelRendererTurbo[] defaultBarrelModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] defaultScopeModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] defaultStockModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] defaultGripModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] defaultGadgetModel = new ModelRendererTurbo[0];

    /** Animated models */
    protected ModelRendererTurbo[] ammoModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] fullammoModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] revolverBarrelModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] revolver2BarrelModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] breakActionModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] altbreakActionModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] slideModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] altslideModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] pumpModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] chargeModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] altpumpModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] boltActionModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] minigunBarrelModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] leverActionModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] hammerModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] althammerModel = new ModelRendererTurbo[0];

    /**
     * Bullet Counter Models. Can be used to display bullet count in-game interface.
     * Each part is represented by number of rounds remaining per magazine.
     * <p>
     * - Simple counter will loop through each part. Allows flexibility for bullet counter UI design.
     * <p>
     * - Adv counter used for counting mags of more than 10, to reduce texture parts. Divides count into digits.
     *	 Less flexibility as it requires 10 textures parts at maximum (numbers 0-9).
     */
    protected ModelRendererTurbo[] bulletCounterModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[][] advBulletCounterModel = new ModelRendererTurbo[0][0];
    /** For Adv Bullet Counter. Reads in numbers from left hand side when false */
    protected boolean countOnRightHandSide;
    /** Toggle the counters active. Saves render performance. */
    protected boolean isBulletCounterActive;
    protected boolean isAdvBulletCounterActive;

    /** The point about which the minigun barrel rotates. Rotation is along the line of the gun through this point */
    protected Vector3f minigunBarrelOrigin = new Vector3f();
    protected Vector3f minigunBarrelSpinDirection = new Vector3f(1F, 0F, 0F);
    protected float minigunBarrelSpinSpeed = 1F;

    /** These designate the locations of 3D attachment models on the gun */
    @Getter
    protected Vector3f barrelAttachPoint = new Vector3f();
    @Getter
    protected Vector3f scopeAttachPoint = new Vector3f();
    @Getter
    protected Vector3f stockAttachPoint = new Vector3f();
    @Getter
    protected Vector3f gripAttachPoint = new Vector3f();
    @Getter
    protected Vector3f gadgetAttachPoint = new Vector3f();
    @Getter
    protected Vector3f slideAttachPoint = new Vector3f();
    @Getter
    protected Vector3f pumpAttachPoint = new Vector3f();
    @Getter
    protected Vector3f accessoryAttachPoint = new Vector3f();

    /** Muzzle flash models */
    protected Vector3f defaultBarrelFlashPoint = null;
    protected Vector3f muzzleFlashPoint = null;
    protected boolean hasFlash;

    /** Arms rendering */
    protected boolean hasArms;
    /** Changes the rotation point to be the hand for easier animation setup */
    protected boolean easyArms;
    protected Vector3f armScale = new Vector3f(0.8F,0.8F,0.8F);
    protected Vector3f leftArmPos = new Vector3f(0,0,0);
    protected Vector3f leftArmRot = new Vector3f(0,0,0);
    protected Vector3f leftArmScale = new Vector3f(1,1,1);
    protected Vector3f rightArmPos = new Vector3f(0,0,0);
    protected Vector3f rightArmRot = new Vector3f(0,0,0);
    protected Vector3f rightArmScale = new Vector3f(1,1,1);
    protected Vector3f rightArmReloadPos = new Vector3f(0,0,0);
    protected Vector3f rightArmReloadRot = new Vector3f(0,0,0);
    protected Vector3f leftArmReloadPos = new Vector3f(0,0,0);
    protected Vector3f leftArmReloadRot = new Vector3f(0,0,0);
    protected Vector3f rightArmChargePos = new Vector3f(0,0,0);
    protected Vector3f rightArmChargeRot = new Vector3f(0,0,0);
    protected Vector3f leftArmChargePos = new Vector3f(0,0,0);
    protected Vector3f leftArmChargeRot = new Vector3f(0,0,0);
    protected Vector3f stagedrightArmReloadPos = new Vector3f(0,0,0);
    protected Vector3f stagedrightArmReloadRot = new Vector3f(0,0,0);
    protected Vector3f stagedleftArmReloadPos = new Vector3f(0,0,0);
    protected Vector3f stagedleftArmReloadRot = new Vector3f(0,0,0);
    protected boolean rightHandAmmo;
    protected boolean leftHandAmmo;

    /** Casing and muzzle flash parameters */
    //  Total distance to translate
    protected Vector3f casingAnimDistance = new Vector3f(0, 0, 16);
    //  Total range in variance for random motion
    protected Vector3f casingAnimSpread = new Vector3f(2, 4, 4);
    //  Number of ticks (I guess?) to complete movement
    protected int casingAnimTime = 20;
    //  Rotation of the casing, 180 is the total rotation. If you do not understand rotation vectors, like me, just use the standard value here.
    protected Vector3f casingRotateVector = new Vector3f(0.1F, 1F, 0.1F);
    protected Vector3f casingAttachPoint = new Vector3f(0F, 0F, 0F);
    // Time before the casing is ejected from gun
    protected int casingDelay = 0;
    // Scale the bullet casing separately from gun
    protected float caseScale = 1F;
    protected float flashScale = 1F;

    /** Recoil and slide based parameters */
    protected float gunSlideDistance = 1F / 4F;
    protected float altgunSlideDistance = 1F / 4F;
    protected float RecoilSlideDistance = 2F / 16F;
    protected float RotateSlideDistance = -3F;
    protected float ShakeDistance = 0F;
    /** Select an amount of recoil per shot, between 0 and 1 */
    protected float recoilAmount = 0.33F;

    /** Charge handle distance/delay/time */
    protected float chargeHandleDistance = 0F;
    protected int chargeDelay = 0;
    protected int chargeDelayAfterReload = 0;
    protected int chargeTime = 1;

    protected EnumAnimationType animationType = EnumAnimationType.NONE;
    protected EnumMeleeAnimation meleeAnimation = EnumMeleeAnimation.DEFAULT;
    protected float tiltGunTime = 0.15F, unloadClipTime = 0.35F, loadClipTime = 0.35F, untiltGunTime = 0.15F;
    /** If true, then the scope attachment will move with the top slide */
    protected boolean scopeIsOnSlide;
    /** If true, then the scope attachment will move with the break action. Can be combined with the above */
    protected boolean scopeIsOnBreakAction;
    /** For rifles and shotguns. Currently a generic reload animation regardless of how full the internal magazine already is */
    protected float numBulletsInReloadAnimation = 1;
    /** For shotgun pump handles, rifle bolts and hammer pullbacks */
    @Getter
    protected int pumpDelay = 0;
    @Getter
    protected int pumpDelayAfterReload = 0;
    @Getter
    protected int pumpTime = 1;
    protected int hammerDelay = 0;
    /** For shotgun pump handle */
    protected float pumpHandleDistance = 4F / 16F;
    /** For end loaded projectiles */
    protected float endLoadedAmmoDistance = 1F;
    /** For break action projectiles */
    protected float breakActionAmmoDistance = 1F;
    /** If true, then the grip attachment will move with the shotgun pump */
    protected boolean gripIsOnPump;
    /** If true, then the gadget attachment will move with the shotgun pump */
    protected boolean gadgetIsOnPump;
    /** The rotation point for the barrel break */
    protected Vector3f barrelBreakPoint = new Vector3f();
    protected Vector3f altbarrelBreakPoint = new Vector3f();
    /** The amount the revolver barrel flips out by */
    protected float revolverFlipAngle = 15F;
    /** The amount the revolver2 barrel flips out by */
    protected float revolver2FlipAngle = 15F;
    /** The rotation point for the revolver flip */
    protected Vector3f revolverFlipPoint = new Vector3f();
    /** The rotation point for the revolver2 flip */
    protected Vector3f revolver2FlipPoint = new Vector3f();
    /** The angle the gun is broken by for break actions */
    protected float breakAngle = 45F;
    protected float altbreakAngle = 45F;
    /** If true, then the gun will perform a spinning reload animation */
    protected boolean spinningCocking;
    /** The point, in model co-ordinates, about which the gun is spun */
    protected Vector3f spinPoint = new Vector3f();
    /** The point where the hammer will pivot and spin from */
    protected Vector3f hammerSpinPoint = new Vector3f();
    protected Vector3f althammerSpinPoint = new Vector3f();
    protected float hammerAngle = 75F;
    protected float althammerAngle = 75F;
    /** Single action cocking check */
    protected boolean isSingleAction;
    /** If true, lock the slide when the last bullet is fired */
    protected boolean slideLockOnEmpty;
    /** If true, move the hands with the pump action */
    protected boolean lefthandPump;
    protected boolean righthandPump;
    /** If true, move the hands with the charge action */
    protected boolean rightHandCharge;
    protected boolean leftHandCharge;
    /** If true, move the hands with the bolt action */
    protected boolean rightHandBolt;
    protected boolean leftHandBolt;
    /** How far to rotate the bolt */
    protected float boltRotationAngle = 0F;
    /** How far to translate the bolt */
    protected float boltCycleDistance = 1F;
    /** Offsets the bolt rotation point to help align it properly */
    protected Vector3f boltRotationOffset = new Vector3f(0F, 0F, 0F);
    protected float pumpModifier = 4F;
    /** Hand offset when gun is charging */
    protected Vector3f chargeModifier = new Vector3f(8F, 4F, 4F);
    /**If true, gun will translate when equipped with a sight attachment */
    protected float gunOffset = 0F;
    protected float crouchZoom = 0F;
    protected boolean fancyStance = true;
    /** deprecated, do not use, use sprintStanceTranslate */
    protected Vector3f stanceTranslate = new Vector3f();
    /** deprecated, do not use, use sprintStanceRotate */
    protected Vector3f stanceRotate = new Vector3f();
    protected Vector3f sprintStanceTranslate = new Vector3f();
    protected Vector3f sprintStanceRotate = new Vector3f();

    /** Custom reload Parameters. If Enum.CUSTOM is set, these parameters can build an animation within the gun model classes */
    protected float rotateGunVertical = 0F;
    protected float rotateGunHorizontal = 0F;
    protected float tiltGun = 0F;
    protected Vector3f translateGun = new Vector3f(0F, 0F, 0F);
    /** Ammo Model reload parameters */
    protected float rotateClipVertical = 0F;
    protected float stagedrotateClipVertical = 0F;
    protected float rotateClipHorizontal = 0F;
    protected float stagedrotateClipHorizontal = 0F;
    protected float tiltClip = 0F;
    protected float stagedtiltClip = 0F;
    protected Vector3f translateClip = new Vector3f(0F, 0F, 0F);
    protected Vector3f stagedtranslateClip = new Vector3f(0F, 0F, 0F);
    protected boolean stagedReload;

    /** Disables moving gun back when ADS. */
    protected boolean stillRenderGunWhenScopedOverlay;
    /** Multiplier for ADS effect (moving gun to middle, e.t.c.) */
    protected float adsEffectMultiplier = 1;
    /** This offsets the render position for third person */
    protected Vector3f thirdPersonOffset = new Vector3f();
    /** This offsets the render position for item frames */
    protected Vector3f itemFrameOffset = new Vector3f();
    /** Allows you to move the rotation helper to determine the required offsets for moving parts */
    protected Vector3f rotationToolOffset = new Vector3f(0F, 0F, 0F);

    @Getter
    private final List<ModelRenderer> boxList = new ArrayList<>();
    @Getter
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private ResourceLocation texture;

    private float smoothing;
    private float reloadRotate;

    public ModelGun()
    {
        super(RenderType::entityTranslucent);
    }

    /**
     * Flips the  Generally only for backwards compatibility
     */
    public void flipAll()
    {
        flip(gunModel);
        flip(defaultBarrelModel);
        flip(defaultScopeModel);
        flip(defaultStockModel);
        flip(defaultGripModel);
        flip(defaultGadgetModel);
        flip(ammoModel);
        flip(fullammoModel);
        flip(slideModel);
        flip(altslideModel);
        flip(pumpModel);
        flip(altpumpModel);
        flip(boltActionModel);
        flip(chargeModel);
        flip(minigunBarrelModel);
        flip(revolverBarrelModel);
        flip(revolver2BarrelModel);
        flip(breakActionModel);
        flip(altbreakActionModel);
        flip(leverActionModel);
        flip(hammerModel);
        flip(althammerModel);
        flip(bulletCounterModel);

        for(ModelRendererTurbo[] mod : advBulletCounterModel)
            flip(mod);
    }

    protected void flip(ModelRendererTurbo[] model)
    {
        for(ModelRendererTurbo part : model)
        {
            part.doMirror(false, true, true);
            part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
        }
    }

    /**
     * Translates the model
     */
    public void translateAll(float x, float y, float z)
    {
        translate(gunModel, x, y, z);
        translate(defaultBarrelModel, x, y, z);
        translate(defaultScopeModel, x, y, z);
        translate(defaultStockModel, x, y, z);
        translate(defaultGripModel, x, y, z);
        translate(defaultGadgetModel, x, y, z);
        translate(ammoModel, x, y, z);
        translate(fullammoModel, x, y, z);
        translate(slideModel, x, y, z);
        translate(altslideModel, x, y, z);
        translate(pumpModel, x, y, z);
        translate(altpumpModel, x, y, z);
        translate(boltActionModel, x, y, z);
        translate(chargeModel, x, y, z);
        translate(minigunBarrelModel, x, y, z);
        translate(revolverBarrelModel, x, y, z);
        translate(revolver2BarrelModel, x, y, z);
        translate(breakActionModel, x, y, z);
        translate(altbreakActionModel, x, y, z);
        translate(leverActionModel, x, y, z);
        translate(hammerModel, x, y, z);
        translate(althammerModel, x, y, z);
        translate(bulletCounterModel, x, y, z);
        translateAttachment(barrelAttachPoint, x, y, z);
        translateAttachment(scopeAttachPoint, x, y, z);
        translateAttachment(gripAttachPoint, x, y, z);
        translateAttachment(stockAttachPoint, x, y, z);
        translateAttachment(gadgetAttachPoint, x, y, z);
        translateAttachment(slideAttachPoint, x, y, z);
        translateAttachment(pumpAttachPoint, x, y, z);
        translateAttachment(accessoryAttachPoint, x, y, z);
    }

    protected void translate(ModelRendererTurbo[] model, float x, float y, float z)
    {
        for(ModelRendererTurbo mod : model)
        {
            mod.rotationPointX += x;
            mod.rotationPointY += y;
            mod.rotationPointZ += z;
        }
    }

    protected void translateAttachment(Vector3f vector, float x, float y, float z)
    {
        vector.x -= x / 16F;
        vector.y -= y / 16F;
        vector.z -= z / 16F;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack pPoseStack, @NotNull VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {}

    @Override
    public void renderItem(ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data)
    {
        float modelScale = type != null ? type.getModelScale() : 1F;
        GunAnimations animations = (data.length > 1 && data[1] instanceof LivingEntity living) ? ModClient.getGunAnimations(living, leftHanded) : new GunAnimations();

        smoothing = Minecraft.getInstance().getFrameTime();
        //Get the reload animation rotation
        reloadRotate = 0F;

        poseStack.pushPose();
        switch (itemDisplayContext)
        {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                if (leftHanded)
                {
                    //TODO
                    poseStack.mulPose(Axis.XP.rotationDegrees(-70F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(48F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(105F));
                    poseStack.translate(-0.1F, -0.22F, -0.15F);
                }
                else
                {
                    //poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
                    //poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
                    //poseStack.translate(0.2F, 0.05F, 0F);
                    //poseStack.scale(1F, 1F, -1F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90F));
                }
                poseStack.translate(thirdPersonOffset.x, thirdPersonOffset.y, thirdPersonOffset.z);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // TODO
                /*IScope scope = gunType.getCurrentScope(item);
                if(FlansModClient.zoomProgress > 0.9F && scope.hasZoomOverlay())
                {
                    poseStack.popPose();
                    return;
                }*/

                float adsSwitch = ModClient.lastZoomProgress + (ModClient.zoomProgress - ModClient.lastZoomProgress) * smoothing;

                if (leftHanded)
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(90F));
                    //poseStack.mulPose(Axis.YP.rotationDegrees(45F));
                    poseStack.translate(-1F, 0.675F, -1.8F);
                }
                else
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(90F));
                    //poseStack.mulPose(Axis.YP.rotationDegrees(45F));
                    //poseStack.mulPose(Axis.ZP.rotationDegrees(-5F * adsSwitch));
                    //poseStack.translate(-1F, 0.675F + 0.180F * adsSwitch, -1F - 0.395F * adsSwitch);
                    //if (type.hasScopeOverlay())
                        poseStack.translate(-0.7F * adsSwitch, -0.12F * adsSwitch, -0.05F * adsSwitch);
                    //poseStack.mulPose(Axis.ZP.rotationDegrees(4.5F * adsSwitch));
                    //poseStack.translate(0F, -0.03F * adsSwitch, 0F);
                    poseStack.translate(0F, 0F, -0.15F);
                }

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

                poseStack.mulPose(Axis.ZP.rotationDegrees(-animations.recoilAngle * (float)Math.sqrt(type.getRecoil()) * 1.5f));
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
        }

        renderGun(animations, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, modelScale);
        poseStack.popPose();
    }

    /**
     * Gun render method, seperated from transforms so that mecha renderer may also call this
     */
    public void renderGun(GunAnimations animations, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale)
    {
        

        //Render the gun and default attachment models
        poseStack.pushPose();
        {
            poseStack.scale(type.getModelScale(), type.getModelScale(), type.getModelScale());

            //Get all the attachments that we may need to render
            /*AttachmentType scopeAttachment = type.getScope(item);
            AttachmentType barrelAttachment = type.getBarrel(item);
            AttachmentType stockAttachment = type.getStock(item);
            AttachmentType gripAttachment = type.getGrip(item);

            ItemStack scopeItemStack = type.getScopeItemStack(item);
            ItemStack barrelItemStack = type.getBarrelItemStack(item);
            ItemStack stockItemStack = type.getStockItemStack(item);
            ItemStack gripItemStack = type.getGripItemStack(item);*/

            render(gunModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            renderCustom(scale, animations);
            //if (scopeAttachment == null && !scopeIsOnSlide && !scopeIsOnBreakAction)
                render(defaultScopeModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            //if (barrelAttachment == null)
                render(defaultBarrelModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            //if (stockAttachment == null)
                render(defaultStockModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            //if (gripAttachment == null && !gripIsOnPump)
                render(defaultGripModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);

            //Render various shoot / reload animated parts
            //Render the slide
            poseStack.pushPose();
            {
                poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * gunSlideDistance, 0F, 0F);
                render(slideModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                //if (scopeAttachment == null && scopeIsOnSlide)
                    render(defaultScopeModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the break action
            poseStack.pushPose();
            {
                poseStack.translate(barrelBreakPoint.x, barrelBreakPoint.y, barrelBreakPoint.z);
                poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -breakAngle));
                poseStack.translate(-barrelBreakPoint.x, -barrelBreakPoint.y, -barrelBreakPoint.z);
                render(breakActionModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                //if (scopeAttachment == null && scopeIsOnBreakAction)
                    render(defaultScopeModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the pump-action handle
            poseStack.pushPose();
            {
                poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * pumpHandleDistance, 0F, 0F);
                render(pumpModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                //if (gripAttachment == null && gripIsOnPump)
                    render(defaultGripModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();

            //Render the minigun barrels
            if (type.getMode() == EnumFireMode.MINIGUN)
            {
                poseStack.pushPose();
                poseStack.translate(minigunBarrelOrigin.x, minigunBarrelOrigin.y, minigunBarrelOrigin.z);
                poseStack.mulPose(Axis.ZP.rotationDegrees(animations.minigunBarrelRotation));
                poseStack.translate(-minigunBarrelOrigin.x, -minigunBarrelOrigin.y, -minigunBarrelOrigin.z);
                render(minigunBarrelModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
                poseStack.popPose();
            }

            //Render the cocking handle

            //Render the revolver barrel
            poseStack.pushPose();
            {
                poseStack.translate(revolverFlipPoint.x, revolverFlipPoint.y, revolverFlipPoint.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(reloadRotate * revolverFlipAngle));
                poseStack.translate(-revolverFlipPoint.x, -revolverFlipPoint.y, -revolverFlipPoint.z);
                render(revolverBarrelModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
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
                    if(effectiveReloadAnimationProgress > tiltGunTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime)
                        clipPosition = (effectiveReloadAnimationProgress - tiltGunTime) / unloadClipTime;
                    if(effectiveReloadAnimationProgress >= tiltGunTime + unloadClipTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime + loadClipTime)
                        clipPosition = 1F - (effectiveReloadAnimationProgress - (tiltGunTime + unloadClipTime)) / loadClipTime;

                    float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - tiltGunTime) / (unloadClipTime + loadClipTime))));

                    //Rotate the gun dependent on the animation type
                    switch(animationType)
                    {
                        case BREAK_ACTION ->
                        {
                            poseStack.translate(barrelBreakPoint.x, barrelBreakPoint.y, barrelBreakPoint.z);
                            poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -breakAngle));
                            poseStack.translate(-barrelBreakPoint.x, -barrelBreakPoint.y, -barrelBreakPoint.z);
                            poseStack.translate(-1F * clipPosition, 0F, 0F);
                        }
                        case REVOLVER ->
                        {
                            poseStack.translate(revolverFlipPoint.x, revolverFlipPoint.y, revolverFlipPoint.z);
                            poseStack.mulPose(Axis.XP.rotationDegrees(reloadRotate * revolverFlipAngle));
                            poseStack.translate(-revolverFlipPoint.x, -revolverFlipPoint.y, -revolverFlipPoint.z);
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
                            poseStack.mulPose(Axis.ZP.rotationDegrees(-15F * reloadRotate * reloadRotate));
                            poseStack.translate(0F, 0.075F * reloadRotate, 0F);
                            poseStack.translate(-2F * clipPosition, -0.3F * clipPosition, 0.5F * clipPosition);
                        }
                        case RIFLE ->
                        {
                            float thing = clipPosition * numBulletsInReloadAnimation;
                            int bulletNum = Mth.floor(thing);
                            float bulletProgress = thing - bulletNum;

                            poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 15F));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 15F));
                            poseStack.translate(bulletProgress * -1F, 0F, bulletProgress * 0.5F);
                        }
                        case RIFLE_TOP ->
                        {
                            float thing = clipPosition * numBulletsInReloadAnimation;
                            int bulletNum = Mth.floor(thing);
                            float bulletProgress = thing - bulletNum;

                            poseStack.mulPose(Axis.YP.rotationDegrees(bulletProgress * 55F));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * 95F));
                            poseStack.translate(bulletProgress * -0.1F, bulletProgress, bulletProgress * 0.5F);
                        }
                        case SHOTGUN, STRIKER ->
                        {
                            float thing = clipPosition * numBulletsInReloadAnimation;
                            int bulletNum = Mth.floor(thing);
                            float bulletProgress = thing - bulletNum;
    
                            poseStack.mulPose(Axis.ZP.rotationDegrees(bulletProgress * -30F));
                            poseStack.translate(bulletProgress * -0.5F, bulletProgress * -1F, 0F);
                        }
                        case CUSTOM ->
                        {
                            poseStack.mulPose(Axis.ZP.rotationDegrees(rotateClipVertical * clipPosition));
                            poseStack.mulPose(Axis.YP.rotationDegrees(rotateClipHorizontal * clipPosition));
                            poseStack.mulPose(Axis.XP.rotationDegrees(tiltClip * clipPosition));
                            poseStack.translate(translateClip.x * clipPosition, translateClip.y * clipPosition, translateClip.z * clipPosition);
                        }
                        case END_LOADED ->
                        {
                            float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
                            
                            poseStack.mulPose(Axis.ZP.rotationDegrees(-45F * dYaw));
                            poseStack.translate(-endLoadedAmmoDistance * dYaw, -0.5F * dYaw, 0F);

                            float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);

                            poseStack.translate(endLoadedAmmoDistance * xDisplacement, 0F, 0F);
                        }
                        case BACK_LOADED ->
                        {
                            float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

                            poseStack.translate(endLoadedAmmoDistance * dYaw, -0.5F * dYaw, 0F);
                            
                            float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);

                            poseStack.translate(-endLoadedAmmoDistance * xDisplacement, 0F, 0F);
                        }
                    }
                }

                if (shouldRender)
                    render(ammoModel, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public void render(ModelRendererTurbo[] models, PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for (ModelRendererTurbo mod : models)
        {
            mod.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }
    }

    public void renderCustom(float scale, GunAnimations anims) {}
}
