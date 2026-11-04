package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotDriveablesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void switchesDefaultOnPersistIndependentlyAndOldPresetsRemainValid()
    {
        BotSettings settings = new BotSettings();
        assertTrue(settings.isDriveRealVehicles());
        assertTrue(settings.isGunRealVehicles());
        assertTrue(settings.isRideRealVehicles());
        settings.setDriveRealVehicles(false);
        assertFalse(BotDriveables.allowed(settings, true, false));
        assertTrue(BotDriveables.allowed(settings, false, true));
        settings.setGunRealVehicles(false);
        assertFalse(BotDriveables.allowed(settings, false, true));
        assertTrue(BotDriveables.allowed(settings, false, false));
        settings.setRideRealVehicles(false);
        BotSettings saved = BotSettings.load(settings.save());
        assertFalse(saved.isDriveRealVehicles());
        assertFalse(saved.isGunRealVehicles());
        assertFalse(saved.isRideRealVehicles());
        CompoundTag old = settings.save();
        for (String key : List.of("drive_real_vehicles", "gun_real_vehicles", "ride_real_vehicles"))
            old.remove(key);
        BotSettings inherited = BotSettings.load(old);
        assertTrue(inherited.isDriveRealVehicles());
        assertTrue(inherited.isGunRealVehicles());
        assertTrue(inherited.isRideRealVehicles());
        CompoundTag document = new CompoundTag();
        net.minecraft.nbt.ListTag teams = new net.minecraft.nbt.ListTag();
        teams.add(net.minecraft.nbt.StringTag.valueOf("red"));
        document.put("teams", teams);
        document.put("settings", old);
        assertDoesNotThrow(() -> new BotSettingsPresets().validate(document));
    }

    @Test
    void recruitmentKeepsFootSoldiersAndReservationsCountAgainstTheCrewLimit()
    {
        Fixture fixture = new Fixture();
        BotBattles.add(fixture.battle);
        try (var tags = mockStatic(BotTag.class); var drivers = mockStatic(NpcDrivers.class))
        {
            tags.when(() -> BotTag.isBot(fixture.first)).thenReturn(true);
            when(fixture.driverSeat.isDriverSeat()).thenReturn(true);
            drivers.when(() -> NpcDrivers.assignedSeat(fixture.second)).thenReturn(Optional.empty());
            assertTrue(BotDriveables.mayRecruit(fixture.first, fixture.driverSeat));
            drivers.when(() -> NpcDrivers.assignedSeat(fixture.second)).thenReturn(Optional.of(fixture.driverSeat));
            assertFalse(BotDriveables.mayRecruit(fixture.first, fixture.driverSeat));
            drivers.when(() -> NpcDrivers.assignedSeat(fixture.second)).thenReturn(Optional.empty());
            fixture.squad.setTask(new SquadTask(SquadTask.Kind.HOLD, new Vec3(100D, 0D, 0D), 4D));
            assertFalse(BotDriveables.mayRecruit(fixture.first, fixture.driverSeat));
            fixture.squad.setTask(new SquadTask(SquadTask.Kind.ATTACK, Vec3.ZERO, 4D));
            assertFalse(BotDriveables.mayRecruit(fixture.first, fixture.driverSeat));
        }
        finally
        {
            BotBattles.remove(fixture.battle);
        }
    }

    @Test
    void passengersTravelWithTheirSquadAndUnloadOnlyWhenGroundedAndSlow()
    {
        Fixture fixture = new Fixture();
        ISeat passenger = mock(ISeat.class);
        when(passenger.getDriveable()).thenReturn(fixture.vehicle);
        when(fixture.driverSeat.getRiddenByEntity()).thenReturn(fixture.second);
        BotBattles.add(fixture.battle);
        try (var tags = mockStatic(BotTag.class); var entities = mockStatic(FlansEntityTypes.class))
        {
            tags.when(() -> BotTag.isBot(fixture.first)).thenReturn(true);
            assertTrue(BotDriveables.transport(fixture.first, fixture.vehicle));
            assertTrue(BotDriveables.mayRecruit(fixture.first, passenger));
            fixture.squad.setTask(new SquadTask(SquadTask.Kind.ATTACK, Vec3.ZERO, 4D));
            assertFalse(BotDriveables.unload(fixture.first, passenger), "airborne passengers stay aboard");
            entities.when(() -> FlansEntityTypes.isGrounded(fixture.vehicle)).thenReturn(true);
            when(fixture.vehicle.getDeltaMovement()).thenReturn(new Vec3(1D, 0D, 0D));
            assertFalse(BotDriveables.unload(fixture.first, passenger), "moving transports must slow down");
            when(fixture.vehicle.getDeltaMovement()).thenReturn(Vec3.ZERO);
            assertTrue(BotDriveables.unload(fixture.first, passenger));
            fixture.settings.setRideRealVehicles(false);
            assertFalse(BotDriveables.mayUse(fixture.first, passenger));
        }
        finally
        {
            BotBattles.remove(fixture.battle);
        }
    }

    @Test
    void passengerDoesNotJoinAnotherChassisWhenSquadmateHasAReservation()
    {
        Fixture fixture = new Fixture();
        ISeat passenger = mock(ISeat.class);
        ISeat reserved = mock(ISeat.class);
        when(passenger.getDriveable()).thenReturn(fixture.vehicle);
        Entity otherVehicle = mock(Entity.class);
        when(reserved.getDriveable()).thenReturn(otherVehicle);
        when(fixture.driverSeat.getRiddenByEntity()).thenReturn(fixture.second);
        BotBattles.add(fixture.battle);
        try (var tags = mockStatic(BotTag.class); var drivers = mockStatic(NpcDrivers.class))
        {
            tags.when(() -> BotTag.isBot(fixture.first)).thenReturn(true);
            drivers.when(() -> NpcDrivers.assignedSeat(fixture.second)).thenReturn(Optional.of(reserved));
            assertFalse(BotDriveables.mayRecruit(fixture.first, passenger));
            drivers.when(() -> NpcDrivers.assignedSeat(fixture.second)).thenReturn(Optional.empty());
            assertTrue(BotDriveables.mayRecruit(fixture.first, passenger));
        }
        finally
        {
            BotBattles.remove(fixture.battle);
        }
    }

    @Test
    void followingAPlayerBoardsEvenWhenTheFollowDestinationIsWithinBoardingRange()
    {
        Fixture fixture = new Fixture();
        net.minecraft.server.level.ServerPlayer player = mock(net.minecraft.server.level.ServerPlayer.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUUID()).thenReturn(playerId);
        when(fixture.driverSeat.getRiddenByEntity()).thenReturn(player);
        fixture.squad.setOrder(new PlayerOrder(playerId, PlayerOrder.Kind.FOLLOW, Vec3.ZERO, 1000L));
        fixture.squad.setTask(new SquadTask(SquadTask.Kind.GUARD, Vec3.ZERO, 5D));
        ISeat passenger = mock(ISeat.class);
        when(passenger.getDriveable()).thenReturn(fixture.vehicle);
        BotBattles.add(fixture.battle);
        try (var tags = mockStatic(BotTag.class))
        {
            tags.when(() -> BotTag.isBot(fixture.first)).thenReturn(true);
            assertTrue(BotDriveables.mayRecruit(fixture.first, passenger));
            assertTrue(BotDriveables.transport(fixture.first, fixture.vehicle));
            when(fixture.driverSeat.getRiddenByEntity()).thenReturn(null);
            assertFalse(BotDriveables.transport(fixture.first, fixture.vehicle));
        }
        finally
        {
            BotBattles.remove(fixture.battle);
        }
    }

    private static final class Fixture
    {
        final ServerLevel level = mock(ServerLevel.class);
        final BotSettings settings = new BotSettings();
        final BotBattle battle = new BotBattle(UUID.randomUUID(), mock(Battleground.class), settings, Map.of());
        final BotSquad squad = battle.newSquad("red", BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, "");
        final EntityNPCInterface first;
        final EntityNPCInterface second;
        final Entity vehicle = mock(Entity.class);
        final Entity driver = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        final ISeat driverSeat = (ISeat) driver;

        Fixture()
        {
            first = soldier();
            second = soldier();
            squad.setTask(new SquadTask(SquadTask.Kind.ATTACK, new Vec3(100D, 0D, 0D), 4D));
            when(driverSeat.getDriveable()).thenReturn(vehicle);
            when(driverSeat.isDriverSeat()).thenReturn(true);
            when(vehicle.level()).thenReturn(level);
            when(vehicle.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 4D, 4D, 4D));
            when(level.getEntities(eq(vehicle), any(AABB.class), any())).thenReturn(List.of(driver));
        }

        private EntityNPCInterface soldier()
        {
            EntityNPCInterface npc = mock(EntityNPCInterface.class);
            UUID id = UUID.randomUUID();
            when(npc.getUUID()).thenReturn(id);
            when(npc.level()).thenReturn(level);
            when(npc.position()).thenReturn(Vec3.ZERO);
            when(npc.isAlive()).thenReturn(true);
            when(level.getEntity(id)).thenReturn(npc);
            BotSlot slot = battle.newSlot("red", BotSlot.Source.INFANTRY, -1);
            slot.setEntity(id);
            slot.setSquad(squad.getId());
            squad.getSlots().add(slot.getId());
            battle.getSlotOfEntity().put(id, slot.getId());
            return npc;
        }
    }
}
