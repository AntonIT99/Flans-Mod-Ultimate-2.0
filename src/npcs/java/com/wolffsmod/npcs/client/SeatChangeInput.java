package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.driveables.FlansDriveableKeys;
import com.mojang.blaze3d.platform.InputConstants;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.network.ChangeSeatPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Flan's seat key while the player rides a vehicle NPC: each press moves them to its next free seat, as in a Flan's
 * driveable. That bind is live only in Flan's own driveables, so the key it is bound to is read directly. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeatChangeInput
{
    /** Whether the key was down last tick, so that holding it moves the player once. */
    private static boolean down;

    /** Once a client tick. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        boolean pressed = minecraft.player != null && minecraft.screen == null && minecraft.player.getVehicle() instanceof EntityNPCInterface vehicle && !NpcCrew.seats(vehicle).isEmpty()
            && held(minecraft, FlansDriveableKeys.changeSeat());
        if (pressed && !down && minecraft.getConnection() != null)
            NpcNetwork.sendToServer(new ChangeSeatPacket());
        down = pressed;
    }

    private static boolean held(Minecraft minecraft, KeyMapping bind)
    {
        if (bind.isUnbound())
            return false;
        InputConstants.Key key = bind.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return InputConstants.isKeyDown(window, key.getValue());
    }
}
