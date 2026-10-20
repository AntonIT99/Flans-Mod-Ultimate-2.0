package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.EquipmentProperty;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.RequiredArgsConstructor;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Per-NPC controls inside the Wolff's Mod tab, using the Custom NPCs stats transport and sound picker.
 *
 * <p>
 * The overview lists the pages under section headings, one column per heading. A page is a list of rows, split into
 * named tabs where they do not fit together, and may link to a sub-page. The title of a page is the path to it.
 * </p>
 */
// Custom NPCs' editor requires the external GUI inheritance chain.
@SuppressWarnings("java:S110")
public class GuiWolffsMod extends GuiNPCInterface2 implements IGuiData, ITextfieldListener
{
    public static final int MENU_ID = 100;
    private static final int TITLE_LABEL_ID = 100;
    private static final int HINT_LABEL_ID = 101;
    private static final int SECTION_LABEL_ID = 120;
    private static final int CATEGORY_BUTTON_ID = 200;
    private static final int BACK_BUTTON_ID = 300;
    private static final int HURT_FLASH_ID = 400;
    private static final int DEATH_ROTATION_ID = 401;
    private static final int HIDE_BODY_ID = 402;
    private static final int IDLE_ENABLED_ID = 404;
    private static final int MOVEMENT_ENABLED_ID = 405;
    private static final int VARIABLE_PITCH_ID = 406;
    private static final int MODEL_DEFAULTS_ID = 407;
    private static final int EDIT_IDLE_ID = 408;
    private static final int EDIT_MOVEMENT_ID = 409;
    private static final int OVERRIDE_REPEAT_ID = 410;
    private static final int CLEAR_SOUND_ID = 411;
    private static final int MODEL_SOUNDS_LINK_ID = 412;
    private static final int ENGINE_SOUND_ID = 500;
    private static final int ENGINE_RANGE_ID = 501;
    private static final int ENGINE_REPEAT_ID = 502;
    private static final int ENGINE_SOUND_FIELD_ID = 503;
    private static final int VALUE_ID = 1000;
    private static final int AGGRO_RANGE_ID = 1100;
    private static final int FIRING_RANGE_ID = 1101;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final String TUNING = "wolffsmodnpcs.tuning.";
    private static final String PRESENTATION = "wolffsmodnpcs.presentation.";
    private static final String WEAPONS = "wolffsmodnpcs.weapons.";
    private static final int TAB_BUTTON_ID = 1300;
    private static final int SERVER_NEAR_ID = 1200;
    private static final int SERVER_FAR_ID = 1201;
    private static final int SERVER_PROFILE_ID = 1202;
    private static final int SERVER_BENCHMARK_ID = 1203;
    private static final int CLIENT_BENCHMARK_ID = 1204;
    /** Distance between the left edges of two overview columns; a page button is this wide less the gap. */
    private static final int OVERVIEW_COLUMN = 134;
    private static final int OVERVIEW_GAP = 6;
    private static final int OVERVIEW_HEADING_HEIGHT = 12;
    /** Where the content of a page starts, and where it starts below a row of tab buttons. */
    private static final int PAGE_TOP = 29;
    private static final int TABBED_TOP = 52;
    private static final int ROW_HEIGHT = 21;
    /** Width of the content of a page, which its tab buttons share. */
    private static final int PAGE_WIDTH = 396;
    /** Leftmost columns of the controls of a page of switches and of one that also holds numbers or choices. */
    private static final int SWITCH_COLUMN = 156;
    private static final int VALUE_COLUMN = 228;
    private static final int SOUND_COLUMN = 278;
    /** The tab of the Sounds page that lists the sound the NPC plays for each use and where it is set. */
    private static final String OVERVIEW_TAB = "effective";
    /**
     * The tab of the Sounds page for the sounds of standing still and moving: the engine controls for a vehicle or plane, the
     * model idle and step switch for any other NPC.
     */
    private static final String IDLE_MOVEMENT_TAB = "idle_movement";
    /** The tabs of the Protection page: its switches, the part hitboxes, and knockback with the minimum hits. */
    private static final String DAMAGE_TAB = "damage";
    private static final String PARTS_TAB = "parts";
    private static final String HITS_TAB = "hits";
    private static final String PATH_SEPARATOR = " > ";
    /** Room for the title of a page, which only Back follows. */
    private static final int TITLE_WIDTH = 340;
    /** What stands for the outer parts of a page path that do not fit. */
    private static final String TRIMMED = "..." + PATH_SEPARATOR;
    private static final String HELP_SUFFIX = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};

    /** A heading of the overview. The pages under the headings of one column follow each other in it. */
    @RequiredArgsConstructor
    private enum Section
    {
        COMBAT(0), MOVEMENT(1), LOOK(2), ADVANCED(2);

        private final int column;

        private String key()
        {
            return "wolffsmodnpcs.section." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** The two Custom NPCs ranges as rows, editable here up to the server's limit rather than the editor's. */
    @RequiredArgsConstructor
    private enum Range
    {
        AGGRO_RANGE(AGGRO_RANGE_ID), FIRING_RANGE(FIRING_RANGE_ID);

        private final int id;
    }

    /** The appearance switches as rows; they are not stored with the weapon options. */
    @RequiredArgsConstructor
    private enum Look
    {
        HURT_FLASH(HURT_FLASH_ID), DEATH_ROTATION(DEATH_ROTATION_ID), HIDE_BODY(HIDE_BODY_ID);

        private final int id;
    }

    /** A part of a page: its rows, listed top to bottom in the order given. Only the tabs of a page with several are named. */
    private record Tab(String name, List<Object> rows)
    {
        private static Tab of(Object... rows)
        {
            return new Tab("", List.of(rows));
        }

        private static Tab named(String name, Object... rows)
        {
            return new Tab(name, List.of(rows));
        }

        private String key()
        {
            return "wolffsmodnpcs.tab." + name;
        }
    }

    /** A page of the tab, listed on the overview under its section in the order given here. */
    private enum Category
    {
        // One page a line, one tab a line: the order here is the order on screen.
        // spotless:off
        EQUIPMENT(Section.COMBAT, Tab.of(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.DUAL_WIELD, Feature.CONSUME_AMMO, Value.WEAPON_SWITCH)),
        MODEL_STATS(Section.COMBAT, Tab.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS)),
        FIRING(Section.COMBAT, Tab.of(Feature.PROJECTILES, Feature.GRENADES, Feature.NATIVE_THROWABLES, Feature.SECONDARY_BANK, Feature.ALTERNATE_BARRELS, Feature.RELOAD_CYCLE,
            Feature.MODEL_MUZZLES, Feature.TURRET_MUZZLES)),
        AIMING(Section.COMBAT, Tab.of(Feature.LEAD_TARGET, Feature.ADVANCED_LEAD, Feature.BALLISTIC_AIM, Feature.FIRE_WHEN_ALIGNED, Value.ALIGN_TOLERANCE, Feature.EYE_AT_MUZZLE)),
        TURRET(Section.COMBAT, Tab.of(Feature.GRADUAL_AIM, Value.AIM_SPEED_YAW, Value.AIM_SPEED_PITCH, Feature.HULL_ASSISTS_AIM, Feature.PASSENGER_GUNS, Feature.TURRET_LOCKED)),
        PROTECTION(Section.COMBAT,
            Tab.named(DAMAGE_TAB, Feature.VEHICLE_DAMAGE, Feature.MODEL_ARMOUR, Feature.FLAN_DAMAGE_TYPES, Feature.INVULNERABLE),
            Tab.named(PARTS_TAB, Feature.PART_HITBOXES, Feature.PART_HEALTH, Feature.PART_EFFECTS, Feature.BROKEN_PARTS_PASS, Feature.PART_REPAIR, Feature.SOLID_HITBOXES, Feature.HITBOX_OUTLINES),
            Tab.named(HITS_TAB)),
        DETECTION(Section.MOVEMENT, Tab.of(Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.SIGHT, Value.VISION_LIMIT, Value.FIRE_LIMIT, Value.TARGET_MARGIN)),
        TERRAIN(Section.MOVEMENT, Tab.of(Value.TERRAIN, Value.MAX_WADE_DEPTH, Value.MIN_WATER_DEPTH, Value.LAND_SPEED, Value.WATER_SPEED)),
        DRIVING(Section.MOVEMENT,
            Tab.named("handling", Feature.DRIVING, Value.DRIVE_PRESET, Value.DRIVE_TYPE, Value.DRIVE_ACCELERATION, Value.DRIVE_BRAKING, Value.DRIVE_COASTING, Value.TURN_RADIUS),
            Tab.named("steering", Feature.HULL_FACES_TRAVEL, Value.HULL_TURN_RATE, Feature.MISALIGNED_THROTTLE, Value.THROTTLE_FLOOR, Value.DRIVE_LOOKAHEAD, Value.CORNER_SPEED),
            Tab.named("reversing", Feature.DRIVE_REVERSE, Value.DRIVE_REVERSE_SPEED, Value.REVERSE_ANGLE, Value.REVERSE_DISTANCE, Value.STOP_DISTANCE, Value.FOLLOW_DISTANCE)),
        FLIGHT(Section.MOVEMENT, Tab.of(Value.FLIGHT_TYPE, Value.ATTACK_ALTITUDE, Value.FLIGHT_SPEED, Value.SUBMARINE_DEPTH)),
        PATHFINDING(Section.MOVEMENT, Tab.of(Feature.ADAPTIVE_PATH_RANGE, Value.PATH_MARGIN, Value.PATH_RETRY_INTERVAL)),
        MODEL(Section.LOOK, Tab.of(Feature.ANIMATED_MODEL, Feature.DOORS_OPEN, Feature.GEAR_UP, Feature.WINGS_FOLDED, Feature.ENGINE_RUNNING, Feature.FULL_THROTTLE, Feature.RIDER_AT_SEAT)),
        ANIMATIONS(Section.LOOK, Tab.of(Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS, Feature.GUN_SHOT_ANIMATION, Feature.GUN_RELOAD_ANIMATION, Feature.GUN_MELEE_ANIMATION)),
        EFFECTS(Section.LOOK, Tab.of(Look.HURT_FLASH, Look.DEATH_ROTATION, Look.HIDE_BODY, Feature.SHOOT_PARTICLES)),
        SOUNDS(Section.LOOK,
            Tab.named(OVERVIEW_TAB),
            Tab.named("weapon", Feature.FIRE_SOUNDS, Feature.RELOAD_SOUNDS, Feature.THROW_SOUNDS, Feature.MELEE_SOUNDS),
            Tab.named(IDLE_MOVEMENT_TAB)),
        PERFORMANCE(Section.ADVANCED, Tab.of(Feature.PROGRESSIVE_SEARCH, Value.SEARCH_NEAR_INTERVAL, Value.SEARCH_FAR_INTERVAL, Feature.STAY_ACTIVE, Value.ACTIVE_RANGE, Feature.MODEL_CULLING,
            Feature.PATH_DEBUG)),
        /** Settings and diagnostics of the whole server rather than of the NPC being edited; its controls are not rows. */
        SERVER(Section.ADVANCED, Tab.of());
        // spotless:on

        private final Section section;
        private final List<Tab> tabs;

        Category(Section section, Tab... tabs)
        {
            this.section = section;
            this.tabs = List.of(tabs);
        }

        private String key()
        {
            return "wolffsmodnpcs.category." + name().toLowerCase(Locale.ROOT);
        }

        /** The line below the page: what its rows act on and how to read them. */
        private String hint()
        {
            return switch (this)
            {
                case EQUIPMENT, MODEL_STATS, FIRING -> WEAPONS + "ammo_hint";
                case PROTECTION, ANIMATIONS, EFFECTS, SOUNDS -> PRESENTATION + "hint";
                case SERVER -> TUNING + (ClientServerSettings.isKnown() ? "server_hint" : "server_waiting");
                default -> TUNING + "hint";
            };
        }
    }

    private boolean loaded;
    private boolean changed;
    private Category category;
    /** Which tab of a page with several is shown. */
    private int tab;
    private NpcEngineSettings editingSound;
    private final CombatPages combat;
    private final SoundOverview soundOverview;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        combat = new CombatPages(this, npc);
        soundOverview = new SoundOverview(this, npc);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
    }

    @Override
    public void init()
    {
        super.init();
        if (category == null)
            initOverview();
        else
            initCategory();
        addLabel(new GuiLabel(HINT_LABEL_ID, loaded ? hint() : WEAPONS + "loading", guiLeft + 8, guiTop + 203));
    }

    private String hint()
    {
        if (category == null)
            return TUNING + "overview_hint";
        return combat.active() ? combat.hint() : category.hint();
    }

    /** One column per section column, each section a heading followed by the buttons of its pages. */
    private void initOverview()
    {
        addLabel(new GuiLabel(TITLE_LABEL_ID, WEAPONS + "title", guiLeft + 8, guiTop + 10));
        Map<Integer, Integer> nextRow = new HashMap<>();
        for (Section section : Section.values())
        {
            int x = guiLeft + 8 + section.column * OVERVIEW_COLUMN;
            // A second section in a column keeps its distance from the pages above it.
            int y = nextRow.containsKey(section.column) ? nextRow.get(section.column) + OVERVIEW_GAP : PAGE_TOP;
            addLabel(new GuiLabel(SECTION_LABEL_ID + section.ordinal(), section.key(), x, guiTop + y));
            y += OVERVIEW_HEADING_HEIGHT;
            for (Category page : Category.values())
            {
                if (page.section != section)
                    continue;
                String key = page.key();
                GuiButtonNop button = new GuiButtonNop(this, CATEGORY_BUTTON_ID + page.ordinal(), x, guiTop + y, OVERVIEW_COLUMN - OVERVIEW_GAP, 20, new String[]{key}, 0);
                button.setEnabled(loaded);
                button.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
                addButton(button);
                y += ROW_HEIGHT;
            }
            nextRow.put(section.column, y);
        }
    }

    private void initCategory()
    {
        addLabel(new GuiLabel(TITLE_LABEL_ID, fitTitle(pageTitle(), combat.active() ? combat.titleWidth() : TITLE_WIDTH), guiLeft + 8, guiTop + 10));
        addButton(new GuiButtonNop(this, BACK_BUTTON_ID, guiLeft + 354, guiTop + 4, 50, 20, new String[]{"gui.back"}, 0));
        if (combat.active())
            combat.init(loaded);
        else if (editingSound != null)
            initSoundEditor();
        else if (category == Category.SERVER)
            initServer();
        else
            initPage();
    }

    /** @return the path to the page shown, from its section down to the sub-page that is open */
    private String pageTitle()
    {
        String path = I18n.get(category.section.key()) + PATH_SEPARATOR + I18n.get(category.key());
        if (combat.active())
            return path + PATH_SEPARATOR + I18n.get(combat.title());
        if (editingSound != null)
            return path + PATH_SEPARATOR + I18n.get(loopKey(editingSound == sounds().getIdle() ? "edit_idle" : "edit_movement"));
        return path;
    }

    /** Drops the outermost parts of a page path until it fits, so the page itself is always named. */
    static String fitTitle(String path, int width)
    {
        String title = path;
        while (Minecraft.getInstance().font.width(title) > width && title.indexOf(PATH_SEPARATOR) != title.lastIndexOf(PATH_SEPARATOR))
            title = TRIMMED + title.substring(title.indexOf(PATH_SEPARATOR, title.startsWith(TRIMMED) ? TRIMMED.length() : 0) + PATH_SEPARATOR.length());
        return title;
    }

    /** The tab buttons of a page with several tabs, the rows of the tab shown and what the page adds below them. */
    private void initPage()
    {
        List<Tab> tabs = category.tabs;
        tab = Math.max(0, Math.min(tabs.size() - 1, tab));
        int top = PAGE_TOP;
        if (tabs.size() > 1)
        {
            int width = (PAGE_WIDTH + 2) / tabs.size() - 2;
            for (int i = 0; i < tabs.size(); i++)
            {
                GuiButtonNop button = new GuiButtonNop(this, TAB_BUTTON_ID + i, guiLeft + 8 + i * (width + 2), guiTop + PAGE_TOP, width, 20, new String[]{tabs.get(i).key()}, 0);
                button.setEnabled(loaded && i != tab);
                addButton(button);
            }
            top = TABBED_TOP;
        }
        Tab shown = tabs.get(tab);
        if (initSoundTab(shown, top))
            return;
        List<Object> rows = shown.rows().stream().filter(this::visible).toList();
        int control = initRows(rows, top);
        int below = top + rows.size() * ROW_HEIGHT;
        if (category == Category.EQUIPMENT)
            combat.initEquipmentLink(loaded, below + 6);
        else if (category == Category.MODEL_STATS && enabled(Feature.TYPE_PROPERTIES))
            combat.initPropertiesLink(loaded, below + 6);
        else if (category == Category.MODEL_STATS)
            // Everything else on the page reads the model, so nothing else is offered until inheritance is on.
            addLabel(new GuiLabel(TITLE_LABEL_ID + 2, "wolffsmodnpcs.combat.properties_disabled", guiLeft + 8, guiTop + below + 6));
        else if (category == Category.PROTECTION && HITS_TAB.equals(shown.name()))
            combat.initProtection(loaded, control, below);
        else if (category == Category.PROTECTION && PARTS_TAB.equals(shown.name()))
            combat.initPartsLink(loaded);
    }

    /** @return whether the tab is one of the Sounds page's tabs that are not a list of rows, which is now laid out */
    private boolean initSoundTab(Tab shown, int top)
    {
        if (OVERVIEW_TAB.equals(shown.name()))
            soundOverview.init(loaded, top);
        else if (IDLE_MOVEMENT_TAB.equals(shown.name()))
            initIdleMovement(guiTop + top);
        else
            return false;
        return true;
    }

    /** A row that only means something while another setting is on is left out until then. */
    private boolean visible(Object row)
    {
        if (row == Value.WEAPON_SWITCH)
            return enabled(Feature.CONSUME_AMMO);
        return row != Feature.MODEL_WEAPON_STATS || enabled(Feature.TYPE_PROPERTIES);
    }

    private boolean enabled(Feature feature)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
    }

    /**
     * Lays out the rows one below the other; their controls share a column that clears the longest label.
     *
     * @return the left edge of that column on screen
     */
    private int initRows(List<Object> rows, int top)
    {
        boolean wide = rows.stream().anyMatch(row -> row instanceof Value || row instanceof Range);
        int column = wide ? VALUE_COLUMN : SWITCH_COLUMN;
        for (Object row : rows)
            column = Math.max(column, 8 + Minecraft.getInstance().font.width(I18n.get(labelKey(row))) + 8);
        int control = guiLeft + column;
        for (int i = 0; i < rows.size(); i++)
        {
            int y = guiTop + top + i * ROW_HEIGHT;
            Object row = rows.get(i);
            if (row instanceof Feature feature)
                featureRow(feature, control, y);
            else if (row instanceof Value value)
                valueRow(value, control, y);
            else if (row instanceof Range range)
                rangeRow(range, control, y);
            else if (row instanceof Look look)
                toggle(look.id, look.name().toLowerCase(Locale.ROOT), lookEnabled(look), control, y);
        }
        return control;
    }

    private static String labelKey(Object row)
    {
        String name = ((Enum<?>) row).name().toLowerCase(Locale.ROOT);
        if (row instanceof Feature)
            return WEAPONS + name;
        return (row instanceof Look ? PRESENTATION : TUNING) + name;
    }

    private boolean lookEnabled(Look look)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        return switch (look)
        {
            case HURT_FLASH -> settings.isHurtFlash();
            case DEATH_ROTATION -> settings.isDeathRotation();
            case HIDE_BODY -> npc.stats.hideKilledBody;
        };
    }

    private void featureRow(Feature feature, int control, int y)
    {
        String key = labelKey(feature);
        addLabel(new GuiLabel(feature.ordinal(), key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        boolean enabled = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
        GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), control, y, 48, 20, NO_YES, enabled ? 1 : 0);
        button.setEnabled(loaded);
        addButton(button);
    }

    private void valueRow(Value value, int control, int y)
    {
        String key = labelKey(value);
        int id = VALUE_ID + value.ordinal();
        int current = NpcTuning.of(npc).get(value);
        addLabel(new GuiLabel(id, key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        Enum<?>[] choices = choices(value);
        if (choices.length > 0)
        {
            String[] names = new String[choices.length];
            for (int choice = 0; choice < choices.length; choice++)
                names[choice] = key + "." + choices[choice].name().toLowerCase(Locale.ROOT);
            GuiButtonNop button = new GuiButtonNop(this, id, control, y, 150, 20, names, current);
            button.setEnabled(loaded);
            addButton(button);
            return;
        }
        numberField(id, control, y, current, value.minimum(), value.maximum(), value.fallback());
    }

    /** @return the named choices of a value that is one, or none for a number */
    private static Enum<?>[] choices(Value value)
    {
        return switch (value)
        {
            case TERRAIN -> NpcTuning.Terrain.values();
            case SIGHT -> NpcTuning.Sight.values();
            case DRIVE_PRESET -> NpcTuning.DrivePreset.values();
            case DRIVE_TYPE -> NpcTuning.DriveType.values();
            case FLIGHT_TYPE -> NpcTuning.Flight.values();
            case WEAPON_SWITCH -> NpcTuning.WeaponSwitch.values();
            default -> new Enum<?>[0];
        };
    }

    /** Whether editing the value by hand means the vehicle no longer follows its preset. */
    private static boolean presetValue(Value value)
    {
        return switch (value)
        {
            case DRIVE_TYPE, DRIVE_ACCELERATION, DRIVE_BRAKING, DRIVE_COASTING, TURN_RADIUS, DRIVE_LOOKAHEAD, CORNER_SPEED, HULL_TURN_RATE -> true;
            default -> false;
        };
    }

    private void editValue(Value value, int number)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        if (value == Value.DRIVE_PRESET)
            tuning.applyPreset(NpcTuning.DrivePreset.values()[Math.max(0, Math.min(NpcTuning.DrivePreset.values().length - 1, number))]);
        else
        {
            tuning.set(value, number);
            if (presetValue(value))
                tuning.set(Value.DRIVE_PRESET, NpcTuning.DrivePreset.CUSTOM.ordinal());
        }
    }

    /** The settings and diagnostics of the whole server. The server sends the values and decides who may change them. */
    private void initServer()
    {
        boolean known = ClientServerSettings.isKnown();
        serverField(SERVER_NEAR_ID, "server_near_interval", ClientServerSettings.getNearInterval(), NpcServerSettings.MAX_NEAR_INTERVAL, 29, known);
        serverField(SERVER_FAR_ID, "server_far_interval", ClientServerSettings.getFarInterval(), NpcServerSettings.MAX_FAR_INTERVAL, 50, known);
        String profile = TUNING + "server_profiling";
        addLabel(new GuiLabel(SERVER_PROFILE_ID, profile, guiLeft + 8, guiTop + 76, profile + HELP_SUFFIX));
        GuiButtonNop toggle = new GuiButtonNop(this, SERVER_PROFILE_ID, guiLeft + 228, guiTop + 71, 48, 20, NO_YES, ClientServerSettings.isProfiling() ? 1 : 0);
        toggle.setEnabled(known);
        addButton(toggle);
        benchmarkButton(SERVER_BENCHMARK_ID, "server_benchmark", 104);
        benchmarkButton(CLIENT_BENCHMARK_ID, "client_benchmark", 128);
    }

    private void serverField(int id, String name, int current, int maximum, int y, boolean known)
    {
        String key = TUNING + name;
        addLabel(new GuiLabel(id, key, guiLeft + 8, guiTop + y + 5, key + HELP_SUFFIX));
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, guiLeft + 228, guiTop + y, 60, 20, Integer.toString(current));
        field.setEditable(known);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(1, maximum, current);
        addTextField(field);
    }

    private void benchmarkButton(int id, String name, int y)
    {
        String key = TUNING + name;
        GuiButtonNop button = new GuiButtonNop(this, id, guiLeft + 8, guiTop + y, 268, 20, new String[]{key}, 0);
        button.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
        addButton(button);
    }

    /** @return whether the field or button belonged to the Server page */
    private boolean editServer(int id, int number)
    {
        int near = ClientServerSettings.getNearInterval();
        int far = ClientServerSettings.getFarInterval();
        boolean profiling = ClientServerSettings.isProfiling();
        switch (id)
        {
            case SERVER_NEAR_ID -> near = number;
            case SERVER_FAR_ID -> far = number;
            case SERVER_PROFILE_ID -> profiling = number == 1;
            case SERVER_BENCHMARK_ID -> {
                NpcNetwork.sendToServer(ServerControlPacket.benchmark(ServerBenchmark.DEFAULT_SECONDS));
                return true;
            }
            case CLIENT_BENCHMARK_ID -> {
                // A benchmark does not measure a screen: the editor closes first.
                close();
                ClientBenchmark.start(ServerBenchmark.DEFAULT_SECONDS);
                return true;
            }
            default -> {
                return false;
            }
        }
        NpcNetwork.sendToServer(new ServerControlPacket(ServerControlPacket.Action.SET, near, far, profiling, 0));
        return true;
    }

    private void rangeRow(Range range, int control, int y)
    {
        String key = labelKey(range);
        int current = range == Range.AGGRO_RANGE ? npc.stats.aggroRange : npc.stats.ranged.getRange();
        addLabel(new GuiLabel(range.id, key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        numberField(range.id, control, y, current, 1, NpcMobility.rangeLimit(), current);
    }

    private void numberField(int id, int x, int y, int current, int minimum, int maximum, int fallback)
    {
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, x, y, 60, 20, Integer.toString(current));
        field.setEditable(loaded);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(minimum, maximum, fallback);
        addTextField(field);
    }

    /** A switch of the appearance and sound settings, at the given position on screen. */
    private void toggle(int id, String name, boolean value, int control, int y)
    {
        switchRow(id, PRESENTATION + name, value, control, y);
    }

    private void switchRow(int id, String key, boolean value, int control, int y)
    {
        addLabel(new GuiLabel(id, key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        GuiButtonNop button = new GuiButtonNop(this, id, control, y, 48, 20, NO_YES, value ? 1 : 0);
        button.setEnabled(loaded);
        addButton(button);
    }

    private NpcVehicleSounds sounds()
    {
        return NpcPresentation.of(npc).getVehicleSounds();
    }

    /** @return the key of a looped-sound control: an engine's for a vehicle or plane, a plain loop's for any other NPC */
    private String loopKey(String name)
    {
        return PRESENTATION + (NpcPresentation.vehicleType(npc).isPresent() ? "" : "loop_") + name;
    }

    /**
     * The Idle & movement tab: the looped idle and movement sounds, whose two buttons open the editor of one each. A vehicle
     * or plane loops its engine and may keep its definition's sounds; any other NPC may loop sounds of its own over its
     * model's idle and step sounds, whose switch takes the place of the vehicle's defaults, with a button to where they are set.
     */
    private void initIdleMovement(int y)
    {
        NpcVehicleSounds settings = sounds();
        boolean vehicle = NpcPresentation.vehicleType(npc).isPresent();
        int control = guiLeft + SOUND_COLUMN;
        loopToggle(IDLE_ENABLED_ID, "idle_enabled", settings.isIdleEnabled(), control, y);
        loopToggle(MOVEMENT_ENABLED_ID, "movement_enabled", settings.isMovementEnabled(), control, y + 24);
        if (settings.isMovementEnabled())
            toggle(VARIABLE_PITCH_ID, "variable_pitch", settings.isVariablePitch(), control, y + 48);
        if (vehicle)
            toggle(MODEL_DEFAULTS_ID, "model_defaults", settings.isModelDefaults(), control, y + 72);
        else
        {
            featureRow(Feature.MODEL_SOUNDS, control, y + 72);
            // The tab is full, so the button sits in the title row, left of Back.
            String key = PRESENTATION + "model_sounds_link";
            GuiButtonNop link = new GuiButtonNop(this, MODEL_SOUNDS_LINK_ID, guiLeft + 236, guiTop + 4, 114, 20, new String[]{key}, 0);
            link.setEnabled(loaded);
            link.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
            addButton(link);
        }
        addButton(new GuiButtonNop(this, EDIT_IDLE_ID, guiLeft + 8, y + 100, 220, 20, new String[]{loopKey("edit_idle")}, 0));
        addButton(new GuiButtonNop(this, EDIT_MOVEMENT_ID, guiLeft + 8, y + 124, 220, 20, new String[]{loopKey("edit_movement")}, 0));
    }

    /** A switch of a looped sound, named for an engine or a plain loop. */
    private void loopToggle(int id, String name, boolean value, int control, int y)
    {
        switchRow(id, loopKey(name), value, control, y);
    }

    private void initSoundEditor()
    {
        boolean editable = !sounds().isModelDefaults();
        String sound = editingSound.getSound();
        addLabel(new GuiLabel(ENGINE_SOUND_ID, PRESENTATION + "engine_sound", guiLeft + 8, guiTop + 34));
        GuiTextFieldNop soundField = new GuiTextFieldNop(ENGINE_SOUND_FIELD_ID, this, guiLeft + 8, guiTop + 53, 252, 20, sound);
        soundField.setMaxLength(256);
        soundField.setEditable(false);
        soundField.setTextColorUneditable(READ_ONLY_COLOR);
        addTextField(soundField);
        addButton(new GuiButtonNop(this, ENGINE_SOUND_ID, guiLeft + 266, guiTop + 53, 72, 20, new String[]{PRESENTATION + "select_sound"}, 0));
        addButton(new GuiButtonNop(this, CLEAR_SOUND_ID, guiLeft + 344, guiTop + 53, 60, 20, new String[]{PRESENTATION + "clear_sound"}, 0));
        getButton(ENGINE_SOUND_ID).setEnabled(loaded && editable);
        getButton(CLEAR_SOUND_ID).setEnabled(loaded && editable);
        engineField(ENGINE_RANGE_ID, "engine_range", editingSound.getRange(), 81, 4096, editable);
        if (sound.startsWith("flansmod:"))
        {
            toggle(OVERRIDE_REPEAT_ID, "override_repeat", editingSound.isOverrideRepeat(), guiLeft + SOUND_COLUMN, guiTop + 105);
            getButton(OVERRIDE_REPEAT_ID).setEnabled(loaded && editable);
        }
        engineField(ENGINE_REPEAT_ID, "engine_repeat", editingSound.effectiveRepeatTicks(), 129, 12000, editable && !editingSound.automaticRepeat());
        if (!editable)
            addLabel(new GuiLabel(601, PRESENTATION + "vehicle_read_only", guiLeft + 8, guiTop + 180));
        addLabel(new GuiLabel(600, editingSound.automaticRepeat() ? PRESENTATION + "repeat_automatic" : PRESENTATION + "repeat_manual", guiLeft + 8, guiTop + 160));
    }

    private void engineField(int id, String name, int value, int y, int maximum, boolean enabled)
    {
        String key = PRESENTATION + name;
        addLabel(new GuiLabel(id, key, guiLeft + 8, guiTop + y + 5, key + HELP_SUFFIX));
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, guiLeft + 278, guiTop + y, 70, 20, Integer.toString(value));
        // Custom NPCs hides disabled fields entirely; EditBox's editability keeps read-only values visible.
        field.setEditable(loaded && enabled);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(1, maximum, value);
        addTextField(field);
    }

    @Override
    public void subGuiClosed(Screen subgui)
    {
        if (subgui instanceof GuiSoundSelection picker)
        {
            if (combat.active())
                changed |= combat.soundChosen(picker.selectedResource);
            else if (picker.selectedResource != null && editingSound != null && !sounds().isModelDefaults())
            {
                editingSound.setSound(picker.selectedResource.toString());
                changed = true;
            }
        }
        init();
    }

    @Override
    public void unFocused(GuiTextFieldNop field)
    {
        if (loaded && combat.unFocused(field))
        {
            changed = true;
            return;
        }
        if (field.isInteger() && editServer(field.id, field.getInteger()))
            return;
        if (loaded && editNumber(field))
        {
            changed = true;
            return;
        }
        if (!loaded || editingSound == null || sounds().isModelDefaults())
            return;
        switch (field.id)
        {
            case ENGINE_RANGE_ID -> editingSound.setRange(field.isInteger() ? field.getInteger() : 1);
            case ENGINE_REPEAT_ID -> {
                if (editingSound.automaticRepeat())
                    return;
                editingSound.setRepeatTicks(field.isInteger() ? field.getInteger() : 20);
            }
            default -> {
                return;
            }
        }
        changed = true;
    }

    /** @return whether the field was one of the simulation numbers or ranges */
    private boolean editNumber(GuiTextFieldNop field)
    {
        if (!field.isInteger())
            return false;
        if (field.id >= VALUE_ID && field.id < VALUE_ID + Value.values().length)
            editValue(Value.values()[field.id - VALUE_ID], field.getInteger());
        else if (field.id == AGGRO_RANGE_ID)
            npc.stats.aggroRange = Math.max(1, Math.min(NpcMobility.rangeLimit(), field.getInteger()));
        else if (field.id == FIRING_RANGE_ID)
            npc.stats.ranged.setRange(Math.max(1, Math.min(NpcMobility.rangeLimit(), field.getInteger())));
        else
            return false;
        return true;
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (!loaded)
            return;
        GuiTextFieldNop.unfocus();
        if (editServer(button.id, button.getValue()))
            return;
        if (navigate(button))
        {
            init();
            return;
        }
        String selection = combat.active() ? combat.soundSelection(button.id) : null;
        if (selection != null)
        {
            openSoundPicker(selection);
            return;
        }
        if (button.id >= 0 && button.id < Feature.values().length)
        {
            ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.values()[button.id], button.getValue() == 1);
            changed = true;
        }
        else if (button.id >= VALUE_ID && button.id < VALUE_ID + Value.values().length)
        {
            editValue(Value.values()[button.id - VALUE_ID], button.getValue());
            changed = true;
        }
        else if (combat.handles(button.id))
            changed |= combat.buttonEvent(button);
        else if (button.id == ENGINE_SOUND_ID)
        {
            openSoundPicker(editingSound.getSound());
            return;
        }
        else
        {
            editSettings(button);
            changed = true;
        }
        init();
    }

    /** Opens the Custom NPCs sound picker on the given sound; the page that asked takes the choice when it closes. */
    private void openSoundPicker(String selection)
    {
        // Select the namespace without committing an empty-path placeholder when the picker closes.
        GuiSoundSelection picker = new GuiSoundSelection(selection.isBlank() ? "flansmod:" : selection);
        if (selection.isBlank())
            picker.selectedResource = null;
        setSubGui(picker);
    }

    /** @return whether the button only moved between pages */
    private boolean navigate(GuiButtonNop button)
    {
        if (button.id == BACK_BUTTON_ID)
            goBack();
        else if (category == Category.SOUNDS && !combat.active() && soundOverview.handles(button.id))
            openSoundSource(soundOverview.target(button.id));
        else if (button.id == MODEL_SOUNDS_LINK_ID)
            combat.openPropertySounds();
        else if (button.id >= CATEGORY_BUTTON_ID && button.id < CATEGORY_BUTTON_ID + Category.values().length)
            openCategory(Category.values()[button.id - CATEGORY_BUTTON_ID]);
        else if (category != null && button.id >= TAB_BUTTON_ID && button.id < TAB_BUTTON_ID + category.tabs.size())
            tab = button.id - TAB_BUTTON_ID;
        else
            return false;
        return true;
    }

    /** Goes from the Effective sounds tab to the page that sets a sound; Back returns to the tab. */
    private void openSoundSource(SoundOverview.Target target)
    {
        switch (target)
        {
            case MAIN_HAND -> combat.openEquipment(CombatPages.Page.MAIN_HAND, EquipmentProperty.Group.FIRING);
            case OFF_HAND -> combat.openEquipment(CombatPages.Page.OFF_HAND, EquipmentProperty.Group.OFF_HAND_GUN);
            case PROPERTIES -> combat.openPropertySounds();
            case ENGINE -> {
                List<Tab> tabs = Category.SOUNDS.tabs;
                for (int i = 0; i < tabs.size(); i++)
                    if (IDLE_MOVEMENT_TAB.equals(tabs.get(i).name()))
                        tab = i;
            }
        }
    }

    private void openCategory(Category opened)
    {
        category = opened;
        tab = 0;
        if (opened == Category.SERVER)
        {
            // The page shows what the server answers, not what an earlier visit left behind.
            ClientServerSettings.forget();
            NpcNetwork.sendToServer(ServerControlPacket.request());
        }
    }

    private void goBack()
    {
        if (combat.back())
            return;
        if (editingSound != null)
            editingSound = null;
        else
            category = null;
    }

    private void editSettings(GuiButtonNop button)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        boolean yes = button.getValue() == 1;
        switch (button.id)
        {
            case HURT_FLASH_ID -> settings.setHurtFlash(yes);
            case DEATH_ROTATION_ID -> settings.setDeathRotation(yes);
            case HIDE_BODY_ID -> npc.stats.hideKilledBody = yes;
            case IDLE_ENABLED_ID -> sounds().setIdleEnabled(yes);
            case MOVEMENT_ENABLED_ID -> sounds().setMovementEnabled(yes);
            case VARIABLE_PITCH_ID -> sounds().setVariablePitch(yes);
            case MODEL_DEFAULTS_ID -> {
                if (yes)
                    sounds().useVehicleDefaults(npc);
                else
                {
                    sounds().setModelDefaults(false);
                    sounds().initializeCustom(npc);
                }
            }
            case EDIT_IDLE_ID, EDIT_MOVEMENT_ID -> {
                if (sounds().isModelDefaults() && NpcPresentation.vehicleType(npc).isPresent())
                    sounds().useVehicleDefaults(npc);
                else
                {
                    // Without a vehicle there are no defaults to keep: the loops are the NPC's own and edited at once.
                    sounds().setModelDefaults(false);
                    sounds().initializeCustom(npc);
                }
                editingSound = button.id == EDIT_IDLE_ID ? sounds().getIdle() : sounds().getMovement();
            }
            case CLEAR_SOUND_ID -> editingSound.setSound("");
            case OVERRIDE_REPEAT_ID -> {
                int interval = editingSound.effectiveRepeatTicks();
                editingSound.setOverrideRepeat(yes);
                if (yes)
                    editingSound.setRepeatTicks(interval);
            }
            default -> {
                /* Other GUI buttons belong to the native tab base. */ }
        }
    }

    @Override
    public void setGuiData(CompoundTag tag)
    {
        npc.stats.readToNBT(tag);
        loaded = true;
        editingSound = null;
        init();
    }

    public static GuiMenuTopButton createTab(IGuiInterface parent, EntityNPCInterface npc, int x, int y)
    {
        return new GuiMenuTopButton(parent, MENU_ID, x, y, "menu.wolffsmodnpcs")
        {
            @Override
            public void onClick(double mouseX, double mouseY)
            {
                GuiTextFieldNop.unfocus();
                parent.save();
                NoppesUtil.openGUI(Minecraft.getInstance().player, new GuiWolffsMod(npc));
            }
        };
    }

    @Override
    public void save()
    {
        GuiTextFieldNop.unfocus();
        if (loaded && changed)
        {
            Packets.sendServer(new SPacketMenuSave(EnumMenuType.STATS, npc.stats.save(new CompoundTag())));
            combat.saved();
            changed = false;
        }
    }
}
