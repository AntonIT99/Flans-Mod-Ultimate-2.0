package com.flansmodultimate;

import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.Identifier;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class FilteringPackResources implements PackResources
{
    private final PackResources delegate;
    private final PackType packType;
    private final AtomicInteger filteredCount = new AtomicInteger();

    public FilteringPackResources(PackResources delegate, PackType packType)
    {
        this.delegate = delegate;
        this.packType = packType;
    }

    private static boolean isExcluded(PackType type, Identifier location)
    {
        if (type != PackType.CLIENT_RESOURCES)
            return false;

        return location.getNamespace().equals(FlansMod.FLANSMOD_ID)
            && (location.getPath().startsWith(ContentManager.FOLDER_TEXTURES_ARMOR + "/")
            || location.getPath().startsWith(ContentManager.FOLDER_TEXTURES_GUI + "/")
            || location.getPath().startsWith(ContentManager.FOLDER_TEXTURES_SKINS + "/")
            || location.getPath().startsWith(ContentManager.FOLDER_SOUND + "/")
            || location.getPath().startsWith(ContentManager.FOLDER_TEXTURES + "/" + ContentManager.FOLDER_TEXTURES_ITEMS + "/")
            || location.getPath().startsWith(ContentManager.FOLDER_TEXTURES + "/" + ContentManager.FOLDER_TEXTURES_BLOCKS + "/"));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... path)
    {
        return delegate.getRootResource(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull Identifier location)
    {
        if (isExcluded(type, location))
            return null;

        return delegate.getResource(type, location);
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output)
    {
        delegate.listResources(type, namespace, path, (location, supplier) -> {
            if (!isExcluded(type, location))
                output.accept(location, supplier);
        });
    }

    @Override
    @NotNull
    public Set<String> getNamespaces(@NotNull PackType type)
    {
        return delegate.getNamespaces(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(@NotNull MetadataSectionType<T> serializer) throws IOException
    {
        MetadataSectionType<PackMetadataSection> packMetadataType = PackMetadataSection.forPackType(packType);
        if (serializer == packMetadataType)
        {
            PackMetadataSection metadata = delegate.getMetadataSection(packMetadataType);
            if (metadata == null)
                return null;

            // Flan packs are transformed by ContentManager before they reach
            // Minecraft. Their original pack.mcmeta often targets a much older
            // game, so report the transformed pack as compatible without
            // modifying the user's archive on every version switch.
            var currentFormat = SharedConstants.getCurrentVersion().packVersion(packType);
            return (T) new PackMetadataSection(metadata.description(), new net.minecraft.util.InclusiveRange<>(currentFormat));
        }

        return delegate.getMetadataSection(serializer);
    }

    @Override
    @NotNull
    public String packId()
    {
        return delegate.packId();
    }

    @Override
    public PackLocationInfo location()
    {
        return delegate.location();
    }

    @Override
    public void close()
    {
        delegate.close();
    }
}
