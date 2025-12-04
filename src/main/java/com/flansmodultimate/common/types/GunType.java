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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

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
     * Modifier for setting the maximum pitch divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     */
    protected float rndRecoilPitchRange = 0.5F;
    /**
     * Modifier for setting the maximum yaw divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
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
    protected Set<String> ammo = new HashSet<>();
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
    protected EnumFireMode mode = EnumFireMode.FULLAUTO;
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
    protected boolean useCustomMeleeWhenShoot;
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
    protected int canLockOnAngle = 5;
    protected int lockOnSoundTime;
    protected String lockOnSound = StringUtils.EMPTY;
    protected int maxRangeLockOn = 80;
    protected boolean canSetPosition;
    /**
     * Determines what the launcher can lock on to
     */
    protected boolean lockOnToDriveables;
    protected boolean lockOnToPlanes;
    protected boolean lockOnToVehicles;
    protected boolean lockOnToMechas;
    protected boolean lockOnToPlayers;
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
    protected int shootSoundLength;
    /**
     * Whether to distort the sound or not. Generally only set to false for looping sounds
     */
    @Getter
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
    protected int idleSoundRange = 50;
    /**
     * The block range for melee sounds
     */
    protected int meleeSoundRange = 50;
    /**
     * The block range for reload sounds
     */
    protected int reloadSoundRange = 50;
    /**
     * The block range for gunshots sounds
     */
    protected int gunSoundRange = 50;

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
    protected boolean allowNightVision = false;

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
    protected String hitTextureName;

    protected String muzzleFlashParticle = ParticleHelper.FM_MUZZLE_FLASH;
    protected float muzzleFlashParticleSize = 1F;
    protected Boolean useMuzzleFlashDefaults = true;
    protected Boolean showMuzzleFlashParticles = true;
    protected Boolean showMuzzleFlashParticlesFirstPerson;
    protected Vector3f muzzleFlashParticlesHandOffset = new Vector3f();
    protected Vector3f muzzleFlashParticlesShoulderOffset = new Vector3f();

    //Attachment settings
    /**
     * If this is true, then all attachments are allowed. Otherwise, the list is checked.
     */
    protected boolean allowAllAttachments = true;
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
    protected float adsSpreadModifier = -1F;
    /** Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg. For shotguns. */
    protected float adsSpreadModifierShotgun = -1F;
    @Getter
    protected float switchDelay;
    protected boolean hasVariableZoom = false;
    protected float minZoom = 1F;
    protected float maxZoom = 4F;
    protected float zoomAugment = 1F;

    //TODO: remove these variables from the GunType -> move into a record class
    /** For shotgun pump handles and rifle bolts */
    @Getter @Setter
    protected int pumpDelay;
    @Getter @Setter
    protected int pumpDelayAfterReload;
    @Getter @Setter
    protected int pumpTime = 1;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        //TODO: ideally replace all length values by readValues

        //Damage
        damage = readValue(split, "Damage", damage, file);
        meleeDamage = readValue(split, "MeleeDamage", meleeDamage, file);
        //TODO: secondary Function priority?
        if (split[0].equalsIgnoreCase("MeleeDamage") && meleeDamage > 0F)
            secondaryFunction = EnumSecondaryFunction.MELEE; // !hasZoomOverlay() ? EnumSecondaryFunction.MELEE : secondaryFunction
        meleeDamageDriveableModifier = readValue(split, "MeleeDamageDriveableModifier", meleeDamageDriveableModifier, file);

        //Reload
        canForceReload = readValue(split, "CanForceReload", canForceReload, file);
        reloadTime = readValue(split, "ReloadTime", reloadTime, file);

        //Fire Rate
        shootDelay = readValue(split, "ShootDelay", shootDelay, file);
        roundsPerMin = readValue(split, "RoundsPerMin", roundsPerMin, file);

        //Accuracy
        bulletSpread = readValue(split, "Accuracy", bulletSpread, file);
        bulletSpread = readValue(split, "Spread", bulletSpread, file);
        spreadPattern = readValue(split, "SpreadPattern", spreadPattern, EnumSpreadPattern.class, file);
        adsSpreadModifier = readValue(split, "ADSSpreadModifier", adsSpreadModifier, file);
        adsSpreadModifierShotgun = readValue(split, "ADSSpreadModifierShotgun", adsSpreadModifierShotgun, file);

        //Recoil
        recoilPitch = readValue(split, "Recoil", recoilPitch, file);
        recoilCounterCoefficient = readValue(split, "CounterRecoilForce", recoilCounterCoefficient, file);
        recoilCounterCoefficientSneaking = readValue(split, "CounterRecoilForceSneaking", recoilCounterCoefficientSneaking, file);
        recoilCounterCoefficientSprinting = readValue(split, "CounterRecoilForceSprinting", recoilCounterCoefficientSprinting, file);
        recoilYaw = readValue(split, "RecoilYaw", recoilYaw, file);
        rndRecoilPitchRange = readValue(split, "RandomRecoilRange", rndRecoilPitchRange, file);
        rndRecoilYawRange = readValue(split, "RandomRecoilYawRange", rndRecoilYawRange, file);
        decreaseRecoilPitch = readValue(split, "DecreaseRecoil", decreaseRecoilPitch, file);
        decreaseRecoilYaw = readValue(split, "DecreaseRecoilYaw", decreaseRecoilYaw, file);
        recoilSneakingMultiplier = readValue(split, "RecoilSneakingMultiplier", recoilSneakingMultiplier, file);
        recoilSprintingMultiplier = readValue(split, "RecoilSprintingMultiplier", recoilSprintingMultiplier, file);
        recoilSneakingMultiplierYaw = readValue(split, "RecoilSneakingMultiplierYaw", recoilSneakingMultiplierYaw, file);
        recoilSprintingMultiplierYaw = readValue(split, "RecoilSprintingMultiplierYaw", recoilSprintingMultiplierYaw, file);
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
        numBullets = readValue(split, "NumBullets", numBullets, file);
        numPrimaryAmmoItems = readValue(split, "NumAmmoSlots", numPrimaryAmmoItems, file);
        numPrimaryAmmoItems = readValue(split, "NumAmmoItemsInGun", numPrimaryAmmoItems, file);
        numPrimaryAmmoItems = readValue(split, "LoadIntoGun", numPrimaryAmmoItems, file);
        numBurstRounds = readValue(split, "NumBurstRounds", numBurstRounds, file);
        allowSpreadByBullet = readValue(split, "AllowSpreadByBullet", allowSpreadByBullet, file);
        allowNumBulletsByBulletType = readValue(split, "AllowNumBulletsByBulletType", allowNumBulletsByBulletType, file);
        if (split[0].equalsIgnoreCase("BulletSpeed"))
        {
            if (split.length > 1 && split[1].equalsIgnoreCase("instant"))
                bulletSpeed = 0F;
            else
                bulletSpeed = readValue(split, "BulletSpeed", bulletSpeed, file);
        }
        if (split[0].equals("Ammo") && split.length > 1)
        {
            ammo.add(ResourceUtils.sanitize(split[1]));
        }

        //Lock on settings
        canLockOnAngle = readValue(split, "CanLockAngle", canLockOnAngle, file);
        lockOnToDriveables = readValue(split, "LockOnToDriveables", lockOnToDriveables, file);
        lockOnToVehicles = readValue(split, "LockOnToVehicles", lockOnToVehicles, file);
        lockOnToPlanes = readValue(split, "LockOnToPlanes", lockOnToPlanes, file);
        lockOnToMechas = readValue(split, "LockOnToMechas", lockOnToMechas, file);
        lockOnToPlayers = readValue(split, "LockOnToPlayers", lockOnToPlayers, file);
        lockOnToLivings = readValue(split, "LockOnToLivings", lockOnToLivings, file);
        maxRangeLockOn = readValue(split, "MaxRangeLockOn", maxRangeLockOn, file);

        //Other settings
        knockback = readValue(split, "Knockback", knockback, file);
        sneakSpreadModifier = readValue(split, "SneakSpreadModifier", sneakSpreadModifier, file);
        sneakSpreadModifier = readValue(split, "SneakSpreadMultiplier", sneakSpreadModifier, file);
        sprintSpreadModifier = readValue(split, "SprintSpreadModifier", sprintSpreadModifier, file);
        sprintSpreadModifier = readValue(split, "SprintSpreadMultiplier", sprintSpreadModifier, file);
        allowRearm = readValue(split, "AllowRearm", allowRearm, file);
        consumeGunUponUse = readValue(split, "ConsumeGunOnUse", consumeGunUponUse, file);
        showCrosshair = readValue(split, "ShowCrosshair", showCrosshair, file);
        dropItemOnShoot = readValue(split, "DropItemOnShoot", dropItemOnShoot, file);
        minigunStartSpeed = readValue(split, "MinigunStartSpeed", minigunStartSpeed, file);
        canShootUnderwater = readValue(split, "CanShootUnderwater", canShootUnderwater, file);
        canSetPosition = readValue(split, "CanSetPosition", canSetPosition, file);
        oneHanded = readValue(split, "OneHanded", oneHanded, file);
        usableByPlayers = readValue(split, "UsableByPlayers", usableByPlayers, file);
        usableByMechas = readValue(split, "UsableByMechas", usableByMechas, file);
        standBackDist = readValue(split, "StandBackDistance", standBackDist, file);
        topViewLimit = readValue(split, "TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue(split, "BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue(split, "SideViewLimit", sideViewLimit, file);
        pivotHeight = readValue(split, "PivotHeight", pivotHeight, file);
        //TODO: implement this
        /*String line = ConfigUtils.configString(config, "ItemUseAction", null);
        try {
            if (line != null) {
                itemUseAction = EnumAction.valueOf(line.toLowerCase());
            }
        } catch (Exception ex) {
            FlansMod.logPackError(file.name, packName, shortName, "ItemUseAction not recognised in gun", new String[]{"ItemUseAction", line}, ex);
        }
        // This is needed, because the presence of the value overrides the default value of zero.
        if (config.containsKey("HipFireWhileSprinting"))
            hipFireWhileSprinting = ConfigUtils.configBool(config, "HipFireWhileSprinting", false) ? 1 : 2;*/

        //Secondary Function & Melee
        if (split[0].equalsIgnoreCase("SecondaryFunction"))
            secondaryFunction = EnumSecondaryFunction.get(split[1]);
        if (split[0].equalsIgnoreCase("UseCustomMelee") && split.length > 1 && Boolean.parseBoolean(split[1]))
            secondaryFunction = EnumSecondaryFunction.CUSTOM_MELEE;
        meleeTime = readValue(split, "MeleeTime", meleeTime, file);
        if (split[0].equalsIgnoreCase("AddNode") && split.length > 6)
        {
            meleePath.add(new Vector3f(Float.parseFloat(split[1]) / 16F, Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F));
            meleePathAngles.add(new Vector3f(Float.parseFloat(split[4]), Float.parseFloat(split[5]), Float.parseFloat(split[6])));
        }
        if ((split[0].equalsIgnoreCase("MeleeDamagePoint") || split[0].equalsIgnoreCase("MeleeDamageOffset")) && split.length > 3)
        {
            meleeDamagePoints.add(new Vector3f(Float.parseFloat(split[1]) / 16F, Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F));
        }
        useCustomMeleeWhenShoot = readValue(split, "UseCustomMeleeWhenShoot", useCustomMeleeWhenShoot, file);

        //Player modifiers
        moveSpeedModifier = readValue(split, "MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue(split, "Slowness", moveSpeedModifier, file);
        knockbackModifier = readValue(split, "KnockbackReduction", knockbackModifier, file);
        knockbackModifier = readValue(split, "KnockbackModifier", knockbackModifier, file);
        switchDelay = readValue(split, "SwitchDelay", switchDelay, file);

        //Information
        showAttachments = readValue(split, "ShowAttachments", showAttachments, file);
        showDamage = readValue(split, "ShowDamage", showDamage, file);
        showRecoil = readValue(split, "ShowRecoil", showRecoil, file);
        showSpread = readValue(split, "ShowAccuracy", showSpread, file);
        showReloadTime = readValue(split, "ShowReloadTime", showReloadTime, file);

        //Sounds
        distortSound = readValue(split, "DistortSound", distortSound, file);
        shootSoundLength = readValue(split, "SoundLength", shootSoundLength, file);
        idleSoundLength = readValue(split, "IdleSoundLength", idleSoundLength, file);
        warmupSoundLength = readValue(split, "WarmupSoundLength", warmupSoundLength, file);
        idleSoundRange = readValue(split, "IdleSoundRange", idleSoundRange, file);
        meleeSoundRange = readValue(split, "MeleeSoundRange", meleeSoundRange, file);
        reloadSoundRange = readValue(split, "ReloadSoundRange", reloadSoundRange, file);
        gunSoundRange = readValue(split, "GunSoundRange", gunSoundRange, file);
        shootSound = readSound(split, "ShootSound", shootSound, file);
        bulletInsert = readSound(split, "BulletInsertSound", bulletInsert, file);
        actionSound = readSound(split, "ActionSound", actionSound, file);
        lastShootSound = readSound(split, "LastShootSound", lastShootSound, file);
        suppressedShootSound = readSound(split, "SuppressedShootSound", suppressedShootSound, file);
        lastShootSoundSuppressed = readSound(split, "LastSuppressedShootSound", lastShootSoundSuppressed, file);
        reloadSound = readSound(split, "ReloadSound", reloadSound, file);
        reloadSoundOnEmpty = readSound(split, "EmptyReloadSound", reloadSoundOnEmpty, file);
        clickSoundOnEmpty = readSound(split, "EmptyClickSound", clickSoundOnEmpty, file);
        clickSoundOnEmptyRepeated = readSound(split, "EmptyClickSoundRepeated", clickSoundOnEmptyRepeated, file);
        idleSound = readSound(split, "IdleSound", idleSound, file);
        meleeSound = readSound(split, "MeleeSound", meleeSound, file);

        //Looping sounds
        warmupSound = readValue(split, "WarmupSound", warmupSound, file);
        loopedSound = readValue(split, "LoopedSound", loopedSound, file);
        loopedSound = readValue(split, "SpinSound", loopedSound, file);
        cooldownSound = readValue(split, "CooldownSound", cooldownSound, file);
        lockOnSound = readSound(split, "LockOnSound", lockOnSound, file);
        distantShootSound = readSound(split, "DistantSound", distantShootSound, file);
        distantShootSound = readSound(split, "DistantShootSound", distantShootSound, file);
        loopedSoundLength = readValue(split, "LoopedSoundLength", loopedSoundLength, file);
        loopedSoundLength = readValue(split, "SpinSoundLength", loopedSoundLength, file);
        lockOnSoundTime = readValue(split, "LockOnSoundTime", lockOnSoundTime, file);
        distantSoundRange = readValue(split, "DistantSoundRange", distantSoundRange, file);

        //Mode
        //TODO: compare code
        /*aSplit = ConfigUtils.getSplitFromKey(config, "Mode");
        if (aSplit != null) {
            try {
                mode = EnumFireMode.getFireMode(aSplit[1]);
                defaultmode = mode;
                submode = new EnumFireMode[aSplit.length - 1];
                for (int i = 0; i < submode.length; i++) {
                    submode[i] = EnumFireMode.getFireMode(aSplit[1 + i]);
                }
            } catch (Exception ex) {
                FlansMod.logPackError(file.name, packName, shortName, "Error thrown while setting gun mode", aSplit, ex);
            }
        }*/
        if (split[0].equalsIgnoreCase("Mode") && split.length > 1)
            mode = EnumFireMode.getFireMode(split[1]);

        //Overlay and zoom settings
        overlayName = ResourceUtils.sanitize(readValue(split, "Scope", overlayName, file));
        zoomFactor = readValue(split, "ZoomLevel", zoomFactor, file);
        fovFactor = readValue(split, "FOVZoomLevel", fovFactor, file);
        allowNightVision = readValue(split, "AllowNightVision", allowNightVision, file);
        hasVariableZoom = readValue(split, "HasVariableZoom", hasVariableZoom, file);
        minZoom = readValue(split, "MinZoom", minZoom, file);
        maxZoom = readValue(split, "MaxZoom", maxZoom, file);
        zoomAugment = readValue(split, "ZoomAugment", zoomAugment, file);
        if (split[0].equalsIgnoreCase("ZoomLevel") && zoomFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ZOOM;
        if (split[0].equalsIgnoreCase("FOVZoomLevel") && fovFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;
        if (maxZoom > 1F && hasVariableZoom)
            secondaryFunction = EnumSecondaryFunction.ZOOM;

        //Models & Textures
        deployable = readValue(split, "Deployable", deployable, file);
        deployableModelName = ResourceUtils.sanitize(readValue(split, "DeployedModel", deployableModelName, file));
        deployableTextureName = ResourceUtils.sanitize(readValue(split, "DeployedTexture", deployableTextureName, file));
        casingModelName = ResourceUtils.sanitize(readValue(split, "CasingModel", casingModelName, file));
        casingTextureName = ResourceUtils.sanitize(readValue(split, "CasingTexture", casingTextureName, file));
        flashModelName = ResourceUtils.sanitize(readValue(split, "FlashModel", flashModelName, file));
        flashTextureName = ResourceUtils.sanitize(readValue(split, "FlashTexture", flashTextureName, file));
        muzzleFlashModelName = ResourceUtils.sanitize(readValue(split, "MuzzleFlashModel", muzzleFlashModelName, file));
        hitTextureName = ResourceUtils.sanitize(readValue(split, "HitTexture", hitTextureName, file));

        //Particles
        muzzleFlashParticle = readValue(split, "MuzzleFlashParticle", muzzleFlashParticle, file);
        muzzleFlashParticleSize = readValue(split, "MuzzleFlashParticleSize", muzzleFlashParticleSize, file);
        showMuzzleFlashParticlesFirstPerson = readValue(split, "ShowMuzzleFlashParticleFirstPerson", showMuzzleFlashParticlesFirstPerson, file);
        //TODO: readValue with Vector
        //muzzleFlashParticlesShoulderOffset = readValue(split, "MuzzleFlashParticleShoulderOffset", muzzleFlashParticlesShoulderOffset, file);
        //muzzleFlashParticlesHandOffset = readValue(split, "MuzzleFlashParticleHandOffset", muzzleFlashParticlesHandOffset, file);
        if (split[0].equalsIgnoreCase("ShowMuzzleFlashParticle"))
        {
            showMuzzleFlashParticles = readValue(split, "ShowMuzzleFlashParticle", showMuzzleFlashParticles, file);
            useMuzzleFlashDefaults = false;
        }

        //Attachment settings
        allowAllAttachments = readValue(split, "AllowAllAttachments", allowAllAttachments, file);
        if (split[0].equalsIgnoreCase("AllowAttachments"))
        {
            for (int i = 1; i < split.length; i++)
                allowedAttachments.add(AttachmentType.getAttachment(ResourceUtils.sanitize(split[i])));
        }
        allowBarrelAttachments = readValue(split, "AllowBarrelAttachments", allowBarrelAttachments, file);
        allowScopeAttachments = readValue(split, "AllowScopeAttachments", allowScopeAttachments, file);
        allowStockAttachments = readValue(split, "AllowStockAttachments", allowStockAttachments, file);
        allowGripAttachments = readValue(split, "AllowGripAttachments", allowGripAttachments, file);
        allowGadgetAttachments = readValue(split, "AllowGadgetAttachments", allowGadgetAttachments, file);
        allowSlideAttachments = readValue(split, "AllowSlideAttachments", allowSlideAttachments, file);
        allowPumpAttachments = readValue(split, "AllowPumpAttachments", allowPumpAttachments, file);
        allowAccessoryAttachments = readValue(split, "AllowAccessoryAttachments", allowAccessoryAttachments, file);
        numGenericAttachmentSlots = readValue(split, "NumGenericAttachmentSlots", numGenericAttachmentSlots, file);

        //Shield settings
        if (split[0].equalsIgnoreCase("Shield") && split.length > 7)
        {
            shield = true;
            shieldDamageAbsorption = Float.parseFloat(split[1]);
            shieldOrigin = new Vector3f(Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F, Float.parseFloat(split[4]) / 16F);
            shieldDimensions = new Vector3f(Float.parseFloat(split[5]) / 16F, Float.parseFloat(split[6]) / 16F, Float.parseFloat(split[7]) / 16F);
        }

        if (FMLEnvironment.dist == Dist.CLIENT)
            processAnimationConfigs(line, split, file);
    }

    protected void processAnimationConfigs(String line, String[] split, TypeFile file)
    {
        //TODO: use a record class and pass it to the model via setter call in ModelCache
    }

    @Override
    protected void postRead()
    {
        super.postRead();
        defaultSpread = bulletSpread;
        recoilYaw /= 10F;
        decreaseRecoilYaw = (decreaseRecoilYaw > 0F) ? decreaseRecoilYaw : 0.5F;

        if (lockOnToDriveables)
        {
            lockOnToPlanes = true;
            lockOnToVehicles = true;
            lockOnToMechas = true;
        }

        if (useCustomMeleeWhenShoot)
            secondaryFunctionWhenShoot = EnumSecondaryFunction.CUSTOM_MELEE;

        useLoopingSounds = StringUtils.isNotBlank(loopedSound);

        if (overlayName.equals("none"))
            overlayName = StringUtils.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void postReadClient()
    {
        super.postReadClient();
        deployableModelClassName = findModelClass(deployableModelName, contentPack);
        deployableTexture = loadTexture(deployableTextureName, this);
        casingModelClassName = findModelClass(casingModelName, contentPack);
        casingTexture = loadTexture(casingTextureName, this);
        flashModelClassName = findModelClass(flashModelName, contentPack);
        flashTexture = loadTexture(flashTextureName, this);
        muzzleFlashModelClassName = findModelClass(muzzleFlashModelName, contentPack);
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
    private void checkForTags(ItemStack gun)
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

    /**
     * Get the melee damage of a specific gun, taking into account attachments
     */
    public float getMeleeDamage(ItemStack stack)
    {
        float stackMeleeDamage = meleeDamage;

        for (AttachmentType attachment : getCurrentAttachments(stack))
            stackMeleeDamage *= attachment.meleeDamageMultiplier;

        return stackMeleeDamage;
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
        float stackRecoil = recoilPitch + (rand.nextFloat() * rndRecoilPitchRange);

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
