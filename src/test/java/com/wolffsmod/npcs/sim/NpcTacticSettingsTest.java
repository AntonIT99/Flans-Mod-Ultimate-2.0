package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcTacticSettingsTest
{
    @Test
    void savesHoldTheIndexOfATacticSoTheExistingOnesKeepTheirPlaces()
    {
        assertEquals(
            List.of(Tactic.CUSTOM_NPCS, Tactic.ADVANCE, Tactic.HOLD, Tactic.COVER, Tactic.STAND_OFF, Tactic.BOUNDING, Tactic.FLANK, Tactic.CIRCLE, Tactic.STRAFING_RUN, Tactic.ORBIT, Tactic.STRAFE),
            List.of(Tactic.values()).subList(0, 11));
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.TACTIC, Tactic.HOLD.ordinal());
        tuning.set(Value.TACTIC_DISTANCE, 999);
        CompoundTag root = new CompoundTag();
        tuning.save(root);
        NpcTuning loaded = new NpcTuning();
        loaded.load(root);
        assertEquals(Tactic.HOLD, loaded.tactic());
        assertEquals(Value.TACTIC_DISTANCE.maximum(), loaded.get(Value.TACTIC_DISTANCE));
    }

    @Test
    void howAnNpcFliesDecidesItsKindBeforeItsModel()
    {
        assertEquals(TacticKind.STANDARD, TacticKind.of(null, Flight.NORMAL));
        assertEquals(TacticKind.PLANE, TacticKind.of(null, Flight.PLANE));
        assertEquals(TacticKind.HELICOPTER, TacticKind.of(null, Flight.HELICOPTER));
        assertEquals(TacticKind.VEHICLE, TacticKind.of(FlanModelKind.VEHICLE, Flight.NORMAL));
        assertEquals(TacticKind.AA_GUN, TacticKind.of(FlanModelKind.AA_GUN, Flight.NORMAL));
        assertEquals(TacticKind.MECHA, TacticKind.of(FlanModelKind.MECHA, Flight.NORMAL));
        assertEquals(TacticKind.PLANE, TacticKind.of(FlanModelKind.PLANE, Flight.NORMAL));
        assertEquals(TacticKind.HELICOPTER, TacticKind.of(FlanModelKind.PLANE, Flight.HELICOPTER));
    }

    @Test
    void everyKindOfNpcThatMovesInAFightHasTacticsOfItsOwn()
    {
        for (TacticKind kind : TacticKind.values())
            assertTrue(List.of(Tactic.values()).stream().filter(tactic -> tactic != Tactic.CUSTOM_NPCS).anyMatch(tactic -> tactic.appliesTo(kind)), kind.name());
        assertTrue(Tactic.STRAFING_RUN.flown() && Tactic.ORBIT.flown());
        assertFalse(Tactic.HOLD.usesDistance());
        assertTrue(Tactic.STAND_OFF.usesDistance());
    }

    @Test
    void aPlaneOnAStrafingRunCarriesOnPastItsTargetUntilItIsFarEnoughToComeRound()
    {
        assertFalse(NpcFlight.extending(false, NpcFlight.PASS_DISTANCE + 1D, 40D), "inbound, still short of the target");
        assertTrue(NpcFlight.extending(false, NpcFlight.PASS_DISTANCE - 1D, 40D), "it has passed over the target");
        assertTrue(NpcFlight.extending(true, 39D, 40D));
        assertFalse(NpcFlight.extending(true, 40D, 40D), "far enough beyond, it comes round");
    }
}
