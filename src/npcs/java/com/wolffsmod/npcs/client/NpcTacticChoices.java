package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcFlight;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Locale;
import java.util.Optional;

/** The tactics the editor offers an NPC: only those written for its kind. Client side. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcTacticChoices
{
    public static TacticKind kind(EntityNPCInterface npc)
    {
        return TacticKind.of(NpcKind.model(npc).map(FlanModelKind::of).orElse(null), NpcFlight.type(npc));
    }

    /** @return the index of the first tactic from the given one on, wrapping round, that is written for the kind */
    static int next(int from, TacticKind kind)
    {
        Tactic[] tactics = Tactic.values();
        for (int step = 0; step < tactics.length; step++)
        {
            int candidate = Math.floorMod(from + step, tactics.length);
            if (tactics[candidate].appliesTo(kind))
                return candidate;
        }
        return Tactic.CUSTOM_NPCS.ordinal();
    }

    /** @return the tactic of the mod's own that replaces Custom NPCs' tactical variant for the NPC, if one does */
    public static Optional<Tactic> replacing(EntityNPCInterface npc)
    {
        Tactic tactic = NpcTuning.of(npc).tactic();
        return tactic != Tactic.CUSTOM_NPCS && tactic.appliesTo(kind(npc)) ? Optional.of(tactic) : Optional.empty();
    }

    public static String key(Tactic tactic)
    {
        return "wolffsmodnpcs.tuning.tactic." + tactic.name().toLowerCase(Locale.ROOT);
    }
}
