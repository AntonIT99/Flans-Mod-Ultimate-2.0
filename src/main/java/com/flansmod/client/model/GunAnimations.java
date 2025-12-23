package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import lombok.NoArgsConstructor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@NoArgsConstructor
public class GunAnimations
{
    public static GunAnimations defaults = new GunAnimations();
    public static Random random = new Random();

    //TODO: check recoil mess here
    /** (Purely aesthetic) gun animation variables */
    public boolean isGunEmpty;
    /** Recoil */
    public float gunRecoil;
    public float lastGunRecoil;
    public float recoilAmount = 0.33F;
    public float recoil;
    public float antiRecoil;
    public float recoilAngle;
    public Vector3f recoilOffset = new Vector3f();
    public Vector3f recoilVelocity = new Vector3f();
    /** Slide */
    public float gunSlide;
    public float lastGunSlide;
    /** Delayed Reload Animations */
    public int timeUntilPump;
    public int timeToPumpFor;
    /** Delayed Reload Animations : -1, 1 = At rest, 0 = Mid Animation */
    public float pumped = -1F;
    public float lastPumped = -1F;
    /** Delayed Reload Animations : Doing the delayed animation */
    public boolean pumping;

    /**
     * Charge handle variables
     */
    public int timeUntilCharge;
    public int timeToChargeFor;
    public float charged = -1F;
    public float lastCharged = -1F;
    public boolean charging;

    public boolean reloading;
    public float reloadAnimationTime;
    public float reloadAnimationProgress;
    public float lastReloadAnimationProgress;
    public int reloadAmmoCount = 1;
    public boolean singlesReload;

    public float minigunBarrelRotation;
    public float minigunBarrelRotationSpeed;

    //TODO: remove redundant variables
    public int muzzleFlash;
    public int muzzleFlashTime;
    public int flashInt;

    /** Casing mechanics */
    public int timeUntilCasing;
    public int casingStage;
    public int lastCasingStage;

    /** Hammer model mechanics. If in single action, the model will play a modified animation and delay hammer reset */
    public float hammerRotation;
    public float althammerRotation;
    public int timeUntilPullback;
    public float gunPullback = -1F;
    public float lastGunPullback = -1F;
    public boolean isFired;

    public Vector3f casingRandom = new Vector3f(0F, 0F, 0F);

    /** Melee animations */
    public int meleeAnimationProgress;
    public int meleeAnimationLength;

    /**
     * Switch animations
     */
    public float switchAnimationProgress;
    public float switchAnimationLength;

    /** Fancy stance / running animation stuff */
    public float runningStanceAnimationProgress;
    public float runningStanceAnimationLength = 4F;
    public Vector3f sprintingStance;
    public int stanceTimer;

    public LookAtState lookAt = LookAtState.NONE;
    public float lookAtTimer = 0;

    public static final int[] lookAtTimes = new int[]{1, 10, 20, 10, 20, 10};

    public enum LookAtState
    {
        NONE,
        TILT1,
        LOOK1,
        TILT2,
        LOOK2,
        UNTILT
    }

    public void updateSprintStance(String shortName)
    {
        long seed = getSeedFromString(shortName);
        sprintingStance = new Vector3f(
            randomSeededFloat(-15, -5, seed),
            randomSeededFloat(0, 45, seed),
            randomSeededFloat(-20, 10, seed)
        );
    }

    private static float randomSeededFloat(float min, float max, long seed)
    {
        return random.nextFloat() * (max - min) + min;
    }

