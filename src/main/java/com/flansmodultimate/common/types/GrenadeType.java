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
    public static final int SMOKE_PARTICLES_COUNT = 50;
    public static final int SMOKE_PARTICLES_RANGE = 30;

    //Misc
    /** The damage imparted by smacking someone over the head with this grenade */
    @Getter
    protected int meleeDamage = 1;

    //Throwing
    /** The delay between subsequent grenade throws */
    @Getter
    protected int throwDelay;
    /** The sound to play upon throwing this grenade */
    @Getter
    protected String throwSound = StringUtils.EMPTY;
    /** The name of the item to drop (if any) when throwing the grenade */
    @Getter
    protected String dropItemOnThrow = null;
    /** Whether you can throw this grenade by right clicking */
    @Getter
    protected boolean canThrow = true;

    //Physics
    @Getter
    protected boolean penetratesBlocks;
    /** The sound to play upon bouncing off a surface */
    @Getter
    protected String bounceSound = StringUtils.EMPTY;
    /** Whether the grenade should stick to surfaces */
    @Getter
    protected boolean sticky;
    /** If true, then the grenade will stick to the player that threw it. Used to make delayed self destruct weapons */
    @Getter
    protected boolean stickToThrower;
    @Getter
    protected boolean stickToEntity;
    @Getter
    protected boolean stickToDriveable;
    @Getter
    protected boolean stickToEntityAfter;
    @Getter
    protected boolean allowStickSound;
    @Getter
    protected int stickSoundRange = 10;
    @Getter
    protected String stickSound;

    @Getter
    protected boolean flashBang;
    @Getter
    protected int flashTime = 200;
    @Getter
    protected int flashRange = 8;

    @Getter
    protected boolean flashSoundEnable;
    @Getter
    protected int flashSoundRange = 16;
    @Getter
    protected String flashSound;

    @Getter
    protected boolean flashDamageEnable;
    @Getter
    protected float flashDamage;

    @Getter
    protected boolean flashEffects;
    @Getter
    protected int flashEffectsId;
    @Getter
    protected int flashEffectsDuration;
    @Getter
    protected int flashEffectsLevel;

    //Conditions for detonation
    /** If > 0 this will act like a mine and explode when a living entity comes within this radius of the grenade */
    @Getter
    protected float livingProximityTrigger = -1F;
    /** If > 0 this will act like a mine and explode when a driveable comes within this radius of the grenade */
    @Getter
    protected float driveableProximityTrigger = -1F;
    /**  If true, then anything attacking this entity will detonate it */
    @Getter
    protected boolean detonateWhenShot;
    /** If true, then this grenade can be detonated by any remote detonator tool */
    @Getter
    protected boolean remote;
    /** How much damage to deal to the entity that triggered it */
    @Getter
    protected float damageToTriggerer;

    //Detonation
    /** Detonation will not occur until after this time */
    @Getter
    protected int primeDelay;

    //Aesthetics
    /** Particles given off in the detonation */
    @Getter
    protected int explodeParticles;
    @Getter
    protected String explodeParticleType = "largesmoke";
    /** Whether the grenade should spin when thrown. Generally false for mines or things that should lie flat */
    @Getter
    protected boolean spinWhenThrown = true;

    //Smoke
    /** Time to remain after detonation */
    @Getter
    protected int smokeTime;
    /** Particles given off after detonation */
    @Getter
    protected String smokeParticleType = "explode";
    /** The effects to be given to people coming too close */
    @Getter
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
        flashEffectsId = readValue(split, "FlashEffectsID", flashEffectsId, file);
        flashEffectsDuration = readValue(split, "FlashEffectsDuration", flashEffectsDuration, file);
        flashEffectsLevel = readValue(split, "FlashEffectsLevel", flashEffectsLevel, file);
        flashBang = readValue(split, "FlashBang", flashBang, file);

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
