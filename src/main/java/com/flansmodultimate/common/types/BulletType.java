package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanExplosion;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor
public class BulletType extends ShootableType
{
    public static final float DEFAULT_BULLET_SPEED = 3F;
    public static final float DEFAULT_PENETRATING_POWER = 0.7F;

    public record RoundStats(float mass, float explosiveMass, float bulletSpeed, float penetrationAt100m) {}

    public record RoundEntry(String name, int count, RoundStats stats) {}

    @Getter
    protected final List<RoundEntry> period = new ArrayList<>();
    protected int periodLength;
    /** Override the bullet speed of the gun if > 0 */
    protected float bulletSpeed;
    /** Penetration @ 0° Angle of Attack (mm) at 100m */
    @Getter
    protected float penetrationAt100m;
    @Getter
    protected float speedMultiplier = 1F;
    /** The number of flak particles to spawn upon exploding */
    @Getter
    protected int flak;
    /** The type of flak particles to spawn */
    @Getter
    protected String flakParticles = FlanParticles.LARGE_SMOKE;

    /** If true then this bullet will burn entites it hits */
    @Getter
    protected boolean setEntitiesOnFire;

    /** Exclusively for driveable usage. Replaces old isBomb and isShell booleans with something more flexible */
    @Getter
    protected EnumWeaponType weaponType = EnumWeaponType.GUN;

    @Getter
    protected String hitSound;
    @Getter
    protected float hitSoundRange = 64F;
    @Getter
    protected boolean hitSoundEnable;
    @Getter
    protected boolean entityHitSoundEnable;

    @Getter
    protected boolean penetrates = true;
    /** Authored penetrating power, superseded by the kinetic derivation when the round declares a mass */
    protected float penetratingPower = 1F;
    /** In % of penetration to remove per tick. */
    @Getter
    protected float penetrationDecay;
    protected float blockPenetrationModifier = -1F;

    /**
     * How much the loss of penetration power affects the damage of the bullet. 0 = damage not affected by that kind of penetration,
     * 1 = damage is fully affected by bullet penetration of that kind
     */
    @Getter
    protected float playerPenetrationEffectOnDamage;
    @Getter
    protected float entityPenetrationEffectOnDamage;
    @Getter
    protected float blockPenetrationEffectOnDamage;
    @Getter
    protected float penetrationDecayEffectOnDamage;

    /** Knocback modifier. less gives less kb, more gives more kb, 1 = normal kb. */
    @Getter
    protected float knockbackModifier;
    /** Lock on variables. If true, then the bullet will search for a target at the moment it is fired */
    @Getter
    protected boolean lockOnToPlanes;
    @Getter
    protected boolean lockOnToVehicles;
    @Getter
    protected boolean lockOnToMechas;
    @Getter
    protected boolean lockOnToPlayers;
    @Getter
    protected boolean lockOnToLivings;
    /** Lock on maximum angle for finding a target */
    @Getter
    protected float maxLockOnAngle = 45F;
    /** Lock on force that pulls the bullet towards its prey. 1 is 10G */
    @Getter
    protected float lockOnForce = 1F;
    @Getter
    protected String trailTexture = StringUtils.EMPTY;
    @Getter
    protected int maxDegreeOfMissile = 20;
    @Getter
    protected int tickStartHoming = 5;
    @Getter
    protected boolean enableSACLOS;
    @Getter
    protected int maxDegreeOfSACLOS = 5;
    @Getter
    protected int maxRangeOfMissile = 256;
    @Getter
    protected boolean manualGuidance;

    @Getter
    protected List<MobEffectInstance> hitEffects = new ArrayList<>();

    @Getter
    protected float dragInAir = AIR_DEFAULT_DRAG;
    @Getter
    protected float dragInWater = WATER_DEFAULT_DRAG;
    @Getter
    protected boolean canSpotEntityDriveable;
    @Getter
    protected int maxRange = -1;
    @Getter
    protected boolean shootForSettingPos;
    @Getter
    protected int shootForSettingPosHeight = 100;
    @Getter
    protected boolean isDoTopAttack;

