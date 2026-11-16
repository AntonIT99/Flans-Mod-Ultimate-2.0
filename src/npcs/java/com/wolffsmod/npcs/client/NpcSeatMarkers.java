package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.debug.FlansDebugMarkers;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Marks the seats and weapons of vehicle NPCs in Flan's debug mode, as Flan's marks those of its driveables: the driver's
 * seat in blue, the others in magenta, each with a line straight up; every barrel of the main weapon bank in cyan and of the
 * secondary one in orange, and the muzzle of each seat gun in green, each with a line along the way it fires. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSeatMarkers
{
    /** Blocks from the player within which vehicles are marked. */
    private static final double RANGE = 64D;
    /** Client ticks a mark stays; marks are renewed each tick. */
    private static final int TICKS = 2;
    private static final Vec3 UP = new Vec3(0D, 2D, 0D);
    /** Blocks the barrel line of a seat gun reaches. */
    private static final double BARREL_LINE = 2D;
    private static final float[] DRIVER = {0F, 0.45F, 1F};
    private static final float[] SEAT = {1F, 0F, 1F};
    private static final float[] GUN = {0F, 1F, 0.25F};
    /** The main and secondary weapon banks, as Flan's marks the shoot points of its driveables. */
    private static final float[] PRIMARY = {0F, 1F, 1F};
    private static final float[] SECONDARY = {1F, 0.5F, 0F};

    /** Once a client tick. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!FlansDebugMarkers.isDebugMode() || minecraft.level == null || minecraft.player == null || minecraft.isPaused())
            return;
        for (Entity entity : minecraft.level.entitiesForRendering())
            if (entity instanceof EntityNPCInterface npc && npc.distanceToSqr(minecraft.player) <= RANGE * RANGE)
                mark(npc);
    }

    private static void mark(EntityNPCInterface npc)
    {
        NpcVehicleState state = NpcVehicleState.of(npc);
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        List<WeaponMount> seats = state.crewSeats(npc);
        if (definition != null && !seats.isEmpty())
        {
            markBank(npc, state, model, definition, false, PRIMARY);
            markBank(npc, state, model, definition, true, SECONDARY);
        }
        for (WeaponMount mount : seats)
        {
            Optional<Vec3> offset = state.seatOffset(npc, mount);
            if (offset.isPresent())
            {
                Vec3 seat = npc.position().add(offset.get());
                float[] colour = mount.driver() ? DRIVER : SEAT;
                FlansDebugMarkers.point(seat, TICKS, colour[0], colour[1], colour[2]);
                FlansDebugMarkers.vector(seat, UP, TICKS, colour[0], colour[1], colour[2]);
                if (definition != null && mount.gun().isPresent())
                    markGun(npc, state, model, definition, mount);
            }
        }
    }

    /** Marks every barrel of a weapon bank where the drawn model holds it, with a line along the way it fires. */
    private static void markBank(EntityNPCInterface npc, NpcVehicleState state, FlanModelEntity model, IContentType definition, boolean secondary, float[] colour)
    {
        List<WeaponMuzzle> muzzles = FlansProjectiles.getMuzzles(definition, secondary, 0, false, state.turretAim());
        if (muzzles.isEmpty())
            return;
        Vec3 direction = state.barrelDirection(npc, !FlansProjectiles.aimsWithTurret(definition, secondary)).scale(BARREL_LINE);
        for (WeaponMuzzle muzzle : muzzles)
        {
            Vec3 position = NpcModels.world(npc, model, muzzle.offset(), state.getHullYaw());
            FlansDebugMarkers.point(position, TICKS, colour[0], colour[1], colour[2]);
            FlansDebugMarkers.vector(position, direction, TICKS, colour[0], colour[1], colour[2]);
        }
    }

    private static void markGun(EntityNPCInterface npc, NpcVehicleState state, FlanModelEntity model, IContentType definition, WeaponMount mount)
    {
        FlansProjectiles.getSeatMuzzle(definition, mount.seat(), 0, state.turretAim(), state.seatAim(mount.seat())).map(muzzle -> NpcModels.world(npc, model, muzzle.offset(), state.getHullYaw()))
            .ifPresent(muzzle ->
            {
                FlansDebugMarkers.point(muzzle, TICKS, GUN[0], GUN[1], GUN[2]);
                FlansDebugMarkers.vector(muzzle, state.seatBarrelDirection(npc, mount).scale(BARREL_LINE), TICKS, GUN[0], GUN[1], GUN[2]);
            });
    }
}
