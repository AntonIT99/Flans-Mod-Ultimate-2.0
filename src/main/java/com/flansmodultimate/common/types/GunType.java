package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.EnumSecondaryFunction;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.GunRecoil;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor
public class GunType extends PaintableType implements IScope
{
    protected static final Random rand = new Random();
    protected static final int DEFAULT_SHOOT_DELAY = 2;

    protected static final String NBT_ATTACHMENTS = "attachments";
    protected static final String NBT_GENERIC = "generic_";
    protected static final String NBT_BARREL = "barrel";
    protected static final String NBT_SCOPE = "scope";
    protected static final String NBT_STOCK = "stock";
    protected static final String NBT_GRIP = "grip";
    protected static final String NBT_GADGET = "gadget";
    protected static final String NBT_SLIDE = "slide";
    protected static final String NBT_PUMP = "pump";
    protected static final String NBT_ACCESSORY = "accessory";
    protected static final String NBT_SECONDARY_FIRE = "secondary_fire";
    protected static final String NBT_GUN_MODE = "gun_mode";


    /** Extended Recoil System */
    protected GunRecoil recoil = new GunRecoil();
    protected boolean useFancyRecoil;

    //Recoil Variables
    /**
     * Base value for Upwards cursor/view recoil
     */
    protected float recoilPitch;
    /**
     * Base value for Left/Right cursor/view recoil
     */
    protected float recoilYaw;
    /**
     * Modifier for setting the maximum additional pitch when randomizing recoil: randomized pitch recoil in [recoilPitch, recoilPitch + rndRecoilYawRange)
     */
    protected float rndRecoilPitchRange = 0.5F;
    /**
     * Modifier for setting the yaw divergence when randomizing recoil: randomized yaw recoil in [recoilYaw - rndRecoilYawRange/2, recoilYaw + rndRecoilYawRange/2)
     */
    protected float rndRecoilYawRange = 0.3F;

    /**
     * DEPRECATED DO NOT USE. Subtracts from pitch recoil when crouching.
     */
    protected float decreaseRecoilPitch;
    /**
     * DEPRECATED DO NOT USE. Divisor for yaw recoil when crouching.
     */
    protected float decreaseRecoilYaw;

    /**
     * The alternatives to the above. Simple multipliers for sneaking, sprinting on yaw and pitch respectively. 1F = no change.
     * -1F is to be used to enable backwards compatibility for sneaking (-2F rather than multiplying)
     */
    protected float recoilSneakingMultiplier = -1F;
    protected float recoilSprintingMultiplier = 1F;
    protected float recoilSneakingMultiplierYaw = 0.8F;
    protected float recoilSprintingMultiplierYaw = 1.2F;

    /* Countering gun recoil can be modelled with angle=n^tick where n is the coefficient here. */
    /**
     * HIGHER means less force to center, meaning it takes longer to return.
     */
    protected float recoilCounterCoefficient = 0.8F;
    /**
     * The above variable but for sprinting.
     */
    protected float recoilCounterCoefficientSprinting = 0.9F;
    /**
     * The above variable but for sneaking.
     */
    protected float recoilCounterCoefficientSneaking = 0.7F;

    //Ammo & Reload Variables
    /**
     * The list of bullet types that can be used in this gun
     */
    @Getter
    protected Set<String> ammo = new LinkedHashSet<>();
    /**
     * Whether the player can press the reload key (default R) to reload this gun
     */
    @Getter
    protected boolean canForceReload = true;
    /**
     * Whether the player can receive ammo for this gun from an ammo mag
     */
    @Getter
    protected boolean allowRearm = true;
    /**
     * The time (in ticks) it takes to reload this gun
     */
    @Getter
    protected int reloadTime;

    //Projectile Mechanic Variables
    /**
     * The amount that bullets spread out when fired from this gun
     */
    @Getter @Setter
    protected float bulletSpread;
    protected EnumSpreadPattern spreadPattern = EnumSpreadPattern.CUBE;
    protected float sneakSpreadModifier = 0.63F;
    protected float sprintSpreadModifier = 1.75F;
    /**
     * If true, spread determined by loaded ammo type
     */
    @Getter
    protected boolean allowSpreadByBullet;
    /**
     * Damage inflicted by this gun. Multiplied by the bullet damage.
     */
    protected float damage;
    /**
     * The damage inflicted upon punching someone with this gun
     */
    protected float meleeDamage = 1F;
    /** Modifier for melee damage against specifically driveable entities. */
    protected float meleeDamageDriveableModifier = 1F;
    /**
     * The speed of bullets upon leaving this gun. 0.0f means instant.
     */
    @Getter
    protected float bulletSpeed = 5F;
    /**
     * The muzzle velocity in m/s. Replaces BulletSpeed. Set to -1F to ignore.
     */
    @Getter
    protected float muzzleVelocity = -1F;
    /**
     * The number of bullet entities created by each shot
     */
    @Getter
    protected int numBullets = 1;
    /**
     * Allows you to set how many bullet entities are fired per shot via the ammo used
     */
    protected boolean allowNumBulletsByBulletType = true;
    /**
     * The delay between shots in ticks (1/20ths of seconds) OUTDATED, USE RPM
     */
    protected float shootDelay;
    /**
     * Number of ammo items that the gun may hold. Most guns will hold one magazine.
     * Some may hold more, such as Nerf pistols, revolvers or shotguns
     */
    protected int numPrimaryAmmoItems = 1;
    /**
     * The fire rate of the gun in RPM, 1200 = MAX
     */
    protected float roundsPerMin;
    /**
     * The firing mode of the gun. One of semi-auto, full-auto, minigun or burst
     */
    @Getter
    protected EnumFireMode mode = EnumFireMode.SEMIAUTO;
    protected EnumFireMode[] submode = new EnumFireMode[]{EnumFireMode.FULLAUTO};
    protected EnumFireMode defaultmode = mode;
    /**
     * The number of bullets to fire per burst in burst mode
     */
    protected int numBurstRounds = 3;
    /**
     * The required speed for minigun mode guns to start firing
     */
    @Getter
    protected float minigunStartSpeed = 15F;
    /**
     * The maximum speed a minigun mode gun can reach
     */
    @Getter
    protected float minigunMaxSpeed = 30F;
    /**
     * Whether this gun can be used underwater
     */
    protected boolean canShootUnderwater = true;
    /**
     * The amount of knockback to impact upon the player per shot (1 is one block)
     */
    @Getter
    protected float knockback;
    /**
     * The secondary function of this gun. By default, the left mouse button triggers this
     */
    @Getter
    protected EnumSecondaryFunction secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;
    protected EnumSecondaryFunction secondaryFunctionWhenShoot = null;
    /**
     * If true, then this gun can be dual wielded
     */
    @Getter
    protected boolean oneHanded;
    /**
     * For one shot items like a panzerfaust
     */
    @Getter
    protected boolean consumeGunUponUse;
    /**
     * Show the crosshair when holding this weapon
     */
    protected boolean showCrosshair = true;
    /**
     * Item to drop on shooting
     */
    @Getter
    protected String dropItemOnShoot;
    /**
     * Set these to make guns only usable by a certain type of entity
     */
    @Getter
    protected boolean usableByPlayers = true;
    protected boolean usableByMechas = true;
    /**
     * If false, then attachments wil not be listed in item GUI
     */
    @Getter
    protected boolean showAttachments = true;
    /**
     * Show statistics
     */
    @Getter
    protected boolean showDamage = true;
    @Getter
    protected boolean showRecoil = true;
    @Getter
    protected boolean showSpread = true;
    @Getter
    protected boolean showReloadTime = true;
    /**
     * Whether Gun makes players to be UseAnim.BOW
     */
    protected UseAnim itemUseAction = UseAnim.BOW;
    /* Whether the gun can be hipfired while sprinting */
    /**
     * 0=use flansmod.cfg default, 1=force allow, 2=force deny
     **/
    protected int hipFireWhileSprinting;

