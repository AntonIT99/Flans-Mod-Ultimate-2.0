package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.platform.world.NpcSavedData;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;

import java.util.*;

/**
 * The Custom NPCs factions managed for battles fought outside Teams rounds: the Flan teams that serve each one as its
 * branches, and how it treats players of no faction; the players who belong to a faction, managed or not, are kept by
 * {@link FactionMembers}. The factions themselves, with their names, colours and hostilities, stay Custom NPCs' own, so
 * its faction menu shows and edits them; any NPC set to one in that menu fights for it. Saved with the world. Server
 * thread.
 */
public final class BattleFactions extends NpcSavedData
{
    private static final String ID = "wolffsmodnpcs_battle_factions";
    private static final String NBT_FACTIONS = "factions";
    private static final String NBT_ID = "id";
    private static final String NBT_OUTSIDERS = "outsiders";
    private static final String NBT_BRANCHES = "branches";
    private static final String NBT_TEAM = "team";
    private static final String NBT_FORCE = "force";
    private static final String NBT_ROLES = "roles";
    private static final String NBT_WEIGHT = "weight";
    private static final String NBT_THEATRES = "theatres";
    /** Members were saved here before {@link FactionMembers} kept them; read only to hand them over. */
    private static final String NBT_MEMBERS = "members";

    /** How a managed faction treats players who belong to no faction. */
    public enum Outsiders
    {
        /** Neither attacks nor protects them, whatever their faction points. */
        NEUTRAL,
        /** By their Custom NPCs faction points, as an unmanaged faction does. */
        POINTS
    }

    /** A managed faction, by its Custom NPCs faction id. */
    @Getter
    public static final class Managed
    {
        private final int id;
        private final List<Branch> branches = new ArrayList<>();
        @Setter
        private Outsiders outsiders = Outsiders.NEUTRAL;

        Managed(int id)
        {
            this.id = id;
        }

        public Optional<Branch> branch(String team)
        {
            return branches.stream().filter(branch -> branch.getTeam().equals(team)).findFirst();
        }
    }

    /** A Flan team serving a faction: the soldiers and vehicles it brings, the roles it fills, and its share of the force. */
    @Getter
    public static final class Branch
    {
        private final String team;
        @Setter
        private BotSettings.TeamForce force;
        /** The roles the branch fills; empty for every role its classes and vehicles can fill. */
        private final Set<BotRole> roles = EnumSet.noneOf(BotRole.class);
        /** The branch's share of a role several branches fill, against theirs. */
        @Setter
        private int weight = 1;
        /** The biomes, and biome tags starting with #, the branch serves in; empty for anywhere. */
        private final Set<String> theatres = new LinkedHashSet<>();

        Branch(String team, BotSettings.TeamForce force)
        {
            this.team = team;
            this.force = force;
        }
    }

    private final Map<Integer, Managed> factions = new LinkedHashMap<>();
    /** The members saved here before membership moved to {@link FactionMembers}, until it takes them. */
    private final Map<UUID, Integer> legacyMembers = new HashMap<>();

    public static BattleFactions get(MinecraftServer server)
    {
        return NpcSavedData.computeIfAbsent(server.overworld(), ID, BattleFactions::new, BattleFactions::load);
    }

    public Optional<Managed> managed(int id)
    {
        return Optional.ofNullable(factions.get(id));
    }

    public Collection<Managed> all()
    {
        return Collections.unmodifiableCollection(factions.values());
    }

    /** Starts managing a Custom NPCs faction, or returns it when it is managed already. */
    public Managed manage(int id)
    {
        Managed managed = factions.computeIfAbsent(id, Managed::new);
        setDirty();
        return managed;
    }

    /** Stops managing a faction; the Custom NPCs faction and its members stay as they are. */
    public boolean release(int id)
    {
        boolean released = factions.remove(id) != null;
        setDirty();
        return released;
    }

    /** Hands over the members saved here before membership moved to {@link FactionMembers}; they are no longer saved here. */
    Map<UUID, Integer> takeLegacyMembers()
    {
        Map<UUID, Integer> taken = Map.copyOf(legacyMembers);
        if (!legacyMembers.isEmpty())
            setDirty();
        legacyMembers.clear();
        return taken;
    }

