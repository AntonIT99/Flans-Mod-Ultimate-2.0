package com.flansmodultimate.compat.sable.platform;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.geometry.EmplacementFrame;
import com.flansmodultimate.compat.sable.SableEmplacementFrame;
import com.flansmodultimate.common.raytracing.Raytracer;
import com.flansmodultimate.compat.sable.SableShotClipper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class SableSetup
{
    private SableSetup()
    {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event)
    {
        if (ModList.get().isLoaded("sable"))
            event.enqueueWork(() ->
            {
                Raytracer.setMovingBlockClipper(SableShotClipper::nearest);
                EmplacementFrame.Lookup.setResolver(SableEmplacementFrame::at);
            });
    }
}
