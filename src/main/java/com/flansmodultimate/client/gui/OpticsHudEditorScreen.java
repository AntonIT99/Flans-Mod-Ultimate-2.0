package com.flansmodultimate.client.gui;

import org.lwjgl.glfw.GLFW;

import com.flansmodultimate.common.driveables.OpticsHud;
import com.flansmodultimate.common.entity.Seat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Lightweight in-world HUD layout editor; copying exports the legacy syntax without modifying packs. */
public final class OpticsHudEditorScreen extends Screen
{
    private final Seat seat;
    private int selected;

    public OpticsHudEditorScreen(Seat seat)
    {
        super(Component.translatable("gui.flansmodultimate.optics.editor"));
        this.seat = seat;
    }

    @Override
    public void tick()
    {
        if (minecraft.player == null || minecraft.player.getVehicle() != seat || !seat.isScoped()) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, height - 52, width, height, 0xB0000000);
        graphics.drawCenteredString(font, title, width / 2, height - 46, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.flansmodultimate.optics.editor_controls"), width / 2, height - 32, 0xFFFFFF);
        graphics.drawCenteredString(font, OpticsHud.ELEMENTS[selected], width / 2, height - 18, 0x55FF55);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers)
    {
        OpticsHud hud = seat.getOpticsHud();
        if (hud == null) return super.keyPressed(key, scanCode, modifiers);
        int sight = seat.getCurrentSight();
        OpticsHud.Element element = hud.getElements()[selected];
        int step = hasShiftDown() ? 10 : 1;
        switch (key)
        {
            case GLFW.GLFW_KEY_TAB -> selected = (selected + 1) % 5;
            case GLFW.GLFW_KEY_LEFT -> element.move(sight, -step, 0);
            case GLFW.GLFW_KEY_RIGHT -> element.move(sight, step, 0);
            case GLFW.GLFW_KEY_UP -> element.move(sight, 0, -step);
            case GLFW.GLFW_KEY_DOWN -> element.move(sight, 0, step);
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> element.resize(sight, 0.1F);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> element.resize(sight, -0.1F);
            case GLFW.GLFW_KEY_C -> {
                StringBuilder lines = new StringBuilder();
                int sourceSeat = seat.getSeatIndex();
                for (var info : seat.getDriveable().getConfigType().getSeats())
                    if (info != null && info.getOptics().getHud() == hud) sourceSeat = info.getId();
                for (int i = 0; i < 5; i++)
                {
                    OpticsHud.Element e = hud.getElements()[i];
                    String prefix = "SeatOptics" + OpticsHud.ELEMENTS[i];
                    lines.append(String.format(Locale.ROOT, "%sPosSight %d %d %d %d%n%sScaleSight %d %d %.2f%n",
                        prefix, sourceSeat, sight + 1, e.x(sight), e.y(sight),
                        prefix, sourceSeat, sight + 1, e.scale(sight)));
                }
                minecraft.keyboardHandler.setClipboard(lines.toString());
            }
            default -> { return super.keyPressed(key, scanCode, modifiers); }
        }
        return true;
    }
}
