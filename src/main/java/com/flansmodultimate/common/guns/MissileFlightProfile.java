package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.BulletType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Flight profile of guided missiles: the roll they show about their flight axis, and the ground skim that lets a missile
 * flown level just above natural ground carry on instead of striking it.
 */
public final class MissileFlightProfile
{
    /** Steeper dives than this, in blocks per tick, always strike the ground. */
    static final double MAX_SKIM_DESCENT = 0.22D;
    /** The horizontal speed must be this many times the vertical speed for a skim. */
    static final double MIN_SKIM_LEVELNESS = 2.5D;
    /** The largest upward correction a skim applies, in blocks per tick. */
    static final double MAX_SKIM_LIFT = 0.04D;
    private static final float WOBBLE_FREQUENCY = 0.42F;

    private MissileFlightProfile()
    {}

    /** Missiles, manually guided and SACLOS rounds, and any round locked onto a target count as guided. */
    public static boolean isGuided(@Nullable BulletType type, boolean lockedOn)
    {
        return type != null && (type.getWeaponType() == EnumWeaponType.MISSILE || type.isManualGuidance() || type.isEnableSACLOS() || lockedOn);
    }

    public static boolean shouldTwirl(@Nullable BulletType type, boolean lockedOn)
    {
        return type != null && type.isMissileTwirl() && isGuided(type, lockedOn);
    }

    /** Roll in degrees about the flight axis at the given age in ticks. */
    public static float twirlDegrees(float ageTicks, float speed, float wobble)
    {
        return ageTicks * speed + (float) Math.sin(ageTicks * WOBBLE_FREQUENCY) * wobble;
    }

    /** Natural ground a missile can skim over: soil, sand, gravel, clay and snow. */
    public static boolean isSkimmableGround(BlockState state)
    {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.SNOW) || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL) || state.is(Blocks.FARMLAND)
            || state.is(Blocks.DIRT_PATH);
    }

    /**
     * Whether a guided round about to strike the top of a ground block skims over it instead.
     *
     * @param heightAboveSurface
     *            the round's height above the top face it would strike, in blocks
     */
    public static boolean shouldSkim(float tolerance, Direction face, boolean skimmableGround, double heightAboveSurface, Vec3 velocity)
    {
        if (tolerance <= 0F || face != Direction.UP || !skimmableGround || velocity.y < -MAX_SKIM_DESCENT)
            return false;
        double horizontalSpeed = velocity.horizontalDistance();
        return heightAboveSurface > 0D && heightAboveSurface <= tolerance && horizontalSpeed > Math.abs(velocity.y) * MIN_SKIM_LEVELNESS;
    }

    /** The velocity after a skim: the descent is cancelled and replaced by a small climb. */
    public static Vec3 skimVelocity(Vec3 velocity, float tolerance)
    {
        return new Vec3(velocity.x, Math.max(velocity.y, Math.min(MAX_SKIM_LIFT, tolerance * 0.25D)), velocity.z);
    }
}
