package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.TacticKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcTacticChoicesTest
{
    @Test
    void theTacticButtonStepsOverTacticsNotWrittenForTheKindAndWrapsRound()
    {
        assertEquals(Tactic.ADVANCE.ordinal(), NpcTacticChoices.next(Tactic.ADVANCE.ordinal(), TacticKind.STANDARD));
        assertEquals(Tactic.STRAFING_RUN.ordinal(), NpcTacticChoices.next(Tactic.ADVANCE.ordinal(), TacticKind.PLANE));
        assertEquals(Tactic.CUSTOM_NPCS.ordinal(), NpcTacticChoices.next(Tactic.ORBIT.ordinal() + 1, TacticKind.PLANE), "after the last plane tactic it wraps round");
        assertEquals(Tactic.CIRCLE.ordinal(), NpcTacticChoices.next(Tactic.BOUNDING.ordinal(), TacticKind.VEHICLE));
        for (TacticKind kind : TacticKind.values())
            for (int from = 0; from < Tactic.values().length; from++)
                assertTrue(Tactic.values()[NpcTacticChoices.next(from, kind)].appliesTo(kind), kind + " from " + from);
    }
}
