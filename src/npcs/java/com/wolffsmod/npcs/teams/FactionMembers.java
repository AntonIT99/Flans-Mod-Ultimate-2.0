package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.platform.world.NpcSavedData;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * The Custom NPCs faction each player belongs to, any faction but those of Teams rounds, and when each player last left
 * each faction. A faction treats its members, and the members of factions it is at war or allied with, by that rather
 * than by faction points alone, in Teams rounds or not; see {@link FactionStance}. Saved with the world. Server thread.
 */
public final class FactionMembers extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_faction_members";
    private static final String NBT_MEMBERS = "members";
    private static final String NBT_LEFT = "left";
    private static final String KEY = "faction.";
    /** How Custom NPCs factions stand toward each other. */
    @SuppressWarnings("unused") // Reached through FactionMixin, which IntelliJ does not see as an entry point.
    private static final FactionStance.Relations RELATIONS = new FactionStance.Relations()
    {
        @Override
        public boolean attacks(int faction, int other)
        {
            return BattleFactions.attacks(faction, other);
        }

        @Override
        public boolean allied(int faction, int other)
        {
            FactionController controller = FactionController.instance;
            return FactionRelations.allied(controller.getFaction(faction), controller.getFaction(other));
        }
    };

    /** What joining a faction came to: the faction left for it, if any, and the points that cost. */
    public record Joined(Optional<Faction> left, int penalty)
    {}

    private final Map<UUID, Integer> members = new HashMap<>();
    /** The game time each player last left each faction, by faction id. */
    private final Map<UUID, Map<Integer, Long>> left = new HashMap<>();

    public static FactionMembers get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, () -> adopting(BattleFactions.get(server).takeLegacyMembers()), FactionMembers::load);
    }

    /** A new store with the members battle factions kept before membership moved here. */
    static FactionMembers adopting(Map<UUID, Integer> legacy)
    {
        FactionMembers data = new FactionMembers();
        data.members.putAll(legacy);
        if (!legacy.isEmpty())
            data.setDirty();
        return data;
    }

    public Optional<Integer> factionOf(UUID player)
    {
        return Optional.ofNullable(members.get(player));
    }

    /** Makes a player belong to a faction, or to none, whatever its joining rules. */
    public void setMember(UUID player, @Nullable Integer faction)
    {
        if (faction == null)
            members.remove(player);
        else
            members.put(player, faction);
        setDirty();
    }

    public long memberCount(int faction)
    {
        return members.values().stream().filter(member -> member == faction).count();
    }

    /** Notes that a player left a faction at the given game time. */
    public void markLeft(UUID player, int faction, long gameTime)
    {
        left.computeIfAbsent(player, id -> new HashMap<>()).put(faction, gameTime);
        setDirty();
    }

    /** The game ticks since a player last left a faction, or empty if they never did. */
    public OptionalLong ticksSinceLeft(UUID player, int faction, long gameTime)
    {
        Long time = left.getOrDefault(player, Map.of()).get(faction);
        return time == null ? OptionalLong.empty() : OptionalLong.of(Math.max(0, gameTime - time));
    }

    // --- Stance ---

    /**
     * How a faction treats a player, by the faction the player belongs to and the player's own points with it, in Teams
     * rounds or not. Empty to leave it to Custom NPCs' faction points, as for players of no faction unless a managed
     * faction is neutral to them, and on clients.
     */
    public static Optional<BotFactions.Stance> stance(int factionId, Player player)
    {
        MinecraftServer server = player.getServer();
        if (player.level().isClientSide || server == null)
            return Optional.empty();
        Faction faction = FactionController.instance.getFaction(factionId);
        if (faction == null)
            return Optional.empty();
        Optional<BattleFactions.Managed> managed = BattleFactions.get(server).managed(factionId);
        Integer own = get(server).members.get(player.getUUID());
        if (own != null && FactionController.instance.getFaction(own) == null)
            own = null;
        boolean grudge = own != null && points(player, faction) < faction.neutralPoints;
        FactionStance.Standing standing = new FactionStance.Standing(own, grudge, player.getAbilities().instabuild);
        BattleFactions.Outsiders outsiders = managed.map(BattleFactions.Managed::getOutsiders).orElse(BattleFactions.Outsiders.POINTS);
        return FactionStance.resolve(managed.isPresent() && BattleFactions.atRest(), factionId, standing, RELATIONS, outsiders);
    }

    // --- Joining and leaving ---

    /** Why a faction refuses to take a player who asks to join it by themselves, or empty when it takes them. */
    public static Optional<FactionJoining.Refusal> refusal(ServerPlayer player, Faction faction)
    {
        FactionJoining joining = FactionJoining.of(faction);
        long now = player.server.overworld().getGameTime();
        OptionalLong since = get(player.server).ticksSinceLeft(player.getUUID(), faction.id, now);
        return FactionJoining.refusal(joining.wolffsmodnpcsJoinRule(), faction.hideFaction, points(player, faction), faction.neutralPoints, faction.friendlyPoints,
            joining.wolffsmodnpcsRejoinCooldown(), since.orElse(Long.MAX_VALUE));
    }

    /** Makes a player a member of a faction by their own choice, leaving the faction they belonged to before as by {@link #leave}. */
    public static Joined join(ServerPlayer player, Faction faction)
    {
        Optional<Faction> previous = leave(player);
        int penalty = previous.map(FactionMembers::leavePenalty).orElse(0);
        get(player.server).setMember(player.getUUID(), faction.id);
        return new Joined(previous, penalty);
    }

    /**
     * Takes a player out of the faction they belong to, by their own choice: they lose the faction's leave penalty in
     * points with it and may join it again only after its rejoin cooldown.
     *
     * @return the faction left, or empty when the player belonged to none
     */
    public static Optional<Faction> leave(ServerPlayer player)
    {
        FactionMembers data = get(player.server);
        Optional<Faction> faction = data.factionOf(player.getUUID()).map(FactionController.instance::getFaction);
        data.setMember(player.getUUID(), null);
        faction.ifPresent(left ->
        {
            data.markLeft(player.getUUID(), left.id, player.server.overworld().getGameTime());
            int penalty = leavePenalty(left);
            if (penalty > 0)
                addPoints(player, left, -penalty);
        });
        return faction;
    }

    /** The points leaving a faction by choice costs. */
    public static int leavePenalty(Faction faction)
    {
        return Math.max(0, FactionJoining.of(faction).wolffsmodnpcsLeavePenalty());
    }

    /** Throws a player out of their faction once their points with it fall below its neutral points. */
    public static void expelIfHated(Player player, int factionId)
    {
        MinecraftServer server = player.getServer();
        if (!(player instanceof ServerPlayer serverPlayer) || server == null)
            return;
        FactionMembers data = get(server);
        Faction faction = FactionController.instance.getFaction(factionId);
        if (faction == null || data.factionOf(player.getUUID()).filter(id -> id == factionId).isEmpty() || points(player, faction) >= faction.neutralPoints)
            return;
        data.setMember(player.getUUID(), null);
        data.markLeft(player.getUUID(), factionId, server.overworld().getGameTime());
        serverPlayer.sendSystemMessage(BotCommands.text(KEY + "expelled", faction.name));
    }

    private static int points(Player player, Faction faction)
    {
        return PlayerData.get(player).factionData.getFactionPoints(player, faction.id);
    }

    private static void addPoints(Player player, Faction faction, int points)
    {
        PlayerData data = PlayerData.get(player);
        data.factionData.increasePoints(player, faction.id, points);
        data.updateClient = true;
    }

    // --- Saving ---

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        CompoundTag memberTag = new CompoundTag();
        members.forEach((player, faction) -> memberTag.putInt(player.toString(), faction));
        tag.put(NBT_MEMBERS, memberTag);
        CompoundTag leftTag = new CompoundTag();
        left.forEach((player, factions) ->
        {
            CompoundTag times = new CompoundTag();
            factions.forEach((faction, time) -> times.putLong(Integer.toString(faction), time));
            leftTag.put(player.toString(), times);
        });
        tag.put(NBT_LEFT, leftTag);
        return tag;
    }

    // NpcSavedData passes the registries; this store holds no registry data.
    @SuppressWarnings("unused")
    static FactionMembers load(CompoundTag tag, HolderLookup.Provider registries)
    {
        FactionMembers data = new FactionMembers();
        CompoundTag memberTag = tag.getCompound(NBT_MEMBERS);
        for (String player : memberTag.getAllKeys())
            if (memberTag.contains(player, Tag.TAG_INT))
                BattleFactions.parseUuid(player).ifPresent(id -> data.members.put(id, memberTag.getInt(player)));
        CompoundTag leftTag = tag.getCompound(NBT_LEFT);
        for (String player : leftTag.getAllKeys())
        {
            CompoundTag times = leftTag.getCompound(player);
            BattleFactions.parseUuid(player).ifPresent(id ->
            {
                for (String faction : times.getAllKeys())
                    parseInt(faction).ifPresent(factionId -> data.left.computeIfAbsent(id, key -> new HashMap<>()).put(factionId, times.getLong(faction)));
            });
        }
        return data;
    }

    private static OptionalInt parseInt(String value)
    {
        try
        {
            return OptionalInt.of(Integer.parseInt(value));
        }
        catch (NumberFormatException e)
        {
            return OptionalInt.empty();
        }
    }
}
