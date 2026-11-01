package com.flansmodultimate.client.gui;

import com.flansmodultimate.client.teams.TeamFlagTextures;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.client.teams.TeamsRoundResult;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Says how a Teams round ended before the scoreboard comes up: a banner with the victory, defeat or draw over the world,
 * the winners' flag, the final score of each side and how the player did. It gives way to the scoreboard after a few
 * seconds, or at once on a click or key press.
 */
public final class TeamsRoundEndScreen extends Screen
{
    private static final long SHOW_MILLIS = 5000L;
    private static final long BACKDROP_IN_MILLIS = 400L;
    private static final long BANNER_GROW_MILLIS = 450L;
    private static final long TEXT_IN_MILLIS = 350L;
    private static final long DETAILS_IN_MILLIS = 700L;
    private static final int BANNER_HEIGHT = 56;
    private static final int TITLE_SCALE = 4;
    private static final int FLAG_WIDTH = 48;
    private static final int FLAG_HEIGHT = 32;
    private static final int VICTORY_COLOUR = 0xF2C94C;
    private static final int DEFEAT_COLOUR = 0xE04B3A;
    private static final int NEUTRAL_COLOUR = 0xE6E6E6;

    private final long openedMillis = System.currentTimeMillis();

    public TeamsRoundEndScreen()
    {
        super(Component.translatable("gui.flansmod.teams.round_over"));
    }

    @Override
    public void tick()
    {
        if (System.currentTimeMillis() - openedMillis > SHOW_MILLIS)
            showScores();
    }

    private void showScores()
    {
        if (minecraft != null)
            minecraft.setScreen(new TeamsScoreScreen());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
            return;
        long shown = System.currentTimeMillis() - openedMillis;
        TeamsRoundResult.Outcome outcome = TeamsRoundResult.outcome(state);
        Team winner = TeamsRoundResult.winner(state);
        int accent = accent(outcome, winner);

        graphics.fillGradient(0, 0, width, height, fade(shown, 0x70), fade(shown, 0xC0));
        int centreY = height / 2 - 20;
        renderBanner(graphics, shown, accent, centreY);
        renderTitle(graphics, shown, title(state, outcome, winner), accent, centreY);

        float details = Mth.clamp((shown - DETAILS_IN_MILLIS) / 400F, 0F, 1F);
        if (details <= 0F)
            return;
        int alpha = Math.max(5, Math.round(255F * details)) << 24;
        TeamFlagTextures.FlagTexture flag = TeamFlagTextures.get(winner);
        if (flag != null)
            TeamFlagTextures.draw(graphics, flag, width / 2 - FLAG_WIDTH / 2, centreY - BANNER_HEIGHT / 2 - FLAG_HEIGHT - 8, FLAG_WIDTH, FLAG_HEIGHT);
        int y = centreY + BANNER_HEIGHT / 2 + 12;
        graphics.drawCenteredString(font, state.getMapName() + " - " + state.getGameType(), width / 2, y, alpha | 0xB4B4B4);
        renderSideScores(graphics, state, y + 16, alpha);
        Component stats = playerStats(state);
        if (stats != null)
            graphics.drawCenteredString(font, stats, width / 2, y + 40, alpha | 0xFFFFFF);
    }

    /** A band across the screen in the colour of the result, growing out from the middle. */
    private void renderBanner(GuiGraphics graphics, long shown, int accent, int centreY)
    {
        float grown = ease(Mth.clamp(shown / (float) BANNER_GROW_MILLIS, 0F, 1F));
        int half = Math.round(width / 2F * grown);
        int top = centreY - BANNER_HEIGHT / 2;
        int bottom = centreY + BANNER_HEIGHT / 2;
        graphics.fill(width / 2 - half, top, width / 2 + half, bottom, 0xB0000000);
        graphics.fillGradient(width / 2 - half, top, width / 2 + half, top + BANNER_HEIGHT / 2, 0x00000000, 0x50000000 | accent);
        graphics.fillGradient(width / 2 - half, top + BANNER_HEIGHT / 2, width / 2 + half, bottom, 0x50000000 | accent, 0x00000000);
        graphics.fill(width / 2 - half, top, width / 2 + half, top + 1, 0xFF000000 | accent);
        graphics.fill(width / 2 - half, bottom - 1, width / 2 + half, bottom, 0xFF000000 | accent);
    }

