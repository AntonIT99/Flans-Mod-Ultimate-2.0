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
            case HUB -> minecraft.gui.setScreen(new TeamsLoadoutHubScreen());
            case CHOOSE -> minecraft.gui.setScreen(new TeamsChooseLoadoutScreen());
            case EDIT -> {
                var current = minecraft.gui.screen() instanceof TeamsLoadoutEditScreen editor ? editor.getSelectedSlot() : com.flansmodultimate.common.teams.LoadoutSlot.PRIMARY;
                minecraft.gui.setScreen(new TeamsLoadoutEditScreen(update.getEditLoadout(), current));
            }
            case REWARD_BOX -> minecraft.gui.setScreen(new TeamsRewardBoxScreen());
            case MISSION_RESULTS -> minecraft.gui.setScreen(new TeamsMissionResultsScreen());
            case CLOSE -> {
                if (minecraft.gui.screen() instanceof TeamsLoadoutHubScreen || minecraft.gui.screen() instanceof TeamsChooseLoadoutScreen
                    || minecraft.gui.screen() instanceof TeamsLoadoutEditScreen || minecraft.gui.screen() instanceof TeamsRewardBoxScreen
                    || minecraft.gui.screen() instanceof TeamsMissionResultsScreen) minecraft.gui.setScreen(null);
            }
            case NONE -> {
                // No-op
            }
        }
    }
}
