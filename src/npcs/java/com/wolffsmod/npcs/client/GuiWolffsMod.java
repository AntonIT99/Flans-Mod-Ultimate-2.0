package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.NpcKind.Scope;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.diag.*;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.presets.NpcPreset;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.teams.FriendlyFire;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Per-NPC controls inside the Wolff's Mod tab, using the Custom NPCs stats transport and sound picker.
 *
 * <p>
 * The overview is split into hub tabs along its top: General, Combat, Movement and Advanced list pages under section
 * headings, one column per heading, below the kind of NPC being edited, a standard NPC or a Flan model; the pages of Server
 * hold the settings of the whole server instead. Locomotion lists only pages meant for this kind; Boarding & piloting uses
 * expandable sections in a scrolling viewport. Other pages are all listed, and those not meant for the kind are greyed.
 * A page is a list of rows, split into named tabs where they do not fit together, and may link to a sub-page. The title of a page is the path
 * to it. The Essentials page gathers the settings most NPCs of the kind need. A row that does nothing for this NPC,
 * because of its kind or because a setting it depends on is off, keeps its control but has a grey label that says why; a
 * row that depends on another row of the same tab is indented below it. A hub tab, page or tab with settings changed from
 * their defaults shows how many. A few pages show lines the server sends instead of rows: what the server finds of this NPC,
 * the census of every NPC, and the latest server benchmarks side by side.
 * </p>
 */
// Custom NPCs' editor requires the external GUI inheritance chain.
@SuppressWarnings("java:S110")
public class GuiWolffsMod extends GuiNPCInterface2 implements IGuiData, ITextfieldListener, ISliderListener
{
    public static final int MENU_ID = 100;
    private static final int TITLE_LABEL_ID = 100;
    private static final int HINT_LABEL_ID = 101;
    private static final int SECTION_LABEL_ID = 120;
    private static final int CATEGORY_BUTTON_ID = 200;
    private static final int BACK_BUTTON_ID = 300;
    private static final int ESSENTIAL_LINK_ID = 310;
    private static final int PRESET_ID = 320;
    private static final int PRESET_APPLY_ID = 321;
    private static final int PRESET_UNDO_ID = 322;
    private static final int HUB_BUTTON_ID = 330;
    private static final int HURT_FLASH_ID = 400;
    private static final int DEATH_ROTATION_ID = 401;
    private static final int HIDE_BODY_ID = 402;
    private static final int KEEP_WRECK_ID = 403;
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
    // 2200-2239 belong to held-item animations and 2300-2315 to crew controls, whose handlers run before page navigation.
    private static final int CONTROLLER_ID = 2400;
    private static final int MOVEMENT_SECTION_ID = 2410;
    private static final int MOVEMENT_UP_ID = 2430;
    private static final int MOVEMENT_DOWN_ID = 2431;
    private static final int MOVEMENT_TOP = 45;
    private static final int MOVEMENT_VISIBLE_ROWS = 7;
    private static final int MOVEMENT_SCROLL_LEFT = 390;
    private static final String LOCOMOTION = "wolffsmodnpcs.locomotion.";
    private static final int AGGRO_RANGE_ID = 1100;
    private static final int FIRING_RANGE_ID = 1101;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    /** The colour Custom NPCs gives the help tooltip of a label, kept for the help below the reason a row is grey. */
    private static final Style HELP_STYLE = Style.EMPTY.withColor(0xFFC75C);
    private static final String TUNING = "wolffsmodnpcs.tuning.";
    private static final String PRESENTATION = "wolffsmodnpcs.presentation.";
    private static final String WEAPONS = "wolffsmodnpcs.weapons.";
    private static final String OVERVIEW = "wolffsmodnpcs.overview.";
    private static final String NEEDS = "wolffsmodnpcs.needs.";
    private static final int TAB_BUTTON_ID = 1300;
    private static final int SERVER_NEAR_ID = 1200;
    private static final int SERVER_FAR_ID = 1201;
    private static final int SERVER_PROFILE_ID = 1202;
    private static final int SERVER_BENCHMARK_ID = 1203;
    private static final int CLIENT_BENCHMARK_ID = 1204;
    private static final int SERVER_AGGRO_ID = 1205;
    private static final int SERVER_FIRING_ID = 1206;
    private static final int SERVER_APPLY_RANGES_ID = 1207;
    /** The labels beside an NPC's own search intervals naming the server's, which an interval of 0 follows. */
    private static final int SERVER_NEAR_VALUE_ID = 1208;
    private static final int SERVER_FAR_VALUE_ID = 1209;
    private static final int SERVER_MODEL_AGGRO_ID = 1210;
    private static final int SERVER_MODEL_FIRING_ID = 1211;
    private static final int SERVER_SEARCH_BUDGET_ID = 1212;
    private static final int SERVER_PATH_BUDGET_ID = 1213;
    private static final int SERVER_STAY_ACTIVE_LIMIT_ID = 1214;
    private static final int SERVER_FREEZE_ID = 1215;
    private static final int SERVER_HEALTH_ID = 1216;
    private static final int SERVER_AMMO_ID = 1217;
    private static final int SERVER_NAME_TAG_RANGE_ID = 1221;
    private static final int SERVER_APPLY_SEARCH_ID = 1218;
    private static final int REPORT_REFRESH_ID = 1219;
    private static final int DIAGNOSIS_COPY_ID = 1220;
    private static final int SERVER_SHOW_NAME_TAGS_ID = 1222;
    private static final int SERVER_HIDE_NAME_TAGS_ID = 1223;
    private static final int SERVER_FRIENDLY_FIRE_NPCS_ID = 1224;
    private static final int SERVER_FRIENDLY_FIRE_PLAYERS_ID = 1225;
    private static final int SERVER_ITEM_DROPS_ID = 1226;
    /** The first of the labels a page of read-only lines lays out, one per line or table cell. */
    private static final int LINE_LABEL_ID = 1500;
    /** Room for the lines of a page of read-only lines. */
    private static final int LINE_HEIGHT = 11;
    private static final int MAX_LINES = 14;
    /** Where the buttons below a page of read-only lines sit, from the top of the GUI. */
    private static final int LINES_BUTTON_TOP = 178;
    /** Client ticks between two requests of what the Diagnosis page shows. */
    private static final int INSPECT_INTERVAL = 20;
    /** The left edges of the columns of the benchmark comparison: the figure, the previous run, the latest run and the change. */
    private static final int[] COMPARISON_COLUMNS = {8, 214, 278, 342};
    private static final String DIAGNOSIS = "wolffsmodnpcs.diagnosis.";
    private static final String CENSUS = "wolffsmodnpcs.census.";
    private static final String COMPARISON = "wolffsmodnpcs.comparison.";
    private static final String WAITING = "waiting";
    /** The tabs of the Diagnosis page, which lay out the lines the server sent rather than rows. */
    private static final String EFFECTIVE_TAB = "effective_values";
    private static final String LIVE_TAB = "live_stats";
    /** The tab of the Profiling & benchmarks page that sets the latest server benchmark beside the one before. */
    private static final String COMPARISON_TAB = "benchmark_comparison";
    private static final int GOOD_COLOR = 0x55FF55;
    private static final int BAD_COLOR = 0xFF5555;
    /** Distance between the left edges of two overview columns; a page button is this wide less the gap. */
    private static final int OVERVIEW_COLUMN = 134;
    private static final int OVERVIEW_GAP = 6;
    private static final int OVERVIEW_HEADING_HEIGHT = 12;
    /** The line naming the kind of NPC, above the columns of a hub tab. */
    private static final int OVERVIEW_KIND_HEIGHT = 14;
    /** Width of a hub tab button; Server stands apart at the right end of the row. */
    private static final int HUB_WIDTH = 76;
    /** The title row of the Essentials page: its title, then a link to each full page, then Back. */
    private static final int LINK_WIDTH = 110;
    /** Where the content of a page starts, and where it starts below a row of tab buttons. */
    private static final int PAGE_TOP = 29;
    private static final int TABBED_TOP = 52;
    private static final int ROW_HEIGHT = 21;
    /** How far a row that depends on the row above it is moved right, per level. */
    private static final int INDENT = 10;
    /** Width of the content of a page, which its tab buttons share. */
    private static final int PAGE_WIDTH = 396;
    /** Leftmost columns of the controls of a page of switches and of one that also holds numbers or choices. */
    private static final int SWITCH_COLUMN = 156;
    private static final int VALUE_COLUMN = 228;
    /** The least left edge of the right one of two columns of rows, past the middle of the page, and its distance from the left one's controls. */
    private static final int COLUMN_LEFT = 8 + PAGE_WIDTH / 2 + 4;
    private static final int COLUMN_GAP = 12;
    private static final int SOUND_COLUMN = 278;
    /** The tab of the Sounds page that lists the sound the NPC plays for each use and where it is set. */
    private static final String OVERVIEW_TAB = "effective";
    /**
     * The tab of the Sounds page for the sounds of standing still and moving: the engine controls for a vehicle or plane, the
     * model idle and step switch for any other NPC.
     */
    private static final String IDLE_MOVEMENT_TAB = "idle_movement";
    /** The tabs of the Damage & armour page: what any NPC takes with knockback and the minimum hits, the model's armour, and its parts. */
    private static final String GENERAL_TAB = "general";
    private static final String SERVER_RANGES_TAB = "ranges";
    private static final String ARMOUR_TAB = "model_armour";
    private static final String PARTS_TAB = "parts";
    private static final String PATH_SEPARATOR = " > ";
    /** Room for the title of a page, which only Back follows. */
    private static final int TITLE_WIDTH = 340;
    /** What stands for the outer parts of a page path that do not fit. */
    private static final String TRIMMED = "..." + PATH_SEPARATOR;
    private static final String HELP_SUFFIX = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    /** The landing gear row is a choice of three, kept as two switches: gear up, and dynamic gear, which outranks it. */
    private static final String[] GEAR_CHOICES = {WEAPONS + "gear_up.down", WEAPONS + "gear_up.up", WEAPONS + "gear_up.dynamic"};
    private static final int GEAR_UP_CHOICE = 1;
    private static final int GEAR_DYNAMIC_CHOICE = 2;
    /** The hub tab the overview shows; kept while the game runs. */
    private static final AtomicReference<Hub> HUB = new AtomicReference<>(Hub.GENERAL);
    private static final String PRESET = "wolffsmodnpcs.preset.";

    /** A tab of the overview, in the row along its top. The pages of Server hold the settings of the whole server instead of this NPC's. */
    private enum Hub
    {
        GENERAL, COMBAT, MOVEMENT, ADVANCED, SERVER;

