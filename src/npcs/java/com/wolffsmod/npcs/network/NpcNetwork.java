package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** The module's own channel. Everything else it sends rides on Custom NPCs' packets. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcNetwork
{
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register()
    {
        CHANNEL.registerMessage(0, VehicleStatePacket.class, VehicleStatePacket::write, VehicleStatePacket::read, (packet, context) ->
        {
            // The handler class is only loaded on a client.
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.wolffsmod.npcs.client.NpcClientPackets.vehicleState(packet)));
            context.get().setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, ServerControlPacket.class, ServerControlPacket::write, ServerControlPacket::read, (packet, context) ->
        {
            ServerPlayer sender = context.get().getSender();
            if (sender != null)
                context.get().enqueueWork(() -> packet.handle(sender));
            else
                context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.wolffsmod.npcs.client.NpcClientPackets.serverSettings(packet)));
            context.get().setPacketHandled(true);
        });
    }

    public static void sendTo(ServerPlayer player, ServerControlPacket packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(ServerControlPacket packet)
    {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToTracking(EntityNPCInterface npc, VehicleStatePacket packet)
    {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> npc), packet);
    }

    /** A client that starts tracking an NPC needs its current state, not just the next change. */
    public static void startTracking(PlayerEvent.StartTracking event)
    {
        if (event.getTarget() instanceof EntityNPCInterface npc && event.getEntity() instanceof ServerPlayer player && NpcVehicleState.of(npc).isActive())
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), NpcVehicleState.of(npc).packet(npc));
    }
}
