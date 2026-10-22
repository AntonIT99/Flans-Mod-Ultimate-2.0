package com.wolffsmod.npcs.teams;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * A battle between managed factions fought outside Teams rounds, on no map and over no objective: each side's soldiers and
 * vehicles come in at the side's zone and fight the sides at war with it, until one side is left, time runs out, or it is
 * stopped. Set up as a draft, then started. Kept with the world, its bots included, and at rest while a Teams round is
 * played. Server thread.
 */
@Getter
@Setter
final class Skirmish
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_DIMENSION = "dimension";
    private static final String NBT_SIDES = "sides";
    private static final String NBT_FACTION = "faction";
    private static final String NBT_ANCHOR = "anchor";
    private static final String NBT_RADIUS = "radius";
    private static final String NBT_TEAMS = "teams";
    private static final String NBT_INFANTRY = "infantry";
    private static final String NBT_SQUADS = "squads";
    private static final String NBT_ROLES = "vehicle_roles";
    private static final String NBT_TYPES = "vehicle_types";
    private static final String NBT_INTENT = "intent";
    private static final String NBT_END = "end";
    private static final String NBT_MINUTES = "minutes";
    private static final String NBT_RESPAWN = "respawn";
    private static final String NBT_RESPAWN_SECONDS = "respawn_seconds";
    private static final String NBT_TICKETS = "tickets";
    private static final String NBT_DIFFICULTY = "difficulty";
    private static final String NBT_PROTECTION = "protection";
    private static final String NBT_STATE = "state";
    private static final String NBT_STARTED = "started_at";
    private static final String NBT_ENDED = "ended_at";
    private static final String NBT_FRONT = "front";
    private static final String NBT_SETTINGS = "settings";
    private static final String NBT_BATTLE = "battle";

    /** Where the sides' ground squads go. */
    public enum Intent
    {
        /** Straight for the nearest enemy. */
        HUNT,
        /** To the middle between the zones, turning to enemies that come near. */
        MEET,
        /** To a front that moves toward the weaker side as the battle goes on. */
        PUSH
    }

    /** When it ends by itself. */
    public enum End
    {
        /** Once only sides at peace with each other are left, the others worn out with nothing left to bring in. */
        WIPEOUT,
        /** After its time limit, won by the side with the most bots standing. */
        TIME,
        /** Only when stopped. */
        NEVER
    }

    public enum State
    {
        /** Being set up; no bot fights yet. */
        DRAFT, RUNNING,
        /** Stopped by hand for a while: its bots stand still. */
        PAUSED,
        /** Decided; its bots are removed shortly. */
        ENDED
    }

    /**
     * A side: a managed faction fighting from a zone.
     *
     * @param request
     *            what the faction is asked to field
     * @param teams
     *            the Flan teams that serve the side in this skirmish, fixed when it starts
     */
    record Side(int faction, Vec3 anchor, double radius, SkirmishComposer.Request request, List<String> teams)
    {
        public Side withRequest(SkirmishComposer.Request changed)
        {
            return new Side(faction, anchor, radius, changed, teams);
        }

        public Side withTeams(List<String> started)
        {
            return new Side(faction, anchor, radius, request, List.copyOf(started));
        }
    }

    private final UUID id;
    private final String name;
    private final ResourceKey<Level> dimension;
    private final List<Side> sides = new ArrayList<>();
    private Intent intent = Intent.MEET;
    private End end = End.WIPEOUT;
    private int timeLimitMinutes = 10;
    /** Whether fallen bots come back, after the respawn time and while the tickets last; otherwise each fights once. */
    private boolean respawn;
    private int respawnSeconds = 15;
    /** Tickets each side may spend on bringing bots back; 0 for no limit. */
    private int tickets;
    private BotSettings.Difficulty difficulty = BotSettings.Difficulty.DEFAULT;
    /** Whether its bots cannot be hurt while it rests during a Teams round. */
    private boolean protection = true;
    private State state = State.DRAFT;
    private long startedAt;
    private long endedAt;
    @Nullable
    private Vec3 front;
    /** The forces of the sides' teams, composed when it starts. */
    private BotSettings settings = new BotSettings();
    /** The bots fighting it, once it started or was taken up again after a restart. */
    @Nullable
    private BotBattle battle;
    /** Its bots as stored, kept as they are until it is taken up again. */
    private CompoundTag pendingBattle = new CompoundTag();

    public Skirmish(UUID id, String name, ResourceKey<Level> dimension)
    {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
    }

    /** @return the side a faction fights on, or -1 when it does not fight here */
    public int sideOf(int faction)
    {
        for (int i = 0; i < sides.size(); i++)
            if (sides.get(i).faction() == faction)
                return i;
        return -1;
    }

    /** @return the side a team serves, or -1 when it serves none */
    public int sideOfTeam(String team)
    {
        for (int i = 0; i < sides.size(); i++)
            if (sides.get(i).teams().contains(team))
                return i;
        return -1;
    }

    /** Whether its bots fight now, or would but for a Teams round. */
    public boolean isLive()
    {
        return state == State.RUNNING || state == State.PAUSED;
    }

    // --- Saving ---

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_ID, id);
        tag.putString(NBT_NAME, name);
        tag.putString(NBT_DIMENSION, dimension.location().toString());
        ListTag sideList = new ListTag();
        for (Side side : sides)
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_FACTION, side.faction());
            entry.put(NBT_ANCHOR, position(side.anchor()));
            entry.putDouble(NBT_RADIUS, side.radius());
            ListTag teams = new ListTag();
            side.teams().forEach(team -> teams.add(StringTag.valueOf(team)));
            entry.put(NBT_TEAMS, teams);
            SkirmishComposer.Request request = side.request();
            entry.putInt(NBT_INFANTRY, request.infantry());
            entry.put(NBT_SQUADS, counts(request.squads()));
            CompoundTag roles = new CompoundTag();
            request.vehiclesByRole().forEach((role, count) -> roles.putInt(role.name(), count));
            entry.put(NBT_ROLES, roles);
            entry.put(NBT_TYPES, counts(request.vehiclesByType()));
            sideList.add(entry);
        }
        tag.put(NBT_SIDES, sideList);
        tag.putString(NBT_INTENT, intent.name());
        tag.putString(NBT_END, end.name());
        tag.putInt(NBT_MINUTES, timeLimitMinutes);
        tag.putBoolean(NBT_RESPAWN, respawn);
        tag.putInt(NBT_RESPAWN_SECONDS, respawnSeconds);
        tag.putInt(NBT_TICKETS, tickets);
        tag.putString(NBT_DIFFICULTY, difficulty.name());
        tag.putBoolean(NBT_PROTECTION, protection);
        tag.putString(NBT_STATE, state.name());
        tag.putLong(NBT_STARTED, startedAt);
        tag.putLong(NBT_ENDED, endedAt);
        if (front != null)
            tag.put(NBT_FRONT, position(front));
        tag.put(NBT_SETTINGS, settings.save());
        CompoundTag battleTag = new CompoundTag();
        if (battle != null)
            BotBattleCodec.write(battle, battleTag);
        else
            battleTag.merge(pendingBattle);
        tag.put(NBT_BATTLE, battleTag);
        return tag;
    }

    /** @return the skirmish a tag holds, or empty for one whose dimension cannot be read */
    public static Optional<Skirmish> load(CompoundTag tag)
    {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(NBT_DIMENSION));
        if (!tag.hasUUID(NBT_ID) || dimension == null)
            return Optional.empty();
        Skirmish skirmish = new Skirmish(tag.getUUID(NBT_ID), tag.getString(NBT_NAME), ResourceKey.create(Registries.DIMENSION, dimension));
        for (Tag element : tag.getList(NBT_SIDES, Tag.TAG_COMPOUND))
        {
            CompoundTag entry = (CompoundTag) element;
            List<String> teams = entry.getList(NBT_TEAMS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
            Map<BotRole, Integer> roles = new EnumMap<>(BotRole.class);
            CompoundTag roleTag = entry.getCompound(NBT_ROLES);
            for (String role : roleTag.getAllKeys())
                Arrays.stream(BotRole.values()).filter(candidate -> candidate.name().equals(role)).findFirst().ifPresent(found -> roles.put(found, roleTag.getInt(role)));
            SkirmishComposer.Request request = new SkirmishComposer.Request(entry.getInt(NBT_INFANTRY), counts(entry.getCompound(NBT_SQUADS)), roles, counts(entry.getCompound(NBT_TYPES)));
            skirmish.sides.add(new Side(entry.getInt(NBT_FACTION), readPosition(entry.getList(NBT_ANCHOR, Tag.TAG_DOUBLE)), entry.getDouble(NBT_RADIUS), request, teams));
        }
        skirmish.intent = BotSettings.parse(Intent.class, tag.getString(NBT_INTENT), Intent.MEET);
        skirmish.end = BotSettings.parse(End.class, tag.getString(NBT_END), End.WIPEOUT);
        skirmish.timeLimitMinutes = Math.max(1, tag.getInt(NBT_MINUTES));
        skirmish.respawn = tag.getBoolean(NBT_RESPAWN);
        skirmish.respawnSeconds = Math.max(0, tag.getInt(NBT_RESPAWN_SECONDS));
        skirmish.tickets = Math.max(0, tag.getInt(NBT_TICKETS));
        skirmish.difficulty = BotSettings.parse(BotSettings.Difficulty.class, tag.getString(NBT_DIFFICULTY), BotSettings.Difficulty.DEFAULT);
        skirmish.protection = tag.getBoolean(NBT_PROTECTION);
        skirmish.state = BotSettings.parse(State.class, tag.getString(NBT_STATE), State.DRAFT);
        skirmish.startedAt = tag.getLong(NBT_STARTED);
        skirmish.endedAt = tag.getLong(NBT_ENDED);
        skirmish.front = tag.contains(NBT_FRONT, Tag.TAG_LIST) ? readPosition(tag.getList(NBT_FRONT, Tag.TAG_DOUBLE)) : null;
        skirmish.settings = BotSettings.load(tag.getCompound(NBT_SETTINGS));
        skirmish.pendingBattle = tag.getCompound(NBT_BATTLE).copy();
        return Optional.of(skirmish);
    }

    private static CompoundTag counts(Map<String, Integer> counts)
    {
        CompoundTag tag = new CompoundTag();
        counts.forEach(tag::putInt);
        return tag;
    }

    private static Map<String, Integer> counts(CompoundTag tag)
    {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String key : tag.getAllKeys())
            counts.put(key, Math.max(0, tag.getInt(key)));
        return counts;
    }

    private static ListTag position(Vec3 position)
    {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(position.x));
        list.add(DoubleTag.valueOf(position.y));
        list.add(DoubleTag.valueOf(position.z));
        return list;
    }

    private static Vec3 readPosition(ListTag list)
    {
        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }
}
