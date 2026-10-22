package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.*;
import com.flansmodultimate.api.ProjectileSources.Source;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.weapons.PilotGun;
import com.flansmodultimate.common.driveables.weapons.ShootPoint;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.ShootableFactory;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketParticle;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.util.ModUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.IntPredicate;

/** Shared projectile construction for addons, with no client or Custom NPCs class dependencies. */
public final class ExternalProjectileSupport
{
    private ExternalProjectileSupport()
    {}

    public static boolean isProjectile(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ShootableItem item && (item.getConfigType() instanceof BulletType || item.getConfigType() instanceof GrenadeType);
    }

    public static boolean isGrenade(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ShootableItem item && item.getConfigType() instanceof GrenadeType;
    }

    public static int roundsPerItem(ItemStack stack)
    {
        return Math.max(0, ShootableItem.getMaxRounds(stack));
    }

    public static Optional<ProjectileSources> getSources(ItemStack ammunition, ItemStack heldWeapon, @Nullable IContentType weapon, @Nullable IContentType platform, boolean secondary,
        boolean weaponStats)
    {
        if (!isProjectile(ammunition))
            return Optional.empty();
        ShootableType ammo = ((ShootableItem) ammunition.getItem()).getConfigType();
        InfoType definition = weaponDefinition(weapon, platform, heldWeapon, ammo);
        return Optional.of(settingSources(ammo, definition, heldWeapon, platform instanceof InfoType type ? type : null, secondary, weaponStats));
    }

    static ProjectileSources settingSources(ShootableType ammo, InfoType definition, @Nullable ItemStack heldWeapon, @Nullable InfoType platform, boolean secondary, boolean weaponStats)
    {
        boolean contentStats = weaponStats && (definition instanceof GunType || definition instanceof AAGunType || definition instanceof DriveableType);
        Source spread = contentStats ? Source.WEAPON : Source.CALLER;
        if (ammo instanceof GrenadeType)
            return new ProjectileSources(Source.AMMUNITION, spread, Source.AMMUNITION, true);
        BulletType bullet = (BulletType) ammo;
        FireableGun reference = new FireableGun(definition, 1F, 0F, 0F, EnumSpreadPattern.CIRCLE);
        AmmoOverride override = new FiredShot(reference, bullet, null, null, 0, platform).getAmmoOverride();
        Source damage = allRounds(bullet, override, round -> override.resolveMass(bullet, round) > 0F) ? Source.AMMUNITION : spread;
        Source speed = ammunitionSpeed(bullet, override) ? Source.AMMUNITION : weaponSpeedSource(definition, contentStats);
        return new ProjectileSources(damage, spread, speed, hasShootSound(definition, heldWeapon, secondary));
    }

    private static Source weaponSpeedSource(InfoType definition, boolean contentStats)
    {
        if (contentStats && (definition instanceof GunType || definition instanceof DriveableType driveable && driveable.getBulletSpeed() > 0F))
            return Source.WEAPON;
        return Source.CALLER;
    }

    private static boolean allRounds(BulletType bullet, AmmoOverride override, IntPredicate predicate)
    {
        List<BulletType.RoundEntry> rounds = override.rounds();
        if (!override.hasRounds() && bullet.hasDifferentRounds())
            rounds = bullet.getPeriod();
        if (rounds.isEmpty())
            return predicate.test(0);
        int position = 0;
        for (BulletType.RoundEntry round : rounds)
        {
            if (!predicate.test(position))
                return false;
            position += round.count();
        }
        return true;
    }

    private static boolean ammunitionSpeed(BulletType bullet, AmmoOverride override)
    {
        if (override.bulletSpeedBlocksPerTick() != null)
            return true;
        if (!override.hasRounds() || !bullet.hasDifferentRounds())
            return allRounds(bullet, override, round -> override.resolveBulletSpeed(bullet, round, 0F, false) > 0F);
        // A replacement round with no velocity falls through to the original belt, which can have a different period.
        int ammoPeriod = bullet.getPeriod().stream().mapToInt(BulletType.RoundEntry::count).sum();
        int modulus = greatestCommonDivisor(override.periodLength(), ammoPeriod);
        int position = 0;
        for (BulletType.RoundEntry round : override.rounds())
        {
            if (round.stats().bulletSpeed() <= 0F && velocityFallsThrough(bullet.getPeriod(), position, round.count(), modulus))
                return false;
            position += round.count();
        }
        return true;
    }

