package com.wolffsmod.npcs.sim;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcRepairsTest
{
    private final EntityNPCInterface npc = mock(EntityNPCInterface.class);
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final AtomicLong time = new AtomicLong();
    private final NpcRepairs repairs = new NpcRepairs();
    private MockedStatic<CustomNpcsPlatform> platform;
    private final PartBody body = new PartBody(new VehicleType(), Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F)), Vec3.ZERO, Vec3.ZERO);

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void platform()
    {
        platform = mockStatic(CustomNpcsPlatform.class);
    }

    @AfterEach
    void closePlatform()
    {
        platform.close();
    }

    private void prepare()
    {
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(level.getGameTime()).thenAnswer(invocation -> time.get());
        when(npc.isAlive()).thenReturn(true);
        when(npc.getMaxHealth()).thenReturn(100F);
        when(npc.getDeltaMovement()).thenReturn(Vec3.ZERO);
        PathNavigation navigation = mock(PathNavigation.class);
        when(npc.getNavigation()).thenReturn(navigation);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        body.damage("core", 500F, false);
    }

    @Test
    void frozenNpcsDoNotAdvanceRepairSessions()
    {
        prepare();
        assertTrue(repairs.start(npc, body));
        platform.when(CustomNpcsPlatform::frozen).thenReturn(true);
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(npc, body);
        assertEquals(500F, body.getHealth());
        platform.when(CustomNpcsPlatform::frozen).thenReturn(false);
        for (int tick = 0; tick < 20; ++tick)
            repairs.tick(npc, body);
        assertEquals(515F, body.getHealth());
    }

    @Test
    void toolRepairUsesPartHpRatherThanNpcHealthAndUpdatesEntityHealth()
    {
        prepare();
        assertTrue(repairs.tool(npc, body, Vec3.ZERO, 100));
        assertEquals(550F, body.getHealth());
        verify(npc).setHealth(55F);
        verify(npc, never()).heal(anyFloat());
    }

    @Test
    void stationarySessionHealsFifteenPercentAndHoldsNavigation()
    {
        prepare();
        assertTrue(repairs.start(npc, body));
        for (int tick = 0; tick < 200; ++tick)
        {
            time.incrementAndGet();
            repairs.tick(npc, body);
        }
        assertEquals(650F, body.getHealth());
        verify(npc).setHealth(65F);
        verify(npc.getNavigation(), atLeastOnce()).stop();
    }

    @Test
    void damageInterruptsAutomaticHealingWithoutBlockingManualTools()
    {
        prepare();
        assertTrue(repairs.start(npc, body));
        repairs.damaged("core", 0L);
        for (int tick = 0; tick < 99; ++tick)
        {
            time.incrementAndGet();
            repairs.tick(npc, body);
        }
        assertEquals(500F, body.getHealth());
        assertTrue(repairs.tool(npc, body, Vec3.ZERO, 100));
        assertEquals(550F, body.getHealth());
        for (int tick = 0; tick < 20; ++tick)
        {
            time.incrementAndGet();
            repairs.tick(npc, body);
        }
        assertEquals(565F, body.getHealth());
    }

    @Test
    void rebuildingSwitchLeavesBrokenPartsOffWithoutDisablingSurvivingPartRepairs()
    {
        prepare();
        PartBody parts = new PartBody(new VehicleType(), Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F), EnumDriveablePart.LEFT_TRACK,
            CollisionBox.inWorldUnits(100F, 3F, 0F, 0F, 1F, 1F, 1F, 1F, 0F)), Vec3.ZERO, Vec3.ZERO);
        parts.damage("leftTrack", 100F, false);
        options.set(Feature.PART_REPAIR, false);
        assertFalse(repairs.tool(npc, parts, new Vec3(-4D, 0D, 0D), 100));
        assertTrue(parts.getPart("leftTrack").orElseThrow().destroyed());
        parts.damage("core", 100F, false);
        assertTrue(repairs.tool(npc, parts, new Vec3(-4D, 0D, 0D), 100));
        assertEquals(950F, parts.getPart("core").orElseThrow().health());
        assertTrue(parts.getPart("leftTrack").orElseThrow().destroyed());
        options.set(Feature.PART_REPAIR, true);
        assertTrue(repairs.tool(npc, parts, new Vec3(-4D, 0D, 0D), 100));
        assertEquals(5F, parts.getPart("leftTrack").orElseThrow().health());
    }

    @Test
    void killedNpcsCannotBeRevivedAndAutomaticRepairSwitchKeepsManualToolsAvailable()
    {
        prepare();
        options.set(Feature.AUTOMATIC_REPAIRS, false);
        options.set(Feature.PART_REPAIR, false);
        assertFalse(repairs.start(npc, body));
        assertTrue(repairs.tool(npc, body, Vec3.ZERO, 100));
        when(npc.isKilled()).thenReturn(true);
        assertFalse(repairs.restore(npc, body));
        assertEquals(550F, body.getHealth());
    }
}
