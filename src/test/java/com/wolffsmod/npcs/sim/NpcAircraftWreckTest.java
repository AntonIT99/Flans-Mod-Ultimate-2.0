package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCFlying;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcAircraftWreckTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"PLANE,false", "HELICOPTER,true", "AUTO,false", "AUTO,true", "NORMAL,false", "NORMAL,true"})
    void deadAircraftFallThroughTheDependenciesRealFlyingTravel(Flight flight, boolean helicopter) throws ReflectiveOperationException
    {
        EntityNPCFlying npc = npc(flight);
        when(npc.isKilled()).thenReturn(true);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        AtomicReference<Vec3> motion = new AtomicReference<>(Vec3.ZERO);
        AtomicReference<Vec3> position = new AtomicReference<>(new Vec3(0D, 80D, 0D));
        when(npc.getDeltaMovement()).thenAnswer(call -> motion.get());
        doAnswer(call ->
        {
            motion.set(call.getArgument(0));
            return null;
        }).when(npc).setDeltaMovement(any(Vec3.class));
        doAnswer(call ->
        {
            position.updateAndGet(value -> value.add(call.getArgument(1, Vec3.class)));
            return null;
        }).when(npc).move(eq(MoverType.SELF), any(Vec3.class));
        when(npc.canFly()).thenCallRealMethod();
        doCallRealMethod().when(npc).travel(any(Vec3.class));
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType type = mock(IDriveableType.class);
        when(model.getInfoType()).thenReturn(type);
        when(type.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        when(type.isHelicopter()).thenReturn(helicopter);
        try (var models = mockStatic(NpcModels.class); var physics = mockStatic(FlansModApi.class))
        {
            models.when(() -> NpcModels.flan(npc)).thenReturn(model);
            physics.when(() -> FlansModApi.getGravityFactor(level)).thenReturn(1D);
            NpcFlight controller = new NpcFlight();
            for (int tick = 0; tick < 20; tick++)
            {
                npc.travel(Vec3.ZERO);
                controller.tick(npc);
            }
            assertTrue(position.get().y < 72D, "The wreck must fall several blocks instead of hovering at its death position");
            assertTrue(motion.get().y < 0D);
            // An aircraft flight type comes down as an airframe, holding its fire; Normal keeps the plain gravity drop.
            boolean airframe = flight != Flight.NORMAL;
            assertEquals(airframe ? "CRASHING" : "NORMAL", controller.state(npc));
            assertEquals(airframe, controller.holdsFire());
            assertEquals(airframe, controller.isCrashing());
            // A dimension with gravity disabled still keeps a stationary wreck suspended.
            physics.when(() -> FlansModApi.getGravityFactor(level)).thenReturn(0D);
            motion.set(Vec3.ZERO);
            NpcFlight suspended = new NpcFlight();
            suspended.tick(npc);
            suspended.tick(npc);
            assertEquals(0D, motion.get().length(), 1.0E-9D);
        }
    }

    @ParameterizedTest
    @CsvSource({"PLANE,ground", "PLANE,wall", "PLANE,water", "HELICOPTER,ground"})
    void shotDownAircraftExplodeOnlyOnceAtImpact(Flight flight, String contact) throws ReflectiveOperationException
    {
        EntityNPCFlying npc = npc(flight);
        when(npc.isKilled()).thenReturn(true);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.getDeltaMovement()).thenReturn(new Vec3(1D, -1D, 0D));
        when(npc.position()).thenReturn(new Vec3(0D, 80D, 0D));
        try (var physics = mockStatic(FlansModApi.class); var effects = mockStatic(FlansAircraftCrashes.class, CALLS_REAL_METHODS))
        {
            physics.when(() -> FlansModApi.getGravityFactor(level)).thenReturn(1D);
            effects.when(() -> FlansAircraftCrashes.crash(eq(level), any(Vec3.class), anyFloat(), anyBoolean())).thenAnswer(call -> null);
            NpcFlight controller = new NpcFlight();
            controller.tick(npc);
            verify(level, never()).explode(any(), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyBoolean(), any(Level.ExplosionInteraction.class));
            when(npc.onGround()).thenReturn(contact.equals("ground"));
            when(npc.isInWater()).thenReturn(contact.equals("water"));
            npc.horizontalCollision = contact.equals("wall");
            controller.tick(npc);
            controller.tick(npc);
            verify(level, times(1)).explode(eq(npc), anyDouble(), anyDouble(), anyDouble(), eq(1.2F), eq(false), eq(Level.ExplosionInteraction.NONE));
        }
    }

    @Test
    void airborneTimeoutDoesNotExplode() throws ReflectiveOperationException
    {
        EntityNPCFlying npc = npc(Flight.PLANE);
        when(npc.isKilled()).thenReturn(true);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.getDeltaMovement()).thenReturn(Vec3.ZERO);
        try (var physics = mockStatic(FlansModApi.class))
        {
            NpcFlight controller = new NpcFlight();
            controller.tick(npc);
            var ticks = NpcFlight.class.getDeclaredField("crashTicks");
            ticks.setAccessible(true);
            ticks.setInt(controller, NpcFlight.MAX_CRASH_TICKS);
            controller.tick(npc);
            assertFalse(controller.isCrashing());
            verify(level, never()).explode(any(), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyBoolean(), any(Level.ExplosionInteraction.class));
        }
    }

    @Test
    void ordinaryFlyingNpcsKeepTheirOwnDeathPhysics() throws ReflectiveOperationException
    {
        EntityNPCFlying npc = npc(Flight.NORMAL);
        when(npc.isKilled()).thenReturn(true);
        try (var models = mockStatic(NpcModels.class))
        {
            new NpcFlight().tick(npc);
            verify(npc, never()).setDeltaMovement(any(Vec3.class));
        }
    }

    @SuppressWarnings("java:S3011") // Mockito skips construction; supply the dependency's final AI container.
    private static EntityNPCFlying npc(Flight flight) throws ReflectiveOperationException
    {
        EntityNPCFlying npc = mock(EntityNPCFlying.class);
        var ai = EntityNPCInterface.class.getField("ais");
        ai.setAccessible(true);
        ai.set(npc, new DataAI(npc));
        npc.ais.movementType = 1;
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.FLIGHT_TYPE, flight.ordinal());
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        return npc;
    }
}
