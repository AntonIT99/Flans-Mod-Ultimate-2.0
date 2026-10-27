package com.flansmodultimate.common.types;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.GroundHandling;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.entity.EngineSound;
import com.flansmodultimate.api.entity.EntityTypeProperties;
import com.flansmodultimate.api.entity.EntityTypeProperties.Sound;
import com.flansmodultimate.api.entity.EntityTypeProperties.Weapon;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.physics.DriveableControlPhysics;
import com.flansmodultimate.common.driveables.physics.MechaPhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.common.driveables.weapons.PilotGun;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.config.ModCommonConfig;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Type inspection shared by addons; no Custom NPCs, entity creation or client dependencies. */
public final class EntityTypePropertySupport
{
    /** Traverse and elevation speed, in degrees per tick, of a mount whose definition states none. */
    private static final float DEFAULT_AIM_SPEED = 8F;
    /** Radians of yaw per tick Vehicle gives a legacy wheeled hull per unit of steering control, turn speed and speed scale. */
    private static final double LEGACY_STEERING_SCALE = 0.1D;
    /** Blocks per tick across the ground above which a driveable that squashes mobs hurts them, and the most it deals. */
    public static final double SQUASH_SPEED = 0.12D;
    private static final double MAX_SQUASH_DAMAGE = 40D;

    private EntityTypePropertySupport()
    {}

    @SuppressWarnings("unused") // Headless IntelliJ loses this public API delegation across source sets.
    public static GroundHandling groundHandling(IContentType type)
    {
        return groundHandling(type, false);
    }

    /**
     * @param water
     *            whether the vehicle is in water; a legacy wheeled hull there turns on a circle its turn speeds set, as
     *            Vehicle steers it with the control held
     */
    public static GroundHandling groundHandling(IContentType type, boolean water)
    {
        if (!(type instanceof VehicleType vehicle))
            return GroundHandling.EMPTY;
        boolean legacy = ModCommonConfig.forceLegacyVehiclePhysics();
        OptionalDouble rate = OptionalDouble.empty();
        OptionalDouble radius = OptionalDouble.empty();
        if (vehicle.usesRealTurnRate(legacy, vehicle.getPushSpeedKmh() > 0F, vehicle.isTracked()))
        {
            rate = OptionalDouble.of(vehicle.getRealTurnRateDegPerSec());
            Float reference = vehicle.getRealWorldSpec().maxSpeedKmh();
            if (!vehicle.isTracked() && reference != null)
            {
                double speed = VehiclePhysicsUnits.kmhToBlocksPerTick(reference) * ModCommonConfig.realisticSpeedScale(vehicle.getResolvedPhysics().category());
                radius = OptionalDouble.of(speed / Math.toRadians(rate.getAsDouble() / 20D));
            }
        }
        else if (water && !vehicle.isTracked())
        {
            double modifier = (vehicle.getTurnLeftModifier() + vehicle.getTurnRightModifier()) * 0.5D;
            if (modifier > 0D)
                radius = OptionalDouble.of(1D / (DriveableControlPhysics.wheeledSteeringVelocityScale(1D) * LEGACY_STEERING_SCALE * DriveableControlPhysics.heldControl() * modifier));
        }
        return new GroundHandling(rate, radius, reverseShare(vehicle, legacy));
    }

    private static OptionalDouble reverseShare(VehicleType vehicle, boolean legacy)
    {
        if (vehicle.getMaxNegativeThrottle() <= 0F)
            return OptionalDouble.of(0D);
        if (legacy)
            return OptionalDouble.empty();
        Float forward = vehicle.getRealWorldSpec().maxSpeedKmh();
        Float backward = vehicle.getRealWorldSpec().ground().maxReverseSpeedKmh();
        return forward != null && Float.isFinite(forward) && forward > 0F && backward != null && Float.isFinite(backward) && backward > 0F
            ? OptionalDouble.of(backward / (double) forward)
            : OptionalDouble.empty();
    }

