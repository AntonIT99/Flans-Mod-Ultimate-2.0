package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlagpoleHeightTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void individualHeightSurvivesReloadAndUpdatesSyncedPickingBounds()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Flagpole pole = new Flagpole(EntityType.SNOWBALL, world.level);
            pole.setPos(4D, 64D, 8D);
            pole.setPoleHeight(12);
            assertEquals(12, pole.getPoleHeight());
            assertEquals(12D, pole.getBoundingBox().getYsize());
            assertEquals(64D, pole.getY());

            CompoundTag saved = new CompoundTag();
            pole.addAdditionalSaveData(saved);
            Flagpole restored = new Flagpole(EntityType.SNOWBALL, world.level);
            restored.readAdditionalSaveData(saved);
            assertEquals(12, restored.getPoleHeight());
            assertEquals(12D, restored.getBoundingBox().getYsize());

            // Tracking clients apply this data accessor update, rather than the operator's edit packet.
            var height = java.util.Objects.requireNonNull(pole.getEntityData().getNonDefaultValues()).stream().filter(value -> Integer.valueOf(12).equals(value.value())).findFirst().orElseThrow();
            restored.setPoleHeight(5);
            restored.getEntityData().assignValues(java.util.List.of(height));
            assertEquals(12D, restored.getBoundingBox().getYsize());
            pole.setPoleHeight(-100);
            assertEquals(2, pole.getPoleHeight());
            pole.setPoleHeight(Integer.MAX_VALUE);
            assertEquals(64, pole.getPoleHeight());
        }
    }

    @Test
    void newPolesAndOldSavesUseTheConfiguredDefault()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            when(ModCommonConfig.get().defaultFlagpoleHeight()).thenReturn(8);
            Flagpole pole = new Flagpole(EntityType.SNOWBALL, world.level);
            assertEquals(8, pole.getPoleHeight());
            // A nonstandard server default must travel in spawn data, even before config sync arrives.
            assertTrue(java.util.Objects.requireNonNull(pole.getEntityData().getNonDefaultValues()).stream().anyMatch(value -> Integer.valueOf(8).equals(value.value())));
            pole.setPoleHeight(15);
            pole.readAdditionalSaveData(new CompoundTag());
            assertEquals(8, pole.getPoleHeight());
        }
    }

    @Test
    void homeFlagFollowsPoleHeightButDroppedFlagsKeepTheirPosition()
    {
        try (EntityTestWorld world = new EntityTestWorld(); var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            Flagpole pole = new Flagpole(EntityType.SNOWBALL, world.level);
            pole.setPos(4D, 64D, 8D);
            when(manager.getBase(pole.getUUID())).thenReturn(Optional.of(pole));
            Flag flag = new Flag(EntityType.SNOWBALL, world.level);
            flag.setBaseId(pole.getUUID());

            pole.setPoleHeight(9);
            flag.resetToBase();
            assertEquals(72D, flag.getY());
            pole.setPoleHeight(15);
            world.tick(flag);
            assertEquals(78D, flag.getY());

            flag.drop(600);
            pole.setPoleHeight(5);
            world.tick(flag);
            assertEquals(78D, flag.getY());
            flag.resetToBase();
            assertEquals(68D, flag.getY());
        }
    }
}
