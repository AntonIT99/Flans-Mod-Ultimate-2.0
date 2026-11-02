package com.flansmodultimate.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A stationary, upward-facing splatter, kept for 35 seconds and removed if its supporting block changes. */
public final class GroundBloodParticle extends TextureSheetParticle
{
    private final BlockPos support;
    private final BlockState supportState;
    private final float rotation;

    private GroundBloodParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites)
    {
        super(level, x, y, z);
        lifetime = 700;
        quadSize = 0.2F + random.nextFloat() * 0.4F;
        rotation = random.nextInt(4) * Mth.HALF_PI;
        support = BlockPos.containing(x, y - 0.02D, z);
        supportState = level.getBlockState(support);
        pickSprite(sprites);
    }

    @Override
    public void tick()
    {
        if (++age >= lifetime || level.getBlockState(support) != supportState || !level.getFluidState(support).isEmpty())
            remove();
        alpha = Mth.clamp((lifetime - age) / 40F, 0F, 1F);
    }

    @Override
    public void render(@NotNull VertexConsumer vertices, @NotNull Camera camera, float partialTick)
    {
        Vec3 relative = new Vec3(x, y, z).subtract(camera.getPosition());
        float sine = Mth.sin(rotation) * quadSize;
        float cosine = Mth.cos(rotation) * quadSize;
        int light = getLightColor(partialTick);
        vertex(vertices, relative.add(-cosine + sine, 0D, -sine - cosine), getU0(), getV0(), light);
        vertex(vertices, relative.add(-cosine - sine, 0D, -sine + cosine), getU0(), getV1(), light);
        vertex(vertices, relative.add(cosine - sine, 0D, sine + cosine), getU1(), getV1(), light);
        vertex(vertices, relative.add(cosine + sine, 0D, sine - cosine), getU1(), getV0(), light);
    }

    private void vertex(VertexConsumer vertices, Vec3 position, float u, float v, int light)
    {
        vertices.addVertex((float) position.x, (float) position.y, (float) position.z).setUv(u, v).setColor(rCol, gCol, bCol, alpha).setLight(light);
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new GroundBloodParticle(level, x, y, z, sprites);
        }
    }
}
