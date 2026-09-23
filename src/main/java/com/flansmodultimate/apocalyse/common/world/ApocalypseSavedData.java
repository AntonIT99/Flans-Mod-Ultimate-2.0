package com.flansmodultimate.apocalyse.common.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ApocalypseSavedData extends SavedData
{
    private static final Factory<ApocalypseSavedData> FACTORY =
        new Factory<>(ApocalypseSavedData::new, ApocalypseSavedData::load);
    private static final String DATA_NAME = "flansmodultimate_apocalypse";
    private static final String NBT_ENTRY_POINTS = "entry_points";
    private static final String NBT_DEATH_POINTS = "death_points";
    private static final String NBT_UUID = "uuid";
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";
    private static final String NBT_COUNTDOWN = "countdown_ticks";
    private static final String NBT_COUNTDOWN_MECHA = "countdown_mecha";
    private static final String NBT_COUNTDOWN_PLACER = "countdown_placer";
    private static final String NBT_COUNTDOWN_LEVEL = "countdown_level";

    private final Map<UUID, BlockPos> entryPoints = new HashMap<>();
    private final Map<UUID, BlockPos> deathPoints = new HashMap<>();

    /** Ticks left before the apocalypse begins, or zero when no trigger is armed. */
    private int countdownTicks;
    @Nullable
    private UUID countdownMechaId;
    @Nullable
    private UUID countdownPlacerId;
    @Nullable
    private ResourceKey<Level> countdownLevel;

    public static ApocalypseSavedData get(ServerLevel level)
    {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld != null ? overworld : level;
        return storageLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static ApocalypseSavedData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        ApocalypseSavedData data = new ApocalypseSavedData();
        ListTag list = tag.getList(NBT_ENTRY_POINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID(NBT_UUID);
            BlockPos pos = new BlockPos(entry.getInt(NBT_X), entry.getInt(NBT_Y), entry.getInt(NBT_Z));
            data.entryPoints.put(uuid, pos);
        }

        ListTag deaths = tag.getList(NBT_DEATH_POINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < deaths.size(); i++)
        {
            CompoundTag entry = deaths.getCompound(i);
            UUID uuid = entry.getUUID(NBT_UUID);
            BlockPos pos = new BlockPos(entry.getInt(NBT_X), entry.getInt(NBT_Y), entry.getInt(NBT_Z));
            data.deathPoints.put(uuid, pos);
        }

        data.countdownTicks = tag.getInt(NBT_COUNTDOWN);
        if (tag.hasUUID(NBT_COUNTDOWN_MECHA))
            data.countdownMechaId = tag.getUUID(NBT_COUNTDOWN_MECHA);
        if (tag.hasUUID(NBT_COUNTDOWN_PLACER))
            data.countdownPlacerId = tag.getUUID(NBT_COUNTDOWN_PLACER);
        if (tag.contains(NBT_COUNTDOWN_LEVEL, Tag.TAG_STRING))
        {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_COUNTDOWN_LEVEL));
            data.countdownLevel = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        }
        return data;
    }

    /** Arms the AI-chip apocalypse trigger placed by {@code placerId} in {@code level}. */
    public void startCountdown(int ticks, UUID mechaId, @Nullable UUID placerId, ResourceKey<Level> level)
    {
        countdownTicks = Math.max(0, ticks);
        countdownMechaId = mechaId;
        countdownPlacerId = placerId;
        countdownLevel = level;
        setDirty();
    }

    public void setCountdownTicks(int ticks)
    {
        countdownTicks = Math.max(0, ticks);
        setDirty();
    }

    public void clearCountdown()
    {
        countdownTicks = 0;
        countdownMechaId = null;
        countdownPlacerId = null;
        countdownLevel = null;
        setDirty();
    }

    public int getCountdownTicks()
    {
        return countdownTicks;
    }

    public boolean isCountdownRunning()
    {
        return countdownTicks > 0 && countdownMechaId != null && countdownLevel != null;
    }

    @Nullable
    public UUID getCountdownMechaId()
    {
        return countdownMechaId;
    }

    @Nullable
    public UUID getCountdownPlacerId()
    {
        return countdownPlacerId;
    }

    @Nullable
    public ResourceKey<Level> getCountdownLevel()
    {
        return countdownLevel;
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
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : entryPoints.entrySet())
        {
            CompoundTag pointTag = new CompoundTag();
            BlockPos pos = entry.getValue();
            pointTag.putUUID(NBT_UUID, entry.getKey());
            pointTag.putInt(NBT_X, pos.getX()); pointTag.putInt(NBT_Y, pos.getY()); pointTag.putInt(NBT_Z, pos.getZ());
            list.add(pointTag);
        }
        tag.put(NBT_ENTRY_POINTS, list);

        ListTag deaths = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : deathPoints.entrySet())
        {
            CompoundTag pointTag = new CompoundTag();
            BlockPos pos = entry.getValue();
            pointTag.putUUID(NBT_UUID, entry.getKey());
            pointTag.putInt(NBT_X, pos.getX()); pointTag.putInt(NBT_Y, pos.getY()); pointTag.putInt(NBT_Z, pos.getZ());
            deaths.add(pointTag);
        }
        tag.put(NBT_DEATH_POINTS, deaths);

        tag.putInt(NBT_COUNTDOWN, countdownTicks);
        if (countdownMechaId != null)
            tag.putUUID(NBT_COUNTDOWN_MECHA, countdownMechaId);
        if (countdownPlacerId != null)
            tag.putUUID(NBT_COUNTDOWN_PLACER, countdownPlacerId);
        if (countdownLevel != null)
            tag.putString(NBT_COUNTDOWN_LEVEL, countdownLevel.location().toString());
        return tag;
    }
}
