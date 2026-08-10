package com.flansmodultimate.apocalyse;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseDatapackSource
{
    public static final String PACK_ID = FlansMod.MOD_ID + ":apocalypse";

    public static RepositorySource create()
    {
        return ApocalypseDatapackSource::loadPacks;
    }

    private static void loadPacks(Consumer<Pack> packAcceptor)
    {
        IModFileInfo modFileInfo = ModList.get().getModFileById(FlansMod.MOD_ID);
        if (modFileInfo == null)
        {
            FlansMod.log.warn("Unable to register Apocalypse datapack: mod file for {} was not found", FlansMod.MOD_ID);
            return;
        }

        Path packRoot = modFileInfo.getFile().findResource("datapacks", "apocalypse");
        Pack.ResourcesSupplier resources = new PathPackResources.PathResourcesSupplier(packRoot);
        PackLocationInfo location = new PackLocationInfo(PACK_ID, Component.literal("Flan's Mod Apocalypse"), PackSource.BUILT_IN, Optional.empty());
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, false);
        Pack pack = Pack.readMetaAndCreate(location, resources, PackType.SERVER_DATA, selection);

        if (pack == null)
        {
            FlansMod.log.warn("Unable to register Apocalypse datapack from {}", packRoot);
            return;
        }

        packAcceptor.accept(pack);
    }
}
