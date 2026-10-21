package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.teams.BotEditor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * What an operator does in the bot editor screen: switch to another round, save its settings, save or remove a squad
 * template, or save, apply or remove a preset, or copy another round's settings. The server checks the operator's permission and that the round is still the one shown, applies the change
 * and sends the screen's state back.
 *
 * @param round
 *            the round shown, as an index into the rotation
 * @param roundId
 *            that round's id, so a change made after the rotation was edited elsewhere is refused
 * @param data
 *            the settings or the template, as tag data; empty for {@link Action#OPEN}
 */
public record BotEditorActionPacket(Action action, int round, UUID roundId, CompoundTag data)
{
    /** What the operator asks for. */
    public enum Action
    {
        OPEN, SAVE_SETTINGS, SAVE_TEMPLATE, REMOVE_TEMPLATE,
        /** Saving the settings shown, as they are edited, under a preset name. */
        SAVE_PRESET,
        /** Applying a preset to the round shown. */
        APPLY_PRESET, REMOVE_PRESET,
        /** Copying another round's settings to the round shown. */
        COPY_ROUND
    }

    public static BotEditorActionPacket open(int round)
    {
        return new BotEditorActionPacket(Action.OPEN, round, new UUID(0L, 0L), new CompoundTag());
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(round);
        buffer.writeUUID(roundId);
        buffer.writeNbt(data);
    }

    public static BotEditorActionPacket read(FriendlyByteBuf buffer)
    {
        Action action = buffer.readEnum(Action.class);
        int round = buffer.readVarInt();
        UUID roundId = buffer.readUUID();
        CompoundTag data = buffer.readNbt();
        return new BotEditorActionPacket(action, round, roundId, data == null ? new CompoundTag() : data);
    }

    public void handle(ServerPlayer player)
    {
        BotEditor.handle(player, this);
    }
}
