package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.item.ItemStackData;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FactionMembersTest
{
    private static final int GERMANY = 4;
    private static final int BANDITS = 9;

    @Test
    void membersOfAnyFactionAndTheirDeparturesSurviveASave()
    {
        FactionMembers before = new FactionMembers();
        UUID soldier = UUID.randomUUID();
        UUID outlaw = UUID.randomUUID();
        before.setMember(soldier, GERMANY);
        before.setMember(outlaw, BANDITS);
        before.markLeft(outlaw, GERMANY, 1000);

        FactionMembers after = FactionMembers.load(before.save(new CompoundTag(), ItemStackData.builtInRegistries()), ItemStackData.builtInRegistries());

        assertEquals(Optional.of(GERMANY), after.factionOf(soldier));
        assertEquals(Optional.of(BANDITS), after.factionOf(outlaw));
        assertEquals(1, after.memberCount(BANDITS));
        assertEquals(OptionalLong.of(200), after.ticksSinceLeft(outlaw, GERMANY, 1200));
        assertEquals(OptionalLong.empty(), after.ticksSinceLeft(soldier, GERMANY, 1200));
    }

    @Test
    void leavingForNoFactionForgetsTheMembership()
    {
        FactionMembers data = new FactionMembers();
        UUID player = UUID.randomUUID();
        data.setMember(player, GERMANY);
        data.setMember(player, null);
        assertEquals(Optional.empty(), data.factionOf(player));
        assertEquals(0, data.memberCount(GERMANY));
    }

    @Test
    void membersSavedWithBattleFactionsAreHandedOverOnce()
    {
        UUID player = UUID.randomUUID();
        CompoundTag legacy = new CompoundTag();
        CompoundTag members = new CompoundTag();
        members.putInt(player.toString(), BANDITS);
        legacy.put("members", members);
        BattleFactions battles = BattleFactions.load(legacy, ItemStackData.builtInRegistries());

        Map<UUID, Integer> taken = battles.takeLegacyMembers();
        FactionMembers data = FactionMembers.adopting(taken);

        assertEquals(Optional.of(BANDITS), data.factionOf(player), "a member of a faction that is not managed keeps it");
        assertTrue(data.isDirty());
        assertTrue(battles.takeLegacyMembers().isEmpty());
        assertFalse(battles.save(new CompoundTag(), ItemStackData.builtInRegistries()).contains("members"));
    }
}
