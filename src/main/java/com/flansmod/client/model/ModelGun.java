package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class ModelGun extends ModelBase implements IFlanTypeModel<GunType>
{
    protected static final Vector3f invalid = new Vector3f(0F, Float.MAX_VALUE, 0F);

    @Getter
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
    @Getter @Setter
    protected boolean countOnRightHandSide;
    /** Toggle the counters active. Saves render performance. */
    @Getter @Setter
    protected boolean isBulletCounterActive;
    @Getter @Setter
    protected boolean isAdvBulletCounterActive;

    /** The point about which the minigun barrel rotates. Rotation is along the line of the gun through this point */
    @Getter @Setter
    protected Vector3f minigunBarrelOrigin = new Vector3f();
    protected Vector3f minigunBarrelSpinDirection = new Vector3f(1F, 0F, 0F);
    protected float minigunBarrelSpinSpeed = 1F;

    /** These designate the locations of 3D attachment models on the gun */
    @Getter @Setter
    protected Vector3f barrelAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f scopeAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f stockAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f gripAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f gadgetAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f slideAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f pumpAttachPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f accessoryAttachPoint = new Vector3f();

    /** Muzzle flash models */
    @Getter @Setter
    protected Vector3f defaultBarrelFlashPoint = null;
    @Getter @Setter
    protected Vector3f muzzleFlashPoint = null;
    @Getter @Setter
    protected boolean hasFlash;

    /** Arms rendering */
    @Getter @Setter
    protected boolean hasArms;
    /** Changes the rotation point to be the hand for easier animation setup */
    @Getter @Setter
    protected boolean easyArms;
    @Getter @Setter
    protected Vector3f armScale = new Vector3f(0.8F,0.8F,0.8F);
    @Getter @Setter
    protected Vector3f leftArmPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmScale = new Vector3f(1,1,1);
    @Getter @Setter
    protected Vector3f rightArmPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f rightArmRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f rightArmScale = new Vector3f(1,1,1);
    @Getter @Setter
    protected Vector3f rightArmReloadPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f rightArmReloadRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmReloadPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmReloadRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f rightArmChargePos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f rightArmChargeRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmChargePos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f leftArmChargeRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f stagedrightArmReloadPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f stagedrightArmReloadRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f stagedleftArmReloadPos = new Vector3f(0,0,0);
    @Getter @Setter
    protected Vector3f stagedleftArmReloadRot = new Vector3f(0,0,0);
    @Getter @Setter
    protected boolean rightHandAmmo;
    @Getter @Setter
    protected boolean leftHandAmmo;

    /** Casing and muzzle flash parameters */
    //  Total distance to translate
    @Getter @Setter
    protected Vector3f casingAnimDistance = new Vector3f(0, 0, 16);
    //  Total range in variance for random motion
    @Getter @Setter
    protected Vector3f casingAnimSpread = new Vector3f(2, 4, 4);
    //  Number of ticks (I guess?) to complete movement
    @Getter @Setter
    protected int casingAnimTime = 20;
    //  Rotation of the casing, 180 is the total rotation. If you do not understand rotation vectors, like me, just use the standard value here.
    @Getter @Setter
    protected Vector3f casingRotateVector = new Vector3f(0.1F, 1F, 0.1F);
    @Getter @Setter
    protected Vector3f casingAttachPoint = new Vector3f(0F, 0F, 0F);
    // Time before the casing is ejected from gun
    @Getter @Setter
    protected int casingDelay;
    // Scale the bullet casing separately from gun
    @Getter @Setter
    protected float caseScale = 1F;
    @Getter @Setter
    protected float flashScale = 1F;

    /** Recoil and slide based parameters */
    @Getter @Setter
    protected float gunSlideDistance = 0.25F;
    @Getter @Setter
    protected float altgunSlideDistance = 0.25F;
    @Getter @Setter
    protected float RecoilSlideDistance = 0.125F;
    @Getter @Setter
    protected float RotateSlideDistance = -3F;
    @Getter @Setter
    protected float ShakeDistance;
    @Getter @Setter
    /** Select an amount of recoil per shot, between 0 and 1 */
    protected float recoilAmount = 0.33F;

    /** Charge handle distance/delay/time */
    @Getter @Setter
    protected float chargeHandleDistance;
    @Getter @Setter
    protected int chargeDelay;
    @Getter @Setter
    protected int chargeDelayAfterReload;
    @Getter @Setter
    protected int chargeTime = 1;

    protected EnumAnimationType animationType = EnumAnimationType.NONE;
    protected EnumMeleeAnimation meleeAnimation = EnumMeleeAnimation.DEFAULT;
    @Getter @Setter
    protected float tiltGunTime = 0.15F;
    @Getter @Setter
    protected float unloadClipTime = 0.35F;
    @Getter @Setter
    protected float loadClipTime = 0.35F;
    protected float untiltGunTime = 0.15F;
    /** If true, then the scope attachment will move with the top slide */
    @Getter @Setter
    protected boolean scopeIsOnSlide;
    /** If true, then the scope attachment will move with the break action. Can be combined with the above */
    @Getter @Setter
    protected boolean scopeIsOnBreakAction;
    /** For rifles and shotguns. Currently a generic reload animation regardless of how full the internal magazine already is */
    @Getter @Setter
    protected float numBulletsInReloadAnimation = 1F;
    /** For shotgun pump handles, rifle bolts and hammer pullbacks */
    @Getter @Setter
    protected int pumpDelay;
    @Getter @Setter
    protected int pumpDelayAfterReload;
    @Getter @Setter
    protected int pumpTime = 1;
    @Getter @Setter
    protected int hammerDelay;
    /** For shotgun pump handle */
    @Getter @Setter
    protected float pumpHandleDistance = 4F / 16F;
    /** For end loaded projectiles */
    @Getter @Setter
    protected float endLoadedAmmoDistance = 1F;
    /** For break action projectiles */
    @Getter @Setter
    protected float breakActionAmmoDistance = 1F;
    /** If true, then the grip attachment will move with the shotgun pump */
    @Getter @Setter
    protected boolean gripIsOnPump;
    /** If true, then the gadget attachment will move with the shotgun pump */
    @Getter @Setter
    protected boolean gadgetIsOnPump;
    /** The rotation point for the barrel break */
    @Getter @Setter
    protected Vector3f barrelBreakPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f altbarrelBreakPoint = new Vector3f();
    /** The amount the revolver barrel flips out by */
    @Getter @Setter
    protected float revolverFlipAngle = 15F;
    /** The amount the revolver2 barrel flips out by */
    @Getter @Setter
    protected float revolver2FlipAngle = 15F;
    /** The rotation point for the revolver flip */
    @Getter @Setter
    protected Vector3f revolverFlipPoint = new Vector3f();
    /** The rotation point for the revolver2 flip */
    @Getter @Setter
    protected Vector3f revolver2FlipPoint = new Vector3f();
    /** The angle the gun is broken by for break actions */
    @Getter @Setter
    protected float breakAngle = 45F;
    @Getter @Setter
    protected float altbreakAngle = 45F;
    /** If true, then the gun will perform a spinning reload animation */
    @Getter @Setter
    protected boolean spinningCocking;
    /** The point, in model co-ordinates, about which the gun is spun */
    @Getter @Setter
    protected Vector3f spinPoint = new Vector3f();
    /** The point where the hammer will pivot and spin from */
    @Getter @Setter
    protected Vector3f hammerSpinPoint = new Vector3f();
    @Getter @Setter
    protected Vector3f althammerSpinPoint = new Vector3f();
    @Getter @Setter
    protected float hammerAngle = 75F;
    @Getter @Setter
    protected float althammerAngle = 75F;
    /** Single action cocking check */
    @Getter @Setter
    protected boolean isSingleAction;
    /** If true, lock the slide when the last bullet is fired */
    @Getter @Setter
    protected boolean slideLockOnEmpty;
    /** If true, move the hands with the pump action */
    @Getter @Setter
    protected boolean lefthandPump;
    @Getter @Setter
    protected boolean righthandPump;
    /** If true, move the hands with the charge action */
    @Getter @Setter
    protected boolean rightHandCharge;
    @Getter @Setter
    protected boolean leftHandCharge;
    /** If true, move the hands with the bolt action */
    @Getter @Setter
    protected boolean rightHandBolt;
    @Getter @Setter
    protected boolean leftHandBolt;
    /** How far to rotate the bolt */
    protected float boltRotationAngle;
    /** How far to translate the bolt */
    protected float boltCycleDistance = 1F;
    /** Offsets the bolt rotation point to help align it properly */
    protected Vector3f boltRotationOffset = new Vector3f(0F, 0F, 0F);
    @Getter @Setter
    protected float pumpModifier = 4F;
    /** Hand offset when gun is charging */
    @Getter @Setter
    protected Vector3f chargeModifier = new Vector3f(8F, 4F, 4F);
    /**If true, gun will translate when equipped with a sight attachment */
    @Getter @Setter
    protected float gunOffset;
    @Getter @Setter
    protected float crouchZoom;
    @Getter @Setter
    protected boolean fancyStance = true;
    /** deprecated, do not use, use sprintStanceTranslate */
    @Getter @Setter
    protected Vector3f stanceTranslate = new Vector3f();
    /** deprecated, do not use, use sprintStanceRotate */
    @Getter @Setter
    protected Vector3f stanceRotate = new Vector3f();
    protected Vector3f sprintStanceTranslate = new Vector3f();
    protected Vector3f sprintStanceRotate = new Vector3f();

    /** Custom reload Parameters. If Enum.CUSTOM is set, these parameters can build an animation within the gun model classes */
    @Getter @Setter
    protected float rotateGunVertical;
    @Getter @Setter
    protected float rotateGunHorizontal;
    @Getter @Setter
    protected float tiltGun;
    @Getter @Setter
    protected Vector3f translateGun = new Vector3f(0F, 0F, 0F);
    /** Ammo Model reload parameters */
    @Getter @Setter
    protected float rotateClipVertical;
    @Getter @Setter
    protected float stagedrotateClipVertical;
    @Getter @Setter
    protected float rotateClipHorizontal;
    @Getter @Setter
    protected float stagedrotateClipHorizontal;
    @Getter @Setter
    protected float tiltClip;
    @Getter @Setter
    protected float stagedtiltClip;
    @Getter @Setter
    protected Vector3f translateClip = new Vector3f(0F, 0F, 0F);
    @Getter @Setter
    protected Vector3f stagedtranslateClip = new Vector3f(0F, 0F, 0F);
    @Getter @Setter
    protected boolean stagedReload;

    /** Disables moving gun back when ADS. */
    @Getter @Setter
    protected boolean stillRenderGunWhenScopedOverlay;
    /** Multiplier for ADS effect (moving gun to middle, e.t.c.) */
    @Getter @Setter
    protected float adsEffectMultiplier = 1;
    /** This offsets the render position for third person */
    @Getter @Setter
    protected Vector3f thirdPersonOffset = new Vector3f();
    /** This offsets the render position for item frames */
    @Getter @Setter
    protected Vector3f itemFrameOffset = new Vector3f();
    /** Allows you to move the rotation helper to determine the required offsets for moving parts */
    protected Vector3f rotationToolOffset = new Vector3f(0F, 0F, 0F);

    protected float reloadRotate;

    @Override
    public void setType(GunType type)
    {
        this.type = type;
        this.type.getAnimationConfig().writeToModel(this);
    }

    @Override
    public Class<GunType> typeClass()
    {
        return GunType.class;
    }

    /**
     * Flips the  Generally only for backwards compatibility
     */
    public void flipAll()
    {
        flip(gunModel);
        flip(backpackModel);
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

        for (ModelRendererTurbo[] mod : advBulletCounterModel)
            flip(mod);
    }

    protected void flip(ModelRendererTurbo[] model)
    {
        for (ModelRendererTurbo part : model)
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
        translate(backpackModel, x, y, z);
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

        for (ModelRendererTurbo[] modelPart : advBulletCounterModel)
            translate(modelPart, x, y, z);
    }

    protected void translate(ModelRendererTurbo[] model, float x, float y, float z)
    {
        for (ModelRendererTurbo mod : model)
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

    public void render(ModelRendererTurbo[] models, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo mod : models)
        {
            mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
        }
    }

    public void renderCustom(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, GunAnimations anims, EnumRenderPass renderPass) {}

    @Deprecated
    public void renderCustom(float scale, GunAnimations anims)
    {
        // Do not call this method since it usually contain calls to the old GlStateManager API
    }
}
