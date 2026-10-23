package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import noppes.npcs.ai.EntityAIRangedAttack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomNpcTacticGoalTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void rushAndNoneLeaveAttackMovementAloneAndMeleeDodgeStopsAtItsEngagementRange() throws ReflectiveOperationException
    {
        try (var tactics = mockStatic(NpcTactics.class); var equipment = mockStatic(NpcEquipment.class))
        {
            EntityNPCInterface npc = npc();
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(false);
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            CustomNpcTactics settings = CustomNpcTactics.of(npc);
            CustomNpcTacticGoal goal = new CustomNpcTacticGoal(npc);
            assertFalse(goal.canUse());
            settings.setVariant(CustomNpcTactics.NONE);
            assertFalse(goal.canUse());
            settings.setVariant(CustomNpcTactics.STAGGER);
            assertTrue(goal.canUse());
            goal.start();
            assertTrue(goal.canContinueToUse());
            when(npc.isInRange(npc.getTarget(), 4D)).thenReturn(true);
            assertFalse(goal.canContinueToUse());
            verifyNoInteractions(npc.getNavigation());
        }
    }

    @Test
    void aFlanTacticOrANonRetaliatingResponseStopsTheNativeManoeuvreWithoutChangingItsSavedChoice() throws ReflectiveOperationException
    {
        try (var tactics = mockStatic(NpcTactics.class); var equipment = mockStatic(NpcEquipment.class); var ranged = mockStatic(NpcEquipmentRangedGoal.class))
        {
            EntityNPCInterface npc = npc();
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(false);
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            CustomNpcTactics settings = CustomNpcTactics.of(npc);
            settings.setVariant(CustomNpcTactics.STALK);
            CustomNpcTacticGoal goal = new CustomNpcTacticGoal(npc);
            assertTrue(goal.canUse());
            goal.start();
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.HOLD);
            assertFalse(goal.canContinueToUse());
            assertFalse(goal.canUse());
            assertEquals(CustomNpcTactics.STALK, settings.getVariant());
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            ranged.when(() -> NpcEquipmentRangedGoal.rushes(npc)).thenReturn(true);
            assertFalse(goal.canUse());
            assertFalse(goal.canContinueToUse());
            assertEquals(CustomNpcTactics.STALK, settings.getVariant());
            ranged.when(() -> NpcEquipmentRangedGoal.rushes(npc)).thenReturn(false);
            assertTrue(goal.canUse());
            npc.ais.onAttack = 2;
            assertFalse(goal.canUse());
        }
    }

    @Test
    void takingMovementFromTheRangedGoalRetainsTheTargetThatItsStopCallbackClears() throws ReflectiveOperationException
    {
        try (var tactics = mockStatic(NpcTactics.class); var equipment = mockStatic(NpcEquipment.class))
        {
            EntityNPCInterface npc = npc();
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(false);
            AtomicReference<LivingEntity> target = new AtomicReference<>(npc.getTarget());
            when(npc.getTarget()).thenAnswer(invocation -> target.get());
            doAnswer(invocation ->
            {
                target.set(invocation.getArgument(0));
                return null;
            }).when(npc).setTarget(any());
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            CustomNpcTactics.of(npc).setVariant(CustomNpcTactics.STALK);
            CustomNpcTacticGoal goal = new CustomNpcTacticGoal(npc);
            assertTrue(goal.canUse());
            target.set(null);
            goal.start();
            assertNotNull(target.get());
            assertTrue(goal.canContinueToUse());
            goal.stop();
            verify(npc.getNavigation()).stop();
            assertNotNull(target.get());
        }
    }

    @Test
    void rangedDodgeWaitsForAFiredBurstThenMovesWithoutTheFiringGoalsNavigation() throws ReflectiveOperationException
    {
        try (var tactics = mockStatic(NpcTactics.class); var equipment = mockStatic(NpcEquipment.class); var random = mockStatic(DefaultRandomPos.class))
        {
            EntityNPCInterface npc = npc();
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(true);
            EntityAIRangedAttack shooting = mock(EntityAIRangedAttack.class);
            when(npc.getRangedTask()).thenReturn(shooting);
            CustomNpcTactics.of(npc).setVariant(CustomNpcTactics.STAGGER);
            CustomNpcTacticGoal goal = new CustomNpcTacticGoal(npc);
            assertFalse(goal.canUse());
            random.verifyNoInteractions();
            when(shooting.hasFired()).thenReturn(true);
            random.when(() -> DefaultRandomPos.getPos(npc, 4, 1)).thenReturn(new Vec3(3D, 64D, 5D));
            assertTrue(goal.canUse());
            goal.start();
            verify(npc.getNavigation()).moveTo(3D, 64D, 5D, 1.2D);
            when(npc.getNavigation().isDone()).thenReturn(true);
            assertFalse(goal.canContinueToUse());
        }
    }

    @Test
    void hitAndRunInMeleeWaitsForDamageBeforeChoosingARetreatPath() throws ReflectiveOperationException
    {
        try (var tactics = mockStatic(NpcTactics.class); var equipment = mockStatic(NpcEquipment.class); var random = mockStatic(DefaultRandomPos.class))
        {
            EntityNPCInterface npc = npc();
            tactics.when(() -> NpcTactics.of(npc)).thenReturn(Tactic.CUSTOM_NPCS);
            equipment.when(() -> NpcEquipment.fightsAtRange(npc)).thenReturn(false);
            when(npc.getHealth()).thenReturn(20F);
            CustomNpcTactics.of(npc).setVariant(CustomNpcTactics.HIT_AND_RUN);
            CustomNpcTacticGoal goal = new CustomNpcTacticGoal(npc);
            assertFalse(goal.canUse());
            random.verifyNoInteractions();
            when(npc.getHealth()).thenReturn(19F);
            Vec3 retreat = new Vec3(8D, 64D, 8D);
            when(npc.getTarget().position()).thenReturn(Vec3.ZERO);
            random.when(() -> DefaultRandomPos.getPosAway(npc, 16, 7, Vec3.ZERO)).thenReturn(retreat);
            assertTrue(goal.canUse());
            goal.start();
            verify(npc.getNavigation()).moveTo(8D, 64D, 8D, 1D);
        }
    }

    private static EntityNPCInterface npc() throws ReflectiveOperationException
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        var aiField = EntityNPCInterface.class.getField("ais");
        aiField.setAccessible(true);
        DataAI ai = mock(DataAI.class, withSettings().extraInterfaces(CustomNpcTactics.Holder.class));
        aiField.set(npc, ai);
        when(((CustomNpcTactics.Holder) npc.ais).wolffsmodnpcsCustomTactics()).thenReturn(new CustomNpcTactics());
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        when(npc.getTarget()).thenReturn(target);
        PathNavigation navigation = mock(PathNavigation.class);
        when(npc.getNavigation()).thenReturn(navigation);
        return npc;
    }
}
