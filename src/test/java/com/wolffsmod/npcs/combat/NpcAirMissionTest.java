package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.mockito.Mockito.*;

class NpcAirMissionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void fighterLeavesANearbyGroundTargetForAVisibleAircraftAcrossSearchBands()
    {
        mission(NpcTuning.AircraftRole.FIGHTER, true, true);
    }

    @Test
    void bomberKeepsGroundSupportDespiteACloserAircraft()
    {
        mission(NpcTuning.AircraftRole.BOMBER, true, false);
    }

    @Test
    void invisibleAircraftCannotDisplaceAVisibleGroundTarget()
    {
        mission(NpcTuning.AircraftRole.FIGHTER, false, false);
    }

    private static void mission(NpcTuning.AircraftRole role, boolean visibleAir, boolean chooseAir)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcTuning tuning = new NpcTuning();
        tuning.set(NpcTuning.Value.AIRCRAFT_ROLE, role.ordinal());
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        LivingEntity ground = mock(LivingEntity.class);
        LivingEntity air = mock(LivingEntity.class);
        when(ground.isAlive()).thenReturn(true);
        when(air.isAlive()).thenReturn(true);
        when(ground.getId()).thenReturn(1);
        when(air.getId()).thenReturn(2);
        when(ground.position()).thenReturn(new Vec3(0D, 0D, 20D));
        when(air.position()).thenReturn(new Vec3(0D, 80D, 120D));
        when(npc.distanceToSqr(ground)).thenReturn(400D);
        when(npc.distanceToSqr(air)).thenReturn(20800D);
        when(npc.getTarget()).thenReturn(ground);
        NpcCombatants combatants = mock(NpcCombatants.class);
        when(combatants.size()).thenReturn(2);
        when(combatants.get(0)).thenReturn(new NpcCombatants.Combatant(ground, null, new AABB(Vec3.ZERO, Vec3.ZERO), -1));
        when(combatants.get(1)).thenReturn(new NpcCombatants.Combatant(air, null, new AABB(Vec3.ZERO, Vec3.ZERO), -1));
        try (var models = mockStatic(NpcModels.class);
            var bombs = mockStatic(NpcBombTargets.class);
            var equipment = mockStatic(NpcEquipment.class);
            var entities = mockStatic(NpcCombatants.class);
            var search = mockStatic(NpcTargetSearch.class);
            var sight = mockStatic(NpcSight.class);
            var teams = mockStatic(TeamsBots.class);
            var aircraft = mockStatic(NpcAirCombat.class, CALLS_REAL_METHODS);
            var selectors = mockConstruction(NPCAttackSelector.class, (selector, context) -> when(selector.isEntityApplicable(any())).thenReturn(true)))
        {
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(true);
            entities.when(() -> NpcCombatants.of(level)).thenReturn(combatants);
            search.when(() -> NpcTargetSearch.range(npc)).thenReturn(192D);
            sight.when(() -> NpcSight.sees(npc, ground)).thenReturn(true);
            sight.when(() -> NpcSight.sees(npc, air)).thenReturn(visibleAir);
            teams.when(() -> TeamsBots.airMission(npc)).thenReturn(Optional.of(new NpcAirCombat.Mission(Vec3.ZERO, 4)));
            aircraft.when(() -> NpcAirCombat.airborne(ground)).thenReturn(false);
            aircraft.when(() -> NpcAirCombat.airborne(air)).thenReturn(true);
            new NpcAirCombat().tick(npc);
            if (chooseAir)
                verify(npc).setTarget(air);
            else
                verify(npc, never()).setTarget(any());
        }
    }
}
