package com.flansmodultimate.client.render.hud;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.driveable.PacketFireControlRange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lases for the driver of a vehicle with an adjusting-barrel fire control ({@code FCSMode AdjustingBarrel}) and hands the
 * range to the server, which raises the shots to the ballistic solution for it.
 *
 * <p>
 * The laser follows the first-person view every few ticks; in another camera the last range is kept. Only loaded terrain
 * and entities other than the driver's own vehicle are measured.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FireControlClient
{
    private static final int LASE_INTERVAL_TICKS = 5;
    /** Change in blocks below which a new range is not worth sending. */
    private static final double SEND_THRESHOLD = 1D;

    private static double range = -1D;
    private static double sentRange = -1D;
    private static int vehicleId = -1;
    private static int ticks;

    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        Seat seat = fireControlSeat();
        if (seat == null)
        {
            reset();
            return;
        }
        Vehicle vehicle = (Vehicle) seat.getDriveable();
        if (vehicle.getId() != vehicleId)
        {
            reset();
            vehicleId = vehicle.getId();
        }
        if (++ticks % LASE_INTERVAL_TICKS != 0 || mc.options.getCameraType() != CameraType.FIRST_PERSON)
            return;
        range = measure(seat, vehicle.getVehicleType().getFireControlMaxRange());
        boolean cleared = range < 0D && sentRange >= 0D;
        boolean changed = range >= 0D && (sentRange < 0D || Math.abs(range - sentRange) >= SEND_THRESHOLD);
        if (cleared || changed)
        {
            sentRange = range;
            PacketHandler.sendToServer(new PacketFireControlRange((float) range));
        }
    }

    /** @return the range the fire control works with, in blocks, or negative for none */
    public static double range()
    {
        return fireControlSeat() == null ? -1D : range;
    }

    public static void reset()
    {
        range = -1D;
        sentRange = -1D;
        vehicleId = -1;
        ticks = 0;
    }

    /** @return the driver seat of a vehicle with an adjusting-barrel fire control the local player sits in, or null */
    @Nullable
    public static Seat fireControlSeat()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Seat seat) || !seat.isDriverSeat() || !(seat.getDriveable() instanceof Vehicle vehicle))
            return null;
        VehicleType type = vehicle.getVehicleType();
        return type != null && type.isFireControlAdjustsBarrel() ? seat : null;
    }

    private static double measure(Seat seat, double maximum)
    {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        Vec3 direction = new Vec3(camera.getLookVector());
        double loaded = maximum;
        for (double distance = 0D; distance < maximum; distance += 16D)
        {
            if (!seat.level().hasChunkAt(BlockPos.containing(start.add(direction.scale(distance)))))
            {
                loaded = distance;
                break;
            }
        }
        Vec3 end = start.add(direction.scale(loaded));
        HitResult block = seat.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double nearest = block.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : start.distanceTo(block.getLocation());
        Entity own = seat.getDriveable();
        for (Entity entity : seat.level().getEntities(mc.player, new AABB(start, end).inflate(1D),
            candidate -> candidate.isPickable() && !(candidate instanceof Seat) && candidate != own && !candidate.isPassengerOfSameVehicle(seat)))
        {
            var hit = entity.getBoundingBox().clip(start, end);
            if (hit.isPresent())
                nearest = Math.min(nearest, start.distanceTo(hit.get()));
        }
        return Double.isFinite(nearest) ? nearest : -1D;
    }
}
