package com.flansmodultimate.platform.world;

import com.flansmodultimate.FlansMod;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** Version boundary for persistent chunk tickets used by the Teams game mode: Forge ForgeChunkManager or a NeoForge ticket controller. */
public final class ChunkTicketPlatform
{
    private static final TicketController CONTROLLER = new TicketController(
        ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "teams"));

    private ChunkTicketPlatform() {}

    public static void register(RegisterTicketControllersEvent event)
    {
        event.register(CONTROLLER);
    }

    /** Adds or removes the owner's forced chunk; returns whether the ticket state changed. */
    public static boolean force(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking)
    {
        return CONTROLLER.forceChunk(level, owner, chunkX, chunkZ, add, ticking);
    }
}
