package com.flansmodultimate.platform.fluid;

import net.neoforged.neoforge.fluids.FluidStack;

/** Version boundary for small fluid API calls whose signatures changed. */
public final class FluidPlatform
{
    private FluidPlatform() {}

    /** A copy of the fluid stack, including its data, with a different amount. */
    public static FluidStack copyWithAmount(FluidStack stack, int amount)
    {
        return stack.copyWithAmount(amount);
    }
}
