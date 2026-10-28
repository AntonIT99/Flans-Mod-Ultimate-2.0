package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.platform.world.NpcSavedData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.EnumMap;
import java.util.Map;

/** A world's friendly fire settings, for factions without settings of their own. Saved with the world; server thread only. */
public final class FriendlyFireSettings extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_friendly_fire";
    private final Map<FriendlyFire.Kind, FriendlyFire.Setting> settings = new EnumMap<>(FriendlyFire.Kind.class);

    public static FriendlyFireSettings get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, FriendlyFireSettings::new, FriendlyFireSettings::load);
    }

    public FriendlyFire.Setting get(FriendlyFire.Kind kind)
    {
        return settings.getOrDefault(kind, FriendlyFire.Setting.DEFAULT);
    }

    public void set(FriendlyFire.Kind kind, FriendlyFire.Setting setting)
    {
        settings.put(kind, setting);
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        return tag.merge(FriendlyFire.save(settings));
    }

    // The saved-data platform supplies registries; the settings are plain switches.
    @SuppressWarnings("unused")
    static FriendlyFireSettings load(CompoundTag tag, HolderLookup.Provider registries)
    {
        FriendlyFireSettings data = new FriendlyFireSettings();
        FriendlyFire.load(tag, data.settings);
        return data;
    }
}
