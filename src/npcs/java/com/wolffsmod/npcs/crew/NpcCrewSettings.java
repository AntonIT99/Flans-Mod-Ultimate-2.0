package com.wolffsmod.npcs.crew;

import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * How a vehicle NPC is crewed: who takes each kind of seat, whether it is crewed by itself whenever it spawns or comes
 * back, what its crew may do and suffer while aboard, and who gets out to fight. Kept with the NPC's stats, so it is saved, edited, cloned and
 * synchronized to the editor with them.
 */
@Getter
@Setter
public final class NpcCrewSettings
{
    public static final String NBT_KEY = "WolffsModCrew";
    private static final String NBT_AUTO = "Auto";
    private static final String NBT_PROTECTED = "Protected";
    private static final String NBT_DESTROYED = "Destroyed";
    private static final String NBT_PASSENGERS_FIRE = "PassengersFire";
    private static final String NBT_SEATS = "Seats";
    private static final String NBT_SEAT = "Seat";
    private static final String NBT_SOURCE = "Source";
    private static final String NBT_HIDDEN_SEATS = "HiddenSeats";
    private static final String NBT_PASSENGERS_DISEMBARK = "PassengersDisembark";
    private static final String NBT_DISEMBARK_SEATS = "DisembarkSeats";
    private static final String NBT_ABOARD_SEATS = "AboardSeats";

    /** What happens to the crew aboard when the vehicle is destroyed. */
    public enum Destroyed
    {
        /** The crew get out beside the vehicle, by parachute from the air, and fight on foot. */
        BAIL_OUT,
        /** The crew die with the vehicle. */
        DIE,
        /** The crew are removed with the vehicle, leaving nothing behind. */
        VANISH;

        public String key()
        {
            return "wolffsmodnpcs.crew.destroyed." + name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final Map<CrewPost, CrewSource> sources = new EnumMap<>(CrewPost.class);
    /** Explicit per-seat choices, including Nobody; absent seats inherit the legacy role source. */
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final Map<Integer, CrewSource> seats = new TreeMap<>();
    /** Seats whose assigned NPC crew are not rendered while aboard. No entry means visible. */
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final Set<Integer> hiddenSeats = new TreeSet<>();
    /** Seats whose crew get out to fight, or stay aboard, whatever their post. No entry follows the post. */
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final Map<Integer, Boolean> disembarkSeats = new TreeMap<>();
    /** Whether the vehicle crews its empty seats by itself each time it spawns or comes back. */
    private boolean autoCrew = true;
    /** Whether the crew take no harm while aboard, the vehicle being what gets hit. */
    private boolean protectedCrew = true;
    private Destroyed destroyed = Destroyed.BAIL_OUT;
    /** Whether crew in seats without a gun fight with their own weapons; drivers and gunners never do. */
    private boolean passengersFire;
    /**
     * Whether crew in seats without a gun get out to fight on foot when the vehicle closes with its target, and from an
     * aircraft above it jump with a parachute they carry. Drivers and gunners stay aboard unless their own seat says otherwise.
     */
    private boolean passengersDisembark = true;

    public static NpcCrewSettings of(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCrew();
    }

    public CrewSource source(CrewPost post)
    {
        return sources.getOrDefault(post, CrewSource.NONE);
    }

    public void setSource(CrewPost post, CrewSource source)
    {
        if (CrewSource.NONE.equals(source))
            sources.remove(post);
        else
            sources.put(post, source);
    }

    public CrewSource source(int seat, CrewPost post)
    {
        return seats.getOrDefault(seat, source(post));
    }

    public void setSource(int seat, CrewSource source)
    {
        if (seat >= 0)
            seats.put(seat, source);
    }

    public boolean hasSeatSource(int seat)
    {
        return seats.containsKey(seat);
    }

    public void clearSeatSource(int seat)
    {
        seats.remove(seat);
    }

    public boolean isSeatVisible(int seat)
    {
        return !hiddenSeats.contains(seat);
    }

    public void setSeatVisible(int seat, boolean visible)
    {
        if (seat < 0)
            return;
        if (visible)
            hiddenSeats.remove(seat);
        else
            hiddenSeats.add(seat);
    }

    /** @return whether the crew member in a seat gets out to fight: as its own seat says, or else whether it is a passenger seat and passengers do */
    public boolean disembarks(int seat, CrewPost post)
    {
        return seatDisembarks(seat).orElse(post == CrewPost.PASSENGER && passengersDisembark);
    }

    /** @return the seat's own choice whether its crew member gets out to fight; empty when the seat follows its post */
    public Optional<Boolean> seatDisembarks(int seat)
    {
        return Optional.ofNullable(disembarkSeats.get(seat));
    }

    /**
     * @param disembarks
     *            whether the seat's crew member gets out to fight; null makes the seat follow its post again
     */
    public void setSeatDisembarks(int seat, @Nullable Boolean disembarks)
    {
        if (seat < 0)
            return;
        if (disembarks == null)
            disembarkSeats.remove(seat);
        else
            disembarkSeats.put(seat, disembarks);
    }

    /** Whether any post names someone to crew with. */
    public boolean namesAnyone()
    {
        return sources.values().stream().anyMatch(CrewSource::names) || seats.values().stream().anyMatch(CrewSource::names);
    }

    /** @return how many settings differ from those of a new NPC */
    public int changes()
    {
        int count = selectionChanges();
        count += autoCrew ? 0 : 1;
        count += protectedCrew ? 0 : 1;
        count += destroyed == Destroyed.BAIL_OUT ? 0 : 1;
        count += passengersDisembark ? 0 : 1;
        return count + (passengersFire ? 1 : 0);
    }

    /** The selections shown on Seats, separate from the shared Behaviour rules. */
    public int selectionChanges()
    {
        return (int) sources.values().stream().filter(source -> source.kind() != CrewSource.Kind.NONE).count() + seats.size() + hiddenSeats.size() + disembarkSeats.size();
    }

    /** Adds the settings to the stats tag; nothing for an NPC that keeps the defaults. */
    public void save(CompoundTag stats)
    {
        if (changes() == 0)
            return;
        stats.put(NBT_KEY, write());
    }

    public CompoundTag write()
    {
        CompoundTag tag = new CompoundTag();
        sources.forEach((post, source) -> tag.put(post.name(), source.save()));
        ListTag seatTags = new ListTag();
        seats.forEach((seat, source) ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_SEAT, seat);
            entry.put(NBT_SOURCE, source.save());
            seatTags.add(entry);
        });
        if (!seatTags.isEmpty())
            tag.put(NBT_SEATS, seatTags);
        if (!hiddenSeats.isEmpty())
            tag.putIntArray(NBT_HIDDEN_SEATS, hiddenSeats.stream().mapToInt(Integer::intValue).toArray());
        if (disembarkSeats.containsValue(true))
            tag.putIntArray(NBT_DISEMBARK_SEATS, seatsChoosing(true));
        if (disembarkSeats.containsValue(false))
            tag.putIntArray(NBT_ABOARD_SEATS, seatsChoosing(false));
        tag.putBoolean(NBT_PASSENGERS_DISEMBARK, passengersDisembark);
        tag.putBoolean(NBT_AUTO, autoCrew);
        tag.putBoolean(NBT_PROTECTED, protectedCrew);
        tag.putString(NBT_DESTROYED, destroyed.name());
        tag.putBoolean(NBT_PASSENGERS_FIRE, passengersFire);
        return tag;
    }

