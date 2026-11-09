package com.flansmodultimate.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The class names a content pack ships, matched exactly first and then ignoring case, as legacy packs were written on case-insensitive file systems. */
final class ClassNameIndex
{
    private final Set<String> exact;
    private final Map<String, String> ignoringCase = new HashMap<>();

    ClassNameIndex(Collection<String> classNames)
    {
        exact = Set.copyOf(classNames);
        classNames.stream().sorted().forEach(name -> ignoringCase.putIfAbsent(name.toLowerCase(Locale.ROOT), name));
    }

    /** @return the name as the pack spells it, or null when the pack does not ship the class */
    @Nullable
    String resolve(String className)
    {
        return exact.contains(className) ? className : ignoringCase.get(className.toLowerCase(Locale.ROOT));
    }
}
