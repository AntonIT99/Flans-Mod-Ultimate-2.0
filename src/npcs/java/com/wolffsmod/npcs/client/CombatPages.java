package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcCombatSettings.EquipmentImport;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.properties.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * The combat pages of the Wolff's Mod tab that are more than a list of switches: knockback and minimum hit damage on the
 * Protection page, the model properties page with its Inherit/Custom modes, and the one-shot equipment import page.
 *
 * <p>
 * Everything is edited on the client copy of the NPC's settings; the tab sends them through the Custom NPCs stats menu
 * when it is left. Imports are only requests here and are carried out by the server.
 * </p>
 */
final class CombatPages
{
    static final int KNOCKBACK_ID = 700;
    static final int MINIMUM_HIT_ID = 710;
    static final int PROPERTIES_PAGE_ID = 720;
    static final int IMPORTS_PAGE_ID = 721;
    static final int GROUP_ID = 730;
    static final int IMPORT_ALL_ID = 760;
    static final int EQUIPMENT_IMPORT_ID = 770;
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

    /** The page shown instead of a category's switches. */
    enum Page
    {
        NONE, PROPERTIES, IMPORTS
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
    }

    boolean active()
    {
        return page != Page.NONE;
    }

    String title()
    {
        return PREFIX + (page == Page.PROPERTIES ? "properties" : "imports");
    }

