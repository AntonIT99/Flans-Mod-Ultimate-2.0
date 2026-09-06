package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.TypeFile;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The ammunition a weapon explicitly refuses, read from the repeatable {@code RemoveAmmo} key.
 *
 * <p>{@code RemoveAmmo} is applied last, after {@code Ammo}, {@code AddAmmo} and every
 * {@code UseAmmoGroup}, so it always wins. It exists so a pack can correct an obviously wrong
 * inherited entry - a main battery that was handed its ship's light AA shell, say - or
 * deliberately narrow what a weapon accepts, without having to rewrite shared ammunition groups.
 *
 * <pre>
 * RemoveAmmo &lt;ammoShortName&gt; [&lt;ammoShortName&gt; ...]
 * </pre>
 *
 * <p>Names are the ammunition's own {@code ShortName} and match case-insensitively.
 */
public final class RemovedAmmo
{
    public static final String KEY = "RemoveAmmo";

    public static final RemovedAmmo EMPTY = new RemovedAmmo(Set.of());

    private final Set<String> keys;

    private RemovedAmmo(Set<String> keys)
    {
        this.keys = Collections.unmodifiableSet(keys);
    }

    public static RemovedAmmo read(@Nullable TypeFile file)
    {
        if (file == null)
            return EMPTY;
        Set<String> keys = new LinkedHashSet<>();
        for (String[] values : AmmoOverrides.lines(file, KEY))
            for (String value : values)
                keys.add(AmmoOverrides.key(value));
        return keys.isEmpty() ? EMPTY : new RemovedAmmo(keys);
    }

    public boolean isEmpty()
    {
        return keys.isEmpty();
    }

    /** The declared names, sanitized, for debug output and tests. */
    public Set<String> keys()
    {
        return keys;
    }

    /**
     * @param ammoOriginalShortName the ammunition's own {@code ShortName}
     * @return true when this weapon has been told not to accept that ammunition
     */
    public boolean removes(@Nullable String ammoOriginalShortName)
    {
        return ammoOriginalShortName != null && !keys.isEmpty()
            && keys.contains(AmmoOverrides.key(ammoOriginalShortName));
    }
}
