package com.flansmodultimate.network.client;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The spectacle of an aircraft going in, sent separately from the blast that
 * does the damage.
 *
 * <p>{@link com.flansmodultimate.common.FlanExplosion} ties its visuals to the
 * radius that breaks blocks and hurts entities, and it emits a single explosion
 * emitter at one point, which reads as a grenade rather than as a fuelled
 * airframe hitting the ground. This spreads emitters, flame, smoke and debris
 * across the whole wreck volume without touching what the blast is allowed to
 * damage.
 */
@NoArgsConstructor
public class PacketDriveableCrashFireball implements IClientPacket
{
    /** Bounds on what a single crash may cost a client in particles. */
    private static final int MAX_EMITTERS = 12;
    private static final int MAX_FLAMES = 140;
    private static final int MAX_SMOKE = 90;
    private static final int MAX_DEBRIS = 70;
    private static final float MAX_RADIUS = 24F;

    private Vec3 position;
    private float radius;

    public PacketDriveableCrashFireball(Vec3 position, float radius)
    {
        this.position = position;
        this.radius = radius;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeFloat(radius);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        radius = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!Float.isFinite(radius) || radius <= 0F)
            return;
        float size = Math.min(radius, MAX_RADIUS);
        RandomSource random = level.random;

        // Several emitters across the wreck rather than one at its centre: a
        // single emitter always looks the same size however big the crash was.
        int emitters = Mth.clamp(Mth.ceil(size * 0.8F), 3, MAX_EMITTERS);
        for (int i = 0; i < emitters; i++)
            spawn(level, FlanParticles.HUGE_EXPLOSION, offset(random, size * 0.45F), Vec3.ZERO, 1F);

        int flames = Math.min(MAX_FLAMES, emitters * 12);
        for (int i = 0; i < flames; i++)
            // Biased upwards so the fireball climbs the way burning fuel does.
            spawn(level, FlanParticles.FM_FLAME, offset(random, size * 0.5F),
                scatter(random, size * 0.05F, size * 0.09F), 1F);

        int smoke = Math.min(MAX_SMOKE, emitters * 8);
        for (int i = 0; i < smoke; i++)
            spawn(level, FlanParticles.FM_BIG_SMOKE, offset(random, size * 0.6F),
                scatter(random, size * 0.02F, size * 0.06F), 1F);

        int debris = Math.min(MAX_DEBRIS, emitters * 6);
        for (int i = 0; i < debris; i++)
            spawn(level, FlanParticles.FM_DEBRIS_1, offset(random, size * 0.3F),
                scatter(random, size * 0.12F, size * 0.16F), 1F);

        for (int i = 0; i < emitters; i++)
            spawn(level, FlanParticles.FM_FLARE, offset(random, size * 0.35F),
                scatter(random, size * 0.08F, size * 0.12F), 1F);
    }

    private Vec3 offset(RandomSource random, float spread)
    {
        return position.add((random.nextDouble() - 0.5D) * 2D * spread,
            random.nextDouble() * spread, (random.nextDouble() - 0.5D) * 2D * spread);
    }

    private static Vec3 scatter(RandomSource random, float horizontal, float vertical)
    {
        return new Vec3((random.nextDouble() - 0.5D) * 2D * horizontal,
            random.nextDouble() * vertical, (random.nextDouble() - 0.5D) * 2D * horizontal);
    }

    private static void spawn(Level level, String particleType, Vec3 at, Vec3 velocity, float scale)
    {
        ClientHooks.RENDER.spawnParticle(particleType, at.x, at.y, at.z,
            velocity.x, velocity.y, velocity.z, scale);
    }
}
