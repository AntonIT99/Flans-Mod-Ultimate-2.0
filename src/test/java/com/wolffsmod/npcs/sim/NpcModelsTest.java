package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.model.FlanModelShape;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcModelsTest
{
    /** Minecraft's sine table is good to a few ten-thousandths. */
    private static final double EPSILON = 1.0E-3D;
    private static final float DEGREES = 0.05F;

    @Test
    void groundVehiclesRotateAboutTheirOriginAndLiftAllWorldPoints()
    {
        withAttitude(20F, 15F, npc ->
        {
            NpcVehicleState state = NpcVehicleState.of(npc);
            when(state.getAttitudeLift()).thenReturn(-0.75F);
            FlanModelEntity model = mock(FlanModelEntity.class);
            FlanModelEntityType type = mock(FlanModelEntityType.class);
            when(model.getModelType()).thenReturn(type);
            when(type.getKind()).thenReturn(FlanModelKind.VEHICLE);
            when(type.getShape()).thenReturn(new FlanModelShape(EntityDimensions.scalable(4F, 3F), 0.5F));
            npc.display = mock(DataDisplay.class);
            when(npc.display.getSize()).thenReturn(10);
            npc.currentAnimation = -1;
            // Rotation does not move the authored origin. Lift is in world blocks, independent of model scale.
            assertVec(new Vec3(0D, 0.25D, 0D), NpcModels.local(npc, model, Vec3.ZERO, 180F));
            Vec3 point = new Vec3(1D, 0D, -2D);
            Vec3 expected = NpcModels.tilt(npc, point).scale(2D).add(0D, 0.25D, 0D);
            assertVec(expected, NpcModels.local(npc, model, point, 180F));
        });
    }

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

    @Test
    void aTurretCompensatesForGroundPitchAndBankToHitAWorldTarget()
    {
        for (float pitch : new float[]{-25F, 25F})
        {
            for (float bank : new float[]{-20F, 20F})
            {
                withAttitude(pitch, bank, npc ->
                {
                    for (float hull : new float[]{0F, 90F, 179F, -90F})
                    {
                        Vec3 target = Vec3.directionFromRotation(-5F, hull + 45F);
                        MountAim compensated = NpcModels.relative(npc, target, hull);
                        assertVec(target, NpcModels.direction(npc, compensated, hull));
                        assertTrue(MountMotion.error(MountAim.REST, compensated) > 1F);
                        // Keeping the level-ground aim would fire away from the target on this tilted hull.
                        Vec3 uncompensated = NpcModels.direction(npc, new MountAim(45F, -5F), hull);
                        assertTrue(target.distanceTo(uncompensated) > 0.05D);
                    }
                });
            }
        }
    }

    @Test
    void turningATiltedHullRequiresRecomputingElevationAsWellAsTraverse()
    {
        withAttitude(-20F, 15F, npc ->
        {
            Vec3 target = Vec3.directionFromRotation(0F, 60F);
            MountAim before = NpcModels.relative(npc, target, 0F);
            MountAim after = NpcModels.relative(npc, target, 30F);
            assertTrue(Math.abs(after.pitch() - before.pitch()) > 1F);
            assertVec(target, NpcModels.direction(npc, after, 30F));
            MountAim yawOnly = new MountAim(before.yaw() - 30F, before.pitch());
            assertTrue(target.distanceTo(NpcModels.direction(npc, yawOnly, 30F)) > 0.05D);
        });
    }
}
