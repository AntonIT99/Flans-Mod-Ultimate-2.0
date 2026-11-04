package com.flansmodultimate.client.damage;

import com.flansmodultimate.FlansModSounds;
import com.flansmodultimate.client.sound.SoundHelper;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The marks an aircraft leaves sliding along the ground on its belly, after a gear-up landing or as a wreck that came
 * in fast: a furrow of scorched earth behind it, dust thrown up, sparks where metal meets stone, and the scream of the
 * scrape. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CrashSlideEffects
{
    /** Blocks a tick the aircraft has to slide at for the slide to leave marks. */
    private static final double SLIDE_SPEED = 0.2D;
    /** Blocks between two furrow marks, so the furrow reads as one line at any speed. */
    private static final double FURROW_SPACING = 1.2D;
    /** Ticks a furrow mark lasts. */
    private static final int FURROW_TICKS = 2400;
    /** Blocks below the aircraft's position searched for the ground it slides on. */
    private static final int GROUND_REACH = 1;
    /** Hardness from which the ground is stone or harder, which metal sliding over throws sparks off. */
    private static final float HARD_GROUND = 1.5F;

    /** Where each sliding aircraft last left a mark. */
    private static final Map<Driveable, Vec3> LAST_MARK = new WeakHashMap<>();

    static void tick(Driveable driveable)
    {
        Vec3 motion = driveable.position().subtract(driveable.xo, driveable.yo, driveable.zo);
        double speed = motion.horizontalDistance();
        if (speed < SLIDE_SPEED || speed > 8D || !slidesOnBelly(driveable))
        {
            LAST_MARK.remove(driveable);
            return;
        }
        Level level = driveable.level();
        BlockPos below = groundBelow(level, driveable.blockPosition());
        if (below == null)
        {
            LAST_MARK.remove(driveable);
            return;
        }
        RandomSource random = level.random;
        BlockState ground = level.getBlockState(below);
        double groundY = below.getY() + 1D;
        Vec3 at = new Vec3(driveable.getX(), groundY, driveable.getZ());
        float width = Math.max(0.8F, driveable.getBbWidth() * 0.4F);
        double density = DamageParticles.density();

        Vec3 last = LAST_MARK.get(driveable);
        if (last == null || last.distanceToSqr(at) >= FURROW_SPACING * FURROW_SPACING)
        {
            DamageParticles.scorchGround(at.x, groundY, at.z, width, FURROW_TICKS, null);
            LAST_MARK.put(driveable, at);
        }
        int dust = DamageParticles.count(speed * 2.5D * density, random);
        for (int i = 0; i < dust; i++)
        {
            Vec3 thrown = motion.scale(-0.2D).add((random.nextDouble() - 0.5D) * 0.15D, 0.06D + random.nextDouble() * 0.08D, (random.nextDouble() - 0.5D) * 0.15D);
            DamageParticles.puff(DamageParticles.groundDust(ground, level, below), at.add((random.nextDouble() - 0.5D) * width, 0.1D, (random.nextDouble() - 0.5D) * width), thrown.x,
                thrown.y, thrown.z, 0.8F + random.nextFloat() * 0.8F, 40 + random.nextInt(30), false);
        }
        // Metal on stone throws sparks; on earth it only ploughs.
        if (ground.getDestroySpeed(level, below) >= HARD_GROUND)
            DamageParticles.sparks(DamageParticles.SPARK, at.add(0D, 0.1D, 0D), 1 + random.nextInt(3), 0.2D + speed * 0.1D, random);
        if (random.nextFloat() < 0.25F)
            SoundHelper.getSoundEvent(FlansModSounds.SOUND_IMPACT_METAL)
                .ifPresent(scrape -> level.playLocalSound(at.x, at.y, at.z, scrape, SoundSource.NEUTRAL, 0.6F + (float) speed * 0.4F, 0.5F + random.nextFloat() * 0.2F, false));
    }

    /** @return whether the aircraft is down on its belly: a working one without its gear, or a wreck */
    private static boolean slidesOnBelly(Driveable driveable)
    {
        if (driveable instanceof Plane plane && !driveable.isWreck())
            return plane.isOnBelly();
        return driveable.isWreck() && driveable.getDamageClass().flies();
    }

    @Nullable
    private static BlockPos groundBelow(Level level, BlockPos from)
    {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int i = 0; i <= GROUND_REACH; i++)
        {
            cursor.move(0, -1, 0);
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty())
                return cursor.immutable();
        }
        return null;
    }
}
