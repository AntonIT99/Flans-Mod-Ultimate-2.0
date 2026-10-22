package com.wolffsmod.npcs.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A reusable squad composition: which classes it brings and how many of each, and how its commander prefers to use it.
 * A class a team does not offer is replaced, when the squad is raised, by one the team does.
 *
 * @param id
 *            the template's key, lower case
 * @param name
 *            the name shown to operators
 * @param posture
 *            whether the squad is preferably sent to attack or to defend
 * @param nameList
 *            the soldier name list its members are named from; empty takes the team's
 * @param members
 *            the classes, in order, the first one leading the squad
 */
public record SquadTemplate(String id, String name, Posture posture, String nameList, List<Member> members)
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_POSTURE = "posture";
    private static final String NBT_NAME_LIST = "name_list";
    private static final String NBT_MEMBERS = "members";
    private static final String NBT_CLASS = "class";
    private static final String NBT_COUNT = "count";

    /** How a commander prefers to use a squad. */
    public enum Posture
    {
        BALANCED, ASSAULT, DEFENSE
    }

    /**
     * @param playerClass
     *            short name of the class
     * @param count
     *            how many of the squad have it
     */
    public record Member(String playerClass, int count)
    {
        public Member
        {
            count = Math.max(1, count);
        }
    }

    public SquadTemplate
    {
        members = List.copyOf(members);
        nameList = nameList == null ? "" : nameList;
        posture = posture == null ? Posture.BALANCED : posture;
    }

    /** @return the class of every member in order, the leader first */
    public List<String> roster()
    {
        List<String> roster = new ArrayList<>();
        for (Member member : members)
            for (int i = 0; i < member.count(); i++)
                roster.add(member.playerClass());
        return roster;
    }

    /** The template as tag data, as the editor screen sends it. */
    public CompoundTag toTag()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, id);
        tag.putString(NBT_NAME, name == null ? "" : name);
        tag.putString(NBT_POSTURE, posture.name());
        tag.putString(NBT_NAME_LIST, nameList);
        ListTag list = new ListTag();
        for (Member member : members)
        {
            CompoundTag entry = new CompoundTag();
            entry.putString(NBT_CLASS, member.playerClass());
            entry.putInt(NBT_COUNT, member.count());
            list.add(entry);
        }
        tag.put(NBT_MEMBERS, list);
        return tag;
    }

    /** @return the template a tag describes, or empty for one without an id */
    public static Optional<SquadTemplate> fromTag(CompoundTag tag)
    {
        String id = tag.getString(NBT_ID).toLowerCase(Locale.ROOT).strip();
        if (id.isEmpty() || !id.matches("[a-z0-9_\\-]+"))
            return Optional.empty();
        List<Member> members = new ArrayList<>();
        for (Tag entry : tag.getList(NBT_MEMBERS, Tag.TAG_COMPOUND))
        {
            CompoundTag member = (CompoundTag) entry;
            if (!member.getString(NBT_CLASS).isBlank())
                members.add(new Member(member.getString(NBT_CLASS), member.getInt(NBT_COUNT)));
        }
        return Optional.of(new SquadTemplate(id, tag.getString(NBT_NAME), BotSettings.parse(Posture.class, tag.getString(NBT_POSTURE), Posture.BALANCED), tag.getString(NBT_NAME_LIST), members));
    }

    public SquadTemplate withMembers(List<Member> replacement)
    {
        return new SquadTemplate(id, name, posture, nameList, replacement);
    }

    public SquadTemplate withPosture(Posture replacement)
    {
        return new SquadTemplate(id, name, replacement, nameList, members);
    }

    public SquadTemplate withNameList(String replacement)
    {
        return new SquadTemplate(id, name, posture, replacement, members);
    }
}