    //Other stuff
    @Getter
    protected boolean vls;
    @Getter
    protected int vlsTime;
    @Getter
    protected boolean fixedDirection;
    @Getter
    protected float turnRadius = 3;
    @Getter
    protected String boostPhaseParticle;
    @Getter
    protected float trackPhaseSpeed = 2;
    @Getter
    protected float trackPhaseTurn = 0.1F;
    @Getter
    protected boolean torpedo;
    @Getter
    protected boolean laserGuidance;

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

    /** 0 = disable, otherwise sets velocity scale on block hit particle fx */
    @Getter
    protected float blockHitFXScale;
    protected boolean readBlockHitFXScale;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        bulletSpeed = readValue("BulletSpeed", bulletSpeed, file);
        float muzzleVelocity = readValue("MuzzleVelocity", 0F, file);
        if (muzzleVelocity > 0F)
            bulletSpeed = muzzleVelocity / 20F;
        speedMultiplier = readValue("BulletSpeedMultiplier", speedMultiplier, file);
        penetrationAt100m = readValue("PenetrationAt100m", penetrationAt100m, file);
        if (!Float.isFinite(penetrationAt100m) || penetrationAt100m < 0F)
        {
            logError("PenetrationAt100m must be a finite non-negative value in millimetres; ignoring it", file);
            penetrationAt100m = 0F;
        }

        flak = readValue("FlakParticles", flak, file);
        flakParticles = readValue("FlakParticleType", flakParticles, file);
        setEntitiesOnFire = readValue("SetEntitiesOnFire", setEntitiesOnFire, file);
        hitSoundEnable = readValue("HitSoundEnable", hitSoundEnable, file);
        entityHitSoundEnable = readValue("EntityHitSoundEnable", entityHitSoundEnable, file);
        // Many content packs have a HitSound line with no parameter for no hit sound -> don't consider it a syntax error
        if (hasValueForConfigField("HitSound", file))
            hitSound = readSound("HitSound", hitSound, file);
        hitSoundRange = readValue("HitSoundRange", hitSoundRange, file);

        penetrates = readValue("Penetrates", true, file);
        penetratingPower = readValue("Penetration", penetratingPower, file);
        penetratingPower = readValue("PenetratingPower", penetratingPower, file);
        penetrationDecay = readValue("PenetrationDecay", penetrationDecay, file);
        blockPenetrationModifier = readValue("BlockPenetrationModifier", blockPenetrationModifier, file);

        playerPenetrationEffectOnDamage = readValue("PlayerPenetrationDamageEffect", playerPenetrationEffectOnDamage, file);
        entityPenetrationEffectOnDamage = readValue("EntityPenetrationDamageEffect", entityPenetrationEffectOnDamage, file);
        blockPenetrationEffectOnDamage = readValue("BlockPenetrationDamageEffect", blockPenetrationEffectOnDamage, file);
        penetrationDecayEffectOnDamage = readValue("PenetrationDecayDamageEffect", penetrationDecayEffectOnDamage, file);
        
        dragInAir = readValue("DragInAir", dragInAir, file);
        dragInWater = readValue("DragInWater", dragInWater, file);

        bulletSpread = readValue("Accuracy", bulletSpread, file);
        bulletSpread = readValue("Spread", bulletSpread, file);
        if (hasValueForConfigField("Dispersion", file))
            bulletSpread = (readValue("Dispersion", 0F, file) * Mth.DEG_TO_RAD) / ShootingHelper.ANGULAR_SPREAD_FACTOR;

        vls = readValue("VLS", vls, file);
        vls = readValue("HasDeadZone", vls, file);
        vlsTime = readValue("DeadZoneTime", vlsTime, file);
        fixedDirection = readValue("FixedTrackDirection", fixedDirection, file);
        turnRadius = readValue("GuidedTurnRadius", turnRadius, file);
        trackPhaseSpeed = readValue("GuidedPhaseSpeed", trackPhaseSpeed, file);
        trackPhaseTurn = readValue("GuidedPhaseTurnSpeed", trackPhaseTurn, file);
        boostPhaseParticle = readValue("BoostParticle", boostPhaseParticle, file);
        torpedo = readValue("Torpedo", torpedo, file);

