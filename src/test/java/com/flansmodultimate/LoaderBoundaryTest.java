package com.flansmodultimate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps Forge and NeoForge references in the platform seams. Main-mod and NPC sources may name {@code net.minecraftforge} or
 * {@code net.neoforged} only under {@code platform/} or {@code mixin/}, in the config spec types Java cannot alias, or in the
 * files listed below. The list is shared by every branch and may only shrink: move new loader code behind a platform class
 * instead of adding an entry, and remove an entry once its file no longer needs the loader.
 */
class LoaderBoundaryTest
{
    private static final List<Path> ROOTS = List.of(Path.of("src/main/java"), Path.of("src/npcs/java"));
    private static final Pattern LOADER_REFERENCE = Pattern.compile("\\bnet\\.(minecraftforge|neoforged)\\.[\\w.]*");
    // ForgeConfigSpec and NeoForge's ModConfigSpec share one builder API; Java has no type alias to hide the package.
    private static final Pattern CONFIG_SPEC = Pattern.compile("net\\.minecraftforge\\.common\\.ForgeConfigSpec\\b[\\w.]*|net\\.neoforged\\.neoforge\\.common\\.ModConfigSpec\\b[\\w.]*");

    /** Files outside the platform seams that still reference a loader, with the reason. */
    private static final Map<String, String> ALLOWED = Map.ofEntries(
        // Mod entry points and deferred registries.
        entry("src/main/java/com/flansmodultimate/FlansMod.java", "@Mod constructor"),
        entry("src/main/java/com/flansmodultimate/FlansModRegistries.java", "deferred registries"),
        entry("src/main/java/com/flansmodultimate/apocalyse/ApocalypseContent.java", "deferred registries"),
        entry("src/main/java/com/flansmodultimate/common/EnchantmentModule.java", "Forge code-registered enchantments; data on NeoForge"),
        entry("src/npcs/java/com/wolffsmod/npcs/NpcsMod.java", "@Mod constructor"),
        // Event subscribers: annotations and event types.
        entry("src/main/java/com/flansmodultimate/event/handler/ClientEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/event/handler/CommonEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/event/handler/ModClientEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/event/handler/RoundRulesEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/apocalyse/event/handler/ClientEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/apocalyse/event/handler/CommonEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/apocalyse/event/handler/ModClientEventHandler.java", "event subscriber"),
        entry("src/main/java/com/flansmodultimate/apocalyse/event/handler/ModCommonEventHandler.java", "event subscriber"),
        entry("src/npcs/java/com/wolffsmod/npcs/client/NpcsClientEvents.java", "event subscriber"),
        // Loader services not yet behind a platform class.
        entry("src/main/java/com/flansmodultimate/PackagedContentPackApi.java", "mod list and file locator"),
        entry("src/main/java/com/flansmodultimate/apocalyse/ApocalypseDatapackSource.java", "pack resources"),
        entry("src/main/java/com/flansmodultimate/client/model/BewlrRoutingModel.java", "baked model data"),
        entry("src/main/java/com/flansmodultimate/client/render/gpu/GpuModelCache.java", "shader registration event"),
        entry("src/main/java/com/flansmodultimate/client/render/thermal/VehicleThermalRenderer.java", "render events"),
        entry("src/main/java/com/flansmodultimate/common/FlanEntityPermissions.java", "permission API"),
        entry("src/main/java/com/flansmodultimate/common/recipe/GunpowderRecipeCondition.java", "recipe condition"),
        entry("src/main/java/com/flansmodultimate/common/teams/SpawnerVehicles.java", "server events"),
        entry("src/main/java/com/flansmodultimate/common/teams/editor/EditorPermissions.java", "permission API"),
        entry("src/main/java/com/flansmodultimate/content/ModernAssetAliases.java", "mod list"),
        entry("src/main/java/com/flansmodultimate/content/PackagedContentLoader.java", "mod list and file locator"),
        entry("src/main/java/com/flansmodultimate/content/SoundPriority.java", "pack resources"),
        entry("src/npcs/java/com/wolffsmod/npcs/diag/BenchmarkReport.java", "mod list"),
        entry("src/npcs/java/com/wolffsmod/npcs/model/FlanModelEntities.java", "registry lookups"));

    /** Allowed files that reference a loader on only one branch, so their entry is not stale on the other. */
    private static final Set<String> SINGLE_BRANCH = Set.of("src/main/java/com/flansmodultimate/common/EnchantmentModule.java");

    @Test
    void loaderReferencesStayInPlatformSeams() throws IOException
    {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<Path, Set<String>> file : loaderReferences().entrySet())
        {
            String path = file.getKey().toString().replace('\\', '/');
            if (!ALLOWED.containsKey(path))
                violations.add(path + " -> " + file.getValue());
        }
        assertTrue(violations.isEmpty(), "Loader references outside platform/ and mixin/; move them behind a platform class:\n" + String.join("\n", violations));
    }

    @Test
    void allowlistHasNoStaleEntries() throws IOException
    {
        Set<String> referencing = new HashSet<>();
        for (Path file : loaderReferences().keySet())
            referencing.add(file.toString().replace('\\', '/'));
        List<String> stale = new ArrayList<>();
        for (String path : ALLOWED.keySet())
        {
            if (!Files.isRegularFile(Path.of(path)) || !referencing.contains(path) && !SINGLE_BRANCH.contains(path))
                stale.add(path);
        }
        assertTrue(stale.isEmpty(), "Remove allowlist entries that no longer reference a loader:\n" + String.join("\n", stale));
    }

    /** Loader packages each scanned file names in code, ignoring comments, config specs and the platform seams. */
    private static Map<Path, Set<String>> loaderReferences() throws IOException
    {
        Map<Path, Set<String>> references = new TreeMap<>();
        for (Path root : ROOTS)
        {
            try (Stream<Path> files = Files.walk(root))
            {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                {
                    String unix = file.toString().replace('\\', '/');
                    if (unix.contains("/platform/") || unix.contains("/mixin/"))
                        continue;
                    Set<String> found = new TreeSet<>();
                    for (String line : Files.readAllLines(file))
                    {
                        String code = line.strip();
                        if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*"))
                            continue;
                        LOADER_REFERENCE.matcher(CONFIG_SPEC.matcher(code).replaceAll("")).results().forEach(match -> found.add(match.group()));
                    }
                    if (!found.isEmpty())
                        references.put(file, found);
                }
            }
        }
        return references;
    }
}