    // --- Faction rules ---

    /** Whether managed factions are at rest: while a Teams round is played, nobody is their enemy. */
    public static boolean atRest()
    {
        return FlansTeams.getRunningRound().isPresent();
    }

    /** Whether the NPCs of two factions leave each other alone because managed factions are at rest. */
    public static boolean atRest(@Nullable MinecraftServer server, int faction, int other)
    {
        if (server == null || !atRest())
            return false;
        BattleFactions data = get(server);
        return data.factions.containsKey(faction) || data.factions.containsKey(other);
    }

    /** Whether the first Custom NPCs faction attacks the second. */
    static boolean attacks(int faction, int other)
    {
        Faction found = FactionController.instance.getFaction(faction);
        return found != null && found.hostileToFaction(other);
    }

    // --- Saving ---

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        for (Managed managed : factions.values())
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_ID, managed.getId());
            entry.putString(NBT_OUTSIDERS, managed.getOutsiders().name());
            ListTag branches = new ListTag();
            for (Branch branch : managed.getBranches())
            {
                CompoundTag branchTag = new CompoundTag();
                branchTag.putString(NBT_TEAM, branch.getTeam());
                branchTag.put(NBT_FORCE, branch.getForce().save());
                ListTag roles = new ListTag();
                branch.getRoles().forEach(role -> roles.add(StringTag.valueOf(role.name())));
                branchTag.put(NBT_ROLES, roles);
                branchTag.putInt(NBT_WEIGHT, branch.getWeight());
                ListTag theatres = new ListTag();
                branch.getTheatres().forEach(theatre -> theatres.add(StringTag.valueOf(theatre)));
                branchTag.put(NBT_THEATRES, theatres);
                branches.add(branchTag);
            }
            entry.put(NBT_BRANCHES, branches);
            list.add(entry);
        }
        tag.put(NBT_FACTIONS, list);
        return tag;
    }

    // NpcSavedData passes the registries; this store holds no registry data.
    @SuppressWarnings("unused")
    static BattleFactions load(CompoundTag tag, HolderLookup.Provider registries)
    {
        BattleFactions data = new BattleFactions();
        for (Tag element : tag.getList(NBT_FACTIONS, Tag.TAG_COMPOUND))
        {
            CompoundTag entry = (CompoundTag) element;
            Managed managed = new Managed(entry.getInt(NBT_ID));
            managed.setOutsiders(BotSettings.parse(Outsiders.class, entry.getString(NBT_OUTSIDERS), Outsiders.NEUTRAL));
            for (Tag branchElement : entry.getList(NBT_BRANCHES, Tag.TAG_COMPOUND))
            {
                CompoundTag branchTag = (CompoundTag) branchElement;
                Branch branch = new Branch(branchTag.getString(NBT_TEAM), BotSettings.TeamForce.load(branchTag.getCompound(NBT_FORCE)));
                for (Tag role : branchTag.getList(NBT_ROLES, Tag.TAG_STRING))
                    roleNamed(role.getAsString()).ifPresent(branch.getRoles()::add);
                branch.setWeight(Math.max(1, branchTag.getInt(NBT_WEIGHT)));
                branchTag.getList(NBT_THEATRES, Tag.TAG_STRING).forEach(theatre -> branch.getTheatres().add(theatre.getAsString()));
                managed.getBranches().add(branch);
            }
            data.factions.put(managed.getId(), managed);
        }
        CompoundTag memberTag = tag.getCompound(NBT_MEMBERS);
        for (String player : memberTag.getAllKeys())
        {
            int faction = memberTag.getInt(player);
            parseUuid(player).ifPresent(id -> data.legacyMembers.put(id, faction));
        }
        return data;
    }

    private static Optional<BotRole> roleNamed(String name)
    {
        return Arrays.stream(BotRole.values()).filter(role -> role.name().equals(name)).findFirst();
    }

    static Optional<UUID> parseUuid(String value)
    {
        try
        {
            return Optional.of(UUID.fromString(value));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }
}
