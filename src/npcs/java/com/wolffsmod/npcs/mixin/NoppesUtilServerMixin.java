package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.FlansModApi;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Ends the editor's tracking pin from {@link SPacketGuiOpenMixin} once the player stops editing that NPC: closing the editor
 * clears the edited NPC, and any other NPC screen replaces it.
 */
@SuppressWarnings("java:S1118") // Mixin class; its static injector is merged into the target, not instantiated.
@Mixin(value = NoppesUtilServer.class, remap = false)
public abstract class NoppesUtilServerMixin
{
    @Inject(method = "setEditingNpc", at = @At("HEAD"))
    private static void wolffsmodnpcsUnpinEditedNpc(Player player, EntityNPCInterface npc, CallbackInfo callback)
    {
        if (player instanceof ServerPlayer serverPlayer && (npc == null || !FlansModApi.isEntityTrackingPinned(serverPlayer, npc)))
            FlansModApi.pinEntityTracking(serverPlayer, null);
    }
}
