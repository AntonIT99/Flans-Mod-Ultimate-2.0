package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
        if (value == ModClientConfig.ENABLE_UNCENSORED_CONTENT)
            return;
        values.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(value);
    }

    boolean canRestore()
    {
        return changedCount() > 0;
    }

    int changedCount()
    {
        return changedValues().size();
    }

    List<ConfigValue<?>> changedValues()
    {
        List<ConfigValue<?>> changed = new ArrayList<>();
        for (Map.Entry<ConfigTarget, Set<ConfigValue<?>>> entry : values.entrySet())
        {
            ConfigTarget target = entry.getKey();
            if (target.ready() && target.editable())
                entry.getValue().stream().filter(value -> differsFromDefault(target, value)).forEach(changed::add);
        }
        return changed;
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
