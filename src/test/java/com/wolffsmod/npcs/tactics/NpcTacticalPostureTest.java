package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.teams.NpcSuppression;
import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NpcTacticalPostureTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void supportGoesProneBrieflyAndCooldownPreventsRepeatedPostureChanges() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.posture.tick(scene.npc);
            assertEquals(AnimationType.CRAWL, scene.npc.currentAnimation);
            assertEquals(AnimationType.CRAWL, scene.posture.animation(AnimationType.AIM));
            assertEquals(AnimationType.SIT, scene.posture.animation(AnimationType.SIT), "an explicit animation wins");
            assertEquals(0.3F, NpcTacticalPosture.speed(scene.npc, 1F));
            scene.now = 80L;
            scene.posture.tick(scene.npc);
            assertEquals(0, scene.npc.currentAnimation);
            scene.now = 120L;
            scene.posture.tick(scene.npc);
            assertEquals(0, scene.npc.currentAnimation);
            verify(scene.npc, times(2)).setCurrentAnimation(anyInt());
        }
    }

    @Test
    void closeEnemiesAndUrgentMovementImmediatelyCancelProne() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.posture.tick(scene.npc);
            when(scene.npc.isInRange(scene.enemy, 8D)).thenReturn(true);
            scene.posture.tick(scene.npc);
            assertEquals(0, scene.npc.currentAnimation);
        }
        try (Scene scene = new Scene())
        {
            scene.posture.tick(scene.npc);
            scene.state.getSafety().setAction(NpcSafetyState.Action.EVADE);
            scene.state.getSafety().setUntil(20L);
            scene.posture.tick(scene.npc);
            assertEquals(0, scene.npc.currentAnimation);
        }
    }

    @Test
    void authoredAnimationsAndUnevenGroundAreRespected() throws ReflectiveOperationException
    {
        try (Scene scene = new Scene())
        {
            scene.npc.ais.animationType = AnimationType.DANCE;
            scene.posture.tick(scene.npc);
            verify(scene.npc, never()).setCurrentAnimation(anyInt());
        }
        try (Scene scene = new Scene())
        {
            scene.cover.when(() -> NpcCover.standable(any(), any())).thenReturn(false);
            scene.posture.tick(scene.npc);
            assertEquals(AnimationType.CROUCH, scene.npc.currentAnimation, "uneven ground allows crouching, not crawling");
            assertEquals(0.6F, NpcTacticalPosture.speed(scene.npc, 1F));
        }
    }

    private static final class Scene implements AutoCloseable
    {
        private final EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        private final LivingEntity enemy = mock(LivingEntity.class);
        private final NpcTacticState state = new NpcTacticState();
        private final NpcTacticalPosture posture = state.getPosture();
        private final MockedStatic<NpcEquipment> equipment = mockStatic(NpcEquipment.class);
        private final MockedStatic<NpcEquipmentRangedGoal> weapons = mockStatic(NpcEquipmentRangedGoal.class);
        private final MockedStatic<NpcInfantryRole> roles = mockStatic(NpcInfantryRole.class);
        private final MockedStatic<TacticKind> kinds = mockStatic(TacticKind.class);
        private final MockedStatic<NpcCover> cover = mockStatic(NpcCover.class);
        private final MockedStatic<NpcSuppression> suppression = mockStatic(NpcSuppression.class);
        private long now;

        @SuppressWarnings("java:S3011") // Mockito skips the dependency's final AI and display container construction.
        private Scene() throws ReflectiveOperationException
        {
            Level level = mock(Level.class);
            var ai = EntityNPCInterface.class.getField("ais");
            ai.setAccessible(true);
            ai.set(npc, new DataAI(npc));
            var display = EntityNPCInterface.class.getField("display");
            display.setAccessible(true);
            DataDisplay appearance = mock(DataDisplay.class);
            when(appearance.getSize()).thenReturn(5);
            display.set(npc, appearance);
            npc.animateAi = mock(EntityAIAnimation.class);
            when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
            when(npc.level()).thenReturn(level);
            when(level.getGameTime()).thenAnswer(call -> now);
            when(npc.getTarget()).thenReturn(enemy);
            when(enemy.isAlive()).thenReturn(true);
            when(enemy.level()).thenReturn(level);
            when(enemy.getEyePosition()).thenReturn(new Vec3(20D, 1.6D, 0D));
            when(npc.position()).thenReturn(Vec3.ZERO);
            when(npc.blockPosition()).thenReturn(BlockPos.ZERO);
            when(npc.isAlive()).thenReturn(true);
            when(npc.onGround()).thenReturn(true);
            RandomSource random = mock(RandomSource.class);
            when(npc.getRandom()).thenReturn(random);
            PathNavigation navigation = mock(PathNavigation.class);
            when(npc.getNavigation()).thenReturn(navigation);
            when(navigation.isDone()).thenReturn(true);
            when(level.clip(any())).thenReturn(BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO));
            doAnswer(call ->
            {
                npc.currentAnimation = call.getArgument(0);
                return null;
            }).when(npc).setCurrentAnimation(anyInt());
            equipment.when(() -> NpcEquipment.enabled(any(), any())).thenReturn(true);
            weapons.when(() -> NpcEquipmentRangedGoal.engages(npc)).thenReturn(true);
            roles.when(() -> NpcInfantryRole.support(npc)).thenReturn(true);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            cover.when(() -> NpcCover.standable(any(), any())).thenReturn(true);
        }

        @Override
        public void close()
        {
            suppression.close();
            cover.close();
            kinds.close();
            roles.close();
            weapons.close();
            equipment.close();
        }
    }
}