        // Some content packs use true and false after this, which confuses things...
        if (readFieldWithOptionalValue("Bomb", false, file))
            weaponType = EnumWeaponType.BOMB;
        if (readFieldWithOptionalValue("Shell", false, file))
            weaponType = EnumWeaponType.SHELL;
        if (readFieldWithOptionalValue("Missile", false, file))
            weaponType = EnumWeaponType.MISSILE;

        weaponType = readValue("WeaponType", weaponType, EnumWeaponType.class, file);

        trailTexture = ResourceUtils.sanitize(readValue("TrailTexture", trailTexture, file));

        lockOnToPlanes = lockOnToVehicles = lockOnToMechas = readValue("LockOnToDriveables", lockOnToVehicles, file);
        lockOnToVehicles = readValue("LockOnToVehicles", lockOnToVehicles, file);
        lockOnToPlanes = readValue("LockOnToPlanes", lockOnToPlanes, file);
        lockOnToMechas = readValue("LockOnToMechas", lockOnToMechas, file);
        lockOnToPlayers = readValue("LockOnToPlayers", lockOnToPlayers, file);
        lockOnToLivings = readValue("LockOnToLivings", lockOnToLivings, file);

        maxLockOnAngle = readValue("MaxLockOnAngle", maxLockOnAngle, file);
        lockOnForce = readValue("LockOnForce", lockOnForce, file);
        lockOnForce = readValue("TurningForce", lockOnForce, file);
        maxDegreeOfMissile = readValue("MaxDegreeOfLockOnMissile", maxDegreeOfMissile, file);
        tickStartHoming = readValue("TickStartHoming", tickStartHoming, file);
        enableSACLOS = readValue("EnableSACLOS", enableSACLOS, file);
        maxDegreeOfSACLOS = readValue("MaxDegreeOFSACLOS", maxDegreeOfSACLOS, file);
        maxRangeOfMissile = readValue("MaxRangeOfMissile", maxRangeOfMissile, file);
        canSpotEntityDriveable = readValue("CanSpotEntityDriveable", canSpotEntityDriveable, file);
        shootForSettingPos = readValue("ShootForSettingPos", shootForSettingPos, file);
        shootForSettingPosHeight = readValue("ShootForSettingPosHeight", shootForSettingPosHeight, file);
        isDoTopAttack = readValue("IsDoTopAttack", isDoTopAttack, file);
        knockbackModifier = readValue("KnockbackModifier", knockbackModifier, file);

        //Submunitions
        hasSubmunitions = readValue("HasSubmunitions", hasSubmunitions, file);
        submunition = readValue("Submunition", submunition, file);
        numSubmunitions = readValue("NumSubmunitions", numSubmunitions, file);
        subMunitionTimer = readValue("SubmunitionDelay", subMunitionTimer, file);
        submunitionSpread = readValue("SubmunitionSpread", submunitionSpread, file);
        destroyOnDeploySubmunition = readValue("DestroyOnDeploySubmunition", destroyOnDeploySubmunition, file);

        addEffects("AddPotionEffect", hitEffects, file, false, false);
        addEffects("PotionEffect", hitEffects, file, false, false);

        manualGuidance = readValue("ManualGuidance", manualGuidance, file);
        laserGuidance = readValue("LaserGuidance", laserGuidance, file);
        maxRange = readValue("MaxRange", maxRange, file);

        blockHitFXScale = readValue("BlockHitFXScale", blockHitFXScale, file);
        readBlockHitFXScale = file.hasConfigLine("BlockHitFXScale");

        if (!penetrates)
            penetratingPower = DEFAULT_PENETRATING_POWER;

        // Clamp to [0, 1]
        dragInAir = Math.max(0, Math.min(1, dragInAir));
        dragInWater = Math.max(0, Math.min(1, dragInWater));

        if (!readBlockHitFXScale)
            blockHitFXScale = (float) ((Math.log(explosionRadius + 2) / Math.log(2.15)) + 0.05);

