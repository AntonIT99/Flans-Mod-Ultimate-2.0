package com.flansmodultimate;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Public API for normal Forge mods that package immutable Flan content packs.
 * Registration must happen during the packaging mod's constructor, before Flan's Mod Ultimate
 * scans the user {@code flan} folder.
 */
public final class PackagedContentPackApi
{
    private static final List<RegisteredModule> modules = new ArrayList<>();

    private PackagedContentPackApi()
    {
    }

    /**
     * Registers packaged content without enforcing any logical packs.
     *
     * @param context packaging mod loading context
     * @param modId packaging mod id
     * @param contentRoot root containing one directory per logical pack
     * @param modelsRoot root containing compiled legacy model classes
     */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId,
                                             String contentRoot, String modelsRoot)
    {
        register(context, modId, contentRoot, modelsRoot, Set.of());
    }

    /**
     * Discovers logical packs below {@code contentRoot}, creates an early Forge configuration for
     * their activation, and registers enabled definitions with the main content loader.
     *
     * @param context packaging mod loading context
     * @param modId packaging mod id
     * @param contentRoot root containing one directory per logical pack
     * @param modelsRoot root containing compiled legacy model classes
     * @param enforcedPackIds packs which cannot be disabled
     */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId,
                                             String contentRoot, String modelsRoot,
                                             Set<String> enforcedPackIds)
    {
        register(context, modId, contentRoot, modelsRoot, enforcedPackIds, Map.of());
    }

    /**
     * Registers packaged content with optional display-name overrides keyed by logical pack ID.
     * Pack IDs without an override retain the generated {@code "Name (Official)"} label.
     */
    public static synchronized void register(FMLJavaModLoadingContext context, String modId,
                                             String contentRoot, String modelsRoot,
                                             Set<String> enforcedPackIds,
                                             Map<String, String> displayNameOverrides)
    {
        if (modules.stream().anyMatch(module -> module.modId().equals(modId)))
            throw new IllegalStateException("Packaged Flan content module already registered: " + modId);

        IModFileInfo modFileInfo = ModList.get().getModFileById(modId);
        if (modFileInfo == null)
            throw new IllegalStateException("Could not find Forge mod file for " + modId);

        Path moduleContentRoot = modFileInfo.getFile().findResource(splitPath(contentRoot));
        Path moduleModelsRoot = modFileInfo.getFile().findResource(splitPath(modelsRoot));
        Path moduleResourceRoot = modFileInfo.getFile().findResource("pack.mcmeta").getParent();
        Path moduleAssetsRoot = modFileInfo.getFile().findResource("assets", FlansMod.FLANSMOD_ID);
        Path modulePath = modFileInfo.getFile().getFilePath();
        String moduleDisplayName = ModList.get().getModContainerById(modId)
            .map(container -> container.getModInfo().getDisplayName())
            .filter(displayName -> !displayName.isBlank())
            .orElse(modId);
        boolean archiveBacked = FMLEnvironment.production;

        if (archiveBacked && (!Files.isRegularFile(modulePath)
            || !modulePath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")))
        {
            throw new IllegalStateException("Production packaged Flan content must be loaded from a JAR: " + modulePath);
        }

        FlansMod.log.info("Registering packaged Flan content module {} in {} mode from {}.", modId,
            archiveBacked ? "production JAR" : "development directory", modulePath);

        List<String> discoveredPackIds = discoverPackIds(moduleContentRoot);
        Set<String> enforced = normalizePackIds(enforcedPackIds);
        Map<String, String> configuredDisplayNames = new LinkedHashMap<>(
            loadDisplayNames(moduleContentRoot.resolve("pack_names.json"))
        );
        configuredDisplayNames.putAll(normalizeDisplayNames(displayNameOverrides));
        Map<String, String> displayNames = Map.copyOf(configuredDisplayNames);
        if (!discoveredPackIds.containsAll(enforced))
        {
            Set<String> missing = new LinkedHashSet<>(enforced);
            missing.removeAll(discoveredPackIds);
            throw new IllegalStateException("Enforced packaged content packs are missing from " + contentRoot + ": " + missing);
        }
        if (!discoveredPackIds.containsAll(displayNames.keySet()))
        {
            Set<String> unknown = new LinkedHashSet<>(displayNames.keySet());
            unknown.removeAll(discoveredPackIds);
            throw new IllegalStateException("Display names were configured for unknown packaged content packs in "
                + contentRoot + ": " + unknown);
        }

        Set<String> enabled = loadEarlySelection(context, modId, discoveredPackIds, enforced);
        List<PackagedContentProvider> providers = new ArrayList<>();
        boolean indexSharedAssets = true;
        for (String packId : orderPackIds(discoveredPackIds, enforced))
        {
            if (!enabled.contains(packId))
            {
                FlansMod.log.info("Packaged content pack '{}' from {} is disabled.", packId, modId);
                continue;
            }

            Path packRoot = moduleContentRoot.resolve(packId);
            boolean definitionsSubdirectory = Files.isDirectory(packRoot.resolve("definitions"));
            Path definitionsRoot = definitionsSubdirectory ? packRoot.resolve("definitions") : packRoot;
            String archivePackRoot = joinArchivePath(contentRoot, packId);
            String archiveDefinitionsRoot = definitionsSubdirectory
                ? joinArchivePath(archivePackRoot, "definitions")
                : archivePackRoot;

            providers.add(new PackagedContentProvider(
                displayNames.getOrDefault(packId, displayName(packId)), moduleDisplayName, packId, modulePath,
                definitionsRoot, moduleAssetsRoot, moduleModelsRoot,
                archiveDefinitionsRoot, joinArchivePath("assets", FlansMod.FLANSMOD_ID), modelsRoot,
                archiveBacked, indexSharedAssets, "flansmodultimate_officialpacks".equals(modId)
            ));
            indexSharedAssets = false;
        }

        RegisteredModule module = new RegisteredModule(modId, moduleResourceRoot, moduleContentRoot, List.copyOf(providers));
        modules.add(module);
        ContentManager.addPackagedContentPacks(providers);
        FlansMod.log.info("Registered {} enabled packaged content pack(s) from {}: {}", providers.size(), modId,
            providers.stream().map(PackagedContentProvider::getPackId).toList());
    }

    static synchronized List<RegisteredModule> getRegisteredModules()
    {
        return List.copyOf(modules);
    }

    private static List<String> discoverPackIds(Path contentRoot)
    {
        if (!Files.isDirectory(contentRoot))
            throw new IllegalStateException("Packaged Flan content root does not exist: " + contentRoot);

        try (Stream<Path> stream = Files.list(contentRoot))
        {
            return stream.filter(Files::isDirectory)
                .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Could not discover packaged Flan content under " + contentRoot, e);
        }
    }

    private static Set<String> loadEarlySelection(FMLJavaModLoadingContext context, String modId,
                                                  List<String> packIds, Set<String> enforced)
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        Map<String, ForgeConfigSpec.BooleanValue> values = new LinkedHashMap<>();
        builder.comment("Pack selection is applied during item registration and therefore requires a game restart.")
            .push("contentPacks");
        for (String packId : packIds)
        {
            if (!enforced.contains(packId))
                values.put(packId, builder.comment("Load the packaged content pack '" + packId + "'.")
                    .define(packId, true));
        }
        builder.pop();
        ForgeConfigSpec spec = builder.build();
        String fileName = modId + "-content-packs.toml";
        context.registerConfig(ModConfig.Type.COMMON, spec, fileName);

        Set<String> enabled = new LinkedHashSet<>(enforced);
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(fileName);
        try
        {
            Files.createDirectories(configPath.getParent());
            try (CommentedFileConfig config = CommentedFileConfig.builder(configPath).sync().build())
            {
                if (Files.isRegularFile(configPath))
                    config.load();
                spec.correct(config);
                config.save();
                spec.setConfig(config);
                values.forEach((packId, value) -> {
                    if (value.get())
                        enabled.add(packId);
                });
                spec.setConfig(null);
            }
        }
        catch (Exception e)
        {
            spec.setConfig(null);
            FlansMod.log.error("Could not read early packaged-content selection from {}. Enabling all packs.", configPath, e);
            enabled.addAll(packIds);
        }

        return Set.copyOf(enabled);
    }

    private static List<String> orderPackIds(List<String> discovered, Set<String> enforced)
    {
        return discovered.stream()
            .sorted(Comparator.comparing((String id) -> !enforced.contains(id)).thenComparing(id -> id))
            .toList();
    }

    private static Set<String> normalizePackIds(Set<String> packIds)
    {
        Set<String> normalized = new LinkedHashSet<>();
        packIds.forEach(id -> normalized.add(id.toLowerCase(Locale.ROOT)));
        return Set.copyOf(normalized);
    }

    private static Map<String, String> normalizeDisplayNames(Map<String, String> displayNames)
    {
        Map<String, String> normalized = new LinkedHashMap<>();
        displayNames.forEach((packId, displayName) -> {
            String normalizedPackId = packId.toLowerCase(Locale.ROOT);
            if (normalizedPackId.isBlank())
                throw new IllegalArgumentException("Pack ID for a display-name override cannot be blank");
            if (displayName.isBlank())
                throw new IllegalArgumentException("Display name for packaged content pack '" + packId + "' cannot be blank");
            normalized.put(normalizedPackId, displayName);
        });
        return Map.copyOf(normalized);
    }

    static Map<String, String> loadDisplayNames(Path mappingFile)
    {
        if (!Files.isRegularFile(mappingFile))
            return Map.of();

        try (var reader = Files.newBufferedReader(mappingFile, StandardCharsets.UTF_8))
        {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject())
                throw new IllegalStateException("Pack display-name mapping must be a JSON object: " + mappingFile);

            Map<String, String> displayNames = new LinkedHashMap<>();
            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
            {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                    throw new IllegalStateException("Display name for packaged content pack '" + entry.getKey()
                        + "' must be a JSON string in " + mappingFile);
                displayNames.put(entry.getKey(), entry.getValue().getAsString());
            }
            return normalizeDisplayNames(displayNames);
        }
        catch (Exception e)
        {
            if (e instanceof IllegalStateException illegalStateException)
                throw illegalStateException;
            throw new IllegalStateException("Could not read packaged content display names from " + mappingFile, e);
        }
    }

    private static String displayName(String packId)
    {
        if (packId.isBlank())
            return packId;
        return Character.toUpperCase(packId.charAt(0)) + packId.substring(1) + " (Official)";
    }

    private static String[] splitPath(String path)
    {
        return Stream.of(path.split("[/\\\\]+"))
            .filter(part -> !part.isBlank())
            .toArray(String[]::new);
    }

    private static String joinArchivePath(String... parts)
    {
        return Stream.of(parts)
            .flatMap(part -> Stream.of(part.split("[/\\\\]+")))
            .filter(part -> !part.isBlank())
            .reduce((left, right) -> left + "/" + right)
            .orElse("");
    }

    record RegisteredModule(String modId, Path resourceRoot, Path contentRoot,
                            List<PackagedContentProvider> providers)
    {
    }
}
