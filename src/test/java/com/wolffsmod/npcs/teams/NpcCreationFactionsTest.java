package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcCreationFactionsTest
{
    @Test
    void overridesStayWithTheirPlayerAfterSavingAndResetting()
    {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        NpcCreationFactions before = new NpcCreationFactions();
        assertTrue(before.factionOf(alice).isEmpty());
        before.set(alice, 0);
        before.set(bob, 7);

        NpcCreationFactions after = NpcCreationFactions.load(before.save(new CompoundTag(), ItemStackData.builtInRegistries()), ItemStackData.builtInRegistries());
        assertEquals(OptionalInt.of(0), after.factionOf(alice), "faction zero is a valid override");
        assertEquals(OptionalInt.of(7), after.factionOf(bob));
        after.reset(alice);
        after.reset(alice);
        assertTrue(after.factionOf(alice).isEmpty());
        assertEquals(OptionalInt.of(7), after.factionOf(bob));

        NpcCreationFactions reloaded = NpcCreationFactions.load(after.save(new CompoundTag(), ItemStackData.builtInRegistries()), ItemStackData.builtInRegistries());
        assertTrue(reloaded.factionOf(alice).isEmpty());
        assertEquals(OptionalInt.of(7), reloaded.factionOf(bob));
    }

    @Test
    void malformedEntriesDoNotDiscardOtherPlayers()
    {
        UUID valid = UUID.randomUUID();
        UUID negative = UUID.randomUUID();
        UUID wrongType = UUID.randomUUID();
        CompoundTag players = new CompoundTag();
        players.putInt(valid.toString(), 4);
        players.putInt("invalid UUID", 9);
        players.putInt(negative.toString(), -1);
        players.putString(wrongType.toString(), "4");
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);

        NpcCreationFactions data = NpcCreationFactions.load(tag, ItemStackData.builtInRegistries());
        assertEquals(OptionalInt.of(4), data.factionOf(valid));
        assertTrue(data.factionOf(negative).isEmpty());
        assertTrue(data.factionOf(wrongType).isEmpty());
    }
}
