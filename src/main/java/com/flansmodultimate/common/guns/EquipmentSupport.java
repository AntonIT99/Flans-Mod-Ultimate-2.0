package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.EquippedArmorProperties;
import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.item.*;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.network.client.gun.PacketGunHolderAnimation;
import com.flansmodultimate.network.client.gun.PacketGunMuzzleFlash;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.util.ModUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Optional;

/** Native equipment actions shared by living-entity addons; no player or addon state is required. */
public final class EquipmentSupport
{
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
        return Optional.of(new EquippedWeaponProperties(type.isThrowable() || !type.getAmmoTypes().isEmpty(), type.isThrowable(), type.isShield(), damage, type.getMeleeDamage(preview, false), speed,
            type.getSpread(preview), type.getNumBullets(preview, ammo), type.getShootDelay(preview), item.getActualReloadTime(preview, ItemStackData.builtInRegistries(), ItemStack.EMPTY),
            type.getThrowChargeTime(), type.getMeleeTime(), type.getShootSound(preview, false), mode.isAutomaticFire(), mode == EnumFireMode.MINIGUN ? type.getMinigunStartSpeed() : 0F,
            mode == EnumFireMode.MINIGUN ? type.getMinigunMaxSpeed() : 0F, impact(ammo)));
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

    public static Optional<EquippedArmorProperties> armor(ItemStack stack)
    {
        if (!(stack.getItem() instanceof CustomArmorItem item))
            return Optional.empty();
        var type = item.getConfigType();
        return Optional.of(new EquippedArmorProperties(type.getDefence(), type.getBulletDefence(), type.isNegateFallDamage(), type.isWaterBreathing(), type.isFireResistance()));
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

    public static boolean loadMagazine(LivingEntity holder, ItemStack weapon, ItemStack spare)
    {
        if (!server(holder) || !(weapon.getItem() instanceof GunItem gun) || !(spare.getItem() instanceof ShootableItem ammo) || !ShootableItem.hasRoundsLeft(spare)
            || !compatible(gun.getConfigType(), weapon, ammo.getConfigType()))
            return false;
        boolean reloaded = false;
        for (int slot = 0; slot < gun.getConfigType().getNumAmmoItemsInGun(weapon) && !spare.isEmpty(); slot++)
        {
            ItemStack old = gun.getAmmoItemStack(weapon, slot, holder.level().registryAccess());
            if (ShootableItem.hasRoundsLeft(old))
                continue;
            if (old.getItem() instanceof ShootableItem spent)
                ModUtils.dropItem(holder.level(), holder, spent.getConfigType().getDropItemOnReload(), spent.getConfigType().getContentPack());
            gun.setBulletItemStack(weapon, spare.split(1), slot, holder.level().registryAccess());
            reloaded = true;
        }
        return reloaded;
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

    public static boolean fire(LivingEntity holder, InteractionHand hand, Vec3 origin, Vec3 direction, boolean sounds, boolean particles)
    {
        ItemStack stack = holder.getItemInHand(hand);
        if (!server(holder) || !holder.isAlive() || !(stack.getItem() instanceof GunItem item) || !Double.isFinite(origin.lengthSqr()) || !Double.isFinite(direction.lengthSqr())
            || direction.lengthSqr() < 1E-8D)
            return false;
        GunType type = item.getConfigType();
        if (type.isPoweredOff(stack) || !type.canShootUnderwater() && holder.isUnderWater() || !item.getGunItemHandler().gunCanBeHandled(holder))
            return false;
        if (type.isThrowable())
            return throwWeapon(holder, hand, stack, type, origin, direction, sounds);
        Loaded loaded = loaded(item, stack);
        if (loaded == null)
            return false;
        ShootableType ammunition = ((ShootableItem) loaded.stack().getItem()).getConfigType();
        FireableGun gun = new FireableGun(type, stack, holder, holder.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND),
            ModUtils.getEnumMovement(holder), !holder.onGround());
        // This shared path retains hitscan when no weapon/ammunition velocity is declared.
        ShootingHelper.fireWeapon(holder.level(), gun, ammunition, type.getNumBullets(stack, ammunition), origin, direction.normalize(), holder, holder, ShootableItem.getRoundsFired(loaded.stack()),
            () -> afterShot(holder, hand, stack, item, loaded));
        if (sounds)
            firingSounds(holder, type, stack, !hasLoadedRound(stack));
        if (particles)
            muzzleFlash(holder, hand, type, stack);
        GunArmPoses.onShotFired(holder, hand);
        return true;
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

    private static boolean throwWeapon(LivingEntity holder, InteractionHand hand, ItemStack stack, GunType type, Vec3 origin, Vec3 direction, boolean sounds)
    {
        ItemStack thrownStack = stack.copyWithCount(1);
        ThrownGun thrown = new ThrownGun(holder.level(), holder, thrownStack, type.getThrowDamage(stack));
        thrown.setPos(origin);
        // Convert Flan angular spread to AbstractArrow's vanilla inaccuracy scale.
        thrown.shoot(direction.x, direction.y, direction.z, type.getBulletSpeed(stack), type.getSpread(stack) * ShootingHelper.ANGULAR_SPREAD_FACTOR / 0.0172275F);
        thrown.pickup = AbstractArrow.Pickup.ALLOWED;
        if (!holder.level().addFreshEntity(thrown))
            return false;
        stack.shrink(1);
        holder.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        if (sounds && !ExternalProjectileSupport.playShootSound(holder, type, thrownStack, false))
            holder.playSound(SoundEvents.TRIDENT_THROW, 1F, 1F);
        GunArmPoses.onShotFired(holder, hand);
        return true;
    }

    @Nullable
    private static Loaded loaded(GunItem item, ItemStack stack)
    {
        for (int slot = 0; slot < item.getConfigType().getNumAmmoItemsInGun(stack); slot++)
        {
            ItemStack ammo = item.getAmmoItemStack(stack, slot, ItemStackData.builtInRegistries());
            if (ShootableItem.hasRoundsLeft(ammo) && ammo.getItem() instanceof ShootableItem shootable && compatible(item.getConfigType(), stack, shootable.getConfigType()))
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
