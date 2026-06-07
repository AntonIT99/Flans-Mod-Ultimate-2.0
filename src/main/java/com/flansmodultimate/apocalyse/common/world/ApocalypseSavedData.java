package com.flansmodultimate.apocalyse.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ApocalypseSavedData extends SavedData
{
    private static final String DATA_NAME = "flansmodultimate_apocalypse";
    private static final String TAG_ENTRY_POINTS = "EntryPoints";
    private static final String TAG_DEATH_POINTS = "DeathPoints";

    private final Map<UUID, BlockPos> entryPoints = new HashMap<>();
    private final Map<UUID, BlockPos> deathPoints = new HashMap<>();

    public static ApocalypseSavedData get(ServerLevel level)
    {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld != null ? overworld : level;
        return storageLevel.getDataStorage().computeIfAbsent(ApocalypseSavedData::load, ApocalypseSavedData::new, DATA_NAME);
    }

    public static ApocalypseSavedData load(CompoundTag tag)
    {
        ApocalypseSavedData data = new ApocalypseSavedData();
        ListTag list = tag.getList(TAG_ENTRY_POINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID("UUID");
            BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            data.entryPoints.put(uuid, pos);
        }

        ListTag deaths = tag.getList(TAG_DEATH_POINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < deaths.size(); i++)
        {
            CompoundTag entry = deaths.getCompound(i);
            UUID uuid = entry.getUUID("UUID");
            BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            data.deathPoints.put(uuid, pos);
        }
        return data;
    }

    public void setEntryPoint(UUID uuid, BlockPos pos)
    {
        entryPoints.put(uuid, pos.immutable());
        setDirty();
    }

    public Optional<BlockPos> getEntryPoint(UUID uuid)
    {
        return Optional.ofNullable(entryPoints.get(uuid));
    }

    public void setDeathPoint(UUID uuid, BlockPos pos)
    {
        deathPoints.put(uuid, pos.immutable());
        setDirty();
    }

    public Optional<BlockPos> getDeathPoint(UUID uuid)
    {
        return Optional.ofNullable(deathPoints.get(uuid));
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : entryPoints.entrySet())
        {
            CompoundTag pointTag = new CompoundTag();
            BlockPos pos = entry.getValue();
            pointTag.putUUID("UUID", entry.getKey());
            pointTag.putInt("X", pos.getX());
            pointTag.putInt("Y", pos.getY());
            pointTag.putInt("Z", pos.getZ());
            list.add(pointTag);
        }
        tag.put(TAG_ENTRY_POINTS, list);

        ListTag deaths = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : deathPoints.entrySet())
        {
            CompoundTag pointTag = new CompoundTag();
            BlockPos pos = entry.getValue();
            pointTag.putUUID("UUID", entry.getKey());
            pointTag.putInt("X", pos.getX());
            pointTag.putInt("Y", pos.getY());
            pointTag.putInt("Z", pos.getZ());
            deaths.add(pointTag);
        }
        tag.put(TAG_DEATH_POINTS, deaths);
        return tag;
    }
}
