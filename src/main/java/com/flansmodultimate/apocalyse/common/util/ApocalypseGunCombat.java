package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.EnumSpreadPattern;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.guns.handler.ShootingHandler;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.platform.item.ItemStackData;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gun handling for the Apocalypse's autonomous shooters, ported from the 1.12.2
 * {@code EntityFlansModShooter} and {@code EntitySkullDrone}.
 *
 * <p>Each call to {@link #tryFire} is one trigger pull. It respects the weapon's own cadence
 * through a shoot delay that counts down every tick, and when the gun runs dry it reloads from
 * the shooter's reserve magazines, waiting out the weapon's reload time before the next shot.
 * The two legacy shooters differed in their timings and aim, which {@link Profile} captures.</p>
 */
public final class ApocalypseGunCombat
{
    private static final String NBT_RESERVE = "ApocalypseReserveAmmo";
    private static final String NBT_SHOOT_DELAY = "ApocalypseShootDelay";

    /** The per-shooter differences between the 1.12.2 survivor and drone gun code. */
    public enum Profile
    {
        /**
         * {@code EntityFlansModShooter}: fires from the eyes at the target's eyes with a small random
         * lean, waits twice the shoot delay between semi-automatic shots and three shoot delays
         * after every third burst round, and reloads in the gun's own reload time.
         */
        SURVIVOR,
        /**
         * {@code EntitySkullDrone}: fires from a block below its body at the target's feet with
         * five times the gun's spread plus ten, never faster than every five ticks on semi-auto,
         * and takes three times the gun's reload time to reload.
         */
        DRONE
    }

    private static final float SURVIVOR_AIM_LEAN = 0.5F;

    private final Mob owner;
    private final Profile profile;
    private final int reserveSlots;
    private final List<ItemStack> reserve = new ArrayList<>();
    private float shootDelay;
    private int soundDelay;

    public ApocalypseGunCombat(Mob owner, Profile profile, int reserveSlots)
    {
        this.owner = owner;
        this.profile = profile;
        this.reserveSlots = reserveSlots;
    }

    /** Counts the weapon's delays down. Call once per server tick. */
    public void tick()
    {
        if (shootDelay > 0F)
            shootDelay--;
        if (soundDelay > 0)
            soundDelay--;
    }

    public boolean isReady()
    {
        return shootDelay <= 0F;
    }

    public boolean hasGun()
    {
        return owner.getMainHandItem().getItem() instanceof GunItem;
    }

    /** Stocks a spare magazine, as long as a reserve slot is free. */
    public void addReserve(ItemStack ammo)
    {
        if (!ammo.isEmpty() && reserve.size() < reserveSlots)
            reserve.add(ammo);
    }

    /** Gives the held gun {@code count} spare magazines of ammunition it accepts. */
    public void stockReserve(RandomSource random, int count)
    {
        if (!(owner.getMainHandItem().getItem() instanceof GunItem gunItem))
            return;
        for (int i = 0; i < count; i++)
        {
            ApocalypseGunHelper.spareAmmoFor(gunItem.getConfigType(), random).ifPresent(stack -> {
                stack.setCount(1);
                addReserve(stack);
            });
        }
    }

    /** Stocks one copy of whatever the held gun is loaded with, as the 1.12.2 boss armed its drones. */
    public void stockCopyOfLoadedAmmo()
    {
        ItemStack gunStack = owner.getMainHandItem();
        if (gunStack.getItem() instanceof GunItem gunItem)
        {
            ItemStack loaded = gunItem.getAmmoItemStack(gunStack, 0, owner.level().registryAccess());
            if (!loaded.isEmpty())
                addReserve(loaded.copyWithCount(1));
        }
    }

    public void dropReserve()
    {
        for (ItemStack stack : reserve)
            Containers.dropItemStack(owner.level(), owner.getX(), owner.getY(), owner.getZ(), stack);
        reserve.clear();
    }

    /**
     * Pulls the trigger at {@code target}.
     *
     * @return {@code true} if a round left the barrel
     */
    public boolean tryFire(Entity target)
    {
        if (owner.level().isClientSide || shootDelay > 0F)
            return false;
        ItemStack gunStack = owner.getMainHandItem();
        if (!(gunStack.getItem() instanceof GunItem gunItem))
            return false;
        GunType gunType = gunItem.getConfigType();

        int slot = findLoadedSlot(gunItem, gunStack);
        if (slot < 0)
        {
            if (reload(gunItem, gunStack) || profile == Profile.DRONE)
            {
                // The legacy drone tried to reload, sound and delay included, whether or not
                // it had anything left to load.
                float reloadTime = gunType.getReloadTime(gunStack);
                shootDelay = profile == Profile.DRONE ? reloadTime * 3F : reloadTime;
                playSound(gunType.getReloadSound(gunStack), gunType.getReloadSoundRange(), gunType.isDistortSound(), false);
            }
            return false;
        }

        ItemStack ammoStack = gunItem.getAmmoItemStack(gunStack, slot, owner.level().registryAccess());
        if (!(ammoStack.getItem() instanceof ShootableItem shootableItem))
            return false;
        ShootableType shootableType = shootableItem.getConfigType();
        boolean lastBullet = ShootableItem.getRoundsRemaining(ammoStack) == 1;

        fire(gunType, gunStack, shootableType, ammoStack, target, () -> {
            ShootableItem.consumeRound(ammoStack);
            gunItem.setBulletItemStack(gunStack, ammoStack, slot, owner.level().registryAccess());
        });

        if (soundDelay <= 0 && playSound(gunType.getShootSound(gunStack, lastBullet), gunType.getGunSoundRange(),
            gunType.isDistortSound(), gunType.isSilencedSound(gunStack)))
            soundDelay = gunType.getShootSoundLength();

        shootDelay = nextShotDelay(gunType, gunStack, ShootableItem.getRoundsFired(ammoStack));
        return true;
    }

    private void fire(GunType gunType, ItemStack gunStack, ShootableType shootableType, ItemStack ammoStack, Entity target, ShootingHandler consume)
    {
        Vec3 origin;
        Vec3 direction;
        FireableGun fireableGun;
        RandomSource random = owner.getRandom();
        if (profile == Profile.DRONE)
        {
            origin = owner.position().subtract(0D, 1D, 0D);
            direction = target.position().subtract(origin);
            FireableGun base = new FireableGun(gunType, gunStack, owner, null, EnumMovement.NONE, true);
            fireableGun = new FireableGun(gunType, base.getDamage(), base.getSpread() * 5F + 10F,
                base.getBulletSpeed(), base.getBulletSpeedMultiplier(), EnumSpreadPattern.CIRCLE);
        }
        else
        {
            origin = owner.getEyePosition();
            direction = target.getEyePosition().subtract(origin).normalize();
            direction = direction.add(random.nextFloat() * direction.x * SURVIVOR_AIM_LEAN,
                random.nextFloat() * direction.y * SURVIVOR_AIM_LEAN, random.nextFloat() * direction.z * SURVIVOR_AIM_LEAN);
            fireableGun = new FireableGun(gunType, gunStack, owner, null, EnumMovement.NONE, !owner.onGround());
        }
        if (direction.lengthSqr() < 1.0E-6D)
            return;

        ShootingHelper.fireWeapon(owner.level(), fireableGun, shootableType, gunType.getNumBullets(gunStack, shootableType),
            origin, direction.normalize(), owner, owner, ShootableItem.getRoundsFired(ammoStack), consume);
    }

    private float nextShotDelay(GunType gunType, ItemStack gunStack, int roundsFired)
    {
        float delay = gunType.getShootDelay(gunStack);
        EnumFireMode mode = gunType.getFireMode(gunStack);
        if (profile == Profile.DRONE)
            return mode == EnumFireMode.SEMIAUTO ? Math.max(delay, 5F) : delay;
        return switch (mode)
        {
            case SEMIAUTO -> 2F * delay;
            case BURST -> roundsFired % 3 == 0 ? 3F * delay : delay;
            default -> delay;
        };
    }

    /** Fills every empty magazine slot of the gun from the fullest compatible reserve magazine. */
    private boolean reload(GunItem gunItem, ItemStack gunStack)
    {
        GunType gunType = gunItem.getConfigType();
        if (gunType.isDeployable())
            return false;
        boolean reloaded = false;
        int slots = gunType.getNumAmmoItemsInGun(gunStack);
        for (int slot = 0; slot < slots; slot++)
        {
            if (ShootableItem.hasRoundsLeft(gunItem.getAmmoItemStack(gunStack, slot, owner.level().registryAccess())))
                continue;
            int best = -1;
            int bestRounds = 0;
            for (int i = 0; i < reserve.size(); i++)
            {
                ItemStack candidate = reserve.get(i);
                if (!(candidate.getItem() instanceof ShootableItem shootable) || !gunType.getAmmoTypes().contains(shootable.getConfigType()))
                    continue;
                int rounds = ShootableItem.getRoundsRemaining(candidate);
                if (rounds > bestRounds)
                {
                    best = i;
                    bestRounds = rounds;
                }
            }
            if (best < 0)
                continue;
            ItemStack magazine = reserve.get(best);
            gunItem.setBulletItemStack(gunStack, magazine.copyWithCount(1), slot, owner.level().registryAccess());
            magazine.shrink(1);
            if (magazine.isEmpty())
                reserve.remove(best);
            reloaded = true;
        }
        return reloaded;
    }

    private int findLoadedSlot(GunItem gunItem, ItemStack gunStack)
    {
        int slots = gunItem.getConfigType().getNumAmmoItemsInGun(gunStack);
        for (int slot = 0; slot < slots; slot++)
        {
            ItemStack ammo = gunItem.getAmmoItemStack(gunStack, slot, owner.level().registryAccess());
            if (ammo.getItem() instanceof ShootableItem && ShootableItem.hasRoundsLeft(ammo))
                return slot;
        }
        return -1;
    }

    private boolean playSound(String sound, float range, boolean distort, boolean silenced)
    {
        if (StringUtils.isBlank(sound))
            return false;
        PacketPlaySound.sendSoundPacket(owner, range, sound, distort, silenced);
        return true;
    }

    public void save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (ItemStack stack : reserve)
            list.add(ItemStackData.save(stack, owner.level().registryAccess()));
        tag.put(NBT_RESERVE, list);
        tag.putFloat(NBT_SHOOT_DELAY, shootDelay);
    }

    public void load(CompoundTag tag)
    {
        reserve.clear();
        for (Tag entry : tag.getList(NBT_RESERVE, Tag.TAG_COMPOUND))
            Optional.of(ItemStackData.parse(owner.level().registryAccess(), (CompoundTag) entry))
                .filter(stack -> !stack.isEmpty()).ifPresent(this::addReserve);
        shootDelay = tag.getFloat(NBT_SHOOT_DELAY);
    }
}
