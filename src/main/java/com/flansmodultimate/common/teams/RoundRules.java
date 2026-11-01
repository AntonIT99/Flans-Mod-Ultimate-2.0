package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

/** A sparse set of overrides. Missing values inherit a server default, then the legacy-compatible built-in default. */
public final class RoundRules
{
    private final Map<RoundRule, String> values = new EnumMap<>(RoundRule.class);

    public String get(RoundRule rule)
    {
        return values.getOrDefault(rule, rule.getDefaultValue());
    }

    public int number(RoundRule rule)
    {
        return Integer.parseInt(get(rule));
    }

    public boolean enabled(RoundRule rule)
    {
        return Boolean.parseBoolean(get(rule));
    }

    public boolean overrides(RoundRule rule)
    {
        return values.containsKey(rule);
    }

    public void set(RoundRule rule, String value)
    {
        values.put(rule, rule.validate(value));
    }

    public void inherit(RoundRule rule)
    {
        values.remove(rule);
    }

    /** A copy resolved at round start; later configuration changes cannot alter a match in progress. */
    public RoundRules resolve(RoundRules defaults)
    {
        RoundRules result = new RoundRules();
        result.values.putAll(defaults.values);
        result.values.putAll(values);
        return result;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        values.forEach((rule, value) -> tag.putString(rule.getKey(), value));
        return tag;
    }

    public static RoundRules load(CompoundTag tag)
    {
        RoundRules rules = new RoundRules();
        for (RoundRule rule : RoundRule.values())
        {
            if (!tag.contains(rule.getKey(), Tag.TAG_STRING))
                continue;
            try
            {
                rules.set(rule, tag.getString(rule.getKey()));
            }
            catch (IllegalArgumentException ignored)
            {
                // A malformed saved value falls back to the inherited rule.
            }
        }
        return rules;
    }
}
