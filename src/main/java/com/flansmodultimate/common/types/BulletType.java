package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelBullet;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.util.ResourceUtils;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor
public class BulletType extends ShootableType
{
    //TODO: make these constants configurable
    public static final double LOCK_ON_RANGE = 128.0;
    public static final int FLAK_PARTICLES_RANGE = 256;

    public static final float DEFAULT_PENETRATING_POWER = 0.7F;

    @Getter
    protected float speedMultiplier = 1F;
    /** The number of flak particles to spawn upon exploding */
    @Getter
    protected int flak;
    /** The type of flak particles to spawn */
    @Getter
    protected String flakParticles = "largesmoke";

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

    protected boolean penetrates = true;
    @Getter
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
        speedMultiplier = readValue("BulletSpeedMultiplier", speedMultiplier, file);

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
    }

    @Override
    @Nullable
    public IModelBase getDefaultModel()
    {
        return new ModelBullet();
    }

    public float getBlockPenetrationModifier()
    {
        return blockPenetrationModifier < 0F ? (float) ((double) ModCommonConfigs.blockPenetrationModifier.get()) : blockPenetrationModifier;
    }
}
