package com.flansmodultimate.common.types;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor
public class GrenadeType extends ShootableType
{
    protected static final float DEFAULT_BOUNCINESS = 0.9F;

    // Misc
    /** The damage imparted by smacking someone over the head with this grenade */
    @Getter
    protected int meleeDamage = 1;

    // Throwing
    /** The delay between subsequent grenade throws */
    @Getter
    protected int throwDelay;
    /** The sound to play upon throwing this grenade */
    @Getter
    protected String throwSound = StringUtils.EMPTY;
    /** The name of the item to drop (if any) when throwing the grenade */
    @Getter
    protected String dropItemOnThrow = null;
    /** Whether you can throw this grenade by right clicking */
    @Getter
    protected boolean canThrow = true;

    // Labjac Edition hold-to-throw, opt-in per grenade
    /**
     * If true, holding and releasing attack throws overhand and holding and releasing use tosses underhand.
     * The grenade is spent only when released, so its fuse also starts then.
     */
    @Getter
    protected boolean holdToThrow;
    @Getter
    protected float overhandThrowSpeedMultiplier = 1F;
    @Getter
    protected float underhandThrowSpeedMultiplier = 0.45F;
    /** Horizontal speed kept after bouncing off a floor or ceiling */
    @Getter
    protected float grenadeGroundFriction = 1F;
    /** Speed kept per tick in flight and after bouncing off a wall */
    @Getter
    protected float grenadeAirDrag = 1F;
    /** A grenade slower than this after a bounce comes to rest; 0.03 by default for hold-to-throw grenades, else 0 */
    @Getter
    protected float grenadeStopSpeed;

    // Physics
    @Getter
    protected boolean penetratesBlocks;
    /** The sound to play upon bouncing off a surface */
    @Getter
    protected String bounceSound = StringUtils.EMPTY;
    /** Whether the grenade should stick to surfaces */
    @Getter
    protected boolean sticky;
    /** If true, then the grenade will stick to the player that threw it. Used to make delayed self destruct weapons */
    @Getter
    protected boolean stickToThrower;
    @Getter
    protected boolean stickToEntity;
    @Getter
    protected boolean stickToDriveable;
    @Getter
    protected boolean stickToEntityAfter;
    @Getter
    protected boolean allowStickSound;
    @Getter
    protected int stickSoundRange = 10;
    @Getter
    protected String stickSound;

    @Getter
    protected boolean flashBang;
    @Getter
    protected int flashTime = 200;
    @Getter
    protected int flashRange = 8;

    @Getter
    protected boolean flashSoundEnable;
    @Getter
    protected int flashSoundRange = 16;
    @Getter
    protected String flashSound;

    @Getter
    protected boolean flashDamageEnable;
    @Getter
    protected float flashDamage;

    @Getter
    protected boolean flashEffects;
    @Getter
    protected int flashEffectsDuration;
    @Getter
    protected int flashEffectsLevel;
    @Getter
    protected List<MobEffectInstance> flashEffectInstances = new ArrayList<>();

    // Conditions for detonation
    @Getter
    protected boolean detonateWhenShot;
    /** If true, then this grenade can be detonated by any remote detonator tool */
    @Getter
    protected boolean remote;

    // Aesthetics
    /** Whether the grenade should spin when thrown. Generally false for mines or things that should lie flat */
    @Getter
    protected boolean spinWhenThrown = true;

    // Deployed bag functionality
    /** If true, then right clicking this "grenade" will give the player health or buffs or ammo as defined below */
    @Getter
    protected boolean isDeployableBag;
    /** The number of times players can use this bag before it runs out */
    @Getter
    protected int numUses = 1;
    /** The amount to heal the player using this bag */
    @Getter
    protected float healAmount;
    /** The potion effects to apply to users of this bag */
    @Getter
    protected List<MobEffectInstance> potionEffects = new ArrayList<>();
    /**
     * The number of clips to give to the player when using this bag
     * When they right click with a gun, they will get this number of clips for that gun.
     * They get the first ammo type, as listed in the gun type file
     * The number of clips they get is multiplied by numBulletsInGun too
     */
    @Getter
    protected int numClips;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        if (!file.hasConfigLine("Bounciness") || !hasValueForConfigField("Bounciness", file))
            bounciness = DEFAULT_BOUNCINESS;

        meleeDamage = readValue("MeleeDamage", meleeDamage, file);

