package com.wolffsmod.npcs.teams;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Bounded, transient reports owned by one battle. Hidden contacts keep their last reported position. Server thread. */
final class BotIntelligence
{
    static final long MEMORY_TICKS = 600L;
    static final int MAX_CONTACTS_PER_SIDE = 128;
    static final long INTERCEPT_TICKS = 100L;

    record Contact(UUID enemy, Vec3 position, boolean armoured, long observedAt, boolean airborne, boolean intercept)
    {}

    private record Report(Contact contact, long interceptUntil)
    {}

    private final Map<Integer, Map<UUID, Report>> sides = new HashMap<>();

    void report(int side, UUID enemy, Vec3 position, boolean armoured, long now)
    {
        report(side, enemy, position, armoured, now, false, false);
    }

    void report(int side, UUID enemy, Vec3 position, boolean armoured, long now, boolean airborne, boolean intercept)
    {
        if (side < 0)
            return;
        Map<UUID, Report> contacts = sides.computeIfAbsent(side, ignored -> new LinkedHashMap<>());
        Report previous = contacts.get(enemy);
        if (previous != null && previous.contact().observedAt() > now)
            return;
        contacts.remove(enemy);
        long interceptUntil = previous == null ? 0L : previous.interceptUntil();
        if (!airborne)
            interceptUntil = 0L;
        else if (intercept)
            interceptUntil = now + INTERCEPT_TICKS;
        contacts.put(enemy, new Report(new Contact(enemy, position, armoured, now, airborne, intercept), interceptUntil));
        if (contacts.size() > MAX_CONTACTS_PER_SIDE)
            contacts.remove(contacts.keySet().iterator().next());
    }

    List<Contact> contacts(int side, long now)
    {
        Map<UUID, Report> contacts = sides.get(side);
        if (contacts == null)
            return List.of();
        contacts.values().removeIf(report -> now - report.contact().observedAt() >= MEMORY_TICKS || report.contact().observedAt() > now);
        return contacts.values().stream().map(report ->
        {
            Contact contact = report.contact();
            return new Contact(contact.enemy(), contact.position(), contact.armoured(), contact.observedAt(), contact.airborne(), now < report.interceptUntil());
        }).toList();
    }

    void clear()
    {
        sides.clear();
    }

    boolean intercept(int side, UUID enemy, long now)
    {
        Map<UUID, Report> contacts = sides.get(side);
        Report report = contacts == null ? null : contacts.get(enemy);
        return report != null && report.contact().airborne() && now >= report.contact().observedAt() && now < report.interceptUntil();
    }
}
