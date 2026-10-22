package com.flansmodultimate.platform.entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

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
        // Enchantment iteration asks FMLEnvironment for the side, which the loader would have set; no server runs here.
        var dist = FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        if (dist.get(null) == null)
            dist.set(null, Dist.DEDICATED_SERVER);
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var attributeHolder = DeferredHolder.class.getDeclaredField("holder");
        attributeHolder.setAccessible(true);
        for (var entry : List.of(NeoForgeMod.SWIM_SPEED, NeoForgeMod.NAMETAG_DISTANCE, NeoForgeMod.CREATIVE_FLIGHT))
        {
            var key = entry.unwrapKey().orElseThrow();
            if (!((DeferredHolder<?, ?>) entry).isBound())
                attributeHolder.set(entry,
                    Holder.direct(entry == NeoForgeMod.CREATIVE_FLIGHT
                        ? new BooleanAttribute("test.flight", false)
                        : new RangedAttribute("test." + key.location().getPath(), entry == NeoForgeMod.SWIM_SPEED ? 1D : 64D, 0D, 1024D)));
        }

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
