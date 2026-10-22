package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotRoundStoreTest
{
    private static final long NOW = 50_000L;

    /** A battle of a round that is only its instance, which is all a stored battle is matched by. */
    private static BotBattle battle(UUID instance)
    {
        return new BotBattle(instance, new RoundBattleground(null), new BotSettings(), Map.of());
    }

    private static BotBattle played(UUID instance)
    {
        BotBattle round = battle(instance);
        round.setOpening(false);
        BotSquad squad = round.newSquad("Germans", BotRole.INFANTRY, BotSlot.Source.SQUAD, 0, "panzergrenadiere");
        squad.setTask(new SquadTask(SquadTask.Kind.ATTACK, new Vec3(10D, 64D, -20D), 8D));
        squad.setCargo("fallschirmjaeger");
        BotSlot slot = round.newSlot("Germans", BotSlot.Source.SQUAD, 0);
        slot.setSquad(squad.getId());
        slot.setName("Hans Mueller");
        slot.setRole("rifleman");
        slot.setEntity(UUID.randomUUID());
        slot.setLastPosition(new Vec3(1D, 65D, 2D));
        slot.setLastSeen(NOW - 10L);
        slot.setKills(7);
        slot.setDeaths(3);
        slot.setSpawns(4);
        squad.getSlots().add(slot.getId());
        round.getTickets().put(1, 42);
        round.getInfantrySpawns().put("Germans", 12);
        round.increment(round.getVehicleSpawns(), "Germans", 2);
        round.getCommanders().put(1, UUID.randomUUID());
        round.getSidesThatFought().add(1);
        round.getFieldedPlayers().add(UUID.randomUUID());
        return round;
    }

    @Test
    void aResumedPlayingGetsBackItsBotsSquadsAndSpending()
    {
        UUID instance = UUID.randomUUID();
        BotBattle before = played(instance);
        CompoundTag saved = new CompoundTag();
        BotBattleCodec.write(before, saved);

        BotRoundStore store = BotRoundStore.load(saved, ItemStackData.builtInRegistries());
        BotBattle after = battle(instance);
        assertTrue(store.resume(after, NOW));

        BotSlot slot = after.getSlots().values().iterator().next();
        BotSlot original = before.getSlots().values().iterator().next();
        assertEquals(original.getName(), slot.getName());
        assertEquals(original.getEntity(), slot.getEntity());
        assertEquals(7, slot.getKills());
        assertEquals(3, slot.getDeaths());
        assertEquals(4, slot.getSpawns());
        assertEquals(original.getLastPosition(), slot.getLastPosition());
        assertTrue(slot.getLastSeen() >= NOW + BotBattleCodec.RESTORE_GRACE, "a restored bot gets time to be loaded before it counts as lost");
        assertEquals(slot.getId(), after.getSlotOfEntity().get(slot.getEntity()));
        BotSquad squad = after.getSquads().values().iterator().next();
        assertEquals("panzergrenadiere", squad.getTemplate());
        assertEquals("fallschirmjaeger", squad.getCargo());
        assertEquals(before.getSquads().values().iterator().next().getTask(), squad.getTask());
        assertEquals(before.getTickets(), after.getTickets());
        assertEquals(before.getInfantrySpawns(), after.getInfantrySpawns());
        assertEquals(before.getVehicleSpawns(), after.getVehicleSpawns());
        assertEquals(before.getCommanders(), after.getCommanders());
        assertEquals(before.getSidesThatFought(), after.getSidesThatFought());
        assertEquals(before.getFieldedPlayers(), after.getFieldedPlayers());
        assertFalse(after.isOpening());
        assertEquals(before.getNextSlot(), after.getNextSlot(), "new slots do not reuse the ids of restored ones");
        assertEquals(before.getNextSquad(), after.getNextSquad());
    }

    @Test
    void anotherPlayingStartsAfresh()
    {
        CompoundTag saved = new CompoundTag();
        BotBattleCodec.write(played(UUID.randomUUID()), saved);
        BotBattle other = battle(UUID.randomUUID());
        assertFalse(BotRoundStore.load(saved, ItemStackData.builtInRegistries()).resume(other, NOW));
        assertTrue(other.getSlots().isEmpty());
        assertTrue(other.getTickets().isEmpty());
    }

    @Test
    void aStoredRoundNotYetResumedIsSavedAgainUnchanged()
    {
        CompoundTag saved = new CompoundTag();
        BotBattleCodec.write(played(UUID.randomUUID()), saved);
        assertEquals(saved, BotRoundStore.load(saved, ItemStackData.builtInRegistries()).save(new CompoundTag(), ItemStackData.builtInRegistries()));
    }

    @Test
    void leadersRallyPointsAndSpawnChoicesSurviveARestart()
    {
        UUID instance = UUID.randomUUID();
        BotBattle before = played(instance);
        BotSquad squad = before.getSquads().values().iterator().next();
        UUID leader = UUID.randomUUID();
        squad.setLeader(leader);
        squad.setRally(new Vec3(5D, 70D, 6D));
        squad.setRallyUntil(NOW + 1000L);
        before.getSpawnChoices().put(leader, BotLeaders.SpawnChoice.RALLY);
        CompoundTag saved = new CompoundTag();
        BotBattleCodec.write(before, saved);
        BotBattle after = battle(instance);
        assertTrue(BotRoundStore.load(saved, ItemStackData.builtInRegistries()).resume(after, NOW));
        BotSquad restored = after.getSquads().values().iterator().next();
        assertEquals(leader, restored.getLeader());
        assertEquals(new Vec3(5D, 70D, 6D), restored.getRally());
        assertEquals(NOW + 1000L, restored.getRallyUntil());
        assertEquals(BotLeaders.SpawnChoice.RALLY, after.getSpawnChoices().get(leader));
    }
}
