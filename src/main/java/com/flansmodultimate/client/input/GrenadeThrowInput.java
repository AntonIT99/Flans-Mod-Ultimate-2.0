package com.flansmodultimate.client.input;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.gun.PacketGrenadeThrow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client side of the Labjac Edition's hold-to-throw grenades. Holding attack and releasing it throws
 * overhand, holding use and releasing it tosses underhand; the invertGrenadeThrowButtons client setting swaps the two. A quick click throws at full speed; holding
 * longer charges the throw from no speed up to full speed, like a bow, and the HUD shows the charge.
 * The release is sent to the server, which validates and throws. The button pressed first decides the
 * throw. Switching items, opening a screen or getting into a vehicle while holding puts the grenade away
 * without throwing it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrenadeThrowInput
{
    private enum Hold
    {
        NONE, OVERHAND, UNDERHAND
    }

    private static Hold hold = Hold.NONE;
    /** Ticks the held button has been down since the tick it was pressed */
    private static int heldTicks;

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

        ModClientConfig config = ModClientConfig.get();
        boolean inverted = config != null && config.invertGrenadeThrowButtons;
        // Overhand and underhand buttons, swapped by the invertGrenadeThrowButtons setting
        boolean overhandDown = (inverted ? mc.options.keyUse : mc.options.keyAttack).isDown();
        boolean underhandDown = (inverted ? mc.options.keyAttack : mc.options.keyUse).isDown();
        switch (hold)
        {
            case NONE -> {
                heldTicks = 0;
                if (overhandDown)
                    hold = Hold.OVERHAND;
                else if (underhandDown)
                    hold = Hold.UNDERHAND;
            }
            case OVERHAND -> {
                if (overhandDown)
                    heldTicks++;
                else
                    release(heldGrenade(player), false);
            }
            case UNDERHAND -> {
                if (underhandDown)
                    heldTicks++;
                else
                    release(heldGrenade(player), true);
            }
        }
    }

    /**
     * Charge of the throw being held, from 0 to 1, for the HUD; -1 when no throw is charging, including
     * during the quick click window that throws at full speed.
     */
    public static float chargeProgress(Player player, float partialTick)
    {
        if (hold == Hold.NONE || !isHoldingHoldToThrowGrenade(player))
            return -1F;
        GrenadeType type = heldGrenade(player);
        float held = heldTicks + partialTick;
        if (held <= GrenadeType.QUICK_THROW_TICKS || type.getThrowChargeTime() <= 0)
            return -1F;
        return type.handThrowCharge(held);
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

    private static GrenadeType heldGrenade(Player player)
    {
        return ((GrenadeItem) player.getMainHandItem().getItem()).getConfigType();
    }

    private static void release(GrenadeType type, boolean underhand)
    {
        hold = Hold.NONE;
        PacketHandler.sendToServer(new PacketGrenadeThrow(underhand, type.handThrowCharge(heldTicks)));
    }
}
