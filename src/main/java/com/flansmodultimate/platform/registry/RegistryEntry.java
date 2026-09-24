package com.flansmodultimate.platform.registry;

import net.minecraftforge.registries.RegistryObject;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Version boundary for a deferred registry entry: Forge {@code RegistryObject} or NeoForge {@code DeferredHolder}. */
public final class RegistryEntry<T> implements Supplier<T>
{
    private final RegistryObject<? extends T> holder;

    private RegistryEntry(RegistryObject<? extends T> holder)
    {
        this.holder = holder;
    }

    public static <T> RegistryEntry<T> of(RegistryObject<? extends T> holder)
    {
        return new RegistryEntry<>(holder);
    }

    @Override
    public T get()
    {
        return holder.get();
    }

    public ResourceLocation getId()
    {
        return holder.getId();
    }

    /** Whether the entry has been registered and can be resolved. */
    public boolean isPresent()
    {
        return holder.isPresent();
    }
}
