package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

import static com.flansmodultimate.util.TypeReaderUtils.*;

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

    @OnlyIn(Dist.CLIENT)
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

    @OnlyIn(Dist.CLIENT)
    public void writeToModel(ModelGun modelGun)
    {
        modelGun.setMinigunBarrelOrigin(Optional.ofNullable(minigunBarrelOrigin).orElse(modelGun.getMinigunBarrelOrigin()));
        modelGun.setBarrelAttachPoint(Optional.ofNullable(barrelAttachPoint).orElse(modelGun.getBarrelAttachPoint()));
        modelGun.setScopeAttachPoint(Optional.ofNullable(scopeAttachPoint).orElse(modelGun.getScopeAttachPoint()));
        modelGun.setStockAttachPoint(Optional.ofNullable(stockAttachPoint).orElse(modelGun.getStockAttachPoint()));
        modelGun.setGripAttachPoint(Optional.ofNullable(gripAttachPoint).orElse(modelGun.getGripAttachPoint()));
        modelGun.setGadgetAttachPoint(Optional.ofNullable(gadgetAttachPoint).orElse(modelGun.getGadgetAttachPoint()));
        modelGun.setSlideAttachPoint(Optional.ofNullable(slideAttachPoint).orElse(modelGun.getSlideAttachPoint()));
        modelGun.setPumpAttachPoint(Optional.ofNullable(pumpAttachPoint).orElse(modelGun.getPumpAttachPoint()));
        modelGun.setAccessoryAttachPoint(Optional.ofNullable(accessoryAttachPoint).orElse(modelGun.getAccessoryAttachPoint()));

        modelGun.setDefaultBarrelFlashPoint(Optional.ofNullable(defaultBarrelFlashPoint).orElse(modelGun.getDefaultBarrelFlashPoint()));
        modelGun.setMuzzleFlashPoint(Optional.ofNullable(muzzleFlashPoint).orElse(modelGun.getMuzzleFlashPoint()));

        modelGun.setHasFlash(Optional.ofNullable(hasFlash).orElse(modelGun.isHasFlash()));
        modelGun.setHasArms(Optional.ofNullable(hasArms).orElse(modelGun.isHasArms()));
        modelGun.setEasyArms(Optional.ofNullable(easyArms).orElse(modelGun.isEasyArms()));
        modelGun.setArmScale(Optional.ofNullable(armScale).orElse(modelGun.getArmScale()));

        modelGun.setLeftArmPos(Optional.ofNullable(leftArmPos).orElse(modelGun.getLeftArmPos()));
        modelGun.setLeftArmRot(Optional.ofNullable(leftArmRot).orElse(modelGun.getLeftArmRot()));
        modelGun.setLeftArmScale(Optional.ofNullable(leftArmScale).orElse(modelGun.getLeftArmScale()));
        modelGun.setRightArmPos(Optional.ofNullable(rightArmPos).orElse(modelGun.getRightArmPos()));
        modelGun.setRightArmRot(Optional.ofNullable(rightArmRot).orElse(modelGun.getRightArmRot()));
        modelGun.setRightArmScale(Optional.ofNullable(rightArmScale).orElse(modelGun.getRightArmScale()));
        modelGun.setRightArmReloadPos(Optional.ofNullable(rightArmReloadPos).orElse(modelGun.getRightArmReloadPos()));
        modelGun.setRightArmReloadRot(Optional.ofNullable(rightArmReloadRot).orElse(modelGun.getRightArmReloadRot()));
        modelGun.setLeftArmReloadPos(Optional.ofNullable(leftArmReloadPos).orElse(modelGun.getLeftArmReloadPos()));
        modelGun.setLeftArmReloadRot(Optional.ofNullable(leftArmReloadRot).orElse(modelGun.getLeftArmReloadRot()));
        modelGun.setRightArmChargePos(Optional.ofNullable(rightArmChargePos).orElse(modelGun.getRightArmChargePos()));
        modelGun.setRightArmChargeRot(Optional.ofNullable(rightArmChargeRot).orElse(modelGun.getRightArmChargeRot()));
        modelGun.setLeftArmChargePos(Optional.ofNullable(leftArmChargePos).orElse(modelGun.getLeftArmChargePos()));
        modelGun.setLeftArmChargeRot(Optional.ofNullable(leftArmChargeRot).orElse(modelGun.getLeftArmChargeRot()));

        modelGun.setStagedrightArmReloadPos(Optional.ofNullable(stagedrightArmReloadPos).orElse(modelGun.getStagedrightArmReloadPos()));
        modelGun.setStagedrightArmReloadRot(Optional.ofNullable(stagedrightArmReloadRot).orElse(modelGun.getStagedrightArmReloadRot()));
        modelGun.setStagedleftArmReloadPos(Optional.ofNullable(stagedleftArmReloadPos).orElse(modelGun.getStagedleftArmReloadPos()));
        modelGun.setStagedleftArmReloadRot(Optional.ofNullable(stagedleftArmReloadRot).orElse(modelGun.getStagedleftArmReloadRot()));

        modelGun.setRightHandAmmo(Optional.ofNullable(rightHandAmmo).orElse(modelGun.isRightHandAmmo()));
        modelGun.setLeftHandAmmo(Optional.ofNullable(leftHandAmmo).orElse(modelGun.isLeftHandAmmo()));

        modelGun.setGunSlideDistance(Optional.ofNullable(gunSlideDistance).orElse(modelGun.getGunSlideDistance()));
        modelGun.setAltgunSlideDistance(Optional.ofNullable(altgunSlideDistance).orElse(modelGun.getAltgunSlideDistance()));
        modelGun.setRecoilSlideDistance(Optional.ofNullable(recoilSlideDistance).orElse(modelGun.getRecoilSlideDistance()));
        modelGun.setRotateSlideDistance(Optional.ofNullable(rotateSlideDistance).orElse(modelGun.getRotateSlideDistance()));
        modelGun.setShakeDistance(Optional.ofNullable(shakeDistance).orElse(modelGun.getShakeDistance()));
        modelGun.setRecoilAmount(Optional.ofNullable(recoilAmount).orElse(modelGun.getRecoilAmount()));

        modelGun.setCasingAnimDistance(Optional.ofNullable(casingAnimDistance).orElse(modelGun.getCasingAnimDistance()));
        modelGun.setCasingAnimSpread(Optional.ofNullable(casingAnimSpread).orElse(modelGun.getCasingAnimSpread()));
        modelGun.setCasingAnimTime(Optional.ofNullable(casingAnimTime).orElse(modelGun.getCasingAnimTime()));
        modelGun.setCasingRotateVector(Optional.ofNullable(casingRotateVector).orElse(modelGun.getCasingRotateVector()));
        modelGun.setCasingAttachPoint(Optional.ofNullable(casingAttachPoint).orElse(modelGun.getCasingAttachPoint()));
        modelGun.setCasingDelay(Optional.ofNullable(casingDelay).orElse(modelGun.getCasingDelay()));
        modelGun.setCaseScale(Optional.ofNullable(caseScale).orElse(modelGun.getCaseScale()));
        modelGun.setFlashScale(Optional.ofNullable(flashScale).orElse(modelGun.getFlashScale()));

        modelGun.setChargeHandleDistance(Optional.ofNullable(chargeHandleDistance).orElse(modelGun.getChargeHandleDistance()));
        modelGun.setChargeDelay(Optional.ofNullable(chargeDelay).orElse(modelGun.getChargeDelay()));
        modelGun.setChargeDelayAfterReload(Optional.ofNullable(chargeDelayAfterReload).orElse(modelGun.getChargeDelayAfterReload()));
        modelGun.setChargeTime(Optional.ofNullable(chargeTime).orElse(modelGun.getChargeTime()));
        modelGun.setCountOnRightHandSide(Optional.ofNullable(countOnRightHandSide).orElse(modelGun.isCountOnRightHandSide()));
        modelGun.setBulletCounterActive(Optional.ofNullable(isBulletCounterActive).orElse(modelGun.isBulletCounterActive()));
        modelGun.setAdvBulletCounterActive(Optional.ofNullable(isAdvBulletCounterActive).orElse(modelGun.isAdvBulletCounterActive()));

        modelGun.setTiltGunTime(Optional.ofNullable(tiltGunTime).orElse(modelGun.getTiltGunTime()));
        modelGun.setUnloadClipTime(Optional.ofNullable(unloadClipTime).orElse(modelGun.getUnloadClipTime()));
        modelGun.setLoadClipTime(Optional.ofNullable(loadClipTime).orElse(modelGun.getLoadClipTime()));

        modelGun.setScopeIsOnSlide(Optional.ofNullable(scopeIsOnSlide).orElse(modelGun.isScopeIsOnSlide()));
        modelGun.setScopeIsOnBreakAction(Optional.ofNullable(scopeIsOnBreakAction).orElse(modelGun.isScopeIsOnBreakAction()));

        modelGun.setNumBulletsInReloadAnimation(Optional.ofNullable(numBulletsInReloadAnimation).orElse(modelGun.getNumBulletsInReloadAnimation()));
        modelGun.setPumpDelay(Optional.ofNullable(pumpDelay).orElse(modelGun.getPumpDelay()));
        modelGun.setPumpDelayAfterReload(Optional.ofNullable(pumpDelayAfterReload).orElse(modelGun.getPumpDelayAfterReload()));
        modelGun.setPumpTime(Optional.ofNullable(pumpTime).orElse(modelGun.getPumpTime()));
        modelGun.setHammerDelay(Optional.ofNullable(hammerDelay).orElse(modelGun.getHammerDelay()));

        modelGun.setPumpHandleDistance(Optional.ofNullable(pumpHandleDistance).orElse(modelGun.getPumpHandleDistance()));
        modelGun.setEndLoadedAmmoDistance(Optional.ofNullable(endLoadedAmmoDistance).orElse(modelGun.getEndLoadedAmmoDistance()));
        modelGun.setBreakActionAmmoDistance(Optional.ofNullable(breakActionAmmoDistance).orElse(modelGun.getBreakActionAmmoDistance()));

        modelGun.setGripIsOnPump(Optional.ofNullable(gripIsOnPump).orElse(modelGun.isGripIsOnPump()));
        modelGun.setGadgetIsOnPump(Optional.ofNullable(gadgetIsOnPump).orElse(modelGun.isGadgetIsOnPump()));

        modelGun.setBarrelBreakPoint(Optional.ofNullable(barrelBreakPoint).orElse(modelGun.getBarrelBreakPoint()));
        modelGun.setAltbarrelBreakPoint(Optional.ofNullable(altbarrelBreakPoint).orElse(modelGun.getAltbarrelBreakPoint()));

        modelGun.setRevolverFlipAngle(Optional.ofNullable(revolverFlipAngle).orElse(modelGun.getRevolverFlipAngle()));
        modelGun.setRevolver2FlipAngle(Optional.ofNullable(revolver2FlipAngle).orElse(modelGun.getRevolver2FlipAngle()));

        modelGun.setRevolverFlipPoint(Optional.ofNullable(revolverFlipPoint).orElse(modelGun.getRevolverFlipPoint()));
        modelGun.setRevolver2FlipPoint(Optional.ofNullable(revolver2FlipPoint).orElse(modelGun.getRevolver2FlipPoint()));

        modelGun.setBreakAngle(Optional.ofNullable(breakAngle).orElse(modelGun.getBreakAngle()));
        modelGun.setAltbreakAngle(Optional.ofNullable(altbreakAngle).orElse(modelGun.getAltbreakAngle()));

        modelGun.setSpinningCocking(Optional.ofNullable(spinningCocking).orElse(modelGun.isSpinningCocking()));

        modelGun.setSpinPoint(Optional.ofNullable(spinPoint).orElse(modelGun.getSpinPoint()));
        modelGun.setHammerSpinPoint(Optional.ofNullable(hammerSpinPoint).orElse(modelGun.getHammerSpinPoint()));
        modelGun.setAlthammerSpinPoint(Optional.ofNullable(althammerSpinPoint).orElse(modelGun.getAlthammerSpinPoint()));
        modelGun.setHammerAngle(Optional.ofNullable(hammerAngle).orElse(modelGun.getHammerAngle()));
        modelGun.setAlthammerAngle(Optional.ofNullable(althammerAngle).orElse(modelGun.getAlthammerAngle()));

        modelGun.setSingleAction(Optional.ofNullable(isSingleAction).orElse(modelGun.isSingleAction()));
        modelGun.setSlideLockOnEmpty(Optional.ofNullable(slideLockOnEmpty).orElse(modelGun.isSlideLockOnEmpty()));
        modelGun.setLefthandPump(Optional.ofNullable(lefthandPump).orElse(modelGun.isLefthandPump()));
        modelGun.setRighthandPump(Optional.ofNullable(righthandPump).orElse(modelGun.isRighthandPump()));
        modelGun.setLeftHandCharge(Optional.ofNullable(leftHandCharge).orElse(modelGun.isLeftHandCharge()));
        modelGun.setRightHandCharge(Optional.ofNullable(rightHandCharge).orElse(modelGun.isRightHandCharge()));
        modelGun.setLeftHandBolt(Optional.ofNullable(leftHandBolt).orElse(modelGun.isLeftHandBolt()));
        modelGun.setRightHandBolt(Optional.ofNullable(rightHandBolt).orElse(modelGun.isRightHandBolt()));

        modelGun.setPumpModifier(Optional.ofNullable(pumpModifier).orElse(modelGun.getPumpModifier()));
        modelGun.setChargeModifier(Optional.ofNullable(chargeModifier).orElse(modelGun.getChargeModifier()));
        modelGun.setGunOffset(Optional.ofNullable(gunOffset).orElse(modelGun.getGunOffset()));
        modelGun.setCrouchZoom(Optional.ofNullable(crouchZoom).orElse(modelGun.getCrouchZoom()));
        modelGun.setFancyStance(Optional.ofNullable(fancyStance).orElse(modelGun.isFancyStance()));
        modelGun.setStanceTranslate(Optional.ofNullable(stanceTranslate).orElse(modelGun.getStanceTranslate()));
        modelGun.setStanceRotate(Optional.ofNullable(stanceRotate).orElse(modelGun.getStanceRotate()));

        modelGun.setRotateGunVertical(Optional.ofNullable(rotateGunVertical).orElse(modelGun.getRotateGunVertical()));
        modelGun.setRotateGunHorizontal(Optional.ofNullable(rotateGunHorizontal).orElse(modelGun.getRotateGunHorizontal()));
        modelGun.setTiltGun(Optional.ofNullable(tiltGun).orElse(modelGun.getTiltGun()));
        modelGun.setTranslateGun(Optional.ofNullable(translateGun).orElse(modelGun.getTranslateGun()));
        modelGun.setRotateClipVertical(Optional.ofNullable(rotateClipVertical).orElse(modelGun.getRotateClipVertical()));
        modelGun.setStagedrotateClipVertical(Optional.ofNullable(stagedrotateClipVertical).orElse(modelGun.getStagedrotateClipVertical()));
        modelGun.setRotateClipVertical(Optional.ofNullable(rotateClipVertical).orElse(modelGun.getRotateClipVertical()));
        modelGun.setStagedrotateClipVertical(Optional.ofNullable(stagedrotateClipVertical).orElse(modelGun.getStagedrotateClipVertical()));
        modelGun.setTiltClip(Optional.ofNullable(tiltClip).orElse(modelGun.getTiltClip()));
        modelGun.setStagedtiltClip(Optional.ofNullable(stagedtiltClip).orElse(modelGun.getStagedtiltClip()));
        modelGun.setTranslateClip(Optional.ofNullable(translateClip).orElse(modelGun.getTranslateClip()));
        modelGun.setStagedtranslateClip(Optional.ofNullable(stagedtranslateClip).orElse(modelGun.getStagedtranslateClip()));
        modelGun.setStagedReload(Optional.ofNullable(stagedReload).orElse(modelGun.isStagedReload()));

        modelGun.setThirdPersonOffset(Optional.ofNullable(thirdPersonOffset).orElse(modelGun.getThirdPersonOffset()));
        modelGun.setItemFrameOffset(Optional.ofNullable(itemFrameOffset).orElse(modelGun.getItemFrameOffset()));
        modelGun.setStillRenderGunWhenScopedOverlay(Optional.ofNullable(stillRenderGunWhenScopedOverlay).orElse(modelGun.isStillRenderGunWhenScopedOverlay()));
        modelGun.setAdsEffectMultiplier(Optional.ofNullable(adsEffectMultiplier).orElse(modelGun.getAdsEffectMultiplier()));
    }
}
