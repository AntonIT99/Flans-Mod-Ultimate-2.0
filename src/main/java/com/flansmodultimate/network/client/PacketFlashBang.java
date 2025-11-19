package com.flansmodultimate.network.client;

import com.flansmodultimate.ModClient;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.util.ModUtils;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

@NoArgsConstructor
public class PacketFlashBang implements IClientPacket
{
    private int time = 10;

    public PacketFlashBang(int flashTime)
    {
        time = flashTime;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(time);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        time = data.readInt();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        if (ModUtils.isThePlayer(player))
        {
            ModClient.setInFlash(true);
            ModClient.setFlashTime(time);
        }
    }
}
