package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Writes the bots of a battle to NBT and reads them back: its slots with names, scores and NPCs, its squads, and what each
 * side has spent. Restored bots are adopted again by their NPCs' UUIDs once their chunks load. Any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotBattleCodec
{
    /** Ticks a restored bot may take to be loaded again with its chunk before it counts as lost. */
    static final long RESTORE_GRACE = 600L;

    private static final String NBT_INSTANCE = "instance";
    private static final String NBT_NEXT_SLOT = "next_slot";
    private static final String NBT_NEXT_SQUAD = "next_squad";
    private static final String NBT_OPENING = "opening";
    private static final String NBT_TICKETS = "tickets";
    private static final String NBT_INFANTRY_SPAWNS = "infantry_spawns";
    private static final String NBT_VEHICLE_SPAWNS = "vehicle_spawns";
    private static final String NBT_SQUAD_DEPLOYMENTS = "squad_deployments";
    private static final String NBT_COMMANDERS = "commanders";
    private static final String NBT_FOUGHT = "sides_that_fought";
    private static final String NBT_FIELDED = "fielded_players";
    private static final String NBT_SLOTS = "slots";
    private static final String NBT_SQUADS = "squads";
    private static final String NBT_ID = "id";
    private static final String NBT_TEAM = "team";
    private static final String NBT_SOURCE = "source";
    private static final String NBT_ENTRY = "entry";
    private static final String NBT_INITIAL = "initial";
    private static final String NBT_SQUAD = "squad";
    private static final String NBT_NAME = "name";
    private static final String NBT_ROLE = "role";
    private static final String NBT_ENTITY = "entity";
    private static final String NBT_RESPAWN_AT = "respawn_at";
    private static final String NBT_LAST_SEEN = "last_seen";
    private static final String NBT_LAST_POSITION = "last_position";
    private static final String NBT_DIED_AT = "died_at";
    private static final String NBT_RETIRED = "retired";
    private static final String NBT_SPAWNS = "spawns";
    private static final String NBT_KILLS = "kills";
    private static final String NBT_DEATHS = "deaths";
    private static final String NBT_WRECK = "wreck_pending";
    private static final String NBT_TEMPLATE = "template";
    private static final String NBT_MEMBERS = "members";
    private static final String NBT_TASK = "task";
    private static final String NBT_KIND = "kind";
    private static final String NBT_TARGET = "target";
    private static final String NBT_RADIUS = "radius";
    private static final String NBT_PATROL = "patrol_index";
    private static final String NBT_CARGO = "cargo";
    private static final String NBT_DISBANDED = "disbanded";
    private static final String NBT_LEADER = "leader";
    private static final String NBT_RALLY = "rally";
    private static final String NBT_RALLY_UNTIL = "rally_until";
    private static final String NBT_SPAWN_CHOICES = "spawn_choices";

    /** @return the instance of the battle stored in a tag, or empty for a tag holding none */
    static Optional<UUID> instance(CompoundTag tag)
    {
        return tag.hasUUID(NBT_INSTANCE) ? Optional.of(tag.getUUID(NBT_INSTANCE)) : Optional.empty();
    }

    // --- Writing ---

    static void write(BotBattle current, CompoundTag tag)
    {
        tag.putUUID(NBT_INSTANCE, current.getInstance());
        tag.putInt(NBT_NEXT_SLOT, current.getNextSlot());
        tag.putInt(NBT_NEXT_SQUAD, current.getNextSquad());
        tag.putBoolean(NBT_OPENING, current.isOpening());
        tag.put(NBT_TICKETS, sides(current.getTickets()));
        CompoundTag infantry = new CompoundTag();
        current.getInfantrySpawns().forEach(infantry::putInt);
        tag.put(NBT_INFANTRY_SPAWNS, infantry);
        tag.put(NBT_VEHICLE_SPAWNS, entries(current.getVehicleSpawns()));
        tag.put(NBT_SQUAD_DEPLOYMENTS, entries(current.getSquadDeployments()));
        CompoundTag commanders = new CompoundTag();
        current.getCommanders().forEach((side, player) -> commanders.putUUID(Integer.toString(side), player));
        tag.put(NBT_COMMANDERS, commanders);
        tag.putIntArray(NBT_FOUGHT, current.getSidesThatFought().stream().mapToInt(Integer::intValue).toArray());
        ListTag fielded = new ListTag();
        current.getFieldedPlayers().forEach(player -> fielded.add(NbtUtils.createUUID(player)));
        tag.put(NBT_FIELDED, fielded);
        CompoundTag choices = new CompoundTag();
        current.getSpawnChoices().forEach((player, choice) -> choices.putString(player.toString(), choice.name()));
        tag.put(NBT_SPAWN_CHOICES, choices);
        ListTag slots = new ListTag();
        current.getSlots().values().forEach(slot -> slots.add(write(slot)));
        tag.put(NBT_SLOTS, slots);
        ListTag squads = new ListTag();
        current.getSquads().values().forEach(squad -> squads.add(write(squad)));
        tag.put(NBT_SQUADS, squads);
    }

    private static CompoundTag write(BotSlot slot)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_ID, slot.getId());
        tag.putString(NBT_TEAM, slot.getTeam());
        tag.putString(NBT_SOURCE, slot.getSource().name());
        tag.putInt(NBT_ENTRY, slot.getEntry());
        tag.putBoolean(NBT_INITIAL, slot.isInitial());
        tag.putInt(NBT_SQUAD, slot.getSquad());
        tag.putString(NBT_NAME, slot.getName());
        tag.putString(NBT_ROLE, slot.getRole());
        if (slot.getEntity() != null)
            tag.putUUID(NBT_ENTITY, slot.getEntity());
        tag.putLong(NBT_RESPAWN_AT, slot.getRespawnAt());
        tag.putLong(NBT_LAST_SEEN, slot.getLastSeen());
        if (slot.getLastPosition() != null)
            tag.put(NBT_LAST_POSITION, position(slot.getLastPosition()));
        tag.putLong(NBT_DIED_AT, slot.getDiedAt());
        tag.putBoolean(NBT_RETIRED, slot.isRetired());
        tag.putInt(NBT_SPAWNS, slot.getSpawns());
        tag.putInt(NBT_KILLS, slot.getKills());
        tag.putInt(NBT_DEATHS, slot.getDeaths());
        tag.putBoolean(NBT_WRECK, slot.isWreckPending());
        return tag;
    }

    private static CompoundTag write(BotSquad squad)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_ID, squad.getId());
        tag.putString(NBT_TEAM, squad.getTeam());
        tag.putString(NBT_ROLE, squad.getRole().name());
        tag.putString(NBT_SOURCE, squad.getSource().name());
        tag.putInt(NBT_ENTRY, squad.getEntry());
        tag.putString(NBT_TEMPLATE, squad.getTemplate());
        tag.putIntArray(NBT_MEMBERS, squad.getSlots().stream().mapToInt(Integer::intValue).toArray());
        SquadTask task = squad.getTask();
        if (task != null)
        {
            CompoundTag taskTag = new CompoundTag();
            taskTag.putString(NBT_KIND, task.kind().name());
            taskTag.put(NBT_TARGET, position(task.target()));
            taskTag.putDouble(NBT_RADIUS, task.radius());
            tag.put(NBT_TASK, taskTag);
        }
        tag.putInt(NBT_PATROL, squad.getPatrolIndex());
        tag.putString(NBT_CARGO, squad.getCargo());
        tag.putBoolean(NBT_DISBANDED, squad.isDisbanded());
        if (squad.getLeader() != null)
            tag.putUUID(NBT_LEADER, squad.getLeader());
        if (squad.getRally() != null)
        {
            tag.put(NBT_RALLY, position(squad.getRally()));
            tag.putLong(NBT_RALLY_UNTIL, squad.getRallyUntil());
        }
        return tag;
    }

    private static CompoundTag sides(Map<Integer, Integer> values)
    {
        CompoundTag tag = new CompoundTag();
        values.forEach((side, value) -> tag.putInt(Integer.toString(side), value));
        return tag;
    }

    private static CompoundTag entries(Map<String, Map<Integer, Integer>> counts)
    {
        CompoundTag tag = new CompoundTag();
        counts.forEach((team, byEntry) -> tag.put(team, sides(byEntry)));
        return tag;
    }

    private static ListTag position(Vec3 position)
    {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(position.x));
        list.add(DoubleTag.valueOf(position.y));
        list.add(DoubleTag.valueOf(position.z));
        return list;
    }

    // --- Reading ---

    /**
     * Fills a battle, freshly made for the same instance, with what was stored. Restored bots count as seen just now, plus
     * a grace, so the chunks they stand in may load before they count as lost.
     */
    static void restore(BotBattle current, CompoundTag tag, long now)
    {
        current.setNextSlot(Math.max(1, tag.getInt(NBT_NEXT_SLOT)));
        current.setNextSquad(Math.max(1, tag.getInt(NBT_NEXT_SQUAD)));
        current.setOpening(tag.getBoolean(NBT_OPENING));
        readSides(tag.getCompound(NBT_TICKETS), current.getTickets());
        CompoundTag infantry = tag.getCompound(NBT_INFANTRY_SPAWNS);
        infantry.getAllKeys().forEach(team -> current.getInfantrySpawns().put(team, infantry.getInt(team)));
        readEntries(tag.getCompound(NBT_VEHICLE_SPAWNS), current.getVehicleSpawns());
        readEntries(tag.getCompound(NBT_SQUAD_DEPLOYMENTS), current.getSquadDeployments());
        CompoundTag commanders = tag.getCompound(NBT_COMMANDERS);
        for (String side : commanders.getAllKeys())
            side(side).ifPresent(index -> current.getCommanders().put(index, commanders.getUUID(side)));
        Arrays.stream(tag.getIntArray(NBT_FOUGHT)).forEach(current.getSidesThatFought()::add);
        for (Tag player : tag.getList(NBT_FIELDED, Tag.TAG_INT_ARRAY))
            current.getFieldedPlayers().add(NbtUtils.loadUUID(player));
        CompoundTag choices = tag.getCompound(NBT_SPAWN_CHOICES);
        for (String player : choices.getAllKeys())
            player(player).ifPresent(id -> current.getSpawnChoices().put(id, BotSettings.parse(BotLeaders.SpawnChoice.class, choices.getString(player), BotLeaders.SpawnChoice.BASE)));
        for (Tag element : tag.getList(NBT_SLOTS, Tag.TAG_COMPOUND))
        {
            BotSlot slot = readSlot((CompoundTag) element, now);
            current.getSlots().put(slot.getId(), slot);
            if (slot.getEntity() != null)
                current.getSlotOfEntity().put(slot.getEntity(), slot.getId());
        }
        for (Tag element : tag.getList(NBT_SQUADS, Tag.TAG_COMPOUND))
        {
            BotSquad squad = readSquad((CompoundTag) element);
            current.getSquads().put(squad.getId(), squad);
        }
    }

    private static BotSlot readSlot(CompoundTag tag, long now)
    {
        BotSlot slot = new BotSlot(tag.getInt(NBT_ID), tag.getString(NBT_TEAM), BotSettings.parse(BotSlot.Source.class, tag.getString(NBT_SOURCE), BotSlot.Source.INFANTRY), tag.getInt(NBT_ENTRY),
            tag.getBoolean(NBT_INITIAL));
        slot.setSquad(tag.getInt(NBT_SQUAD));
        slot.setName(tag.getString(NBT_NAME));
        slot.setRole(tag.getString(NBT_ROLE));
        slot.setEntity(tag.hasUUID(NBT_ENTITY) ? tag.getUUID(NBT_ENTITY) : null);
        slot.setRespawnAt(tag.getLong(NBT_RESPAWN_AT));
        slot.setLastSeen(Math.max(tag.getLong(NBT_LAST_SEEN), now + RESTORE_GRACE));
        slot.setLastPosition(tag.contains(NBT_LAST_POSITION, Tag.TAG_LIST) ? readPosition(tag.getList(NBT_LAST_POSITION, Tag.TAG_DOUBLE)) : null);
        slot.setDiedAt(tag.getLong(NBT_DIED_AT));
        slot.setRetired(tag.getBoolean(NBT_RETIRED));
        slot.setSpawns(tag.getInt(NBT_SPAWNS));
        slot.setKills(tag.getInt(NBT_KILLS));
        slot.setDeaths(tag.getInt(NBT_DEATHS));
        slot.setWreckPending(tag.getBoolean(NBT_WRECK));
        return slot;
    }

    private static BotSquad readSquad(CompoundTag tag)
    {
        BotSquad squad = new BotSquad(tag.getInt(NBT_ID), tag.getString(NBT_TEAM), BotSettings.parse(BotRole.class, tag.getString(NBT_ROLE), BotRole.INFANTRY),
            BotSettings.parse(BotSlot.Source.class, tag.getString(NBT_SOURCE), BotSlot.Source.INFANTRY), tag.getInt(NBT_ENTRY), tag.getString(NBT_TEMPLATE));
        Arrays.stream(tag.getIntArray(NBT_MEMBERS)).forEach(squad.getSlots()::add);
        if (tag.contains(NBT_TASK, Tag.TAG_COMPOUND))
        {
            CompoundTag task = tag.getCompound(NBT_TASK);
            squad.setTask(new SquadTask(BotSettings.parse(SquadTask.Kind.class, task.getString(NBT_KIND), SquadTask.Kind.GUARD), readPosition(task.getList(NBT_TARGET, Tag.TAG_DOUBLE)),
                task.getDouble(NBT_RADIUS)));
        }
        squad.setPatrolIndex(tag.getInt(NBT_PATROL));
        squad.setCargo(tag.getString(NBT_CARGO));
        squad.setDisbanded(tag.getBoolean(NBT_DISBANDED));
        squad.setLeader(tag.hasUUID(NBT_LEADER) ? tag.getUUID(NBT_LEADER) : null);
        if (tag.contains(NBT_RALLY, Tag.TAG_LIST))
        {
            squad.setRally(readPosition(tag.getList(NBT_RALLY, Tag.TAG_DOUBLE)));
            squad.setRallyUntil(tag.getLong(NBT_RALLY_UNTIL));
        }
        return squad;
    }

    private static void readSides(CompoundTag tag, Map<Integer, Integer> into)
    {
        for (String side : tag.getAllKeys())
            side(side).ifPresent(index -> into.put(index, tag.getInt(side)));
    }

    private static void readEntries(CompoundTag tag, Map<String, Map<Integer, Integer>> into)
    {
        for (String team : tag.getAllKeys())
            readSides(tag.getCompound(team), into.computeIfAbsent(team, ignored -> new HashMap<>()));
    }

    private static Optional<Integer> side(String key)
    {
        try
        {
            return Optional.of(Integer.parseInt(key));
        }
        catch (NumberFormatException e)
        {
            return Optional.empty();
        }
    }

    private static Optional<UUID> player(String key)
    {
        try
        {
            return Optional.of(UUID.fromString(key));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    private static Vec3 readPosition(ListTag list)
    {
        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }
}
