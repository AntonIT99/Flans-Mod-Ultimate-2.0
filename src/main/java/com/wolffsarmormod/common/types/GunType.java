package com.wolffsarmormod.common.types;

import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.common.guns.EnumFireMode;
import com.wolffsarmormod.common.guns.EnumSecondaryFunction;
import com.wolffsarmormod.common.guns.EnumSpreadPattern;
import com.wolffsarmormod.common.item.BulletItem;
import com.wolffsarmormod.common.item.GrenadeItem;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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
    protected int recoil;
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
    protected List<ShootableType> ammo = new ArrayList<>();
    protected List<ShootableType> nonExplosiveAmmo = new ArrayList<>();
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
    protected boolean allowNumBulletsByBulletType = false;
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
    public boolean showAttachments = true;
    /**
     * Show statistics
     */
    public boolean showDamage = false, showRecoil = false, showSpread = false, showReloadTime = false;
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
    protected String meleeSound;
    /**
     * The time delay between custom melee attacks
     */
    protected int meleeTime = 1;
    /**
     * The path the melee weapon takes
     */
    protected ArrayList<Vector3f> meleePath = new ArrayList<>(), meleePathAngles = new ArrayList<>();
    /**
     * The points on the melee weapon that damage is actually done from.
     */
    protected ArrayList<Vector3f> meleeDamagePoints = new ArrayList<>();


    //Deployable Settings
    /**
     * If true, then the bullet does not shoot when right-clicked, but must instead be placed on the ground
     */
    @Getter
    protected boolean deployable = false;
    /**
     * The deployable model
     */
    //TODO ModelMG
    //protected ModelMG deployableModel;
    protected String deployableModelString;
    /**
     * The deployable model's texture
     */
    protected String deployableTexture;
    /**
     * Various deployable settings controlling the player view limits and standing position
     */
    protected float standBackDist = 1.5F, topViewLimit = -60F, bottomViewLimit = 30F, sideViewLimit = 45F, pivotHeight = 0.375F;

    //Default Scope Settings. Overriden by scope attachments
    //In many cases, this will simply be iron sights
    /**
     * Default scope overlay texture
     */
    protected String defaultScopeTexture;
    /**
     * Whether the default scope has an overlay
     */
    protected boolean hasScopeOverlay = false;
    /**
     * The zoom level of the default scope
     */
    protected float zoomLevel = 1.0F;
    /**
     * The FOV zoom level of the default scope
     */
    protected float FOVFactor = 1.5F;
    /**
     * Gives night vision while scoped if true
     */
    protected boolean allowNightVision = false;

    //Model variables
    /**
     * For guns with 3D models
     */
    @Getter
    protected ModelGun model;

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
    protected boolean allowAllAttachments = false;
    /**
     * The list of allowed attachments for this gun
     */
    //TODO AttachmentType
    //protected ArrayList<AttachmentType> allowedAttachments = new ArrayList<>();
    /**
     * Whether each attachment slot is available
     */
    protected boolean allowBarrelAttachments = false, allowScopeAttachments = false,
            allowStockAttachments = false, allowGripAttachments = false, allowGadgetAttachments = false,
            allowSlideAttachments = false, allowPumpAttachments = false, allowAccessoryAttachments = false;
    /**
     * The number of generic attachment slots there are on this gun
     */
    protected int numGenericAttachmentSlots = 0;

    /**
     * The static hashmap of all guns by shortName
     */
    protected static HashMap<String, GunType> guns = new HashMap<>();
    /**
     * The static list of all guns
     */
    protected static ArrayList<GunType> gunList = new ArrayList<>();

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
    // Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg
    protected float adsSpreadModifier = -1F;
    // Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg. For shotguns.
    protected float adsSpreadModifierShotgun = -1F;

    protected float switchDelay = 0;

    protected boolean hasVariableZoom = false;
    protected float minZoom = 1;
    protected float maxZoom = 4;
    protected float zoomAugment = 1;

    /**
     * Whether the default scope has an overlay
     */
    public boolean hasScopeOverlay() {
        return hasScopeOverlay;
    }

    @Override
    public float getFOVFactor()
    {
        return 0;
    }

    @Override
    public float getZoomFactor()
    {
        return 0;
    }

    @Override
    public boolean hasZoomOverlay()
    {
        return false;
    }

    @Override
    public String getZoomOverlay()
    {
        return "";
    }

    public boolean useLoopingSounds()
    {
        return useLoopingSounds;
    }

    public boolean isCorrectAmmo(ShootableType type)
    {
        return ammo.contains(type);
    }

    public boolean isCorrectAmmo(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
            return false;
        else if(stack.getItem() instanceof BulletItem bulletItem)
        {
            return isCorrectAmmo(bulletItem.getConfigType());
        }
        else if(stack.getItem() instanceof GrenadeItem grenadeItem)
        {
            return isCorrectAmmo(grenadeItem.getConfigType());
        }
        return false;
    }

    /**
     * Return the currently active scope on this gun. Search attachments, and by default, simply give the gun
     */
    public IScope getCurrentScope(ItemStack gunStack)
    {
        //TODO: implement attachments
        /*IScope attachedScope = getScope(gunStack);
        return attachedScope == null ? this : attachedScope;*/
        return this;
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
        //TODO: implement attachments
        /*
        checkForTags(gun);
        ArrayList<AttachmentType> attachments = new ArrayList<>();
        NBTTagCompound attachmentTags = gun.getTagCompound().getCompoundTag("attachments");
        NBTTagList genericsList = attachmentTags.getTagList("generics", (byte)10); //TODO : Check this 10 is correct
        for(int i = 0; i < numGenericAttachmentSlots; i++)
        {
            appendToList(gun, "generic_" + i, attachments);
        }
        appendToList(gun, "barrel", attachments);
        appendToList(gun, "scope", attachments);
        appendToList(gun, "stock", attachments);
        appendToList(gun, "grip", attachments);
        return attachments;
        */
        return Collections.emptyList();
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
        //TODO: implement attachments
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

    /**
     * @return Returns the pumpDelayAfterReload if a model exits, otherwise 0
     */
    public int getPumpDelayAfterReload()
    {
        if (model != null)
            return model.getPumpDelayAfterReload();

        return 0;
    }

    /**
     * @return Returns the pumpDelay if a model exits, otherwise 0
     */
    public int getPumpDelay()
    {
        if (model != null)
            return model.getPumpDelay();

        return 0;
    }

    /**
     * @return the pump time if a model exits, otherwise 1
     */
    public int getPumpTime()
    {
        if (model != null)
            return model.getPumpTime();

        return 0;
    }
}
