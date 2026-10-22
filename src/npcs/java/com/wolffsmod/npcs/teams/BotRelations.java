package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;

/** Who fights whom, in any battle being fought: the bots' own judgement of friend and foe. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotRelations
{
    /** Whether both entities fight in the same battle, on sides hostile to each other. */
    static boolean areEnemies(Entity first, Entity second)
    {
        for (BotBattle battle : BotBattles.all())
        {
            if (battle.getGround().areEnemies(first, second))
                return true;
        }
        return false;
    }
}
