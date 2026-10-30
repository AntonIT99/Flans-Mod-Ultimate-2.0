package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.api.commandcentre.FlansCommandCentre;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketCommandCentreAction;
import com.flansmodultimate.network.server.teams.PacketTeamsAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * What the Teams menu key opens: one entry each for team selection, class selection and the scoreboard, so the three
 * share a single default key. Operators also get a command centre shortcut. Each entry asks the server for its screen
 * exactly as the dedicated key binds do.
 */
public final class TeamsMenuScreen extends Screen
{
    private static final int PANEL_WIDTH = 256;
    private static final int ROW_HEIGHT = 24;
    /** Header above the rows, drawn from the same texture as the team selection window. */
    private static final int HEADER_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 7;
    private static final List<Entry> ENTRIES = List.of(new Entry("gui.flansmod.teams.choose_team", PacketTeamsAction::openTeamMenu),
        new Entry("gui.flansmod.teams.choose_class", PacketTeamsAction::openClassMenu), new Entry("gui.flansmod.teams.scores", PacketTeamsAction::openScoreboard));
    private boolean showCommandCentre;

    private record Entry(String titleKey, Supplier<PacketTeamsAction> request)
    {}

    public TeamsMenuScreen()
    {
        super(Component.translatable("key.flansmodultimate.teams_menu"));
    }

    @Override
    protected void init()
    {
        showCommandCentre = minecraft.player != null && minecraft.player.hasPermissions(FlansCommandCentre.PERMISSION_LEVEL);
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = panelTop();
        for (int i = 0; i < ENTRIES.size(); i++)
        {
            Entry entry = ENTRIES.get(i);
            addRenderableWidget(Button.builder(Component.translatable(entry.titleKey()), ignored -> open(entry)).bounds(left + 10, top + HEADER_HEIGHT + 2 + ROW_HEIGHT * i, 236, 20).build());
        }
        if (showCommandCentre)
            addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.centre.title"), ignored ->
            {
                onClose();
                PacketHandler.sendToServer(PacketCommandCentreAction.open());
            }).bounds(left + 10, top + HEADER_HEIGHT + 2 + ROW_HEIGHT * ENTRIES.size(), 236, 20).build());
    }

    /** Closes this menu first, so a refusal the server sends to chat is not hidden behind it. */
    private void open(Entry entry)
    {
        onClose();
        PacketHandler.sendToServer(entry.request().get());
    }

    private int panelTop()
    {
        return height / 2 - (HEADER_HEIGHT + ROW_HEIGHT * rowCount() + FOOTER_HEIGHT) / 2;
    }

    private int rowCount()
    {
        return ENTRIES.size() + (showCommandCentre ? 1 : 0);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = panelTop();
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top, 0, 0, PANEL_WIDTH, HEADER_HEIGHT, 256, 256);
        for (int row = 0; row < rowCount(); row++)
            graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top + HEADER_HEIGHT + ROW_HEIGHT * row, 0, 48, PANEL_WIDTH, ROW_HEIGHT, 256, 256);
        graphics.blit(FlansModTextures.TEXTURE_GUI_TEAMS, left, top + HEADER_HEIGHT + ROW_HEIGHT * rowCount(), 0, 73, PANEL_WIDTH, FOOTER_HEIGHT, 256, 256);
        graphics.drawString(font, title, left + 8, top + 7, 0xFFFFFF, true);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The key that opened this menu closes it again. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (KeyInputHandler.isTeamsMenuKey(keyCode, scanCode))
        {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
