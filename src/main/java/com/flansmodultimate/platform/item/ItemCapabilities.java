package com.flansmodultimate.platform.item;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/** Version boundary for item capability lookups (Forge capabilities or NeoForge item capabilities). */
public final class ItemCapabilities
{
    private ItemCapabilities() {}

    /** The stack's energy storage, or null if it has none. */
    @Nullable
    public static IEnergyStorage energy(ItemStack stack)
    {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    /** The stack's fluid handler, or null if it has none. The handler may replace the container as it drains. */
    @Nullable
    public static IFluidHandlerItem fluidHandler(ItemStack stack)
    {
        return stack.getCapability(Capabilities.FluidHandler.ITEM);
    }
}
