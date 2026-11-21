package com.flansmodultimate.client.render.effects;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Draws the trailing control wire of wire-guided missiles ({@code HasLine}), as the Labjac fork does.
 *
 * <p>
 * The wire is purely visual and lives on the client only: it records the path the missile flies from its first
 * seen position, lets laid cable sag to the ground after a moment, and fades out once the missile is gone.
 * Guidance never depends on it.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuidanceWireRenderer
{
    private static final double POINT_SPACING_SQUARED = 0.25D * 0.25D;
    private static final int MAX_POINTS = 96;
    private static final int FADE_TICKS = 40;
    /** Ticks a laid point hangs in the air before falling. */
    private static final int HANG_TICKS = 6;
    private static final double MAX_FALL = 24D;
    /** The wire leaves the missile slightly behind its centre. */
    private static final double REAR_OFFSET = 0.24D;
    private static final double MAX_RENDER_DISTANCE_SQUARED = 2048D * 2048D;
    private static final float SHADE = 0.12F;
    private static final float ALPHA = 0.85F;

    private static final Map<Integer, Wire> WIRES = new HashMap<>();
    private static int clientTicks;

    private static final class Wire
    {
        private final List<CablePoint> points = new ArrayList<>();
        private Bullet bullet;
        private int fadeTicks = -1;
    }

    private record CablePoint(Vec3 position, int laidTick, double groundY)
    {}

    /** Discovers new wire-guided missiles, extends live wires and ages fading ones. */
    public static void tick(ClientLevel level)
    {
        if (level == null)
        {
            reset();
            return;
        }
        clientTicks++;
        for (Entity entity : level.entitiesForRendering())
        {
            if (entity instanceof Bullet bullet && !WIRES.containsKey(bullet.getId()) && hasLine(bullet))
            {
                Wire wire = new Wire();
                wire.bullet = bullet;
                WIRES.put(bullet.getId(), wire);
            }
        }

        Iterator<Wire> iterator = WIRES.values().iterator();
        while (iterator.hasNext())
        {
            Wire wire = iterator.next();
            if (wire.fadeTicks < 0 && (wire.bullet.isRemoved() || wire.bullet.level() != level))
                wire.fadeTicks = 0;
            if (wire.fadeTicks >= 0)
            {
                if (++wire.fadeTicks > FADE_TICKS)
                    iterator.remove();
                continue;
            }
            lay(level, wire, wire.bullet.position());
        }
    }

    public static void reset()
    {
        WIRES.clear();
    }

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Camera camera, float partialTick)
    {
        if (WIRES.isEmpty())
            return;
        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        for (Wire wire : WIRES.values())
        {
            if (wire.points.isEmpty())
                continue;
            float alpha = ALPHA * (wire.fadeTicks < 0 ? 1F : Math.max(0F, 1F - (wire.fadeTicks + partialTick) / FADE_TICKS));
            if (alpha <= 0F || wire.points.get(0).position().distanceToSqr(cameraPosition) > MAX_RENDER_DISTANCE_SQUARED)
                continue;

            Vec3 previous = null;
            for (CablePoint point : wire.points)
            {
                Vec3 current = settled(point, partialTick).subtract(cameraPosition);
                if (previous != null)
                    addLine(pose, lines, previous, current, alpha);
                previous = current;
            }
            if (wire.fadeTicks < 0 && previous != null)
                addLine(pose, lines, previous, tip(wire.bullet, partialTick).subtract(cameraPosition), alpha);
        }
        buffer.endBatch(RenderType.lines());
    }

    private static boolean hasLine(Bullet bullet)
    {
        BulletType type = bullet.getConfigType();
        return type != null && type.isHasLine();
    }

    private static void lay(ClientLevel level, Wire wire, Vec3 position)
    {
        List<CablePoint> points = wire.points;
        if (!points.isEmpty() && points.get(points.size() - 1).position().distanceToSqr(position) < POINT_SPACING_SQUARED)
            return;
        points.add(new CablePoint(position, clientTicks, groundHeight(level, position)));
        if (points.size() > MAX_POINTS)
            simplify(points);
    }

    private static double groundHeight(ClientLevel level, Vec3 position)
    {
        BlockPos pos = BlockPos.containing(position);
        if (!level.hasChunkAt(pos))
            return Double.NEGATIVE_INFINITY;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) + 0.04D;
    }

    /** Laid cable hangs briefly, then falls until it rests on loaded terrain. */
    private static Vec3 settled(CablePoint point, float partialTick)
    {
        double age = clientTicks + partialTick - point.laidTick() - HANG_TICKS;
        Vec3 position = point.position();
        if (age <= 0D)
            return position;
        double fallen = Math.max(point.groundY(), position.y - Math.min(MAX_FALL, age * age * 0.045D));
        return new Vec3(position.x, Math.min(position.y, fallen), position.z);
    }

    private static Vec3 tip(Bullet bullet, float partialTick)
    {
        Vec3 position = bullet.getPosition(partialTick);
        Vec3 motion = bullet.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D)
            return position;
        return position.subtract(motion.normalize().scale(REAR_OFFSET));
    }

    /** Removes the least visible bend, keeping both endpoints and sharp turns. */
    private static void simplify(List<CablePoint> points)
    {
        int best = 1;
        double smallest = Double.POSITIVE_INFINITY;
        for (int i = 1; i < points.size() - 1; i++)
        {
            Vec3 a = points.get(i - 1).position();
            Vec3 b = points.get(i).position();
            Vec3 c = points.get(i + 1).position();
            Vec3 ac = c.subtract(a);
            double lengthSquared = ac.lengthSqr();
            double t = lengthSquared < 1.0E-6D ? 0D : Math.max(0D, Math.min(1D, b.subtract(a).dot(ac) / lengthSquared));
            double error = b.subtract(a.add(ac.scale(t))).lengthSqr();
            if (error < smallest)
            {
                smallest = error;
                best = i;
            }
        }
        points.remove(best);
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 from, Vec3 to, float alpha)
    {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-8D)
            return;
        Vec3 normal = delta.normalize();
        VertexPlatform.lineVertex(consumer, pose, (float) from.x, (float) from.y, (float) from.z, SHADE, SHADE, SHADE, alpha, (float) normal.x, (float) normal.y, (float) normal.z);
        VertexPlatform.lineVertex(consumer, pose, (float) to.x, (float) to.y, (float) to.z, SHADE, SHADE, SHADE, alpha, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
