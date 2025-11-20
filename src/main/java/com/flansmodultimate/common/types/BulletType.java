package com.flansmodultimate.common.types;

import com.flansmod.client.model.ModelBullet;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.util.ResourceUtils;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

@NoArgsConstructor
public class BulletType extends ShootableType
{
    //TODO: make these constants configurable
    public static final double LOCK_ON_RANGE = 128.0;
    public static final int FLAK_PARTICLES_RANGE = 200;

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
    @Getter
    protected int primeDelay;

    /** Exclusively for driveable usage. Replaces old isBomb and isShell booleans with something more flexible */
    @Getter
    protected EnumWeaponType weaponType = EnumWeaponType.GUN;

    @Getter
    protected String hitSound;
    @Getter
    protected float hitSoundRange = 64F;
    protected boolean hitSoundEnable;
    protected boolean entityHitSoundEnable;

    protected boolean penetrates = true;
    @Getter
    protected float penetratingPower = 1F;
    /** In % of penetration to remove per tick. */
    @Getter
    protected float penetrationDecay;

    /**
     * How much the loss of penetration power affects the damage of the bullet. 0 = damage not affected by that kind of penetration,
     * 1 = damage is fully affected by bullet penetration of that kind
     */
    protected float playerPenetrationEffectOnDamage;
    protected float entityPenetrationEffectOnDamage;
    protected float blockPenetrationEffectOnDamage;
    protected float penetrationDecayEffectOnDamage;

    /** Knocback modifier. less gives less kb, more gives more kb, 1 = normal kb. */
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
    /** Lock on force that pulls the bullet towards its prey */
    @Getter
    protected float lockOnForce = 1F;
    @Getter
    protected String trailTexture = StringUtils.EMPTY;
    protected int maxDegreeOfMissile = 20;
    protected int tickStartHoming = 5;
    protected boolean enableSACLOS;
    protected int maxDegreeOfSACLOS = 5;
    protected int maxRangeOfMissile = 150;

    @Getter
    protected boolean manualGuidance;
    protected int lockOnFuse = 10;

    @Getter
    protected List<MobEffectInstance> hitEffects = new ArrayList<>();

    @Getter
    protected float dragInAir = 0.99F;
    @Getter
    protected float dragInWater = 0.8F;
    protected boolean canSpotEntityDriveable;
    @Getter
    protected int maxRange = -1;
    @Getter
    protected boolean shootForSettingPos;
    @Getter
    protected int shootForSettingPosHeight = 100;
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
    protected boolean fancyDescription = true;
    @Getter
    protected boolean laserGuidance;

    /** 0 = disable, otherwise sets velocity scale on block hit particle fx */
    protected float blockHitFXScale;
    protected boolean readBlockHitFXScale;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        flak = readValue(split, "FlakParticles", flak, file);
        flakParticles = readValue(split, "FlakParticleType", flakParticles, file);
        setEntitiesOnFire = readValue(split, "SetEntitiesOnFire", setEntitiesOnFire, file);
        hitSoundEnable = readValue(split, "HitSoundEnable", hitSoundEnable, file);
        entityHitSoundEnable = readValue(split, "EntityHitSoundEnable", entityHitSoundEnable, file);
        // Many content packs have a HitSound line with no parameter for no hit sound -> don't consider it a syntax error
        if (split.length > 1)
            hitSound = readSound(split, "HitSound", hitSound, file);
        hitSoundRange = readValue(split, "HitSoundRange", hitSoundRange, file);

        penetrates = readValue(split, "Penetrates", true, file);
        penetratingPower = readValue(split, "Penetration", penetratingPower, file);
        penetratingPower = readValue(split, "PenetratingPower", penetratingPower, file);
        penetrationDecay = readValue(split, "PenetrationDecay", penetrationDecay, file);

        playerPenetrationEffectOnDamage = readValue(split, "PlayerPenetrationDamageEffect", playerPenetrationEffectOnDamage, file);
        entityPenetrationEffectOnDamage = readValue(split, "EntityPenetrationDamageEffect", entityPenetrationEffectOnDamage, file);
        blockPenetrationEffectOnDamage = readValue(split, "BlockPenetrationDamageEffect", blockPenetrationEffectOnDamage, file);
        penetrationDecayEffectOnDamage = readValue(split, "PenetrationDecayDamageEffect", penetrationDecayEffectOnDamage, file);
        
        dragInAir = readValue(split, "DragInAir", dragInAir, file);
        dragInWater = readValue(split, "DragInWater", dragInWater, file);

        bulletSpread = readValue(split, "Accuracy", bulletSpread, file);
        bulletSpread = readValue(split, "Spread", bulletSpread, file);
        primeDelay = readValue(split, "PrimeDelay", primeDelay, file);
        primeDelay = readValue(split, "TriggerDelay", primeDelay, file);

