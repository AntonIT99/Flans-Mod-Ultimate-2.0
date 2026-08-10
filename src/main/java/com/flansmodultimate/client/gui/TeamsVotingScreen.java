package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.network.server.PacketTeamsAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Automatic intermission voting screen modelled after the 1.7.10 GUI. */
public final class TeamsVotingScreen extends Screen
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/vote.png");
    private int panelHeight;

    public TeamsVotingScreen()
    {
        super(Component.translatable("gui.flansmod.teams.vote_title"));
    }

    @Override
    protected void init()
    {
        PacketTeamsState state = TeamsClientState.get();
        int count = state == null ? 0 : state.getVoteOptions().size();
        panelHeight = 29 + count * 24;
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;
        for (int i = 0; i < count; i++)
        {
            int option = i + 1;
            addRenderableWidget(Button.builder(Component.translatable("gui.flansmod.teams.vote"), ignored ->
                PacketHandler.sendToServer(PacketTeamsAction.castVote(option)))
                .bounds(left + 198, top + 24 + 24 * i, 48, 20).build());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
            return;
        int left = width / 2 - 128;
        int top = height / 2 - panelHeight / 2;
        graphics.blit(TEXTURE, left, top, 0, 0, 256, 22, 256, 256);
        for (int row = 0; row < state.getVoteOptions().size(); row++)
            graphics.blit(TEXTURE, left, top + 22 + 24 * row, 0, 23, 256, 24, 256, 256);
        graphics.blit(TEXTURE, left, top + 22 + 24 * state.getVoteOptions().size(), 0, 73, 256, 7, 256, 256);
        graphics.drawString(font, title, left + 8, top + 7, 0xFFFFFF, true);
        graphics.drawString(font, Integer.toString(Math.max(0, state.getIntermissionTicks() / 20)), left + 232, top + 7, 0xFFFFFF, true);
        for (int row = 0; row < state.getVoteOptions().size(); row++)
        {
            PacketTeamsState.VoteOption option = state.getVoteOptions().get(row);
            int y = top + 25 + 24 * row;
            graphics.drawString(font, option.mapName(), left + 10, y, 0xFFFFFF, false);
            graphics.drawString(font, option.gameType() + (option.teams().isBlank() ? "" : " — " + option.teams()), left + 10, y + 10, 0xD0D0D0, false);
            int colour = state.getPlayerVote() == row + 1 ? 0x55FF55 : 0xFFFFFF;
            graphics.drawCenteredString(font, Integer.toString(option.votes()), left + 188, y + 5, colour);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
