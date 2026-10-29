package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.EquipmentValues.Kind;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.*;
import java.util.regex.Pattern;

/**
 * The combat pages of the Wolff's Mod tab that are more than a list of switches: knockback and minimum hit damage on the
 * Damage & armour page, the model properties page with its Inherit/Custom modes that the Inherit from model page opens, the
 * weapon and armor properties pages with the same modes that the Equipment page opens, and the part hitbox page that the
 * Damage & armour page opens.
 *
 * <p>
 * Everything is edited on the client copy of the NPC's settings; the tab sends them through the Custom NPCs stats menu
 * when it is left. Each value cell shows the value the NPC uses and edits the same field as the standard editor; imports
 * are applied to that copy at once.
 * </p>
 */
final class CombatPages
{
    static final int KNOCKBACK_ID = 700;
    static final int MINIMUM_HIT_ID = 710;
    private static final Pattern MELEE_THRESHOLD_INPUT = Pattern.compile("-1?|\\d*");
    static final int PROPERTIES_PAGE_ID = 720;
    static final int MAIN_HAND_PAGE_ID = 721;
    static final int ARMOR_PAGE_ID = 723;
    static final int OFF_HAND_PAGE_ID = 724;
    static final int PROJECTILE_SLOT_PAGE_ID = 725;
    static final int PARTS_PAGE_ID = 722;
    static final int GROUP_ID = 730;
    static final int EQUIPMENT_GROUP_ID = 740;
    static final int IMPORT_ALL_ID = 760;
    static final int EQUIPMENT_IMPORT_ALL_ID = 761;
    static final int INHERIT_ALL_ID = 762;
    static final int EQUIPMENT_INHERIT_ALL_ID = 763;
    static final int EQUIPMENT_MODE_ID = 1500;
    static final int EQUIPMENT_IMPORT_ID = 1550;
    static final int EQUIPMENT_VALUE_ID = 1600;
    static final int PROPERTY_MODE_ID = 800;
    static final int PROPERTY_IMPORT_ID = 850;
    static final int PROPERTY_VALUE_ID = 900;
    static final int PROPERTY_SELECT_ID = 950;
    static final int EQUIPMENT_SELECT_ID = 1650;
    private static final int NOTICE_ID = 990;
    /** The engine rows of the Sounds tab, idle then movement: Inherit/Custom, value and Select. */
    private static final int ENGINE_MODE_ID = 1800;
    private static final int ENGINE_VALUE_ID = 1802;
    private static final int ENGINE_SELECT_ID = 1804;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    /** Width of each of the three property buttons that share a row of the Equipment page. */
    private static final int EQUIPMENT_LINK_WIDTH = 130;
    private static final int INHERITED_COLOR = 0x408040;
    private static final int MAX_VALUE_LENGTH = 20;
    /** A value cell's width, and that of a sound's, which shares the room with its Select button. */
    private static final int VALUE_WIDTH = 112;
    private static final int SOUND_VALUE_WIDTH = 96;
    private static final int SELECT_WIDTH = 36;
    /** The Inherit/Custom button of a row, from the row's left edge, kept narrow so that a sound's value has room. */
    private static final int MODE_LEFT = 258;
    private static final int MODE_WIDTH = 60;
    private static final String PREFIX = "wolffsmodnpcs.combat.";
    private static final String HELP_SUFFIX = ".help";
    private static final String IMPORT = PREFIX + "import";
    private static final String IMPORT_ALL = PREFIX + "import_all";
    private static final String INHERIT_ALL = PREFIX + "inherit_all";
    private static final String VALUE_OWN = PREFIX + "value.own";
    private static final String SELECT_HELP = PREFIX + "select" + HELP_SUFFIX;
    private static final String[] MODES = {PREFIX + "mode.inherit", PREFIX + "mode.custom"};
    /** Left edge of Inherit all; Import all follows it and Back follows that. */
    private static final int BULK_LEFT = 226;
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    /** The model properties that name a sound, whose rows offer the sound picker. */
    private static final Set<NpcTypeProperty> SOUND_PROPERTIES = Set.of(NpcTypeProperty.FIRING_SOUND, NpcTypeProperty.IDLE_SOUND, NpcTypeProperty.STEP_SOUND);
    /** The read-only tooltips of the standard editor already explain how each property is derived. */
    private static final Map<NpcTypeProperty, String> EXPLANATIONS = Map.ofEntries(Map.entry(NpcTypeProperty.HEALTH, "health"), Map.entry(NpcTypeProperty.AGGRO_RANGE, "aggro_range"),
        Map.entry(NpcTypeProperty.CAN_DROWN, "can_drown"), Map.entry(NpcTypeProperty.NO_FALL_DAMAGE, "no_fall_damage"), Map.entry(NpcTypeProperty.DAMAGE, "damage"),
        Map.entry(NpcTypeProperty.ACCURACY, "accuracy"), Map.entry(NpcTypeProperty.SPEED, "speed"), Map.entry(NpcTypeProperty.RANGE, "range"), Map.entry(NpcTypeProperty.DELAY_MIN, "min_delay"),
        Map.entry(NpcTypeProperty.DELAY_MAX, "max_delay"), Map.entry(NpcTypeProperty.BURST_DELAY, "burst_delay"), Map.entry(NpcTypeProperty.BURST, "burst_count"),
        Map.entry(NpcTypeProperty.SHOT_COUNT, "shot_count"), Map.entry(NpcTypeProperty.FIRING_SOUND, "firing_sound"), Map.entry(NpcTypeProperty.MELEE_REACH, "melee_reach"),
        Map.entry(NpcTypeProperty.WALKING_SPEED, "walking_speed"), Map.entry(NpcTypeProperty.IDLE_SOUND, "idle_sound"), Map.entry(NpcTypeProperty.STEP_SOUND, "step_sound"),
        Map.entry(NpcTypeProperty.POTION_IMMUNE, "potion_immune"), Map.entry(NpcTypeProperty.IGNORE_COBWEB, "ignore_cobweb"), Map.entry(NpcTypeProperty.HEALTH_REGEN, "health_regen"),
        Map.entry(NpcTypeProperty.COMBAT_REGEN, "combat_regen"), Map.entry(NpcTypeProperty.MELEE_DAMAGE, "melee_damage"), Map.entry(NpcTypeProperty.MELEE_DELAY, "melee_delay"));

