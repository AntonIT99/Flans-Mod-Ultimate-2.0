package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketFlashBang implements IClientPacket
{
    private int time = 10;

    public PacketFlashBang(int flashTime)
    {
        time = flashTime;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeInt(time);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        time = data.readInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.updateFlash(true, time);
    }
}
