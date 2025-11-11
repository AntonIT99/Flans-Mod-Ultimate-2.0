package com.flansmodultimate.common.types;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

@NoArgsConstructor
public class GrenadeType extends ShootableType
{
    //Misc
    /** The damage imparted by smacking someone over the head with this grenade */
    protected int meleeDamage = 1;

    //Throwing
    /** The delay between subsequent grenade throws */
    protected int throwDelay;
    /** The sound to play upon throwing this grenade */
    @Getter
    protected String throwSound = StringUtils.EMPTY;
    /** The name of the item to drop (if any) when throwing the grenade */
    protected String dropItemOnThrow = null;
    /** Whether you can throw this grenade by right clicking */
    protected boolean canThrow = true;

    //Physics
    /** Whether this grenade may pass through entities or blocks */
    protected boolean penetratesEntities;
    protected boolean penetratesBlocks;
    /** The sound to play upon bouncing off a surface */
    protected String bounceSound = StringUtils.EMPTY;
    /** Whether the grenade should stick to surfaces */
    protected boolean sticky;
    /** If true, then the grenade will stick to the player that threw it. Used to make delayed self destruct weapons */
    protected boolean stickToThrower;

    protected boolean stickToEntity;
    protected boolean stickToDriveable;
    protected boolean stickToEntityAfter;
    protected boolean allowStickSound;
    protected int stickSoundRange = 10;
    protected String stickSound;

    protected boolean flashBang;
    protected int flashTime = 200;
    protected int flashRange = 8;

    protected boolean flashSoundEnable;
    protected int flashSoundRange = 16;
    protected String flashSound;

    protected boolean flashDamageEnable;
    protected float flashDamage;

    protected boolean flashEffects;
    protected int flashEffectsID;
    protected int flashEffectsDuration;
    protected int flashEffectsLevel;

    protected boolean motionSensor;
    protected float motionSensorRange = 5.0F;
    protected float motionSoundRange = 20.0F;
    protected String motionSound;
    protected int motionTime = 20;

    //Conditions for detonation
    /** If > 0 this will act like a mine and explode when a living entity comes within this radius of the grenade */
    protected float livingProximityTrigger = -1F;
    /** If > 0 this will act like a mine and explode when a driveable comes within this radius of the grenade */
    protected float driveableProximityTrigger = -1F;
    /**  If true, then anything attacking this entity will detonate it */
    @Getter
    protected boolean detonateWhenShot;
    /** If true, then this grenade can be detonated by any remote detonator tool */
    protected boolean remote;
    /** How much damage to deal to the entity that triggered it */
    protected float damageToTriggerer;

    //Detonation
    /** Detonation will not occur until after this time */
    @Getter
    protected int primeDelay;

    //Aesthetics
    /** Particles given off in the detonation */
    protected int explodeParticles;
    protected String explodeParticleType = "largesmoke";
    /** Whether the grenade should spin when thrown. Generally false for mines or things that should lie flat */
    @Getter
    protected boolean spinWhenThrown = true;

    //Smoke
    /** Time to remain after detonation */
    protected int smokeTime;
    /** Particles given off after detonation */
    protected String smokeParticleType = "explode";
    /** The effects to be given to people coming too close */
    protected List<MobEffectInstance> smokeEffects = new ArrayList<>();
    /** The radius for smoke effects to take place in */
    @Getter
    protected float smokeRadius = 5F;

    //Deployed bag functionality
    /** If true, then right clicking this "grenade" will give the player health or buffs or ammo as defined below */
    @Getter
    protected boolean isDeployableBag;
    /** The number of times players can use this bag before it runs out */
    @Getter
    protected int numUses = 1;
    /** The amount to heal the player using this bag */
    @Getter
    protected float healAmount;
    /** The potion effects to apply to users of this bag */
    @Getter
    protected List<MobEffectInstance> potionEffects = new ArrayList<>();
    /** The number of clips to give to the player when using this bag
     * When they right click with a gun, they will get this number of clips for that gun.
     * They get the first ammo type, as listed in the gun type file
     * The number of clips they get is multiplied by numBulletsInGun too
     */
    @Getter
    protected int numClips;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        meleeDamage = readValue(split, "MeleeDamage", meleeDamage, file);
        //Grenade Throwing
        throwDelay = readValue(split, "ThrowDelay", throwDelay, file);
        meleeDamage = readValue(split, "MeleeDamage", meleeDamage, file);
        throwSound = readValue(split, "ThrowSound", throwSound, file);
        dropItemOnThrow = readValue(split, "DropItemOnThrow", dropItemOnThrow, file);
        canThrow = readValue(split, "CanThrow", canThrow, file);

