package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Asks the server to change one entry of its common config. The server has the final say: only an operator,
 * or the owner of a single player world, may change anything, and the spec still validates the value. Either
 * way the player is sent the values that ended up in force, so a refused change does not linger on screen.
 */
@NoArgsConstructor
public class PacketSetCommonConfigValue implements IServerPacket
{
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private String path;
    private Object value;

    public PacketSetCommonConfigValue(List<String> path, Object value)
    {
        this.path = ConfigSpecValues.joinPath(path);
        this.value = value;
    }

    /** Whether the given player is allowed to change the common config, as the server decides it. */
    public static boolean mayEditCommonConfig(ServerPlayer player)
    {
        if (player.hasPermissions(REQUIRED_PERMISSION_LEVEL))
            return true;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSingleplayer() && server.isSingleplayerOwner(player.getGameProfile());
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(path);
        ConfigSpecValues.writeValue(data, value);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        path = data.readUtf();
        value = ConfigSpecValues.readValue(data);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        String name = player.getGameProfile().getName();
        if (!mayEditCommonConfig(player))
        {
            FlansMod.log.warn("{} tried to change the server setting {} without permission", name, path);
            player.sendSystemMessage(Component.translatable("gui.flansmodultimate.options.server_option_denied").withStyle(ChatFormatting.RED));
            PacketRequestCommonConfig.sendTo(player);
            return;
        }

        if (ModCommonConfig.setRuntimeValue(ConfigSpecValues.splitPath(path), value))
        {
            FlansMod.log.info("{} set the server setting {} to {}", name, path, value);
            player.sendSystemMessage(Component.translatable("gui.flansmodultimate.options.server_option_changed",
                path, String.valueOf(value)).withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Refused by the spec, or another operator won the race: show them what is in force instead
        PacketRequestCommonConfig.sendTo(player);
    }
}
