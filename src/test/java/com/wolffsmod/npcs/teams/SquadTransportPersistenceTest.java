package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SquadTransportPersistenceTest
{
    @Test
    void restartPreservesAssignmentIdentityAndPhaseButRebuildsSeatLeases()
    {
        UUID instance = UUID.randomUUID();
        BotBattle before = new BotBattle(instance, new RoundBattleground(null), new BotSettings(), Map.of());
        BotSquad squad = before.newSquad("red", BotRole.INFANTRY, BotSlot.Source.CARGO, 0, "section");
        UUID soldier = UUID.randomUUID();
        BotSlot slot = before.newSlot("red", BotSlot.Source.CARGO, 0);
        slot.setEntity(soldier);
        slot.setSquad(squad.getId());
        slot.setKills(4);
        squad.getSlots().add(slot.getId());
        SquadTransport assignment = new SquadTransport(UUID.randomUUID(), UUID.randomUUID(), List.of(soldier), new Vec3(12D, 64D, -8D), 100L);
        assignment.setPhase(SquadTransport.Phase.UNLOADING);
        assignment.getReservations().put(soldier, 7);
        squad.setTransport(assignment);
        CompoundTag saved = new CompoundTag();
        BotBattleCodec.write(before, saved);
        BotBattle after = new BotBattle(instance, new RoundBattleground(null), new BotSettings(), Map.of());
        BotBattleCodec.restore(after, saved, 1000L);
        SquadTransport restored = after.getSquads().get(squad.getId()).getTransport();
        assertNotNull(restored);
        assertEquals(assignment.getVehicle(), restored.getVehicle());
        assertEquals(assignment.getPlayer(), restored.getPlayer());
        assertEquals(Set.of(soldier), restored.getSoldiers());
        assertEquals(SquadTransport.Phase.UNLOADING, restored.getPhase());
        assertEquals(assignment.getLastPosition(), restored.getLastPosition());
        assertTrue(restored.getLastSeen() >= 1000L + BotBattleCodec.RESTORE_GRACE);
        assertTrue(restored.getReservations().isEmpty());
        assertEquals(soldier, after.getSlots().get(slot.getId()).getEntity());
        assertEquals(4, after.getSlots().get(slot.getId()).getKills());
    }

    @Test
    void oldSavesAndMalformedAssignmentsHaveNoTransport()
    {
        assertTrue(SquadTransport.load(new CompoundTag()).isEmpty());
        CompoundTag tag = new CompoundTag();
        tag.putUUID("vehicle", UUID.randomUUID());
        tag.putUUID("player", UUID.randomUUID());
        CompoundTag soldiers = new CompoundTag();
        soldiers.putBoolean("broken-uuid", true);
        tag.put("soldiers", soldiers);
        assertTrue(SquadTransport.load(tag).isEmpty());
        soldiers.putBoolean(UUID.randomUUID().toString(), true);
        assertTrue(SquadTransport.load(tag).isPresent());
        tag.putDouble("x", Double.NaN);
        assertTrue(SquadTransport.load(tag).isEmpty());
    }
}
