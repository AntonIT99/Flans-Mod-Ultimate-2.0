package com.flansmodultimate.client.gui;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketSpawnerEditState;
import com.flansmodultimate.network.server.teams.PacketSpawnerEditAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Compact editor; only Save submits a change, while Escape and Cancel discard it. */
public final class TeamsSpawnerEditScreen extends Screen
{
    private final PacketSpawnerEditState state;
    private EditBox secondsField;
    private Button saveButton;
    private boolean waitForVehicle;
    private SpawnerNpcPolicy npcPolicy;

    public TeamsSpawnerEditScreen(PacketSpawnerEditState state)
    {
        super(Component.translatable("gui.flansmod.teams.spawner_settings"));
        this.state = state;
        waitForVehicle = state.isWaitForVehicle();
        npcPolicy = state.getNpcPolicy();
    }

    @Override
    protected void init()
    {
        int left = width / 2 - 120;
        int top = height / 2 - (state.isVehicle() ? 95 : 65);
        String value = secondsField == null ? Integer.toString(state.getDelayTicks() / 20) : secondsField.getValue();
        secondsField = new EditBox(font, left + 12, top + 48, 216, 20, Component.translatable("gui.flansmod.teams.spawn_delay_seconds"));
        secondsField.setMaxLength(9);
        secondsField.setFilter(text -> text.chars().allMatch(character -> character >= '0' && character <= '9'));
        secondsField.setValue(value);
        addRenderableWidget(secondsField);
        int buttonsY = top + (state.isVehicle() ? 158 : 98);
        saveButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored -> save()).bounds(left + 12, buttonsY, 104, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose()).bounds(left + 124, buttonsY, 104, 20).build());
        if (state.isVehicle())
        {
            addRenderableWidget(Button.builder(replacementLabel(), button ->
            {
                waitForVehicle = !waitForVehicle;
                button.setMessage(replacementLabel());
            }).bounds(left + 12, top + 98, 216, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.flansmod.teams.npc_selection"),
                ignored -> Minecraft.getInstance().setScreen(new TeamsSpawnerNpcScreen(this, npcPolicy, policy -> npcPolicy = policy))).bounds(left + 12, top + 122, 216, 20).build());
        }
        secondsField.setResponder(ignored -> refreshSaveButton());
        refreshSaveButton();
        setInitialFocus(secondsField);
    }

    private int enteredSeconds()
    {
        try
        {
            return Integer.parseInt(secondsField.getValue());
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    private void refreshSaveButton()
    {
        int seconds = enteredSeconds();
        saveButton.active = seconds >= 1 && seconds <= TeamSpawnerBlockEntity.MAX_SPAWN_DELAY_SECONDS;
    }

    private void save()
    {
        if (!saveButton.active)
            return;
        PacketHandler.sendToServer(new PacketSpawnerEditAction(state.getPos(), state.getObjectId(), enteredSeconds(), waitForVehicle, npcPolicy));
        onClose();
    }

    @Override
    public void tick()
    {
        secondsField.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 120;
        int top = height / 2 - (state.isVehicle() ? 95 : 65);
        graphics.fill(left, top, left + 240, top + (state.isVehicle() ? 190 : 130), 0xFF252525);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.spawn_delay_seconds"), left + 12, top + 32, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.spawn_delay_range", TeamSpawnerBlockEntity.MAX_SPAWN_DELAY_SECONDS), left + 12, top + 77,
            saveButton.active ? 0xAAAAAA : 0xFF7777);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private Component replacementLabel()
    {
        return Component.translatable(waitForVehicle ? "gui.flansmod.teams.wait_for_vehicle" : "gui.flansmod.teams.nearby_only");
    }
}