    private static boolean velocityFallsThrough(List<BulletType.RoundEntry> original, int start, int count, int modulus)
    {
        int position = 0;
        for (BulletType.RoundEntry round : original)
        {
            if (round.stats().bulletSpeed() <= 0F && overlappingPhases(start, count, position, round.count(), modulus))
                return true;
            position += round.count();
        }
        return false;
    }

    private static boolean overlappingPhases(int first, int firstCount, int second, int secondCount, int modulus)
    {
        // Two repeating intervals can share a shot when their residue intervals overlap modulo gcd(period1, period2).
        if (firstCount >= modulus || secondCount >= modulus)
            return true;
        int distance = Math.floorMod(second - first, modulus);
        return distance < firstCount || (long) distance + secondCount > modulus;
    }

    private static int greatestCommonDivisor(int first, int second)
    {
        while (second != 0)
        {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }

    private static boolean hasShootSound(InfoType definition, @Nullable ItemStack heldWeapon, boolean secondary)
    {
        if (definition instanceof GunType gun)
            return StringUtils.isNotBlank(gun.getShootSound(matchingGunStack(gun, heldWeapon), false));
        if (definition instanceof AAGunType aa)
            return StringUtils.isNotBlank(aa.getShootSound());
        return definition instanceof DriveableType driveable && StringUtils.isNotBlank(driveable.shootSound(secondary));
    }

    public static Optional<ProjectileShot> prepare(LivingEntity shooter, ItemStack ammunition, ItemStack heldWeapon, @Nullable IContentType weapon, @Nullable IContentType platform, boolean secondary,
        ProjectileParameters parameters)
    {
        if (!serverThread(shooter) || !isProjectile(ammunition) || !Float.isFinite(parameters.damage()) || !Float.isFinite(parameters.spread()) || !Float.isFinite(parameters.speed()))
            return Optional.empty();
        ShootableType ammo = ((ShootableItem) ammunition.getItem()).getConfigType();
        InfoType definition = weaponDefinition(weapon, platform, heldWeapon, ammo);
        InfoType platformType = platform instanceof InfoType type ? type : null;
        FireableGun fireable = resolveWeapon(definition, heldWeapon, shooter, platformType, secondary, parameters);
        int count = projectileCount(definition, heldWeapon, ammo, parameters.weaponStats());
        fireable.applyAmmunition(ammo);
        return Optional.of(new Prepared(shooter, ammo, fireable, platformType, Math.max(0, parameters.round()), Math.max(1, Math.min(128, count))));
    }

    private static InfoType weaponDefinition(@Nullable IContentType weapon, @Nullable IContentType platform, ItemStack heldWeapon, ShootableType ammo)
    {
        if (weapon instanceof InfoType type)
            return type;
        if (platform instanceof InfoType type)
            return type;
        return heldWeapon.getItem() instanceof GunItem gun ? gun.getConfigType() : ammo;
    }

    private static int projectileCount(InfoType definition, ItemStack heldWeapon, ShootableType ammo, boolean weaponStats)
    {
        if (!weaponStats)
            return 1;
        if (definition instanceof GunType gun)
            return gun.getNumBullets(matchingGunStack(gun, heldWeapon), ammo);
        return definition instanceof AAGunType aa ? aa.getNumBullets() : Math.max(1, ammo.getNumBullets());
    }

    @Nullable
    private static ItemStack matchingGunStack(GunType gun, @Nullable ItemStack heldWeapon)
    {
        return heldWeapon != null && heldWeapon.getItem() instanceof GunItem item && item.getConfigType() == gun ? heldWeapon : null;
    }

    static FireableGun resolveWeapon(InfoType definition, @Nullable ItemStack heldWeapon, LivingEntity shooter, @Nullable InfoType platform, boolean secondary, ProjectileParameters parameters)
    {
        float fallbackSpeed = Math.max(0.01F, parameters.speed());
        if (!parameters.weaponStats())
            return fallbackWeapon(definition, parameters);
        if (definition instanceof GunType gun)
            return mountedGun(gun, heldWeapon, shooter, platform, secondary);
        if (definition instanceof AAGunType aa)
            return new FireableGun(aa, aa.getDamage(), aa.getBulletSpread(), fallbackSpeed, aa.getSpreadPattern());
        if (definition instanceof DriveableType driveable)
            return new FireableGun(driveable, Math.max(0F, secondary ? driveable.getDamageMultiplierSecondary() : driveable.getDamageMultiplierPrimary()), driveable.getBulletSpread(),
                driveable.getBulletSpeed() > 0F ? driveable.getBulletSpeed() : fallbackSpeed, EnumSpreadPattern.CIRCLE);
        return fallbackWeapon(definition, parameters);
    }

    private static FireableGun fallbackWeapon(InfoType definition, ProjectileParameters parameters)
    {
        return new FireableGun(definition, Math.max(0F, parameters.damage()), Math.max(0F, parameters.spread()), Math.max(0.01F, parameters.speed()), EnumSpreadPattern.CIRCLE);
    }

    private static FireableGun mountedGun(GunType gun, @Nullable ItemStack heldWeapon, LivingEntity shooter, @Nullable InfoType platform, boolean secondary)
    {
        ItemStack stack = matchingGunStack(gun, heldWeapon);
        FireableGun fireable = stack == null ? new FireableGun(gun) : new FireableGun(gun, stack, shooter, null, ModUtils.getEnumMovement(shooter), !shooter.onGround());
        if (platform instanceof DriveableType driveable && !driveable.isReadWeaponsFromGunTypes())
            fireable.multiplyDamage(secondary ? driveable.getDamageMultiplierSecondary() : driveable.getDamageMultiplierPrimary());
        return fireable;
    }

    public static List<WeaponMuzzle> getMuzzles(IContentType model, boolean secondary, int sequence, boolean alternate)
    {
        return getMuzzles(model, secondary, sequence, alternate, MountAim.REST);
    }

    public static List<WeaponMuzzle> getMuzzles(IContentType model, boolean secondary, int sequence, boolean alternate, MountAim aim)
    {
        if (model instanceof AAGunType aa)
            return aaMuzzles(aa, sequence, alternate, aim);
        if (!(model instanceof DriveableType type))
            return List.of();
        List<ShootPoint> points = type.shootPoints(secondary);
        List<WeaponMuzzle> muzzles = new ArrayList<>();
        List<WeaponMuzzle.Particle> particles = type.shootParticle(secondary).stream()
            .map(particle -> new WeaponMuzzle.Particle(particle.name(), previewVelocity(new Vec3(particle.x(), particle.y(), particle.z())))).toList();
        boolean alternatePoints = alternate && type.alternate(secondary);
        for (int i = 0; i < points.size(); i++)
        {
            if (alternatePoints && i != Math.floorMod(sequence, points.size()))
                continue;
            int barrelSequence = alternatePoints ? Math.floorDiv(sequence, points.size()) : sequence;
            muzzles.addAll(pointMuzzles(type, points.get(i), particles, barrelSequence, alternate, aim));
        }
        return List.copyOf(muzzles);
    }

    public static Optional<WeaponMuzzle> getSeatMuzzle(IContentType model, int seat, int sequence, MountAim turretAim)
    {
        SeatInfo info = model instanceof DriveableType type ? type.getSeat(seat) : null;
        if (info == null || info.getGunType() == null)
            return Optional.empty();
        DriveableType type = (DriveableType) model;
        int barrel = Math.floorMod(sequence, info.getGunBarrelCount());
        // As a crewed driveable lifts its seat guns, so the muzzle clears the gunner's own head.
        Vec3 position = LegacyDriveableCoordinates.toLocal(info.getGunOrigin(barrel)).add(0D, Driveable.PASSENGER_GUN_MOUNTED_OFFSET, 0D);
        if (turretAim.yaw() != 0F && EnumDriveablePart.isTurretMounted(info.getPart()))
            position = aimTurretPoint(type, position, new MountAim(turretAim.yaw(), 0F));
        List<WeaponMuzzle.Particle> particles = type.getShootParticlesPassenger(seat).stream()
            .map(particle -> new WeaponMuzzle.Particle(particle.name(), previewVelocity(new Vec3(particle.x(), particle.y(), particle.z())))).toList();
        return Optional.of(new WeaponMuzzle(position, Optional.of(info.getGunType()), particles));
    }

    private static List<WeaponMuzzle> pointMuzzles(DriveableType type, ShootPoint point, List<WeaponMuzzle.Particle> particles, int sequence, boolean alternate, MountAim aim)
    {
        boolean aimed = (aim.yaw() != 0F || aim.pitch() != 0F) && EnumDriveablePart.isTurretMounted(point.getRootPos().getPart());
        List<WeaponMuzzle> muzzles = new ArrayList<>();
        for (int barrel = 0; barrel < point.getBarrelCount(); barrel++)
        {
            if (alternate && barrel != Math.floorMod(sequence, point.getBarrelCount()))
                continue;
            var root = point.getRootPos().getPosition();
            var offset = point.getBarrelOffPos(barrel);
            Vec3 position = LegacyDriveableCoordinates.toLocal(new Vec3(root.x + offset.x, root.y + offset.y, root.z + offset.z));
            if (aimed)
                position = aimTurretPoint(type, position, aim);
            IContentType mounted = point.getRootPos() instanceof PilotGun pilot ? pilot.getType() : null;
            muzzles.add(new WeaponMuzzle(position, Optional.ofNullable(mounted), particles));
        }
        return muzzles;
    }

    /**
     * Carries a rest-pose point of the turret to where the aimed turret holds it, in the preview frame. The gun elevates
     * first, then the whole turret turns round TurretOrigin, as the vehicle model draws it. The pivot of the barrel on
     * the loaded model is only known to clients, so the gun elevates round TurretOrigin here.
     */
    static Vec3 aimTurretPoint(DriveableType type, Vec3 point, MountAim aim)
    {
        Vec3 pivot = LegacyDriveableCoordinates.toLocal(type.getTurretOrigin());
        Vec3 pitched = LegacyDriveableCoordinates.rotateBarrelPitchLocal(point.subtract(pivot), aim.pitch());
        // The preview frame is not mirrored as the entity frame is, so a turn to the right is the opposite rotation.
        Vec3 offset = LegacyDriveableCoordinates.toLocal(type.getTurretOriginOffset());
        Vec3 turned = LegacyDriveableCoordinates.rotateTurretYawLocal(pitched, -aim.yaw());
        return pivot.add(turned).add(LegacyDriveableCoordinates.rotateTurretYawLocal(offset, -aim.yaw())).subtract(offset);
    }

    private static Vec3 previewVelocity(Vec3 legacy)
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(legacy);
        return new Vec3(-local.x, local.y, local.z);
    }

