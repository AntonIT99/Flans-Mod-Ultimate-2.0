package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.centre.CommandCentre;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * An operator's request from the Teams command centre: open it, refresh what it shows, or run a {@code /teams} command.
 * Intent only: the server checks the permission, runs the command as the player and answers with the centre's state.
 */
@NoArgsConstructor
public final class PacketCommandCentreAction implements IServerPacket
{
    /** Longest command line accepted, well above what any {@code /teams} command needs. */
    public static final int MAX_COMMAND = 4096;

    private String command = "";
    private boolean open;

    private PacketCommandCentreAction(String command, boolean open)
    {
        this.command = command;
        this.open = open;
    }

    public static PacketCommandCentreAction open()
    {
        return new PacketCommandCentreAction("", true);
    }

    public static PacketCommandCentreAction refresh()
    {
        return new PacketCommandCentreAction("", false);
    }

    public static PacketCommandCentreAction run(String command)
    {
        return new PacketCommandCentreAction(command.length() > MAX_COMMAND ? command.substring(0, MAX_COMMAND) : command, false);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(command, MAX_COMMAND);
        data.writeBoolean(open);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        command = data.readUtf(MAX_COMMAND);
        open = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        CommandCentre.handle(player, command, open);
    }
}
