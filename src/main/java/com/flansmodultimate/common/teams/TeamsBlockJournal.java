package com.flansmodultimate.common.teams;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** First-change terrain journal, persisted alongside the round rather than held only in memory. */
final class TeamsBlockJournal
{
    private static final String NBT_BLOCK_ENTITY = "block_entity";

    private record Position(ResourceKey<Level> dimension, BlockPos pos)
    {}

    private record Snapshot(BlockState state, CompoundTag blockEntity)
    {}

    private final Map<Position, Snapshot> blocks = new LinkedHashMap<>();
    private boolean restoring;

    void capture(ServerLevel level, BlockPos pos, BlockState replacement)
    {
        if (restoring)
            return;
        Position key = new Position(level.dimension(), pos.immutable());
        if (blocks.containsKey(key))
            return;
        BlockState original = level.getBlockState(pos);
        if (original == replacement)
            return;
        BlockEntity entity = level.getBlockEntity(pos);
        blocks.put(key, new Snapshot(original, entity == null ? null : entity.saveWithFullMetadata()));
    }

    void restore(MinecraftServer server)
    {
        restoring = true;
        try
        {
            // Install the entire structure before allowing shape updates, so doors, beds and supports survive.
            for (Map.Entry<Position, Snapshot> entry : blocks.entrySet())
            {
                Position key = entry.getKey();
                ServerLevel level = server.getLevel(key.dimension());
                if (level == null)
                    continue;
                Clearable.tryClear(level.getBlockEntity(key.pos()));
                level.setBlock(key.pos(), entry.getValue().state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                CompoundTag tag = entry.getValue().blockEntity();
                if (tag != null)
                {
                    BlockEntity entity = level.getBlockEntity(key.pos());
                    if (entity != null)
                    {
                        entity.load(tag.copy());
                        entity.setChanged();
                        level.sendBlockUpdated(key.pos(), entry.getValue().state(), entry.getValue().state(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            // Keep entries for temporarily unavailable dimensions rather than silently losing them.
            blocks.keySet().removeIf(key -> server.getLevel(key.dimension()) != null);
        }
        finally
        {
            restoring = false;
        }
    }

    ListTag save()
    {
        ListTag list = new ListTag();
        blocks.forEach((key, snapshot) ->
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", key.dimension().location().toString());
            tag.putLong("pos", key.pos().asLong());
            tag.put("state", NbtUtils.writeBlockState(snapshot.state()));
            if (snapshot.blockEntity() != null)
                tag.put(NBT_BLOCK_ENTITY, snapshot.blockEntity().copy());
            list.add(tag);
        });
        return list;
    }

    @SuppressWarnings("deprecation") // Forge deprecates the vanilla block registry, whose holder lookup decodes saved block states.
    void load(ListTag list)
    {
        blocks.clear();
        for (Tag element : list)
        {
            CompoundTag tag = (CompoundTag) element;
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
            if (dimensionId == null)
                continue;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("state"));
            blocks.put(new Position(dimension, BlockPos.of(tag.getLong("pos"))), new Snapshot(state, tag.contains(NBT_BLOCK_ENTITY) ? tag.getCompound(NBT_BLOCK_ENTITY).copy() : null));
        }
    }
}
