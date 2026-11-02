package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.IEmplacement;
import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcSeatPriorityTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S3011") // Mockito skips construction of the final Custom NPCs faction container.
    void onlyAlliesMayTakeAnEmplacementAndDeploymentHasItsOwnSwitch() throws ReflectiveOperationException
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(200L);
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcDriver.Holder.class));
        var faction = EntityNPCInterface.class.getField("faction");
        faction.setAccessible(true);
        faction.set(npc, mock(Faction.class));
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        NpcDriver state = new NpcDriver();
        when(((NpcDriver.Holder) npc).wolffsmodnpcsDriver()).thenReturn(state);
        Entity gun = mock(Entity.class, withSettings().extraInterfaces(IEmplacement.class));
        UUID gunId = UUID.randomUUID();
        when(gun.getUUID()).thenReturn(gunId);
        when(npc.getVehicle()).thenReturn(gun);
        Player ally = mock(Player.class);
        Player enemy = mock(Player.class);
        try (var drivers = mockStatic(NpcDrivers.class); var equipment = mockStatic(NpcEquipment.class); var bots = mockStatic(TeamsBots.class))
        {
            drivers.when(() -> NpcDrivers.ally(npc, ally)).thenReturn(true);
            assertTrue(NpcSeatPriority.mayYield(npc, ally, false));
            assertFalse(NpcSeatPriority.mayYield(npc, enemy, false));
            assertFalse(NpcSeatPriority.mayYield(npc, ally, true));
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.PLAYER_SEAT_DEPLOYMENT)).thenReturn(true);
            bots.when(() -> TeamsBots.playerSeatDeployment(npc)).thenReturn(true);
            assertTrue(NpcSeatPriority.mayYield(npc, ally, true));
            bots.when(() -> TeamsBots.playerSeatDeployment(npc)).thenReturn(false);
            assertFalse(NpcSeatPriority.mayYield(npc, ally, true));
            assertTrue(NpcSeatPriority.giveWay(npc, ally, false));
            verify((IEmplacement) gun).setEmplacementFiring(false);
            verify(npc).stopRiding();
            assertTrue(state.getBoardAfter() > 200L);
            assertTrue(state.refuses(gunId, 200L));
        }
    }

    @Test
    void aDriverMovesToAFreePassengerSeatAndDoesNotImmediatelyReclaimTheControls()
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(200L);
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcDriver.Holder.class));
        when(npc.level()).thenReturn(level);
        NpcDriver state = new NpcDriver();
        when(((NpcDriver.Holder) npc).wolffsmodnpcsDriver()).thenReturn(state);
        Entity vehicle = mock(Entity.class);
        UUID vehicleId = UUID.randomUUID();
        when(vehicle.getUUID()).thenReturn(vehicleId);
        when(vehicle.level()).thenReturn(level);
        when(vehicle.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 3D, 3D, 3D));
        Entity driver = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        Entity passenger = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        ISeat driverSeat = (ISeat) driver;
        when(driverSeat.getDriveable()).thenReturn(vehicle);
        when(driverSeat.isDriverSeat()).thenReturn(true);
        when(driver.isAlive()).thenReturn(true);
        when(passenger.isAlive()).thenReturn(true);
        when(npc.getVehicle()).thenReturn(driver);
        when(npc.startRiding(passenger, true)).thenReturn(true);
        when(level.getEntities(eq(vehicle), any(AABB.class), any())).thenReturn(List.of(driver, passenger));
        assertTrue(NpcDrivers.yieldSeat(npc));
        verify(driverSeat).setAutomatedTriggers(false, false);
        verify(npc).stopRiding();
        verify(npc).startRiding(passenger, true);
        assertEquals(vehicleId, state.getBoarded());
        assertTrue(state.getBoardAfter() > 200L);
        assertTrue(state.refuses(vehicleId, 200L));
    }
}
