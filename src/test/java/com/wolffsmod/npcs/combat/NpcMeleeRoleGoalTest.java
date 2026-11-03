package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcCombatAdvance;
import com.wolffsmod.npcs.tactics.NpcCombatIntent;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import com.wolffsmod.npcs.tactics.NpcInfantryRole;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.teams.BotSettings.ClassRole;
import com.wolffsmod.npcs.teams.NpcSuppression;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcMeleeRoleGoalTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void openingThrowEndsWhenCloseOrAfterOneLaunchAndDoesNotAffectSkirmishers()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        LivingEntity target = mock(LivingEntity.class);
        NpcTacticState state = new NpcTacticState();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
        UUID enemy = UUID.randomUUID();
        when(target.getUUID()).thenReturn(enemy);
        NpcTuning tuning = new NpcTuning();
        try (var roles = mockStatic(NpcInfantryRole.class);
            var equipment = mockStatic(NpcEquipment.class);
            var settings = mockStatic(NpcTuning.class);
            var kinds = mockStatic(TacticKind.class);
            var platform = mockStatic(CustomNpcsPlatform.class);
            var movement = mockStatic(NpcCombatMovement.class))
        {
            settings.when(() -> NpcTuning.of(npc)).thenReturn(tuning);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            movement.when(() -> NpcCombatMovement.allowed(npc)).thenReturn(true);
            roles.when(() -> NpcInfantryRole.of(npc)).thenReturn(ClassRole.MELEE);
            equipment.when(() -> NpcEquipment.meleeWeapon(npc)).thenReturn(sword);
            equipment.when(() -> NpcEquipment.meleeWeapon(sword)).thenReturn(true);
            assertFalse(NpcMeleeRoleGoal.committed(npc, target));
            state.setMeleeThrowTarget(enemy);
            assertTrue(NpcMeleeRoleGoal.committed(npc, target));
            tuning.set(NpcTuning.Value.TACTIC, NpcTuning.Tactic.HOLD.ordinal());
            assertFalse(NpcMeleeRoleGoal.committed(npc, target), "an explicit tactic overrides the automatic melee charge");
            tuning.set(NpcTuning.Value.TACTIC, NpcTuning.Tactic.CUSTOM_NPCS.ordinal());
            state.setMeleeThrowTarget(null);
            when(npc.isInRange(target, 8D)).thenReturn(true);
            assertTrue(NpcMeleeRoleGoal.committed(npc, target));
            roles.when(() -> NpcInfantryRole.of(npc)).thenReturn(ClassRole.SKIRMISHER);
            assertFalse(NpcMeleeRoleGoal.committed(npc, target));
        }
    }

    @Test
    void meleeClosesThenStrikesWithANativeCooldownAndPinningStopsTheCharge()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        Level level = mock(Level.class);
        PathNavigation navigation = mock(PathNavigation.class);
        Sensing sensing = mock(Sensing.class);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        when(npc.getTarget()).thenReturn(target);
        when(npc.getMainHandItem()).thenReturn(sword);
        when(npc.getNavigation()).thenReturn(navigation);
        when(npc.getSensing()).thenReturn(sensing);
        when(sensing.hasLineOfSight(target)).thenReturn(true);
        LookControl look = mock(LookControl.class);
        when(npc.getLookControl()).thenReturn(look);
        when(npc.level()).thenReturn(level);
        when(target.position()).thenReturn(new Vec3(10D, 0D, 0D));
        npc.stats = mock(DataStats.class);
        npc.stats.melee = mock(DataMelee.class);
        when(npc.stats.melee.getRange()).thenReturn(3F);
        when(npc.stats.melee.getDelay()).thenReturn(20);
        try (var roles = mockStatic(NpcInfantryRole.class);
            var equipment = mockStatic(NpcEquipment.class);
            var movement = mockStatic(NpcCombatMovement.class);
            var suppression = mockStatic(NpcSuppression.class);
            var orders = mockStatic(NpcCombatAdvance.class))
        {
            roles.when(() -> NpcInfantryRole.of(npc)).thenReturn(ClassRole.RIFLEMAN);
            equipment.when(() -> NpcEquipment.meleeWeapon(npc)).thenReturn(sword);
            equipment.when(() -> NpcEquipment.meleeWeapon(sword)).thenReturn(true);
            equipment.when(() -> NpcEquipment.meleeHand(npc)).thenReturn(InteractionHand.MAIN_HAND);
            movement.when(() -> NpcCombatMovement.permits(npc, target.position())).thenReturn(true);
            NpcMeleeRoleGoal goal = new NpcMeleeRoleGoal(npc);
            goal.tick();
            verify(navigation).moveTo(target, 1D);
            when(npc.position()).thenReturn(Vec3.ZERO);
            orders.when(() -> NpcCombatAdvance.assigned(npc)).thenReturn(new NpcCombatIntent(Vec3.ZERO, 2D));
            when(level.getGameTime()).thenReturn(10L);
            goal.tick();
            verify(navigation).stop();
            verify(navigation, times(1)).moveTo(target, 1D);
            orders.when(() -> NpcCombatAdvance.assigned(npc)).thenReturn(null);
            suppression.when(() -> NpcSuppression.isPinned(npc)).thenReturn(true);
            goal.tick();
            verify(navigation, times(2)).stop();
            when(npc.isInRange(target, 3D)).thenReturn(true);
            goal.tick();
            goal.tick();
            verify(npc, times(1)).doHurtTarget(target);
            when(level.getGameTime()).thenReturn(30L);
            goal.tick();
            verify(npc, times(2)).doHurtTarget(target);
        }
    }
}
