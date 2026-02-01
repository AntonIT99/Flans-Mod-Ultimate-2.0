package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class GunAnimationConfig
{
    private Vector3f minigunBarrelOrigin = null;
    private Vector3f barrelAttachPoint = null;
    private Vector3f scopeAttachPoint = null;
    private Vector3f stockAttachPoint = null;
    private Vector3f gripAttachPoint = null;
    private Vector3f gadgetAttachPoint = null;
    private Vector3f slideAttachPoint = null;
    private Vector3f pumpAttachPoint = null;
    private Vector3f accessoryAttachPoint = null;

    private Vector3f defaultBarrelFlashPoint = null;
    private Vector3f muzzleFlashPoint = null;

    private Boolean hasFlash = null;
    private Boolean hasArms = null;
    private Boolean easyArms = null;
    private Vector3f armScale = null;

    private Vector3f leftArmPos = null;
    private Vector3f leftArmRot = null;
    private Vector3f leftArmScale = null;
    private Vector3f rightArmPos = null;
    private Vector3f rightArmRot = null;
    private Vector3f rightArmScale = null;
    private Vector3f rightArmReloadPos = null;
    private Vector3f rightArmReloadRot = null;
    private Vector3f leftArmReloadPos = null;
    private Vector3f leftArmReloadRot = null;
    private Vector3f rightArmChargePos = null;
    private Vector3f rightArmChargeRot = null;
    private Vector3f leftArmChargePos = null;
    private Vector3f leftArmChargeRot = null;

    private Vector3f stagedrightArmReloadPos = null;
    private Vector3f stagedrightArmReloadRot = null;
    private Vector3f stagedleftArmReloadPos = null;
    private Vector3f stagedleftArmReloadRot = null;

    private Boolean rightHandAmmo = null;
    private Boolean leftHandAmmo = null;

    private Float gunSlideDistance = null;
    private Float altgunSlideDistance = null;
    private Float recoilSlideDistance = null;
    private Float rotateSlideDistance = null;
    private Float shakeDistance = null;
    private Float recoilAmount = null;

    private Vector3f casingAnimDistance = null;
    private Vector3f casingAnimSpread = null;
    private Integer casingAnimTime = null;
    private Vector3f casingRotateVector = null;
    private Vector3f casingAttachPoint = null;
    private Integer casingDelay = null;
    private Float caseScale = null;
    private Float flashScale = null;

    private Float chargeHandleDistance = null;
    private Integer chargeDelay = null;
    private Integer chargeDelayAfterReload = null;
    private Integer chargeTime = null;
    private Boolean countOnRightHandSide = null;
    private Boolean isBulletCounterActive;
    private Boolean isAdvBulletCounterActive = null;

    private Float tiltGunTime = null;
    private Float unloadClipTime = null;
    private Float loadClipTime = null;

    private Boolean scopeIsOnSlide = null;
    private Boolean scopeIsOnBreakAction = null;

    private Float numBulletsInReloadAnimation = null;
    private Integer pumpDelay = null;
    private Integer pumpDelayAfterReload = null;
    private Integer pumpTime = null;
    private Integer hammerDelay = null;

    private Float pumpHandleDistance = null;
    private Float endLoadedAmmoDistance = null;
    private Float breakActionAmmoDistance = null;

    private Boolean gripIsOnPump = null;
    private Boolean gadgetIsOnPump = null;

    private Vector3f barrelBreakPoint = null;
    private Vector3f altbarrelBreakPoint = null;

    private Float revolverFlipAngle = null;
    private Float revolver2FlipAngle = null;

    private Vector3f revolverFlipPoint = null;
    private Vector3f revolver2FlipPoint = null;

    private Float breakAngle = null;
    private Float altbreakAngle = null;

    private Boolean spinningCocking = null;

    private Vector3f spinPoint = null;
    private Vector3f hammerSpinPoint = null;
    private Vector3f althammerSpinPoint = null;
    private Float hammerAngle = null;
    private Float althammerAngle = null;

    private Boolean isSingleAction = null;
    private Boolean slideLockOnEmpty = null;
    private Boolean lefthandPump = null;
    private Boolean righthandPump = null;
    private Boolean rightHandCharge = null;
    private Boolean leftHandCharge = null;
    private Boolean rightHandBolt = null;
    private Boolean leftHandBolt = null;

    private Float pumpModifier = null;
    private Vector3f chargeModifier = null;
    private Float gunOffset = null;
    private Float crouchZoom = null;
    private Boolean fancyStance = null;
    private Vector3f stanceTranslate = null;
    private Vector3f stanceRotate = null;

    private Float rotateGunVertical = null;
    private Float rotateGunHorizontal = null;
    private Float tiltGun = null;
    private Vector3f translateGun = null;
    private Float rotateClipVertical = null;
    private Float stagedrotateClipVertical = null;
    private Float tiltClip = null;
    private Float stagedtiltClip = null;
    private Vector3f translateClip = null;
    private Vector3f stagedtranslateClip = null;
    private Boolean stagedReload = null;

    private Vector3f thirdPersonOffset = null;
    private Vector3f itemFrameOffset = null;
    private Boolean stillRenderGunWhenScopedOverlay = null;
    private Float adsEffectMultiplier = null;

    public void read(TypeFile file)
    {
        minigunBarrelOrigin = readVector("animMinigunBarrelOrigin", file);
        barrelAttachPoint = readVector("animBarrelAttachPoint", file);
        scopeAttachPoint = readVector("animScopeAttachPoint", file);
        stockAttachPoint = readVector("animStockAttachPoint", file);
        gripAttachPoint = readVector("animGripAttachPoint", file);
        gadgetAttachPoint = readVector("animGadgetAttachPoint", file);
        slideAttachPoint = readVector("animSlideAttachPoint", file);
        pumpAttachPoint = readVector("animPumpAttachPoint", file);
        accessoryAttachPoint = readVector("animAccessoryAttachPoint", file);

        defaultBarrelFlashPoint = readVector("animDefaultBarrelFlashPoint", file);
        muzzleFlashPoint = readVector("animMuzzleFlashPoint", file);

        hasFlash = readBoolean("animHasFlash", file);
        hasArms = readBoolean("animHasArms", file);
        easyArms = readBoolean("easyArms", file);
        armScale = readVector("armScale", file);

        leftArmPos = readVector("animLeftArmPos", file);
        leftArmRot = readVector("animLeftArmRot", file);
        leftArmScale = readVector("animLeftArmScale", file);
        rightArmPos = readVector("animRightArmPos", file);
        rightArmRot = readVector("animRightArmRot", file);
        rightArmScale = readVector("animRightArmScale", file);
        rightArmReloadPos = readVector("animRightArmReloadPos", file);
        rightArmReloadRot = readVector("animRightArmReloadRot", file);
        leftArmReloadPos = readVector("animLeftArmReloadPos", file);
        leftArmReloadRot = readVector("animLeftArmReloadRot", file);
        rightArmChargePos = readVector("animRightArmChargePos", file);
        rightArmChargeRot = readVector("animRightArmChargeRot", file);
        leftArmChargePos = readVector("animLeftArmChargePos", file);
        leftArmChargeRot = readVector("animLeftArmChargeRot", file);

        stagedrightArmReloadPos = readVector("animStagedRightArmReloadPos", file);
        stagedrightArmReloadRot = readVector("animStagedRightArmReloadRot", file);
        stagedleftArmReloadPos = readVector("animStagedLeftArmReloadPos", file);
        stagedleftArmReloadRot = readVector("animStagedLeftArmReloadRot", file);

        rightHandAmmo = readBoolean("animRightHandAmmo", file);
        leftHandAmmo = readBoolean("animLeftHandAmmo", file);

        gunSlideDistance = readFloat("animGunSlideDistance", file);
        altgunSlideDistance = readFloat("animAltGunSlideDistance", file);
        recoilSlideDistance = readFloat("animRecoilSlideDistance", file);
        rotateSlideDistance = readFloat("animRotatedSlideDistance", file);
        shakeDistance = readFloat("animShakeDistance", file);
        recoilAmount = readFloat("animRecoilAmount", file);

        casingAnimDistance = readVector("animCasingAnimDistance", file);
        casingAnimSpread = readVector("animCasingAnimSpread", file);
        casingAnimTime = readInteger("animCasingAnimTime", file);
        casingRotateVector = readVector("animCasingRotateVector", file);
        casingAttachPoint = readVector("animCasingAttachPoint", file);
        casingDelay = readInteger("animCasingDelay", file);
        caseScale = readFloat("animCasingScale", file);
        flashScale = readFloat("animFlashScale", file);

        chargeHandleDistance = readFloat("animChargeHandleDistance", file);
        chargeDelay = readInteger("animChargeDelay", file);
        chargeDelayAfterReload = readInteger("animChargeDelayAfterReload", file);
        chargeTime = readInteger("animChargeTime", file);
        countOnRightHandSide = readBoolean("animCountOnRightHandSide", file);
        isBulletCounterActive = readBoolean("animIsBulletCounterActive", file);
        isAdvBulletCounterActive = readBoolean("animIsAdvBulletCounterActive", file);
        
        tiltGunTime = readFloat("animTiltGunTime", file);
        unloadClipTime = readFloat("animUnloadClipTime", file);
        loadClipTime = readFloat("animLoadClipTime", file);

        scopeIsOnSlide = readBoolean("animScopeIsOnSlide", file);
        scopeIsOnBreakAction = readBoolean("animScopeIsOnBreakAction", file);
        
        numBulletsInReloadAnimation = readFloat("animNumBulletsInReloadAnimation", file);
        pumpDelay = readInteger("animPumpDelay", file);
        pumpDelayAfterReload = readInteger("animPumpDelayAfterReload", file);
        pumpTime = readInteger("animPumpTime", file);
        hammerDelay = readInteger("animHammerDelay", file);

        pumpHandleDistance = readFloat("animPumpHandleDistance", file);
        endLoadedAmmoDistance = readFloat("animEndLoadedAmmoDistance", file);
        breakActionAmmoDistance = readFloat("animBreakActionAmmoDistance", file);

        gripIsOnPump = readBoolean("animGripIsOnPump", file);
        gadgetIsOnPump = readBoolean("animGadgetsOnPump", file);

        barrelBreakPoint = readVector("animBarrelBreakPoint", file);
        altbarrelBreakPoint = readVector("animAltBarrelBreakPoint", file);

        revolverFlipAngle = readFloat("animRevolverFlipAngle", file);
        revolver2FlipAngle = readFloat("animRevolver2FlipAngle", file);

        revolverFlipPoint = readVector("animRevolverFlipPoint", file);
        revolver2FlipPoint = readVector("animRevolver2FlipPoint", file);

        breakAngle = readFloat("animBreakAngle", file);
        altbreakAngle = readFloat("animAltBreakAngle", file);

        spinningCocking = readBoolean("animSpinningCocking", file);

        spinPoint = readVector("animSpinPoint", file);
        hammerSpinPoint = readVector("animHammerSpinPoint", file);
        althammerSpinPoint = readVector("animAltHammerSpinPoint", file);
        hammerAngle = readFloat("animHammerAngle", file);
        althammerAngle = readFloat("animAltHammerAngle", file);

        isSingleAction = readBoolean("animIsSingleAction", file);
        slideLockOnEmpty = readBoolean("animSlideLockOnEmpty", file);
        lefthandPump = readBoolean("animLeftHandPump", file);
        righthandPump = readBoolean("animRightHandPump", file);
        leftHandCharge = readBoolean("animLeftHandCharge", file);
        rightHandCharge = readBoolean("animRightHandCharge", file);
        leftHandBolt = readBoolean("animLeftHandBolt", file);
        rightHandBolt = readBoolean("animRightHandBolt", file);

        pumpModifier = readFloat("animPumpModifier", file);
        chargeModifier = readVector("animChargeModifier", file);
        gunOffset = readFloat("animGunOffset", file);
        crouchZoom = readFloat("animCrouchZoom", file);
        fancyStance = readBoolean("animFancyStance", file);
        stanceTranslate = readVector("animTranslateClip", file);
        stanceRotate = readVector("animStanceRotate", file);

        rotateGunVertical = readFloat("animRotateGunVertical", file);
        rotateGunHorizontal = readFloat("animRotateGunHorizontal", file);
        tiltGun = readFloat("animTiltGun", file);
        translateGun = readVector("animTranslateGun", file);
        rotateClipVertical = readFloat("animRotateClipVertical", file);
        stagedrotateClipVertical = readFloat("animStagedRotateClipVertical", file);
        rotateClipVertical = readFloat("animRotateClipHorizontal", file);
        stagedrotateClipVertical = readFloat("animStagedRotateClipHorizontal", file);
        tiltClip = readFloat("animTiltClip", file);
        stagedtiltClip = readFloat("animStagedTiltClip", file);
        translateClip = readVector("animTranslateClip", file);
        stagedtranslateClip = readVector("animStagedTranslateClip", file);
        stagedReload = readBoolean("animStagedReload", file);

        thirdPersonOffset = readVector("animThirdPersonOffset", file);
        itemFrameOffset = readVector("animItemFrameOffset", file);
        stillRenderGunWhenScopedOverlay = readBoolean("animStillRenderGunWhenScopedOverlay", file);
        adsEffectMultiplier = readFloat("animAdsEffectMultiplier", file);
    }
}
