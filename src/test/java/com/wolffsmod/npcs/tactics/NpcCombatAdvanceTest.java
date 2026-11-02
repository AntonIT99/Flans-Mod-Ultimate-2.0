package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcSprinting;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcSafetyState.Action;
import com.wolffsmod.npcs.teams.NpcSuppression;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCombatAdvanceTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aSoldierMovesTowardItsOrderWhileRetainingAnEnemyOffToTheSide() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            assertTrue(scene.goal.canUse());
            scene.goal.start();
            scene.goal.tick();
            assertEquals(Action.FIRE, scene.state.getSafety().getAction());
            scene.now = 50L;
            scene.goal.tick();
            Vec3 bound = scene.state.getSafety().getDestination();
            assertNotNull(bound);
            assertTrue(bound.x > 5D, "progress follows the eastward order, not the enemy to the south");
            assertTrue(bound.horizontalDistance() <= scene.tuning.get(Value.ADVANCE_DISTANCE));
            assertTrue(Math.abs(bound.z) < 1D);
            assertTrue(NpcCombatAdvance.holdsFire(scene.npc));
            NpcSprinting.tick(scene.npc);
            verify(scene.npc).setSprinting(true);
            scene.position = bound;
            scene.now = 60L;
            scene.goal.tick();
            assertEquals(Action.FIRE, scene.state.getSafety().getAction());
            assertFalse(NpcCombatAdvance.holdsFire(scene.npc));
            assertEquals(100L, scene.state.getSafety().getAdvanceReadyAt());
            scene.now = 90L;
            scene.goal.tick();
            assertEquals(bound, scene.state.getSafety().getDestination(), "arrival starts the firing pause, not departure");
            verify(scene.npc, never()).setTarget(any());
        }
    }

    @Test
    void reloadingCanStartABoundEarlyButPinningCannotStartAnExposedBound() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.goal.canUse();
            scene.state.reloading(0L, 80);
            scene.now = 10L;
            scene.goal.tick();
            assertEquals(Action.ADVANCE, scene.state.getSafety().getAction());
        }
        try (Scene scene = new Scene())
        {
            scene.suppression.when(() -> NpcSuppression.isPinned(scene.npc)).thenReturn(true);
            scene.goal.canUse();
            scene.now = 50L;
            scene.goal.tick();
            assertNotEquals(Action.ADVANCE, scene.state.getSafety().getAction());
            verify(scene.navigation, never()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    @Test
    void aPinnedSoldierCanStillEscapeIntoCoverAhead() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.options.set(Feature.TACTICAL_COVER, true);
            scene.state.threat(scene.enemy, scene.enemy, 0L, 300L);
            BlockPos cover = new BlockPos(4, 0, 0);
            scene.cover.when(() -> NpcCover.safePositions(eq(scene.npc), anyList(), anyInt(), any())).thenReturn(List.of(cover));
            scene.suppression.when(() -> NpcSuppression.isPinned(scene.npc)).thenReturn(true);
            assertTrue(scene.goal.canUse());
            scene.goal.start();
            scene.goal.tick();
            assertEquals(Action.HIDE, scene.state.getSafety().getAction());
            assertEquals(Vec3.atBottomCenterOf(cover), scene.state.getSafety().getDestination());
            verify(scene.navigation).moveTo(4.5D, 0D, 0.5D, 1D);
        }
    }

    @Test
    void disabledAdvanceAndExplicitHoldsDoNotTakeOverMovement() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.options.set(Feature.COMBAT_ADVANCE, false);
            assertFalse(scene.goal.canUse());
            scene.options.set(Feature.COMBAT_ADVANCE, true);
            scene.tuning.set(Value.COMBAT_MOVEMENT, NpcTuning.MovementFreedom.HOLD.ordinal());
            assertFalse(scene.goal.canUse());
            scene.tuning.set(Value.COMBAT_MOVEMENT, NpcTuning.MovementFreedom.LOCAL.ordinal());
            scene.tuning.set(Value.TACTIC, NpcTuning.Tactic.HOLD.ordinal());
            assertFalse(scene.goal.canUse());
        }
    }

    @Test
    void anUnreachableAdvanceGetsABoundedRetryInsteadOfRepeatedlyTakingTheSameSpot() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.goal.canUse();
            scene.now = 50L;
            scene.goal.tick();
            Vec3 failed = scene.state.getSafety().getDestination();
            when(scene.navigation.isDone()).thenReturn(true);
            scene.now = 111L;
            scene.goal.tick();
            assertEquals(Action.NONE, scene.state.getSafety().getAction());
            scene.now = 160L;
            scene.goal.tick();
            assertNotEquals(failed, scene.state.getSafety().getDestination());
        }
    }

    @Test
    void assignedTravelCrossesSuccessiveLocalAreasButUnorderedPursuitKeepsItsAnchor() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.position = new Vec3(40D, 0D, 0D);
            scene.state.getSafety().setAnchor(Vec3.ZERO);
            scene.state.getSafety().setAdvanceIntent(scene.order);
            assertTrue(NpcCombatMovement.permits(scene.npc, new Vec3(46D, 0D, 0D)));
            assertTrue(NpcCombatMovement.permits(scene.npc, new Vec3(38D, 0D, 3D)), "travelling soldiers may still take nearby cover behind or beside them");
            scene.state.getSafety().setAdvanceIntent(new NpcCombatIntent(new Vec3(60D, 0D, 0D), 2D, Vec3.ZERO, 2D, false));
            assertFalse(NpcCombatMovement.permits(scene.npc, new Vec3(46D, 0D, 0D)));
            scene.state.getSafety().setAdvanceIntent(scene.order);
            scene.position = scene.order.destination();
            assertFalse(NpcCombatMovement.permits(scene.npc, new Vec3(70D, 0D, 0D)), "reaching the objective keeps later cover inside its area");
        }
    }

    @Test
    void aSoldierAtItsObjectiveKeepsItsPositionInsteadOfChasingTheCombatTarget() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.position = scene.order.destination();
            assertTrue(scene.goal.canUse());
            scene.goal.start();
            for (long tick = 0L; tick <= 100L; tick += 10L)
            {
                scene.now = tick;
                scene.goal.tick();
                assertEquals(Action.FIRE, scene.state.getSafety().getAction());
                assertEquals(scene.order.destination(), scene.state.getSafety().getDestination());
            }
            verify(scene.navigation, never()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
            assertFalse(NpcCombatAdvance.holdsFire(scene.npc));
        }
    }

    @Test
    void aShooterCoversAnAdvancingAllyOnlyWhileItCanActuallyReturnFire() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene(); var combatants = mockStatic(NpcCombatants.class); var relations = mockStatic(NpcDrivers.class); var ranges = mockStatic(NpcWeaponRange.class))
        {
            ServerLevel level = mock(ServerLevel.class);
            when(scene.npc.level()).thenReturn(level);
            NpcCombatants fighters = mock(NpcCombatants.class);
            combatants.when(() -> NpcCombatants.of(level)).thenReturn(fighters);
            EntityNPCInterface ally = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
            NpcTacticState allyState = new NpcTacticState();
            when(((NpcTacticState.Holder) ally).wolffsmodnpcsTacticState()).thenReturn(allyState);
            allyState.getSafety().setAction(Action.ADVANCE);
            allyState.getSafety().setUntil(50L);
            when(ally.isAlive()).thenReturn(true);
            when(ally.getTarget()).thenReturn(scene.enemy);
            when(fighters.npcSize()).thenReturn(1);
            NpcCombatants.Combatant entry = new NpcCombatants.Combatant(ally, ally, new AABB(0D, 0D, 0D, 1D, 2D, 1D), 0);
            when(fighters.npc(0)).thenReturn(entry);
            relations.when(() -> NpcDrivers.ally(scene.npc, ally)).thenReturn(true);
            ranges.when(() -> NpcWeaponRange.firing(scene.npc)).thenReturn(128);
            assertTrue(NpcCombatAdvance.coversAlly(scene.npc, scene.enemy, 10L));
            assertFalse(NpcCombatAdvance.coversAlly(scene.npc, scene.enemy, 50L), "a completed or expired bound cannot pin the covering soldier indefinitely");
            scene.state.reloading(0L, 20);
            assertFalse(NpcCombatAdvance.coversAlly(scene.npc, scene.enemy, 10L));
            when(scene.npc.getSensing().hasLineOfSight(scene.enemy)).thenReturn(false);
            assertFalse(NpcCombatAdvance.coversAlly(scene.npc, scene.enemy, 30L));
        }
    }

    private static final class Scene implements AutoCloseable
    {
        private final EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        private final LivingEntity enemy = mock(LivingEntity.class);
        private final Level level = mock(Level.class);
        private final PathNavigation navigation = mock(PathNavigation.class);
        private final NpcTacticState state = new NpcTacticState();
        private final NpcTuning tuning = new NpcTuning();
        private final NpcWeaponOptions options = new NpcWeaponOptions();
        private final NpcCombatIntent order = new NpcCombatIntent(new Vec3(60D, 0D, 0D), 2D);
        private final MockedStatic<NpcEquipment> equipment = mockStatic(NpcEquipment.class);
        private final MockedStatic<NpcEquipmentRangedGoal> weapons = mockStatic(NpcEquipmentRangedGoal.class);
        private final MockedStatic<NpcVehicleThreats> threats = mockStatic(NpcVehicleThreats.class);
        private final MockedStatic<TacticKind> kinds = mockStatic(TacticKind.class);
        private final MockedStatic<NpcCover> cover = mockStatic(NpcCover.class);
        private final MockedStatic<NpcSuppression> suppression = mockStatic(NpcSuppression.class);
        private final MockedStatic<TeamsBots> orders = mockStatic(TeamsBots.class);
        private final NpcSafetyGoal goal;
        private Vec3 position = Vec3.ZERO;
        private long now;

        @SuppressWarnings("java:S3011") // Install Custom NPCs' final containers on the Mockito entity, which skips construction.
        private Scene() throws ReflectiveOperationException
        {
            var ai = EntityNPCInterface.class.getField("ais");
            ai.setAccessible(true);
            ai.set(npc, new DataAI(npc));
            npc.ais.canSprint = true;
            npc.ais.returnToStart = false;
            npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
            var ranged = DataStats.class.getField("ranged");
            ranged.setAccessible(true);
            ranged.set(npc.stats, new DataRanged(npc));
            when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
            when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
            when(npc.level()).thenReturn(level);
            when(npc.getUUID()).thenReturn(UUID.randomUUID());
            when(level.getGameTime()).thenAnswer(call -> now);
            when(npc.position()).thenAnswer(call -> position);
            when(npc.isAlive()).thenReturn(true);
            when(npc.getTarget()).thenReturn(enemy);
            when(npc.getNavigation()).thenReturn(navigation);
            when(navigation.isDone()).thenReturn(true);
            doAnswer(call ->
            {
                when(navigation.isDone()).thenReturn(true);
                return null;
            }).when(navigation).stop();
            when(navigation.moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenAnswer(call ->
            {
                when(navigation.isDone()).thenReturn(false);
                return true;
            });
            Sensing sensing = mock(Sensing.class);
            when(npc.getSensing()).thenReturn(sensing);
            when(sensing.hasLineOfSight(enemy)).thenReturn(true);
            when(enemy.level()).thenReturn(level);
            when(enemy.isAlive()).thenReturn(true);
            Vec3 enemyPosition = new Vec3(0D, 0D, 40D);
            when(enemy.position()).thenReturn(enemyPosition);
            when(enemy.getEyePosition()).thenReturn(enemyPosition);
            when(npc.isInRange(eq(enemy), anyDouble())).thenAnswer(call -> position.distanceTo(enemyPosition) <= call.<Double>getArgument(1));
            options.set(Feature.TACTICAL_COVER, false);
            options.set(Feature.VEHICLE_EVASION, false);
            options.set(Feature.ALLY_SPACING, false);
            equipment.when(() -> NpcEquipment.enabled(eq(npc), any())).thenAnswer(call -> options.enabled(call.getArgument(1)));
            weapons.when(() -> NpcEquipmentRangedGoal.engages(npc)).thenReturn(true);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            threats.when(() -> NpcVehicleThreats.fireOrigin(enemy)).thenReturn(enemyPosition);
            cover.when(() -> NpcCover.standable(eq(level), any())).thenReturn(true);
            orders.when(() -> TeamsBots.combatIntent(npc)).thenReturn(Optional.of(order));
            goal = new NpcSafetyGoal(npc);
        }

        @Override
        public void close()
        {
            orders.close();
            suppression.close();
            cover.close();
            kinds.close();
            threats.close();
            weapons.close();
            equipment.close();
        }
    }
}
