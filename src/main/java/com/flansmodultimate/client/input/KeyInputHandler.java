package com.flansmodultimate.client.input;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketRequestDebug;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.LogicalSide;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyInputHandler
{
    private static final String CATEGORY = "key.categories." + FlansMod.MOD_ID;

    private static final KeyMapping reloadKey = new KeyMapping("key." + FlansMod.MOD_ID + ".reload", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);
    private static final KeyMapping lookAtGunKey = new KeyMapping("key." + FlansMod.MOD_ID + ".lookAtGun", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_M, CATEGORY);
    private static final KeyMapping debugKey = new KeyMapping("key." + FlansMod.MOD_ID + ".debug", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, InputConstants.KEY_F10, CATEGORY);


    //TODO: implement all key bindings

    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(reloadKey);
        event.register(lookAtGunKey);
        event.register(debugKey);
    }

    public static void checkKeys()
    {
        boolean noScreen = (Minecraft.getInstance().screen == null);

        if (isGunContext() && noScreen)
        {
            if (reloadKey.consumeClick())
            {
                doReload();
                return;
            }
            if (lookAtGunKey.consumeClick())
            {
                doLookAtGun();
            }
            if (debugKey.consumeClick())
            {
                if (ModClient.isDebug())
                    ModClient.setDebug(false);
                else
                    PacketHandler.sendToServer(new PacketRequestDebug());

            }
        }
    }

    private static boolean isGunContext()
    {
        Player player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem);
    }

    private static void doReload()
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        PlayerData data = PlayerData.getInstance(player, LogicalSide.CLIENT);
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offhandStack = player.getOffhandItem();

        if (data.getShootTimeRight() <= 0.0F && data.getShootTimeLeft() <= 0.0F)
        {
            if (mainHandStack.getItem() instanceof GunItem gunItem && !(offhandStack.getItem() instanceof GunItem))
            {
                if (gunItem.getBehavior().canReload(player.getInventory()))
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.MAIN_HAND));
            }
            else if (offhandStack.getItem() instanceof GunItem gunItem && !(mainHandStack.getItem() instanceof GunItem))
            {
                if (gunItem.getBehavior().canReload(player.getInventory()))
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.OFF_HAND));
            }
            else if (mainHandStack.getItem() instanceof GunItem mainHandGunItem && offhandStack.getItem() instanceof GunItem offhandGunItem)
            {
                if (offhandGunItem.getBehavior().canReload(player.getInventory())
                        && (!mainHandGunItem.getBehavior().canReload(player.getInventory()) || (!mainHandGunItem.getBehavior().hasEmptyAmmo(mainHandStack) && offhandGunItem.getBehavior().hasEmptyAmmo(offhandStack))))
                {
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.OFF_HAND));
                }
                else if (mainHandGunItem.getBehavior().canReload(player.getInventory()))
                {
                   PacketHandler.sendToServer(new PacketGunReload(InteractionHand.MAIN_HAND));
                }
            }
        }
    }


    private static void doLookAtGun()
    {
        Player player = Minecraft.getInstance().player;
        ModClient.getGunAnimations(player, InteractionHand.MAIN_HAND).lookAt = GunAnimations.LookAtState.TILT1;
        ModClient.getGunAnimations(player, InteractionHand.OFF_HAND).lookAt = GunAnimations.LookAtState.TILT1;
    }
}
