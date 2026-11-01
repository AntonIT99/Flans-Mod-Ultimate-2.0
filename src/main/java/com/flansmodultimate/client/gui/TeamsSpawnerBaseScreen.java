package com.flansmodultimate.client.gui;

import com.flansmodultimate.network.client.teams.PacketSpawnerEditState.BaseChoice;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Searchable base assignment; the parent editor submits the selection only when saved. */
public final class TeamsSpawnerBaseScreen extends Screen
{
    private static final int PANEL_WIDTH = 320;
    private static final int PAGE_SIZE = 5;
    private final Screen parent;
    private final List<BaseChoice> bases;
    @Nullable
    private final UUID selected;
    private final Consumer<BaseChoice> apply;
    private final List<Button> rows = new ArrayList<>();
    private EditBox search;
    private Button previous;
    private Button next;
    private int page;

    public TeamsSpawnerBaseScreen(Screen parent, List<BaseChoice> bases, @Nullable UUID selected, Consumer<BaseChoice> apply)
    {
        super(Component.translatable("gui.flansmod.teams.spawner_base"));
        this.parent = parent;
        this.bases = List.copyOf(bases);
        this.selected = selected;
        this.apply = apply;
    }

    @Override
    protected void init()
    {
        int left = (width - PANEL_WIDTH) / 2;
        int top = height / 2 - 110;
        rows.clear();
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left, top + 26, PANEL_WIDTH, 18, title);
        search.setMaxLength(128);
        search.setHint(Component.translatable("gui.flansmod.teams.spawner_search"));
        search.setValue(query);
        search.setResponder(ignored ->
        {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.flansmod.teams.spawner_no_base"), ignored -> choose(null)).bounds(left, top + 50, PANEL_WIDTH, 20).build());
        for (int index = 0; index < PAGE_SIZE; index++)
        {
            int slot = index;
            rows.add(addRenderableWidget(Button.builder(Component.empty(), ignored -> choose(entries().get(page * PAGE_SIZE + slot))).bounds(left, top + 74 + index * 22, PANEL_WIDTH, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored ->
        {
            page--;
            refresh();
        }).bounds(left, top + 190, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored ->
        {
            page++;
            refresh();
        }).bounds(left + PANEL_WIDTH - 24, top + 190, 24, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose()).bounds(left + 30, top + 190, PANEL_WIDTH - 60, 20).build());
        refresh();
        setInitialFocus(search);
    }

    private List<BaseChoice> entries()
    {
        String query = search.getValue().toLowerCase(Locale.ROOT);
        return bases.stream().filter(base -> (base.name() + " " + base.map() + " " + base.position().toShortString()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void refresh()
    {
        List<BaseChoice> entries = entries();
        for (int index = 0; index < rows.size(); index++)
        {
            Button row = rows.get(index);
            int entry = page * PAGE_SIZE + index;
            row.visible = entry < entries.size();
            if (row.visible)
            {
                BaseChoice base = entries.get(entry);
                Component label = Component.literal((base.id().equals(selected) ? "[x] " : "") + base.name() + " (" + base.map() + ")");
                row.setMessage(label);
                row.setTooltip(Tooltip.create(label.copy().append("\n" + base.position().toShortString() + "\n" + base.id())));
            }
        }
        previous.active = page > 0;
        next.active = (page + 1) * PAGE_SIZE < entries.size();
    }

    private void choose(@Nullable BaseChoice base)
    {
        apply.accept(base);
        onClose();
    }

    @Override
    public void tick()
    {
        search.tick();
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = height / 2 - 110;
        graphics.fill(left - 6, top, left + PANEL_WIDTH + 6, top + 218, 0xFF252525);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        if (entries().isEmpty())
            graphics.drawCenteredString(font, Component.translatable("gui.flansmod.teams.spawner_no_bases"), width / 2, top + 120, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
