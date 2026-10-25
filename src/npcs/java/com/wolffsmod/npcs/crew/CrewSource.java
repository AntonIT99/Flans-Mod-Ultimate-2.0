package com.wolffsmod.npcs.crew;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * Who crews the seats of one post of a vehicle NPC: nobody, a Custom NPCs clone, or a new NPC equipped with the loadout
 * of a Flan's player class.
 *
 * @param cloneTab
 *            the clone's tab, from 1 to 15
 * @param team
 *            short name of the team whose class it is, empty for a class looked up on its own
 * @param playerClass
 *            short name of the class
 */
public record CrewSource(Kind kind, int cloneTab, String cloneName, String team, String playerClass, int skin)
{
    public static final int RANDOM_SKIN = -1;
    public static final int MAX_CLONE_TAB = 15;
    public static final CrewSource NONE = new CrewSource(Kind.NONE, 1, "", "", "");
    private static final String NBT_KIND = "Kind";
    private static final String NBT_CLONE_TAB = "CloneTab";
    private static final String NBT_CLONE_NAME = "CloneName";
    private static final String NBT_TEAM = "Team";
    private static final String NBT_CLASS = "Class";
    private static final String NBT_SKIN = "Skin";

    /** Where a crew member comes from. */
    public enum Kind
    {
        /** The seats stay empty. */
        NONE,
        /** A clone from Custom NPCs' clone storage, as saved. */
        CLONE,
        /** A new NPC with the loadout of a Flan's player class. */
        CLASS;

        public String key()
        {
            return "wolffsmodnpcs.crew.source." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** Old role-wide sources used the first class skin. */
    public CrewSource(Kind kind, int cloneTab, String cloneName, String team, String playerClass)
    {
        this(kind, cloneTab, cloneName, team, playerClass, 0);
    }

    public CrewSource
    {
        kind = kind == null ? Kind.NONE : kind;
        cloneTab = Math.max(1, Math.min(MAX_CLONE_TAB, cloneTab));
        cloneName = cloneName == null ? "" : cloneName.strip();
        team = team == null ? "" : team.strip();
        playerClass = playerClass == null ? "" : playerClass.strip();
        skin = Math.max(RANDOM_SKIN, skin);
    }

    public CrewSource withKind(Kind changed)
    {
        return new CrewSource(changed, cloneTab, cloneName, team, playerClass, skin);
    }

    public CrewSource withClone(int tab, String name)
    {
        return new CrewSource(kind, tab, name, team, playerClass, skin);
    }

    public CrewSource withClass(String teamName, String className)
    {
        return new CrewSource(kind, cloneTab, cloneName, teamName, className, team.equals(teamName) && playerClass.equals(className) ? skin : 0);
    }

    public CrewSource withSkin(int variant)
    {
        return new CrewSource(kind, cloneTab, cloneName, team, playerClass, variant);
    }

    /** Chooses once per recruit on the server; the resulting index is synchronized with that NPC. */
    public int chooseSkin(int variants, RandomSource random)
    {
        if (variants <= 0)
            return 0;
        if (skin == RANDOM_SKIN)
            return random.nextInt(variants);
        return skin < variants ? skin : 0;
    }

    /** Whether the source names someone to crew with; a clone without a name or a class without one names nobody. */
    public boolean names()
    {
        return switch (kind)
        {
            case NONE -> false;
            case CLONE -> !cloneName.isEmpty();
            case CLASS -> !playerClass.isEmpty();
        };
    }

    CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_KIND, kind.name());
        tag.putInt(NBT_CLONE_TAB, cloneTab);
        tag.putString(NBT_CLONE_NAME, cloneName);
        tag.putString(NBT_TEAM, team);
        tag.putString(NBT_CLASS, playerClass);
        tag.putInt(NBT_SKIN, skin);
        return tag;
    }

    static CrewSource load(CompoundTag tag)
    {
        Kind kind = Kind.NONE;
        for (Kind candidate : Kind.values())
            if (candidate.name().equals(tag.getString(NBT_KIND)))
                kind = candidate;
        return new CrewSource(kind, tag.getInt(NBT_CLONE_TAB), tag.getString(NBT_CLONE_NAME), tag.getString(NBT_TEAM), tag.getString(NBT_CLASS), tag.getInt(NBT_SKIN));
    }
}
