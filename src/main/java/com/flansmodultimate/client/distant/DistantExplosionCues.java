package com.flansmodultimate.client.distant;

import com.flansmodultimate.network.client.PacketDistantExplosion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Large explosions too far away for their particles, shown as a flash, a fireball and a rising smoke column
 * built from boxes, so a battle on the horizon can be seen. Drawn by the far-terrain renderer beyond the
 * vanilla chunks and by {@link DistantBoxRenderer} within them. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantExplosionCues
{
    /** Most explosions shown at once; the oldest makes way for a new one. */
    private static final int MAX_CUES = 24;
    private static final int SMOKE_PUFFS = 5;
    private static final int FLASH_TICKS = 4;
    private static final List<Cue> cues = new ArrayList<>();

    public static void accept(PacketDistantExplosion packet)
    {
        if (!DistantHorizonsClient.explosionsEnabled())
            return;
        if (cues.size() >= MAX_CUES)
            cues.remove(0).close();
        cues.add(new Cue(packet.getCenter(), packet.getExplosionRadius(), packet.isFiery()));
    }

    static void tick(Vec3 camera, IDistantTerrain terrain, boolean enabled, double handoff)
    {
        if (!enabled)
        {
            reset();
            return;
        }

        Iterator<Cue> iterator = cues.iterator();
        while (iterator.hasNext())
        {
            Cue cue = iterator.next();
            cue.age++;
            if (cue.age > cue.lifetime)
            {
                cue.close();
                iterator.remove();
            }
            else
                cue.update(terrain, camera, handoff);
        }
    }

    /** Calls {@code action} for every shape that {@link DistantBoxRenderer} draws this tick. */
    static void forEachNear(DistantBoxRenderer.ShapeConsumer action)
    {
        for (Cue cue : cues)
        {
            if (!cue.near)
                continue;
            action.accept(cue, cue.fire, DistantBoxStyle.GLOW);
            action.accept(cue, cue.smoke, DistantBoxStyle.SMOKE);
        }
    }

    public static void reset()
    {
        for (Cue cue : cues)
            cue.close();
        cues.clear();
    }

    /** A box of side {@code 2 * half} centred {@code height} above the origin; empty when {@code half} is 0. */
    private static DistantBox cube(double height, double half, int argb)
    {
        float h = (float) Math.max(0D, half);
        float y = (float) height;
        return new DistantBox(-h, y - h, -h, h, y + h, h, argb);
    }

    private static int argb(double alpha, double red, double green, double blue)
    {
        return (Mth.clamp((int) Math.round(alpha), 0, 255) << 24) | (Mth.clamp((int) Math.round(red), 0, 255) << 16)
            | (Mth.clamp((int) Math.round(green), 0, 255) << 8) | Mth.clamp((int) Math.round(blue), 0, 255);
    }

    private static final class Cue implements IDistantBoxGroup.Origin
    {
        private final Vec3 center;
        private final double radius;
        private final int fireTicks;
        private final int lifetime;
        private final boolean fiery;
        private int age;
        private List<DistantBox> fire = List.of();
        private List<DistantBox> smoke = List.of();
        @Nullable
        private IDistantBoxGroup fireGroup;
        @Nullable
        private IDistantBoxGroup smokeGroup;
        private boolean near;

        private Cue(Vec3 center, float explosionRadius, boolean fiery)
        {
            this.center = center;
            this.radius = Mth.clamp(Float.isFinite(explosionRadius) ? explosionRadius : 1F, 1F, 64F);
            this.fiery = fiery;
            this.fireTicks = fiery ? 80 : 30;
            // Bigger explosions raise smoke that lingers longer
            this.lifetime = Mth.clamp((int) (160 + radius * 24), 160, 600);
        }

        @Override
        public Vec3 at(float partialTick)
        {
            return center;
        }

        private void update(IDistantTerrain terrain, Vec3 camera, double handoff)
        {
            buildShapes();
            near = center.distanceTo(camera) < handoff;
            boolean far = !near && terrain.drawsBoxes();
            fireGroup = show(terrain, fireGroup, far && age < fireTicks, "explosion_fire", DistantBoxStyle.GLOW, fire);
            smokeGroup = show(terrain, smokeGroup, far, "explosion_smoke", DistantBoxStyle.SMOKE, smoke);
        }

        @Nullable
        private IDistantBoxGroup show(IDistantTerrain terrain, @Nullable IDistantBoxGroup group, boolean visible, String name,
                                      DistantBoxStyle style, List<DistantBox> boxes)
        {
            if (!visible)
            {
                if (group != null)
                    group.setActive(false);
                return group;
            }
            if (group != null && !group.isValid())
            {
                group.close();
                group = null;
            }
            if (group == null)
            {
                group = terrain.createGroup(name, style);
                if (group == null)
                    return null;
                group.setOrigin(this);
            }
            group.setBoxes(boxes);
            group.setActive(true);
            return group;
        }

        /** Shapes for the current age; the box counts never change, so the groups update in place. */
        private void buildShapes()
        {
            double flash = age < FLASH_TICKS ? 1D - (double) age / FLASH_TICKS : 0D;
            double heat = Math.min(1D, (double) age / fireTicks);
            double grow = Math.min(1D, age / 6D);
            double fireAlpha = age < fireTicks ? 235D * Math.pow(1D - heat, 0.6D) : 0D;
            fire = List.of(
                cube(radius * 0.2D, flash > 0D ? radius * 1.6D * (0.7D + 0.3D * flash) : 0D, argb(255D * flash, 255D, 246D, 227D)),
                cube(radius * (0.2D + 0.3D * heat), age < fireTicks ? radius * (0.6D + 0.5D * grow) : 0D,
                    argb(fireAlpha, Mth.lerp(heat, 255D, 150D), Mth.lerp(heat, 214D, 38D), Mth.lerp(heat, 110D, 12D))));

            double life = (double) age / lifetime;
            List<DistantBox> puffs = new ArrayList<>(SMOKE_PUFFS);
            for (int puff = 0; puff < SMOKE_PUFFS; puff++)
            {
                double puffAge = age - (2D + puff * 4D);
                if (puffAge < 0D)
                {
                    puffs.add(cube(0D, 0D, 0));
                    continue;
                }
                double rise = radius * (0.4D + 0.85D * puff) + puffAge * (0.02D * radius + 0.04D);
                double half = 0.5D * radius * (0.9D + 0.22D * puff) * (0.55D + 0.45D * Math.min(1D, puffAge / 30D)) * (1D + 0.6D * life);
                double alpha = 210D * Math.min(1D, puffAge / 8D) * Math.pow(1D - life, 1.2D);
                double grey = fiery ? 50D + 6D * puff : 92D + 7D * puff;
                puffs.add(cube(rise, half, argb(alpha, grey, grey - 2D, grey - 4D)));
            }
            smoke = puffs;
        }

        private void close()
        {
            if (fireGroup != null)
                fireGroup.close();
            if (smokeGroup != null)
                smokeGroup.close();
            fireGroup = null;
            smokeGroup = null;
        }
    }
}
