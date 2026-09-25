package com.flansmodultimate.platform.fluid;

import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

import net.minecraft.world.entity.Entity;

/** Version boundary for small fluid API calls whose signatures changed. */
public final class FluidPlatform
{
    private FluidPlatform() {}

    /** A copy of the fluid stack, including its data, with a different amount. */
    public static FluidStack copyWithAmount(FluidStack stack, int amount)
    {
        return stack.copyWithAmount(amount);
    }

    /** Whether the entity's eyes are in the water fluid type. */
    public static boolean isEyeInWater(Entity entity)
    {
        return entity.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value());
    }
}
