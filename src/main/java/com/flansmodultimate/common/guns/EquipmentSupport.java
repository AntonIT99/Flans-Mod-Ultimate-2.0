package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.equipment.EquippedArmorProperties;
import com.flansmodultimate.api.equipment.EquippedShieldProperties;
import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.WeaponOverrides;
import com.flansmodultimate.api.equipment.WeaponTraits;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.item.*;
import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.network.client.gun.PacketGunHolderAnimation;
import com.flansmodultimate.network.client.gun.PacketGunMuzzleFlash;
import com.flansmodultimate.platform.PlatformSounds;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.ModUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Native equipment actions shared by living-entity addons; no player or addon state is required. */
public final class EquipmentSupport
{
    /** Marks a magazine a gun was loaded with although it does not take it, so that the gun still fires it. */
    private static final String CHOSEN_ROUND_KEY = "FlansChosenRound";

    private EquipmentSupport()
    {}

    public static Optional<EquippedWeaponProperties> weapon(ItemStack stack, ItemStack spare)
    {
        if (!(stack.getItem() instanceof GunItem item))
            return Optional.empty();
        ItemStack preview = stack.copy();
        GunType type = item.getConfigType();
        Loaded loaded = loaded(item, preview);
        ItemStack ammunition = loaded == null ? spare : loaded.stack();
        ShootableType ammo = ammunition.getItem() instanceof ShootableItem shootable ? shootable.getConfigType() : null;
        if (ammo != null && !compatible(type, preview, ammo))
            ammo = null;

        double damage;
        if (type.isThrowable())
            damage = type.getThrowDamage(preview);
        else if (ammo == null)
            damage = type.getDamage(preview);
        else
            damage = type.getDamageForDisplay(ammo, preview);

        double speed = type.getBulletSpeed(preview);
        EnumFireMode mode = type.getFireMode(preview);
        if (ammo instanceof com.flansmodultimate.common.types.BulletType bullet)
        {
            FireableGun gun = new FireableGun(type, preview, null, ItemStack.EMPTY, EnumMovement.NONE, false);
            gun.applyAmmunition(ammo);
            speed = new FiredShot(gun, bullet, null, null, loaded == null ? 0 : ShootableItem.getRoundsFired(ammunition)).getMuzzleVelocity(false);
        }
        EquippedWeaponProperties.Impact impact = type.isThrowable()
            ? new EquippedWeaponProperties.Impact(1D, 0.5D, 1D, 0D, SoundEvents.TRIDENT_HIT.getLocation().toString(), SoundEvents.TRIDENT_HIT_GROUND.getLocation().toString(), false)
            : impact(ammo);
        return Optional.of(new EquippedWeaponProperties(type.isThrowable() || !type.getAmmoTypes().isEmpty(), type.isThrowable(), type.isShield(), damage, type.getMeleeDamage(preview, false), speed,
            type.getSpread(preview), type.getNumBullets(preview, ammo), type.getShootDelay(preview), item.getActualReloadTime(preview, ItemStackData.builtInRegistries(), ItemStack.EMPTY),
            type.getThrowChargeTime(), type.getMeleeTime(), type.getShootSound(preview, false), mode.isAutomaticFire(), mode == EnumFireMode.MINIGUN ? type.getMinigunStartSpeed() : 0F,
            mode == EnumFireMode.MINIGUN ? type.getMinigunMaxSpeed() : 0F, impact));
    }

    public static OptionalDouble armorPenetration(ItemStack stack, ItemStack spare)
    {
        if (!(stack.getItem() instanceof GunItem item))
            return OptionalDouble.empty();
        Loaded loaded = loaded(item, stack);
        ItemStack ammunition = loaded == null ? spare : loaded.stack();
        ShootableType ammo = ammunition.getItem() instanceof ShootableItem shootable ? shootable.getConfigType() : null;
        if (ammo instanceof com.flansmodultimate.common.types.BulletType bullet && compatible(item.getConfigType(), stack, ammo))
            return OptionalDouble.of(Math.max(0F, bullet.getPenetrationAt100m(0)));
        return OptionalDouble.of(0D);
    }