    //Launcher variables
    @Getter
    protected int canLockOnAngle = 5;
    @Getter
    protected int lockOnSoundTime;
    @Getter
    protected String lockOnSound = StringUtils.EMPTY;
    @Getter
    protected int maxRangeLockOn = 80;
    @Getter
    protected boolean canSetPosition;
    /**
     * Determines what the launcher can lock on to
     */
    @Getter
    protected boolean lockOnToDriveables;
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

    //Shields
	/*A shield is actually a gun without any shoot functionality (similar to knives or binoculars)
	and a load of shield code on top. This means that guns can have in built shields (think Nerf Stampede) */
    /**
     * Whether this gun has a shield piece
     */
    @Getter
    protected boolean shield = false;
    /**
     * Shield collision box definition. In model co-ordinates
     */
    @Getter
    protected Vector3f shieldOrigin;
    @Getter
    protected Vector3f shieldDimensions;
    /**
     * Percentage of damage blocked between 0.00-1.00 (0.50F = 50%)
     */
    @Getter
    protected float shieldDamageAbsorption;

    //Sounds
    /**
     * The sound played upon shooting
     */
    @Getter
    protected String shootSound;
    /**
     * Bullet insert reload sound
     */
    protected String bulletInsert = FlansMod.SOUND_DEFAULT_SHELL_INSERT;
    /**
     * Pump Sound
     */
    protected String actionSound;
    /**
     * The sound to play upon shooting on last round
     */
    protected String lastShootSound;

    /**
     * The sound played upon shooting with a suppressor
     */
    protected String suppressedShootSound;
    protected String lastShootSoundSuppressed;

    /**
     * The length of the sound for looping sounds
     */
    @Getter
    protected int shootSoundLength;
    /**
     * Whether to distort the sound or not. Generally only set to false for looping sounds
     */
    protected String reloadSound;
    /**
     * The sound to play upon reloading when empty
     */
    protected String reloadSoundOnEmpty;
    /**
     * The sound to play open firing when empty(once)
     */
    protected String clickSoundOnEmpty;
    /**
     * Sound to play on firing when empty(multiple times)
     */
    protected String clickSoundOnEmptyRepeated;
    /**
     * The sound to play while holding the weapon in the hand
     */
    @Getter
    protected String idleSound;

    //Sound Modifiers
    /**
     * Whether to distort the sound or not. Generally only set to false for looping sounds
     */
    protected boolean distortSound = true;
    /**
     * The length of the idle sound for looping sounds (miniguns)
     */
    @Getter
    protected int idleSoundLength;
    /**
     * The block range for idle sounds (for miniguns etc.)
     */
    @Getter
    protected int idleSoundRange = (int) FlansMod.SOUND_RANGE;
    /**
     * The block range for melee sounds
     */
    @Getter
    protected int meleeSoundRange = (int) FlansMod.SOUND_RANGE;
    /**
     * The block range for reload sounds
     */
    @Getter
    protected int reloadSoundRange = (int) FlansMod.SOUND_RANGE;
    /**
     * The block range for gunshots sounds
     */
    @Getter
    protected int gunSoundRange = (int) FlansMod.GUN_FIRE_SOUND_RANGE;

    /**
     * Sound to be played outside of normal range
     */
    protected String distantShootSound = "";
    /**
     * Max range for the sound to be played
     */
    protected int distantSoundRange = 100;

    //Looping sounds
    /**
     * Whether the looping sounds should be used. Automatically set if the player sets any one of the following sounds
     */
    @Getter
    protected boolean useLoopingSounds;
    /**
     * Played when the player starts to hold shoot
     */
    @Getter
    protected String warmupSound;
    @Getter
    protected int warmupSoundLength = 20;
    /**
     * Played in a loop until player stops holding shoot
     */
    @Getter
    protected String loopedSound;
    @Getter
    protected int loopedSoundLength = 20;
    /**
     * Played when the player stops holding shoot
     */
    @Getter
    protected String cooldownSound;

    //Custom Melee Stuff
    /**
     * The sound to play upon weapon swing
     */
    @Getter
    protected String meleeSound;
    /**
     * The time delay between custom melee attacks
     */
    @Getter
    protected int meleeTime = 1;
    /**
     * The path the melee weapon takes
     */
    @Getter
    protected ArrayList<Vector3f> meleePath = new ArrayList<>();
    @Getter
    protected ArrayList<Vector3f> meleePathAngles = new ArrayList<>();
    /**
     * The points on the melee weapon that damage is actually done from.
     */
    @Getter
    protected ArrayList<Vector3f> meleeDamagePoints = new ArrayList<>();

