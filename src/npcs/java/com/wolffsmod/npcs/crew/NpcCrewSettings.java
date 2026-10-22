package com.wolffsmod.npcs.crew;

import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * How a vehicle NPC is crewed: who takes each kind of seat, whether it is crewed by itself whenever it spawns or comes
 * back, and what its crew may do and suffer while aboard. Kept with the NPC's stats, so it is saved, edited, cloned and
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
    /** Whether the vehicle crews its empty seats by itself each time it spawns or comes back. */
    private boolean autoCrew;
    /** Whether the crew take no harm while aboard, the vehicle being what gets hit. */
    private boolean protectedCrew = true;
    private Destroyed destroyed = Destroyed.BAIL_OUT;
    /** Whether crew in seats without a gun fight with their own weapons; drivers and gunners never do. */
    private boolean passengersFire;

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

    /** Whether any post names someone to crew with. */
    public boolean namesAnyone()
    {
        return sources.values().stream().anyMatch(CrewSource::names);
    }

    /** @return how many settings differ from those of a new NPC */
    public int changes()
    {
        int count = (int) sources.values().stream().filter(source -> source.kind() != CrewSource.Kind.NONE).count();
        count += autoCrew ? 1 : 0;
        count += protectedCrew ? 0 : 1;
        count += destroyed == Destroyed.BAIL_OUT ? 0 : 1;
        return count + (passengersFire ? 1 : 0);
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
        tag.putBoolean(NBT_AUTO, autoCrew);
        tag.putBoolean(NBT_PROTECTED, protectedCrew);
        tag.putString(NBT_DESTROYED, destroyed.name());
        tag.putBoolean(NBT_PASSENGERS_FIRE, passengersFire);
        return tag;
    }

    /** Reads the settings from the stats tag; a tag without them returns every setting to its default. */
    public void load(CompoundTag stats)
    {
        read(stats.getCompound(NBT_KEY));
    }

    public void read(CompoundTag tag)
    {
        sources.clear();
        for (CrewPost post : CrewPost.values())
            if (tag.contains(post.name(), Tag.TAG_COMPOUND))
                setSource(post, CrewSource.load(tag.getCompound(post.name())));
        autoCrew = tag.getBoolean(NBT_AUTO);
        protectedCrew = !tag.contains(NBT_PROTECTED) || tag.getBoolean(NBT_PROTECTED);
        destroyed = Destroyed.BAIL_OUT;
        for (Destroyed candidate : Destroyed.values())
            if (candidate.name().equals(tag.getString(NBT_DESTROYED)))
                destroyed = candidate;
        passengersFire = tag.getBoolean(NBT_PASSENGERS_FIRE);
    }
}
