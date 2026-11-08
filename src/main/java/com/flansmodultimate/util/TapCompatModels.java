package com.flansmodultimate.util;

import com.flansmodultimate.content.IContentProvider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Models that Tyrants and Plebeians expects the mod to provide, because the mod it was made for compiled them in.
 * They live in a package of their own and are only offered to that pack, so no other content pack can reach or clash with them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TapCompatModels
{
    private static final String LEGACY_PACKAGE = "com.flansmod.client.model.";
    private static final String COMPAT_PACKAGE = "com.flansmodultimate.compat.tap.model.";
    private static final List<String> PACK_DESCRIPTIONS = List.of("tyrants and plebeians", "tyrants & plebeians");
    /** Models the pack ships under another package than its definitions name. */
    private static final Map<String, String> MOVED_CLASSES = Map.of(LEGACY_PACKAGE + "NewAge.ModelZF41Scope", LEGACY_PACKAGE + "W44.ModelZF41Scope");
    private static final Map<Path, Boolean> DETECTED_PACKS = new ConcurrentHashMap<>();

    /** @return the compatibility class standing in for a legacy model class, or null when the name is outside the legacy package */
    public static String compatClassName(String modelClassName)
    {
        return modelClassName.startsWith(LEGACY_PACKAGE) ? COMPAT_PACKAGE + modelClassName.substring(LEGACY_PACKAGE.length()) : null;
    }

    /** @return where the pack really ships a model class it names wrongly, or null */
    public static String movedClassName(String modelClassName)
    {
        return MOVED_CLASSES.get(modelClassName);
    }

    public static boolean isTapDescription(String description)
    {
        String normalized = description.toLowerCase(Locale.ROOT);
        return PACK_DESCRIPTIONS.stream().anyMatch(normalized::contains);
    }

    public static boolean isTapPack(IContentProvider contentPack)
    {
        if (contentPack.isPreprocessed())
            return false;
        Path root = contentPack.isArchive() ? contentPack.getExtractedPath() : contentPack.getPath();
        return DETECTED_PACKS.computeIfAbsent(root.toAbsolutePath().normalize(), TapCompatModels::readIsTapPack);
    }

    private static boolean readIsTapPack(Path root)
    {
        Path mcMeta = root.resolve("pack.mcmeta");
        if (!Files.isRegularFile(mcMeta))
            return false;
        try (Reader reader = Files.newBufferedReader(mcMeta, StandardCharsets.UTF_8))
        {
            JsonObject pack = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("pack");
            return pack != null && pack.has("description") && isTapDescription(pack.get("description").getAsString());
        }
        catch (IOException | RuntimeException exception)
        {
            FlansLog.log.debug("Could not read {}", mcMeta, exception);
            return false;
        }
    }
}
