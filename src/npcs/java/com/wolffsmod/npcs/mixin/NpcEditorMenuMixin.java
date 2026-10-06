package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.GuiWolffsMod;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/** Adds the module's page to Custom NPCs' shared editor navigation. */
@Mixin(value = GuiNpcMenu.class, remap = false)
public abstract class NpcEditorMenuMixin
{
    @Shadow
    private IGuiInterface parent;
    @Shadow
    private GuiMenuTopButton[] topButtons;
    @Shadow
    private int activeMenu;
    @Shadow
    private EntityNPCInterface npc;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void addWolffsModTab(int guiLeft, int guiTop, int width, CallbackInfo callback)
    {
        if (npc == null)
            return;

        GuiMenuTopButton global = topButtons[5];
        GuiMenuTopButton tab = GuiWolffsMod.createTab(parent, npc, global.getX() + global.getWidth(), guiTop - 17);
        tab.active = activeMenu == GuiWolffsMod.MENU_ID;
        // Leave Delete and Close reachable even when translated tab labels fill the row.
        if (tab.getX() + tab.getWidth() > topButtons[7].getX())
        {
            tab.setX(guiLeft + 4);
            tab.setY(guiTop - 39);
        }
        topButtons = Arrays.copyOf(topButtons, topButtons.length + 1);
        topButtons[topButtons.length - 1] = tab;
    }
}
