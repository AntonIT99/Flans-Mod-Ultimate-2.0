package com.flansmodultimate.platform.entity;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.*;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.List;
import java.util.Map;

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

        initializeAttributes();
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

    @SuppressWarnings({"java:S3011", "UnstableApiUsage"}) // Plain JUnit needs Forge's internal, normally loader-driven RegistryObject updates.
    private static void initializeAttributes() throws ReflectiveOperationException
    {
        if (ForgeMod.SWIM_SPEED.isPresent())
            return;
        Map<RegistryObject<Attribute>, Attribute> attributes = Map.of(ForgeMod.SWIM_SPEED, new RangedAttribute("forge.swim_speed", 1D, 0D, 1024D).setSyncable(true), ForgeMod.NAMETAG_DISTANCE,
            new RangedAttribute("forge.name_tag_distance", 64D, 0D, 64D).setSyncable(true), ForgeMod.ENTITY_GRAVITY, new RangedAttribute("forge.entity_gravity", 0.08D, -8D, 8D).setSyncable(true),
            ForgeMod.BLOCK_REACH, new RangedAttribute("forge.block_reach", 4.5D, 0D, 1024D).setSyncable(true), ForgeMod.ENTITY_REACH,
            new RangedAttribute("forge.entity_reach", 3D, 0D, 1024D).setSyncable(true), ForgeMod.STEP_HEIGHT_ADDITION, new RangedAttribute("forge.step_height", 0D, -512D, 512D).setSyncable(true));
        ForgeRegistry<Attribute> registry = (ForgeRegistry<Attribute>) ForgeRegistries.ATTRIBUTES;
        var update = RegistryObject.class.getDeclaredMethod("updateReference", IForgeRegistry.class);
        update.setAccessible(true);
        registry.unfreeze();
        try
        {
            for (var entry : attributes.entrySet())
            {
                registry.register(entry.getKey().getId(), entry.getValue());
                update.invoke(entry.getKey(), registry);
            }
        }
        finally
        {
            registry.freeze();
        }
    }
}
