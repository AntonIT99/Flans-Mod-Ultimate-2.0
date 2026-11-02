package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcSafetyState.Action;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared order-aware infantry advance and covering fire, independent of battle membership. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCombatAdvance
{
    /** Explicit tactics and Custom NPCs' specialist variants keep their movement decisions. */
    public static boolean enabled(EntityNPCInterface npc)
    {
        return NpcEquipment.enabled(npc, Feature.COMBAT_ADVANCE) && NpcEquipmentRangedGoal.engages(npc) && NpcTuning.of(npc).tactic() == Tactic.CUSTOM_NPCS
            && CustomNpcsPlatform.tacticalVariant(npc) == 0;
    }

    /** Orders remain useful even while a target has interrupted ordinary objective or following goals. */
    @Nullable
    public static NpcCombatIntent assigned(EntityNPCInterface npc)
    {
        NpcCombatIntent order = TeamsBots.combatIntent(npc).orElse(null);
        if (order != null)
            return order;
        LivingEntity owner = npc.getOwner();
        if (owner != null && owner.isAlive() && owner.level() == npc.level())
            return new NpcCombatIntent(owner.position(), 5D);
        return null;
    }

    /** Without an order, close to the weapon's preferred distance using only a visible or remembered enemy position. */
    @Nullable
    public static NpcCombatIntent intent(EntityNPCInterface npc, @Nullable Vec3 knownEnemy)
    {
        NpcCombatIntent assigned = assigned(npc);
        if (assigned != null)
            return assigned;
        if (knownEnemy == null)
            return null;
        double preferred = NpcTactics.engagement(npc).preferred();
        return new NpcCombatIntent(knownEnemy, preferred, knownEnemy, preferred, false);
    }

    /** A nearby ally already crossing gets covering fire; nobody waits for a shooter who cannot actually engage. */
    public static boolean coversAlly(EntityNPCInterface npc, LivingEntity target, long now)
    {
        if (!NpcEquipment.enabled(npc, Feature.COVERING_FIRE) || !(npc.level() instanceof ServerLevel level) || !npc.getSensing().hasLineOfSight(target)
            || !npc.isInRange(target, NpcWeaponRange.firing(npc)) || NpcTacticState.of(npc).isReloading(now))
            return false;
        var coordinated = TeamsBots.coveringFire(npc, now);
        if (coordinated.isPresent())
            return coordinated.get();
        NpcCombatants combatants = NpcCombatants.of(level);
        for (int index = 0; index < combatants.npcSize(); index++)
        {
            EntityNPCInterface ally = combatants.npc(index).npc();
            if (ally == null || ally == npc || !ally.isAlive() || ally.isKilled() || ally.isPassenger() || ally.getTarget() != target || ally.distanceToSqr(npc) > 256D || !NpcDrivers.ally(npc, ally))
                continue;
            NpcSafetyState movement = NpcTacticState.of(ally).getSafety();
            if (movement.active(now) && movement.getAction() == Action.ADVANCE)
                return true;
        }
        return false;
    }

    public static int firePause(EntityNPCInterface npc)
    {
        return NpcCombatPolicy.value(npc, Value.ADVANCE_FIRE_TICKS);
    }

    /** The equipped weapon reloads during a dash but waits until the soldier stops to aim and fire. */
    public static boolean holdsFire(EntityNPCInterface npc)
    {
        if (!(npc instanceof NpcTacticState.Holder holder))
            return false;
        NpcSafetyState state = holder.wolffsmodnpcsTacticState().getSafety();
        return state.active(npc.level().getGameTime()) && state.getAction() == Action.ADVANCE;
    }
}
