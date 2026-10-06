package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(time);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        time = data.readInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.updateFlash(true, time);
    }
}