    public static Optional<WeaponTraits> traits(ItemStack stack, ItemStack spare)
    {
        if (!(stack.getItem() instanceof GunItem item))
            return Optional.empty();
        GunType type = item.getConfigType();
        IScope scope = type.getCurrentScope(stack);
        Loaded loaded = loaded(item, stack);
        ShootableType ammo = null;
        if (loaded != null && loaded.stack().getItem() instanceof ShootableItem shootable)
            ammo = shootable.getConfigType();
        else if (spare.getItem() instanceof ShootableItem shootable && compatible(type, stack, shootable.getConfigType()))
            ammo = shootable.getConfigType();
        else if (!type.getAmmoTypes().isEmpty())
            ammo = type.getAmmoTypes().get(0);
        int capacity = ammo == null ? 0 : type.getNumAmmoItemsInGun(stack) * Math.max(1, ammo.getRoundsPerItem());
        return Optional.of(new WeaponTraits(Math.max(scope.getZoomFactor(), scope.getFovFactor()), capacity));
    }

    private static EquippedWeaponProperties.Impact impact(@Nullable ShootableType ammo)
    {
        if (ammo == null)
            return EquippedWeaponProperties.Impact.NONE;
        if (!(ammo instanceof com.flansmodultimate.common.types.BulletType bullet))
            return new EquippedWeaponProperties.Impact(1D, ammo.getHitBoxSize(), ammo.getFallSpeed(), ammo.getExplosionRadius(), "", "", false);
        // Mirrors ShootingHelper: entity and block impacts are gated separately; blocks fall back to material sounds.
        String sound = StringUtils.defaultString(bullet.getHitSound());
        return new EquippedWeaponProperties.Impact(bullet.getKnockbackModifier(), bullet.getHitBoxSize(), bullet.getFallSpeed(), bullet.getExplosionRadius(),
            bullet.isEntityHitSoundEnable() ? sound : "", bullet.isHitSoundEnable() ? sound : "", bullet.isHitSoundEnable() && sound.isBlank());
    }

    public static Optional<EquippedShieldProperties> shield(ItemStack stack)
    {
        if (!(stack.getItem() instanceof GunItem item) || !item.getConfigType().isShield())
            return Optional.empty();
        GunType type = item.getConfigType();
        return Optional.of(new EquippedShieldProperties(type.getShieldDamageAbsorption(), type.getShieldBlockChance(), type.getShieldMaxBlockableMeleeDamage()));
    }

    public static Optional<EquippedArmorProperties> armor(ItemStack stack)
    {
        if (!(stack.getItem() instanceof CustomArmorItem item))
            return Optional.empty();
        var type = item.getConfigType();
        return Optional.of(new EquippedArmorProperties(type.getDefence(), type.getBulletDefence(), type.isNegateFallDamage(), type.isWaterBreathing(), type.isFireResistance(), type.isInvisible()));
    }

    public static boolean hasLoadedRound(ItemStack stack)
    {
        return stack.getItem() instanceof GunItem item && loaded(item, stack) != null;
    }

    public static Optional<ItemStack> loadedAmmunition(ItemStack stack)
    {
        if (!(stack.getItem() instanceof GunItem item))
            return Optional.empty();
        Loaded loaded = loaded(item, stack.copy());
        return loaded == null ? Optional.empty() : Optional.of(loaded.stack().copy());
    }

    public static int loadedRoundIndex(ItemStack stack)
    {
        return loadedAmmunition(stack).map(ShootableItem::getRoundsFired).orElse(0);
    }

    public static boolean deployable(ItemStack weapon)
    {
        return weapon.getItem() instanceof GunItem gun && gun.getConfigType().isDeployable();
    }

    public static boolean acceptsAmmunition(ItemStack weapon, ItemStack ammunition)
    {
        return acceptsAmmunition(weapon, ammunition, false);
    }

    /**
     * @param anyBullet
     *            whether a Flan bullet the gun does not take is loaded as well
     */
    public static boolean acceptsAmmunition(ItemStack weapon, ItemStack ammunition, boolean anyBullet)
    {
        return weapon.getItem() instanceof GunItem gun && ammunition.getItem() instanceof ShootableItem ammo && ShootableItem.hasRoundsLeft(ammunition)
            && loadable(gun.getConfigType(), weapon, ammo.getConfigType(), anyBullet);
    }

    public static boolean loadMagazine(LivingEntity holder, ItemStack weapon, ItemStack spare)
    {
        return loadMagazine(holder, weapon, spare, false);
    }

