package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.NpcRemotePacket;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketRemoteNpcsGet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Supplies precise distances, unique identities and factions through the wand's existing permission-checked scan. */
@Mixin(value = SPacketRemoteNpcsGet.class, remap = false)
public abstract class SPacketRemoteNpcsGetMixin
{
    @Inject(method = "sendNearbyNpcs", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("AutoCloseableResource") // The server owns the level; scanning must not close it.
    private static void wolffsmodnpcsScan(ServerPlayer player, CallbackInfo callback)
    {
        List<NpcRemotePacket.Entry> entries = new ArrayList<>();
        for (Entity entity : player.serverLevel().getAllEntities())
        {
            if (entity instanceof EntityNPCInterface npc && !npc.isRemoved())
            {
                var faction = npc.getFaction();
                entries.add(new NpcRemotePacket.Entry(npc.getId(), npc.display.getName(), player.distanceToSqr(npc), faction.id, faction.name));
            }
        }
        NpcNetwork.sendTo(player, new NpcRemotePacket(entries));
        callback.cancel();
    }
}
