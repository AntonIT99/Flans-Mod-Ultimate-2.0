package com.flansmodultimate.content;

import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.flansmodultimate.content.ContentPackPaths.*;

/** Assigns one alias to the complete armor layer group, using cached pixel signatures. */
final class LegacyTextureAliases
{
    @Getter
    private final Map<IContentProvider, Map<String, DynamicReference>> armorTextureReferences = new HashMap<>();
    @Getter
    private final Map<IContentProvider, Map<String, DynamicReference>> guiTextureReferences = new HashMap<>();
    @Getter
    private final Map<IContentProvider, Map<String, DynamicReference>> skinsTextureReferences = new HashMap<>();
    private final Map<String, Map<String, TextureGroup>> textures = new HashMap<>();
    private final Map<String, Set<String>> reserved = new HashMap<>();
    private final Map<String, Set<String>> signatureNames = new HashMap<>();
    private final Map<IContentProvider, Map<String, Map<String, Map<String, String>>>> inputs = new HashMap<>();
    /** Name indexes read by {@link #prepare}, reused when they already hold every signature a pack needs. */
    private final Map<IContentProvider, Map<String, Map<String, Map<String, String>>>> prepared = new HashMap<>();
    private record TextureGroup(IContentProvider provider, Map<String, String> layers)
    {}
    private record PreparedNames(Map<String, Map<String, Map<String, String>>> names, String error, long nanos)
    {}

    LegacyTextureAliases()
    {
        for (String folder : List.of(FOLDER_TEXTURES_ARMOR, FOLDER_TEXTURES_GUI, FOLDER_TEXTURES_SKINS))
        {
            textures.put(folder, new HashMap<>());
            reserved.put(folder, new HashSet<>());
            signatureNames.put(folder, new HashSet<>());
        }
    }

    void initialize(IContentProvider provider)
    {
        armorTextureReferences.putIfAbsent(provider, new HashMap<>());
        guiTextureReferences.putIfAbsent(provider, new HashMap<>());
        skinsTextureReferences.putIfAbsent(provider, new HashMap<>());
    }

    /** Reserve authored names from every pack before allocating suffixes. */
    void prepare(List<IContentProvider> providers)
    {
        prepare(providers, ContentLoadingWorkers.sequential());
    }

    /** Indexes the packs on the workers, then reserves their names in pack order. */
    void prepare(List<IContentProvider> providers, ContentLoadingWorkers workers)
    {
        List<IContentProvider> indexed = providers.stream().filter(IContentProvider::shouldIndexAssetsForConflicts).toList();
        List<PreparedNames> results = workers.map(indexed, LegacyTextureAliases::readNames);
        for (int i = 0; i < indexed.size(); i++)
        {
            IContentProvider provider = indexed.get(i);
            PreparedNames result = results.get(i);
            if (result.names() != null)
            {
                result.names().forEach((folder, groups) -> groups.keySet().forEach(name ->
                {
                    if (!reserved.get(folder).add(name))
                        signatureNames.get(folder).add(name);
                }));
                prepared.put(provider, result.names());
            }
            else
                FlansLog.log.error("Could not reserve legacy texture names in '{}': {}", provider.getName(), result.error());
            FlansLog.log.debug("{}: Reserved legacy texture names in {} ms", provider.getName(), result.nanos() / 1_000_000);
        }
    }

    private static PreparedNames readNames(IContentProvider provider)
    {
        long start = System.nanoTime();
        // Walks the whole pack once: its assets are cut from that walk here and again when it registers.
        ContentFileCache.prefetch(provider);
        try
        {
            return new PreparedNames(PackAssetIndex.legacyNames(provider), null, System.nanoTime() - start);
        }
        catch (IOException | RuntimeException e)
        {
            return new PreparedNames(null, e.toString(), System.nanoTime() - start);
        }
    }

    private Map<String, Map<String, Map<String, String>>> read(IContentProvider provider)
    {
        return inputs.computeIfAbsent(provider, ignored ->
        {
            try
            {
                // Nothing has written to the pack's textures since it was prepared, so its name index
                // stands as long as no collision asks for a signature it does not hold.
                Map<String, Map<String, Map<String, String>>> names = prepared.remove(provider);
                Map<String, Map<String, Map<String, String>>> input = names != null && !PackAssetIndex.needsSignatures(names, signatureNames)
                    ? names
                    : PackAssetIndex.legacy(provider, signatureNames);
                input.forEach((folder, groups) -> reserved.get(folder).addAll(groups.keySet()));
                return input;
            }
            catch (IOException | RuntimeException e)
            {
                FlansLog.log.error("Could not index legacy textures in '{}': {}", provider.getName(), e.toString());
                return Map.of();
            }
        });
    }

    void findDuplicates(IContentProvider provider)
    {
        read(provider).forEach((folder, groups) ->
        {
            Map<String, DynamicReference> references = switch (folder)
            {
                case FOLDER_TEXTURES_ARMOR -> armorTextureReferences.get(provider);
                case FOLDER_TEXTURES_GUI -> guiTextureReferences.get(provider);
                default -> skinsTextureReferences.get(provider);
            };
            groups.forEach((original, layers) ->
            {
                Map<String, TextureGroup> registered = textures.get(folder);
                TextureGroup existing = registered.get(original);
                String alias = original;
                if (existing != null && !existing.layers().equals(layers))
                {
                    if (provider.isPreprocessed())
                        FlansLog.log.error("Conflicting texture '{}/{}' in read-only bundled content [{}] and [{}]. Rename one of the bundled textures.", folder, original,
                            provider.getConflictDisplayName(), existing.provider().getConflictDisplayName());
                    else
                    {
                        String persisted = references.containsKey(original) ? references.get(original).get() : original;
                        if (!persisted.equals(original) && reserved.get(folder).add(persisted))
                            alias = persisted;
                        else
                            for (int suffix = 2;; suffix++)
                                if (reserved.get(folder).add(original + "_" + suffix))
                                {
                                    alias = original + "_" + suffix;
                                    break;
                                }
                    }
                }
                DynamicReference.storeOrUpdate(original, alias, references);
                // Sharing a texture must not change the owner before the next armor layer is checked.
                registered.putIfAbsent(alias, new TextureGroup(provider, layers));
            });
        });
    }

    void rememberAfterProcessing(IContentProvider provider)
    {
        if (!inputs.containsKey(provider))
            return;
        FileSystem fs = FileUtils.createFileSystem(provider);
        try
        {
            PackAssetIndex.rememberLegacy(provider, fs, inputs.get(provider));
        }
        catch (IOException e)
        {
            FlansLog.log.warn("Could not cache processed legacy textures '{}': {}", provider.getName(), e.toString());
        }
        finally
        {
            FileUtils.closeFileSystem(fs, provider);
        }
    }

    static String getArmorTextureBaseName(String name)
    {
        return name.endsWith("_1") || name.endsWith("_2") ? name.substring(0, name.length() - 2) : name;
    }
}
