package com.flansmodultimate.platform.entity;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.server.Bootstrap;

import java.util.List;
import java.util.Map;

/** Loader bootstrap needed to construct real entities in plain JUnit, without launching Minecraft. */
public final class EntityTestBootstrap
{
    private EntityTestBootstrap()
    {}

    // NeoForge's internal discovery bootstrap and holder binding are needed only in this unit-test fixture.
    @SuppressWarnings({"java:S3011", "UnstableApiUsage"})
    public static void initialize() throws ReflectiveOperationException
    {
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        if (NeoForgeMod.EMPTY_TYPE.isBound())
            return;

        var holder = DeferredHolder.class.getDeclaredField("holder");
        holder.setAccessible(true);
        for (var entry : List.of(NeoForgeMod.EMPTY_TYPE, NeoForgeMod.WATER_TYPE, NeoForgeMod.LAVA_TYPE))
        {
            var key = entry.unwrapKey().orElseThrow();
            if (!NeoForgeRegistries.FLUID_TYPES.containsKey(key))
                Registry.register(NeoForgeRegistries.FLUID_TYPES, key, new FluidType(FluidType.Properties.create().canPushEntity(false))
                {});
            holder.set(entry, NeoForgeRegistries.FLUID_TYPES.getHolder(key).orElseThrow());
        }
    }
}
