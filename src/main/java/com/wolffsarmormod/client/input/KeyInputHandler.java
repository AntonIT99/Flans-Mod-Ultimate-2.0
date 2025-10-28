package com.wolffsarmormod.client.input;

import com.flansmod.client.model.GunAnimations;
import com.mojang.blaze3d.platform.InputConstants;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.common.PlayerData;
import com.wolffsarmormod.common.item.GunItem;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.network.PacketHandler;
import com.wolffsarmormod.network.PacketReload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.LogicalSide;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyInputHandler
{
    private static final String CATEGORY = "key.categories." + ArmorMod.MOD_ID;

    private static final KeyMapping reloadKey = new KeyMapping("key." + ArmorMod.MOD_ID + ".reload", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);
    private static final KeyMapping lookAtGunKey = new KeyMapping("key." + ArmorMod.MOD_ID + ".lookAtGun", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_L, CATEGORY);

    //TODO: implement all key bindings

    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(reloadKey);
        event.register(lookAtGunKey);
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
        }
    }

    private static boolean isGunContext()
    {
        Player player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem);
    }

    private static void doReload()
    {
        Player player = Objects.requireNonNull(Minecraft.getInstance().player);
        PlayerData data = PlayerData.getInstance(player, LogicalSide.CLIENT);
        ItemStack stack = player.getMainHandItem();

        if (data.getShootTimeRight() <= 0.0F && stack.getItem() instanceof GunItem gunItem)
        {
            GunType type = gunItem.getConfigType();

            if (gunItem.getBehavior().canReload(player.getInventory()))
            {
                PacketHandler.sendToServer(new PacketReload(InteractionHand.MAIN_HAND, true));

                // Set player shoot delay to be the reload delay
                // Set both gun delays to avoid reloading two guns at once
                float reloadTime = type.getReloadTime(stack);
                data.setShootTimeRight(reloadTime);
                data.setShootTimeLeft(reloadTime);

                //TODO: Implement EnchantmentModule
                //float reloadDelay = EnchantmentModule.modifyReloadTime(type.reloadTime, player, player.getHeldItemOffhand());

                GunAnimations animations = ModClient.getGunAnimations(player, InteractionHand.MAIN_HAND);

                int pumpDelay = type.getModel() == null ? 0 : type.getModel().getPumpDelayAfterReload();
                int pumpTime = type.getModel() == null ? 1 : type.getModel().getPumpTime();
                animations.doReload(type.getReloadTime(), pumpDelay, pumpTime);

                data.setReloadingRight(true);
                data.setBurstRoundsRemainingRight(0);
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