    //Deployable Settings
    /**
     * If true, then the bullet does not shoot when right-clicked, but must instead be placed on the ground
     */
    @Getter
    protected boolean deployable;
    protected String deployableModelName = StringUtils.EMPTY;
    @Getter
    protected String deployableModelClassName = StringUtils.EMPTY;
    @OnlyIn(Dist.CLIENT)
    protected ResourceLocation deployableTexture;
    /**
     * The deployable model's texture
     */
    protected String deployableTextureName = StringUtils.EMPTY;
    /**
     * Various deployable settings controlling the player view limits and standing position
     */
    protected float standBackDist = 1.5F;
    protected float topViewLimit = -60F;
    protected float bottomViewLimit = 30F;
    protected float sideViewLimit = 45F;
    protected float pivotHeight = 0.375F;

    //Default Scope Settings. Overriden by scope attachments
    //In many cases, this will simply be iron sights
    /**
     * The zoom level of the default scope
     */
    @Getter
    protected float zoomFactor = 1F;
    /**
     * The FOV zoom level of the default scope
     */
    @Getter
    protected float fovFactor = 1.5F;
    /**
     * Gives night vision while scoped if true
     */
    @Getter
    protected boolean allowNightVision;

    /**
     * For adding a bullet casing model to render
     */
    protected String casingModelName = StringUtils.EMPTY;
    @Getter
    protected String casingModelClassName = StringUtils.EMPTY;
    @OnlyIn(Dist.CLIENT)
    protected ResourceLocation casingTexture;
    /**
     * For adding a muzzle flash model to render
     */
    protected String flashModelName = StringUtils.EMPTY;
    @Getter
    protected String flashModelClassName = StringUtils.EMPTY;
    @OnlyIn(Dist.CLIENT)
    protected ResourceLocation flashTexture;
    protected String muzzleFlashModelName = StringUtils.EMPTY;
    @Getter
    protected String muzzleFlashModelClassName = StringUtils.EMPTY;
    /**
     * Set a bullet casing texture
     */
    protected String casingTextureName = StringUtils.EMPTY;
    /**
     * Set a muzzle flash texture
     */
    protected String flashTextureName = StringUtils.EMPTY;
    /**
     * Set a hit marker texture
     */
    protected String hitTextureName = StringUtils.EMPTY;

    protected String muzzleFlashParticle = ParticleHelper.FM_MUZZLE_FLASH;
    protected float muzzleFlashParticleSize = 1F;
    protected boolean useMuzzleFlashDefaults = true;
    protected boolean showMuzzleFlashParticles = true;
    protected boolean showMuzzleFlashParticlesFirstPerson;
    protected Vector3f muzzleFlashParticlesHandOffset = new Vector3f();
    protected Vector3f muzzleFlashParticlesShoulderOffset = new Vector3f();

    //Attachment settings
    /**
     * If this is true, then all attachments are allowed. Otherwise, the list is checked.
     */
    protected boolean allowAllAttachments = true; // TODO: config option to force disable all attachments restrictions
    /**
     * The list of allowed attachments for this gun
     */
    protected List<AttachmentType> allowedAttachments = new ArrayList<>();
    /**
     * Whether each attachment slot is available
     */
    protected boolean allowBarrelAttachments;
    protected boolean allowScopeAttachments;
    protected boolean allowStockAttachments;
    protected boolean allowGripAttachments;
    protected boolean allowGadgetAttachments;
    protected boolean allowSlideAttachments;
    protected boolean allowPumpAttachments;
    protected boolean allowAccessoryAttachments;
    /**
     * The number of generic attachment slots there are on this gun
     */
    protected int numGenericAttachmentSlots;

    //Modifiers
    /**
     * Speeds up or slows down player movement when this item is held
     */
    protected float moveSpeedModifier = 1F;
    /**
     * Gives knockback resistance to the player
     */
    protected float knockbackModifier;
    /**
     * Default spread of the gun. Do not modify.
     */
    protected float defaultSpread;
    /** Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg */
    @Getter
    protected float adsSpreadModifier = -1F;
    /** Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg. For shotguns. */
    @Getter
    protected float adsSpreadModifierShotgun = -1F;
    @Getter
    protected float switchDelay;
    protected boolean hasVariableZoom = false;
    protected float minZoom = 1F;
    protected float maxZoom = 4F;
    protected float zoomAugment = 1F;

    @Getter
    protected final GunAnim anim = new GunAnim();

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        //Damage
        damage = readValue("Damage", damage, file);
        meleeDamage = readValue("MeleeDamage", meleeDamage, file);
        meleeDamageDriveableModifier = readValue("MeleeDamageDriveableModifier", meleeDamageDriveableModifier, file);

        //Reload
        canForceReload = readValue("CanForceReload", canForceReload, file);
        reloadTime = readValue("ReloadTime", reloadTime, file);

        //Fire Rate
        shootDelay = readValue("ShootDelay", shootDelay, file);
        roundsPerMin = readValue("RoundsPerMin", roundsPerMin, file);

        //Accuracy
        bulletSpread = readValue("Accuracy", bulletSpread, file);
        bulletSpread = readValue("Spread", bulletSpread, file);
        spreadPattern = readValue("SpreadPattern", spreadPattern, EnumSpreadPattern.class, file);
        adsSpreadModifier = readValue("ADSSpreadModifier", adsSpreadModifier, file);
        adsSpreadModifierShotgun = readValue("ADSSpreadModifierShotgun", adsSpreadModifierShotgun, file);

        //Recoil
        recoilPitch = readValue("Recoil", recoilPitch, file);
        recoilCounterCoefficient = readValue("CounterRecoilForce", recoilCounterCoefficient, file);
        recoilCounterCoefficientSneaking = readValue("CounterRecoilForceSneaking", recoilCounterCoefficientSneaking, file);
        recoilCounterCoefficientSprinting = readValue("CounterRecoilForceSprinting", recoilCounterCoefficientSprinting, file);
        recoilYaw = readValue("RecoilYaw", recoilYaw, file);
        rndRecoilPitchRange = readValue("RandomRecoilRange", rndRecoilPitchRange, file);
        rndRecoilYawRange = readValue("RandomRecoilYawRange", rndRecoilYawRange, file);
        decreaseRecoilPitch = readValue("DecreaseRecoil", decreaseRecoilPitch, file);
        decreaseRecoilYaw = readValue("DecreaseRecoilYaw", decreaseRecoilYaw, file);
        recoilSneakingMultiplier = readValue("RecoilSneakingMultiplier", recoilSneakingMultiplier, file);
        recoilSprintingMultiplier = readValue("RecoilSprintingMultiplier", recoilSprintingMultiplier, file);
        recoilSneakingMultiplierYaw = readValue("RecoilSneakingMultiplierYaw", recoilSneakingMultiplierYaw, file);
        recoilSprintingMultiplierYaw = readValue("RecoilSprintingMultiplierYaw", recoilSprintingMultiplierYaw, file);
        readValues("FancyRecoil", file, 1).ifPresent(fancyRecoil -> {
            try
            {
                recoil.read(fancyRecoil);
                useFancyRecoil = true;
            }
            catch (Exception ex)
            {
                useFancyRecoil = false;
                logError("Failed to read fancy recoil", file);
            }
        });

