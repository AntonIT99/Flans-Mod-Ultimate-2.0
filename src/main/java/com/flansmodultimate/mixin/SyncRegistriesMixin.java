package com.flansmodultimate.mixin;

import com.flansmodultimate.platform.network.ContentPackLogin;
import net.neoforged.neoforge.network.configuration.SyncRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

/** Sends pack names and fingerprints before NeoForge's registry packets, including its early configuration task. */
@Mixin(value = SyncRegistries.class, remap = false)
@SuppressWarnings("UnstableApiUsage") // The internal early registry task runs before the public configuration-task event.
public abstract class SyncRegistriesMixin
{
    @Inject(method = "run", at = @At("HEAD"))
    private void flansmodultimateSendManifestFirst(Consumer<CustomPacketPayload> sender, CallbackInfo ci)
    {
        ContentPackLogin.send(sender);
    }
}
