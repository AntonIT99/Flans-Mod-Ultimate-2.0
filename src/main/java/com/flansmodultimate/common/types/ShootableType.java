package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.FlanExplosion;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.hasValueForConfigField;
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

    public enum EnumFragType
    {
        DEFAULT(0.0f, 0.0f, 0.0f),
        /** Thin casing / offensive or concussion style (e.g., Stielhandgranate 24). */
        LOW_FRAG(22.0f, 6.0f, 0.9f),
        /** Typical fragmentation grenade (e.g., Mills bomb, Mk 2, many “standard” frags). */
        STD_FRAG(35.0f, 10.0f, 2.2f),
        /** Defensive / prefragmented / scored casing (large danger radius). */
        HIGH_FRAG(65.0f, 14.0f, 4.5f),
        /** Shrapnel-packed / IED-style (nails, ball bearings, pipe bomb). */
        IED_SHRAPNEL(90.0f, 18.0f, 7.0f),
        /** Artillery / mortar / HE rocket type casing fragments. */
        HE_SHELL(50.0f, 13.0f, 3.8f),
        /** General-purpose aerial bomb fragments (blast dominates, fragments still dangerous). */
        GP_BOMB(40.0f, 13.0f, 2.6f),
        /** Thick-case / penetrator / “earthquake” style (less long-range frag emphasis). */
        THICK_CASE(28.0f, 10.0f, 1.4f),
        /** Airburst / proximity-fused anti-personnel (optimized fragment distribution). */
        AIRBURST_AP(80.0f, 13.0f, 5.5f);

        public final float kFragRadius;
        public final float kFragDamage;
        public final float fragIntensity;

        EnumFragType(float kFragRadius, float kFragDamage, float fragIntensity)
        {
            this.kFragRadius = kFragRadius;
            this.kFragDamage = kFragDamage;
            this.fragIntensity = fragIntensity;
        }
    }

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
    /** Explosive mass in kg TNT equivalent. Used for the new damage system. Will be ignored when 0 */
    @Getter
    protected float explosiveMass;
    /** The radius in which to spread fire */
    @Getter
    protected float fireRadius;
    /** The explosion radius upon detonation */
    protected float explosionRadius;
    /** The explosion blast radius upon detonation */
    protected float blastRadius;
    /** The explosion frag radius upon detonation */
    @Getter
    protected float fragRadius;
    /** Power of explosion. Multiplier, 1 = vanilla behaviour */
    protected float explosionPower = 1F;
    /** Whether the explosion can destroy blocks */
    @Getter
    protected boolean explosionBreaksBlocks = true;
    /** Explosion blast damage vs various classes of entities */
    protected DamageStats explosionBlastDamage = new DamageStats();
    /** Explosion frag damage vs various classes of entities */
    @Getter
    protected DamageStats explosionFragDamage = new DamageStats();
    @Getter
    protected float fragIntensity;
    protected EnumFragType fragType = EnumFragType.DEFAULT;
    /** The name of the item to drop upon detonating */
    @Getter
    protected String dropItemOnDetonate;
    /** Sound to play upon detonation */
    @Getter
    protected String detonateSound = StringUtils.EMPTY;

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
    /** Particles given off in the detonation */
    @Getter
    protected int explodeParticles;
    @Getter
    protected String explodeParticleType = "largesmoke";

    public boolean useKineticDamageSystem()
    {
        return mass > 0F;
    }

    public boolean useTNTEquivalentDamageSystem()
    {
        return explosiveMass > 0F;
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
        if (hasValueForConfigField("Dispersion", file))
            bulletSpread = readValue("Dispersion", 0F, file) * Mth.DEG_TO_RAD / ShootingHelper.ANGULAR_SPREAD_FACTOR;
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
        explosionBreaksBlocks = readValue("ExplosionBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionsBreaksBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionBreakBlocks", explosionBreaksBlocks, file);
        explosionBreaksBlocks = readValue("ExplosionsBreakBlocks", explosionBreaksBlocks, file);

        explosiveMass = readValue("ExplosiveMass", explosiveMass, file);
        explosionRadius = readValue("ExplosionRadius", explosionRadius, file);
        explosionRadius = readValue("Explosion", explosionRadius, file);
        explosionPower = readValue("ExplosionPower", explosionPower, file);
        explosionBlastDamage.setDamage(readValue("BlastDamage", explosionBlastDamage.getDamage(), file));
        explosionBlastDamage.setDamage(readValue("ExplosionDamage", explosionBlastDamage.getDamage(), file));
        explosionBlastDamage.setDamage(readValue("ExplosionDamageVsEntity", explosionBlastDamage.getDamage(), file));
        explosionBlastDamage.setReadDamage(file.hasConfigLine("ExplosionDamage") || file.hasConfigLine("ExplosionDamageVsEntity"));
        explosionBlastDamage.setDamageVsLiving(readValue("ExplosionDamageVsLiving", explosionBlastDamage.getDamageVsLiving(), file));
        explosionBlastDamage.setReadDamageVsLiving(file.hasConfigLine("ExplosionDamageVsLiving"));
        explosionBlastDamage.setDamageVsPlayer(readValue("ExplosionDamageVsPlayer", explosionBlastDamage.getDamageVsPlayer(), file));
        explosionBlastDamage.setDamageVsPlayer(readValue("ExplosionDamageVsPlayers", explosionBlastDamage.getDamageVsPlayer(), file));
        explosionBlastDamage.setReadDamageVsPlayer(file.hasConfigLine("ExplosionDamageVsPlayer") || file.hasConfigLine("ExplosionDamageVsPlayers"));
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsVehicle", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsVehicles", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDrivable", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDrivables", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setReadDamageVsVehicles(file.hasConfigLine("ExplosionDamageVsVehicle") || file.hasConfigLine("ExplosionDamageVsVehicles") || file.hasConfigLine("ExplosionDamageVsDrivable") || file.hasConfigLine("ExplosionDamageVsDrivables"));
        explosionBlastDamage.setDamageVsPlanes(readValue("ExplosionDamageVsPlane", explosionBlastDamage.getDamageVsPlanes(), file));
        explosionBlastDamage.setDamageVsPlanes(readValue("ExplosionDamageVsPlanes", explosionBlastDamage.getDamageVsPlanes(), file));
        explosionBlastDamage.setReadDamageVsPlanes(file.hasConfigLine("ExplosionDamageVsPlane") || file.hasConfigLine("ExplosionDamageVsPlanes"));

        blastRadius = readValue("BlastRadius", blastRadius, file);
        fragRadius = readValue("BlastRadius", fragRadius, file);
        fragIntensity = readValue("FragIntensity", fragIntensity, file);
        explosionFragDamage.setDamage(readValue("FragDamage", explosionFragDamage.getDamage(), file));
        explosionFragDamage.setDamage(readValue("FragDamageVsEntity", explosionFragDamage.getDamage(), file));
        explosionFragDamage.setReadDamage(file.hasConfigLine("FragDamage") || file.hasConfigLine("FragDamageVsEntity"));
        explosionFragDamage.setDamageVsLiving(readValue("FragDamageVsLiving", explosionFragDamage.getDamageVsLiving(), file));
        explosionFragDamage.setReadDamageVsLiving(file.hasConfigLine("FragDamageVsLiving"));
        explosionFragDamage.setDamageVsPlayer(readValue("FragDamageVsPlayer", explosionFragDamage.getDamageVsPlayer(), file));
        explosionFragDamage.setDamageVsPlayer(readValue("FragDamageVsPlayers", explosionFragDamage.getDamageVsPlayer(), file));
        explosionFragDamage.setReadDamageVsPlayer(file.hasConfigLine("FragDamageVsPlayer") || file.hasConfigLine("FragDamageVsPlayers"));
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsVehicle", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsVehicles", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsDrivable", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsDrivables", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setReadDamageVsVehicles(file.hasConfigLine("FragDamageVsVehicle") || file.hasConfigLine("FragDamageVsVehicles") || file.hasConfigLine("FragDamageVsDrivable") || file.hasConfigLine("FragDamageVsDrivables"));
        explosionFragDamage.setDamageVsPlanes(readValue("FragDamageVsPlane", explosionFragDamage.getDamageVsPlanes(), file));
        explosionFragDamage.setDamageVsPlanes(readValue("FragDamageVsPlanes", explosionFragDamage.getDamageVsPlanes(), file));
        explosionFragDamage.setReadDamageVsPlanes(file.hasConfigLine("FragDamageVsPlane") || file.hasConfigLine("FragDamageVsPlanes"));

        fragType = readValue("FragType", fragType, EnumFragType.class, file);
        if (fragType != EnumFragType.DEFAULT)
        {
            if (useTNTEquivalentDamageSystem())
            {
                explosionFragDamage = new DamageStats();
                explosionFragDamage.setDamage((float) (fragType.kFragDamage * Math.cbrt(explosiveMass)));
                fragRadius = (float) (fragType.kFragRadius * Math.cbrt(explosiveMass));
            }
            fragIntensity = fragType.fragIntensity;
        }

        damage.calculate();
        explosionFragDamage.calculate();
        explosionBlastDamage.scale(8F * explosionRadius + 1F);
        explosionBlastDamage.calculate();

        dropItemOnDetonate = readValue("DropItemOnDetonate", dropItemOnDetonate, file);
        detonateSound = readValue("DetonateSound", detonateSound, file);

        //Particles
        smokeParticleCount = readValue("FlareParticleCount", smokeParticleCount, file);
        debrisParticleCount = readValue("DebrisParticleCount", debrisParticleCount, file);
        trailParticles = readValue("TrailParticles", trailParticles, file);
        trailParticles = readValue("SmokeTrail", trailParticles, file);
        trailParticleType = readValue("TrailParticleType", trailParticleType, file);
        explodeParticles = readValue("NumExplodeParticles", explodeParticles, file);
        explodeParticleType = readValue("ExplodeParticles", explodeParticleType, file);
    }

    public DamageStats getExplosionBlastDamage()
    {
        if (useTNTEquivalentDamageSystem())
        {
            DamageStats newExplosionDamage = new DamageStats();
            newExplosionDamage.setDamage((float) (ModCommonConfig.get().newDamageSystemExplosiveDamageReference() * Math.cbrt(explosiveMass)));
            newExplosionDamage.calculate();
            return newExplosionDamage;
        }
        return explosionBlastDamage;
    }

    public float getExplosionRadius()
    {
        if (useTNTEquivalentDamageSystem())
        {
            return (float) (ModCommonConfig.get().newDamageSystemExplosiveRadiusReference() * Math.cbrt(explosiveMass));
        }
        return explosionRadius;
    }

    public float getBlastRadius()
    {
        if (useTNTEquivalentDamageSystem())
        {
            return ModCommonConfig.get().newDamageSystemBlastToExplosionRadiusRatio() * getExplosionRadius();
        }
        return explosionRadius;
    }

    public float getExplosionPower()
    {
        if (useTNTEquivalentDamageSystem())
        {
            return (float) (ModCommonConfig.get().newDamageSystemExplosivePowerReference() * Math.cbrt(explosiveMass));
        }
        return explosionPower;
    }

    public FlanExplosion.Stats getExplosionStats()
    {
        return new FlanExplosion.Stats(getExplosionRadius(), getExplosionPower(), getBlastRadius(), getExplosionBlastDamage(), fragRadius, fragIntensity, explosionFragDamage);
    }

    public float getDamageForDisplay(GunType gunType, ItemStack gunStack, @Nullable Class<? extends Entity> entityClass)
    {
        if (useKineticDamageSystem())
            return (float) (ModCommonConfig.get().newDamageSystemDamageReference() * 0.001 * Math.sqrt(mass) * gunType.getBulletSpeed(gunStack) * 20.0);
        else
            return getDamage().getDamageAgainstEntityClass(entityClass) * gunType.getDamage(gunStack);
    }

    public float getDispersionForDisplay() {
        return Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * bulletSpread;
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

    public static Optional<ShootableType> getAmmoType(String shortname, IContentProvider contentPack)
    {
        return getAmmoTypes(Set.of(shortname), contentPack).stream().findFirst();
    }
}
