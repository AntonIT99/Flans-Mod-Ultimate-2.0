package com.flansmodultimate.platform.entity;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.List;

/** Loader bootstrap needed to construct real entities in plain JUnit, without launching Minecraft. */
public final class EntityTestBootstrap
{
    private EntityTestBootstrap()
    {}

    // Forge creates custom registries and fills RegistryObjects inside package-private loader hooks.
    @SuppressWarnings({"java:S3011", "unchecked"})
    public static void initialize() throws ReflectiveOperationException
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries initialize first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }

        if (ForgeRegistries.FLUID_TYPES.get() != null)
            return;

        var vanillaFluids = List.of(ForgeMod.EMPTY_TYPE, ForgeMod.WATER_TYPE, ForgeMod.LAVA_TYPE);
        var builder = new RegistryBuilder<FluidType>().setName(ForgeRegistries.Keys.FLUID_TYPES.location());
        var create = RegistryBuilder.class.getDeclaredMethod("create");
        create.setAccessible(true);
        IForgeRegistry<FluidType> fluids = (IForgeRegistry<FluidType>) create.invoke(builder);
        var update = RegistryObject.class.getDeclaredMethod("updateReference", IForgeRegistry.class);
        update.setAccessible(true);
        for (var entry : vanillaFluids)
        {
            fluids.register(entry.getId(), new FluidType(FluidType.Properties.create().canPushEntity(false))
            {});
            update.invoke(entry, fluids);
        }
    }
}
