package com.flansmodultimate.client.gui.options;

import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
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
        return changedCount() > 0;
    }

    int changedCount()
    {
        int count = 0;
        for (Map.Entry<ConfigTarget, Set<ConfigValue<?>>> entry : values.entrySet())
        {
            ConfigTarget target = entry.getKey();
            if (target.ready() && target.editable())
                count += (int) entry.getValue().stream().filter(value -> differsFromDefault(target, value)).count();
        }
        return count;
    }

    private static boolean differsFromDefault(ConfigTarget target, ConfigValue<?> value)
    {
        return !Objects.equals(target.get(value), value.getDefault());
    }

    void restore()
    {
        for (Map.Entry<ConfigTarget, Set<ConfigValue<?>>> entry : values.entrySet())
        {
            ConfigTarget target = entry.getKey();
            // Permissions may have changed while the confirmation was open.
            if (!target.ready() || !target.editable())
                continue;
            boolean changed = false;
            for (ConfigValue<?> value : entry.getValue())
            {
                if (differsFromDefault(target, value))
                {
                    target.setWhileDragging(value, value.getDefault());
                    changed = true;
                }
            }
            if (changed)
                target.flush();
        }
    }
}
