package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketBaseEditState;
import com.flansmodultimate.network.client.teams.PacketCapturePointEditState;
import com.flansmodultimate.network.server.teams.PacketCapturePointEditAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Operator settings of a capture point: its name, letter, capture radius, starting owner and map. */
public final class TeamsCapturePointEditScreen extends Screen
{
    private static final int MAPS_PER_PAGE = 3;
    /** Round team ids a capture point can start with: nobody, or either team. */
    private static final int[] OWNER_IDS = {0, 2, 3};
    private static final String[] OWNER_KEYS = {"gui.flansmod.teams.capture_point.owner_none", "gui.flansmod.teams.capture_point.owner_first", "gui.flansmod.teams.capture_point.owner_second"};

    private final PacketCapturePointEditState state;
    private final List<Button> symbolButtons = new ArrayList<>();
    private final List<Button> ownerButtons = new ArrayList<>();
    private final List<Button> mapButtons = new ArrayList<>();
    private EditBox nameField;
    private EditBox radiusField;
    private Button previousPage;
    private Button nextPage;
    private char selectedSymbol;
    private int selectedOwner;
    private String selectedMap;
    private int page;

    public TeamsCapturePointEditScreen(PacketCapturePointEditState state)
    {
        super(Component.translatable("gui.flansmod.teams.capture_point.title"));
        this.state = state;
        selectedSymbol = state.getSymbol();
        selectedOwner = state.getOwnerId();
        selectedMap = state.getSelectedMap();
    }

    @Override
    protected void init()
    {
        symbolButtons.clear();
        ownerButtons.clear();
        mapButtons.clear();
        int left = width / 2 - 128;
        int top = height / 2 - 94;
        nameField = new EditBox(font, left + 70, top + 21, 179, 16, Component.translatable("gui.flansmod.teams.base_name"));
        nameField.setMaxLength(CapturePointBlockEntity.MAX_NAME_LENGTH);
        nameField.setValue(state.getPointName());
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        for (char symbol = CapturePointBlockEntity.FIRST_SYMBOL; symbol <= CapturePointBlockEntity.LAST_SYMBOL; symbol++)
        {
            char choice = symbol;
            Button button = Button.builder(Component.literal(String.valueOf(symbol)), ignored ->
            {
                selectedSymbol = choice;
                refreshButtons();
            }).bounds(left + 6 + 50 * (symbol - CapturePointBlockEntity.FIRST_SYMBOL), top + 40, 46, 20).build();
            symbolButtons.add(button);
            addRenderableWidget(button);
        }

        radiusField = new EditBox(font, left + 50, top + 65, 36, 16, Component.translatable("gui.flansmod.teams.capture_point.radius"));
        radiusField.setMaxLength(3);
        radiusField.setFilter(value -> value.chars().allMatch(Character::isDigit));
        radiusField.setValue(Integer.toString(state.getCaptureRadius()));
        addRenderableWidget(radiusField);

        for (int i = 0; i < OWNER_IDS.length; i++)
        {
            int owner = OWNER_IDS[i];
            Button button = Button.builder(Component.translatable(OWNER_KEYS[i]), ignored ->
            {
                selectedOwner = owner;
                refreshButtons();
            }).bounds(left + 92 + 53 * i, top + 63, 50, 20).build();
            ownerButtons.add(button);
            addRenderableWidget(button);
        }

        for (int i = 0; i < MAPS_PER_PAGE; i++)
        {
            int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> selectMap(slot)).bounds(left + 28, top + 99 + 22 * i, 200, 20).build();
            mapButtons.add(button);
            addRenderableWidget(button);
        }
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), ignored ->
        {
            page--;
            refreshButtons();
        }).bounds(left + 6, top + 121, 20, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), ignored ->
        {
            page++;
            refreshButtons();
        }).bounds(left + 230, top + 121, 20, 20).build());
        refreshButtons();
    }

    private void selectMap(int slot)
    {
        int index = page * MAPS_PER_PAGE + slot;
        if (index < state.getMaps().size())
            selectedMap = state.getMaps().get(index).id();
        refreshButtons();
    }

    private void refreshButtons()
    {
        for (int i = 0; i < symbolButtons.size(); i++)
            symbolButtons.get(i).active = selectedSymbol != CapturePointBlockEntity.FIRST_SYMBOL + i;
        for (int i = 0; i < ownerButtons.size(); i++)
            ownerButtons.get(i).active = selectedOwner != OWNER_IDS[i];
        for (int i = 0; i < mapButtons.size(); i++)
        {
            int index = page * MAPS_PER_PAGE + i;
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
        nextPage.visible = (page + 1) * MAPS_PER_PAGE < state.getMaps().size();
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
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.capture_point.radius"), left + 6, top + 69, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.map"), left + 6, top + 88, 0xFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose()
    {
        int radius;
        try
        {
            radius = Integer.parseInt(radiusField.getValue());
        }
        catch (NumberFormatException ignored)
        {
            radius = state.getCaptureRadius();
        }
        PacketHandler.sendToServer(new PacketCapturePointEditAction(state.getPointId(), nameField.getValue(), selectedSymbol, radius, selectedOwner, selectedMap));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
