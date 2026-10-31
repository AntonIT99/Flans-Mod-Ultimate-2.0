package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The operator's editor for the bots of each round of the rotation and for the squad templates. It edits a copy of a
 * round's settings and sends it to the server on saving; the server checks the operator's permission, keeps the
 * settings with the round and sends the editor's state back. Numbers are typed; clones, class weights, squads, class
 * roles and crew classes are chosen in pickers of their own, and the remaining lists are edited as one line of text
 * each. Each setting explains itself in its tooltip. Unsaved edits are marked on the save button and kept while the
 * server sends new state for the same round; leaving the round or the editor with them asks first.
 */
public final class BotEditorScreen extends Screen
{
    private static final int ROW_HEIGHT = 22;
    private static final int LABEL_WIDTH = 120;
    private static final int FIELD_WIDTH = 220;
    /** Width of one of the two columns of the round page, labels included. */
    private static final int COLUMN_WIDTH = (LABEL_WIDTH + FIELD_WIDTH) / 2 - 2;
    /** Width of a number box that shares its row with a button. */
    private static final int SHORT_FIELD_WIDTH = FIELD_WIDTH / 3;
    private static final int TOP = 50;
    private static final int TEXT_LIMIT = 2048;
    private static final int LABEL_COLOUR = 0xE0E0E0;
    private static final int NOTE_COLOUR = 0xA0A0A0;
    private static final String NONE = "none";
    private static final String PREFIX = "wolffsmodnpcs.editor.";
    private static final String TIP = ".tip";
    private static final String COUNT = "count";
    private static final String BUDGET = "budget";
    private static final String TOTAL = "total";
    private static final String VEHICLES = "vehicles";
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

    /** @return every squad template, for the squad picker */
    List<SquadTemplate> templates()
    {
        return templates;
    }

