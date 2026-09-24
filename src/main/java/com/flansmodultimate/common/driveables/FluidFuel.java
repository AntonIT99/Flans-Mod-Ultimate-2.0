package com.flansmodultimate.common.driveables;

import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which liquids a driveable will burn, and what a bucket of each is worth.
 *
 * <p>Any container that reports Forge's fluid-handler capability can be emptied into a tank,
 * which is how 1.7.10's BuildCraft oil and fuel buckets are supported without depending on
 * BuildCraft at all: its buckets are ordinary {@code BucketItem}s, so Forge already exposes
 * their contents through the standard capability, and the fuel table names the fluids by
 * registry id. A server can add or retune any other mod's liquid fuel the same way.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FluidFuel
{
    // Its own logger, so parsing the table does not pull the mod class in behind it.
    private static final Logger log = LogUtils.getLogger();

    /** Millibuckets in one bucket, the unit the fuel table is quoted in. */
    public static final int BUCKET = FluidType.BUCKET_VOLUME;

    private static final List<Rule> RULES = new ArrayList<>();
    /** Resolved lookups, cleared whenever the table is rebuilt. */
    private static final Map<Fluid, Integer> RESOLVED = new ConcurrentHashMap<>();

    /**
     * One fuel table entry.
     *
     * <p>{@code path} is matched whole unless the entry ended in {@code *}, in which case it
     * is a prefix. That keeps families such as BuildCraft's ten oil and fuel grades, each with
     * three heat variants, to one line apiece.</p>
     */
    private record Rule(String namespace, String path, boolean prefix, int fuelPerBucket)
    {
        boolean matches(ResourceLocation id)
        {
            if (!namespace.equals(id.getNamespace()))
                return false;
            return prefix ? id.getPath().startsWith(path) : path.equals(id.getPath());
        }
    }

    /** Replaces the fuel table. Lines are {@code <fluid id>; <fuel per bucket>}. */
    public static synchronized void rebuild(List<String> lines)
    {
        RULES.clear();
        RESOLVED.clear();

        for (String line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;

            String[] parts = trimmed.split(";");
            if (parts.length != 2)
            {
                log.warn("Invalid fluid fuel line: {}", line);
                continue;
            }

            try
            {
                RULES.add(parseRule(parts[0].trim(), Integer.parseInt(parts[1].trim())));
            }
            catch (Exception e)
            {
                log.error("Failed to parse fluid fuel line '{}': {}", line, e.getMessage());
            }
        }
    }

    private static Rule parseRule(String pattern, int fuelPerBucket)
    {
        if (fuelPerBucket <= 0)
            throw new IllegalArgumentException("fuel per bucket must be positive");
        int separator = pattern.indexOf(':');
        if (separator <= 0 || separator == pattern.length() - 1)
            throw new IllegalArgumentException("expected <namespace>:<path>");

        String namespace = pattern.substring(0, separator).toLowerCase(Locale.ROOT);
        String path = pattern.substring(separator + 1).toLowerCase(Locale.ROOT);
        boolean prefix = path.endsWith("*");
        return new Rule(namespace, prefix ? path.substring(0, path.length() - 1) : path, prefix, fuelPerBucket);
    }

    /** Fuel one whole bucket of this liquid is worth, or zero if it does not burn. */
    public static int fuelPerBucket(@Nullable Fluid fluid)
    {
        if (fluid == null || RULES.isEmpty())
            return 0;
        return RESOLVED.computeIfAbsent(fluid, FluidFuel::resolve);
    }

    private static int resolve(Fluid fluid)
    {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return id == null ? 0 : fuelPerBucket(id);
    }

    /** Fuel a bucket of the liquid with this registry id is worth, or zero if it does not burn. */
    static int fuelPerBucket(@NotNull ResourceLocation id)
    {
        // First match wins, so a specific entry placed above a wildcard overrides it.
        for (Rule rule : List.copyOf(RULES))
            if (rule.matches(id))
                return rule.fuelPerBucket();
        return 0;
    }

    /** Whether this stack is a container holding a liquid the fuel table recognises. */
    public static boolean isFuelContainer(@NotNull ItemStack stack)
    {
        IFluidHandlerItem handler = handlerFor(stack);
        return handler != null && !firstBurnableTank(handler).isEmpty();
    }

    /**
     * The fluid handler of a single item taken from this stack, or null if it holds no fluid.
     *
     * <p>The handler works on its own copy of one item, because Forge's bucket wrapper refuses
     * to drain a stack of more than one and replaces the container as it drains. The caller
     * puts the result back with {@link IFluidHandlerItem#getContainer()}.</p>
     */
    @Nullable
    public static IFluidHandlerItem handlerFor(@NotNull ItemStack stack)
    {
        if (stack.isEmpty())
            return null;
        return stack.copyWithCount(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
    }

    /** Contents of the first tank holding something this driveable can burn, else empty. */
    @NotNull
    public static FluidStack firstBurnableTank(@NotNull IFluidHandlerItem handler)
    {
        for (int tank = 0; tank < handler.getTanks(); tank++)
        {
            FluidStack held = handler.getFluidInTank(tank);
            if (!held.isEmpty() && fuelPerBucket(held.getFluid()) > 0)
                return held.copy();
        }
        return FluidStack.EMPTY;
    }
}
