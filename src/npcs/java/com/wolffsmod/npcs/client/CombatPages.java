package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.EquipmentValues.Kind;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.*;

/**
 * The combat pages of the Wolff's Mod tab that are more than a list of switches: knockback and minimum hit damage on the
 * Protection page, the model properties page with its Inherit/Custom modes that the Model stats page opens, the
 * weapon and armor properties pages with the same modes that the Equipment page opens, and the part hitbox page that the
 * Protection page opens.
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
    static final int PROPERTIES_PAGE_ID = 720;
    static final int MAIN_HAND_PAGE_ID = 721;
    static final int ARMOR_PAGE_ID = 723;
    static final int OFF_HAND_PAGE_ID = 724;
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
    private static final int NOTICE_ID = 990;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final int INHERITED_COLOR = 0x408040;
    private static final int MAX_VALUE_LENGTH = 20;
    private static final String PREFIX = "wolffsmodnpcs.combat.";
    private static final String HELP_SUFFIX = ".help";
    private static final String IMPORT = PREFIX + "import";
    private static final String IMPORT_ALL = PREFIX + "import_all";
    private static final String INHERIT_ALL = PREFIX + "inherit_all";
    /** Left edge of Inherit all; Import all follows it and Back follows that. */
    private static final int BULK_LEFT = 226;
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    /** The read-only tooltips of the standard editor already explain how each property is derived. */
    private static final Map<NpcTypeProperty, String> EXPLANATIONS = Map.ofEntries(Map.entry(NpcTypeProperty.HEALTH, "health"), Map.entry(NpcTypeProperty.AGGRO_RANGE, "aggro_range"),
        Map.entry(NpcTypeProperty.CAN_DROWN, "can_drown"), Map.entry(NpcTypeProperty.NO_FALL_DAMAGE, "no_fall_damage"), Map.entry(NpcTypeProperty.DAMAGE, "damage"),
        Map.entry(NpcTypeProperty.ACCURACY, "accuracy"), Map.entry(NpcTypeProperty.SPEED, "speed"), Map.entry(NpcTypeProperty.RANGE, "range"), Map.entry(NpcTypeProperty.DELAY_MIN, "min_delay"),
        Map.entry(NpcTypeProperty.DELAY_MAX, "max_delay"), Map.entry(NpcTypeProperty.BURST_DELAY, "burst_delay"), Map.entry(NpcTypeProperty.BURST, "burst_count"),
        Map.entry(NpcTypeProperty.SHOT_COUNT, "shot_count"), Map.entry(NpcTypeProperty.FIRING_SOUND, "firing_sound"), Map.entry(NpcTypeProperty.MELEE_REACH, "melee_reach"),
        Map.entry(NpcTypeProperty.WALKING_SPEED, "walking_speed"), Map.entry(NpcTypeProperty.IDLE_SOUND, "idle_sound"), Map.entry(NpcTypeProperty.STEP_SOUND, "step_sound"));

    private final GuiBasic gui;
    private final EntityNPCInterface npc;
    private Page page = Page.NONE;
    private Group group = Group.GENERAL;
    private EquipmentProperty.Group equipmentGroup = EquipmentProperty.Group.MELEE;
    private final PartHitboxPage parts;

    /** The page shown instead of a category's switches. */
    enum Page
    {
        NONE, PROPERTIES, MAIN_HAND, OFF_HAND, ARMOR, PARTS;

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
                default -> null;
            };
        }
    }

    /** Tabs of the model properties page; every property belongs to exactly one. */
    enum Group
    {
        GENERAL(NpcTypeProperty.HEALTH, NpcTypeProperty.AGGRO_RANGE, NpcTypeProperty.MELEE_REACH, NpcTypeProperty.WALKING_SPEED, NpcTypeProperty.CAN_DROWN, NpcTypeProperty.NO_FALL_DAMAGE), WEAPON(
            NpcTypeProperty.RANGE, NpcTypeProperty.DAMAGE, NpcTypeProperty.ACCURACY, NpcTypeProperty.SPEED, NpcTypeProperty.SHOT_COUNT), TIMING(NpcTypeProperty.DELAY_MIN, NpcTypeProperty.DELAY_MAX,
                NpcTypeProperty.BURST_DELAY, NpcTypeProperty.BURST), SOUNDS(NpcTypeProperty.FIRING_SOUND, NpcTypeProperty.IDLE_SOUND, NpcTypeProperty.STEP_SOUND);

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

    private NpcCombatSettings settings()
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat();
    }

    // Category extras

    /** The button on the Model stats page that opens the model properties page. */
    void initPropertiesLink(boolean loaded, int y)
    {
        link(PROPERTIES_PAGE_ID, "properties", loaded, y);
    }

    /** The buttons on the Equipment page that open the weapon and the armor properties pages. */
    void initEquipmentLink(boolean loaded, int y)
    {
        link(MAIN_HAND_PAGE_ID, "main_hand_properties", loaded, y);
        link(OFF_HAND_PAGE_ID, "off_hand_properties", loaded, y + 21);
        link(ARMOR_PAGE_ID, "armor_properties", loaded, y + 42);
    }

    /** The button on the Protection page that opens the part hitbox page. */
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

    /** Knockback handling and the minimum hit damage of each kind, below the Protection switches and in their column. */
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
            GuiTextFieldNop field = new GuiTextFieldNop(id, gui, control, rowY, 60, 20, Integer.toString(settings.minimumHit(kinds[i])));
            field.setEditable(loaded);
            field.setTextColorUneditable(READ_ONLY_COLOR);
            field.numbersOnly = true;
            field.setMinMaxDefault(0, NpcCombatSettings.MAX_MINIMUM_HIT, 0);
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
        Map<NpcTypeProperty, String> readouts = properties.type().map(type -> NpcTypeReadouts.create(type, inherited, options, properties.usesNativeProjectile())).orElse(Map.of());
        Group[] groups = Group.values();
        for (int i = 0; i < groups.length; i++)
        {
            GuiButtonNop tab = new GuiButtonNop(gui, GROUP_ID + i, gui.guiLeft + 8 + i * 84, gui.guiTop + 29, 82, 20, new String[]{PREFIX + "group." + groups[i].name().toLowerCase(Locale.ROOT)}, 0);
            tab.setEnabled(loaded && groups[i] != group);
            gui.addButton(tab);
        }
        NpcCombatSettings settings = settings();
        bulkButtons(INHERIT_ALL_ID, loaded && group.properties().stream().anyMatch(property -> properties.supplied().contains(property) && settings.isCustom(property)), IMPORT_ALL_ID,
            loaded && group.properties().stream().anyMatch(inherited::containsKey), INHERIT_ALL + HELP_SUFFIX, IMPORT_ALL + HELP_SUFFIX);
        if (inherited.isEmpty())
            gui.addLabel(new GuiLabel(NOTICE_ID, PREFIX + (options.enabled(Feature.TYPE_PROPERTIES) ? "no_model" : "properties_disabled"), gui.guiLeft + 8, gui.guiTop + 186));
        List<NpcTypeProperty> rows = group.properties();
        for (int i = 0; i < rows.size(); i++)
            propertyRow(rows.get(i), gui.guiTop + 55 + i * 21, loaded, inherited.get(rows.get(i)), readouts.get(rows.get(i)));
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
        String explanation = "wolffsmodnpcs.weapons.read_only.type_properties." + EXPLANATIONS.get(property);
        gui.addLabel(new GuiLabel(PROPERTY_MODE_ID + id, PREFIX + "property." + name, x, y + 5, explanation));
        // Mounts that differ in a value still decide it, though there is no single value to import.
        boolean supplied = NpcTypeProperties.of(npc).supplied().contains(property);
        boolean custom = settings.isCustom(property);
        Tag own = NpcTypeProperties.of(npc).own(npc, property);
        Kind kind = kind(own);
        if (supplied && !custom)
            valueControl(PROPERTY_VALUE_ID + id, y, kind, modelValue(value, readout), Cell.INHERITED, loaded, Component.translatable(PREFIX + "value.inherited"));
        else
            valueControl(PROPERTY_VALUE_ID + id, y, kind, own, Cell.OWN, loaded, Component.translatable(PREFIX + "value.own"));
        GuiButtonNop mode = new GuiButtonNop(gui, PROPERTY_MODE_ID + id, x + 248, y, 70, 20, new String[]{PREFIX + "mode.inherit", PREFIX + "mode.custom"}, custom ? 1 : 0);
        mode.setEnabled(loaded && supplied);
        mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "mode" + HELP_SUFFIX)));
        gui.addButton(mode);
        // Importing only means something for a single value the NPC keeps as its own.
        if (value != null && custom)
            importButton(PROPERTY_IMPORT_ID + id, y, loaded, IMPORT + HELP_SUFFIX);
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
        GuiTextFieldNop field = new GuiTextFieldNop(id, gui, x, y, 112, 20, text(value));
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
        gui.addLabel(new GuiLabel(EQUIPMENT_MODE_ID + id, key, x, y + 5, key + HELP_SUFFIX));
        boolean custom = settings.isCustom(property);
        equipmentValue(property, y, loaded, value);
        GuiButtonNop mode;
        if (property.isSwitchable())
        {
            mode = new GuiButtonNop(gui, EQUIPMENT_MODE_ID + id, x + 248, y, 70, 20, new String[]{PREFIX + "mode.inherit", PREFIX + "mode.custom"}, custom ? 1 : 0);
            mode.setEnabled(loaded);
            mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "equipment_mode" + HELP_SUFFIX)));
        }
        else
        {
            // The item always times its own shots and fires its own rounds; the NPC's values only serve without it.
            mode = new GuiButtonNop(gui, EQUIPMENT_MODE_ID + id, x + 248, y, 70, 20, new String[]{PREFIX + "mode.item"}, 0);
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
            valueControl(id, y, kind, EquipmentValues.read(npc, property), Cell.OWN, loaded, Component.translatable(PREFIX + "value.own"));
    }

    // Events

    boolean handles(int id)
    {
        return id == KNOCKBACK_ID || id == PROPERTIES_PAGE_ID || id == MAIN_HAND_PAGE_ID || id == OFF_HAND_PAGE_ID || id == ARMOR_PAGE_ID || id == PARTS_PAGE_ID || parts.handles(id)
            || id == IMPORT_ALL_ID || id == EQUIPMENT_IMPORT_ALL_ID || id == INHERIT_ALL_ID || id == EQUIPMENT_INHERIT_ALL_ID || id >= GROUP_ID && id < GROUP_ID + Group.values().length
            || id >= EQUIPMENT_GROUP_ID && id < EQUIPMENT_GROUP_ID + EquipmentProperty.Group.values().length || equipmentControl(id)
            || id >= PROPERTY_MODE_ID && id < PROPERTY_VALUE_ID + NpcTypeProperty.values().length;
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
            settings.setCustom(EquipmentProperty.values()[id - EQUIPMENT_MODE_ID], button.getValue() == 1);
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
        if (parts.handles(id))
            return parts.buttonEvent(button);
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
        if (page == Page.PROPERTIES && field.id >= PROPERTY_VALUE_ID && field.id < PROPERTY_VALUE_ID + NpcTypeProperty.values().length)
            return propertyEdited(field, NpcTypeProperty.values()[field.id - PROPERTY_VALUE_ID]);
        if (page.equipment() && field.id >= EQUIPMENT_VALUE_ID && field.id < EQUIPMENT_VALUE_ID + EquipmentProperty.values().length)
            return equipmentEdited(field, EquipmentProperty.values()[field.id - EQUIPMENT_VALUE_ID]);
        int index = field.id - MINIMUM_HIT_ID;
        if (index < 0 || index >= NpcDamageCategory.values().length)
            return false;
        settings().setMinimumHit(NpcDamageCategory.values()[index], field.isInteger() ? field.getInteger() : 0);
        return true;
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
            case HEALTH -> Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
            case AGGRO_RANGE, RANGE -> NpcMobility.rangeLimit();
            case ACCURACY, SPEED, WALKING_SPEED -> 100D;
            case SHOT_COUNT -> 10D;
            case MELEE_REACH -> 64D;
            case DELAY_MIN, DELAY_MAX, BURST_DELAY, BURST -> 9999D;
            default -> 100_000D;
        };
        double min = switch (property)
        {
            case HEALTH, AGGRO_RANGE, RANGE, SHOT_COUNT, DELAY_MIN, DELAY_MAX, BURST -> 1D;
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
