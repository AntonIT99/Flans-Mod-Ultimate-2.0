package com.flansmodultimate.client.gui.options;

import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Tracks only the config values represented on one screen, including compound preset controls. */
final class ConfigDefaults
{
    private final Map<ConfigTarget, Set<ConfigValue<?>>> values = new LinkedHashMap<>();

    void clear()
    {
        values.clear();
    }

    void add(ConfigTarget target, ConfigValue<?> value)
    {
        values.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(value);
    }

    boolean canRestore()
    {
        return values.keySet().stream().anyMatch(target -> target.ready() && target.editable());
    }

    void restore()
    {
        for (Map.Entry<ConfigTarget, Set<ConfigValue<?>>> entry : values.entrySet())
        {
            ConfigTarget target = entry.getKey();
            // Permissions may have changed while the confirmation was open.
            if (!target.ready() || !target.editable())
                continue;
            for (ConfigValue<?> value : entry.getValue())
                target.setWhileDragging(value, value.getDefault());
            target.flush();
        }
    }
}
