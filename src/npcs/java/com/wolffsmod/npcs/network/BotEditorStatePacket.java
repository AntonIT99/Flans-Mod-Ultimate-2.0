package com.wolffsmod.npcs.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;

/**
 * What the bot editor screen shows an operator: the rounds of the rotation, the bot settings of the one being edited,
 * and the squad templates. Sent when the screen opens and after each change the operator saves.
 *
 * @param round
 *            the round being edited, as an index into {@code rounds}, or -1 when the rotation is empty
 * @param settings
 *            that round's bot settings, as they are saved
 * @param templates
 *            every squad template
 */
public record BotEditorStatePacket(List<Round> rounds, int round, CompoundTag settings, List<CompoundTag> templates)
{
    private static final int MAX_TEXT = 256;

    /**
     * A round of the rotation.
     *
     * @param teams
     *            the short names of its teams, in round order
     */
    public record Round(UUID id, String label, List<String> teams)
    {}

    public BotEditorStatePacket
    {
        rounds = List.copyOf(rounds);
        templates = List.copyOf(templates);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeCollection(rounds, (out, entry) ->
        {
            out.writeUUID(entry.id());
            out.writeUtf(entry.label(), MAX_TEXT);
            out.writeCollection(entry.teams(), (teamOut, team) -> teamOut.writeUtf(team, MAX_TEXT));
        });
        buffer.writeVarInt(round);
        buffer.writeNbt(settings);
        buffer.writeCollection(templates, FriendlyByteBuf::writeNbt);
    }

    public static BotEditorStatePacket read(FriendlyByteBuf buffer)
    {
        List<Round> rounds = buffer.readList(in -> new Round(in.readUUID(), in.readUtf(MAX_TEXT), in.readList(teamIn -> teamIn.readUtf(MAX_TEXT))));
        int round = buffer.readVarInt();
        CompoundTag settings = buffer.readNbt();
        List<CompoundTag> templates = buffer.readList(FriendlyByteBuf::readNbt);
        return new BotEditorStatePacket(rounds, round, settings == null ? new CompoundTag() : settings, templates.stream().map(tag -> tag == null ? new CompoundTag() : tag).toList());
    }
}
