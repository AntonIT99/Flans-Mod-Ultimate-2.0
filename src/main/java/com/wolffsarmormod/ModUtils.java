package com.wolffsarmormod;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModUtils
{
    /**
     * Returns true iff the given player is the local client player.
     * Always false on a dedicated server.
     * Safe for common code: only accesses Minecraft client classes when running on CLIENT.
     */
    public static boolean isThePlayer(@Nullable Player player)
    {
        if (player == null)
            return false;

        // Avoid classloading client-only classes on server
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            LocalPlayer local = mc.player;
            return local != null && local.getUUID().equals(player.getUUID());
        }
        return false;
    }
}
