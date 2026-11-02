package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Resolves optional battle overrides without changing a clone's saved per-NPC behaviour. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCombatPolicy
{
    private static final AtomicReference<Function<EntityNPCInterface, NpcTacticalProfile>> OVERRIDE = new AtomicReference<>(npc -> NpcTacticalProfile.INHERIT);

    public static void overrides(Function<EntityNPCInterface, NpcTacticalProfile> source)
    {
        OVERRIDE.set(source);
    }

    public static boolean feature(EntityNPCInterface npc, Feature feature, boolean own)
    {
        if (NpcTacticalProfile.BALANCED.feature(feature) == null)
            return own;
        Boolean chosen = profile(npc).feature(feature);
        return chosen == null ? own : chosen;
    }

    public static int value(EntityNPCInterface npc, Value value)
    {
        Integer chosen = profile(npc).value(value);
        return chosen == null ? NpcTuning.of(npc).get(value) : chosen;
    }

    private static NpcTacticalProfile profile(EntityNPCInterface npc)
    {
        if (!(npc instanceof NpcTacticState.Holder holder))
            return OVERRIDE.get().apply(npc);
        NpcSafetyState state = holder.wolffsmodnpcsTacticState().getSafety();
        long now = npc.level().getGameTime();
        if (state.getPolicyAt() != now)
        {
            state.setPolicyAt(now);
            state.setPolicy(OVERRIDE.get().apply(npc));
        }
        return state.getPolicy();
    }
}
