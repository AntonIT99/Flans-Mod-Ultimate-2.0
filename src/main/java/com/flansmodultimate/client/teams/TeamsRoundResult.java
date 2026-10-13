package com.flansmodultimate.client.teams;

import com.flansmodultimate.client.sound.SoundHelper;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Optional;

/** How the round that just ended went for the local player, and the sound that tells them so. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsRoundResult
{
    public enum Outcome
    {
        /** No round has just ended. */
        NONE, VICTORY, DEFEAT, DRAW,
        /** A team won and the local player was on neither side. */
        WATCHED
    }

    public static Outcome outcome(@Nullable PacketTeamsState state)
    {
        if (state == null || !state.isRoundDecided())
            return Outcome.NONE;
        if (state.getWinnerId().isBlank())
            return Outcome.DRAW;
        if (state.getPlayerTeamId() < 2)
            return Outcome.WATCHED;
        // The winner is reported as the first team of its side; everyone allied with it won too.
        return state.getWinnerId().equals(state.getSelectedTeam()) || state.areAllied(state.getWinnerId(), state.getSelectedTeam()) ? Outcome.VICTORY : Outcome.DEFEAT;
    }

    /** The winning team as content on this client, the first of its side for an alliance, or null for a draw or a team this client does not have. */
    @Nullable
    public static Team winner(PacketTeamsState state)
    {
        return state.getWinnerId().isBlank() ? null : Team.getTeam(state.getWinnerId());
    }

    /**
     * Plays the sound of the result once, when the round ends: the victory sound of the winners
     * for those on that team, their own defeat sound for the losers, and the draw sound otherwise.
     * A winning team can switch its sounds off with {@code HasVictorySound False}.
     */
    public static void announce(PacketTeamsState state)
    {
        Team winner = winner(state);
        if (winner != null && !winner.isVictorySoundEnabled())
            return;
        Outcome outcome = outcome(state);
        Team own = Team.getTeam(state.getSelectedTeam());
        Optional<SoundEvent> sound = switch (outcome)
        {
            case VICTORY -> SoundHelper.getSoundEvent(winner == null ? Team.DEFAULT_VICTORY_SOUND : winner.getVictorySound()).or(() -> Optional.of(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE));
            case DEFEAT -> SoundHelper.getSoundEvent(own == null ? Team.DEFAULT_DEFEAT_SOUND : own.getDefeatSound());
            case DRAW -> SoundHelper.getSoundEvent(Team.DRAW_SOUND);
            default -> Optional.empty();
        };
        sound.ifPresent(event -> Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1F, 1F)));
    }
}
