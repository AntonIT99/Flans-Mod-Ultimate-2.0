package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketBaseEditState;
import com.flansmodultimate.network.server.teams.PacketBaseEditAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Operator base settings GUI adapted from the legacy 256x189 screen. */
public final class TeamsBaseEditScreen extends Screen
{
    private final PacketBaseEditState state;
    private final List<Button> mapButtons = new ArrayList<>();
    private EditBox nameField;
    private Button noTeamButton;
    private Button spectatorButton;
    private Button teamButton;
    private Button previousTeam;
    private Button nextTeam;
    private Button previousPage;
    private Button nextPage;
    private int selectedOwner;
    /** The team slot the team button stands for, from 1; kept while the base belongs to nobody. */
    private int teamNumber;
    private String selectedMap;
    private int page;

    public TeamsBaseEditScreen(PacketBaseEditState state)
    {
        super(Component.translatable("gui.flansmod.teams.base_settings"));
        this.state = state;
        selectedOwner = state.getOwnerId();
        teamNumber = Math.max(1, Math.min(TeamsRound.MAX_TEAMS, selectedOwner - TeamsRound.FIRST_TEAM_ID + 1));
        selectedMap = state.getSelectedMap();
    }

    @Override
    protected void init()
    {
        mapButtons.clear();
        int left = width / 2 - 128;
        int top = height / 2 - 94;
        nameField = new EditBox(font, left + 70, top + 21, 179, 16, Component.translatable("gui.flansmod.teams.base_name"));
        nameField.setMaxLength(60);
        nameField.setValue(state.getBaseName());
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        // Nobody, the spectators, then one team slot of a round, stepped through with the arrows beside it.
        noTeamButton = addRenderableWidget(Button.builder(Component.literal("No Team"), ignored -> selectOwner(0)).bounds(left + 6, top + 38, 58, 20).build());
        spectatorButton = addRenderableWidget(Button.builder(Component.literal("Spectator"), ignored -> selectOwner(1)).bounds(left + 68, top + 38, 58, 20).build());
        previousTeam = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> selectTeam(teamNumber - 1)).bounds(left + 130, top + 38, 20, 20).build());
        teamButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> selectTeam(teamNumber)).bounds(left + 152, top + 38, 76, 20).build());
        nextTeam = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> selectTeam(teamNumber + 1)).bounds(left + 230, top + 38, 20, 20).build());

        for (int i = 0; i < 5; i++)
        {
            int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> selectMap(slot)).bounds(left + 28, top + 75 + 22 * i, 200, 20).build();
            mapButtons.add(button);
            addRenderableWidget(button);
        }
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), ignored ->
        {
            page--;
            refreshButtons();
        }).bounds(left + 6, top + 119, 20, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), ignored ->
        {
            page++;
            refreshButtons();
        }).bounds(left + 230, top + 119, 20, 20).build());
        refreshButtons();
    }

    private void selectMap(int slot)
    {
        int index = page * 5 + slot;
        if (index < state.getMaps().size())
            selectedMap = state.getMaps().get(index).id();
        refreshButtons();
    }

    private void selectOwner(int owner)
    {
        selectedOwner = owner;
        refreshButtons();
    }

    /** Gives the base to a team slot of a round: its first team, its second, and so on. */
    private void selectTeam(int number)
    {
        teamNumber = Math.max(1, Math.min(TeamsRound.MAX_TEAMS, number));
        selectOwner(teamNumber + TeamsRound.FIRST_TEAM_ID - 1);
    }

    private void refreshButtons()
    {
        noTeamButton.active = selectedOwner != 0;
        spectatorButton.active = selectedOwner != 1;
        teamButton.active = selectedOwner < TeamsRound.FIRST_TEAM_ID;
        teamButton.setMessage(Component.literal("Team " + teamNumber));
        previousTeam.active = teamNumber > 1;
        nextTeam.active = teamNumber < TeamsRound.MAX_TEAMS;
        for (int i = 0; i < mapButtons.size(); i++)
        {
            int index = page * 5 + i;
            Button button = mapButtons.get(i);
            button.visible = index < state.getMaps().size();
            if (button.visible)
            {
                PacketBaseEditState.MapChoice map = state.getMaps().get(index);
                button.setMessage(Component.literal(map.name()));
                button.active = !map.id().equals(selectedMap);
            }
        }
        previousPage.visible = page > 0;
        nextPage.visible = (page + 1) * 5 < state.getMaps().size();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 128;
        int top = height / 2 - 94;
        graphics.blit(FlansModTextures.TEXTURE_GUI_BASEEDIT, left, top, 0, 0, 256, 189, 256, 256);
        graphics.drawString(font, title, left + 6, top + 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.base_name"), left + 6, top + 24, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.map"), left + 6, top + 64, 0xFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose()
    {
        PacketHandler.sendToServer(new PacketBaseEditAction(state.getBaseId(), nameField.getValue(), selectedMap, selectedOwner));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
