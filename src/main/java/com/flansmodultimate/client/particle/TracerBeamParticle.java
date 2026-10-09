package com.flansmodultimate.client.particle;

import com.flansmodultimate.client.render.thermal.HeatMaskQuads;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A glowing, untextured tracer beam between two points, from the Labjac Edition's {@code EntityTracerBeamFX}.
 *
 * <p>
 * The beam is six additive ribbons around its axis: a wide faint halo facing the camera and across it,
 * two diagonal mid layers, and a bright narrow core. It is drawn at full brightness and fades out over
 * {@value #BEAM_LIFETIME_TICKS} ticks, so the beams a round leaves every tick blend into one streak.
 * </p>
 */
public class TracerBeamParticle extends Particle
{
    static final int BEAM_LIFETIME_TICKS = 3;

    /** Untextured quads added onto the scene without writing depth, so beams glow through each other. */
    static final ParticleRenderType RENDER_TYPE = new ParticleRenderType()
    {
        @Override
        public BufferBuilder begin(@NotNull Tesselator tesselator, @NotNull TextureManager textureManager)
        {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.enableCull();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
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

    /** Colour from 0 to 1, opacity and core half-width in blocks of a beam. */
    record Style(float red, float green, float blue, float alpha, float width)
    {}

    private TracerBeamParticle(ClientLevel level, Vec3 start, Vec3 end, Style style)
    {
        super(level, end.x, end.y, end.z);
        this.start = start;
        this.end = end;
        this.red = style.red();
        this.green = style.green();
        this.blue = style.blue();
        this.beamAlpha = style.alpha();
        this.beamWidth = style.width();
        lifetime = BEAM_LIFETIME_TICKS;
        gravity = 0F;
        hasPhysics = false;
        // The whole beam, so it is not culled while only its tail is in view
        setBoundingBox(new AABB(start, end).inflate(style.width() * 3D));
    }

    /** Adds a beam to the client's particles. */
    public static void spawn(Vec3 start, Vec3 end, float red, float green, float blue, float alpha, float width)
    {
        Minecraft mc = Minecraft.getInstance();
        // Written to reject NaN as well as non-positive values
        boolean invisible = Float.isNaN(alpha) || alpha <= 0F || Float.isNaN(width) || width <= 0F;
        if (mc.level == null || invisible || start.distanceToSqr(end) < 1.0E-8D)
            return;
        mc.particleEngine.add(new TracerBeamParticle(mc.level, start, end, new Style(red, green, blue, alpha, width)));
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
        Vec3[] axes = axes(from, to);
        if (axes.length == 0)
            return;
        Vec3 side = axes[0];
        Vec3 up = axes[1];
        Vec3 diagonalA = side.add(up).normalize();
        Vec3 diagonalB = side.subtract(up).normalize();

        ribbon(buffer, from, to, side, beamWidth * 2.7F, alpha * 0.22F);
        ribbon(buffer, from, to, up, beamWidth * 2.7F, alpha * 0.18F);
        ribbon(buffer, from, to, diagonalA, beamWidth * 1.45F, alpha * 0.34F);
        ribbon(buffer, from, to, diagonalB, beamWidth * 1.45F, alpha * 0.34F);
        ribbon(buffer, from, to, side, beamWidth * 0.55F, alpha);
        ribbon(buffer, from, to, up, beamWidth * 0.55F, alpha * 0.9F);
    }

    /**
     * Draws the beam's core into the thermal heat mask, where tracers glow as in the Labjac Edition.
     *
     * @return whether anything was drawn
     */
    public boolean renderHeat(VertexConsumer mask, Matrix4f matrix, Vec3 cameraPos, Frustum frustum)
    {
        if (!frustum.isVisible(getBoundingBox()))
            return false;
        Vec3 from = start.subtract(cameraPos);
        Vec3 to = end.subtract(cameraPos);
        Vec3[] axes = axes(from, to);
        if (axes.length == 0)
            return false;
        heatRibbon(mask, matrix, from, to, axes[0]);
        heatRibbon(mask, matrix, from, to, axes[1]);
        return true;
    }

    private void heatRibbon(VertexConsumer mask, Matrix4f matrix, Vec3 from, Vec3 to, Vec3 across)
    {
        Vec3 offset = across.scale(beamWidth * 1.45F);
        HeatMaskQuads.quad(mask, matrix, vector(to.add(offset)), vector(to.subtract(offset)), vector(from.subtract(offset)), vector(from.add(offset)));
    }

    private static Vector3f vector(Vec3 value)
    {
        return new Vector3f((float) value.x, (float) value.y, (float) value.z);
    }

    /**
     * The beam's two axes across its length in camera-relative space, the first facing the camera at the origin,
     * or none for a degenerate beam.
     */
    private static Vec3[] axes(Vec3 from, Vec3 to)
    {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-8D)
            return new Vec3[0];
        direction = direction.normalize();
        Vec3 middle = from.add(to).scale(0.5D);
        Vec3 side = direction.cross(middle.scale(-1D));
        if (side.lengthSqr() < 1.0E-8D)
            side = new Vec3(-direction.z, 0D, direction.x);
        if (side.lengthSqr() < 1.0E-8D)
            side = new Vec3(1D, 0D, 0D);
        side = side.normalize();
        return new Vec3[]{side, direction.cross(side).normalize()};
    }

    private void ribbon(VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 across, float width, float alpha)
    {
        Vec3 offset = across.scale(width);
        vertex(buffer, to.add(offset), alpha);
        vertex(buffer, to.subtract(offset), alpha);
        vertex(buffer, from.subtract(offset), alpha);
        vertex(buffer, from.add(offset), alpha);
        // 1.21 particle render types have no end hook to restore culling; emit both windings.
        vertex(buffer, from.add(offset), alpha);
        vertex(buffer, from.subtract(offset), alpha);
        vertex(buffer, to.subtract(offset), alpha);
        vertex(buffer, to.add(offset), alpha);
    }

    private void vertex(VertexConsumer buffer, Vec3 position, float alpha)
    {
        buffer.addVertex((float) position.x, (float) position.y, (float) position.z).setColor(red, green, blue, alpha);
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return RENDER_TYPE;
    }
}