        vls = readValue(split, "VLS", vls, file);
        vls = readValue(split, "HasDeadZone", vls, file);
        vlsTime = readValue(split, "DeadZoneTime", vlsTime, file);
        fixedDirection = readValue(split, "FixedTrackDirection", fixedDirection, file);
        turnRadius = readValue(split, "GuidedTurnRadius", turnRadius, file);
        trackPhaseSpeed = readValue(split, "GuidedPhaseSpeed", trackPhaseSpeed, file);
        trackPhaseTurn = readValue(split, "GuidedPhaseTurnSpeed", trackPhaseTurn, file);
        boostPhaseParticle = readValue(split, "BoostParticle", boostPhaseParticle, file);
        torpedo = readValue(split, "Torpedo", torpedo, file);

        // Some content packs use 'true' and false after this, which confuses things...
        if (split[0].equalsIgnoreCase("Bomb") && !(split.length > 1 && split[1].equalsIgnoreCase(Boolean.FALSE.toString())))
            weaponType = EnumWeaponType.BOMB;
        if (split[0].equalsIgnoreCase("Shell") && !(split.length > 1 && split[1].equalsIgnoreCase(Boolean.FALSE.toString())))
            weaponType = EnumWeaponType.SHELL;
        if (split[0].equalsIgnoreCase("Missile") && !(split.length > 1 && split[1].equalsIgnoreCase(Boolean.FALSE.toString())))
            weaponType = EnumWeaponType.MISSILE;
        weaponType = readValue(split, "WeaponType", weaponType, EnumWeaponType.class, file);

        trailTexture = ResourceUtils.sanitize(readValue(split, "TrailTexture", trailTexture, file));

        if (split[0].equalsIgnoreCase("LockOnToDriveables"))
            lockOnToPlanes = lockOnToVehicles = lockOnToMechas =  readValue(split, "LockOnToDriveables", lockOnToVehicles, file);

        lockOnToVehicles = readValue(split, "LockOnToVehicles", lockOnToVehicles, file);
        lockOnToPlanes = readValue(split, "LockOnToPlanes", lockOnToPlanes, file);
        lockOnToMechas = readValue(split, "LockOnToMechas", lockOnToMechas, file);
        lockOnToPlayers = readValue(split, "LockOnToPlayers", lockOnToPlayers, file);
        lockOnToLivings = readValue(split, "LockOnToLivings", lockOnToLivings, file);

        maxLockOnAngle = readValue(split, "MaxLockOnAngle", maxLockOnAngle, file);
        lockOnForce = readValue(split, "LockOnForce", lockOnForce, file);
        lockOnForce = readValue(split, "TurningForce", lockOnForce, file);
        maxDegreeOfMissile = readValue(split, "MaxDegreeOfLockOnMissile", maxDegreeOfMissile, file);
        tickStartHoming = readValue(split, "TickStartHoming", tickStartHoming, file);
        enableSACLOS = readValue(split, "EnableSACLOS", enableSACLOS, file);
        maxDegreeOfSACLOS = readValue(split, "MaxDegreeOFSACLOS", maxDegreeOfSACLOS, file);
        maxRangeOfMissile = readValue(split, "MaxRangeOfMissile", maxRangeOfMissile, file);
        canSpotEntityDriveable = readValue(split, "CanSpotEntityDriveable", canSpotEntityDriveable, file);
        shootForSettingPos = readValue(split, "ShootForSettingPos", shootForSettingPos, file);
        shootForSettingPosHeight = readValue(split, "ShootForSettingPosHeight", shootForSettingPosHeight, file);
        isDoTopAttack = readValue(split, "IsDoTopAttack", isDoTopAttack, file);
        knockbackModifier = readValue(split, "KnockbackModifier", knockbackModifier, file);

        addEffects(readValues(split, "AddPotionEffect", file), hitEffects, line, file, false, false);
        addEffects(readValues(split, "PotionEffect", file), hitEffects, line, file, false, false);

        manualGuidance = readValue(split, "ManualGuidance", manualGuidance, file);
        laserGuidance = readValue(split, "LaserGuidance", laserGuidance, file);
        lockOnFuse = readValue(split, "LockOnFuse", lockOnFuse, file);
        maxRange = readValue(split, "MaxRange", maxRange, file);
        fancyDescription = readValue(split, "FancyDescription", fancyDescription, file);
        speedMultiplier = readValue(split, "BulletSpeedMultiplier", speedMultiplier, file);

        blockHitFXScale = readValue(split, "BlockHitFXScale", blockHitFXScale, file);
        if (split[0].equalsIgnoreCase("BlockHitFXScale"))
            readBlockHitFXScale = true;
    }

    @Override
    protected void postRead()
    {
        super.postRead();
        
        if (!penetrates)
            penetratingPower = 0.7F;

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
    protected IModelBase getDefaultModel()
    {
        return new ModelBullet();
    }
}
