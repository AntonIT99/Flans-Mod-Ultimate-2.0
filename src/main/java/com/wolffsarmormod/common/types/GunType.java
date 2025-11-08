package com.wolffsarmormod.common.types;

import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.common.guns.EnumFireMode;
import com.wolffsarmormod.common.guns.EnumSecondaryFunction;
import com.wolffsarmormod.common.guns.EnumSpreadPattern;
import com.wolffsarmormod.util.ResourceUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.wolffsarmormod.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class GunType extends PaintableType implements IScope
{
    protected static final Random rand = new Random();
    protected static final int DEFAULT_SHOOT_DELAY = 2;

    /**Extended Recoil System
     * ported by SecretAgent12
     */
    //TODO GunRecoil
    //protected GunRecoil recoil = new GunRecoil();
    @Getter
    protected float recoil;
    protected boolean useFancyRecoil = false;

    //Gun Behaviour Variables

    //Recoil Variables
    /**
     * Base value for Upwards cursor/view recoil
     */
    protected float recoilPitch = 0.0F;
    /**
     * Base value for Left/Right cursor/view recoil
     */
    protected float recoilYaw = 0.0F;
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
    protected float decreaseRecoilPitch = 0F;
    /**
     * DEPRECATED DO NOT USE. Divisor for yaw recoil when crouching.
     */
    protected float decreaseRecoilYaw = 0F;

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
    protected boolean allowRearm = true;
    /**
     * The time (in ticks) it takes to reload this gun
     */
    @Getter
    protected int reloadTime;
    /**
     * Number of ammo items that the gun may hold. Most guns will hold one magazine.
     * Some may hold more, such as Nerf pistols, revolvers or shotguns
     */
    protected int numPrimaryAmmoItems = 1;

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
    protected boolean allowSpreadByBullet = false;
    /**
     * Damage inflicted by this gun. Multiplied by the bullet damage.
     */
    protected float damage = 0;
    /**
     * The damage inflicted upon punching someone with this gun
     */
    protected float meleeDamage = 1;
    // Modifier for melee damage against specifically driveable entities.
    protected float meleeDamageDriveableModifier = 1;
    /**
     * The speed of bullets upon leaving this gun. 0.0f means instant.
     */
    @Getter
    protected float bulletSpeed = 5.0F;
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
    protected float shootDelay = 0;
    /**
     * Number of ammo items that the gun may hold. Most guns will hold one magazine.
     * Some may hold more, such as Nerf pistols, revolvers or shotguns
     */
    @Getter
    protected int numAmmoItemsInGun = 1;
    /**
     * The fire rate of the gun in RPM, 1200 = MAX
     */
    protected float roundsPerMin = 0;
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
     * The amount of knockback to impact upon the player per shot
     */
    @Getter
    protected float knockback = 0F;
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
    protected boolean oneHanded = false;
    /**
     * For one shot items like a panzerfaust
     */
    @Getter
    protected boolean consumeGunUponUse = false;
    /**
     * Show the crosshair when holding this weapon
     */
    protected boolean showCrosshair = true;
    /**
     * Item to drop on shooting
     */
    @Getter
    protected String dropItemOnShoot = null;
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
    protected int hipFireWhileSprinting = 0;

    //Launcher variables
    protected int canLockOnAngle = 5;
    protected int lockOnSoundTime = 0;
    protected String lockOnSound = "";
    protected int maxRangeLockOn = 80;
    protected boolean canSetPosition = false;
    /**
     * Determines what the launcher can lock on to
     */
    protected boolean lockOnToPlanes = false, lockOnToVehicles = false, lockOnToMechas = false, lockOnToPlayers = false, lockOnToLivings = false;


    //Shields
	/*A shield is actually a gun without any shoot functionality (similar to knives or binoculars)
	and a load of shield code on top. This means that guns can have in built shields (think Nerf Stampede) */
    /**
     * Whether or not this gun has a shield piece
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
    protected float shieldDamageAbsorption = 0F;

    //Sounds
    /**
     * The sound played upon shooting
     */
    @Getter
    protected String shootSound;
    /**
     * Bullet insert reload sound
     */
    protected String bulletInsert = "defaultshellinsert";
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
     * The block range for idle sounds (for miniguns etc)
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
    protected boolean useLoopingSounds = false;
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
    protected boolean deployable = false;
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
    protected float standBackDist = 1.5F, topViewLimit = -60F, bottomViewLimit = 30F, sideViewLimit = 45F, pivotHeight = 0.375F;

    //Default Scope Settings. Overriden by scope attachments
    //In many cases, this will simply be iron sights
    /**
     * The zoom level of the default scope
     */
    @Getter
    protected float zoomFactor = 1.0F;
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
    //TODO ModelCasing
    //protected ModelCasing casingModel;
    protected String casingModelString;
    /**
     * For adding a muzzle flash model to render
     */
    //TODO ModelFlash
    //protected ModelFlash flashModel;
    //protected ModelMuzzleFlash muzzleFlashModel;
    protected String flashModelString;
    /**
     * Set a bullet casing texture
     */
    protected String casingTexture;
    /**
     * Set a muzzle flash texture
     */
    protected String flashTexture;
    /**
     * Set a hit marker texture
     */
    protected String hitTexture;

    protected String muzzleFlashParticle = "flansmod.muzzleflash";
    protected float muzzleFlashParticleSize = 1F;
    protected Boolean useMuzzleFlashDefaults = true;
    protected Boolean showMuzzleFlashParticles = true;
    protected Boolean showMuzzleFlashParticlesFirstPerson = false;
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
    protected boolean allowBarrelAttachments = false;
    protected boolean allowScopeAttachments = false;
    protected boolean allowStockAttachments = false;
    protected boolean allowGripAttachments = false;
    protected boolean allowGadgetAttachments = false;
    protected boolean allowSlideAttachments = false;
    protected boolean allowPumpAttachments = false;
    protected boolean allowAccessoryAttachments = false;
    /**
     * The number of generic attachment slots there are on this gun
     */
    protected int numGenericAttachmentSlots = 0;

    //Modifiers
    /**
     * Speeds up or slows down player movement when this item is held
     */
    protected float moveSpeedModifier = 1F;
    /**
     * Gives knockback resistance to the player
     */
    protected float knockbackModifier = 0F;
    /**
     * Default spread of the gun. Do not modify.
     */
    protected float defaultSpread = 0F;
    /** Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg */
    protected float adsSpreadModifier = -1F;
    /** Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg. For shotguns. */
    protected float adsSpreadModifierShotgun = -1F;

    protected float switchDelay = 0;

    protected boolean hasVariableZoom = false;
    protected float minZoom = 1F;
    protected float maxZoom = 4F;
    protected float zoomAugment = 1F;

    /** For shotgun pump handles and rifle bolts */
    @Getter @Setter
    protected int pumpDelay = 0;
    @Getter @Setter
    protected int pumpDelayAfterReload = 0;
    @Getter @Setter
    protected int pumpTime = 1;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        damage = readValue(split, "Damage", damage, file);
        canForceReload = readValue(split, "CanForceReload", canForceReload, file);
        reloadTime = readValue(split, "ReloadTime", reloadTime, file);
        recoil = readValue(split, "Recoil", recoil, file);
        knockback = readValue(split, "Knockback", knockback, file);
        bulletSpread = readValue(split, "Accuracy", bulletSpread, file);
        bulletSpread = readValue(split, "Spread", bulletSpread, file);
        numBullets = readValue(split, "NumBullets", numBullets, file);
        consumeGunUponUse = readValue(split, "ConsumeGunOnUse", consumeGunUponUse, file);
        dropItemOnShoot = readValue(split, "DropItemOnShoot", dropItemOnShoot, file);
        numBurstRounds = readValue(split, "NumBurstRounds", numBurstRounds, file);
        minigunStartSpeed = readValue(split, "MinigunStartSpeed", minigunStartSpeed, file);
        meleeDamage = readValue(split, "MeleeDamage", meleeDamage, file);
        if (split[0].equalsIgnoreCase("MeleeDamage") && meleeDamage > 0F)
            secondaryFunction = EnumSecondaryFunction.MELEE;

        //Information
        showAttachments = readValue(split, "ShowAttachments", showAttachments, file);
        showDamage = readValue(split, "ShowDamage", showDamage, file);
        showRecoil = readValue(split, "ShowRecoil", showRecoil, file);
        showSpread = readValue(split, "ShowAccuracy", showSpread, file);
        showReloadTime = readValue(split, "ShowReloadTime", showReloadTime, file);

        //Sounds
        shootDelay = readValue(split, "ShootDelay", shootDelay, file);
        shootSoundLength = readValue(split, "SoundLength", shootSoundLength, file);
        distortSound = readValue(split, "DistortSound", distortSound, file);
        idleSoundLength = readValue(split, "IdleSoundLength", idleSoundLength, file);
        warmupSoundLength = readValue(split, "WarmupSoundLength", warmupSoundLength, file);
        loopedSoundLength = readValue(split, "LoopedSoundLength", loopedSoundLength, file);
        loopedSoundLength = readValue(split, "SpinSoundLength", loopedSoundLength, file);
        shootSound = readSound(split, "ShootSound", shootSound, file);
        reloadSound = readSound(split, "ReloadSound", reloadSound, file);
        idleSound = readSound(split, "IdleSound", idleSound, file);
        meleeSound = readSound(split, "MeleeSound", meleeSound, file);

        //Looping sounds
        warmupSound = readValue(split, "WarmupSound", warmupSound, file);
        loopedSound = readValue(split, "LoopedSound", loopedSound, file);
        loopedSound = readValue(split, "SpinSound", loopedSound, file);
        cooldownSound = readValue(split, "CooldownSound", cooldownSound, file);

        //Modes and zoom settings
        overlayName = ResourceUtils.sanitize(readValue(split, "Scope", overlayName, file));
        if (split[0].equalsIgnoreCase("Mode") && split.length > 1)
            mode = EnumFireMode.getFireMode(split[1]);
        zoomFactor = readValue(split, "ZoomLevel", zoomFactor, file);
        if (split[0].equalsIgnoreCase("ZoomLevel") && zoomFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ZOOM;
        fovFactor = readValue(split, "FOVZoomLevel", fovFactor, file);
        if (split[0].equalsIgnoreCase("FOVZoomLevel") && fovFactor > 1F)
            secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;
        deployable = readValue(split, "Deployable", deployable, file);
        deployableModelName = readValue(split, "DeployedModel", deployableModelName, file);
        deployableTextureName = ResourceUtils.sanitize(readValue(split, "DeployedTexture", deployableTextureName, file));
        //TODO: MuzzleFlashModel
        standBackDist = readValue(split, "StandBackDistance", standBackDist, file);
        topViewLimit = readValue(split, "TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue(split, "BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue(split, "SideViewLimit", sideViewLimit, file);
        pivotHeight = readValue(split, "PivotHeight", pivotHeight, file);
        numAmmoItemsInGun = readValue(split, "NumAmmoSlots", numAmmoItemsInGun, file);
        numAmmoItemsInGun = readValue(split, "NumAmmoItemsInGun", numAmmoItemsInGun, file);
        numAmmoItemsInGun = readValue(split, "LoadIntoGun", numAmmoItemsInGun, file);
        canShootUnderwater = readValue(split, "CanShootUnderwater", canShootUnderwater, file);
        oneHanded = readValue(split, "OneHanded", oneHanded, file);
        usableByPlayers = readValue(split, "UsableByPlayers", usableByPlayers, file);
        usableByMechas = readValue(split, "UsableByMechas", usableByMechas, file);
        spreadPattern = readValue(split, "SpreadPattern", spreadPattern, EnumSpreadPattern.class, file);

        if (split[0].equals("Ammo") && split.length > 1)
        {
            ammo.add(ResourceUtils.sanitize(split[1]));
        }

        if (split[0].equalsIgnoreCase("BulletSpeed"))
        {
            if (split.length > 1 && split[1].equalsIgnoreCase("instant"))
                bulletSpeed = 0F;
            else
                bulletSpeed = readValue(split, "BulletSpeed", bulletSpeed, file);
        }
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

        //Player modifiers
        moveSpeedModifier = readValue(split, "MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue(split, "Slowness", moveSpeedModifier, file);
        knockbackModifier = readValue(split, "KnockbackReduction", knockbackModifier, file);
        knockbackModifier = readValue(split, "KnockbackModifier", knockbackModifier, file);

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
        numGenericAttachmentSlots = readValue(split, "NumGenericAttachmentSlots", numGenericAttachmentSlots, file);

        //Shield settings
        if (split[0].equalsIgnoreCase("shield") && split.length > 7)
        {
            shield = true;
            shieldDamageAbsorption = Float.parseFloat(split[1]);
            shieldOrigin = new Vector3f(Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F, Float.parseFloat(split[4]) / 16F);
            shieldDimensions = new Vector3f(Float.parseFloat(split[5]) / 16F, Float.parseFloat(split[6]) / 16F, Float.parseFloat(split[7]) / 16F);
        }
    }

    @Override
    protected void postRead()
    {
        super.postRead();
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

    /*public AttachmentType getScope(ItemStack gun)
    {
        return getAttachment(gun, "scope");
    }*/

    /**
     * Returns all attachments currently attached to the specified gun
     */
    public List<AttachmentType> getCurrentAttachments(ItemStack gun)
    {
        checkForTags(gun);
        List<AttachmentType> attachments = new ArrayList<>();
        for (int i = 0; i < numGenericAttachmentSlots; i++)
        {
            appendToList(gun, "generic_" + i, attachments);
        }
        appendToList(gun, "barrel", attachments);
        appendToList(gun, "scope", attachments);
        appendToList(gun, "stock", attachments);
        appendToList(gun, "grip", attachments);
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
        return getAttachment(gun, "barrel");
    }

    public AttachmentType getScope(ItemStack gun)
    {
        return getAttachment(gun, "scope");
    }

    public AttachmentType getStock(ItemStack gun)
    {
        return getAttachment(gun, "stock");
    }

    public AttachmentType getGrip(ItemStack gun)
    {
        return getAttachment(gun, "grip");
    }

    public AttachmentType getGeneric(ItemStack gun, int i)
    {
        return getAttachment(gun, "generic_" + i);
    }

    //Attachment ItemStack getter methods
    public ItemStack getBarrelItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, "barrel");
    }

    public ItemStack getScopeItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, "scope");
    }

    public ItemStack getStockItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, "stock");
    }

    public ItemStack getGripItemStack(ItemStack gun)
    {
        return getAttachmentItemStack(gun, "grip");
    }

    public ItemStack getGenericItemStack(ItemStack gun, int i)
    {
        return getAttachmentItemStack(gun, "generic_" + i);
    }

    /**
     * Generalised attachment getter method
     */
    public AttachmentType getAttachment(ItemStack gun, String name)
    {
        checkForTags(gun);
        CompoundTag tag = Objects.requireNonNull(gun.getTag()); // non-null because checkForTags used getOrCreateTag()
        CompoundTag attachments = tag.getCompound("attachments");
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
        CompoundTag attachments = tag.getCompound("attachments");
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
        if (!tag.contains("attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachments = new CompoundTag();
            for (int i = 0; i < numGenericAttachmentSlots; i++)
            {
                attachments.put("generic_" + i, new CompoundTag());
            }
            attachments.put("barrel", new CompoundTag());
            attachments.put("scope", new CompoundTag());
            attachments.put("stock", new CompoundTag());
            attachments.put("grip", new CompoundTag());

            tag.put("attachments", attachments);
        }
    }

    /**
     * Get the melee damage of a specific gun, taking into account attachments
     */
    public float getMeleeDamage(ItemStack stack)
    {
        float stackMeleeDamage = meleeDamage;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackMeleeDamage *= attachment.meleeDamageMultiplier;
        }
        return stackMeleeDamage;
    }

    /**
     * Get the damage of a specific gun, taking into account attachments
     */
    public float getDamage(ItemStack stack)
    {
        float stackDamage = damage;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackDamage *= attachment.damageMultiplier;
        }
        return stackDamage;
    }

    /**
     * Get the bullet spread of a specific gun, taking into account attachments
     */
    public float getSpread(ItemStack stack)
    {
        float stackSpread = bulletSpread;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackSpread *= attachment.spreadMultiplier;
        }
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
     * Get the recoil of a specific gun, taking into account attachments
     */
    public float getRecoil(ItemStack stack)
    {
        float stackRecoil = recoil;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackRecoil *= attachment.recoilMultiplier;
        }
        return stackRecoil;
    }

    /**
     * Get the bullet speed of a specific gun, taking into account attachments
     */
    public float getBulletSpeed(ItemStack stack)
    {
        float stackBulletSpeed = bulletSpeed;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackBulletSpeed *= attachment.bulletSpeedMultiplier;
        }
        return stackBulletSpeed;
    }

    /**
     * Get the reload time of a specific gun, taking into account attachments
     */
    public float getReloadTime(ItemStack stack)
    {
        float stackReloadTime = reloadTime;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackReloadTime *= attachment.reloadTimeMultiplier;
        }
        return stackReloadTime;
    }

    /**
     * Get the firing mode of a specific gun, taking into account attachments
     */
    public EnumFireMode getFireMode(ItemStack stack)
    {
        for(AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (attachment.modeOverride != null)
                return attachment.modeOverride;
        }
        return mode;
    }


    public float getShootDelay(ItemStack stack)
    {
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            if (attachment.modeOverride == EnumFireMode.BURST)
                return Math.max(shootDelay, 3);
        }

        float stackShootDelay = shootDelay;
        for (AttachmentType attachment : getCurrentAttachments(stack))
        {
            stackShootDelay *= attachment.shootDelayMultiplier;
        }
        return stackShootDelay;
    }
}
