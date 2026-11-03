package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotSquadTacticsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void leadershipOrdersReachMovementAndRepeatedReadsDoNotResampleOrSearchTheWorld()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        BotBattle battle = new BotBattle(UUID.randomUUID(), mock(Battleground.class), new BotSettings(), Map.of());
        BotSquad squad = spy(battle.newSquad("us", BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, ""));
        battle.getSquads().put(squad.getId(), squad);
        BotSquadCombat combat = mock(BotSquadCombat.class);
        doReturn(combat).when(squad).getCombat();
        Vec3 target = new Vec3(100D, 64D, 0D);
        SquadTask task = new SquadTask(SquadTask.Kind.ATTACK, target, 6D);
        squad.setTask(task);
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        UUID id = UUID.randomUUID();
        when(npc.getUUID()).thenReturn(id);
        when(npc.isAlive()).thenReturn(true);
        when(npc.position()).thenReturn(new Vec3(0D, 64D, 0D));
        when(level.getEntity(id)).thenReturn(npc);
        BotSlot slot = battle.newSlot("us", BotSlot.Source.INFANTRY, -1);
        slot.setSquad(squad.getId());
        slot.setEntity(id);
        squad.getSlots().add(slot.getId());
        battle.getSlotOfEntity().put(id, slot.getId());
        var snapshot = new SquadCombat.Member(id, npc.position(), 10, true, false, false, false, BotSettings.ClassRole.ASSAULT, true, target);
        var field = new BotSquadTactics.Field(List.of(target), List.of(npc.position()), List.of(), List.of(), List.of(), 0);
        var view = new Doctrine.SquadView(squad.getId(), BotRole.INFANTRY, SquadTemplate.Posture.BALANCED, npc.position(), 1, false, null);
        when(combat.commandMembers(level, battle, squad, 0L)).thenReturn(List.of(snapshot));
        BotSquadTactics.update(level, battle, view, field, 0);
        assertEquals(SquadTactics.Phase.SUPPORT, squad.getTactics().getPhase());
        clearInvocations(level);
        for (int i = 0; i < 100; i++)
        {
            var order = BotCommander.order(server, battle, npc).orElseThrow();
            assertEquals(SquadTask.Kind.HOLD, order.kind());
            assertEquals(npc.position(), order.spot());
        }
        verifyNoInteractions(level);
        verify(combat, times(1)).commandMembers(level, battle, squad, 0L);
        // An explicit player order immediately bypasses and clears tactical leadership.
        squad.setOrder(new PlayerOrder(UUID.randomUUID(), PlayerOrder.Kind.HOLD, target, 1000L));
        squad.setTask(squad.getOrder().task(target));
        BotSquadTactics.update(level, battle, view, field, 0);
        assertNull(squad.getTactics().order(task, id));
        assertEquals(target, BotCommander.order(server, battle, npc).orElseThrow().spot());
    }
}
