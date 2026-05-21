package com.flansmodultimate;

import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class FilteringPackResources implements PackResources
{
    private final PackResources delegate;
    private final AtomicInteger filteredCount = new AtomicInteger();

    public FilteringPackResources(PackResources delegate)
    {
        this.delegate = delegate;
    }

    private static boolean isExcluded(PackType type, ResourceLocation location)
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
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location)
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
    public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) throws IOException
    {
        return delegate.getMetadataSection(serializer);
    }

    @Override
    @NotNull
    public String packId()
    {
        return delegate.packId();
    }

    @Override
    public void close()
    {
        delegate.close();
    }
}