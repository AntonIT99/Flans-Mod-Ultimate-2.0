package com.flansmodultimate.mixin;

import com.flansmodultimate.api.driveables.IFlanSeatOccupant;
import com.flansmodultimate.common.driveables.physics.PlaneRiderRotation;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MountedRiderRenderingTest
{
    @SuppressWarnings("ReferenceToMixin") // Unit-test the callbacks directly; production never instantiates or references the mixin.
    private final LivingEntityNameTagRangeMixin hooks = new LivingEntityNameTagRangeMixin()
    {};

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyOccupantsWhoseSeatsOwnTheirBodyHeadingBypassTheLivingMountClamp() throws ReflectiveOperationException
    {
        LivingEntity mount = mock(LivingEntity.class);
        LivingEntity rider = mock(LivingEntity.class, withSettings().extraInterfaces(IFlanSeatOccupant.class));
        IFlanSeatOccupant occupant = (IFlanSeatOccupant) rider;
        when(rider.getVehicle()).thenReturn(mount);
        Method hook = hook("flansmodultimateBodyRotationMount", LivingEntity.class);
        assertSame(mount, hook.invoke(hooks, rider));
        when(occupant.flansmodultimateHasSeatBodyRotation()).thenReturn(true);
        assertNull(hook.invoke(hooks, rider));
        Player player = mock(Player.class);
        when(player.getVehicle()).thenReturn(mount);
        assertSame(mount, hook.invoke(hooks, player));
        when(rider.getVehicle()).thenReturn(null);
        assertNull(hook.invoke(hooks, rider));
    }

    @Test
    void aircraftAttitudeAppliesIdenticallyToNpcAndPlayerRiders() throws ReflectiveOperationException
    {
        Plane plane = mock(Plane.class);
        when(plane.getPrevYaw()).thenReturn(20F);
        when(plane.getYaw()).thenReturn(30F);
        when(plane.getEntityFacingYaw(25F)).thenReturn(25F);
        when(plane.getPrevPitch()).thenReturn(10F);
        when(plane.getPitch()).thenReturn(30F);
        when(plane.getPrevRoll()).thenReturn(-30F);
        when(plane.getRoll()).thenReturn(-50F);
        Seat seat = mock(Seat.class);
        when(seat.getDriveable()).thenReturn(plane);
        Method hook = hook("flansmodultimateRotateRiderWithPlane", LivingEntity.class, PoseStack.class, float.class, float.class, float.class, CallbackInfo.class);
        Vector3f expected = PlaneRiderRotation.at(plane, 0.5F, 60F).transform(new Vector3f(0F, 1F, 0F));
        for (LivingEntity rider : List.of(mock(LivingEntity.class), mock(Player.class)))
        {
            when(rider.getVehicle()).thenReturn(seat);
            PoseStack pose = new PoseStack();
            hook.invoke(hooks, rider, pose, 0F, 60F, 0.5F, new CallbackInfo("setupRotations", false));
            Vector3f up = pose.last().normal().transform(new Vector3f(0F, 1F, 0F));
            assertEquals(expected.x, up.x, 1.0E-5F);
            assertEquals(expected.y, up.y, 1.0E-5F);
            assertEquals(expected.z, up.z, 1.0E-5F);
        }
    }

    @Test
    void redirectedMountLookupBelongsToTheBodyClampAndLeavesBothSittingChecksAlone() throws IOException
    {
        // A change in vanilla's lookup order must not silently disable the sitting pose or leave an unsafe cast.
        try (var input = getClass().getResourceAsStream("/net/minecraft/client/renderer/entity/LivingEntityRenderer.class"))
        {
            assertNotNull(input);
            ClassNode renderer = new ClassNode();
            new ClassReader(input).accept(renderer, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            MethodNode render = renderer.methods.stream().filter(method -> method.name.equals("render") && method.desc.startsWith("(Lnet/minecraft/world/entity/LivingEntity;")).findFirst()
                .orElseThrow();
            List<MethodInsnNode> lookups = new ArrayList<>();
            for (var instruction : render.instructions)
                if (instruction instanceof MethodInsnNode call && call.name.equals("getVehicle"))
                    lookups.add(call);
            assertEquals(4, lookups.size());
            assertEquals(Opcodes.INSTANCEOF, lookups.get(2).getNext().getOpcode());
            assertEquals("net/minecraft/world/entity/LivingEntity", ((TypeInsnNode) lookups.get(2).getNext()).desc);
            assertEquals(Opcodes.CHECKCAST, lookups.get(3).getNext().getOpcode());
        }
    }

    @SuppressWarnings({"java:S3011", "ReferenceToMixin"}) // Exercise the actual mixin callbacks without starting the renderer or the game.
    private static Method hook(String name, Class<?>... parameters) throws NoSuchMethodException
    {
        Method method = LivingEntityNameTagRangeMixin.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }
}
