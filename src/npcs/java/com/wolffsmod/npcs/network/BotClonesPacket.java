package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.teams.BotEditor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * The Custom NPCs clones the bot editor offers an operator to make a team's bots from. A request without a name asks for
 * the names saved in a clone tab, one with a name for that clone's data, to preview it; the server answers with the same
 * tab, name and token, so a late answer is not taken for a later choice.
 *
 * @param names
 *            the clones of the tab, in an answer to a request without a name
 * @param data
 *            the clone's entity data, in an answer to a request with a name; empty when there is no such clone
 */
public record BotClonesPacket(int tab, String name, UUID token, boolean request, List<String> names, CompoundTag data)
{
    private static final int MAX_NAME_LENGTH = 256;

    public BotClonesPacket
    {
        names = List.copyOf(names);
    }

    public static BotClonesPacket list(int tab, UUID token)
    {
        return new BotClonesPacket(tab, "", token, true, List.of(), new CompoundTag());
    }

    public static BotClonesPacket preview(int tab, String name, UUID token)
    {
        return new BotClonesPacket(tab, name, token, true, List.of(), new CompoundTag());
    }

    /** @return the answer to this request */
    public BotClonesPacket answer(List<String> clones, CompoundTag clone)
    {
        return new BotClonesPacket(tab, name, token, false, clones, clone);
    }

    // A method reference to writeNbt is ambiguous on 1.21, whose buffer overloads it; the lambda is shared.
    @SuppressWarnings("java:S1612")
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(tab);
        buffer.writeUtf(name, MAX_NAME_LENGTH);
        buffer.writeUUID(token);
        buffer.writeBoolean(request);
        // Requests carry no clone data. Only the server supplies it.
        if (request)
            return;
        buffer.writeCollection(names, (out, clone) -> out.writeUtf(clone, MAX_NAME_LENGTH));
        buffer.writeNbt(data);
    }

    public static BotClonesPacket read(FriendlyByteBuf buffer)
    {
        int tab = buffer.readVarInt();
        String name = buffer.readUtf(MAX_NAME_LENGTH);
        UUID token = buffer.readUUID();
        boolean request = buffer.readBoolean();
        if (request)
            return new BotClonesPacket(tab, name, token, true, List.of(), new CompoundTag());
        List<String> names = buffer.readList(in -> in.readUtf(MAX_NAME_LENGTH));
        CompoundTag data = buffer.readNbt();
        return new BotClonesPacket(tab, name, token, false, names, data == null ? new CompoundTag() : data);
    }

    public void handle(ServerPlayer player)
    {
        if (request)
            BotEditor.clones(player, this);
    }
}
