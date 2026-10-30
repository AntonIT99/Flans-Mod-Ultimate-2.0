package com.wolffsmod.npcs.sim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * How long a navigation pursuing an entity keeps its path before it searches again, as Efficient path searches has it,
 * instead of searching on every call as Custom NPCs' AI asks it to. One per navigation; server thread.
 */
public final class NpcPursuit
{
    private long next = Long.MIN_VALUE;
    private int pursued = -1;

    /**
     * @param following
     *            whether the navigation still follows a path
     * @return whether the navigation keeps the path it follows rather than searching for the entity now; a new pursuit starts
     *         at a moment of the mob's own, and afterwards it keeps the interval
     */
    public boolean keeps(Mob mob, Entity entity, long now, boolean following)
    {
        int interval = NpcMobility.pursuitInterval(mob);
        if (interval <= 1)
            return false;
        if (entity.getId() == pursued && now < next && following)
            return true;
        next = now + interval + (entity.getId() == pursued ? 0 : Math.floorMod(mob.getId(), interval));
        pursued = entity.getId();
        return false;
    }
}
