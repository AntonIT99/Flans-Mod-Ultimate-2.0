package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.BotEditorActionPacket;
import com.wolffsmod.npcs.network.BotEditorStatePacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;
import com.wolffsmod.npcs.teams.SquadTemplate;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * The operator's editor for the bots of each round of the rotation and for the squad templates. It edits a copy of a
 * round's settings and sends it to the server on saving; the server checks the operator's permission, keeps the
 * settings with the round and sends the editor's state back. Lists are edited as one line of text each.
 */
public final class BotEditorScreen extends Screen
{
    private static final int ROW_HEIGHT = 22;
    private static final int LABEL_WIDTH = 120;
    private static final int FIELD_WIDTH = 220;
    /** Width of one of the two columns of the round page, labels included. */
    private static final int COLUMN_WIDTH = (LABEL_WIDTH + FIELD_WIDTH) / 2 - 2;
    private static final int TOP = 50;
    private static final int TEXT_LIMIT = 2048;
    private static final String NONE = "none";
    private static final String PREFIX = "wolffsmodnpcs.editor.";
    private static final String COUNT = "count";
    private static final String SQUADS = "squads";

    /** The pages of the editor. */
    private enum Tab
    {
        ROUND, TEAM, FORCES, TEMPLATES
    }

    private BotEditorStatePacket state;
    private BotSettings settings;
    private List<SquadTemplate> templates;
    private Tab tab = Tab.ROUND;
    private int team;
    /** The template being edited, as an index into {@link #templates}; past the end for a new one. */
    private int template;
    private final Map<String, EditBox> fields = new LinkedHashMap<>();
    private final List<Label> labels = new ArrayList<>();
    /** Values chosen with cycle buttons on the template page, kept until the template is saved. */
    private SquadTemplate.Posture posture = SquadTemplate.Posture.BALANCED;
    private String templateNames = "";

    private record Label(Component text, int x, int y)
    {}

    public BotEditorScreen(BotEditorStatePacket state)
    {
        super(Component.translatable("wolffsmodnpcs.editor.title"));
        accept(state);
    }

    /** Shows new state from the server; edits not saved yet are dropped. */
    public void update(BotEditorStatePacket update)
    {
        accept(update);
        rebuildWidgets();
    }

    private void accept(BotEditorStatePacket update)
    {
        state = update;
        // A round without settings opens with bots switched off, ready to be set up.
        settings = BotSettings.load(update.settings());
        templates = update.templates().stream().map(SquadTemplate::fromTag).flatMap(Optional::stream).toList();
        template = Math.min(template, templates.size());
        team = Math.min(team, Math.max(0, teams().size() - 1));
        loadTemplateChoices();
    }

    private List<String> teams()
    {
        return state.round() < 0 ? List.of() : state.rounds().get(state.round()).teams();
    }

