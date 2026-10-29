package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcModelsTest
{
    /** Minecraft's sine table is good to a few ten-thousandths. */
    private static final double EPSILON = 1.0E-3D;
    private static final float DEGREES = 0.05F;

    private static void assertVec(Vec3 expected, Vec3 actual)
    {
        assertEquals(expected.x, actual.x, EPSILON, () -> "x of " + actual);
        assertEquals(expected.y, actual.y, EPSILON, () -> "y of " + actual);
        assertEquals(expected.z, actual.z, EPSILON, () -> "z of " + actual);
    }

    /** Runs a check against an NPC whose model is pitched nose down and banked right by the given degrees. */
    private static void withAttitude(float pitch, float bank, java.util.function.Consumer<EntityNPCInterface> check)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        NpcVehicleState state = mock(NpcVehicleState.class);
        when(state.getAttitudePitch()).thenReturn(pitch);
        when(state.getAttitudeBank()).thenReturn(bank);
        try (MockedStatic<NpcVehicleState> states = mockStatic(NpcVehicleState.class))
        {
            states.when(() -> NpcVehicleState.of(npc)).thenReturn(state);
            check.accept(npc);
        }
    }

    @Test
    void onALevelHullAnAimPointsAsMinecraftWouldTurnIt()
    {
        withAttitude(0F, 0F, npc ->
        {
            for (float hull : new float[]{0F, 37F, -120F, 180F})
            {
                MountAim aim = new MountAim(25F, -15F);
                assertVec(MountMotion.direction(aim, hull), NpcModels.direction(npc, aim, hull));
                MountAim back = NpcModels.relative(npc, MountMotion.direction(aim, hull), hull);
                assertEquals(aim.yaw(), back.yaw(), DEGREES);
                assertEquals(aim.pitch(), back.pitch(), DEGREES);
            }
        });
    }

    @Test
    void aPitchedAndBankedHullCarriesItsGunsWithIt()
    {
        withAttitude(20F, 35F, npc ->
        {
            // A gun fixed along the hull of a plane diving at 20 degrees fires 20 degrees down, whatever its bank.
            Vec3 nose = NpcModels.direction(npc, MountAim.REST, 90F);
            assertVec(Vec3.directionFromRotation(20F, 90F), nose);
            // The aim relative to the tilted hull undoes the tilt.
            MountAim aim = new MountAim(-40F, 10F);
            MountAim back = NpcModels.relative(npc, NpcModels.direction(npc, aim, 90F), 90F);
            assertEquals(aim.yaw(), back.yaw(), DEGREES);
            assertEquals(aim.pitch(), back.pitch(), DEGREES);
            assertEquals(0F, MountMotion.error(MountAim.REST, NpcModels.relative(npc, nose, 90F)), DEGREES);
        });
    }
}
