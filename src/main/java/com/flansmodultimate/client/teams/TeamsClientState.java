package com.flansmodultimate.client.teams;

import com.flansmodultimate.client.gui.TeamsScoreScreen;
import com.flansmodultimate.client.gui.TeamsSelectScreen;
import com.flansmodultimate.client.gui.TeamsVotingScreen;
import com.flansmodultimate.network.client.PacketTeamsState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** Client-only snapshot store. Server snapshots remain the sole source of truth. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsClientState
{
    private static PacketTeamsState snapshot;

    public static PacketTeamsState get()
    {
        return snapshot;
    }

    /** Matches legacy Teams visibility: active players cannot see spectator models. */
    public static boolean shouldHidePlayer(Player renderedPlayer)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        return hasActiveRound() && viewer != null && renderedPlayer != viewer
            && !isTeamsSpectator(viewer) && isTeamsSpectator(renderedPlayer);
    }

    /** Enemy tags are hidden in team modes; every active-player tag is hidden in FFA. */
    public static boolean shouldHideNameTag(Player renderedPlayer)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        if (!hasActiveRound() || viewer == null || renderedPlayer == viewer || isTeamsSpectator(viewer))
            return false;
        if (isTeamsSpectator(renderedPlayer))
            return true;
        if (!snapshot.isSortedByTeam())
            return true;

        Optional<String> viewerTeam = findTeam(viewer);
        Optional<String> renderedTeam = findTeam(renderedPlayer);
        return viewerTeam.isPresent() && renderedTeam.isPresent() && !viewerTeam.get().equals(renderedTeam.get());
    }

    private static boolean hasActiveRound()
    {
        return snapshot != null && snapshot.isEnabled() && snapshot.isRoundRunning();
    }

    private static boolean isTeamsSpectator(Player player)
    {
        if (player.isSpectator())
            return true;
        // The selected team is an intent until the server applies it. For the local player,
        // the synchronized vanilla spectator state is therefore the authoritative value.
        if (Minecraft.getInstance().player == player)
            return false;
        return findTeam(player).map("spectators"::equals).orElse(false);
    }

    private static Optional<String> findTeam(Player player)
    {
        if (snapshot == null)
            return Optional.empty();
        String name = player.getGameProfile().getName();
        for (PacketTeamsState.TeamScore team : snapshot.getTeamScores())
            if (team.players().stream().anyMatch(entry -> entry.name().equalsIgnoreCase(name)))
                return Optional.of(team.id());
        if (Minecraft.getInstance().player == player && !snapshot.getSelectedTeam().isBlank())
            return Optional.of(snapshot.getSelectedTeam());
        return Optional.empty();
    }

    public static void accept(PacketTeamsState update)
    {
        Minecraft minecraft = Minecraft.getInstance();
        snapshot = update;
        if (update.getOpenScreen() == PacketTeamsState.OpenScreen.NONE && minecraft.screen instanceof TeamsSelectScreen)
            return;
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

    public static void clear()
    {
        snapshot = null;
    }
}
