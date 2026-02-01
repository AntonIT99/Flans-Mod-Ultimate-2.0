package com.flansmodultimate.hooks.client;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.input.GunInputState;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.GunItemHandler;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.hooks.IClientGunHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketDeployedGunInput;
import com.flansmodultimate.network.server.PacketGunInput;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ClientGunHooksImpl implements IClientGunHooks
{
    @Override
    public void clientTickGunItem(GunItem gunItem, Level level, Player player, PlayerData data, ItemStack gunStack, InteractionHand hand, boolean dualWield)
    {
        if (player != Minecraft.getInstance().player || gunItem.getConfigType().isDeployable() || !gunItem.getConfigType().isUsableByPlayers() || !gunItem.getGunItemHandler().gunCanBeHandled(player))
            return;

        GunType configType = gunItem.getConfigType();
        GunItemHandler gunItemHandler = gunItem.getGunItemHandler();

        // Force release actions when entering a GUI
        if (Minecraft.getInstance().screen != null && (data.isShooting(hand) || data.isSecondaryFunctionKeyPressed()))
        {
            data.setShootKeyPressed(hand, false);
            data.setSecondaryFunctionKeyPressed(false);
            PacketHandler.sendToServer(new PacketGunInput(false, data.isPrevShootKeyPressed(hand), false, hand));
        }

        GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(hand);
        GunInputState.ButtonState secondaryFunctionState = GunInputState.getSecondaryFunctionState();

        // Scope handling
        gunItemHandler.handleScope(player, gunStack, primaryFunctionState, secondaryFunctionState, dualWield);

        GunAnimations animations = ModClient.getGunAnimations(player, hand);

        // Switch Delay
        gunItemHandler.handleGunSwitchDelay(data, animations, hand);

        // Client Shooting
        if (data.isShooting(hand))
            gunItemHandler.doPlayerShootClient(level, player, data, animations, gunStack, hand);

        if (ModClient.getSwitchTime() <= 0)
        {
            // Don’t shoot certain entities under crosshair
            if (configType.getPrimaryFunction() == EnumFunction.SHOOT && gunItemHandler.shouldBlockFireAtCrosshair())
                primaryFunctionState = new GunInputState.ButtonState(false, primaryFunctionState.isPrevPressed());

            data.setShootKeyPressed(hand, primaryFunctionState.isPressed());
            data.setPrevShootKeyPressed(hand, primaryFunctionState.isPrevPressed());
            data.setSecondaryFunctionKeyPressed(secondaryFunctionState.isPressed());

            // Send update to server when keys are pressed or released
            if (primaryFunctionState.isPressed() != primaryFunctionState.isPrevPressed() || secondaryFunctionState.isPressed() != secondaryFunctionState.isPrevPressed())
                PacketHandler.sendToServer(new PacketGunInput(primaryFunctionState.isPressed(), primaryFunctionState.isPrevPressed(), secondaryFunctionState.isPressed(), hand));
        }
    }

    @Override
    public void clientTickDeployedGun(DeployedGun deployedGun)
    {
        if (deployedGun.getFirstPassenger() != Minecraft.getInstance().player)
            return;

        // Force release key when entering a GUI
        if (Minecraft.getInstance().screen != null)
        {
            if (deployedGun.isShootKeyPressed())
            {
                deployedGun.setShootKeyPressed(false);
                PacketHandler.sendToServer(new PacketDeployedGunInput(deployedGun, false, deployedGun.isPrevShootKeyPressed()));
            }
        }
        else
        {
            GunInputState.ButtonState primaryFunctionState = GunInputState.getPrimaryFunctionState(InteractionHand.MAIN_HAND);
            deployedGun.setShootKeyPressed(primaryFunctionState.isPressed());
            deployedGun.setPrevShootKeyPressed(primaryFunctionState.isPrevPressed());

            // Send update to server when key is pressed or released
            if (deployedGun.isShootKeyPressed() != deployedGun.isPrevShootKeyPressed())
                PacketHandler.sendToServer(new PacketDeployedGunInput(deployedGun, deployedGun.isShootKeyPressed(), deployedGun.isPrevShootKeyPressed()));
        }
    }

    @Override
    @Nullable
    public HitResult getClientHitResult()
    {
        return Minecraft.getInstance().hitResult;
    }
}
