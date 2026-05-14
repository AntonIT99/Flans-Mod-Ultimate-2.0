package com.flansmodultimate;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import static net.minecraft.server.packs.repository.Pack.readPackInfo;

public class ModRepositorySource extends FolderRepositorySource
{
    protected final Path folder;
    protected final PackType packType;

    public ModRepositorySource(Path pFolder)
    {
        this(pFolder, PackType.CLIENT_RESOURCES);
    }

    public ModRepositorySource(Path pFolder, PackType packType)
    {
        super(pFolder, packType, PackSource.BUILT_IN);
        folder = pFolder;
        this.packType = packType;
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> pOnLoad) {
        try
        {
            FileUtil.createDirectoriesSafe(folder);
            discoverPacks(folder, false, (path, resourcesSupplier) ->
            {
                String fileName = path.getFileName().toString();
                Pack.Info mcmetaFileInfo = readPackInfo("file/" + fileName, resourcesSupplier);

                int packFormat = SharedConstants.getCurrentVersion().getPackVersion(packType);
                Pack.Info info = new Pack.Info((mcmetaFileInfo != null) ? mcmetaFileInfo.description() : MutableComponent.create(new LiteralContents(FilenameUtils.getBaseName(fileName))),
                    packFormat, packFormat, (mcmetaFileInfo != null) ? mcmetaFileInfo.requestedFeatures() : FeatureFlagSet.of(), false);

                Pack.ResourcesSupplier filteredSupplier = packId -> new FilteringPackResources(resourcesSupplier.open(packId));

                Pack pack = Pack.create("file/" + fileName, Component.literal(fileName), true, filteredSupplier, info, packType, Pack.Position.TOP, false, PackSource.BUILT_IN);
                pOnLoad.accept(pack);
            });
        }
        catch (IOException ioexception)
        {
            FlansMod.log.warn("Failed to list packs in {}", folder, ioexception);
        }
    }
}