    @Override
    protected void init()
    {
        fields.clear();
        labels.clear();
        int left = left();
        int tabWidth = (LABEL_WIDTH + FIELD_WIDTH) / Tab.values().length;
        for (Tab page : Tab.values())
        {
            String name = page.name().toLowerCase(Locale.ROOT);
            addRenderableWidget(Button.builder(Component.translatable(PREFIX + "tab." + name), button -> switchTab(page)).bounds(left + page.ordinal() * tabWidth, 26, tabWidth - 2, 20)
                .tooltip(tip("tab." + name)).build()).active = page != tab;
        }
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

    private int left()
    {
        return width / 2 - (LABEL_WIDTH + FIELD_WIDTH) / 2;
    }

    // --- Pages ---

    /** The round's own settings, in two columns of six so the page fits a small window. */
    private void initRound(int left)
    {
        int second = left + COLUMN_WIDTH + 4;
        addRenderableWidget(switchButton(left, y(0), COLUMN_WIDTH, "enabled", settings.isEnabled(), settings::setEnabled));
        addRenderableWidget(modeButton(left, y(1), COLUMN_WIDTH));
        addRenderableWidget(enumButton(left, y(2), COLUMN_WIDTH, "nameplates", BotSettings.Nameplates.values(), settings.getNameplates(), settings::setNameplates));
        addRenderableWidget(enumButton(left, y(3), COLUMN_WIDTH, "scoreboard", BotSettings.Scoreboard.values(), settings.getScoreboard(), settings::setScoreboard));
        addCompactField(left, 4, TOTAL, Integer.toString(settings.getTotal()));
        // Only share mode shares the total out; the others keep it unused.
        fields.get(TOTAL).setEditable(settings.getMode() == BotSettings.Mode.SHARE);
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
        addRenderableWidget(difficultyButton(left, y(0), width));
        addRenderableWidget(switchButton(left, y(1), width, "player_tickets", settings.isPlayerTickets(), settings::setPlayerTickets));
        addRenderableWidget(switchButton(left, y(2), width, "radio", settings.isRadio(), settings::setRadio));
        addRenderableWidget(switchButton(left, y(3), width, "suppression", settings.isSuppression(), settings::setSuppression));
        addRenderableWidget(switchButton(left, y(4), width, "rally_points", settings.isRallyPoints(), settings::setRallyPoints));
        addField(left, 5, RETREAT, Integer.toString(settings.getRetreatHealth()));
        addRenderableWidget(switchButton(left, y(6), width, "vehicle_crews", settings.isVehicleCrews(), settings::setVehicleCrews));
    }

    private CycleButton<Boolean> switchButton(int x, int y, int width, String key, boolean value, Consumer<Boolean> setter)
    {
        return CycleButton.onOffBuilder(value).withTooltip(chosen -> tip(key)).create(x, y, width, 20, Component.translatable(PREFIX + key), (button, chosen) -> setter.accept(chosen));
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
            .bounds(left + LABEL_WIDTH, y(1), half, 20).tooltip(tip("apply_preset")).build()).active = !presets.isEmpty();
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "remove_preset"), button -> presetAction(BotEditorActionPacket.Action.REMOVE_PRESET, chosen))
            .bounds(left + LABEL_WIDTH + half + 4, y(1), half, 20).build()).active = !presets.isEmpty();
        addField(left, 2, PRESET_NAME, chosen);
        addRenderableWidget(
            Button.builder(Component.translatable(PREFIX + "save_preset"), button -> savePreset()).bounds(left + LABEL_WIDTH, y(3), FIELD_WIDTH, 20).tooltip(tip("save_preset")).build());
        copyFrom = Math.floorMod(copyFrom, state.rounds().size());
        addSelector(left, 4, Component.translatable(PREFIX + "copy_source", state.rounds().get(copyFrom).label()), () -> copyFrom--, () -> copyFrom++);
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "copy_round"), button -> copyRound()).bounds(left + LABEL_WIDTH, y(5), FIELD_WIDTH, 20).tooltip(tip("copy_round"))
            .build()).active = copyFrom != state.round();
    }

    /**
     * The team's loose infantry: how many, read by the round's count mode shown beside it, and how they are made. A line
     * below says how many soldiers that fields.
     */
    private void initTeam(int left)
    {
        if (teams().isEmpty())
            return;
        addTeamSelector(left);
        String name = teams().get(team);
        BotSettings.TeamForce force = settings.team(name);
        int beside = left + LABEL_WIDTH + SHORT_FIELD_WIDTH + 4;
        int besideWidth = FIELD_WIDTH - SHORT_FIELD_WIDTH - 4;
        addField(left, 1, COUNT, COUNT + "." + lower(settings.getMode()), Integer.toString(force.getCount()), SHORT_FIELD_WIDTH);
        addRenderableWidget(modeButton(beside, y(1), besideWidth));
        addField(left, 2, BUDGET, BUDGET, Integer.toString(force.getBudget()), SHORT_FIELD_WIDTH);
        addRenderableWidget(nameListButton(beside, y(2), besideWidth, force.getNameList(), force::setNameList));
        addPickerButton(left, 3, "clone", cloneSummary(force), () -> new BotClonePicker(this, name, force));
        addPickerButton(left, 4, "classes", weightsSummary(force), () -> new BotClassWeightsPicker(this, name, force));
    }

    /** The team's squads, vehicles, class roles and vehicle crews. */
    private void initForces(int left)
    {
        if (teams().isEmpty())
            return;
        addTeamSelector(left);
        String name = teams().get(team);
        BotSettings.TeamForce force = settings.team(name);
        addPickerButton(left, 1, "squads", squadsSummary(force), () -> new BotSquadsPicker(this, name, force));
        addField(left, 2, VEHICLES, BotSettingsText.formatVehicles(force.getVehicles()));
        addPickerButton(left, 3, "roles", rolesSummary(force), () -> new BotRolesPicker(this, name, force));
        addCrewButtons(left, name, force);
        labels.add(new Label(Component.translatable("wolffsmodnpcs.editor.vehicles_help"), left, y(5) + 6));
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
        addRenderableWidget(nameListButton(left + LABEL_WIDTH, y(5), FIELD_WIDTH, templateNames, value -> templateNames = value));
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
        saveButton = addRenderableWidget(Button.builder(saveLabel(), button -> saveSettings()).bounds(left, height - 30, half, 20).tooltip(tip("save")).build());
        saveButton.active = state.round() >= 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + half + 4, height - 30, half, 20).build());
    }

    // --- What the pickers chose ---

    private static Component cloneSummary(BotSettings.TeamForce force)
    {
        if (!force.usesClone())
            return Component.translatable(PREFIX + "clone.plain");
        return Component.translatable(PREFIX + (force.isKeepCloneEquipment() ? "clone.chosen_equipped" : "clone.chosen"), force.getCloneTab(), force.getCloneName());
    }

    private static Component weightsSummary(BotSettings.TeamForce force)
    {
        if (force.getClassWeights().isEmpty())
            return Component.translatable(PREFIX + "classes.equal");
        StringJoiner joiner = new StringJoiner(", ");
        force.getClassWeights().forEach((playerClass, weight) -> joiner.add(className(playerClass) + " " + weight));
        return Component.literal(joiner.toString());
    }

    private Component squadsSummary(BotSettings.TeamForce force)
    {
        if (force.getSquads().isEmpty())
            return Component.translatable(PREFIX + "squads.none");
        StringJoiner joiner = new StringJoiner(", ");
        for (BotSettings.SquadEntry entry : force.getSquads())
        {
            String name = templates.stream().filter(candidate -> candidate.id().equals(entry.template()) && !candidate.name().isBlank()).map(SquadTemplate::name).findFirst().orElse(entry.template());
            joiner.add(name + " ×" + entry.count());
        }
        return Component.literal(joiner.toString());
    }

    private static Component rolesSummary(BotSettings.TeamForce force)
    {
        if (force.getClassRoles().isEmpty())
            return Component.translatable(PREFIX + "roles.auto");
        StringJoiner joiner = new StringJoiner(", ");
        force.getClassRoles().forEach((playerClass, role) -> joiner.add(className(playerClass) + ": " + BotClassPicker.roleName(role).getString()));
        return Component.literal(joiner.toString());
    }

    /** @return a class's name as its pack gives it, or its short name when no loaded class has it */
    private static String className(String playerClass)
    {
        return FlansModApi.getPlayerClass(playerClass).map(IPlayerClass::getNameComponent).map(Component::getString).orElse(playerClass);
    }

    // --- Widgets ---

    private static int y(int row)
    {
        return TOP + row * ROW_HEIGHT;
    }

    private static String lower(Enum<?> constant)
    {
        return constant.name().toLowerCase(Locale.ROOT);
    }

    /** @return the tooltip that explains a setting, or null for one without */
    @Nullable
    private static Tooltip tip(String key)
    {
        String text = PREFIX + key + TIP;
        return I18n.exists(text) ? Tooltip.create(Component.translatable(text)) : null;
    }

    private void addField(int left, int row, String key, String value)
    {
        addField(left, row, key, key, value, FIELD_WIDTH);
    }

    /**
     * @param label
     *            the key of the label and the tooltip, which may tell more than the setting's own key, such as what a
     *            count means in the round's count mode
     */
    private void addField(int left, int row, String key, String label, String value, int boxWidth)
    {
        EditBox box = new EditBox(font, left + LABEL_WIDTH, y(row), boxWidth, 20, Component.translatable(PREFIX + label));
        box.setMaxLength(TEXT_LIMIT);
        box.setValue(value);
        box.setTooltip(tip(label));
        fields.put(key, addRenderableWidget(box));
        labels.add(new Label(Component.translatable(PREFIX + label), left, y(row) + 6));
    }

    /** A button naming what a picker chose; it opens the picker after keeping what the page's fields hold. */
    private void addPickerButton(int left, int row, String key, Component summary, Supplier<Screen> picker)
    {
        labels.add(new Label(Component.translatable(PREFIX + key), left, y(row) + 6));
        addRenderableWidget(Button.builder(summary, button -> openPicker(picker)).bounds(left + LABEL_WIDTH, y(row), FIELD_WIDTH, 20).tooltip(tip(key)).build());
    }

    private void openPicker(Supplier<Screen> picker)
    {
        commit();
        if (minecraft != null)
            minecraft.setScreen(picker.get());
    }

    /** The crew class of each kind of seat of the team's vehicles, a button each in one row. */
    private void addCrewButtons(int left, String name, BotSettings.TeamForce force)
    {
        int row = 4;
        int buttonWidth = (FIELD_WIDTH - 8) / 3;
        labels.add(new Label(Component.translatable(PREFIX + "crew"), left, y(row) + 6));
        for (CrewPost post : CrewPost.values())
        {
            String crew = BotSettingsText.crewClass(force.crewClass(post));
            boolean named = !BotSettingsText.ANY_CLASS.equals(crew) && !BotSettings.TeamForce.NO_CREW.equalsIgnoreCase(crew);
            Component value = named ? Component.literal(className(crew)) : Component.translatable(PREFIX + "crew." + crew.toLowerCase(Locale.ROOT));
            Tooltip help = Tooltip.create(Component.translatable(PREFIX + "crew_" + post.id()).append("\n").append(Component.translatable(PREFIX + "crew" + TIP)));
            addRenderableWidget(
                Button.builder(Component.translatable(PREFIX + "crew_button." + post.id(), value), button -> openPicker(() -> new BotCrewPicker(this, name, force, post, settings.isVehicleCrews())))
                    .bounds(left + LABEL_WIDTH + post.ordinal() * (buttonWidth + 4), y(row), buttonWidth, 20).tooltip(help).build());
        }
    }

    /** A field of a compact column: its label above-left, the box to the right, half as wide as a full one. */
    private void addCompactField(int x, int row, String key, String value)
    {
        int boxWidth = COLUMN_WIDTH / 3;
        EditBox box = new EditBox(font, x + COLUMN_WIDTH - boxWidth, y(row), boxWidth, 20, Component.translatable(PREFIX + key));
        box.setMaxLength(TEXT_LIMIT);
        box.setValue(value);
        box.setTooltip(tip(key));
        fields.put(key, addRenderableWidget(box));
        labels.add(new Label(Component.translatable(PREFIX + key), x, y(row) + 6));
    }

    /** The bot difficulty, whose tooltip details what the chosen level does. */
    private CycleButton<BotSettings.Difficulty> difficultyButton(int x, int y, int width)
    {
        return CycleButton.<BotSettings.Difficulty>builder(level -> Component.literal(level.name().toLowerCase(Locale.ROOT))).withValues(BotSettings.Difficulty.values())
            .withInitialValue(settings.getDifficulty())
            .withTooltip(level -> Tooltip.create(Component.translatable("wolffsmodnpcs.centre.difficulty." + level.name().toLowerCase(Locale.ROOT) + ".tip")))
            .create(x, y, width, 20, Component.translatable(PREFIX + "difficulty"), (button, chosen) -> settings.setDifficulty(chosen));
    }

    /**
     * The count mode, which decides what each team's infantry count means. A change shows the page again, for the total is
     * only typed in share mode and the count is named after what it means.
     */
    private CycleButton<BotSettings.Mode> modeButton(int x, int y, int width)
    {
        return enumButton(x, y, width, "mode", BotSettings.Mode.values(), settings.getMode(), chosen ->
        {
            commit();
            settings.setMode(chosen);
            rebuildWidgets();
        });
    }

    /** A choice of constants, each named and explained by translations of its own when it has them. */
    private <E extends Enum<E>> CycleButton<E> enumButton(int x, int y, int width, String key, E[] values, E value, Consumer<E> setter)
    {
        return CycleButton.<E>builder(constant -> valueName(key, constant)).withValues(values).withInitialValue(value).withTooltip(constant -> tip(key + "." + lower(constant))).create(x, y, width, 20,
            Component.translatable(PREFIX + key), (button, chosen) -> setter.accept(chosen));
    }

    private static Component valueName(String key, Enum<?> constant)
    {
        String name = PREFIX + key + "." + lower(constant);
        return I18n.exists(name) ? Component.translatable(name) : Component.literal(lower(constant));
    }

    private CycleButton<String> nameListButton(int x, int y, int width, String value, Consumer<String> setter)
    {
        List<String> lists = new ArrayList<>();
        lists.add("");
        lists.addAll(SoldierNames.ids());
        return CycleButton.<String>builder(list -> Component.literal(list.isEmpty() ? NONE : list)).withValues(lists).withInitialValue(lists.contains(value) ? value : "")
            .withTooltip(list -> tip("names")).create(x, y, width, 20, Component.translatable("wolffsmodnpcs.editor.names"), (button, chosen) -> setter.accept(chosen));
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
                settings.setTotal(Math.max(0, number(TOTAL, settings.getTotal())));
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

    /** The infantry page's numbers; its clone and class weights are set by their pickers. */
    private void commitTeam()
    {
        if (teams().isEmpty() || !fields.containsKey(COUNT))
            return;
        BotSettings.TeamForce force = settings.team(teams().get(team));
        force.setCount(Math.max(0, number(COUNT, force.getCount())));
        force.setBudget(Math.max(0, number(BUDGET, force.getBudget())));
    }

    /** The forces page's vehicles; its squads, class roles and crew classes are set by their pickers. */
    private void commitForces()
    {
        if (teams().isEmpty() || !fields.containsKey(VEHICLES))
            return;
        BotSettings.TeamForce force = settings.team(teams().get(team));
        List<BotSettings.VehicleEntry> vehicles = BotSettingsText.parseVehicles(text(VEHICLES), force.getVehicles());
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

    /**
     * What the shown team's count fields in the round's count mode, with the count as it is typed: the soldiers it keeps
     * in the field, its part of the shared total, or the fighters bots keep it at.
     */
    private Component planned()
    {
        String shown = teams().get(team);
        // Read without settings.team, which would add an empty force for each team and mark the round as edited.
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String name : teams())
            counts.put(name, Optional.ofNullable(settings.getTeams().get(name)).map(BotSettings.TeamForce::getCount).orElse(0));
        int count = Math.max(0, number(COUNT, counts.get(shown)));
        counts.put(shown, count);
        int fielded = ForcePlanner.infantryTargets(settings.getMode(), settings.getTotal(), counts, Map.of()).getOrDefault(shown, 0);
        int weights = counts.values().stream().mapToInt(Integer::intValue).sum();
        return switch (settings.getMode())
        {
            case FIXED -> Component.translatable(PREFIX + "planned.fixed", fielded);
            case SHARE -> Component.translatable(PREFIX + "planned.share", fielded, settings.getTotal(), count, weights);
            case FILL -> Component.translatable(PREFIX + "planned.fill", count);
        };
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        for (Label label : labels)
            graphics.drawString(font, label.text(), label.x(), label.y(), LABEL_COLOUR);
        if (tab == Tab.TEAM && !teams().isEmpty())
            graphics.drawWordWrap(font, planned(), left(), y(5) + 2, LABEL_WIDTH + FIELD_WIDTH, NOTE_COLOUR);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