        //Grenade Physics
        penetratesEntities = readValue(split, "PenetratesEntities", penetratesEntities, file);
        penetratesBlocks = readValue(split, "PenetratesBlocks", penetratesBlocks, file);

        bounceSound = readValue(split, "BounceSound", bounceSound, file);
        livingProximityTrigger = readValue(split, "LivingProximityTrigger", livingProximityTrigger, file);
        driveableProximityTrigger = readValue(split, "VehicleProximityTrigger", driveableProximityTrigger, file);
        damageToTriggerer = readValue(split, "DamageToTriggerer", damageToTriggerer, file);
        primeDelay = readValue(split, "PrimeDelay", primeDelay, file);
        primeDelay = readValue(split, "TriggerDelay", primeDelay, file);

        //Sticky settings
        sticky = readValue(split, "Sticky", sticky, file);
        stickToThrower = readValue(split, "StickToThrower", stickToThrower, file);
        stickToEntity = readValue(split, "StickToEntity", stickToEntity, file);
        stickToDriveable = readValue(split, "StickToDriveable", stickToDriveable, file);
        stickToEntityAfter = readValue(split, "StickToEntityAfter", stickToEntityAfter, file);
        allowStickSound = readValue(split, "AllowStickSound", allowStickSound, file);
        stickSoundRange = readValue(split, "StickSoundRange", stickSoundRange, file);
        stickSound = readSound(split, "StickSound", stickSound, file);

        explodeParticles = readValue(split, "NumExplodeParticles", explodeParticles, file);
        explodeParticleType = readValue(split, "ExplodeParticles", explodeParticleType, file);
        smokeTime = readValue(split, "SmokeTime", smokeTime, file);
        explodeParticles = readValue(split, "NumExplodeParticles", explodeParticles, file);
        smokeParticleType = readValue(split, "SmokeParticles", smokeParticleType, file);
        addEffects(readValues(split, "SmokeEffect", file), smokeEffects, line, file, false, false);

        smokeRadius = readValue(split, "SmokeRadius", smokeRadius, file);
        spinWhenThrown = readValue(split, "SpinWhenThrown", spinWhenThrown, file);
        remote = readValue(split, "Remote", remote, file);

        flashBang = readValue(split, "FlashBang", flashBang, file);
        flashTime = readValue(split, "FlashTime", flashTime, file);
        flashRange = readValue(split, "FlashRange", flashRange, file);
        flashSoundEnable = readValue(split, "FlashSoundEnable", flashSoundEnable, file);
        flashSoundRange = readValue(split, "FlashSoundRange", flashSoundRange, file);
        flashSound = readSound(split, "FlashSound", flashSound, file);
        flashDamageEnable = readValue(split, "FlashDamageEnable", flashDamageEnable, file);
        flashDamage = readValue(split, "FlashDamage", flashDamage, file);
        flashEffects = readValue(split, "FlashEffects", flashEffects, file);
        flashEffectsID = readValue(split, "FlashEffectsID", flashEffectsID, file);
        flashEffectsDuration = readValue(split, "FlashEffectsDuration", flashEffectsDuration, file);
        flashEffectsLevel = readValue(split, "FlashEffectsLevel", flashEffectsLevel, file);
        flashBang = readValue(split, "FlashBang", flashBang, file);

        motionSensor = readValue(split, "MotionSensor", motionSensor, file);
        motionSensorRange = readValue(split, "MotionSensorRange", motionSensorRange, file);
        motionSoundRange = readValue(split, "MotionSoundRange", motionSoundRange, file);
        motionSound = readSound(split, "MotionSound", motionSound, file);
        motionTime = readValue(split, "MotionTime", motionTime, file);

        detonateWhenShot = readValue(split, "DetonateWhenShot", detonateWhenShot, file);

        //Deployable Bag Stuff
        if (split[0].equalsIgnoreCase("DeployableBag"))
        {
            if (split.length > 1)
                isDeployableBag = true;
            else
                isDeployableBag = readValue(split, "DeployableBag", isDeployableBag, file);
        }

        numUses = readValue(split, "NumUses", numUses, file);
        healAmount = readValue(split, "HealAmount", healAmount, file);

        addEffects(readValues(split, "AddPotionEffect", file), potionEffects, line, file, false, false);
        addEffects(readValues(split, "PotionEffect", file), potionEffects, line, file, false, false);

        numClips = readValue(split, "NumClips", numClips, file);
    }
}
