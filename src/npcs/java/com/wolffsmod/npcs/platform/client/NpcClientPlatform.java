package com.wolffsmod.npcs.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;

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

    /** Puts a text field's cursor at the start of its text without selecting anything. */
    public static void moveCursorToStart(EditBox box)
    {
        box.moveCursorToStart();
    }

    /**
     * Draws an NPC preview on a Custom NPCs screen, turned to the mouse or kept at the given rotation. The NeoForge build of
     * Custom NPCs has no overload with the switch, so its platform calls the entity display directly.
     */
    @SuppressWarnings("java:S107") // The Custom NPCs overload's seven arguments plus the screen that draws them.
    public static void drawNpc(GuiBasic screen, GuiGraphics graphics, LivingEntity entity, int x, int y, float zoom, int rotation, boolean followMouse)
    {
        screen.drawNpc(graphics, entity, x, y, zoom, rotation, followMouse);
    }

    /** Scales a label already turned to the camera so its text reads left to right, as name tags do. */
    public static void scaleCameraFacingLabel(PoseStack pose, float scale)
    {
        pose.scale(-scale, -scale, scale);
    }
}