    private final GuiBasic gui;
    private final EntityNPCInterface npc;
    private Page page = Page.NONE;
    private Group group = Group.GENERAL;
    private EquipmentProperty.Group equipmentGroup = EquipmentProperty.Group.MELEE;
    private final PartHitboxPage parts;
    private final ModelTuningRows tuning;
    /** The model property or equipment statistic whose Select button opened the sound picker, until the picker closes. */
    @Nullable
    private Enum<?> choosingSound;

    /** The page shown instead of a category's switches. */
    enum Page
    {
        NONE, PROPERTIES, MAIN_HAND, OFF_HAND, ARMOR, PARTS, PROJECTILE_SLOT;

        /** Whether the page lists equipment statistics, those of the weapons in hand or of the worn armor. */
        boolean equipment()
        {
            return slot() != null;
        }

        /** @return the equipment slot whose statistics the page lists, or null for a page of another kind */
        @Nullable
        EquipmentProperty.Slot slot()
        {
            return switch (this)
            {
                case MAIN_HAND -> EquipmentProperty.Slot.MAIN_HAND;
                case OFF_HAND -> EquipmentProperty.Slot.OFF_HAND;
                case ARMOR -> EquipmentProperty.Slot.ARMOR;
                case PROJECTILE_SLOT -> EquipmentProperty.Slot.PROJECTILE_SLOT;
                default -> null;
            };
        }
    }

    /**
     * Tabs of the model properties page; every property belongs to exactly one. Driving, Engine and Turning hold no property
     * but the movement values of a content-pack model, laid out by {@link ModelTuningRows}; Engine splits off Driving's mass
     * and engine power so a ground vehicle's rows fit the page.
     */
    enum Group
    {
        GENERAL(NpcTypeProperty.HEALTH, NpcTypeProperty.AGGRO_RANGE, NpcTypeProperty.MELEE_DAMAGE, NpcTypeProperty.MELEE_REACH, NpcTypeProperty.MELEE_DELAY, NpcTypeProperty.WALKING_SPEED), SURVIVAL(
            NpcTypeProperty.CAN_DROWN, NpcTypeProperty.NO_FALL_DAMAGE, NpcTypeProperty.POTION_IMMUNE, NpcTypeProperty.IGNORE_COBWEB, NpcTypeProperty.HEALTH_REGEN,
            NpcTypeProperty.COMBAT_REGEN), WEAPON(NpcTypeProperty.RANGE, NpcTypeProperty.DAMAGE, NpcTypeProperty.ACCURACY, NpcTypeProperty.SPEED, NpcTypeProperty.SHOT_COUNT), TIMING(
                NpcTypeProperty.DELAY_MIN, NpcTypeProperty.DELAY_MAX, NpcTypeProperty.BURST_DELAY,
                NpcTypeProperty.BURST), SOUNDS(NpcTypeProperty.FIRING_SOUND, NpcTypeProperty.IDLE_SOUND, NpcTypeProperty.STEP_SOUND), DRIVING, ENGINE, TURNING;

        private final List<NpcTypeProperty> properties;

        Group(NpcTypeProperty... properties)
        {
            this.properties = List.of(properties);
        }

        List<NpcTypeProperty> properties()
        {
            return properties;
        }
    }

    CombatPages(GuiBasic gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
        parts = new PartHitboxPage(gui, npc);
        tuning = new ModelTuningRows(gui, npc);
    }

    boolean active()
    {
        return page != Page.NONE;
    }

    String title()
    {
        return switch (page)
        {
            case PROPERTIES -> PREFIX + "properties";
            case PARTS -> PREFIX + "parts";
            case ARMOR -> PREFIX + "armor_properties";
            case OFF_HAND -> PREFIX + "off_hand_properties";
            case PROJECTILE_SLOT -> PREFIX + "projectile_slot_properties";
            default -> PREFIX + "main_hand_properties";
        };
    }

