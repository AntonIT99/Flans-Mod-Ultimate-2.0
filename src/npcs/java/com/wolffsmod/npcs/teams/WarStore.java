package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.platform.world.NpcSavedData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;

import java.util.*;

/** The wars of the world with their territories and engagements, written out whenever the world is saved. Server thread. */
final class WarStore extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_wars";
    private static final String NBT_WARS = "wars";

    private final Map<UUID, War> wars = new LinkedHashMap<>();

    static WarStore get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, WarStore::new, WarStore::load);
    }

    Collection<War> all()
    {
        return Collections.unmodifiableCollection(wars.values());
    }

    /** @return the war of a name, whatever its case */
    Optional<War> named(String name)
    {
        return wars.values().stream().filter(war -> war.getName().equalsIgnoreCase(name)).findFirst();
    }

    void add(War war)
    {
        wars.put(war.getId(), war);
        setDirty();
    }

    void remove(War war)
    {
        wars.remove(war.getId());
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        wars.values().forEach(war -> list.add(war.save()));
        tag.put(NBT_WARS, list);
        return tag;
    }

    // NpcSavedData passes the registries; this store holds no registry data.
    @SuppressWarnings("unused")
    static WarStore load(CompoundTag tag, HolderLookup.Provider registries)
    {
        WarStore store = new WarStore();
        for (Tag element : tag.getList(NBT_WARS, Tag.TAG_COMPOUND))
            War.load((CompoundTag) element).ifPresent(war -> store.wars.put(war.getId(), war));
        return store;
    }
}
