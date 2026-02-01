package com.flansmodultimate.network.client;

import com.flansmodultimate.client.SoundHelper;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        SoundHelper.cancelSound(instanceUUID);
    }
}