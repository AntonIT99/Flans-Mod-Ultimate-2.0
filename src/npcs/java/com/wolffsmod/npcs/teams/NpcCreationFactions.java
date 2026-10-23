package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.NotNull;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/** Per-builder faction overrides for NPCs created with tools or commands. Saved with the world; server thread only. */
public final class NpcCreationFactions extends FlanSavedData
{
    private static final String ID = "wolffsmodnpcs_creation_factions";
    private static final String PLAYERS = "players";
    private final Map<UUID, Integer> factions = new HashMap<>();

    public static NpcCreationFactions get(MinecraftServer server)
    {
        return SavedDataPlatform.computeIfAbsent(server.overworld(), ID, NpcCreationFactions::new, NpcCreationFactions::load);
    }

    public OptionalInt factionOf(UUID player)
    {
        Integer faction = factions.get(player);
        return faction == null ? OptionalInt.empty() : OptionalInt.of(faction);
    }

    public void set(UUID player, int faction)
    {
        factions.put(player, faction);
        setDirty();
    }

    public void reset(UUID player)
    {
        factions.remove(player);
        setDirty();
    }

    /** Applies the executing player's override, including NPC passengers, while leaving console and command-block spawns alone. */
    public static void applyCommand(CommandSourceStack source, Entity entity)
    {
        if (source.getEntity() instanceof ServerPlayer player)
            entity.getSelfAndPassengers().forEach(spawned -> apply(player, spawned));
    }

    /** Only called by player creation paths, never by entity loading, scripts, or automatic bot spawning. */
    public static void apply(Player creator, Entity entity)
    {
        if (!(creator instanceof ServerPlayer player) || !(entity instanceof EntityNPCInterface npc))
            return;
        MinecraftServer server = player.getServer();
        if (server == null)
            return;
        OptionalInt id = get(server).factionOf(player.getUUID());
        if (id.isEmpty())
            return;
        Faction faction = FactionController.instance.getFaction(id.getAsInt());
        // A removed faction must not silently assign the controller's fallback faction.
        if (faction != null)
        {
            npc.setFaction(faction.id);
            npc.faction = faction;
            npc.updateClient = true;
        }
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        CompoundTag players = new CompoundTag();
        factions.forEach((player, faction) -> players.putInt(player.toString(), faction));
        tag.put(PLAYERS, players);
        return tag;
    }

    // The saved-data platform supplies registries; faction ids are plain integers.
    @SuppressWarnings("unused")
    static NpcCreationFactions load(CompoundTag tag, HolderLookup.Provider registries)
    {
        NpcCreationFactions data = new NpcCreationFactions();
        CompoundTag players = tag.getCompound(PLAYERS);
        for (String player : players.getAllKeys())
        {
            if (!players.contains(player, Tag.TAG_INT) || players.getInt(player) < 0)
                continue;
            try
            {
                data.factions.put(UUID.fromString(player), players.getInt(player));
            }
            catch (IllegalArgumentException ignored)
            {
                // Ignore malformed entries without losing other builders' settings.
            }
        }
        return data;
    }
}
