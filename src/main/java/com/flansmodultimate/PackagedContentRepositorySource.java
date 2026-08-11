package com.flansmodultimate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.file.Path;
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
        int packFormat = SharedConstants.getCurrentVersion().getPackVersion(packType);
        Pack.Info info = new Pack.Info(Component.literal(displayName), packFormat, packFormat,
            FeatureFlagSet.of(), false);
        Pack.ResourcesSupplier resources = packId -> new PathPackResources(packId, root, true);
        Pack pack = Pack.create(id, Component.literal(displayName), true, resources, info,
            packType, Pack.Position.TOP, true, PackSource.BUILT_IN);
        acceptor.accept(pack);
    }
}
