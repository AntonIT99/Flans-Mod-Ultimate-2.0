package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.teams.EditorClientState;
import com.flansmodultimate.common.teams.editor.TeamEditorStore;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Bounded chunks keep large edited libraries below the transport's individual packet limit. */
@NoArgsConstructor
public final class PacketEditorState implements IClientPacket
{
    public static final int CHUNK_SIZE = 24000;
    private UUID transfer;
    private long revision;
    private int index;
    private int count;
    private String content = "";
    private String open = "";
    private String key = "";
    private String message = "";

    public static void send(ServerPlayer player, TeamEditorStore.Document document, long revision, String open, String key, String message)
    {
        JsonObject root = new JsonObject();
        root.add("teams", document.teams());
        root.add("classes", document.classes());
        String json = root.toString();
        UUID transfer = UUID.randomUUID();
        int count = Math.max(1, (json.length() + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int index = 0; index < count; index++)
        {
            PacketEditorState packet = new PacketEditorState();
            packet.transfer = transfer;
            packet.revision = revision;
            packet.index = index;
            packet.count = count;
            packet.content = json.substring(index * CHUNK_SIZE, Math.min(json.length(), (index + 1) * CHUNK_SIZE));
            packet.open = open;
            packet.key = key;
            packet.message = message.length() > 1000 ? message.substring(0, 1000) : message;
            PacketHandler.sendTo(packet, player);
        }
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(transfer);
        data.writeLong(revision);
        data.writeVarInt(index);
        data.writeVarInt(count);
        data.writeUtf(content, CHUNK_SIZE);
        data.writeUtf(open, 16);
        data.writeUtf(key, 512);
        data.writeUtf(message, 1000);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        transfer = data.readUUID();
        revision = data.readLong();
        index = data.readVarInt();
        count = data.readVarInt();
        content = data.readUtf(CHUNK_SIZE);
        open = data.readUtf(16);
        key = data.readUtf(512);
        message = data.readUtf(1000);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        EditorClientState.accept(transfer, revision, index, count, content, open, key, message);
    }
}
