package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.minecraft.util.RandomSource;

import java.util.Random;

@Getter
@NoArgsConstructor
public class GunAnimations
{
    public static final Random random = new Random();

    /** (Purely aesthetic) gun animation variables */
    protected boolean isGunEmpty;
    /** Recoil */
    protected float gunRecoil;
    protected float lastGunRecoil;
    protected float recoilAmount = 0.33F;
    /** Slide */
    protected float gunSlide;
    protected float lastGunSlide;
    /** Delayed Reload Animations */
    protected int timeUntilPump;
    protected int timeToPumpFor;
    /** Delayed Reload Animations : -1, 1 = At rest, 0 = Mid Animation */
    protected float pumped = -1F;
    protected float lastPumped = -1F;
    /** Delayed Reload Animations : Doing the delayed animation */
    protected boolean pumping;

    /** Charge handle variables */
    protected int timeUntilCharge;
    protected int timeToChargeFor;
    protected float charged = -1F;
    protected float lastCharged = -1F;
    protected boolean charging;

    protected boolean reloading;
    protected float reloadAnimationTime;
    protected float reloadAnimationProgress;
    protected float lastReloadAnimationProgress;
    protected int reloadAmmoCount = 1;
    protected boolean singlesReload;

    protected float minigunBarrelRotation;
    protected float minigunBarrelRotationSpeed;

    protected int muzzleFlashTime;
    protected int flashInt;

    /** Casing mechanics */
    protected int timeUntilCasing;
    protected int casingStage;
    protected int lastCasingStage;

    /** Hammer model mechanics. If in single action, the model will play a modified animation and delay hammer reset */
    protected float hammerRotation;
    protected float althammerRotation;
    protected int timeUntilPullback;
    protected float gunPullback = -1F;
    protected float lastGunPullback = -1F;
    protected boolean isFired;

    protected Vector3f casingRandom = new Vector3f(0F, 0F, 0F);

    /** Melee animations */
    protected int meleeAnimationProgress;
    protected int meleeAnimationLength;

    /** Switch animations */
    @Setter
    protected float switchAnimationProgress;
    @Setter
    protected float switchAnimationLength;

    /** Fancy stance / running animation stuff */
    protected float runningStanceAnimationProgress;
    protected float runningStanceAnimationLength = 4F;
    protected Vector3f sprintingStance;
    protected int stanceTimer;

    @Setter
    protected EnumLookAtState lookAt = EnumLookAtState.NONE;
    protected float lookAtTimer = 0;

    protected static final int[] lookAtTimes = new int[]{1, 10, 20, 10, 20, 10};

    public enum EnumLookAtState
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
        long seed = seedFromString(shortName);
        RandomSource r = RandomSource.create(seed);
        sprintingStance = new Vector3f(nextRange(r, -15f, -5f), nextRange(r,   0f, 45f), nextRange(r, -20f, 10f));
    }

    private static float nextRange(RandomSource r, float min, float max)
    {
        return min + r.nextFloat() * (max - min);
    }

    /** FNV-1a 64-bit: stable, fast, good enough for seeds */
    private static long seedFromString(String s)
    {
        if (s == null)
            return 0L;
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++)
        {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
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
                    lookAt = EnumLookAtState.values()[(lookAt.ordinal() + 1) % EnumLookAtState.values().length];
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
