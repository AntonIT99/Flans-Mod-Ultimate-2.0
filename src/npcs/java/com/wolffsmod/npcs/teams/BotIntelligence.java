package com.wolffsmod.npcs.teams;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Bounded, transient reports owned by one battle. Hidden contacts keep their last reported position. Server thread. */
final class BotIntelligence
{
    static final long MEMORY_TICKS = 600L;
    static final int MAX_CONTACTS_PER_SIDE = 128;

    record Contact(UUID enemy, Vec3 position, boolean armoured, long observedAt, boolean airborne, boolean intercept)
    {}

    private final Map<Integer, Map<UUID, Contact>> sides = new HashMap<>();

    void report(int side, UUID enemy, Vec3 position, boolean armoured, long now)
    {
        report(side, enemy, position, armoured, now, false, false);
    }

    void report(int side, UUID enemy, Vec3 position, boolean armoured, long now, boolean airborne, boolean intercept)
    {
        if (side < 0)
            return;
        Map<UUID, Contact> contacts = sides.computeIfAbsent(side, ignored -> new LinkedHashMap<>());
        Contact previous = contacts.get(enemy);
        if (previous != null && previous.observedAt() > now)
            return;
        contacts.remove(enemy);
        contacts.put(enemy, new Contact(enemy, position, armoured, now, airborne, intercept));
        if (contacts.size() > MAX_CONTACTS_PER_SIDE)
            contacts.remove(contacts.keySet().iterator().next());
    }

    List<Contact> contacts(int side, long now)
    {
        Map<UUID, Contact> contacts = sides.get(side);
        if (contacts == null)
            return List.of();
        contacts.values().removeIf(contact -> now - contact.observedAt() >= MEMORY_TICKS || contact.observedAt() > now);
        return List.copyOf(contacts.values());
    }

    void clear()
    {
        sides.clear();
    }
}
