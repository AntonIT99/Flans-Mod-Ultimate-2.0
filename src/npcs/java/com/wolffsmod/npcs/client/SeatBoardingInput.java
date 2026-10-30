package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.crew.NpcSeatInteraction;
import com.wolffsmod.npcs.network.BoardSeatPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/** Routes right-clicks on empty vehicle NPC seats through the NPC's normal interaction, even outside its central hitbox. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeatBoardingInput
{
    /** @return whether a use click was sent for the nearest empty seat under the crosshair */
    public static boolean use(InteractionHand hand)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || minecraft.player.isSpectator() || minecraft.player.isSecondaryUseActive())
            return false;
        EntityNPCInterface chosen = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (Entity entity : minecraft.level.entitiesForRendering())
        {
            if (!(entity instanceof EntityNPCInterface npc) || !npc.isAlive() || npc.isKilled() || minecraft.player.getVehicle() == npc)
                continue;
            NpcSeatInteraction.Hit hit = NpcSeatInteraction.pick(npc, minecraft.player).orElse(null);
            if (hit != null && !NpcCrew.occupied(npc, hit.mount().seat()))
            {
                double distance = minecraft.player.getEyePosition().distanceToSqr(hit.point());
                if (distance < nearest)
                {
                    chosen = npc;
                    nearest = distance;
                }
            }
        }
        if (chosen == null)
            return false;
        NpcNetwork.sendToServer(new BoardSeatPacket(chosen.getId(), hand));
        return true;
    }
}
