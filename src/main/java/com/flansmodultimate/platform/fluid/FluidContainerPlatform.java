package com.flansmodultimate.platform.fluid;

import com.flansmodultimate.platform.item.ItemCapabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

/** Loader boundary for looking up item fluid handlers and inspecting their tanks. */
public final class FluidContainerPlatform
{
    private FluidContainerPlatform()
    {}

    /**
     * The fluid handler of a single item taken from this stack, or null if it holds no fluid.
     *
     * <p>
     * The handler works on its own copy of one item, because the loader's bucket wrapper refuses
     * to drain a stack of more than one and replaces the container as it drains. The caller
     * puts the result back with {@link IFluidHandlerItem#getContainer()}.
     * </p>
     */
    @Nullable
    public static IFluidHandlerItem handlerFor(@NotNull ItemStack stack)
    {
        if (stack.isEmpty())
            return null;
        return ItemCapabilities.fluidHandler(stack.copyWithCount(1));
    }

    /** A copy of the first nonempty tank accepted by the predicate, else empty. */
    @NotNull
    public static FluidStack firstMatchingTank(@NotNull IFluidHandlerItem handler, @NotNull Predicate<Fluid> accepts)
    {
        for (int tank = 0; tank < handler.getTanks(); tank++)
        {
            FluidStack held = handler.getFluidInTank(tank);
            if (!held.isEmpty() && accepts.test(held.getFluid()))
                return held.copy();
        }
        return FluidStack.EMPTY;
    }

    /** Executes a drain of the specified fluid, preserving its stack data. */
    public static FluidStack drain(IFluidHandlerItem handler, FluidStack fluid, int amount)
    {
        return handler.drain(FluidPlatform.copyWithAmount(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }
}
