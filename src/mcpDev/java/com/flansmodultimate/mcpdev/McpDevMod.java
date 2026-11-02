package com.flansmodultimate.mcpdev;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Development source set loaded only by client runs with withMinecraftMcp. */
@Mod("flansmcpdev")
@Mod.EventBusSubscriber(modid = "flansmcpdev", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("java:S1118") // Forge instantiates the mod entrypoint.
public final class McpDevMod
{
    private static McpDevServer server;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            server = new McpDevServer(Integer.getInteger("fmu.mcp.port", 9976));
            server.start();
        });
    }
}