    public static List<WeaponMount> mounts(IContentType type)
    {
        if (type instanceof AAGunType aa)
        {
            float side = Math.abs(aa.getSideViewLimit());
            MountAim minimum = new MountAim(side >= 180F ? -360F : -side, -Math.abs(aa.getTopViewLimit()));
            MountAim maximum = new MountAim(side >= 180F ? 360F : side, aa.getBottomViewLimit());
            return List.of(new WeaponMount(0, true, false, minimum, maximum, DEFAULT_AIM_SPEED, DEFAULT_AIM_SPEED, Vec3.ZERO, Vec3.ZERO, Optional.empty()));
        }
        if (!(type instanceof DriveableType driveable))
            return List.of();
        List<WeaponMount> mounts = new ArrayList<>();
        for (SeatInfo seat : driveable.getSeats())
        {
            if (seat == null)
                continue;
            float yawSpeed = Mth.clamp(Math.abs(seat.getAimingSpeed().x), 0.25F, 45F);
            float pitchSpeed = Mth.clamp(Math.abs(seat.getAimingSpeed().y), 0.25F, 45F);
            boolean onTurret = EnumDriveablePart.isTurretMounted(seat.getPart());
            Vec3 position = LegacyDriveableCoordinates.toLocal(seat.getPosition());
            // As Driveable seats a rider: RotatedOffset always turns with the turret, TurretOriginOffset only for turret seats.
            Vec3 turretOffset = LegacyDriveableCoordinates.toLocal(seat.getRotatedOffset());
            if (onTurret && driveable.getTurretOriginOffset() != null)
                turretOffset = turretOffset.add(LegacyDriveableCoordinates.toLocal(driveable.getTurretOriginOffset()));
            mounts.add(new WeaponMount(seat.getId(), seat.isDriver(), onTurret, new MountAim(seat.getMinYaw(), seat.getMinPitch()), new MountAim(seat.getMaxYaw(), seat.getMaxPitch()), yawSpeed,
                pitchSpeed, position, onTurret ? LegacyDriveableCoordinates.toLocal(driveable.getTurretOrigin()) : position, Optional.ofNullable(seat.getGunType()), turretOffset));
        }
        mounts.sort(Comparator.comparingInt(WeaponMount::seat));
        return List.copyOf(mounts);
    }

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

    public static OptionalDouble squashDamage(IContentType type, double horizontalSpeed)
    {
        boolean squashes = type instanceof VehicleType vehicle && vehicle.isSquashMobs() || type instanceof MechaType mecha && mecha.isSquashMobs();
        return squashes && horizontalSpeed > SQUASH_SPEED ? OptionalDouble.of(squashDamage(horizontalSpeed)) : OptionalDouble.empty();
    }

    /** @return what a driveable that squashes mobs deals one it runs into at a speed across the ground in blocks per tick */
    public static double squashDamage(double horizontalSpeed)
    {
        return Math.min(MAX_SQUASH_DAMAGE, 2D + horizontalSpeed * 12D);
    }

    public static Optional<EntityTypeProperties> read(IContentType type, boolean secondary)
    {
        return read(type, secondary, true);
    }

    public static Optional<EntityTypeProperties> read(IContentType type, boolean secondary, boolean mountedWeapons)
    {
        if (type instanceof AAGunType aa)
        {
            // An AA gun fires at the default every Flan bullet has, unless its ammunition states a velocity.
            Weapon weapon = new Weapon(aa.getShortName(), aa.getDamage(), aa.getBulletSpread(), OptionalDouble.of(BulletType.DEFAULT_BULLET_SPEED), Math.max(1, aa.getNumBullets()),
                aa.getShootSound());
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
        float multiplier = ammunition.getItem() instanceof ShootableItem shootable ? shootable.getConfigType().getReloadTimeMultiplier() : 1F;
        return reloadCycle(type, secondary, ShootableItem.getMaxRounds(ammunition), multiplier);
    }

    /**
     * @param roundsPerItem
     *            rounds one item of the ammunition holds; anything below one counts as one
     * @param reloadMultiplier
     *            the ammunition's reload time multiplier
     */
    public static Optional<ReloadCycle> reloadCycle(IContentType type, boolean secondary, int roundsPerItem, float reloadMultiplier)
    {
        int perItem = Math.max(1, roundsPerItem);
        float multiplier = Float.isFinite(reloadMultiplier) && reloadMultiplier > 0F ? reloadMultiplier : 1F;
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

    /** A seat gun reloads after every item of its ammunition, as a crewed driveable's does, never faster than it fires. */
    public static Optional<ReloadCycle> seatReloadCycle(IContentType type, int seat, ItemStack ammunition)
    {
        SeatInfo info = type instanceof DriveableType driveable ? driveable.getSeat(seat) : null;
        GunType gun = info == null ? null : info.getGunType();
        if (gun == null)
            return Optional.empty();
        float delay = gun.getShootDelay(null);
        return Optional.of(new ReloadCycle(ShootableItem.getMaxRounds(ammunition), delay, Math.max(gun.getReloadTime(), delay), OptionalInt.empty()));
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
