package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.platform.world.NpcSavedData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The skirmishes of the world, drafts included, with the state of their bots, written out whenever the world is saved. The
 * NPCs themselves are saved with their chunks; a skirmish adopts them again when the server starts. Server thread.
 */
final class SkirmishStore extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_skirmishes";
    private static final String NBT_SKIRMISHES = "skirmishes";
    private static final String NBT_PRESETS = "presets";

    private final Map<UUID, Skirmish> skirmishes = new LinkedHashMap<>();
    /** Skirmishes set up once and kept as drafts, to start alike again; by name, lower case. */
    private final Map<String, Skirmish> presets = new TreeMap<>();

    public static SkirmishStore get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, SkirmishStore::new, SkirmishStore::load);
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

    public Optional<Skirmish> byId(UUID id)
    {
        return Optional.ofNullable(skirmishes.get(id));
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

    public Map<String, Skirmish> presets()
    {
        return Collections.unmodifiableMap(presets);
    }

    /** Keeps a skirmish as a preset of a name, replacing one of the same name. */
    public void savePreset(String name, Skirmish skirmish)
    {
        String key = name.toLowerCase(Locale.ROOT);
        presets.put(key, skirmish.draftCopy(UUID.randomUUID(), key, skirmish.getDimension(), Vec3.ZERO));
        setDirty();
    }

    public boolean removePreset(String name)
    {
        boolean removed = presets.remove(name.toLowerCase(Locale.ROOT)) != null;
        setDirty();
        return removed;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        skirmishes.values().forEach(skirmish -> list.add(skirmish.save()));
        tag.put(NBT_SKIRMISHES, list);
        ListTag presetList = new ListTag();
        presets.values().forEach(preset -> presetList.add(preset.save()));
        tag.put(NBT_PRESETS, presetList);
        return tag;
    }

    // NpcSavedData passes the registries; this store holds no registry data.
    @SuppressWarnings("unused")
    static SkirmishStore load(CompoundTag tag, HolderLookup.Provider registries)
    {
        SkirmishStore store = new SkirmishStore();
        for (Tag element : tag.getList(NBT_SKIRMISHES, Tag.TAG_COMPOUND))
            Skirmish.load((CompoundTag) element).ifPresent(skirmish -> store.skirmishes.put(skirmish.getId(), skirmish));
        for (Tag element : tag.getList(NBT_PRESETS, Tag.TAG_COMPOUND))
            Skirmish.load((CompoundTag) element).ifPresent(preset -> store.presets.put(preset.getName(), preset));
        return store;
    }
}