        //TODO: read fancy recoil
        /*String[] aSplit = ConfigUtils.getSplitFromKey(config, "FancyRecoil");
        try {
            if (aSplit != null && aSplit.length > 1) {
                recoil.read(aSplit);
                useFancyRecoil = true;
            }
        } catch (Exception ex) {
            useFancyRecoil = false;
            FlansMod.logPackError(file.name, packName, shortName, "Failed to read fancy recoil", aSplit, ex);
        }*/

        //Ammo
        numBullets = readValue("NumBullets", numBullets, file);
        numPrimaryAmmoItems = readValue("NumAmmoSlots", numPrimaryAmmoItems, file);
        numPrimaryAmmoItems = readValue("NumAmmoItemsInGun", numPrimaryAmmoItems, file);
        numPrimaryAmmoItems = readValue("LoadIntoGun", numPrimaryAmmoItems, file);
        numBurstRounds = readValue("NumBurstRounds", numBurstRounds, file);
        allowSpreadByBullet = readValue("AllowSpreadByBullet", allowSpreadByBullet, file);
        allowNumBulletsByBulletType = readValue("AllowNumBulletsByBulletType", allowNumBulletsByBulletType, file);
        bulletSpeed = readValue("BulletSpeed", StringUtils.EMPTY, file).equalsIgnoreCase("instant") ? 0F : readValue("BulletSpeed", bulletSpeed, file);
        muzzleVelocity = readValue("MuzzleVelocity", muzzleVelocity, file);
        if (muzzleVelocity > 0F)
            bulletSpeed = muzzleVelocity / 20F;
        readLines("Ammo", file).ifPresent(lines -> lines.forEach(ammoLine -> ammo.add(ResourceUtils.sanitize(ammoLine))));

        //Lock on settings
        canLockOnAngle = readValue("CanLockAngle", canLockOnAngle, file);
        lockOnToDriveables = readValue("LockOnToDriveables", lockOnToDriveables, file);
        lockOnToVehicles = readValue("LockOnToVehicles", lockOnToVehicles, file);
        lockOnToPlanes = readValue("LockOnToPlanes", lockOnToPlanes, file);
        lockOnToMechas = readValue("LockOnToMechas", lockOnToMechas, file);
        lockOnToPlayers = readValue("LockOnToPlayers", lockOnToPlayers, file);
        lockOnToLivings = readValue("LockOnToLivings", lockOnToLivings, file);
        maxRangeLockOn = readValue("MaxRangeLockOn", maxRangeLockOn, file);

