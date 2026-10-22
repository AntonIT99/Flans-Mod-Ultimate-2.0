package com.flansmodultimate.mixin;

import com.flansmodultimate.platform.network.ContentPackDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

/** Replaces verbose registry disconnect reasons with bounded, per-connection content-pack diagnostics. */
@Mixin(Connection.class)
public abstract class ConnectionMixin
{
    @ModifyVariable(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
    @SuppressWarnings("DataFlowIssue") // Mixin instances are Connections at runtime.
    private Component flansmodultimateRegistryDisconnectReason(Component reason)
    {
        return ContentPackDiagnostics.disconnectReason((Connection) (Object) this, reason);
    }
}
