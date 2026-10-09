package com.flansmodultimate.client.input;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.gun.PacketGrenadeThrow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client side of the Labjac Edition's hold-to-throw grenades. Holding attack and releasing it throws
 * overhand, holding use and releasing it tosses underhand. The release is sent to the server, which
 * validates and throws. The button pressed first decides the throw. Switching items, opening a screen
 * or getting into a vehicle while holding puts the grenade away without throwing it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrenadeThrowInput
{
    private enum Hold
    {
        NONE, OVERHAND, UNDERHAND
    }

    private static Hold hold = Hold.NONE;

    /** Samples the attack and use buttons once per client tick. */
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null || !isHoldingHoldToThrowGrenade(player))
        {
            hold = Hold.NONE;
            return;
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean useDown = mc.options.keyUse.isDown();
        switch (hold)
        {
            case NONE -> {
                if (attackDown)
                    hold = Hold.OVERHAND;
                else if (useDown)
                    hold = Hold.UNDERHAND;
            }
            case OVERHAND -> {
                if (!attackDown)
                    release(false);
            }
            case UNDERHAND -> {
                if (!useDown)
                    release(true);
            }
        }
    }

    /** Whether the player holds, in the main hand, a grenade that is thrown by holding a button. */
    public static boolean isHoldingHoldToThrowGrenade(Player player)
    {
        if (!(player.getMainHandItem().getItem() instanceof GrenadeItem grenadeItem) || !grenadeItem.getConfigType().isHoldToThrow())
            return false;
        Entity vehicle = player.getVehicle();
        return !(vehicle instanceof DeployedGun || vehicle instanceof AAGun) && KeyInputHandler.resolveDriveable(player) == null;
    }

    /** Forgets any held throw, for example when leaving a world. */
    public static void reset()
    {
        hold = Hold.NONE;
    }

    private static void release(boolean underhand)
    {
        hold = Hold.NONE;
        PacketHandler.sendToServer(new PacketGrenadeThrow(underhand));
    }
}
