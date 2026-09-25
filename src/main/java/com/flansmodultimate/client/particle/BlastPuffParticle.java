package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

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
    /** Largest drawn half-width, in blocks, at which culling by the particle's centre is still invisible. */
    private static final float MAX_CULLED_SIZE = 1.0F;

    private final Look look;
    private final float baseSize;
    private final float spin;

    /**
     * How one puff looks over its life.
     *
     * @param startR   colour at birth
     * @param endR     colour at death; the colour moves toward it quickly at first, then settles
     * @param alpha    opacity at birth, fading to nothing by the end of its life
     * @param growth   how much larger than its birth size the puff ends up, 0 for no growth
     * @param drag     velocity kept each tick
     * @param buoyancy upward acceleration in blocks per tick squared, negative to sink
     * @param glow     share of its life over which the puff fades from full brightness to the
     *                 world's light, 0 for a puff lit like any other
     * @param additive whether the puff adds light to what is behind it rather than covering it
     * @param collides whether the puff stops against blocks
     */
    public record Look(float startR, float startG, float startB, float endR, float endG, float endB,
                       float alpha, float growth, float drag, float buoyancy, float glow,
                       boolean additive, boolean collides) {}

    protected BlastPuffParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                                SpriteSet sprites, Look look, float size, int lifetime)
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
    @Nullable
    public static BlastPuffParticle create(ClientLevel level, double x, double y, double z,
                                           double vx, double vy, double vz, Look look, float size, int lifetime)
    {
        return sprites == null ? null : new BlastPuffParticle(level, x, y, z, vx, vy, vz, sprites, look, size, lifetime);
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
        move(xd, yd, zd);
        xd *= friction;
        yd *= friction;
        zd *= friction;
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
     * The particle engine culls against the particle's bounding box, which is the small collision
     * box and not the quad that is drawn. For a puff several blocks across that makes the whole
     * puff vanish as soon as its centre leaves the screen, so a column or cap visibly loses pieces
     * whenever the camera turns. Culling stays on for puffs small enough that the difference is
     * under a block, where it cannot be seen.
     */
    @Override
    public boolean shouldCull()
    {
        return baseSize * (1F + look.growth()) < MAX_CULLED_SIZE;
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return look.additive() ? LegacyParticleRenderTypes.PREMULTIPLIED : LegacyParticleRenderTypes.TRANSLUCENT;
    }

    /** A plain grey puff, which is what a bare {@code /particle flansmod:blast_puff} gives. */
    private static final Look DEFAULT_LOOK = new Look(0.6F, 0.6F, 0.6F, 0.45F, 0.45F, 0.45F,
        0.8F, 1.0F, 0.92F, 0.002F, 0F, false, true);

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