    /** The result in large letters, settling from a little larger as it fades in. */
    private void renderTitle(GuiGraphics graphics, long shown, Component text, int accent, int centreY)
    {
        float in = Mth.clamp((shown - BANNER_GROW_MILLIS * 0.5F) / (float) TEXT_IN_MILLIS, 0F, 1F);
        if (in <= 0F)
            return;
        float scale = TITLE_SCALE * (1F + 0.35F * (1F - ease(in)));
        // A long name of a winning team is drawn smaller so that it fits across the screen.
        scale = Math.min(scale, (width - 20F) / Math.max(1, font.width(text)));
        int alpha = Math.max(5, Math.round(255F * in));
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, centreY - (font.lineHeight - 1) * scale / 2F, 0F);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawCenteredString(font, text, 0, 0, alpha << 24 | accent);
        graphics.pose().popPose();
    }

    /** The final score of every side in its colour, the player's own side marked. */
    private void renderSideScores(GuiGraphics graphics, PacketTeamsState state, int y, int alpha)
    {
        if (!state.isSortedByTeam())
            return;
        List<Component> parts = new ArrayList<>();
        for (int sideId : state.getSideIds())
        {
            PacketTeamsState.TeamScore side = state.getTeamScore(sideId);
            if (side == null)
                continue;
            String name = state.getSideName(sideId);
            parts.add(Component.literal(name + "  " + side.score()).withStyle(style -> style.withColor(side.colour()).withBold(sideId == state.getPlayerSideId())));
        }
        if (parts.isEmpty())
            return;
        Component line = parts.get(0).copy();
        for (int i = 1; i < parts.size(); i++)
            line = Component.empty().append(line).append(Component.literal("   |   ").withStyle(style -> style.withColor(0x808080))).append(parts.get(i));
        graphics.drawCenteredString(font, line, width / 2, y, alpha | 0xFFFFFF);
    }

    /** The player's score, kills and deaths in the round, or null when they did not play in it. */
    @Nullable
    private Component playerStats(PacketTeamsState state)
    {
        if (minecraft == null || minecraft.player == null)
            return null;
        String name = minecraft.player.getGameProfile().getName();
        return state.getTeamScores().stream().flatMap(team -> team.players().stream()).filter(player -> player.name().equalsIgnoreCase(name)).findFirst()
            .map(player -> (Component) Component.translatable("gui.flansmod.teams.score").append(" " + player.score() + "     ").append(Component.translatable("gui.flansmod.teams.kills"))
                .append(" " + player.kills() + "     ").append(Component.translatable("gui.flansmod.teams.deaths")).append(" " + player.deaths()))
            .orElse(null);
    }

    private static Component title(PacketTeamsState state, TeamsRoundResult.Outcome outcome, @Nullable Team winner)
    {
        return switch (outcome)
        {
            case VICTORY -> Component.translatable("gui.flansmod.teams.victory");
            case DEFEAT -> Component.translatable("gui.flansmod.teams.defeat");
            case WATCHED -> Component.translatable("gui.flansmod.teams.team_wins", winnerName(state, winner));
            default -> Component.translatable("gui.flansmod.teams.draw");
        };
    }

    /** Who won, naming every team of a winning alliance. */
    private static String winnerName(PacketTeamsState state, @Nullable Team winner)
    {
        String side = state.getSideName(state.getSideId(state.getWinnerId()));
        if (!side.isEmpty())
            return side;
        return winner == null ? state.getWinnerId() : winner.getNameComponent().getString();
    }

    private static int accent(TeamsRoundResult.Outcome outcome, @Nullable Team winner)
    {
        return switch (outcome)
        {
            case VICTORY -> VICTORY_COLOUR;
            case DEFEAT -> DEFEAT_COLOUR;
            case WATCHED -> winner == null ? NEUTRAL_COLOUR : winner.getTeamColour() & 0xFFFFFF;
            default -> NEUTRAL_COLOUR;
        };
    }

    /** A black of the given alpha that fades in as the screen opens. */
    private static int fade(long shown, int alpha)
    {
        return Math.round(alpha * Mth.clamp(shown / (float) BACKDROP_IN_MILLIS, 0F, 1F)) << 24;
    }

    private static float ease(float progress)
    {
        float rest = 1F - progress;
        return 1F - rest * rest * rest;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        showScores();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (super.keyPressed(keyCode, scanCode, modifiers))
            return true;
        showScores();
        return true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
