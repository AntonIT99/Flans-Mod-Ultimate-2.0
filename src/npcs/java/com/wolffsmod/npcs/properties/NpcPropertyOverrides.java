package com.wolffsmod.npcs.properties;

import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Reversible runtime overlay; normal saves and menu packets always carry the user's stored defaults. */
public final class NpcPropertyOverrides
{
    private final Map<NpcTypeProperty, Tag> defaults = new EnumMap<>(NpcTypeProperty.class);
    private Map<NpcTypeProperty, Tag> values = Map.of();

    public boolean refresh(Map<NpcTypeProperty, Tag> next, Function<NpcTypeProperty, Tag> read, BiConsumer<NpcTypeProperty, Tag> write)
    {
        boolean changed = false;
        // Some setters affect a pair of fields (minimum/maximum delay); capture all defaults before any writes.
        next.keySet().forEach(property -> defaults.computeIfAbsent(property, read));
        for (NpcTypeProperty property : NpcTypeProperty.values())
        {
            Tag replacement = next.get(property);
            if (replacement == null)
            {
                Tag saved = defaults.remove(property);
                if (saved != null)
                {
                    write.accept(property, saved);
                    changed = true;
                }
                continue;
            }
            if (!replacement.equals(read.apply(property)))
            {
                write.accept(property, replacement);
                changed = true;
            }
        }
        values = Map.copyOf(next);
        return changed;
    }

    public Map<NpcTypeProperty, Tag> values()
    {
        return values;
    }

    public void loaded(Component component)
    {
        // The component has just read new user defaults, so stale captured values must not replace them.
        defaults.keySet().removeIf(property -> property.getComponent() == component);
    }

    public void saveDefaults(Component component, CompoundTag tag)
    {
        defaults.forEach((property, value) ->
        {
            if (property.getComponent() == component)
                tag.put(property.getNbtKey(), value.copy());
        });
    }

    public Tag stored(NpcTypeProperty property, Function<NpcTypeProperty, Tag> read)
    {
        Tag stored = defaults.get(property);
        return stored == null ? read.apply(property) : stored;
    }
}
