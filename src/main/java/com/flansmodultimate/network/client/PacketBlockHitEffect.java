package com.flansmodultimate.network.client;

import com.flansmodultimate.client.render.ParticleHelper;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
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
    public void encodeInto(FriendlyByteBuf data)
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
    public void decodeInto(FriendlyByteBuf data)
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
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        BlockState state = level.getBlockState(pos);
        Vec3i facingDir = facingDirection.getNormal();
        Vec3 motion = new Vec3(motionX, motionY, motionZ);
        Vec3 direction = motion.normalize();

        // Default Hit Particles
        for (int i = 0; i < 2; i++)
        {
            double scale = level.random.nextGaussian() * 0.1 + 0.5;

            double vx = facingDir.getX() * scale + level.random.nextGaussian() * 0.025;
            double vy = facingDir.getY() * scale + level.random.nextGaussian() * 0.025;
            double vz = facingDir.getZ() * scale + level.random.nextGaussian() * 0.025;

            vx += direction.x * 0.25;
            vy += direction.y * 0.25;
            vz += direction.z * 0.25;

            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, vx, vy, vz);
        }

        double scale = level.random.nextGaussian() * 0.05 + 0.05;
        double vx = facingDir.getX() * scale + level.random.nextGaussian() * 0.025;
        double vy = facingDir.getY() * scale + level.random.nextGaussian() * 0.025;
        double vz = facingDir.getZ() * scale + level.random.nextGaussian() * 0.025;

        level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);

        // Flan's Mod Ultimate Special Particles
        if (explosionRadius > 30 || blockHitFXScale <= 0)
            return;

        GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
        double scalingFactor = (graphics == GraphicsStatus.FAST) ? 2.0 : 10.0;
        int numBlockParticles = (int) Math.round(Math.pow(explosionRadius + 1.0, 1.5) * scalingFactor + 20.0);
        double velocityFactor = Math.sqrt(explosionRadius + 1.0) * blockHitFXScale * 0.5D;
        int blockId = Block.getId(state);

        for (int i = 0; i < numBlockParticles; i++)
        {
            double px1 = x + (level.random.nextDouble() - 0.3) * bbWidth * 0.05;
            double py1 = y + (level.random.nextDouble() - 0.3) * bbWidth * 0.05;
            double pz1 = z + (level.random.nextDouble() - 0.3) * bbWidth * 0.05;

            double vx1 = -motion.x * (0.0011 + level.random.nextGaussian() * 0.008) * velocityFactor;
            double vy1 = Math.abs(0.305 + level.random.nextDouble() * 0.125) * velocityFactor;
            double vz1 = -motion.z * (0.0011 + level.random.nextGaussian() * 0.008) * velocityFactor;

            double px2 = x + (level.random.nextDouble() - 0.6) * bbWidth * 0.75;
            double py2 = y + (level.random.nextDouble() - 0.6) * bbWidth * 0.75;
            double pz2 =  + (level.random.nextDouble() - 0.6) * bbWidth * 0.75;

            double vx2 = -motion.x * (0.415 + level.random.nextGaussian() * 0.1) * velocityFactor;
            double vy2 = -motion.y * (0.425 + Math.abs(level.random.nextGaussian() * 0.1)) * velocityFactor;
            double vz2 = -motion.z * (0.415 + level.random.nextGaussian() * 0.1) * velocityFactor;

            ParticleHelper.spawnFromString(level, ParticleHelper.BLOCK_DUST + "_" + blockId, px1, py1, pz1, vx1, vy1, vz1);
            ParticleHelper.spawnFromString(level, ParticleHelper.BLOCK_CRACK + "_" + blockId, px2, py2, pz2, vx2, vy2, vz2);
        }
    }
}
