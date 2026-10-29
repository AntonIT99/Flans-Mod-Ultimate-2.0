package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NpcCrewAlignmentTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"0, 179, -179, -179, 181, 181", "1, 179, -179, -179, 181, 181", "2, 179, -179, -100, 181, 256", "2, -179, 179, 100, -181, -256"})
    void crewKeepTheirSeatBodyHeadingWithoutSpinningAcrossTheYawBoundary(int index, float previousBody, float facing, float look, float body, float head)
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        LivingEntity rider = mock(LivingEntity.class, withSettings().extraInterfaces(NpcCrewHolder.class));
        WeaponMount mount = new WeaponMount(index, index == 0, index == 1, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO,
            index == 1 ? Optional.of(mock(IContentType.class)) : Optional.empty());
        when(((NpcCrewHolder) rider).wolffsmodnpcsCrewSeat()).thenReturn(index);
        when(vehicle.getPassengers()).thenReturn(List.of(rider));
        when(vehicle.getYHeadRot()).thenReturn(look);
        when(vehicle.getXRot()).thenReturn(12F);
        when(rider.getYHeadRot()).thenReturn(look);
        rider.yBodyRot = previousBody;
        // Minecraft 1.21 places riders by their attachment point, which a mock leaves null.
        when(rider.getVehicleAttachmentPoint(vehicle)).thenReturn(Vec3.ZERO);
        NpcVehicleState state = mock(NpcVehicleState.class);
        when(state.crewSeats(vehicle)).thenReturn(List.of(mount));
        when(state.seatOffset(vehicle, mount)).thenReturn(Optional.of(Vec3.ZERO));
        when(state.seatFrameYaw(mount)).thenReturn(facing);
        when(state.seatBodyYaw(mount)).thenReturn(facing);
        when(state.seatFacing(mount)).thenReturn(look);
        when(state.seatAim(index)).thenReturn(new MountAim(0F, -18F));
        Entity.MoveFunction move = mock(Entity.MoveFunction.class);
        try (var states = mockStatic(NpcVehicleState.class))
        {
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            assertTrue(NpcCrew.positionRider(vehicle, rider, move));
            verify(rider).setYBodyRot(body);
            verify(rider).setYHeadRot(head);
            verify(rider).setYRot(head);
            if (index == 0)
                verify(rider).setXRot(12F);
            else if (index == 1)
                verify(rider).setXRot(-18F);
            else
                verify(rider, never()).setXRot(anyFloat());
        }
    }
}
