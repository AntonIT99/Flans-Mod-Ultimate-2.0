package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.*;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.crew.*;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.*;

/** Passenger adapters for physical driveables and Custom NPC vehicle models. Server thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SquadTransportSeats
{
    record Seat(int index, Entity mount, Vec3 approach, boolean free)
    {}

    static boolean groundTransport(Entity vehicle)
    {
        return vehicle.isAlive() && !(vehicle instanceof EntityNPCInterface npc && npc.isKilled()) && !FlansEntityTypes.isWreck(vehicle)
            && NpcVehicleThreats.type(vehicle).map(type -> type.getDriveableKind() == DriveableKind.VEHICLE && !type.isShip()).orElse(false);
    }

    static boolean stopped(Entity vehicle)
    {
        return vehicle.onGround() && vehicle.getDeltaMovement().lengthSqr() <= 0.05D * 0.05D;
    }

    static List<Seat> seats(Entity vehicle)
    {
        List<Seat> seats = new ArrayList<>();
        if (vehicle instanceof EntityNPCInterface npc && NpcModels.flan(npc) != null)
        {
            for (NpcCrew.Seat seat : NpcCrew.seats(npc))
            {
                if (seat.post() == CrewPost.PASSENGER)
                {
                    Vec3 offset = NpcVehicleState.of(npc).seatOffset(npc, seat.mount()).orElse(Vec3.ZERO);
                    seats.add(new Seat(seat.index(), npc, approach(vehicle, offset, seat.index()), !NpcCrew.occupied(npc, seat.index())));
                }
            }
        }
        else
        {
            for (Entity entity : vehicle.level().getEntities(vehicle, vehicle.getBoundingBox().inflate(reach(vehicle) + 8D),
                entity -> entity instanceof ISeat seat && seat.getDriveable() == vehicle && !seat.isDriverSeat() && seat.getGunType().isEmpty()))
            {
                ISeat seat = (ISeat) entity;
                seats.add(new Seat(seat.getSeatIndex(), entity, approach(vehicle, entity.position().subtract(vehicle.position()), seat.getSeatIndex()), seat.getRiddenByEntity() == null));
            }
        }
        seats.sort(Comparator.comparingInt(Seat::index));
        return seats;
    }

    private static Vec3 approach(Entity vehicle, Vec3 offset, int index)
    {
        Vec3 side = new Vec3(offset.x, 0D, offset.z);
        if (side.lengthSqr() < 0.01D)
            side = Vec3.directionFromRotation(0F, vehicle.getYRot() + (index % 2 == 0 ? 90F : -90F));
        return vehicle.position().add(side.normalize().scale(reach(vehicle) + 1D));
    }

    static double reach(Entity vehicle)
    {
        double scale = vehicle instanceof EntityNPCInterface npc ? NpcModels.scale(npc) : 1D;
        return NpcVehicleThreats.type(vehicle).flatMap(FlansEntityTypes::getHullFootprint)
            .map(hull -> Math.hypot(hull.length(), hull.width()) * 0.5D * scale).orElse((double) vehicle.getBbWidth());
    }

    static boolean aboard(Entity soldier, Entity vehicle)
    {
        Entity mount = soldier.getVehicle();
        return mount == vehicle || mount instanceof ISeat seat && seat.getDriveable() == vehicle;
    }

    static boolean allowed(EntityNPCInterface soldier, Entity vehicle)
    {
        if (vehicle instanceof EntityNPCInterface npc)
        {
            if (!NpcDrivers.ally(soldier, npc))
                return false;
            return npc.getPassengers().stream().allMatch(passenger -> NpcDrivers.ally(soldier, passenger));
        }
        return NpcDrivers.transportPermitted(soldier, vehicle);
    }

    static boolean board(EntityNPCInterface soldier, Seat seat)
    {
        if (seat.mount() instanceof EntityNPCInterface vehicle)
            return !NpcCrew.occupied(vehicle, seat.index()) && NpcCrew.board(vehicle, soldier, seat.index());
        return NpcDrivers.boardTransport(soldier, seat.mount());
    }

    /** Tries supported, dry, collision-free exits without loading chunks or dropping soldiers through the hull. */
    static Optional<Vec3> exit(ServerLevel level, Entity vehicle, EntityNPCInterface soldier)
    {
        double radius = reach(vehicle) + 1.5D;
        for (int index = 0; index < 16; index++)
        {
            double angle = index * Math.PI / 8D;
            Vec3 side = vehicle.position().add(Math.cos(angle) * radius, 0D, Math.sin(angle) * radius);
            for (int dy : new int[] {0, 1, -1, 2, -2})
            {
                BlockPos feet = BlockPos.containing(side).offset(0, dy, 0);
                if (!level.hasChunkAt(feet) || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(feet).isEmpty())
                    continue;
                Vec3 spot = new Vec3(side.x, feet.getY(), side.z);
                double halfWidth = soldier.getBbWidth() * 0.5D;
                AABB bounds = new AABB(spot.x - halfWidth, spot.y, spot.z - halfWidth, spot.x + halfWidth, spot.y + Math.max(1.8D, soldier.getBbHeight()), spot.z + halfWidth);
                if (level.noCollision(soldier, bounds) && !bounds.intersects(vehicle.getBoundingBox())
                    && level.clip(new ClipContext(vehicle.position().add(0D, 1D, 0D), spot.add(0D, 1D, 0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, soldier)).getType() == HitResult.Type.MISS)
                    return Optional.of(spot);
            }
        }
        return Optional.empty();
    }

    static void disembark(EntityNPCInterface soldier, Vec3 spot)
    {
        if (soldier.getVehicle() instanceof ISeat)
            NpcDrivers.disembark(soldier);
        else
        {
            soldier.stopRiding();
            ((NpcCrewHolder) soldier).wolffsmodnpcsCrewSeat(NpcCrew.NO_SEAT);
        }
        soldier.teleportTo(spot.x, spot.y, spot.z);
        soldier.ais.setStartPos(BlockPos.containing(spot));
        soldier.getNavigation().stop();
    }
}