    @Override
    protected void init()
    {
        fields.clear();
        labels.clear();
        int left = width / 2 - (LABEL_WIDTH + FIELD_WIDTH) / 2;
        int tabWidth = (LABEL_WIDTH + FIELD_WIDTH) / Tab.values().length;
        for (Tab page : Tab.values())
            addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.editor.tab." + page.name().toLowerCase(Locale.ROOT)), button -> switchTab(page))
                .bounds(left + page.ordinal() * tabWidth, 26, tabWidth - 2, 20).build()).active = page != tab;
        if (state.round() < 0)
        {
            addBottomButtons(left);
            return;
        }
        switch (tab)
        {
            case ROUND -> initRound(left);
            case TEAM -> initTeam(left);
            case FORCES -> initForces(left);
            case TEMPLATES -> initTemplates(left);
        }
        addBottomButtons(left);
    }

    // --- Pages ---

    /** The round's own settings, in two columns of six so the page fits a small window. */
    private void initRound(int left)
    {
        int second = left + COLUMN_WIDTH + 4;
        addRenderableWidget(
            CycleButton.onOffBuilder(settings.isEnabled()).create(left, y(0), COLUMN_WIDTH, 20, Component.translatable("wolffsmodnpcs.editor.enabled"), (button, value) -> settings.setEnabled(value)));
        addRenderableWidget(enumButton(left, y(1), COLUMN_WIDTH, "mode", BotSettings.Mode.values(), settings.getMode(), settings::setMode));
        addRenderableWidget(enumButton(left, y(2), COLUMN_WIDTH, "nameplates", BotSettings.Nameplates.values(), settings.getNameplates(), settings::setNameplates));
        addRenderableWidget(enumButton(left, y(3), COLUMN_WIDTH, "scoreboard", BotSettings.Scoreboard.values(), settings.getScoreboard(), settings::setScoreboard));
        addCompactField(left, 4, "total", Integer.toString(settings.getTotal()));
        addCompactField(left, 5, "respawn", Integer.toString(settings.getRespawnSeconds()));
        addCompactField(second, 0, "tickets", Integer.toString(settings.getTickets()));
        addCompactField(second, 1, "squadsize", Integer.toString(settings.getSquadSize()));
        addCompactField(second, 2, "abandon", Integer.toString(settings.getAbandonChance()));
        addCompactField(second, 3, "rubberband", Integer.toString(settings.getRubberBand()));
        addCompactField(second, 4, "capture_infantry", Float.toString(settings.getInfantryCaptureWeight()));
        addCompactField(second, 5, "capture_vehicles", Float.toString(settings.getVehicleCaptureWeight()));
    }

    private void initTeam(int left)
    {
        if (teams().isEmpty())
            return;
        addTeamSelector(left);
        BotSettings.TeamForce force = settings.team(teams().get(team));
        int row = 1;
        addField(left, row++, COUNT, Integer.toString(force.getCount()));
        addField(left, row++, "budget", Integer.toString(force.getBudget()));
        addRenderableWidget(nameListButton(left, row++, force.getNameList(), force::setNameList));
        addField(left, row++, "clone_tab", Integer.toString(force.getCloneTab()));
        addField(left, row++, "clone_name", force.getCloneName());
        addField(left, row, "classes", BotSettingsText.formatClassWeights(force.getClassWeights()));
    }

    private void initForces(int left)
    {
        if (teams().isEmpty())
            return;
        addTeamSelector(left);
        BotSettings.TeamForce force = settings.team(teams().get(team));
        addField(left, 1, SQUADS, BotSettingsText.formatSquads(force.getSquads()));
        addField(left, 2, "vehicles", BotSettingsText.formatVehicles(force.getVehicles()));
        String available = templates.isEmpty() ? NONE : String.join(", ", templates.stream().map(SquadTemplate::id).toList());
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.squads_help"), left, y(3)));
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.vehicles_help"), left, y(3) + 12));
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.templates_available", available), left, y(3) + 24));
    }

    private void initTemplates(int left)
    {
        SquadTemplate edited = template < templates.size() ? templates.get(template) : null;
        Component shown = edited == null ? Component.translatable("wolffsmodnpcs.editor.new_template") : Component.literal(edited.id() + " (" + edited.name() + ")");
        addSelector(left, shown, () -> cycleTemplate(-1), () -> cycleTemplate(1));
        addField(left, 1, "template_id", edited == null ? "" : edited.id());
        addField(left, 2, "template_name", edited == null ? "" : edited.name());
        addField(left, 3, "members", edited == null ? "" : BotSettingsText.formatMembers(edited.members()));
        addRenderableWidget(enumButton(left + LABEL_WIDTH, y(4), FIELD_WIDTH, "posture", SquadTemplate.Posture.values(), posture, value -> posture = value));
        addRenderableWidget(nameListButton(left, 5, templateNames, value -> templateNames = value));
        addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.editor.save_template"), button -> saveTemplate()).bounds(left + LABEL_WIDTH, y(6), FIELD_WIDTH / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.editor.remove_template"), button -> removeTemplate())
            .bounds(left + LABEL_WIDTH + FIELD_WIDTH / 2 + 2, y(6), FIELD_WIDTH / 2 - 2, 20).build()).active = edited != null;
    }

    private void addBottomButtons(int left)
    {
        Component round = state.round() < 0 ? Component.translatable("wolffsmodnpcs.editor.no_rounds") : Component.literal(state.rounds().get(state.round()).label());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> openRound(state.round() - 1)).bounds(left, height - 54, 20, 20).build()).active = state.round() > 0;
        addRenderableWidget(Button.builder(round, button ->
        {
            // The label only names the round.
        }).bounds(left + 22, height - 54, LABEL_WIDTH + FIELD_WIDTH - 44, 20).build()).active = false;
        addRenderableWidget(
            Button.builder(Component.literal(">"), button -> openRound(state.round() + 1)).bounds(left + LABEL_WIDTH + FIELD_WIDTH - 20, height - 54, 20, 20).build()).active = state.round() >= 0
                && state.round() < state.rounds().size() - 1;
        int half = (LABEL_WIDTH + FIELD_WIDTH) / 2 - 2;
        addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.editor.save"), button -> saveSettings()).bounds(left, height - 30, half, 20).build()).active = state.round() >= 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + half + 4, height - 30, half, 20).build());
    }

    // --- Widgets ---

    private static int y(int row)
    {
        return TOP + row * ROW_HEIGHT;
    }

    private void addField(int left, int row, String key, String value)
    {
        EditBox box = new EditBox(font, left + LABEL_WIDTH, y(row), FIELD_WIDTH, 20, Component.translatable(PREFIX + key));
        box.setMaxLength(TEXT_LIMIT);
        box.setValue(value);
        fields.put(key, addRenderableWidget(box));
        labels.add(new Label(Component.translatable(PREFIX + key), left, y(row) + 6));
    }

    /** A field of a compact column: its label above-left, the box to the right, half as wide as a full one. */
    private void addCompactField(int x, int row, String key, String value)
    {
        int boxWidth = COLUMN_WIDTH / 3;
        EditBox box = new EditBox(font, x + COLUMN_WIDTH - boxWidth, y(row), boxWidth, 20, Component.translatable(PREFIX + key));
        box.setMaxLength(TEXT_LIMIT);
        box.setValue(value);
        fields.put(key, addRenderableWidget(box));
        labels.add(new Label(Component.translatable(PREFIX + key), x, y(row) + 6));
    }

    private <E extends Enum<E>> CycleButton<E> enumButton(int x, int y, int width, String key, E[] values, E value, Consumer<E> setter)
    {
        return CycleButton.<E>builder(constant -> Component.literal(constant.name().toLowerCase(Locale.ROOT))).withValues(values).withInitialValue(value).create(x, y, width, 20,
            Component.translatable(PREFIX + key), (button, chosen) -> setter.accept(chosen));
    }

    private CycleButton<String> nameListButton(int left, int row, String value, Consumer<String> setter)
    {
        List<String> lists = new ArrayList<>();
        lists.add("");
        lists.addAll(SoldierNames.ids());
        return CycleButton.<String>builder(list -> Component.literal(list.isEmpty() ? NONE : list)).withValues(lists).withInitialValue(lists.contains(value) ? value : "").create(left + LABEL_WIDTH,
            y(row), FIELD_WIDTH, 20, Component.translatable("wolffsmodnpcs.editor.names"), (button, chosen) -> setter.accept(chosen));
    }

    /** Chooses the team the team pages edit; the round has one at least. */
    private void addTeamSelector(int left)
    {
        addSelector(left, Component.translatable("wolffsmodnpcs.editor.team", teams().get(team)), () -> cycleTeam(-1), () -> cycleTeam(1));
    }

    /** A choice in the first row: arrows on either side of what is chosen. */
    private void addSelector(int left, Component shown, Runnable previous, Runnable next)
    {
        addRenderableWidget(Button.builder(Component.literal("<"), button -> previous.run()).bounds(left + LABEL_WIDTH, y(0), 20, 20).build());
        addRenderableWidget(Button.builder(shown, button ->
        {
            // The label only names the choice.
        }).bounds(left + LABEL_WIDTH + 22, y(0), FIELD_WIDTH - 44, 20).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> next.run()).bounds(left + LABEL_WIDTH + FIELD_WIDTH - 20, y(0), 20, 20).build());
    }

    // --- Editing ---

    /** Takes the text of the page's fields into the settings, so they survive a change of page or team. */
    private void commit()
    {
        switch (tab)
        {
            case ROUND -> {
                settings.setTotal(Math.max(0, number("total", settings.getTotal())));
                settings.setRespawnSeconds(Math.max(0, number("respawn", settings.getRespawnSeconds())));
                settings.setTickets(Math.max(0, number("tickets", settings.getTickets())));
                settings.setSquadSize(Math.max(1, number("squadsize", settings.getSquadSize())));
                settings.setAbandonChance(Math.max(0, Math.min(100, number("abandon", settings.getAbandonChance()))));
                settings.setRubberBand(Math.max(0, number("rubberband", settings.getRubberBand())));
                settings.setInfantryCaptureWeight(Math.max(0F, decimal("capture_infantry", settings.getInfantryCaptureWeight())));
                settings.setVehicleCaptureWeight(Math.max(0F, decimal("capture_vehicles", settings.getVehicleCaptureWeight())));
            }
            case TEAM -> commitTeam();
            case FORCES -> commitForces();
            case TEMPLATES -> {
                // Templates are saved on their own, with their button.
            }
        }
    }

    private void commitTeam()
    {
        if (teams().isEmpty() || !fields.containsKey(COUNT))
            return;
        BotSettings.TeamForce force = settings.team(teams().get(team));
        force.setCount(Math.max(0, number(COUNT, force.getCount())));
        force.setBudget(Math.max(0, number("budget", force.getBudget())));
        force.setCloneTab(Math.max(0, number("clone_tab", force.getCloneTab())));
        force.setCloneName(text("clone_name").strip());
        force.getClassWeights().clear();
        force.getClassWeights().putAll(BotSettingsText.parseClassWeights(text("classes")));
    }

    private void commitForces()
    {
        if (teams().isEmpty() || !fields.containsKey(SQUADS))
            return;
        BotSettings.TeamForce force = settings.team(teams().get(team));
        List<BotSettings.VehicleEntry> vehicles = BotSettingsText.parseVehicles(text("vehicles"), force.getVehicles());
        force.getSquads().clear();
        force.getSquads().addAll(BotSettingsText.parseSquads(text(SQUADS)));
        force.getVehicles().clear();
        force.getVehicles().addAll(vehicles);
    }

    private String text(String key)
    {
        EditBox box = fields.get(key);
        return box == null ? "" : box.getValue();
    }

    private int number(String key, int fallback)
    {
        return fields.containsKey(key) ? BotSettingsText.parseNumber(text(key), fallback) : fallback;
    }

    private float decimal(String key, float fallback)
    {
        return fields.containsKey(key) ? BotSettingsText.parseDecimal(text(key), fallback) : fallback;
    }

    private void switchTab(Tab page)
    {
        commit();
        tab = page;
        rebuildWidgets();
    }

    private void cycleTeam(int step)
    {
        commit();
        team = Math.floorMod(team + step, teams().size());
        rebuildWidgets();
    }

    private void cycleTemplate(int step)
    {
        template = Math.floorMod(template + step, templates.size() + 1);
        loadTemplateChoices();
        rebuildWidgets();
    }

    private void loadTemplateChoices()
    {
        SquadTemplate edited = template < templates.size() ? templates.get(template) : null;
        posture = edited == null ? SquadTemplate.Posture.BALANCED : edited.posture();
        templateNames = edited == null ? "" : edited.nameList();
    }

    private void openRound(int round)
    {
        NpcNetwork.sendToServer(BotEditorActionPacket.open(round));
    }

    private void saveSettings()
    {
        commit();
        BotEditorStatePacket.Round round = state.rounds().get(state.round());
        NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.SAVE_SETTINGS, state.round(), round.id(), settings.save()));
    }

    private void saveTemplate()
    {
        SquadTemplate edited = new SquadTemplate(text("template_id").strip(), text("template_name").strip(), posture, templateNames, BotSettingsText.parseMembers(text("members")));
        NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.SAVE_TEMPLATE, state.round(), roundId(), edited.toTag()));
    }

    private void removeTemplate()
    {
        if (template >= templates.size())
            return;
        CompoundTag data = new CompoundTag();
        data.putString("id", templates.get(template).id());
        NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.REMOVE_TEMPLATE, state.round(), roundId(), data));
    }

    private UUID roundId()
    {
        return state.round() < 0 ? new UUID(0L, 0L) : state.rounds().get(state.round()).id();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        for (Label label : labels)
            graphics.drawString(font, label.text(), label.x(), label.y(), 0xE0E0E0);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
