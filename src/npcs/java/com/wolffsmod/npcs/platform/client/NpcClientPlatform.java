package com.wolffsmod.npcs.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.client.gui.custom.components.CustomGuiEntityDisplay;
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
    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    /** Scales a label already turned to the camera so its text reads left to right, as name tags do. */
    public static void scaleCameraFacingLabel(PoseStack pose, float scale)
    {
        // Minecraft 1.21 turns the camera rotation half a turn further than 1.20.1, so the text keeps its X axis.
        pose.scale(scale, -scale, scale);
    }

    /** Puts a text field's cursor at the start of its text without selecting anything. */
    public static void moveCursorToStart(EditBox box)
    {
        box.moveCursorToStart(false);
    }

    /**
     * Draws an NPC preview on a Custom NPCs screen, turned to the mouse or kept at the given rotation. The installed
     * NeoForge build has no {@code drawNpc} overload with the switch, so this calls the entity display it would have used.
     */
    public static void drawNpc(GuiBasic screen, GuiGraphics graphics, LivingEntity entity, int x, int y, float zoom, int rotation, boolean followMouse)
    {
        CustomGuiEntityDisplay.drawEntity(graphics, entity, x, y, zoom, rotation, screen.wrapper.mouseX, screen.wrapper.mouseY, screen.guiLeft, screen.guiTop, followMouse);
    }
}
