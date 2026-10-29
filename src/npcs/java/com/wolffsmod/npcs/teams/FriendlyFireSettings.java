package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.ServerControlPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.MinecraftServer;

/**
 * A world's friendly fire settings, for factions without settings of their own. Kept with the other server settings, so
 * the Server page of the Wolff's Mod tab and the command change the same values; server thread only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FriendlyFireSettings
{
    public static FriendlyFire.Setting get(MinecraftServer server, FriendlyFire.Kind kind)
    {
        return FriendlyFire.Setting.values()[NpcServerSettings.get(server).get(setting(kind))];
    }

    /** Changes the setting and tells every player, so an open Server page shows it. */
    public static void set(MinecraftServer server, FriendlyFire.Kind kind, FriendlyFire.Setting value)
    {
        NpcServerSettings settings = NpcServerSettings.get(server);
        settings.set(setting(kind), value.ordinal());
        server.getPlayerList().getPlayers().forEach(player -> NpcNetwork.sendTo(player, ServerControlPacket.values(settings)));
    }

    private static NpcServerSettings.Setting setting(FriendlyFire.Kind kind)
    {
        return kind == FriendlyFire.Kind.NPCS ? NpcServerSettings.Setting.FRIENDLY_FIRE_NPCS : NpcServerSettings.Setting.FRIENDLY_FIRE_PLAYERS;
    }
}
