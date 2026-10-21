package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.BotEditorActionPacket;
import com.wolffsmod.npcs.network.BotEditorStatePacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.teams.BotEditor;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;
import com.wolffsmod.npcs.teams.SquadTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * The operator's editor for the bots of each round of the rotation and for the squad templates. It edits a copy of a
 * round's settings and sends it to the server on saving; the server checks the operator's permission, keeps the
 * settings with the round and sends the editor's state back. Lists are edited as one line of text each. Unsaved edits
 * are marked on the save button and kept while the server sends new state for the same round; leaving the round or the
 * editor with them asks first.
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
    private static final String ROLES = "roles";
    private static final String RETREAT = "retreat";
    private static final String PRESET_NAME = "preset_name";
    /** Ticks between checks whether the edits differ from what the server holds. */
    private static final int CHECK_TICKS = 10;

    /** The pages of the editor. */
    private enum Tab
    {
        ROUND, RULES, TEAM, FORCES, TEMPLATES, PRESETS
    }

    private BotEditorStatePacket state;
    private BotSettings settings;
    /** The round's settings as the server holds them, to tell unsaved edits by. */
    private CompoundTag saved = new CompoundTag();
    private boolean unsaved;
    private int ticks;
    @Nullable
    private Button saveButton;
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
    /** The preset chosen on the preset page, as an index into the state's presets. */
    private int preset;
    /** The round chosen to copy settings from, as an index into the rotation. */
    private int copyFrom;

    private record Label(Component text, int x, int y)
    {}

    public BotEditorScreen(BotEditorStatePacket state)
    {
        super(Component.translatable("wolffsmodnpcs.editor.title"));
        accept(state);
    }

    /** Shows new state from the server; edits not saved yet are kept while it is for the same round. */
    public void update(BotEditorStatePacket update)
    {
        accept(update);
        rebuildWidgets();
    }

    private void accept(BotEditorStatePacket update)
    {
        boolean keep = state != null && roundId(state).equals(roundId(update)) && hasUnsavedEdits();
        state = update;
        // A round without settings opens with bots switched off, ready to be set up.
        saved = BotSettings.load(update.settings()).save();
        if (!keep)
            settings = BotSettings.load(update.settings());
        unsaved = keep && !settings.save().equals(saved);
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
            case RULES -> initRules(left);
            case TEAM -> initTeam(left);
            case FORCES -> initForces(left);
            case TEMPLATES -> initTemplates(left);
            case PRESETS -> initPresets(left);
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

    /** How bots fight and what players may do with them. */
    private void initRules(int left)
    {
        int width = LABEL_WIDTH + FIELD_WIDTH;
        addRenderableWidget(enumButton(left, y(0), width, "difficulty", BotSettings.Difficulty.values(), settings.getDifficulty(), settings::setDifficulty));
        addRenderableWidget(switchButton(left, y(1), width, "player_tickets", settings.isPlayerTickets(), settings::setPlayerTickets));
        addRenderableWidget(switchButton(left, y(2), width, "radio", settings.isRadio(), settings::setRadio));
        addRenderableWidget(switchButton(left, y(3), width, "suppression", settings.isSuppression(), settings::setSuppression));
        addRenderableWidget(switchButton(left, y(4), width, "rally_points", settings.isRallyPoints(), settings::setRallyPoints));
        addField(left, 5, RETREAT, Integer.toString(settings.getRetreatHealth()));
    }

    private CycleButton<Boolean> switchButton(int x, int y, int width, String key, boolean value, Consumer<Boolean> setter)
    {
        return CycleButton.onOffBuilder(value).create(x, y, width, 20, Component.translatable(PREFIX + key), (button, chosen) -> setter.accept(chosen));
    }

    /** Presets of bot settings to apply to the round, or save it as, and another round to copy settings from. */
    private void initPresets(int left)
    {
        List<String> presets = state.presets();
        preset = presets.isEmpty() ? 0 : Math.floorMod(preset, presets.size());
        String chosen = presets.isEmpty() ? "" : presets.get(preset);
        addSelector(left, 0, Component.translatable(PREFIX + "preset", chosen.isEmpty() ? NONE : chosen), () -> preset--, () -> preset++);
        int half = FIELD_WIDTH / 2 - 2;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "apply_preset"), button -> presetAction(BotEditorActionPacket.Action.APPLY_PRESET, chosen))
            .bounds(left + LABEL_WIDTH, y(1), half, 20).build()).active = !presets.isEmpty();
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "remove_preset"), button -> presetAction(BotEditorActionPacket.Action.REMOVE_PRESET, chosen))
            .bounds(left + LABEL_WIDTH + half + 4, y(1), half, 20).build()).active = !presets.isEmpty();
        addField(left, 2, PRESET_NAME, chosen);
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save_preset"), button -> savePreset()).bounds(left + LABEL_WIDTH, y(3), FIELD_WIDTH, 20).build());
        copyFrom = Math.floorMod(copyFrom, state.rounds().size());
        addSelector(left, 4, Component.translatable(PREFIX + "copy_source", state.rounds().get(copyFrom).label()), () -> copyFrom--, () -> copyFrom++);
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "copy_round"), button -> copyRound()).bounds(left + LABEL_WIDTH, y(5), FIELD_WIDTH, 20).build()).active = copyFrom != state
            .round();
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
        addField(left, row, "clone_tab", Integer.toString(force.getCloneTab()), FIELD_WIDTH / 3);
        addRenderableWidget(CycleButton.onOffBuilder(force.isKeepCloneEquipment()).create(left + LABEL_WIDTH + FIELD_WIDTH / 3 + 4, y(row++), FIELD_WIDTH - FIELD_WIDTH / 3 - 4, 20,
            Component.translatable("wolffsmodnpcs.editor.keep_equipment"), (button, value) -> force.setKeepCloneEquipment(value)));
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
        addField(left, 3, ROLES, BotSettingsText.formatRoles(force.getClassRoles()));
        String available = templates.isEmpty() ? NONE : String.join(", ", templates.stream().map(SquadTemplate::id).toList());
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.squads_help"), left, y(4)));
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.vehicles_help"), left, y(4) + 12));
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.roles_help"), left, y(4) + 24));
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.templates_available", available), left, y(4) + 36));
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
        saveButton = addRenderableWidget(Button.builder(saveLabel(), button -> saveSettings()).bounds(left, height - 30, half, 20).build());
        saveButton.active = state.round() >= 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + half + 4, height - 30, half, 20).build());
    }

    // --- Widgets ---

    private static int y(int row)
    {
        return TOP + row * ROW_HEIGHT;
    }

    private void addField(int left, int row, String key, String value)
    {
        addField(left, row, key, value, FIELD_WIDTH);
    }

    private void addField(int left, int row, String key, String value, int boxWidth)
    {
        EditBox box = new EditBox(font, left + LABEL_WIDTH, y(row), boxWidth, 20, Component.translatable(PREFIX + key));
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
        addSelector(left, 0, shown, previous, next);
    }

    /** A choice in a row: arrows on either side of what is chosen; an arrow that only changes the choice rebuilds the page. */
    private void addSelector(int left, int row, Component shown, Runnable previous, Runnable next)
    {
        addRenderableWidget(Button.builder(Component.literal("<"), button ->
        {
            previous.run();
            rebuildWidgets();
        }).bounds(left + LABEL_WIDTH, y(row), 20, 20).build());
        addRenderableWidget(Button.builder(shown, button ->
        {
            // The label only names the choice.
        }).bounds(left + LABEL_WIDTH + 22, y(row), FIELD_WIDTH - 44, 20).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
        {
            next.run();
            rebuildWidgets();
        }).bounds(left + LABEL_WIDTH + FIELD_WIDTH - 20, y(row), 20, 20).build());
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
            case RULES -> settings.setRetreatHealth(Math.max(0, Math.min(100, number(RETREAT, settings.getRetreatHealth()))));
            case TEAM -> commitTeam();
            case FORCES -> commitForces();
            case TEMPLATES, PRESETS -> {
                // Templates and presets are saved on their own, with their buttons.
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
        force.getClassRoles().clear();
        force.getClassRoles().putAll(BotSettingsText.parseRoles(text(ROLES)));
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
        confirmLeaving(() -> NpcNetwork.sendToServer(BotEditorActionPacket.open(round)));
    }

    @Override
    public void onClose()
    {
        confirmLeaving(super::onClose);
    }

    /** Leaves the round's edits behind, after asking when some are not saved. */
    private void confirmLeaving(Runnable leave)
    {
        if (!hasUnsavedEdits() || minecraft == null)
        {
            leave.run();
            return;
        }
        Minecraft game = minecraft;
        game.setScreen(new ConfirmScreen(discard ->
        {
            game.setScreen(this);
            if (discard)
            {
                settings = BotSettings.load(saved);
                unsaved = false;
                leave.run();
            }
        }, Component.translatable("wolffsmodnpcs.editor.unsaved_title"), Component.translatable("wolffsmodnpcs.editor.unsaved_question")));
    }

    /** Whether the edits, those still in the page's fields included, differ from the settings the server holds. */
    private boolean hasUnsavedEdits()
    {
        if (settings == null || state == null || state.round() < 0)
            return false;
        commit();
        return !settings.save().equals(saved);
    }

    @Override
    public void tick()
    {
        if (++ticks % CHECK_TICKS != 0)
            return;
        boolean now = hasUnsavedEdits();
        if (now != unsaved && saveButton != null)
        {
            unsaved = now;
            saveButton.setMessage(saveLabel());
        }
    }

    private Component saveLabel()
    {
        return Component.translatable(unsaved ? "wolffsmodnpcs.editor.save_unsaved" : "wolffsmodnpcs.editor.save");
    }

    private void saveSettings()
    {
        commit();
        BotEditorStatePacket.Round round = state.rounds().get(state.round());
        NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.SAVE_SETTINGS, state.round(), round.id(), settings.save()));
    }

    private void presetAction(BotEditorActionPacket.Action action, String name)
    {
        CompoundTag data = new CompoundTag();
        data.putString(BotEditor.NAME, name);
        if (action == BotEditorActionPacket.Action.APPLY_PRESET)
            confirmLeaving(() -> NpcNetwork.sendToServer(new BotEditorActionPacket(action, state.round(), roundId(), data)));
        else
            NpcNetwork.sendToServer(new BotEditorActionPacket(action, state.round(), roundId(), data));
    }

    /** Saves the settings as they are edited, unsaved changes included, under the name given. */
    private void savePreset()
    {
        commit();
        CompoundTag data = new CompoundTag();
        data.putString(BotEditor.NAME, text(PRESET_NAME).strip().toLowerCase(Locale.ROOT));
        data.put(BotEditor.SETTINGS, settings.save());
        NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.SAVE_PRESET, state.round(), roundId(), data));
    }

    private void copyRound()
    {
        CompoundTag data = new CompoundTag();
        data.putInt(BotEditor.FROM, copyFrom);
        confirmLeaving(() -> NpcNetwork.sendToServer(new BotEditorActionPacket(BotEditorActionPacket.Action.COPY_ROUND, state.round(), roundId(), data)));
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
        return roundId(state);
    }

    private static UUID roundId(BotEditorStatePacket shown)
    {
        return shown.round() < 0 ? new UUID(0L, 0L) : shown.rounds().get(shown.round()).id();
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
