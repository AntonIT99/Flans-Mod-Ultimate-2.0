package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ShootableType extends InfoType
{
    public static final int EXPLODE_PARTICLES_RANGE = 200;
    public static final double FALL_SPEED_COEFFICIENT = (9.81 / 400.0);

    private static final Map<IContentProvider, Map<String, ShootableType>> registeredAmmoList = new HashMap<>();

    /** Controls whether it has full luminescence */
    @Getter
    protected boolean hasLight;
    /** Controls if it lights up the area around it */
    @Getter
    protected boolean hasDynamicLight;

    //Item Stuff
    /** The maximum number of grenades that can be stacked together */
    @Getter
    protected int maxStackSize = 1;
    /** Items dropped on various events */
    @Getter
    protected String dropItemOnReload;
    @Getter
    protected String dropItemOnShoot;
    @Getter
    protected String dropItemOnHit;
    /** The number of rounds fired by a gun per item */
    @Getter
    protected int roundsPerItem;
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
    protected float fallSpeed = 1F; //TODO: idea - define weight
    /** The speed at which to throw the grenade. 0 will just drop it on the floor */
    @Getter
    protected float throwSpeed = 1F;
    /** Hit box size */
    @Getter
    protected float hitBoxSize = 0.5F;
    /** Upon hitting a block or entity, the grenade will be deflected and its motion will be multiplied by this constant */
    @Getter
    protected float bounciness = 0F;

    //Damage to hit entities
    /** Amount of damage to impart upon various entities */
    @Getter
    protected final DamageStats damage = new DamageStats();
    /** Whether this grenade will break glass when thrown against it */
    @Getter
    protected boolean breaksGlass;
    @Getter
    protected float ignoreArmorProbability;
    @Getter
    protected float ignoreArmorDamageFactor;

    //Detonation Conditions
    /** If 0, then the grenade will last until some other detonation condition is met, else the grenade will detonate after this time (in ticks) */
    @Getter
    protected int fuse;
    /** After this time the grenade will despawn quietly. 0 means no despawn time */
    @Getter
    protected int despawnTime;
    /** If true, then this will explode upon hitting something */
    @Getter
    protected boolean explodeOnImpact;
    /** If > 0 this will act like a mine and explode when a living entity comes within this radius of the grenade */
    @Getter
    protected float livingProximityTrigger = -1F;
    /** If > 0 this will act like a mine and explode when a driveable comes within this radius of the grenade */
    @Getter
    protected float driveableProximityTrigger = -1F;
    /** How much damage to deal to the entity that triggered it */
    @Getter
    protected float damageToTriggerer;
    /** Detonation will not occur until after this time */
    @Getter
    protected int primeDelay;

    //Detonation Stuff
    /** The radius in which to spread fire */
    @Getter
    protected float fireRadius;
    /** The radius of explosion upon detonation */
    @Getter
    protected float explosionRadius;
    /** Power of explosion. Multiplier, 1 = vanilla behaviour */
    @Getter
    protected float explosionPower = 1F;
    /** Whether the explosion can destroy blocks */
    @Getter
    protected boolean explosionBreaksBlocks = true;
    /** Explosion damage vs various classes of entities */
    @Getter
    protected final DamageStats explosionDamage = new DamageStats();
    /** The name of the item to drop upon detonating */
    @Getter
    protected String dropItemOnDetonate;
    /** Sound to play upon detonation */
    @Getter
    protected String detonateSound = StringUtils.EMPTY;

    //Submunitions
    @Getter
    protected boolean hasSubmunitions;
    @Getter
    protected String submunition = StringUtils.EMPTY;
    @Getter
    protected int numSubmunitions;
    @Getter
    protected int subMunitionTimer;
    protected float submunitionSpread = 1;
    @Getter
    protected boolean destroyOnDeploySubmunition;

    //Particles and Smoke
    /** Whether trail particles are given off */
    @Getter
    protected boolean trailParticles;
    /** Trail particles given off by this while being thrown */
    @Getter
    protected String trailParticleType = "smoke";
    @Getter
    protected int smokeParticleCount;
    @Getter
    protected int debrisParticleCount;
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
    /** Particles given off in the detonation */
    @Getter
    protected int explodeParticles;
    @Getter
    protected String explodeParticleType = "largesmoke";

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
        if (split[0].equalsIgnoreCase("Damage") || split[0].equalsIgnoreCase("DamageVsEntity") || split[0].equalsIgnoreCase("HitEntityDamage"))
        {
            damage.setDamage(readValue(split, "Damage", damage.getDamage(), file));
            damage.setDamage(readValue(split, "DamageVsEntity", damage.getDamage(), file));
            damage.setDamage(readValue(split, "HitEntityDamage", damage.getDamage(), file));
            damage.setReadDamage(true);
        }
        else if (split[0].equalsIgnoreCase("DamageVsLiving"))
        {
            damage.setDamageVsLiving(readValue(split, "DamageVsLiving", damage.getDamageVsLiving(), file));
            damage.setReadDamageVsLiving(true);
        }
        else if (split[0].equalsIgnoreCase("DamageVsPlayer") || split[0].equalsIgnoreCase("DamageVsPlayers"))
        {
            damage.setDamageVsPlayer(readValue(split, "DamageVsPlayer", damage.getDamageVsPlayer(), file));
            damage.setDamageVsPlayer(readValue(split, "DamageVsPlayers", damage.getDamageVsPlayer(), file));
            damage.setReadDamageVsPlayer(true);
        }
        else if (split[0].equalsIgnoreCase("DamageVsVehicle") || split[0].equalsIgnoreCase("DamageVsVehicles") || split[0].equalsIgnoreCase("DamageVsDrivable") || split[0].equalsIgnoreCase("DamageVsDrivables"))
        {
            damage.setDamageVsVehicles(readValue(split, "DamageVsVehicle", damage.getDamageVsVehicles(), file));
            damage.setDamageVsVehicles(readValue(split, "DamageVsVehicles", damage.getDamageVsVehicles(), file));
            damage.setDamageVsVehicles(readValue(split, "DamageVsDrivable", damage.getDamageVsVehicles(), file));
            damage.setDamageVsVehicles(readValue(split, "DamageVsDrivables", damage.getDamageVsVehicles(), file));
            damage.setReadDamageVsVehicles(true);
        }
        else if (split[0].equalsIgnoreCase("DamageVsPlane") || split[0].equalsIgnoreCase("DamageVsPlanes"))
        {
            damage.setDamageVsPlanes(readValue(split, "DamageVsPlane", damage.getDamageVsPlanes(), file));
            damage.setDamageVsPlanes(readValue(split, "DamageVsPlanes", damage.getDamageVsPlanes(), file));
            damage.setReadDamageVsPlanes(true);
        }

        ignoreArmorProbability = readValue(split, "IgnoreArmorProbability", ignoreArmorProbability, file);
        ignoreArmorDamageFactor = readValue(split, "IgnoreArmorDamageFactor", ignoreArmorDamageFactor, file);
        breaksGlass = readValue(split, "BreaksGlass", breaksGlass, file);
        bounciness = readValue(split, "Bounciness", bounciness, file);
        hasLight = readValue(split, "HasLight", hasLight, file);
        hasDynamicLight = readValue(split, "HasDynamicLight", hasDynamicLight, file);

        // Detonation conditions etc
        fuse = readValue(split, "Fuse", fuse, file);
        despawnTime = readValue(split, "DespawnTime", despawnTime, file);
        explodeOnImpact = readValue(split, "ExplodeOnImpact", explodeOnImpact, file);
        explodeOnImpact = readValue(split, "DetonateOnImpact", explodeOnImpact, file);
        livingProximityTrigger = readValue(split, "LivingProximityTrigger", livingProximityTrigger, file);
        driveableProximityTrigger = readValue(split, "VehicleProximityTrigger", driveableProximityTrigger, file);
        damageToTriggerer = readValue(split, "DamageToTriggerer", damageToTriggerer, file);
        primeDelay = readValue(split, "PrimeDelay", primeDelay, file);
        primeDelay = readValue(split, "TriggerDelay", primeDelay, file);

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

        if (split[0].equalsIgnoreCase("ExplosionDamage") || split[0].equalsIgnoreCase("ExplosionDamageVsEntity"))
        {
            explosionDamage.setDamage(readValue(split, "ExplosionDamage", explosionDamage.getDamage(), file));
            explosionDamage.setDamage(readValue(split, "ExplosionDamageVsEntity", explosionDamage.getDamage(), file));
            explosionDamage.setReadDamage(true);
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsLiving"))
        {
            explosionDamage.setDamageVsLiving(readValue(split, "ExplosionDamageVsLiving", explosionDamage.getDamageVsLiving(), file));
            explosionDamage.setReadDamageVsLiving(true);
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsPlayer") || split[0].equalsIgnoreCase("ExplosionDamageVsPlayers"))
        {
            explosionDamage.setDamageVsPlayer(readValue(split, "ExplosionDamageVsPlayer", explosionDamage.getDamageVsPlayer(), file));
            explosionDamage.setDamageVsPlayer(readValue(split, "ExplosionDamageVsPlayers", explosionDamage.getDamageVsPlayer(), file));
            explosionDamage.setReadDamageVsPlayer(true);
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsVehicle") || split[0].equalsIgnoreCase("ExplosionDamageVsVehicles") || split[0].equalsIgnoreCase("ExplosionDamageVsDrivable") || split[0].equalsIgnoreCase("ExplosionDamageVsDrivables"))
        {
            explosionDamage.setDamageVsVehicles(readValue(split, "ExplosionDamageVsVehicle", explosionDamage.getDamageVsVehicles(), file));
            explosionDamage.setDamageVsVehicles(readValue(split, "ExplosionDamageVsVehicles", explosionDamage.getDamageVsVehicles(), file));
            explosionDamage.setDamageVsVehicles(readValue(split, "ExplosionDamageVsDrivable", explosionDamage.getDamageVsVehicles(), file));
            explosionDamage.setDamageVsVehicles(readValue(split, "ExplosionDamageVsDrivables", explosionDamage.getDamageVsVehicles(), file));
            explosionDamage.setReadDamageVsVehicles(true);
        }
        else if (split[0].equalsIgnoreCase("ExplosionDamageVsPlane") || split[0].equalsIgnoreCase("ExplosionDamageVsPlanes"))
        {
            explosionDamage.setDamageVsPlanes(readValue(split, "ExplosionDamageVsPlane", explosionDamage.getDamageVsPlanes(), file));
            explosionDamage.setDamageVsPlanes(readValue(split, "ExplosionDamageVsPlanes", explosionDamage.getDamageVsPlanes(), file));
            explosionDamage.setReadDamageVsPlanes(true);
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
        explodeParticles = readValue(split, "NumExplodeParticles", explodeParticles, file);
        explodeParticleType = readValue(split, "ExplodeParticles", explodeParticleType, file);
        smokeTime = readValue(split, "SmokeTime", smokeTime, file);
        smokeParticleType = readValue(split, "SmokeParticles", smokeParticleType, file);
        addEffects(readValues(split, "SmokeEffect", file), smokeEffects, line, file, false, false);
        smokeRadius = readValue(split, "SmokeRadius", smokeRadius, file);
    }

    @Override
    protected void postRead()
    {
        super.postRead();
        damage.calculate();
        explosionDamage.calculate();
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

    public static Optional<ShootableType> getAmmoType(String shortname, IContentProvider contentPack) {
        return getAmmoTypes(Set.of(shortname), contentPack).stream().findFirst();
    }
}
