package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
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
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(instanceUUID);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        instanceUUID = data.readUUID();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.SOUND.cancelSound(instanceUUID);
    }
}