    String hint()
    {
        return PREFIX + "import_hint";
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

    /** The buttons on the Equipment page that open the two statistics pages. */
    void initEquipmentLinks(boolean loaded, int y)
    {
        link(PROPERTIES_PAGE_ID, "properties", loaded, y);
        link(IMPORTS_PAGE_ID, "imports", loaded, y + 24);
    }

    private void link(int id, String name, boolean loaded, int y)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, gui.guiLeft + 8, gui.guiTop + y, 160, 20, new String[]{PREFIX + name}, 0);
        button.setEnabled(loaded);
        gui.addButton(button);
        gui.addLabel(new GuiLabel(id, PREFIX + name + ".summary", gui.guiLeft + 176, gui.guiTop + y + 6));
    }

    /** Knockback handling and the minimum hit damage of each kind, below the Protection switches. */
    void initProtection(boolean loaded, int y)
    {
        NpcCombatSettings settings = settings();
        int x = gui.guiLeft + 8;
        String knockback = PREFIX + "knockback";
        gui.addLabel(new GuiLabel(KNOCKBACK_ID, knockback, x, gui.guiTop + y + 5, knockback + HELP_SUFFIX));
        GuiButtonNop mode = new GuiButtonNop(gui, KNOCKBACK_ID, x + 148, gui.guiTop + y, 90, 20, names(knockback, Knockback.values()), settings.getKnockback().ordinal());
        mode.setEnabled(loaded);
        gui.addButton(mode);
        NpcDamageCategory[] kinds = {NpcDamageCategory.PROJECTILE, NpcDamageCategory.MELEE, NpcDamageCategory.EXPLOSION};
        for (int i = 0; i < kinds.length; i++)
        {
            int rowY = gui.guiTop + y + 21 * (i + 1);
            int id = MINIMUM_HIT_ID + kinds[i].ordinal();
            String key = PREFIX + "minimum_hit." + kinds[i].name().toLowerCase(Locale.ROOT);
            gui.addLabel(new GuiLabel(id, key, x, rowY + 5, PREFIX + "minimum_hit" + HELP_SUFFIX));
            GuiTextFieldNop field = new GuiTextFieldNop(id, gui, x + 148, rowY, 60, 20, Integer.toString(settings.minimumHit(kinds[i])));
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
        else if (page == Page.IMPORTS)
            initImports(loaded);
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
        GuiButtonNop all = new GuiButtonNop(gui, IMPORT_ALL_ID, gui.guiLeft + 264, gui.guiTop + 4, 86, 20, new String[]{PREFIX + "import_all"}, 0);
        all.setEnabled(loaded && group.properties().stream().anyMatch(inherited::containsKey));
        all.setTooltip(Tooltip.create(Component.translatable(PREFIX + "import_all" + HELP_SUFFIX)));
        gui.addButton(all);
        if (inherited.isEmpty())
            gui.addLabel(new GuiLabel(NOTICE_ID, PREFIX + (options.enabled(Feature.TYPE_PROPERTIES) ? "no_model" : "properties_disabled"), gui.guiLeft + 8, gui.guiTop + 186));
        List<NpcTypeProperty> rows = group.properties();
        for (int i = 0; i < rows.size(); i++)
            propertyRow(rows.get(i), gui.guiTop + 55 + i * 21, loaded, inherited.get(rows.get(i)), readouts.get(rows.get(i)));
    }

    private void propertyRow(NpcTypeProperty property, int y, boolean loaded, @Nullable Tag value, @Nullable String readout)
    {
        NpcCombatSettings settings = settings();
        int x = gui.guiLeft + 8;
        int id = property.ordinal();
        String name = property.name().toLowerCase(Locale.ROOT);
        String explanation = "wolffsmodnpcs.weapons.read_only.type_properties." + EXPLANATIONS.get(property);
        gui.addLabel(new GuiLabel(PROPERTY_MODE_ID + id, PREFIX + "property." + name, x, y + 5, explanation));
        boolean offered = value != null;
        boolean custom = settings.isCustom(property);
        String text = offered ? display(value, readout) : "-";
        GuiLabel shown = new GuiLabel(PROPERTY_VALUE_ID + id, shorten(text), x + 122, y + 5);
        // The model's value is live while inherited and only a reference once the NPC keeps its own.
        shown.setColor(custom ? READ_ONLY_COLOR : INHERITED_COLOR);
        if (offered)
            shown.setTooltip(Tooltip.create(Component.translatable(PREFIX + (custom ? "value.custom" : "value.inherited"), text)));
        gui.addLabel(shown);
        GuiButtonNop mode = new GuiButtonNop(gui, PROPERTY_MODE_ID + id, x + 248, y, 70, 20, new String[]{PREFIX + "mode.inherit", PREFIX + "mode.custom"}, custom ? 1 : 0);
        mode.setEnabled(loaded && offered);
        mode.setTooltip(Tooltip.create(Component.translatable(PREFIX + "mode" + HELP_SUFFIX)));
        gui.addButton(mode);
        boolean queued = settings.importRequested(property);
        GuiButtonNop copy = new GuiButtonNop(gui, PROPERTY_IMPORT_ID + id, x + 320, y, 76, 20, new String[]{importLabel(queued)}, 0);
        copy.setEnabled(loaded && offered && !queued);
        copy.setTooltip(Tooltip.create(Component.translatable(IMPORT + HELP_SUFFIX)));
        gui.addButton(copy);
    }

    private static String display(Tag value, @Nullable String readout)
    {
        if (value instanceof ByteTag flag)
            return I18n.get(flag.getAsByte() != 0 ? "gui.yes" : "gui.no");
        return readout == null ? value.getAsString() : readout;
    }

    static String shorten(String text)
    {
        return text.length() <= MAX_VALUE_LENGTH ? text : "..." + text.substring(text.length() - MAX_VALUE_LENGTH + 3);
    }

    /** An import is carried out when the tab is left; until then its button reads as queued. */
    private static String importLabel(boolean queued)
    {
        return queued ? IMPORT + ".queued" : IMPORT;
    }

    private void initImports(boolean loaded)
    {
        NpcCombatSettings settings = settings();
        EquipmentImport[] kinds = EquipmentImport.values();
        for (int i = 0; i < kinds.length; i++)
        {
            int y = gui.guiTop + 34 + i * 48;
            int id = EQUIPMENT_IMPORT_ID + i;
            String key = PREFIX + "equipment." + kinds[i].name().toLowerCase(Locale.ROOT);
            gui.addLabel(new GuiLabel(id, key, gui.guiLeft + 8, y + 5, key + HELP_SUFFIX));
            gui.addLabel(new GuiLabel(id + 10, key + ".summary", gui.guiLeft + 8, y + 24));
            boolean queued = settings.importRequested(kinds[i]);
            GuiButtonNop copy = new GuiButtonNop(gui, id, gui.guiLeft + 278, y, 110, 20, new String[]{importLabel(queued)}, 0);
            copy.setEnabled(loaded && !queued);
            copy.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
            gui.addButton(copy);
        }
    }

    // Events

    boolean handles(int id)
    {
        return id == KNOCKBACK_ID || id == PROPERTIES_PAGE_ID || id == IMPORTS_PAGE_ID || id == IMPORT_ALL_ID || id >= GROUP_ID && id < GROUP_ID + Group.values().length
            || id >= EQUIPMENT_IMPORT_ID && id < EQUIPMENT_IMPORT_ID + EquipmentImport.values().length || id >= PROPERTY_MODE_ID && id < PROPERTY_IMPORT_ID + NpcTypeProperty.values().length;
    }

    /** @return whether the click changed a setting that has to be saved */
    boolean buttonEvent(GuiButtonNop button)
    {
        NpcCombatSettings settings = settings();
        int id = button.id;
        if (id == PROPERTIES_PAGE_ID || id == IMPORTS_PAGE_ID)
        {
            page = id == PROPERTIES_PAGE_ID ? Page.PROPERTIES : Page.IMPORTS;
            return false;
        }
        if (id >= GROUP_ID && id < GROUP_ID + Group.values().length)
        {
            group = Group.values()[id - GROUP_ID];
            return false;
        }
        if (id == KNOCKBACK_ID)
            settings.setKnockback(Knockback.values()[Math.floorMod(button.getValue(), Knockback.values().length)]);
        else if (id == IMPORT_ALL_ID)
        {
            Map<NpcTypeProperty, Tag> inherited = NpcTypeProperties.of(npc).inherited();
            group.properties().stream().filter(inherited::containsKey).forEach(settings::requestImport);
        }
        else if (id >= EQUIPMENT_IMPORT_ID && id < EQUIPMENT_IMPORT_ID + EquipmentImport.values().length)
            settings.requestImport(EquipmentImport.values()[id - EQUIPMENT_IMPORT_ID]);
        else if (id >= PROPERTY_IMPORT_ID)
            settings.requestImport(NpcTypeProperty.values()[id - PROPERTY_IMPORT_ID]);
        else
            settings.setCustom(NpcTypeProperty.values()[id - PROPERTY_MODE_ID], button.getValue() == 1);
        return true;
    }

    /** @return whether the field belongs to these pages and its value was stored */
    boolean unFocused(GuiTextFieldNop field)
    {
        int index = field.id - MINIMUM_HIT_ID;
        if (index < 0 || index >= NpcDamageCategory.values().length)
            return false;
        settings().setMinimumHit(NpcDamageCategory.values()[index], field.isInteger() ? field.getInteger() : 0);
        return true;
    }

    /** The server has been sent the requests; saving again must not repeat them. */
    void saved()
    {
        settings().clearRequests();
    }
}
