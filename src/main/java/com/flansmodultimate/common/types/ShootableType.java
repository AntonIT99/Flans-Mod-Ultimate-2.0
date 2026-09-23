package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.explosions.ExplosionScaling;
import com.flansmodultimate.common.explosions.FlanExplosion;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
    /** Conversion factor between the gram and kilogram scales the mass parameters are authored at. */
    public static final float GRAMS_PER_KILOGRAM = 1000F;
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

    /**
     * {@code kFragRadius} is the fragmentation radius of a 1 kg charge in blocks, on the same
     * footing as {@code newDamageSystemBlastRadiusReference} so the two envelopes are directly
     * comparable. HE_SHELL sits exactly on the blast reference, because the artillery rounds the
     * blast curve was fitted to are the ones whose quoted casualty radius includes their
     * fragments; thinner casings reach less far and shrapnel-packed ones further.
     */
    public enum EnumFragType
    {
        DEFAULT(0.0f, 0.0f, 0.0f),
        /** Thin casing / offensive or concussion style (e.g., Stielhandgranate 24). */
        LOW_FRAG(9.5f, 6.0f, 0.9f),
        /** Typical fragmentation grenade (e.g., Mills bomb, Mk 2, many “standard” frags). */
        STD_FRAG(15.5f, 10.0f, 2.2f),
        /** Defensive sleeve / fragmentation jacket fitted to a grenade body. */
        SLEEVE_FRAG(18.5f, 11.0f, 2.8f),
        /** Defensive / prefragmented / scored casing with a larger casualty radius. */
        HIGH_FRAG(20.0f, 12.0f, 3.0f),
        /** Shrapnel-packed / IED-style (nails, ball bearings, pipe bomb). */
        IED_SHRAPNEL(26.5f, 14.0f, 4.0f),
        /** Artillery / mortar / HE rocket type casing fragments. */
        HE_SHELL(22.0f, 13.0f, 3.8f),
        /** General-purpose aerial bomb fragments (blast dominates, fragments still dangerous). */
        GP_BOMB(17.5f, 13.0f, 2.6f),
        /** Thick-case / penetrator / “earthquake” style (less long-range frag emphasis). */
        THICK_CASE(12.5f, 10.0f, 1.4f),
        /** Airburst / proximity-fused anti-personnel (optimized fragment distribution). */
        AIRBURST_AP(24.0f, 12.0f, 3.2f);

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

    /*
     * What loading this ammunition does to the weapon firing it. These scale the
     * weapon's own numbers the way an attachment does, and stack on top of any
     * attachments, so a heavy round can be authored as harder hitting but slower
     * to reload and less controllable. A weapon that fires several kinds of
     * ammunition therefore changes character with what is loaded.
     *
     * BulletSpeedMultiplier is not here: BulletType carries its own, applied to
     * the resolved muzzle velocity rather than to the weapon.
     */
    /** Scales the damage the weapon deals with this ammunition loaded. */
    @Getter
    protected float damageMultiplier = 1F;
    /** Scales the weapon's bullet spread with this ammunition loaded. */
    @Getter
    protected float spreadMultiplier = 1F;
    /** Scales the weapon's recoil with this ammunition loaded. */
    @Getter
    protected float recoilMultiplier = 1F;
    /** Scales how long the weapon takes to reload with this ammunition. */
    @Getter
    protected float reloadTimeMultiplier = 1F;

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
    /**
     * Mass of the projectile in g. Authored as {@code Mass} (grams) or {@code MassKg} (kilograms).
     * Used for the new damage system. Will be ignored when 0
     */
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
    /**
     * Explosive mass in kg TNT equivalent. Authored as {@code ExplosiveMassTNTg} (grams) or
     * {@code ExplosiveMassTNTKg} (kilograms). Used for the new damage system. Will be ignored when 0
     */
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
    /** Time in ticks that the projectile remains as a smoke source after detonation. */
    @Getter
    protected int smokeTime;
    /** Particles emitted by the smoke source. */
    @Getter
    protected String smokeParticleType = FlanParticles.EXPLODE;
    /** Number of smoke particles emitted per tick. */
    protected int smokeParticlesCount;
    protected boolean readSmokeParticlesCount;
    /** Effects applied to unprotected living entities inside the smoke. */
    @Getter
    protected final List<MobEffectInstance> smokeEffects = new ArrayList<>();
    /** Radius in blocks in which smoke effects are applied. */
    @Getter
    protected float smokeRadius = 5F;

    @Override
    public void onItemRegistration(String registeredItemId)
    {
        super.onItemRegistration(registeredItemId);
        registeredAmmoList.putIfAbsent(contentPack, new HashMap<>());
        registeredAmmoList.get(contentPack).put(originalShortName, this);
    }

    /** Reads a weapon modifier, keeping the current factor when the authored one is not usable. */
    private float readPositiveMultiplier(String key, float current, TypeFile file)
    {
        float value = readValue(key, current, file);
        return Float.isFinite(value) && value > 0F ? value : current;
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
        maxStackSize = Math.max(maxStackSize, recipeOutput);
        dropItemOnShoot = readValue("DropItemOnShoot", dropItemOnShoot, file);
        dropItemOnReload = readValue("DropItemOnReload", dropItemOnReload, file);
        dropItemOnHit = readValue("DropItemOnHit", dropItemOnHit, file);
        roundsPerItem = readValue("RoundsPerItem", roundsPerItem, file);
        numBullets = readValue("NumBullets", numBullets, file);

        // Weapon modifiers. A zero or negative factor is meaningless here and
        // would silently disable the stat, so only positive values are taken.
        damageMultiplier = readPositiveMultiplier("DamageMultiplier", damageMultiplier, file);
        spreadMultiplier = readPositiveMultiplier("SpreadMultiplier", spreadMultiplier, file);
        recoilMultiplier = readPositiveMultiplier("RecoilMultiplier", recoilMultiplier, file);
        reloadTimeMultiplier = readPositiveMultiplier("ReloadTimeMultiplier", reloadTimeMultiplier, file);

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
        // MassKg is the same stat authored at a kilogram scale, so it is converted into the grams
        // everything downstream already expects.
        if (hasValueForConfigField("MassKg", file))
            mass = readValue("MassKg", 0F, file) * GRAMS_PER_KILOGRAM;
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
        damage.setDamageVsVehicles(readValue("DamageVsDriveable", damage.getDamageVsVehicles(), file));
        damage.setDamageVsVehicles(readValue("DamageVsDriveables", damage.getDamageVsVehicles(), file));
        damage.setReadDamageVsVehicles(file.hasConfigLine("DamageVsVehicle") || file.hasConfigLine("DamageVsVehicles") || file.hasConfigLine("DamageVsDriveable") || file.hasConfigLine("DamageVsDriveables"));
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

        // Stored in kg TNT equivalent, but authored at whichever scale suits the charge: the key
        // spells out the unit so a rifle grenade and a bomb can both be written without leading zeroes.
        if (hasValueForConfigField("ExplosiveMassTNTg", file))
            explosiveMass = readValue("ExplosiveMassTNTg", 0F, file) / GRAMS_PER_KILOGRAM;
        if (hasValueForConfigField("ExplosiveMassTNTKg", file))
            explosiveMass = readValue("ExplosiveMassTNTKg", 0F, file);
        if (!Float.isFinite(explosiveMass) || explosiveMass < 0F)
        {
            logError("ExplosiveMassTNTg/ExplosiveMassTNTKg must be a finite non-negative TNT equivalent; ignoring it", file);
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
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDriveable", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setDamageVsVehicles(readValue("ExplosionDamageVsDriveables", explosionBlastDamage.getDamageVsVehicles(), file));
        explosionBlastDamage.setReadDamageVsVehicles(file.hasConfigLine("ExplosionDamageVsVehicle") || file.hasConfigLine("ExplosionDamageVsVehicles") || file.hasConfigLine("ExplosionDamageVsDriveable") || file.hasConfigLine("ExplosionDamageVsDriveables"));
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
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsDriveable", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setDamageVsVehicles(readValue("FragDamageVsDriveables", explosionFragDamage.getDamageVsVehicles(), file));
        explosionFragDamage.setReadDamageVsVehicles(file.hasConfigLine("FragDamageVsVehicle") || file.hasConfigLine("FragDamageVsVehicles") || file.hasConfigLine("FragDamageVsDriveable") || file.hasConfigLine("FragDamageVsDriveables"));
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
                fragRadius = ExplosionScaling.fragRadius(fragType.kFragRadius, explosiveMass);
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
        explodeParticles = readValue("ExplodeParticles", explodeParticles, file);
        explodeParticleType = readValue("ExplodeParticleType", explodeParticleType, file);
        smokeTime = readValue("SmokeTime", smokeTime, file);
        smokeParticleType = readValue("SmokeParticles", smokeParticleType, file);
        smokeParticleType = readValue("SmokeParticleType", smokeParticleType, file);
        smokeParticlesCount = readValue("SmokeParticlesCount", smokeParticlesCount, file);
        readSmokeParticlesCount = hasValueForConfigField("SmokeParticlesCount", file);
        smokeRadius = readValue("SmokeRadius", smokeRadius, file);
        addEffects("SmokeEffect", smokeEffects, file, false, false);
    }

    public int getSmokeParticlesCount()
    {
        return readSmokeParticlesCount ? smokeParticlesCount : ModCommonConfig.smokeParticlesCount();
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
        float radius = useNewExplosionSystem()
            ? ExplosionScaling.craterRadius(ModCommonConfig.get().newDamageSystemExplosiveRadiusReference(), getExplosiveMass())
            : explosionRadius;

        // Mirrors the cap FlanExplosion.Stats applies to every explosion it simulates, so a
        // tooltip never advertises a crater bigger than what the server will actually carve.
        float maxRadius = (float) ModCommonConfig.maxExplosionRadius();
        if (Float.isFinite(maxRadius) && maxRadius > 0F)
            radius = Math.min(radius, maxRadius);
        return radius;
    }

    public float getBlastRadius()
    {
        if (useNewExplosionSystem())
        {
            return ExplosionScaling.blastRadius(ModCommonConfig.get().newDamageSystemBlastRadiusReference(), getExplosiveMass());
        }
        return blastRadius;
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

    /**
     * Every registered ammunition type, ordered by content pack name and then by shortname.
     *
     * <p>The registry behind this is a hash map, so an unordered walk would pick a different
     * type from one load to the next. Callers that fall back to "any ammunition of this kind",
     * as rearming a driveable whose definition declares none has to, need that pick to be the
     * same on every load.</p>
     */
    public static List<ShootableType> registeredAmmoTypes()
    {
        List<ShootableType> all = new ArrayList<>();
        registeredAmmoList.values().forEach(byShortname -> all.addAll(byShortname.values()));
        all.sort(Comparator.comparing(ShootableType::contentPackName).thenComparing(ShootableType::getOriginalShortName));
        return all;
    }

    private static String contentPackName(ShootableType type)
    {
        return type.getContentPack() == null ? StringUtils.EMPTY : StringUtils.defaultString(type.getContentPack().getName());
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
