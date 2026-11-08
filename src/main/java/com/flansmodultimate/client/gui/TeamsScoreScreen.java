package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.teams.*;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/** Legacy-styled team and free-for-all scoreboard. */
public final class TeamsScoreScreen extends Screen
{
    private static final int RESULT_TOP = 12;
    private static final int RESULT_FLAG_WIDTH = 48;
    private static final int RESULT_FLAG_HEIGHT = 32;

    /** A line of a side's column: a player and the colour their name is drawn in. */
    private record Row(PacketTeamsState.PlayerScore player, int colour)
    {}

    public TeamsScoreScreen()
    {
        super(Component.translatable("gui.flansmod.teams.scores"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        PacketTeamsState state = TeamsClientState.get();
        if (state == null || state.getTeamScores().isEmpty())
        {
            graphics.drawCenteredString(font, Component.translatable("gui.flansmod.teams.no_round"), width / 2, height / 2, 0xFFFFFF);
            return;
        }
        int sides = state.getSideIds().size();
        if (state.isSortedByTeam() && sides == 2)
            renderTwoSides(graphics, state);
        else if (state.isSortedByTeam() && sides > 2)
            renderManySides(graphics, state);
        else
            renderFreeForAll(graphics, state);
        renderResult(graphics, state);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Once the round is over, says at the top of the screen how it went for this player, in the
     * colour of the winning team and beside its flag when the team has one.
     */
    private void renderResult(GuiGraphics graphics, PacketTeamsState state)
    {
        TeamsRoundResult.Outcome outcome = TeamsRoundResult.outcome(state);
        if (outcome == TeamsRoundResult.Outcome.NONE)
            return;
        Team winner = TeamsRoundResult.winner(state);
        Component text = switch (outcome)
        {
            case VICTORY -> Component.translatable("gui.flansmod.teams.victory");
            case DEFEAT -> Component.translatable("gui.flansmod.teams.defeat");
            case WATCHED -> Component.translatable("gui.flansmod.teams.team_wins", winnerName(state, winner));
            default -> Component.translatable("gui.flansmod.teams.draw");
        };
        int colour = winner == null ? 0xFFFFFF : winner.getTeamColour();
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, RESULT_TOP, 0F);
        graphics.pose().scale(2F, 2F, 1F);
        graphics.drawCenteredString(font, text, 0, 0, 0xFF000000 | colour);
        graphics.pose().popPose();

        TeamFlagTextures.FlagTexture flag = TeamFlagTextures.get(winner);
        if (flag != null)
            TeamFlagTextures.draw(graphics, flag, width / 2 - font.width(text) - RESULT_FLAG_WIDTH - 6, RESULT_TOP - 6, RESULT_FLAG_WIDTH, RESULT_FLAG_HEIGHT);
    }

    /** Who won, naming every team of a winning alliance. */
    private static String winnerName(PacketTeamsState state, @Nullable Team winner)
    {
        String side = state.getSideName(state.getSideId(state.getWinnerId()));
        if (!side.isEmpty())
            return side;
        return winner == null ? state.getWinnerId() : winner.getNameComponent().getString();
    }

    /** The players of a side, best first. Those of an alliance are drawn in the colour of their own team to tell them apart. */
    private static List<Row> rows(PacketTeamsState state, int sideId)
    {
        List<PacketTeamsState.TeamScore> teams = state.getTeamsOnSide(sideId);
        return teams.stream().flatMap(team -> team.players().stream().map(player -> new Row(player, teams.size() > 1 ? 0xFF000000 | team.colour() : 0xFFFFFF)))
            .sorted(Comparator.comparingInt((Row row) -> row.player().score()).reversed()).toList();
    }

    private void renderTwoSides(GuiGraphics graphics, PacketTeamsState state)
    {
        renderManySides(graphics, state);
    }

    /** Shares measured statistic columns across teams so even large scores stay aligned. */
    private void renderManySides(GuiGraphics graphics, PacketTeamsState state)
    {
        List<Integer> sideIds = state.getSideIds();
        List<List<Row>> sideRows = sideIds.stream().map(sideId -> rows(state, sideId)).toList();
        String[] headings = { Component.translatable("gui.flansmod.teams.score").getString(), Component.translatable("gui.flansmod.teams.kills").getString(),
            Component.translatable("gui.flansmod.teams.deaths").getString() };
        int[] statisticWidths = new int[3];
        for (int statistic = 0; statistic < statisticWidths.length; statistic++)
            statisticWidths[statistic] = font.width(headings[statistic]);
        int nameWidth = 90;
        int teamWidth = 0;
        for (int column = 0; column < sideIds.size(); column++)
        {
            PacketTeamsState.TeamScore side = state.getTeamScore(sideIds.get(column));
            if (side != null)
                teamWidth = Math.max(teamWidth, font.width(state.getSideName(sideIds.get(column))) + font.width(Integer.toString(side.score())) + 24);
            for (Row row : sideRows.get(column))
            {
                nameWidth = Math.max(nameWidth, Math.min(150, font.width(row.player().displayName())));
                int[] values = statistics(state, row.player());
                for (int statistic = 0; statistic < statisticWidths.length; statistic++)
                    statisticWidths[statistic] = Math.max(statisticWidths[statistic], font.width(Integer.toString(values[statistic])));
            }
        }
        int columnWidth = Math.max(teamWidth, nameWidth + statisticWidths[0] + statisticWidths[1] + statisticWidths[2] + 44);
        int guiWidth = columnWidth * sideIds.size() + 8;
        int lines = sideRows.stream().mapToInt(List::size).max().orElse(0);
        int guiHeight = 76 + 13 * lines;
        float scale = Math.min(1F, Math.min((width - 16F) / guiWidth, (height - 64F) / guiHeight));
        int left = -guiWidth / 2;
        int top = 0;
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, Math.max(48F, (height - guiHeight * scale) / 2F), 0F);
        graphics.pose().scale(scale, scale, 1F);
        graphics.fill(left, top, left + guiWidth, guiHeight, 0xEE15191F);
        graphics.drawString(font, font.plainSubstrByWidth(state.getMapName(), guiWidth / 2 - 12), left + 8, 7, 0xFFFFFF, false);
        graphics.drawString(font, state.getGameType(), left + guiWidth - 8 - font.width(state.getGameType()), 7, 0xFFFFFF, false);
        graphics.drawString(font, timeText(state), left + 8, 22, 0xDDDDDD, false);
        String limit = Component.translatable("gui.flansmod.teams.score_limit", state.getScoreLimit()).getString();
        graphics.drawString(font, limit, left + guiWidth - 8 - font.width(limit), 22, 0xDDDDDD, false);
        for (int column = 0; column < sideIds.size(); column++)
        {
            int sideId = sideIds.get(column);
            PacketTeamsState.TeamScore side = state.getTeamScore(sideId);
            if (side == null)
                continue;
            int x = left + 4 + columnWidth * column;
            int right = x + columnWidth - 8;
            int colour = 0xFF000000 | side.colour();
            graphics.fill(x, 37, x + columnWidth - 4, 39, colour);
            String score = Integer.toString(side.score());
            graphics.drawString(font, font.plainSubstrByWidth(state.getSideName(sideId), columnWidth - 24 - font.width(score)), x + 4, 44, colour, true);
            graphics.drawString(font, score, right - font.width(score), 44, 0xFFFFFF, false);
            int[] edges = { right - statisticWidths[2] - statisticWidths[1] - 16, right - statisticWidths[2] - 8, right };
            graphics.drawString(font, Component.translatable("gui.flansmod.teams.name"), x + 4, 60, 0xBBBBBB, false);
            for (int statistic = 0; statistic < headings.length; statistic++)
                graphics.drawString(font, headings[statistic], edges[statistic] - font.width(headings[statistic]), 60, 0xBBBBBB, false);
            List<Row> players = sideRows.get(column);
            for (int row = 0; row < players.size(); row++)
            {
                int y = 73 + 13 * row;
                graphics.fill(x, y - 2, x + columnWidth - 4, y + 10, row % 2 == 0 ? 0xFF2A3038 : 0xFF20262D);
                graphics.drawString(font, font.plainSubstrByWidth(players.get(row).player().displayName().getString(), edges[0] - statisticWidths[0] - x - 12), x + 4, y,
                    players.get(row).colour(), false);
                int[] values = statistics(state, players.get(row).player());
                for (int statistic = 0; statistic < values.length; statistic++)
                {
                    String value = Integer.toString(values[statistic]);
                    graphics.drawString(font, value, edges[statistic] - font.width(value), y, 0xFFFFFF, false);
                }
            }
        }
        graphics.pose().popPose();
    }

    private static int[] statistics(PacketTeamsState state, PacketTeamsState.PlayerScore player)
    {
        return new int[] { player.score(), state.isShowZombieScore() ? player.zombieScore() : player.kills(), player.deaths() };
    }

    private void renderFreeForAll(GuiGraphics graphics, PacketTeamsState state)
    {
        List<PacketTeamsState.PlayerScore> players = state.getTeamScores().stream().flatMap(team -> team.players().stream())
            .sorted(Comparator.comparingInt(PacketTeamsState.PlayerScore::score).reversed()).toList();
        int guiHeight = 34 + 9 * players.size();
        int left = width / 2 - 128;
        int top = height / 2 - guiHeight / 2;
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES, left, top, 0, 45, 256, 24, 256, 256);
        for (int line = 0; line < players.size(); line++)
            graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES, left, top + 24 + 9 * line, 0, 71, 256, 9, 256, 256);
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES, left, top + 24 + 9 * players.size(), 0, 87, 256, 10, 256, 256);
        graphics.drawCenteredString(font, state.getGameType() + " — " + timeText(state), width / 2, top + 4, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.name"), left + 8, top + 14, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.score"), left + 100, top + 14, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.kills"), left + 150, top + 14, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.deaths"), left + 200, top + 14, 0xFFFFFF, false);
        for (int row = 0; row < players.size(); row++)
        {
            PacketTeamsState.PlayerScore player = players.get(row);
            int y = top + 25 + 9 * row;
            graphics.drawString(font, player.displayName().getString(), left + 8, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.score()), left + 100, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.kills()), left + 150, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.deaths()), left + 200, y, 0xFFFFFF, false);
        }
    }

    private String timeText(PacketTeamsState state)
    {
        if (!state.isRoundRunning())
            return Component.translatable("gui.flansmod.teams.round_over").getString();
        if (state.isStaging() || state.isOvertime())
            return TeamsClientState.clock(state);
        int seconds = Math.max(0, state.getTimeLeftTicks() / 20);
        return Component.translatable("gui.flansmod.teams.time_left", seconds / 60, String.format("%02d", seconds % 60)).getString();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