        //Other settings
        knockback = readValue("Knockback", knockback, file);
        sneakSpreadModifier = readValue("SneakSpreadModifier", sneakSpreadModifier, file);
        sneakSpreadModifier = readValue("SneakSpreadMultiplier", sneakSpreadModifier, file);
        sprintSpreadModifier = readValue("SprintSpreadModifier", sprintSpreadModifier, file);
        sprintSpreadModifier = readValue("SprintSpreadMultiplier", sprintSpreadModifier, file);
        allowRearm = readValue("AllowRearm", allowRearm, file);
        consumeGunUponUse = readValue("ConsumeGunOnUse", consumeGunUponUse, file);
        showCrosshair = readValue("ShowCrosshair", showCrosshair, file);
        dropItemOnShoot = readValue("DropItemOnShoot", dropItemOnShoot, file);
        minigunStartSpeed = readValue("MinigunStartSpeed", minigunStartSpeed, file);
        canShootUnderwater = readValue("CanShootUnderwater", canShootUnderwater, file);
        canSetPosition = readValue("CanSetPosition", canSetPosition, file);
        oneHanded = readValue("OneHanded", oneHanded, file);
        usableByPlayers = readValue("UsableByPlayers", usableByPlayers, file);
        usableByMechas = readValue("UsableByMechas", usableByMechas, file);
        standBackDist = readValue("StandBackDistance", standBackDist, file);
        topViewLimit = readValue("TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue("BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue("SideViewLimit", sideViewLimit, file);
        pivotHeight = readValue("PivotHeight", pivotHeight, file);
        itemUseAction = readValue("ItemUseAction", itemUseAction, UseAnim.class, file);
        // This is needed, because the presence of the value overrides the default value of zero.
        if (file.hasConfigLine("HipFireWhileSprinting"))
            hipFireWhileSprinting = readValue("HipFireWhileSprinting", false, file) ? 1 : 2;

        // Melee
        meleeTime = readValue("MeleeTime", meleeTime, file);
        readFloatValues("AddNode", file, 6).ifPresent(values -> {
            meleePath.add(new Vector3f(values[0] / 16F, values[1] / 16F, values[2] / 16F));
            meleePathAngles.add(new Vector3f(values[3], values[4], values[5]));
        });
        readFloatValues("MeleeDamagePoint", file, 3)
                .ifPresent(values -> meleeDamagePoints.add(new Vector3f(values[0] / 16F, values[1] / 16F, values[2] / 16F)));
        readFloatValues("MeleeDamageOffset", file, 3)
                .ifPresent(values -> meleeDamagePoints.add(new Vector3f(values[0] / 16F, values[1] / 16F, values[2] / 16F)));

        //Player modifiers
        moveSpeedModifier = readValue("MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue("Slowness", moveSpeedModifier, file);
        knockbackModifier = readValue("KnockbackReduction", knockbackModifier, file);
        knockbackModifier = readValue("KnockbackModifier", knockbackModifier, file);
        switchDelay = readValue("SwitchDelay", switchDelay, file);

        //Information
        showAttachments = readValue("ShowAttachments", showAttachments, file);
        showDamage = readValue("ShowDamage", showDamage, file);
        showRecoil = readValue("ShowRecoil", showRecoil, file);
        showSpread = readValue("ShowAccuracy", showSpread, file);
        showReloadTime = readValue("ShowReloadTime", showReloadTime, file);

        //Sounds
        distortSound = readValue("DistortSound", distortSound, file);
        shootSoundLength = readValue("SoundLength", shootSoundLength, file);
        idleSoundLength = readValue("IdleSoundLength", idleSoundLength, file);
        warmupSoundLength = readValue("WarmupSoundLength", warmupSoundLength, file);
        idleSoundRange = readValue("IdleSoundRange", idleSoundRange, file);
        meleeSoundRange = readValue("MeleeSoundRange", meleeSoundRange, file);
        reloadSoundRange = readValue("ReloadSoundRange", reloadSoundRange, file);
        gunSoundRange = readValue("GunSoundRange", gunSoundRange, file);
        shootSound = readSound("ShootSound", shootSound, file);
        bulletInsert = readSound("BulletInsertSound", bulletInsert, file);
        actionSound = readSound("ActionSound", actionSound, file);
        lastShootSound = readSound("LastShootSound", lastShootSound, file);
        suppressedShootSound = readSound("SuppressedShootSound", suppressedShootSound, file);
        lastShootSoundSuppressed = readSound("LastSuppressedShootSound", lastShootSoundSuppressed, file);
        reloadSound = readSound("ReloadSound", reloadSound, file);
        reloadSoundOnEmpty = readSound("EmptyReloadSound", reloadSoundOnEmpty, file);
        clickSoundOnEmpty = readSound("EmptyClickSound", clickSoundOnEmpty, file);
        clickSoundOnEmptyRepeated = readSound("EmptyClickSoundRepeated", clickSoundOnEmptyRepeated, file);
        idleSound = readSound("IdleSound", idleSound, file);
        meleeSound = readSound("MeleeSound", meleeSound, file);

        //Looping sounds
        warmupSound = readValue("WarmupSound", warmupSound, file);
        loopedSound = readValue("LoopedSound", loopedSound, file);
        loopedSound = readValue("SpinSound", loopedSound, file);
        cooldownSound = readValue("CooldownSound", cooldownSound, file);
        lockOnSound = readSound("LockOnSound", lockOnSound, file);
        distantShootSound = readSound("DistantSound", distantShootSound, file);
        distantShootSound = readSound("DistantShootSound", distantShootSound, file);
        loopedSoundLength = readValue("LoopedSoundLength", loopedSoundLength, file);
        loopedSoundLength = readValue("SpinSoundLength", loopedSoundLength, file);
        lockOnSoundTime = readValue("LockOnSoundTime", lockOnSoundTime, file);
        distantSoundRange = readValue("DistantSoundRange", distantSoundRange, file);
        useLoopingSounds = StringUtils.isNotBlank(loopedSound);

        //Mode
        mode = readValue("Mode", mode, EnumFireMode.class, file);
        defaultmode = mode;
        String[] submodeSplit = readValues("Mode", file);
        if (submodeSplit.length > 0)
        {
            submode = new EnumFireMode[submodeSplit.length - 1];
            for (int i = 0; i < submode.length; i++)
                submode[i] = EnumFireMode.getFireMode(submodeSplit[i + 1]);
        }

        //Overlay and zoom settings
        overlayName = readResource("Scope", overlayName, file);
        if (overlayName.equals("none"))
            overlayName = StringUtils.EMPTY;
        zoomFactor = readValue("ZoomLevel", zoomFactor, file);
        fovFactor = readValue("FOVZoomLevel", fovFactor, file);
        allowNightVision = readValue("AllowNightVision", allowNightVision, file);
        hasVariableZoom = readValue("HasVariableZoom", hasVariableZoom, file);
        minZoom = readValue("MinZoom", minZoom, file);
        maxZoom = readValue("MaxZoom", maxZoom, file);
        zoomAugment = readValue("ZoomAugment", zoomAugment, file);
        if (maxZoom > 1F && hasVariableZoom)
            secondaryFunction = EnumSecondaryFunction.ZOOM;

        //Models & Textures
        deployable = readValue("Deployable", deployable, file);
        deployableModelName = readResource("DeployedModel", deployableModelName, file);
        deployableTextureName = readResource("DeployedTexture", deployableTextureName, file);
        casingModelName = readResource("CasingModel", casingModelName, file);
        casingTextureName = readResource("CasingTexture", casingTextureName, file);
        flashModelName = readResource("FlashModel", flashModelName, file);
        flashTextureName = readResource("FlashTexture", flashTextureName, file);
        muzzleFlashModelName = readResource("MuzzleFlashModel", muzzleFlashModelName, file);
        hitTextureName = readResource("HitTexture", hitTextureName, file);

        //Particles
        muzzleFlashParticle = readValue("MuzzleFlashParticle", muzzleFlashParticle, file);
        muzzleFlashParticleSize = readValue("MuzzleFlashParticleSize", muzzleFlashParticleSize, file);
        showMuzzleFlashParticlesFirstPerson = readValue("ShowMuzzleFlashParticleFirstPerson", showMuzzleFlashParticlesFirstPerson, file);
        muzzleFlashParticlesShoulderOffset = readVector("MuzzleFlashParticleShoulderOffset", muzzleFlashParticlesShoulderOffset, file);
        muzzleFlashParticlesHandOffset = readVector("MuzzleFlashParticleHandOffset", muzzleFlashParticlesHandOffset, file);
        showMuzzleFlashParticles = readValue("ShowMuzzleFlashParticle", showMuzzleFlashParticles, file);
        useMuzzleFlashDefaults = file.hasConfigLine("ShowMuzzleFlashParticle");

        //Attachment settings
        allowAllAttachments = readValue("AllowAllAttachments", allowAllAttachments, file);
        if (hasValueForConfigField("AllowAttachments", file))
            readValuesToList("AllowAttachments", file).forEach(attachment -> allowedAttachments.add(AttachmentType.getAttachment(ResourceUtils.sanitize(attachment))));

        allowBarrelAttachments = readValue("AllowBarrelAttachments", allowBarrelAttachments, file);
        allowScopeAttachments = readValue("AllowScopeAttachments", allowScopeAttachments, file);
        allowStockAttachments = readValue("AllowStockAttachments", allowStockAttachments, file);
        allowGripAttachments = readValue("AllowGripAttachments", allowGripAttachments, file);
        allowGadgetAttachments = readValue("AllowGadgetAttachments", allowGadgetAttachments, file);
        allowSlideAttachments = readValue("AllowSlideAttachments", allowSlideAttachments, file);
        allowPumpAttachments = readValue("AllowPumpAttachments", allowPumpAttachments, file);
        allowAccessoryAttachments = readValue("AllowAccessoryAttachments", allowAccessoryAttachments, file);
        numGenericAttachmentSlots = readValue("NumGenericAttachmentSlots", numGenericAttachmentSlots, file);

        //Shield settings
        readFloatValues("Shield", file, 7).ifPresent(values -> {
            shield = true;
            shieldDamageAbsorption = values[0];
            shieldOrigin = new Vector3f(values[1] / 16F, values[2] / 16F, values[3] / 16F);
            shieldDimensions = new Vector3f(values[4] / 16F, values[5] / 16F, values[6] / 16F);
        });

        //Secondary Function
        if (readValue("UseCustomMeleeWhenShoot", false, file))
            secondaryFunctionWhenShoot = EnumSecondaryFunction.CUSTOM_MELEE;
        if (file.hasConfigLine("ZoomLevel") && zoomFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ZOOM;
        if (file.hasConfigLine("FOVZoomLevel") && fovFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;
        if (maxZoom > 1F && hasVariableZoom)
            secondaryFunction = EnumSecondaryFunction.ZOOM;
        if (file.hasConfigLine("MeleeDamage") && meleeDamage > 0F && StringUtils.isBlank(overlayName))
            secondaryFunction = EnumSecondaryFunction.MELEE;
        if (readFieldWithOptionalValue("UseCustomMelee", false, file))
            secondaryFunction = EnumSecondaryFunction.CUSTOM_MELEE;
        secondaryFunction = EnumSecondaryFunction.get(readValue("SecondaryFunction", secondaryFunction.toString(), file));

        defaultSpread = bulletSpread;
        recoilYaw /= 10F;
        decreaseRecoilYaw = (decreaseRecoilYaw > 0F) ? decreaseRecoilYaw : 0.5F;

        if (lockOnToDriveables)
        {
            lockOnToPlanes = true;
            lockOnToVehicles = true;
            lockOnToMechas = true;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void readClient(TypeFile file)
    {
        super.readClient(file);

        deployableModelClassName = findModelClass(deployableModelName, contentPack);
        deployableTexture = loadTexture(deployableTextureName, this);
        casingModelClassName = findModelClass(casingModelName, contentPack);
        casingTexture = loadTexture(casingTextureName, this);
        flashModelClassName = findModelClass(flashModelName, contentPack);
        flashTexture = loadTexture(flashTextureName, this);
        muzzleFlashModelClassName = findModelClass(muzzleFlashModelName, contentPack);

        anim.read(file);
    }

    public Optional<ShootableType> getDefaultAmmo()
    {
        if (!ammo.isEmpty()) {
            return ShootableType.getAmmoType(ammo.iterator().next(), contentPack);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasZoomOverlay()
    {
        return getOverlay().isPresent();
    }

    @Override
    public ResourceLocation getZoomOverlay()
    {
        return Optional.ofNullable(overlay).orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);
    }

    public List<ShootableType> getAmmoTypes()
    {
        return ShootableType.getAmmoTypes(ammo, contentPack);
    }

    /**
     * Return the currently active scope on this gun. Search attachments, and by default, simply give the gun
     */
    @NotNull
    public IScope getCurrentScope(ItemStack gunStack)
    {
        IScope attachedScope = getScope(gunStack);
        return attachedScope == null ? this : attachedScope;
    }

    /**
     * Returns all attachments currently attached to the specified gun
     */
    public List<AttachmentType> getCurrentAttachments(ItemStack gun)
    {
        checkForTags(gun);
        List<AttachmentType> attachments = new ArrayList<>();
        for (int i = 0; i < numGenericAttachmentSlots; i++)
        {
            appendToList(gun, NBT_GENERIC + i, attachments);
        }
        appendToList(gun, NBT_BARREL, attachments);
        appendToList(gun, NBT_SCOPE, attachments);
        appendToList(gun, NBT_STOCK, attachments);
        appendToList(gun, NBT_GRIP, attachments);
        appendToList(gun, NBT_GADGET, attachments);
        appendToList(gun, NBT_SLIDE, attachments);
        appendToList(gun, NBT_PUMP, attachments);
        appendToList(gun, NBT_ACCESSORY, attachments);
        return attachments;
    }

    /**
     * Private method for attaching attachments to a list of attachments with a null check
     */
    private void appendToList(ItemStack gun, String name, List<AttachmentType> attachments)
    {
        AttachmentType type = getAttachment(gun, name);
        if (type != null)
            attachments.add(type);
    }

    //Attachment getter methods
    public AttachmentType getBarrel(ItemStack gun)
    {
        return getAttachment(gun, NBT_BARREL);
    }

    public AttachmentType getScope(ItemStack gun)
    {
        return getAttachment(gun, NBT_SCOPE);
    }

    public AttachmentType getStock(ItemStack gun)
    {
        return getAttachment(gun, NBT_STOCK);
    }

    public AttachmentType getGrip(ItemStack gun)
    {
        return getAttachment(gun, NBT_GRIP);
    }

    public AttachmentType getGadget(ItemStack gun) {
        return getAttachment(gun, NBT_GADGET);
    }

    public AttachmentType getSlide(ItemStack gun) {
        return getAttachment(gun, NBT_SLIDE);
    }

    public AttachmentType getPump(ItemStack gun) {
        return getAttachment(gun, NBT_PUMP);
    }

    public AttachmentType getAccessory(ItemStack gun) {
        return getAttachment(gun, NBT_ACCESSORY);
    }

    public AttachmentType getGeneric(ItemStack gun, int i)
    {
        return getAttachment(gun, NBT_GENERIC + i);
    }

    //Attachment ItemStack getter methods
    public ItemStack getBarrelItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, NBT_BARREL);
    }

    public ItemStack getScopeItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, NBT_SCOPE);
    }

    public ItemStack getStockItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, NBT_STOCK);
    }

