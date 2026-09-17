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
                addEncryptedResourcePack(acceptor, module);
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
        int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        Pack.Info info = new Pack.Info(Component.literal("Optional uncensored Flan content"),
            packFormat, packFormat, FeatureFlagSet.of(), false);
        Pack.ResourcesSupplier resources = packId -> new EncryptedResourcePack(packId, module.modId(), bundlePath);
        Pack pack = Pack.create(id, Component.literal("Optional uncensored Flan content"), true, resources,
            info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.BUILT_IN);
        acceptor.accept(pack);
    }

    static String encryptedPackId(String modId)
    {
        return modId + ":_encrypted_assets";
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
