package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotObjectiveGoalTest
{
    @Test
    void seatedDriversKeepObjectiveRoutingWhileGunnersAndPassengersDoNot() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        when(npc.isPassenger()).thenReturn(true);
        BotCommander.Order order = new BotCommander.Order(new Vec3(100D, 0D, 0D), 4D, BotRole.INFANTRY, SquadTask.Kind.ATTACK);
        try (var drivers = mockStatic(NpcDrivers.class); var bots = mockStatic(TeamsBots.class))
        {
            bots.when(() -> TeamsBots.orderFor(npc)).thenReturn(Optional.of(order));
            assertFalse(new BotObjectiveGoal(npc).canUse());
            drivers.when(() -> NpcDrivers.isDriver(npc)).thenReturn(true);
            BotObjectiveGoal goal = new BotObjectiveGoal(npc);
            assertTrue(goal.canUse());
            assertTrue(goal.canContinueToUse());
            when(npc.getX()).thenReturn(95D);
            net.minecraft.world.entity.Entity seatEntity = mock(net.minecraft.world.entity.Entity.class, withSettings().extraInterfaces(com.flansmodultimate.api.driveables.ISeat.class));
            net.minecraft.world.entity.Entity vehicle = mock(net.minecraft.world.entity.Entity.class);
            when(npc.getVehicle()).thenReturn(seatEntity);
            when(((com.flansmodultimate.api.driveables.ISeat) seatEntity).getDriveable()).thenReturn(vehicle);
            assertFalse(new BotObjectiveGoal(npc).canUse(), "drivers stop outside the tight infantry post");
            drivers.when(() -> NpcDrivers.isDriver(npc)).thenReturn(false);
            assertFalse(goal.canContinueToUse());
        }
    }
}
