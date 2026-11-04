package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared, bounded modern implementations of the additional Labjac sprites. All motion is cosmetic. */
public final class LegacyEffectParticle extends ParticleBase
{
    /** Presets retain distinct textures, lifetimes and motion without duplicating renderers. */
    @Getter
    public enum Style
    {
        BLOOD(18, 0.22F, 1.5F, 0.008F, false, false), OVERKILL(30, 0.35F, 2F, 0.025F, false, false), TANK_DEATH(400, 0.6F, 0.5F, -0.001F, false, true), SHIP_DEATH(400, 0.9F, 0.5F, -0.001F, false,
            true), NUKE(400, 1.4F, 0.5F, -0.001F, false, true), SMOKE_SHELL(500, 0.8F, 5F, -0.0002F, true, false), MUSTARD(500, 0.8F, 5F, 0F, true, false), CHLORINE(500, 0.8F, 5F, 0F, true,
                false), WATER(55, 0.7F, 2F, 0.025F, false, false), WATER_MINI(28, 0.2F, 1F, 0.025F, false, false), SHIP_SMOKE(150, 0.7F, 3F, -0.001F, true, false), GUN_SMOKE(12, 0.045F, 2F, -0.0004F,
                    true, false), LOCKED(10, 0.35F, 0F, 0F, false, false), FLASHER(5, 0.08F, 0F, 0.04F, false, false), NU_FLASH(3, 0.75F, 0.5F, 0F, false, true), NU_MUZZLE(3, 0.3F, 0.2F, 0F, false,
                        true), NU_SPARK(7, 0.12F, -0.5F, 0.02F, false, true), CASING(45, 0.06F, 0F, 0.04F, false, false), TURRET_TEST(40, 0.4F, 0F, 0F, false, false);

        private final int lifetime;
        private final float size;
        private final float growth;
        private final float fall;
        private final boolean wind;
        private final boolean hot;

        Style(int lifetime, float size, float growth, float fall, boolean wind, boolean hot)
        {
            this.lifetime = lifetime;
            this.size = size;
            this.growth = growth;
            this.fall = fall;
            this.wind = wind;
            this.hot = hot;
        }

        boolean column()
        {
            return this == TANK_DEATH || this == SHIP_DEATH || this == NUKE;
        }
    }

    private static final Set<LegacyEffectParticle> CONTROLLERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LegacyEffectParticle> CLOUDS = Collections.newSetFromMap(new WeakHashMap<>());
    private static ClientLevel controllerLevel;
    private final Style style;
    private float baseSize;
    private Entity owner;
    private Vec3 ownerOffset;

