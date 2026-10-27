package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.FlansModApi;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketGuiOpen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Opening the NPC editor pins the NPC to the editing player, so an NPC beyond the client's tracking range, opened from the
 * remote, still exists on the client the editor resolves it on. {@link NoppesUtilServerMixin} ends the pin.
 */
@SuppressWarnings("java:S1118") // Mixin class; its static injector is merged into the target, not instantiated.
@Mixin(value = SPacketGuiOpen.class, remap = false)
public abstract class SPacketGuiOpenMixin
{
    @Inject(method = "sendOpenGui", at = @At("HEAD"))
    private static void wolffsmodnpcsPinEditedNpc(Player player, EnumGuiType gui, EntityNPCInterface npc, BlockPos pos, CallbackInfo callback)
    {
        // Every tab of the editor main menu; its other screens reopen with the same NPC, which keeps the pin
        if (player instanceof ServerPlayer serverPlayer && npc != null && gui.name().startsWith("MainMenu"))
            FlansModApi.pinEntityTracking(serverPlayer, npc);
    }
}
