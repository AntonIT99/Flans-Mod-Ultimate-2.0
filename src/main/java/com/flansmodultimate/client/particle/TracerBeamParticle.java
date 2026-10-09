package com.flansmodultimate.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A glowing, untextured tracer beam between two points, from the Labjac Edition's {@code EntityTracerBeamFX}.
 *
 * <p>
 * The beam is six additive ribbons around its axis: a wide faint halo facing the camera and across it,
 * two diagonal mid layers, and a bright narrow core. It is drawn at full brightness and fades out over
 * {@value #LIFETIME} ticks, so the beams a round leaves every tick blend into one streak.
 * </p>
 */
public class TracerBeamParticle extends Particle
{
    static final int LIFETIME = 3;

    /** Untextured quads added onto the scene without writing depth, so beams glow through each other. */
    static final ParticleRenderType RENDER_TYPE = new ParticleRenderType()
    {
        @Override
        public void begin(BufferBuilder builder, @NotNull TextureManager textureManager)
        {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public void end(Tesselator tesselator)
        {
            tesselator.end();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString()
        {
            return "FLAN_TRACER_BEAM";
        }
    };

    private final Vec3 start;
    private final Vec3 end;
    private final float red;
    private final float green;
    private final float blue;
    private final float beamAlpha;
    private final float beamWidth;

    protected TracerBeamParticle(ClientLevel level, Vec3 start, Vec3 end, float red, float green, float blue, float alpha, float width)
    {
        super(level, end.x, end.y, end.z);
        this.start = start;
        this.end = end;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.beamAlpha = alpha;
        this.beamWidth = width;
        lifetime = LIFETIME;
        gravity = 0F;
        hasPhysics = false;
        // The whole beam, so it is not culled while only its tail is in view
        setBoundingBox(new AABB(start, end).inflate(width * 3D));
    }

    /** Adds a beam to the client's particles. */
    public static void spawn(Vec3 start, Vec3 end, float red, float green, float blue, float alpha, float width)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(alpha > 0F) || !(width > 0F) || start.distanceToSqr(end) < 1.0E-8D)
            return;
        mc.particleEngine.add(new TracerBeamParticle(mc.level, start, end, red, green, blue, alpha, width));
    }

    @Override
    public void tick()
    {
        if (age++ >= lifetime)
            remove();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, Camera camera, float partialTicks)
    {
        float life = 1F - (age + partialTicks) / lifetime;
        float alpha = beamAlpha * life;
        if (alpha <= 0F)
            return;

        Vec3 cameraPos = camera.getPosition();
        Vec3 from = start.subtract(cameraPos);
        Vec3 to = end.subtract(cameraPos);
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-8D)
            return;
        direction = direction.normalize();

        // Turn the first ribbon to face the camera, which sits at the origin of this camera-relative space
        Vec3 middle = from.add(to).scale(0.5D);
        Vec3 side = direction.cross(middle.scale(-1D));
        if (side.lengthSqr() < 1.0E-8D)
            side = new Vec3(-direction.z, 0D, direction.x);
        if (side.lengthSqr() < 1.0E-8D)
            side = new Vec3(1D, 0D, 0D);
        side = side.normalize();
        Vec3 up = direction.cross(side).normalize();
        Vec3 diagonalA = side.add(up).normalize();
        Vec3 diagonalB = side.subtract(up).normalize();

        ribbon(buffer, from, to, side, beamWidth * 2.7F, alpha * 0.22F);
        ribbon(buffer, from, to, up, beamWidth * 2.7F, alpha * 0.18F);
        ribbon(buffer, from, to, diagonalA, beamWidth * 1.45F, alpha * 0.34F);
        ribbon(buffer, from, to, diagonalB, beamWidth * 1.45F, alpha * 0.34F);
        ribbon(buffer, from, to, side, beamWidth * 0.55F, alpha);
        ribbon(buffer, from, to, up, beamWidth * 0.55F, alpha * 0.9F);
    }

    private void ribbon(VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 across, float width, float alpha)
    {
        Vec3 offset = across.scale(width);
        vertex(buffer, to.add(offset), alpha);
        vertex(buffer, to.subtract(offset), alpha);
        vertex(buffer, from.subtract(offset), alpha);
        vertex(buffer, from.add(offset), alpha);
    }

    private void vertex(VertexConsumer buffer, Vec3 position, float alpha)
    {
        buffer.vertex(position.x, position.y, position.z).color(red, green, blue, alpha).endVertex();
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return RENDER_TYPE;
    }
}
