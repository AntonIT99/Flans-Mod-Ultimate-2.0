package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TeamsMissionResultsScreen extends Screen
{
    public TeamsMissionResultsScreen()
    {
        super(Component.literal("Mission Results"));
    }

    @Override
    protected void init()
    {
        addRenderableWidget(Button.builder(Component.literal("Continue"), ignored -> PacketHandler.sendToServer(PacketLoadoutAction.openHub()))
            .bounds(width / 2 - 40, height / 2 + 72, 80, 20)
            .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics); int left = width / 2 - 128;

        int top = height / 2 - 100;
        graphics.blit(FlansMod.TEXTURE_GUI_TEAMSMISSIONRESULTS, left, top, 0, 0, 256, 200, 512, 256);
        PacketLoadoutState state = LoadoutClientState.get();
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);

        if (state != null)
        {
            graphics.drawCenteredString(font, "Rank " + state.getRank(), width / 2, top + 74, 0xFFFFFF);
            graphics.drawCenteredString(font, state.getExperience() + " / " + state.getExperienceForNextRank() + " XP", width / 2, top + 92, 0xFFFFFF);
            TeamsRankIcon.draw(graphics, state.getRank(), 0, left + 8, top + 123, true);
            if (state.getExperienceForNextRank() != Integer.MAX_VALUE)
                TeamsRankIcon.draw(graphics, state.getRank() + 1, 0, left + 216, top + 123, true);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
