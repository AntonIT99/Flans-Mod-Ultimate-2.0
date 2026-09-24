package com.flansmodultimate.common.digitalammo;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.client.PacketSyncDigitalAmmo;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DigitalAmmoHelper
{
    public static boolean isDigitalAmmoEnabled()
    {
        try
        {
            CommonConfigSnapshot config = ModCommonConfig.get();
            return config != null && config.enableDigitalAmmoSystem();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static boolean hasEnoughDigitalAmmo(ServerPlayer player, GunItem gunItem)
    {
        if (!isDigitalAmmoEnabled()) return false;

        GunType gunType = gunItem.getConfigType();
        int consumeType = gunType.getConsumeBulletType();
        double bulletsNeeded = gunType.getBulletsPerReload();

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
        return PlayerBulletStorage.hasEnoughBullets(bulletData, consumeType, bulletsNeeded);
    }

    public static boolean tryReloadFromDigitalAmmo(ServerPlayer player, GunItem gunItem, ItemStack gunStack, int ammoSlot)
    {
        return tryReloadFromDigitalAmmo(player, gunItem, gunStack, List.of(ammoSlot));
    }

    public static boolean tryReloadFromDigitalAmmo(ServerPlayer player, GunItem gunItem, ItemStack gunStack, List<Integer> ammoSlots)
    {
        if (!isDigitalAmmoEnabled()) return false;
        if (ammoSlots == null || ammoSlots.isEmpty()) return false;

        GunType gunType = gunItem.getConfigType();
        int consumeType = gunType.getConsumeBulletType();
        double bulletsNeeded = gunType.getBulletsPerReload();

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());

        if (!PlayerBulletStorage.hasEnoughBullets(bulletData, consumeType, bulletsNeeded))
        {
            return false;
        }

        List<ShootableType> allowedAmmo = gunType.getAmmoTypes();
        if (allowedAmmo == null || allowedAmmo.isEmpty())
        {
            return false;
        }

        ShootableType ammoType = allowedAmmo.get(0);
        int magazineSize = Math.max(1, ammoType.getRoundsPerItem());
        ItemStack ammoStack = createVirtualAmmoStack(ammoType, magazineSize);

        if (ammoStack.isEmpty())
        {
            return false;
        }

        PlayerBulletStorage.takeBulletsById(bulletData, consumeType, bulletsNeeded);
        boolean loadedAny = false;
        for (int ammoSlot : ammoSlots)
        {
            if (ammoSlot < 0)
                continue;
            gunItem.setBulletItemStack(gunStack, ammoStack.copy(), ammoSlot, player.level().registryAccess());
            loadedAny = true;
        }

        if (!loadedAny)
        {
            PlayerBulletStorage.addBulletsById(bulletData, consumeType, bulletsNeeded);
            return false;
        }

        PacketSyncDigitalAmmo.syncToClient(player);

        return true;
    }

    private static ItemStack createVirtualAmmoStack(ShootableType ammoType, int rounds)
    {
        if (ammoType == null) return ItemStack.EMPTY;

        ItemStack stack = ModUtils.getItemStack(ammoType).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (stack.getItem() instanceof ShootableItem)
        {
            ShootableItem.setRoundsRemaining(stack, rounds);
        }

        return stack;
    }

    public static void addAmmoToPlayer(ServerPlayer player, int typeId, int amount)
    {
        if (!isDigitalAmmoEnabled()) return;

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
        double current = PlayerBulletStorage.getBulletsTypeById(bulletData, typeId);
        int maxAmount = bulletData.getMaxAmount();
        double newAmount = Math.min(current + amount, maxAmount);
        PlayerBulletStorage.setBulletsById(bulletData, typeId, newAmount);

        PacketSyncDigitalAmmo.syncToClient(player);
    }

    public static void setPlayerAmmo(ServerPlayer player, int typeId, int amount)
    {
        if (!isDigitalAmmoEnabled()) return;

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
        int maxAmount = bulletData.getMaxAmount();
        int clampedAmount = Math.min(amount, maxAmount);
        PlayerBulletStorage.setBulletsById(bulletData, typeId, clampedAmount);

        PacketSyncDigitalAmmo.syncToClient(player);
    }

    public static double getPlayerAmmo(ServerPlayer player, int typeId)
    {
        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
        return PlayerBulletStorage.getBulletsTypeById(bulletData, typeId);
    }
}
