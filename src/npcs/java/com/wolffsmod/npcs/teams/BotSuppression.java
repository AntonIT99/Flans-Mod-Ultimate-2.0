package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.entity.ISuppressionListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.*;

/**
 * Rounds passing close to a soldier bot suppress it, as near misses suppress players: it aims worse while suppressed, and
 * once pinned down it stops advancing until the fire lets up. Suppression wears off within a few seconds. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotSuppression implements ISuppressionListener
{
    static final BotSuppression INSTANCE = new BotSuppression();
    /** Suppression a near miss of full intensity adds. */
    private static final double PER_MISS = 1D;
    private static final double MOST = 3D;
    /** Suppression that wears off each tick. */
    private static final double DECAY = 0.05D;
    /** Suppression from which a bot is pinned down and stops advancing. */
    static final double PINNED = 1.5D;
    /** Share of its accuracy a bot keeps at the most suppression. */
    private static final double WORST_AIM = 0.5D;
    private static final double GONE = 0.05D;

    /**
     * How suppressed one bot is.
     *
     * @param accuracy
     *            the bot's own ranged accuracy, given back once the suppression wears off
     */
    private record State(double level, long tick, int accuracy)
    {
        double at(long now)
        {
            return Math.max(0D, level - DECAY * Math.max(0L, now - tick));
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    @Override
    public void suppressed(Mob target, @Nullable LivingEntity shooter, float intensity)
    {
        Optional<BotBattle> current = BotBattles.of(target);
        if (current.isEmpty() || !current.get().getSettings().isSuppression() || !(target instanceof EntityNPCInterface npc) || shooter != null && !hostile(target, shooter))
            return;
        BotSlot slot = BotBattles.slotOf(current.get(), target).orElse(null);
        if (slot == null || slot.isVehicle())
            return;
        long now = target.level().getGameTime();
        DataRanged ranged = npc.stats.ranged;
        State before = STATES.get(target.getUUID());
        int accuracy = before == null ? ranged.getAccuracy() : before.accuracy();
        double level = Math.min(MOST, (before == null ? 0D : before.at(now)) + PER_MISS * intensity);
        STATES.put(target.getUUID(), new State(level, now, accuracy));
        aim(npc, accuracy, level);
    }

    /** A bot is only suppressed by the enemy; its own side's fire passing by does not count. */
    private static boolean hostile(Entity target, LivingEntity shooter)
    {
        return BotRelations.areEnemies(target, shooter);
    }

    private static void aim(EntityNPCInterface npc, int accuracy, double level)
    {
        double kept = 1D - (1D - WORST_AIM) * Math.min(1D, level / MOST);
        npc.stats.ranged.setAccuracy((int) Math.round(accuracy * kept));
    }

    /** Takes a bot's newly trained accuracy as the one it gets back once suppression wears off, and aims worse from it meanwhile. */
    static void rebase(EntityNPCInterface npc, int accuracy)
    {
        State state = STATES.get(npc.getUUID());
        if (state == null)
            return;
        STATES.put(npc.getUUID(), new State(state.level(), state.tick(), accuracy));
        aim(npc, accuracy, state.at(npc.level().getGameTime()));
    }

    /** Whether a bot is pinned down by fire and holds where it is. */
    static boolean isPinned(Entity npc)
    {
        State state = STATES.get(npc.getUUID());
        return state != null && state.at(npc.level().getGameTime()) >= PINNED;
    }

    /** Lets suppression wear off: aim comes back as it fades, and bots that died or left are forgotten. */
    static void tick(ServerLevel level)
    {
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, State>> entries = STATES.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<UUID, State> entry = entries.next();
            State state = entry.getValue();
            double current = state.at(now);
            Entity entity = level.getEntity(entry.getKey());
            boolean present = entity instanceof EntityNPCInterface npc && npc.isAlive();
            if (present)
                aim((EntityNPCInterface) entity, state.accuracy(), current);
            if (!present || current <= GONE)
                entries.remove();
        }
    }

    /** Gives every suppressed bot its aim back and forgets them, as a round ends or the server stops, so no bot is saved aiming worse. */
    static void forget(MinecraftServer server)
    {
        for (Map.Entry<UUID, State> entry : STATES.entrySet())
        {
            for (ServerLevel level : server.getAllLevels())
            {
                if (level.getEntity(entry.getKey()) instanceof EntityNPCInterface npc)
                    npc.stats.ranged.setAccuracy(entry.getValue().accuracy());
            }
        }
        STATES.clear();
    }
}
