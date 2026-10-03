package com.flansmodultimate.network.client;

import com.flansmodultimate.client.distant.DistantExplosionCues;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A large explosion too far away for its particles to have been sent, for players who draw far terrain. The
 * client shows it as a flash, a fireball and a smoke column.
 */
@Getter
@NoArgsConstructor
public class PacketDistantExplosion implements IClientPacket
{
    private Vec3 center = Vec3.ZERO;
    /** Radius of the explosion's visuals, in blocks. */
    private float explosionRadius;
    /** Radius of its overpressure, in blocks; never smaller than {@link #explosionRadius}. */
    private float blastRadius;
    /** Whether it burns, which keeps its fireball alight longer. */
    private boolean fiery;

    public PacketDistantExplosion(Vec3 center, float explosionRadius, float blastRadius, boolean fiery)
    {
        this.center = center;
        this.explosionRadius = explosionRadius;
        this.blastRadius = blastRadius;
        this.fiery = fiery;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeDouble(center.x);
        data.writeDouble(center.y);
        data.writeDouble(center.z);
        data.writeFloat(explosionRadius);
        data.writeFloat(blastRadius);
        data.writeBoolean(fiery);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        center = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        explosionRadius = data.readFloat();
        blastRadius = data.readFloat();
        fiery = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        DistantExplosionCues.accept(this);
    }
}
