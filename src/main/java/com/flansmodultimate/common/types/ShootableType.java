package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.config.ModCommonConfigs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ShootableType extends InfoType
{
    public static final int EXPLODE_PARTICLES_RANGE = 256;
    public static final double FALL_SPEED_COEFFICIENT = (9.81 / 400.0);
    public static final float AIR_DEFAULT_DRAG = 0.99F;
    public static final float WATER_DEFAULT_DRAG = 0.8F;
    public static final float LAVA_DEFAULT_DRAG = 0.6F;

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
    protected float bulletSpread = -1F;

    //Physics and Stuff
    /** The speed at which the grenade should fall */
    @Getter
    protected float fallSpeed = 1F;
    /** The speed at which to throw the grenade. 0 will just drop it on the floor */
    @Getter
    protected float throwSpeed = 1F;
    /** Hit box size */
    @Getter
    protected float hitBoxSize = 0.5F;
    /** Upon hitting a block or entity, the grenade will be deflected and its motion will be multiplied by this constant */
    @Getter
    protected float bounciness;
    /** Mass of the projectile in g. Used for the new damage system. Will be ignored when 0 */
    @Getter
    protected float mass;

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
    @Getter
    protected float submunitionSpread = 1F;
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

    public boolean useNewDamageSystem()
    {
        return mass > 0F;
    }

    @Override
    public void onItemRegistration(String registeredItemId)
    {
        super.onItemRegistration(registeredItemId);
        registeredAmmoList.putIfAbsent(contentPack, new HashMap<>());
        registeredAmmoList.get(contentPack).put(originalShortName, this);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        //Item Stuff
        maxStackSize = readValue("StackSize", maxStackSize, file);
        maxStackSize = readValue("MaxStackSize", maxStackSize, file);
        dropItemOnShoot = readValue("DropItemOnShoot", dropItemOnShoot, file);
        dropItemOnReload = readValue("DropItemOnReload", dropItemOnReload, file);
        dropItemOnHit = readValue("DropItemOnHit", dropItemOnHit, file);
        roundsPerItem = readValue("RoundsPerItem", roundsPerItem, file);
        numBullets = readValue("NumBullets", numBullets, file);

        // Physics
        bulletSpread = readValue("Accuracy", bulletSpread, file);
        bulletSpread = readValue("Spread", bulletSpread, file);
        fallSpeed = readValue("FallSpeed", fallSpeed, file);
        throwSpeed = readValue("ThrowSpeed", throwSpeed, file);
        throwSpeed = readValue("ShootSpeed", throwSpeed, file);
        hitBoxSize = readValue("HitBoxSize", fallSpeed, file);
        mass = readValue("Mass", mass, file);

        //Hit stuff
        damage.setDamage(readValue("Damage", damage.getDamage(), file));
        damage.setDamage(readValue("DamageVsEntity", damage.getDamage(), file));
        damage.setDamage(readValue("HitEntityDamage", damage.getDamage(), file));
        damage.setReadDamage(file.hasConfigLine("Damage") || file.hasConfigLine("DamageVsEntity") || file.hasConfigLine("HitEntityDamage"));
        damage.setDamageVsLiving(readValue("DamageVsLiving", damage.getDamageVsLiving(), file));
        damage.setReadDamageVsLiving(file.hasConfigLine("DamageVsLiving"));
        damage.setDamageVsPlayer(readValue("DamageVsPlayer", damage.getDamageVsPlayer(), file));
        damage.setDamageVsPlayer(readValue("DamageVsPlayers", damage.getDamageVsPlayer(), file));
        damage.setReadDamageVsPlayer(file.hasConfigLine("DamageVsPlayer") || file.hasConfigLine("DamageVsPlayers"));
        damage.setDamageVsVehicles(readValue("DamageVsVehicle", damage.getDamageVsVehicles(), file));
        damage.setDamageVsVehicles(readValue("DamageVsVehicles", damage.getDamageVsVehicles(), file));
        damage.setDamageVsVehicles(readValue("DamageVsDrivable", damage.getDamageVsVehicles(), file));
        damage.setDamageVsVehicles(readValue("DamageVsDrivables", damage.getDamageVsVehicles(), file));
        damage.setReadDamageVsVehicles(file.hasConfigLine("DamageVsVehicle") || file.hasConfigLine("DamageVsVehicles") || file.hasConfigLine("DamageVsDrivable") || file.hasConfigLine("DamageVsDrivables"));
        damage.setDamageVsPlanes(readValue("DamageVsPlane", damage.getDamageVsPlanes(), file));
        damage.setDamageVsPlanes(readValue("DamageVsPlanes", damage.getDamageVsPlanes(), file));
        damage.setReadDamageVsPlanes(file.hasConfigLine("DamageVsPlane") || file.hasConfigLine("DamageVsPlanes"));

        ignoreArmorProbability = readValue("IgnoreArmorProbability", ignoreArmorProbability, file);
        ignoreArmorDamageFactor = readValue("IgnoreArmorDamageFactor", ignoreArmorDamageFactor, file);
        breaksGlass = readValue("BreaksGlass", breaksGlass, file);
        bounciness = readValue("Bounciness", bounciness, file);
        hasLight = readValue("HasLight", hasLight, file);
        hasDynamicLight = readValue("HasDynamicLight", hasDynamicLight, file);

        // Detonation conditions etc
        fuse = readValue("Fuse", fuse, file);
        despawnTime = readValue("DespawnTime", despawnTime, file);
        explodeOnImpact = readValue("ExplodeOnImpact", explodeOnImpact, file);
        explodeOnImpact = readValue("DetonateOnImpact", explodeOnImpact, file);
        livingProximityTrigger = readValue("LivingProximityTrigger", livingProximityTrigger, file);
        driveableProximityTrigger = readValue("VehicleProximityTrigger", driveableProximityTrigger, file);
        damageToTriggerer = readValue("DamageToTriggerer", damageToTriggerer, file);
        primeDelay = readValue("PrimeDelay", primeDelay, file);
        primeDelay = readValue("TriggerDelay", primeDelay, file);

        //Detonation
        fireRadius = readValue("FireRadius", fireRadius, file);
        fireRadius = readValue("Fire", fireRadius, file);
        explosionRadius = readValue("ExplosionRadius", explosionRadius, file);
        explosionRadius = readValue("Explosion", explosionRadius, file);
        explosionPower = readValue("ExplosionPower", explosionPower, file);
        explosionBreaksBlocks = readValue("ExplosionBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionsBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionBreakBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionsBreakBlocks", explosionBreaksBlocks, file);

        explosionDamage.setDamage(readValue("ExplosionDamage", explosionDamage.getDamage(), file));
        explosionDamage.setDamage(readValue("ExplosionDamageVsEntity", explosionDamage.getDamage(), file));
        explosionDamage.setReadDamage(file.hasConfigLine("ExplosionDamage") || file.hasConfigLine("ExplosionDamageVsEntity"));
        explosionDamage.setDamageVsLiving(readValue("ExplosionDamageVsLiving", explosionDamage.getDamageVsLiving(), file));
        explosionDamage.setReadDamageVsLiving(file.hasConfigLine("ExplosionDamageVsLiving"));
        explosionDamage.setDamageVsPlayer(readValue("ExplosionDamageVsPlayer", explosionDamage.getDamageVsPlayer(), file));
        explosionDamage.setDamageVsPlayer(readValue("ExplosionDamageVsPlayers", explosionDamage.getDamageVsPlayer(), file));
        explosionDamage.setReadDamageVsPlayer(file.hasConfigLine("ExplosionDamageVsPlayer") || file.hasConfigLine("ExplosionDamageVsPlayers"));
        explosionDamage.setDamageVsVehicles(readValue("ExplosionDamageVsVehicle", explosionDamage.getDamageVsVehicles(), file));
        explosionDamage.setDamageVsVehicles(readValue("ExplosionDamageVsVehicles", explosionDamage.getDamageVsVehicles(), file));
        explosionDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDrivable", explosionDamage.getDamageVsVehicles(), file));
        explosionDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDrivables", explosionDamage.getDamageVsVehicles(), file));
        explosionDamage.setReadDamageVsVehicles(file.hasConfigLine("ExplosionDamageVsVehicle") || file.hasConfigLine("ExplosionDamageVsVehicles") || file.hasConfigLine("ExplosionDamageVsDrivable") || file.hasConfigLine("ExplosionDamageVsDrivables"));
        explosionDamage.setDamageVsPlanes(readValue("ExplosionDamageVsPlane", explosionDamage.getDamageVsPlanes(), file));
        explosionDamage.setDamageVsPlanes(readValue("ExplosionDamageVsPlanes", explosionDamage.getDamageVsPlanes(), file));
        explosionDamage.setReadDamageVsPlanes(file.hasConfigLine("ExplosionDamageVsPlane") || file.hasConfigLine("ExplosionDamageVsPlanes"));

        dropItemOnDetonate = readValue("DropItemOnDetonate", dropItemOnDetonate, file);
        detonateSound = readValue("DetonateSound", detonateSound, file);

        //Submunitions
        hasSubmunitions = readValue("HasSubmunitions", hasSubmunitions, file);
        submunition = readValue("Submunition", submunition, file);
        numSubmunitions = readValue("NumSubmunitions", numSubmunitions, file);
        subMunitionTimer = readValue("SubmunitionDelay", subMunitionTimer, file);
        submunitionSpread = readValue("SubmunitionSpread", submunitionSpread, file);
        destroyOnDeploySubmunition = readValue("DestroyOnDeploySubmunition", destroyOnDeploySubmunition, file);
        smokeParticleCount = readValue("FlareParticleCount", smokeParticleCount, file);
        debrisParticleCount = readValue("DebrisParticleCount", debrisParticleCount, file);

        //Particles
        trailParticles = readValue("TrailParticles", trailParticles, file);
        trailParticles = readValue("SmokeTrail", trailParticles, file);
        trailParticleType = readValue("TrailParticleType", trailParticleType, file);
        explodeParticles = readValue("NumExplodeParticles", explodeParticles, file);
        explodeParticleType = readValue("ExplodeParticles", explodeParticleType, file);
        smokeTime = readValue("SmokeTime", smokeTime, file);
        smokeParticleType = readValue("SmokeParticles", smokeParticleType, file);
        smokeRadius = readValue("SmokeRadius", smokeRadius, file);
        addEffects("SmokeEffect", smokeEffects, file, false, false);

        damage.calculate();
        explosionDamage.calculate();
    }

    public float getDamageForDisplay(GunType gunType, ItemStack gunStack, @Nullable Class<? extends Entity> entityClass)
    {
        if (useNewDamageSystem())
            return (float) (ModCommonConfigs.newDamageSystemReference.get() * 0.001 * Math.sqrt(mass) * gunType.getBulletSpeed(gunStack) * 20.0);
        else
            return getDamage().getDamageAgainstEntityClass(entityClass) * gunType.getDamage(gunStack);
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
                list.addAll(registeredAmmoList.values().stream().map(shootableTypeMap -> shootableTypeMap.get(shortname)).filter(Objects::nonNull).toList());
            }
        }
        return list;
    }

    public static Optional<ShootableType> getAmmoType(String shortname, IContentProvider contentPack) {
        return getAmmoTypes(Set.of(shortname), contentPack).stream().findFirst();
    }
}
