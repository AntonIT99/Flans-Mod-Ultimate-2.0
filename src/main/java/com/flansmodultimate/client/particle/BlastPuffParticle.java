package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.physics.ModPhysics;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A soft puff whose colour, growth, drag, buoyancy and glow are chosen by whoever spawns it.
 * <p>
 * The legacy particles each hard-code one look, which is why an explosion built from them can only
 * ever be more or fewer of the same sprites. This one is a single white cloud sprite tinted and
 * shaped per spawn, so the same particle serves as a warm afterglow, a ground-hugging dust skirt, a
 * fireball cooling into smoke as it climbs and the dark cap of a heavy charge. It is only ever
 * created directly by {@link ExplosionSpectacle}; the registered type exists so the sprites load
 * through the normal particle atlas.
 */
public class BlastPuffParticle extends TextureSheetParticle
{
    /** Captured when the provider is registered; the set itself is refilled on every resource reload. */
    @Nullable
    private static SpriteSet sprites;

    private final Look look;
    private final float baseSize;
    /** Turn per tick; a puff lying on the ground keeps still. */
    protected float spin;

    /**
     * How one puff looks over its life.
     *
     * @param startR
     *            colour at birth
     * @param endR
     *            colour at death; the colour moves toward it quickly at first, then settles
     * @param alpha
     *            opacity at birth, fading to nothing by the end of its life
     * @param growth
     *            how much larger than its birth size the puff ends up, 0 for no growth
     * @param drag
     *            velocity kept each tick
     * @param buoyancy
     *            upward acceleration in blocks per tick squared, negative to sink
     * @param glow
     *            share of its life over which the puff fades from full brightness to the
     *            world's light, 0 for a puff lit like any other
     * @param additive
     *            whether the puff adds light to what is behind it rather than covering it
     * @param collides
     *            whether the puff stops against blocks
     */
    public record Look(float startR, float startG, float startB, float endR, float endG, float endB, float alpha, float growth, float drag, float buoyancy, float glow, boolean additive,
        boolean collides)
    {}

