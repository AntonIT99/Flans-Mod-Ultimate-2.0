package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.explosions.FlanExplosion;
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
 * The spectacle of an aircraft going in, sent separately from the blast that
 * does the damage.
 *
 * <p>
 * {@link FlanExplosion} ties its visuals to the
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

    private int driveableId = -1;
    private Vec3 position;
    private float radius;
    /** Whether it went into water, which throws up a splash and steam instead of a fireball. */
    private boolean water;

    public PacketDriveableCrashFireball(Vec3 position, float radius)
    {
        this.position = position;
        this.radius = radius;
    }

    public PacketDriveableCrashFireball(int driveableId, Vec3 position, float radius)
    {
        this(position, radius);
        this.driveableId = driveableId;
    }

    public PacketDriveableCrashFireball(int driveableId, Vec3 position, float radius, boolean water)
    {
        this(driveableId, position, radius);
        this.water = water;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(driveableId);
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeFloat(radius);
        data.writeBoolean(water);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        driveableId = data.readVarInt();
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        radius = data.readFloat();
        water = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!Float.isFinite(radius) || radius <= 0F)
            return;
        float size = Math.min(radius, MAX_RADIUS);
        RandomSource random = level.random;
        ClientHooks.SOUND.playDistantBlast(position, size);
        if (water)
        {
            ditch(random, size);
            return;
        }
        ClientHooks.RENDER.waterImpact(position, size);
        spawn(FlanParticles.FM_TANK_DEATH, position, Vec3.ZERO, Math.min(2F, size * 0.25F));
        // The ground where it went in stays burnt.
        if (level.getEntity(driveableId) instanceof Driveable owner)
            ClientHooks.RENDER.spawnScorchMark(position, size * 0.6F, owner);
        else if (driveableId < 0)
            ClientHooks.RENDER.spawnScorchMark(position, size * 0.6F);

        // Several emitters across the wreck rather than one at its centre: a
        // single emitter always looks the same size however big the crash was.
        int emitters = Mth.clamp(Mth.ceil(size * 0.8F), 3, MAX_EMITTERS);
        for (int i = 0; i < emitters; i++)
            spawn(FlanParticles.HUGE_EXPLOSION, offset(random, size * 0.45F), Vec3.ZERO, 1F);

        int flames = Math.min(MAX_FLAMES, emitters * 12);
        for (int i = 0; i < flames; i++)
            // Biased upwards so the fireball climbs the way burning fuel does.
            spawn(FlanParticles.FM_FLAME, offset(random, size * 0.5F), scatter(random, size * 0.05F, size * 0.09F), 1F);

        int smoke = Math.min(MAX_SMOKE, emitters * 8);
        for (int i = 0; i < smoke; i++)
            spawn(FlanParticles.LARGE_SMOKE, offset(random, size * 0.6F), scatter(random, size * 0.02F, size * 0.06F), 1F);

        int debris = Math.min(MAX_DEBRIS, emitters * 6);
        for (int i = 0; i < debris; i++)
            spawn(FlanParticles.FM_DEBRIS_1, offset(random, size * 0.3F), scatter(random, size * 0.12F, size * 0.16F), 1F);

        for (int i = 0; i < emitters; i++)
            spawn(FlanParticles.FM_FLARE, offset(random, size * 0.35F), scatter(random, size * 0.08F, size * 0.12F), 1F);
    }

    /** An aircraft going into the water: a column of spray, steam where the hot engine went under, and floating wreckage. */
    private void ditch(RandomSource random, float size)
    {
        ClientHooks.RENDER.waterImpact(position, size * 1.6F);
        int plumes = Mth.clamp(Mth.ceil(size * 2F), 4, 24);
        for (int i = 0; i < plumes; i++)
            spawn(FlanParticles.FM_WATER, offset(random, size * 0.4F), scatter(random, size * 0.03F, size * 0.12F), 1F);
        int steam = Math.min(MAX_SMOKE, plumes * 3);
        for (int i = 0; i < steam; i++)
            spawn(FlanParticles.CLOUD, offset(random, size * 0.5F), scatter(random, size * 0.01F, size * 0.04F), 1F);
        int debris = Math.min(MAX_DEBRIS, plumes * 2);
        for (int i = 0; i < debris; i++)
            spawn(FlanParticles.FM_DEBRIS_1, offset(random, size * 0.3F), scatter(random, size * 0.1F, size * 0.1F), 1F);
    }

    private Vec3 offset(RandomSource random, float spread)
    {
        return position.add((random.nextDouble() - 0.5D) * 2D * spread, random.nextDouble() * spread, (random.nextDouble() - 0.5D) * 2D * spread);
    }

    private static Vec3 scatter(RandomSource random, float horizontal, float vertical)
    {
        return new Vec3((random.nextDouble() - 0.5D) * 2D * horizontal, random.nextDouble() * vertical, (random.nextDouble() - 0.5D) * 2D * horizontal);
    }

    private static void spawn(String particleType, Vec3 at, Vec3 velocity, float scale)
    {
        ClientHooks.RENDER.spawnParticle(particleType, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z, scale);
    }
}
