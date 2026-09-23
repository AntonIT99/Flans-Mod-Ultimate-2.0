package com.flansmodultimate.apocalyse;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import net.minecraft.ChatFormatting;
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

/**
 * Registers the toggleable datapack that supplies the Apocalypse {@code level_stem}.
 *
 * <p>Only the dimension itself lives here. Its {@code dimension_type}, biomes and
 * {@code noise_settings} ship in the always-loaded {@code data/flansmodapocalypse} folder
 * on purpose: Minecraft writes a world's whole {@code LEVEL_STEM} registry into level.dat, and
 * those entries are stored as plain registry keys. If the referenced definitions disappeared when
 * this pack was disabled, decoding level.dat would fail and the world would refuse to load.
 * Keeping them always present means disabling this pack only affects newly created worlds.
 *
 * <p>The pack is optional and never enabled automatically, because the dimension it adds stays in
 * a world's {@code level.dat} for good and the world then needs the mod to load. Players choose it
 * per world when creating or first opening one, see {@code ApocalypseWorldChoice}; a dedicated
 * server enables it with {@code initial-enabled-packs} for a new world, or with
 * {@code /datapack enable} and a restart for an existing one.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseDatapackSource
{
    public static final String PACK_ID = FlansMod.MOD_ID + ":apocalypse";
    /** Built in, but never switched on for a world by itself: each world opts in explicitly. */
    private static final PackSource OPT_IN = PackSource.create(name ->
        Component.translatable("pack.nameAndSource", name, Component.translatable("pack.source.builtin")).withStyle(ChatFormatting.GRAY), false);

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
