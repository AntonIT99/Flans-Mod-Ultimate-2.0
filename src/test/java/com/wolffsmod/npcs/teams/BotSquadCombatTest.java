package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotSquadCombatTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aRetargetedMemberCannotLoseItsNewReservationToAnOlderCachedSnapshot()
    {
        ServerLevel level = mock(ServerLevel.class);
        BotBattle battle = new BotBattle(UUID.randomUUID(), mock(Battleground.class), new BotSettings(), Map.of());
        BotSquad squad = battle.newSquad("us", BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, "");
        EntityNPCInterface first = member(level, battle, squad);
        EntityNPCInterface second = member(level, battle, squad);
        LivingEntity initial = mock(LivingEntity.class);
        LivingEntity next = mock(LivingEntity.class);
        when(initial.getId()).thenReturn(10);
        when(next.getId()).thenReturn(20);
        when(initial.isAlive()).thenReturn(true);
        when(next.isAlive()).thenReturn(true);
        when(initial.level()).thenReturn(level);
        when(next.level()).thenReturn(level);
        when(first.getTarget()).thenReturn(initial);
        when(second.getTarget()).thenReturn(initial);
        BotSquadCombat combat = squad.getCombat();
        BlockPos cover = new BlockPos(10, 64, 0);
        try (var equipment = mockStatic(NpcEquipment.class))
        {
            equipment.when(() -> NpcEquipment.enabled(any(), eq(Feature.COVERING_FIRE))).thenReturn(false);
            var original = combat.members(first, battle, squad, 0L);
            assertSame(original, combat.members(first, battle, squad, 1L));
            verify(level, times(1)).getEntity(first.getUUID());
            when(first.getTarget()).thenReturn(next);
            var refreshed = combat.members(first, battle, squad, 2L);
            assertTrue(combat.decisions().claim(first.getUUID(), 20, cover, refreshed, 2L));
            assertFalse(combat.decisions().available(second.getUUID(), cover, combat.members(second, battle, squad, 2L), 2L));
            when(level.getEntity(first.getUUID())).thenReturn(null);
            assertTrue(combat.decisions().available(second.getUUID(), cover, combat.members(second, battle, squad, 7L), 7L));
        }
    }

    private static EntityNPCInterface member(ServerLevel level, BotBattle battle, BotSquad squad)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        UUID id = UUID.randomUUID();
        when(npc.getUUID()).thenReturn(id);
        when(npc.level()).thenReturn(level);
        when(npc.position()).thenReturn(new Vec3(0D, 64D, 0D));
        when(npc.isAlive()).thenReturn(true);
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        when(level.getEntity(id)).thenReturn(npc);
        BotSlot slot = battle.newSlot("us", BotSlot.Source.INFANTRY, -1);
        slot.setEntity(id);
        squad.getSlots().add(slot.getId());
        return npc;
    }
}
