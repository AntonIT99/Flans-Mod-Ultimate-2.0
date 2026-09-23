package com.flansmodultimate.client;

import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketReloadPreferences;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;

/**
 * Sends the player's own reload inventory preferences to the server, so that reloads the server performs on
 * their behalf, requested or automatic, follow them instead of the server's own defaults.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReloadPreferencesSync
{
    public static void sendToServer()
    {
        ModClientConfig config = ModClientConfig.get();
        Minecraft minecraft = Minecraft.getInstance();
        // The client config is baked before the game exists, and there is nothing to tell while not connected
        if (config == null || minecraft == null || minecraft.getConnection() == null)
            return;

        PacketHandler.sendToServer(new PacketReloadPreferences(config.combineAmmoOnReload, config.ammoToUpperInventoryOnReload));
    }
}
