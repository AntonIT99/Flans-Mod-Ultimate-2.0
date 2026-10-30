package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Right-click targets at the seats' current world positions, including seats outside the NPC's central hitbox. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSeatInteraction
{
    /** Width of a seated rider's interaction target, in blocks. */
    private static final double HALF_WIDTH = 0.4D;
    /** Height above the seat that a seated rider occupies, in blocks. */
    private static final double HEIGHT = 1.2D;

    public record Hit(WeaponMount mount, Vec3 point)
    {}

    /** The nearest seat under the player's crosshair, within their entity reach and unobstructed by blocks or other entities. Either side. */
    public static Optional<Hit> pick(EntityNPCInterface vehicle, Player player)
    {
        double reach = NpcPlatform.entityInteractionRange(player);
        if (reach <= 0D)
            return Optional.empty();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        Vec3 unobstructed = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getLocation();
        return hit(vehicle, start, unobstructed).filter(hit -> ProjectileUtil.getEntityHitResult(player, start, hit.point(), new AABB(start, hit.point()).inflate(1D),
            entity -> entity != vehicle && entity.isPickable() && !entity.isSpectator(), start.distanceToSqr(hit.point())) == null);
    }

    /** Traces the seat targets along a world-space segment; occupancy is checked separately so occupied seats cannot be clicked through. */
    static Optional<Hit> hit(EntityNPCInterface vehicle, Vec3 start, Vec3 end)
    {
        NpcVehicleState state = NpcVehicleState.of(vehicle);
        Hit nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (WeaponMount mount : state.crewSeats(vehicle))
        {
            Optional<Vec3> offset = state.seatOffset(vehicle, mount);
            if (offset.isEmpty())
                continue;
            Vec3 seat = vehicle.position().add(offset.get());
            AABB box = new AABB(seat.x - HALF_WIDTH, seat.y - 0.2D, seat.z - HALF_WIDTH, seat.x + HALF_WIDTH, seat.y + HEIGHT, seat.z + HALF_WIDTH);
            Optional<Vec3> point = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (point.isPresent() && start.distanceToSqr(point.get()) < distance)
            {
                nearest = new Hit(mount, point.get());
                distance = start.distanceToSqr(point.get());
            }
        }
        return Optional.ofNullable(nearest);
    }
}
