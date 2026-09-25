package com.flansmodultimate.platform.fluid;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;

import net.minecraft.world.entity.Entity;

/** Version boundary for small fluid API calls whose signatures changed. */
public final class FluidPlatform
{
    private FluidPlatform() {}

    /** A copy of the fluid stack, including its data, with a different amount. */
    public static FluidStack copyWithAmount(FluidStack stack, int amount)
    {
        return new FluidStack(stack, amount);
    }

    /** Whether the entity's eyes are in the water fluid type. */
    public static boolean isEyeInWater(Entity entity)
    {
        return entity.isEyeInFluidType(ForgeMod.WATER_TYPE.get());
    }
}
