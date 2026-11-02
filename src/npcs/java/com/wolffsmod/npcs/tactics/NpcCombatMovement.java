package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.MovementFreedom;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.phys.Vec3;

/** Movement permissions shared by safety, flanking and vehicle positioning; existing explicit hold orders win. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCombatMovement
{
    public static boolean allowed(EntityNPCInterface npc)
    {
        return !npc.isNoAi() && npc.isAlive() && !npc.isKilled() && npc.ais.onAttack == 0 && NpcTuning.of(npc).tactic() != Tactic.HOLD
            && NpcCombatPolicy.value(npc, Value.COMBAT_MOVEMENT) != MovementFreedom.HOLD.ordinal();
    }

    public static Vec3 anchor(EntityNPCInterface npc)
    {
        if (npc.getOwner() != null)
            return npc.getOwner().position();
        if (npc.ais.returnToStart)
            return new Vec3(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
        NpcSafetyState state = NpcTacticState.of(npc).getSafety();
        if (state.getAnchor() == null)
            state.setAnchor(npc.position());
        return state.getAnchor();
    }

    public static boolean permits(EntityNPCInterface npc, Vec3 point)
    {
        if (!allowed(npc))
            return false;
        if (NpcCombatPolicy.value(npc, Value.COMBAT_MOVEMENT) == MovementFreedom.FREE.ordinal() && npc.getOwner() == null && !npc.ais.returnToStart)
            return true;
        double radius = NpcCombatPolicy.value(npc, Value.MANOEUVRE_RADIUS);
        return point.subtract(anchor(npc)).horizontalDistanceSqr() <= radius * radius;
    }
}
