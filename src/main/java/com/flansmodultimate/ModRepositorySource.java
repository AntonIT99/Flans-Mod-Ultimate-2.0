package com.flansmodultimate;

import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class ModRepositorySource extends FolderRepositorySource
{
    private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    protected final Path folder;
    protected final PackType packType;
    protected final PackSource packSource;

    public ModRepositorySource(Path pFolder)
    {
        this(pFolder, PackType.CLIENT_RESOURCES);
    }

    public ModRepositorySource(Path pFolder, PackType packType)
    {
        super(pFolder, packType, PackSource.BUILT_IN, new net.minecraft.world.level.validation.DirectoryValidator(path -> true));
        folder = pFolder;
        this.packType = packType;
        this.packSource = PackSource.BUILT_IN;
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> pOnLoad)
    {
        try
        {
            net.minecraft.FileUtil.createDirectoriesSafe(folder);

            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(folder))
            {
                for (Path path : directorystream)
                {
                    Pack.ResourcesSupplier supplier = createPackResourcesSupplier(path);
                    if (supplier == null)
                        continue;

                    Pack.ResourcesSupplier filteredSupplier = new Pack.ResourcesSupplier()
                    {
                        @Override
                        public PackResources openPrimary(PackLocationInfo location)
                        {
                            return new FilteringPackResources(supplier.openPrimary(location));
                        }

                        @Override
                        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata)
                        {
                            return new FilteringPackResources(supplier.openFull(location, metadata));
                        }
                    };

                    String fileName = path.getFileName().toString();
                    PackLocationInfo locationInfo = new PackLocationInfo(
                        "file/" + fileName,
                        Component.literal(fileName),
                        packSource,
                        Optional.empty()
                    );

                    Pack pack = Pack.readMetaAndCreate(locationInfo, filteredSupplier, packType, DISCOVERED_PACK_SELECTION_CONFIG);
                    if (pack != null)
                    {
                        pOnLoad.accept(pack);
                    }
                }
            }
        }
        catch (IOException ioexception)
        {
            FlansMod.log.warn("Failed to list packs in {}", folder, ioexception);
        }
    }

    private Pack.ResourcesSupplier createPackResourcesSupplier(Path path)
    {
        if (Files.isDirectory(path))
        {
            return new PathPackResources.PathResourcesSupplier(path);
        }

        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".zip") || fileName.endsWith(".jar"))
        {
            return new FilePackResources.FileResourcesSupplier(path);
        }

        return null;
    }
}