    private static List<WeaponMuzzle> aaMuzzles(AAGunType type, int sequence, boolean alternate, MountAim aim)
    {
        // The preview frame faces the gun yaw of 180 degrees.
        float yaw = 180F + aim.yaw();
        List<WeaponMuzzle> muzzles = new ArrayList<>();
        Vec3[] pivots = type.getMeasuredBarrelPivots();
        Vec3[] tips = type.getMeasuredBarrelMuzzles();
        for (int barrel = 0; barrel < type.getNumBarrels(); barrel++)
        {
            if (alternate && type.isFireAlternately() && barrel != Math.floorMod(sequence, type.getNumBarrels()))
                continue;
            Vec3 offset = type.hasMeasuredBarrels() && !type.firesFromBarrelLine(barrel)
                ? AAGunBarrelGeometry.modelBarrelOffset(pivots[barrel], tips[barrel], yaw, aim.pitch())
                : AAGunBarrelGeometry.legacyBarrelOffset(type.getBarrelX()[barrel], type.getBarrelY()[barrel], type.getBarrelZ()[barrel], yaw, aim.pitch());
            muzzles.add(new WeaponMuzzle(offset, Optional.empty(), List.of()));
        }
        return List.copyOf(muzzles);
    }

    public static boolean playShootSound(Entity shooter, @Nullable IContentType weapon, ItemStack heldWeapon, boolean secondary)
    {
        if (!serverThread(shooter))
            return false;
        IContentType type = weapon;
        if (type == null && heldWeapon.getItem() instanceof GunItem gun)
            type = gun.getConfigType();
        String sound = "";
        double range = ModCommonConfig.get().gunFireSoundRange();
        boolean distort = true;
        boolean silenced = false;
        if (type instanceof GunType gun)
        {
            ItemStack stack = matchingGunStack(gun, heldWeapon);
            sound = gun.getShootSound(stack, false);
            range = gun.getGunSoundRange();
            distort = gun.isDistortSound();
            silenced = gun.isSilencedSound(stack);
        }
        else if (type instanceof AAGunType aa)
        {
            sound = aa.getShootSound();
            range = aa.getGunSoundRange();
        }
        else if (type instanceof DriveableType driveable)
            sound = driveable.shootSound(secondary);
        if (StringUtils.isBlank(sound))
            return false;
        PacketPlaySound.sendSoundPacket(shooter, range, sound, distort, silenced);
        return true;
    }

