package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.tactics.NpcSafetyState.Action;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import noppes.npcs.ai.EntityAISprintToTarget;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcSprintingTest
{
    private EntityNPCInterface npc;
    private LivingEntity target;
    private PathNavigation navigation;
    private boolean sprinting;
    private double distance = 12D;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    @SuppressWarnings("java:S3011") // Mockito skips construction; install the dependency's real final AI container.
    void setup() throws ReflectiveOperationException
    {
        npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        var ai = EntityNPCInterface.class.getField("ais");
        ai.setAccessible(true);
        ai.set(npc, new DataAI(npc));
        npc.ais.canSprint = true;
        target = mock(LivingEntity.class);
        navigation = mock(PathNavigation.class);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        when(target.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        when(target.isAlive()).thenReturn(true);
        when(npc.getTarget()).thenReturn(target);
        when(npc.getNavigation()).thenReturn(navigation);
        when(npc.onGround()).thenReturn(true);
        when(npc.isInRange(eq(target), anyDouble())).thenAnswer(call -> distance <= call.<Double>getArgument(1));
        when(npc.isSprinting()).thenAnswer(call -> sprinting);
        doAnswer(call ->
        {
            sprinting = call.getArgument(0);
            return null;
        }).when(npc).setSprinting(anyBoolean());
    }

    @Test
    void urgentCombatMovesSprintEvenNearAnEnemyAndWhenHurt()
    {
        var safety = ((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState().getSafety();
        distance = 3D;
        npc.hurtTime = 10;
        safety.setUntil(100L);
        for (Action action : new Action[]{Action.EVADE, Action.RETREAT, Action.HIDE, Action.ADVANCE, Action.FLANK})
        {
            safety.setAction(action);
            NpcSprinting.tick(npc, false);
            assertTrue(sprinting, action.name());
        }
        when(navigation.isDone()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void sprintingActuallyIncreasesTheConfiguredWalkingSpeed()
    {
        assertEquals(0.25F, NpcSprinting.movementSpeed(npc, 0.25F));
        NpcSprinting.tick(npc, false);
        assertEquals(0.325F, NpcSprinting.movementSpeed(npc, 0.25F), 0.00001F);
        npc.ais.movementType = 1;
        assertEquals(0.25F, NpcSprinting.movementSpeed(npc, 0.25F));
        npc.ais.movementType = 0;
        when(npc.isPassenger()).thenReturn(true);
        assertEquals(0.25F, NpcSprinting.movementSpeed(npc, 0.25F));
    }

    @Test
    void objectiveTravelSprintsWithoutAnEnemyButOrdinaryWanderingDoesNot()
    {
        when(npc.getTarget()).thenReturn(null);
        when(npc.position()).thenReturn(Vec3.ZERO);
        when(navigation.getTargetPos()).thenReturn(new BlockPos(20, 0, 0));
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        NpcSprinting.tick(npc, true);
        assertTrue(sprinting);
        when(navigation.getTargetPos()).thenReturn(new BlockPos(7, 0, 0));
        NpcSprinting.tick(npc, true);
        assertTrue(sprinting);
        when(navigation.getTargetPos()).thenReturn(new BlockPos(5, 0, 0));
        NpcSprinting.tick(npc, true);
        assertFalse(sprinting);
        when(navigation.getTargetPos()).thenReturn(new BlockPos(20, 0, 0));
        when(navigation.isDone()).thenReturn(true);
        NpcSprinting.tick(npc, true);
        assertFalse(sprinting);
    }

    @Test
    void startsFromRestOnTheTickThePathIsReadyAndNeverStopsPursuit()
    {
        GoalSelector goals = new GoalSelector(() -> InactiveProfiler.INSTANCE);
        goals.addGoal(1, new EntityAISprintToTarget(npc));
        Goal pursuit = new Goal()
        {
            {
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse()
            {
                return true;
            }

            @Override
            public void tick()
            {
                navigation.moveTo(target, 1.3D);
                when(navigation.isDone()).thenReturn(false);
            }

            @Override
            public void stop()
            {
                navigation.stop();
            }
        };
        goals.addGoal(2, pursuit);
        when(navigation.isDone()).thenReturn(true);
        NpcSprinting.removeLegacyGoal(goals);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);

        for (int tick = 0; tick < 20; tick++)
        {
            goals.tick();
            NpcSprinting.tick(npc, false);
            assertTrue(sprinting);
        }
        verify(navigation, times(20)).moveTo(target, 1.3D);
        verify(navigation, never()).stop();
        verify(npc, times(1)).setSprinting(true);
    }

    @Test
    void straightMovementDamageAndJumpsDoNotInterruptAnActiveChase()
    {
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        npc.hurtTime = 10;
        when(npc.onGround()).thenReturn(false);
        for (Vec3 velocity : new Vec3[]{Vec3.ZERO, new Vec3(0.2D, 0D, 0D), new Vec3(0D, 0D, -0.2D)})
        {
            when(npc.getDeltaMovement()).thenReturn(velocity);
            NpcSprinting.tick(npc, false);
            assertTrue(sprinting);
        }
        when(navigation.isDone()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void chaseDoesNotFlickerAroundItsStartDistanceButStopsNearTheTarget()
    {
        distance = 8D;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        distance = 8.1D;
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        for (double blocks : new double[]{7.9D, 8.1D, 7.5D, 6.1D})
        {
            distance = blocks;
            NpcSprinting.tick(npc, false);
            assertTrue(sprinting);
        }
        distance = 6D;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        distance = 7D;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void retreatSprintsAwayFromACloseThreatAndSettlesOnceItHasSpace()
    {
        npc.ais.onAttack = 2;
        distance = 8D;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        distance = 7D;
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        distance = 8D;
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        distance = 9.1D;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void lostDeadOrTransferredTargetsStopSprintingImmediately()
    {
        NpcSprinting.tick(npc, false);
        when(npc.getTarget()).thenReturn(null);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        when(npc.getTarget()).thenReturn(target);
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        when(target.isAlive()).thenReturn(false);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        when(target.isAlive()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        Level otherLevel = mock(Level.class);
        when(target.level()).thenReturn(otherLevel);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void disablingSprintOrChangingTheAttackResponseTakesEffectOnTheNextTick()
    {
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        npc.ais.canSprint = false;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        npc.ais.canSprint = true;
        NpcSprinting.tick(npc, false);
        assertTrue(sprinting);
        npc.ais.onAttack = 3;
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }

    @Test
    void mountedAirborneSwimmingAndInactiveNpcsDoNotStartGroundSprints()
    {
        when(npc.onGround()).thenReturn(false);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        when(npc.onGround()).thenReturn(true);
        when(npc.isPassenger()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        when(npc.isPassenger()).thenReturn(false);
        for (int movement : new int[]{1, 2})
        {
            npc.ais.movementType = movement;
            NpcSprinting.tick(npc, false);
            assertFalse(sprinting);
        }
        npc.ais.movementType = 0;
        when(npc.isInWater()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
        when(npc.isInWater()).thenReturn(false);
        when(npc.isNoAi()).thenReturn(true);
        NpcSprinting.tick(npc, false);
        assertFalse(sprinting);
    }
}
