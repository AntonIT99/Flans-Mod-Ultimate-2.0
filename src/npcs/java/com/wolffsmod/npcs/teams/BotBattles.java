package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;

import java.util.*;

/**
 * The battles bots fight now, by the instance their bots are tagged with, such as the playing of the Teams round being
 * played. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotBattles
{
    private static final Map<UUID, BotBattle> BATTLES = new LinkedHashMap<>();

    static void add(BotBattle battle)
    {
        BATTLES.put(battle.getInstance(), battle);
    }

    static void remove(BotBattle battle)
    {
        BATTLES.remove(battle.getInstance());
    }

    static Optional<BotBattle> get(UUID instance)
    {
        return Optional.ofNullable(BATTLES.get(instance));
    }

    /** @return every battle fought now, in the order they began */
    static Collection<BotBattle> all()
    {
        return Collections.unmodifiableCollection(BATTLES.values());
    }

    static boolean isEmpty()
    {
        return BATTLES.isEmpty();
    }

    /** @return the battle in which an entity is the bot of a slot, or empty for anyone else, crews of vehicle bots included */
    static Optional<BotBattle> of(Entity entity)
    {
        for (BotBattle battle : BATTLES.values())
        {
            if (battle.getSlotOfEntity().containsKey(entity.getUUID()))
                return Optional.of(battle);
        }
        return Optional.empty();
    }

    /** @return the slot an entity is the bot of in its battle, or empty for anyone else */
    static Optional<BotSlot> slotOf(BotBattle battle, Entity entity)
    {
        Integer slotId = battle.getSlotOfEntity().get(entity.getUUID());
        return Optional.ofNullable(slotId == null ? null : battle.getSlots().get(slotId));
    }
}
