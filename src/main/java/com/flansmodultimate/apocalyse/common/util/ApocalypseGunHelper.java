package com.flansmodultimate.apocalyse.common.util;

import com.flansmodultimate.common.guns.*;
import com.flansmodultimate.common.guns.handler.ShootingHandler;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseGunHelper
{
    public static Optional<ItemStack> randomLoadedGun(RandomSource random, boolean preferSemiAuto)
    {
        Optional<GunType> preferred = randomGun(random, preferSemiAuto);
        if (preferred.isPresent())
            return loadGun(preferred.get(), random, false);
        return randomGun(random, false).flatMap(type -> loadGun(type, random, false));
    }

    /** Display loot uses the whole dungeon gun pool, including weapons with only explosive ammo. */
    public static Optional<ItemStack> randomLootGun(RandomSource random)
    {
        List<GunType> guns = InfoType.getInfoTypes().values().stream().filter(GunType.class::isInstance).map(GunType.class::cast).distinct()
            .filter(gun -> gun.getDungeonChance() != 0 && ModUtils.getItemStack(gun).isPresent()).sorted(Comparator.comparing(InfoType::getShortName, String.CASE_INSENSITIVE_ORDER)).toList();
        return guns.isEmpty() ? Optional.empty() : loadGun(guns.get(random.nextInt(guns.size())), random, true);
    }

    public static Optional<GunType> randomGun(RandomSource random, boolean preferSemiAuto)
    {
        List<GunType> guns = new ArrayList<>();
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            if (!(type instanceof GunType gun))
                continue;
            // As 1.12.2, only guns allowed in dungeon loot arm the wasteland, which keeps
            // novelty weapons such as the Nerf blasters out of it.
            if (gun.isDeployable() || gun.isShield() || !gun.isUsableByPlayers() || gun.getDungeonChance() == 0)
                continue;
            if (preferSemiAuto && gun.getMode() != EnumFireMode.SEMIAUTO)
                continue;
            if (combatAmmoTypes(gun, false).isEmpty())
                continue;
            if (ModUtils.getItemStack(gun).isEmpty())
                continue;
            guns.add(gun);
        }
        if (guns.isEmpty())
            return Optional.empty();
        return Optional.of(guns.get(random.nextInt(guns.size())));
    }

    public static Optional<ItemStack> loadGun(GunType gun, RandomSource random, boolean explosivesAllowed)
    {
        Optional<ItemStack> maybeGun = ModUtils.getItemStack(gun);
        if (maybeGun.isEmpty() || !(maybeGun.get().getItem() instanceof GunItem gunItem))
            return Optional.empty();

        ItemStack gunStack = maybeGun.get();
        List<Paintjob> paintjobs = new ArrayList<>(gun.getNonLegendaryPaintjobs().values());
        if (!paintjobs.isEmpty())
            gun.applyPaintjobToStack(gunStack, paintjobs.get(random.nextInt(paintjobs.size())));

        List<? extends ShootableType> ammo = explosivesAllowed ? gun.getAmmoTypes().stream().filter(type -> ModUtils.getItemStack(type).isPresent()).toList() : combatAmmoTypes(gun, false);
        if (ammo.isEmpty())
            return Optional.of(gunStack);

        int slots = Math.max(1, gun.getNumAmmoItemsInGun(gunStack));
        for (int slot = 0; slot < slots; slot++)
        {
            int ammoSlot = slot;
            ShootableType ammoType = ammo.get(random.nextInt(ammo.size()));
            ModUtils.getItemStack(ammoType).ifPresent(ammoStack ->
            {
                int rounds = Math.max(1, ammoType.getRoundsPerItem());
                ShootableItem.setRoundsRemaining(ammoStack, rounds);
                gunItem.setBulletItemStack(gunStack, ammoStack, ammoSlot, com.flansmodultimate.platform.item.ItemStackData.builtInRegistries());
            });
        }
        return Optional.of(gunStack);
    }

    public static Optional<ItemStack> randomAmmoStack(RandomSource random)
    {
        List<ShootableType> ammoTypes = new ArrayList<>();
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            if (type instanceof ShootableType shootable && ModUtils.getItemStack(shootable).isPresent())
                ammoTypes.add(shootable);
        }
        if (ammoTypes.isEmpty())
            return Optional.empty();

        ShootableType selected = ammoTypes.get(random.nextInt(ammoTypes.size()));
        int count = selected.getMaxStackSize() > 1 && random.nextBoolean() ? 1 + random.nextInt(Math.min(3, selected.getMaxStackSize())) : 1;
        return ModUtils.getItemStack(selected, count).map(stack ->
        {
            if (stack.getItem() instanceof ShootableItem)
                ShootableItem.setRoundsRemaining(stack, Math.max(1, selected.getRoundsPerItem()));
            return stack;
        });
    }

    /** A spare magazine for {@code gun}, for stocking the cargo of whatever carries it. */
    public static Optional<ItemStack> spareAmmoFor(GunType gun, RandomSource random)
    {
        List<BulletType> ammo = combatAmmoTypes(gun, false);
        if (ammo.isEmpty())
            return Optional.empty();
        BulletType selected = ammo.get(random.nextInt(ammo.size()));
        int count = selected.getMaxStackSize() > 1 ? 1 + random.nextInt(Math.min(3, selected.getMaxStackSize())) : 1;
        return ModUtils.getItemStack(selected, count).map(stack ->
        {
            if (stack.getItem() instanceof ShootableItem)
                ShootableItem.setRoundsRemaining(stack, Math.max(1, selected.getRoundsPerItem()));
            return stack;
        });
    }

    public static boolean shootLoadedGun(LivingEntity shooter, LivingEntity target)
    {
        ItemStack gunStack = shooter.getMainHandItem();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
            return false;

        GunType gunType = gunItem.getConfigType();
        LoadedAmmo loaded = firstLoadedBullet(gunItem, gunStack, shooter.level().registryAccess());
        if (loaded == null)
            return false;

        Vec3 origin = shooter.getEyePosition(0.0F);
        Vec3 aimPoint = target.getEyePosition(0.0F).add(0.0D, target.getBbHeight() * 0.1D, 0.0D);
        Vec3 direction = aimPoint.subtract(origin);
        if (direction.lengthSqr() < 0.0001D)
            return false;
        direction = direction.normalize();

        ItemStack ammoStack = loaded.stack();
        BulletType bulletType = loaded.type();
        ShootingHandler handler = () ->
        {
            ShootableItem.consumeRound(ammoStack);
            gunItem.setBulletItemStack(gunStack, ammoStack, loaded.slot(), shooter.level().registryAccess());
        };
        FiredShot firedShot = new FiredShot(gunType, bulletType, gunStack, ammoStack, ItemStack.EMPTY, shooter);
        ShootingHelper.fireGun(shooter.level(), firedShot, gunType.getNumBullets(gunStack, bulletType), origin, direction, handler);
        GunArmPoses.onShotFired(shooter, InteractionHand.MAIN_HAND);
        return true;
    }

    @Nullable
    private static LoadedAmmo firstLoadedBullet(GunItem gunItem, ItemStack gunStack, net.minecraft.core.HolderLookup.Provider registries)
    {
        int slots = gunItem.getConfigType().getNumAmmoItemsInGun(gunStack);
        for (int slot = 0; slot < slots; slot++)
        {
            ItemStack ammoStack = gunItem.getAmmoItemStack(gunStack, slot, registries);
            if (!ShootableItem.hasRoundsLeft(ammoStack) || !(ammoStack.getItem() instanceof ShootableItem shootableItem))
                continue;
            if (shootableItem.getConfigType() instanceof BulletType bulletType)
                return new LoadedAmmo(slot, ammoStack, bulletType);
        }
        return null;
    }

    private static List<BulletType> combatAmmoTypes(GunType gun, boolean explosivesAllowed)
    {
        List<BulletType> ammo = new ArrayList<>();
        for (ShootableType shootableType : gun.getAmmoTypes())
        {
            if (!(shootableType instanceof BulletType bulletType))
                continue;
            if (!explosivesAllowed && bulletType.getExplosiveMass() > 0.0F)
                continue;
            if (ModUtils.getItemStack(bulletType).isEmpty())
                continue;
            ammo.add(bulletType);
        }
        return ammo;
    }

    private record LoadedAmmo(int slot, ItemStack stack, BulletType type)
    {}
}
