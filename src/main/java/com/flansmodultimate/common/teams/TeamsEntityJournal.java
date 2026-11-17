package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.AAGun;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What a map already carried when a round started, persisted alongside the round: its grenade devices, vehicles and
 * AA guns, and, where the map says where it ends, the other creatures and objects standing inside it. A round takes
 * away only what was placed while it ran, so anything it used up, destroyed, damaged or moved is put back the way the
 * map had it once the round is over.
 */
final class TeamsEntityJournal
{
    private static final String NBT_DIMENSION = "dimension";
    private static final String NBT_ENTITY_ID = "uuid";
    private static final String NBT_ENTITY = "entity";
    /** Dropped from a snapshot before it is placed again, so the replacement cannot clash with the entity it replaces. */
    private static final String NBT_UUID = "UUID";
    private static final String NBT_PASSENGERS = "Passengers";

    private record Key(ResourceKey<Level> dimension, UUID entity)
    {}

    private final Map<Key, CompoundTag> entities = new LinkedHashMap<>();

    /** Snapshots an entity as it stands before the round, keeping the first state recorded for it. */
    void capture(ServerLevel level, Entity entity)
    {
        Key key = new Key(level.dimension(), entity.getUUID());
        if (entities.containsKey(key))
            return;
        CompoundTag tag = new CompoundTag();
        if (entity.save(tag))
            entities.put(key, tag);
    }

    /**
     * Puts every snapshot back: one that is still exactly as the map had it is left alone, anything else is replaced
     * by the entity the snapshot describes.
     */
    void restore(MinecraftServer server)
    {
        for (Map.Entry<Key, CompoundTag> entry : entities.entrySet())
        {
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level == null)
                continue;
            Entity present = level.getEntity(entry.getKey().entity());
            if (present != null && !present.isRemoved())
            {
                CompoundTag current = new CompoundTag();
                if (present.save(current) && current.equals(entry.getValue()))
                    continue;
                // Replaced rather than reloaded in place: a wreck keeps state of its own that loading cannot undo.
                discard(present);
            }
            CompoundTag tag = entry.getValue().copy();
            renew(tag);
            EntityType.loadEntityRecursive(tag, level, placed ->
            {
                level.addFreshEntity(placed);
                return placed;
            });
        }
        // Keep entries for temporarily unavailable dimensions rather than silently losing them.
        entities.keySet().removeIf(key -> server.getLevel(key.dimension()) != null);
    }

    /** Lets a snapshot and everything riding in it come back under new identities, clear of the ones being replaced. */
    private static void renew(CompoundTag tag)
    {
        tag.remove(NBT_UUID);
        for (Tag passenger : tag.getList(NBT_PASSENGERS, Tag.TAG_COMPOUND))
            renew((CompoundTag) passenger);
    }

    /** Takes an entity away to make room for its snapshot, without letting the removal spill its contents. */
    private static void discard(Entity entity)
    {
        if (entity instanceof AAGun gun)
            gun.discardWithoutDrops();
        else
            entity.discard();
    }

    ListTag save()
    {
        ListTag list = new ListTag();
        entities.forEach((key, snapshot) ->
        {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_DIMENSION, key.dimension().location().toString());
            tag.putUUID(NBT_ENTITY_ID, key.entity());
            tag.put(NBT_ENTITY, snapshot.copy());
            list.add(tag);
        });
        return list;
    }

    void load(ListTag list)
    {
        entities.clear();
        for (Tag element : list)
        {
            CompoundTag tag = (CompoundTag) element;
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(NBT_DIMENSION));
            if (dimensionId == null || !tag.hasUUID(NBT_ENTITY_ID))
                continue;
            entities.put(new Key(ResourceKey.create(Registries.DIMENSION, dimensionId), tag.getUUID(NBT_ENTITY_ID)), tag.getCompound(NBT_ENTITY).copy());
        }
    }
}
