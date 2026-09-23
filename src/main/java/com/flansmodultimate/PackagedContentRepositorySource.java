package com.flansmodultimate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Supplies enabled packaged assets and recipes as required, top-priority built-in packs. */
public final class PackagedContentRepositorySource
{
    private PackagedContentRepositorySource()
    {
    }

    public static RepositorySource create(PackType packType)
    {
        return acceptor -> loadPacks(packType, acceptor);
    }

    private static void loadPacks(PackType packType, Consumer<Pack> acceptor)
    {
        for (PackagedContentPackApi.RegisteredModule module : PackagedContentPackApi.getRegisteredModules())
        {
            if (packType == PackType.CLIENT_RESOURCES)
            {
                addPack(acceptor, module.modId() + ":assets",
                    "Official Flan content assets", module.resourceRoot());
                addEncryptedResourcePack(acceptor, module);
                continue;
            }

            for (PackagedContentProvider provider : module.providers())
            {
                Path logicalPackRoot = module.contentRoot().resolve(provider.getPackId());
                if (java.nio.file.Files.isDirectory(logicalPackRoot.resolve("data")))
                {
                    addPack(acceptor, module.modId() + ":" + provider.getPackId(),
                        provider.getName(), logicalPackRoot);
                }
            }
        }
    }

    private static void addEncryptedResourcePack(Consumer<Pack> acceptor,
                                                 PackagedContentPackApi.RegisteredModule module)
    {
        Path bundlePath = module.resourceRoot().resolve(EncryptedResourcePack.BUNDLE_RESOURCE_PATH);
        if (!java.nio.file.Files.isRegularFile(bundlePath))
            return;

        // PackRepository stores discovered packs in a sorted map and inserts required TOP packs
        // in reverse key order. The leading underscore makes this pack sort before ":assets",
        // which places the encrypted overlay after the normal assets in the effective stack.
        String id = encryptedPackId(module.modId());
        PackLocationInfo location = new PackLocationInfo(id, Component.literal("Optional uncensored Flan content"),
            PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier()
        {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info)
            {
                return new EncryptedResourcePack(info, module.modId(), bundlePath);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo info, Pack.Metadata metadata)
            {
                return new EncryptedResourcePack(info, module.modId(), bundlePath);
            }
        };
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);
        Pack pack = Pack.readMetaAndCreate(location, resources, PackType.CLIENT_RESOURCES, selection);
        if (pack != null)
            acceptor.accept(pack);
    }

    static String encryptedPackId(String modId)
    {
        return modId + ":_encrypted_assets";
    }

    private static void addPack(Consumer<Pack> acceptor, String id,
                                String displayName, Path root)
    {
        Pack.ResourcesSupplier resources = new PathPackResources.PathResourcesSupplier(root);
        PackLocationInfo location = new PackLocationInfo(id, Component.literal(displayName), PackSource.BUILT_IN, Optional.empty());
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, false);
        // Logical pack directories contain data but no pack.mcmeta. Supply the
        // metadata here, as the 1.20.1 Pack.Info constructor did, so NeoForge
        // does not silently discard their recipes during repository discovery.
        Pack.Metadata metadata = new Pack.Metadata(Component.literal(displayName),
            PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
        acceptor.accept(new Pack(location, resources, metadata, selection));
    }
}
