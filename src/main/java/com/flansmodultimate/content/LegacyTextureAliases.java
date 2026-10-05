package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
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
    @Getter private final Map<IContentProvider, Map<String, DynamicReference>> armorTextureReferences = new HashMap<>();
    @Getter private final Map<IContentProvider, Map<String, DynamicReference>> guiTextureReferences = new HashMap<>();
    @Getter private final Map<IContentProvider, Map<String, DynamicReference>> skinsTextureReferences = new HashMap<>();
    private final Map<String, Map<String, TextureGroup>> textures = new HashMap<>();
    private final Map<String, Set<String>> reserved = new HashMap<>();
    private final Map<IContentProvider, Map<String, Map<String, Map<String, String>>>> inputs = new HashMap<>();
    private record TextureGroup(IContentProvider provider, Map<String, String> layers) {}

    LegacyTextureAliases()
    {
        for (String folder : List.of(FOLDER_TEXTURES_ARMOR, FOLDER_TEXTURES_GUI, FOLDER_TEXTURES_SKINS))
        {
            textures.put(folder, new HashMap<>());
            reserved.put(folder, new HashSet<>());
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
        for (IContentProvider provider : providers)
            if (provider.shouldIndexAssetsForConflicts())
                read(provider);
    }

    private Map<String, Map<String, Map<String, String>>> read(IContentProvider provider)
    {
        return inputs.computeIfAbsent(provider, ignored -> {
            try
            {
                Map<String, Map<String, Map<String, String>>> input = PackAssetIndex.legacy(provider);
                input.forEach((folder, groups) -> reserved.get(folder).addAll(groups.keySet()));
                return input;
            }
            catch (IOException | RuntimeException e)
            {
                FlansMod.log.error("Could not index legacy textures in '{}': {}", provider.getName(), e.toString());
                return Map.of();
            }
        });
    }

    void findDuplicates(IContentProvider provider)
    {
        read(provider).forEach((folder, groups) -> {
            Map<String, DynamicReference> references = switch (folder) {
                case FOLDER_TEXTURES_ARMOR -> armorTextureReferences.get(provider);
                case FOLDER_TEXTURES_GUI -> guiTextureReferences.get(provider);
                default -> skinsTextureReferences.get(provider);
            };
            groups.forEach((original, layers) -> {
                Map<String, TextureGroup> registered = textures.get(folder);
                TextureGroup existing = registered.get(original);
                String alias = original;
                if (existing != null && !existing.layers().equals(layers))
                {
                    if (provider.isPreprocessed())
                        FlansMod.log.error("Conflicting texture '{}/{}' in read-only bundled content [{}] and [{}]. Rename one of the bundled textures.", folder, original, provider.getConflictDisplayName(), existing.provider().getConflictDisplayName());
                    else
                    {
                        String persisted = references.containsKey(original) ? references.get(original).get() : original;
                        if (!persisted.equals(original) && reserved.get(folder).add(persisted))
                            alias = persisted;
                        else
                            for (int suffix = 2; ; suffix++)
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
            FlansMod.log.warn("Could not cache processed legacy textures '{}': {}", provider.getName(), e.toString());
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