    /**
     * @param anyBullet
     *            whether a Flan bullet the gun does not take is loaded as well; each such magazine is marked, so that the
     *            gun fires it
     */
    public static boolean loadMagazine(LivingEntity holder, ItemStack weapon, ItemStack spare, boolean anyBullet)
    {
        if (!server(holder) || !acceptsAmmunition(weapon, spare, anyBullet))
            return false;
        GunItem gun = (GunItem) weapon.getItem();
        boolean chosen = !compatible(gun.getConfigType(), weapon, ((ShootableItem) spare.getItem()).getConfigType());
        boolean reloaded = false;
        for (int slot = 0; slot < gun.getConfigType().getNumAmmoItemsInGun(weapon) && !spare.isEmpty(); slot++)
        {
            ItemStack old = gun.getAmmoItemStack(weapon, slot, holder.level().registryAccess());
            if (ShootableItem.hasRoundsLeft(old))
                continue;
            if (old.getItem() instanceof ShootableItem spent)
                ModUtils.dropItem(holder.level(), holder, spent.getConfigType().getDropItemOnReload(), spent.getConfigType().getContentPack());
            ItemStack magazine = spare.split(1);
            if (chosen)
                ItemStackData.update(magazine, tag -> tag.putBoolean(CHOSEN_ROUND_KEY, true));
            gun.setBulletItemStack(weapon, magazine, slot, holder.level().registryAccess());
            reloaded = true;
        }
        return reloaded;
    }

    private static boolean loadable(GunType type, ItemStack weapon, ShootableType ammunition, boolean anyBullet)
    {
        return compatible(type, weapon, ammunition) || anyBullet && ammunition instanceof BulletType;
    }

    /** Raises the gun's aim pose and plays its shot animation for those who see a mob holder, as a native shot does. */
    public static void animateShot(LivingEntity holder, InteractionHand hand)
    {
        if (server(holder) && holder.getItemInHand(hand).getItem() instanceof GunItem)
            GunArmPoses.onShotFired(holder, hand);
    }

    /** Plays a mob-held gun's reload animation for those who see the holder. */
    public static void animateReload(LivingEntity holder, InteractionHand hand, float reloadTicks)
    {
        if (server(holder) && !(holder instanceof Player) && Float.isFinite(reloadTicks) && reloadTicks > 0F && holder.getItemInHand(hand).getItem() instanceof GunItem)
            PacketHandler.sendToTracking(new PacketGunHolderAnimation(holder.getId(), hand, PacketGunHolderAnimation.Kind.RELOAD, reloadTicks), holder);
    }

    /** Plays a mob-held gun's melee animation for those who see the holder. */
    public static void animateMelee(LivingEntity holder, InteractionHand hand)
    {
        if (server(holder) && !(holder instanceof Player) && holder.getItemInHand(hand).getItem() instanceof GunItem)
            PacketHandler.sendToTracking(new PacketGunHolderAnimation(holder.getId(), hand, PacketGunHolderAnimation.Kind.MELEE, 0F), holder);
    }

    /** Plays the held gun's melee sound with its authored range, as a player's gun melee does. */
    public static boolean meleeSound(LivingEntity holder, InteractionHand hand)
    {
        if (!server(holder) || !(holder.getItemInHand(hand).getItem() instanceof GunItem gun))
            return false;
        GunType type = gun.getConfigType();
        if (StringUtils.isBlank(type.getMeleeSound()))
            return false;
        PacketPlaySound.sendSoundPacket(holder, type.getMeleeSoundRange(), type.getMeleeSound(), type.isDistortSound());
        return true;
    }

    public static void reloadSound(LivingEntity holder, ItemStack weapon)
    {
        if (server(holder) && weapon.getItem() instanceof GunItem gun)
        {
            GunType type = gun.getConfigType();
            String sound = type.getReloadSound(weapon);
            if (!sound.isBlank())
                PacketPlaySound.sendSoundPacket(holder, type.getReloadSoundRange(), sound, false);
        }
    }

