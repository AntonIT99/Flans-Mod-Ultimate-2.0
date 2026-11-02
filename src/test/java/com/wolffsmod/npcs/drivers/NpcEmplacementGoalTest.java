package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IEmplacement;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcEmplacementGoalTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S3011") // Mockito skips the constructor that supplies the final Custom NPCs AI container.
    void mountedOperatorsFireOnlyWithSightAndLeaveAfterLosingTheirTarget() throws ReflectiveOperationException
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcDriver.Holder.class));
        when(((NpcDriver.Holder) npc).wolffsmodnpcsDriver()).thenReturn(new NpcDriver());
        var ai = EntityNPCInterface.class.getField("ais");
        ai.setAccessible(true);
        ai.set(npc, new DataAI(npc));
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        PathNavigation navigation = mock(PathNavigation.class);
        when(npc.getNavigation()).thenReturn(navigation);
        Entity mount = mock(Entity.class, withSettings().extraInterfaces(IEmplacement.class));
        IEmplacement emplacement = (IEmplacement) mount;
        when(mount.isAlive()).thenReturn(true);
        when(npc.getVehicle()).thenReturn(mount);
        IContentType definition = mock(IContentType.class);
        when(emplacement.getEmplacementType()).thenReturn(Optional.of(definition));
        when(emplacement.hasEmplacementAmmunition()).thenReturn(true);
        when(emplacement.getEmplacementOrigin()).thenReturn(Vec3.ZERO);
        when(emplacement.getEmplacementDirection()).thenReturn(new Vec3(0D, 0D, 1D));
        when(emplacement.canEmplacementAimAt(any())).thenReturn(true);
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        when(target.getBoundingBox()).thenReturn(new AABB(-0.5D, -0.5D, 19.5D, 0.5D, 0.5D, 20.5D));
        when(npc.getTarget()).thenReturn(target);
        when(level.clip(any(ClipContext.class))).thenReturn(BlockHitResult.miss(new Vec3(0D, 0D, 20D), Direction.UP, BlockPos.ZERO));
        try (var equipment = mockStatic(NpcEquipment.class);
            var crews = mockStatic(NpcCrew.class);
            var models = mockStatic(NpcModels.class);
            var bots = mockStatic(TeamsBots.class);
            var platform = mockStatic(CustomNpcsPlatform.class);
            var skill = mockStatic(NpcSkill.class))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.MAN_EMPLACEMENTS)).thenReturn(true);
            crews.when(() -> NpcCrew.seat(npc)).thenReturn(-1);
            bots.when(() -> TeamsBots.mayManEmplacements(npc)).thenReturn(true);
            NpcEmplacementGoal goal = new NpcEmplacementGoal(npc);
            assertTrue(goal.canUse());
            goal.start();
            goal.tick();
            verify(emplacement).setEmplacementFiring(true);
            when(level.clip(any(ClipContext.class))).thenReturn(new BlockHitResult(new Vec3(0D, 0D, 10D), Direction.NORTH, BlockPos.ZERO, false));
            goal.tick();
            verify(emplacement).setEmplacementFiring(false);
            when(npc.getTarget()).thenReturn(null);
            for (int tick = 0; tick < 100; tick++)
                goal.tick();
            assertFalse(goal.canContinueToUse());
            goal.stop();
            verify(npc).stopRiding();
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.MAN_EMPLACEMENTS)).thenReturn(false);
            assertFalse(goal.canUse());
        }
    }
}
