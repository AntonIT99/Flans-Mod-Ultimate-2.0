package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.mojang.logging.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for the content types that have no item of their own: player classes, teams and
 * loadout pools.
 *
 * <p>Types that do have an item are already kept apart by {@code ContentManager}, which gives a
 * colliding shortname an {@code _2} alias so both packs keep a working item. Item-less types used
 * to be stored in a plain map keyed by shortname, so the second pack to define, say, a class named
 * {@code assault} silently replaced the first one and that class became unreachable. This applies
 * the same rule to them: the first definition keeps the plain shortname, later ones are registered
 * under {@code shortname_2}, {@code shortname_3} and so on, and every definition stays reachable.</p>
 *
 * <p>A reference written inside a content pack, such as the {@code AddClass} lines of a team, is
 * resolved against that pack first, so a pack always sees its own definitions no matter which pack
 * happened to load first. References from outside a pack (commands, saved games, network packets)
 * use the unique name, which is what {@link InfoType#getShortName()} returns.</p>
 *
 * <p>Two definitions of one shortname inside a single pack are a mistake in that pack rather than a
 * collision between packs, so the later one is dropped with a warning, exactly as items are.</p>
 */
public final class ItemlessTypeRegistry<T extends InfoType>
{
    /** Its own logger rather than the mod's, so the registry stays usable without a loaded mod. */
    private static final Logger LOG = LogUtils.getLogger();

    private final String label;
    /** Unique name to type, in load order, so listings stay in the order the packs were read. */
    private final Map<String, T> byUniqueName = new LinkedHashMap<>();
    /** Content pack to (shortname as that pack writes it, unique name). */
    private final Map<IContentProvider, Map<String, String>> aliasesByPack = new HashMap<>();

    public ItemlessTypeRegistry(String label)
    {
        this.label = label;
    }

    /**
     * Registers a freshly loaded definition and returns the unique name it is reachable under, or
     * an empty string when the definition was dropped as a duplicate within its own content pack.
     */
    public String register(T type)
    {
        String original = normalize(type.getOriginalShortName());
        if (original.isEmpty())
            return StringUtils.EMPTY;

        Map<String, String> packAliases = aliasesByPack.computeIfAbsent(type.getContentPack(),
            ignored -> new LinkedHashMap<>());
        String existing = packAliases.get(original);
        if (existing != null)
        {
            LOG.warn("Detected conflict for {} id '{}' in same content pack: {} and {}. Ignoring {}",
                label, original, type, byUniqueName.get(existing), type.getFileName());
            return StringUtils.EMPTY;
        }

        String unique = original;
        for (int suffix = 2; byUniqueName.containsKey(unique); suffix++)
            unique = original + "_" + suffix;
        if (!unique.equals(original))
        {
            LOG.warn("Detected conflict for {} id '{}': {} and {}. Creating id alias '{}' in [{}]",
                label, original, type, byUniqueName.get(original), unique, packName(type));
        }

        byUniqueName.put(unique, type);
        packAliases.put(original, unique);
        return unique;
    }

    public Collection<T> values()
    {
        return Collections.unmodifiableCollection(byUniqueName.values());
    }

    /**
     * Resolves a unique name. A plain shortname still resolves to the first pack that defined it,
     * which is the definition that kept the plain name.
     */
    @Nullable
    public T get(@Nullable String id)
    {
        return StringUtils.isBlank(id) ? null : byUniqueName.get(normalize(id));
    }

    /** Resolves a reference written inside {@code provider}, preferring that pack's own definition. */
    @Nullable
    public T get(@Nullable String id, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(id))
            return null;

        Map<String, String> packAliases = provider == null ? null : aliasesByPack.get(provider);
        String unique = packAliases == null ? null : packAliases.get(normalize(id));
        return unique != null ? byUniqueName.get(unique) : get(id);
    }

    private static String normalize(@Nullable String value)
    {
        return value == null ? StringUtils.EMPTY : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String packName(InfoType type)
    {
        return type.getContentPack() == null ? "unknown" : type.getContentPack().getName();
    }
}
