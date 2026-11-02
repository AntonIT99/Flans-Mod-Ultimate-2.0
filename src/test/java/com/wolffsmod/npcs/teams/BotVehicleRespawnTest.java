package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotVehicleRespawnTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void successfulNpcSpawnBlocksTheNextVehicleUntilThePlateIsReadyAgain()
    {
        BotSettings settings = new BotSettings();
        settings.team("red").getVehicles().add(new BotSettings.VehicleEntry("tank", 2, 0, 0, 1, null, ""));
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("red");
        SpawnerNpcPolicy policy = mock(SpawnerNpcPolicy.class);
        when(policy.allows(any())).thenReturn(true);
        TeamsSpawner spawner = new TeamsSpawner(UUID.randomUUID(), TeamsSpawner.Kind.VEHICLE, Level.OVERWORLD, BlockPos.ZERO, null, team, policy);
        Battleground ground = mock(Battleground.class);
        when(ground.team("red")).thenReturn(Optional.of(team));
        when(ground.spawners()).thenReturn(List.of(spawner));
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of("red", 1));
        BotSlot first = battle.newSlot("red", BotSlot.Source.VEHICLE, 0);
        BotSlot second = battle.newSlot("red", BotSlot.Source.VEHICLE, 0);
        first.setRole("tank");
        second.setRole("tank");
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        FlanModelEntityType type = mock(FlanModelEntityType.class);
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.getUUID()).thenReturn(UUID.randomUUID());
        when(npc.position()).thenReturn(Vec3.ZERO);
        AtomicBoolean ready = new AtomicBoolean(true);
        try (var teams = mockStatic(FlansTeams.class);
            var models = mockStatic(FlanModelNpcs.class);
            var placement = mockStatic(BotSpawnPlacement.class, CALLS_REAL_METHODS);
            var factory = mockStatic(BotFactory.class);
            var crews = mockStatic(BotCrews.class))
        {
            teams.when(() -> FlansTeams.isVehicleSpawnerReady(spawner)).thenAnswer(call -> ready.get());
            teams.when(() -> FlansTeams.recordVehicleSpawnerSpawn(spawner)).thenAnswer(call ->
            {
                ready.set(false);
                return null;
            });
            models.when(() -> FlanModelNpcs.type("tank")).thenReturn(Optional.of(type));
            placement.when(() -> BotSpawnPlacement.vehicle(eq(level), eq(type), any(), anyFloat())).thenReturn(Optional.of(Vec3.ZERO));
            placement.when(() -> BotSpawnPlacement.recordSpawner(npc, spawner.id())).thenAnswer(call -> null);
            factory.when(() -> BotFactory.vehicle(eq(level), any(), anyFloat(), any())).thenReturn(Optional.of(npc));
            assertTrue(TeamsBots.spawnAt(level, battle, first, 100L, new Random(1), null).isPresent());
            assertFalse(ready.get());
            assertTrue(TeamsBots.spawnAt(level, battle, second, 100L, new Random(1), null).isEmpty());
            assertEquals(0, second.getSpawns());
            assertEquals(1, battle.getVehicleSpawns().get("red").get(0));
            teams.verify(() -> FlansTeams.recordVehicleSpawnerSpawn(spawner), times(1));

            // A failed placement must leave the plate free and charge neither tickets nor the vehicle budget.
            ready.set(true);
            placement.when(() -> BotSpawnPlacement.vehicle(eq(level), eq(type), any(), anyFloat())).thenReturn(Optional.empty());
            assertTrue(TeamsBots.spawnAt(level, battle, second, 200L, new Random(1), null).isEmpty());
            assertTrue(ready.get());
            teams.verify(() -> FlansTeams.recordVehicleSpawnerSpawn(spawner), times(1));
        }
    }
}
