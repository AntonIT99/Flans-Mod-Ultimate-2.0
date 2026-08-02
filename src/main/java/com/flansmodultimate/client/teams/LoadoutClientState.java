package com.flansmodultimate.client.teams;

import com.flansmodultimate.client.gui.TeamsChooseLoadoutScreen;
import com.flansmodultimate.client.gui.TeamsLoadoutEditScreen;
import com.flansmodultimate.client.gui.TeamsLoadoutHubScreen;
import com.flansmodultimate.client.gui.TeamsMissionResultsScreen;
import com.flansmodultimate.client.gui.TeamsRewardBoxScreen;
import com.flansmodultimate.network.client.PacketLoadoutState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;

/** Client cache for server-authored ranked progression snapshots. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoadoutClientState
{
    private static PacketLoadoutState snapshot;

    public static PacketLoadoutState get()
    {
        return snapshot;
    }

    public static void accept(PacketLoadoutState update)
    {
        snapshot = update;
        Minecraft minecraft = Minecraft.getInstance();
        switch (update.getOpenScreen())
        {
            case HUB -> minecraft.setScreen(new TeamsLoadoutHubScreen());
            case CHOOSE -> minecraft.setScreen(new TeamsChooseLoadoutScreen());
            case EDIT -> {
                var current = minecraft.screen instanceof TeamsLoadoutEditScreen editor ? editor.getSelectedSlot() : com.flansmodultimate.common.teams.LoadoutSlot.PRIMARY;
                minecraft.setScreen(new TeamsLoadoutEditScreen(update.getEditLoadout(), current));
            }
            case REWARD_BOX -> minecraft.setScreen(new TeamsRewardBoxScreen());
            case MISSION_RESULTS -> minecraft.setScreen(new TeamsMissionResultsScreen());
            case CLOSE -> {
                if (minecraft.screen instanceof TeamsLoadoutHubScreen || minecraft.screen instanceof TeamsChooseLoadoutScreen
                    || minecraft.screen instanceof TeamsLoadoutEditScreen || minecraft.screen instanceof TeamsRewardBoxScreen
                    || minecraft.screen instanceof TeamsMissionResultsScreen) minecraft.setScreen(null);
            }
            case NONE -> {
                // No-op
            }
        }
    }
}
