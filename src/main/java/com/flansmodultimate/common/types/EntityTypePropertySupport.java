package com.flansmodultimate.common.types;

import com.flansmodultimate.api.EntityTypeProperties;
import com.flansmodultimate.api.EntityTypeProperties.Sound;
import com.flansmodultimate.api.EntityTypeProperties.Weapon;
import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.common.driveables.physics.MechaPhysics;
import com.flansmodultimate.common.driveables.weapons.PilotGun;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/** Type inspection shared by addons; no Custom NPCs, entity creation or client dependencies. */
public final class EntityTypePropertySupport
{
    private EntityTypePropertySupport()
    {}

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
