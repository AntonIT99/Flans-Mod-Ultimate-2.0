package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

/** Edits each seat role for a single crew scope; Cancel discards this scope's draft. Client side. */
final class BotCrewSettingsScreen extends Screen
{
    private static final String PREFIX = "wolffsmodnpcs.picker.";
    private final BotEditorScreen editor;
    private final Screen parent;
    private final String team;
    private final BotSettings.TeamForce force;
    private final BotSettings.TeamForce draft = new BotSettings.TeamForce();
    private final String scope;
    private final boolean crewsOn;

    BotCrewSettingsScreen(BotEditorScreen editor, Screen parent, String team, BotSettings.TeamForce force, String scope, Component name, boolean crewsOn)
    {
        super(name);
        this.editor = editor;
        this.parent = parent;
        this.team = team;
        this.force = force;
        this.scope = scope;
        this.crewsOn = crewsOn;
        draft.copyCrewFrom(force);
    }

    @Override
    protected void init()
    {
        int panelWidth = Math.min(340, width - 20);
        int left = (width - panelWidth) / 2;
        int top = Math.max(52, (height - 100) / 2);
        for (CrewPost post : CrewPost.values())
        {
            String value = draft.crewClass(scope, post);
            Component name;
            if (value == null)
                name = Component.translatable(PREFIX + "crew.inherit");
            else if (value.isEmpty() || BotSettings.TeamForce.NO_CREW.equalsIgnoreCase(value))
                name = Component.translatable(PREFIX + (value.isEmpty() ? "crew.any" : "crew.none"));
            else
                name = ComponentUtils.formatList(BotSettingsText.crewClasses(value), playerClass ->
                    FlansModApi.getPlayerClass(playerClass).map(IPlayerClass::getNameComponent).orElse(Component.literal(playerClass)));
            Component label = Component.translatable("wolffsmodnpcs.editor.crew_button." + post.id(), name);
            addRenderableWidget(Button.builder(label, button -> Minecraft.getInstance().setScreen(new BotCrewPicker(editor, this, team, draft, scope, post, crewsOn)))
                .bounds(left, top + post.ordinal() * 24, panelWidth, 20).tooltip(Tooltip.create(Component.translatable(PREFIX + "crew.hint." + post.id()))).build());
        }
        int half = (panelWidth - 4) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button ->
        {
            force.copyCrewFrom(draft);
            onClose();
        }).bounds(left, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(left + half + 4, height - 28, half, 20).build());
    }

    @Override
    public void onClose()
    {
        if (minecraft != null)
            minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawWordWrap(font, Component.translatable(PREFIX + (crewsOn ? "crew.scopes.hint" : "crew.off")), 10, 28, width - 20, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
