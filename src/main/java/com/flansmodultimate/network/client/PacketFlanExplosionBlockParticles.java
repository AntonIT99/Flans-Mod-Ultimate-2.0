package com.flansmodultimate.network.client;

import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@NoArgsConstructor
public class PacketFlanExplosionBlockParticles implements IClientPacket
{
    private Vec3 center;
    private float radius;
    private long[] blockPosLongs;

    public PacketFlanExplosionBlockParticles(Vec3 center, float radius, List<BlockPos> positions)
    {
        this.center = center;
        this.radius = radius;

        this.blockPosLongs = new long[positions.size()];
        for (int i = 0; i < positions.size(); i++)
            this.blockPosLongs[i] = positions.get(i).asLong();
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(center.x);
        data.writeDouble(center.y);
        data.writeDouble(center.z);
        data.writeFloat(radius);

        data.writeVarInt(blockPosLongs.length);
        for (long l : blockPosLongs)
            data.writeLong(l);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        center = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        radius = data.readFloat();

        int n = data.readVarInt();
        blockPosLongs = new long[n];
        for (int i = 0; i < n; i++)
            blockPosLongs[i] = data.readLong();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        for (long l : blockPosLongs)
        {
            BlockPos pos = BlockPos.of(l);
            spawnBlockBurst(level, center, pos, radius);
        }
    }

    private void spawnBlockBurst(Level level, Vec3 center, BlockPos pos, float radius)
    {
        double px = pos.getX() + level.random.nextDouble();
        double py = pos.getY() + level.random.nextDouble();
        double pz = pos.getZ() + level.random.nextDouble();

        double dx = px - center.x;
        double dy = py - center.y;
        double dz = pz - center.z;

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6)
            return;

        dx /= len; dy /= len; dz /= len;

        double scale = 0.5D / (len / radius + 0.1D);
        scale *= (level.random.nextDouble() * level.random.nextDouble() + 0.3D);

        double vx = dx * scale;
        double vy = dy * scale;
        double vz = dz * scale;

        level.addParticle(ParticleTypes.EXPLOSION, (px + center.x) / 2.0D, (py + center.y) / 2.0D, (pz + center.z) / 2.0D, vx, vy, vz);
        level.addParticle(ParticleTypes.SMOKE, px, py, pz, vx, vy, vz);
    }
}
