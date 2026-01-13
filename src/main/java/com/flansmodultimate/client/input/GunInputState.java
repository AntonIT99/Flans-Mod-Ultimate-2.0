package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

import com.flansmodultimate.config.ModClientConfigs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunInputState
{
    private static boolean shootPressed;
    private static boolean prevShootPressed;
    private static boolean offhandShootPressed;
    private static boolean prevOffhandShootPressed;
    @Getter
    private static boolean prevAimPressed;
    @Getter
    private static boolean aimPressed;

    @OnlyIn(Dist.CLIENT)
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.isPaused() || mc.screen != null)
            return;

        EnumMouseButton shootBtn = ModClientConfigs.shootButton.get();
        EnumMouseButton shootOffBtn = ModClientConfigs.shootButtonOffhand.get();
        EnumMouseButton aimBtn = ModClientConfigs.aimButton.get();

        prevShootPressed = shootPressed;
        prevOffhandShootPressed = offhandShootPressed;
        prevAimPressed = aimPressed;
        shootPressed = isMousePressed(shootBtn);
        offhandShootPressed = isMousePressed(shootOffBtn);
        aimPressed = isMousePressed(aimBtn);
    }

    private static boolean isMousePressed(EnumMouseButton btn)
    {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, btn.toGlfw()) == GLFW.GLFW_PRESS;
    }

    public static boolean isShootPressed(InteractionHand hand)
    {
        return (hand == InteractionHand.OFF_HAND) ? offhandShootPressed : shootPressed;
    }

    public static boolean isPrevShootPressed(InteractionHand hand)
    {
        return (hand == InteractionHand.OFF_HAND) ? prevOffhandShootPressed : prevShootPressed;
    }
}
