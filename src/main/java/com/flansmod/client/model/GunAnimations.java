package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.render.effects.FirstPersonGunEffects;
import com.flansmodultimate.client.render.effects.GunSmokePuff;
import com.flansmodultimate.common.types.GunType;
import lombok.*;

import net.minecraft.util.RandomSource;

import java.util.Random;

@Getter
@Setter
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
    /** True for the client tick in which a pump/bolt cycle closes. */
    protected boolean pumpFinished;

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
    /** The gun still held live rounds when this reload began */
    protected boolean tacticalReload;
    /** A single-magazine gun reloading round by round, playing its start, loop and end keyframe animations */
    protected boolean loopReload;

    /** Keyframe shoot animation progress, 0 to 1; at rest once it reaches 1 */
    protected float keyframeRecoilProgress = 1F;
    protected float lastKeyframeRecoilProgress = 1F;
    protected float keyframeRecoilStep;

    /** Ammo variant ({@code AmmoModelType}) last drawn, and the one drawn when the current reload began */
    protected int lastAmmoModelType = 1;
    protected int preReloadAmmoModelType = 1;

    /** Tyrants and Plebeians smoke and casing effects, consumed by the first-person renderer */
    protected boolean createMuzzleSmoke;
    protected boolean createChamberSmoke;
    protected boolean createCasing;
    protected final GunSmokePuff[] muzzleSmoke = FirstPersonGunEffects.createSmoke(16);
    protected int nextMuzzleSmokeIndex;

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
    protected int secondarySwitchRemaining;
    protected int secondarySwitchLength;

    /** Fancy stance / running animation stuff */
    protected float runningStanceAnimationProgress;
    protected float sprintTransitionProgress;
    protected float lastSprintTransitionProgress;
    protected float runningStanceAnimationLength = 4F;
    protected Vector3f sprintingStance;
    protected int stanceTimer;

    @Setter
    protected EnumLookAtState lookAt = EnumLookAtState.NONE;
    protected int lookAtTimer = 0;

    @Getter
    public enum EnumLookAtState
    {
        NONE(1), TILT1(10), LOOK1(20), TILT2(10), LOOK2(20), UNTILT(10);

        private final int time;

        EnumLookAtState(int time)
        {
            this.time = time;
        }
    }

    /** Client-tick pose state for an authored RunPosTime; rendering only interpolates it. */
    public void updateSprintTransition(boolean sprinting, float ticks)
    {
        lastSprintTransitionProgress = sprintTransitionProgress;
        sprintTransitionProgress = com.flansmodultimate.common.guns.GunHandlingTiming.sprintProgress(sprintTransitionProgress, sprinting, ticks);
        if (ticks <= 0F)
            lastSprintTransitionProgress = sprintTransitionProgress;
    }

    public void beginSecondaryModeSwitch(int ticks)
    {
        secondarySwitchLength = Math.max(0, Math.min(1200, ticks));
        secondarySwitchRemaining = secondarySwitchLength;
    }

    public void updateSprintStance(GunType type)
    {
        long seed = seedFromString(type.getShortName());
        RandomSource r = RandomSource.create(seed);
        sprintingStance = new Vector3f(nextRange(r, -15f, -5f), nextRange(r, 0f, 45f), nextRange(r, -20f, 10f));
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
        pumpFinished = false;
        secondarySwitchRemaining = Math.max(0, secondarySwitchRemaining - 1);
        // Assign values
        lastPumped = pumped;
        lastCharged = charged;
        lastGunPullback = gunPullback;
        lastCasingStage = casingStage;

        // Time until pump-action
        if (timeUntilPump > 0)
        {
            timeUntilPump--;
            if (timeUntilPump == 0)
            {
                // Pump it!
                pumping = true;
                lastPumped = pumped = -1F;
                ModClient.setShotState(1);
            }
        }

        // Timer until pulling back the charge handle/bolt
        if (timeUntilCharge > 0)
        {
            timeUntilCharge--;
            if (timeUntilCharge == 0)
            {
                // Pump it!
                charging = true;
                lastCharged = charged = -1F;
            }
        }

        // Time until hammer pullback
        if (timeUntilPullback > 0)
        {
            timeUntilPullback--;
            if (timeUntilPullback == 0)
            {
                // Reset the hammer
                isFired = true;
                lastGunPullback = gunPullback = -1F;
            }
        }
        else
        {
            // Automatically reset hammer
            hammerRotation *= 0.6F;
            althammerRotation *= 0.6F;
        }

        // Time until bullet casing ejection
        if (timeUntilCasing > 0)
        {
            timeUntilCasing--;
            if (timeUntilCasing == 0)
            {
                casingStage++;
                createCasing = true;
            }
        }
        else
        {
            casingStage++;
            if (casingStage == 1)
                createCasing = true;
        }

        for (GunSmokePuff puff : muzzleSmoke)
            puff.tick(0F, 0F, 0F);

        if (muzzleFlashTime > 0)
            muzzleFlashTime--;

        if (pumping)
        {
            pumped += 2F / timeToPumpFor;
            if (pumped >= 0.999F)
            {
                pumping = false;
                pumpFinished = true;
            }
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

        // Recoil model
        lastGunRecoil = gunRecoil;
        if (gunRecoil > 0)
            gunRecoil *= 0.7F;

        // Slide model
        lastGunSlide = gunSlide;
        if (isGunEmpty)
            lastGunSlide = gunSlide = 0.5F;
        if (!isGunEmpty && gunSlide > 0.9)
            gunSlide -= 0.1F;
        else if (gunSlide > 0 && !isGunEmpty)
            gunSlide *= 0.5F;

        lastKeyframeRecoilProgress = keyframeRecoilProgress;
        if (keyframeRecoilProgress < 1F)
            keyframeRecoilProgress = Math.min(1F, keyframeRecoilProgress + keyframeRecoilStep);

        // Reload
        lastReloadAnimationProgress = reloadAnimationProgress;
        if (reloading)
            reloadAnimationProgress += 1F / reloadAnimationTime;
        if (reloading && reloadAnimationProgress >= 0.9F)
            isGunEmpty = false;
        if (reloading && reloadAnimationProgress >= 1F)
            reloading = false;

        minigunBarrelRotation += minigunBarrelRotationSpeed;
        minigunBarrelRotationSpeed *= 0.9F;

        if (meleeAnimationLength > 0)
        {
            meleeAnimationProgress++;
            // If we are done, reset
            if (meleeAnimationProgress > meleeAnimationLength)
                meleeAnimationProgress = meleeAnimationLength = 0;
        }

        if (switchAnimationProgress > 0)
        {
            switchAnimationProgress += 1F;
            // If we are done, reset
            if (switchAnimationProgress == switchAnimationLength)
                switchAnimationLength = 0;
        }

        if (runningStanceAnimationProgress > 0 && runningStanceAnimationProgress < runningStanceAnimationLength)
            runningStanceAnimationProgress += 1F;

        if (stanceTimer > 0)
            stanceTimer--;

        switch (lookAt)
        {
            case NONE :
                lookAtTimer = 0;
                break;
            case TILT1, LOOK1, TILT2, LOOK2, UNTILT :
            {
                lookAtTimer++;
                if (lookAtTimer >= lookAt.getTime())
                {
                    lookAt = EnumLookAtState.values()[(lookAt.ordinal() + 1) % EnumLookAtState.values().length];
                    lookAtTimer = 0;
                }
                break;
            }
            default :
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
        // Accumulative recoil function
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
        createCasing = false;
        createMuzzleSmoke = true;
        createChamberSmoke = true;

        if (pumpDelay == 0)
            ModClient.setShotState(1);
    }

    /**
     * Restart the keyframe shoot animation. Like Tyrants and Plebeians it lasts {@code 20 * 0.56667 * (recoilTime / 20)}
     * ticks with whole-number division, so models authored for it keep their timing; under 20 ticks it ends at once.
     */
    // Whole-number division is deliberate: it matches the Tyrants and Plebeians timing
    @SuppressWarnings("java:S2184")
    public void startKeyframeRecoil(int recoilTime)
    {
        float duration = 20F * 0.56667F * (recoilTime / 20);
        keyframeRecoilStep = duration > 0F ? 1F / duration : 1F;
        lastKeyframeRecoilProgress = keyframeRecoilProgress = duration > 0F ? 0F : 1F;
    }

    public void doReload(float reloadTime, int pumpDelay, int pumpTime, int chargeDelay, int chargeTime, int ammoCount, boolean hasMultipleAmmo)
    {
        doReload(reloadTime, pumpDelay, pumpTime, chargeDelay, chargeTime, ammoCount, hasMultipleAmmo, false, 0);
    }

    /** @param loopRounds rounds loaded one at a time by a round-by-round reload, or 0 for any other reload */
    // Extends the existing overload with the reload kind that the server sends along
    @SuppressWarnings("java:S107")
    public void doReload(float reloadTime, int pumpDelay, int pumpTime, int chargeDelay, int chargeTime, int ammoCount, boolean hasMultipleAmmo, boolean tactical, int loopRounds)
    {
        tacticalReload = tactical;
        loopReload = loopRounds > 0;
        preReloadAmmoModelType = lastAmmoModelType;
        reloading = true;
        lastReloadAnimationProgress = reloadAnimationProgress = 0F;
        reloadAnimationTime = reloadTime;
        timeUntilPump = pumpDelay;
        timeToPumpFor = pumpTime;
        timeUntilCharge = chargeDelay;
        timeToChargeFor = chargeTime;
        reloadAmmoCount = loopReload ? loopRounds : ammoCount;
        singlesReload = hasMultipleAmmo;
        ModClient.setLastBulletReload(ammoCount - 1);
    }

    /** The muzzle puff slot to reuse for the next shot. */
    public GunSmokePuff nextMuzzleSmoke()
    {
        GunSmokePuff puff = muzzleSmoke[nextMuzzleSmokeIndex];
        nextMuzzleSmokeIndex = (nextMuzzleSmokeIndex + 1) % muzzleSmoke.length;
        return puff;
    }

    /** Takes a pending effect request, so each shot spawns it once. */
    public boolean consumeMuzzleSmoke()
    {
        boolean pending = createMuzzleSmoke;
        createMuzzleSmoke = false;
        return pending;
    }

    public boolean consumeChamberSmoke()
    {
        boolean pending = createChamberSmoke;
        createChamberSmoke = false;
        return pending;
    }

    public boolean consumeCasing()
    {
        boolean pending = createCasing;
        createCasing = false;
        return pending;
    }

    public void cancelReload()
    {
        reloading = false;
        tacticalReload = false;
        loopReload = false;
        lastReloadAnimationProgress = reloadAnimationProgress = 0F;
        reloadAnimationTime = 0;
    }

    public void doMelee(int meleeTime)
    {
        meleeAnimationLength = meleeTime;
    }
}
