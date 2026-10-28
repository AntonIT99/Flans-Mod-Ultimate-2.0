package com.wolffsmod.npcs.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Version boundary for the client screen and render calls the module's screens and labels make. Client side. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcClientPlatform
{
    /** Draws the dimmed background behind a screen. */
    // Part of the cross-version platform signature; 1.21 also passes the mouse and partial tick.
    @SuppressWarnings({"java:S1172", "unused"})
    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics);
    }

    /** Scales a label already turned to the camera so its text reads left to right, as name tags do. */
    public static void scaleCameraFacingLabel(PoseStack pose, float scale)
    {
        pose.scale(-scale, -scale, scale);
    }
}
