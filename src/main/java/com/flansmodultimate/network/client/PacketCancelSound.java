package com.flansmodultimate.network.client;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

@NoArgsConstructor
public class PacketCancelSound implements IClientPacket
{
    private UUID instanceUUID;

    public PacketCancelSound(UUID instanceUUID)
    {
        this.instanceUUID = instanceUUID;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUUID(instanceUUID);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        instanceUUID = data.readUUID();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        if (ModClient.getCancellableSounds().containsKey(instanceUUID))
        {
            Minecraft.getInstance().getSoundManager().stop(ModClient.getCancellableSounds().get(instanceUUID));
            ModClient.getCancellableSounds().remove(instanceUUID);
        }
    }
}
