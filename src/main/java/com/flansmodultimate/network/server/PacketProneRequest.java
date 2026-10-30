package com.flansmodultimate.network.server;

import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The Go Prone key asks the server to lay the player down or stand them up; the server decides and answers. */
@NoArgsConstructor
public class PacketProneRequest implements IServerPacket
{
    private boolean prone;

    public PacketProneRequest(boolean prone)
    {
        this.prone = prone;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(prone);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        prone = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ProneSupport.request(player, prone);
    }
}
