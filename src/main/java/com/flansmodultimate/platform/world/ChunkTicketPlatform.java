package com.flansmodultimate.platform.world;

import com.flansmodultimate.FlansMod;
import net.minecraftforge.common.world.ForgeChunkManager;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** Version boundary for persistent chunk tickets used by the Teams game mode: Forge ForgeChunkManager or a NeoForge ticket controller. */
public final class ChunkTicketPlatform
{
    private ChunkTicketPlatform() {}

    /** Adds or removes the owner's forced chunk; returns whether the ticket state changed. */
    public static boolean force(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking)
    {
        return ForgeChunkManager.forceChunk(level, FlansMod.MOD_ID, owner, chunkX, chunkZ, add, ticking);
    }
}