    // Mirrors the particle provider's position, velocity and sprite inputs, plus its preset.
    @SuppressWarnings("java:S107")
    private LegacyEffectParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Style style)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        this.style = style;
        lifetime = style.lifetime;
        baseSize = style.size;
        xd = vx;
        yd = vy;
        zd = vz;
        hasPhysics = !style.column() && style != Style.LOCKED && style != Style.TURRET_TEST;
        if (style == Style.CASING)
        {
            roll = random.nextFloat() * Mth.TWO_PI;
            rCol = 0.85F;
            gCol = 0.62F;
            bCol = 0.2F;
        }
        setSize(0.08F, 0.08F);
        pickSprite(sprites);
        updateVisuals();
    }

    @Override
    public void tick()
    {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (owner != null)
        {
            if (owner.isRemoved() || owner.level() != level)
            {
                remove();
                return;
            }
            setPos(owner.getX() + ownerOffset.x, owner.getY() + ownerOffset.y, owner.getZ() + ownerOffset.z);
        }
        if (age++ >= lifetime)
        {
            remove();
            return;
        }
        if (style.column())
            emitColumn();
        else
            tickMotion();
        updateVisuals();
    }

    private void tickMotion()
    {
        yd -= style.fall;
        if (style.wind)
        {
            xd += ParticleWind.x();
            zd += ParticleWind.z();
        }
        move(xd, yd, zd);
        xd *= 0.96D;
        yd *= 0.96D;
        zd *= 0.96D;
        if (style == Style.CASING)
        {
            if (onGround)
            {
                xd *= 0.3D;
                zd *= 0.3D;
            }
            else
                roll += 0.5F;
        }
        if (style == Style.FLASHER)
        {
            ParticleHelper.spawnFromString(FlanParticles.EXPLODE, x, y, z, 0D, 0D, 0D, 0.25F);
            if (age == lifetime || onGround)
            {
                ParticleHelper.spawnFromString(FlanParticles.FM_SMOKE_BURST, x, y, z, 0D, 0D, 0D, 0.5F);
                remove();
            }
        }
    }

    private void emitColumn()
    {
        boolean water = level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
        if (age % 6 != 0)
            return;
        String flame = water ? FlanParticles.BUBBLE : FlanParticles.FM_FLAME;
        ParticleHelper.spawnFromString(flame, x, y, z, 0D, 0.08D, 0D, baseSize);
        if (!water)
            ParticleHelper.spawnFromString(style == Style.SHIP_DEATH ? FlanParticles.FM_SHIP_SMOKE : FlanParticles.FM_SMOKE, x, y + baseSize, z, 0D, 0.04D, 0D, baseSize);
    }

    void follow(Entity entity)
    {
        owner = entity;
        ownerOffset = new Vec3(x, y, z).subtract(entity.position());
    }

    @Override
    protected void updateVisuals()
    {
        float progress = Mth.clamp(age / (float) Math.max(1, lifetime), 0F, 1F);
        quadSize = baseSize * (1F + style.growth * Mth.sqrt(progress));
        alpha = (style.wind ? 0.65F : 1F) * (1F - progress * progress);
        if (style == Style.GUN_SMOKE)
            // The flash lasts two ticks. Let it lead before the smoke becomes visible.
            alpha = age < 2 ? 0F : 0.22F * (1F - progress);
        if (style.column())
        {
            // The visible column is its children; its controller must not draw a flame under water.
            quadSize = 0F;
            alpha = 0F;
        }
    }

    @Override
    public @NotNull Particle scale(float factor)
    {
        if (Float.isFinite(factor) && factor > 0F)
        {
            baseSize *= Mth.clamp(factor, 0.01F, 8F);
            updateVisuals();
        }
        return this;
    }

    public boolean isHot()
    {
        return style.hot && !style.column();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType()
    {
        return style == Style.GUN_SMOKE ? LegacyParticleRenderTypes.GUN_SMOKE : super.getRenderType();
    }

    @Override
    public int getLightColor(float partialTick)
    {
        return style.hot ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick)
    {
        if (hiddenByPreferences())
            return;
        if (style != Style.CASING)
        {
            super.render(buffer, camera, partialTick);
            return;
        }
        // A narrow brass case, using the existing brass skin instead of the fork's missing Casing1-6 images.
        Vec3 eye = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - eye.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - eye.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - eye.z);
        Quaternionf rotation = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTick, oRoll, roll));
        Vector3f[] corners = {new Vector3f(-0.35F, -1F, 0F), new Vector3f(-0.35F, 1F, 0F), new Vector3f(0.35F, 1F, 0F), new Vector3f(0.35F, -1F, 0F)};
        float[] u = {getU1(), getU1(), getU0(), getU0()};
        float[] v = {getV1(), getV0(), getV0(), getV1()};
        for (int i = 0; i < corners.length; i++)
        {
            Vector3f corner = corners[i].rotate(rotation).mul(quadSize).add(px, py, pz);
            buffer.addVertex(corner.x, corner.y, corner.z).setUv(u[i], v[i]).setColor(rCol, gCol, bCol, alpha).setLight(getLightColor(partialTick));
        }
    }

    @SuppressWarnings("java:S2696") // This bounded allowance belongs to the active client level, not an individual provider.
    static void useLevel(ClientLevel level)
    {
        if (controllerLevel != level)
        {
            controllerLevel = level;
            CONTROLLERS.clear();
            CLOUDS.clear();
        }
    }

    private boolean hiddenByPreferences()
    {
        ModClientConfig config = ModClientConfig.get();
        return config != null && ((style == Style.BLOOD || style == Style.OVERKILL) && !config.showBloodEffects || style == Style.GUN_SMOKE && !config.showWeaponSmoke);
    }

    /** One provider per registered preset; sprites remain resource-reload aware. */
    public record Provider(SpriteSet sprites, Style style) implements ParticleProvider<SimpleParticleType>
    {
        private static boolean allowController()
        {
            CONTROLLERS.removeIf(particle -> !particle.isAlive());
            return CONTROLLERS.size() < 16;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            useLevel(level);
            if (style.wind && style.lifetime > 100)
            {
                CLOUDS.removeIf(particle -> !particle.isAlive());
                if (CLOUDS.size() >= 256)
                    return null;
            }
            if (style.column() && !allowController())
                return null;
            if (style == Style.CASING && ModClientConfig.get() != null && !ModClientConfig.get().showCasingEjections)
                return null;
            LegacyEffectParticle particle = new LegacyEffectParticle(level, x, y, z, vx, vy, vz, sprites, style);
            if (particle.hiddenByPreferences())
                return null;
            if (style.column())
                CONTROLLERS.add(particle);
            if (style.wind && style.lifetime > 100)
                CLOUDS.add(particle);
            return particle;
        }
    }
}
