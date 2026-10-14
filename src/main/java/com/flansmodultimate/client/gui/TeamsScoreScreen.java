package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.client.teams.TeamsRoundResult;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/** Legacy-styled team and free-for-all scoreboard. */
public final class TeamsScoreScreen extends Screen
{
    private static final int RESULT_TOP = 12;
    private static final int RESULT_FLAG_WIDTH = 48;
    private static final int RESULT_FLAG_HEIGHT = 32;
    private static final int MANY_SIDES_MAX_COLUMN_WIDTH = 150;

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

        ResourceLocation flag = winner == null ? null : winner.getVictoryFlagTextureLocation();
        if (flag != null)
            graphics.blit(flag, width / 2 - font.width(text) - RESULT_FLAG_WIDTH - 6, RESULT_TOP - 6, 0F, 0F, RESULT_FLAG_WIDTH, RESULT_FLAG_HEIGHT, RESULT_FLAG_WIDTH, RESULT_FLAG_HEIGHT);
    }

    /** Who won, naming every team of a winning alliance. */
    private static String winnerName(PacketTeamsState state, @Nullable Team winner)
    {
        String side = state.getSideName(state.getSideId(state.getWinnerId()));
        if (!side.isEmpty())
            return side;
        return winner == null ? state.getWinnerId() : winner.getName();
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
        List<Integer> sideIds = state.getSideIds();
        int lines = sideIds.stream().mapToInt(sideId -> rows(state, sideId).size()).max().orElse(0);
        int guiHeight = 78 + 9 * lines;
        int left = width / 2 - 156;
        int top = height / 2 - guiHeight / 2;
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES2, left, top, 100, 0, 312, 66, 512, 256);
        for (int line = 0; line < lines; line++)
            graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES2, left, top + 66 + 9 * line, 100, 71, 312, 9, 512, 256);
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMSSCORES2, left, top + 66 + lines * 9, 100, 168, 312, 12, 512, 256);

        graphics.drawString(font, state.getMapName(), left + 6, top + 6, 0xFFFFFF, false);
        graphics.drawString(font, state.getGameType(), left + 306 - font.width(state.getGameType()), top + 6, 0xFFFFFF, false);
        String time = timeText(state);
        graphics.drawString(font, time, left + 10, top + 20, 0xFFFFFF, false);
        String limit = Component.translatable("gui.flansmod.teams.score_limit", state.getScoreLimit()).getString();
        graphics.drawString(font, limit, left + 302 - font.width(limit), top + 20, 0xFFFFFF, false);

        for (int column = 0; column < 2; column++)
        {
            int sideId = sideIds.get(column);
            PacketTeamsState.TeamScore side = state.getTeamScore(sideId);
            if (side == null)
                continue;
            int x = left + 10 + 151 * column;
            int colour = 0xFF000000 | side.colour();
            graphics.drawString(font, font.plainSubstrByWidth(state.getSideName(sideId), 118), x, top + 39, colour, false);
            graphics.drawString(font, Integer.toString(side.score()), left + 133 + 151 * column, top + 39, colour, false);
            List<Row> rows = rows(state, sideId);
            for (int row = 0; row < rows.size(); row++)
            {
                PacketTeamsState.PlayerScore player = rows.get(row).player();
                int y = top + 67 + 9 * row;
                graphics.drawString(font, font.plainSubstrByWidth(player.name(), 88), left + 12 + 151 * column, y, rows.get(row).colour(), false);
                drawCentered(graphics, Integer.toString(player.score()), left + 111 + 151 * column, y);
                drawCentered(graphics, Integer.toString(state.isShowZombieScore() ? player.zombieScore() : player.kills()), left + 127 + 151 * column, y);
                drawCentered(graphics, Integer.toString(player.deaths()), left + 143 + 151 * column, y);
            }
        }
    }

    /**
     * Three or four sides do not fit the legacy two-column sheet, so they get plain columns of
     * their own: the side and its score on top, then name, score, kills and deaths of each player.
     */
    private void renderManySides(GuiGraphics graphics, PacketTeamsState state)
    {
        List<Integer> sideIds = state.getSideIds();
        int lines = sideIds.stream().mapToInt(sideId -> rows(state, sideId).size()).max().orElse(0);
        int columnWidth = Math.min(MANY_SIDES_MAX_COLUMN_WIDTH, (width - 24) / sideIds.size());
        int guiWidth = columnWidth * sideIds.size() + 8;
        int guiHeight = 50 + 9 * lines;
        int left = width / 2 - guiWidth / 2;
        int top = Math.max(RESULT_TOP + RESULT_FLAG_HEIGHT, height / 2 - guiHeight / 2);
        graphics.fill(left, top, left + guiWidth, top + guiHeight, 0xC0101010);
        graphics.fill(left, top + 30, left + guiWidth, top + 31, 0xFF505050);

        graphics.drawString(font, state.getMapName(), left + 6, top + 5, 0xFFFFFF, false);
        graphics.drawString(font, state.getGameType(), left + guiWidth - 6 - font.width(state.getGameType()), top + 5, 0xFFFFFF, false);
        graphics.drawString(font, timeText(state), left + 6, top + 17, 0xFFFFFF, false);
        String limit = Component.translatable("gui.flansmod.teams.score_limit", state.getScoreLimit()).getString();
        graphics.drawString(font, limit, left + guiWidth - 6 - font.width(limit), top + 17, 0xFFFFFF, false);

        for (int column = 0; column < sideIds.size(); column++)
        {
            int sideId = sideIds.get(column);
            PacketTeamsState.TeamScore side = state.getTeamScore(sideId);
            if (side == null)
                continue;
            int x = left + 4 + columnWidth * column;
            int right = x + columnWidth - 4;
            int colour = 0xFF000000 | side.colour();
            String score = Integer.toString(side.score());
            graphics.drawString(font, font.plainSubstrByWidth(state.getSideName(sideId), columnWidth - 12 - font.width(score)), x + 2, top + 35, colour, false);
            graphics.drawString(font, score, right - font.width(score), top + 35, colour, false);
            List<Row> rows = rows(state, sideId);
            for (int row = 0; row < rows.size(); row++)
            {
                PacketTeamsState.PlayerScore player = rows.get(row).player();
                int y = top + 46 + 9 * row;
                graphics.drawString(font, font.plainSubstrByWidth(player.name(), columnWidth - 60), x + 2, y, rows.get(row).colour(), false);
                drawCentered(graphics, Integer.toString(player.score()), right - 42, y);
                drawCentered(graphics, Integer.toString(player.kills()), right - 24, y);
                drawCentered(graphics, Integer.toString(player.deaths()), right - 8, y);
            }
        }
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
            graphics.drawString(font, player.name(), left + 8, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.score()), left + 100, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.kills()), left + 150, y, 0xFFFFFF, false);
            graphics.drawString(font, Integer.toString(player.deaths()), left + 200, y, 0xFFFFFF, false);
        }
    }

    private String timeText(PacketTeamsState state)
    {
        if (!state.isRoundRunning())
            return Component.translatable("gui.flansmod.teams.round_over").getString();
        int seconds = Math.max(0, state.getTimeLeftTicks() / 20);
        return Component.translatable("gui.flansmod.teams.time_left", seconds / 60, String.format("%02d", seconds % 60)).getString();
    }

    private void drawCentered(GuiGraphics graphics, String value, int x, int y)
    {
        graphics.drawString(font, value, x - font.width(value) / 2, y, 0xFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
