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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.UUID;

/** Compact editor; only Save submits a change, while Escape and Cancel discard it. */
public final class TeamsSpawnerEditScreen extends Screen
{
    private static final int PANEL_WIDTH = 320;
    private static final int MARGIN = 12;
    private static final String OPTION_VALUE = "options.generic_value";
    private final PacketSpawnerEditState state;
    private EditBox secondsField;
    private Button saveButton;
    private boolean waitForVehicle;
    private SpawnerNpcPolicy npcPolicy;
    private boolean npcSpawnAll;
    private UUID baseId;
    private String baseName;
    private boolean updateBase;

    public TeamsSpawnerEditScreen(PacketSpawnerEditState state)
    {
        super(Component.translatable("gui.flansmod.teams.spawner_settings"));
        this.state = state;
        waitForVehicle = state.isWaitForVehicle();
        npcPolicy = state.getNpcPolicy();
        npcSpawnAll = state.isNpcSpawnAll();
        baseId = state.getBaseId();
        baseName = state.getBaseName();
    }

    /** Names the policy the way the spawner applies it, so the current setting is readable without opening the selection. */
    private static Component npcPolicyValue(SpawnerNpcPolicy policy)
    {
        Component name = Component.translatable("gui.flansmod.teams.npc_mode_" + policy.mode().name().toLowerCase(Locale.ROOT));
        return switch (policy.mode())
        {
            case NONE, ALL -> name;
            case CATEGORIES, DEFINITIONS -> Component.translatable("gui.flansmod.teams.npc_mode_count", name, policy.selections().size());
        };
    }

    @Override
    protected void init()
    {
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = (height - panelHeight()) / 2;
        int inner = PANEL_WIDTH - MARGIN * 2;
        Component baseLabel = Component.translatable(OPTION_VALUE, Component.translatable("gui.flansmod.teams.spawner_base"),
            baseId == null ? Component.translatable("gui.flansmod.teams.spawner_no_base") : Component.literal(baseName));
        Button base = addRenderableWidget(Button.builder(baseLabel, ignored -> Minecraft.getInstance().setScreen(new TeamsSpawnerBaseScreen(this, state.getBases(), baseId, choice ->
        {
            baseId = choice == null ? null : choice.id();
            baseName = choice == null ? "" : choice.name() + " (" + choice.map() + ")";
            updateBase = true;
        }))).bounds(left + MARGIN, top + 30, inner, 20).build());
        base.setTooltip(Tooltip.create(baseLabel));
        if (!state.isPlayerSpawner())
        {
            String value = secondsField == null ? Integer.toString(state.getDelayTicks() / 20) : secondsField.getValue();
            secondsField = new EditBox(font, left + MARGIN, top + 70, inner, 20, Component.translatable("gui.flansmod.teams.spawn_delay_seconds"));
            secondsField.setMaxLength(9);
            secondsField.setFilter(text -> text.chars().allMatch(character -> character >= '0' && character <= '9'));
            secondsField.setValue(value);
            addRenderableWidget(secondsField);
        }
        int buttonsY = top + panelHeight() - 32;
        int half = (inner - MARGIN) / 2;
        saveButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored -> save()).bounds(left + MARGIN, buttonsY, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose()).bounds(left + PANEL_WIDTH - MARGIN - half, buttonsY, half, 20).build());
        if (state.isVehicle())
        {
            Button respawn = addRenderableWidget(Button.builder(respawnLabel(), button ->
            {
                waitForVehicle = !waitForVehicle;
                button.setMessage(respawnLabel());
                button.setTooltip(respawnTooltip());
            }).bounds(left + MARGIN, top + 110, inner, 20).build());
            respawn.setTooltip(respawnTooltip());
            Button npc = addRenderableWidget(Button.builder(Component.translatable(OPTION_VALUE, Component.translatable("gui.flansmod.teams.npc_allowed"), npcPolicyValue(npcPolicy)),
                ignored -> Minecraft.getInstance().setScreen(new TeamsSpawnerNpcScreen(this, npcPolicy, policy -> npcPolicy = policy))).bounds(left + MARGIN, top + 134, inner, 20).build());
            npc.setTooltip(Tooltip.create(Component.translatable("gui.flansmod.teams.npc_selection_tooltip")));
            Button deployment = addRenderableWidget(Button.builder(deploymentLabel(), button ->
            {
                npcSpawnAll = !npcSpawnAll;
                button.setMessage(deploymentLabel());
            }).bounds(left + MARGIN, top + 158, inner, 20).build());
            deployment.active = npcPolicy.mode() != SpawnerNpcPolicy.Mode.NONE;
            deployment.setTooltip(Tooltip.create(Component.translatable("gui.flansmod.teams.npc_deployment_tooltip")));
        }
        if (secondsField != null)
        {
            secondsField.setResponder(ignored -> refreshSaveButton());
            setInitialFocus(secondsField);
        }
        refreshSaveButton();
    }

    private int panelHeight()
    {
        if (state.isVehicle())
            return 226;
        return state.isPlayerSpawner() ? 92 : 148;
    }

    private Component deploymentLabel()
    {
        return Component.translatable(OPTION_VALUE, Component.translatable("gui.flansmod.teams.npc_deployment"),
            Component.translatable(npcSpawnAll ? "gui.flansmod.teams.npc_spawn_all" : "gui.flansmod.teams.npc_spawn_one"));
    }

    private int enteredSeconds()
    {
        if (state.isPlayerSpawner())
            return state.getDelayTicks() / 20;
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
        saveButton.active = state.isPlayerSpawner() || seconds >= 1 && seconds <= TeamSpawnerBlockEntity.MAX_SPAWN_DELAY_SECONDS;
    }

    private void save()
    {
        if (!saveButton.active)
            return;
        PacketHandler.sendToServer(new PacketSpawnerEditAction(state.getPos(), state.getObjectId(), enteredSeconds(), waitForVehicle, npcPolicy, npcSpawnAll, updateBase, baseId));
        onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = (height - panelHeight()) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight(), 0xFF252525);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);
        if (!state.isPlayerSpawner())
        {
            graphics.drawString(font, Component.translatable("gui.flansmod.teams.spawn_delay_seconds"), left + MARGIN, top + 58, 0xFFFFFF);
            graphics.drawString(font, Component.translatable("gui.flansmod.teams.spawn_delay_range", TeamSpawnerBlockEntity.MAX_SPAWN_DELAY_SECONDS), left + MARGIN, top + 96,
                saveButton.active ? 0xAAAAAA : 0xFF7777);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private Component respawnLabel()
    {
        return Component.translatable(OPTION_VALUE, Component.translatable("gui.flansmod.teams.respawn_condition"),
            Component.translatable(waitForVehicle ? "gui.flansmod.teams.wait_for_vehicle" : "gui.flansmod.teams.nearby_only"));
    }

    private Tooltip respawnTooltip()
    {
        return Tooltip.create(Component.translatable(waitForVehicle ? "gui.flansmod.teams.wait_for_vehicle_tooltip" : "gui.flansmod.teams.nearby_only_tooltip"));
    }
}
