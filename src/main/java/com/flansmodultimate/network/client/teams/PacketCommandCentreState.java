package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.gui.centre.CommandCentreClient;
import com.flansmodultimate.common.teams.centre.CommandCentre;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.PacketIO;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Teams command centre shows an operator: every section the server builds, by key, and the output of the
 * command the operator ran last, if this answers one.
 */
@NoArgsConstructor
public final class PacketCommandCentreState implements IClientPacket
{
    private boolean open;
    private CompoundTag sections = new CompoundTag();
    private List<CommandCentre.Feedback> feedback = List.of();

    public PacketCommandCentreState(boolean open, CompoundTag sections, List<CommandCentre.Feedback> feedback)
    {
        this.open = open;
        this.sections = sections;
        this.feedback = List.copyOf(feedback);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(open);
        data.writeNbt(sections);
        data.writeVarInt(feedback.size());
        for (CommandCentre.Feedback line : feedback)
        {
            PacketIO.writeComponent(data, line.text());
            data.writeBoolean(line.failure());
        }
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        open = data.readBoolean();
        CompoundTag read = data.readNbt();
        sections = read == null ? new CompoundTag() : read;
        int count = Math.min(data.readVarInt(), CommandCentre.MOST_FEEDBACK);
        List<CommandCentre.Feedback> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            lines.add(new CommandCentre.Feedback(PacketIO.readComponent(data), data.readBoolean()));
        feedback = List.copyOf(lines);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        CommandCentreClient.accept(open, sections, feedback);
    }
}
