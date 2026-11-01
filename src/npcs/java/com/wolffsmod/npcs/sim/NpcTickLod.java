package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * How often an NPC decides what to do. Near a player it decides every tick; farther out, where a tick of stale steering
 * cannot be told apart, it decides every second or every fourth tick instead, and the NPCs of a battle are spread over
 * those ticks so that no tick carries all of them. Goal and target decisions are what a battle of hundreds of soldiers
 * spends its tick time on.
 *
 * <p>
 * Only the decisions are spaced out: the senses, the goal and target selectors, the navigator and the movement controls.
 * Everything else still runs every tick, so a soldier keeps walking where it was steering, keeps falling, keeps taking
 * damage and keeps being drawn exactly as before. What a skipped tick does postpone is anything a goal does on its own
 * tick, a shot among it, so beyond the near distance a shot can be up to one period late. Both sides of a fight are
 * spaced out alike, and the near distance is a good deal farther than infantry fight at, so what a player watches is
 * untouched.
 * </p>
 *
 * <p>
 * An NPC in a seat, and one drawn as a vehicle, mecha, aircraft or AA gun, always decides every tick: there a stale tick
 * of steering drives a machine off its line.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcTickLod
{
    /** @return whether the NPC works out its decisions this tick */
    public static boolean decides(Mob mob)
    {
        if (!(mob instanceof EntityNPCInterface npc) || mob.level().isClientSide || !NpcsCommonConfig.aiTickLod())
            return true;
        int period = period(npc);
        // Spread by identity, so a formation does not decide in lockstep and leave every other tick empty.
        return period <= 1 || Math.floorMod(npc.tickCount + npc.getId(), period) == 0;
    }

    /** @return ticks between the NPC's decisions */
    private static int period(EntityNPCInterface npc)
    {
        if (npc.isPassenger() || TacticKind.of(npc) != TacticKind.STANDARD)
            return 1;
        double near = NpcsCommonConfig.aiTickLodNearDistance();
        double far = Math.max(near, NpcsCommonConfig.aiTickLodFarDistance());
        double nearest = nearestPlayerSquared(npc);
        if (nearest <= near * near)
            return 1;
        return nearest <= far * far ? NpcsCommonConfig.aiTickLodMidPeriod() : NpcsCommonConfig.aiTickLodFarPeriod();
    }

    /** @return squared blocks to the nearest player who is no spectator, or {@link Double#MAX_VALUE} while there is none */
    private static double nearestPlayerSquared(EntityNPCInterface npc)
    {
        double nearest = Double.MAX_VALUE;
        for (Player player : npc.level().players())
        {
            if (!player.isSpectator())
                nearest = Math.min(nearest, player.distanceToSqr(npc));
        }
        return nearest;
    }
}
