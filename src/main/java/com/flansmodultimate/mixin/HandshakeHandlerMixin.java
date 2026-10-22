package com.flansmodultimate.mixin;

import com.flansmodultimate.platform.network.ContentPackLogin;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Sends the server's pack manifest before any Forge registry snapshot can fail on the client. */
@Mixin(value = HandshakeHandler.class, remap = false)
public abstract class HandshakeHandlerMixin
{
    @Shadow
    private List<NetworkRegistry.LoginPayload> messageList;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void flansmodultimateSendManifestFirst(CallbackInfo ci)
    {
        messageList = new ArrayList<>(messageList);
        messageList.sort(Comparator.comparing(payload -> !ContentPackLogin.CHANNEL_ID.equals(payload.getChannelName())));
    }
}
