package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.teams.editor.EditorCatalog;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Separate picker for adding a class without mixing unrelated classes into the team's membership list. */
@SuppressWarnings("DataFlowIssue") // Screen initializes minecraft before init and input callbacks.
public final class EditorClassPickerScreen extends Screen
{
    private final Screen parent;
    private final Set<String> assigned;
    private final Consumer<PlayerClass> selected;
    private String query = "";
    private int page;
    private int left;
    private int panelWidth;
    private int rows;

    public EditorClassPickerScreen(Screen parent, Set<String> assigned, Consumer<PlayerClass> selected)
    {
        super(TeamsEditorScreen.tr("add_class"));
        this.parent = parent;
        this.assigned = Set.copyOf(assigned);
        this.selected = selected;
    }

    @Override
    @SuppressWarnings("DuplicatedCode") // Each picker owns its search widget and query lifecycle.
    protected void init()
    {
        panelWidth = Math.min(560, width - 24);
        left = (width - panelWidth) / 2;
        rows = Math.max(1, (height - 120) / 28);
        EditBox search = new EditBox(font, left, 35, panelWidth, 20, TeamsEditorScreen.tr("search"));
        search.setHint(TeamsEditorScreen.tr("search"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            page = 0;
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1)).bounds(left, height - 42, 25, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
        {
            if ((page + 1) * rows < choices().size())
                page++;
        }).bounds(left + 30, height - 42, 25, 20).build());
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("back"), button -> onClose()).bounds(left + panelWidth - 85, height - 42, 85, 20).build());
    }

    private List<PlayerClass> choices()
    {
        String needle = query.toLowerCase(Locale.ROOT);
        return PlayerClass.values().stream().filter(type -> !assigned.contains(EditorCatalog.key(type)))
            .filter(type -> type.getName().toLowerCase(Locale.ROOT).contains(needle) || type.getShortName().contains(needle))
            .sorted(Comparator.comparing(PlayerClass::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(PlayerClass::getShortName)).toList();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && mouseX >= left && mouseX < left + panelWidth && mouseY >= 65 && mouseY < 65 + rows * 28)
        {
            List<PlayerClass> choices = choices();
            int index = page * rows + ((int) mouseY - 65) / 28;
            if (index < choices.size())
            {
                selected.accept(choices.get(index));
                minecraft.setScreen(parent);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        List<PlayerClass> choices = choices();
        for (int index = page * rows; index < Math.min(choices.size(), (page + 1) * rows); index++)
        {
            PlayerClass type = choices.get(index);
            int y = 65 + index % rows * 28;
            boolean hovered = mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + 26;
            graphics.fill(left, y, left + panelWidth, y + 26, hovered ? 0xFF42516A : 0xFF2B3444);
            graphics.drawString(font, font.plainSubstrByWidth(type.getName(), panelWidth - 10), left + 5, y + 3, 0xFFFFFF);
            graphics.drawString(font, font.plainSubstrByWidth(type.getContentPack().getName(), panelWidth - 10), left + 5, y + 14, 0xAABBDD);
            if (hovered)
                graphics.renderTooltip(font, Component.literal(type.getShortName()), mouseX, mouseY);
        }
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
