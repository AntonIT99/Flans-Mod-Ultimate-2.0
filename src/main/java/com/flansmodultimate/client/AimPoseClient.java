package com.flansmodultimate.client;

import com.flansmodultimate.client.gui.options.ConfigOptionFactory;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.config.EnumAimPose;
import com.flansmodultimate.config.EnumPlayerAimPose;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketAimPosePreference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The player's own aim pose choice: switched from the options screen or the Toggle Aim Pose key, and sent
 * to the server so that everyone else sees the player hold their guns the way they chose.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AimPoseClient
{
    public static void sendToServer()
    {
        ModClientConfig config = ModClientConfig.get();
        Minecraft minecraft = Minecraft.getInstance();
        // The client config is baked before the game exists, and there is nothing to tell while not connected
        if (config == null || minecraft == null || minecraft.getConnection() == null)
            return;

        boolean dynamic = config.aimPose == EnumAimPose.DYNAMIC;
        // Shown at once in third person rather than after the server echoes it back
        if (minecraft.player != null)
            PlayerData.getInstance(minecraft.player).setDynamicAimPose(dynamic);
        PacketHandler.sendToServer(new PacketAimPosePreference(dynamic));
    }

    /** Switches between the enforced and the dynamic aim pose, and says which one is now in force. */
    public static void toggle()
    {
        Minecraft minecraft = Minecraft.getInstance();
        ModClientConfig config = ModClientConfig.get();
        if (config == null || minecraft.player == null)
            return;

        EnumAimPose next = config.aimPose == EnumAimPose.ENFORCED ? EnumAimPose.DYNAMIC : EnumAimPose.ENFORCED;
        ModClientConfig.setAndSave(ModClientConfig.AIM_POSE, next);

        Component choice = ConfigOptionFactory.valueLabel(EnumAimPose.class, next);
        EnumPlayerAimPose serverRule = ModCommonConfig.playerAimPose();
        Component message = serverRule == EnumPlayerAimPose.FREE_CHOICE
            ? Component.translatable("message.flansmodultimate.aim_pose", choice)
            : Component.translatable("message.flansmodultimate.aim_pose_server_forced", choice,
                ConfigOptionFactory.valueLabel(EnumAimPose.class,
                    serverRule == EnumPlayerAimPose.DYNAMIC ? EnumAimPose.DYNAMIC : EnumAimPose.ENFORCED));
        minecraft.player.displayClientMessage(message, true);
    }
}