    // Mirrors the vanilla particle constructor shape: level, position and velocity.
    @SuppressWarnings("java:S107")
    protected BlastPuffParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Look look, float size, int lifetime)
    {
        super(level, x, y, z);
        this.look = look;
        this.baseSize = size;
        this.lifetime = Math.max(1, lifetime);
        this.hasPhysics = look.collides();
        this.friction = look.drag();
        this.gravity = 0F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.spin = (random.nextFloat() - 0.5F) * 0.06F;
        pickSprite(sprites);
        updateLook();
    }

    /** Creates a puff, or returns {@code null} before the particle sprites have loaded. */
    // Mirrors the vanilla particle constructor shape: level, position and velocity.
    @SuppressWarnings("java:S107")
    @Nullable
    public static BlastPuffParticle create(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, Look look, float size, int lifetime)
    {
        return sprites == null ? null : new BlastPuffParticle(level, x, y, z, vx, vy, vz, sprites, look, size, lifetime);
    }

    /**
     * Creates a puff lying flat on the ground rather than facing the camera, such as a scorch mark, or returns
     * {@code null} before the particle sprites have loaded. It neither moves nor turns.
     */
    @Nullable
    public static BlastPuffParticle createFlat(ClientLevel level, double x, double y, double z, Look look, float size, int lifetime)
    {
        return sprites == null ? null : new Flat(level, x, y, z, sprites, look, size, lifetime);
    }

    @Override
    public void tick()
    {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;

        if (age++ >= lifetime)
        {
            remove();
            return;
        }

        yd += look.buoyancy();
        // What rises drifts with the wind, so every column over a battlefield leans the same way.
        if (look.buoyancy() > 0F)
        {
            xd += ParticleWind.x();
            zd += ParticleWind.z();
        }
        move(xd, yd, zd);
        xd *= ModPhysics.dragRetention(friction, level);
        yd *= ModPhysics.dragRetention(friction, level);
        zd *= ModPhysics.dragRetention(friction, level);
        roll += spin;

        updateLook();
    }

    private void updateLook()
    {
        float t = Mth.clamp(age / (float) lifetime, 0F, 1F);
        // Cools quickly and then lingers, the way a fireball goes from orange to smoke.
        float cooling = 1F - (1F - t) * (1F - t);
        float fade = 1F - t * t;

        quadSize = baseSize * (1F + look.growth() * Mth.sqrt(t));
        alpha = look.alpha() * fade;

        float r = Mth.lerp(cooling, look.startR(), look.endR());
        float g = Mth.lerp(cooling, look.startG(), look.endG());
        float b = Mth.lerp(cooling, look.startB(), look.endB());
        // The additive blend takes the colour as it is, so the fade has to be carried by the colour.
        float brightness = look.additive() ? alpha : 1F;
        setColor(r * brightness, g * brightness, b * brightness);
    }

    @Override
    protected int getLightColor(float partialTick)
    {
        int world = super.getLightColor(partialTick);
        if (look.glow() <= 0F)
            return world;

        float t = (age + partialTick) / lifetime;
        int glowLevel = Mth.floor(15F * Mth.clamp(1F - t / look.glow(), 0F, 1F));
        return LightTexture.pack(Math.max(LightTexture.block(world), glowLevel), LightTexture.sky(world));
    }

    /**
     * NeoForge culls against this box, which by default is the small collision box grown by one
     * block. For a puff several blocks across that would make the whole puff vanish as soon as its
     * centre left the screen, so a column or cap would visibly lose pieces whenever the camera
     * turned. The box is grown to the largest quad the puff will draw.
     */
    @Override
    public AABB getRenderBoundingBox(float partialTicks)
    {
        return getBoundingBox().inflate(Math.max(1.0F, baseSize * (1F + look.growth())));
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return look.additive() ? LegacyParticleRenderTypes.PREMULTIPLIED : LegacyParticleRenderTypes.TRANSLUCENT;
    }

    /** A puff lying flat on the ground, drawn from both sides so that it shows however the quad is wound. */
    private static final class Flat extends BlastPuffParticle
    {
        private static final float[][] CORNERS = {{-1F, -1F}, {-1F, 1F}, {1F, 1F}, {1F, -1F}};

        // Mirrors the vanilla particle constructor shape: level, position and sprites, with the look.
        @SuppressWarnings("java:S107")
        private Flat(ClientLevel level, double x, double y, double z, SpriteSet sprites, Look look, float size, int lifetime)
        {
            super(level, x, y, z, 0D, 0D, 0D, sprites, look, size, lifetime);
            spin = 0F;
            hasPhysics = false;
        }

        @Override
        public void render(@NotNull VertexConsumer buffer, Camera camera, float partialTick)
        {
            Vec3 view = camera.getPosition();
            float cx = (float) (Mth.lerp(partialTick, xo, x) - view.x);
            float cy = (float) (Mth.lerp(partialTick, yo, y) - view.y);
            float cz = (float) (Mth.lerp(partialTick, zo, z) - view.z);
            float size = getQuadSize(partialTick);
            float cos = Mth.cos(roll) * size;
            float sin = Mth.sin(roll) * size;
            int light = getLightColor(partialTick);
            float[] us = {getU0(), getU0(), getU1(), getU1()};
            float[] vs = {getV1(), getV0(), getV0(), getV1()};
            for (int pass = 0; pass < 2; pass++)
            {
                for (int i = 0; i < 4; i++)
                {
                    int corner = pass == 0 ? i : 3 - i;
                    float dx = CORNERS[corner][0];
                    float dz = CORNERS[corner][1];
                    buffer.addVertex(cx + dx * cos - dz * sin, cy, cz + dx * sin + dz * cos).setUv(us[corner], vs[corner]).setColor(rCol, gCol, bCol, alpha).setLight(light);
                }
            }
        }
    }

    /** A plain grey puff, which is what a bare {@code /particle flansmod:blast_puff} gives. */
    private static final Look DEFAULT_LOOK = new Look(0.6F, 0.6F, 0.6F, 0.45F, 0.45F, 0.45F, 0.8F, 1.0F, 0.92F, 0.002F, 0F, false, true);

    public record Provider(SpriteSet spriteSet) implements ParticleProvider<SimpleParticleType>
    {
        public Provider
        {
            sprites = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new BlastPuffParticle(level, x, y, z, vx, vy, vz, spriteSet, DEFAULT_LOOK, 1.0F, 60);
        }
    }
}
