package com.flansmodultimate.client.gui;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Searchable, paginated multi-selection of NPC categories or exact content-pack short names. */
public final class TeamsSpawnerNpcScreen extends Screen
{
    private static final List<String> CATEGORIES = List.of("vehicles", "planes", "mechas", "aa_guns", "ships");
    private final Screen parent;
    private final Consumer<SpawnerNpcPolicy> apply;
    private final Set<String> selectedCategories = new HashSet<>();
    private final Set<String> definitions = new HashSet<>();
    private final List<IContentType> catalog = FlansModApi.getTypes().stream().filter(type -> !SpawnerNpcPolicy.category(type).isEmpty()).sorted(Comparator.comparing(IContentType::getShortName))
        .toList();
    private SpawnerNpcPolicy.Mode mode;
    private EditBox search;
    private int page;
    private final List<Button> choices = new ArrayList<>();
    private Button previous;
    private Button next;

    public TeamsSpawnerNpcScreen(Screen parent, SpawnerNpcPolicy policy, Consumer<SpawnerNpcPolicy> apply)
    {
        super(Component.translatable("gui.flansmod.teams.npc_selection"));
        this.parent = parent;
        this.apply = apply;
        mode = policy.mode();
        (mode == SpawnerNpcPolicy.Mode.CATEGORIES ? selectedCategories : definitions).addAll(policy.selections());
    }

    @Override
    protected void init()
    {
        int left = width / 2 - 150;
        int top = height / 2 - 120;
        choices.clear();
        int index = 0;
        for (SpawnerNpcPolicy.Mode option : SpawnerNpcPolicy.Mode.values())
        {
            addRenderableWidget(Button.builder(Component.translatable("gui.flansmod.teams.npc_mode_" + option.name().toLowerCase(Locale.ROOT)), ignored ->
            {
                mode = option;
                page = 0;
                refresh();
            }).bounds(left + index++ * 75, top + 24, 73, 20).build());
        }
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left, top + 50, 300, 18, Component.translatable("gui.flansmod.teams.npc_search"));
        search.setMaxLength(128);
        search.setValue(query);
        search.setHint(Component.translatable("gui.flansmod.teams.npc_search"));
        search.setResponder(ignored ->
        {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);
        for (int i = 0; i < 6; i++)
        {
            int slot = i;
            choices.add(addRenderableWidget(Button.builder(Component.empty(), ignored -> toggle(slot)).bounds(left, top + 74 + i * 21, 300, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored ->
        {
            page--;
            refresh();
        }).bounds(left, top + 204, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored ->
        {
            page++;
            refresh();
        }).bounds(left + 276, top + 204, 24, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored ->
        {
            apply.accept(new SpawnerNpcPolicy(mode, mode == SpawnerNpcPolicy.Mode.CATEGORIES ? selectedCategories : definitions));
            onClose();
        }).bounds(left + 28, top + 204, 120, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose()).bounds(left + 152, top + 204, 120, 20).build());
        refresh();
    }

    private List<String> entries()
    {
        if (mode == SpawnerNpcPolicy.Mode.CATEGORIES)
            return CATEGORIES;
        if (mode != SpawnerNpcPolicy.Mode.DEFINITIONS)
            return List.of();
        String query = search.getValue().toLowerCase(Locale.ROOT);
        return catalog.stream().filter(type -> type.getShortName().toLowerCase(Locale.ROOT).contains(query) || label(type).getString().toLowerCase(Locale.ROOT).contains(query))
            .map(IContentType::getShortName).toList();
    }

    private static Component label(IContentType type)
    {
        return type.getItem() == null ? Component.literal(type.getShortName()) : Component.translatable(type.getItem().getDescriptionId());
    }

    private void toggle(int slot)
    {
        List<String> entries = entries();
        int index = page * 6 + slot;
        if (index >= entries.size())
            return;
        Set<String> selected = mode == SpawnerNpcPolicy.Mode.CATEGORIES ? selectedCategories : definitions;
        String id = entries.get(index);
        if (!selected.add(id))
            selected.remove(id);
        refresh();
    }

    private void refresh()
    {
        List<String> entries = entries();
        search.visible = mode == SpawnerNpcPolicy.Mode.DEFINITIONS;
        for (int i = 0; i < choices.size(); i++)
        {
            int index = page * 6 + i;
            Button button = choices.get(i);
            button.visible = index < entries.size();
            if (!button.visible)
                continue;
            String id = entries.get(index);
            boolean category = mode == SpawnerNpcPolicy.Mode.CATEGORIES;
            Component name = category
                ? Component.translatable("gui.flansmod.teams.npc_" + id)
                : catalog.stream().filter(type -> type.getShortName().equals(id)).findFirst().map(TeamsSpawnerNpcScreen::label).orElse(Component.literal(id));
            Set<String> selected = category ? selectedCategories : definitions;
            button.setMessage(Component.literal(selected.contains(id) ? "[x] " : "[ ] ").append(name));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(id)));
        }
        previous.active = page > 0;
        next.active = (page + 1) * 6 < entries.size();
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick()
    {
        search.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.fill(width / 2 - 156, height / 2 - 126, width / 2 + 156, height / 2 + 110, 0xFF252525);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 114, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.flansmod.teams.npc_mode_" + mode.name().toLowerCase(Locale.ROOT)), width / 2, height / 2 + 110, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