    private static long getSeedFromString(String str)
    {
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] digest = messageDigest.digest(str.getBytes());
            long seed = 0;
            for (int i = 0; i < 8; i++)
                seed = (seed << 8) | (digest[i] & 0xff);
            return seed;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 not supported", e);
        }
    }

    public void update()
    {
        //Assign values
        lastPumped = pumped;
        lastCharged = charged;
        lastGunPullback = gunPullback;
        lastCasingStage = casingStage;

        //Time until pump-action
        if (timeUntilPump > 0)
        {
            timeUntilPump--;
            if (timeUntilPump == 0)
            {
                //Pump it!
                pumping = true;
                lastPumped = pumped = -1F;
                ModClient.setShotState(1);
            }
        }

        //Timer until pulling back the charge handle/bolt
        if (timeUntilCharge > 0)
        {
            timeUntilCharge--;
            if (timeUntilCharge == 0)
            {
                //Pump it!
                charging = true;
                lastCharged = charged = -1F;
            }
        }

        //Time until hammer pullback
        if (timeUntilPullback > 0)
        {
            timeUntilPullback--;
            if (timeUntilPullback == 0)
            {
                //Reset the hammer
                isFired = true;
                lastGunPullback = gunPullback = -1F;
            }
        }
        else
        {
            //Automatically reset hammer
            hammerRotation *= 0.6F;
            althammerRotation *= 0.6F;
        }

        //Time until bullet casing ejection
        if (timeUntilCasing > 0)
        {
            timeUntilCasing--;
            if (timeUntilCasing == 0)
                casingStage++;
        }
        else
        {
            casingStage++;
        }

        if (muzzleFlashTime > 0)
            muzzleFlashTime--;
        if (muzzleFlash > 0)
            muzzleFlash--;

        if(pumping)
        {
            pumped += 2F / timeToPumpFor;
            if (pumped >= 0.999F)
                pumping = false;
        }
        if (charging)
        {
            charged += 2F / timeToChargeFor;
            if (charged >= 0.999F)
                charging = false;
        }

        if (isFired)
        {
            gunPullback += 2F / 4;
            if (gunPullback >= 0.999F)
                isFired = false;
        }

        //Recoil model
        lastGunRecoil = gunRecoil;
        if (gunRecoil > 0)
            gunRecoil *= 0.7F;

        float scale = 0.5f;
        float offsetScale = 0.005f;

        if (recoil > 0)
            recoil *= 0.5F;

        recoilVelocity.x += (float) ((random.nextGaussian() - 0.5f) * recoil * offsetScale);
        recoilVelocity.y += (float) ((random.nextGaussian() - 0.5f) * recoil * offsetScale);
        recoilVelocity.z += (float) ((random.nextGaussian() - 0.25f) * recoil * offsetScale);
        recoilVelocity.scale(0.5f);

        Vector3f.add(recoilOffset, recoilVelocity, recoilOffset);

        recoilOffset.scale(0.9f);

        recoilAngle -= recoil * scale;
        antiRecoil += recoil;

        recoilAngle += antiRecoil * 0.2f * scale;
        antiRecoil *= 0.8F;

        //Slide model
        lastGunSlide = gunSlide;
        if (isGunEmpty)
            lastGunSlide = gunSlide = 0.5F;
        if (!isGunEmpty && gunSlide > 0.9)
            gunSlide -= 0.1F;
        else if (gunSlide > 0 && !isGunEmpty)
            gunSlide *= 0.5F;

        //Reload
        lastReloadAnimationProgress = reloadAnimationProgress;
        if(reloading)
            reloadAnimationProgress += 1F / reloadAnimationTime;
        if (reloading && reloadAnimationProgress >= 0.9F)
            isGunEmpty = false;
        if (reloading && reloadAnimationProgress >= 1F)
            reloading = false;

        minigunBarrelRotation += minigunBarrelRotationSpeed;
        minigunBarrelRotationSpeed *= 0.9F;

        if(meleeAnimationLength > 0)
        {
            meleeAnimationProgress++;
            //If we are done, reset
            if (meleeAnimationProgress > meleeAnimationLength)
                meleeAnimationProgress = meleeAnimationLength = 0;
        }

        if (switchAnimationProgress > 0)
        {
            switchAnimationProgress++;
            //If we are done, reset
            if (switchAnimationProgress == switchAnimationLength)
                switchAnimationLength = 0;
        }

        if (runningStanceAnimationProgress > 0 && runningStanceAnimationProgress < runningStanceAnimationLength)
            runningStanceAnimationProgress++;

        if (stanceTimer > 0)
            stanceTimer--;

        switch (lookAt)
        {
            case NONE:
                lookAtTimer = 0;
                break;
            case TILT1, LOOK1, TILT2, LOOK2, UNTILT:
            {
                lookAtTimer++;
                if (lookAtTimer >= lookAtTimes[lookAt.ordinal()])
                {
                    lookAt = LookAtState.values()[(lookAt.ordinal() + 1) % LookAtState.values().length];
                    lookAtTimer = 0;
                }
                break;
            }
            default:
                break;
        }
    }

    public void onGunEmpty(boolean atLastBullet)
    {
        isGunEmpty = atLastBullet;
    }

    public void addMinigunBarrelRotationSpeed(Float speed)
    {
        minigunBarrelRotationSpeed += speed;
    }

    public void doShoot(int pumpDelay, int pumpTime, int hammerDelay, float hammerAngle, float althammerAngle, int casingDelay)
    {
        //Accumulative recoil function
        lastGunRecoil = gunRecoil += recoilAmount;

        minigunBarrelRotationSpeed += 2F;
        lastGunSlide = gunSlide = 1F;
        timeUntilPump = pumpDelay;
        timeToPumpFor = pumpTime;
        timeUntilPullback = hammerDelay;
        timeUntilCasing = casingDelay;
        hammerRotation = hammerAngle;
        althammerRotation = althammerAngle;
        muzzleFlash = 2;
        muzzleFlashTime = 2;
        stanceTimer = 20;

        // 0: 50%, 1: 25%, 2: 25%
        flashInt = Math.max(0, random.nextInt(4) - 1);

        casingRandom.x = ((random.nextFloat() * 2) - 1);
        casingRandom.y = ((random.nextFloat() * 2) - 1);
        casingRandom.z = ((random.nextFloat() * 2) - 1);
        casingStage = 0;

        if (pumpDelay == 0)
            ModClient.setShotState(1);
    }

    public void doReload(float reloadTime, int pumpDelay, int pumpTime, int chargeDelay, int chargeTime, int ammoCount, boolean hasMultipleAmmo)
    {
        reloading = true;
        lastReloadAnimationProgress = reloadAnimationProgress = 0F;
        reloadAnimationTime = reloadTime;
        timeUntilPump = pumpDelay;
        timeToPumpFor = pumpTime;
        timeUntilCharge = chargeDelay;
        timeToChargeFor = chargeTime;
        reloadAmmoCount = ammoCount;
        singlesReload = hasMultipleAmmo;
        ModClient.setLastBulletReload(ammoCount - 1);
    }

    public void cancelReload()
    {
        reloading = false;
        lastReloadAnimationProgress = reloadAnimationProgress = 0F;
        reloadAnimationTime = 0;
    }

    public void doMelee(int meleeTime)
    {
        meleeAnimationLength = meleeTime;
    }
}