    private int[] seatsChoosing(boolean disembarks)
    {
        return disembarkSeats.entrySet().stream().filter(entry -> entry.getValue() == disembarks).mapToInt(Map.Entry::getKey).toArray();
    }

    /** Reads the settings from the stats tag; a tag without them returns every setting to its default. */
    public void load(CompoundTag stats)
    {
        read(stats.getCompound(NBT_KEY));
    }

    public void read(CompoundTag tag)
    {
        sources.clear();
        seats.clear();
        hiddenSeats.clear();
        disembarkSeats.clear();
        for (CrewPost post : CrewPost.values())
            if (tag.contains(post.name(), Tag.TAG_COMPOUND))
                setSource(post, CrewSource.load(tag.getCompound(post.name())));
        ListTag seatTags = tag.getList(NBT_SEATS, Tag.TAG_COMPOUND);
        for (int i = 0; i < seatTags.size(); i++)
        {
            CompoundTag entry = seatTags.getCompound(i);
            if (entry.contains(NBT_SEAT, Tag.TAG_INT) && entry.contains(NBT_SOURCE, Tag.TAG_COMPOUND))
                setSource(entry.getInt(NBT_SEAT), CrewSource.load(entry.getCompound(NBT_SOURCE)));
        }
        for (int seat : tag.getIntArray(NBT_HIDDEN_SEATS))
            setSeatVisible(seat, false);
        for (int seat : tag.getIntArray(NBT_DISEMBARK_SEATS))
            setSeatDisembarks(seat, true);
        for (int seat : tag.getIntArray(NBT_ABOARD_SEATS))
            setSeatDisembarks(seat, false);
        passengersDisembark = !tag.contains(NBT_PASSENGERS_DISEMBARK) || tag.getBoolean(NBT_PASSENGERS_DISEMBARK);
        autoCrew = !tag.contains(NBT_AUTO) || tag.getBoolean(NBT_AUTO);
        protectedCrew = !tag.contains(NBT_PROTECTED) || tag.getBoolean(NBT_PROTECTED);
        destroyed = Destroyed.BAIL_OUT;
        for (Destroyed candidate : Destroyed.values())
            if (candidate.name().equals(tag.getString(NBT_DESTROYED)))
                destroyed = candidate;
        passengersFire = tag.getBoolean(NBT_PASSENGERS_FIRE);
    }
}
