package com.flansmodultimate.common.digitalammo;

import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public final class DigitalAmmoStorageHandler
{
    private static final String TAG_DIGITAL_AMMO = "FlansModDigitalAmmo";

    private DigitalAmmoStorageHandler() {}

    private static boolean isDigitalAmmoEnabled()
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

    public static void savePlayerAmmo(Player player)
    {
        if (!isDigitalAmmoEnabled()) return;
        if (player == null || player.level() == null || player.level().isClientSide) return;

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag ammoTag = new CompoundTag();

        ListTag bulletsList = new ListTag();
        int numTypes = bulletData.getNumTypes();
        for (int i = 0; i < numTypes; i++)
        {
            CompoundTag bulletTag = new CompoundTag();
            bulletTag.putInt("Type", i + 1);
            bulletTag.putDouble("Amount", bulletData.getBullets(i + 1));
            bulletsList.add(bulletTag);
        }
        ammoTag.put("Bullets", bulletsList);
        ammoTag.putInt("NumTypes", numTypes);

        persistentData.put(TAG_DIGITAL_AMMO, ammoTag);
    }

    public static void loadPlayerAmmo(Player player)
    {
        if (!isDigitalAmmoEnabled()) return;
        if (player == null || player.level() == null || player.level().isClientSide) return;

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(TAG_DIGITAL_AMMO)) return;

        CompoundTag ammoTag = persistentData.getCompound(TAG_DIGITAL_AMMO);
        if (ammoTag == null) return;

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());

        ListTag bulletsList = ammoTag.getList("Bullets", Tag.TAG_COMPOUND);
        for (int i = 0; i < bulletsList.size(); i++)
        {
            CompoundTag bulletTag = bulletsList.getCompound(i);
            int type = bulletTag.getInt("Type");
            double amount = bulletTag.getDouble("Amount");
            bulletData.setBullets(type, amount);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!isDigitalAmmoEnabled()) return;

        Player player = event.getEntity();
        if (player == null || player.level() == null || player.level().isClientSide) return;

        loadPlayerAmmo(player);

        if (player instanceof ServerPlayer serverPlayer)
        {
            com.flansmodultimate.network.client.PacketSyncDigitalAmmo.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!isDigitalAmmoEnabled()) return;

        Player player = event.getEntity();
        if (player == null || player.level() == null || player.level().isClientSide) return;

        savePlayerAmmo(player);
        PlayerBulletStorage.clearPlayerData(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event)
    {
        if (!isDigitalAmmoEnabled()) return;
        savePlayerAmmo(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event)
    {
        if (!isDigitalAmmoEnabled()) return;
        loadPlayerAmmo(event.getEntity());
    }
}
