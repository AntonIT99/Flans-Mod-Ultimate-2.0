package com.wolffsmod.npcs.mixin;

import noppes.npcs.CustomNpcs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;

/** Prevents Custom NPCs' save-directory lookup from treating a headless GameTest server as an integrated client server. */
@SuppressWarnings("java:S1118") // Mixin class; its static injector is merged into the target, not instantiated.
@Mixin(value = CustomNpcs.class, remap = false)
public abstract class CustomNpcsMixin
{
    @Redirect(method = "getLevelSaveDirectory(Ljava/lang/String;Z)Ljava/io/File;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isDedicatedServer()Z", remap = true))
    private static boolean wolffsmodnpcsUseServerSaveDirectory(MinecraftServer server)
    {
        // Only this lookup needs the dedicated-server branch; the server's actual type and external-saving rules stay intact.
        return server instanceof GameTestServer || server.isDedicatedServer();
    }
}
