package com.flansmodultimate.client.teams;

import com.flansmodultimate.client.gui.TeamsScoreScreen;
import com.flansmodultimate.client.gui.TeamsSelectScreen;
import com.flansmodultimate.client.gui.TeamsVotingScreen;
import com.flansmodultimate.network.client.PacketTeamsState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;

/** Client-only snapshot store. Server snapshots remain the sole source of truth. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsClientState
{
    private static PacketTeamsState snapshot;

    public static PacketTeamsState get()
    {
        return snapshot;
    }

    public static void accept(PacketTeamsState update)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (update.getOpenScreen() == PacketTeamsState.OpenScreen.NONE && minecraft.screen instanceof TeamsSelectScreen)
            return;
        snapshot = update;
        switch (update.getOpenScreen())
        {
            case TEAM_SELECT -> minecraft.setScreen(new TeamsSelectScreen(false));
            case CLASS_SELECT -> minecraft.setScreen(new TeamsSelectScreen(true));
            case SCOREBOARD -> minecraft.setScreen(new TeamsScoreScreen());
            case VOTING -> minecraft.setScreen(new TeamsVotingScreen());
            case CLOSE -> {
                if (minecraft.screen instanceof TeamsSelectScreen || minecraft.screen instanceof TeamsScoreScreen
                    || minecraft.screen instanceof TeamsVotingScreen)
                    minecraft.setScreen(null);
            }
            case NONE -> {
                if (minecraft.screen instanceof TeamsVotingScreen && update.getVoteOptions().isEmpty())
                    minecraft.setScreen(null);
            }
        }
    }
}
