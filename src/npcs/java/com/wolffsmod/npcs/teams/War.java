package com.wolffsmod.npcs.teams;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * An endless conflict between managed factions over territory held chunk by chunk. Wherever players come near a border
 * between factions at war, the war opens an engagement there: a skirmish whose sides come in from either side of the
 * border and push. A side that stands alone in the other's chunk long enough takes it, and the engagement moves on with
 * the front; an engagement nobody is near any more falls asleep. A war is fought in some dimensions or all of them.
 * Server thread.
 */
@Getter
@Setter
final class War
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_FACTIONS = "factions";
    private static final String NBT_DIMENSIONS = "dimensions";
    private static final String NBT_TERRITORY = "territory";
    private static final String NBT_DIMENSION = "dimension";
    private static final String NBT_CHUNKS = "chunks";
    private static final String NBT_OWNERS = "owners";
    private static final String NBT_RUNNING = "running";
    private static final String NBT_INFANTRY = "infantry";
    private static final String NBT_RESPAWN = "respawn_seconds";
    private static final String NBT_TICKETS = "tickets";
    private static final String NBT_RANGE = "range";
    private static final String NBT_ENGAGEMENTS = "engagements";
    private static final String NBT_MOST = "most_engagements";
    private static final String NBT_FLIP = "flip_seconds";
    private static final String NBT_DIFFICULTY = "difficulty";
    private static final String NBT_SKIRMISH = "skirmish";
    private static final String NBT_FIRST = "first";
    private static final String NBT_SECOND = "second";
    private static final String NBT_FIRST_FACTION = "first_faction";
    private static final String NBT_SECOND_FACTION = "second_faction";
    private static final String NBT_PRESSING = "pressing";
    private static final String NBT_PROGRESS = "progress";
    private static final String NBT_IDLE = "idle";
    private static final String NBT_COUNT = "count";

    /** A fight at a border of the war, fought as a skirmish of its own. */
    @Getter
    @Setter
    static final class Engagement
    {
        private final UUID skirmish;
        private WarFront.Border border;
        /** The side gaining ground, 0 or 1 as in the border, or -1 for neither. */
        private int pressing = -1;
        /** Seconds the side has pressed on without a break. */
        private int progress;
        /** Seconds nobody has been near it. */
        private int idle;

        Engagement(UUID skirmish, WarFront.Border border)
        {
            this.skirmish = skirmish;
            this.border = border;
        }

        /**
         * Counts a while of fighting: a side pressing on gains it, until it takes the other's chunk; a break or a change
         * of side starts over.
         *
         * @return whether the pressing side takes the chunk now
         */
        boolean advance(int side, int seconds, int flipSeconds)
        {
            if (side < 0 || side != pressing)
            {
                pressing = side;
                progress = side < 0 ? 0 : seconds;
            }
            else
                progress += seconds;
            return pressing >= 0 && progress >= flipSeconds;
        }
    }

    private final UUID id;
    private final String name;
    /** The managed factions fighting it. */
    private final List<Integer> factions = new ArrayList<>();
    /** The dimensions it is fought in; empty for all of them. */
    private final Set<ResourceKey<Level>> dimensions = new LinkedHashSet<>();
    /** Who holds each chunk, by dimension and chunk position key. */
    private final Map<ResourceKey<Level>, Map<Long, Integer>> territory = new LinkedHashMap<>();
    private final List<Engagement> engagements = new ArrayList<>();
    private boolean running;
    /** Loose infantry each side of an engagement fields. */
    private int infantry = 12;
    private int respawnSeconds = 30;
    /** Tickets each side of an engagement may spend; 0 for no limit. */
    private int tickets;
    /** Blocks from a border within which players wake an engagement there. */
    private int range = 128;
    /** Engagements fought at once at most. */
    private int mostEngagements = 3;
    /** Seconds a side must stand alone in the other's chunk to take it. */
    private int flipSeconds = 60;
    private BotSettings.Difficulty difficulty = BotSettings.Difficulty.DEFAULT;
    /** Engagements opened so far, to name the next. */
    private int count;

    War(UUID id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /** Whether the war is fought in a dimension. */
    boolean covers(ResourceKey<Level> dimension)
    {
        return dimensions.isEmpty() || dimensions.contains(dimension);
    }

    /** @return who holds the chunks of a dimension, made empty on first use */
    Map<Long, Integer> territory(ResourceKey<Level> dimension)
    {
        return territory.computeIfAbsent(dimension, ignored -> new HashMap<>());
    }

    /** @return the chunks each faction holds, in all dimensions */
    Map<Integer, Integer> holdings()
    {
        Map<Integer, Integer> held = new LinkedHashMap<>();
        factions.forEach(faction -> held.put(faction, 0));
        territory.values().forEach(chunks -> chunks.values().forEach(faction -> held.merge(faction, 1, Integer::sum)));
        return held;
    }

    // --- Saving ---

    CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_ID, id);
        tag.putString(NBT_NAME, name);
        tag.putIntArray(NBT_FACTIONS, factions.stream().mapToInt(Integer::intValue).toArray());
        ListTag dimensionList = new ListTag();
        dimensions.forEach(dimension -> dimensionList.add(StringTag.valueOf(dimension.location().toString())));
        tag.put(NBT_DIMENSIONS, dimensionList);
        ListTag territoryList = new ListTag();
        territory.forEach((dimension, chunks) ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putString(NBT_DIMENSION, dimension.location().toString());
            entry.put(NBT_CHUNKS, new LongArrayTag(chunks.keySet().stream().mapToLong(Long::longValue).toArray()));
            entry.putIntArray(NBT_OWNERS, chunks.values().stream().mapToInt(Integer::intValue).toArray());
            territoryList.add(entry);
        });
        tag.put(NBT_TERRITORY, territoryList);
        ListTag engagementList = new ListTag();
        engagements.forEach(engagement -> engagementList.add(save(engagement)));
        tag.put(NBT_ENGAGEMENTS, engagementList);
        tag.putBoolean(NBT_RUNNING, running);
        tag.putInt(NBT_INFANTRY, infantry);
        tag.putInt(NBT_RESPAWN, respawnSeconds);
        tag.putInt(NBT_TICKETS, tickets);
        tag.putInt(NBT_RANGE, range);
        tag.putInt(NBT_MOST, mostEngagements);
        tag.putInt(NBT_FLIP, flipSeconds);
        tag.putString(NBT_DIFFICULTY, difficulty.name());
        tag.putInt(NBT_COUNT, count);
        return tag;
    }

    private static CompoundTag save(Engagement engagement)
    {
        CompoundTag entry = new CompoundTag();
        entry.putUUID(NBT_SKIRMISH, engagement.getSkirmish());
        entry.putLong(NBT_FIRST, engagement.getBorder().first());
        entry.putInt(NBT_FIRST_FACTION, engagement.getBorder().firstFaction());
        entry.putLong(NBT_SECOND, engagement.getBorder().second());
        entry.putInt(NBT_SECOND_FACTION, engagement.getBorder().secondFaction());
        entry.putInt(NBT_PRESSING, engagement.getPressing());
        entry.putInt(NBT_PROGRESS, engagement.getProgress());
        entry.putInt(NBT_IDLE, engagement.getIdle());
        return entry;
    }

    static Optional<War> load(CompoundTag tag)
    {
        if (!tag.hasUUID(NBT_ID))
            return Optional.empty();
        War war = new War(tag.getUUID(NBT_ID), tag.getString(NBT_NAME));
        Arrays.stream(tag.getIntArray(NBT_FACTIONS)).forEach(war.factions::add);
        for (Tag dimension : tag.getList(NBT_DIMENSIONS, Tag.TAG_STRING))
            dimension(dimension.getAsString()).ifPresent(war.dimensions::add);
        for (Tag element : tag.getList(NBT_TERRITORY, Tag.TAG_COMPOUND))
        {
            CompoundTag entry = (CompoundTag) element;
            long[] chunks = entry.getLongArray(NBT_CHUNKS);
            int[] owners = entry.getIntArray(NBT_OWNERS);
            dimension(entry.getString(NBT_DIMENSION)).ifPresent(dimension ->
            {
                Map<Long, Integer> held = war.territory(dimension);
                for (int i = 0; i < Math.min(chunks.length, owners.length); i++)
                    held.put(chunks[i], owners[i]);
            });
        }
        for (Tag element : tag.getList(NBT_ENGAGEMENTS, Tag.TAG_COMPOUND))
        {
            CompoundTag entry = (CompoundTag) element;
            if (!entry.hasUUID(NBT_SKIRMISH))
                continue;
            Engagement engagement = new Engagement(entry.getUUID(NBT_SKIRMISH),
                new WarFront.Border(entry.getLong(NBT_FIRST), entry.getInt(NBT_FIRST_FACTION), entry.getLong(NBT_SECOND), entry.getInt(NBT_SECOND_FACTION)));
            engagement.setPressing(entry.getInt(NBT_PRESSING));
            engagement.setProgress(entry.getInt(NBT_PROGRESS));
            engagement.setIdle(entry.getInt(NBT_IDLE));
            war.engagements.add(engagement);
        }
        war.running = tag.getBoolean(NBT_RUNNING);
        war.infantry = Math.max(0, tag.getInt(NBT_INFANTRY));
        war.respawnSeconds = Math.max(0, tag.getInt(NBT_RESPAWN));
        war.tickets = Math.max(0, tag.getInt(NBT_TICKETS));
        war.range = Math.max(16, tag.getInt(NBT_RANGE));
        war.mostEngagements = Math.max(1, tag.getInt(NBT_MOST));
        war.flipSeconds = Math.max(5, tag.getInt(NBT_FLIP));
        war.difficulty = BotSettings.parse(BotSettings.Difficulty.class, tag.getString(NBT_DIFFICULTY), BotSettings.Difficulty.DEFAULT);
        war.count = tag.getInt(NBT_COUNT);
        return Optional.of(war);
    }

    private static Optional<ResourceKey<Level>> dimension(String text)
    {
        ResourceLocation id = ResourceLocation.tryParse(text);
        return id == null ? Optional.empty() : Optional.of(ResourceKey.create(Registries.DIMENSION, id));
    }
}
