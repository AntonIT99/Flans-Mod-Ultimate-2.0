package com.flansmodultimate.common.digitalammo;

import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.client.PacketSyncDigitalAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class DigitalAmmoSupplyHandler
{
    private static final Set<ResourceLocation> supplyBlocks = new HashSet<>();
    private static final Set<UUID> cooldownPlayers = new HashSet<>();

    private DigitalAmmoSupplyHandler() {}

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

    public static void reloadSupplyBlocks()
    {
        supplyBlocks.clear();
        try
        {
            CommonConfigSnapshot config = ModCommonConfig.get();
            if (config == null || !config.enableDigitalAmmoSystem()) return;

            List<String> blockIds = config.digitalAmmoSupplyBlocks();
            for (String blockId : blockIds)
            {
                try
                {
                    ResourceLocation loc = ResourceLocation.parse(blockId.trim());
                    supplyBlocks.add(loc);
                }
                catch (Exception e)
                {
                    // Invalid block ID, skip
                }
            }
        }
        catch (Exception e)
        {
            // Config not loaded yet
        }
    }

    public static boolean isSupplyBlock(ResourceLocation blockId)
    {
        return supplyBlocks.contains(blockId);
    }

    public static void clearPlayerCooldown(UUID playerId)
    {
        cooldownPlayers.remove(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            clearPlayerCooldown(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!isDigitalAmmoEnabled()) return;

        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ServerPlayer player)
        {
            if (event.getUseBlock() == Event.Result.DENY) return;

            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();

            if (!isSupplyBlock(blockId)) return;

            UUID playerId = player.getUUID();
            if (cooldownPlayers.contains(playerId)) return;

            cooldownPlayers.add(playerId);

            int supplyAmount = ModCommonConfig.get().digitalAmmoSupplyAmount();
            int numTypes = ModCommonConfig.get().digitalAmmoNumTypes();

            PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(playerId);
            int maxAmount = bulletData.getMaxAmount();
            for (int i = 1; i <= numTypes; i++)
            {
                double current = PlayerBulletStorage.getBulletsTypeById(bulletData, i);
                double newAmount = Math.min(current + supplyAmount, maxAmount);
                PlayerBulletStorage.setBulletsById(bulletData, i, newAmount);
            }

            PacketSyncDigitalAmmo.syncToClient(player);

            net.minecraft.server.MinecraftServer server = level.getServer();
            if (server != null)
            {
                server.execute(() -> cooldownPlayers.remove(playerId));
            }
        }
    }
}
