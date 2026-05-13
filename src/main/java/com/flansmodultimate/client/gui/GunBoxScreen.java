package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.inventory.GunBoxMenu;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GunBoxScreen extends AbstractContainerScreen<GunBoxMenu>
{
    public GunBoxScreen(GunBoxMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gg, float partialTick, int mouseX, int mouseY)
    {

    }
}
