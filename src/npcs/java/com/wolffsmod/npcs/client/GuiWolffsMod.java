package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.NpcKind.Scope;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.RequiredArgsConstructor;
import noppes.npcs.client.CustomNpcResourceListener;
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
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Per-NPC controls inside the Wolff's Mod tab, using the Custom NPCs stats transport and sound picker.
 *
 * <p>
 * The overview names the kind of NPC being edited, a standard NPC or a Flan model, and lists the pages meant for it under
 * section headings, one column per heading; Show all lists the others too. A page is a list of rows, split into named
 * tabs where they do not fit together, and may link to a sub-page. The title of a page is the path to it. The Essentials
 * page gathers the settings most NPCs of the kind need. A row that does nothing for this NPC, because of its kind or
 * because a setting it depends on is off, keeps its control but has a grey label that says why; a row that depends on
 * another row of the same tab is indented below it. A page or tab with settings changed from their defaults shows how
 * many, and is listed even when it is not meant for the kind, so that nothing in effect is hidden.
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
    private static final int ESSENTIALS_BUTTON_ID = 301;
    private static final int SHOW_ALL_ID = 302;
    private static final int SERVER_BUTTON_ID = 303;
    private static final int ESSENTIAL_LINK_ID = 310;
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
    /** Distance between the left edges of two overview columns; a page button is this wide less the gap. */
    private static final int OVERVIEW_COLUMN = 134;
    private static final int OVERVIEW_GAP = 6;
    private static final int OVERVIEW_HEADING_HEIGHT = 12;
    /** The title row of the overview: the kind of NPC, then Essentials, Show all and Server. */
    private static final int OVERVIEW_TITLE_WIDTH = 170;
    private static final int ESSENTIALS_LEFT = 184;
    private static final int SHOW_ALL_LEFT = 250;
    private static final int SERVER_LEFT = 344;
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
    private static final String ARMOUR_TAB = "model_armour";
    private static final String PARTS_TAB = "parts";
    private static final String PATH_SEPARATOR = " > ";
    /** Room for the title of a page, which only Back follows. */
    private static final int TITLE_WIDTH = 340;
    /** What stands for the outer parts of a page path that do not fit. */
    private static final String TRIMMED = "..." + PATH_SEPARATOR;
    private static final String HELP_SUFFIX = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    /** Whether the overview lists every page rather than those meant for the NPC's kind; kept while the game runs. */
    private static final AtomicBoolean SHOW_ALL = new AtomicBoolean();

    /** A heading of the overview. The pages under the headings of one column follow each other in it. */
    @RequiredArgsConstructor
    private enum Section
    {
        MODEL(0), COMBAT(1), MOVEMENT(2), LOOK(0), ADVANCED(1);

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
        HURT_FLASH(HURT_FLASH_ID, true), DEATH_ROTATION(DEATH_ROTATION_ID, true), HIDE_BODY(HIDE_BODY_ID, false);

        private final int id;
        private final boolean fallback;
    }

    /**
     * A part of a page: its rows, listed top to bottom in the order given, and the NPCs it is meant for. Only the tabs of a
     * page with several are named.
     */
    private record Tab(String name, Scope scope, List<Object> rows)
    {
        private static Tab of(Object... rows)
        {
            return new Tab("", Scope.ALL, List.of(rows));
        }

        private static Tab named(String name, Object... rows)
        {
            return named(name, Scope.ALL, rows);
        }

        private static Tab named(String name, Scope scope, Object... rows)
        {
            return new Tab(name, scope, List.of(rows));
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
        Map.entry(Feature.CONSUME_AMMO, Need.on(Feature.ITEM_WEAPONS)),
        Map.entry(Feature.ARMOR_POTION_EFFECTS, Need.on(Feature.ITEM_ARMOR)),
        Map.entry(Value.WEAPON_SWITCH, Need.on(Feature.CONSUME_AMMO)),
        Map.entry(Feature.MODEL_WEAPON_STATS, Need.on(Feature.TYPE_PROPERTIES)),
        Map.entry(Feature.TURRET_MUZZLES, Need.on(Feature.MODEL_MUZZLES)),
        Map.entry(Feature.MANNED_GUNS_FIRE, Need.on(Feature.PASSENGER_GUNS)),
        Map.entry(Feature.ADVANCED_LEAD, Need.on(Feature.LEAD_TARGET)),
        Map.entry(Value.ALIGN_TOLERANCE, Need.on(Feature.FIRE_WHEN_ALIGNED)),
        Map.entry(Value.AIM_SPEED_YAW, Need.on(Feature.GRADUAL_AIM)),
        Map.entry(Value.AIM_SPEED_PITCH, Need.on(Feature.GRADUAL_AIM)),
        Map.entry(Feature.MODEL_ARMOUR, Need.on(Feature.VEHICLE_DAMAGE)),
        Map.entry(Feature.FLAN_DAMAGE_TYPES, Need.off(Feature.ITEM_ARMOR)),
        Map.entry(Feature.PART_HEALTH, Need.on(Feature.PART_HITBOXES)),
        Map.entry(Feature.PART_EFFECTS, Need.on(Feature.PART_HEALTH)),
        Map.entry(Feature.SOLID_HITBOXES, Need.on(Feature.PART_HITBOXES)),
        Map.entry(Value.VISION_LIMIT, Need.choice(Value.SIGHT, NpcTuning.Sight.SEE_THROUGH, NpcTuning.Sight.SEE_AND_FIRE_THROUGH)),
        Map.entry(Value.FIRE_LIMIT, Need.choice(Value.SIGHT, NpcTuning.Sight.SEE_AND_FIRE_THROUGH)),
        Map.entry(Value.MAX_WADE_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.GROUND)),
        Map.entry(Value.MIN_WATER_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT)),
        Map.entry(Value.SUBMARINE_DEPTH, Need.choice(Value.TERRAIN, NpcTuning.Terrain.WATERCRAFT)),
        Map.entry(Value.ATTACK_ALTITUDE, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE, NpcTuning.Flight.HELICOPTER)),
        Map.entry(Value.FLIGHT_SPEED, Need.choice(Value.FLIGHT_TYPE, NpcTuning.Flight.PLANE)),
        Map.entry(Value.TACTIC_DISTANCE, Need.choice(Value.TACTIC, Arrays.stream(NpcTuning.Tactic.values()).filter(NpcTuning.Tactic::usesDistance).toArray(Enum<?>[]::new))),
        Map.entry(Value.COVER_RADIUS, Need.choice(Value.TACTIC, NpcTuning.Tactic.COVER)),
        Map.entry(Feature.DRIVING, Need.on(Feature.HULL_FACES_TRAVEL)),
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
        Map.entry(Value.PATH_MARGIN, Need.on(Feature.ADAPTIVE_PATH_RANGE)),
        Map.entry(Value.PATH_RETRY_INTERVAL, Need.on(Feature.ADAPTIVE_PATH_RANGE)),
        Map.entry(Feature.DOORS_OPEN, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.GEAR_UP, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.WINGS_FOLDED, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.ENGINE_RUNNING, Need.on(Feature.ANIMATED_MODEL)),
        Map.entry(Feature.FULL_THROTTLE, Need.on(Feature.ENGINE_RUNNING)),
        Map.entry(Value.SEARCH_NEAR_INTERVAL, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Value.SEARCH_FAR_INTERVAL, Need.on(Feature.PROGRESSIVE_SEARCH)),
        Map.entry(Value.ACTIVE_RANGE, Need.on(Feature.STAY_ACTIVE)));
    // spotless:on

    /** A page of the tab, listed on the overview under its section in the order given here; one without a section opens from the title row. */
    private enum Category
    {
        // One page a line, one tab a line: the order here is the order on screen.
        // spotless:off
        /** The settings most NPCs of the edited kind need, with links to the pages they come from; its rows depend on the kind. */
        ESSENTIALS(null, Scope.ALL),
        MODEL_STATS(Section.MODEL, Scope.MODELS, Tab.of(Feature.TYPE_PROPERTIES, Feature.MODEL_WEAPON_STATS)),
        MODEL(Section.MODEL, Scope.MODELS, Tab.of(Feature.ANIMATED_MODEL, Feature.DOORS_OPEN, Feature.GEAR_UP, Feature.WINGS_FOLDED, Feature.ENGINE_RUNNING, Feature.FULL_THROTTLE,
            Feature.RIDER_AT_SEAT, Feature.MODEL_CULLING)),
        TURRET(Section.MODEL, Scope.MODELS,
            Tab.named("turret_aim", Feature.GRADUAL_AIM, Value.AIM_SPEED_YAW, Value.AIM_SPEED_PITCH, Feature.HULL_ASSISTS_AIM, Feature.PASSENGER_GUNS, Feature.MANNED_GUNS_FIRE,
                Feature.TURRET_LOCKED),
            Tab.named(CrewPage.TAB, Scope.DRIVEABLES)),
        EQUIPMENT(Section.COMBAT, Scope.STANDARD, Tab.of(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.ARMOR_POTION_EFFECTS, Feature.DUAL_WIELD, Feature.CONSUME_AMMO, Value.WEAPON_SWITCH)),
        FIRING(Section.COMBAT, Scope.ALL, Tab.of(Feature.PROJECTILES, Feature.GRENADES, Feature.NATIVE_THROWABLES)),
        MODEL_WEAPONS(Section.COMBAT, Scope.MODELS, Tab.of(Feature.SECONDARY_BANK, Feature.ALTERNATE_BARRELS, Feature.RELOAD_CYCLE, Feature.MODEL_MUZZLES, Feature.TURRET_MUZZLES,
            Feature.EYE_AT_MUZZLE)),
        AIMING(Section.COMBAT, Scope.ALL, Tab.of(Feature.LEAD_TARGET, Feature.ADVANCED_LEAD, Feature.BALLISTIC_AIM, Feature.FIRE_WHEN_ALIGNED, Value.ALIGN_TOLERANCE)),
        PROTECTION(Section.COMBAT, Scope.ALL,
            Tab.named(GENERAL_TAB, Feature.INVULNERABLE, Feature.FLAN_DAMAGE_TYPES),
            Tab.named(ARMOUR_TAB, Scope.MODELS, Feature.VEHICLE_DAMAGE, Feature.MODEL_ARMOUR),
            Tab.named(PARTS_TAB, Scope.DRIVEABLES, Feature.PART_HITBOXES, Feature.PART_HEALTH, Feature.PART_EFFECTS, Feature.BROKEN_PARTS_PASS, Feature.PART_REPAIR, Feature.SOLID_HITBOXES,
                Feature.HITBOX_OUTLINES)),
        TACTICS(Section.MOVEMENT, Scope.ALL, Tab.of(Value.TACTIC, Value.TACTIC_DISTANCE, Value.COVER_RADIUS)),
        DETECTION(Section.MOVEMENT, Scope.ALL, Tab.of(Range.AGGRO_RANGE, Range.FIRING_RANGE, Value.SIGHT, Value.VISION_LIMIT, Value.FIRE_LIMIT, Value.TARGET_MARGIN)),
        TERRAIN(Section.MOVEMENT, Scope.ALL, Tab.of(Value.TERRAIN, Value.MAX_WADE_DEPTH, Value.MIN_WATER_DEPTH, Value.LAND_SPEED, Value.WATER_SPEED)),
        DRIVING(Section.MOVEMENT, Scope.VEHICLES,
            Tab.named("handling", Feature.DRIVING, Value.DRIVE_PRESET, Value.DRIVE_TYPE, Value.DRIVE_ACCELERATION, Value.DRIVE_BRAKING, Value.DRIVE_COASTING, Value.TURN_RADIUS),
            Tab.named("steering", Feature.HULL_FACES_TRAVEL, Value.HULL_TURN_RATE, Feature.MISALIGNED_THROTTLE, Value.THROTTLE_FLOOR, Value.DRIVE_LOOKAHEAD, Value.CORNER_SPEED),
            Tab.named("reversing", Feature.DRIVE_REVERSE, Value.DRIVE_REVERSE_SPEED, Value.REVERSE_ANGLE, Value.REVERSE_DISTANCE, Value.STOP_DISTANCE, Value.FOLLOW_DISTANCE)),
        FLIGHT(Section.MOVEMENT, Scope.PLANES_AND_VEHICLES, Tab.of(Value.FLIGHT_TYPE, Value.ATTACK_ALTITUDE, Value.FLIGHT_SPEED, Value.SUBMARINE_DEPTH)),
        PATHFINDING(Section.MOVEMENT, Scope.ALL, Tab.of(Feature.ADAPTIVE_PATH_RANGE, Value.PATH_MARGIN, Value.PATH_RETRY_INTERVAL)),
        ANIMATIONS(Section.LOOK, Scope.STANDARD,
            Tab.named("equipment_animation", Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS, Feature.GUN_SHOT_ANIMATION, Feature.GUN_RELOAD_ANIMATION, Feature.GUN_MELEE_ANIMATION),
            Tab.named(HeldItemAnimationPage.ITEMS), Tab.named(HeldItemAnimationPage.ARMS_TAB)),
        EFFECTS(Section.LOOK, Scope.ALL, Tab.of(Look.HURT_FLASH, Look.DEATH_ROTATION, Look.HIDE_BODY, Feature.SHOOT_PARTICLES)),
        SOUNDS(Section.LOOK, Scope.ALL,
            Tab.named(OVERVIEW_TAB),
            Tab.named("weapon", Feature.FIRE_SOUNDS, Feature.RELOAD_SOUNDS, Feature.THROW_SOUNDS, Feature.MELEE_SOUNDS),
            Tab.named(IDLE_MOVEMENT_TAB)),
        PERFORMANCE(Section.ADVANCED, Scope.ALL, Tab.of(Feature.PROGRESSIVE_SEARCH, Value.SEARCH_NEAR_INTERVAL, Value.SEARCH_FAR_INTERVAL, Feature.STAY_ACTIVE, Value.ACTIVE_RANGE,
            Feature.PATH_DEBUG)),
        /** Settings and diagnostics of the whole server rather than of the NPC being edited; its controls are not rows. */
        SERVER(null, Scope.ALL, Tab.of());
        // spotless:on

        @Nullable
        private final Section section;
        private final Scope scope;
        private final List<Tab> tabs;

        Category(@Nullable Section section, Scope scope, Tab... tabs)
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
                case SERVER -> TUNING + (ClientServerSettings.isKnown() ? "server_hint" : "server_waiting");
                default -> TUNING + "hint";
            };
        }
    }

    private boolean loaded;
    private boolean changed;
    private Category category;
    /** The page that linked to the open one, which Back returns to instead of the overview. */
    @Nullable
    private Category returnTo;
    /** Which tab of a page with several is shown. */
    private int tab;
    /** Whether the next press of the button applying the ranges to every NPC carries it out; any other edit asks again. */
    private boolean confirmApplyRanges;
    private NpcKind kind;
    private NpcEngineSettings editingSound;
    private final CombatPages combat;
    private final SoundOverview soundOverview;
    private final HeldItemAnimationPage heldItems;
    private final CrewPage crew;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        combat = new CombatPages(this, npc);
        soundOverview = new SoundOverview(this, npc);
        heldItems = new HeldItemAnimationPage(this, npc);
        crew = new CrewPage(this, npc);
        kind = NpcKind.of(npc);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
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
        addLabel(new GuiLabel(HINT_LABEL_ID, loaded ? hint() : WEAPONS + "loading", guiLeft + 8, guiTop + 203));
    }

    private String hint()
    {
        if (category == null)
        {
            if (SHOW_ALL.get())
                return OVERVIEW + "hint_all";
            return OVERVIEW + (kind == NpcKind.STANDARD ? "hint_standard" : "hint_model");
        }
        return combat.active() ? combat.hint() : category.hint();
    }

    private static Font font()
    {
        return Minecraft.getInstance().font;
    }

    // Overview

    /**
     * The kind of NPC with the Essentials, Show all and Server buttons above one column per section column, each section a
     * heading followed by the buttons of its pages that are listed.
     */
    private void initOverview()
    {
        initOverviewTitle();
        Map<Integer, Integer> nextRow = new HashMap<>();
        for (Section section : Section.values())
        {
            List<Category> pages = Arrays.stream(Category.values()).filter(page -> page.section == section && listed(page)).toList();
            if (pages.isEmpty())
                continue;
            int x = guiLeft + 8 + section.column * OVERVIEW_COLUMN;
            // A second section in a column keeps its distance from the pages above it.
            int y = nextRow.containsKey(section.column) ? nextRow.get(section.column) + OVERVIEW_GAP : PAGE_TOP;
            addLabel(new GuiLabel(SECTION_LABEL_ID + section.ordinal(), section.key(), x, guiTop + y));
            y += OVERVIEW_HEADING_HEIGHT;
            for (Category page : pages)
            {
                pageButton(page, x, guiTop + y);
                y += ROW_HEIGHT;
            }
            nextRow.put(section.column, y);
        }
    }

    /** The kind of NPC, with its model, and the buttons of the title row. */
    private void initOverviewTitle()
    {
        Component title = NpcKind.model(npc).map(type -> (Component) Component.translatable(OVERVIEW + "title_model", Component.translatable(kind.key()), type.getName()))
            .orElseGet(() -> Component.translatable(kind.key()));
        String text = title.getString();
        if (font().width(text) > OVERVIEW_TITLE_WIDTH)
            text = font().plainSubstrByWidth(text, OVERVIEW_TITLE_WIDTH - font().width("...")) + "...";
        GuiLabel label = new GuiLabel(TITLE_LABEL_ID, Component.literal(text), CustomNpcResourceListener.getDefaultTextColor(), guiLeft + 8, guiTop + 10, 40, 10);
        label.setTooltip(Tooltip.create(title.copy().append("\n").append(Component.translatable(kind.key() + HELP_SUFFIX).withStyle(HELP_STYLE))));
        addLabel(label);
        titleButton(ESSENTIALS_BUTTON_ID, ESSENTIALS_LEFT, 64, Component.translatable(Category.ESSENTIALS.key()), Component.translatable(Category.ESSENTIALS.key() + HELP_SUFFIX), loaded);
        long hidden = Arrays.stream(Category.values()).filter(page -> page.section != null && !listed(page)).count();
        boolean all = SHOW_ALL.get();
        titleButton(SHOW_ALL_ID, SHOW_ALL_LEFT, 92, all ? Component.translatable(OVERVIEW + "fewer") : Component.translatable(OVERVIEW + "show_all", hidden),
            Component.translatable(OVERVIEW + (all ? "fewer" : "show_all") + HELP_SUFFIX), loaded && (all || hidden > 0));
        titleButton(SERVER_BUTTON_ID, SERVER_LEFT, 60, Component.translatable(OVERVIEW + "server"), Component.translatable(Category.SERVER.key() + HELP_SUFFIX), loaded);
    }

    private void titleButton(int id, int left, int width, Component message, Component tooltip, boolean enabled)
    {
        GuiButtonNop button = new GuiButtonNop(this, id, guiLeft + left, guiTop + 4, width, 20, new String[]{""}, 0);
        button.setMessage(message);
        button.setTooltip(Tooltip.create(tooltip));
        button.setEnabled(enabled);
        addButton(button);
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

    /** Whether the overview lists the page: it is meant for this kind of NPC, Show all is on, or it holds changed settings. */
    private boolean listed(Category page)
    {
        return SHOW_ALL.get() || page.scope.includes(kind) || changes(page) > 0;
    }

    /** @return the tabs of the page that are shown: as for pages, those meant for this kind, all of them, or changed ones */
    private List<Tab> tabs(Category page)
    {
        if (page == Category.ESSENTIALS)
            return List.of(Tab.of(essentials().toArray()));
        List<Tab> shown = page.tabs.stream().filter(candidate -> SHOW_ALL.get() || candidate.scope().includes(kind) || changes(candidate) > 0).toList();
        return shown.isEmpty() ? page.tabs : shown;
    }

    // Changed settings

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
        if (row instanceof Feature feature)
            return enabled(feature) != feature.isDefaultEnabled();
        if (row instanceof Value value)
            return NpcTuning.of(npc).get(value) != value.fallback();
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
            case VEHICLE -> List.of(Category.MODEL_STATS, Category.DRIVING);
            case PLANE -> List.of(Category.MODEL_STATS, Category.FLIGHT);
            case MECHA, AA_GUN -> List.of(Category.MODEL_STATS, Category.TURRET);
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
            titleButton(ESSENTIAL_LINK_ID + i, left, LINK_WIDTH, Component.translatable(link.key()), Component.translatable(OVERVIEW + "open", Component.literal(path(link))), loaded);
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
        else if (category == Category.SERVER)
            initServer();
        else
            initPage();
    }

    /** @return the path to the page shown, from its section, or the page that linked to it, down to the sub-page that is open */
    private String pageTitle()
    {
        String path = returnTo != null ? I18n.get(returnTo.key()) + PATH_SEPARATOR + I18n.get(category.key()) : path(category);
        if (combat.active())
            return path + PATH_SEPARATOR + I18n.get(combat.title());
        if (editingSound != null)
            return path + PATH_SEPARATOR + I18n.get(loopKey(editingSound == sounds().getIdle() ? "edit_idle" : "edit_movement"));
        return path;
    }

    /** @return the page's name after its section's, or alone for a page opened from the title row */
    private static String path(Category page)
    {
        return page.section == null ? I18n.get(page.key()) : I18n.get(page.section.key()) + PATH_SEPARATOR + I18n.get(page.key());
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
        List<Tab> tabs = tabs(category);
        tab = Math.max(0, Math.min(tabs.size() - 1, tab));
        int top = PAGE_TOP;
        if (tabs.size() > 1)
        {
            initTabButtons(tabs);
            top = TABBED_TOP;
        }
        Tab shown = tabs.get(tab);
        if (HeldItemAnimationPage.ITEMS.equals(shown.name()) || HeldItemAnimationPage.ARMS_TAB.equals(shown.name()))
        {
            heldItems.init(shown.name(), loaded, top);
            return;
        }
        if (CrewPage.TAB.equals(shown.name()))
        {
            crew.init(loaded, top);
            return;
        }
        if (initSoundTab(shown, top))
            return;
        if (category == Category.ESSENTIALS)
            initEssentialLinks();
        List<Object> rows = shown.rows().stream().filter(this::visible).toList();
        int control = initRows(rows, top);
        initPageExtras(shown, control, top + rows.size() * ROW_HEIGHT);
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
        else if (category == Category.MODEL_STATS && enabled(Feature.TYPE_PROPERTIES))
            combat.initPropertiesLink(loaded, below + 6);
        else if (category == Category.MODEL_STATS)
            // Everything else on the page reads the model, so nothing else is offered until inheritance is on.
            addLabel(new GuiLabel(TITLE_LABEL_ID + 2, "wolffsmodnpcs.combat.properties_disabled", guiLeft + 8, guiTop + below + 6));
        else if (category == Category.PROTECTION && GENERAL_TAB.equals(shown.name()))
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
            column = Math.max(column, 8 + indent(row, rows) + font().width(I18n.get(labelKey(row))) + 8);
        int control = guiLeft + column;
        for (int i = 0; i < rows.size(); i++)
        {
            int y = guiTop + top + i * ROW_HEIGHT;
            Object row = rows.get(i);
            rowLabel(row, rows, y);
            if (row instanceof Feature feature)
                featureButton(feature, control, y);
            else if (row instanceof Value value)
                valueControl(value, control, y);
            else if (row instanceof Range range)
                rangeField(range, control, y);
            else if (row instanceof Look look)
                switchButton(look.id, lookEnabled(look), control, y);
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

    private static int rowId(Object row)
    {
        if (row instanceof Value value)
            return VALUE_ID + value.ordinal();
        if (row instanceof Range range)
            return range.id;
        if (row instanceof Look look)
            return look.id;
        return ((Feature) row).ordinal();
    }

    /**
     * The label of a row, indented below what it depends on. A row that does nothing for this NPC is grey, and its help
     * starts with why.
     */
    private void rowLabel(Object row, List<Object> rows, int y)
    {
        String key = labelKey(row);
        GuiLabel label = new GuiLabel(rowId(row), key, guiLeft + 8 + indent(row, rows), y + 5, key + HELP_SUFFIX);
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
        if (row instanceof Feature feature)
            return Component.translatable(NO_YES[feature.isDefaultEnabled() ? 1 : 0]);
        if (row instanceof Look look)
            return Component.translatable(NO_YES[look.fallback ? 1 : 0]);
        if (!(row instanceof Value value))
            return null;
        Enum<?>[] choices = choices(value);
        if (choices.length > value.fallback() && value.fallback() >= 0)
            return Component.translatable(labelKey(value) + "." + choices[value.fallback()].name().toLowerCase(Locale.ROOT));
        return Component.literal(Integer.toString(value.fallback()));
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
        if (!need.values().contains(current(parent)))
        {
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

    /** @return the value of a switch, 1 for Yes, or of a choice or number */
    private int current(Enum<?> row)
    {
        if (row instanceof Feature feature)
            return enabled(feature) ? 1 : 0;
        return NpcTuning.of(npc).get((Value) row);
    }

    /** @return the page, and its tab, that holds the row */
    private static String location(Enum<?> row)
    {
        for (Category page : Category.values())
            for (Tab candidate : page.tabs)
                if (candidate.rows().contains(row))
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
                case TYPE_PROPERTIES, MODEL_WEAPON_STATS, SECONDARY_BANK, ALTERNATE_BARRELS, RELOAD_CYCLE, MODEL_MUZZLES, TURRET_MUZZLES, EYE_AT_MUZZLE, ADVANCED_LEAD, FIRE_WHEN_ALIGNED, GRADUAL_AIM,
                    PASSENGER_GUNS, TURRET_LOCKED, VEHICLE_DAMAGE, MODEL_ARMOUR, ANIMATED_MODEL, DOORS_OPEN, GEAR_UP, WINGS_FOLDED, ENGINE_RUNNING, FULL_THROTTLE, RIDER_AT_SEAT, MODEL_CULLING ->
                    Scope.MODELS;
                case PART_HITBOXES, PART_HEALTH, PART_EFFECTS, BROKEN_PARTS_PASS, PART_REPAIR, SOLID_HITBOXES, HITBOX_OUTLINES, MANNED_GUNS_FIRE -> Scope.DRIVEABLES;
                case HULL_ASSISTS_AIM, MISALIGNED_THROTTLE, DRIVING, DRIVE_REVERSE -> Scope.VEHICLES;
                case HULL_FACES_TRAVEL -> Scope.HULLS;
                default -> Scope.ALL;
            };
        }
        if (row instanceof Value value)
        {
            return switch (value)
            {
                case ALIGN_TOLERANCE, AIM_SPEED_YAW, AIM_SPEED_PITCH -> Scope.MODELS;
                case DRIVE_PRESET, DRIVE_TYPE, DRIVE_ACCELERATION, DRIVE_BRAKING, DRIVE_COASTING, TURN_RADIUS, THROTTLE_FLOOR, DRIVE_LOOKAHEAD, CORNER_SPEED, DRIVE_REVERSE_SPEED, REVERSE_ANGLE,
                    REVERSE_DISTANCE, STOP_DISTANCE, FOLLOW_DISTANCE -> Scope.VEHICLES;
                case HULL_TURN_RATE -> Scope.HULLS;
                case COVER_RADIUS -> Scope.STANDARD;
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
        };
    }

    /** The model sounds switch of the Idle & movement tab, a row outside a list of rows. */
    private void modelSoundsRow(int control, int y)
    {
        rowLabel(Feature.MODEL_SOUNDS, List.of(), y);
        featureButton(Feature.MODEL_SOUNDS, control, y);
    }

    private void featureButton(Feature feature, int control, int y)
    {
        GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), control, y, 48, 20, NO_YES, enabled(feature) ? 1 : 0);
        button.setEnabled(loaded);
        addButton(button);
    }

    private void valueControl(Value value, int control, int y)
    {
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
            case TACTIC -> NpcTuning.Tactic.values();
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
        serverField(SERVER_AGGRO_ID, "server_aggro_range", ClientServerSettings.getAggroRange(), NpcMobility.rangeLimit(), 92, known);
        serverField(SERVER_FIRING_ID, "server_firing_range", ClientServerSettings.getFiringRange(), NpcMobility.rangeLimit(), 113, known);
        GuiButtonNop apply = serverButton(SERVER_APPLY_RANGES_ID, confirmApplyRanges ? "server_apply_ranges_confirm" : "server_apply_ranges", 136);
        apply.setEnabled(known);
        serverButton(SERVER_BENCHMARK_ID, "server_benchmark", 158);
        serverButton(CLIENT_BENCHMARK_ID, "client_benchmark", 180);
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
        int near = ClientServerSettings.getNearInterval();
        int far = ClientServerSettings.getFarInterval();
        boolean profiling = ClientServerSettings.isProfiling();
        int aggro = ClientServerSettings.getAggroRange();
        int firing = ClientServerSettings.getFiringRange();
        boolean confirming = confirmApplyRanges;
        confirmApplyRanges = false;
        switch (id)
        {
            case SERVER_NEAR_ID -> near = number;
            case SERVER_FAR_ID -> far = number;
            case SERVER_PROFILE_ID -> profiling = number == 1;
            case SERVER_AGGRO_ID -> aggro = number;
            case SERVER_FIRING_ID -> firing = number;
            case SERVER_APPLY_RANGES_ID -> {
                // The first press only asks for a second one: the ranges of every NPC are overwritten.
                if (confirming)
                    NpcNetwork.sendToServer(ServerControlPacket.applyRanges());
                else
                    confirmApplyRanges = true;
                init();
                return true;
            }
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
        NpcNetwork.sendToServer(ServerControlPacket.set(near, far, profiling, aggro, firing));
        return true;
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
        return heldItems.field(field) || crew.field(field) || combat.unFocused(field);
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
        if (ownPageButton(button) || navigate(button))
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

    /** @return whether the button belongs to a tab that lays out its own controls, which then handled it */
    private boolean ownPageButton(GuiButtonNop button)
    {
        if (heldItems.handles(button.id))
            changed |= heldItems.button(button);
        else if (crew.handles(button.id))
            changed |= crew.button(button);
        else
            return false;
        return true;
    }

    /** @return whether the button only moved between pages or changed which pages are listed */
    private boolean navigate(GuiButtonNop button)
    {
        int id = button.id;
        if (id == BACK_BUTTON_ID)
            goBack();
        else if (category == Category.SOUNDS && !combat.active() && soundOverview.handles(id))
            openSoundSource(soundOverview.target(id));
        else if (id == MODEL_SOUNDS_LINK_ID)
            combat.openPropertySounds();
        else if (id >= CATEGORY_BUTTON_ID && id < CATEGORY_BUTTON_ID + Category.values().length)
            openCategory(Category.values()[id - CATEGORY_BUTTON_ID]);
        else if (id == ESSENTIALS_BUTTON_ID)
            openCategory(Category.ESSENTIALS);
        else if (id == SERVER_BUTTON_ID)
            openCategory(Category.SERVER);
        else if (id == SHOW_ALL_ID)
            SHOW_ALL.set(!SHOW_ALL.get());
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
