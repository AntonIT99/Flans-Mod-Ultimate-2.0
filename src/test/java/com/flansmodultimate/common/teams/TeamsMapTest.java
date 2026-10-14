package com.flansmodultimate.common.teams;

import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TeamsMapTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        // NeoForge loads optional feature flags from its discovery list during vanilla bootstrap.
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void boundsAreOrderedWhicheverCornersAreGiven()
    {
        TeamsMap.Bounds bounds = TeamsMap.Bounds.of(new BlockPos(10, 80, -5), new BlockPos(-10, 60, 5));
        assertEquals(new TeamsMap.Bounds(-10, 60, -5, 10, 80, 5), bounds);
        assertTrue(bounds.contains(new Vec3(0D, 70D, 0D)));
        assertTrue(bounds.contains(new Vec3(10D, 80D, 5D)));
        assertFalse(bounds.contains(new Vec3(10.5D, 70D, 0D)));
        assertFalse(bounds.contains(new Vec3(0D, 59.9D, 0D)));
    }

    @Test
    void everythingAddedForTheObjectiveModesSurvivesSaving()
    {
        TeamsMap map = new TeamsMap("Harbour", "The Harbour", Level.OVERWORLD);
        UUID flagpole = UUID.randomUUID();
        UUID capturePoint = UUID.randomUUID();
        map.addBase(flagpole, new BlockPos(1, 2, 3));
        map.addBase(capturePoint, new BlockPos(4, 5, 6));
        assertTrue(map.markCapturePoint(capturePoint, true));
        map.setBounds(TeamsMap.Bounds.of(new BlockPos(0, 0, 0), new BlockPos(100, 200, 300)));
        assertTrue(map.setDeployImage("Harbour_Map.png"));
        map.setDeployImageCrop(0.75F, 0.1F, 0.25F, 0.9F);
        map.replaceDeployLayout(List.of(new DeployLayoutEntry("hq_2", 0.1F, 0.5F), new DeployLayoutEntry("objective_" + capturePoint, 2F, -1F)));

        TeamsMap loaded = TeamsMap.load(map.save());
        assertEquals("harbour", loaded.getShortName());
        assertEquals(map.getBounds(), loaded.getBounds());
        assertEquals("harbour_map", loaded.getDeployImage());
        assertEquals(new TeamsMap.ImageCrop(0.25F, 0.1F, 0.75F, 0.9F), loaded.getDeployImageCrop());
        assertEquals(List.of(new DeployLayoutEntry("hq_2", 0.1F, 0.5F), new DeployLayoutEntry("objective_" + capturePoint, 1F, 0F)), loaded.getDeployLayout());
        assertTrue(loaded.hasCapturePoints());
        assertEquals(2, loaded.getBases().size());
    }

    @Test
    void mapsSavedBeforeTheObjectiveModesLoadWithDefaults()
    {
        CompoundTag old = new TeamsMap("old", "Old", Level.OVERWORLD).save();
        for (String key : List.of("bounds", "deploy_image", "deploy_uv", "deploy_layout"))
            old.remove(key);
        TeamsMap loaded = TeamsMap.load(old);
        assertNull(loaded.getBounds());
        assertEquals("", loaded.getDeployImage());
        assertEquals(TeamsMap.ImageCrop.FULL, loaded.getDeployImageCrop());
        assertTrue(loaded.getDeployLayout().isEmpty());
        assertFalse(loaded.hasCapturePoints());
    }

    @Test
    void imageNamesThatCouldLeaveTheImageFolderAreRefused()
    {
        TeamsMap map = new TeamsMap("map", "Map", Level.OVERWORLD);
        assertTrue(map.setDeployImage("city"));
        for (String unsafe : List.of("../secret", "a/b", "flansmod:city", "has space", "a.b"))
        {
            assertFalse(map.setDeployImage(unsafe), unsafe);
            assertEquals("city", map.getDeployImage());
        }
        assertTrue(map.setDeployImage("none"));
        assertEquals("", map.getDeployImage());
    }

    @Test
    void aCropNeverCollapsesToNothing()
    {
        TeamsMap.ImageCrop crop = TeamsMap.ImageCrop.normalised(1F, 0.5F, 1F, 0.5F);
        assertTrue(crop.maxU() - crop.minU() >= 0.0199F);
        assertTrue(crop.maxV() - crop.minV() >= 0.0199F);
        assertTrue(crop.maxU() <= 1.0001F);
    }

    @Test
    void onlyBasesOfTheMapCanBeMarkedAsCapturePointsAndRemovingOneForgetsIt()
    {
        TeamsMap map = new TeamsMap("map", "Map", Level.OVERWORLD);
        UUID point = UUID.randomUUID();
        assertFalse(map.markCapturePoint(point, true));
        map.addBase(point, BlockPos.ZERO);
        assertTrue(map.markCapturePoint(point, true));
        assertFalse(map.markCapturePoint(point, true));
        map.removeBase(point);
        assertFalse(map.hasCapturePoints());
    }

    @Test
    void aLayoutCannotGrowWithoutBoundAndDropsUnusableKeys()
    {
        TeamsMap map = new TeamsMap("map", "Map", Level.OVERWORLD);
        List<DeployLayoutEntry> entries = new ArrayList<>();
        entries.add(new DeployLayoutEntry("", 0.5F, 0.5F));
        entries.add(new DeployLayoutEntry("x".repeat(DeployLayoutEntry.MAX_KEY_LENGTH + 1), 0.5F, 0.5F));
        for (int i = 0; i < TeamsMap.MAX_DEPLOY_LAYOUT_ENTRIES + 10; i++)
            entries.add(new DeployLayoutEntry("objective_" + i, 0.5F, 0.5F));
        map.replaceDeployLayout(entries);
        assertEquals(TeamsMap.MAX_DEPLOY_LAYOUT_ENTRIES, map.getDeployLayout().size());
        assertEquals("objective_0", map.getDeployLayout().get(0).key());
    }
}