        // Grenade Throwing
        throwDelay = readValue("ThrowDelay", throwDelay, file);
        meleeDamage = readValue("MeleeDamage", meleeDamage, file);
        throwSound = readSound("ThrowSound", throwSound, file);
        dropItemOnThrow = readValue("DropItemOnThrow", dropItemOnThrow, file);
        canThrow = readValue("CanThrow", canThrow, file);
        readHoldToThrow(file);
        penetratesBlocks = readValue("PenetratesBlocks", penetratesBlocks, file);
        bounceSound = readSound("BounceSound", bounceSound, file);

        // Sticky settings
        sticky = readValue("Sticky", sticky, file);
        stickToThrower = readValue("StickToThrower", stickToThrower, file);
        stickToEntity = readValue("StickToEntity", stickToEntity, file);
        stickToDriveable = readValue("StickToDriveable", stickToDriveable, file);
        stickToEntityAfter = readValue("StickToEntityAfter", stickToEntityAfter, file);
        allowStickSound = readValue("AllowStickSound", allowStickSound, file);
        stickSoundRange = readValue("StickSoundRange", stickSoundRange, file);
        stickSound = readSound("StickSound", stickSound, file);

        spinWhenThrown = readValue("SpinWhenThrown", spinWhenThrown, file);
        remote = readValue("Remote", remote, file);

        flashBang = readValue("FlashBang", flashBang, file);
        flashTime = readValue("FlashTime", flashTime, file);
        flashRange = readValue("FlashRange", flashRange, file);
        flashSoundEnable = readValue("FlashSoundEnable", flashSoundEnable, file);
        flashSoundRange = readValue("FlashSoundRange", flashSoundRange, file);
        flashSound = readSound("FlashSound", flashSound, file);
        flashDamageEnable = readValue("FlashDamageEnable", flashDamageEnable, file);
        flashDamage = readValue("FlashDamage", flashDamage, file);
        flashEffects = readValue("FlashEffects", flashEffects, file);
        flashEffectsDuration = readValue("FlashEffectsDuration", flashEffectsDuration, file);
        flashEffectsLevel = readValue("FlashEffectsLevel", flashEffectsLevel, file);
        addEffects("FlashEffectsID", flashEffectInstances, file, false, false, flashEffectsDuration, flashEffectsLevel);
        addEffects("AddFlashEffect", flashEffectInstances, file, false, false, flashEffectsDuration, flashEffectsLevel);
        flashBang = readValue("FlashBang", flashBang, file);

        detonateWhenShot = readValue("DetonateWhenShot", detonateWhenShot, file);

        // Deployable Bag Stuff
        isDeployableBag = readFieldWithOptionalValue("DeployableBag", isDeployableBag, file);

        numUses = readValue("NumUses", numUses, file);
        healAmount = readValue("HealAmount", healAmount, file);

        addEffects("AddPotionEffect", potionEffects, file, false, false);
        addEffects("PotionEffect", potionEffects, file, false, false);

        numClips = readValue("NumClips", numClips, file);
    }

    private void readHoldToThrow(TypeFile file)
    {
        holdToThrow = readValue("HoldToThrow", holdToThrow, file);
        overhandThrowSpeedMultiplier = nonNegative(readValue("OverhandThrowSpeedMultiplier", overhandThrowSpeedMultiplier, file), 1F);
        underhandThrowSpeedMultiplier = nonNegative(readValue("UnderhandThrowSpeedMultiplier", underhandThrowSpeedMultiplier, file), 0.45F);
        grenadeGroundFriction = nonNegative(readValue("GrenadeGroundFriction", grenadeGroundFriction, file), 1F);
        grenadeAirDrag = nonNegative(readValue("GrenadeAirDrag", grenadeAirDrag, file), 1F);
        float defaultStopSpeed = holdToThrow ? 0.03F : 0F;
        grenadeStopSpeed = nonNegative(readValue("GrenadeStopSpeed", defaultStopSpeed, file), defaultStopSpeed);
    }

    /**
     * Turns an ordinary throw of this grenade into a hand throw. Overhand throws lob half again as high; underhand
     * tosses climb at 0.6 times the rate with a small upward kick. Both are scaled by their throw speed multiplier.
     */
    public Vec3 handThrowVelocity(Vec3 legacyThrow, boolean underhand)
    {
        float multiplier = underhand ? underhandThrowSpeedMultiplier : overhandThrowSpeedMultiplier;
        double y = legacyThrow.y * multiplier * (underhand ? 0.6D : 1.5D);
        if (underhand)
            y += 0.08D * throwSpeed;
        return new Vec3(legacyThrow.x * multiplier, y, legacyThrow.z * multiplier);
    }

    private static float nonNegative(float value, float fallback)
    {
        return Float.isFinite(value) ? Math.max(0F, value) : fallback;
    }
}
