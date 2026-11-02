package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A wreck with nothing left to burn coming down on land: a burnt-out hull dropped off a bridge or thrown by a blast.
 *
 * <p>
 * {@link PacketDriveableCrashFireball} is the fuelled version of this, and reads as an airframe going in. A hull that
 * has already burnt out has no fireball left in it, so what is seen instead is the dust it throws up, the ground it
 * marks, and the noise several tons of scrap makes when it lands.
 * </p>
 */
@NoArgsConstructor
public class PacketDriveableImpact implements IClientPacket
{
    /** Bounds on what a single impact may cost a client in particles. */
    private static final int MAX_DUST = 60;
    private static final int MAX_DEBRIS = 24;
    private static final float MAX_RADIUS = 16F;

    private Vec3 position;
    private float radius;
    private float severity;

    public PacketDriveableImpact(Vec3 position, float radius, float severity)
    {
        this.position = position;
        this.radius = radius;
        this.severity = severity;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeFloat(radius);
        data.writeFloat(severity);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        radius = data.readFloat();
        severity = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!Float.isFinite(radius) || radius <= 0F || !Float.isFinite(severity) || severity <= 0F)
            return;
        float size = Math.min(radius, MAX_RADIUS);
        float force = Mth.clamp(severity, 0F, 1F);
        RandomSource random = level.random;

        // A dust skirt rolling outwards along the ground, not a cloud climbing like burning fuel.
        int dust = Mth.clamp(Mth.ceil(size * 4F * force), 4, MAX_DUST);
        for (int i = 0; i < dust; i++)
        {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double reach = 0.3D + random.nextDouble() * size * 0.5D;
            Vec3 at = position.add(Math.cos(angle) * reach, random.nextDouble() * 0.4D, Math.sin(angle) * reach);
            Vec3 drift = new Vec3(Math.cos(angle) * 0.06D * force, 0.01D + random.nextDouble() * 0.03D, Math.sin(angle) * 0.06D * force);
            ClientHooks.RENDER.spawnParticle(FlanParticles.LARGE_SMOKE, at.x, at.y, at.z, drift.x, drift.y, drift.z, 0.8F + random.nextFloat() * 0.6F);
        }

        int debris = Mth.clamp(Mth.ceil(size * 1.5F * force), 2, MAX_DEBRIS);
        for (int i = 0; i < debris; i++)
        {
            Vec3 at = position.add((random.nextDouble() - 0.5D) * size, random.nextDouble() * 0.5D, (random.nextDouble() - 0.5D) * size);
            ClientHooks.RENDER.spawnParticle(FlanParticles.FM_DEBRIS_1, at.x, at.y, at.z, (random.nextDouble() - 0.5D) * 0.12D, 0.05D + random.nextDouble() * 0.1D,
                (random.nextDouble() - 0.5D) * 0.12D, 1F);
        }

        // Only a bad landing leaves a mark; one that merely bumps down does not scar the ground.
        if (force > 0.4F)
            ClientHooks.RENDER.spawnScorchMark(position, size * 0.35F * force);
    }
}
