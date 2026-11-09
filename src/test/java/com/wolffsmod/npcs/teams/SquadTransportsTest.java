package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SquadTransportsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void pickupAndRepeatedDeploymentKeepTheSameSoldiersSlotsAndSpending()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            BotSlot original = fixture.battle.getSlots().values().iterator().next();
            original.setKills(3);
            original.setSpawns(1);
            fixture.battle.getTickets().put(0, 42);
            SquadTransports.tick(fixture.level, fixture.battle, 101L);
            assertEquals(SquadTransport.Phase.EMBARKED, fixture.squad.getTransport().getPhase());
            assertSame(fixture.vehicle, fixture.riding.get());
            assertEquals(0F, fixture.weight[0], "embarked soldiers cannot capture objectives");

            SquadTransports.request(fixture.player, fixture.squad.getId(), null, true);
            fixture.stopped = false;
            SquadTransports.tick(fixture.level, fixture.battle, 102L);
            assertSame(fixture.vehicle, fixture.riding.get(), "unloading waits for the driver to stop");
            fixture.stopped = true;
            fixture.exit = Optional.empty();
            SquadTransports.tick(fixture.level, fixture.battle, 103L);
            assertSame(fixture.vehicle, fixture.riding.get(), "blocked exits do not eject passengers");
            fixture.exit = Optional.of(new Vec3(5D, 64D, 0D));
            SquadTransports.tick(fixture.level, fixture.battle, 104L);
            assertNull(fixture.riding.get());
            assertNull(fixture.squad.getTransport());
            assertEquals(2F, fixture.weight[0], "on-foot capture strength is restored");
            fixture.pickup();
            SquadTransports.tick(fixture.level, fixture.battle, 105L);
            assertSame(fixture.vehicle, fixture.riding.get());
            assertEquals(1, fixture.battle.getSlots().size());
            assertSame(original, fixture.battle.getSlots().get(original.getId()));
            assertEquals(fixture.soldier.getUUID(), original.getEntity());
            assertEquals(3, original.getKills());
            assertEquals(1, original.getSpawns());
            assertEquals(42, fixture.battle.getTickets().get(0));
            verify(fixture.level, never()).addFreshEntity(any());
            verify(fixture.soldier, never()).delete();
        }
    }

    @Test
    void movingAwayBeforeBoardingDoesNotTeleportSoldiersAndCancellationReleasesThem()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            fixture.stopped = false;
            SquadTransports.tick(fixture.level, fixture.battle, 101L);
            assertNull(fixture.riding.get());
            assertTrue(SquadTransports.manages(fixture.soldier));
            SquadTransports.request(fixture.player, fixture.squad.getId(), null, true);
            SquadTransports.tick(fixture.level, fixture.battle, 102L);
            assertNull(fixture.squad.getTransport());
            assertFalse(SquadTransports.manages(fixture.soldier));
        }
    }

    @Test
    void anUnreachablePickupTimesOutWithoutSpawningOrKillingAnyone()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            fixture.stopped = false;
            SquadTransports.tick(fixture.level, fixture.battle, 1301L);
            assertNull(fixture.squad.getTransport());
            assertNull(fixture.riding.get());
            assertEquals(1, fixture.battle.getSlots().size());
            assertEquals(0, fixture.battle.getSlots().values().iterator().next().getDeaths());
        }
    }

    @Test
    void unloadedTransportWaitsButAConfirmedMissingTransportEventuallyReleasesItsAssignment()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            when(fixture.level.getEntity(fixture.vehicle.getUUID())).thenReturn(null);
            when(fixture.level.isLoaded(any())).thenReturn(false);
            SquadTransports.tick(fixture.level, fixture.battle, 5000L);
            assertNotNull(fixture.squad.getTransport());
            when(fixture.level.isLoaded(any())).thenReturn(true);
            SquadTransports.tick(fixture.level, fixture.battle, 5001L);
            assertNull(fixture.squad.getTransport());
            assertEquals(0, fixture.battle.getSlots().values().iterator().next().getDeaths());
        }
    }

    @Test
    void requestsRejectInsufficientCapacityEnemyVehiclesAndPlayersOnAnotherSide()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.seats.when(() -> SquadTransportSeats.seats(fixture.vehicle)).thenReturn(List.of());
            SquadTransports.request(fixture.player, fixture.squad.getId(), fixture.vehicle.getUUID(), false);
            assertNull(fixture.squad.getTransport());
            fixture.seats.when(() -> SquadTransportSeats.allowed(fixture.soldier, fixture.vehicle)).thenReturn(false);
            SquadTransports.request(fixture.player, fixture.squad.getId(), fixture.vehicle.getUUID(), false);
            assertNull(fixture.squad.getTransport());
            when(fixture.ground.sideOf(fixture.player)).thenReturn(1);
            SquadTransports.request(fixture.player, fixture.squad.getId(), fixture.vehicle.getUUID(), false);
            assertNull(fixture.squad.getTransport());
            assertNull(fixture.riding.get());
        }
    }

    @Test
    void separateSquadsCannotReserveTheSamePassengerSeat()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            BotSquad second = fixture.battle.newSquad("red", BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, "");
            second.setLeader(fixture.player.getUUID());
            BotSlot member = fixture.battle.newSlot("red", BotSlot.Source.INFANTRY, -1);
            member.setEntity(fixture.soldier.getUUID());
            member.setSquad(second.getId());
            second.getSlots().add(member.getId());
            SquadTransports.request(fixture.player, second.getId(), fixture.vehicle.getUUID(), false);
            assertNull(second.getTransport());
            assertNotNull(fixture.squad.getTransport());
        }
    }

    @Test
    void destroyedTransportAndBailoutRestoreCaptureWithoutCountingAnotherDeath()
    {
        try (Fixture fixture = new Fixture())
        {
            fixture.pickup();
            SquadTransports.tick(fixture.level, fixture.battle, 101L);
            fixture.riding.set(null); // The existing vehicle destruction policy bailed the passenger out.
            fixture.seats.when(() -> SquadTransportSeats.groundTransport(fixture.vehicle)).thenReturn(false);
            SquadTransports.tick(fixture.level, fixture.battle, 102L);
            assertNull(fixture.squad.getTransport());
            assertEquals(2F, fixture.weight[0]);
            assertEquals(0, fixture.battle.getSlots().values().iterator().next().getDeaths());
        }
    }

    private static final class Fixture implements AutoCloseable
    {
        final ServerLevel level = mock(ServerLevel.class);
        final MinecraftServer server = mock(MinecraftServer.class);
        final ServerPlayer player = mock(ServerPlayer.class);
        final Battleground ground = mock(Battleground.class);
        final ITeam team = mock(ITeam.class);
        final BotBattle battle = new BotBattle(UUID.randomUUID(), ground, new BotSettings(), Map.of());
        final BotSquad squad = battle.newSquad("red", BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, "");
        final Entity vehicle = mock(Entity.class);
        final EntityNPCInterface soldier = mock(EntityNPCInterface.class);
        final AtomicReference<Entity> riding = new AtomicReference<>();
        final float[] weight = {2F};
        final MockedStatic<SquadTransportSeats> seats = mockStatic(SquadTransportSeats.class);
        final MockedStatic<FlansTeams> teams = mockStatic(FlansTeams.class);
        final MockedStatic<NpcDrivers> drivers = mockStatic(NpcDrivers.class);
        boolean stopped = true;
        Optional<Vec3> exit = Optional.of(new Vec3(5D, 64D, 0D));

        Fixture()
        {
            UUID playerId = UUID.randomUUID();
            UUID vehicleId = UUID.randomUUID();
            UUID soldierId = UUID.randomUUID();
            PlayerList players = mock(PlayerList.class);
            when(level.getServer()).thenReturn(server);
            when(server.getPlayerList()).thenReturn(players);
            when(players.getPlayer(playerId)).thenReturn(player);
            when(player.getUUID()).thenReturn(playerId);
            when(player.level()).thenReturn(level);
            when(player.isAlive()).thenReturn(true);
            when(level.dimension()).thenReturn(Level.OVERWORLD);
            when(ground.dimension()).thenReturn(Level.OVERWORLD);
            when(ground.round()).thenReturn(Optional.empty());
            when(ground.team("red")).thenReturn(Optional.of(team));
            when(ground.sideOf(team)).thenReturn(0);
            when(ground.sideOf(player)).thenReturn(0);
            when(vehicle.getUUID()).thenReturn(vehicleId);
            when(vehicle.position()).thenReturn(new Vec3(0D, 64D, 0D));
            when(level.getEntity(vehicleId)).thenReturn(vehicle);
            when(soldier.getUUID()).thenReturn(soldierId);
            when(soldier.level()).thenReturn(level);
            when(soldier.getPersistentData()).thenReturn(new CompoundTag());
            PathNavigation navigation = mock(PathNavigation.class);
            when(soldier.getNavigation()).thenReturn(navigation);
            Vec3 vehiclePosition = vehicle.position();
            when(soldier.position()).thenReturn(vehiclePosition);
            when(soldier.isAlive()).thenReturn(true);
            when(soldier.getVehicle()).thenAnswer(call -> riding.get());
            when(soldier.isPassenger()).thenAnswer(call -> riding.get() != null);
            when(level.getEntity(soldierId)).thenReturn(soldier);
            when(level.getGameTime()).thenReturn(100L);
            BotSlot slot = battle.newSlot("red", BotSlot.Source.INFANTRY, -1);
            slot.setEntity(soldierId);
            slot.setSquad(squad.getId());
            squad.getSlots().add(slot.getId());
            squad.setLeader(playerId);
            battle.getSlotOfEntity().put(soldierId, slot.getId());
            teams.when(() -> FlansTeams.getTeam(soldier)).thenReturn(Optional.of(team));
            teams.when(() -> FlansTeams.getCaptureWeight(soldier)).thenAnswer(call -> weight[0]);
            teams.when(() -> FlansTeams.assignCombatant(eq(soldier), eq(team), anyFloat())).thenAnswer(call ->
            {
                weight[0] = call.getArgument(2);
                return true;
            });
            seats.when(() -> SquadTransportSeats.groundTransport(vehicle)).thenReturn(true);
            seats.when(() -> SquadTransportSeats.stopped(vehicle)).thenAnswer(call -> stopped);
            seats.when(() -> SquadTransportSeats.allowed(soldier, vehicle)).thenReturn(true);
            seats.when(() -> SquadTransportSeats.seats(vehicle)).thenAnswer(call -> List.of(new SquadTransportSeats.Seat(3, vehicle, vehicle.position(), riding.get() == null)));
            seats.when(() -> SquadTransportSeats.aboard(soldier, vehicle)).thenAnswer(call -> riding.get() == vehicle);
            seats.when(() -> SquadTransportSeats.board(eq(soldier), any())).thenAnswer(call ->
            {
                riding.set(vehicle);
                return true;
            });
            seats.when(() -> SquadTransportSeats.exit(level, vehicle, soldier)).thenAnswer(call -> exit);
            seats.when(() -> SquadTransportSeats.disembark(eq(soldier), any())).thenAnswer(call ->
            {
                riding.set(null);
                return null;
            });
            BotBattles.add(battle);
        }

        void pickup()
        {
            SquadTransports.request(player, squad.getId(), vehicle.getUUID(), false);
            assertNotNull(squad.getTransport());
        }

        @Override
        public void close()
        {
            BotBattles.remove(battle);
            drivers.close();
            teams.close();
            seats.close();
        }
    }
}
