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

    public ModRepositorySource(Path pFolder)
    {
        super(pFolder, PackType.CLIENT_RESOURCES, PackSource.BUILT_IN);
        folder = pFolder;
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

                int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
                Pack.Info info = new Pack.Info(
                        (mcmetaFileInfo != null) ? mcmetaFileInfo.description() : MutableComponent.create(new LiteralContents(FilenameUtils.getBaseName(fileName))),
                        packFormat,
                        packFormat,
                        (mcmetaFileInfo != null) ? mcmetaFileInfo.requestedFeatures() : FeatureFlagSet.of(),
                        false);

                Pack pack = Pack.create("file/" + fileName, Component.literal(fileName), true, resourcesSupplier, info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, false, PackSource.BUILT_IN);
                pOnLoad.accept(pack);
            });
        }
        catch (IOException ioexception)
        {
            FlansMod.log.warn("Failed to list packs in {}", folder, ioexception);
        }
    }
}
