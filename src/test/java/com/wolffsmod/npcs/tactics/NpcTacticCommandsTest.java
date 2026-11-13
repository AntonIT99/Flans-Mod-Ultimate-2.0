package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcTacticCommandsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static EntityNPCInterface npc(NpcTuning tuning)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        npc.stats = stats;
        return npc;
    }

    @Test
    void onlyNpcsATacticIsWrittenForTakeItAndTheOthersKeepTheirOwn()
    {
        NpcTuning soldier = new NpcTuning();
        NpcTuning plane = new NpcTuning();
        plane.set(Value.TACTIC, Tactic.ORBIT.ordinal());
        EntityNPCInterface infantry = npc(soldier);
        EntityNPCInterface aircraft = npc(plane);
        Map<EntityNPCInterface, TacticKind> kinds = Map.of(infantry, TacticKind.STANDARD, aircraft, TacticKind.PLANE);

        NpcTacticCommands.Outcome outcome = NpcTacticCommands.apply(List.of(infantry, aircraft), Tactic.CHARGE, null, kinds::get);

        assertEquals(new NpcTacticCommands.Outcome(1, 1), outcome);
        assertEquals(Tactic.CHARGE, soldier.tactic());
        assertEquals(Tactic.ORBIT, plane.tactic());
    }

    @Test
    void aGivenDistanceIsSetWithTheTacticAndResetReachesEveryKind()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.TACTIC_DISTANCE, 40);
        EntityNPCInterface npc = npc(tuning);

        NpcTacticCommands.apply(List.of(npc), Tactic.STAND_OFF, 24, unused -> TacticKind.STANDARD);
        assertEquals(Tactic.STAND_OFF, tuning.tactic());
        assertEquals(24, tuning.get(Value.TACTIC_DISTANCE));

        NpcTacticCommands.apply(List.of(npc), Tactic.CUSTOM_NPCS, null, unused -> TacticKind.HELICOPTER);
        assertEquals(Tactic.CUSTOM_NPCS, tuning.tactic());
        assertEquals(24, tuning.get(Value.TACTIC_DISTANCE));
    }

    @Test
    void everyTacticHasItsOwnWordAndNoneCollidesWithReset()
    {
        List<String> words = Arrays.stream(Tactic.values()).map(NpcTacticCommands::word).toList();
        assertEquals(words.size(), words.stream().distinct().count());
        assertFalse(words.contains("reset"));
        assertTrue(words.contains("hold_line"));
    }
}
