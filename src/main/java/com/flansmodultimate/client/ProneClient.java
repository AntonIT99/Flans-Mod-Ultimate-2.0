package com.flansmodultimate.client;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketProneRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Go Prone key: each press lies down or stands up, or with the holdToProne setting the player lies prone while it is held.
 * The client only asks; the server decides and answers, and the jump key stands a prone player up instead of jumping.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProneClient
{
    private static boolean lastRequested;
    private static int ticksSinceRequest = Integer.MAX_VALUE;
    private static boolean jumpWasDown;

    /** Each client tick, before the gun and driveable binds. */
    public static void checkKey(KeyMapping key, @Nullable LocalPlayer player, boolean noScreen)
    {
        boolean pressed = false;
        while (key.consumeClick())
            pressed = true;
        if (ticksSinceRequest < Integer.MAX_VALUE)
            ticksSinceRequest++;
        if (player == null || player.isPassenger())
            return;

        boolean prone = PlayerData.getInstance(player).isProne();
        ModClientConfig config = ModClientConfig.get();
        if (config != null && config.holdToProne)
        {
            // Opening a screen releases every key; the player stays as they are until they are back in the game.
            if (!noScreen)
                return;
            boolean wanted = key.isDown();
            // The server refuses to lie down again too soon after standing up, so a held key asks again now and then.
            if (wanted != prone && (wanted != lastRequested || ticksSinceRequest >= ProneSupport.LIE_DOWN_COOLDOWN))
                request(wanted);
        }
        else if (pressed && noScreen)
            request(!prone);
    }

    /** Takes the jump away from a prone player, and stands them up with it when each press of the Go Prone key switches. */
    public static void filterMovementInput(Player player, Input input)
    {
        boolean jumpDown = input.jumping;
        boolean jumpPressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;
        if (!(player instanceof LocalPlayer) || !PlayerData.getInstance(player).isProne())
            return;
        input.jumping = false;
        ModClientConfig config = ModClientConfig.get();
        if (jumpPressed && (config == null || !config.holdToProne))
            request(false);
    }

    private static void request(boolean prone)
    {
        lastRequested = prone;
        ticksSinceRequest = 0;
        PacketHandler.sendToServer(new PacketProneRequest(prone));
    }
}
