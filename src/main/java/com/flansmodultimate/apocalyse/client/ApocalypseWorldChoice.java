package com.flansmodultimate.apocalyse.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.config.ModApocalypseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where a world stands on the Apocalypse dimension, and how a player's decision is recorded.
 *
 * <p>A world that has the dimension can no longer be opened once the mod is removed, because its
 * {@code level.dat} keeps the dimension, so it is never added without the player choosing it. The
 * decision lives in the world's own data pack lists: the Apocalypse pack listed as enabled means
 * the player chose it, listed as disabled means they declined it. Minecraft already records every
 * available pack a new world did not enable as disabled, so a world created without the Apocalypse
 * is never asked again.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseWorldChoice
{
    private static final String NBT_DATA = "Data";
    private static final String NBT_DATA_PACKS = "DataPacks";
    private static final String NBT_ENABLED = "Enabled";
    private static final String NBT_DISABLED = "Disabled";
    private static final String NBT_WORLD_GEN_SETTINGS = "WorldGenSettings";
    private static final String NBT_DIMENSIONS = "dimensions";

    /** Whether the Apocalypse can be chosen at all in this installation. */
    public static boolean isAvailable()
    {
        return ModApocalypseConfig.apocalypseDimensionDatapackEnabled();
    }

    /** Whether a world about to be created with this configuration can be offered the Apocalypse. */
    public static boolean isOffered(WorldDataConfiguration configuration)
    {
        return isAvailable() && (configuration.dataPacks().getEnabled().contains(ApocalypseDatapackSource.PACK_ID)
            || configuration.dataPacks().getDisabled().contains(ApocalypseDatapackSource.PACK_ID));
    }

    public static boolean isEnabled(WorldDataConfiguration configuration)
    {
        return configuration.dataPacks().getEnabled().contains(ApocalypseDatapackSource.PACK_ID);
    }

    /**
     * Whether the player still has to decide for this saved world: it has no Apocalypse dimension
     * and its data pack lists say nothing about the Apocalypse pack either way.
     */
    public static boolean needsChoice(Minecraft minecraft, String levelId)
    {
        if (!isAvailable())
            return false;
        Path levelData = minecraft.getLevelSource().getBaseDir().resolve(levelId).resolve(LevelResource.LEVEL_DATA_FILE.getId());
        if (!Files.isRegularFile(levelData))
            return false;
        try
        {
            CompoundTag data = NbtIo.readCompressed(levelData, NbtAccounter.unlimitedHeap()).getCompound(NBT_DATA);
            if (data.getCompound(NBT_WORLD_GEN_SETTINGS).getCompound(NBT_DIMENSIONS)
                .contains(ApocalypseContent.APOCALYPSE_LEVEL.location().toString()))
                return false;
            CompoundTag dataPacks = data.getCompound(NBT_DATA_PACKS);
            return !containsString(dataPacks.getList(NBT_ENABLED, Tag.TAG_STRING), ApocalypseDatapackSource.PACK_ID)
                && !containsString(dataPacks.getList(NBT_DISABLED, Tag.TAG_STRING), ApocalypseDatapackSource.PACK_ID);
        }
        catch (IOException | RuntimeException exception)
        {
            // Leave an unreadable world to Minecraft's own error handling rather than asking about it.
            FlansMod.log.warn("Could not read {} to check for the Apocalypse dimension", levelData, exception);
            return false;
        }
    }

    /**
     * Records the player's decision in the world's data pack lists, so it takes effect as the world
     * loads. Written the way Minecraft saves {@code level.dat}, keeping the previous one as
     * {@code level.dat_old}.
     */
    public static void record(Minecraft minecraft, String levelId, boolean withApocalypse) throws IOException
    {
        try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(levelId))
        {
            Path levelData = access.getLevelPath(LevelResource.LEVEL_DATA_FILE);
            CompoundTag root = NbtIo.readCompressed(levelData, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound(NBT_DATA);
            boolean hadDataPacks = data.contains(NBT_DATA_PACKS, Tag.TAG_COMPOUND);
            CompoundTag dataPacks = data.getCompound(NBT_DATA_PACKS);
            ListTag enabled = dataPacks.getList(NBT_ENABLED, Tag.TAG_STRING);
            ListTag disabled = dataPacks.getList(NBT_DISABLED, Tag.TAG_STRING);
            // A world saved before data packs existed implicitly runs vanilla alone.
            if (!hadDataPacks && enabled.isEmpty())
                enabled.add(StringTag.valueOf("vanilla"));
            removeString(enabled, ApocalypseDatapackSource.PACK_ID);
            removeString(disabled, ApocalypseDatapackSource.PACK_ID);
            (withApocalypse ? enabled : disabled).add(StringTag.valueOf(ApocalypseDatapackSource.PACK_ID));
            dataPacks.put(NBT_ENABLED, enabled);
            dataPacks.put(NBT_DISABLED, disabled);
            data.put(NBT_DATA_PACKS, dataPacks);
            root.put(NBT_DATA, data);

            Path levelDirectory = access.getLevelPath(LevelResource.ROOT);
            Path written = Files.createTempFile(levelDirectory, "level", ".dat");
            NbtIo.writeCompressed(root, written);
            Util.safeReplaceFile(levelData, written, access.getLevelPath(LevelResource.OLD_LEVEL_DATA_FILE));
        }
        FlansMod.log.info("World '{}' {} the Apocalypse dimension", levelId, withApocalypse ? "now includes" : "keeps out");
    }

    private static boolean containsString(ListTag list, String value)
    {
        for (int index = 0; index < list.size(); index++)
        {
            if (value.equals(list.getString(index)))
                return true;
        }
        return false;
    }

    private static void removeString(ListTag list, String value)
    {
        list.removeIf(tag -> value.equals(tag.getAsString()));
    }
}
