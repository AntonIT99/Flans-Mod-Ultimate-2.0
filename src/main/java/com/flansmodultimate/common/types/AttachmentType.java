package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.EnumAttachmentType;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.item.AttachmentItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class AttachmentType extends PaintableType implements IScope
{
    /** The type of attachment. Each gun can have one barrel, one scope, one grip, one stock and some number of generics up to a limit set by the gun */
    @Getter
    protected EnumAttachmentType enumAttachmentType = EnumAttachmentType.GENERIC;

    //Attachment Function add-ons
    /** This variable controls whether or not bullet sounds should be muffled */
    @Getter
    protected boolean silencer;
    /** If true, then this attachment will act like a flashlight */
    @Getter
    protected boolean flashlight;
    /** Flashlight range. How far away it lights things up */
    @Getter
    protected float flashlightRange = 10F;
    /** Flashlight strength between 0 and 15 */
    @Getter
    protected int flashlightStrength = 12;
    /** If true, disable the muzzle flash model */
    @Getter
    protected boolean disableMuzzleFlash;

    //Gun behaviour modifiers
    /** These stack between attachments and apply themselves to the gun's default spread */
    @Getter
    protected float spreadMultiplier = 1F;
    /** Likewise these stack and affect recoil */
    @Getter
    protected float recoilMultiplier = 1F;
    /** The return to center force LOWER = BETTER */
    protected float recoilControlMultiplier = 1F;
    protected float recoilControlMultiplierSneaking = 1F;
    protected float recoilControlMultiplierSprinting = 1F;
    /** Another stacking variable for damage */
    @Getter
    protected float damageMultiplier = 1F;
    /** Melee damage modifier */
    @Getter
    protected float meleeDamageMultiplier = 1F;
    /** Bullet speed modifier */
    @Getter
    protected float bulletSpeedMultiplier = 1F;
    @Getter
    protected float shootDelayMultiplier = 1F;
    /** This modifies the reload time, which is then rounded down to the nearest tick */
    @Getter
    protected float reloadTimeMultiplier = 1F;
    /** Movement speed modifier */
    protected float moveSpeedMultiplier = 1F;
    /** If set to anything other than null, then this attachment will override the weapon's default firing mode */
    protected EnumFireMode modeOverride = null;
    protected String modeOverrideString = StringUtils.EMPTY;
    protected EnumSpreadPattern spreadPattern = null;

    //Underbarrel functions
    /** This variable controls whether the underbarrel is enabled */
    protected boolean secondaryFire;
    /** The list of bullet types that can be used in the secondary mode */
    protected List<String> secondaryAmmo = new ArrayList<>();
    /** The delay between shots in ticks (1/20ths of seconds) */
    protected float secondaryDamage = 1;
    /** The delay between shots in ticks (1/20ths of seconds) */
    @Getter @Setter
    protected float secondarySpread = 1;
    /** The speed of bullets upon leaving this gun */
    protected float secondarySpeed = 5.0F;
    /** The time (in ticks) it takes to reload this gun */
    protected int secondaryReloadTime = 1;
    /** The delay between shots in ticks (1/20ths of seconds) */
    protected int secondaryShootDelay = 1;
    /** The sound played upon shooting */
    protected String secondaryShootSound;
    /** The sound to play upon reloading */
    protected String secondaryReloadSound;
    /** The firing mode of the gun. One of semi-auto, full-auto, minigun or burst */
    protected EnumFireMode secondaryFireMode = EnumFireMode.SEMIAUTO;
    protected String secondaryFireModeString = StringUtils.EMPTY;
    /** The sound to play if toggling between primary and underbarrel */
    protected String toggleSound;
    /** The number of bullet entities created by each shot */
    protected int secondaryNumBullets = 1;
    /** The number of bullet stacks in the magazine */
    protected int numSecAmmoItems = 1;

    //Scope variables (These variables only come into play for scope attachments)
    /** The zoomLevel of this scope */
    @Getter
    protected float zoomFactor = 1F;
    /** The FOV zoom level of this scope */
    @Getter
    protected float fovFactor = 1F;
    /** If true, then this scope will active night vision potion effect*/
    @Getter
    protected boolean hasNightVision;

    protected float minZoom = 1;
    protected float maxZoom = 4;
    protected float zoomAugment = 1;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        enumAttachmentType = EnumAttachmentType.get(readValue("AttachmentType", StringUtils.EMPTY, file));

        silencer = readValue("Silencer", silencer, file);
        disableMuzzleFlash = readValue("DisableMuzzleFlash", disableMuzzleFlash, file);
        disableMuzzleFlash = readValue("DisableFlash", disableMuzzleFlash, file);

        //Flashlight settings
        flashlight = readValue("Flashlight", flashlight, file);
        flashlightRange = readValue("FlashlightRange", flashlightRange, file);
        flashlightStrength = readValue("FlashlightStrength", flashlightStrength, file);

        //Mode override
        modeOverrideString = readValue("ModeOverride", modeOverrideString, file);

        //Secondary Stuff
        secondaryFire = readValue("SecondaryMode", secondaryFire, file);

        String ammo = readValue("SecondaryAmmo", StringUtils.EMPTY, file);
        if (!ammo.isBlank())
            secondaryAmmo.add(ammo);

        secondaryDamage = readValue("SecondaryDamage", secondaryDamage, file);
        secondarySpread = readValue("SecondarySpread", secondarySpread, file);
        secondarySpread = readValue("SecondaryAccuracy", secondarySpread, file);
        secondarySpeed = readValue("SecondaryBulletSpeed", secondarySpeed, file);
        secondaryShootDelay = readValue("SecondaryShootDelay", secondaryShootDelay, file);
        secondaryReloadTime = readValue("SecondaryReloadTime", secondaryReloadTime, file);
        secondaryShootDelay = readValue("SecondaryShootDelay", secondaryShootDelay, file);
        secondaryNumBullets = readValue("SecondaryNumBullets", secondaryNumBullets, file);
        numSecAmmoItems = readValue("LoadSecondaryIntoGun", numSecAmmoItems, file);

        secondaryFireModeString = readValue("SecondaryFireMode", secondaryFireModeString, file);

        secondaryShootSound = readSound("SecondaryShootSound", secondaryShootSound, file);
        secondaryReloadSound = readSound("SecondaryReloadSound", secondaryReloadSound, file);
        toggleSound = readSound("ModeSwitchSound", toggleSound, file);

        //Multipliers
        meleeDamageMultiplier = readValue("MeleeDamageMultiplier", meleeDamageMultiplier, file);
        damageMultiplier = readValue("DamageMultiplier", damageMultiplier, file);
        spreadMultiplier = readValue("SpreadMultiplier", spreadMultiplier, file);
        recoilMultiplier = readValue("RecoilMultiplier", recoilMultiplier, file);
        recoilControlMultiplier = readValue("RecoilControlMultiplier", recoilControlMultiplier, file);
        recoilControlMultiplierSneaking = readValue("RecoilControlMultiplierSneaking", recoilControlMultiplierSneaking, file);
        recoilControlMultiplierSprinting = readValue("RecoilControlMultiplierSprinting", recoilControlMultiplierSprinting, file);
        bulletSpeedMultiplier = readValue("BulletSpeedMultiplier", bulletSpeedMultiplier, file);
        recoilControlMultiplierSprinting = readValue("RecoilControlMultiplierSprinting", recoilControlMultiplierSprinting, file);
        moveSpeedMultiplier = readValue("MovementSpeedMultiplier", moveSpeedMultiplier, file);
        moveSpeedMultiplier = readValue("MoveSpeedModifier", moveSpeedMultiplier, file);

        //Scope Variables
        minZoom = readValue("MinZoom", minZoom, file);
        maxZoom = readValue("MaxZoom", maxZoom, file);
        zoomAugment = readValue("ZoomAugment", zoomAugment, file);
        zoomFactor = readValue("ZoomLevel", zoomFactor, file);
        fovFactor = readValue("FOVZoomLevel", fovFactor, file);

        overlayName = readResource("ZoomOverlay", overlayName, file);

        hasNightVision = readValue("HasNightVision", hasNightVision, file);

        if (modeOverrideString != null)
            modeOverride = EnumFireMode.getFireMode(modeOverrideString);
        if (secondaryFireModeString != null)
            secondaryFireMode = EnumFireMode.getFireMode(secondaryFireModeString);

        if (overlayName.equals("none"))
            overlayName = StringUtils.EMPTY;
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

    @Nullable
    public static AttachmentType getFromNBT(CompoundTag tags) {
        ItemStack stack = ItemStack.of(tags);
        if (!stack.isEmpty() && stack.getItem() instanceof AttachmentItem attachment) {
            return attachment.getConfigType();
        }
        return null;
    }

    public static AttachmentType getAttachment(String s)
    {
        if (InfoType.getInfoType(s) instanceof AttachmentType attachmentType)
        {
            return attachmentType;
        }
        return null;
    }
}
