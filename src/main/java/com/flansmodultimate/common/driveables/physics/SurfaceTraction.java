package com.flansmodultimate.common.driveables.physics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the ground under the wheels is made of, and what that does to a ground vehicle.
 *
 * <p>
 * The block each wheel stands on is sorted into a handful of surface kinds, and the kind yields two gameplay factors: a
 * grip multiplier on tractive force, braking and steering, and a speed factor on the speed the surface lets the vehicle
 * hold. Tracks and all-wheel drive recover part of the penalty on loose and soft ground, as their vehicles do; on slick
 * ice they help far less. The factors are deliberately moderate: a truck in deep mud is slowed, not stopped.
 * </p>
 *
 * <p>
 * Classification and response are pure, so they can be tested without a world; callers sample the block states and
 * average the per-wheel samples.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SurfaceTraction
{
    /** Block friction above which a surface counts as slick; vanilla blocks default to 0.6, ice to 0.98. */
    private static final float SLICK_FRICTION = 0.61F;
    /** Share of the penalty tracks recover on ground that yields; all-wheel drive recovers half as much. */
    private static final float TRACKED_RELIEF = 0.6F;
    private static final float ALL_WHEEL_RELIEF = 0.3F;

    /** The kinds of ground a wheel can stand on, with the penalties a plain wheeled layout takes there. */
    public enum Kind
    {
        /** Stone, dirt, grass, roads: full grip, full speed. */
        FIRM(1F, 1F, 0F),
        /** Sand, gravel, snow cover, farmland: loose footing that costs some grip and speed. */
        LOOSE(0.85F, 0.85F, 1F),
        /** Mud, soul sand and soil, powder snow, honey: ground that yields and swallows momentum. */
        SOFT(0.7F, 0.6F, 1F),
        /** Ice and slime: hardly any grip, but nothing slows the vehicle down either. */
        SLICK(0.3F, 1F, 0.35F);

        private final float grip;
        private final float speedFactor;
        /** How much of the drive layout's relief applies here: tracks grip mud far better than ice. */
        private final float reliefScale;

        Kind(float grip, float speedFactor, float reliefScale)
        {
            this.grip = grip;
            this.speedFactor = speedFactor;
            this.reliefScale = reliefScale;
        }
    }

    /** The factors a surface leaves a vehicle: both 1 on firm ground, both in {@code (0, 1]}. */
    public record Sample(float grip, float speedFactor)
    {
        public static final Sample FIRM = new Sample(1F, 1F);
    }

    /** Sorts the block a wheel stands on into its surface kind. The named blocks back the tags up where no tags are loaded. */
    public static Kind classify(BlockState state)
    {
        if (state.isAir())
            return Kind.FIRM;
        if (state.getBlock().getFriction() > SLICK_FRICTION)
            return Kind.SLICK;
        if (state.is(Blocks.MUD) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.HONEY_BLOCK))
            return Kind.SOFT;
        if (state.is(BlockTags.SAND) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || state.is(BlockTags.SNOW) || state.is(Blocks.SNOW)
            || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.FARMLAND))
            return Kind.LOOSE;
        return Kind.FIRM;
    }

    /** The factors this kind of ground leaves a vehicle with the given drive layout. */
    public static Sample sample(Kind kind, EnumDriveType driveType)
    {
        if (kind == null || kind == Kind.FIRM)
            return Sample.FIRM;
        float relief = kind.reliefScale * layoutRelief(driveType);
        return new Sample(Mth.lerp(relief, kind.grip, 1F), Mth.lerp(relief, kind.speedFactor, 1F));
    }

    /** Averages per-wheel samples; {@link Sample#FIRM} without any. */
    public static Sample average(double gripSum, double speedFactorSum, int wheels)
    {
        if (wheels <= 0)
            return Sample.FIRM;
        return new Sample((float) Mth.clamp(gripSum / wheels, 0D, 1D), (float) Mth.clamp(speedFactorSum / wheels, 0D, 1D));
    }

    /**
     * Multiplier on steering authority for the given grip: understeer on ice stays gentle rather than
     * proportional, so a sliding vehicle still answers its wheel.
     */
    public static float steeringFactor(float grip)
    {
        return 0.5F + 0.5F * Mth.clamp(grip, 0F, 1F);
    }

    private static float layoutRelief(EnumDriveType driveType)
    {
        if (driveType == EnumDriveType.TRACKED)
            return TRACKED_RELIEF;
        return driveType == EnumDriveType.AWD ? ALL_WHEEL_RELIEF : 0F;
    }
}
