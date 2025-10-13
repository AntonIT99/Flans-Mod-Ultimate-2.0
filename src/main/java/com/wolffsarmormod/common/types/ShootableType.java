package com.wolffsarmormod.common.types;

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

    //Item Stuff
    /**
     * The maximum number of grenades that can be stacked together
     */
    protected int maxStackSize = 1;
    /**
     * Items dropped on various events
     */
    protected String dropItemOnReload = null, dropItemOnShoot = null, dropItemOnHit = null;
    /**
     * The number of rounds fired by a gun per item
     */
    protected int roundsPerItem = 1;
    /**
     * The number of bullet entities to create per round
     */
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

    //Damage to hit entities
    /**
     * Amount of damage to impart upon various entities
     */
    protected float damageVsLiving = 1, damageVsDriveable = 1;
    /**
     * Whether this grenade will break glass when thrown against it
     */
    protected boolean breaksGlass = false;

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
    protected boolean explodeOnImpact = false;

    //Detonation Stuff
    /**
     * The radius in which to spread fire
     */
    protected float fireRadius = 0F;
    /**
     * The radius of explosion upon detonation
     */
    protected float explosionRadius = 0F;
    /**
     * Whether the explosion can destroy blocks
     */
    protected boolean explosionBreaksBlocks = true;
    /**
     * The name of the item to drop upon detonating
     */
    protected String dropItemOnDetonate = null;
    /**
     * Sound to play upon detonation
     */
    protected String detonateSound = "";
}
