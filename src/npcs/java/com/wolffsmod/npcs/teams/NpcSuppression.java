package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.entity.ISuppressionListener;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.TacticKind;
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
 * Rounds passing close to a soldier suppress it, as near misses suppress players: it aims worse while suppressed, and once
 * pinned down it stops advancing until the fire lets up. Suppression wears off within a few seconds. A soldier bot is
 * suppressed as its battle's settings say, and any other NPC on foot while its own switch is on; only the fire of its
 * enemies counts. A suppressed NPC is saved with the aim it had before. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSuppression implements ISuppressionListener
{
    public static final NpcSuppression INSTANCE = new NpcSuppression();
    /** Suppression a near miss of full intensity adds. */
    private static final double PER_MISS = 1D;
    private static final double MOST = 3D;
    /** Suppression that wears off each tick. */
    private static final double DECAY = 0.05D;
    /** Suppression from which a soldier is pinned down and stops advancing. */
    static final double PINNED = 1.5D;
    /** Share of its accuracy a soldier keeps at the most suppression. */
    private static final double WORST_AIM = 0.5D;
    private static final double GONE = 0.05D;
    /** Ticks between two passes that give suppressed soldiers their aim back as the suppression fades. */
    private static final int FADE_INTERVAL = 20;

    /**
     * How suppressed one soldier is.
     *
     * @param accuracy
     *            the soldier's own ranged accuracy, given back once the suppression wears off
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
        if (!(target instanceof EntityNPCInterface npc) || !suppressible(npc) || shooter != null && !hostile(npc, shooter))
            return;
        long now = target.level().getGameTime();
        DataRanged ranged = npc.stats.ranged;
        State before = STATES.get(target.getUUID());
        int accuracy = before == null ? ranged.getAccuracy() : before.accuracy();
        double level = Math.min(MOST, (before == null ? 0D : before.at(now)) + PER_MISS * intensity);
        STATES.put(target.getUUID(), new State(level, now, accuracy));
        aim(npc, accuracy, level);
    }

    /** Whether near misses suppress the NPC: a soldier bot as its battle's settings say, any other NPC on foot as its own switch says. */
    static boolean suppressible(EntityNPCInterface npc)
    {
        return BotBattles.of(npc).map(battle -> battle.getSettings().isSuppression() && BotBattles.slotOf(battle, npc).map(slot -> !slot.isVehicle()).orElse(false))
            .orElseGet(() -> !BotTag.isBot(npc) && NpcEquipment.enabled(npc, Feature.SUPPRESSIBLE) && TacticKind.of(npc) == TacticKind.STANDARD);
    }

    /** Only the enemy's fire suppresses: in a battle, that of the other sides; otherwise that of whoever the NPC is hostile to. */
    private static boolean hostile(EntityNPCInterface npc, LivingEntity shooter)
    {
        return BotBattles.of(npc).isPresent() ? BotRelations.areEnemies(npc, shooter) : NpcDrivers.hostile(npc, shooter);
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

    /** Whether a soldier is pinned down by fire and holds where it is rather than advance. */
    public static boolean isPinned(Entity npc)
    {
        State state = STATES.get(npc.getUUID());
        return state != null && state.at(npc.level().getGameTime()) >= PINNED;
    }

    /** @return the accuracy a suppressed NPC gets back once the suppression wears off, which is the one it is saved with; empty while it is not suppressed */
    public static OptionalInt accuracyBefore(Entity npc)
    {
        State state = STATES.get(npc.getUUID());
        return state == null ? OptionalInt.empty() : OptionalInt.of(state.accuracy());
    }

    /** Lets suppression wear off every second: aim comes back as it fades, and soldiers that died or left are forgotten. */
    public static void serverTick(MinecraftServer server)
    {
        if (STATES.isEmpty() || server.getTickCount() % FADE_INTERVAL != 0)
            return;
        Iterator<Map.Entry<UUID, State>> entries = STATES.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<UUID, State> entry = entries.next();
            EntityNPCInterface npc = find(server, entry.getKey());
            State state = entry.getValue();
            double current = npc == null ? 0D : state.at(npc.level().getGameTime());
            if (npc != null && npc.isAlive())
                aim(npc, state.accuracy(), current);
            if (npc == null || !npc.isAlive() || current <= GONE)
                entries.remove();
        }
    }

    @Nullable
    private static EntityNPCInterface find(MinecraftServer server, UUID id)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            if (level.getEntity(id) instanceof EntityNPCInterface npc)
                return npc;
        }
        return null;
    }

    /** Gives every suppressed soldier its aim back and forgets them, as a round ends or the server stops, so no bot is saved aiming worse. */
    static void forget(MinecraftServer server)
    {
        for (Map.Entry<UUID, State> entry : STATES.entrySet())
        {
            EntityNPCInterface npc = find(server, entry.getKey());
            if (npc != null)
                npc.stats.ranged.setAccuracy(entry.getValue().accuracy());
        }
        STATES.clear();
    }
}
