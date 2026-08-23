package com.flansmodultimate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.nio.file.Path;
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
                addPack(acceptor, packType, module.modId() + ":assets",
                    "Official Flan content assets", module.resourceRoot());
                continue;
            }

            for (PackagedContentProvider provider : module.providers())
            {
                Path logicalPackRoot = module.contentRoot().resolve(provider.getPackId());
                if (java.nio.file.Files.isDirectory(logicalPackRoot.resolve("data")))
                {
                    addPack(acceptor, packType, module.modId() + ":" + provider.getPackId(),
                        provider.getName(), logicalPackRoot);
                }
            }
        }
    }

    private static void addPack(Consumer<Pack> acceptor, PackType packType, String id,
                                String displayName, Path root)
    {
        Pack.ResourcesSupplier pathResources = new PathPackResources.PathResourcesSupplier(root);
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier()
        {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo location)
            {
                return new CompatiblePackResources(pathResources.openPrimary(location), packType);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo location, Pack.Metadata metadata)
            {
                return new CompatiblePackResources(pathResources.openFull(location, metadata), packType);
            }
        };
        PackLocationInfo location = new PackLocationInfo(id, Component.literal(displayName), PackSource.BUILT_IN, Optional.empty());
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, false);
        Pack pack = Pack.readMetaAndCreate(location, resources, packType, selection);
        if (pack != null)
            acceptor.accept(pack);
    }
}
