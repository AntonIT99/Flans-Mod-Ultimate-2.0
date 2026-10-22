package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * The skirmishes of the world, drafts included, with the state of their bots, written out whenever the world is saved. The
 * NPCs themselves are saved with their chunks; a skirmish adopts them again when the server starts. Server thread.
 */
final class SkirmishStore extends SavedData
{
    private static final String ID = "wolffsmodnpcs_skirmishes";
    private static final String NBT_SKIRMISHES = "skirmishes";

    private final Map<UUID, Skirmish> skirmishes = new LinkedHashMap<>();

    public static SkirmishStore get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(SkirmishStore::load, SkirmishStore::new, ID);
    }

    public Collection<Skirmish> all()
    {
        return Collections.unmodifiableCollection(skirmishes.values());
    }

    /** @return the skirmish of a name, whatever its case */
    public Optional<Skirmish> named(String name)
    {
        return skirmishes.values().stream().filter(skirmish -> skirmish.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void add(Skirmish skirmish)
    {
        skirmishes.put(skirmish.getId(), skirmish);
        setDirty();
    }

    public void remove(Skirmish skirmish)
    {
        skirmishes.remove(skirmish.getId());
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag)
    {
        ListTag list = new ListTag();
        skirmishes.values().forEach(skirmish -> list.add(skirmish.save()));
        tag.put(NBT_SKIRMISHES, list);
        return tag;
    }

    static SkirmishStore load(CompoundTag tag)
    {
        SkirmishStore store = new SkirmishStore();
        for (Tag element : tag.getList(NBT_SKIRMISHES, Tag.TAG_COMPOUND))
            Skirmish.load((CompoundTag) element).ifPresent(skirmish -> store.skirmishes.put(skirmish.getId(), skirmish));
        return store;
    }
}