    public ItemStack getGripItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, NBT_GRIP);
    }

    public ItemStack getGadgetItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, NBT_GADGET);
    }

    public ItemStack getSlideItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, NBT_SLIDE);
    }

    public ItemStack getPumpItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, NBT_PUMP);
    }

    public ItemStack getAccessoryItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, NBT_ACCESSORY);
    }

    public ItemStack getGenericItemStack(ItemStack gun, int i)
    {
        return getAttachmentItemStack(gun, NBT_GENERIC + i);
    }

    /**
     * Generalised attachment getter method
     */
    public AttachmentType getAttachment(ItemStack gun, String name)
    {
        checkForTags(gun);
        CompoundTag tag = Objects.requireNonNull(gun.getTag()); // non-null because checkForTags used getOrCreateTag()
        CompoundTag attachments = tag.getCompound(NBT_ATTACHMENTS);
        CompoundTag data = attachments.getCompound(name);
        return AttachmentType.getFromNBT(data);
    }

    /**
     * Generalised attachment ItemStack getter method
     */
    public ItemStack getAttachmentItemStack(ItemStack gun, String name)
    {
        checkForTags(gun);
        CompoundTag tag = Objects.requireNonNull(gun.getTag());
        CompoundTag attachments = tag.getCompound(NBT_ATTACHMENTS);
        CompoundTag stackTag = attachments.getCompound(name);
        return ItemStack.of(stackTag); // Empty tag -> ItemStack.EMPTY
    }

    /**
     * Method to check for null tags and assign default empty tags in that case
     */
    public void checkForTags(ItemStack gun)
    {
        // Ensure the root tag exists
        CompoundTag tag = gun.getOrCreateTag();

        // If there's no "attachments" compound, create and populate it
        if (!tag.contains(NBT_ATTACHMENTS, Tag.TAG_COMPOUND))
        {
            CompoundTag attachments = new CompoundTag();

            for (int i = 0; i < numGenericAttachmentSlots; i++)
                attachments.put(NBT_GENERIC + i, new CompoundTag());

            attachments.put(NBT_BARREL, new CompoundTag());
            attachments.put(NBT_SCOPE, new CompoundTag());
            attachments.put(NBT_STOCK, new CompoundTag());
            attachments.put(NBT_GRIP, new CompoundTag());
            attachments.put(NBT_GADGET, new CompoundTag());
            attachments.put(NBT_SLIDE, new CompoundTag());
            attachments.put(NBT_PUMP, new CompoundTag());
            attachments.put(NBT_ACCESSORY, new CompoundTag());

            tag.put(NBT_ATTACHMENTS, attachments);
        }
    }

    public String getReloadSound(ItemStack stack)
    {
        if (getSecondaryFire(stack) && getGrip(stack) != null && StringUtils.isNotBlank(getGrip(stack).secondaryReloadSound))
            return getGrip(stack).secondaryReloadSound;
        else if (StringUtils.isNotBlank(reloadSoundOnEmpty))
            return reloadSoundOnEmpty;
        return reloadSound;
    }

    /**
     * Get the melee damage of a specific gun, taking into account attachments
     */
    public float getMeleeDamage(ItemStack stack, boolean driveable)
    {
        float stackMeleeDamage = meleeDamage;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackMeleeDamage *= attachment.meleeDamageMultiplier;

        return stackMeleeDamage * (driveable ? meleeDamageDriveableModifier : 1F);
    }

    /**
     * Get the damage of a specific gun, taking into account attachments
     */
    public float getDamage(ItemStack stack)
    {
        float stackDamage = damage;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackDamage = getGrip(stack).secondaryDamage;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackDamage *= attachment.damageMultiplier;

        return (float) (stackDamage * ModCommonConfigs.gunDamageModifier.get());
    }

    /**
     * Get the bullet spread of a specific gun, taking into account attachments
     */
    public float getSpread(ItemStack stack, boolean sneaking, boolean sprinting)
    {
        float stackSpread = bulletSpread;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackSpread = getGrip(stack).secondarySpread;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackSpread *= attachment.spreadMultiplier;

        if (sprinting)
            stackSpread *= sprintSpreadModifier;
        else if (sneaking)
            stackSpread *= sneakSpreadModifier;

        return stackSpread;
    }

    /**
     * Get the default spread of a specific gun, taking into account attachments
     */
    public float getDefaultSpread(ItemStack stack)
    {
        float stackSpread = defaultSpread;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackSpread *= attachment.spreadMultiplier;

        return stackSpread;
    }

    public EnumSpreadPattern getSpreadPattern(ItemStack stack)
    {
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (attachment.spreadPattern != null)
                return attachment.spreadPattern;
        }
        return spreadPattern;
    }

    /**
     * Get the pitch recoil of a specific gun, taking into account attachments, randomess and sneak/sprint
     */
    public float getRecoilPitch(ItemStack stack, boolean sneaking, boolean sprinting)
    {
        float stackRecoil = recoilPitch + (rand.nextFloat() * rndRecoilYawRange);

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackRecoil *= attachment.recoilMultiplier;

        if (sneaking)
        {
            if (decreaseRecoilPitch != 0)
            {
                // backwards compatibility
                stackRecoil -= decreaseRecoilPitch;
            }
            else if (recoilSneakingMultiplier == -1)
            {
                // backwards compatibility 2: simulate decreaseRecoilPitch 2
                stackRecoil = stackRecoil < 0.5F ? 0 : stackRecoil - 0.5F;
            }
            else
            {
                stackRecoil *= recoilSneakingMultiplier;
            }
        }
        else if (sprinting)
        {
            stackRecoil *= recoilSprintingMultiplier;
        }

        return (float) (stackRecoil * ModCommonConfigs.gunRecoilModifier.get());
    }

    /**
     * Get the yaw recoil of a specific gun, taking into account attachments, randomess and sneak/sprint
     */
    public float getRecoilYaw(ItemStack stack, boolean sneaking, boolean sprinting)
    {
        float stackRecoilYaw = recoilYaw + ((rand.nextFloat() - 0.5F) * rndRecoilYawRange);

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackRecoilYaw *= attachment.recoilMultiplier;

        if (sneaking)
        {
            if (decreaseRecoilYaw < 0)
            {
                stackRecoilYaw /= decreaseRecoilYaw;
            }
            else
            {
                stackRecoilYaw *= recoilSneakingMultiplierYaw;
            }
        }
        else if (sprinting)
        {
            stackRecoilYaw *= recoilSprintingMultiplierYaw;
        }

        return (float) (stackRecoilYaw * ModCommonConfigs.gunRecoilModifier.get());
    }

    public float getDisplayVerticalRecoil(ItemStack stack)
    {
        float stackRecoil = recoilPitch;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackRecoil *= attachment.recoilMultiplier;

        return (float) (stackRecoil * ModCommonConfigs.gunRecoilModifier.get());
    }

    public float getDisplayHorizontalRecoil(ItemStack stack)
    {
        float stackRecoilYaw = recoilYaw;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackRecoilYaw *= attachment.recoilMultiplier;

        return (float) (stackRecoilYaw * ModCommonConfigs.gunRecoilModifier.get());
    }

    /**
     * Get the bullet speed of a specific gun, taking into account attachments
     */
    public float getBulletSpeed(ItemStack stack, ItemStack bulletStack)
    {
        float stackBulletSpeed;

        if (bulletStack != null && bulletStack.getItem() instanceof BulletItem bulletItem)
            stackBulletSpeed = bulletSpeed * bulletItem.getConfigType().speedMultiplier;
        else
            stackBulletSpeed = bulletSpeed;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackBulletSpeed = getGrip(stack).secondarySpeed;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackBulletSpeed *= attachment.bulletSpeedMultiplier;

        return stackBulletSpeed;
    }

    /**
     * Get the bullet speed of a specific gun, taking into account attachments
     */
    public float getBulletSpeed(ItemStack stack)
    {
        float stackBulletSpeed = bulletSpeed;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackBulletSpeed = getGrip(stack).secondarySpeed;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackBulletSpeed *= attachment.bulletSpeedMultiplier;

        return stackBulletSpeed;
    }

    /**
     * Get the reload time of a specific gun, taking into account attachments
     */
    public float getReloadTime(ItemStack stack)
    {
        float stackReloadTime = reloadTime;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackReloadTime = getGrip(stack).secondaryReloadTime;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackReloadTime *= attachment.reloadTimeMultiplier;

        return stackReloadTime;
    }

    /**
     * Get the fire rate of a specific gun
     */
    public float getShootDelay(ItemStack stack)
    {
        float fireRate;
        if (getGrip(stack) != null && getSecondaryFire(stack))
            fireRate = getGrip(stack).secondaryShootDelay;
        else if (roundsPerMin != 0F)
            fireRate = 1200F / roundsPerMin;
        else if (shootDelay != 0F)
            fireRate = shootDelay;
        else
            fireRate = DEFAULT_SHOOT_DELAY;


        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (attachment.modeOverride == EnumFireMode.BURST)
                return Math.max(fireRate, 3F);
        }

        float stackShootDelay = fireRate;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackShootDelay *= attachment.shootDelayMultiplier;
        }
        return stackShootDelay;
    }

    public float getShootDelay()
    {
        if (roundsPerMin != 0F)
            return 1200F / roundsPerMin;
        else if (shootDelay != 0F)
            return shootDelay;
        else
            return DEFAULT_SHOOT_DELAY;
    }

    /**
     * Get the number of bullets fired per shot of a specific gun
     */
    public int getNumBullets(ItemStack stack)
    {
        int amount = numBullets;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            amount = getGrip(stack).secondaryNumBullets;

        return amount;
    }

    /**
     * Get the movement speed of a specific gun, taking into account attachments
     */
    public float getMovementSpeed(ItemStack stack)
    {
        float stackMovement = moveSpeedModifier;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackMovement *= attachment.moveSpeedMultiplier;

        return stackMovement;
    }

    /**
     * Get the recoil counter coefficient of the gun, taking into account attachments
     */
    public float getRecoilControl(ItemStack stack, boolean isSprinting, boolean isSneaking)
    {
        float control;

        if (isSprinting)
            control = recoilCounterCoefficientSprinting;
        else if (isSneaking)
            control = recoilCounterCoefficientSneaking;
        else
            control = recoilCounterCoefficient;

        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (isSprinting)
                control *= attachment.recoilControlMultiplierSprinting;
            else if (isSneaking)
                control *= attachment.recoilControlMultiplierSneaking;
            else
                control *= attachment.recoilControlMultiplier;
        }

        return Math.max(0, Math.min(1, control));
    }

    /**
     * Set the secondary or primary fire mode
     */
    public void setSecondaryFire(ItemStack stack, boolean mode)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(NBT_SECONDARY_FIRE, mode);
    }

    /**
     * Get whether the gun is in secondary or primary fire mode
     */
    public boolean getSecondaryFire(ItemStack stack)
    {
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains(NBT_SECONDARY_FIRE))
        {
            tag.putBoolean(NBT_SECONDARY_FIRE, false);
            return false;
        }

        return tag.getBoolean(NBT_SECONDARY_FIRE);
    }

    /**
     * Get the max size of ammo items depending on what mode the gun is in
     */
    public int getNumAmmoItemsInGun(ItemStack stack)
    {
        if (getGrip(stack) != null && getSecondaryFire(stack))
            return getGrip(stack).numSecAmmoItems;
        else
            return numPrimaryAmmoItems;
    }

    public void setFireMode(ItemStack stack, EnumFireMode mode)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_GUN_MODE, mode.name());
    }

    /**
     * Get the firing mode of a specific gun, taking into account attachments and secondary fire mode
     */
    public EnumFireMode getFireMode(ItemStack stack)
    {
        // Check for secondary fire mode
        if (getGrip(stack) != null && getSecondaryFire(stack))
            return getGrip(stack).secondaryFireMode;

        // Check for any mode overrides from attachments
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (attachment.modeOverride != null)
                return attachment.modeOverride;
        }

        // Set the fire mode from the gun stack
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_GUN_MODE))
        {
            EnumFireMode stored = EnumFireMode.getFireMode(tag.getString(NBT_GUN_MODE));
            for (EnumFireMode allowed : submode)
            {
                if (allowed == stored)
                    return stored;
            }
        }

        // Reset fire mode to default for the gun stack
        setFireMode(stack, mode);
        return mode;
    }
}
