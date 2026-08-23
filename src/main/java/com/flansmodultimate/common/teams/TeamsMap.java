package com.flansmodultimate.common.teams;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeamsMap
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_DIMENSION = "dimension";
    private static final String NBT_BASES = "bases";
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";

    @Getter
    private final String shortName;
    @Getter
    private String name;
    @Getter @Setter
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

    public void setName(String name)
    {
        this.name = StringUtils.defaultIfBlank(name, shortName).trim();
    }

    public Set<UUID> getBases()
    {
        return Collections.unmodifiableSet(bases);
    }

    public boolean addBase(UUID id)
    {
        return bases.add(id);
    }

    public boolean addBase(UUID id, BlockPos position)
    {
        basePositions.put(id, position.immutable());
        return bases.add(id);
    }

    public boolean removeBase(UUID id)
    {
        basePositions.remove(id);
        return bases.remove(id);
    }

    public Map<UUID, BlockPos> getBasePositions()
    {
        return Collections.unmodifiableMap(basePositions);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, shortName);
        tag.putString(NBT_NAME, name);
        tag.putString(NBT_DIMENSION, dimension.identifier().toString());
        ListTag baseList = new ListTag();
        for (UUID id : bases)
        {
            CompoundTag base = new CompoundTag();
            base.store(NBT_ID, UUIDUtil.CODEC, id);
            BlockPos position = basePositions.get(id);
            if (position != null)
            {
                base.putInt(NBT_X, position.getX());
                base.putInt(NBT_Y, position.getY());
                base.putInt(NBT_Z, position.getZ());
            }
            baseList.add(base);
        }
        tag.put(NBT_BASES, baseList);
        return tag;
    }

    public static TeamsMap load(CompoundTag tag)
    {
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(NBT_DIMENSION, ""));
        if (dimensionId == null)
            dimensionId = Level.OVERWORLD.identifier();
        TeamsMap map = new TeamsMap(tag.getStringOr(NBT_ID, "unknown"), tag.getStringOr(NBT_NAME, "Unknown"), ResourceKey.create(Registries.DIMENSION, dimensionId));
        for (Tag entry : tag.getListOrEmpty(NBT_BASES))
        {
            CompoundTag base = (CompoundTag) entry;
            UUID id = base.read(NBT_ID, UUIDUtil.LENIENT_CODEC).orElse(null);
            if (id != null)
            {
                map.bases.add(id);
                if (base.contains(NBT_X) && base.contains(NBT_Y) && base.contains(NBT_Z))
                    map.basePositions.put(id, new BlockPos(base.getIntOr(NBT_X, 0), base.getIntOr(NBT_Y, 0), base.getIntOr(NBT_Z, 0)));
            }
        }
        return map;
    }
}
