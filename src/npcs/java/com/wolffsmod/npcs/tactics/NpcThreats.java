package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.entity.ISuppressionListener;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Enemies shooting at an NPC, wherever they shoot from. Whoever hits an NPC that fights back, or fires a Flan round close
 * past it, becomes its threat for as many seconds as its threat memory says: the NPC keeps that enemy as its target
 * however far beyond its aggro range, and turns on the one firing past it when it has nobody else to fight. Allied NPCs
 * within its alert radius learn of the enemy too, whether or not the NPC fights back itself. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcThreats implements ISuppressionListener
{
    public static final NpcThreats INSTANCE = new NpcThreats();
    /** Ticks between two calls of one NPC to its allies, so that automatic fire does not look for them with every round. */
    static final int ALERT_INTERVAL = 20;
    private static final int TICKS_PER_SECOND = 20;
    /** Custom NPCs' response to an attack of an NPC that fights back. */
    private static final int RETALIATE = 0;

    @Override
    public void suppressed(Mob target, @Nullable LivingEntity shooter, float intensity)
    {
        if (!(target instanceof EntityNPCInterface npc) || shooter == null || npc.isKilled() || !valid(npc, shooter) || !NpcDrivers.hostile(npc, shooter))
            return;
        long now = npc.level().getGameTime();
        if (remembersThreats(npc))
        {
            remember(npc, shooter, now);
            // Custom NPCs answers a hit itself; a round that misses is answered here, unless the NPC is busy with someone else.
            npc.onAttack(shooter);
        }
        alert(npc, shooter, now);
    }

    /** The NPC was hurt by the attacker, which Custom NPCs has already made its target unless it fights someone nearer. */
    public static void hit(EntityNPCInterface npc, LivingEntity attacker)
    {
        if (npc.isKilled() || !valid(npc, attacker))
            return;
        long now = npc.level().getGameTime();
        if (remembersThreats(npc))
            remember(npc, attacker, now);
        alert(npc, attacker, now);
    }

    /** @return whether the NPC keeps the entity as its target because it remembers it shooting at it, wherever it shoots from */
    public static boolean keeps(EntityNPCInterface npc, LivingEntity target)
    {
        double sight = NpcSight.range();
        return remembersThreats(npc) && NpcTacticState.of(npc).remembers(target, npc.level().getGameTime()) && valid(npc, target) && npc.distanceToSqr(target) <= sight * sight;
    }

    /** @return the enemy the NPC remembers shooting at it, or null */
    @Nullable
    public static LivingEntity threat(EntityNPCInterface npc)
    {
        return remembersThreats(npc) ? NpcTacticState.of(npc).threat(npc.level().getGameTime()) : null;
    }

    /** @return whether the NPC fights back and remembers who shoots at it at all */
    static boolean remembersThreats(EntityNPCInterface npc)
    {
        return !npc.isKilled() && npc.ais.onAttack == RETALIATE && NpcTuning.of(npc).get(Value.THREAT_MEMORY) > 0;
    }

    /** @return whether the NPC may fight the entity at all: alive, in its level, not itself or its owner, and no player that cannot be hurt */
    static boolean valid(EntityNPCInterface npc, LivingEntity enemy)
    {
        if (enemy == npc || !enemy.isAlive() || enemy.level() != npc.level() || enemy == npc.getOwner())
            return false;
        return !(enemy instanceof Player player) || !player.getAbilities().invulnerable && !player.isSpectator();
    }

    private static void remember(EntityNPCInterface npc, LivingEntity enemy, long now)
    {
        NpcTacticState.of(npc).threat(enemy, npc.getTarget(), now, now + (long) NpcTuning.of(npc).get(Value.THREAT_MEMORY) * TICKS_PER_SECOND);
    }

    /** Tells the allies of the NPC, its own faction within its alert radius, who is shooting at it. */
    private static void alert(EntityNPCInterface npc, LivingEntity enemy, long now)
    {
        int radius = NpcTuning.of(npc).get(Value.ALERT_RADIUS);
        if (radius <= 0 || !NpcTacticState.of(npc).alert(now, ALERT_INTERVAL))
            return;
        double reach = (double) radius * radius;
        for (EntityNPCInterface ally : npc.level().getEntitiesOfClass(EntityNPCInterface.class, npc.getBoundingBox().inflate(radius),
            other -> other != npc && !other.isKilled() && other.faction.id == npc.faction.id && other.distanceToSqr(npc) <= reach))
            answer(ally, enemy, now);
    }

    /**
     * An ally that fights the enemy remembers it, and takes it on unless it is busy with someone else: one fighting a target
     * it cannot reach turns on the enemy instead when it can reach that, as a marksman or a vehicle can.
     */
    private static void answer(EntityNPCInterface ally, LivingEntity enemy, long now)
    {
        if (ally.ais.onAttack != RETALIATE || !valid(ally, enemy) || !NpcDrivers.hostile(ally, enemy))
            return;
        if (remembersThreats(ally))
            remember(ally, enemy, now);
        LivingEntity current = ally.getTarget();
        if (current == null)
            ally.onAttack(enemy);
        else if (current != enemy && !reaches(ally, current) && reaches(ally, enemy))
            ally.setTarget(enemy);
    }

    private static boolean reaches(EntityNPCInterface npc, LivingEntity target)
    {
        double firing = NpcWeaponRange.firing(npc);
        return npc.distanceToSqr(target) <= firing * firing;
    }
}
