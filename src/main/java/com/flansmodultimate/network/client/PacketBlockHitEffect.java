package com.flansmodultimate.network.client;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketBlockHitEffect implements IClientPacket
{
    private float x;
    private float y;
    private float z;

    private float motionX;
    private float motionY;
    private float motionZ;

    private int blockX;
    private int blockY;
    private int blockZ;

    private Direction facingDirection;

    private float explosionRadius;
    private float blockHitFXScale;
    private float bbWidth;

    public PacketBlockHitEffect(Vec3 hit, Vec3 motion, BlockPos position, Direction facingDirection, float explosionRadius, float blockHitFXScale, float bbWidth)
    {
        this((float) hit.x, (float) hit.y, (float) hit.z, (float) motion.x, (float) motion.y, (float) motion.z, position.getX(), position.getY(), position.getZ(), facingDirection, explosionRadius, blockHitFXScale, bbWidth);
    }

    public PacketBlockHitEffect(float x, float y, float z, float motionX, float motionY, float motionZ, int blockX, int blockY, int blockZ, Direction facingDirection, float explosionRadius, float blockHitFXScale, float bbWidth)
    {
        this.x = x;
        this.y = y;
        this.z = z;

        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;

        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;

        this.facingDirection = facingDirection;

        this.explosionRadius = explosionRadius;
        this.blockHitFXScale = blockHitFXScale;
        this.bbWidth = bbWidth;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);

        data.writeFloat(motionX);
        data.writeFloat(motionY);
        data.writeFloat(motionZ);

        data.writeInt(blockX);
        data.writeInt(blockY);
        data.writeInt(blockZ);

        data.writeEnum(facingDirection);

        data.writeFloat(explosionRadius);
        data.writeFloat(blockHitFXScale);
        data.writeFloat(bbWidth);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();

        motionX = data.readFloat();
        motionY = data.readFloat();
        motionZ = data.readFloat();

        blockX = data.readInt();
        blockY = data.readInt();
        blockZ = data.readInt();

        facingDirection = data.readEnum(Direction.class);

        explosionRadius = data.readFloat();
        blockHitFXScale = data.readFloat();
        bbWidth = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (explosionRadius > 30 || blockHitFXScale <= 0)
            return;

        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        BlockState state = level.getBlockState(pos);
        Vec3 motion = new Vec3(motionX, motionY, motionZ);

        double scalingFactor = ClientHooks.RENDER.hasFancyGraphics() ? 10.0 : 2.0;
        int numBlockParticles = (int)(Math.pow(explosionRadius + 1.0, 1.5) * scalingFactor + 20.0);
        double velocityFactor = Math.sqrt(explosionRadius + 1.0) * blockHitFXScale * 0.5;

        for (int i = 0; i < numBlockParticles; i++)
        {
            double px1 = x + (level.random.nextFloat() - 0.3) * bbWidth * 0.05;
            double py1 = y + (level.random.nextFloat() - 0.3) * bbWidth * 0.05;
            double pz1 = z + (level.random.nextFloat() - 0.3) * bbWidth * 0.05;

            double vx1 = -motion.x * (0.0011 + level.random.nextGaussian() * 0.008) * velocityFactor;
            double vy1 = Math.abs(0.305 + level.random.nextDouble() * 0.125) * velocityFactor;
            double vz1 = -motion.z * (0.0011 + level.random.nextGaussian() * 0.008) * velocityFactor;

            double px2 = x + (level.random.nextFloat() - 0.6) * bbWidth * 0.75;
            double py2 = y + (level.random.nextFloat() - 0.6) * bbWidth * 0.75;
            double pz2 = z + (level.random.nextFloat() - 0.6) * bbWidth * 0.75;

            double vx2 = -motion.x * (0.415 + level.random.nextGaussian() * 0.1) * velocityFactor;
            double vy2 = -motion.y * (0.425 + Math.abs(level.random.nextGaussian() * 0.1)) * velocityFactor;
            double vz2 = -motion.z * (0.415 + level.random.nextGaussian() * 0.1) * velocityFactor;

            ClientHooks.RENDER.spawnParticle(FlanParticles.BLOCK_DUST, state, pos, px1, py1, pz1, vx1, vy1, vz1, 1.0F);
            ClientHooks.RENDER.spawnParticle(FlanParticles.BLOCK_CRACK, state, pos, px2, py2, pz2, vx2, vy2, vz2, 1.0F);
        }
    }
}
