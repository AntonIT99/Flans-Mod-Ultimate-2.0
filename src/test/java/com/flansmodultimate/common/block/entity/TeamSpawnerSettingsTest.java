package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamSpawnerSettingsTest
{
    @Test
    void npcOnlyAndNonVehicleTemplatesDoNotAdvertiseVehicleDeployment()
    {
        TeamSpawnerBlockEntity plate = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        assertFalse(plate.hasVehicleTemplate());
        plate.addTemplate(new ItemStack(Items.STONE));
        assertFalse(plate.hasVehicleTemplate());
        ItemStack template = mock(ItemStack.class);
        when(template.copy()).thenReturn(template);
        AAGunItem item = mock(AAGunItem.class);
        when(template.getItem()).thenReturn(item);
        when(template.getHoverName()).thenReturn(Component.literal("AA gun"));
        plate.addTemplate(template);
        assertTrue(plate.hasVehicleTemplate());
        assertEquals(Component.literal("AA gun"), plate.getVehicleTemplateName());
        TeamSpawnerBlockEntity playerPlate = spawner(TeamSpawnerBlockEntity.Mode.PLAYER);
        playerPlate.addTemplate(template);
        assertFalse(playerPlate.hasVehicleTemplate());
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static TeamSpawnerBlockEntity spawner(TeamSpawnerBlockEntity.Mode mode)
    {
        // 1.21 block entities validate their state against the type.
        BlockEntityType<?> type = mock(BlockEntityType.class);
        when(type.isValid(any())).thenReturn(true);
        return new TeamSpawnerBlockEntity(type, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), mode);
    }

    @Test
    void npcOnlyPlateCountsDownAndSuccessfulSpawnsShareAPersistedCooldown()
    {
        TeamSpawnerBlockEntity plate = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(1L);
        assertFalse(plate.isSpawnReady());
        for (int tick = 0; tick < 10; tick++)
            TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, plate.getBlockState(), plate);
        assertTrue(plate.isSpawnReady());
        plate.setSpawnDelaySeconds(2);
        plate.recordSpawn();
        assertFalse(plate.isSpawnReady());
        for (int tick = 0; tick < 25; tick++)
            TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, plate.getBlockState(), plate);

        CompoundTag saved = new CompoundTag();
        plate.saveData(saved, null);
        TeamSpawnerBlockEntity restored = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        restored.loadData(saved, null);
        assertFalse(restored.isSpawnReady());
        for (int tick = 0; tick < 14; tick++)
            TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, restored.getBlockState(), restored);
        assertFalse(restored.isSpawnReady());
        TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, restored.getBlockState(), restored);
        assertTrue(restored.isSpawnReady());
        // Readiness stays available until a spawn actually succeeds.
        TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, restored.getBlockState(), restored);
        assertTrue(restored.isSpawnReady());
    }

    @Test
    void mixedPlateStaysAvailableToNpcsWhenItsTemplateVehicleAlreadyExists()
    {
        TeamSpawnerBlockEntity plate = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        plate.addTemplate(new ItemStack(Items.STONE));
        plate.setVehicleSettings(false, SpawnerNpcPolicy.ALL);
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(1L);
        when(level.getEntities(isNull(Entity.class), any(AABB.class), any())).thenReturn(List.of(mock(Entity.class)));
        for (int tick = 0; tick < 10; tick++)
            TeamSpawnerBlockEntity.serverTick(level, BlockPos.ZERO, plate.getBlockState(), plate);
        verify(level).getEntities(isNull(Entity.class), any(AABB.class), any());
        assertTrue(plate.isSpawnReady(), "An occupied template allocation must not consume the NPC cooldown");
        plate.recordSpawn();
        assertFalse(plate.isSpawnReady(), "An NPC spawn on the mixed plate must start the shared cooldown");
    }

    @Test
    void connectingImmediatelySyncsTheDisplayColourWithoutAnActiveRound()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            UUID baseId = UUID.randomUUID();
            ITeamBase base = mock(ITeamBase.class);
            Team team = mock(Team.class);
            when(manager.getBase(baseId)).thenReturn(Optional.of(base));
            when(manager.getDisplayTeamForBase(base)).thenReturn(team);
            when(team.getTeamColour()).thenReturn(0x123456);
            for (TeamSpawnerBlockEntity.Mode mode : TeamSpawnerBlockEntity.Mode.values())
            {
                TeamSpawnerBlockEntity spawner = spawner(mode);
                ServerLevel level = mock(ServerLevel.class);
                spawner.setLevel(level);
                spawner.setBaseId(baseId);
                assertEquals(0x123456, spawner.getTeamColour());
                assertNull(spawner.getOwningTeam(), "Display colour must not grant gameplay ownership outside a round");
                verify(level, atLeastOnce()).sendBlockUpdated(BlockPos.ZERO, spawner.getBlockState(), spawner.getBlockState(), 3);
                CompoundTag tag = new CompoundTag();
                spawner.saveData(tag, null);
                TeamSpawnerBlockEntity client = spawner(mode);
                client.loadData(tag, null);
                assertEquals(0x123456, client.getTeamColour());
                spawner.setBaseId(null);
                assertEquals(TeamSpawnerBlockEntity.UNOWNED_COLOUR, spawner.getTeamColour());
            }
        }
    }

    @Test
    void oldAndNewVehicleSpawnersDefaultToAllAndSequentialSurvivesSaving()
    {
        TeamSpawnerBlockEntity spawner = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        assertTrue(spawner.isNpcSpawnAll());
        CompoundTag old = new CompoundTag();
        old.putString("mode", "VEHICLE");
        spawner.loadData(old, null);
        assertTrue(spawner.isNpcSpawnAll());
        spawner.setNpcSpawnAll(false);
        CompoundTag saved = new CompoundTag();
        spawner.saveData(saved, null);
        TeamSpawnerBlockEntity restored = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        restored.loadData(saved, null);
        assertFalse(restored.isNpcSpawnAll());
    }
}
