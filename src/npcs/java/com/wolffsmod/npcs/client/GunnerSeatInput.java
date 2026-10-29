package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.network.GunnerTriggerPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * The trigger of the seat gun the player mans in a gunner's seat of a vehicle NPC: the attack button, which then neither
 * swings nor strikes, sent to the server whenever it is pressed or let go. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunnerSeatInput
{
    /** What the server was last told. */
    private static boolean held;

    /** Once a client tick. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        boolean down = minecraft.player != null && minecraft.screen == null && mans(minecraft.player) && minecraft.options.keyAttack.isDown();
        if (down == held || minecraft.getConnection() == null)
            return;
        held = down;
        NpcNetwork.sendToServer(new GunnerTriggerPacket(down));
    }

    /** @return whether the attack button belongs to the seat gun the player mans rather than their hands */
    public static boolean mans(Player player)
    {
        return NpcCrew.mannedGun(player).isPresent();
    }

    /** Forgets the trigger with the connection, whose server lets go of it itself. */
    public static void clear()
    {
        held = false;
    }
}
