package com.wolffsarmormod.common.types;

import lombok.Getter;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;

public class GrenadeType extends ShootableType
{
    //Misc
    /** The damage imparted by smacking someone over the head with this grenade */
    protected int meleeDamage = 1;

    //Throwing
    /** The delay between subsequent grenade throws */
    protected int throwDelay = 0;
    /** The sound to play upon throwing this grenade */
    protected String throwSound = "";
    /** The name of the item to drop (if any) when throwing the grenade */
    protected String dropItemOnThrow = null;
    /** Whether you can throw this grenade by right clicking */
    protected boolean canThrow = true;

    //Physics
    /** Whether this grenade may pass through entities or blocks */
    protected boolean penetratesEntities = false;
    protected boolean penetratesBlocks = false;
    /** The sound to play upon bouncing off a surface */
    protected String bounceSound = "";
    /** Whether the grenade should stick to surfaces */
    protected boolean sticky = false;
    /** If true, then the grenade will stick to the player that threw it. Used to make delayed self destruct weapons */
    protected boolean stickToThrower = false;

    protected boolean stickToEntity = false;
    protected boolean stickToDriveable = false;
    protected boolean stickToEntityAfter = false;
    protected boolean allowStickSound = false;
    protected int stickSoundRange = 10;
    protected String stickSound;

    protected boolean flashBang = false;
    protected int flashTime = 200;
    protected int flashRange = 8;

    protected boolean flashSoundEnable = false;
    protected int flashSoundRange = 16;
    protected String flashSound;

    protected boolean flashDamageEnable = false;
    protected float flashDamage;

    protected boolean flashEffects = false;
    protected int flashEffectsID;
    protected int flashEffectsDuration;
    protected int flashEffectsLevel;

    protected boolean motionSensor = false;
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
    protected boolean detonateWhenShot = false;
    /** If true, then this grenade can be detonated by any remote detonator tool */
    protected boolean remote = false;
    /** How much damage to deal to the entity that triggered it */
    protected float damageToTriggerer = 0F;

    //Detonation
    /** Detonation will not occur until after this time */
    protected int primeDelay = 0;
    //protected boolean antiAirMine = true;
    //protected int antiAirMineAngle = 10;

    //Aesthetics
    /** Particles given off in the detonation */
    protected int explodeParticles = 0;
    protected String explodeParticleType = "largesmoke";
    /** Whether the grenade should spin when thrown. Generally false for mines or things that should lie flat */
    protected boolean spinWhenThrown = true;

    //Smoke
    /** Time to remain after detonation */
    protected int smokeTime = 0;
    /** Particles given off after detonation */
    protected String smokeParticleType = "explode";
    /** The effects to be given to people coming too close */
    protected ArrayList<MobEffectInstance> smokeEffects = new ArrayList<>();
    /** The radius for smoke effects to take place in */
    protected float smokeRadius = 5F;

    //Deployed bag functionality
    /** If true, then right clicking this "grenade" will give the player health or buffs or ammo as defined below */
    @Getter
    protected boolean isDeployableBag = false;
    /** The number of times players can use this bag before it runs out */
    protected int numUses = 1;
    /** The amount to heal the player using this bag */
    protected float healAmount = 0;
    /** The potion effects to apply to users of this bag */
    protected ArrayList<MobEffectInstance> potionEffects = new ArrayList<>();
    /** The number of clips to give to the player when using this bag
     * When they right click with a gun, they will get this number of clips for that gun.
     * They get the first ammo type, as listed in the gun type file
     * The number of clips they get is multiplied by numBulletsInGun too
     */
    protected int numClips = 0;
}
