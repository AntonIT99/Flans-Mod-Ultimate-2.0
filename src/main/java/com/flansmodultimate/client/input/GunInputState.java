package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

import com.flansmodultimate.config.ModClientConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunInputState
{
    public record ButtonState(boolean isPressed, boolean isPrevPressed) {}

    private static ButtonState primaryFunctionState = new ButtonState(false, false);
    private static ButtonState offhandPrimaryFunctionState = new ButtonState(false, false);
    private static ButtonState secondaryFunctionState = new ButtonState(false, false);

    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.isPaused())
            return;

        if (Minecraft.getInstance().screen == null)
        {
            EnumMouseButton shootBtn = ModClientConfig.get().shootButton;
            EnumMouseButton shootOffBtn = ModClientConfig.get().shootButtonOffhand;
            EnumMouseButton aimBtn = ModClientConfig.get().aimButton;

            primaryFunctionState = new ButtonState(isMousePressed(shootBtn), primaryFunctionState.isPressed);
            offhandPrimaryFunctionState = new ButtonState(isMousePressed(shootOffBtn), offhandPrimaryFunctionState.isPressed);
            secondaryFunctionState = new ButtonState(isMousePressed(aimBtn), secondaryFunctionState.isPressed);
        }
        else
        {
            primaryFunctionState = new ButtonState(false, primaryFunctionState.isPressed);
            offhandPrimaryFunctionState = new ButtonState(false, offhandPrimaryFunctionState.isPressed);
            secondaryFunctionState = new ButtonState(false, secondaryFunctionState.isPressed);
        }
    }

    private static boolean isMousePressed(EnumMouseButton btn)
    {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, btn.toGlfw()) == GLFW.GLFW_PRESS;
    }

    public static ButtonState getPrimaryFunctionState(InteractionHand hand)
    {
        return (hand == InteractionHand.OFF_HAND) ? offhandPrimaryFunctionState : primaryFunctionState;
    }

    /**
     * The button of a gun's secondary function, such as aiming. An off-hand gun held alone fires with the
     * off-hand button, so when that is also the aim button it takes the free main hand's button instead, as
     * in 1.12.2.
     */
    public static EnumMouseButton getSecondaryButton(InteractionHand hand, boolean mainHandEmpty)
    {
        ModClientConfig config = ModClientConfig.get();
        if (hand == InteractionHand.OFF_HAND && mainHandEmpty && config.aimButton == config.shootButtonOffhand)
            return config.shootButton;
        return config.aimButton;
    }

    public static ButtonState getSecondaryFunctionState(InteractionHand hand, boolean mainHandEmpty)
    {
        return getSecondaryButton(hand, mainHandEmpty) == ModClientConfig.get().aimButton
            ? secondaryFunctionState : primaryFunctionState;
    }
}
