package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.ProjectileSources;
import com.flansmodultimate.api.ProjectileSources.Source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Custom NPCs widget IDs whose stored defaults are inactive for the selected Flan attack. */
public record RangedControlLocks(Map<Integer, Reason> textFields, Map<Integer, Reason> buttons, Map<Integer, Reason> labels)
{
    public enum Page
    {
        RANGED, PROJECTILE
    }

    public enum Reason
    {
        WEAPON_STATS, AMMUNITION, FIRING_SOUND, TYPE_PROPERTIES, ITEM_WEAPON, ITEM_ARMOR
    }

    public RangedControlLocks
    {
        textFields = Map.copyOf(textFields);
        buttons = Map.copyOf(buttons);
        labels = Map.copyOf(labels);
    }

    public static RangedControlLocks create(Page page, boolean nativeProjectile, ProjectileSources sources, boolean flanSounds)
    {
        Map<Integer, Reason> fields = new HashMap<>();
        Map<Integer, Reason> buttons = new HashMap<>();
        Map<Integer, Reason> labels = new HashMap<>();
        if (!nativeProjectile)
            return new RangedControlLocks(fields, buttons, labels);
        if (page == Page.RANGED)
            ranged(fields, buttons, labels, sources, flanSounds);
        else
            projectile(fields, buttons, labels, sources);
        return new RangedControlLocks(fields, buttons, labels);
    }

    static ProjectileSources combine(List<ProjectileSources> sources)
    {
        return new ProjectileSources(combine(sources, ProjectileSources::damage), combine(sources, ProjectileSources::spread), combine(sources, ProjectileSources::speed),
            !sources.isEmpty() && sources.stream().allMatch(ProjectileSources::firingSound));
    }

    static RangedControlLocks merge(RangedControlLocks first, RangedControlLocks second)
    {
        Map<Integer, Reason> fields = new HashMap<>(first.textFields());
        Map<Integer, Reason> buttons = new HashMap<>(first.buttons());
        Map<Integer, Reason> labels = new HashMap<>(first.labels());
        fields.putAll(second.textFields());
        buttons.putAll(second.buttons());
        labels.putAll(second.labels());
        return new RangedControlLocks(fields, buttons, labels);
    }

    private static Source combine(List<ProjectileSources> sources, Function<ProjectileSources, Source> setting)
    {
        if (sources.isEmpty() || sources.stream().anyMatch(source -> setting.apply(source) == Source.CALLER))
            return Source.CALLER;
        return sources.stream().allMatch(source -> setting.apply(source) == Source.AMMUNITION) ? Source.AMMUNITION : Source.WEAPON;
    }

    private static void ranged(Map<Integer, Reason> fields, Map<Integer, Reason> buttons, Map<Integer, Reason> labels, ProjectileSources sources, boolean flanSounds)
    {
        lockSource(fields, 1, sources.spread());
        fields.put(10, Reason.AMMUNITION);
        fields.put(11, Reason.AMMUNITION);
        if (flanSounds && sources.firingSound())
            fields.put(7, Reason.FIRING_SOUND);
        labels.putAll(fields);
        for (int id : new int[]{7, 10, 11})
            if (fields.containsKey(id))
                buttons.put(id, fields.get(id));
    }

    private static void projectile(Map<Integer, Reason> fields, Map<Integer, Reason> buttons, Map<Integer, Reason> labels, ProjectileSources sources)
    {
        lockSource(fields, 1, sources.damage());
        lockSource(fields, 4, sources.speed());
        fields.put(2, Reason.AMMUNITION);
        fields.put(3, Reason.AMMUNITION);
        fields.put(5, Reason.AMMUNITION);
        labels.putAll(fields);
        for (int id : new int[]{0, 1, 3, 4, 5, 6, 7, 8, 9, 10})
            buttons.put(id, Reason.AMMUNITION);
        for (int id : new int[]{5, 6, 7, 8, 10, 11})
            labels.put(id, Reason.AMMUNITION);
    }

    private static void lockSource(Map<Integer, Reason> fields, int id, Source source)
    {
        if (source != Source.CALLER)
            fields.put(id, source == Source.WEAPON ? Reason.WEAPON_STATS : Reason.AMMUNITION);
    }
}