        if (textureName.isBlank())
            textureName = FlansMod.DEFAULT_BULLET_TEXTURE;
        if (trailTexture.isBlank())
            trailTexture = FlansMod.DEFAULT_BULLET_TRAIL_TEXTURE;

        if (roundsPerItem > 1)
        {
            // AddRound [name] [count] [mass in g] [explosive mass in kg TNT equivalent] [muzzle velocity in m/s]
            readValuesInLines("AddRound", file, 3).ifPresent(rounds -> rounds.forEach(round -> {
                period.add(new RoundEntry(round[0], Integer.parseInt(round[1]), readRoundStats(round, file)));
            }));
            periodLength = period.stream().mapToInt(RoundEntry::count).sum();
        }
    }

    @Override
    public boolean useKineticDamageSystem()
    {
        return mass > 0F || hasDifferentRounds();
    }

    @Override
    public boolean useNewExplosionSystem()
    {
        return explosiveMass > 0F || hasDifferentRounds();
    }

    /**
     * Penetrating power of the first round, without any weapon-supplied velocity.
     *
     * @see #getPenetratingPower(int, float)
     */
    public float getPenetratingPower()
    {
        return getPenetratingPower(0, 0F);
    }

    /**
     * Penetrating power of the round fired at the given position of the magazine.
     *
     * <p>Ammunition that uses the kinetic damage system, meaning it declares a projectile {@code Mass}, derives its
     * penetrating power from muzzle kinetic energy instead of from {@code Penetration} / {@code PenetratingPower},
     * so that mass and muzzle velocity alone determine both damage and penetration. Ammunition without a mass, and
     * ammunition explicitly declared as {@code Penetrates false}, keeps its authored value.
     *
     * @param shotsFired                    position in the magazine, which selects the round of an {@code AddRound} belt
     * @param weaponBulletSpeedBlocksPerTick velocity the firing weapon gives the projectile, used only when neither the
     *                                       round nor the ammunition declares one; pass 0 when no weapon is known
     */
    public float getPenetratingPower(int shotsFired, float weaponBulletSpeedBlocksPerTick)
    {
        if (!penetrates || !useKineticDamageSystem())
            return penetratingPower;

        float mass = getMass(shotsFired);
        if (mass <= 0F)
            return penetratingPower;

        return ShootingHelper.getKineticPenetratingPower(mass, getBulletSpeed(shotsFired, weaponBulletSpeedBlocksPerTick));
    }

    /**
     * @return the velocity of the round fired at the given position of the magazine, falling back to the velocity
     * supplied by the weapon and then to the default bullet speed
     */
    public float getBulletSpeed(int shotsFired, float weaponBulletSpeedBlocksPerTick)
    {
        float speed = hasDifferentRounds() ? statsForShot(shotsFired).bulletSpeed : bulletSpeed;
        boolean useMultiplier = speedMultiplier > 0F && speedMultiplier != 1F;

        if (speed > 0F)
            return useMultiplier ? speed * speedMultiplier : speed;
        if (weaponBulletSpeedBlocksPerTick > 0F)
            return weaponBulletSpeedBlocksPerTick;

        return useMultiplier ? DEFAULT_BULLET_SPEED * speedMultiplier : DEFAULT_BULLET_SPEED;
    }

    public float getBulletSpeed(boolean enforceDefaultFallback)
    {
        float speed = hasDifferentRounds() ? statsForShot(0).bulletSpeed : bulletSpeed;
        boolean useMultiplier = speedMultiplier > 0F && speedMultiplier != 1F;
        speed = useMultiplier ? speed * speedMultiplier : speed;

        if (enforceDefaultFallback && speed <= 0F)
            return useMultiplier ? DEFAULT_BULLET_SPEED * speedMultiplier : DEFAULT_BULLET_SPEED;

        return speed;
    }

    public float getBulletSpeed()
    {
        return getBulletSpeed(false);
    }

    @Override
    public float getMass()
    {
        return getMass(0);
    }

    public float getMass(int shotsFired)
    {
        if (hasDifferentRounds())
        {
            return statsForShot(shotsFired).mass;
        }
        return super.getMass();
    }

    private RoundStats readRoundStats(String[] round, TypeFile file)
    {
        float roundMass = nonNegativeRoundValue(round, 2, "mass in grams", file);
        float roundExplosiveMass = nonNegativeRoundValue(round, 3, "explosive mass in kg TNT equivalent", file);
        float roundSpeed = nonNegativeRoundValue(round, 4, "muzzle velocity in m/s", file) / 20F;
        float roundPenetration = nonNegativeRoundValue(round, 5, "penetration at 100 m in millimetres", file);
        return new RoundStats(roundMass, roundExplosiveMass, roundSpeed, roundPenetration);
    }

    private float nonNegativeRoundValue(String[] round, int index, String description, TypeFile file)
    {
        if (index >= round.length)
            return 0F;
        float value;
        try
        {
            value = Float.parseFloat(round[index]);
        }
        catch (NumberFormatException ex)
        {
            logError("AddRound " + description + " must be numeric; using zero", file);
            return 0F;
        }
        if (Float.isFinite(value) && value >= 0F)
            return value;
        logError("AddRound " + description + " must be finite and non-negative; using zero", file);
        return 0F;
    }

    public float getPenetrationAt100m(int shotsFired)
    {
        return hasDifferentRounds() ? statsForShot(shotsFired).penetrationAt100m() : penetrationAt100m;
    }

    @Override
    public float getExplosiveMass()
    {
        if (hasDifferentRounds())
        {
            return statsForShot(0).explosiveMass;
        }
        return super.getExplosiveMass();
    }

    @Override
    public FlanExplosion.Stats getExplosionStats(@Nullable Entity explosiveEntity)
    {
        if (explosiveEntity instanceof Bullet bullet)
        {
            FiredShot shot = bullet.getFiredShot();
            // A per-weapon AmmoExplosiveMass or AddRoundForAmmo override replaces the
            // charge for this shot; otherwise a belt round's own charge is used.
            boolean overridden = shot != null && !shot.getAmmoOverride().isEmpty();
            if (overridden || bullet.getConfigType().hasDifferentRounds())
            {
                float explosiveCharge = shot != null ? shot.getExplosiveMass()
                    : statsForShot(0).explosiveMass();
                return explosionStatsForCharge(explosiveCharge);
            }
        }
        return super.getExplosionStats(explosiveEntity);
    }

    /** Derives the whole explosion profile from one bursting charge in kg TNT equivalent. */
    private FlanExplosion.Stats explosionStatsForCharge(float explosiveCharge)
    {
        float explosionRadius = (float) (ModCommonConfig.get().newDamageSystemExplosiveRadiusReference() * Math.cbrt(explosiveCharge));
        float explosionPower = (float) (ModCommonConfig.get().newDamageSystemExplosivePowerReference() * Math.cbrt(explosiveCharge));
        float explosionBlastRadius = ModCommonConfig.get().newDamageSystemBlastToExplosionRadiusRatio() * explosionRadius;
        DamageStats explosionBlastDamage = new DamageStats();
        explosionBlastDamage.setDamage((float) (ModCommonConfig.get().newDamageSystemExplosiveDamageReference() * Math.cbrt(explosiveCharge)));
        explosionBlastDamage.calculate();
        return new FlanExplosion.Stats(explosionRadius, explosionPower, explosionBlastRadius,
            explosionBlastDamage, fragRadius, fragIntensity, explosionFragDamage,
            Float.isFinite(explosiveCharge) && explosiveCharge > 0F ? explosiveCharge : 0F);
    }

    public boolean hasDifferentRounds()
    {
        return roundsPerItem > 1 && !period.isEmpty();
    }

    public RoundStats statsForShot(int shotsFired)
    {
        int k = Math.floorMod(shotsFired, periodLength);
        for (RoundEntry r : period)
        {
            if (k < r.count())
                return r.stats();
            k -= r.count();
        }

        // unreachable if periodLen computed correctly
        return period.get(period.size() - 1).stats();
    }

    public float getBlockPenetrationModifier()
    {
        return blockPenetrationModifier < 0F ? (float) ModCommonConfig.get().blockPenetrationModifier() : blockPenetrationModifier;
    }
}
