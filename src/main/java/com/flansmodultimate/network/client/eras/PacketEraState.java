package com.flansmodultimate.network.client.eras;

import com.flansmodultimate.client.eras.EraClientState;
import com.flansmodultimate.common.eras.EraState;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** The player's {@link EraState}, in bounded chunks because date overrides for whole packs can exceed one packet. */
@NoArgsConstructor
public final class PacketEraState implements IClientPacket
{
    public static final int CHUNK_SIZE = 24000;
    /** More chunks than this can only come from a corrupt stream; the client discards the transfer. */
    public static final int MAX_CHUNKS = 256;
    private UUID transfer;
    private int index;
    private int count;
    private String content = "";

    public static void send(ServerPlayer player, EraState state)
    {
        String json = state.toJson().toString();
        UUID transfer = UUID.randomUUID();
        int count = Math.max(1, (json.length() + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int index = 0; index < count; index++)
        {
            PacketEraState packet = new PacketEraState();
            packet.transfer = transfer;
            packet.index = index;
            packet.count = count;
            packet.content = json.substring(index * CHUNK_SIZE, Math.min(json.length(), (index + 1) * CHUNK_SIZE));
            PacketHandler.sendTo(packet, player);
        }
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(transfer);
        data.writeVarInt(index);
        data.writeVarInt(count);
        data.writeUtf(content, CHUNK_SIZE);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        transfer = data.readUUID();
        index = data.readVarInt();
        count = data.readVarInt();
        content = data.readUtf(CHUNK_SIZE);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        EraClientState.accept(transfer, index, count, content);
    }
}
