package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ShootableType extends InfoType
{
    private static final Map<IContentProvider, Map<String, ShootableType>> registeredAmmoList = new HashMap<>();

    //Aesthetics
    /** Whether trail particles are given off */
    @Getter
    protected boolean trailParticles;
    /** Trail particles given off by this while being thrown */
    @Getter
    protected String trailParticleType = "smoke";

    // hasLight controls whether it has full luminescence.
    // hasDynamicLight controls if it lights up the area around it.
    @Getter
    protected boolean hasLight;
    protected boolean hasDynamicLight;

    //Item Stuff
    /** The maximum number of grenades that can be stacked together */
    @Getter
    protected int maxStackSize = 1;
    /** Items dropped on various events */
    @Getter
    protected String dropItemOnReload = null;
    @Getter
    protected String dropItemOnShoot = null;
    @Getter
    protected String dropItemOnHit = null;
    /** The number of rounds fired by a gun per item */
    @Getter
    protected int roundsPerItem = 0;
    /** Number of bullets to fire per shot if allowNumBulletsByBulletType = true */
    @Getter
    protected int numBullets = 1;
    /**
     * Bullet spread multiplier to be applied to gun's bullet spread
     * Ammo-based spread setting if allowSpreadByBullet = true
     */
    @Getter
    protected float bulletSpread = 1F;

    //Physics and Stuff
    /** The speed at which the grenade should fall */
    @Getter
    protected float fallSpeed = 1.0F;
    /** The speed at which to throw the grenade. 0 will just drop it on the floor */
    @Getter
    protected float throwSpeed = 1.0F;
    /** Hit box size */
    @Getter
    protected float hitBoxSize = 0.5F;
    /** Upon hitting a block or entity, the grenade will be deflected and its motion will be multiplied by this constant */
    protected float bounciness = 0.9F;

    //Damage to hit entities
    /** Amount of damage to impart upon various entities */
    protected float damage = 1.0F;
    @Getter
    protected float damageVsLiving = 1.0F;
    protected float damageVsPlayer = 1.0F;
    protected float damageVsEntity = 1.0F;
    @Getter
    protected float damageVsVehicles = 1.0F;
    @Getter
    protected float damageVsPlanes = 1.0F;
    protected boolean readDamageVsLiving;
    protected boolean readDamageVsPlayer;
    protected boolean readDamageVsEntity;
    protected boolean readDamageVsVehicles;
    protected boolean readDamageVsPlanes;
    /** Whether this grenade will break glass when thrown against it */
    @Getter
    protected boolean breaksGlass;
    protected float ignoreArmorProbability = 0;
    protected float ignoreArmorDamageFactor = 0;
    protected float blockPenetrationModifier = -1;

    //Detonation Conditions
    /** If 0, then the grenade will last until some other detonation condition is met, else the grenade will detonate after this time (in ticks) */
    @Getter
    protected int fuse = 0;
    /** After this time the grenade will despawn quietly. 0 means no despawn time */
    protected int despawnTime = 0;
    /** If true, then this will explode upon hitting something */
    @Getter
    protected boolean explodeOnImpact;

    //Detonation Stuff
    /** The radius in which to spread fire */
    @Getter
    protected float fireRadius = 0F;
    /** The radius of explosion upon detonation */
    @Getter
    protected float explosionRadius = 0F;
    /** Power of explosion. Multiplier, 1 = vanilla behaviour */
    @Getter
    protected float explosionPower = 1F;
    /** Whether the explosion can destroy blocks */
    @Getter
    protected boolean explosionBreaksBlocks = true;
    /** Explosion damage vs various classes of entities */
    protected float explosionDamage = 1.0F;
    @Getter
    protected float explosionDamageVsLiving = 1.0F;
    @Getter
    protected float explosionDamageVsPlayer = 1.0F;
    @Getter
    protected float explosionDamageVsPlane = 1.0F;
    @Getter
    protected float explosionDamageVsVehicle = 1.0F;
    protected boolean readExplosionDamageVsLiving;
    protected boolean readExplosionDamageVsPlayer;
    protected boolean readExplosionDamageVsVehicles;
    protected boolean readExplosionDamageVsPlanes;
    /** The name of the item to drop upon detonating */
    @Getter
    protected String dropItemOnDetonate = null;
    /** Sound to play upon detonation */
    @Getter
    protected String detonateSound = "";

    protected boolean hasSubmunitions;
    protected String submunition = "";
    protected int numSubmunitions = 0;
    protected int subMunitionTimer = 0;
    protected float submunitionSpread = 1;
    protected boolean destroyOnDeploySubmunition;

    @Getter
    protected int smokeParticleCount = 0;
    @Getter
    protected int debrisParticleCount = 0;

    @Override
    public void onItemRegistration(String registeredItemId)
    {
        super.onItemRegistration(registeredItemId);
        registeredAmmoList.putIfAbsent(contentPack, new HashMap<>());
        registeredAmmoList.get(contentPack).put(originalShortName, this);
    }

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        //Item Stuff
        maxStackSize = readValue(split, "StackSize", maxStackSize, file);
        maxStackSize = readValue(split, "MaxStackSize", maxStackSize, file);
        dropItemOnShoot = readValue(split, "DropItemOnShoot", dropItemOnShoot, file);
        dropItemOnReload = readValue(split, "DropItemOnReload", dropItemOnReload, file);
        dropItemOnHit = readValue(split, "DropItemOnHit", dropItemOnHit, file);
        roundsPerItem = readValue(split, "RoundsPerItem", roundsPerItem, file);
        numBullets = readValue(split, "NumBullets", numBullets, file);

        // Physics
        bulletSpread = readValue(split, "Accuracy", bulletSpread, file);
        bulletSpread = readValue(split, "Spread", bulletSpread, file);
        fallSpeed = readValue(split, "FallSpeed", fallSpeed, file);
        throwSpeed = readValue(split, "ThrowSpeed", throwSpeed, file);
        throwSpeed = readValue(split, "ShootSpeed", throwSpeed, file);
        hitBoxSize = readValue(split, "HitBoxSize", fallSpeed, file);

        //Hit stuff
        damage = readValue(split, "Damage", damage, file);

        if (split[0].equalsIgnoreCase("DamageVsLiving") || split[0].equalsIgnoreCase("HitEntityDamage"))
        {
            damageVsLiving = readValue(split, "DamageVsLiving", damageVsLiving, file);
            damageVsLiving = readValue(split, "HitEntityDamage", damageVsLiving, file);
            readDamageVsLiving = true;
        }
        else if (split[0].equalsIgnoreCase("DamageVsPlayer")) {
            damageVsPlayer = readValue(split, "DamageVsPlayer", damageVsPlayer, file);
            readDamageVsPlayer = true;
        }
        else if (split[0].equalsIgnoreCase("DamageVsEntity"))
        {
            damageVsEntity = readValue(split, "DamageVsEntity", damageVsEntity, file);
            readDamageVsEntity = true;
        }
        else if (split[0].equalsIgnoreCase("DamageVsVehicles"))
        {
            damageVsVehicles = readValue(split, "DamageVsVehicles", damageVsVehicles, file);
            readDamageVsVehicles = true;
        }
        else if (split[0].equalsIgnoreCase("DamageVsPlanes"))
        {
            damageVsPlanes = readValue(split, "DamageVsPlanes", damageVsPlanes, file);
            readDamageVsPlanes = true;
        }

        blockPenetrationModifier = readValue(split, "BlockPenetrationModifier", blockPenetrationModifier, file);
        ignoreArmorProbability = readValue(split, "IgnoreArmorProbability", ignoreArmorProbability, file);
        ignoreArmorDamageFactor = readValue(split, "IgnoreArmorDamageFactor", ignoreArmorDamageFactor, file);
        breaksGlass = readValue(split, "reaksGlass", breaksGlass, file);
        bounciness = readValue(split, "Bounciness", bounciness, file);
        hasLight = readValue(split, "HasLight", hasLight, file);
        hasDynamicLight = readValue(split, "HasDynamicLight", hasDynamicLight, file);

        // Detonation conditions etc
        fuse = readValue(split, "Fuse", fuse, file);
        despawnTime = readValue(split, "DespawnTime", despawnTime, file);
        explodeOnImpact = readValue(split, "ExplodeOnImpact", explodeOnImpact, file);
        explodeOnImpact = readValue(split, "DetonateOnImpact", explodeOnImpact, file);

        //Detonation
        fireRadius = readValue(split, "FireRadius", fireRadius, file);
        fireRadius = readValue(split, "Fire", fireRadius, file);
        explosionRadius = readValue(split, "ExplosionRadius", explosionRadius, file);
        explosionRadius = readValue(split, "Explosion", explosionRadius, file);
        explosionPower = readValue(split, "ExplosionPower", explosionPower, file);
        explosionBreaksBlocks = readValue(split, "ExplosionBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue(split, "ExplosionsBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue(split, "ExplosionBreakBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue(split, "ExplosionsBreakBlocks", explosionBreaksBlocks, file);

        explosionDamage = readValue(split, "ExplosionDamage", explosionDamage, file);
        if (split[0].equalsIgnoreCase("ExplosionDamageVsLiving") || split[0].equalsIgnoreCase("ExplosionDamageVsDrivable"))
        {
            explosionDamageVsLiving = readValue(split, "ExplosionDamageVsLiving", explosionDamageVsLiving, file);
            explosionDamageVsVehicle = readValue(split, "ExplosionDamageVsDrivable", explosionDamageVsVehicle, file);
            readExplosionDamageVsLiving = true;
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsPlayer"))
        {
            explosionDamageVsPlane = readValue(split, "ExplosionDamageVsPlane", explosionDamageVsPlane, file);
            readExplosionDamageVsPlayer = true;
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsPlane"))
        {
            explosionDamageVsPlayer = readValue(split, "ExplosionDamageVsPlayer", explosionDamageVsPlayer, file);
            readExplosionDamageVsPlanes = true;
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsVehicle"))
        {
            explosionDamageVsVehicle = readValue(split, "ExplosionDamageVsVehicle", explosionDamageVsVehicle, file);
            readExplosionDamageVsVehicles = true;
        }

        dropItemOnDetonate = readValue(split, "DropItemOnDetonate", dropItemOnDetonate, file);
        detonateSound = readValue(split, "DetonateSound", detonateSound, file);

        //Submunitions
        hasSubmunitions = readValue(split, "HasSubmunitions", hasSubmunitions, file);
        submunition = readValue(split, "Submunition", submunition, file);
        numSubmunitions = readValue(split, "NumSubmunitions", numSubmunitions, file);
        subMunitionTimer = readValue(split, "SubmunitionDelay", subMunitionTimer, file);
        submunitionSpread = readValue(split, "SubmunitionSpread", submunitionSpread, file);
        destroyOnDeploySubmunition = readValue(split, "DestroyOnDeploySubmunition", destroyOnDeploySubmunition, file);
        smokeParticleCount = readValue(split, "FlareParticleCount", smokeParticleCount, file);
        debrisParticleCount = readValue(split, "DebrisParticleCount", debrisParticleCount, file);

        //Particles
        trailParticles = readValue(split, "TrailParticles", trailParticles, file);
        trailParticles = readValue(split, "SmokeTrail", trailParticles, file);
        trailParticleType = readValue(split, "TrailParticleType", trailParticleType, file);
    }

    @Override
    protected void postRead()
    {
        super.postRead();

        if (!readDamageVsLiving)
            damageVsLiving = damage;
        if (!readDamageVsPlayer)
            damageVsPlayer = damageVsLiving;
        if (!readDamageVsVehicles)
            damageVsVehicles = damage;
        if (!readDamageVsEntity)
            damageVsEntity = damageVsVehicles;
        if (!readDamageVsPlanes)
            damageVsPlanes = damageVsVehicles;

        if (!readExplosionDamageVsLiving)
            explosionDamageVsLiving = explosionDamage;
        if (!readDamageVsPlayer)
            explosionDamageVsPlayer = explosionDamageVsLiving;
        if (!readDamageVsVehicles)
            explosionDamageVsVehicle = explosionDamage;
        if (!readDamageVsPlanes)
            explosionDamageVsPlane = explosionDamageVsVehicle;
    }

    public float getDamage(Entity entity)
    {
        if (entity instanceof Player)
            return damageVsPlayer;
        else if (entity instanceof Plane)
            return damageVsPlanes;
        else if (entity instanceof Driveable)
            return damageVsVehicles;
        else if (entity instanceof LivingEntity)
            return damageVsLiving;
        else
            return damage;
    }

    public static List<ShootableType> getAmmoTypes(Set<String> shortnames, IContentProvider contentPack)
    {
        ArrayList<ShootableType> list = new ArrayList<>();
        for (String shortname : shortnames)
        {
            // Search for ammo with a corresponding shortname in the same content pack
            // If no ammo is found, search for all ammos with a corresponding shortname in all content packs
            if (registeredAmmoList.containsKey(contentPack) && registeredAmmoList.get(contentPack).containsKey(shortname))
            {
                list.add(registeredAmmoList.get(contentPack).get(shortname));
            }
            else
            {
                list.addAll(registeredAmmoList.values().stream().map(pack -> pack.get(shortname)).toList());
            }
        }
        return list;
    }
}