    /**
     * The sound a placed AA gun or driveable bank makes when it starts reloading. A bank of mounted guns uses the gun's
     * own reload sound before the bank's, like the driveable does; an ordnance bank uses its bank or shared sound.
     */
    public static boolean playReloadSound(Entity source, IContentType type, boolean secondary)
    {
        if (!serverThread(source))
            return false;
        String sound = "";
        double range = ModCommonConfig.get().reloadSoundRange();
        if (type instanceof AAGunType aa)
        {
            sound = aa.getReloadSound();
            range = aa.getReloadSoundRange();
        }
        else if (type instanceof DriveableType driveable && driveable.weaponType(secondary) == EnumWeaponType.GUN)
        {
            GunType gun = driveable.getPilotGunType(secondary);
            sound = StringUtils.firstNonBlank(gun == null ? null : gun.getReloadSound(null), secondary ? driveable.getReloadSoundSecondary() : driveable.getReloadSoundPrimary(), "");
            if (gun != null && StringUtils.isNotBlank(gun.getReloadSound(null)))
                range = gun.getReloadSoundRange();
        }
        else if (type instanceof DriveableType driveable)
            sound = driveable.reloadSound(secondary);
        if (StringUtils.isBlank(sound))
            return false;
        PacketPlaySound.sendSoundPacket(source, range, sound, false);
        return true;
    }

