package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends NPCs that Teleport home when inactive straight back to their start position while they do not simulate: beyond
 * the server's simulation distance, where their own return-home goal cannot run, or resting beyond their dormant range.
 *
 * <p>
 * Only an NPC that Custom NPCs would walk home anyway is moved, never while a player is near enough to have it on their
 * client, and only once the chunk of its start position and its entities are loaded. Server side, once a second.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcInactiveReturn
{
    private static final int INTERVAL = 20;
    /** Distance in blocks from its start within which a standing NPC is left to walk the rest itself. */
    private static final double STANDING_MARGIN = 2D;

    /** At the end of every server level tick. */
    public static void levelTick(ServerLevel level)
    {
        if (level.getGameTime() % INTERVAL != 0)
            return;
        // Collected first: moving an NPC while the level's entities are being walked would change what is walked.
        List<EntityNPCInterface> homeward = new ArrayList<>();
        for (Entity entity : level.getAllEntities())
        {
            if (entity instanceof EntityNPCInterface npc && shouldReturn(level, npc))
                homeward.add(npc);
        }
        homeward.forEach(NpcInactiveReturn::sendHome);
    }

    private static boolean shouldReturn(ServerLevel level, EntityNPCInterface npc)
    {
        if (npc.isRemoved() || !NpcEquipment.enabled(npc, Feature.RETURN_WHEN_INACTIVE) || !npc.ais.shouldReturnHome() || npc.hasOwner() || npc.isPassenger() || npc.isKilled() || npc.isInteracting())
            return false;
        if (level.isPositionEntityTicking(npc.blockPosition()) && !NpcDormancy.dormant(npc))
            return false;
        ChunkPos start = new ChunkPos(npc.ais.startPos());
        return level.getChunkSource().hasChunk(start.x, start.z) && level.areEntitiesLoaded(start.toLong()) && awayFromHome(npc) && !watched(level, npc);
    }

    /** @return whether the NPC's own return-home goal would set off for its start, as Custom NPCs decides it */
    private static boolean awayFromHome(EntityNPCInterface npc)
    {
        double x = npc.getX() - npc.getStartXPos();
        double z = npc.getZ() - npc.getStartZPos();
        return switch (npc.ais.getMovingType())
        {
            case 0 -> x * x + z * z > STANDING_MARGIN * STANDING_MARGIN;
            // Wandering range 0 roams without limit, as the return goal treats it; a height below the world ignores height.
            case 1 -> npc.ais.walkingRange != 0 && !npc.isInRange(npc.getStartXPos(), Double.NEGATIVE_INFINITY, npc.getStartZPos(), npc.ais.walkingRange);
            case 2 -> npc.ais.distanceToSqrToPathPoint() >= (double) CustomNpcs.NpcNavRange * CustomNpcs.NpcNavRange;
            default -> false;
        };
    }

    /** @return whether a player is within the horizontal distance at which the server sends the NPC to their client */
    private static boolean watched(ServerLevel level, EntityNPCInterface npc)
    {
        double range = level.getServer().getScaledTrackingDistance(npc.getType().clientTrackingRange() * 16);
        for (Player player : level.players())
        {
            double x = player.getX() - npc.getX();
            double z = player.getZ() - npc.getZ();
            if (x * x + z * z <= range * range)
                return true;
        }
        return false;
    }

    private static void sendHome(EntityNPCInterface npc)
    {
        npc.setTarget(null);
        npc.getNavigation().stop();
        npc.setDeltaMovement(Vec3.ZERO);
        npc.resetFallDistance();
        // Also carries any crew riding the NPC along.
        npc.teleportTo(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
    }
}
