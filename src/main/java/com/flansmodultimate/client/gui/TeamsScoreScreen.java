package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.network.client.PacketTeamsState;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/** Legacy-styled team and free-for-all scoreboard. */
public final class TeamsScoreScreen extends Screen
{
    private static final ResourceLocation DM_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_scores.png");
    private static final ResourceLocation TEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_scores_2.png");

    public TeamsScoreScreen()
    {
        super(Component.translatable("gui.flansmod.teams.scores"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        PacketTeamsState state = TeamsClientState.get();
        if (state == null || state.getTeamScores().isEmpty())
        {
            graphics.drawCenteredString(font, Component.translatable("gui.flansmod.teams.no_round"), width / 2, height / 2, 0xFFFFFF);
            return;
        }
        if (state.isSortedByTeam() && state.getTeamScores().size() == 2)
            renderTwoTeams(graphics, state);
        else
            renderFreeForAll(graphics, state);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTwoTeams(GuiGraphics graphics, PacketTeamsState state)
    {
        int lines = state.getTeamScores().stream().mapToInt(team -> team.players().size()).max().orElse(0);
        int guiHeight = 78 + 9 * lines;
        int left = width / 2 - 156;
        int top = height / 2 - guiHeight / 2;
        graphics.blit(TEAM_TEXTURE, left, top, 100, 0, 312, 66, 512, 256);
        for (int line = 0; line < lines; line++)
            graphics.blit(TEAM_TEXTURE, left, top + 66 + 9 * line, 100, 71, 312, 9, 512, 256);
        graphics.blit(TEAM_TEXTURE, left, top + 66 + lines * 9, 100, 168, 312, 12, 512, 256);

        graphics.drawString(font, state.getMapName(), left + 6, top + 6, 0xFFFFFF, false);
        graphics.drawString(font, state.getGameType(), left + 306 - font.width(state.getGameType()), top + 6, 0xFFFFFF, false);
        String time = timeText(state);
        graphics.drawString(font, time, left + 10, top + 20, 0xFFFFFF, false);
        String limit = Component.translatable("gui.flansmod.teams.score_limit", state.getScoreLimit()).getString();
        graphics.drawString(font, limit, left + 302 - font.width(limit), top + 20, 0xFFFFFF, false);

        for (int column = 0; column < 2; column++)
        {
            PacketTeamsState.TeamScore team = state.getTeamScores().get(column);
            int x = left + 10 + 151 * column;
            int colour = 0xFF000000 | team.colour();
            graphics.drawString(font, team.name(), x, top + 39, colour, false);
            graphics.drawString(font, Integer.toString(team.score()), left + 133 + 151 * column, top + 39, colour, false);
            for (int row = 0; row < team.players().size(); row++)
            {
                PacketTeamsState.PlayerScore player = team.players().get(row);
                int y = top + 67 + 9 * row;
                graphics.drawString(font, player.name(), left + 12 + 151 * column, y, 0xFFFFFF, false);
                drawCentered(graphics, Integer.toString(player.score()), left + 111 + 151 * column, y);
                drawCentered(graphics, Integer.toString(state.isShowZombieScore() ? player.zombieScore() : player.kills()), left + 127 + 151 * column, y);
                drawCentered(graphics, Integer.toString(player.deaths()), left + 143 + 151 * column, y);
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
        graphics.blit(DM_TEXTURE, left, top, 0, 45, 256, 24, 256, 256);
        for (int line = 0; line < players.size(); line++)
            graphics.blit(DM_TEXTURE, left, top + 24 + 9 * line, 0, 71, 256, 9, 256, 256);
        graphics.blit(DM_TEXTURE, left, top + 24 + 9 * players.size(), 0, 87, 256, 10, 256, 256);
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
