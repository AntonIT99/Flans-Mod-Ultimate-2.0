package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.debug.FlansDebugMarkers;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.WeaponMount;
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

import java.util.Optional;

/**
 * Marks the seats of vehicle NPCs in Flan's debug mode, as Flan's marks the seats of its driveables: the driver's in blue,
 * the others in magenta, each with a line straight up, and the muzzle of each seat gun in green with a line along its
 * barrel. Client side.
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
        for (WeaponMount mount : state.crewSeats(npc))
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
