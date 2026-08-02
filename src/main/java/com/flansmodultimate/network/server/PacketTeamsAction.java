package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.client.PacketTeamsState;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public final class PacketTeamsAction implements IServerPacket
{
    public enum Action
    {
        SELECT_TEAM,
        SELECT_CLASS,
        CAST_VOTE,
        OPEN_TEAM,
        OPEN_CLASS,
        OPEN_SCOREBOARD
    }

    private Action action = Action.OPEN_SCOREBOARD;
    private String value = "";

    private PacketTeamsAction(Action action, String value)
    {
        this.action = action;
        this.value = value;
    }

    public static PacketTeamsAction selectTeam(String id)
    {
        return new PacketTeamsAction(Action.SELECT_TEAM, id);
    }

    public static PacketTeamsAction selectClass(String id)
    {
        return new PacketTeamsAction(Action.SELECT_CLASS, id);
    }

    public static PacketTeamsAction castVote(int option)
    {
        return new PacketTeamsAction(Action.CAST_VOTE, Integer.toString(option));
    }

    public static PacketTeamsAction openTeamMenu()
    {
        return new PacketTeamsAction(Action.OPEN_TEAM, "");
    }

    public static PacketTeamsAction openClassMenu()
    {
        return new PacketTeamsAction(Action.OPEN_CLASS, "");
    }

    public static PacketTeamsAction openScoreboard()
    {
        return new PacketTeamsAction(Action.OPEN_SCOREBOARD, "");
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeByte(action.ordinal());
        data.writeUtf(value, 128);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        int actionId = data.readUnsignedByte();
        action = actionId < Action.values().length ? Action.values()[actionId] : Action.OPEN_SCOREBOARD;
        value = data.readUtf(128);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        TeamsManager manager = TeamsManager.getInstance();
        switch (action)
        {
            case OPEN_TEAM -> {
                if (manager.getCurrentLoadoutPool().isPresent()) manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.HUB, 0, "");
                else manager.syncPlayer(player, PacketTeamsState.OpenScreen.TEAM_SELECT);
            }
            case OPEN_CLASS -> {
                PlayerData data = PlayerData.getInstance(player);
                if (data.getNewTeam() == null || data.getNewTeam() == Team.SPECTATORS || data.getNewTeam().getClasses().isEmpty())
                    player.sendSystemMessage(Component.literal("Select a playable team first"));
                else
                    manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLASS_SELECT);
            }
            case OPEN_SCOREBOARD -> manager.syncPlayer(player, PacketTeamsState.OpenScreen.SCOREBOARD);
            case CAST_VOTE -> {
                int option;
                try { option = Integer.parseInt(value); }
                catch (NumberFormatException ignored) { return; }
                if (!manager.castVote(player, option))
                    player.sendSystemMessage(Component.literal("That voting option is not available"));
            }
            case SELECT_TEAM -> selectTeam(manager, player);
            case SELECT_CLASS -> selectClass(manager, player);
        }
    }

    private void selectTeam(TeamsManager manager, ServerPlayer player)
    {
        if ("$builder".equals(value))
        {
            if (!manager.selectBuilder(player))
                player.sendSystemMessage(Component.literal("Builder mode requires operator permission"));
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLOSE);
            return;
        }

        Team team = Team.getTeam(value);
        if (team == null || !manager.selectTeam(player, team, false))
        {
            player.sendSystemMessage(Component.literal("That team is unavailable or would unbalance the round"));
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.TEAM_SELECT);
            return;
        }

        if (team == Team.SPECTATORS || team.getClasses().isEmpty())
        {
            manager.respawnPlayer(player, true);
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLOSE);
        }
        else if (manager.getCurrentLoadoutPool().isPresent())
        {
            manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.CHOOSE, 0, "");
        }
        else
        {
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLASS_SELECT);
        }
    }

    private void selectClass(TeamsManager manager, ServerPlayer player)
    {
        PlayerClass playerClass = PlayerClass.getPlayerClass(value);
        if (!manager.selectClass(player, playerClass))
        {
            player.sendSystemMessage(Component.literal("That class is unavailable or requires a higher rank"));
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLASS_SELECT);
            return;
        }
        manager.respawnPlayer(player, true);
        manager.syncPlayer(player, PacketTeamsState.OpenScreen.CLOSE);
    }
}
