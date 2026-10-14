package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.advanced.GuiNPCSoundsMenu;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

/**
 * Explains what Wolff's Mod does for one read-only parameter. Custom NPCs reuses widget IDs across widget kinds, so a parameter is named by its screen, widget kind and ID.
 * Parameters without a dedicated translation keep the reason's generic text.
 */
public final class ReadOnlyTooltips
{
    private static final String PREFIX = "wolffsmodnpcs.weapons.read_only.";
    private static final Map<Integer, String> RANGED_FIELDS = Map.ofEntries(entry(1, "accuracy"), entry(2, "range"), entry(3, "min_delay"), entry(4, "max_delay"), entry(5, "burst_delay"),
        entry(6, "burst_count"), entry(7, "firing_sound"), entry(8, "shot_count"), entry(9, "melee_range"), entry(10, "block_hit_sound"), entry(11, "entity_hit_sound"));
    private static final Map<Integer, String> RANGED_BUTTONS = Map.of(7, "firing_sound", 9, "aim_animation", 10, "block_hit_sound", 11, "entity_hit_sound", 13, "indirect_fire");
    /** Labels share their widget's ID, except melee range: field 9 is labelled 16, and label 9 belongs to the aim button. */
    private static final Map<Integer, String> RANGED_LABELS = relabel(merge(RANGED_FIELDS, RANGED_BUTTONS), RANGED_FIELDS, Map.of(16, 9));
    private static final Map<Integer, String> PROJECTILE_FIELDS = Map.of(1, "damage", 2, "knockback", 3, "size", 4, "speed", 5, "effect_duration");
    private static final Map<Integer, String> PROJECTILE_BUTTONS = Map.of(0, "gravity", 1, "acceleration", 3, "explosion", 4, "effect", 5, "trail", 6, "glow", 7, "render_mode", 8, "spin", 9, "stick",
        10, "effect_amplifier");
    /** Label ID to the button it describes; the effect duration field has no label. */
    private static final Map<Integer, String> PROJECTILE_LABELS = relabel(PROJECTILE_FIELDS, PROJECTILE_BUTTONS, Map.of(5, 0, 6, 3, 7, 4, 8, 5, 10, 8, 11, 9));
    private static final Map<Integer, String> MELEE_FIELDS = Map.of(1, "melee_damage", 2, "melee_reach", 3, "melee_delay", 4, "melee_knockback", 6, "melee_effect_duration");
    private static final Map<Integer, String> MELEE_BUTTONS = Map.of(5, "melee_effect", 7, "melee_effect_amplifier");
    private static final Map<Integer, String> MELEE_LABELS = merge(MELEE_FIELDS, MELEE_BUTTONS);
    private static final Map<Integer, String> STATS_FIELDS = Map.of(0, "health", 1, "aggro_range");
    private static final Map<Integer, String> STATS_BUTTONS = Map.of(4, "fire_immune", 5, "can_drown", 7, "no_fall_damage");
    /** Label ID to the toggle it describes. */
    private static final Map<Integer, String> STATS_LABELS = relabel(STATS_FIELDS, STATS_BUTTONS, Map.of(10, 4, 11, 5, 13, 7));
    private static final Map<Integer, String> MOVEMENT = Map.of(14, "walking_speed");
    private static final Map<Integer, String> SOUNDS = Map.of(0, "idle_sound", 5, "step_sound");
    private static final Map<Integer, String> RESISTANCES = Map.of(0, "knockback_resistance", 1, "projectile_resistance", 2, "melee_resistance", 3, "explosion_resistance");

    public enum Widget
    {
        FIELD, BUTTON, LABEL, SLIDER
    }

    private ReadOnlyTooltips()
    {}

    /** The parameter's own explanation followed by how to restore the NPC value, or the reason's generic text. */
    public static Tooltip create(GuiBasic gui, Widget widget, int id, Reason reason)
    {
        String generic = PREFIX + reason.name().toLowerCase(Locale.ROOT);
        String parameter = parameter(gui, widget, id);
        String specific = parameter == null ? null : generic + "." + parameter;
        if (specific == null || !I18n.exists(specific))
            return Tooltip.create(Component.translatable(generic));
        return Tooltip.create(Component.translatable(specific).append("\n").append(Component.translatable(generic + ".restore")));
    }

    /** Every parameter name a screen can report, for checking translations. */
    static Set<String> names()
    {
        return Stream.of(RANGED_FIELDS, RANGED_BUTTONS, RANGED_LABELS, PROJECTILE_FIELDS, PROJECTILE_BUTTONS, PROJECTILE_LABELS, MELEE_FIELDS, MELEE_BUTTONS, MELEE_LABELS, STATS_FIELDS, STATS_BUTTONS,
            STATS_LABELS, MOVEMENT, SOUNDS, RESISTANCES).flatMap(names -> names.values().stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    static String parameter(GuiBasic gui, Widget widget, int id)
    {
        return parameters(gui, widget).get(id);
    }

    private static Map<Integer, String> parameters(GuiBasic gui, Widget widget)
    {
        if (gui instanceof SubGuiNpcRangeProperties)
            return pick(widget, RANGED_FIELDS, RANGED_BUTTONS, RANGED_LABELS);
        if (gui instanceof SubGuiNpcProjectiles)
            return pick(widget, PROJECTILE_FIELDS, PROJECTILE_BUTTONS, PROJECTILE_LABELS);
        if (gui instanceof SubGuiNpcMeleeProperties)
            return pick(widget, MELEE_FIELDS, MELEE_BUTTONS, MELEE_LABELS);
        if (gui instanceof GuiNpcStats)
            return pick(widget, STATS_FIELDS, STATS_BUTTONS, STATS_LABELS);
        if (gui instanceof SubGuiNpcMovement)
            return pick(widget, MOVEMENT, Map.of(), MOVEMENT);
        if (gui instanceof GuiNPCSoundsMenu)
            return pick(widget, SOUNDS, SOUNDS, SOUNDS);
        if (gui instanceof SubGuiNpcResistanceProperties)
            return widget == Widget.SLIDER || widget == Widget.LABEL ? RESISTANCES : Map.of();
        return Map.of();
    }

    /** Both tables, with the second winning shared IDs. */
    private static Map<Integer, String> merge(Map<Integer, String> first, Map<Integer, String> second)
    {
        Map<Integer, String> merged = new HashMap<>(first);
        merged.putAll(second);
        return Map.copyOf(merged);
    }

    /** {@code base} plus each moved label named after the {@code source} widget it describes. */
    private static Map<Integer, String> relabel(Map<Integer, String> base, Map<Integer, String> source, Map<Integer, Integer> moved)
    {
        Map<Integer, String> labels = new HashMap<>(base);
        moved.forEach((label, widget) -> labels.put(label, source.get(widget)));
        return Map.copyOf(labels);
    }

    private static Map<Integer, String> pick(Widget widget, Map<Integer, String> fields, Map<Integer, String> buttons, Map<Integer, String> labels)
    {
        return switch (widget)
        {
            case FIELD -> fields;
            case BUTTON -> buttons;
            case LABEL -> labels;
            case SLIDER -> Map.of();
        };
    }
}