    String hint()
    {
        // With the slot's switch off, nothing on the page is inherited: that matters more than how to use the page.
        if (page.equipment() && !((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(equipmentGroup.feature()))
            return PREFIX + "equipment_off." + equipmentGroup.feature().name().toLowerCase(Locale.ROOT);
        return PREFIX + (page == Page.PARTS ? "parts_hint" : "import_hint");
    }

    /** @return whether a combat page was open and has been closed */
    boolean back()
    {
        if (page == Page.NONE)
            return false;
        page = Page.NONE;
        return true;
    }

    /** Opens the model properties page on its Sounds tab, as the Sounds page's overview links to it. */
    void openPropertySounds()
    {
        page = Page.PROPERTIES;
        group = Group.SOUNDS;
    }

    /** Opens an equipment page on the given tab, as the Sounds page's overview links to it. */
    void openEquipment(Page shown, EquipmentProperty.Group tab)
    {
        page = shown;
        equipmentGroup = tab;
    }

    private NpcCombatSettings settings()
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat();
    }

    // Category extras

    /** The button on the Inherit from model page that opens the model properties page. */
    void initPropertiesLink(boolean loaded, int y)
    {
        link(PROPERTIES_PAGE_ID, "properties", loaded, y);
    }

    /** The buttons on the Equipment page that open the weapon and the armor properties pages, side by side with what they do as tooltips. */
    void initEquipmentLink(boolean loaded, int y)
    {
        int[] ids = {MAIN_HAND_PAGE_ID, OFF_HAND_PAGE_ID, ARMOR_PAGE_ID};
        String[] names = {"main_hand_properties", "off_hand_properties", "armor_properties"};
        for (int i = 0; i < ids.length; i++)
        {
            GuiButtonNop button = new GuiButtonNop(gui, ids[i], gui.guiLeft + 8 + i * (EQUIPMENT_LINK_WIDTH + 2), gui.guiTop + y, EQUIPMENT_LINK_WIDTH, 20, new String[]{PREFIX + names[i]}, 0);
            button.setEnabled(loaded);
            button.setTooltip(Tooltip.create(Component.translatable(PREFIX + names[i] + ".summary")));
            gui.addButton(button);
        }
    }

    /** Native projectile-slot overrides have their own page, independent of weapons in hand. */
    void initProjectileSlotLink(boolean loaded, int y)
    {
        link(PROJECTILE_SLOT_PAGE_ID, "projectile_slot_properties", loaded, y);
    }

    /** The button on the Damage & armour page that opens the part hitbox page. */
    void initPartsLink(boolean loaded)
    {
        // The tab is full of switches, so the button sits in the title row, left of Back.
        GuiButtonNop button = new GuiButtonNop(gui, PARTS_PAGE_ID, gui.guiLeft + 236, gui.guiTop + 4, 114, 20, new String[]{PREFIX + "parts"}, 0);
        button.setEnabled(loaded);
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + "parts.summary")));
        gui.addButton(button);
    }

    private void link(int id, String name, boolean loaded, int y)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, gui.guiLeft + 8, gui.guiTop + y, 160, 20, new String[]{PREFIX + name}, 0);
        button.setEnabled(loaded);
        gui.addButton(button);
        gui.addLabel(new GuiLabel(id, PREFIX + name + ".summary", gui.guiLeft + 176, gui.guiTop + y + 6));
    }

    /** Knockback handling and the minimum hit damage of each kind, below the General switches of the Damage & armour page and in their column. */
    void initProtection(boolean loaded, int control, int y)
    {
        NpcCombatSettings settings = settings();
        int x = gui.guiLeft + 8;
        String knockback = PREFIX + "knockback";
        gui.addLabel(new GuiLabel(KNOCKBACK_ID, knockback, x, gui.guiTop + y + 5, knockback + HELP_SUFFIX));
        GuiButtonNop mode = new GuiButtonNop(gui, KNOCKBACK_ID, control, gui.guiTop + y, 90, 20, names(knockback, Knockback.values()), settings.getKnockback().ordinal());
        mode.setEnabled(loaded);
        gui.addButton(mode);
        NpcDamageCategory[] kinds = {NpcDamageCategory.PROJECTILE, NpcDamageCategory.MELEE, NpcDamageCategory.EXPLOSION};
        for (int i = 0; i < kinds.length; i++)
        {
            int rowY = gui.guiTop + y + 21 * (i + 1);
            int id = MINIMUM_HIT_ID + kinds[i].ordinal();
            String key = PREFIX + "minimum_hit." + kinds[i].name().toLowerCase(Locale.ROOT);
            gui.addLabel(new GuiLabel(id, key, x, rowY + 5, PREFIX + "minimum_hit" + HELP_SUFFIX));
            boolean melee = kinds[i] == NpcDamageCategory.MELEE;
            int value = melee && settings.inheritsMeleeThreshold() ? -1 : settings.minimumHit(kinds[i]);
            GuiTextFieldNop field = new GuiTextFieldNop(id, gui, control, rowY, 60, 20, Integer.toString(value));
            field.setEditable(loaded);
            field.setTextColorUneditable(READ_ONLY_COLOR);
            // Custom NPCs' numbersOnly filter rejects minus signs even when the minimum is negative.
            field.numbersOnly = !melee;
            if (melee)
                field.setFilter(text -> MELEE_THRESHOLD_INPUT.matcher(text).matches());
            field.setMinMaxDefault(melee ? -1 : 0, NpcCombatSettings.MAX_MINIMUM_HIT, melee ? -1 : 0);
            gui.addTextField(field);
        }
    }

    private static String[] names(String key, Enum<?>[] values)
    {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++)
            names[i] = key + "." + values[i].name().toLowerCase(Locale.ROOT);
        return names;
    }

    // Pages

    void init(boolean loaded)
    {
        if (page == Page.PROPERTIES)
            initProperties(loaded);
        else if (page.equipment())
            initEquipment(loaded);
        else if (page == Page.PARTS)
            parts.init(loaded);
    }

    private void initProperties(boolean loaded)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        properties.refresh(npc);
        Map<NpcTypeProperty, Tag> inherited = properties.inherited();
        Map<NpcTypeProperty, String> readouts = NpcTypeReadouts.create(properties.type().orElse(null), inherited, options, properties.usesNativeProjectile());
        initGroupTabs(loaded);
        if (ModelTuningRows.holds(group))
        {
            bulkButtons(INHERIT_ALL_ID, loaded && tuning.anyCustom(group), IMPORT_ALL_ID, loaded && tuning.anyInherited(group), INHERIT_ALL + HELP_SUFFIX, IMPORT_ALL + HELP_SUFFIX);
            tuning.init(group, gui.guiTop + 55, loaded);
            return;
        }
        NpcCombatSettings settings = settings();
        bulkButtons(INHERIT_ALL_ID, loaded && group.properties().stream().anyMatch(property -> properties.supplied().contains(property) && settings.isCustom(property)), IMPORT_ALL_ID,
            loaded && group.properties().stream().anyMatch(inherited::containsKey), INHERIT_ALL + HELP_SUFFIX, IMPORT_ALL + HELP_SUFFIX);
        if (inherited.isEmpty())
            gui.addLabel(new GuiLabel(NOTICE_ID, PREFIX + (options.enabled(Feature.TYPE_PROPERTIES) ? "no_model" : "properties_disabled"), gui.guiLeft + 8, gui.guiTop + 186));
        // A vehicle or plane sounds like its engine: its engine rows replace the idle and step sounds a model offers other NPCs.
        boolean engine = NpcPresentation.vehicleType(npc).isPresent();
        List<NpcTypeProperty> rows = group.properties().stream().filter(property -> !engine || property != NpcTypeProperty.IDLE_SOUND && property != NpcTypeProperty.STEP_SOUND).toList();
        for (int i = 0; i < rows.size(); i++)
            propertyRow(rows.get(i), gui.guiTop + 55 + i * 21, loaded, inherited.get(rows.get(i)), readouts.get(rows.get(i)));
        if (rows.size() < group.properties().size())
        {
            engineRow(EngineChannel.IDLE, gui.guiTop + 55 + rows.size() * 21, loaded);
            engineRow(EngineChannel.MOVEMENT, gui.guiTop + 55 + (rows.size() + 1) * 21, loaded);
        }
    }

    /** The tabs of the model properties page; the movement tabs are offered only for a model that has movement values to supply. */
    private void initGroupTabs(boolean loaded)
    {
        List<Group> groups = Arrays.stream(Group.values()).filter(candidate -> ModelTuningRows.visible(npc, candidate)).toList();
        if (!groups.contains(group))
            group = Group.GENERAL;
        int width = (396 + 2) / groups.size() - 2;
        for (int i = 0; i < groups.size(); i++)
        {
            Group tabGroup = groups.get(i);
            GuiButtonNop tab = new GuiButtonNop(gui, GROUP_ID + tabGroup.ordinal(), gui.guiLeft + 8 + i * (width + 2), gui.guiTop + 29, width, 20,
                new String[]{PREFIX + "group." + tabGroup.name().toLowerCase(Locale.ROOT)}, 0);
            tab.setEnabled(loaded && tabGroup != group);
            gui.addButton(tab);
        }
    }

    /** The two engine sounds of a vehicle or plane, which the Idle & movement tab of the Sounds page also edits. */
    enum EngineChannel
    {
        IDLE, MOVEMENT;

        private boolean moving()
        {
            return this == MOVEMENT;
        }
    }

    /**
     * A row for one engine sound, on the settings the Idle & movement tab edits: Inherit plays the model's engine sounds, Custom
     * the NPC's own. The two rows share that choice, as the tab does.
     */
    private void engineRow(EngineChannel channel, int y, boolean loaded)
    {
        NpcVehicleSounds sounds = NpcPresentation.of(npc).getVehicleSounds();
        int x = gui.guiLeft + 8;
        int index = channel.ordinal();
        String name = PREFIX + "property.engine_" + channel.name().toLowerCase(Locale.ROOT) + "_sound";
        gui.addLabel(new GuiLabel(ENGINE_MODE_ID + index, name, x, y + 5, name + HELP_SUFFIX));
        boolean custom = !sounds.isModelDefaults();
        if (custom)
            valueControl(ENGINE_VALUE_ID + index, y, Kind.TEXT, StringTag.valueOf(engine(sounds, channel).getSound()), Cell.OWN, loaded, Component.translatable(VALUE_OWN));
        else
            valueControl(ENGINE_VALUE_ID + index, y, Kind.TEXT, StringTag.valueOf(NpcVehicleSounds.modelSound(npc, channel.moving())), Cell.INHERITED, loaded,
                Component.translatable(PREFIX + "value.inherited"));
        selectButton(ENGINE_SELECT_ID + index, ENGINE_VALUE_ID + index, y, loaded, SELECT_HELP);
        GuiButtonNop mode = new GuiButtonNop(gui, ENGINE_MODE_ID + index, x + MODE_LEFT, y, MODE_WIDTH, 20, MODES.clone(), custom ? 1 : 0);
        mode.setEnabled(loaded);
        mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "mode.engine" + HELP_SUFFIX)));
        gui.addButton(mode);
    }

    private static NpcEngineSettings engine(NpcVehicleSounds sounds, EngineChannel channel)
    {
        return channel.moving() ? sounds.getMovement() : sounds.getIdle();
    }

    /** Plays the NPC's own engine sounds from now on, starting from the model's. */
    private void customEngineSounds()
    {
        NpcVehicleSounds sounds = NpcPresentation.of(npc).getVehicleSounds();
        if (!sounds.isModelDefaults())
            return;
        sounds.setModelDefaults(false);
        sounds.initializeCustom(npc);
    }

    /** Inherit or Custom on an engine row; the value cell and Select are handled with the other fields and pickers. */
    private void engineButton(int id, GuiButtonNop button)
    {
        if (id >= ENGINE_MODE_ID + EngineChannel.values().length)
            return;
        if (button.getValue() == 1)
            customEngineSounds();
        else
            NpcPresentation.of(npc).getVehicleSounds().useVehicleDefaults(npc);
    }

    private static boolean engineControl(int id)
    {
        return id >= ENGINE_MODE_ID && id < ENGINE_SELECT_ID + EngineChannel.values().length;
    }

    /**
     * A row of the model properties page. Its value is the one the NPC uses: the model's, read-only, while inherited, or the
     * NPC's own, editable and the same value the standard editor shows, otherwise.
     */
    private void propertyRow(NpcTypeProperty property, int y, boolean loaded, @Nullable Tag value, @Nullable String readout)
    {
        NpcCombatSettings settings = settings();
        int x = gui.guiLeft + 8;
        int id = property.ordinal();
        String name = property.name().toLowerCase(Locale.ROOT);
        String muted = mutedModelSound(property);
        String explanation = muted != null ? muted : "wolffsmodnpcs.weapons.read_only.type_properties." + EXPLANATIONS.get(property);
        GuiLabel label = new GuiLabel(PROPERTY_MODE_ID + id, PREFIX + "property." + name, x, y + 5, explanation);
        if (muted != null)
            label.setColor(READ_ONLY_COLOR);
        gui.addLabel(label);
        // Mounts that differ in a value still decide it, though there is no single value to import.
        boolean supplied = NpcTypeProperties.of(npc).supplied().contains(property);
        boolean custom = settings.isCustom(property);
        Tag own = NpcTypeProperties.of(npc).own(npc, property);
        Kind kind = kind(own);
        if (supplied && !custom)
            valueControl(PROPERTY_VALUE_ID + id, y, kind, modelValue(value, readout), Cell.INHERITED, loaded, Component.translatable(PREFIX + "value.inherited"));
        else
            valueControl(PROPERTY_VALUE_ID + id, y, kind, own, Cell.OWN, loaded, Component.translatable(VALUE_OWN));
        if (SOUND_PROPERTIES.contains(property))
            selectButton(PROPERTY_SELECT_ID + id, PROPERTY_VALUE_ID + id, y, loaded, SELECT_HELP);
        GuiButtonNop mode = new GuiButtonNop(gui, PROPERTY_MODE_ID + id, x + MODE_LEFT, y, MODE_WIDTH, 20, MODES.clone(), custom ? 1 : 0);
        mode.setEnabled(loaded && supplied);
        mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "mode" + HELP_SUFFIX)));
        gui.addButton(mode);
        // Importing only means something for a single value the NPC keeps as its own.
        if (value != null && custom)
            importButton(PROPERTY_IMPORT_ID + id, y, loaded, IMPORT + HELP_SUFFIX);
    }

    /**
     * @return the explanation of a model sound that a switch of the Sounds page keeps from the NPC, which then plays its own,
     *         or null when the model's sound is not switched off or there is no model
     */
    @Nullable
    private String mutedModelSound(NpcTypeProperty property)
    {
        if (NpcTypeProperties.of(npc).type().isEmpty())
            return null;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (property == NpcTypeProperty.FIRING_SOUND && !options.enabled(Feature.FIRE_SOUNDS))
            return PREFIX + "muted.fire_sounds";
        if ((property == NpcTypeProperty.IDLE_SOUND || property == NpcTypeProperty.STEP_SOUND) && !options.enabled(Feature.MODEL_SOUNDS))
            return PREFIX + "muted.model_sounds";
        return null;
    }

    /** A Select button right of a sound's value cell, which narrows to make room; choosing a sound makes it the NPC's own. */
    private void selectButton(int id, int valueId, int y, boolean enabled, String help)
    {
        GuiTextFieldNop field = gui.getTextField(valueId);
        if (field != null)
            field.setWidth(SOUND_VALUE_WIDTH);
        GuiButtonNop select = new GuiButtonNop(gui, id, gui.guiLeft + 132 + SOUND_VALUE_WIDTH, y, SELECT_WIDTH, 20, new String[]{"wolffsmodnpcs.presentation.select_sound"}, 0);
        select.setEnabled(enabled);
        select.setTooltip(Tooltip.create(Component.translatable(help)));
        gui.addButton(select);
    }

    /** @return the model value to show: its readout, which lists each mount's value, unless it is a switch */
    @Nullable
    private static Tag modelValue(@Nullable Tag value, @Nullable String readout)
    {
        if (value instanceof ByteTag || readout == null)
            return value;
        return StringTag.valueOf(readout);
    }

    /** Who a value cell shows the value of, which decides whether it can be edited. */
    private enum Cell
    {
        /** The model's or the equipment's value, in use. */
        INHERITED,
        /** The NPC's own value, which the model currently supplies through a field it shares. */
        MODEL_LOCKED,
        /** The NPC's own value, editable. */
        OWN
    }

    private static Kind kind(Tag value)
    {
        if (value instanceof ByteTag)
            return Kind.FLAG;
        if (value instanceof IntTag)
            return Kind.INTEGER;
        return value instanceof FloatTag ? Kind.DECIMAL : Kind.TEXT;
    }

    /**
     * A value cell: a Yes/No button for a flag, a text field for anything else, or plain text for a value spread over
     * several fields. Only the NPC's own value can be edited; the standard editor shows and edits the same field.
     */
    private void valueControl(int id, int y, Kind kind, @Nullable Tag value, Cell cell, boolean loaded, Component tooltip)
    {
        int x = gui.guiLeft + 130;
        boolean editable = loaded && cell == Cell.OWN;
        if (value == null || kind == Kind.NONE)
        {
            String text = value == null ? "-" : value.getAsString();
            GuiLabel shown = new GuiLabel(id, Component.literal(shorten(text)), cell == Cell.INHERITED ? INHERITED_COLOR : READ_ONLY_COLOR, x, y + 5, 40, 0);
            shown.setTooltip(Tooltip.create(tooltip.copy().append("\n").append(text)));
            gui.addLabel(shown);
            return;
        }
        if (kind == Kind.FLAG && value instanceof NumericTag flag)
        {
            GuiButtonNop button = new GuiButtonNop(gui, id, x, y, 60, 20, NO_YES, flag.getAsByte() != 0 ? 1 : 0);
            button.setEnabled(editable);
            button.setTooltip(Tooltip.create(tooltip));
            gui.addButton(button);
            return;
        }
        GuiTextFieldNop field = new GuiTextFieldNop(id, gui, x, y, VALUE_WIDTH, 20, text(value));
        field.setMaxLength(256);
        field.setEditable(editable);
        field.setTextColorUneditable(cell == Cell.INHERITED ? INHERITED_COLOR : READ_ONLY_COLOR);
        field.setTooltip(Tooltip.create(tooltip));
        gui.addTextField(field);
    }

    private static String text(Tag value)
    {
        if (value instanceof FloatTag decimal)
        {
            float number = decimal.getAsFloat();
            return number == Math.rint(number) ? Long.toString((long) number) : String.format(Locale.ROOT, "%.2f", number);
        }
        return value.getAsString();
    }

    /** @return the field's value as a tag of the kind the cell edits, or null when it holds no valid number */
    @Nullable
    private static Tag parse(GuiTextFieldNop field, Kind kind)
    {
        return switch (kind)
        {
            case INTEGER -> field.isInteger() ? IntTag.valueOf(field.getInteger()) : null;
            case DECIMAL -> field.isFloat() ? FloatTag.valueOf(field.getFloat()) : null;
            case TEXT -> StringTag.valueOf(field.getValue());
            default -> null;
        };
    }

    private void importButton(int id, int y, boolean loaded, String help)
    {
        GuiButtonNop copy = new GuiButtonNop(gui, id, gui.guiLeft + 328, y, 76, 20, new String[]{IMPORT}, 0);
        copy.setEnabled(loaded);
        copy.setTooltip(Tooltip.create(Component.translatable(help)));
        gui.addButton(copy);
    }

    /** Inherit all and Import all, side by side in the title row left of Back; the title gives way to them. */
    private void bulkButtons(int inheritId, boolean inheritEnabled, int importId, boolean importEnabled, String inheritHelp, String importHelp)
    {
        GuiButtonNop inherit = new GuiButtonNop(gui, inheritId, gui.guiLeft + BULK_LEFT, gui.guiTop + 4, 62, 20, new String[]{INHERIT_ALL}, 0);
        inherit.setEnabled(inheritEnabled);
        inherit.setTooltip(Tooltip.create(Component.translatable(inheritHelp)));
        gui.addButton(inherit);
        GuiButtonNop copy = new GuiButtonNop(gui, importId, gui.guiLeft + BULK_LEFT + 64, gui.guiTop + 4, 62, 20, new String[]{IMPORT_ALL}, 0);
        copy.setEnabled(importEnabled);
        copy.setTooltip(Tooltip.create(Component.translatable(importHelp)));
        gui.addButton(copy);
    }

    /** @return how far the title of the open page may reach before the buttons beside it */
    int titleWidth()
    {
        return page == Page.PROPERTIES || page.equipment() ? BULK_LEFT - 12 : 340;
    }

    static String shorten(String text)
    {
        return text.length() <= MAX_VALUE_LENGTH ? text : "..." + text.substring(text.length() - MAX_VALUE_LENGTH + 3);
    }

    /**
     * One tab per kind of slot, one row per statistic: what the slot gives now, whether the NPC inherits it or keeps its
     * own value, and a button that copies the slot's value into the NPC's own.
     */
    private void initEquipment(boolean loaded)
    {
        // The weapon page holds the tabs of the weapons in hand, the armor page those of the worn armor.
        EquipmentProperty.Slot shown = page.slot();
        List<EquipmentProperty.Group> groups = Arrays.stream(EquipmentProperty.Group.values()).filter(candidate -> candidate.getSlot() == shown).toList();
        if (!groups.contains(equipmentGroup))
            equipmentGroup = groups.get(0);
        int width = (396 + 2) / groups.size() - 2;
        for (int i = 0; i < groups.size(); i++)
        {
            EquipmentProperty.Group tabGroup = groups.get(i);
            String key = PREFIX + "equipment_group." + tabGroup.name().toLowerCase(Locale.ROOT);
            GuiButtonNop tab = new GuiButtonNop(gui, EQUIPMENT_GROUP_ID + tabGroup.ordinal(), gui.guiLeft + 8 + i * (width + 2), gui.guiTop + 29, width, 20, new String[]{key}, 0);
            tab.setEnabled(loaded && tabGroup != equipmentGroup);
            tab.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
            gui.addButton(tab);
        }
        List<EquipmentProperty> rows = equipmentGroup.properties();
        Map<EquipmentProperty, String> values = new EnumMap<>(EquipmentProperty.class);
        rows.forEach(property -> Optional.ofNullable(EquipmentReadouts.value(npc, property)).ifPresent(value -> values.put(property, value)));
        NpcCombatSettings settings = settings();
        bulkButtons(EQUIPMENT_INHERIT_ALL_ID, loaded && rows.stream().anyMatch(settings::isCustom), EQUIPMENT_IMPORT_ALL_ID, loaded && !values.isEmpty(),
            PREFIX + "equipment_inherit_all" + HELP_SUFFIX, PREFIX + "equipment_import_all" + HELP_SUFFIX);
        for (int i = 0; i < rows.size(); i++)
            equipmentRow(rows.get(i), gui.guiTop + 55 + i * 21, loaded, values.get(rows.get(i)));
    }

    /**
     * A row of the weapon or armor properties page. Its value is the one the NPC uses: the slot's, read-only, while it takes the
     * statistic from its equipment, or the NPC's own, editable and the same value the standard editor shows, otherwise.
     */
    private void equipmentRow(EquipmentProperty property, int y, boolean loaded, @Nullable String value)
    {
        NpcCombatSettings settings = settings();
        int x = gui.guiLeft + 8;
        int id = property.ordinal();
        String key = PREFIX + "equipment." + property.name().toLowerCase(Locale.ROOT);
        boolean custom = settings.isCustom(property);
        boolean muted = mutedItemSound(property);
        GuiLabel label = new GuiLabel(EQUIPMENT_MODE_ID + id, key, x, y + 5, muted ? PREFIX + "muted.item_fire_sounds" : key + HELP_SUFFIX);
        if (muted)
            label.setColor(READ_ONLY_COLOR);
        gui.addLabel(label);
        equipmentValue(property, y, loaded, value);
        if (soundStatistic(property))
            selectButton(EQUIPMENT_SELECT_ID + id, EQUIPMENT_VALUE_ID + id, y, loaded && !EquipmentValues.modelLocked(npc, property),
                PREFIX + (EquipmentValues.modelLocked(npc, property) ? "equipment_value.model" : "select" + HELP_SUFFIX));
        GuiButtonNop mode;
        if (property.isSwitchable())
        {
            mode = new GuiButtonNop(gui, EQUIPMENT_MODE_ID + id, x + MODE_LEFT, y, MODE_WIDTH, 20, MODES.clone(), custom ? 1 : 0);
            mode.setEnabled(loaded);
            mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "equipment_mode" + HELP_SUFFIX)));
        }
        else
        {
            // The item always times its own shots and fires its own rounds; the NPC's values only serve without it.
            mode = new GuiButtonNop(gui, EQUIPMENT_MODE_ID + id, x + MODE_LEFT, y, MODE_WIDTH, 20, new String[]{PREFIX + "mode.item"}, 0);
            mode.setEnabled(false);
            mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "mode.item" + HELP_SUFFIX)));
        }
        gui.addButton(mode);
        // A statistic the item always decides can only be copied; any other only means importing while the NPC keeps its own.
        if (value != null && property.importable() && (!property.isSwitchable() || custom))
            importButton(EQUIPMENT_IMPORT_ID + id, y, loaded, PREFIX + (property.isSwitchable() ? "equipment_import" : "equipment_copy") + HELP_SUFFIX);
    }

    private static Tag inheritedValue(Kind kind, @Nullable String value)
    {
        if (kind == Kind.FLAG)
            return ByteTag.ONE;
        return StringTag.valueOf(value == null ? "-" : value);
    }

    /**
     * A statistic spread over several fields: the slot's value while the item decides it, otherwise the NPC's own melee
     * effect, which the standard editor edits.
     *
     * @param itemValue
     *            the slot's value while the item decides the statistic, or null when the NPC's own applies
     */
    private void severalFields(EquipmentProperty property, int id, int y, boolean loaded, @Nullable String itemValue)
    {
        if (itemValue != null || !property.isSwitchable())
        {
            valueControl(id, y, Kind.NONE, itemValue == null ? null : StringTag.valueOf(itemValue), Cell.INHERITED, loaded, Component.translatable(PREFIX + "equipment_value.inherited"));
            return;
        }
        valueControl(id, y, Kind.NONE, StringTag.valueOf(EquipmentReadouts.ownEffect(npc)), Cell.OWN, loaded, Component.translatable(PREFIX + "equipment_value.own_effect"));
    }

    private void equipmentValue(EquipmentProperty property, int y, boolean loaded, @Nullable String value)
    {
        int id = EQUIPMENT_VALUE_ID + property.ordinal();
        Kind kind = EquipmentValues.kind(property);
        boolean inUse = EquipmentValues.inUse(npc, property);
        Component inherited = Component.translatable(PREFIX + "equipment_value.inherited");
        if (kind == Kind.NONE)
            severalFields(property, id, y, loaded, inUse || !property.isSwitchable() ? value : null);
        else if (inUse)
            // An armor effect is only in use where the armor grants it.
            valueControl(id, y, kind, inheritedValue(kind, value), Cell.INHERITED, loaded, inherited);
        else if (EquipmentValues.modelLocked(npc, property))
            valueControl(id, y, kind, EquipmentValues.read(npc, property), Cell.MODEL_LOCKED, loaded, Component.translatable(PREFIX + "equipment_value.model"));
        else
            valueControl(id, y, kind, EquipmentValues.read(npc, property), Cell.OWN, loaded, Component.translatable(VALUE_OWN));
    }

    /** Whether the statistic is a firing sound the NPC can keep its own value of, whose row offers the sound picker. */
    private static boolean soundStatistic(EquipmentProperty property)
    {
        return property.isSwitchable() && EquipmentValues.kind(property) == Kind.TEXT;
    }

    /** Whether the hand's Flan gun gives the statistic's firing sound, but Flan firing sounds are off, so the gun fires silently. */
    private boolean mutedItemSound(EquipmentProperty property)
    {
        if (!soundStatistic(property) || !EquipmentValues.inUse(npc, property) || NpcEquipment.enabled(npc, Feature.FIRE_SOUNDS))
            return false;
        InteractionHand hand = property == property.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return NpcEquipment.flan(npc, hand).isPresent();
    }

    // Sound picker

    /**
     * Remembers which row's Select button was clicked.
     *
     * @return the sound to open the picker on, the value the row shows, or null when the button is not a Select button
     */
    @Nullable
    String soundSelection(int id)
    {
        if (page == Page.PROPERTIES && id >= ENGINE_SELECT_ID && id < ENGINE_SELECT_ID + EngineChannel.values().length)
        {
            EngineChannel channel = EngineChannel.values()[id - ENGINE_SELECT_ID];
            choosingSound = channel;
            NpcVehicleSounds sounds = NpcPresentation.of(npc).getVehicleSounds();
            return sounds.isModelDefaults() ? NpcVehicleSounds.modelSound(npc, channel.moving()) : engine(sounds, channel).getSound();
        }
        if (page == Page.PROPERTIES && id >= PROPERTY_SELECT_ID && id < PROPERTY_SELECT_ID + NpcTypeProperty.values().length)
        {
            NpcTypeProperty property = NpcTypeProperty.values()[id - PROPERTY_SELECT_ID];
            choosingSound = property;
            NpcTypeProperties properties = NpcTypeProperties.of(npc);
            Tag shown = properties.supplied().contains(property) && !settings().isCustom(property) ? properties.inherited().get(property) : properties.own(npc, property);
            return shown == null ? "" : shown.getAsString();
        }
        if (page.equipment() && id >= EQUIPMENT_SELECT_ID && id < EQUIPMENT_SELECT_ID + EquipmentProperty.values().length)
        {
            EquipmentProperty property = EquipmentProperty.values()[id - EQUIPMENT_SELECT_ID];
            choosingSound = property;
            Tag own = EquipmentValues.read(npc, property);
            return own == null ? "" : own.getAsString();
        }
        return null;
    }

    /**
     * Makes the sound chosen in the picker the NPC's own value of the row that opened it, which then stops inheriting; the
     * picker closed without a choice changes nothing.
     *
     * @return whether a setting changed that has to be saved
     */
    boolean soundChosen(@Nullable ResourceLocation sound)
    {
        Enum<?> row = choosingSound;
        choosingSound = null;
        if (sound == null)
            return false;
        StringTag value = StringTag.valueOf(sound.toString());
        if (row instanceof EngineChannel channel)
        {
            customEngineSounds();
            engine(NpcPresentation.of(npc).getVehicleSounds(), channel).setSound(sound.toString());
            return true;
        }
        if (row instanceof NpcTypeProperty property)
        {
            NpcTypeProperties properties = NpcTypeProperties.of(npc);
            if (properties.supplied().contains(property))
                settings().setCustom(property, true);
            properties.setOwn(npc, property, value);
            return true;
        }
        if (row instanceof EquipmentProperty property && !EquipmentValues.modelLocked(npc, property))
        {
            settings().setCustom(property, true);
            EquipmentValues.write(npc, property, value);
            return true;
        }
        return false;
    }

    // Events

    boolean handles(int id)
    {
        return id == KNOCKBACK_ID || id == PROPERTIES_PAGE_ID || id == MAIN_HAND_PAGE_ID || id == OFF_HAND_PAGE_ID || id == ARMOR_PAGE_ID || id == PROJECTILE_SLOT_PAGE_ID || id == PARTS_PAGE_ID
            || parts.handles(id) || id == IMPORT_ALL_ID || id == EQUIPMENT_IMPORT_ALL_ID || id == INHERIT_ALL_ID || id == EQUIPMENT_INHERIT_ALL_ID
            || id >= GROUP_ID && id < GROUP_ID + Group.values().length || id >= EQUIPMENT_GROUP_ID && id < EQUIPMENT_GROUP_ID + EquipmentProperty.Group.values().length || equipmentControl(id)
            || id >= PROPERTY_MODE_ID && id < PROPERTY_VALUE_ID + NpcTypeProperty.values().length || engineControl(id) || ModelTuningRows.handles(id);
    }

    private static boolean equipmentControl(int id)
    {
        return id >= EQUIPMENT_MODE_ID && id < EQUIPMENT_MODE_ID + EquipmentProperty.values().length || id >= EQUIPMENT_IMPORT_ID && id < EQUIPMENT_IMPORT_ID + EquipmentProperty.values().length
            || id >= EQUIPMENT_VALUE_ID && id < EQUIPMENT_VALUE_ID + EquipmentProperty.values().length;
    }

    /** A click on the import, mode or value buttons of the weapon or armor properties page. */
    private void equipmentEvent(NpcCombatSettings settings, GuiButtonNop button)
    {
        int id = button.id;
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        // Imports are applied to the editor's copy of the NPC at once, and sent with the rest when the tab is left.
        if (id == EQUIPMENT_INHERIT_ALL_ID)
            equipmentGroup.properties().forEach(property -> settings.setCustom(property, false));
        else if (id == EQUIPMENT_IMPORT_ALL_ID)
            equipmentGroup.properties().stream().filter(property -> EquipmentReadouts.value(npc, property) != null).forEach(property -> properties.importNow(npc, property));
        else if (id >= EQUIPMENT_VALUE_ID)
        {
            EquipmentProperty property = EquipmentProperty.values()[id - EQUIPMENT_VALUE_ID];
            if (ownEditable(property))
                EquipmentValues.write(npc, property, ByteTag.valueOf(button.getValue() == 1));
        }
        else if (id >= EQUIPMENT_IMPORT_ID)
            properties.importNow(npc, EquipmentProperty.values()[id - EQUIPMENT_IMPORT_ID]);
        else
            equipmentMode(settings, EquipmentProperty.values()[id - EQUIPMENT_MODE_ID], button.getValue() == 1);
    }

    private void equipmentMode(NpcCombatSettings settings, EquipmentProperty property, boolean custom)
    {
        // A first native-speed override starts with the exact item default, including fractional tenths such as 0.75.
        if (property == EquipmentProperty.NATIVE_THROW_SPEED && custom && !Float.isFinite(settings.getNativeThrowSpeed()))
            NpcEquipmentImport.apply(npc, property);
        settings.setCustom(property, custom);
    }

    /** @return whether the button only moved to another page or tab */
    private boolean navigate(int id)
    {
        if (id == PROPERTIES_PAGE_ID)
            page = Page.PROPERTIES;
        else if (id == MAIN_HAND_PAGE_ID)
            page = Page.MAIN_HAND;
        else if (id == OFF_HAND_PAGE_ID)
            page = Page.OFF_HAND;
        else if (id == ARMOR_PAGE_ID)
            page = Page.ARMOR;
        else if (id == PROJECTILE_SLOT_PAGE_ID)
            page = Page.PROJECTILE_SLOT;
        else if (id == PARTS_PAGE_ID)
            page = Page.PARTS;
        else if (id >= EQUIPMENT_GROUP_ID && id < EQUIPMENT_GROUP_ID + EquipmentProperty.Group.values().length)
            equipmentGroup = EquipmentProperty.Group.values()[id - EQUIPMENT_GROUP_ID];
        else if (id >= GROUP_ID && id < GROUP_ID + Group.values().length)
            group = Group.values()[id - GROUP_ID];
        else
            return false;
        return true;
    }

    /** @return whether the click changed a setting that has to be saved */
    boolean buttonEvent(GuiButtonNop button)
    {
        NpcCombatSettings settings = settings();
        int id = button.id;
        if (navigate(id))
            return false;
        if (id == EQUIPMENT_IMPORT_ALL_ID || id == EQUIPMENT_INHERIT_ALL_ID || equipmentControl(id))
        {
            equipmentEvent(settings, button);
            return true;
        }
        if (engineControl(id))
        {
            engineButton(id, button);
            return true;
        }
        if (parts.handles(id))
            return parts.buttonEvent(button);
        if (tuning.claims(id, group))
            return tuning.event(group, button);
        if (id == KNOCKBACK_ID)
            settings.setKnockback(Knockback.values()[Math.floorMod(button.getValue(), Knockback.values().length)]);
        else if (id == INHERIT_ALL_ID)
            group.properties().forEach(property -> settings.setCustom(property, false));
        else if (id == IMPORT_ALL_ID)
            group.properties().forEach(property -> NpcTypeProperties.of(npc).importNow(npc, property));
        else if (id >= PROPERTY_VALUE_ID)
        {
            NpcTypeProperty property = NpcTypeProperty.values()[id - PROPERTY_VALUE_ID];
            if (ownEditable(property))
                NpcTypeProperties.of(npc).setOwn(npc, property, ByteTag.valueOf(button.getValue() == 1));
        }
        else if (id >= PROPERTY_IMPORT_ID)
            NpcTypeProperties.of(npc).importNow(npc, NpcTypeProperty.values()[id - PROPERTY_IMPORT_ID]);
        else
            settings.setCustom(NpcTypeProperty.values()[id - PROPERTY_MODE_ID], button.getValue() == 1);
        return true;
    }

    /** @return whether the field belongs to these pages and its value was stored */
    boolean unFocused(GuiTextFieldNop field)
    {
        if (page == Page.PARTS)
            return parts.unFocused(field);
        if (page == Page.PROPERTIES && ModelTuningRows.handles(field.id))
            return tuning.field(field);
        if (page == Page.PROPERTIES && field.id >= ENGINE_VALUE_ID && field.id < ENGINE_VALUE_ID + EngineChannel.values().length)
        {
            NpcVehicleSounds sounds = NpcPresentation.of(npc).getVehicleSounds();
            if (sounds.isModelDefaults())
                return false;
            engine(sounds, EngineChannel.values()[field.id - ENGINE_VALUE_ID]).setSound(field.getValue());
            return true;
        }
        if (page == Page.PROPERTIES && field.id >= PROPERTY_VALUE_ID && field.id < PROPERTY_VALUE_ID + NpcTypeProperty.values().length)
            return propertyEdited(field, NpcTypeProperty.values()[field.id - PROPERTY_VALUE_ID]);
        if (page.equipment() && field.id >= EQUIPMENT_VALUE_ID && field.id < EQUIPMENT_VALUE_ID + EquipmentProperty.values().length)
            return equipmentEdited(field, EquipmentProperty.values()[field.id - EQUIPMENT_VALUE_ID]);
        int index = field.id - MINIMUM_HIT_ID;
        if (index < 0 || index >= NpcDamageCategory.values().length)
            return false;
        minimumHitEdited(field, NpcDamageCategory.values()[index]);
        return true;
    }

    private void minimumHitEdited(GuiTextFieldNop field, NpcDamageCategory category)
    {
        int minimum = field.isInteger() ? field.getInteger() : -1;
        if (category == NpcDamageCategory.MELEE && minimum < 0)
            settings().inheritMeleeThreshold();
        else
            settings().setMinimumHit(category, minimum);
    }

    /** Whether the model property's cell edits the NPC's own value: the model does not supply it right now. */
    private boolean ownEditable(NpcTypeProperty property)
    {
        return settings().isCustom(property) || !NpcTypeProperties.of(npc).supplied().contains(property);
    }

    /** Whether the equipment statistic's cell edits the NPC's own value: neither the slot nor the model supplies it. */
    private boolean ownEditable(EquipmentProperty property)
    {
        return EquipmentValues.kind(property) != Kind.NONE && !EquipmentValues.inUse(npc, property) && !EquipmentValues.modelLocked(npc, property);
    }

    private boolean propertyEdited(GuiTextFieldNop field, NpcTypeProperty property)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        Tag value = parse(field, kind(properties.own(npc, property)));
        if (!ownEditable(property) || value == null)
            return false;
        properties.setOwn(npc, property, clamp(property, value));
        return true;
    }

    private boolean equipmentEdited(GuiTextFieldNop field, EquipmentProperty property)
    {
        Tag value = parse(field, EquipmentValues.kind(property));
        if (!ownEditable(property) || value == null)
            return false;
        EquipmentValues.write(npc, property, value);
        return true;
    }

    /** Keeps an edited model property within what Custom NPCs and the inheritance accept for its field. */
    private static Tag clamp(NpcTypeProperty property, Tag value)
    {
        if (!(value instanceof NumericTag number) || value instanceof ByteTag)
            return value;
        double max = switch (property)
        {
            case HEALTH -> NpcPlatform.maxHealthLimit();
            case AGGRO_RANGE, RANGE -> NpcMobility.rangeLimit();
            case ACCURACY, SPEED, WALKING_SPEED -> 100D;
            case SHOT_COUNT -> 10D;
            case MELEE_REACH -> 64D;
            case MELEE_DELAY -> 1000D;
            case DELAY_MIN, DELAY_MAX, BURST_DELAY, BURST -> 9999D;
            default -> 100_000D;
        };
        double min = switch (property)
        {
            case HEALTH, AGGRO_RANGE, RANGE, SHOT_COUNT, DELAY_MIN, DELAY_MAX, BURST, MELEE_DELAY -> 1D;
            default -> 0D;
        };
        double clamped = Math.max(min, Math.min(max, number.getAsDouble()));
        return value instanceof FloatTag ? FloatTag.valueOf((float) clamped) : IntTag.valueOf((int) clamped);
    }

    /** The server has been sent the requests; saving again must not repeat them. */
    void saved()
    {
        settings().clearRequests();
    }
}
