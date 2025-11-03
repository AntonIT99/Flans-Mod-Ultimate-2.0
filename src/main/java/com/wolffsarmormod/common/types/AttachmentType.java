package com.wolffsarmormod.common.types;

import com.wolffsarmormod.common.guns.EnumAttachmentType;
import com.wolffsarmormod.common.guns.EnumFireMode;
import com.wolffsarmormod.common.guns.EnumSpreadPattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.wolffsarmormod.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class AttachmentType extends PaintableType implements IScope
{
    //TODO: implement attachment Item
    /** The type of attachment. Each gun can have one barrel, one scope, one grip, one stock and some number of generics up to a limit set by the gun */
    protected EnumAttachmentType attachmentType = EnumAttachmentType.GENERIC;
    protected String attachmentTypeString = StringUtils.EMPTY;

    //Attachment Function add-ons
    /** This variable controls whether or not bullet sounds should be muffled */
    @Getter
    protected boolean silencer;
    /** If true, then this attachment will act like a flashlight */
    protected boolean flashlight;
    /** Flashlight range. How far away it lights things up */
    protected float flashlightRange = 10F;
    /** Flashlight strength between 0 and 15 */
    protected int flashlightStrength = 12;
    /** If true, disable the muzzle flash model */
    protected boolean disableMuzzleFlash;

    //Gun behaviour modifiers
    /** These stack between attachments and apply themselves to the gun's default spread */
    protected float spreadMultiplier = 1F;
    /** Likewise these stack and affect recoil */
    protected float recoilMultiplier = 1F;
    /** The return to center force LOWER = BETTER */
    protected float recoilControlMultiplier = 1F;
    protected float recoilControlMultiplierSneaking = 1F;
    protected float recoilControlMultiplierSprinting = 1F;
    /** Another stacking variable for damage */
    protected float damageMultiplier = 1F;
    /** Melee damage modifier */
    protected float meleeDamageMultiplier = 1F;
    /** Bullet speed modifier */
    protected float bulletSpeedMultiplier = 1F;
    protected float shootDelayMultiplier = 1F;
    /** This modifies the reload time, which is then rounded down to the nearest tick */
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
    protected boolean hasNightVision;

    protected float minZoom = 1;
    protected float maxZoom = 4;
    protected float zoomAugment = 1;

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);

        attachmentTypeString = readValue(split, "AttachmentType", attachmentTypeString, file);

        silencer = readValue(split, "Silencer", silencer, file);
        disableMuzzleFlash = readValue(split, "DisableMuzzleFlash", disableMuzzleFlash, file);
        disableMuzzleFlash = readValue(split, "DisableFlash", disableMuzzleFlash, file);

        //Flashlight settings
        flashlight = readValue(split, "Flashlight", flashlight, file);
        flashlightRange = readValue(split, "FlashlightRange", flashlightRange, file);
        flashlightStrength = readValue(split, "FlashlightStrength", flashlightStrength, file);

        //Mode override
        modeOverrideString = readValue(split, "ModeOverride", modeOverrideString, file);

        //Secondary Stuff
        secondaryFire = readValue(split, "SecondaryMode", secondaryFire, file);

        String ammo = readValue(split, "SecondaryAmmo", StringUtils.EMPTY, file);
        if (!ammo.isBlank())
            secondaryAmmo.add(ammo);

        secondaryDamage = readValue(split, "SecondaryDamage", secondaryDamage, file);
        secondarySpread = readValue(split, "SecondarySpread", secondarySpread, file);
        secondarySpread = readValue(split, "SecondaryAccuracy", secondarySpread, file);
        secondarySpeed = readValue(split, "SecondaryBulletSpeed", secondarySpeed, file);
        secondaryShootDelay = readValue(split, "SecondaryShootDelay", secondaryShootDelay, file);
        secondaryReloadTime = readValue(split, "SecondaryReloadTime", secondaryReloadTime, file);
        secondaryShootDelay = readValue(split, "SecondaryShootDelay", secondaryShootDelay, file);
        secondaryNumBullets = readValue(split, "SecondaryNumBullets", secondaryNumBullets, file);
        numSecAmmoItems = readValue(split, "LoadSecondaryIntoGun", numSecAmmoItems, file);

        secondaryFireModeString = readValue(split, "SecondaryFireMode", secondaryFireModeString, file);

        secondaryShootSound = readSound(split, "SecondaryShootSound", secondaryShootSound, file);
        secondaryReloadSound = readSound(split, "SecondaryReloadSound", secondaryReloadSound, file);
        toggleSound = readSound(split, "ModeSwitchSound", toggleSound, file);

        //Multipliers
        meleeDamageMultiplier = readValue(split, "MeleeDamageMultiplier", meleeDamageMultiplier, file);
        damageMultiplier = readValue(split, "DamageMultiplier", damageMultiplier, file);
        spreadMultiplier = readValue(split, "SpreadMultiplier", spreadMultiplier, file);
        recoilMultiplier = readValue(split, "RecoilMultiplier", recoilMultiplier, file);
        recoilControlMultiplier = readValue(split, "RecoilControlMultiplier", recoilControlMultiplier, file);
        recoilControlMultiplierSneaking = readValue(split, "RecoilControlMultiplierSneaking", recoilControlMultiplierSneaking, file);
        recoilControlMultiplierSprinting = readValue(split, "RecoilControlMultiplierSprinting", recoilControlMultiplierSprinting, file);
        bulletSpeedMultiplier = readValue(split, "BulletSpeedMultiplier", bulletSpeedMultiplier, file);
        recoilControlMultiplierSprinting = readValue(split, "RecoilControlMultiplierSprinting", recoilControlMultiplierSprinting, file);
        moveSpeedMultiplier = readValue(split, "MovementSpeedMultiplier", moveSpeedMultiplier, file);
        moveSpeedMultiplier = readValue(split, "MoveSpeedModifier", moveSpeedMultiplier, file);

        //Scope Variables
        minZoom = readValue(split, "MinZoom", minZoom, file);
        maxZoom = readValue(split, "MaxZoom", maxZoom, file);
        zoomAugment = readValue(split, "ZoomAugment", zoomAugment, file);
        zoomFactor = readValue(split, "ZoomLevel", zoomFactor, file);
        fovFactor = readValue(split, "FOVZoomLevel", fovFactor, file);

        overlayName = readValue(split, "ZoomOverlay", overlayName, file).toLowerCase();

        hasNightVision = readValue(split, "HasNightVision", hasNightVision, file);
    }

    @Override
    protected void postRead()
    {
        super.postRead();

        attachmentType = EnumAttachmentType.get(attachmentTypeString);

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
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }

    @Nullable
    public static AttachmentType getFromNBT(CompoundTag tags) {
        ItemStack stack = ItemStack.of(tags);
        //TODO: implement AttachmentItem
        /*if (!stack.isEmpty() && stack.getItem() instanceof ItemAttachment attachment) {
            return attachment.type;
        }*/
        return null;
    }
}
