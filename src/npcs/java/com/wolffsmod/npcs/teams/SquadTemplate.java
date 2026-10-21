package com.wolffsmod.npcs.teams;

import java.util.ArrayList;
import java.util.List;

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
