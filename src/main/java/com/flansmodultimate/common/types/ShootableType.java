package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.FlanExplosion;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ShootableType extends InfoType
{
    public static final int EXPLODE_PARTICLES_RANGE = 256;
    public static final double FALL_SPEED_COEFFICIENT = (9.81 / 400.0);
    public static final float AIR_DEFAULT_DRAG = 0.99F;
    public static final float WATER_DEFAULT_DRAG = 0.8F;
    public static final float LAVA_DEFAULT_DRAG = 0.6F;

    private static final Map<IContentProvider, Map<String, ShootableType>> registeredAmmoList = new HashMap<>();

    @Getter
    private static final Map<String, List<ShootableType>> additionalAmmoMapping = new HashMap<>();

    /**
     * Ammo groups declared through "AddToAmmoGroup <group name>", keyed by the case insensitive group name.
     * A group is declared the first time a shootable type refers to it; every later reference to the same
     * name simply adds that shootable type to the existing group.
     */
    private static final Map<String, AmmoGroup> ammoGroups = new LinkedHashMap<>();
    /**
     * Incremented whenever a shootable type joins a group, so consumers caching resolved ammo lists can tell
     * that a group grew after their cache was built.
     */
    @Getter
    private static int ammoGroupRevision;

    /** A named set of ammo items that guns, AA guns and driveables pull in with "UseAmmoGroup <group name>" */
    public static final class AmmoGroup
    {
        /** The group name as written on the config line that declared it */
        @Getter
        private final String name;
        private final List<ShootableType> members = new ArrayList<>();

        private AmmoGroup(String name)
        {
            this.name = name;
        }

        @Unmodifiable
        public List<ShootableType> getMembers()
        {
            return List.copyOf(members);
        }
    }

    public enum EnumFragType
    {
        DEFAULT(0.0f, 0.0f, 0.0f),
        /** Thin casing / offensive or concussion style (e.g., Stielhandgranate 24). */
        LOW_FRAG(22.0f, 6.0f, 0.9f),
        /** Typical fragmentation grenade (e.g., Mills bomb, Mk 2, many “standard” frags). */
        STD_FRAG(35.0f, 10.0f, 2.2f),
        /** Defensive sleeve / fragmentation jacket fitted to a grenade body. */
        SLEEVE_FRAG(42.0f, 11.0f, 2.8f),
        /** Defensive / prefragmented / scored casing with a larger casualty radius. */
        HIGH_FRAG(45.0f, 12.0f, 3.0f),
        /** Shrapnel-packed / IED-style (nails, ball bearings, pipe bomb). */
        IED_SHRAPNEL(60.0f, 14.0f, 4.0f),
        /** Artillery / mortar / HE rocket type casing fragments. */
        HE_SHELL(50.0f, 13.0f, 3.8f),
        /** General-purpose aerial bomb fragments (blast dominates, fragments still dangerous). */
        GP_BOMB(40.0f, 13.0f, 2.6f),
        /** Thick-case / penetrator / “earthquake” style (less long-range frag emphasis). */
        THICK_CASE(28.0f, 10.0f, 1.4f),
        /** Airburst / proximity-fused anti-personnel (optimized fragment distribution). */
        AIRBURST_AP(55.0f, 12.0f, 3.2f);

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
    /** Number of bullets to fire per shot if allowNumBulletsByBulletType = true. 0 = defer to gun's NumBullets */
    @Getter
    protected int numBullets = 0;
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
    protected String trailParticleType = FlanParticles.SMOKE;
    @Getter
    protected int smokeParticleCount;
    @Getter
    protected int debrisParticleCount;
    /** Particles given off in the detonation */
    @Getter
    protected int explodeParticles;
    @Getter
    protected String explodeParticleType = FlanParticles.LARGE_SMOKE;

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

        readLines("AddAmmoFor", file).ifPresent(lines -> lines.forEach(type -> additionalAmmoMapping.computeIfAbsent(type, key -> new ArrayList<>()).add(this)));
        readValuesInLines("AddToAmmoGroup", file).ifPresent(lines -> lines.forEach(values -> joinAmmoGroup(values, file)));

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
        hitBoxSize = readValue("HitBoxSize", hitBoxSize, file);
        mass = readValue("Mass", mass, file);
        if (!Float.isFinite(mass) || mass < 0F)
        {
            logError("Mass must be a finite non-negative value in grams; kinetic damage will use the fixed fallback", file);
            mass = 0F;
        }

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
        if (!Float.isFinite(explosiveMass) || explosiveMass < 0F)
        {
            logError("ExplosiveMass must be a finite non-negative value in kg TNT equivalent; ignoring it", file);
            explosiveMass = 0F;
        }
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
        fragRadius = readValue("FragRadius", fragRadius, file);
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
            if (explosiveMass > 0F)
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

    public boolean useKineticDamageSystem()
    {
        return mass > 0F;
    }

    public boolean useNewExplosionSystem()
    {
        return explosiveMass > 0F;
    }

    public DamageStats getExplosionBlastDamage()
    {
        if (useNewExplosionSystem())
        {
            DamageStats newExplosionDamage = new DamageStats();
            newExplosionDamage.setDamage((float) (ModCommonConfig.get().newDamageSystemExplosiveDamageReference() * Math.cbrt(getExplosiveMass())));
            newExplosionDamage.calculate();
            return newExplosionDamage;
        }
        return explosionBlastDamage;
    }

    public float getExplosionRadius()
    {
        if (useNewExplosionSystem())
        {
            return (float) (ModCommonConfig.get().newDamageSystemExplosiveRadiusReference() * Math.cbrt(getExplosiveMass()));
        }
        return explosionRadius;
    }

    public float getBlastRadius()
    {
        if (useNewExplosionSystem())
        {
            return (float) (ModCommonConfig.get().newDamageSystemBlastRadiusReference() * Math.cbrt(getExplosiveMass()));
        }
        return explosionRadius;
    }

    public float getExplosionPower()
    {
        if (useNewExplosionSystem())
        {
            return (float) (ModCommonConfig.get().newDamageSystemExplosivePowerReference() * Math.cbrt(getExplosiveMass()));
        }
        return explosionPower;
    }

    @SuppressWarnings("java:S1172")
    public FlanExplosion.Stats getExplosionStats(@Nullable Entity explosiveEntity)
    {
        return new FlanExplosion.Stats(getExplosionRadius(), getExplosionPower(), getBlastRadius(),
            getExplosionBlastDamage(), fragRadius, fragIntensity, explosionFragDamage,
            useNewExplosionSystem() ? getExplosiveMass() : 0F);
    }

    public float getDispersionForDisplay() {
        return Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * bulletSpread;
    }

    /**
     * Registers this shootable type in the ammo group named by an "AddToAmmoGroup" line, declaring the group
     * if this is the first type to mention it.
     */
    private void joinAmmoGroup(String[] values, TypeFile file)
    {
        String groupName = readAmmoGroupName("AddToAmmoGroup", values, file);
        if (groupName == null)
            return;

        AmmoGroup group = ammoGroups.computeIfAbsent(normalizeAmmoGroupName(groupName), key -> new AmmoGroup(groupName));
        if (!group.members.contains(this))
        {
            group.members.add(this);
            ammoGroupRevision++;
        }
    }

    /**
     * Reads the "UseAmmoGroup" lines of a gun, AA gun or driveable into the given set. Group names may contain
     * spaces, so the whole value of a line is kept as a single name.
     */
    public static void readAmmoGroups(TypeFile file, Set<String> groupNames)
    {
        readValuesInLines("UseAmmoGroup", file).ifPresent(lines -> lines.forEach(values -> {
            String groupName = readAmmoGroupName("UseAmmoGroup", values, file);
            if (groupName != null)
                groupNames.add(groupName);
        }));
    }

    @Nullable
    private static String readAmmoGroupName(String key, String[] values, TypeFile file)
    {
        String groupName = String.join(StringUtils.SPACE, values);
        if (groupName.isBlank())
        {
            logError(key + " is missing a group name", file);
            return null;
        }
        return groupName;
    }

    public static String normalizeAmmoGroupName(String groupName)
    {
        return groupName.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static AmmoGroup getAmmoGroup(String groupName)
    {
        return ammoGroups.get(normalizeAmmoGroupName(groupName));
    }

    /**
     * Resolves every ammo item belonging to the given groups, in declaration order and without duplicates.
     * Unknown group names resolve to nothing here; they are reported once by {@link #validateAmmoGroups}
     * because ammo lists are resolved lazily and repeatedly at runtime.
     */
    public static List<ShootableType> findAmmoTypesInGroups(Collection<String> groupNames)
    {
        Set<ShootableType> ammoInGroups = new LinkedHashSet<>();
        for (String groupName : groupNames)
        {
            AmmoGroup group = getAmmoGroup(groupName);
            if (group != null)
                ammoInGroups.addAll(group.members);
        }
        return new ArrayList<>(ammoInGroups);
    }

    /**
     * Reports "UseAmmoGroup" references that no ammo item ever joined. Must run once every content pack has
     * been read, since a group may be declared by a pack loaded after the one using it.
     */
    public static void validateAmmoGroups(InfoType source, Collection<String> groupNames)
    {
        for (String groupName : groupNames)
        {
            if (getAmmoGroup(groupName) == null)
                FlansMod.log.warn("UseAmmoGroup refers to unknown ammo group '{}' in {}", groupName, source);
        }
    }

    public static List<ShootableType> findAmmoTypes(Set<String> shortnames, IContentProvider contentPack)
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

    public static Optional<ShootableType> findAmmoType(String shortname, IContentProvider contentPack)
    {
        return findAmmoTypes(Set.of(shortname), contentPack).stream().findFirst();
    }
}