    /** The main gun's {@code ShootReloadSound}, a shell being worked into the breech. */
    public static boolean playChamberSound(Entity source, IContentType type)
    {
        if (!serverThread(source) || !(type instanceof DriveableType driveable) || StringUtils.isBlank(driveable.getShootReloadSound()))
            return false;
        PacketPlaySound.sendSoundPacket(source, ModCommonConfig.get().reloadSoundRange(), driveable.getShootReloadSound(), false);
        return true;
    }

    public static void shootParticles(Entity shooter, WeaponMuzzle muzzle, Vec3 origin, float bodyYaw, float scale)
    {
        if (!serverThread(shooter) || !finite(origin) || !Float.isFinite(bodyYaw) || !Float.isFinite(scale) || scale <= 0F)
            return;
        List<WeaponMuzzle.Particle> particles = muzzle.particles().isEmpty() ? List.of(new WeaponMuzzle.Particle(FlanParticles.FM_FLAME, Vec3.ZERO)) : muzzle.particles();
        for (WeaponMuzzle.Particle particle : particles)
        {
            Vec3 velocity = particle.velocity().yRot((float) Math.toRadians(180F - bodyYaw)).scale(scale);
            PacketHandler.sendToAllAround(new PacketParticle(particle.name(), origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z, scale), origin, 128D, shooter.level().dimension());
        }
    }

    private static boolean serverThread(Entity entity)
    {
        return entity.level() instanceof ServerLevel level && level.getServer().isSameThread();
    }

    private static boolean finite(Vec3 vector)
    {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private record Prepared(LivingEntity shooter, ShootableType ammo, FireableGun gun, @Nullable InfoType platform, int round, int count) implements ProjectileShot
    {
        @Override
        public float speed()
        {
            if (ammo instanceof BulletType bullet)
                return new FiredShot(gun, bullet, shooter, shooter, round, platform).getMuzzleVelocity();
            if (ammo instanceof GrenadeType grenade)
                return Math.max(0.01F, 0.5F * grenade.getThrowSpeed());
            throw new IllegalStateException("Unsupported projectile: " + ammo.getShortName());
        }

        @Override
        public double gravity()
        {
            return ModPhysics.gravity(ShootableType.FALL_SPEED_COEFFICIENT * ammo.getFallSpeed(), shooter.level());
        }

        @Override
        public double drag()
        {
            float base = ammo instanceof BulletType bullet ? bullet.getDragInAir() : ShootableType.AIR_DEFAULT_DRAG;
            return ModPhysics.dragRetention(base, shooter.level());
        }

        @Override
        public List<Entity> launch(Vec3 origin, Vec3 direction, boolean throwSound)
        {
            if (!serverThread(shooter) || !shooter.isAlive() || !finite(origin) || !finite(direction) || direction.lengthSqr() < 1.0E-12D)
                return List.of();
            List<Entity> entities = new ArrayList<>();
            Vec3 aim = direction.normalize();
            for (int i = 0; i < count; i++)
            {
                Entity projectile;
                if (ammo instanceof GrenadeType grenade)
                {
                    Vec3 velocity = ShootingHelper.calculateShootingMotionVector(shooter.getRandom(), aim, gun.getSpread(), speed(), gun.getSpreadPattern());
                    projectile = new Grenade(shooter.level(), grenade, origin, velocity.normalize(), shooter, throwSound && i == 0);
                    projectile.setDeltaMovement(velocity);
                }
                else
                    projectile = ShootableFactory.createBullet(shooter.level(), new FiredShot(gun, (BulletType) ammo, shooter, shooter, round, platform), origin, aim);
                if (shooter.level().addFreshEntity(projectile))
                    entities.add(projectile);
            }
            return List.copyOf(entities);
        }
    }
}
