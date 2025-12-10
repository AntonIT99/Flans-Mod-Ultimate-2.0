package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModCommonConfigs;
import com.flansmodultimate.network.PacketHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event)
    {
        PacketHandler.registerPackets();
    }

    @SubscribeEvent
    public static void onModConfigReload(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == ModCommonConfigs.CONFIG)
            ModCommonConfigs.bake();
    }
}