    public static boolean fire(LivingEntity holder, InteractionHand hand, Vec3 origin, Vec3 direction, boolean sounds, boolean particles, WeaponOverrides overrides)
    {
        ItemStack stack = holder.getItemInHand(hand);
        if (!server(holder) || !holder.isAlive() || !(stack.getItem() instanceof GunItem item) || !Double.isFinite(origin.lengthSqr()) || !Double.isFinite(direction.lengthSqr())
            || direction.lengthSqr() < 1E-8D)
            return false;
        GunType type = item.getConfigType();
        if (type.isPoweredOff(stack) || !type.canShootUnderwater() && holder.isUnderWater() || !item.getGunItemHandler().gunCanBeHandled(holder))
            return false;
        if (type.isThrowable())
            return throwWeapon(holder, hand, stack, origin, direction, sounds);
        Loaded loaded = loaded(item, stack);
        if (loaded == null)
            return false;
        ShootableType ammunition = ((ShootableItem) loaded.stack().getItem()).getConfigType();
        FireableGun gun = overridden(new FireableGun(type, stack, holder, holder.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND),
            ModUtils.getEnumMovement(holder), !holder.onGround()), overrides);
        int projectiles = Math.max(1, Math.min(128, overrides.projectiles().orElse(type.getNumBullets(stack, ammunition))));
        // This shared path retains hitscan when no weapon/ammunition velocity is declared.
        ShootingHelper.fireWeapon(holder.level(), gun, ammunition, projectiles, origin, direction.normalize(), holder, holder, ShootableItem.getRoundsFired(loaded.stack()),
            () -> afterShot(holder, hand, stack, item, loaded));
        if (sounds)
            firingSounds(holder, type, stack, !hasLoadedRound(stack));
        if (particles)
            muzzleFlash(holder, hand, type, stack);
        GunArmPoses.onShotFired(holder, hand);
        return true;
    }

    /** The held gun with the caller's statistics in place of its own; its speed multiplier and spread pattern are kept. */
    private static FireableGun overridden(FireableGun gun, WeaponOverrides overrides)
    {
        if (overrides.isEmpty())
            return gun;
        float damage = (float) overrides.damage().stream().filter(Double::isFinite).map(value -> Math.max(0D, value)).findFirst().orElse(gun.getDamage());
        float spread = (float) overrides.spread().stream().filter(Double::isFinite).map(value -> Math.max(0D, value)).findFirst().orElse(gun.getSpread());
        float speed = (float) overrides.speed().stream().filter(Double::isFinite).map(value -> Math.max(0.01D, value)).findFirst().orElse(gun.getBulletSpeed());
        return new FireableGun(gun.getType(), damage, spread, speed, gun.getBulletSpeedMultiplier(), gun.getSpreadPattern());
    }

    private static void firingSounds(LivingEntity holder, GunType type, ItemStack stack, boolean lastRound)
    {
        String sound = type.getShootSound(stack, lastRound);
        if (!sound.isBlank())
            PacketPlaySound.sendSoundPacket(holder, type.getGunSoundRange(), sound, type.isDistortSound(), type.isSilencedSound(stack));
        if (!type.getDistantShootSound().isBlank())
            PacketHandler.sendToDonut(holder.level().dimension(), holder.position(), type.getGunSoundRange(), type.getDistantSoundRange(),
                new PacketPlaySound(holder.position(), type.getDistantSoundRange(), type.getDistantShootSound(), false, false, null));
    }

    private static void muzzleFlash(LivingEntity holder, InteractionHand hand, GunType type, ItemStack stack)
    {
        var barrel = type.getBarrel(stack);
        if (!type.shouldShowMuzzleFlashParticles() || type.getMuzzleFlashParticle().isBlank() || barrel != null && barrel.isDisableMuzzleFlash())
            return;
        PacketHandler.sendToTracking(new PacketGunMuzzleFlash(holder.getUUID(), hand, type.getMuzzleFlashParticle(), type.getMuzzleFlashParticleSize(), true), holder);
    }

    private static void afterShot(LivingEntity holder, InteractionHand hand, ItemStack stack, GunItem item, Loaded loaded)
    {
        GunType type = item.getConfigType();
        ShootableType ammunition = ((ShootableItem) loaded.stack().getItem()).getConfigType();
        ModUtils.dropItem(holder.level(), holder, ammunition.getDropItemOnShoot(), ammunition.getContentPack());
        ModUtils.dropItem(holder.level(), holder, type.getDropItemOnShoot(), type.getContentPack());
        if (type.getKnockback() > 0F && !holder.isCrouching())
            holder.setDeltaMovement(holder.getDeltaMovement().subtract(holder.getLookAngle().scale(type.getKnockback() * 0.1D)));
        ShootableItem.consumeRound(loaded.stack());
        item.setBulletItemStack(stack, loaded.stack(), loaded.slot(), holder.level().registryAccess());
        if (type.isConsumeGunUponUse())
            holder.setItemInHand(hand, ItemStack.EMPTY);
    }

    private static boolean throwWeapon(LivingEntity holder, InteractionHand hand, ItemStack stack, Vec3 origin, Vec3 direction, boolean sounds)
    {
        Optional<ProjectileShot> prepared = prepareThrow(holder, stack);
        if (prepared.isEmpty() || prepared.get().launch(origin, direction, sounds).isEmpty())
            return false;
        stack.shrink(1);
        holder.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        GunArmPoses.onShotFired(holder, hand);
        return true;
    }

    public static Optional<ProjectileShot> prepareThrow(LivingEntity holder, ItemStack stack)
    {
        if (!server(holder) || !holder.isAlive() || stack.isEmpty() || !(stack.getItem() instanceof GunItem item))
            return Optional.empty();
        GunType type = item.getConfigType();
        if (!type.isThrowable() || type.isPoweredOff(stack) || !type.canShootUnderwater() && holder.isUnderWater() || !item.getGunItemHandler().gunCanBeHandled(holder))
            return Optional.empty();
        float speed = type.getBulletSpeed(stack);
        float spread = type.getSpread(stack, ModUtils.getEnumMovement(holder), !holder.onGround());
        float damage = type.getThrowDamage(stack);
        if (!Float.isFinite(speed) || speed <= 0F || !Float.isFinite(spread) || !Float.isFinite(damage))
            return Optional.empty();
        return Optional.of(new PreparedThrow(holder, stack.copyWithCount(1), type, speed, spread, damage));
    }

    private record PreparedThrow(LivingEntity holder, ItemStack stack, GunType type, float speed, float spread, float damage) implements ProjectileShot
    {
        @Override
        public double gravity()
        {
            return ModPhysics.gravity(ShootableType.FALL_SPEED_COEFFICIENT, holder.level());
        }

        @Override
        public double drag()
        {
            return ModPhysics.dragRetention(ShootableType.AIR_DEFAULT_DRAG, holder.level());
        }

        @Override
        public List<Entity> launch(Vec3 origin, Vec3 direction, boolean throwSound)
        {
            if (!server(holder) || !holder.isAlive() || !Double.isFinite(origin.lengthSqr()) || !Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1E-8D)
                return List.of();
            ThrownGun thrown = new ThrownGun(holder.level(), holder, stack.copy(), damage);
            thrown.setPos(origin);
            // The player's throw uses the same conversion from Flan angular spread to vanilla inaccuracy.
            thrown.shoot(direction.x, direction.y, direction.z, speed, spread * ShootingHelper.ANGULAR_SPREAD_FACTOR / 0.0172275F);
            thrown.pickup = AbstractArrow.Pickup.ALLOWED;
            if (!holder.level().addFreshEntity(thrown))
                return List.of();
            if (throwSound && !ExternalProjectileSupport.playShootSound(holder, type, stack, false))
                holder.playSound(PlatformSounds.tridentThrow(), 1F, 1F);
            return List.of(thrown);
        }
    }

    @Nullable
    private static Loaded loaded(GunItem item, ItemStack stack)
    {
        for (int slot = 0; slot < item.getConfigType().getNumAmmoItemsInGun(stack); slot++)
        {
            ItemStack ammo = item.getAmmoItemStack(stack, slot, ItemStackData.builtInRegistries());
            // A magazine loaded by choice, which the gun does not take, is fired all the same.
            boolean fires = ammo.getItem() instanceof ShootableItem shootable
                && (compatible(item.getConfigType(), stack, shootable.getConfigType()) || ItemStackData.copy(ammo).getBoolean(CHOSEN_ROUND_KEY));
            if (ShootableItem.hasRoundsLeft(ammo) && fires)
                return new Loaded(slot, ammo);
        }
        return null;
    }

    private static boolean compatible(GunType type, ItemStack weapon, ShootableType ammunition)
    {
        var grip = type.getGrip(weapon);
        if (grip != null && type.getSecondaryFire(weapon))
            return ShootableType.findAmmoTypes(new LinkedHashSet<>(grip.getSecondaryAmmo()), grip.getContentPack()).contains(ammunition);
        return type.getAmmoTypes().contains(ammunition);
    }

    private static boolean server(LivingEntity entity)
    {
        return entity.level() instanceof ServerLevel level && level.getServer().isSameThread();
    }

    private record Loaded(int slot, ItemStack stack)
    {}
}
