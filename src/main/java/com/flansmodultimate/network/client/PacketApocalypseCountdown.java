package com.flansmodultimate.network.client;

import com.flansmodultimate.apocalyse.client.ApocalypseClientState;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Ticks left before an armed AI-chip apocalypse trigger fires. Zero means no countdown. */
@NoArgsConstructor
public class PacketApocalypseCountdown implements IClientPacket
{
    private int ticksRemaining;

    public PacketApocalypseCountdown(int ticksRemaining)
    {
        this.ticksRemaining = ticksRemaining;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeVarInt(Math.max(0, ticksRemaining));
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        ticksRemaining = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ApocalypseClientState.setCountdownTicks(ticksRemaining);
    }
}
