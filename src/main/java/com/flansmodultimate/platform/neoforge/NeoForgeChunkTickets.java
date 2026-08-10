package com.flansmodultimate.platform.neoforge;

import com.flansmodultimate.FlansMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.util.UUID;

/** NeoForge boundary for persistent chunk tickets used by the Teams game mode. */
public final class NeoForgeChunkTickets
{
    private static final TicketController CONTROLLER = new TicketController(
        ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "teams"));

    private NeoForgeChunkTickets() {}

    public static void register(RegisterTicketControllersEvent event)
    {
        event.register(CONTROLLER);
    }

    public static boolean force(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking)
    {
        return CONTROLLER.forceChunk(level, owner, chunkX, chunkZ, add, ticking);
    }
}
