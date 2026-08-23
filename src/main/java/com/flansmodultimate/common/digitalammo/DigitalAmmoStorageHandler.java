package com.flansmodultimate.common.digitalammo;

import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber
public final class DigitalAmmoStorageHandler
{
    private static final String NBT_DIGITAL_AMMO = "flansmod_digital_ammo";
    private static final String NBT_BULLETS = "bullets";
    private static final String NBT_NUM_TYPES = "num_types";
    private static final String NBT_TYPE = "type";
    private static final String NBT_AMOUNT = "amount";

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
        if (!isDigitalAmmoEnabled())
            return;

        if (player == null || player.level().isClientSide())
            return;

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag ammoTag = new CompoundTag();

        ListTag bulletsList = new ListTag();
        int numTypes = bulletData.getNumTypes();
        for (int i = 0; i < numTypes; i++)
        {
            CompoundTag bulletTag = new CompoundTag();
            bulletTag.putInt(NBT_TYPE, i + 1);
            bulletTag.putDouble(NBT_AMOUNT, bulletData.getBullets(i + 1));
            bulletsList.add(bulletTag);
        }
        ammoTag.put(NBT_BULLETS, bulletsList);
        ammoTag.putInt(NBT_NUM_TYPES, numTypes);

        persistentData.put(NBT_DIGITAL_AMMO, ammoTag);
    }

    public static void loadPlayerAmmo(Player player)
    {
        if (!isDigitalAmmoEnabled())
            return;

        if (player == null || player.level().isClientSide())
            return;

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(NBT_DIGITAL_AMMO))
            return;

        CompoundTag ammoTag = persistentData.getCompound(NBT_DIGITAL_AMMO).orElseGet(CompoundTag::new);

        PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());

        ListTag bulletsList = ammoTag.getListOrEmpty(NBT_BULLETS);
        for (int i = 0; i < bulletsList.size(); i++)
        {
            CompoundTag bulletTag = bulletsList.getCompound(i).orElseGet(CompoundTag::new);
            int type = bulletTag.getIntOr(NBT_TYPE, 0);
            double amount = bulletTag.getDoubleOr(NBT_AMOUNT, 0D);
            if (type <= 0)
                continue;
            bulletData.setBullets(type, amount);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!isDigitalAmmoEnabled())
            return;

        Player player = event.getEntity();
        if (player == null || player.level().isClientSide())
            return;

        loadPlayerAmmo(player);

        if (player instanceof ServerPlayer serverPlayer)
        {
            com.flansmodultimate.network.client.PacketSyncDigitalAmmo.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!isDigitalAmmoEnabled())
            return;

        Player player = event.getEntity();
        if (player == null || player.level().isClientSide())
            return;

        savePlayerAmmo(player);
        PlayerBulletStorage.clearPlayerData(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event)
    {
        if (!isDigitalAmmoEnabled())
            return;

        savePlayerAmmo(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event)
    {
        if (!isDigitalAmmoEnabled())
            return;

        loadPlayerAmmo(event.getEntity());
    }
}
