package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

import com.flansmodultimate.config.ModClientConfigs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunInputState
{
    @Getter
    private static boolean shootPressed;
    @Getter
    private static boolean offhandShootPressed;
    @Getter
    private static boolean aimPressed;
    @Getter
    private static boolean prevShootPressed;
    @Getter
    private static boolean prevOffhandShootPressed;
    @Getter
    private static boolean prevAimPressed;

    @OnlyIn(Dist.CLIENT)
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.isPaused() || mc.screen != null)
            return;

        // Read config every tick (fine) or cache & refresh on config reload if you prefer
        EnumActionButton shootBtn = ModClientConfigs.shootButton.get();
        EnumActionButton shootOffBtn = ModClientConfigs.shootButtonOffhand.get();
        EnumActionButton aimBtn = ModClientConfigs.aimButton.get();

        prevShootPressed = shootPressed;
        prevOffhandShootPressed = offhandShootPressed;
        prevAimPressed = aimPressed;
        shootPressed = isMousePressed(shootBtn);
        offhandShootPressed = isMousePressed(shootOffBtn);
        aimPressed = isMousePressed(aimBtn);
    }

    private static boolean isMousePressed(EnumActionButton btn)
    {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, btn.toGlfw()) == GLFW.GLFW_PRESS;
    }
}
