package com.flansmodultimate.common.types;

import com.flansmodultimate.api.*;
import com.flansmodultimate.api.EntityTypeProperties.Sound;
import com.flansmodultimate.api.EntityTypeProperties.Weapon;
import com.flansmodultimate.common.driveables.EngineSoundPitch;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.physics.DriveableControlPhysics;
import com.flansmodultimate.common.driveables.physics.MechaPhysics;
import com.flansmodultimate.common.driveables.weapons.PilotGun;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.item.ShootableItem;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Type inspection shared by addons; no Custom NPCs, entity creation or client dependencies. */
public final class EntityTypePropertySupport
{
    private EntityTypePropertySupport()
    {}

    public static Optional<EngineSound> engineSound(IContentType type)
    {
        if (!(type instanceof DriveableType driveable) || type instanceof MechaType || StringUtils.isBlank(driveable.getEngineSound()))
            return Optional.empty();
        int length = driveable.getEngineSoundLength();
        if (length <= 0)
            length = driveable.getStartSoundLength();
        return Optional.of(new EngineSound(driveable.getEngineSound(), driveable.getEngineSoundRange(), Math.max(1, length > 0 ? length : 20)));
    }

    public static Optional<EngineSound> engineIdleSound(IContentType type)
    {
        if (!(type instanceof DriveableType driveable) || type instanceof MechaType || StringUtils.isBlank(driveable.getEngineIdleLoopSound()))
            return Optional.empty();
        String sound = driveable.getEngineIdleLoopSound();
        int duration = driveable.getEngineSoundLength();
        if (sound.equals(driveable.getIdleSound()))
            duration = driveable.getIdleSoundLength();
        else if (sound.equals(driveable.getStartSound()))
            duration = driveable.getStartSoundLength();
        return Optional.of(new EngineSound(sound, driveable.getEngineSoundRange(), Math.max(1, duration > 0 ? duration : 20)));
    }

    public static float enginePitch(IContentType type, float movement)
    {
        var defaults = type instanceof PlaneType ? new EngineSoundPitch(0.5F, 1F, 1.5F) : new EngineSoundPitch(0.5F, 0.8F, 1.2F);
        var curve = type instanceof DriveableType driveable ? driveable.getEngineSoundPitchCurve(defaults) : defaults;
        return Math.min(2F, Math.max(0.5F, DriveableControlPhysics.engineSoundPitch(movement, curve, 1F)));
    }

    public static Optional<EntityTypeProperties> read(IContentType type, boolean secondary)
    {
        return read(type, secondary, true);
    }

