package com.wolffsarmormod.network;

import com.flansmod.common.vector.Vector3f;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

@NoArgsConstructor
public class PacketBlockHitEffect extends PacketBase
{
    private float x;
    private float y;
    private float z;

    private float directionX;
    private float directionY;
    private float directionZ;

    private int blockX;
    private int blockY;
    private int blockZ;

    private Direction facingDirection;

    public PacketBlockHitEffect(Vector3f hit, Vector3f direction, BlockPos position, Direction facingDirection)
    {
        this(hit.x, hit.y, hit.z, direction.x, direction.y, direction.z, position.getX(), position.getY(), position.getZ(), facingDirection);
    }

    public PacketBlockHitEffect(float x, float y, float z, float directionX, float directionY, float directionZ, int blockX, int blockY, int blockZ, Direction facingDirection)
    {
        this.x = x;
        this.y = y;
        this.z = z;

        this.directionX = directionX;
        this.directionY = directionY;
        this.directionZ = directionZ;

        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;

        this.facingDirection = facingDirection;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);

        data.writeFloat(directionX);
        data.writeFloat(directionY);
        data.writeFloat(directionZ);

        data.writeInt(blockX);
        data.writeInt(blockY);
        data.writeInt(blockZ);

        data.writeEnum(facingDirection);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();

        directionX = data.readFloat();
        directionY = data.readFloat();
        directionZ = data.readFloat();

        blockX = data.readInt();
        blockY = data.readInt();
        blockZ = data.readInt();

        facingDirection = data.readEnum(Direction.class);
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        BlockState state = level.getBlockState(pos);
        Vec3i facingDir = facingDirection.getNormal();

        for (int i = 0; i < 2; i++)
        {
            // TODO: [1.12] Check why this isn't moving right
            double scale = level.random.nextGaussian() * 0.1 + 0.5;

            double motionX = facingDir.getX() * scale + level.random.nextGaussian() * 0.025;
            double motionY = facingDir.getY() * scale + level.random.nextGaussian() * 0.025;
            double motionZ = facingDir.getZ() * scale + level.random.nextGaussian() * 0.025;

            motionX += directionX;
            motionY += directionY;
            motionZ += directionZ;

            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, motionX, motionY, motionZ);
        }

        double scale = level.random.nextGaussian() * 0.05 + 0.05;
        double motionX = facingDir.getX() * scale + level.random.nextGaussian() * 0.025;
        double motionY = facingDir.getY() * scale + level.random.nextGaussian() * 0.025;
        double motionZ = facingDir.getZ() * scale + level.random.nextGaussian() * 0.025;

        level.addParticle(ParticleTypes.CLOUD, x, y, z, motionX, motionY, motionZ);
    }
}
