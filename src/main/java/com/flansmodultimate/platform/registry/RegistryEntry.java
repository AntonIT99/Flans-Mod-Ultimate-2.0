package com.flansmodultimate.platform.registry;

import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Version boundary for a deferred registry entry: Forge {@code RegistryObject} or NeoForge {@code DeferredHolder}. */
public final class RegistryEntry<T> implements Supplier<T>
{
    private final DeferredHolder<?, ?> holder;

    private RegistryEntry(DeferredHolder<?, ?> holder)
    {
        this.holder = holder;
    }

    public static <R, T extends R> RegistryEntry<T> of(DeferredHolder<R, ? extends T> holder)
    {
        return new RegistryEntry<>(holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get()
    {
        return (T) holder.get();
    }

    public ResourceLocation getId()
    {
        return holder.getId();
    }

    /** Whether the entry has been registered and can be resolved. */
    public boolean isPresent()
    {
        return holder.isBound();
    }

    /** The registry holder, for 1.21 APIs that take one; only valid when the entry type is the registry type. */
    @SuppressWarnings("unchecked")
    public Holder<T> holder()
    {
        return (Holder<T>) holder;
    }
}