    public static Optional<EntityTypeProperties> read(IContentType type, boolean secondary, boolean mountedWeapons)
    {
        if (type instanceof AAGunType aa)
        {
            Weapon weapon = new Weapon(aa.getShortName(), aa.getDamage(), aa.getBulletSpread(), OptionalDouble.empty(), Math.max(1, aa.getNumBullets()), aa.getShootSound());
            return Optional.of(new EntityTypeProperties(positive(aa.getHealth()), positive(aa.getTargetRange()), OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), Optional.empty(),
                positive(aa.getShootDelay()), Map.of(), List.of(weapon)));
        }
        if (!(type instanceof DriveableType driveable))
            return Optional.empty();
        MechaType mecha = driveable instanceof MechaType selected ? selected : null;
        Map<Sound, String> sounds = new LinkedHashMap<>();
        putSound(sounds, Sound.IDLE, driveable.getIdleSound());
        putSound(sounds, Sound.STEP, mecha == null ? driveable.getEngineSound() : mecha.getStompSound());
        OptionalDouble speed = mecha == null ? OptionalDouble.empty() : nonnegative(MechaPhysics.movementSpeed(mecha.getMoveSpeed(), mecha.getRealWorldSpec().maxSpeedKmh(), 1F, 1F));
        boolean fallDamage = driveable.getFallDamageFactor() > 0F && (mecha == null || mecha.isTakeFallDamage() && mecha.getFallDamageMultiplier() > 0F);
        return Optional.of(new EntityTypeProperties(positive(driveable.getTotalHp()), OptionalDouble.empty(), speed, mecha == null ? OptionalDouble.empty() : nonnegative(mecha.getReach()),
            Optional.of(driveable.isWorksUnderWater()), Optional.of(fallDamage), positive(driveable.shootDelay(secondary)), sounds,
            mountedWeapons ? weapons(driveable, secondary) : List.of(platformWeapon(driveable, secondary))));
    }

    /**
     * The firing and reloading rhythm a placed AA gun or a driveable bank keeps with this ammunition. An AA gun empties
     * every ammunition slot before it reloads; a driveable bank reloads after its magazine, the smaller of its declared
     * reload rounds and what one item holds.
     */
    public static Optional<ReloadCycle> reloadCycle(IContentType type, boolean secondary, ItemStack ammunition)
    {
        int perItem = Math.max(1, ShootableItem.getMaxRounds(ammunition));
        float multiplier = ammunition.getItem() instanceof ShootableItem shootable ? shootable.getConfigType().getReloadTimeMultiplier() : 1F;
        if (type instanceof AAGunType aa)
        {
            int barrels = Math.max(1, aa.getNumBarrels());
            int rounds = perItem * Math.max(1, aa.getAmmoSlotCount());
            // Firing together spends a round from every barrel per trigger; alternating spends one.
            int together = aa.isShareAmmo() ? (int) Math.ceil(perItem / (double) barrels) : perItem;
            int volleys = aa.isFireAlternately() ? rounds : together;
            return Optional.of(new ReloadCycle(volleys, aa.getShootDelay(), Math.max(aa.getReloadTime() * multiplier, aa.getShootDelay()), OptionalInt.empty()));
        }
        if (!(type instanceof DriveableType driveable) || driveable.weaponType(secondary) == EnumWeaponType.NONE)
            return Optional.empty();
        boolean gunBank = driveable.weaponType(secondary) == EnumWeaponType.GUN;
        int declared = driveable.reloadRounds(secondary);
        int magazine = declared > 0 ? Math.min(declared, perItem) : perItem;
        // Only a bank of mounted guns slows down for heavy ammunition; ordnance banks chamber at their own pace.
        double reload = driveable.reloadTime(secondary) * (gunBank ? multiplier : 1F);
        boolean chamberSound = !gunBank && !secondary && driveable.getReloadSoundTick() != DriveableType.RELOAD_SOUND_TICK_UNSET && StringUtils.isNotBlank(driveable.getShootReloadSound());
        return Optional.of(new ReloadCycle(magazine, driveable.shootDelay(secondary), reload, chamberSound ? OptionalInt.of(driveable.getReloadSoundTick()) : OptionalInt.empty()));
    }

    private static List<Weapon> weapons(DriveableType type, boolean secondary)
    {
        Map<String, Weapon> weapons = new LinkedHashMap<>();
        for (var point : type.shootPoints(secondary))
        {
            if (point.getRootPos() instanceof PilotGun pilot && pilot.getType() != null)
            {
                GunType gun = pilot.getType();
                FireableGun fireable = new FireableGun(gun);
                if (!type.isReadWeaponsFromGunTypes())
                    fireable.multiplyDamage(secondary ? type.getDamageMultiplierSecondary() : type.getDamageMultiplierPrimary());
                weapons.putIfAbsent(gun.getShortName(), weapon(gun.getShortName(), fireable, gun.getNumBullets(null, null), gun.getShootSound(null, false)));
            }
            else
                weapons.putIfAbsent(type.getShortName(), platformWeapon(type, secondary));
        }
        // A model with no authored muzzles still supplies its own bank defaults when firing from eye height.
        if (weapons.isEmpty())
            weapons.put(type.getShortName(), platformWeapon(type, secondary));
        return List.copyOf(weapons.values());
    }

    private static Weapon platformWeapon(DriveableType type, boolean secondary)
    {
        FireableGun fireable = new FireableGun(type, secondary ? type.getDamageMultiplierSecondary() : type.getDamageMultiplierPrimary(), type.getBulletSpread(), type.getBulletSpeed(),
            EnumSpreadPattern.CIRCLE);
        return weapon(type.getShortName(), fireable, 1, type.shootSound(secondary));
    }

    private static Weapon weapon(String name, FireableGun fireable, int pellets, String sound)
    {
        return new Weapon(name, fireable.getDamage(), fireable.getSpread(), positive(fireable.getBulletSpeed()), Math.max(1, pellets), sound);
    }

    private static void putSound(Map<Sound, String> sounds, Sound role, String sound)
    {
        if (StringUtils.isNotBlank(sound))
            sounds.put(role, sound);
    }

    private static OptionalDouble positive(double value)
    {
        return Double.isFinite(value) && value > 0D ? OptionalDouble.of(value) : OptionalDouble.empty();
    }

    private static OptionalDouble nonnegative(double value)
    {
        return Double.isFinite(value) && value >= 0D ? OptionalDouble.of(value) : OptionalDouble.empty();
    }
}
