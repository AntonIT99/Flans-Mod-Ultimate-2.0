package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketBaseEditState;
import com.flansmodultimate.network.server.PacketBaseEditAction;
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
    private final List<Button> ownerButtons = new ArrayList<>();
    private final List<Button> mapButtons = new ArrayList<>();
    private EditBox nameField;
    private Button previousPage;
    private Button nextPage;
    private int selectedOwner;
    private String selectedMap;
    private int page;

    public TeamsBaseEditScreen(PacketBaseEditState state)
    {
        super(Component.translatable("gui.flansmod.teams.base_settings"));
        this.state = state;
        selectedOwner = state.getOwnerId();
        selectedMap = state.getSelectedMap();
    }

    @Override
    protected void init()
    {
        int left = width / 2 - 128;
        int top = height / 2 - 94;
        nameField = new EditBox(font, left + 70, top + 21, 179, 16, Component.translatable("gui.flansmod.teams.base_name"));
        nameField.setMaxLength(60);
        nameField.setValue(state.getBaseName());
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        String[] owners = { "No Team", "Spectator", "Team 1", "Team 2" };
        for (int i = 0; i < owners.length; i++)
        {
            int owner = i;
            Button button = Button.builder(Component.literal(owners[i]), ignored -> selectedOwner = owner)
                .bounds(left + 6 + 62 * i, top + 38, 58, 20).build();
            ownerButtons.add(button);
            addRenderableWidget(button);
        }

        for (int i = 0; i < 5; i++)
        {
            int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> selectMap(slot))
                .bounds(left + 28, top + 75 + 22 * i, 200, 20).build();
            mapButtons.add(button);
            addRenderableWidget(button);
        }
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> { page--; refreshButtons(); })
            .bounds(left + 6, top + 119, 20, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> { page++; refreshButtons(); })
            .bounds(left + 230, top + 119, 20, 20).build());
        refreshButtons();
    }

    private void selectMap(int slot)
    {
        int index = page * 5 + slot;
        if (index < state.getMaps().size())
            selectedMap = state.getMaps().get(index).id();
        refreshButtons();
    }

    private void refreshButtons()
    {
        for (int i = 0; i < ownerButtons.size(); i++)
            ownerButtons.get(i).active = selectedOwner != i;
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
        renderBackground(graphics);
        int left = width / 2 - 128;
        int top = height / 2 - 94;
        graphics.blit(FlansMod.TEXTURE_GUI_BASEEDIT, left, top, 0, 0, 256, 189, 256, 256);
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
