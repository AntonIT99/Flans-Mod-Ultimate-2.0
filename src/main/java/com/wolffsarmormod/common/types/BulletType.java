package com.wolffsarmormod.common.types;

import com.flansmod.client.model.ModelBullet;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.driveables.EnumWeaponType;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.wolffsarmormod.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class BulletType extends ShootableType
{
    public static final double LOCK_ON_RANGE = 128.0;

    protected float speedMultiplier = 1F;
    /** The number of flak particles to spawn upon exploding */
    @Getter
    protected int flak = 0;
    /** The type of flak particles to spawn */
    @Getter
    protected String flakParticles = "largesmoke";

    /** If true then this bullet will burn entites it hits */
    @Getter
    protected boolean setEntitiesOnFire = false;

    /** If > 0 this will act like a mine and explode when a living entity comes within this radius of the grenade */
    protected float livingProximityTrigger = -1F;
    /** If > 0 this will act like a mine and explode when a driveable comes within this radius of the grenade */
    protected float driveableProximityTrigger = -1F;
    /** How much damage to deal to the entity that triggered it */
    protected float damageToTriggerer = 0F;
    /** Detonation will not occur until after this time */
    protected int primeDelay = 0;
    /** Particles given off in the detonation */
    protected int explodeParticles = 0;
    protected String explodeParticleType = "largesmoke";

    /** Exclusively for driveable usage. Replaces old isBomb and isShell booleans with something more flexible */
    @Getter
    protected EnumWeaponType weaponType = EnumWeaponType.GUN;

    @Getter
    protected String hitSound;
    @Getter
    protected float hitSoundRange = 64F;
    protected boolean hitSoundEnable = false;
    protected boolean entityHitSoundEnable = false;

    @Getter
    protected float penetratingPower = 1F;
    // In % of penetration to remove per tick.
    protected float penetrationDecay = 0F;

    /*
     * How much the loss of penetration power affects the damage of the bullet. 0 = damage not affected by that kind of penetration,
     * 1 = damage is fully affected by bullet penetration of that kind
     */
    protected float playerPenetrationEffectOnDamage = 0F;
    protected float entityPenetrationEffectOnDamage = 0F;
    protected float blockPenetrationEffectOnDamage = 0F;
    protected float penetrationDecayEffectOnDamage = 0F;

    // Knocback modifier. less gives less kb, more gives more kb, 1 = normal kb.
    protected float knockbackModifier;
    /** Lock on variables. If true, then the bullet will search for a target at the moment it is fired */
    @Getter
    protected boolean lockOnToPlanes = false;
    @Getter
    protected boolean lockOnToVehicles = false;
    @Getter
    protected boolean lockOnToMechas = false;
    @Getter
    protected boolean lockOnToPlayers = false;
    @Getter
    protected boolean lockOnToLivings = false;
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
    protected boolean enableSACLOS = false;
    protected int maxDegreeOfSACLOS = 5;
    protected int maxRangeOfMissile = 150;
    //protected int maxDegreeOfMissileXAxis = 10;
    //protected int maxDegreeOfMissileYAxis = 10;
    //protected int maxDegreeOfMissileZAxis = 10;

    protected boolean manualGuidance = false;
    protected int lockOnFuse = 10;

    @Getter
    protected List<MobEffectInstance> hitEffects = new ArrayList<>();

    /** Number of bullets to fire per shot if allowNumBulletsByBulletType = true */
    protected int numBullets = -1;

    protected float dragInAir   = 0.99F;
    protected float dragInWater = 0.80F;

    protected boolean canSpotEntityDriveable = false;

    protected int maxRange = -1;

    protected boolean shootForSettingPos = false;
    protected int shootForSettingPosHeight = 100;

    protected boolean isDoTopAttack = false;


    //Smoke
    /** Time to remain after detonation */
    protected int smokeTime = 0;
    /** Particles given off after detonation */

    protected String smokeParticleType = "explode";
    /** The effects to be given to people coming too close */

    protected ArrayList<MobEffectInstance> smokeEffects = new ArrayList<>();
    /** The radius for smoke effects to take place in */

    protected float smokeRadius = 5F;
    protected boolean TVguide = true;

    //Other stuff
    protected boolean VLS = false;
    protected int VLSTime = 0;
    protected boolean fixedDirection = false;
    protected float turnRadius = 3;
    protected String boostPhaseParticle;
    protected float trackPhaseSpeed = 2;
    protected float trackPhaseTurn = 0.2F;

    protected boolean torpedo = false;

    protected boolean fancyDescription = true;

    protected boolean laserGuidance = false;

    // 0 = disable, otherwise sets velocity scale on block hit particle fx
    protected float blockHitFXScale;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        if (split[0].equalsIgnoreCase("Bomb"))
            weaponType = EnumWeaponType.BOMB;
        if (split[0].equalsIgnoreCase("Shell"))
            weaponType = EnumWeaponType.SHELL;
        if (split[0].equalsIgnoreCase("Missile"))
            weaponType = EnumWeaponType.MISSILE;

        weaponType = readValue(split, "WeaponType", weaponType, EnumWeaponType.class, file);
    }

    @Override
    protected void postRead()
    {
        super.postRead();

        if (textureName.isBlank())
            textureName = ArmorMod.DEFAULT_BULLET_TEXTURE;
        if (trailTexture.isBlank())
            trailTexture = ArmorMod.DEFAULT_BULLET_TRAIL_TEXTURE;
    }

    @Override
    @Nullable
    protected IModelBase getDefaultModel()
    {
        return new ModelBullet();
    }
}
