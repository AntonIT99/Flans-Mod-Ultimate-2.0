package com.wolffsarmormod.event;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.network.PacketHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event)
    {
        PacketHandler.registerPackets();
    }
}