        private String key()
        {
            return "wolffsmodnpcs.hub." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** A heading of a hub tab, over one column of page buttons; a hub tab with one section shows no heading. */
    @RequiredArgsConstructor
    private enum Section
    {
        // One hub tab a line, its columns left to right.
        // spotless:off
        START(Hub.GENERAL, 0), MODEL(Hub.GENERAL, 1), LOOK(Hub.GENERAL, 2),
        WEAPONS(Hub.COMBAT, 0), TARGETING(Hub.COMBAT, 1), PROTECTION(Hub.COMBAT, 2),
        BEHAVIOUR(Hub.MOVEMENT, 0), TRAVEL(Hub.MOVEMENT, 1),
        ADVANCED(Hub.ADVANCED, 0),
        SERVER(Hub.SERVER, 0);
        // spotless:on

        private final Hub hub;
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

    /** The settings of the whole server as rows. The server sends their values and decides who may change them. */
    @RequiredArgsConstructor
    private enum ServerRow
    {
        // spotless:off
        NEAR_INTERVAL(SERVER_NEAR_ID, Setting.NEAR_INTERVAL), FAR_INTERVAL(SERVER_FAR_ID, Setting.FAR_INTERVAL), SEARCH_BUDGET(SERVER_SEARCH_BUDGET_ID, Setting.SEARCH_BUDGET),
        PROFILING(SERVER_PROFILE_ID, Setting.PROFILING),
        AGGRO_RANGE(SERVER_AGGRO_ID, Setting.AGGRO_RANGE), FIRING_RANGE(SERVER_FIRING_ID, Setting.FIRING_RANGE),
        MODEL_AGGRO_RANGE(SERVER_MODEL_AGGRO_ID, Setting.MODEL_AGGRO_RANGE), MODEL_FIRING_RANGE(SERVER_MODEL_FIRING_ID, Setting.MODEL_FIRING_RANGE),
        FREEZE_AI(SERVER_FREEZE_ID, Setting.FREEZE_AI), STAY_ACTIVE_LIMIT(SERVER_STAY_ACTIVE_LIMIT_ID, Setting.STAY_ACTIVE_LIMIT), PATH_BUDGET(SERVER_PATH_BUDGET_ID, Setting.PATH_BUDGET),
        HEALTH_PERCENT(SERVER_HEALTH_ID, Setting.HEALTH_PERCENT), AMMO_POLICY(SERVER_AMMO_ID, Setting.AMMO_POLICY),
        NAME_TAG_RANGE(SERVER_NAME_TAG_RANGE_ID, Setting.NAME_TAG_RANGE), ITEM_DROPS(SERVER_ITEM_DROPS_ID, Setting.ITEM_DROPS),
        FRIENDLY_FIRE_NPCS(SERVER_FRIENDLY_FIRE_NPCS_ID, Setting.FRIENDLY_FIRE_NPCS), FRIENDLY_FIRE_PLAYERS(SERVER_FRIENDLY_FIRE_PLAYERS_ID, Setting.FRIENDLY_FIRE_PLAYERS);
        // spotless:on

        private final int id;
        private final Setting setting;

        /** @return the value the server last sent, 1 for Yes for a switch */
        private int current()
        {
            return ClientServerSettings.get(setting);
        }

        /** @return the value a new world starts with, 1 for Yes for a switch */
        private int fallback()
        {
            return setting.getFallback();
        }

        private int maximum()
        {
            return switch (this)
            {
                case AGGRO_RANGE, FIRING_RANGE, MODEL_AGGRO_RANGE, MODEL_FIRING_RANGE -> NpcMobility.rangeLimit();
                default -> setting.getMaximum();
            };
        }

        private boolean toggle()
        {
            return this == PROFILING || this == FREEZE_AI || this == ITEM_DROPS;
        }

        /** @return the named choices of a row that is one, or none */
        private Enum<?>[] choices()
        {
            return switch (this)
            {
                case AMMO_POLICY -> NpcServerSettings.AmmoPolicy.values();
                case FRIENDLY_FIRE_NPCS, FRIENDLY_FIRE_PLAYERS -> FriendlyFire.Setting.values();
                default -> new Enum<?>[0];
            };
        }
    }

    /** The appearance switches as rows; they are not stored with the weapon options. */
    @RequiredArgsConstructor
    private enum Look
    {
        HURT_FLASH(HURT_FLASH_ID, true), DEATH_ROTATION(DEATH_ROTATION_ID, true), HIDE_BODY(HIDE_BODY_ID, false), KEEP_WRECK(KEEP_WRECK_ID, false);

        private final int id;
        private final boolean fallback;
    }

    /**
     * Marks among the rows of a tab: COLUMN ends the left column and starts one beside it, BELOW ends the columns, and the
     * rows after it span the page below the longer column.
     */
    private enum Layout
    {
        COLUMN, BELOW
    }

    /** A combined editor row; the underlying feature flags retain their save and packet identities. */
    private enum MovementRow
    {
        CONTROLLER
    }

    /** Where rows were laid out: the control column of the first of them, and the top of what follows them, both from the top left of the GUI. */
    private record Placed(int control, int bottom)
    {}

    /**
     * A part of a page: its rows, listed top to bottom in the order given and split into columns by {@link Layout} marks,
     * and the NPCs it is meant for. Only the tabs of a page with several are named. Boarding & piloting uses these as inline sections
     * instead of tabs, with an initial expanded state.
     */
    private record Tab(String name, Scope scope, List<Object> rows, boolean expanded)
    {
        private static Tab of(Object... rows)
        {
            return new Tab("", Scope.ALL, List.of(rows), true);
        }

        private static Tab named(String name, Object... rows)
        {
            return named(name, Scope.ALL, rows);
        }

        private static Tab named(String name, Scope scope, Object... rows)
        {
            return new Tab(name, scope, List.of(rows), true);
        }

        /** A section of a locomotion page, initially folded until the user asks for its detailed settings. */
        private static Tab folded(String name, Object... rows)
        {
            return new Tab(name, Scope.ALL, List.of(rows), false);
        }

        private String key()
        {
            return "wolffsmodnpcs.tab." + name;
        }
    }

    /**
     * A row that only acts while another row, a switch or a choice, has one of the given values: 1 for Yes and 0 for No, or
     * the index of a choice.
     */
    private record Need(Enum<?> parent, Set<Integer> values)
    {
        private static Need on(Feature parent)
        {
            return new Need(parent, Set.of(1));
        }

        // A factory for any switch, beside on() and choice(), though one row needs it so far.
        @SuppressWarnings("SameParameterValue")
        private static Need off(Feature parent)
        {
            return new Need(parent, Set.of(0));
        }

        private static Need choice(Value parent, Enum<?>... choices)
        {
            return new Need(parent, Arrays.stream(choices).map(Enum::ordinal).collect(Collectors.toUnmodifiableSet()));
        }
    }

    /** What each dependent row needs; a row of the same tab as what it needs is indented below it. */
    // spotless:off
    private static final Map<Enum<?>, Need> REQUIREMENTS = Map.ofEntries(
        Map.entry(Feature.DUAL_WIELD, Need.on(Feature.ITEM_WEAPONS)),
        Map.entry(Feature.MELEE_DUAL_WIELD, Need.on(Feature.ITEM_WEAPONS)),
        Map.entry(Value.GRENADE_USE, Need.on(Feature.GRENADES)),
        Map.entry(Value.NATIVE_THROWABLE_USE, Need.on(Feature.NATIVE_THROWABLES)),
        Map.entry(Value.THROWN_WEAPON_USE, Need.on(Feature.ITEM_WEAPONS)),
        Map.entry(Feature.ARMOR_POTION_EFFECTS, Need.on(Feature.ITEM_ARMOR)),
        Map.entry(Value.WEAPON_SWITCH, Need.on(Feature.CONSUME_AMMO)),
        Map.entry(Feature.MODEL_WEAPON_STATS, Need.on(Feature.TYPE_PROPERTIES)),
        Map.entry(Feature.TURRET_MUZZLES, Need.on(Feature.MODEL_MUZZLES)),
        Map.entry(Feature.DRIVER_GUNS, Need.on(Feature.MODEL_MUZZLES)),
        Map.entry(Feature.MANNED_GUNS_FIRE, Need.on(Feature.PASSENGER_GUNS)),
        Map.entry(Feature.ADVANCED_LEAD, Need.on(Feature.LEAD_TARGET)),
        Map.entry(Value.ALIGN_TOLERANCE, Need.on(Feature.FIRE_WHEN_ALIGNED)),
        Map.entry(Value.AIM_SPEED_YAW, Need.on(Feature.GRADUAL_AIM)),
        Map.entry(Value.AIM_SPEED_PITCH, Need.on(Feature.GRADUAL_AIM)),
        Map.entry(Feature.MODEL_ARMOUR, Need.on(Feature.VEHICLE_DAMAGE)),
        Map.entry(Feature.FLAN_DAMAGE_TYPES, Need.off(Feature.ITEM_ARMOR)),
        Map.entry(Feature.PART_HEALTH, Need.on(Feature.PART_HITBOXES)),
        Map.entry(Feature.PART_EFFECTS, Need.on(Feature.PART_HEALTH)),
        Map.entry(Feature.AUTOMATIC_REPAIRS, Need.on(Feature.PART_HEALTH)),
        Map.entry(Feature.SOLID_HITBOXES, Need.on(Feature.PART_HITBOXES)),
        Map.entry(Value.VISION_LIMIT, Need.choice(Value.SIGHT, NpcTuning.Sight.SEE_THROUGH, NpcTuning.Sight.SEE_AND_FIRE_THROUGH)),
        Map.entry(Value.FIRE_LIMIT, Need.choice(Value.SIGHT, NpcTuning.Sight.SEE_AND_FIRE_THROUGH)),
        Map.entry(Value.MAX_WADE_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.GROUND)),
        Map.entry(Value.MIN_WATER_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT)),
        Map.entry(Value.SUBMARINE_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT)),
        Map.entry(Value.DIVE_RATE, Need.choice(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT)),
        Map.entry(Value.STEP_HEIGHT, Need.on(Feature.STEP_CLIMB)),
        Map.entry(Value.ATTACK_ALTITUDE, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.HELICOPTER, NpcTuning.Flight.AUTO)),
        Map.entry(Value.FLIGHT_SPEED, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.AUTO)),
        Map.entry(Value.CRUISE_SPEED, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.AUTO)),
        Map.entry(Value.FLIGHT_TURN_RATE, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.HELICOPTER, NpcTuning.Flight.AUTO)),
        Map.entry(Value.CLIMB_RATE, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.HELICOPTER, NpcTuning.Flight.AUTO)),
        Map.entry(Feature.MODEL_FLIGHT, Need.on(Feature.TYPE_PROPERTIES)),
        Map.entry(Value.TACTIC_DISTANCE, Need.choice(Value.TACTIC, Arrays.stream(NpcTuning.Tactic.values()).filter(NpcTuning.Tactic::usesDistance).toArray(Enum<?>[]::new))),
        Map.entry(Value.COVER_RADIUS, Need.choice(Value.TACTIC, NpcTuning.Tactic.COVER)),
        Map.entry(Value.BLAST_MARGIN, Need.on(Feature.BLAST_STANDOFF)),
        Map.entry(Feature.DRIVING, Need.on(Feature.HULL_FACES_TRAVEL)),
        Map.entry(Value.HULL_TURN_RATE, Need.on(Feature.HULL_FACES_TRAVEL)),
        Map.entry(Value.DRIVE_PRESET, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_TYPE, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_ACCELERATION, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_BRAKING, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_COASTING, Need.on(Feature.DRIVING)),
        Map.entry(Value.TURN_RADIUS, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_LOOKAHEAD, Need.on(Feature.DRIVING)),
        Map.entry(Value.CORNER_SPEED, Need.on(Feature.DRIVING)),
        Map.entry(Feature.MISALIGNED_THROTTLE, Need.on(Feature.HULL_FACES_TRAVEL)),
        Map.entry(Value.THROTTLE_FLOOR, Need.on(Feature.MISALIGNED_THROTTLE)),
        Map.entry(Feature.DRIVE_REVERSE, Need.on(Feature.DRIVING)),
        Map.entry(Value.DRIVE_REVERSE_SPEED, Need.on(Feature.DRIVING)),
        Map.entry(Value.REVERSE_ANGLE, Need.on(Feature.DRIVE_REVERSE)),
        Map.entry(Value.REVERSE_DISTANCE, Need.on(Feature.DRIVE_REVERSE)),
        Map.entry(Value.STOP_DISTANCE, Need.on(Feature.DRIVING)),
        Map.entry(Value.FOLLOW_DISTANCE, Need.on(Feature.DRIVING)),
        Map.entry(Value.STEERING_RESPONSE, Need.on(Feature.DRIVING)),
        Map.entry(Value.STATIONARY_TURN, Need.on(Feature.DRIVING)),
        Map.entry(Value.TRAFFIC_STOP_GAP, Need.on(Feature.DRIVING)),
        Map.entry(Value.PATH_MARGIN, Need.on(Feature.ADAPTIVE_PATH_RANGE)),
        Map.entry(Value.PATH_RETRY_INTERVAL, Need.on(Feature.ADAPTIVE_PATH_RANGE)),
        Map.entry(Feature.DOORS_OPEN, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.GEAR_UP, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.WINGS_FOLDED, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.ENGINE_RUNNING, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.FULL_THROTTLE, Need.on(Feature.ENGINE_RUNNING)),
        Map.entry(Value.SEARCH_NEAR_INTERVAL, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Value.SEARCH_FAR_INTERVAL, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Feature.ADVANCED_SEARCH_INTERVALS, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Feature.STAGGER_NEAR_SEARCH, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Value.SEARCH_BAND_0_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.SEARCH_BAND_1_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.SEARCH_BAND_2_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.SEARCH_BAND_3_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.SEARCH_BAND_4_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.SEARCH_BAND_5_INTERVAL, Need.on(Feature.ADVANCED_SEARCH_INTERVALS)),
        Map.entry(Value.ACTIVE_RANGE, Need.on(Feature.STAY_ACTIVE)),
        Map.entry(Value.BOARD_RANGE, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.TAKE_DRIVEABLES, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.KEEP_DISTANCE, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.CRUISE_HEIGHT, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.ATTACK_HEIGHT, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.CIRCLE_RADIUS, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.PLANE_LANDING_DELAY, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.HOVER_HEIGHT, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.HELICOPTER_LANDING_DELAY, Need.on(Feature.DRIVE_DRIVEABLES)),
        Map.entry(Value.LOW_FUEL, Need.on(Feature.DRIVE_DRIVEABLES)));
    // spotless:on

    /** A page of the tab, listed on the overview under its section in the order given here. */
    private enum Category
    {
        // One page a line, one tab a line: the order here is the order on screen.
        // spotless:off
        /** The settings most NPCs of the edited kind need, with links to the pages they come from; its rows depend on the kind. */
        ESSENTIALS(Section.START, Scope.ALL),
        MODEL_STATS(Section.MODEL, Scope.ALL, Tab.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS)),
        MODEL(Section.MODEL, Scope.MODELS, Tab.of(Feature.ANIMATED_MODEL, Feature.DOORS_OPEN, Feature.GEAR_UP, Feature.WINGS_FOLDED, Feature.ENGINE_RUNNING, Feature.FULL_THROTTLE,
            Layout.COLUMN, Feature.RIDER_AT_SEAT, Feature.MODEL_CULLING)),
        CREW(Section.MODEL, Scope.DRIVEABLES, Tab.named(CrewPage.TAB), Tab.named(CrewPage.RULES_TAB)),
        EQUIPMENT(Section.WEAPONS, Scope.STANDARD, Tab.of(Feature.ITEM_WEAPONS, Feature.DUAL_WIELD, Feature.MELEE_DUAL_WIELD, Feature.CONSUME_AMMO, Feature.FITTING_SLOT_AMMO, Layout.COLUMN,
            Feature.ITEM_ARMOR,
            Feature.ARMOR_POTION_EFFECTS, Layout.BELOW, Value.WEAPON_SWITCH)),
        FIRING(Section.WEAPONS, Scope.ALL, Tab.of(Feature.PROJECTILES, Feature.GRENADES, Value.GRENADE_USE, Feature.NATIVE_THROWABLES, Value.NATIVE_THROWABLE_USE, Value.THROWN_WEAPON_USE,
            Feature.CONSUME_AMMO, Feature.FITTING_SLOT_AMMO)),
        MODEL_WEAPONS(Section.WEAPONS, Scope.MODELS,
            Tab.named("weapon_operation", Feature.SECONDARY_BANK, Feature.ALTERNATE_BARRELS, Feature.RELOAD_CYCLE, Feature.RUN_OVER),
            Tab.named("weapon_crew", Feature.DRIVER_GUNS, Feature.MANNED_GUNS_FIRE, Feature.UNMANNED_GUNS_FIRE, Feature.GUNNER_TARGETS),
            Tab.named("weapon_positions", Feature.MODEL_MUZZLES, Feature.TURRET_MUZZLES, Feature.EYE_AT_MUZZLE)),
        AIMING(Section.TARGETING, Scope.ALL,
            Tab.named("target_tracking", Feature.LEAD_TARGET, Feature.ADVANCED_LEAD, Feature.BALLISTIC_AIM, Feature.FIRE_WHEN_ALIGNED, Value.ALIGN_TOLERANCE),
            Tab.named("gun_movement", Scope.MODELS, Feature.GRADUAL_AIM, Value.AIM_SPEED_YAW, Value.AIM_SPEED_PITCH, Feature.HULL_ASSISTS_AIM, Feature.MANOEUVRE_TO_FIRE, Feature.PASSENGER_GUNS,
                Feature.TURRET_LOCKED)),
        PROTECTION(Section.PROTECTION, Scope.ALL,
            Tab.named(GENERAL_TAB, Feature.INVULNERABLE, Feature.FLAN_DAMAGE_TYPES),
            Tab.named(ARMOUR_TAB, Scope.MODELS, Feature.VEHICLE_DAMAGE, Feature.MODEL_ARMOUR),
            Tab.named(PARTS_TAB, Scope.DRIVEABLES, Feature.PART_HITBOXES, Feature.PART_HEALTH, Feature.PART_EFFECTS, Feature.AUTOMATIC_REPAIRS, Feature.BROKEN_PARTS_PASS, Feature.PART_REPAIR, Feature.SOLID_HITBOXES,
                Feature.HITBOX_OUTLINES)),
        TACTICS(Section.BEHAVIOUR, Scope.ALL, Tab.of(Value.TACTIC, Value.TACTIC_DISTANCE, Value.COVER_RADIUS, Feature.BLAST_STANDOFF, Value.BLAST_MARGIN, Feature.SHIP_WEAPON_BEARING)),
        DETECTION(Section.BEHAVIOUR, Scope.ALL, Tab.of(Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.SIGHT, Value.VISION_LIMIT, Value.FIRE_LIMIT, Value.TARGET_MARGIN)),
        PATHFINDING(Section.BEHAVIOUR, Scope.ALL, Tab.of(Feature.ADAPTIVE_PATH_RANGE, Value.PATH_MARGIN, Value.PATH_RETRY_INTERVAL, Feature.RETURN_HOME_TELEPORT)),
        MOVEMENT(Section.TRAVEL, Scope.ALL,
            Tab.named("movement_terrain", Value.TERRAIN, Value.MAX_WADE_DEPTH, Value.MIN_WATER_DEPTH, Feature.STEP_CLIMB, Value.STEP_HEIGHT, Feature.SLOPE_PHYSICS),
            Tab.named("movement_speed", Value.LAND_SPEED, Value.WATER_SPEED),
            Tab.named("movement_watercraft", Value.SUBMARINE_DEPTH, Value.DIVE_RATE),
            Tab.named("movement_flight", Scope.PLANES_AND_VEHICLES, Value.FLIGHT_TYPE, Value.ATTACK_ALTITUDE, Feature.MODEL_FLIGHT, Value.FLIGHT_SPEED, Value.CRUISE_SPEED,
                Value.FLIGHT_TURN_RATE, Value.CLIMB_RATE)),
        VEHICLE_HANDLING(Section.TRAVEL, Scope.VEHICLES,
            Tab.named("vehicle_setup", MovementRow.CONTROLLER, Value.DRIVE_PRESET, Value.DRIVE_TYPE),
            Tab.named("vehicle_speed_stopping", Value.DRIVE_ACCELERATION, Value.DRIVE_BRAKING, Value.DRIVE_COASTING, Value.STOP_DISTANCE, Value.FOLLOW_DISTANCE, Value.TRAFFIC_STOP_GAP),
            Tab.named("vehicle_steering", Value.HULL_TURN_RATE, Value.TURN_RADIUS, Value.STEERING_RESPONSE, Value.STATIONARY_TURN),
            Tab.named("vehicle_cornering", Value.DRIVE_LOOKAHEAD, Value.CORNER_SPEED, Feature.MISALIGNED_THROTTLE, Value.THROTTLE_FLOOR),
            Tab.named("vehicle_reversing", Feature.DRIVE_REVERSE, Value.DRIVE_REVERSE_SPEED, Value.REVERSE_ANGLE, Value.REVERSE_DISTANCE)),
        BOARDING(Section.TRAVEL, Scope.STANDARD,
            Tab.named("pilot_boarding", Feature.DRIVE_DRIVEABLES, Value.BOARD_RANGE, Value.TAKE_DRIVEABLES),
            Tab.folded("pilot_ground", Value.KEEP_DISTANCE),
            Tab.folded("pilot_aircraft", Value.CRUISE_HEIGHT, Value.ATTACK_HEIGHT, Value.CIRCLE_RADIUS, Value.PLANE_LANDING_DELAY, Value.HOVER_HEIGHT, Value.HELICOPTER_LANDING_DELAY, Value.LOW_FUEL)),
        ANIMATIONS(Section.LOOK, Scope.STANDARD,
            Tab.named("equipment_animation", Value.AIM_POSE, Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS, Feature.GUN_SHOT_ANIMATION, Feature.GUN_RELOAD_ANIMATION, Feature.GUN_MELEE_ANIMATION),
            Tab.named(HeldItemAnimationPage.ITEMS), Tab.named(HeldItemAnimationPage.ARMS_TAB)),
        EFFECTS(Section.LOOK, Scope.ALL, Tab.of(Look.HURT_FLASH, Look.DEATH_ROTATION, Look.HIDE_BODY, Look.KEEP_WRECK, Feature.SHOOT_PARTICLES, Feature.MUZZLE_FLASHES)),
        SOUNDS(Section.LOOK, Scope.ALL,
            Tab.named(OVERVIEW_TAB),
            Tab.named("weapon", Feature.FIRE_SOUNDS, Feature.RELOAD_SOUNDS, Feature.THROW_SOUNDS, Feature.MELEE_SOUNDS),
            Tab.named(IDLE_MOVEMENT_TAB)),
        PERFORMANCE(Section.ADVANCED, Scope.ALL,
            Tab.named("target_search", Feature.PROGRESSIVE_SEARCH, Value.SEARCH_NEAR_INTERVAL, Value.SEARCH_FAR_INTERVAL, Feature.STAGGER_NEAR_SEARCH),
            Tab.named("search_bands", Feature.ADVANCED_SEARCH_INTERVALS, Value.SEARCH_BAND_0_INTERVAL, Value.SEARCH_BAND_1_INTERVAL, Value.SEARCH_BAND_2_INTERVAL,
                Value.SEARCH_BAND_3_INTERVAL, Value.SEARCH_BAND_4_INTERVAL, Value.SEARCH_BAND_5_INTERVAL),
            Tab.named("activity", Feature.STAY_ACTIVE, Value.ACTIVE_RANGE, Value.DORMANT_RANGE, Feature.RETURN_WHEN_INACTIVE),
            Tab.named("debug", Feature.PATH_DEBUG, Feature.AIM_DEBUG)),
        /** What the server finds of this NPC: the values it acts on and what it is doing; its tabs lay out lines, not rows. */
        DIAGNOSIS(Section.ADVANCED, Scope.ALL, Tab.named(EFFECTIVE_TAB), Tab.named(LIVE_TAB)),
        /** The pages of the whole server: their rows are {@link ServerRow}s, and the actions they offer sit below them. */
        SERVER_SEARCH(Section.SERVER, Scope.ALL, Tab.of(ServerRow.NEAR_INTERVAL, ServerRow.FAR_INTERVAL, ServerRow.SEARCH_BUDGET)),
        SERVER_RULES(Section.SERVER, Scope.ALL,
            Tab.named(SERVER_RANGES_TAB, ServerRow.AGGRO_RANGE, ServerRow.FIRING_RANGE, ServerRow.MODEL_AGGRO_RANGE, ServerRow.MODEL_FIRING_RANGE),
            Tab.named("rules", ServerRow.HEALTH_PERCENT, ServerRow.AMMO_POLICY, ServerRow.NAME_TAG_RANGE, ServerRow.ITEM_DROPS),
            Tab.named("friendly_fire", ServerRow.FRIENDLY_FIRE_NPCS, ServerRow.FRIENDLY_FIRE_PLAYERS)),
        SERVER_LIMITS(Section.SERVER, Scope.ALL, Tab.of(ServerRow.FREEZE_AI, ServerRow.STAY_ACTIVE_LIMIT, ServerRow.PATH_BUDGET)),
        /** The loaded NPCs of every level, which the server counts when the page opens; lines, not rows. */
        CENSUS(Section.SERVER, Scope.ALL, Tab.of()),
        FACTIONS(Section.SERVER, Scope.ALL, Tab.named("team_factions")),
        DIAGNOSTICS(Section.SERVER, Scope.ALL, Tab.named("profiling", ServerRow.PROFILING), Tab.named(COMPARISON_TAB));
        // spotless:on

        private final Section section;
        private final Scope scope;
        private final List<Tab> tabs;

        Category(Section section, Scope scope, Tab... tabs)
        {
            this.section = section;
            this.scope = scope;
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
                case MOVEMENT, VEHICLE_HANDLING -> LOCOMOTION + "tabs_hint";
                case BOARDING -> LOCOMOTION + "hint";
                default -> TUNING + "hint";
            };
        }
    }

    private boolean loaded;
    /** Whether the server has sent the settings presets set, which the tab does not otherwise receive. */
    private boolean presetsLoaded;
    private boolean changed;
    private Category category;
    /** The page that linked to the open one, which Back returns to instead of the overview. */
    @Nullable
    private Category returnTo;
    /** Which tab of a page with several is shown. */
    private int tab;
    /** Folded sections and viewport position are local editor state, never NPC gameplay data. */
    private final Set<String> toggledMovementSections = new HashSet<>();
    private int movementOffset;
    private int movementRowCount;
    /** Whether the next press of the button applying the ranges to every NPC carries it out; any other edit asks again. */
    private boolean confirmApplyRanges;
    /** The same for the button handing the search intervals to every NPC. */
    private boolean confirmApplySearch;
    /** Client ticks the Diagnosis page has been open, which time its requests. */
    private int inspectTicks;
    private NpcKind kind;
    private NpcEngineSettings editingSound;
    private final CombatPages combat;
    private final SoundOverview soundOverview;
    private final HeldItemAnimationPage heldItems;
    private final CrewPage crew;
    private final TeamFactionsPage teamFactions;
    private final SettingsTooltip settingsTooltip = new SettingsTooltip();

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        combat = new CombatPages(this, npc);
        soundOverview = new SoundOverview(this, npc);
        heldItems = new HeldItemAnimationPage(this, npc);
        crew = new CrewPage(this, npc);
        teamFactions = new TeamFactionsPage(this, npc);
        kind = NpcKind.of(npc);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
        NpcNetwork.sendToServer(NpcPresetPacket.request(npc.getId()));
        // The Server pages and the server's intervals beside this NPC's own both need the server's settings.
        requestServerSettings();
    }

    @Override
    public void init()
    {
        super.init();
        // The model may have changed in the Display tab since the last visit.
        kind = NpcKind.of(npc);
        if (category == null)
            initOverview();
        else
            initCategory();
        addLabel(hintLabel(loaded ? hint() : WEAPONS + "loading"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        settingsTooltip.beginFrame();
        super.render(graphics, mouseX, mouseY, partialTick);
        settingsTooltip.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public void setTooltipForNextRenderPass(@NotNull List<FormattedCharSequence> tooltip, @NotNull ClientTooltipPositioner positioner, boolean override)
    {
        settingsTooltip.queue(tooltip, positioner, override);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (hasShiftDown() && settingsTooltip.scroll(delta))
            return true;
        if (category == Category.BOARDING && !hasSubGui() && mouseX >= guiLeft + 8 && mouseX < guiLeft + 404 && mouseY >= guiTop + MOVEMENT_TOP
            && mouseY < guiTop + MOVEMENT_TOP + MOVEMENT_VISIBLE_ROWS * ROW_HEIGHT && delta != 0D)
        {
            scrollMovement(delta > 0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    /** The line below the page; one too long for the page is cut short, and its tooltip holds all of it. */
    private GuiLabel hintLabel(String key)
    {
        String text = I18n.get(key);
        if (font().width(text) <= PAGE_WIDTH)
            return new GuiLabel(HINT_LABEL_ID, key, guiLeft + 8, guiTop + 203);
        String cut = font().plainSubstrByWidth(text, PAGE_WIDTH - font().width("...")) + "...";
        GuiLabel label = new GuiLabel(HINT_LABEL_ID, Component.literal(cut), CustomNpcsPlatform.defaultTextColor(), guiLeft + 8, guiTop + 203, 40, 10);
        label.setTooltip(Tooltip.create(Component.literal(text)));
        return label;
    }

    private String hint()
    {
        if ((category == null ? HUB.get() : category.section.hub) == Hub.SERVER)
            return TUNING + (ClientServerSettings.isKnown() ? "server_hint" : "server_waiting");
        if (category == null)
            return OVERVIEW + "hint";
        if (category == Category.DIAGNOSIS)
            return DIAGNOSIS + "hint";
        return combat.active() ? combat.hint() : category.hint();
    }

    private static Font font()
    {
        return Minecraft.getInstance().font;
    }

    // Overview

    /**
     * The row of hub tabs, then the hub tab shown: the kind of NPC, or for Server that its pages hold for the whole server,
     * above one column per section, each a heading followed by the buttons of its pages. The Start here column ends with the
     * presets.
     */
    private void initOverview()
    {
        initHubButtons();
        Hub hub = HUB.get();
        if (hub == Hub.SERVER)
            addLabel(new GuiLabel(TITLE_LABEL_ID, OVERVIEW + "server_title", guiLeft + 8, guiTop + PAGE_TOP));
        else
            initKindLabel();
        List<Section> sections = Arrays.stream(Section.values()).filter(section -> section.hub == hub).toList();
        for (Section section : sections)
        {
            int x = guiLeft + 8 + section.column * OVERVIEW_COLUMN;
            int y = PAGE_TOP + OVERVIEW_KIND_HEIGHT;
            if (sections.size() > 1)
            {
                addLabel(new GuiLabel(SECTION_LABEL_ID + section.ordinal(), section.key(), x, guiTop + y));
                y += OVERVIEW_HEADING_HEIGHT;
            }
            for (Category page : Category.values())
            {
                if (page.section != section || page.section == Section.TRAVEL && !page.scope.includes(kind))
                    continue;
                pageButton(page, x, guiTop + y);
                y += ROW_HEIGHT;
            }
            if (section == Section.START)
                initPresets(x, guiTop + y);
        }
    }

    /** One button per hub tab along the top, each counting the changed settings of its pages; the one shown is pressed. */
    private void initHubButtons()
    {
        for (Hub hub : Hub.values())
        {
            int left = hub == Hub.SERVER ? 8 + PAGE_WIDTH - HUB_WIDTH : 8 + hub.ordinal() * (HUB_WIDTH + 2);
            int changes = changes(hub);
            GuiButtonNop button = new GuiButtonNop(this, HUB_BUTTON_ID + hub.ordinal(), guiLeft + left, guiTop + 4, HUB_WIDTH, 20, new String[]{hub.key()}, 0);
            button.setMessage(marked(Component.translatable(hub.key()), changes, HUB_WIDTH));
            MutableComponent tooltip = Component.translatable(hub.key() + HELP_SUFFIX);
            if (changes > 0)
                tooltip.append("\n").append(Component.translatable(OVERVIEW + "changed", changes).withStyle(ChatFormatting.GOLD));
            button.setTooltip(Tooltip.create(tooltip));
            button.setEnabled(loaded && hub != HUB.get());
            addButton(button);
        }
    }

    private void selectHub(Hub hub)
    {
        HUB.set(hub);
        if (hub == Hub.SERVER)
            requestServerSettings();
    }

    private static void requestServerSettings()
    {
        // The tab shows what the server answers, not what an earlier visit left behind.
        ClientServerSettings.forget();
        NpcNetwork.sendToServer(ServerControlPacket.request());
    }

    // Presets

    /** The presets below the Start here pages: a button choosing the preset, whose tooltip lists what it sets, and Apply. */
    private void initPresets(int x, int y)
    {
        NpcPreset preset = NpcPresetControls.selected(npc);
        boolean matches = presetsLoaded && preset.matches(npc);
        Tooltip tooltip = Tooltip.create(NpcPresetControls.tooltip(npc, preset, presetsLoaded));
        int width = OVERVIEW_COLUMN - OVERVIEW_GAP;
        String[] names = Arrays.stream(NpcPreset.values()).map(NpcPreset::key).toArray(String[]::new);
        GuiButtonNop select = new GuiButtonNop(this, PRESET_ID, x, y, width, 20, names, preset.ordinal());
        MutableComponent label = Component.translatable(preset.key());
        if (matches)
            label.append(Component.literal(" \u2714").withStyle(ChatFormatting.GREEN));
        select.setMessage(label);
        select.setTooltip(tooltip);
        select.setEnabled(loaded);
        addButton(select);
        GuiButtonNop apply = new GuiButtonNop(this, PRESET_APPLY_ID, x, y + ROW_HEIGHT, (width - 4) / 2, 20, new String[]{PRESET + "apply"}, 0);
        apply.setTooltip(tooltip);
        apply.setEnabled(loaded && presetsLoaded && !matches);
        addButton(apply);
        GuiButtonNop undo = new GuiButtonNop(this, PRESET_UNDO_ID, x + (width + 4) / 2, y + ROW_HEIGHT, (width - 4) / 2, 20, new String[]{PRESET + "undo"}, 0);
        undo.setEnabled(loaded && NpcPresetControls.canUndo(npc));
        addButton(undo);
    }

    private NpcPreset selectedPreset()
    {
        return NpcPresetControls.selected(npc);
    }

    /** Shows the preset settings the server sent for the NPC, if this tab edits it. */
    public void presetsReceived(EntityNPCInterface received)
    {
        if (received != npc)
            return;
        presetsLoaded = true;
        init();
    }

    /** The kind of NPC, with its model, above the columns of a hub tab. */
    private void initKindLabel()
    {
        Component title = NpcKind.model(npc).map(type -> (Component) Component.translatable(OVERVIEW + "title_model", Component.translatable(kind.key()), type.getName()))
            .orElseGet(() -> Component.translatable(kind.key()));
        String text = title.getString();
        if (font().width(text) > PAGE_WIDTH)
            text = font().plainSubstrByWidth(text, PAGE_WIDTH - font().width("...")) + "...";
        GuiLabel label = new GuiLabel(TITLE_LABEL_ID, Component.literal(text), CustomNpcsPlatform.defaultTextColor(), guiLeft + 8, guiTop + PAGE_TOP, 40, 10);
        label.setTooltip(Tooltip.create(title.copy().append("\n").append(Component.translatable(kind.key() + HELP_SUFFIX).withStyle(HELP_STYLE))));
        addLabel(label);
    }

    /**
     * A page button. Its label counts the settings of the page changed from their defaults; a page shown although it is not
     * meant for this kind of NPC is greyed and says who it is for.
     */
    private void pageButton(Category page, int x, int y)
    {
        int width = OVERVIEW_COLUMN - OVERVIEW_GAP;
        int changes = changes(page);
        boolean meant = page.scope.includes(kind);
        MutableComponent name = Component.translatable(page.key());
        if (!meant)
            name.withStyle(ChatFormatting.GRAY);
        GuiButtonNop button = new GuiButtonNop(this, CATEGORY_BUTTON_ID + page.ordinal(), x, y, width, 20, new String[]{page.key()}, 0);
        button.setMessage(marked(name, changes, width));
        MutableComponent tooltip = Component.translatable(page.key() + HELP_SUFFIX);
        if (changes > 0)
            tooltip.append("\n").append(Component.translatable(OVERVIEW + "changed", changes).withStyle(ChatFormatting.GOLD));
        if (!meant)
            tooltip.append("\n").append(Component.translatable(OVERVIEW + "other_kind", Component.translatable(page.scope.key())).withStyle(ChatFormatting.GRAY));
        button.setTooltip(Tooltip.create(tooltip));
        button.setEnabled(loaded);
        addButton(button);
    }

    /** @return the label followed by the number of changed settings, where there are some and it fits the button */
    private static Component marked(MutableComponent label, int changes, int width)
    {
        if (changes <= 0)
            return label;
        Component marker = Component.literal(" •" + changes).withStyle(ChatFormatting.GOLD);
        return font().width(label.getString() + marker.getString()) <= width - 8 ? label.append(marker) : label;
    }

    /** @return the tabs of the page; those not meant for this kind of NPC are omitted for Locomotion and greyed elsewhere */
    private List<Tab> tabs(Category page)
    {
        if (page == Category.ESSENTIALS)
            return List.of(Tab.of(essentials().toArray()));
        if (page.section == Section.TRAVEL)
            return page.tabs.stream().filter(candidate -> candidate.scope().includes(kind)).toList();
        return page.tabs;
    }

    // Changed settings

    private int changes(Hub hub)
    {
        return Arrays.stream(Category.values()).filter(page -> page.section.hub == hub).mapToInt(this::changes).sum();
    }

    private int changes(Category page)
    {
        return page == Category.ESSENTIALS ? 0 : page.tabs.stream().mapToInt(this::changes).sum();
    }

    private int changes(Tab shown)
    {
        int count = (int) shown.rows().stream().filter(this::changed).count();
        count += heldItems.changes(shown.name());
        count += crew.changes(shown.name());
        if (GENERAL_TAB.equals(shown.name()))
        {
            NpcCombatSettings settings = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat();
            count += settings.getKnockback() == Knockback.MODEL ? 0 : 1;
            for (NpcDamageCategory hit : new NpcDamageCategory[]{NpcDamageCategory.PROJECTILE, NpcDamageCategory.MELEE, NpcDamageCategory.EXPLOSION})
                count += settings.minimumHit(hit) == 0 ? 0 : 1;
        }
        return count;
    }

    /** Whether the row holds another value than an NPC gets by default. The Custom NPCs ranges have no default of the tab's. */
    private boolean changed(Object row)
    {
        if (row == MovementRow.CONTROLLER)
            return movementController() != NpcMovementController.VEHICLE;
        if (row == Feature.GEAR_UP)
            return gearChoice() != GEAR_DYNAMIC_CHOICE;
        if (row instanceof Feature feature)
            return enabled(feature) != feature.isDefaultEnabled();
        if (row instanceof Value value)
            return NpcTuning.of(npc).get(value) != value.fallback();
        if (row instanceof ServerRow setting)
            return setting.current() != setting.fallback();
        return row instanceof Look look && lookEnabled(look) != look.fallback;
    }

    // Essentials

    /** @return the rows of the Essentials page for this kind of NPC */
    private List<Object> essentials()
    {
        return switch (kind)
        {
            case STANDARD -> List.of(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.CONSUME_AMMO, Value.WEAPON_SWITCH, Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.SIGHT, Value.TACTIC);
            case VEHICLE -> List.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS, Feature.PART_HITBOXES, Value.DRIVE_PRESET, Value.TERRAIN, Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.TACTIC);
            case PLANE ->
                List.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS, Feature.PART_HITBOXES, Value.FLIGHT_TYPE, Value.ATTACK_ALTITUDE, Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.TACTIC);
            case MECHA -> List.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS, Feature.PART_HITBOXES, Feature.GRADUAL_AIM, Value.TERRAIN, Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.TACTIC);
            case AA_GUN ->
                List.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS, Feature.ALTERNATE_BARRELS, Feature.GRADUAL_AIM, Feature.FIRE_WHEN_ALIGNED, Range.AGGRO_RANGE, Range.FIRING_RANGE);
        };
    }

    /** @return the pages the Essentials page links to, where the rest of what it gathers is set */
    private List<Category> essentialLinks()
    {
        return switch (kind)
        {
            case STANDARD -> List.of(Category.EQUIPMENT, Category.AIMING);
            case VEHICLE -> List.of(Category.MODEL_STATS, Category.VEHICLE_HANDLING);
            case PLANE -> List.of(Category.MODEL_STATS, Category.MOVEMENT);
            case MECHA, AA_GUN -> List.of(Category.MODEL_STATS, Category.AIMING);
        };
    }

    /** The links of the Essentials page, in the title row left of Back. */
    private void initEssentialLinks()
    {
        List<Category> links = essentialLinks();
        for (int i = 0; i < links.size(); i++)
        {
            Category link = links.get(i);
            int left = 352 - (links.size() - i) * (LINK_WIDTH + 2);
            GuiButtonNop button = new GuiButtonNop(this, ESSENTIAL_LINK_ID + i, guiLeft + left, guiTop + 4, LINK_WIDTH, 20, new String[]{link.key()}, 0);
            button.setTooltip(Tooltip.create(Component.translatable(OVERVIEW + "open", Component.literal(path(link)))));
            button.setEnabled(loaded);
            addButton(button);
        }
    }

    // Pages

    private void initCategory()
    {
        int titleWidth = TITLE_WIDTH;
        if (combat.active())
            titleWidth = combat.titleWidth();
        else if (category == Category.ESSENTIALS)
            titleWidth = 352 - essentialLinks().size() * (LINK_WIDTH + 2) - 12;
        addLabel(new GuiLabel(TITLE_LABEL_ID, fitTitle(pageTitle(), titleWidth), guiLeft + 8, guiTop + 10));
        addButton(new GuiButtonNop(this, BACK_BUTTON_ID, guiLeft + 354, guiTop + 4, 50, 20, new String[]{"gui.back"}, 0));
        if (combat.active())
            combat.init(loaded);
        else if (editingSound != null)
            initSoundEditor();
        else
            initPage();
    }

    /** @return the path to the page shown, from its hub tab, or the page that linked to it, down to the sub-page that is open */
    private String pageTitle()
    {
        String path = returnTo != null ? I18n.get(returnTo.key()) + PATH_SEPARATOR + I18n.get(category.key()) : path(category);
        if (combat.active())
            return path + PATH_SEPARATOR + I18n.get(combat.title());
        if (editingSound != null)
            return path + PATH_SEPARATOR + I18n.get(loopKey(editingSound == sounds().getIdle() ? "edit_idle" : "edit_movement"));
        return path;
    }

    /** @return the page's name after its hub tab's */
    private static String path(Category page)
    {
        return I18n.get(page.section.hub.key()) + PATH_SEPARATOR + I18n.get(page.key());
    }

    /** Drops the outermost parts of a page path until it fits, so the page itself is always named. */
    static String fitTitle(String path, int width)
    {
        String title = path;
        while (font().width(title) > width && title.indexOf(PATH_SEPARATOR) != title.lastIndexOf(PATH_SEPARATOR))
            title = TRIMMED + title.substring(title.indexOf(PATH_SEPARATOR, title.startsWith(TRIMMED) ? TRIMMED.length() : 0) + PATH_SEPARATOR.length());
        return title;
    }

    /** The tab buttons of a page with several tabs, the rows of the tab shown and what the page adds below them. */
    private void initPage()
    {
        if (category == Category.BOARDING)
        {
            initMovementPage();
            return;
        }
        List<Tab> tabs = tabs(category);
        tab = Math.max(0, Math.min(tabs.size() - 1, tab));
        int top = PAGE_TOP;
        if (tabs.size() > 1)
        {
            initTabButtons(tabs);
            top = TABBED_TOP;
        }
        Tab shown = tabs.get(tab);
        if (category == Category.FACTIONS)
        {
            teamFactions.init(top);
            return;
        }
        if (HeldItemAnimationPage.ITEMS.equals(shown.name()) || HeldItemAnimationPage.ARMS_TAB.equals(shown.name()))
        {
            heldItems.init(shown.name(), loaded, top);
            return;
        }
        if (CrewPage.owns(shown.name()))
        {
            crew.init(shown.name(), loaded, top);
            return;
        }
        if (initLinesTab(shown, top))
            return;
        if (initSoundTab(shown, top))
            return;
        if (category == Category.ESSENTIALS)
            initEssentialLinks();
        Placed placed = initLayout(shown.rows().stream().filter(this::visible).toList(), top);
        initPageExtras(shown, placed.control(), placed.bottom());
    }

    private boolean movementPage()
    {
        return category != null && category.section == Section.TRAVEL;
    }

    private boolean expanded(Tab section)
    {
        return section.expanded() != toggledMovementSections.contains(section.name());
    }

    /** Inline sections share one viewport; only complete visible rows become widgets, so hidden controls cannot receive input. */
    private void initMovementPage()
    {
        addLabel(textLabel(HINT_LABEL_ID + 1, Component.translatable(category.key() + ".intro"), guiLeft + 8, guiTop + PAGE_TOP, PAGE_WIDTH, READ_ONLY_COLOR));
        List<Object> rows = new ArrayList<>();
        for (Tab section : category.tabs)
        {
            if (!section.scope().includes(kind))
                continue;
            rows.add(section);
            if (expanded(section))
                rows.addAll(section.rows().stream().filter(this::visible).toList());
        }
        movementRowCount = rows.size();
        movementOffset = Math.max(0, Math.min(movementOffset, Math.max(0, movementRowCount - MOVEMENT_VISIBLE_ROWS)));
        for (int index = movementOffset; index < Math.min(rows.size(), movementOffset + MOVEMENT_VISIBLE_ROWS); index++)
        {
            int y = guiTop + MOVEMENT_TOP + (index - movementOffset) * ROW_HEIGHT;
            Object row = rows.get(index);
            if (row instanceof Tab section)
                movementSection(section, y);
            else
            {
                Tab section = category.tabs.stream().filter(candidate -> candidate.rows().contains(row)).findFirst().orElseThrow();
                initRow(row, section.rows(), 8, guiLeft + VALUE_COLUMN, y);
            }
        }
        if (movementRowCount > MOVEMENT_VISIBLE_ROWS)
            initMovementScroll();
    }

    private void movementSection(Tab section, int y)
    {
        int index = category.tabs.indexOf(section);
        int width = MOVEMENT_SCROLL_LEFT - 12;
        GuiButtonNop button = new GuiButtonNop(this, MOVEMENT_SECTION_ID + index, guiLeft + 8, y, width, 20, new String[]{section.key()}, 0);
        MutableComponent title = Component.literal(expanded(section) ? "- " : "+ ").append(Component.translatable(section.key()));
        button.setMessage(marked(title, changes(section), width));
        button.setTooltip(Tooltip.create(Component.translatable(LOCOMOTION + (expanded(section) ? "collapse" : "expand"))));
        button.setEnabled(loaded);
        addButton(button);
    }

    private void initMovementScroll()
    {
        int x = guiLeft + MOVEMENT_SCROLL_LEFT;
        int y = guiTop + MOVEMENT_TOP;
        GuiButtonNop up = new GuiButtonNop(this, MOVEMENT_UP_ID, x, y, 14, 20, "▲");
        up.setTooltip(Tooltip.create(Component.translatable(LOCOMOTION + "scroll_up")));
        up.setEnabled(loaded && movementOffset > 0);
        addButton(up);
        GuiButtonNop down = new GuiButtonNop(this, MOVEMENT_DOWN_ID, x, y + (MOVEMENT_VISIBLE_ROWS - 1) * ROW_HEIGHT, 14, 20, "▼");
        down.setTooltip(Tooltip.create(Component.translatable(LOCOMOTION + "scroll_down")));
        down.setEnabled(loaded && movementOffset < movementRowCount - MOVEMENT_VISIBLE_ROWS);
        addButton(down);
    }

    private void scrollMovement(int direction)
    {
        // Commit an edited number before replacing the widgets, just as when navigating to another page.
        GuiTextFieldNop.unfocus();
        movementOffset = Math.max(0, Math.min(Math.max(0, movementRowCount - MOVEMENT_VISIBLE_ROWS), movementOffset + direction));
        init();
    }

    /**
     * Lays out the rows in blocks split by {@link Layout#BELOW}: one column, or two side by side where a block has a
     * {@link Layout#COLUMN} mark. The right column starts in the middle of the page, or past the controls of the left one
     * where they reach further.
     */
    private Placed initLayout(List<Object> rows, int top)
    {
        int control = -1;
        int y = top;
        int start = 0;
        for (int end = 0; end <= rows.size(); end++)
        {
            if (end < rows.size() && rows.get(end) != Layout.BELOW)
                continue;
            List<Object> block = rows.subList(start, end);
            start = end + 1;
            int split = block.indexOf(Layout.COLUMN);
            int blockControl;
            int height = block.size();
            if (split < 0)
                blockControl = initRows(block, 8, y, block.stream().anyMatch(GuiWolffsMod::wide) ? VALUE_COLUMN : SWITCH_COLUMN);
            else
            {
                List<Object> left = block.subList(0, split);
                List<Object> right = block.subList(split + 1, block.size());
                blockControl = initRows(left, 8, y, 0);
                int reach = blockControl - guiLeft + left.stream().mapToInt(GuiWolffsMod::controlWidth).max().orElse(0);
                initRows(right, Math.max(COLUMN_LEFT, reach + COLUMN_GAP), y, 0);
                height = Math.max(left.size(), right.size());
            }
            if (control < 0)
                control = blockControl;
            y += height * ROW_HEIGHT;
        }
        return new Placed(control, y);
    }

    /** Whether the row's control is a choice or a number, which the controls of a single column leave more room for. */
    private static boolean wide(Object row)
    {
        return row instanceof Value || row instanceof Range || row instanceof ServerRow setting && !setting.toggle();
    }

    private static int controlWidth(Object row)
    {
        if (row instanceof Value value)
            return choices(value).length > 0 ? 150 : 60;
        if (row instanceof ServerRow setting && setting.choices().length > 0)
            return 150;
        return wide(row) ? 60 : 48;
    }

    /** One button per tab across the page; a tab shown although it is not meant for this kind of NPC is greyed. */
    private void initTabButtons(List<Tab> tabs)
    {
        int width = (PAGE_WIDTH + 2) / tabs.size() - 2;
        for (int i = 0; i < tabs.size(); i++)
        {
            Tab candidate = tabs.get(i);
            boolean meant = candidate.scope().includes(kind);
            MutableComponent name = Component.translatable(candidate.key());
            if (!meant)
                name.withStyle(ChatFormatting.GRAY);
            GuiButtonNop button = new GuiButtonNop(this, TAB_BUTTON_ID + i, guiLeft + 8 + i * (width + 2), guiTop + PAGE_TOP, width, 20, new String[]{candidate.key()}, 0);
            button.setMessage(marked(name, changes(candidate), width));
            if (!meant)
                button.setTooltip(Tooltip.create(Component.translatable(OVERVIEW + "other_kind", Component.translatable(candidate.scope().key()))));
            button.setEnabled(loaded && i != tab);
            addButton(button);
        }
    }

    /** What a page adds below its rows: links to its sub-pages, or the controls that are not rows. */
    private void initPageExtras(Tab shown, int control, int below)
    {
        if (category == Category.EQUIPMENT)
            combat.initEquipmentLink(loaded, below + 6);
        else if (category == Category.FIRING)
            combat.initProjectileSlotLink(loaded, below + 6);
        else if (category == Category.MODEL_STATS && enabled(Feature.TYPE_PROPERTIES))
            combat.initPropertiesLink(loaded, below + 6);
        else if (category == Category.MODEL_STATS)
            // Everything else on the page reads the model, so nothing else is offered until inheritance is on.
            addLabel(new GuiLabel(TITLE_LABEL_ID + 2, "wolffsmodnpcs.combat.properties_disabled", guiLeft + 8, guiTop + below + 6));
        else if (category == Category.PROTECTION && GENERAL_TAB.equals(shown.name()))
            combat.initProtection(loaded, control, below);
        else if (category == Category.PROTECTION && PARTS_TAB.equals(shown.name()))
            combat.initPartsLink(loaded);
        else
            initServerActions(shown, below);
    }

    /** The actions of a Server page, below its rows: handing settings to every NPC, or the benchmarks. */
    private void initServerActions(Tab shown, int below)
    {
        if (category == Category.SERVER_RULES && "rules".equals(shown.name()))
        {
            serverButton(SERVER_SHOW_NAME_TAGS_ID, "server_show_name_tags", below + 6).setEnabled(ClientServerSettings.isKnown());
            serverButton(SERVER_HIDE_NAME_TAGS_ID, "server_hide_name_tags", below + 6 + 22).setEnabled(ClientServerSettings.isKnown());
        }
        else if (category == Category.SERVER_RULES && SERVER_RANGES_TAB.equals(shown.name()))
            serverButton(SERVER_APPLY_RANGES_ID, confirmApplyRanges ? "server_apply_ranges_confirm" : "server_apply_ranges", below + 6).setEnabled(ClientServerSettings.isKnown());
        else if (category == Category.SERVER_SEARCH)
            serverButton(SERVER_APPLY_SEARCH_ID, confirmApplySearch ? "server_apply_search_confirm" : "server_apply_search", below + 6).setEnabled(ClientServerSettings.isKnown());
        else if (category == Category.DIAGNOSTICS)
        {
            serverButton(SERVER_BENCHMARK_ID, "server_benchmark", below + 6);
            serverButton(CLIENT_BENCHMARK_ID, "client_benchmark", below + 6 + 22);
        }
    }

    // Pages of lines

    /**
     * @return whether the tab is one that shows lines the server sent rather than rows: the Diagnosis tabs, the census and
     *         the benchmark comparison, which is now laid out
     */
    private boolean initLinesTab(Tab shown, int top)
    {
        if (EFFECTIVE_TAB.equals(shown.name()) || LIVE_TAB.equals(shown.name()))
        {
            List<Component> lines = EFFECTIVE_TAB.equals(shown.name()) ? NpcInspectionView.effective(npc.getId()) : NpcInspectionView.live(npc.getId());
            textLines(lines.isEmpty() ? List.of(Component.translatable(DIAGNOSIS + WAITING)) : lines, top);
            GuiButtonNop copy = new GuiButtonNop(this, DIAGNOSIS_COPY_ID, guiLeft + 8, guiTop + LINES_BUTTON_TOP, 200, 20, new String[]{DIAGNOSIS + "copy"}, 0);
            copy.setTooltip(Tooltip.create(Component.translatable(DIAGNOSIS + "copy" + HELP_SUFFIX)));
            copy.setEnabled(loaded && NpcInspectionView.known(npc.getId()));
            addButton(copy);
            return true;
        }
        if (category == Category.CENSUS)
        {
            ServerReportPacket report = ClientServerSettings.getReport();
            textLines(report == null ? List.of(Component.translatable(CENSUS + WAITING)) : NpcCensusLines.lines(report), top);
            refreshButton();
            return true;
        }
        if (COMPARISON_TAB.equals(shown.name()))
        {
            initComparison(top);
            refreshButton();
            return true;
        }
        return false;
    }

    private void refreshButton()
    {
        GuiButtonNop refresh = new GuiButtonNop(this, REPORT_REFRESH_ID, guiLeft + 8, guiTop + LINES_BUTTON_TOP, 120, 20, new String[]{CENSUS + "refresh"}, 0);
        refresh.setTooltip(Tooltip.create(Component.translatable(CENSUS + "refresh" + HELP_SUFFIX)));
        addButton(refresh);
    }

    /** Lays out read-only lines from the given height within the GUI; one too long for the page is cut short, and its tooltip holds all of it. */
    private void textLines(List<Component> lines, int top)
    {
        int shown = Math.min(lines.size(), MAX_LINES);
        for (int index = 0; index < shown; index++)
            addLabel(textLabel(LINE_LABEL_ID + index, lines.get(index), guiLeft + 8, guiTop + top + index * LINE_HEIGHT, PAGE_WIDTH, CustomNpcsPlatform.defaultTextColor()));
    }

    private static GuiLabel textLabel(int id, Component text, int x, int y, int width, int color)
    {
        String full = text.getString();
        if (font().width(full) <= width)
            return new GuiLabel(id, text, color, x, y, width, 10);
        String cut = font().plainSubstrByWidth(full, width - font().width("...")) + "...";
        GuiLabel label = new GuiLabel(id, Component.literal(cut), color, x, y, width, 10);
        label.setTooltip(Tooltip.create(text));
        return label;
    }

    /**
     * The latest server benchmark beside the one before it, one figure a line, with the change between them coloured by
     * whether it is better or worse.
     */
    private void initComparison(int top)
    {
        ServerReportPacket report = ClientServerSettings.getReport();
        if (report == null || report.benchmarks().isEmpty())
        {
            textLines(List.of(Component.translatable(report == null ? CENSUS + WAITING : COMPARISON + "none")), top);
            return;
        }
        List<BenchmarkResult> results = report.benchmarks();
        BenchmarkResult latest = results.get(results.size() - 1);
        BenchmarkResult previous = results.size() > 1 ? results.get(results.size() - 2) : null;
        int id = LINE_LABEL_ID;
        int color = CustomNpcsPlatform.defaultTextColor();
        int y = guiTop + top;
        addLabel(textLabel(id++, Component.translatable(COMPARISON + "figure"), guiLeft + COMPARISON_COLUMNS[0], y, 200, color));
        addLabel(textLabel(id++, run(previous), guiLeft + COMPARISON_COLUMNS[1], y, 60, color));
        addLabel(textLabel(id++, run(latest), guiLeft + COMPARISON_COLUMNS[2], y, 60, color));
        addLabel(textLabel(id++, Component.translatable(COMPARISON + "change"), guiLeft + COMPARISON_COLUMNS[3], y, 60, color));
        for (BenchmarkResult.Figure figure : BenchmarkResult.Figure.values())
        {
            y += LINE_HEIGHT;
            id = comparisonRow(figure, previous, latest, id, y);
        }
        if (previous == null)
            addLabel(textLabel(id, Component.translatable(COMPARISON + "one"), guiLeft + 8, y + LINE_HEIGHT + 4, PAGE_WIDTH, READ_ONLY_COLOR));
    }

    /**
     * One figure of the comparison at the given height on screen: its name, the value of each run and the change.
     *
     * @return the id of the next label
     */
    private int comparisonRow(BenchmarkResult.Figure figure, @Nullable BenchmarkResult previous, BenchmarkResult latest, int id, int y)
    {
        int color = CustomNpcsPlatform.defaultTextColor();
        int next = id;
        String name = COMPARISON + figure.name().toLowerCase(Locale.ROOT);
        GuiLabel label = textLabel(next++, Component.translatable(name), guiLeft + COMPARISON_COLUMNS[0], y, 200, color);
        label.setTooltip(Tooltip.create(Component.translatable(name + HELP_SUFFIX)));
        addLabel(label);
        addLabel(textLabel(next++, Component.literal(figure(latest.figure(figure))), guiLeft + COMPARISON_COLUMNS[2], y, 60, color));
        if (previous == null)
            return next;
        double before = previous.figure(figure);
        double change = before == 0D ? 0D : 100D * (latest.figure(figure) - before) / Math.abs(before);
        addLabel(textLabel(next++, Component.literal(figure(before)), guiLeft + COMPARISON_COLUMNS[1], y, 60, color));
        addLabel(textLabel(next++, Component.literal(String.format(Locale.ROOT, "%+.1f%%", change)), guiLeft + COMPARISON_COLUMNS[3], y, 60, changeColor(figure, change, color)));
        return next;
    }

    /** @return green for a change for the better, red for one for the worse, or the plain colour for hardly any */
    private static int changeColor(BenchmarkResult.Figure figure, double change, int plain)
    {
        if (Math.abs(change) < 0.5D)
            return plain;
        boolean better = figure.lowerIsBetter() ? change < 0D : change > 0D;
        return better ? GOOD_COLOR : BAD_COLOR;
    }

    /** @return the heading of a run's column: its label, or that it was unnamed, marked when it was cut short */
    private static Component run(@Nullable BenchmarkResult result)
    {
        if (result == null)
            return Component.translatable(COMPARISON + "previous");
        MutableComponent name = result.label().isEmpty() ? Component.translatable(COMPARISON + "unnamed") : Component.literal(result.label());
        return result.aborted() ? name.append("*") : name;
    }

    private static String figure(double value)
    {
        return String.format(Locale.ROOT, Math.abs(value) >= 100D ? "%.0f" : "%.2f", value);
    }

    /** Copies what the Diagnosis page shows, with the NPC and its settings changed from their defaults, for a bug report. */
    private void copySummary()
    {
        StringBuilder text = new StringBuilder();
        text.append(I18n.get(DIAGNOSIS + "summary_title")).append('\n');
        text.append(String.format(Locale.ROOT, "%s (id %d) at %.1f %.1f %.1f in %s%n", npc.getName().getString(), npc.getId(), npc.getX(), npc.getY(), npc.getZ(), npc.level().dimension().location()));
        text.append(I18n.get(kind.key()));
        NpcKind.model(npc).ifPresent(type -> text.append(": ").append(type.getName()).append(" (").append(type.getShortName()).append(')'));
        text.append("\n\n").append(I18n.get(Tab.named(EFFECTIVE_TAB).key())).append('\n');
        NpcInspectionView.effective(npc.getId()).forEach(line -> text.append("  ").append(line.getString()).append('\n'));
        text.append('\n').append(I18n.get(Tab.named(LIVE_TAB).key())).append('\n');
        NpcInspectionView.live(npc.getId()).forEach(line -> text.append("  ").append(line.getString()).append('\n'));
        text.append('\n').append(I18n.get(DIAGNOSIS + "summary_changed")).append('\n');
        // A row listed on several pages is named once.
        Set<Object> rows = new LinkedHashSet<>();
        for (Category page : Category.values())
            for (Tab candidate : page.tabs)
                rows.addAll(candidate.rows());
        // The combined controller still reports the original saved feature flags for diagnostics.
        rows.add(Feature.HULL_FACES_TRAVEL);
        rows.add(Feature.DRIVING);
        for (Object row : rows)
        {
            if (row instanceof Feature feature && changed(feature))
                text.append("  ").append(feature.name()).append(" = ").append(row == Feature.GEAR_UP ? I18n.get(GEAR_CHOICES[gearChoice()]) : enabled(feature)).append('\n');
            else if (row instanceof Value value && changed(value))
                text.append("  ").append(value.name()).append(" = ").append(NpcTuning.of(npc).get(value)).append('\n');
            else if (row instanceof Look look && changed(look))
                text.append("  ").append(look.name()).append(" = ").append(lookEnabled(look)).append('\n');
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(text.toString());
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(DIAGNOSIS + "copied"), true);
    }

    /** Shows what the server found of the NPC, if the Diagnosis page shows this NPC. */
    public void inspected(int entityId)
    {
        if (entityId == npc.getId() && category == Category.DIAGNOSIS)
            init();
    }

    @Override
    public void tick()
    {
        super.tick();
        // While the Diagnosis page is open, it asks the server once a second; the first request starts the count.
        if (category != Category.DIAGNOSIS || !loaded)
            return;
        if (inspectTicks++ % INSPECT_INTERVAL == 0)
            NpcNetwork.sendToServer(NpcInspectPacket.request(npc.getId()));
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
        return row != Feature.MODEL_WEAPON_STATS || kind != NpcKind.STANDARD && enabled(Feature.TYPE_PROPERTIES);
    }

    private boolean enabled(Feature feature)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
    }

    /**
     * Lays out the rows one below the other from the given left edge; their controls share a column, at least the given
     * one, that clears the longest label. Locomotion keeps the given column and clips labels to fit beside it.
     *
     * @return the left edge of that column on screen
     */
    private int initRows(List<Object> rows, int left, int top, int minimum)
    {
        int column = minimum;
        if (!movementPage())
            for (Object row : rows)
                column = Math.max(column, left + indent(row, rows) + font().width(I18n.get(labelKey(row))) + 8);
        int control = guiLeft + column;
        for (int i = 0; i < rows.size(); i++)
        {
            int y = guiTop + top + i * ROW_HEIGHT;
            initRow(rows.get(i), rows, left, control, y);
        }
        return control;
    }

    private void initRow(Object row, List<Object> rows, int left, int control, int y)
    {
        rowLabel(row, rows, left, y);
        if (row == MovementRow.CONTROLLER)
        {
            String[] modes = Arrays.stream(NpcMovementController.values()).map(NpcMovementController::key).toArray(String[]::new);
            GuiButtonNop button = new GuiButtonNop(this, CONTROLLER_ID, control, y, 150, 20, modes, movementController().ordinal());
            button.setEnabled(loaded);
            valueTooltip(button, modes[movementController().ordinal()]);
            addButton(button);
        }
        else if (row instanceof Feature feature)
            featureButton(feature, control, y);
        else if (row instanceof Value value)
            valueControl(value, control, y);
        else if (row instanceof Range range)
            rangeField(range, control, y);
        else if (row instanceof Look look)
            switchButton(look.id, lookEnabled(look), control, y);
        else if (row instanceof ServerRow setting)
            serverControl(setting, control, y);
    }

    private NpcMovementController movementController()
    {
        return NpcMovementController.of(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions());
    }

    private static String labelKey(Object row)
    {
        if (row == MovementRow.CONTROLLER)
            return LOCOMOTION + "controller";
        String name = ((Enum<?>) row).name().toLowerCase(Locale.ROOT);
        if (row instanceof Feature)
            return WEAPONS + name;
        if (row instanceof ServerRow)
            return TUNING + "server_" + name;
        return (row instanceof Look ? PRESENTATION : TUNING) + name;
    }

    private static int rowId(Object row)
    {
        if (row == MovementRow.CONTROLLER)
            return CONTROLLER_ID;
        if (row instanceof Value value)
            return VALUE_ID + value.ordinal();
        if (row instanceof Range range)
            return range.id;
        if (row instanceof ServerRow setting)
            return setting.id;
        if (row instanceof Look look)
            return look.id;
        return ((Feature) row).ordinal();
    }

    /**
     * The label of a row, indented below what it depends on. A row that does nothing for this NPC is grey, and its help
     * starts with why.
     */
    private void rowLabel(Object row, List<Object> rows, int left, int y)
    {
        String key = labelKey(row);
        int labelLeft = left + indent(row, rows);
        GuiLabel label = movementPage()
            ? textLabel(rowId(row), Component.translatable(key), guiLeft + labelLeft, y + 5, VALUE_COLUMN - labelLeft - 8, CustomNpcsPlatform.defaultTextColor())
            : new GuiLabel(rowId(row), key, guiLeft + labelLeft, y + 5, key + HELP_SUFFIX);
        Component reason = inactive(row);
        if (reason != null)
        {
            label.setColor(READ_ONLY_COLOR);
            label.setTooltip(Tooltip.create(withDefault(reason.copy().withStyle(ChatFormatting.RED).append("\n\n").append(Component.translatable(key + HELP_SUFFIX).withStyle(HELP_STYLE)), row)));
        }
        else if (defaultText(row) != null)
            label.setTooltip(Tooltip.create(withDefault(Component.translatable(key + HELP_SUFFIX).withStyle(HELP_STYLE), row)));
        addLabel(label);
    }

    /** @return the help followed by the row's default value, in gold while the row holds another value */
    private MutableComponent withDefault(MutableComponent help, Object row)
    {
        Component value = defaultText(row);
        if (value == null)
            return help;
        return help.append("\n").append(Component.translatable(OVERVIEW + "default", value).withStyle(changed(row) ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }

    /** @return the value an NPC gets by default, as its control shows it, or null for a row without a default of the tab's */
    @Nullable
    private static Component defaultText(Object row)
    {
        if (row == MovementRow.CONTROLLER)
            return Component.translatable(NpcMovementController.VEHICLE.key());
        if (row == Feature.GEAR_UP)
            return Component.translatable(GEAR_CHOICES[GEAR_DYNAMIC_CHOICE]);
        if (row instanceof Feature feature)
            return Component.translatable(NO_YES[feature.isDefaultEnabled() ? 1 : 0]);
        if (row instanceof Look look)
            return Component.translatable(NO_YES[look.fallback ? 1 : 0]);
        if (row instanceof ServerRow setting)
            return serverDefault(setting);
        if (!(row instanceof Value value))
            return null;
        Enum<?>[] choices = choices(value);
        if (choices.length > value.fallback() && value.fallback() >= 0)
            return Component.translatable(labelKey(value) + "." + choices[value.fallback()].name().toLowerCase(Locale.ROOT));
        return Component.literal(Integer.toString(value.fallback()));
    }

    private static Component serverDefault(ServerRow setting)
    {
        if (setting.toggle())
            return Component.translatable(NO_YES[setting.fallback()]);
        if (setting.choices().length > 0)
            return Component.translatable(labelKey(setting) + "." + setting.choices()[setting.fallback()].name().toLowerCase(Locale.ROOT));
        return Component.literal(Integer.toString(setting.fallback()));
    }

    /** @return how far the row is moved right: one step for each row above it in the list that it depends on */
    private static int indent(Object row, List<Object> rows)
    {
        Need need = requirement(row);
        return need != null && rows.contains(need.parent()) ? INDENT + indent(need.parent(), rows) : 0;
    }

    /** @return what the row needs to act, or null for a row that always acts */
    @Nullable
    private static Need requirement(Object row)
    {
        return row instanceof Enum<?> key ? REQUIREMENTS.get(key) : null;
    }

    /** @return why the row does nothing for this NPC now, or null while it acts */
    @Nullable
    private Component inactive(Object row)
    {
        Scope scope = scope(row);
        if (!scope.includes(kind))
            return Component.translatable(NEEDS + "kind", Component.translatable(scope.key()));
        Need need = requirement(row);
        if (need == null)
            return null;
        Enum<?> parent = need.parent();
        if (!need.values().contains(current(parent)) || parent == Feature.DRIVING && movementController() != NpcMovementController.VEHICLE)
        {
            if (parent == Feature.HULL_FACES_TRAVEL || parent == Feature.DRIVING)
                return Component.translatable(LOCOMOTION + (parent == Feature.DRIVING ? "needs_vehicle_controller" : "needs_hull_controller"), Component.literal(location(parent)));
            Component name = Component.translatable(labelKey(parent));
            Component where = Component.literal(location(parent));
            if (parent instanceof Value value)
            {
                String choices = need.values().stream().sorted().map(choice -> I18n.get(labelKey(value) + "." + choices(value)[choice].name().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.joining(" / "));
                return Component.translatable(NEEDS + "choice", name, choices, where);
            }
            return Component.translatable(NEEDS + (need.values().contains(1) ? "on" : "off"), name, where);
        }
        // What the row needs is set, but it may itself do nothing.
        return inactive(parent);
    }

    /** Sets a switch from its button: 1 for Yes, or the gear row's choice of down, up or dynamic. */
    private void setFeature(Feature feature, int value)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (feature == Feature.GEAR_UP)
        {
            options.set(Feature.GEAR_UP, value == GEAR_UP_CHOICE);
            options.set(Feature.GEAR_DYNAMIC, value == GEAR_DYNAMIC_CHOICE);
        }
        else
            options.set(feature, value == 1);
    }

    /** @return the landing gear the NPC draws: down, up, or dynamic, by its choice on the gear row */
    private int gearChoice()
    {
        if (enabled(Feature.GEAR_DYNAMIC))
            return GEAR_DYNAMIC_CHOICE;
        return enabled(Feature.GEAR_UP) ? GEAR_UP_CHOICE : 0;
    }

    /** @return the value of a switch, 1 for Yes, or of a choice or number */
    private int current(Enum<?> row)
    {
        if (row == Feature.GEAR_UP)
            return gearChoice();
        if (row instanceof Feature feature)
            return enabled(feature) ? 1 : 0;
        return NpcTuning.of(npc).get((Value) row);
    }

    /** @return the page, and its tab, that holds the row */
    private static String location(Enum<?> row)
    {
        for (Category page : Category.values())
            for (Tab candidate : page.tabs)
                if (candidate.rows().contains(row) || (row == Feature.HULL_FACES_TRAVEL || row == Feature.DRIVING) && candidate.rows().contains(MovementRow.CONTROLLER))
                    return I18n.get(page.key()) + (candidate.name().isEmpty() ? "" : PATH_SEPARATOR + I18n.get(candidate.key()));
        return "";
    }

    /** @return the kinds of NPC the row does something for */
    private static Scope scope(Object row)
    {
        if (row instanceof Feature feature)
        {
            return switch (feature)
            {
                case MODEL_WEAPON_STATS, SECONDARY_BANK, ALTERNATE_BARRELS, RELOAD_CYCLE, MODEL_MUZZLES, TURRET_MUZZLES, EYE_AT_MUZZLE, ADVANCED_LEAD, FIRE_WHEN_ALIGNED, GRADUAL_AIM, PASSENGER_GUNS,
                    TURRET_LOCKED, VEHICLE_DAMAGE, MODEL_ARMOUR, ANIMATED_MODEL, DOORS_OPEN, GEAR_UP, WINGS_FOLDED, ENGINE_RUNNING, FULL_THROTTLE, RIDER_AT_SEAT, MODEL_CULLING -> Scope.MODELS;
                case PART_HITBOXES, PART_HEALTH, PART_EFFECTS, AUTOMATIC_REPAIRS, BROKEN_PARTS_PASS, PART_REPAIR, SOLID_HITBOXES, HITBOX_OUTLINES, MANNED_GUNS_FIRE, UNMANNED_GUNS_FIRE, GUNNER_TARGETS,
                    DRIVER_GUNS, MUZZLE_FLASHES -> Scope.DRIVEABLES;
                case HULL_ASSISTS_AIM, MANOEUVRE_TO_FIRE, MISALIGNED_THROTTLE, DRIVING, DRIVE_REVERSE -> Scope.VEHICLES;
                case HULL_FACES_TRAVEL -> Scope.HULLS;
                case RUN_OVER, STEP_CLIMB -> Scope.GROUND_MACHINES;
                case SLOPE_PHYSICS -> Scope.VEHICLES;
                case MODEL_FLIGHT -> Scope.PLANES_AND_VEHICLES;
                case DRIVE_DRIVEABLES -> Scope.STANDARD;
                default -> Scope.ALL;
            };
        }
        if (row instanceof Value value)
        {
            return switch (value)
            {
                case ALIGN_TOLERANCE, AIM_SPEED_YAW, AIM_SPEED_PITCH -> Scope.MODELS;
                case DRIVE_PRESET, DRIVE_TYPE, DRIVE_ACCELERATION, DRIVE_BRAKING, DRIVE_COASTING, TURN_RADIUS, THROTTLE_FLOOR, DRIVE_LOOKAHEAD, CORNER_SPEED, DRIVE_REVERSE_SPEED, REVERSE_ANGLE,
                    REVERSE_DISTANCE, STOP_DISTANCE, FOLLOW_DISTANCE, STEERING_RESPONSE, STATIONARY_TURN, TRAFFIC_STOP_GAP -> Scope.VEHICLES;
                case HULL_TURN_RATE -> Scope.HULLS;
                case STEP_HEIGHT -> Scope.GROUND_MACHINES;
                case COVER_RADIUS, BOARD_RANGE, TAKE_DRIVEABLES, KEEP_DISTANCE, CRUISE_HEIGHT, ATTACK_HEIGHT, CIRCLE_RADIUS, PLANE_LANDING_DELAY, HOVER_HEIGHT, HELICOPTER_LANDING_DELAY, LOW_FUEL,
                    GRENADE_USE, THROWN_WEAPON_USE, NATIVE_THROWABLE_USE -> Scope.STANDARD;
                default -> Scope.ALL;
            };
        }
        return Scope.ALL;
    }

    private boolean lookEnabled(Look look)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        return switch (look)
        {
            case HURT_FLASH -> settings.isHurtFlash();
            case DEATH_ROTATION -> settings.isDeathRotation();
            case HIDE_BODY -> npc.stats.hideKilledBody;
            case KEEP_WRECK -> settings.isKeepWreck();
        };
    }

    /** The model sounds switch of the Idle & movement tab, a row outside a list of rows. */
    private void modelSoundsRow(int control, int y)
    {
        rowLabel(Feature.MODEL_SOUNDS, List.of(), 8, y);
        featureButton(Feature.MODEL_SOUNDS, control, y);
    }

    private void featureButton(Feature feature, int control, int y)
    {
        if (feature == Feature.GEAR_UP)
        {
            int gear = gearChoice();
            GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), control, y, 60, 20, GEAR_CHOICES, gear);
            button.setEnabled(loaded);
            valueTooltip(button, GEAR_CHOICES[gear]);
            addButton(button);
            return;
        }
        boolean on = enabled(feature);
        GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), control, y, 48, 20, NO_YES, on ? 1 : 0);
        button.setEnabled(loaded && (!movementPage() || inactive(feature) == null));
        valueTooltip(button, labelKey(feature) + (on ? ".yes" : ".no"));
        addButton(button);
    }

    /** What the selected value does, on its control, for a row whose help explains its values one by one. The page is rebuilt on every click. */
    private static void valueTooltip(GuiButtonNop button, String valueKey)
    {
        if (I18n.exists(valueKey + HELP_SUFFIX))
            button.setTooltip(Tooltip.create(Component.translatable(valueKey + HELP_SUFFIX).withStyle(HELP_STYLE)));
    }

    private void valueControl(Value value, int control, int y)
    {
        boolean editable = loaded && (!movementPage() || inactive(value) == null);
        if (GroundHandlingControls.render(this, npc, value, VALUE_ID + value.ordinal(), control, y, editable))
            return;
        int id = VALUE_ID + value.ordinal();
        int current = NpcTuning.of(npc).get(value);
        Enum<?>[] choices = choices(value);
        if (choices.length > 0)
        {
            String key = labelKey(value);
            String[] names = new String[choices.length];
            for (int choice = 0; choice < choices.length; choice++)
                names[choice] = key + "." + choices[choice].name().toLowerCase(Locale.ROOT);
            GuiButtonNop button = new GuiButtonNop(this, id, control, y, 150, 20, names, current);
            button.setEnabled(editable);
            if (current >= 0 && current < names.length)
                valueTooltip(button, names[current]);
            addButton(button);
            return;
        }
        numberField(id, control, y, current, value.minimum(), value.maximum(), value.fallback());
        getTextField(id).setEditable(editable);
        serverValueLabel(value, control + 66, y + 5);
    }

    /** Beside an NPC's own search interval, the server's, which the NPC follows while its own is 0. */
    private void serverValueLabel(Value value, int x, int y)
    {
        if (!ClientServerSettings.isKnown())
            return;
        int id;
        int server;
        if (value == Value.SEARCH_NEAR_INTERVAL)
        {
            id = SERVER_NEAR_VALUE_ID;
            server = ClientServerSettings.getNearInterval();
        }
        else if (value == Value.SEARCH_FAR_INTERVAL)
        {
            id = SERVER_FAR_VALUE_ID;
            server = ClientServerSettings.getFarInterval();
        }
        else
            return;
        String key = TUNING + "server_value";
        Component text = Component.translatable(key, server);
        GuiLabel label = new GuiLabel(id, text, READ_ONLY_COLOR, x, y, font().width(text), 10);
        label.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
        addLabel(label);
    }

    /** @return the named choices of a value that is one, or none for a number */
    private static Enum<?>[] choices(Value value)
    {
        return switch (value)
        {
            case AIM_POSE -> NpcTuning.AimPose.values();
            case STATIONARY_TURN -> NpcTuning.StationaryTurn.values();
            case TERRAIN -> NpcTuning.Terrain.values();
            case SIGHT -> NpcTuning.Sight.values();
            case DRIVE_PRESET -> NpcTuning.DrivePreset.values();
            case DRIVE_TYPE -> NpcTuning.DriveType.values();
            case FLIGHT_TYPE -> NpcTuning.Flight.values();
            case WEAPON_SWITCH -> NpcTuning.WeaponSwitch.values();
            case TACTIC -> NpcTuning.Tactic.values();
            case TAKE_DRIVEABLES -> NpcTuning.Claim.values();
            case GRENADE_USE, THROWN_WEAPON_USE, NATIVE_THROWABLE_USE -> NpcTuning.Throwing.values();
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

    /** The control of a setting of the whole server, read-only until the server has sent the values. */
    private void serverControl(ServerRow setting, int control, int y)
    {
        boolean known = ClientServerSettings.isKnown();
        if (setting.toggle())
        {
            GuiButtonNop toggle = new GuiButtonNop(this, setting.id, control, y, 48, 20, NO_YES, setting.current());
            toggle.setEnabled(known);
            addButton(toggle);
            return;
        }
        Enum<?>[] choices = setting.choices();
        if (choices.length > 0)
        {
            String key = labelKey(setting);
            String[] names = Arrays.stream(choices).map(choice -> key + "." + choice.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
            int current = Math.max(0, Math.min(names.length - 1, setting.current()));
            GuiButtonNop choice = new GuiButtonNop(this, setting.id, control, y, 150, 20, names, current);
            choice.setEnabled(known);
            valueTooltip(choice, names[current]);
            addButton(choice);
            return;
        }
        GuiTextFieldNop field = new GuiTextFieldNop(setting.id, this, control, y, 60, 20, Integer.toString(setting.current()));
        field.setEditable(known);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(setting.setting.getMinimum(), setting.maximum(), setting.current());
        addTextField(field);
    }

    /** An action of a Server page, below its rows, from the given height within the GUI. */
    private GuiButtonNop serverButton(int id, String name, int y)
    {
        String key = TUNING + name;
        GuiButtonNop button = new GuiButtonNop(this, id, guiLeft + 8, guiTop + y, 268, 20, new String[]{key}, 0);
        button.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
        addButton(button);
        return button;
    }

    /** @return whether the field or button belonged to the Server page */
    private boolean editServer(int id, int number)
    {
        boolean confirmingRanges = confirmApplyRanges;
        boolean confirmingSearch = confirmApplySearch;
        confirmApplyRanges = false;
        confirmApplySearch = false;
        Optional<ServerRow> row = Arrays.stream(ServerRow.values()).filter(candidate -> candidate.id == id).findFirst();
        if (row.isPresent())
        {
            NpcNetwork.sendToServer(ServerControlPacket.set(ClientServerSettings.with(row.get().setting, number)));
            return true;
        }
        switch (id)
        {
            case SERVER_APPLY_RANGES_ID -> {
                // The first press only asks for a second one: the ranges of every NPC are overwritten.
                if (confirmingRanges)
                    NpcNetwork.sendToServer(ServerControlPacket.applyRanges());
                else
                    confirmApplyRanges = true;
                init();
            }
            case SERVER_APPLY_SEARCH_ID -> {
                // As with the ranges: every NPC's own intervals are dropped.
                if (confirmingSearch)
                    NpcNetwork.sendToServer(ServerControlPacket.applySearch());
                else
                    confirmApplySearch = true;
                init();
            }
            case REPORT_REFRESH_ID -> {
                requestReport();
                init();
            }
            case SERVER_SHOW_NAME_TAGS_ID -> NpcNetwork.sendToServer(ServerControlPacket.nameTags(true));
            case SERVER_HIDE_NAME_TAGS_ID -> NpcNetwork.sendToServer(ServerControlPacket.nameTags(false));
            case SERVER_BENCHMARK_ID -> NpcNetwork.sendToServer(ServerControlPacket.benchmark(ServerBenchmark.DEFAULT_SECONDS));
            case CLIENT_BENCHMARK_ID -> {
                // A benchmark does not measure a screen: the editor closes first.
                close();
                ClientBenchmark.start(ServerBenchmark.DEFAULT_SECONDS);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Asks the server for the NPC census and its latest benchmarks; the page shows them once they arrive. */
    private static void requestReport()
    {
        ClientServerSettings.forgetReport();
        NpcNetwork.sendToServer(ServerControlPacket.report());
    }

    private void rangeField(Range range, int control, int y)
    {
        int current = range == Range.AGGRO_RANGE ? npc.stats.aggroRange : npc.stats.ranged.getRange();
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
        GuiLabel label = new GuiLabel(id, key, guiLeft + 8, y + 5, key + HELP_SUFFIX);
        Arrays.stream(Look.values()).filter(look -> look.id == id).findFirst()
            .ifPresent(look -> label.setTooltip(Tooltip.create(withDefault(Component.translatable(key + HELP_SUFFIX).withStyle(HELP_STYLE), look))));
        addLabel(label);
        switchButton(id, value, control, y);
    }

    private void switchButton(int id, boolean value, int control, int y)
    {
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
            modelSoundsRow(control, y + 72);
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
        if (subgui instanceof CrewSelectionMenu menu)
            changed |= crew.selected(menu);
        else if (subgui instanceof CrewCopyMenu menu)
            changed |= crew.selected(menu);
        else if (subgui instanceof GuiSoundSelection picker)
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

    void crewResult(CrewResultPacket packet)
    {
        crew.receive(packet);
    }

    void factionRelations(FactionRelationsPacket packet)
    {
        teamFactions.receive(packet);
        refreshPage();
    }

    @Override
    public void unFocused(GuiTextFieldNop field)
    {
        if (teamFactions.unFocused(field))
            return;
        if (loaded && editEquipmentField(field))
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

    private boolean editEquipmentField(GuiTextFieldNop field)
    {
        return heldItems.field(field) || combat.unFocused(field);
    }

    /** @return whether the field was one of the simulation numbers or ranges */
    private boolean editNumber(GuiTextFieldNop field)
    {
        if (!field.isInteger())
            return false;
        if (field.id >= VALUE_ID && field.id < VALUE_ID + Value.values().length)
        {
            Value value = Value.values()[field.id - VALUE_ID];
            if (GroundHandlingControls.inherited(npc, value))
                return false;
            editValue(value, field.getInteger());
        }
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
        if (ownPageButton(button) || navigate(button))
        {
            refreshPage();
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
            setFeature(Feature.values()[button.id], button.getValue());
            changed = true;
        }
        else if (button.id >= VALUE_ID && button.id < VALUE_ID + Value.values().length)
        {
            Value value = Value.values()[button.id - VALUE_ID];
            int number = button.getValue();
            // Only the tactics written for this kind of NPC are offered; the others are stepped over.
            if (value == Value.TACTIC)
            {
                number = NpcTacticChoices.next(number, NpcTacticChoices.kind(npc));
                button.setDisplay(number);
            }
            editValue(value, number);
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

    private void refreshPage()
    {
        // setSubGui already initializes both screens; rebuilding again resets the picker's controls.
        if (!hasSubGui())
            init();
    }

    /** @return whether the button belongs to a tab that lays out its own controls, which then handled it */
    private boolean ownPageButton(GuiButtonNop button)
    {
        if (button.id == CONTROLLER_ID)
        {
            NpcMovementController.values()[button.getValue()].apply(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions());
            changed = true;
        }
        else if (teamFactions.handles(button.id))
            teamFactions.button(button);
        else if (button.id == DIAGNOSIS_COPY_ID)
            copySummary();
        else if (heldItems.handles(button.id))
            changed |= heldItems.button(button);
        else if (crew.handles(button.id))
        {
            // An action can save the editor itself; do not restore the dirty flag captured before that save.
            boolean edited = crew.button(button);
            changed |= edited;
        }
        else if (GroundHandlingControls.button(npc, button))
            changed = true;
        else
            return false;
        return true;
    }

    /** @return whether the button only moved between pages or changed which pages are listed */
    private boolean navigate(GuiButtonNop button)
    {
        int id = button.id;
        if (navigateMovement(id))
            return true;
        if (id == BACK_BUTTON_ID)
            goBack();
        else if (category == Category.SOUNDS && !combat.active() && soundOverview.handles(id))
            openSoundSource(soundOverview.target(id));
        else if (id == MODEL_SOUNDS_LINK_ID)
            combat.openPropertySounds();
        else if (id >= CATEGORY_BUTTON_ID && id < CATEGORY_BUTTON_ID + Category.values().length)
            openCategory(Category.values()[id - CATEGORY_BUTTON_ID]);
        else if (category == null && id >= HUB_BUTTON_ID && id < HUB_BUTTON_ID + Hub.values().length)
            selectHub(Hub.values()[id - HUB_BUTTON_ID]);
        else if (presetButton(button))
            return true;
        else if (category == Category.ESSENTIALS && id >= ESSENTIAL_LINK_ID && id < ESSENTIAL_LINK_ID + essentialLinks().size())
        {
            openCategory(essentialLinks().get(id - ESSENTIAL_LINK_ID));
            returnTo = Category.ESSENTIALS;
        }
        else if (category != null && id >= TAB_BUTTON_ID && id < TAB_BUTTON_ID + tabs(category).size())
            tab = id - TAB_BUTTON_ID;
        else
            return false;
        return true;
    }

    /** Section expansion and scrolling only change editor navigation, never the NPC's dirty flag. */
    private boolean navigateMovement(int id)
    {
        if (category != Category.BOARDING)
            return false;
        if (id >= MOVEMENT_SECTION_ID && id < MOVEMENT_SECTION_ID + category.tabs.size())
        {
            String section = category.tabs.get(id - MOVEMENT_SECTION_ID).name();
            if (!toggledMovementSections.add(section))
                toggledMovementSections.remove(section);
        }
        else if (id == MOVEMENT_UP_ID || id == MOVEMENT_DOWN_ID)
            movementOffset += id == MOVEMENT_UP_ID ? -1 : 1;
        else
            return false;
        return true;
    }

    private boolean presetButton(GuiButtonNop button)
    {
        switch (button.id)
        {
            case PRESET_ID -> NpcPresetControls.select(npc, button.getValue());
            case PRESET_UNDO_ID -> NpcNetwork.sendToServer(NpcPresetPacket.undo(npc.getId()));
            case PRESET_APPLY_ID -> NpcNetwork.sendToServer(NpcPresetPacket.apply(npc.getId(), selectedPreset()));
            default -> {
                return false;
            }
        }
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
                List<Tab> tabs = tabs(Category.SOUNDS);
                for (int i = 0; i < tabs.size(); i++)
                    if (IDLE_MOVEMENT_TAB.equals(tabs.get(i).name()))
                        tab = i;
            }
        }
    }

    private void openCategory(Category opened)
    {
        category = opened;
        returnTo = null;
        tab = 0;
        movementOffset = 0;
        inspectTicks = 0;
        if (opened == Category.CENSUS || opened == Category.DIAGNOSTICS)
            requestReport();
    }

    private void goBack()
    {
        if (combat.back())
            return;
        if (editingSound != null)
            editingSound = null;
        else if (returnTo != null)
        {
            category = returnTo;
            returnTo = null;
            tab = 0;
        }
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
            case KEEP_WRECK_ID -> settings.setKeepWreck(yes);
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
        // A clone list may arrive after its selection menu was closed. It is not NPC statistics.
        if (tag.contains("List"))
            return;
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

    @Override
    public void mouseDragged(GuiSliderNop slider)
    {
        float value = slider.sliderValue;
        GuiTextFieldNop.unfocus();
        slider.sliderValue = value;
        changed |= heldItems.slider(slider);
    }

    @Override
    public void mousePressed(GuiSliderNop slider)
    {
        GuiTextFieldNop.unfocus();
    }

    @Override
    public void mouseReleased(GuiSliderNop slider)
    {
        changed |= heldItems.slider(slider);
    }
}
