package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BattleFactionsTest
{
    private static final int GERMANY = 4;
    private static final int BRITAIN = 7;

    private static BattleFactions germany()
    {
        BattleFactions data = new BattleFactions();
        BattleFactions.Managed managed = data.manage(GERMANY);
        managed.setOutsiders(BattleFactions.Outsiders.POINTS);
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        force.setCount(12);
        force.getVehicles().add(new BotSettings.VehicleEntry("tiger", 2, 0, 60, 3, null, ""));
        BattleFactions.Branch wehrmacht = new BattleFactions.Branch("wehrmacht", force);
        wehrmacht.getRoles().addAll(Set.of(BotRole.INFANTRY, BotRole.ARMOR));
        wehrmacht.setWeight(3);
        managed.getBranches().add(wehrmacht);
        managed.getBranches().add(new BattleFactions.Branch("luftwaffe", new BotSettings.TeamForce()));
        return data;
    }

    @Test
    void factionsBranchesAndMembersSurviveASave()
    {
        BattleFactions before = germany();
        UUID player = UUID.randomUUID();
        before.setMember(player, GERMANY);

        BattleFactions after = BattleFactions.load(before.save(new CompoundTag()));

        BattleFactions.Managed managed = after.managed(GERMANY).orElseThrow();
        assertEquals(BattleFactions.Outsiders.POINTS, managed.getOutsiders());
        assertEquals(2, managed.getBranches().size());
        BattleFactions.Branch wehrmacht = managed.branch("wehrmacht").orElseThrow();
        assertEquals(Set.of(BotRole.INFANTRY, BotRole.ARMOR), wehrmacht.getRoles());
        assertEquals(3, wehrmacht.getWeight());
        assertEquals(12, wehrmacht.getForce().getCount());
        assertEquals("tiger", wehrmacht.getForce().getVehicles().get(0).type());
        assertTrue(managed.branch("luftwaffe").orElseThrow().getRoles().isEmpty(), "a branch set to no roles fills every role it can");
        assertEquals(Optional.of(GERMANY), after.factionOf(player));
    }

    @Test
    void playersCanOnlyBelongToAManagedFaction()
    {
        BattleFactions data = germany();
        UUID player = UUID.randomUUID();
        data.setMember(player, BRITAIN);
        assertEquals(Optional.empty(), data.factionOf(player));
    }

    @Test
    void releasingAFactionLetsItsMembersGo()
    {
        BattleFactions data = germany();
        UUID player = UUID.randomUUID();
        data.setMember(player, GERMANY);
        assertTrue(data.release(GERMANY));
        assertEquals(Optional.empty(), data.factionOf(player));
        assertTrue(data.managed(GERMANY).isEmpty());
    }
}
