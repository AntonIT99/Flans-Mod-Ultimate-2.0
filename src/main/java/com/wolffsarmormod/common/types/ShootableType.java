package com.wolffsarmormod.common.types;

import lombok.Getter;

public abstract class ShootableType extends InfoType
{
    //Aesthetics
    /**
     * Whether trail particles are given off
     */
    protected boolean trailParticles = false;
    /**
     * Trail particles given off by this while being thrown
     */
    protected String trailParticleType = "smoke";

    // hasLight controls whether it has full luminescence.
    // hasDynamicLight controls if it lights up the area around it.
    protected boolean hasLight = false;
    protected boolean hasDynamicLight = false;

    //Item Stuff
    /**
     * The maximum number of grenades that can be stacked together
     */
    @Getter
    protected int maxStackSize = 1;
    /**
     * Items dropped on various events
     */
    @Getter
    protected String dropItemOnReload = null;
    @Getter
    protected String dropItemOnShoot = null;
    @Getter
    protected String dropItemOnHit = null;
    /**
     * The number of rounds fired by a gun per item
     */
    @Getter
    protected int roundsPerItem = 0;
    /**
     * The number of bullet entities to create per round
     */
    @Getter
    protected int numBullets = 1;
    /**
     * Bullet spread multiplier to be applied to gun's bullet spread
     */
    protected float bulletSpread = 1F;

    //Physics and Stuff
    /**
     * The speed at which the grenade should fall
     */
    protected float fallSpeed = 1.0F;
    /**
     * The speed at which to throw the grenade. 0 will just drop it on the floor
     */
    protected float throwSpeed = 1.0F;
    /**
     * Hit box size
     */
    protected float hitBoxSize = 0.5F;
    /**
     * Upon hitting a block or entity, the grenade will be deflected and its motion will be multiplied by this constant
     */
    protected float bounciness = 0.9F;

    //Damage to hit entities
    /**
     * Amount of damage to impart upon various entities
     */
    protected float damageVsPlayer = 1.0F;
    protected float damageVsEntity = 1.0F;
    @Getter
    protected float damageVsLiving = 1.0F;
    protected float damageVsVehicles = 1.0F;
    protected float damageVsPlanes = 1.0F;
    protected boolean readDamageVsPlayer = false;
    protected boolean readDamageVsEntity = false;
    protected boolean readDamageVsPlanes = false;
    /**
     * Whether this grenade will break glass when thrown against it
     */
    @Getter
    protected boolean breaksGlass = false;
    protected float ignoreArmorProbability = 0;
    protected float ignoreArmorDamageFactor = 0;
    protected float blockPenetrationModifier = -1;

    //Detonation Conditions
    /**
     * If 0, then the grenade will last until some other detonation condition is met, else the grenade will detonate after this time (in ticks)
     */
    protected int fuse = 0;
    /**
     * After this time the grenade will despawn quietly. 0 means no despawn time
     */
    protected int despawnTime = 0;
    /**
     * If true, then this will explode upon hitting something
     */
    @Getter
    protected boolean explodeOnImpact = false;

    //Detonation Stuff
    /**
     * The radius in which to spread fire
     */
    @Getter
    protected float fireRadius = 0F;
    /**
     * The radius of explosion upon detonation
     */
    @Getter
    protected float explosionRadius = 0F;
    /**
     * Power of explosion. Multiplier, 1 = vanilla behaviour
     */
    protected float explosionPower = 1F;
    /**
     * Whether the explosion can destroy blocks
     */
    @Getter
    protected boolean explosionBreaksBlocks = true;
    /**
     * Explosion damage vs various classes of entities
     */
    protected float explosionDamageVsLiving = 1.0F;
    protected float explosionDamageVsDriveable = 1.0F;
    protected float explosionDamageVsPlayer = 1.0F;
    protected float explosionDamageVsPlane = 1.0F;
    protected float explosionDamageVsVehicle = 1.0F;
    /**
     * The name of the item to drop upon detonating
     */
    protected String dropItemOnDetonate = null;
    /**
     * Sound to play upon detonation
     */
    protected String detonateSound = "";

    protected boolean hasSubmunitions = false;
    protected String submunition = "";
    protected int numSubmunitions = 0;
    protected int subMunitionTimer = 0;
    protected float submunitionSpread = 1;
    protected boolean destroyOnDeploySubmunition = false;

    protected int smokeParticleCount = 0;
    protected int debrisParticleCount = 0;
}
