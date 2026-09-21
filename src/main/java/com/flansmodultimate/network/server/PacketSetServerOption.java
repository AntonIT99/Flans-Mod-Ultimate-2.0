package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Asks the server to change one of the few common config settings the in-game options screen exposes.
 * The server has the final say: only an operator, or the owner of a single player world, may change them.
 */
@NoArgsConstructor
public class PacketSetServerOption implements IServerPacket
{
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private ModCommonConfig.RuntimeOption option;
    private boolean value;

    public PacketSetServerOption(ModCommonConfig.RuntimeOption option, boolean value)
    {
        this.option = option;
        this.value = value;
    }

    /** Whether the given player is allowed to change these settings, as the server decides it. */
    public static boolean mayEditServerOptions(ServerPlayer player)
    {
        if (player.hasPermissions(REQUIRED_PERMISSION_LEVEL))
            return true;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSingleplayer() && server.isSingleplayerOwner(player.getGameProfile());
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(option);
        data.writeBoolean(value);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        option = data.readEnum(ModCommonConfig.RuntimeOption.class);
        value = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!mayEditServerOptions(player))
        {
            FlansMod.log.warn("{} tried to change the server setting {} without permission", player.getGameProfile().getName(), option.configPath());
            player.sendSystemMessage(Component.translatable("gui.flansmodultimate.options.server_option_denied").withStyle(ChatFormatting.RED));
            // Tell them what is really in force, so their options screen stops showing the refused value
            ModCommonConfigSync.syncClientIfServer(player);
            return;
        }

        boolean changed = ModCommonConfig.setRuntimeOption(option, value);
        if (!changed)
        {
            // Two operators can race for the same setting; the loser gets the value that won
            ModCommonConfigSync.syncClientIfServer(player);
            return;
        }

        FlansMod.log.info("{} set the server setting {} to {}", player.getGameProfile().getName(), option.configPath(), value);
        player.sendSystemMessage(Component.translatable("gui.flansmodultimate.options.server_option_changed",
            option.configPath(), Component.translatable(value ? "options.on" : "options.off")).withStyle(ChatFormatting.YELLOW));
    }
}
