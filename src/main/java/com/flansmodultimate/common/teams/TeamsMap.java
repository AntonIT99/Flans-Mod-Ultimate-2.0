package com.flansmodultimate.common.teams;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent description of an arena and its team bases. */
public final class TeamsMap
{
    private final String shortName;
    private String name;
    private ResourceKey<Level> dimension;
    private final Set<UUID> bases = new LinkedHashSet<>();
    private final Map<UUID, BlockPos> basePositions = new LinkedHashMap<>();

    public TeamsMap(String shortName, String name, ResourceKey<Level> dimension)
    {
        if (StringUtils.isBlank(shortName))
            throw new IllegalArgumentException("Map short name cannot be blank");
        this.shortName = shortName.trim().toLowerCase(java.util.Locale.ROOT);
        this.name = StringUtils.defaultIfBlank(name, shortName).trim();
        this.dimension = dimension;
    }

    public String getShortName() { return shortName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = StringUtils.defaultIfBlank(name, shortName).trim(); }
    public ResourceKey<Level> getDimension() { return dimension; }
    public void setDimension(ResourceKey<Level> dimension) { this.dimension = dimension; }
    public Set<UUID> getBases() { return Collections.unmodifiableSet(bases); }
    public boolean addBase(UUID id) { return bases.add(id); }
    public boolean addBase(UUID id, BlockPos position) { basePositions.put(id, position.immutable()); return bases.add(id); }
    public boolean removeBase(UUID id) { basePositions.remove(id); return bases.remove(id); }
    public Map<UUID, BlockPos> getBasePositions() { return Collections.unmodifiableMap(basePositions); }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", shortName);
        tag.putString("Name", name);
        tag.putString("Dimension", dimension.location().toString());
        ListTag baseList = new ListTag();
        for (UUID id : bases)
        {
            CompoundTag base = new CompoundTag();
            base.putUUID("Id", id);
            BlockPos position = basePositions.get(id);
            if (position != null)
            {
                base.putInt("X", position.getX());
                base.putInt("Y", position.getY());
                base.putInt("Z", position.getZ());
            }
            baseList.add(base);
        }
        tag.put("Bases", baseList);
        return tag;
    }

    public static TeamsMap load(CompoundTag tag)
    {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimensionId == null)
            dimensionId = Level.OVERWORLD.location();
        TeamsMap map = new TeamsMap(tag.getString("Id"), tag.getString("Name"), ResourceKey.create(Registries.DIMENSION, dimensionId));
        for (Tag entry : tag.getList("Bases", Tag.TAG_COMPOUND))
        {
            CompoundTag base = (CompoundTag) entry;
            if (base.hasUUID("Id"))
            {
                UUID id = base.getUUID("Id");
                map.bases.add(id);
                if (base.contains("X") && base.contains("Y") && base.contains("Z"))
                    map.basePositions.put(id, new BlockPos(base.getInt("X"), base.getInt("Y"), base.getInt("Z")));
            }
        }
        return map;
    }
}
