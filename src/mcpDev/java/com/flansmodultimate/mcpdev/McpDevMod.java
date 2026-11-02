package com.flansmodultimate.mcpdev;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Development source set registered only while Minecraft MCP is prepared. ModDevGradle loads every registered mod in every
 * run, so the companion stays idle unless its MCP client run sets {@code fmu.mcp.port}.
 */
@Mod(value = "flansmcpdev", dist = Dist.CLIENT)
@EventBusSubscriber(modid = "flansmcpdev", value = Dist.CLIENT)
@SuppressWarnings("java:S1118") // NeoForge instantiates the mod entrypoint.
public final class McpDevMod
{
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event)
    {
        Integer port = Integer.getInteger("fmu.mcp.port");
        if (port == null)
            return;
        // The HTTP server's own threads keep it alive.
        event.enqueueWork(() -> new McpDevServer(port).start());
    }
}
