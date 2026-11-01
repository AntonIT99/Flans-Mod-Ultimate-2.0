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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.function.Consumer;

/** Searchable, paginated multi-selection of NPC categories or exact content-pack short names. */
public final class TeamsSpawnerNpcScreen extends Screen
{
    private static final List<String> CATEGORIES = List.of("vehicles", "planes", "mechas", "aa_guns", "ships");
    private static final int ROWS = 6;
    private static final int PANEL_WIDTH = 300;
    private final Screen parent;
    private final Consumer<SpawnerNpcPolicy> apply;
    private final Set<String> selectedCategories = new HashSet<>();
    private final Set<String> definitions = new HashSet<>();
    private final List<IContentType> catalog = FlansModApi.getTypes().stream().filter(type -> !SpawnerNpcPolicy.category(type).isEmpty()).sorted(Comparator.comparing(IContentType::getShortName))
        .toList();
    private final List<Button> modes = new ArrayList<>();
    private final List<Button> choices = new ArrayList<>();
    private SpawnerNpcPolicy.Mode mode;
    private EditBox search;
    private int page;
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

    /** Translation key of the mode's own name; the same key plus {@code _info} describes what it does. */
    private static String modeKey(SpawnerNpcPolicy.Mode mode)
    {
        return "gui.flansmod.teams.npc_mode_" + mode.name().toLowerCase(Locale.ROOT);
    }

    private static Component label(IContentType type)
    {
        return type.getItem() == null ? Component.literal(type.getShortName()) : Component.translatable(type.getItem().getDescriptionId());
    }

    @Override
    protected void init()
    {
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = height / 2 - 122;
        modes.clear();
        choices.clear();
        int index = 0;
        for (SpawnerNpcPolicy.Mode option : SpawnerNpcPolicy.Mode.values())
        {
            Button button = addRenderableWidget(Button.builder(Component.translatable(modeKey(option)), ignored ->
            {
                mode = option;
                page = 0;
                refresh();
            }).bounds(left + index++ * 75, top + 22, 73, 20).build());
            button.setTooltip(Tooltip.create(Component.translatable(modeKey(option) + "_info")));
            modes.add(button);
        }
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left, top + 46, PANEL_WIDTH, 18, Component.translatable("gui.flansmod.teams.npc_search"));
        search.setMaxLength(128);
        search.setValue(query);
        search.setHint(Component.translatable("gui.flansmod.teams.npc_search"));
        search.setResponder(ignored ->
        {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);
        for (int i = 0; i < ROWS; i++)
        {
            int slot = i;
            choices.add(addRenderableWidget(Button.builder(Component.empty(), ignored -> toggle(slot)).bounds(left, top + 68 + i * 21, PANEL_WIDTH, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored ->
        {
            page--;
            refresh();
        }).bounds(left, top + 208, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored ->
        {
            page++;
            refresh();
        }).bounds(left + PANEL_WIDTH - 24, top + 208, 24, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored ->
        {
            apply.accept(new SpawnerNpcPolicy(mode, selectable() ? selected() : Set.of()));
            onClose();
        }).bounds(left + 28, top + 208, 120, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose()).bounds(left + 152, top + 208, 120, 20).build());
        refresh();
    }

    /** Whether the current mode picks single entries, as opposed to covering everything or nothing. */
    private boolean selectable()
    {
        return mode == SpawnerNpcPolicy.Mode.CATEGORIES || mode == SpawnerNpcPolicy.Mode.DEFINITIONS;
    }

    private Set<String> selected()
    {
        return mode == SpawnerNpcPolicy.Mode.CATEGORIES ? selectedCategories : definitions;
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

    private void toggle(int slot)
    {
        List<String> entries = entries();
        int index = page * ROWS + slot;
        if (index >= entries.size())
            return;
        Set<String> selected = selected();
        String id = entries.get(index);
        if (!selected.add(id))
            selected.remove(id);
        refresh();
    }

    private void refresh()
    {
        List<String> entries = entries();
        search.visible = mode == SpawnerNpcPolicy.Mode.DEFINITIONS;
        SpawnerNpcPolicy.Mode[] options = SpawnerNpcPolicy.Mode.values();
        for (int i = 0; i < modes.size(); i++)
            modes.get(i).active = options[i] != mode;
        for (int i = 0; i < choices.size(); i++)
        {
            int index = page * ROWS + i;
            Button button = choices.get(i);
            button.visible = index < entries.size();
            if (!button.visible)
                continue;
            String id = entries.get(index);
            boolean category = mode == SpawnerNpcPolicy.Mode.CATEGORIES;
            Component name = category
                ? Component.translatable("gui.flansmod.teams.npc_" + id)
                : catalog.stream().filter(type -> type.getShortName().equals(id)).findFirst().map(TeamsSpawnerNpcScreen::label).orElse(Component.literal(id));
            button.setMessage(Component.literal(selected().contains(id) ? "[x] " : "[ ] ").append(name));
            button.setTooltip(Tooltip.create(Component.literal(id)));
        }
        previous.visible = selectable();
        next.visible = selectable();
        previous.active = page > 0;
        next.active = (page + 1) * ROWS < entries.size();
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
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = height / 2 - 122;
        graphics.fill(left - 6, top - 6, left + PANEL_WIDTH + 6, top + 234, 0xFF252525);
        graphics.drawCenteredString(font, title, width / 2, top + 6, 0xFFFFFF);
        if (selectable())
            renderStatus(graphics, left, top);
        else
        {
            List<FormattedCharSequence> lines = font.split(Component.translatable(modeKey(mode) + "_info"), PANEL_WIDTH - 16);
            for (int i = 0; i < lines.size(); i++)
                graphics.drawCenteredString(font, lines.get(i), width / 2, top + 120 + i * 12, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Page position on the left, how much the mode currently permits on the right, in red while it permits nothing. */
    private void renderStatus(GuiGraphics graphics, int left, int top)
    {
        int pages = Math.max(1, (entries().size() + ROWS - 1) / ROWS);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.npc_page", page + 1, pages), left + 2, top + 194, 0xAAAAAA);
        boolean empty = selected().isEmpty();
        Component summary = empty ? Component.translatable("gui.flansmod.teams.npc_no_selection") : Component.translatable("gui.flansmod.teams.npc_selected_count", selected().size());
        graphics.drawString(font, summary, left + PANEL_WIDTH - 2 - font.width(summary), top + 194, empty ? 0xFF7777 : 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
