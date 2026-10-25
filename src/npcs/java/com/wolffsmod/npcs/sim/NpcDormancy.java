package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.player.Player;

/**
 * Lets an NPC with a dormant range rest while no player is near: beyond that distance from every player it looks for no
 * targets and lets the one it has go, so a garrison nobody is watching costs the server little. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcDormancy
{
    /** @return whether the NPC has a dormant range and no player, spectators aside, is within it */
    public static boolean dormant(EntityNPCInterface npc)
    {
        int range = NpcTuning.of(npc).get(Value.DORMANT_RANGE);
        if (range <= 0 || npc.level().isClientSide)
            return false;
        double reach = (double) range * range;
        for (Player player : npc.level().players())
        {
            if (!player.isSpectator() && player.distanceToSqr(npc) <= reach)
                return false;
        }
        return true;
    }

    /** Lets a dormant NPC's target go. Called now and then from the NPC's tick. */
    public static void rest(EntityNPCInterface npc)
    {
        if (npc.getTarget() != null && dormant(npc))
            npc.setTarget(null);
    }
}
